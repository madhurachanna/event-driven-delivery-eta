package com.delivery.payment.entity;

/** Payment lifecycle: PENDING → AUTHORIZED or PENDING → FAILED (terminal). */
public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    FAILED
}
