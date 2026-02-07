package com.delivery.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * Common event envelope used by all services.
 * 
 * This ensures a consistent contract across the system:
 * - All events have the same metadata structure
 * - Payload varies by event type
 * - Schema versioning allows evolution without breaking consumers
 * 
 * @param <T> The payload type (e.g., OrderPayload, PaymentPayload)
 */
public class EventEnvelope<T> {

    private String eventId;
    private String eventType;
    private Integer schemaVersion;
    private Instant occurredAt;
    private Instant producedAt;
    private String orderId;
    private String correlationId;
    private String producer;
    private T payload;

    // Default constructor for Jackson deserialization
    public EventEnvelope() {
    }

    // Builder-style constructor
    private EventEnvelope(Builder<T> builder) {
        this.eventId = builder.eventId;
        this.eventType = builder.eventType;
        this.schemaVersion = builder.schemaVersion;
        this.occurredAt = builder.occurredAt;
        this.producedAt = builder.producedAt;
        this.orderId = builder.orderId;
        this.correlationId = builder.correlationId;
        this.producer = builder.producer;
        this.payload = builder.payload;
    }

    // Static factory to create a new event
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    // Getters
    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getProducedAt() {
        return producedAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getProducer() {
        return producer;
    }

    public T getPayload() {
        return payload;
    }

    // Setters for Jackson
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public void setProducedAt(Instant producedAt) {
        this.producedAt = producedAt;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }

    /**
     * Builder pattern for creating events.
     * Ensures required fields are set and generates defaults.
     */
    public static class Builder<T> {
        private String eventId = UUID.randomUUID().toString();
        private String eventType;
        private Integer schemaVersion = 1;
        private Instant occurredAt;
        private Instant producedAt = Instant.now();
        private String orderId;
        private String correlationId;
        private String producer;
        private T payload;

        public Builder<T> eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder<T> schemaVersion(Integer schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        public Builder<T> occurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public Builder<T> orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder<T> correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder<T> producer(String producer) {
            this.producer = producer;
            return this;
        }

        public Builder<T> payload(T payload) {
            this.payload = payload;
            return this;
        }

        public EventEnvelope<T> build() {
            if (eventType == null)
                throw new IllegalStateException("eventType is required");
            if (orderId == null)
                throw new IllegalStateException("orderId is required");
            if (occurredAt == null)
                occurredAt = Instant.now();
            return new EventEnvelope<>(this);
        }
    }
}
