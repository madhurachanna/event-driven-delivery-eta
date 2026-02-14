package com.delivery.order.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payload for the order.created Kafka event.
 * 
 * This is what goes into the EventEnvelope.payload field.
 * Downstream services (inventory-service) deserialize this
 * to understand what was ordered.
 */
public class OrderCreatedPayload {

    private String customerId;
    private BigDecimal totalAmount;
    private String currency;
    private List<OrderItemDetail> items;

    // Default constructor for Jackson
    public OrderCreatedPayload() {
    }

    // Factory method
    public static OrderCreatedPayload from(String customerId, BigDecimal totalAmount,
            String currency, List<OrderItemDetail> items) {
        OrderCreatedPayload payload = new OrderCreatedPayload();
        payload.customerId = customerId;
        payload.totalAmount = totalAmount;
        payload.currency = currency;
        payload.items = items;
        return payload;
    }

    // Getters
    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public List<OrderItemDetail> getItems() {
        return items;
    }

    /**
     * Nested class for item details in the event payload.
     */
    public static class OrderItemDetail {

        private String itemId;
        private Integer quantity;
        private BigDecimal unitPrice;

        public OrderItemDetail() {
        }

        public static OrderItemDetail from(String itemId, Integer quantity, BigDecimal unitPrice) {
            OrderItemDetail detail = new OrderItemDetail();
            detail.itemId = itemId;
            detail.quantity = quantity;
            detail.unitPrice = unitPrice;
            return detail;
        }

        public String getItemId() {
            return itemId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }
    }
}
