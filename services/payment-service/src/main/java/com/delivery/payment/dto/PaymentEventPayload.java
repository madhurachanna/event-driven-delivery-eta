package com.delivery.payment.dto;

import com.delivery.payment.entity.PaymentStatus;

import java.math.BigDecimal;

/**
 * Payload for payment.authorized and payment.failed events.
 * 
 * This is what we publish to Kafka in the EventEnvelope.payload field.
 */
public class PaymentEventPayload {

    private Long paymentId;
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String failureReason;

    // Default constructor
    public PaymentEventPayload() {
    }

    // Builder-style static factory
    public static PaymentEventPayload fromAuthorized(Long paymentId, String orderId,
            BigDecimal amount, String currency) {
        PaymentEventPayload payload = new PaymentEventPayload();
        payload.paymentId = paymentId;
        payload.orderId = orderId;
        payload.amount = amount;
        payload.currency = currency;
        payload.status = PaymentStatus.AUTHORIZED;
        return payload;
    }

    public static PaymentEventPayload fromFailed(Long paymentId, String orderId,
            BigDecimal amount, String currency,
            String reason) {
        PaymentEventPayload payload = new PaymentEventPayload();
        payload.paymentId = paymentId;
        payload.orderId = orderId;
        payload.amount = amount;
        payload.currency = currency;
        payload.status = PaymentStatus.FAILED;
        payload.failureReason = reason;
        return payload;
    }

    // Getters
    public Long getPaymentId() {
        return paymentId;
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

    public PaymentStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
