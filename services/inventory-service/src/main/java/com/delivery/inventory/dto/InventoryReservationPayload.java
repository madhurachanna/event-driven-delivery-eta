package com.delivery.inventory.dto;

import java.math.BigDecimal;

/**
 * Outbound payload for inventory.reserved and inventory.rejected events.
 * The payment-service consumes this to know the order amount to charge.
 */
public class InventoryReservationPayload {

    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String customerId;

    public InventoryReservationPayload() {
    }

    public static InventoryReservationPayload from(String orderId, BigDecimal amount,
            String currency, String customerId) {
        InventoryReservationPayload payload = new InventoryReservationPayload();
        payload.orderId = orderId;
        payload.amount = amount;
        payload.currency = currency;
        payload.customerId = customerId;
        return payload;
    }

    public String getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getCustomerId() {
        return customerId;
    }
}
