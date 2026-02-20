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
 * Kafka consumer for inventory events. Triggers payment processing
 * when inventory is successfully reserved for an order.
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

    @KafkaListener(topics = Topics.INVENTORY_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
    public void handleInventoryEvent(String message) {
        log.info("Received inventory event: {}", message);

        try {
            JsonNode eventNode = objectMapper.readTree(message);
            String eventType = eventNode.get("eventType").asText();
            String orderId = eventNode.get("orderId").asText();
            String eventId = eventNode.get("eventId").asText();
            String correlationId = eventNode.has("correlationId")
                    ? eventNode.get("correlationId").asText()
                    : eventId;

            log.info("Processing event: type={}, orderId={}, eventId={}",
                    eventType, orderId, eventId);

            switch (eventType) {
                case EventTypes.INVENTORY_RESERVED:
                    handleInventoryReserved(eventNode, eventId, correlationId);
                    break;
                case EventTypes.INVENTORY_REJECTED:
                    log.info("Inventory rejected for order: {}, skipping payment", orderId);
                    break;
                default:
                    log.warn("Unknown event type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Failed to process event: {}", e.getMessage(), e);
            throw new RuntimeException("Event processing failed", e);
        }
    }

    private void handleInventoryReserved(JsonNode eventNode, String eventId,
            String correlationId) {
        String orderId = eventNode.get("orderId").asText();
        JsonNode payload = eventNode.get("payload");

        BigDecimal amount = payload.has("amount")
                ? new BigDecimal(payload.get("amount").asText())
                : new BigDecimal("0.00");
        String currency = payload.has("currency")
                ? payload.get("currency").asText()
                : "USD";

        log.info("Processing payment for order: {}, amount: {} {}", orderId, amount, currency);

        paymentProcessor.processPayment(orderId, eventId, amount, currency, correlationId);
    }
}
