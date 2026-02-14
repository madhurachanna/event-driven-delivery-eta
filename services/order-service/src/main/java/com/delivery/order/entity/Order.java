package com.delivery.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order entity — persisted to Postgres.
 * 
 * CONCEPT: JPA Entity
 * - @Entity marks this class for database persistence
 * - @Table specifies the table name
 * - @Id marks the primary key
 * - Hibernate auto-creates/updates table based on this class
 * 
 * CONCEPT: @OneToMany
 * - One Order has many OrderItems
 * - CascadeType.ALL = when we save/delete Order, items are saved/deleted too
 * - orphanRemoval = if an item is removed from the list, it's deleted from DB
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Business identifier for the order (UUID string).
     * This is what we use in Kafka events and across services.
     * Different from the database primary key (id).
     */
    @Column(nullable = false, unique = true)
    private String orderId;

    /**
     * Customer who placed the order.
     */
    @Column(nullable = false)
    private String customerId;

    /**
     * Total order amount (sum of all line items).
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Currency code (e.g., USD, EUR).
     */
    @Column(nullable = false, length = 3)
    private String currency;

    /**
     * Order status — tracks lifecycle through the event-driven pipeline.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    /**
     * When the order was created.
     */
    @Column(nullable = false)
    private Instant createdAt;

    /**
     * When the order was last updated.
     */
    private Instant updatedAt;

    /**
     * Line items in this order.
     * CascadeType.ALL = save/delete items with the order.
     * orphanRemoval = delete items removed from the list.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    // Default constructor for JPA
    public Order() {
    }

    /**
     * Factory method to create a new order.
     * Generates a UUID for orderId and sets initial status to CREATED.
     */
    public static Order create(String customerId, String currency) {
        Order order = new Order();
        order.orderId = UUID.randomUUID().toString();
        order.customerId = customerId;
        order.currency = currency;
        order.status = OrderStatus.CREATED;
        order.totalAmount = BigDecimal.ZERO;
        order.createdAt = Instant.now();
        return order;
    }

    /**
     * Add an item to this order and recalculate total.
     */
    public void addItem(OrderItem item) {
        items.add(item);
        recalculateTotal();
    }

    /**
     * Recalculate total amount from all line items.
     */
    private void recalculateTotal() {
        this.totalAmount = items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Status transitions
    public void markInventoryReserved() {
        this.status = OrderStatus.INVENTORY_RESERVED;
        this.updatedAt = Instant.now();
    }

    public void markInventoryRejected() {
        this.status = OrderStatus.INVENTORY_REJECTED;
        this.updatedAt = Instant.now();
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
