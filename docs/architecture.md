# Architecture

## System Overview

An event-driven microservices system that processes orders through inventory reservation, payment authorization, and delivery assignment — updating a real-time read model for order tracking.

Each service owns its data, communicates exclusively through Kafka events, and can be deployed and scaled independently.

## Service Map

| Service | Port | Responsibility | Data Store | Kafka Role |
|---------|------|---------------|------------|------------|
| **order-service** | 8081 | Accept orders via REST, publish `order.created` | Postgres (`order_db`) | Producer |
| **inventory-service** | 8083 | Reserve stock, publish `inventory.reserved` or `inventory.rejected` | Postgres (`inventory_db`) | Consumer → Producer |
| **payment-service** | 8082 | Process payments with three-layer dedup, publish `payment.authorized` or `payment.failed` | Postgres (`delivery_eta`) + Redis | Consumer → Producer |
| **delivery-service** | 8084 | Assign driver and ETA, publish `delivery.assigned` + `eta.updated` | Postgres (`delivery_db`) | Consumer → Producer |
| **query-api** | 8080 | Materialize all events into a denormalized read model, serve REST queries | Cassandra (`delivery_query`) | Consumer |

## Event Flow (Happy Path)

```
                POST /api/orders
                      │
                      ▼
              ┌───────────────┐
              │ order-service  │──────── Postgres (order_db)
              └───────┬───────┘
                      │ order.created
                      ▼
              ┌───────────────┐
              │  inventory-   │──────── Postgres (inventory_db)
              │   service     │
              └───────┬───────┘
                      │ inventory.reserved
                      ▼
              ┌───────────────┐
              │   payment-    │──────── Postgres (delivery_eta)
              │   service     │──────── Redis (dedup + locks)
              └───────┬───────┘
                      │ payment.authorized
                      ▼
              ┌───────────────┐
              │  delivery-    │──────── Postgres (delivery_db)
              │   service     │
              └───────┬───────┘
                      │ delivery.assigned
                      │ eta.updated
                      ▼
              ┌───────────────┐
              │   query-api   │──────── Cassandra (delivery_query)
              └───────────────┘
                      │
                      ▼
              GET /api/orders/{id}
```

All events pass through Kafka topics. The query-api consumes from **all four** topics to build its read model.

## Event Types

| Event | Topic | Producer | Trigger |
|-------|-------|----------|---------|
| `order.created` | `raw.order-events` | order-service | POST /api/orders |
| `inventory.reserved` | `raw.inventory-events` | inventory-service | Stock available for order |
| `inventory.rejected` | `raw.inventory-events` | inventory-service | Insufficient stock |
| `payment.authorized` | `raw.payment-events` | payment-service | Payment gateway approved |
| `payment.failed` | `raw.payment-events` | payment-service | Payment gateway declined |
| `delivery.assigned` | `raw.delivery-events` | delivery-service | Driver assigned, initial ETA set |
| `eta.updated` | `raw.delivery-events` | delivery-service | ETA recalculated |

All events use the shared `EventEnvelope<T>` contract from the `common` module, providing consistent metadata (eventId, eventType, orderId, correlationId, timestamp, producer) across services.

## Order State Machine

```
CREATED ──▶ INVENTORY_RESERVED ──▶ PAYMENT_AUTHORIZED ──▶ DELIVERY_ASSIGNED
   │                                      │
   ▼                                      ▼
INVENTORY_REJECTED                  PAYMENT_FAILED
```

Each state transition is driven by consuming the corresponding event. The query-api tracks the current state in its Cassandra read model.

## Key Design Patterns

### Event-Driven Architecture

Services are fully decoupled. The order-service has no knowledge of inventory, payment, or delivery services. Each service reacts to events published by the upstream service. This enables:

- **Independent deployment** — Change one service without redeploying others.
- **Independent scaling** — Scale payment processing without affecting order creation.
- **Failure isolation** — If delivery-service goes down, orders and payments continue working.

### Choreography-Based Saga

The order fulfillment flow is a distributed transaction (saga) implemented through event choreography rather than a central orchestrator. Each service decides what to do based on the events it receives, and publishes the result for the next service.

**Trade-off:** Simpler than orchestration (no single point of failure), but harder to visualize the overall flow. The event chain is implicit in the code rather than explicit in one place.

### CQRS (Command Query Responsibility Segregation)

Write operations go through individual service databases (Postgres), while read operations are served from a denormalized view in Cassandra:

```
Write side:   order-service → Postgres (order_db)        ← normalized, transactional
              payment-service → Postgres (delivery_eta)   ← normalized, transactional

Read side:    query-api → Cassandra (delivery_query)      ← denormalized, eventually consistent
```

**Why separate stores?**

- **Postgres** is optimized for transactional writes with strong consistency.
- **Cassandra** is optimized for fast reads at scale with tunable consistency.
- Each side can be scaled and optimized independently.
- The read model is materialized from events, so it's always rebuildable.

**Trade-off:** The read model is eventually consistent — there's a short delay between a write and when it appears in the query-api. For an order tracking UI, this is acceptable.

### Idempotency (Three-Layer Deduplication)

Kafka's at-least-once delivery means events can be duplicated. The payment-service implements three complementary layers to prevent double charges:

1. **Distributed Lock** — Serializes per-orderId processing across pods.
2. **Redis Cache** — O(1) fast-path duplicate detection.
3. **DB Unique Constraint** — Durable safety net.

See [Idempotency & Deduplication](idempotency.md) and [Distributed Locking](distributed-locking.md) for full details.

### Event Envelope Pattern

All events share a common structure via `EventEnvelope<T>` in the `common` module:

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "payment.authorized",
  "schemaVersion": 1,
  "occurredAt": "2024-02-07T15:00:00Z",
  "producedAt": "2024-02-07T15:00:01Z",
  "orderId": "order-123",
  "correlationId": "order-123",
  "producer": "payment-service",
  "payload": { ... }
}
```

This provides a consistent contract for routing, tracing, and auditing without coupling services to each other's internal data models.

## Infrastructure

See [Infrastructure Services](infrastructure-services.md) for Docker Compose setup, Kafka configuration, database details, and health checks.

## Kubernetes Deployment

The system includes full K8s manifests for deploying to a Kubernetes cluster. See [`k8s/README.md`](../k8s/README.md) for deployment instructions and a mapping of K8s concepts used.

## Further Reading

| Document | Contents |
|----------|----------|
| [Infrastructure Services](infrastructure-services.md) | Docker Compose, Kafka topics, Postgres databases, Redis usage |
| [Idempotency & Deduplication](idempotency.md) | Three-layer strategy, two-step commit pattern, tuning |
| [Distributed Locking](distributed-locking.md) | Redis lock placement, self-injection pattern, failure scenarios |
| [Observability](observability.md) | Micrometer metrics catalog, Prometheus queries, Actuator config |
