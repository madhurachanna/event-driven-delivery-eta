package com.delivery.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;

/**
 * Configures a Redis-backed distributed lock registry for cross-instance
 * payment serialization. Uses SET NX with a 30-second TTL to prevent
 * deadlocks if a pod crashes while holding a lock.
 */
@Configuration
public class DistributedLockConfig {

    private static final String LOCK_REGISTRY_KEY = "payment-lock";
    private static final long LOCK_EXPIRY_MS = 30_000;

    @Bean
    public RedisLockRegistry redisLockRegistry(RedisConnectionFactory connectionFactory) {
        return new RedisLockRegistry(connectionFactory, LOCK_REGISTRY_KEY, LOCK_EXPIRY_MS);
    }
}
