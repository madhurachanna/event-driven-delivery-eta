package com.delivery.common.event;

/**
 * Constants for Kafka topic names.
 * 
 * All services reference these constants to ensure consistency.
 * Topics follow the pattern: {namespace}.{domain}-events
 */
public final class Topics {

    private Topics() {
    } // Prevent instantiation

    // Main event topics (one per domain)
    public static final String ORDER_EVENTS = "raw.order-events";
    public static final String INVENTORY_EVENTS = "raw.inventory-events";
    public static final String PAYMENT_EVENTS = "raw.payment-events";
    public static final String DELIVERY_EVENTS = "raw.delivery-events";

    // Dead-letter queues
    public static final String DLQ_ORDER_EVENTS = "dlq.order-events";
    public static final String DLQ_INVENTORY_EVENTS = "dlq.inventory-events";
    public static final String DLQ_PAYMENT_EVENTS = "dlq.payment-events";
    public static final String DLQ_DELIVERY_EVENTS = "dlq.delivery-events";
}
