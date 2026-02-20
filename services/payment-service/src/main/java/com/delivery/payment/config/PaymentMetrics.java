package com.delivery.payment.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Centralized payment metrics exposed via Micrometer / Prometheus.
 * Scraped at {@code /actuator/prometheus}.
 */
@Component
public class PaymentMetrics {

    private final Counter processedCounter;
    private final Counter failedCounter;
    private final Counter duplicateCounter;
    private final Counter redisDedupHitCounter;
    private final Counter dbDedupHitCounter;
    private final Counter lockTimeoutCounter;
    private final Timer processingTimer;

    public PaymentMetrics(MeterRegistry registry) {
        this.processedCounter = Counter.builder("payment.processed.total")
                .description("Total payments successfully authorized")
                .register(registry);

        this.failedCounter = Counter.builder("payment.failed.total")
                .description("Total payments failed at gateway")
                .register(registry);

        this.duplicateCounter = Counter.builder("payment.duplicate.total")
                .description("Total duplicate payment events detected")
                .register(registry);

        this.redisDedupHitCounter = Counter.builder("payment.dedup.redis.hit.total")
                .description("Duplicates caught by Redis fast-path")
                .register(registry);

        this.dbDedupHitCounter = Counter.builder("payment.dedup.db.hit.total")
                .description("Duplicates caught by DB (Redis miss)")
                .register(registry);

        this.lockTimeoutCounter = Counter.builder("payment.lock.timeout.total")
                .description("Failed to acquire distributed lock within timeout")
                .register(registry);

        this.processingTimer = Timer.builder("payment.processing.duration")
                .description("Payment processing duration (end-to-end)")
                .register(registry);
    }

    public void recordProcessed() {
        processedCounter.increment();
    }

    public void recordFailed() {
        failedCounter.increment();
    }

    public void recordDuplicate() {
        duplicateCounter.increment();
    }

    public void recordRedisDedupHit() {
        redisDedupHitCounter.increment();
    }

    public void recordDbDedupHit() {
        dbDedupHitCounter.increment();
    }

    public void recordLockTimeout() {
        lockTimeoutCounter.increment();
    }

    public Timer getProcessingTimer() {
        return processingTimer;
    }
}
