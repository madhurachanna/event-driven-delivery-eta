package com.delivery.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed event deduplication. Provides O(1) duplicate detection
 * with configurable TTL. Acts as a fast first layer before the DB
 * unique constraint safety net.
 */
@Service
public class RedisDeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(RedisDeduplicationService.class);

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;
    private final String keyPrefix;

    public RedisDeduplicationService(
            StringRedisTemplate redisTemplate,
            @Value("${dedup.redis.ttl-hours:24}") int ttlHours,
            @Value("${dedup.redis.key-prefix:payment:dedup:}") String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofHours(ttlHours);
        this.keyPrefix = keyPrefix;
    }

    /** Returns {@code true} if this eventId has already been processed. */
    public boolean isDuplicate(String eventId) {
        String key = keyPrefix + eventId;
        boolean duplicate = Boolean.TRUE.equals(redisTemplate.hasKey(key));

        if (duplicate) {
            log.info("Redis dedup HIT: eventId={}", eventId);
        } else {
            log.debug("Redis dedup MISS: eventId={}", eventId);
        }

        return duplicate;
    }

    /**
     * Marks an eventId as processed with TTL. Called after successful DB commit.
     * Failures are logged but not propagated — DB constraint is the safety net.
     */
    public void markProcessed(String eventId) {
        String key = keyPrefix + eventId;
        try {
            redisTemplate.opsForValue().set(key, "1", ttl);
            log.debug("Marked eventId={} (TTL={}h)", eventId, ttl.toHours());
        } catch (Exception e) {
            log.warn("Failed to mark eventId={} in Redis: {}", eventId, e.getMessage());
        }
    }
}
