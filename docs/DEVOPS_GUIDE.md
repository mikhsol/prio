# Jeeves - DevOps & Infrastructure Guide

## Infrastructure Overview

### Cloud Architecture

We follow a cloud-agnostic approach where possible, with primary deployment on AWS. The infrastructure is fully defined as code using Terraform.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              EDGE LAYER                                      │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                    CloudFlare / AWS CloudFront                          ││
│  │                    (CDN, DDoS Protection, WAF)                          ││
│  └─────────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
┌─────────────────────────────────────┼───────────────────────────────────────┐
│                              AWS REGION                                      │
│                                     │                                        │
│  ┌─────────────────────────────────┴──────────────────────────────────────┐ │
│  │                         Application Load Balancer                       │ │
│  └─────────────────────────────────┬──────────────────────────────────────┘ │
│                                    │                                         │
│  ┌────────────────────── VPC ──────┴────────────────────────────────────┐   │
│  │                                                                       │   │
│  │  ┌──────────── Public Subnets ────────────────────────────────────┐  │   │
│  │  │  NAT Gateway    │    Bastion Host (optional)                   │  │   │
│  │  └────────────────────────────────────────────────────────────────┘  │   │
│  │                              │                                        │   │
│  │  ┌──────────── Private Subnets (Apps) ────────────────────────────┐  │   │
│  │  │                                                                 │  │   │
│  │  │  ┌─────────────────── EKS Cluster ──────────────────────────┐  │  │   │
│  │  │  │                                                           │  │  │   │
│  │  │  │   ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐    │  │  │   │
│  │  │  │   │  API    │  │   AI    │  │  Auth   │  │  Sync   │    │  │  │   │
│  │  │  │   │  Pods   │  │  Pods   │  │  Pods   │  │  Pods   │    │  │  │   │
│  │  │  │   └─────────┘  └─────────┘  └─────────┘  └─────────┘    │  │  │   │
│  │  │  │                                                           │  │  │   │
│  │  │  └───────────────────────────────────────────────────────────┘  │  │   │
│  │  │                                                                 │  │   │
│  │  └─────────────────────────────────────────────────────────────────┘  │   │
│  │                              │                                        │   │
│  │  ┌──────────── Private Subnets (Data) ────────────────────────────┐  │   │
│  │  │                                                                 │  │   │
│  │  │   ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐   │  │   │
│  │  │   │  RDS        │  │  ElastiCache│  │  OpenSearch         │   │  │   │
│  │  │   │  PostgreSQL │  │  Redis      │  │  (optional)         │   │  │   │
│  │  │   └─────────────┘  └─────────────┘  └─────────────────────┘   │  │   │
│  │  │                                                                 │  │   │
│  │  └─────────────────────────────────────────────────────────────────┘  │   │
│  │                                                                       │   │
│  └───────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Infrastructure as Code

### Directory Structure

```
infrastructure/
├── terraform/
│   ├── environments/
│   │   ├── dev/
│   │   │   ├── main.tf
│   │   │   ├── variables.tf
│   │   │   └── terraform.tfvars
│   │   ├── staging/
│   │   └── production/
│   ├── modules/
│   │   ├── vpc/
│   │   ├── eks/
│   │   ├── rds/
│   │   ├── elasticache/
│   │   ├── s3/
│   │   └── monitoring/
│   └── shared/
│       └── backend.tf
├── kubernetes/
│   ├── base/
│   │   ├── namespace.yaml
│   │   ├── network-policies/
│   │   └── rbac/
│   ├── apps/
│   │   ├── api/
│   │   ├── ai-engine/
│   │   ├── auth/
│   │   └── sync/
│   └── overlays/
│       ├── dev/
│       ├── staging/
│       └── production/
└── helm/
    └── jeeves/
        ├── Chart.yaml
        ├── values.yaml
        └── templates/
```

### Terraform Conventions

