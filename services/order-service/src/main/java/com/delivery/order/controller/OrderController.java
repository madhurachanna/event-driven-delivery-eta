package com.delivery.order.controller;

import com.delivery.order.dto.OrderRequest;
import com.delivery.order.entity.Order;
import com.delivery.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for order operations.
 * 
 * CONCEPT: @RestController
 * - Combines @Controller + @ResponseBody
 * - Methods return data directly (not a view name)
 * - Spring auto-serializes return value to JSON
 * 
 * Endpoints:
 * - POST /api/orders → Create a new order
 * - GET /api/orders/{orderId} → Get order by ID
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Create a new order.
     * 
     * CONCEPT: @PostMapping + @RequestBody
     * - @PostMapping maps POST requests to this method
     * - @RequestBody tells Spring to deserialize the JSON body into OrderRequest
     * - ResponseEntity lets us control the HTTP status code
     */
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
        log.info("Received order request from customer: {}", request.getCustomerId());

        try {
            Order order = orderService.createOrder(request);

            // Return a clean response (not the full entity to avoid circular refs)
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "orderId", order.getOrderId(),
                    "customerId", order.getCustomerId(),
                    "totalAmount", order.getTotalAmount(),
                    "currency", order.getCurrency(),
                    "status", order.getStatus().name(),
                    "createdAt", order.getCreatedAt().toString()));

        } catch (Exception e) {
            log.error("Failed to create order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create order: " + e.getMessage()));
        }
    }

    /**
     * Get an order by its business ID.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId) {
        try {
            Order order = orderService.getOrder(orderId);

            return ResponseEntity.ok(Map.of(
                    "orderId", order.getOrderId(),
                    "customerId", order.getCustomerId(),
                    "totalAmount", order.getTotalAmount(),
                    "currency", order.getCurrency(),
                    "status", order.getStatus().name(),
                    "createdAt", order.getCreatedAt().toString()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
