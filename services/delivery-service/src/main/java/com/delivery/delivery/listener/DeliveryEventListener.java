package com.delivery.delivery.listener;

import com.delivery.common.event.EventTypes;
import com.delivery.common.event.Topics;
import com.delivery.delivery.service.DeliveryProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DeliveryEventListener {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventListener.class);

    private final ObjectMapper objectMapper;
    private final DeliveryProcessor deliveryProcessor;

    public DeliveryEventListener(ObjectMapper objectMapper,
            DeliveryProcessor deliveryProcessor) {
        this.objectMapper = objectMapper;
        this.deliveryProcessor = deliveryProcessor;
    }

    @KafkaListener(topics = Topics.PAYMENT_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentEvent(String message) {
        log.info("Received payment event");

        try {
            JsonNode eventNode = objectMapper.readTree(message);
            String eventType = eventNode.get("eventType").asText();
            String orderId = eventNode.get("orderId").asText();
            String correlationId = eventNode.has("correlationId")
                    ? eventNode.get("correlationId").asText()
                    : orderId;

            log.info("Processing event: type={}, orderId={}", eventType, orderId);

            switch (eventType) {
                case EventTypes.PAYMENT_AUTHORIZED:
                    deliveryProcessor.processPaymentAuthorized(orderId, correlationId);
                    break;
                case EventTypes.PAYMENT_FAILED:
                    log.info("Payment failed for orderId={}, no delivery needed", orderId);
                    break;
                default:
                    log.warn("Ignoring unknown event type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Failed to process payment event: {}", e.getMessage(), e);
            throw new RuntimeException("Event processing failed", e);
        }
    }
}
