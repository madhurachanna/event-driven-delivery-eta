package com.delivery.order.dto;

import java.util.List;

/**
 * Incoming DTO for the POST /api/orders endpoint.
 * 
 * Example JSON:
 * {
 * "customerId": "cust-001",
 * "currency": "USD",
 * "items": [
 * {"itemId": "SKU-001", "quantity": 2, "unitPrice": 25.00}
 * ]
 * }
 */
public class OrderRequest {

    private String customerId;
    private String currency;
    private List<OrderItemRequest> items;

    // Default constructor for Jackson
    public OrderRequest() {
    }

    // Getters and Setters
    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }
}
