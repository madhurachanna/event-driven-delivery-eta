# Payment Service

Processes payments in response to inventory reservation events. Part of the event-driven delivery ETA platform.

## Quick Start

```bash
# Prerequisites: Postgres, Redis, Kafka running locally
mvn spring-boot:run    # Starts on port 8082
```

## Architecture

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────┐
│    Kafka      │────▶│  PaymentEvent    │────▶│  Payment     │
│ (inventory    │     │  Listener        │     │  Processor   │
│  events)      │     └──────────────────┘     └──────┬───────┘
└──────────────┘                                      │
                                                      ▼
                                          ┌───────────────────────┐
                                          │  Three-Layer Dedup    │
                                          │  1. Distributed Lock  │
                                          │  2. Redis (fast O(1)) │
                                          │  3. DB Constraint     │
                                          └───────────┬───────────┘
                                                      │
                               ┌──────────────────────┼──────────────────────┐
                               ▼                      ▼                      ▼
                         ┌──────────┐           ┌──────────┐           ┌──────────┐
                         │ Postgres │           │  Redis   │           │  Kafka   │
                         │ (source  │           │ (dedup   │           │ (payment │
                         │  of      │           │  cache)  │           │  events) │
                         │  truth)  │           └──────────┘           └──────────┘
                         └──────────┘
```

**Event flow:** `inventory.reserved` → process payment → publish `payment.authorized` or `payment.failed`

## Key Design Decisions

Detailed technical documentation is in the [docs/](docs/) folder:

| Document | Contents |
|----------|----------|
| [Idempotency & Deduplication](../../docs/idempotency.md) | Three-layer dedup strategy, Redis vs DB trade-offs, TTL tuning |
| [Distributed Locking](../../docs/distributed-locking.md) | Lock placement relative to transactions, self-injection pattern |
| [Observability](../../docs/observability.md) | Metrics catalog, Prometheus/Actuator configuration |

## API / Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Health check (DB, Redis, Kafka) |
| `GET /actuator/prometheus` | Prometheus metrics scrape endpoint |

## Configuration

See [application.yml](src/main/resources/application.yml) for all configurable properties. Key knobs:

| Property | Default | Purpose |
|----------|---------|---------|
| `dedup.redis.ttl-hours` | 24 | Redis dedup key TTL |
| `dedup.redis.key-prefix` | `payment:dedup:` | Redis key namespace |
| `management.endpoints.web.exposure.include` | `health,info,prometheus` | Exposed actuator endpoints |

## Project Structure

```
src/main/java/com/delivery/payment/
├── config/
│   ├── DistributedLockConfig.java   # RedisLockRegistry bean
│   ├── JacksonConfig.java           # ObjectMapper with Java time
│   └── PaymentMetrics.java          # Micrometer counters & timer
├── dto/
│   ├── InventoryEventPayload.java   # Inbound event payload
│   └── PaymentEventPayload.java     # Outbound event payload
├── entity/
│   ├── Payment.java                 # JPA entity
│   └── PaymentStatus.java           # PENDING → AUTHORIZED | FAILED
├── listener/
│   └── PaymentEventListener.java    # Kafka consumer
├── publisher/
│   └── PaymentEventPublisher.java   # Kafka producer
├── repository/
│   └── PaymentRepository.java       # Spring Data JPA
└── service/
    ├── PaymentProcessor.java        # Core business logic
    └── RedisDeduplicationService.java
```
