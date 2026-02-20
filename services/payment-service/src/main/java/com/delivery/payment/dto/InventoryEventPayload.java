package com.delivery.payment.dto;

import java.math.BigDecimal;

/** Payload extracted from incoming {@code inventory.reserved} events. */
public class InventoryEventPayload {

    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String customerId;

    public InventoryEventPayload() {
    }

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

    public void setCustomerId(String id) {
        this.customerId = id;
    }
}
