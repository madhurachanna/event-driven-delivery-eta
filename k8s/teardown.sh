#!/bin/bash
set -e

echo "=== Tearing down Delivery ETA from Kubernetes ==="
echo ""

echo "▸ Deleting all resources in 'delivery' namespace..."
kubectl delete namespace delivery --ignore-not-found

echo "✓ All resources deleted"
echo ""
echo "Note: PersistentVolumes may need manual cleanup depending on your storage class."
echo "  kubectl get pv | grep delivery"
