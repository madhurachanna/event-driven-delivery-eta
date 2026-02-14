package com.delivery.order.dto;

import java.math.BigDecimal;

/**
 * DTO for a single item in the order request.
 * 
 * CONCEPT: DTO (Data Transfer Object)
 * - Simple class to receive data from the REST client
 * - Separate from the entity to decouple API contract from DB schema
 */
public class OrderItemRequest {

    private String itemId;
    private Integer quantity;
    private BigDecimal unitPrice;

    // Default constructor for Jackson
    public OrderItemRequest() {
    }

    // Getters and Setters
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
