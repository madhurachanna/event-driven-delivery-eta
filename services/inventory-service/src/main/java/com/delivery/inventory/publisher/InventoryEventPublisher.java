package com.delivery.inventory.publisher;

import com.delivery.common.event.EventEnvelope;
import com.delivery.common.event.EventTypes;
import com.delivery.common.event.Topics;
import com.delivery.inventory.dto.InventoryReservationPayload;
import com.delivery.inventory.entity.ReservationStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventPublisher.class);
    private static final String PRODUCER_NAME = "inventory-service";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public InventoryEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishInventoryEvent(String orderId, String correlationId,
            InventoryReservationPayload payload, ReservationStatus status) {

        String eventType = status == ReservationStatus.RESERVED
                ? EventTypes.INVENTORY_RESERVED
                : EventTypes.INVENTORY_REJECTED;

        EventEnvelope<InventoryReservationPayload> envelope =
                EventEnvelope.<InventoryReservationPayload>builder()
                        .eventType(eventType)
                        .orderId(orderId)
                        .correlationId(correlationId)
                        .producer(PRODUCER_NAME)
                        .payload(payload)
                        .build();

        try {
            String json = objectMapper.writeValueAsString(envelope);

            kafkaTemplate.send(Topics.INVENTORY_EVENTS, orderId, json)
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
