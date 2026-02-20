package com.delivery.delivery.service;

import com.delivery.common.event.EventTypes;
import com.delivery.delivery.dto.DeliveryEventPayload;
import com.delivery.delivery.entity.Delivery;
import com.delivery.delivery.entity.DeliveryStatus;
import com.delivery.delivery.publisher.DeliveryEventPublisher;
import com.delivery.delivery.repository.DeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DeliveryProcessor {

    private static final Logger log = LoggerFactory.getLogger(DeliveryProcessor.class);

    private static final List<String> DRIVERS = List.of(
            "Alex Rodriguez", "Jordan Smith", "Sam Patel",
            "Taylor Kim", "Casey Johnson");

    private final DeliveryRepository deliveryRepository;
    private final DeliveryEventPublisher eventPublisher;

    public DeliveryProcessor(DeliveryRepository deliveryRepository,
            DeliveryEventPublisher eventPublisher) {
        this.deliveryRepository = deliveryRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void processPaymentAuthorized(String orderId, String correlationId) {

        if (deliveryRepository.existsByOrderId(orderId)) {
            log.info("Delivery already exists for orderId={}, skipping", orderId);
            return;
        }

        String driverName = assignDriver();
        Instant eta = calculateEta();

        Delivery delivery = Delivery.create(orderId, driverName, eta);
        deliveryRepository.save(delivery);

        log.info("Delivery assigned: orderId={}, deliveryId={}, driver={}, eta={}",
                orderId, delivery.getDeliveryId(), driverName, eta);

        // Publish delivery.assigned
        DeliveryEventPayload assignedPayload = DeliveryEventPayload.from(
                orderId, delivery.getDeliveryId(), driverName,
                DeliveryStatus.ASSIGNED.name(), eta);

        eventPublisher.publishDeliveryEvent(
                EventTypes.DELIVERY_ASSIGNED, orderId, correlationId, assignedPayload);

        // Simulate an ETA recalculation (e.g., driver hit traffic)
        Instant updatedEta = eta.plus(Duration.ofMinutes(
                ThreadLocalRandom.current().nextInt(-5, 10)));

        delivery.updateEta(updatedEta);
        deliveryRepository.save(delivery);

        DeliveryEventPayload etaPayload = DeliveryEventPayload.from(
                orderId, delivery.getDeliveryId(), driverName,
                DeliveryStatus.ASSIGNED.name(), updatedEta);

        eventPublisher.publishDeliveryEvent(
                EventTypes.ETA_UPDATED, orderId, correlationId, etaPayload);

        log.info("ETA updated for orderId={}: {} -> {}", orderId, eta, updatedEta);
    }

    private String assignDriver() {
        return DRIVERS.get(ThreadLocalRandom.current().nextInt(DRIVERS.size()));
    }

    /** Initial ETA: 30-60 minutes from now. */
    private Instant calculateEta() {
        int minutes = ThreadLocalRandom.current().nextInt(30, 61);
        return Instant.now().plus(Duration.ofMinutes(minutes));
    }
}
