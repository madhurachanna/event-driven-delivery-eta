package com.delivery.payment.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payment entity - persisted to Postgres.
 * 
 * CONCEPT: JPA Entity
 * - @Entity marks this class for database persistence
 * - @Table specifies the table name
 * - @Id marks the primary key
 * - Hibernate auto-creates/updates table based on this class
 * 
 * CONCEPT: Idempotency Key
 * - Kafka delivers at-least-once → duplicates possible
 * - We store 'idempotencyKey' (usually eventId) with UNIQUE constraint
 * - If we try to insert duplicate → database rejects it
 * - This prevents processing same payment twice!
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The order this payment is for.
     */
    @Column(nullable = false)
    private String orderId;

    /**
     * Unique key to prevent duplicate processing.
     * Usually the eventId from the Kafka message.
     */
    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    /**
     * Payment amount.
     * Using BigDecimal for money - never use float/double for currency!
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Currency code (e.g., USD, EUR).
     */
    @Column(nullable = false, length = 3)
    private String currency;

    /**
     * Payment status: PENDING, AUTHORIZED, FAILED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    /**
     * When the payment was created.
     */
    @Column(nullable = false)
    private Instant createdAt;

    /**
     * When the payment was last updated.
     */
    private Instant updatedAt;

    // Default constructor for JPA
    public Payment() {
    }

    // Builder-style creation
    public static Payment create(String orderId, String idempotencyKey,
            BigDecimal amount, String currency) {
        Payment payment = new Payment();
        payment.orderId = orderId;
        payment.idempotencyKey = idempotencyKey;
        payment.amount = amount;
        payment.currency = currency;
        payment.status = PaymentStatus.PENDING;
        payment.createdAt = Instant.now();
        return payment;
    }

    // Status transitions
    public void authorize() {
        this.status = PaymentStatus.AUTHORIZED;
        this.updatedAt = Instant.now();
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
