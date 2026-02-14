package com.delivery.order.publisher;

import com.delivery.common.event.EventEnvelope;
import com.delivery.common.event.EventTypes;
import com.delivery.common.event.Topics;
import com.delivery.order.dto.OrderCreatedPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes order events to Kafka.
 * 
 * CONCEPT: KafkaTemplate
 * - Spring's helper for publishing messages to Kafka
 * - send(topic, key, value) publishes a message
 * - Key is used for partitioning (same key → same partition)
 * - Using orderId as key ensures all events for same order
 * go to the same partition (preserves ordering)
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    private static final String PRODUCER_NAME = "order-service";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish order.created event to Kafka.
     * 
     * @param orderId The order's business ID (used as partition key)
     * @param payload The event payload with order details
     */
    public void publishOrderCreated(String orderId, OrderCreatedPayload payload) {

        // Build the event envelope using the shared contract
        EventEnvelope<OrderCreatedPayload> envelope = EventEnvelope.<OrderCreatedPayload>builder()
                .eventType(EventTypes.ORDER_CREATED)
                .orderId(orderId)
                .correlationId(orderId) // For new orders, orderId is the correlation root
                .producer(PRODUCER_NAME)
                .payload(payload)
                .build();

        try {
            String json = objectMapper.writeValueAsString(envelope);

            // CONCEPT: Partition Key
            // Using orderId as key ensures all events for same order
            // go to same partition (preserves ordering)
            kafkaTemplate.send(Topics.ORDER_EVENTS, orderId, json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish order.created for order={}: {}",
                                    orderId, ex.getMessage());
                        } else {
                            log.info("Published order.created for order={} to partition={}",
                                    orderId,
                                    result.getRecordMetadata().partition());
                        }
                    });

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order.created event: {}", e.getMessage());
            throw new RuntimeException("Event serialization failed", e);
        }
    }
}
