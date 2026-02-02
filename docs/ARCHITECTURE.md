# Jeeves - Technical Architecture

## Overview

Jeeves is designed as a modern, cloud-native personal assistant with mobile-first architecture. The system prioritizes low latency, high availability, privacy, and seamless user experience.

---

## Architecture Principles

### 12-Factor App Compliance
1. **Codebase**: Single repo with monorepo structure
2. **Dependencies**: Explicitly declared, vendored where necessary
3. **Config**: Environment variables, never in code
4. **Backing Services**: Treated as attached resources
5. **Build, Release, Run**: Strictly separated stages
6. **Processes**: Stateless, share-nothing processes
7. **Port Binding**: Self-contained services
8. **Concurrency**: Scale via process model
9. **Disposability**: Fast startup, graceful shutdown
10. **Dev/Prod Parity**: Keep environments similar
11. **Logs**: Treat as event streams
12. **Admin Processes**: Run as one-off processes

### Design Principles
- **Mobile-First**: Optimize for mobile constraints
- **Offline-First**: Core features work without connectivity
- **Privacy-First**: Minimize data collection, encrypt everything
- **API-First**: All functionality exposed via APIs
- **Event-Driven**: Loose coupling via events

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENT LAYER                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │   iOS App    │  │ Android App  │  │   Web App    │  │  Watch Apps  │    │
│  │   (Swift)    │  │   (Kotlin)   │  │   (Next.js)  │  │              │    │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘    │
│           │                │                │                │              │
│           └────────────────┴────────────────┴────────────────┘              │
│                                    │                                         │
│                            ┌───────▼───────┐                                │
│                            │   API Gateway  │                                │
│                            │   (Kong/AWS)   │                                │
│                            └───────┬───────┘                                │
└────────────────────────────────────┼────────────────────────────────────────┘
                                     │
┌────────────────────────────────────┼────────────────────────────────────────┐
│                              SERVICE LAYER                                   │
├────────────────────────────────────┼────────────────────────────────────────┤
│                                    │                                         │
│  ┌─────────────────────────────────┼─────────────────────────────────────┐  │
│  │                          Load Balancer                                 │  │
│  └─────────────────────────────────┼─────────────────────────────────────┘  │
│                                    │                                         │
│  ┌──────────────┐  ┌──────────────┬┴─────────────┐  ┌──────────────────┐   │
│  │   Auth       │  │   Core API   │   AI Engine  │  │   Integrations   │   │
│  │   Service    │  │   (Go)       │   (Rust)     │  │   Service (Go)   │   │
│  │   (Go)       │  │              │              │  │                  │   │
│  └──────────────┘  └──────────────┴──────────────┘  └──────────────────┘   │
│         │                  │              │                   │             │
│  ┌──────┴──────────────────┴──────────────┴───────────────────┴─────────┐  │
│  │                          Message Queue (NATS)                         │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│         │                  │              │                   │             │
│  ┌──────┴──────┐  ┌───────┴──────┐  ┌────┴───────┐  ┌────────┴──────────┐  │
│  │  Scheduler  │  │   Notifier   │  │   Sync     │  │   Analytics       │  │
│  │   (Rust)    │  │   (Go)       │  │   (Rust)   │  │   (Go)            │  │
│  └─────────────┘  └──────────────┘  └────────────┘  └───────────────────┘  │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
                                     │
┌────────────────────────────────────┼────────────────────────────────────────┐
│                              DATA LAYER                                      │
├────────────────────────────────────┼────────────────────────────────────────┤
│                                    │                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐ │
│  │  PostgreSQL  │  │    Redis     │  │   Vector DB  │  │   Object Store   │ │
│  │  (Primary)   │  │   (Cache)    │  │  (Pinecone)  │  │      (S3)        │ │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────────┘ │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
                                     │
┌────────────────────────────────────┼────────────────────────────────────────┐
│                           EXTERNAL SERVICES                                  │
├────────────────────────────────────┼────────────────────────────────────────┤
│                                    │                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐ │
│  │   LLM API    │  │  Speech API  │  │  Calendar    │  │   Push (APNs/    │ │
│  │  (OpenAI/    │  │  (Whisper/   │  │  (Google/    │  │     FCM)         │ │
│  │   Claude)    │  │   TTS)       │  │   Apple)     │  │                  │ │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────────┘ │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Service Descriptions

### Auth Service (Go)
**Purpose**: Handle all authentication and authorization

**Responsibilities**:
- User registration and login
- OAuth 2.0 / OIDC implementation
- Token management (JWT)
- Session management
- MFA implementation
- API key management

**Tech Stack**:
- Go 1.22+
- Gin framework
- PostgreSQL (user data)
- Redis (sessions, rate limiting)

