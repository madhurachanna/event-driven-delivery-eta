package com.delivery.payment.service;

import com.delivery.payment.dto.PaymentEventPayload;
import com.delivery.payment.entity.Payment;
import com.delivery.payment.entity.PaymentStatus;
import com.delivery.payment.publisher.PaymentEventPublisher;
import com.delivery.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Business logic for payment processing.
 * 
 * CONCEPT: Service Layer
 * - Contains business logic (not in controller or repository)
 * - Coordinates between multiple components
 * - Handles transactions with @Transactional
 */
@Service
public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    public PaymentProcessor(PaymentRepository paymentRepository,
            PaymentEventPublisher paymentEventPublisher) {
        this.paymentRepository = paymentRepository;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    /**
     * Process a payment for an order.
     * 
     * CONCEPT: Idempotency Handling
     * 1. Check if payment already exists (by idempotencyKey)
     * 2. If exists → return existing (duplicate request)
     * 3. If not exists → create new payment
     * 4. Handle race condition via unique constraint
     * 
     * @param orderId        The order to pay for
     * @param idempotencyKey Unique key (usually eventId) to prevent duplicates
     * @param amount         Payment amount
     * @param currency       Currency code
     * @param correlationId  For event tracing
     * @return The payment record
     */
    @Transactional
    public Payment processPayment(String orderId, String idempotencyKey,
            BigDecimal amount, String currency,
            String correlationId) {

        log.info("Processing payment: orderId={}, idempotencyKey={}, amount={}",
                orderId, idempotencyKey, amount);

        // Step 1: Check for existing payment (idempotency)
        if (paymentRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Payment already processed for idempotencyKey={}", idempotencyKey);
            return paymentRepository.findByOrderId(orderId).orElseThrow();
        }

        // Step 2: Create new payment
        Payment payment = Payment.create(orderId, idempotencyKey, amount, currency);

        try {
            // Step 3: Simulate payment processing
            boolean paymentSuccessful = simulatePaymentGateway(amount);

            if (paymentSuccessful) {
                payment.authorize();
                log.info("Payment authorized for orderId={}", orderId);
            } else {
                payment.fail();
                log.warn("Payment failed for orderId={}", orderId);
            }

            // Step 4: Save to database
            payment = paymentRepository.save(payment);

            // Step 5: Publish event to Kafka
            paymentEventPublisher.publishPaymentEvent(payment, correlationId);

            return payment;

        } catch (DataIntegrityViolationException e) {
            // CONCEPT: Race Condition Handling
            // If two threads try to insert same idempotencyKey simultaneously,
            // one succeeds and one gets this exception.
            // We catch it and return the existing payment.
            log.warn("Concurrent duplicate detected for idempotencyKey={}", idempotencyKey);
            return paymentRepository.findByOrderId(orderId).orElseThrow();
        }
    }

    /**
     * Simulate a payment gateway call.
     * In real system, this would call Stripe, PayPal, etc.
     * 
     * For demo: 90% success rate.
     */
    private boolean simulatePaymentGateway(BigDecimal amount) {
        // Simulate some processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 90% success rate for demo
        return Math.random() > 0.1;
    }
}
