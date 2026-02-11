# Learning Notes: Event-Driven Delivery ETA System

## Kafka Fundamentals

### Topics & Partitions

| Concept | Definition | Example |
|---------|------------|---------|
| **Topic** | Named channel for messages | `raw.payment-events` |
| **Partition** | Subdivision for parallelism | 3 partitions per topic |
| **Partition Key** | Determines which partition | `orderId` → same order, same partition |

```
Topic: raw.inventory-events (3 partitions)
┌──────────┐  ┌──────────┐  ┌──────────┐
│Partition0│  │Partition1│  │Partition2│
│ Order-A  │  │ Order-B  │  │ Order-C  │
│ Order-D  │  │ Order-E  │  │ Order-F  │
└──────────┘  └──────────┘  └──────────┘
```

### Consumer Groups

**Same group** = messages split (work sharing)
**Different groups** = messages duplicated (each group sees all)

```
                    Topic
                      │
         ┌────────────┼────────────┐
         ▼                         ▼
   payment-service-group     analytics-group
   (splits work)             (gets same messages)
```

### Partitions vs Instances

| Scenario | Result |
|----------|--------|
| 3 partitions, 3 instances | Perfect — each gets 1 |
| 3 partitions, 2 instances | One instance gets 2 partitions |
| 2 partitions, 3 instances | One instance sits IDLE |

**Rule**: Max parallelism = number of partitions

---

## Java/Spring Concepts

### Package Structure

```
src/main/java/com/delivery/payment/listener/
                 ↑      ↑       ↑
              domain  module  feature
```

Package in code MUST match folder path after `src/main/java/`.

### @KafkaListener

```java
@KafkaListener(topics = Topics.INVENTORY_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
public void handleEvent(String message) { ... }
```

- `topics` — which Kafka topic to subscribe to
- `groupId` — consumer group (from application.yml via `${}`)
- Spring auto-commits offset after method completes successfully

### Property Placeholders

`${spring.kafka.consumer.group-id}` reads from `application.yml`:
```yaml
spring:
  kafka:
    consumer:
      group-id: payment-service-group  # ← injected
```

---

## System Flow

```
order.created → inventory.reserved → payment.authorized → delivery.assigned → eta.updated
```

### Payment Service

| Listens To | Publishes |
|------------|-----------|
| `raw.inventory-events` | `raw.payment-events` |
| `inventory.reserved` | `payment.authorized` or `payment.failed` |

---

## Database Patterns

### Database Per Service

Each service owns its tables — no cross-service JOINs.
Communication happens via Kafka events, not database queries.

| Service | Storage |
|---------|---------|
| order-service | Postgres (orders) |
| payment-service | Postgres (payments) |
| delivery-service | Cassandra (history) + Redis (current) |

### Idempotency

Kafka delivers at-least-once → duplicates possible.
Solution: Store `idempotency_key` (event ID) with unique constraint.

---

## Quick Reference

| Term | One-liner |
|------|-----------|
| Partition | Unit of parallelism in Kafka |
| Consumer Group | Consumers that share partition work |
| Offset | Bookmark — position in partition |
| ACID | Atomicity, Consistency, Isolation, Durability |
| At-least-once | Message may be delivered multiple times |
| Idempotency | Same operation, same result (handles duplicates) |
