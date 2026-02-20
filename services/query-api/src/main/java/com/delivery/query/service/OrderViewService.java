package com.delivery.query.service;

import com.delivery.query.entity.OrderView;
import com.delivery.query.repository.OrderViewRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Materializes Kafka events into the Cassandra read model.
 * Each handler updates the denormalized OrderView with the
 * latest state from the corresponding event.
 */
@Service
public class OrderViewService {

    private static final Logger log = LoggerFactory.getLogger(OrderViewService.class);

    private final OrderViewRepository repository;

    public OrderViewService(OrderViewRepository repository) {
        this.repository = repository;
    }

    public void handleOrderCreated(String orderId, JsonNode payload) {
        OrderView view = new OrderView();
        view.setOrderId(orderId);
        view.setCustomerId(payload.get("customerId").asText());
        view.setTotalAmount(new BigDecimal(payload.get("totalAmount").asText()));
        view.setCurrency(payload.has("currency") ? payload.get("currency").asText() : "USD");
        view.setStatus("CREATED");
        view.setCreatedAt(Instant.now());
        view.setUpdatedAt(Instant.now());

        repository.save(view);
        log.info("OrderView created for orderId={}", orderId);
    }

    public void handleInventoryReserved(String orderId) {
        updateStatus(orderId, "INVENTORY_RESERVED");
    }

    public void handleInventoryRejected(String orderId) {
        updateStatus(orderId, "INVENTORY_REJECTED");
    }

    public void handlePaymentAuthorized(String orderId, JsonNode payload) {
        OrderView view = findOrCreate(orderId);
        view.setStatus("PAYMENT_AUTHORIZED");
        view.setUpdatedAt(Instant.now());
        if (payload.has("paymentId") && !payload.get("paymentId").isNull()) {
            view.setPaymentId(payload.get("paymentId").asLong());
        }
        repository.save(view);
        log.info("OrderView updated to PAYMENT_AUTHORIZED for orderId={}", orderId);
    }

    public void handlePaymentFailed(String orderId) {
        updateStatus(orderId, "PAYMENT_FAILED");
    }

    public void handleDeliveryAssigned(String orderId, JsonNode payload) {
        OrderView view = findOrCreate(orderId);
        view.setStatus("DELIVERY_ASSIGNED");
        view.setUpdatedAt(Instant.now());
        if (payload.has("deliveryId")) {
            view.setDeliveryId(payload.get("deliveryId").asText());
        }
        if (payload.has("driverName")) {
            view.setDriverName(payload.get("driverName").asText());
        }
        if (payload.has("estimatedDeliveryTime") && !payload.get("estimatedDeliveryTime").isNull()) {
            view.setEstimatedDeliveryTime(Instant.parse(payload.get("estimatedDeliveryTime").asText()));
        }
        repository.save(view);
        log.info("OrderView updated to DELIVERY_ASSIGNED for orderId={}", orderId);
    }

    public void handleEtaUpdated(String orderId, JsonNode payload) {
        OrderView view = findOrCreate(orderId);
        view.setUpdatedAt(Instant.now());
        if (payload.has("estimatedDeliveryTime") && !payload.get("estimatedDeliveryTime").isNull()) {
            view.setEstimatedDeliveryTime(Instant.parse(payload.get("estimatedDeliveryTime").asText()));
        }
        repository.save(view);
        log.info("OrderView ETA updated for orderId={}", orderId);
    }

    public Optional<OrderView> getOrder(String orderId) {
        return repository.findById(orderId);
    }

    public List<OrderView> getAllOrders() {
        return repository.findAll();
    }

    private void updateStatus(String orderId, String status) {
        OrderView view = findOrCreate(orderId);
        view.setStatus(status);
        view.setUpdatedAt(Instant.now());
        repository.save(view);
        log.info("OrderView updated to {} for orderId={}", status, orderId);
    }

    /**
     * Handles out-of-order event delivery: if the OrderView doesn't exist yet
     * (e.g., order.created hasn't been processed), create a partial view so
     * downstream status updates are not lost.
     */
    private OrderView findOrCreate(String orderId) {
        return repository.findById(orderId).orElseGet(() -> {
            OrderView view = new OrderView();
            view.setOrderId(orderId);
            view.setCreatedAt(Instant.now());
            return view;
        });
    }
}