### Core API Service (Go)
**Purpose**: Main API gateway for client applications

**Responsibilities**:
- RESTful API for all client operations
- Request validation and transformation
- Rate limiting and throttling
- Request routing to appropriate services
- Response aggregation

**Tech Stack**:
- Go 1.22+
- Echo framework
- OpenAPI 3.0 spec
- gRPC for inter-service communication

### AI Engine (Rust)
**Purpose**: Process natural language and generate intelligent responses

**Responsibilities**:
- Intent recognition and entity extraction
- Context management
- LLM orchestration and prompt engineering
- Response generation
- Personalization engine
- Knowledge retrieval (RAG)

**Tech Stack**:
- Rust 2024 edition
- Axum framework
- tokio async runtime
- LLM APIs (OpenAI, Anthropic)
- Vector database for embeddings

### Integrations Service (Go)
**Purpose**: Connect with third-party services

**Responsibilities**:
- Calendar integration (Google, Apple, Outlook)
- Email integration
- Smart home integration
- Third-party API management
- OAuth flow for external services
- Webhook handling

**Tech Stack**:
- Go 1.22+
- Gin framework
- OAuth2 client libraries
- Webhook processing

### Scheduler Service (Rust)
**Purpose**: Handle all time-based operations

**Responsibilities**:
- Task scheduling and reminders
- Recurring event management
- Time-zone handling
- Proactive notification triggers
- Daily briefing generation

**Tech Stack**:
- Rust 2024 edition
- Tokio-cron for scheduling
- PostgreSQL (schedules)
- NATS (event publishing)

### Notifier Service (Go)
**Purpose**: Manage all outbound notifications

**Responsibilities**:
- Push notification delivery (APNs, FCM)
- Email notifications
- SMS notifications (optional)
- Notification preferences
- Delivery tracking

**Tech Stack**:
- Go 1.22+
- APNs/FCM SDKs
- Redis (rate limiting)
- PostgreSQL (history)

### Sync Service (Rust)
**Purpose**: Handle data synchronization between devices

**Responsibilities**:
- Conflict resolution
- Offline sync queue management
- Real-time sync via WebSocket
- Data versioning

**Tech Stack**:
- Rust 2024 edition
- Axum + WebSocket
- CRDTs for conflict resolution
- PostgreSQL (sync state)

### Analytics Service (Go)
**Purpose**: Collect and process analytics data

**Responsibilities**:
- Event ingestion
- Real-time metrics aggregation
- Usage analytics
- Feature flag evaluation
- A/B test tracking

**Tech Stack**:
- Go 1.22+
- ClickHouse (analytics DB)
- Redis Streams (event ingestion)
- Prometheus metrics

---

## Mobile Architecture

### iOS App (Swift)
```
┌─────────────────────────────────────────────────────────────────┐
│                        Presentation Layer                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │   SwiftUI   │  │   UIKit     │  │   WidgetKit / Intents   │  │
│  │   Views     │  │   Legacy    │  │   Extensions            │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                        Domain Layer                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │  ViewModels │  │  Use Cases  │  │   Domain Models         │  │
│  │  (MVVM)     │  │             │  │                         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                        Data Layer                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ Repositories│  │  Network    │  │   Local Storage         │  │
│  │             │  │  (URLSession│  │   (Core Data / SQLite)  │  │
│  │             │  │   + Combine)│  │                         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                        Core Layer                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │  Speech     │  │  Keychain   │  │   Analytics / Logging   │  │
│  │  Framework  │  │  Services   │  │                         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Android App (Kotlin)
```
┌─────────────────────────────────────────────────────────────────┐
│                        Presentation Layer                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │   Jetpack   │  │   Compose   │  │   Widgets / Assistant   │  │
│  │   Compose   │  │   Navigation│  │   Integration           │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                        Domain Layer                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │  ViewModels │  │  Use Cases  │  │   Domain Models         │  │
│  │  (MVVM)     │  │  (Coroutines│  │                         │  │
│  │             │  │   + Flow)   │  │                         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                        Data Layer                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ Repositories│  │  Network    │  │   Local Storage         │  │
│  │             │  │  (Retrofit  │  │   (Room + DataStore)    │  │
│  │             │  │   + OkHttp) │  │                         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                        Core Layer                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │  Speech     │  │  Encrypted  │  │   Analytics / Logging   │  │
│  │  Recognition│  │  SharedPrefs│  │   (Timber + Sentry)     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Data Architecture

### Primary Database (PostgreSQL)

