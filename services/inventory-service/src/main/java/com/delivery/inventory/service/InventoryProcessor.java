package com.delivery.inventory.service;

import com.delivery.inventory.dto.InventoryReservationPayload;
import com.delivery.inventory.entity.Reservation;
import com.delivery.inventory.entity.ReservationStatus;
import com.delivery.inventory.publisher.InventoryEventPublisher;
import com.delivery.inventory.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class InventoryProcessor {

    private static final Logger log = LoggerFactory.getLogger(InventoryProcessor.class);

    private final ReservationRepository reservationRepository;
    private final InventoryEventPublisher eventPublisher;

    public InventoryProcessor(ReservationRepository reservationRepository,
            InventoryEventPublisher eventPublisher) {
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void processOrder(String orderId, String correlationId,
            String customerId, BigDecimal totalAmount, String currency) {

        if (reservationRepository.existsByOrderId(orderId)) {
            log.info("Reservation already exists for orderId={}, skipping", orderId);
            return;
        }

        log.info("Checking inventory for orderId={}, amount={} {}",
                orderId, totalAmount, currency);

        boolean inStock = simulateStockCheck();
        ReservationStatus status = inStock
                ? ReservationStatus.RESERVED
                : ReservationStatus.REJECTED;

        Reservation reservation = Reservation.create(
                orderId, customerId, totalAmount, currency, status);
        reservationRepository.save(reservation);

        log.info("Reservation {} for orderId={}", status, orderId);

        InventoryReservationPayload payload = InventoryReservationPayload.from(
                orderId, totalAmount, currency, customerId);

        eventPublisher.publishInventoryEvent(orderId, correlationId, payload, status);
    }

    /** Simulates a stock availability check. 90% of orders have stock available. */
    private boolean simulateStockCheck() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Math.random() > 0.1;
    }
}