```hcl
# Example module structure
# modules/rds/main.tf

resource "aws_db_instance" "main" {
  identifier = var.identifier
  
  engine               = "postgres"
  engine_version       = var.engine_version
  instance_class       = var.instance_class
  allocated_storage    = var.allocated_storage
  storage_encrypted    = true
  kms_key_id          = var.kms_key_arn
  
  db_name  = var.database_name
  username = var.master_username
  password = random_password.master.result
  
  vpc_security_group_ids = [var.security_group_id]
  db_subnet_group_name   = var.subnet_group_name
  
  backup_retention_period = var.backup_retention_days
  backup_window          = "03:00-04:00"
  maintenance_window     = "Mon:04:00-Mon:05:00"
  
  deletion_protection = var.environment == "production"
  skip_final_snapshot = var.environment != "production"
  
  performance_insights_enabled = true
  
  tags = merge(var.tags, {
    Name        = var.identifier
    Environment = var.environment
  })
}
```

---

## CI/CD Pipeline

### GitHub Actions Workflow

```yaml
# .github/workflows/ci.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  RUST_BACKTRACE: 1
  CARGO_TERM_COLOR: always

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Rust
        uses: dtolnay/rust-action@stable
        with:
          components: rustfmt, clippy
          
      - name: Cache cargo
        uses: actions/cache@v4
        with:
          path: |
            ~/.cargo/bin/
            ~/.cargo/registry/
            ~/.cargo/git/
            target/
          key: ${{ runner.os }}-cargo-${{ hashFiles('**/Cargo.lock') }}
          
      - name: Format check
        run: cargo fmt --all -- --check
        
      - name: Clippy
        run: cargo clippy -j 2 --all-targets -- -D warnings
        
      - name: Setup Go
        uses: actions/setup-go@v5
        with:
          go-version: '1.22'
          
      - name: Go lint
        uses: golangci/golangci-lint-action@v4

  test:
    runs-on: ubuntu-latest
    needs: lint
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: jeeves_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
      redis:
        image: redis:7
        ports:
          - 6379:6379
          
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Rust
        uses: dtolnay/rust-action@stable
        
      - name: Run Rust tests
        run: cargo test -j 2 -- --test-threads=4
        env:
          DATABASE_URL: postgres://test:test@localhost:5432/jeeves_test
          REDIS_URL: redis://localhost:6379
          
      - name: Setup Go
        uses: actions/setup-go@v5
        with:
          go-version: '1.22'
          
      - name: Run Go tests
        run: go test -race -coverprofile=coverage.out ./...
        
      - name: Upload coverage
        uses: codecov/codecov-action@v4

  security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Rust security audit
        run: |
          cargo install cargo-audit
          cargo audit
          
      - name: Go security scan
        run: |
          go install github.com/securego/gosec/v2/cmd/gosec@latest
          gosec ./...
          
      - name: Trivy scan
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: '.'
          severity: 'CRITICAL,HIGH'

  build:
    runs-on: ubuntu-latest
    needs: [test, security]
    if: github.ref == 'refs/heads/main' || github.ref == 'refs/heads/develop'
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
        
      - name: Login to ECR
        uses: aws-actions/amazon-ecr-login@v2
        
      - name: Build and push API
        uses: docker/build-push-action@v5
        with:
          context: ./services/api
          push: true
          tags: |
            ${{ env.ECR_REGISTRY }}/jeeves-api:${{ github.sha }}
            ${{ env.ECR_REGISTRY }}/jeeves-api:${{ github.ref_name }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  deploy-staging:
    runs-on: ubuntu-latest
    needs: build
    if: github.ref == 'refs/heads/develop'
    environment: staging
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Deploy to staging
        run: |
          # ArgoCD sync or kubectl apply
          argocd app sync jeeves-staging --revision ${{ github.sha }}

  deploy-production:
    runs-on: ubuntu-latest
    needs: build
    if: github.ref == 'refs/heads/main'
    environment: production
    
    steps:
      - name: Deploy canary
        run: |
          argocd app sync jeeves-production --revision ${{ github.sha }} \
            --preview-name canary --preview-replicas 1
            
      - name: Wait for canary metrics
        run: sleep 300  # 5 minute observation
        
      - name: Promote or rollback
        run: |
          # Check metrics and either promote or rollback
          ./scripts/check-canary-metrics.sh && \
          argocd app sync jeeves-production --revision ${{ github.sha }} || \
          argocd app rollback jeeves-production
```

