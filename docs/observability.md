# Observability

## Stack

| Component | Role |
|-----------|------|
| **Micrometer** | Vendor-neutral metrics API (like SLF4J for metrics) |
| **Spring Boot Actuator** | Exposes `/actuator/*` endpoints (health, info, prometheus) |
| **Prometheus** | Scrapes `/actuator/prometheus` on a configurable interval |
| **Grafana** | Dashboards and alerting (not yet configured — add as a next step) |

## Endpoints

| URL | Description |
|-----|-------------|
| `GET /actuator/health` | Component health: DB, Redis, Kafka — returns `UP`/`DOWN` with details |
| `GET /actuator/info` | Application metadata |
| `GET /actuator/prometheus` | All metrics in Prometheus exposition format |

## Metrics Reference

All metrics are defined in `PaymentMetrics.java` and registered with the Micrometer `MeterRegistry`.

### Counters

| Metric | Prometheus Name | Description |
|--------|----------------|-------------|
| `payment.processed.total` | `payment_processed_total` | Payments successfully authorized |
| `payment.failed.total` | `payment_failed_total` | Payments failed at gateway |
| `payment.duplicate.total` | `payment_duplicate_total` | Duplicate events detected (all layers) |
| `payment.dedup.redis.hit.total` | `payment_dedup_redis_hit_total` | Duplicates caught by Redis fast-path |
| `payment.dedup.db.hit.total` | `payment_dedup_db_hit_total` | Duplicates caught by DB constraint |
| `payment.lock.timeout.total` | `payment_lock_timeout_total` | Distributed lock acquisition timeouts |

### Timer / Histogram

| Metric | Prometheus Name | Description |
|--------|----------------|-------------|
| `payment.processing.duration` | `payment_processing_duration_seconds` | End-to-end duration (lock acquire → commit → lock release) |

Percentile histograms are enabled in `application.yml`:

```yaml
management:
  metrics:
    distribution:
      percentiles-histogram:
        payment.processing.duration: true
```

This allows querying p50, p95, p99 in Prometheus:

```promql
histogram_quantile(0.99, rate(payment_processing_duration_seconds_bucket[5m]))
```

## Example Queries

### Payment throughput
```promql
rate(payment_processed_total[5m])
```

### Duplicate event ratio
```promql
rate(payment_duplicate_total[5m]) / rate(payment_processed_total[5m])
```

### Redis dedup effectiveness
```promql
rate(payment_dedup_redis_hit_total[5m])
  / (rate(payment_dedup_redis_hit_total[5m]) + rate(payment_dedup_db_hit_total[5m]))
```

### P99 processing latency
```promql
histogram_quantile(0.99, rate(payment_processing_duration_seconds_bucket[5m]))
```

## Configuration

Key properties in `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    tags:
      application: payment-service    # global tag on all metrics
```
