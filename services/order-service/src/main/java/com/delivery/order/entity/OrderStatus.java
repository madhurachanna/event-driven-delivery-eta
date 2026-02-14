package com.delivery.order.entity;

/**
 * Order status enum.
 * 
 * CONCEPT: State Machine
 * Orders transition through these states as events flow through the system:
 * CREATED → INVENTORY_RESERVED → PAYMENT_AUTHORIZED (happy path)
 * CREATED → INVENTORY_REJECTED (no stock)
 * CREATED → INVENTORY_RESERVED → PAYMENT_FAILED (payment declined)
 */
public enum OrderStatus {
    CREATED, // Order placed, awaiting inventory check
    INVENTORY_RESERVED, // Stock reserved, awaiting payment
    INVENTORY_REJECTED, // Insufficient stock
    PAYMENT_AUTHORIZED, // Payment successful, ready for delivery
    PAYMENT_FAILED // Payment declined
}