---

## Container Configuration

### Dockerfile Standards

```dockerfile
# services/api/Dockerfile
# Multi-stage build for Go service

# Build stage
FROM golang:1.22-alpine AS builder

WORKDIR /app

# Cache dependencies
COPY go.mod go.sum ./
RUN go mod download

# Build
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-w -s" -o /api ./cmd/api

# Runtime stage
FROM gcr.io/distroless/static-debian12:nonroot

COPY --from=builder /api /api

USER nonroot:nonroot

EXPOSE 8080

ENTRYPOINT ["/api"]
```

```dockerfile
# services/ai-engine/Dockerfile
# Multi-stage build for Rust service

# Build stage
FROM rust:1.79-slim-bookworm AS builder

WORKDIR /app

# Install dependencies
RUN apt-get update && apt-get install -y \
    pkg-config \
    libssl-dev \
    && rm -rf /var/lib/apt/lists/*

# Cache dependencies
COPY Cargo.toml Cargo.lock ./
RUN mkdir src && echo "fn main() {}" > src/main.rs
RUN cargo build --release -j 2 && rm -rf src

# Build actual application
COPY . .
RUN touch src/main.rs && cargo build --release -j 2

# Runtime stage
FROM debian:bookworm-slim

RUN apt-get update && apt-get install -y \
    ca-certificates \
    libssl3 \
    && rm -rf /var/lib/apt/lists/*

RUN useradd -m -u 1000 appuser
USER appuser

COPY --from=builder /app/target/release/ai-engine /usr/local/bin/

EXPOSE 8080

CMD ["ai-engine"]
```

### Docker Compose (Development)

```yaml
# docker-compose.yml
version: '3.8'

services:
  api:
    build:
      context: ./services/api
      dockerfile: Dockerfile.dev
    ports:
      - "8080:8080"
    environment:
      - DATABASE_URL=postgres://jeeves:password@postgres:5432/jeeves
      - REDIS_URL=redis://redis:6379
      - LOG_LEVEL=debug
    volumes:
      - ./services/api:/app
    depends_on:
      - postgres
      - redis

  ai-engine:
    build:
      context: ./services/ai-engine
      dockerfile: Dockerfile.dev
    ports:
      - "8081:8080"
    environment:
      - RUST_LOG=debug
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    volumes:
      - ./services/ai-engine:/app
      - cargo-cache:/usr/local/cargo/registry

  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: jeeves
      POSTGRES_USER: jeeves
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./scripts/init-db.sql:/docker-entrypoint-initdb.d/init.sql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data

  nats:
    image: nats:2.10-alpine
    ports:
      - "4222:4222"
      - "8222:8222"  # Monitoring
    command: ["--jetstream", "--store_dir=/data"]
    volumes:
      - nats-data:/data

volumes:
  postgres-data:
  redis-data:
  nats-data:
  cargo-cache:
```

---

## Kubernetes Configuration

### Base Configuration

```yaml
# kubernetes/base/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: jeeves
  labels:
    app.kubernetes.io/name: jeeves
    pod-security.kubernetes.io/enforce: restricted
```

```yaml
# kubernetes/apps/api/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api
  namespace: jeeves
spec:
  replicas: 3
  selector:
    matchLabels:
      app: api
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: api
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "9090"
    spec:
      serviceAccountName: api
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
        seccompProfile:
          type: RuntimeDefault
      containers:
        - name: api
          image: jeeves/api:latest
          ports:
            - containerPort: 8080
              name: http
            - containerPort: 9090
              name: metrics
          env:
            - name: DATABASE_URL
              valueFrom:
                secretKeyRef:
                  name: database-credentials
                  key: url
            - name: REDIS_URL
              valueFrom:
                configMapKeyRef:
                  name: api-config
                  key: redis_url
          resources:
            requests:
              cpu: "100m"
              memory: "128Mi"
            limits:
              cpu: "500m"
              memory: "512Mi"
          securityContext:
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true
            capabilities:
              drop:
                - ALL
          livenessProbe:
            httpGet:
              path: /health/live
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /health/ready
              port: 8080
            initialDelaySeconds: 5
            periodSeconds: 5
          volumeMounts:
            - name: tmp
              mountPath: /tmp
      volumes:
        - name: tmp
          emptyDir: {}
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
            - weight: 100
              podAffinityTerm:
                labelSelector:
                  matchLabels:
                    app: api
                topologyKey: kubernetes.io/hostname
```

