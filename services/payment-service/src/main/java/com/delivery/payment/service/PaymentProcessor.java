package com.delivery.payment.service;

import com.delivery.payment.config.PaymentMetrics;
import com.delivery.payment.entity.Payment;
import com.delivery.payment.publisher.PaymentEventPublisher;
import com.delivery.payment.repository.PaymentRepository;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Core payment processing with three-layer duplicate protection:
 * <ol>
 * <li>Distributed lock on orderId (prevents concurrent processing)</li>
 * <li>Redis dedup — fast O(1) check for sequential duplicates</li>
 * <li>DB unique constraint — durable safety net</li>
 * </ol>
 * Post-commit hooks ensure Redis cache and Kafka events are only
 * written after a successful DB commit (two-step commit pattern).
 *
 * @see RedisDeduplicationService
 * @see com.delivery.payment.config.DistributedLockConfig
 */
@Service
public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);
    private static final long LOCK_WAIT_SECONDS = 5;

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    private final RedisDeduplicationService deduplicationService;
    private final RedisLockRegistry lockRegistry;
    private final PaymentMetrics metrics;
    private final PaymentProcessor self; // self-injection for @Transactional proxy

    public PaymentProcessor(PaymentRepository paymentRepository,
            PaymentEventPublisher paymentEventPublisher,
            RedisDeduplicationService deduplicationService,
            RedisLockRegistry lockRegistry,
            PaymentMetrics metrics,
            @Lazy PaymentProcessor self) {
        this.paymentRepository = paymentRepository;
        this.paymentEventPublisher = paymentEventPublisher;
        this.deduplicationService = deduplicationService;
        this.lockRegistry = lockRegistry;
        this.metrics = metrics;
        this.self = self;
    }

    /**
     * Acquires a distributed lock on {@code orderId}, then delegates to the
     * transactional processing method. The lock is held outside the transaction
     * so it covers the full span from dedup check through DB commit.
     */
    public Payment processPayment(String orderId, String idempotencyKey,
            BigDecimal amount, String currency,
            String correlationId) {

        Timer.Sample timerSample = Timer.start();
        Lock lock = lockRegistry.obtain("order:" + orderId);

        boolean acquired;
        try {
            acquired = lock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for lock on orderId=" + orderId, e);
        }

        if (!acquired) {
            metrics.recordLockTimeout();
            log.warn("Could not acquire lock for orderId={} within {}s, will retry",
                    orderId, LOCK_WAIT_SECONDS);
            throw new RuntimeException("Could not acquire payment lock for orderId=" + orderId);
        }

        log.debug("Acquired distributed lock for orderId={}", orderId);
        try {
            return self.processPaymentTransactional(orderId, idempotencyKey,
                    amount, currency, correlationId);
        } finally {
            lock.unlock();
            timerSample.stop(metrics.getProcessingTimer());
            log.debug("Released distributed lock for orderId={}", orderId);
        }
    }

    /**
     * Runs inside an active transaction. Checks Redis then DB for duplicates,
     * processes the payment, and registers post-commit hooks for Redis cache
     * update and Kafka event publication.
     *
     * <p>
     * Must be {@code public} for Spring's proxy-based AOP; called via
     * self-injection.
     * </p>
     */
    @Transactional
    public Payment processPaymentTransactional(String orderId, String idempotencyKey,
            BigDecimal amount, String currency,
            String correlationId) {

        log.info("Processing payment: orderId={}, idempotencyKey={}, amount={}",
                orderId, idempotencyKey, amount);

        // Layer 2: Redis fast-path
        if (deduplicationService.isDuplicate(idempotencyKey)) {
            metrics.recordDuplicate();
            metrics.recordRedisDedupHit();
            log.info("Duplicate detected via Redis for idempotencyKey={}", idempotencyKey);
            return paymentRepository.findByOrderId(orderId).orElseThrow();
        }

        // Layer 3: DB safety-net (handles Redis TTL expiry / restarts)
        if (paymentRepository.existsByIdempotencyKey(idempotencyKey)) {
            metrics.recordDuplicate();
            metrics.recordDbDedupHit();
            log.info("Duplicate detected via DB for idempotencyKey={}", idempotencyKey);
            deduplicationService.markProcessed(idempotencyKey); // backfill Redis
            return paymentRepository.findByOrderId(orderId).orElseThrow();
        }

        // Process payment
        Payment payment = Payment.create(orderId, idempotencyKey, amount, currency);

        try {
            boolean paymentSuccessful = simulatePaymentGateway(amount);

            if (paymentSuccessful) {
                payment.authorize();
                metrics.recordProcessed();
                log.info("Payment authorized for orderId={}", orderId);
            } else {
                payment.fail();
                metrics.recordFailed();
                log.warn("Payment failed for orderId={}", orderId);
            }

            Payment savedPayment = paymentRepository.save(payment);

            // Post-commit: update Redis cache and publish Kafka event only after DB commit
            // succeeds
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            deduplicationService.markProcessed(idempotencyKey);
                            paymentEventPublisher.publishPaymentEvent(savedPayment, correlationId);
                        }
                    });

            log.info("Payment saved, post-commit actions registered for orderId={}", orderId);
            return savedPayment;

        } catch (DataIntegrityViolationException e) {
            // Rare with distributed lock; possible if lock TTL expires mid-processing
            metrics.recordDuplicate();
            log.warn("Concurrent duplicate detected for idempotencyKey={}", idempotencyKey);
            deduplicationService.markProcessed(idempotencyKey);
            return paymentRepository.findByOrderId(orderId).orElseThrow();
        }
    }

    /** Simulates a payment gateway call. 90% success rate, ~100ms latency. */
    private boolean simulatePaymentGateway(BigDecimal amount) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Math.random() > 0.1;
    }
}
