package com.delivery.delivery.dto;

import java.time.Instant;

/**
 * Outbound payload for delivery.assigned and eta.updated events.
 */
public class DeliveryEventPayload {

    private String orderId;
    private String deliveryId;
    private String driverName;
    private String status;
    private Instant estimatedDeliveryTime;

    public DeliveryEventPayload() {
    }

    public static DeliveryEventPayload from(String orderId, String deliveryId,
            String driverName, String status, Instant estimatedDeliveryTime) {
        DeliveryEventPayload payload = new DeliveryEventPayload();
        payload.orderId = orderId;
        payload.deliveryId = deliveryId;
        payload.driverName = driverName;
        payload.status = status;
        payload.estimatedDeliveryTime = estimatedDeliveryTime;
        return payload;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getStatus() {
        return status;
    }

    public Instant getEstimatedDeliveryTime() {
        return estimatedDeliveryTime;
    }
}
