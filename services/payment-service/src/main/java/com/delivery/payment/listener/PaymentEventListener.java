package com.delivery.payment.listener;

import com.delivery.common.event.EventTypes;
import com.delivery.common.event.Topics;
import com.delivery.payment.service.PaymentProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Kafka consumer that listens for inventory events.
 * 
 * CONCEPT: @KafkaListener
 * - Spring creates a background thread that polls Kafka
 * - Each message triggers this method
 * - groupId identifies this consumer group (for offset tracking)
 * - Multiple instances with same groupId share partitions
 */
@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final ObjectMapper objectMapper;
    private final PaymentProcessor paymentProcessor;

    public PaymentEventListener(ObjectMapper objectMapper, PaymentProcessor paymentProcessor) {
        this.objectMapper = objectMapper;
        this.paymentProcessor = paymentProcessor;
    }

    /**
     * Listens to inventory events topic.
     * 
     * CONCEPT: Consumer Flow
     * 1. Kafka delivers message to this method
     * 2. We deserialize and check event type
     * 3. Only process "inventory.reserved" (ignore others)
     * 4. If method completes without exception → offset committed
     * 5. If exception thrown → message will be redelivered (at-least-once)
     */
    @KafkaListener(topics = Topics.INVENTORY_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
    public void handleInventoryEvent(String message) {
        log.info("Received inventory event: {}", message);

        try {
            // Parse the JSON event envelope
            JsonNode eventNode = objectMapper.readTree(message);
            String eventType = eventNode.get("eventType").asText();
            String orderId = eventNode.get("orderId").asText();
            String eventId = eventNode.get("eventId").asText();
            String correlationId = eventNode.has("correlationId")
                    ? eventNode.get("correlationId").asText()
                    : eventId;

            log.info("Processing event: type={}, orderId={}, eventId={}",
                    eventType, orderId, eventId);

            // Route based on event type
            switch (eventType) {
                case EventTypes.INVENTORY_RESERVED:
                    handleInventoryReserved(eventNode, eventId, correlationId);
                    break;
                case EventTypes.INVENTORY_REJECTED:
                    handleInventoryRejected(eventNode);
                    break;
                default:
                    log.warn("Unknown event type: {}", eventType);
            }

        } catch (Exception e) {
            // CONCEPT: Error Handling
            // In production, we'd send to DLQ after retries
            // For now, log and rethrow to trigger retry
            log.error("Failed to process event: {}", e.getMessage(), e);
            throw new RuntimeException("Event processing failed", e);
        }
    }

    private void handleInventoryReserved(JsonNode eventNode, String eventId,
            String correlationId) {
        String orderId = eventNode.get("orderId").asText();
        JsonNode payload = eventNode.get("payload");

        // Extract payment details from payload
        BigDecimal amount = payload.has("amount")
                ? new BigDecimal(payload.get("amount").asText())
                : new BigDecimal("0.00");
        String currency = payload.has("currency")
                ? payload.get("currency").asText()
                : "USD";

        log.info("Processing payment for order: {}, amount: {} {}", orderId, amount, currency);

        // Process payment using eventId as idempotency key
        paymentProcessor.processPayment(orderId, eventId, amount, currency, correlationId);
    }

    private void handleInventoryRejected(JsonNode eventNode) {
        String orderId = eventNode.get("orderId").asText();
        log.info("Inventory rejected for order: {}, skipping payment", orderId);

        // No payment needed - inventory wasn't available
        // Could optionally publish payment.skipped event
    }
}
