# ClinicOS on Kubernetes

Production-grade manifests for the ClinicOS platform, sized for an initial
~10,000 customers.

## Files

| File | Purpose |
|------|---------|
| `00-namespace.yaml` | `clinicos` namespace |
| `01-configmap.yaml` | Shared non-secret config (DB URL, Redis, Kafka, service URLs) |
| `02-secrets.example.yaml` | Secret **template** — create the real one with `kubectl create secret` |
| `10-infra.yaml` | Postgres/Redis/Kafka for **dev/staging only** (use managed services in prod) |
| `20-apps.yaml` | Deployments + Services for all 9 microservices |
| `30-autoscaling.yaml` | HPAs (gateway, patient, appointment) + PodDisruptionBudgets |
| `40-ingress.yaml` | TLS ingress → gateway only |

## Deploy

```bash
# 1. Build & push images (replace REGISTRY with e.g. asia-south1-docker.pkg.dev/PROJECT/clinicos)
mvn clean package -DskipTests
for m in clinic-patient clinic-appointment clinic-followup clinic-notification clinic-billing clinic-emr clinic-staff clinic-feedback clinic-gateway; do
  docker build -t REGISTRY/$m:1.0.0 ./$m && docker push REGISTRY/$m:1.0.0
done

# 2. Create namespace + config
kubectl apply -f k8s/00-namespace.yaml -f k8s/01-configmap.yaml

# 3. Create real secrets (never apply 02-secrets.example.yaml with defaults!)
kubectl -n clinicos create secret generic clinicos-secrets \
  --from-literal=DB_PASSWORD='...' \
  --from-literal=JWT_SECRET='...' \
  --from-literal=MAIL_USERNAME='' --from-literal=MAIL_PASSWORD='' \
  --from-literal=GOOGLE_CLIENT_ID='' --from-literal=GOOGLE_CLIENT_SECRET=''

# 4. Infra (dev/staging) then apps
kubectl apply -f k8s/10-infra.yaml
sed "s|REGISTRY|your-registry-here|g" k8s/20-apps.yaml | kubectl apply -f -
kubectl apply -f k8s/30-autoscaling.yaml -f k8s/40-ingress.yaml

# 5. Verify
kubectl -n clinicos get pods,hpa,ingress
```

## Production checklist

- [ ] Replace in-cluster Postgres with **Cloud SQL / RDS** (update `DB_URL` in the ConfigMap, remove Postgres from `10-infra.yaml`)
- [ ] Replace in-cluster Redis with **Memorystore / ElastiCache**
- [ ] Replace single-node Kafka with **MSK / Confluent Cloud / Strimzi** (3 brokers, RF=3)
- [ ] Point `40-ingress.yaml` at your real domain + cert-manager issuer
- [ ] Enable `management.health.probes.enabled=true` in each service's prod profile for readiness/liveness endpoints
- [ ] Install metrics-server (required for HPA) and Prometheus stack (`kube-prometheus-stack` Helm chart)

## Sizing guidance (10k customers ≈ 200–500 concurrent users)

| Component | Initial | Scale trigger |
|-----------|---------|---------------|
| Gateway | 2 pods (0.5–2 CPU) | HPA at 65% CPU, max 8 |
| Patient/Appointment | 2 pods each | HPA at 65% CPU, max 6 |
| Other services | 1–2 pods | Manual/HPA later |
| Postgres | 2 vCPU / 8 GB, 300 conns | Read replica when read p95 > 100ms |
| Redis | 512 MB–1 GB | Monitor eviction rate |
| Kafka | 1 broker dev / 3 prod | Partition count 6 per topic |
| Nodes | 3 × 4 vCPU / 16 GB | Cluster autoscaler |