```yaml
# kubernetes/apps/api/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: api
  namespace: jeeves
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: api
  minReplicas: 3
  maxReplicas: 20
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
        - type: Pods
          value: 4
          periodSeconds: 60
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Percent
          value: 10
          periodSeconds: 60
```

---

## Monitoring & Observability

### Prometheus Configuration

```yaml
# monitoring/prometheus/rules.yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: jeeves-alerts
  namespace: monitoring
spec:
  groups:
    - name: jeeves.rules
      rules:
        - alert: HighErrorRate
          expr: |
            sum(rate(http_requests_total{job="api",status=~"5.."}[5m])) 
            / sum(rate(http_requests_total{job="api"}[5m])) > 0.01
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: "High error rate detected"
            description: "Error rate is {{ $value | humanizePercentage }}"
            
        - alert: HighLatency
          expr: |
            histogram_quantile(0.99, 
              sum(rate(http_request_duration_seconds_bucket{job="api"}[5m])) 
              by (le)) > 1
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "High latency detected"
            description: "P99 latency is {{ $value }}s"
            
        - alert: PodRestarting
          expr: |
            increase(kube_pod_container_status_restarts_total{namespace="jeeves"}[1h]) > 3
          labels:
            severity: warning
          annotations:
            summary: "Pod restarting frequently"
```

### Grafana Dashboard

```json
{
  "dashboard": {
    "title": "Jeeves Overview",
    "panels": [
      {
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total{job=\"api\"}[5m])) by (status)"
          }
        ]
      },
      {
        "title": "Latency P50/P95/P99",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.50, sum(rate(http_request_duration_seconds_bucket{job=\"api\"}[5m])) by (le))",
            "legendFormat": "P50"
          },
          {
            "expr": "histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket{job=\"api\"}[5m])) by (le))",
            "legendFormat": "P95"
          },
          {
            "expr": "histogram_quantile(0.99, sum(rate(http_request_duration_seconds_bucket{job=\"api\"}[5m])) by (le))",
            "legendFormat": "P99"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "singlestat",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total{job=\"api\",status=~\"5..\"}[5m])) / sum(rate(http_requests_total{job=\"api\"}[5m])) * 100"
          }
        ]
      }
    ]
  }
}
```

---

## Deployment Strategies

### Canary Deployment

```yaml
# kubernetes/apps/api/canary.yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: api
  namespace: jeeves
spec:
  replicas: 10
  strategy:
    canary:
      maxSurge: "20%"
      maxUnavailable: 0
      steps:
        - setWeight: 5
        - pause: {duration: 5m}
        - setWeight: 20
        - pause: {duration: 5m}
        - setWeight: 50
        - pause: {duration: 10m}
        - setWeight: 100
      analysis:
        templates:
          - templateName: success-rate
        startingStep: 2
        args:
          - name: service-name
            value: api
  selector:
    matchLabels:
      app: api
  template:
    # ... same as deployment
```

### Rollback Procedure

```bash
#!/bin/bash
# scripts/rollback.sh

set -e

NAMESPACE="jeeves"
DEPLOYMENT="api"
REVISION="${1:-1}"  # Default to previous revision

echo "Rolling back $DEPLOYMENT to revision $REVISION..."

# Check current status
kubectl rollout status deployment/$DEPLOYMENT -n $NAMESPACE

# Perform rollback
kubectl rollout undo deployment/$DEPLOYMENT -n $NAMESPACE --to-revision=$REVISION

# Wait for rollback to complete
kubectl rollout status deployment/$DEPLOYMENT -n $NAMESPACE --timeout=300s

echo "Rollback completed successfully"
```

