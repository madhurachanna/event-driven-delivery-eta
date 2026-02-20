package com.delivery.payment.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/** Payment entity persisted to Postgres. */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderId;

    /**
     * Unique key (usually Kafka eventId) enforced by DB to prevent double charges.
     */
    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    public Payment() {
    }

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

    public void authorize() {
        this.status = PaymentStatus.AUTHORIZED;
        this.updatedAt = Instant.now();
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
        this.updatedAt = Instant.now();
    }

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
