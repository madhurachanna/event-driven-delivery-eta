# Event-Driven Delivery ETA System

An event-driven microservices system that simulates an order-to-delivery workflow and continuously updates delivery ETA via Kafka events.

Local Definition of Done: `docker compose up` starts infra + all services and the demo runs end-to-end.

## Architecture (V1)
Flow (Kafka): `order.created → inventory.reserved|inventory.rejected → payment.authorized|payment.failed → delivery.assigned → eta.updated`.

All events share a common envelope to keep contracts stable and extensible (V2-ready via additive schema evolution).

## Services
- `order-service`: REST API creates orders in Postgres and publishes `order.created` to `raw.order-events`.
- `inventory-service`: Consumes `raw.order-events`, reserves/rejects stock, and publishes to `raw.inventory-events`.
- `payment-service`: Consumes `raw.inventory-events`, writes payments in Postgres, publishes to `raw.payment-events`.
- `delivery-service` (DeliveryETA): Consumes `raw.payment-events`, emits `delivery.assigned` and periodic `eta.updated`, stores history in Cassandra and current view in Redis.
- `query-api`: UI-facing API that reads current view from Redis (cache-aside), rebuilds from Postgres + Cassandra, and provides optional history + basic DLQ admin endpoints.

## Kafka topics
Main topics (one per domain area):
- `raw.order-events`
- `raw.inventory-events`
- `raw.payment-events`
- `raw.delivery-events`

Dead-letter topics:
- `dlq.order-events`
- `dlq.inventory-events`
- `dlq.payment-events`
- `dlq.delivery-events`

## Event envelope (common contract)
Every event includes:
- `eventId`
- `eventType`
- `schemaVersion` (starts at 1)
- `occurredAt`
- `producedAt`
- `orderId` (Kafka partition key)
- `correlationId`
- `producer`
- `payload`

Global rules:
- Partition key: `orderId` for order-lifecycle topics.
- Payload is immutable (new info = new event).
- Schema changes are additive.

DLQ headers (standard):
- `x-retry-count`
- `x-original-topic`
- `x-error`

## Reliability baseline
Kafka processing is at-least-once, so consumers must be idempotent (dedupe by `eventId`/idempotency key).

Use bounded retries, publish to DLQ on terminal failure, and commit offsets only after side effects are persisted.

## Repo layout
- `services/`: Spring Boot microservices (`order-service`, `inventory-service`, `payment-service`, `delivery-service`, `query-api`)
- `infra/compose/`: local Docker Compose stack for Kafka + Postgres + Redis + Cassandra
- `infra/k8s/`: optional local Kubernetes manifests (kind)
- `frontend/angular-app/`: minimal UI to create and track orders (optional for V1)
- `docs/`: architecture, event contracts, and runbooks

## Local run (infra first)
Start infra:
- `docker compose -f infra/compose/compose.yml up -d`

Then start services (instructions evolve as services are implemented) and run the demo:
- Create an order (UI or `POST /orders`) → copy `orderId`.
- Track status/ETA via `GET /ui/orders/{orderId}` on Query API.
