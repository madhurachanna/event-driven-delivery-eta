# Idempotency & Deduplication

## Problem

Kafka provides **at-least-once** delivery. Network blips, consumer rebalances, or broker failovers can cause the same `inventory.reserved` event to be delivered multiple times. Without deduplication, the payment service would charge a customer more than once for the same order.

## Three-Layer Strategy

The payment service uses three complementary layers, each addressing a different failure mode:

### Layer 1 — Distributed Lock (per orderId)

```
Lock lock = lockRegistry.obtain("order:" + orderId);
lock.tryLock(5, TimeUnit.SECONDS);
```

- **What it prevents:** Two threads/pods processing the *same order* simultaneously.
- **Mechanism:** Redis `SET NX` with 30-second TTL via `RedisLockRegistry`.
- **Scope:** Per `orderId` — unrelated orders are not blocked.
- **Failure mode:** If the lock holder crashes, the lock auto-releases after 30s.

### Layer 2 — Redis Dedup Cache (fast path)

```
redisTemplate.hasKey("payment:dedup:" + eventId)  // O(1), sub-millisecond
```

- **What it prevents:** Sequential duplicate events (same event delivered minutes apart).
- **Mechanism:** After successful DB commit, store `eventId` in Redis with a 24-hour TTL.
- **Performance:** O(1) lookup, sub-millisecond — catches ~99% of duplicates without touching Postgres.
- **Failure mode:** Redis restart or TTL expiry → key is lost. Layer 3 catches this.

### Layer 3 — Database Unique Constraint (safety net)

```sql
ALTER TABLE payments ADD CONSTRAINT uk_idempotency_key UNIQUE (idempotency_key);
```

- **What it prevents:** Any duplicate that slipped past Layers 1 and 2.
- **Mechanism:** The `idempotencyKey` column has a `UNIQUE` constraint. Attempting a duplicate insert throws `DataIntegrityViolationException`, which is caught and handled gracefully.
- **Trade-off:** Hitting this layer means a wasted DB round-trip, but it guarantees correctness.

## Two-Step Commit Pattern

Redis and Kafka writes happen **after** the DB transaction commits:

```
1. Begin transaction
2. Check Redis → Check DB → Process → Save to DB
3. Register afterCommit() hook
4. Transaction commits ← only now is the payment durable
5. afterCommit() fires:
   a. Mark eventId in Redis
   b. Publish Kafka event
```

**Why not write to Redis/Kafka inside the transaction?**

If the DB commit fails after we've already written to Redis, there's a false-positive dedup entry — future legitimate events for that order would be silently skipped. The `TransactionSynchronization.afterCommit()` hook guarantees side effects only occur after a successful commit.

**What if Redis/Kafka fails after commit?**

- **Redis failure:** Logged and ignored. The DB constraint is the durable safety net. The next duplicate will hit Layer 3 instead of Layer 2 — slower but still correct.
- **Kafka failure:** Logged as an error. In production, this would trigger a DLQ or compensating mechanism.

## Tuning

| Parameter | Default | Guidance |
|-----------|---------|----------|
| `dedup.redis.ttl-hours` | 24 | Should exceed your maximum Kafka consumer retry window. If retries can span 12 hours, set ≥12. |
| Lock TTL (`DistributedLockConfig`) | 30s | Should exceed worst-case payment processing time (gateway timeout + DB latency). |
| Lock wait (`LOCK_WAIT_SECONDS`) | 5s | How long a thread waits for a lock. If exceeded, the event is redelivered by Kafka. |
