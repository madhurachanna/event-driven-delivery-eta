package com.delivery.payment.entity;

/**
 * Payment status enum.
 * 
 * CONCEPT: State Machine
 * Payments transition: PENDING → AUTHORIZED or PENDING → FAILED
 * Once AUTHORIZED or FAILED, the payment is final.
 */
public enum PaymentStatus {
    PENDING, // Payment created, not yet processed
    AUTHORIZED, // Payment successful
    FAILED // Payment failed (insufficient funds, card declined, etc.)
}
