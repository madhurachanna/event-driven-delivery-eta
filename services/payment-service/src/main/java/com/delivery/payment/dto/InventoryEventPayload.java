package com.delivery.payment.dto;

import java.math.BigDecimal;

/**
 * Payload from inventory.reserved events.
 * 
 * CONCEPT: DTO (Data Transfer Object)
 * - Simple class to hold data from incoming events
 * - Maps to the "payload" field in EventEnvelope
 */
public class InventoryEventPayload {

    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String customerId;

    // Default constructor for Jackson
    public InventoryEventPayload() {
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
}
