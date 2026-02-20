# Infrastructure Services

All infrastructure services are defined in [`infra/compose/compose.yml`](../infra/compose/compose.yml) and run as Docker containers.

## Services

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| **Zookeeper** | `confluentinc/cp-zookeeper:7.5.0` | 2181 | Kafka cluster coordination (leader election, broker membership, config storage) |
| **Kafka** | `confluentinc/cp-kafka:7.5.0` | 9092 (host), 29092 (Docker internal) | Event streaming backbone — all inter-service communication flows through Kafka topics |
| **Postgres** | `postgres:15-alpine` | 5432 | Relational store for order, inventory, payment, and delivery services (source of truth) |
| **Cassandra** | `cassandra:4.1` | 9042 | Read-optimized query store for the CQRS read model (query-api) |
| **Redis** | `redis:7-alpine` | 6379 | Event deduplication cache and distributed locking (payment-service) |

## Startup

```bash
cd infra/compose
docker compose up -d
```

Services start in dependency order via health checks — Kafka waits for Zookeeper, all others start independently.

## Kafka

### Listener Configuration

Kafka exposes two listeners for different network contexts:

| Listener | Address | Used by |
|----------|---------|---------|
| `PLAINTEXT_HOST` | `localhost:9092` | Application services running on the host (outside Docker) |
| `PLAINTEXT` | `kafka:29092` | Services running inside the Docker network |

In Kubernetes, a single `PLAINTEXT` listener is used since all pods communicate over the cluster network.

### Topics

Topics are auto-created (`KAFKA_AUTO_CREATE_TOPICS_ENABLE: true`). In production, topics should be pre-created with explicit partition counts and replication factors.

| Topic | Producer | Consumer(s) |
|-------|----------|-------------|
| `raw.order-events` | order-service | inventory-service, query-api |
| `raw.inventory-events` | inventory-service | payment-service, query-api |
| `raw.payment-events` | payment-service | delivery-service, query-api |
| `raw.delivery-events` | delivery-service | query-api |

Dead-letter queues follow the same naming pattern with a `dlq.` prefix:

| DLQ Topic | Source |
|-----------|--------|
| `dlq.order-events` | Failed order event processing |
| `dlq.inventory-events` | Failed inventory event processing |
| `dlq.payment-events` | Failed payment event processing |
| `dlq.delivery-events` | Failed delivery event processing |

Topic and event-type constants are defined in the shared `common` module (`Topics.java`, `EventTypes.java`) so all services stay in sync.

## Postgres

Each microservice owns its own database, enforcing data isolation at the storage level:

| Database | Service | Tables |
|----------|---------|--------|
| `order_db` | order-service | `orders`, `order_items` |
| `inventory_db` | inventory-service | `reservations` |
| `delivery_eta` | payment-service | `payments` |
| `delivery_db` | delivery-service | `deliveries` |

- **Credentials:** `postgres` / `postgres` (dev only — use K8s Secrets or a vault in production)
- **Initialization:** `infra/compose/init-postgres.sql` creates all databases on first startup. Mounted into the container at `/docker-entrypoint-initdb.d/`.
- **Schema management:** Tables are auto-created via JPA `ddl-auto: update` in development. Production deployments should use versioned migrations (Flyway / Liquibase).

## Cassandra

Used exclusively by the query-api service as the CQRS read model store.

- **Keyspace:** `delivery_query` (auto-created by the application on startup)
- **Table:** `order_view` — denormalized view materialized from all event streams
- **Datacenter:** `dc1` (SimpleStrategy, replication factor 1 for local dev)

## Redis

Used by the payment service for two distinct purposes:

| Use Case | Key Pattern | TTL |
|----------|-------------|-----|
| Event deduplication | `payment:dedup:{eventId}` | 24h |
| Distributed locks | `payment-lock:order:{orderId}` | 30s |

See [Idempotency](idempotency.md) and [Distributed Locking](distributed-locking.md) for detailed design rationale.

## Data Persistence

All stateful services use named Docker volumes:

| Volume | Service | Path in Container |
|--------|---------|-------------------|
| `postgres_data` | Postgres | `/var/lib/postgresql/data` |
| `cassandra_data` | Cassandra | `/var/lib/cassandra` |
| `redis_data` | Redis | `/data` |

Data persists across container restarts. To reset all data: `docker compose down -v`.

## Health Checks

Every infrastructure service includes a health check:

| Service | Check | Interval |
|---------|-------|----------|
| Zookeeper | TCP connect on port 2181 | 10s |
| Kafka | `kafka-broker-api-versions` | 10s |
| Postgres | `pg_isready` | 10s |
| Cassandra | `cqlsh -e 'describe cluster'` | 30s |
| Redis | `redis-cli ping` | 10s |

Use `docker compose ps` to verify all services are healthy before starting application services.
