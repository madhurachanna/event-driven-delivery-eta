package com.delivery.common.event;

/**
 * Constants for event types.
 * 
 * Event types follow the pattern: {domain}.{action}
 * These are used in the eventType field of EventEnvelope.
 */
public final class EventTypes {

    private EventTypes() {
    } // Prevent instantiation

    // Order events (produced by order-service)
    public static final String ORDER_CREATED = "order.created";

    // Inventory events (produced by inventory-service)
    public static final String INVENTORY_RESERVED = "inventory.reserved";
    public static final String INVENTORY_REJECTED = "inventory.rejected";

    // Payment events (produced by payment-service)
    public static final String PAYMENT_AUTHORIZED = "payment.authorized";
    public static final String PAYMENT_FAILED = "payment.failed";

    // Delivery events (produced by delivery-service)
    public static final String DELIVERY_ASSIGNED = "delivery.assigned";
    public static final String ETA_UPDATED = "eta.updated";
}