```sql
-- Core user data
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    preferences JSONB DEFAULT '{}'
);

-- Tasks
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    title TEXT NOT NULL,
    description TEXT,
    due_date TIMESTAMPTZ,
    priority INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'pending',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    sync_version BIGINT DEFAULT 0
);

-- Conversations
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    started_at TIMESTAMPTZ DEFAULT NOW(),
    ended_at TIMESTAMPTZ,
    context JSONB DEFAULT '{}'
);

-- Messages
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID REFERENCES conversations(id),
    role VARCHAR(50) NOT NULL, -- user, assistant, system
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'
);
```

### Caching Strategy (Redis)

```
# Session cache
session:{user_id} -> JWT claims (TTL: 1h)

# Rate limiting
ratelimit:{user_id}:{endpoint} -> count (TTL: 1m)

# User preferences cache
preferences:{user_id} -> JSONB (TTL: 15m)

# Active conversation context
context:{user_id} -> conversation state (TTL: 30m)

# Feature flags
flags:{user_id} -> evaluated flags (TTL: 5m)
```

### Vector Database (Pinecone/pgvector)

Used for:
- Semantic search over user's notes and tasks
- RAG for personalized responses
- Similar task/event suggestions
- Knowledge base retrieval

---

## Security Architecture

### Defense in Depth

```
┌─────────────────────────────────────────────────────────────────┐
│                          WAF / DDoS Protection                   │
├─────────────────────────────────────────────────────────────────┤
│                          API Gateway                             │
│                    (Rate Limiting, Auth)                         │
├─────────────────────────────────────────────────────────────────┤
│                          Service Mesh                            │
│                    (mTLS, Traffic Control)                       │
├─────────────────────────────────────────────────────────────────┤
│                    Application Security                          │
│              (Input Validation, OWASP Controls)                  │
├─────────────────────────────────────────────────────────────────┤
│                       Data Security                              │
│              (Encryption at Rest, Field-level)                   │
└─────────────────────────────────────────────────────────────────┘
```

### Encryption

| Data Type | At Rest | In Transit | Method |
|-----------|---------|------------|--------|
| User credentials | AES-256-GCM | TLS 1.3 | Argon2id hashing |
| Personal data | AES-256-GCM | TLS 1.3 | Field-level encryption |
| Conversation history | AES-256-GCM | TLS 1.3 | User-key encryption |
| API keys | AES-256-GCM | TLS 1.3 | HSM-backed |

---

## Infrastructure

### Kubernetes Deployment

```yaml
# Example service deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-engine
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    spec:
      containers:
      - name: ai-engine
        image: jeeves/ai-engine:latest
        resources:
          requests:
            cpu: "500m"
            memory: "512Mi"
          limits:
            cpu: "2000m"
            memory: "2Gi"
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
        readinessProbe:
          httpGet:
            path: /ready
            port: 8080
```

### Observability Stack

- **Metrics**: Prometheus + Grafana
- **Logging**: Loki + Grafana
- **Tracing**: Jaeger / OpenTelemetry
- **Alerting**: Alertmanager + PagerDuty

---

## API Design

### RESTful Endpoints

```
# Authentication
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout

# User
GET    /api/v1/user/profile
PATCH  /api/v1/user/profile
GET    /api/v1/user/preferences
PUT    /api/v1/user/preferences

# Tasks
GET    /api/v1/tasks
POST   /api/v1/tasks
GET    /api/v1/tasks/{id}
PATCH  /api/v1/tasks/{id}
DELETE /api/v1/tasks/{id}

# Conversations
POST   /api/v1/conversations
GET    /api/v1/conversations/{id}
POST   /api/v1/conversations/{id}/messages
GET    /api/v1/conversations/{id}/messages

# Assistant
POST   /api/v1/assistant/query
POST   /api/v1/assistant/voice
GET    /api/v1/assistant/suggestions

# Sync
POST   /api/v1/sync/push
POST   /api/v1/sync/pull
```

### WebSocket Protocol

```json
// Client -> Server
{
  "type": "query",
  "id": "uuid",
  "payload": {
    "text": "What's on my schedule today?"
  }
}

// Server -> Client (streaming response)
{
  "type": "response",
  "id": "uuid",
  "payload": {
    "text": "You have 3 meetings...",
    "complete": false
  }
}
```

---

## Performance Targets

| Metric | Target | Measurement |
|--------|--------|-------------|
| API p50 latency | < 100ms | Prometheus |
| API p99 latency | < 500ms | Prometheus |
| AI query latency | < 2s | Prometheus |
| App cold start | < 2s | Firebase/Crashlytics |
| Database query | < 50ms | pg_stat_statements |
| Availability | 99.9% | Uptime monitoring |

---

*Document Owner: Principal Backend/Infrastructure Engineer*
*Last Updated: August 2025*
*Status: Living Document*
