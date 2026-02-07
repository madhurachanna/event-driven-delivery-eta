# Infrastructure Services

This document explains each infrastructure service we're using, the concepts behind them, and how they connect.

---

## Zookeeper

### What is it?
Zookeeper is a **distributed coordination service** — think of it as a "referee" for distributed systems.

### Why does Kafka need it?

| Purpose | What Zookeeper Does |
|---------|---------------------|
| **Leader election** | Decides which Kafka broker handles writes for each partition |
| **Cluster membership** | Tracks which Kafka brokers are alive |
| **Config storage** | Stores topic configs, ACLs, quotas |

### Key Configuration

```yaml
ZOOKEEPER_CLIENT_PORT: 2181     # Port Kafka connects to
ZOOKEEPER_TICK_TIME: 2000       # Base time unit (ms) for heartbeats
```

- **Tick Time**: The base time unit in milliseconds. Session timeouts and heartbeat intervals are multiples of this.
  - Lower value = faster failure detection, but more sensitive to network jitter
  - 2000ms (2 seconds) is a common default

### When to use Zookeeper vs KRaft?

| Mode | Description | Use when |
|------|-------------|----------|
| **Zookeeper** | Traditional, battle-tested | Learning, production stability |
| **KRaft** | Kafka's built-in consensus (3.x+) | Simpler deployment, fewer moving parts |

We're using Zookeeper because it's still the most documented approach for learning Kafka.

---

## Next: Kafka

_Coming soon..._
