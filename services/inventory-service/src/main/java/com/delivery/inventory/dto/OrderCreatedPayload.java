package com.delivery.inventory.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mirrors the payload published by order-service in order.created events.
 * Used to deserialize the incoming Kafka message payload.
 */
public class OrderCreatedPayload {

    private String customerId;
    private BigDecimal totalAmount;
    private String currency;
    private List<OrderItemDetail> items;

    public OrderCreatedPayload() {
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<OrderItemDetail> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDetail> items) {
        this.items = items;
    }

    public static class OrderItemDetail {

        private String itemId;
        private Integer quantity;
        private BigDecimal unitPrice;

        public OrderItemDetail() {
        }

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }
    }
}
