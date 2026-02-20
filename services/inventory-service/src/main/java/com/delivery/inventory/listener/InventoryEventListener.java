package com.delivery.inventory.listener;

import com.delivery.common.event.EventTypes;
import com.delivery.common.event.Topics;
import com.delivery.inventory.service.InventoryProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class InventoryEventListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final ObjectMapper objectMapper;
    private final InventoryProcessor inventoryProcessor;

    public InventoryEventListener(ObjectMapper objectMapper,
            InventoryProcessor inventoryProcessor) {
        this.objectMapper = objectMapper;
        this.inventoryProcessor = inventoryProcessor;
    }

    @KafkaListener(topics = Topics.ORDER_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderEvent(String message) {
        log.info("Received order event");

        try {
            JsonNode eventNode = objectMapper.readTree(message);
            String eventType = eventNode.get("eventType").asText();
            String orderId = eventNode.get("orderId").asText();
            String correlationId = eventNode.has("correlationId")
                    ? eventNode.get("correlationId").asText()
                    : orderId;

            log.info("Processing event: type={}, orderId={}", eventType, orderId);

            if (EventTypes.ORDER_CREATED.equals(eventType)) {
                handleOrderCreated(eventNode, correlationId);
            } else {
                log.warn("Ignoring unknown event type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Failed to process order event: {}", e.getMessage(), e);
            throw new RuntimeException("Event processing failed", e);
        }
    }

    private void handleOrderCreated(JsonNode eventNode, String correlationId) {
        String orderId = eventNode.get("orderId").asText();
        JsonNode payload = eventNode.get("payload");

        String customerId = payload.get("customerId").asText();
        BigDecimal totalAmount = new BigDecimal(payload.get("totalAmount").asText());
        String currency = payload.has("currency")
                ? payload.get("currency").asText()
                : "USD";

        inventoryProcessor.processOrder(
                orderId, correlationId, customerId, totalAmount, currency);
    }
}
