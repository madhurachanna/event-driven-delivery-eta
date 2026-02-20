package com.delivery.query.listener;

import com.delivery.common.event.EventTypes;
import com.delivery.common.event.Topics;
import com.delivery.query.service.OrderViewService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes events from ALL domain topics and delegates to OrderViewService
 * to materialize the CQRS read model in Cassandra.
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final ObjectMapper objectMapper;
    private final OrderViewService orderViewService;

    public OrderEventListener(ObjectMapper objectMapper, OrderViewService orderViewService) {
        this.objectMapper = objectMapper;
        this.orderViewService = orderViewService;
    }

    @KafkaListener(
            topics = {
                    Topics.ORDER_EVENTS,
                    Topics.INVENTORY_EVENTS,
                    Topics.PAYMENT_EVENTS,
                    Topics.DELIVERY_EVENTS
            },
            groupId = "${spring.kafka.consumer.group-id}")
    public void handleEvent(String message) {
        try {
            JsonNode eventNode = objectMapper.readTree(message);
            String eventType = eventNode.get("eventType").asText();
            String orderId = eventNode.get("orderId").asText();
            JsonNode payload = eventNode.get("payload");

            log.info("Query API received event: type={}, orderId={}", eventType, orderId);

            switch (eventType) {
                case EventTypes.ORDER_CREATED:
                    orderViewService.handleOrderCreated(orderId, payload);
                    break;
                case EventTypes.INVENTORY_RESERVED:
                    orderViewService.handleInventoryReserved(orderId);
                    break;
                case EventTypes.INVENTORY_REJECTED:
                    orderViewService.handleInventoryRejected(orderId);
                    break;
                case EventTypes.PAYMENT_AUTHORIZED:
                    orderViewService.handlePaymentAuthorized(orderId, payload);
                    break;
                case EventTypes.PAYMENT_FAILED:
                    orderViewService.handlePaymentFailed(orderId);
                    break;
                case EventTypes.DELIVERY_ASSIGNED:
                    orderViewService.handleDeliveryAssigned(orderId, payload);
                    break;
                case EventTypes.ETA_UPDATED:
                    orderViewService.handleEtaUpdated(orderId, payload);
                    break;
                default:
                    log.warn("Ignoring unknown event type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Failed to process event for read model: {}", e.getMessage(), e);
        }
    }
}
