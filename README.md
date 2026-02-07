# Event-Driven Delivery ETA System

An event-driven microservices system that simulates an order-to-delivery workflow and continuously updates delivery ETA via Kafka events.

## Quick Start

### Prerequisites
- Java 21
- Maven
- Docker & Docker Compose

### 1. Start Infrastructure
```bash
docker compose -f infra/compose/compose.yml up -d
```

This starts: Zookeeper, Kafka, Postgres, Cassandra, Redis

### 2. Build Common Module (first time only)
```bash
cd services/common
mvn clean install
```

### 3. Run a Service
```bash
cd services/payment-service  # or order-service, etc.
mvn spring-boot:run
```

## Architecture

```
Flow (Kafka): order.created → inventory.reserved|rejected → payment.authorized|failed → delivery.assigned → eta.updated
```

## Services

| Service | Port | Owner | Description |
|---------|------|-------|-------------|
| order-service | 8081 | Team A | REST API creates orders, publishes `order.created` |
| inventory-service | 8083 | Team A | Reserves/rejects stock |
| payment-service | 8082 | Team B | Processes payments |
| delivery-service | 8084 | Team B | Manages delivery + ETA |
| query-api | 8080 | Team B | Read API for UI |

## Kafka Topics

| Topic | Purpose |
|-------|---------|
| `raw.order-events` | Order lifecycle events |
| `raw.inventory-events` | Stock reservation events |
| `raw.payment-events` | Payment events |
| `raw.delivery-events` | Delivery + ETA events |
| `dlq.*` | Dead-letter queues |

## Project Structure

```
├── infra/compose/          # Docker Compose for local infra
├── services/
│   ├── common/             # Shared event contracts (use this!)
│   ├── order-service/      # Team A
│   ├── inventory-service/  # Team A
│   ├── payment-service/    # Team B
│   ├── delivery-service/   # Team B
│   └── query-api/          # Team B
└── docs/                   # Architecture docs
```

## Event Envelope (Shared Contract)

All events use `com.delivery.common.event.EventEnvelope`:

```json
{
  "eventId": "uuid",
  "eventType": "order.created",
  "schemaVersion": 1,
  "occurredAt": "2024-02-07T15:00:00Z",
  "producedAt": "2024-02-07T15:00:01Z",
  "orderId": "order-123",
  "correlationId": "corr-456",
  "producer": "order-service",
  "payload": { ... }
}
```

## Adding Common Module as Dependency

In your service's `pom.xml`:
```xml
<dependency>
    <groupId>com.delivery</groupId>
    <artifactId>common</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Local Definition of Done

`docker compose up` starts infra + all services and the demo runs end-to-end.
