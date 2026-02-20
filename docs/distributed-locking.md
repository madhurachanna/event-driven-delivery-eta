# Distributed Locking

## Problem

When multiple pods consume from the same Kafka partition (or events are spread across partitions), two instances might try to process the same `orderId` concurrently. The Redis dedup check and DB unique constraint can both be bypassed under concurrent execution:

```
Thread A: check Redis (miss) → check DB (miss) → process...
Thread B: check Redis (miss) → check DB (miss) → process...  ← both pass!
```

A distributed lock serializes payment processing per order, ensuring only one thread processes a given orderId at a time across all pods.

## Implementation

### Lock Registry

Configured in `DistributedLockConfig.java`:

```java
new RedisLockRegistry(connectionFactory, "payment-lock", 30_000)
```

- **Registry key prefix:** `payment-lock` — all lock keys are namespaced under `payment-lock:*` in Redis.
- **Expiry:** 30 seconds — auto-release to prevent deadlocks if a pod crashes.
- **Underlying mechanism:** Redis `SET payment-lock:order:{orderId} <uuid> NX EX 30`

### Lock Placement

The lock is acquired **outside** the `@Transactional` boundary:

```
acquire lock
  └─ @Transactional
       ├─ check Redis
       ├─ check DB
       ├─ process payment
       └─ DB commit
release lock
```

**Why not inside?**

If the lock were inside the transaction:

```
@Transactional starts
  └─ acquire lock → process → release lock → ... commit still pending ...
                                                 ↑ Thread B can acquire lock here
                                                   before Thread A's commit is visible
```

By locking outside, the DB commit completes before the lock is released, so the next thread always sees committed data.

### Self-Injection Pattern

Spring's `@Transactional` uses proxy-based AOP. A direct call to `this.processPaymentTransactional()` bypasses the proxy and the annotation is silently ignored.

**Solution:** Inject the bean into itself via `@Lazy`:

```java
public PaymentProcessor(..., @Lazy PaymentProcessor self) {
    this.self = self;
}

public Payment processPayment(...) {
    // lock acquired
    return self.processPaymentTransactional(...);  // goes through the proxy
}
```

`@Lazy` breaks the circular dependency — Spring creates a lazy proxy reference that resolves when first called.

## Failure Scenarios

| Scenario | Behavior |
|----------|----------|
| Pod crashes while holding lock | Lock auto-expires after 30s. Kafka redelivers the event. |
| Lock acquisition times out (5s) | Exception thrown. Kafka consumer retries via redelivery. `payment.lock.timeout.total` metric incremented. |
| Redis down | `lockRegistry.obtain()` fails. Payment processing is blocked until Redis recovers. |
| Lock expires during slow gateway | Another pod could acquire the lock. DB unique constraint (Layer 3) catches the resulting race condition. |
