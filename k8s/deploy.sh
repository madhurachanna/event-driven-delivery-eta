#!/bin/bash
set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
K8S_DIR="$PROJECT_ROOT/k8s"

echo "=== Delivery ETA — Kubernetes Deployment ==="
echo ""

# ── Step 1: Build Docker images ──────────────────────────────────
echo "▸ Building Docker images (build context: project root)..."

SERVICES=("order-service" "inventory-service" "payment-service" "delivery-service" "query-api")

for svc in "${SERVICES[@]}"; do
    echo "  Building delivery/$svc..."
    docker build \
        -f "$PROJECT_ROOT/services/$svc/Dockerfile" \
        -t "delivery/$svc:latest" \
        "$PROJECT_ROOT"
done

echo "✓ All images built"
echo ""

# ── Step 2: Create namespace ─────────────────────────────────────
echo "▸ Creating namespace..."
kubectl apply -f "$K8S_DIR/namespace.yml"

# ── Step 3: Deploy infrastructure ────────────────────────────────
echo "▸ Deploying infrastructure (Zookeeper, Kafka, Postgres, Cassandra, Redis)..."
kubectl apply -f "$K8S_DIR/infrastructure/"

echo "▸ Waiting for infrastructure to be ready..."
kubectl -n delivery wait --for=condition=ready pod -l app=zookeeper --timeout=120s
kubectl -n delivery wait --for=condition=ready pod -l app=kafka --timeout=120s
kubectl -n delivery wait --for=condition=ready pod -l app=postgres --timeout=120s
kubectl -n delivery wait --for=condition=ready pod -l app=redis --timeout=120s
echo "  (Cassandra takes ~60-90s — services will retry connections)"
echo "✓ Core infrastructure ready"
echo ""

# ── Step 4: Deploy microservices ─────────────────────────────────
echo "▸ Deploying microservices..."
kubectl apply -f "$K8S_DIR/services/"
echo "✓ All services deployed"
echo ""

# ── Step 5: Show status ─────────────────────────────────────────
echo "▸ Pod status:"
kubectl get pods -n delivery
echo ""
echo "▸ Services:"
kubectl get svc -n delivery
echo ""
echo "=== Deployment complete ==="
echo ""
echo "Access points (minikube):"
echo "  Create order:  curl -X POST \$(minikube service order-service -n delivery --url)/api/orders \\"
echo "                   -H 'Content-Type: application/json' \\"
echo "                   -d '{\"customerId\":\"cust-001\",\"currency\":\"USD\",\"items\":[{\"itemId\":\"SKU-001\",\"quantity\":2,\"unitPrice\":25.00}]}'"
echo ""
echo "  Query orders:  curl \$(minikube service query-api -n delivery --url)/api/orders"
echo ""
echo "  Watch logs:    kubectl logs -f deployment/order-service -n delivery"
echo "  All pods:      kubectl get pods -n delivery -w"
