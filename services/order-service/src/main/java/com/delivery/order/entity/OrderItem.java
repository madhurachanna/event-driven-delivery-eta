package com.delivery.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * OrderItem entity — a line item within an order.
 * 
 * CONCEPT: JPA @ManyToOne
 * - Each OrderItem belongs to one Order
 * - @JoinColumn specifies the foreign key column in the order_items table
 * - The Order entity has a @OneToMany back-reference
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the parent order.
     * FetchType.LAZY = don't load Order unless accessed (performance).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Product SKU/identifier.
     */
    @Column(nullable = false)
    private String itemId;

    /**
     * How many of this item were ordered.
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Price per unit.
     * Using BigDecimal for money — never use float/double for currency!
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // Default constructor for JPA
    public OrderItem() {
    }

    // Factory method
    public static OrderItem create(Order order, String itemId, Integer quantity, BigDecimal unitPrice) {
        OrderItem item = new OrderItem();
        item.order = order;
        item.itemId = itemId;
        item.quantity = quantity;
        item.unitPrice = unitPrice;
        return item;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public String getItemId() {
        return itemId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    /**
     * Line total = quantity × unitPrice
     */
    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
