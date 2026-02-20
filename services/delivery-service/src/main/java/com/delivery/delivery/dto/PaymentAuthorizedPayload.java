package com.delivery.delivery.dto;

import java.math.BigDecimal;

/**
 * Mirrors the payload from payment.authorized events published by payment-service.
 */
public class PaymentAuthorizedPayload {

    private Long paymentId;
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String status;

    public PaymentAuthorizedPayload() {
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
