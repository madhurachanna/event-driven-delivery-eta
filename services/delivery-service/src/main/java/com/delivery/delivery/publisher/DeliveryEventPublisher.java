package com.delivery.delivery.publisher;

import com.delivery.common.event.EventEnvelope;
import com.delivery.common.event.Topics;
import com.delivery.delivery.dto.DeliveryEventPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DeliveryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventPublisher.class);
    private static final String PRODUCER_NAME = "delivery-service";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DeliveryEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishDeliveryEvent(String eventType, String orderId,
            String correlationId, DeliveryEventPayload payload) {

        EventEnvelope<DeliveryEventPayload> envelope =
                EventEnvelope.<DeliveryEventPayload>builder()
                        .eventType(eventType)
                        .orderId(orderId)
                        .correlationId(correlationId)
                        .producer(PRODUCER_NAME)
                        .payload(payload)
                        .build();

        try {
            String json = objectMapper.writeValueAsString(envelope);

            kafkaTemplate.send(Topics.DELIVERY_EVENTS, orderId, json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} for order={}: {}",
                                    eventType, orderId, ex.getMessage());
                        } else {
                            log.info("Published {} for order={} to partition={}",
                                    eventType, orderId,
                                    result.getRecordMetadata().partition());
                        }
                    });

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", e.getMessage());
            throw new RuntimeException("Event serialization failed", e);
        }
    }
}
