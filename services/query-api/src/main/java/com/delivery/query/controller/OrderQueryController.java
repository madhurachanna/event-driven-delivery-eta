package com.delivery.query.controller;

import com.delivery.query.entity.OrderView;
import com.delivery.query.service.OrderViewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for querying the denormalized order read model (CQRS query side).
 * Reads from Cassandra — completely independent of the write-side Postgres databases.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderQueryController {

    private final OrderViewService orderViewService;

    public OrderQueryController(OrderViewService orderViewService) {
        this.orderViewService = orderViewService;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId) {
        return orderViewService.getOrder(orderId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(Map.of("error", "Order not found: " + orderId)));
    }

    @GetMapping
    public List<OrderView> getAllOrders() {
        return orderViewService.getAllOrders();
    }
}
