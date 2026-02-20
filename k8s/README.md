# Kubernetes Deployment

Deploy the full Delivery ETA system to a local Kubernetes cluster.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/)
- [minikube](https://minikube.sigs.k8s.io/docs/start/) or [kind](https://kind.sigs.k8s.io/)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)

## Architecture in K8s

```
                         ┌─ delivery namespace ──────────────────────────────────────┐
                         │                                                           │
  curl (NodePort:30081)──┤──▶ order-service ──▶ Kafka ──▶ inventory-service          │
                         │                        │         │                        │
                         │                        ▼         ▼                        │
                         │                      Kafka ──▶ payment-service ──▶ Redis  │
                         │                        │                                  │
                         │                        ▼                                  │
                         │                      Kafka ──▶ delivery-service           │
                         │                        │                                  │
                         │                        ▼                                  │
  curl (NodePort:30080)──┤──▶ query-api ◄─── Kafka (all topics) ──▶ Cassandra       │
                         │                                                           │
                         │   Postgres (order_db, inventory_db, delivery_eta,         │
                         │             delivery_db)                                  │
                         └───────────────────────────────────────────────────────────┘
```

## Quick Start (minikube)

```bash
# 1. Start minikube
minikube start --memory=4096 --cpus=4

# 2. Point Docker CLI to minikube's daemon (so images are available in-cluster)
eval $(minikube docker-env)

# 3. Deploy everything
chmod +x k8s/deploy.sh
./k8s/deploy.sh

# 4. Wait for all pods to be Running
kubectl get pods -n delivery -w

# 5. Test the flow
ORDER_URL=$(minikube service order-service -n delivery --url)
QUERY_URL=$(minikube service query-api -n delivery --url)

# Create an order
curl -X POST "$ORDER_URL/api/orders" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"cust-001","currency":"USD","items":[{"itemId":"SKU-001","quantity":2,"unitPrice":25.00}]}'

# Wait a few seconds for events to propagate, then query
sleep 5
curl "$QUERY_URL/api/orders"
```

## Quick Start (kind)

```bash
# 1. Create cluster
kind create cluster --name delivery

# 2. Deploy (builds + loads images into kind)
chmod +x k8s/deploy.sh

# Build images first
for svc in order-service inventory-service payment-service delivery-service query-api; do
  docker build -f services/$svc/Dockerfile -t delivery/$svc:latest .
  kind load docker-image delivery/$svc:latest --name delivery
done

# Apply manifests
kubectl apply -f k8s/namespace.yml
kubectl apply -f k8s/infrastructure/
kubectl apply -f k8s/services/

# 3. Port-forward to access services
kubectl port-forward svc/order-service 8081:8081 -n delivery &
kubectl port-forward svc/query-api 8080:8080 -n delivery &
```

## K8s Concepts Demonstrated

| K8s Resource | Where Used | Why |
|---|---|---|
| **Namespace** | `delivery` | Isolates all resources; easy cleanup |
| **Deployment** | All services + infra | Declarative pod management with rollback |
| **Service (ClusterIP)** | Internal services | In-cluster DNS discovery (`kafka:9092`) |
| **Service (NodePort)** | order-service, query-api | External access for testing |
| **PersistentVolumeClaim** | Postgres, Kafka, Cassandra, Redis | Data survives pod restarts |
| **ConfigMap** | Postgres init SQL | Inject config without rebuilding images |
| **Secret** | Postgres password | Sensitive values separated from manifests |
| **Resource requests/limits** | All pods | Scheduler placement + OOM protection |
| **Readiness probes** | All pods | K8s only routes traffic to healthy pods |
| **Environment variables** | All services | Externalized config (12-factor app) |
| **Labels & selectors** | All resources | Service-to-Pod routing, organization |

## Useful Commands

```bash
# Watch all pods
kubectl get pods -n delivery -w

# Logs for a service
kubectl logs -f deployment/order-service -n delivery

# Describe a pod (debugging)
kubectl describe pod -l app=payment-service -n delivery

# Scale a service
kubectl scale deployment inventory-service --replicas=3 -n delivery

# Restart a service
kubectl rollout restart deployment/payment-service -n delivery

# Port-forward for debugging
kubectl port-forward svc/postgres 5432:5432 -n delivery

# Exec into a pod
kubectl exec -it deployment/redis -n delivery -- redis-cli

# Tear down everything
./k8s/teardown.sh
```

## File Structure

```
k8s/
├── README.md
├── deploy.sh              # Build images + deploy all resources
├── teardown.sh            # Delete the namespace and all resources
├── namespace.yml
├── infrastructure/
│   ├── zookeeper.yml      # Deployment + Service + PVC
│   ├── kafka.yml          # Deployment + Service + PVC
│   ├── postgres.yml       # Deployment + Service + PVC + ConfigMap + Secret
│   ├── cassandra.yml      # Deployment + Service + PVC
│   └── redis.yml          # Deployment + Service + PVC
└── services/
    ├── order-service.yml       # Deployment + NodePort Service
    ├── inventory-service.yml   # Deployment + ClusterIP Service
    ├── payment-service.yml     # Deployment + ClusterIP Service
    ├── delivery-service.yml    # Deployment + ClusterIP Service
    └── query-api.yml           # Deployment + NodePort Service
```