---

## Local Development Setup

### Prerequisites

```bash
# Install required tools
brew install docker kubectl helm terraform

# Install kind for local Kubernetes
brew install kind

# Install development tools
brew install go rust node

# Rust tools
cargo install cargo-watch cargo-audit
```

### Local Kubernetes Setup

```bash
#!/bin/bash
# scripts/local-setup.sh

# Create kind cluster
kind create cluster --name jeeves --config=- <<EOF
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
    kubeadmConfigPatches:
      - |
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            node-labels: "ingress-ready=true"
    extraPortMappings:
      - containerPort: 80
        hostPort: 80
        protocol: TCP
      - containerPort: 443
        hostPort: 443
        protocol: TCP
  - role: worker
  - role: worker
EOF

# Install ingress controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

# Wait for ingress to be ready
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=90s

# Apply local configuration
kubectl apply -k kubernetes/overlays/local/
```

---

## Operational Runbooks

### Service Restart

```bash
#!/bin/bash
# Graceful restart of a service

SERVICE=$1
NAMESPACE="${2:-jeeves}"

echo "Restarting $SERVICE in $NAMESPACE..."

# Trigger rolling restart
kubectl rollout restart deployment/$SERVICE -n $NAMESPACE

# Monitor the rollout
kubectl rollout status deployment/$SERVICE -n $NAMESPACE --timeout=300s

echo "Restart complete"
```

### Database Migration

```bash
#!/bin/bash
# Run database migrations

ENV="${1:-staging}"

# Get database credentials
DB_URL=$(kubectl get secret database-credentials -n jeeves -o jsonpath='{.data.url}' | base64 -d)

# Run migrations
docker run --rm \
  -e DATABASE_URL="$DB_URL" \
  jeeves/migrator:latest \
  migrate up

echo "Migrations complete"
```

### Log Collection

```bash
#!/bin/bash
# Collect logs for debugging

SERVICE=$1
SINCE="${2:-1h}"
OUTPUT_DIR="./logs-$(date +%Y%m%d-%H%M%S)"

mkdir -p $OUTPUT_DIR

# Get pod names
PODS=$(kubectl get pods -n jeeves -l app=$SERVICE -o jsonpath='{.items[*].metadata.name}')

for POD in $PODS; do
  echo "Collecting logs from $POD..."
  kubectl logs -n jeeves $POD --since=$SINCE > "$OUTPUT_DIR/$POD.log"
done

# Create archive
tar -czf "$OUTPUT_DIR.tar.gz" $OUTPUT_DIR
rm -rf $OUTPUT_DIR

echo "Logs saved to $OUTPUT_DIR.tar.gz"
```

---

## Cost Optimization

### Resource Right-Sizing

```yaml
# Vertical Pod Autoscaler for right-sizing
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: api-vpa
  namespace: jeeves
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: api
  updatePolicy:
    updateMode: "Off"  # Just recommendations, manual apply
  resourcePolicy:
    containerPolicies:
      - containerName: api
        minAllowed:
          cpu: 50m
          memory: 64Mi
        maxAllowed:
          cpu: 2
          memory: 2Gi
```

### Spot/Preemptible Instances

```yaml
# Node pool for non-critical workloads
apiVersion: karpenter.sh/v1alpha5
kind: Provisioner
metadata:
  name: spot-provisioner
spec:
  requirements:
    - key: karpenter.sh/capacity-type
      operator: In
      values: ["spot"]
    - key: kubernetes.io/arch
      operator: In
      values: ["amd64"]
  limits:
    resources:
      cpu: 100
      memory: 200Gi
  ttlSecondsAfterEmpty: 30
```

---

*Document Owner: Principal Backend/Infrastructure Engineer*
*Last Updated: August 2025*
*Status: Living Document*
