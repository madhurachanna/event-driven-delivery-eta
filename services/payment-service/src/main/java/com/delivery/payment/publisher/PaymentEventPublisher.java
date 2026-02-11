package com.delivery.payment.publisher;

import com.delivery.common.event.EventEnvelope;
import com.delivery.common.event.EventTypes;
import com.delivery.common.event.Topics;
import com.delivery.payment.dto.PaymentEventPayload;
import com.delivery.payment.entity.Payment;
import com.delivery.payment.entity.PaymentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes payment events to Kafka.
 * 
 * CONCEPT: KafkaTemplate
 * - Spring's helper for publishing messages to Kafka
 * - send(topic, key, value) publishes a message
 * - Key is used for partitioning (same key → same partition)
 */
@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);
    private static final String PRODUCER_NAME = "payment-service";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish payment event based on payment status.
     */
    public void publishPaymentEvent(Payment payment, String correlationId) {
        String eventType = payment.getStatus() == PaymentStatus.AUTHORIZED
                ? EventTypes.PAYMENT_AUTHORIZED
                : EventTypes.PAYMENT_FAILED;

        PaymentEventPayload payload = payment.getStatus() == PaymentStatus.AUTHORIZED
                ? PaymentEventPayload.fromAuthorized(
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getAmount(),
                        payment.getCurrency())
                : PaymentEventPayload.fromFailed(
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        "Payment declined");

        // Build the event envelope
        EventEnvelope<PaymentEventPayload> envelope = EventEnvelope.<PaymentEventPayload>builder()
                .eventType(eventType)
                .orderId(payment.getOrderId())
                .correlationId(correlationId)
                .producer(PRODUCER_NAME)
                .payload(payload)
                .build();

        try {
            String json = objectMapper.writeValueAsString(envelope);

            // CONCEPT: Partition Key
            // Using orderId as key ensures all events for same order
            // go to same partition (preserves ordering)
            kafkaTemplate.send(Topics.PAYMENT_EVENTS, payment.getOrderId(), json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event for order={}: {}",
                                    payment.getOrderId(), ex.getMessage());
                        } else {
                            log.info("Published {} for order={} to partition={}",
                                    eventType,
                                    payment.getOrderId(),
                                    result.getRecordMetadata().partition());
                        }
                    });

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", e.getMessage());
            throw new RuntimeException("Event serialization failed", e);
        }
    }
}
