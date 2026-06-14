# FarmRakshak — Product Requirements Document (PRD)

> **Version:** 1.0  
> **Date:** 2026-03-26  
> **Status:** Draft  
> **Author:** Engineering Team

---

## 1. Executive Summary

FarmRakshak is an AI-powered agriculture assistance platform that empowers Indian farmers to detect crop diseases via image recognition, receive personalized treatment recommendations, weather alerts, market-aware advisory, and multilingual content — all through a mobile-first API and SEO-optimized web presence.

---

## 2. Target Users & Roles

### 2.1 User Personas

| Persona | Description | Priority |
|---------|-------------|----------|
| **Farmer** | Smallholder/marginal farmer using a smartphone; may be semi-literate. Primary consumer of all features. | P0 |
| **Agriculture Expert** | Domain specialist who reviews AI results, adds advisories, and moderates content. | P1 |
| **Admin Operator** | Internal team member managing users, content, analytics, and system health. | P1 |
| **Government Agency** | Future consumer of aggregated disease/crop data dashboards. | P2 |
| **Agri Company** | Future partner providing market prices or sponsored advisories. | P2 |

### 2.2 Role-Permission Matrix

| Action | Farmer | Expert | Admin |
|--------|--------|--------|-------|
| Register / Login | ✅ | ✅ | ✅ |
| Upload crop image | ✅ | ❌ | ❌ |
| View disease result | ✅ | ✅ | ✅ |
| View treatment suggestions | ✅ | ✅ | ✅ |
| View advisory | ✅ | ✅ | ✅ |
| View blog | ✅ | ✅ | ✅ |
| Change language | ✅ | ✅ | ✅ |
| View upload history | ✅ | ❌ | ✅ |
| Receive alerts | ✅ | ✅ | ✅ |
| Review disease reports | ❌ | ✅ | ✅ |
| Add advisory content | ❌ | ✅ | ✅ |
| Suggest treatments | ❌ | ✅ | ✅ |
| Moderate reports | ❌ | ✅ | ✅ |
| Manage users | ❌ | ❌ | ✅ |
| Manage blogs | ❌ | ❌ | ✅ |
| Manage crop/disease data | ❌ | ❌ | ✅ |
| View analytics | ❌ | ❌ | ✅ |
| System monitoring | ❌ | ❌ | ✅ |

---

## 3. Complete Feature List

### F1 — User Management

**Registration channels:**
- Mobile number (OTP-ready architecture; OTP provider pluggable)
- Email + password

**Authentication:**
- JWT access token (short-lived, 15 min)
- JWT refresh token (long-lived, 7 days, stored server-side hash)
- Token revocation on logout
- BCrypt password hashing (strength 12)

**Profile fields:**
| Field | Type | Required |
|-------|------|----------|
| `name` | String | Yes |
| `mobile` | String | Yes |
| `email` | String | No |
| `village` | String | No |
| `district` | String | No |
| `state` | Enum (Indian states) | Yes |
| `primaryCrops` | List\<String\> | No |
| `languagePreference` | Enum | Yes (default `EN`) |
| `profileImageUrl` | String | No |

### F2 — Crop Disease Detection

**Workflow:**

```
Farmer → Upload Image → Validate → Store in MinIO → Publish Kafka Event
    → AI Service Analyzes → Store Result → Notify Farmer
```

**Response payload:**
```json
{
  "analysisId": "uuid",
  "status": "COMPLETED | PROCESSING | FAILED",
  "diseaseName": "Late Blight",
  "confidence": 0.94,
  "description": "Fungal disease caused by Phytophthora infestans...",
  "treatment": ["Apply Mancozeb 75% WP @ 2g/L", "..."],
  "prevention": ["Use resistant varieties", "..."],
  "imageUrl": "s3://crop-images/userId/2026-03-26/img.jpg",
  "analyzedAt": "2026-03-26T10:00:00Z"
}
```

**Processing rules:**
- Async processing via Kafka event `crop-analysis-topic`
- AI service timeout: 30 seconds
- Retry policy: 3 attempts with exponential backoff (2s, 4s, 8s)
- Fallback: return `PROCESSING` status, queue for retry

### F3 — Image Storage (MinIO)

**Bucket structure:**
```
crop-images/
  └── {userId}/
      └── {yyyy-MM-dd}/
          └── {uuid}_{originalFilename}.{ext}
```

**Validation rules:**
| Rule | Value |
|------|-------|
| Max file size | 10 MB |
| Allowed MIME types | `image/jpeg`, `image/png`, `image/webp` |
| Magic-byte validation | Yes (not just extension check) |
| Executable rejection | Yes |

**Storage config:**
- Pre-signed URL generation for reads (1-hour expiry)
- Auto-bucket creation on startup

### F4 — AI Service (Python FastAPI)

**Endpoint:**
```
POST /api/v1/analyze
Content-Type: application/json

{ "imageUrl": "https://minio:9000/crop-images/..." }
```

**Response:**
```json
{
  "disease": "Powdery Mildew",
  "confidence": 0.87,
  "description": "White powdery coating on leaves...",
  "treatment": ["Apply Sulfur dust", "Neem oil spray"],
  "prevention": ["Ensure air circulation", "Avoid overhead watering"]
}
```

**Backend integration rules:**
- Circuit breaker pattern (Resilience4j)
- Timeout: 30s
- Retry: 3× with exponential backoff
- Fallback: return `{"status": "PROCESSING", "message": "Analysis queued"}`

### F5 — Weather Service

**Data provided:**
- Current temperature (°C)
- Humidity (%)
- Rain forecast (next 24h)
- Weather alerts (heavy rain, frost, heatwave)

**Caching:**
- Redis cache per `(latitude, longitude)` key
- TTL: 3 hours
- Cache-aside pattern: check Redis → miss → call external API → store in Redis

**External API:** OpenWeatherMap (pluggable via interface)

### F6 — Crop Advisory

**Advisory types:**
- Fertilizer suggestions (crop + season specific)
- Seasonal farming advice
- Pest management best practices
- Irrigation guidelines

**Management:**
- Admin/Expert creates advisories via REST API
- Tagged by: crop, season, region, language
- Soft-deletable

### F7 — Blog CMS

**Blog entity fields:**
| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | PK |
| `title` | String | Max 200 chars |
| `slug` | String | Unique, URL-safe |
| `content` | Text | Markdown supported |
| `excerpt` | String | Max 300 chars |
| `coverImageUrl` | String | |
| `seoTitle` | String | Max 60 chars |
| `seoDescription` | String | Max 160 chars |
| `tags` | List\<String\> | |
| `authorId` | UUID | FK → users |
| `status` | Enum | `DRAFT`, `PUBLISHED`, `ARCHIVED` |
| `publishedAt` | Timestamp | |
| `createdAt` | Timestamp | |
| `updatedAt` | Timestamp | |

**Features:**
- Slug auto-generation from title
- Tag-based filtering
- Pagination (default 10, max 50)
- Full-text search (PostgreSQL `tsvector`)

### F8 — Multi-Language Support

**Supported languages (Phase 1):**

| Code | Language |
|------|----------|
| `en` | English |
| `hi` | Hindi |
| `te` | Telugu |
| `mr` | Marathi |
| `kn` | Kannada |

**Future:** Tamil (`ta`), Bengali (`bn`), Gujarati (`gu`)

**Architecture:**
- Translation key-value table: `translations(key, locale, value)`
- API returns `Accept-Language` aware responses
- No hardcoded UI text in backend responses
- Client-side i18n bundle endpoint: `GET /api/v1/i18n/{locale}`

### F9 — Notifications

**Notification triggers:**
| Event | Channel | Priority |
|-------|---------|----------|
| Disease analysis complete | In-app, Push | High |
| Weather alert | In-app, Push | High |
| New advisory published | In-app | Medium |
| New blog published | In-app | Low |

**Architecture:**
- Kafka topic: `notification-topic`
- Notification service consumes events
- Stores notifications in DB
- Marks read/unread
- Push notification integration ready (FCM interface)

### F10 — History Tracking

**User can view:**
- All past image uploads (paginated)
- Disease analysis results per upload
- Dates, status, confidence scores
- Thumbnail image URLs (pre-signed)

**API:** `GET /api/v1/users/{userId}/history?page=0&size=10`

### F11 — Admin Dashboard

**Capabilities:**
| Feature | Description |
|---------|-------------|
| User management | List, search, activate/deactivate users |
| Upload analytics | Total uploads, per-day chart data |
| AI usage | Analysis count, avg confidence, failure rate |
| Disease reports | Most detected diseases, regional breakdown |
| Blog management | CRUD operations |
| System health | Service status, uptime (via Actuator) |

---

## 4. Functional Requirements

### FR-01: User Registration
- System SHALL accept registration via mobile or email
- System SHALL validate uniqueness of mobile/email
- System SHALL hash passwords before storage
- System SHALL return JWT on successful registration

### FR-02: User Authentication
- System SHALL issue access + refresh token pair on login
- System SHALL reject expired/revoked tokens
- System SHALL support token refresh without re-login

### FR-03: Image Upload & Analysis
- System SHALL validate image file type and size before storage
- System SHALL store images in MinIO with structured paths
- System SHALL publish Kafka event on successful upload
- System SHALL invoke AI service asynchronously
- System SHALL store analysis results and notify user

### FR-04: Weather Data
- System SHALL return weather data for user's location
- System SHALL cache weather data in Redis with 3-hour TTL
- System SHALL generate alerts for extreme weather conditions

### FR-05: Advisory
- System SHALL allow Admin/Expert to create crop advisories
- System SHALL filter advisories by crop, season, region, language

### FR-06: Blog
- System SHALL support full CRUD for blog posts
- System SHALL auto-generate SEO-friendly slugs
- System SHALL support pagination and tag-based filtering

### FR-07: Notifications
- System SHALL generate notifications for disease results, weather alerts, and new advisories
- System SHALL support read/unread status per user

### FR-08: Multi-Language
- System SHALL serve all translatable content based on `Accept-Language` header
- System SHALL provide a i18n key-value endpoint per locale

### FR-09: History
- System SHALL persist all uploads and analysis results
- System SHALL return paginated history per user

### FR-10: Admin Operations
- System SHALL restrict admin APIs to `ROLE_ADMIN`
- System SHALL provide aggregate analytics endpoints

---

## 5. Non-Functional Requirements

| Category | Requirement | Target |
|----------|-------------|--------|
| **Performance** | API response time (p95) | < 500ms |
| **Performance** | Image upload + AI analysis (async) | < 60s end-to-end |
| **Scalability** | Concurrent users | 10,000+ |
| **Scalability** | Horizontal scaling | Stateless services behind LB |
| **Availability** | Uptime SLA | 99.5% |
| **Security** | Authentication | JWT (RS256 preferred) |
| **Security** | Data at rest | AES-256 (DB-level) |
| **Security** | Data in transit | TLS 1.2+ |
| **Security** | Password storage | BCrypt (strength 12) |
| **Reliability** | AI service failure | Graceful fallback + retry |
| **Reliability** | Message delivery | At-least-once (Kafka) |
| **Observability** | Logging | Structured JSON, correlation ID |
| **Observability** | Health checks | Spring Actuator `/health` |
| **Maintainability** | Code coverage target | 70%+ |
| **Portability** | Containerization | Docker |
| **Portability** | Cloud agnostic | S3-compatible, standard PG/Redis/Kafka |

---

## 6. Architecture Decisions

### AD-01: Microservices over Monolith
**Decision:** Decompose into 11+ independently deployable services.  
**Rationale:** Team scalability, independent deployments, fault isolation, technology heterogeneity (Java + Python).

### AD-02: API Gateway Pattern
**Decision:** Spring Cloud Gateway as single entry point.  
**Rationale:** Centralized auth validation, rate limiting, routing, and CORS handling.

### AD-03: Service Discovery
**Decision:** Netflix Eureka for service registration and discovery.  
**Rationale:** Dynamic service resolution without hardcoded URLs; well-supported in Spring Cloud.

### AD-04: Event-Driven Architecture for Image Analysis
**Decision:** Kafka for async image analysis pipeline.  
**Rationale:** Decouples upload from AI processing; enables retry, audit, and replay.

### AD-05: S3-Compatible Object Storage
**Decision:** MinIO for image storage.  
**Rationale:** S3-compatible API enables future migration to AWS S3/GCS with zero code change.

### AD-06: Redis for Caching & Session
**Decision:** Redis for weather cache, rate limiting, and OTP storage.  
**Rationale:** Sub-millisecond reads, TTL support, widely supported.

### AD-07: PostgreSQL as Primary Database
**Decision:** Each service owns its database schema (database-per-service).  
**Rationale:** Data ownership, independent evolution, no cross-service joins.

### AD-08: Flyway for Schema Migrations
**Decision:** Flyway for versioned, repeatable database migrations.  
**Rationale:** Auditable schema history, rollback support, CI/CD friendly.

### AD-09: Python FastAPI for AI Service
**Decision:** Separate Python service for ML inference.  
**Rationale:** Python ML ecosystem (PyTorch/TensorFlow), independent scaling, team specialization.

### AD-10: Next.js for SEO Website
**Decision:** Next.js for the public-facing website.  
**Rationale:** SSR/SSG for SEO, React ecosystem, image optimization built-in.

---

## 7. System Architecture

### 7.1 High-Level Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                          CLIENTS                                     │
│         Mobile App (Flutter/RN)    │    Next.js Website               │
└──────────────────┬─────────────────┴──────────────┬──────────────────┘
                   │            HTTPS                │
                   ▼                                 ▼
         ┌─────────────────────────────────────────────────┐
         │              GATEWAY SERVICE                     │
         │   (Spring Cloud Gateway)                        │
         │   • JWT validation filter                       │
         │   • Rate limiting (Redis-backed)                │
         │   • Route definitions                           │
         │   • CORS configuration                          │
         └───────┬─────────────────────────────────────────┘
                 │            Service Discovery (Eureka)
        ┌────────┼────────┬──────────┬──────────┬──────────┐
        ▼        ▼        ▼          ▼          ▼          ▼
   ┌────────┐┌────────┐┌────────┐┌────────┐┌────────┐┌────────┐
   │ Auth   ││ User   ││ Crop   ││Weather ││Advisory││ Blog   │
   │Service ││Service ││Service ││Service ││Service ││Service │
   └───┬────┘└───┬────┘└───┬────┘└───┬────┘└───┬────┘└───┬────┘
       │         │         │         │         │         │
       ▼         ▼         ▼         ▼         ▼         ▼
   ┌──────────────────────────────────────────────────────────┐
   │                    PostgreSQL                             │
   │   auth_db │ user_db │ crop_db │ advisory_db │ blog_db    │
   └──────────────────────────────────────────────────────────┘

   Crop Service ──► MinIO (S3)    Weather Service ──► Redis
       │                                                │
       ▼                                                ▼
   ┌──────────┐                                 ┌──────────────┐
   │  Kafka   │────────────────────────────────►│ Notification │
   │ Broker   │                                 │   Service    │
   └──────────┘                                 └──────────────┘
       │
       ▼
   ┌──────────────┐
   │  AI Service  │  (Python FastAPI)
   │  /analyze    │
   └──────────────┘
```

### 7.2 Service Registry

| Service | Port | Database | Dependencies |
|---------|------|----------|--------------|
| `eureka-server` | 8761 | — | — |
| `gateway-service` | 8080 | — | Eureka, Redis |
| `auth-service` | 8081 | `auth_db` | Eureka, Redis |
| `user-service` | 8082 | `user_db` | Eureka |
| `crop-service` | 8083 | `crop_db` | Eureka, MinIO, Kafka |
| `ai-client-service` | 8084 | — | Eureka, AI Service |
| `weather-service` | 8085 | — | Eureka, Redis |
| `advisory-service` | 8086 | `advisory_db` | Eureka |
| `notification-service` | 8087 | `notification_db` | Eureka, Kafka |
| `blog-service` | 8088 | `blog_db` | Eureka |
| `admin-service` | 8089 | — | Eureka, all service APIs |
| `ai-service` (Python) | 8090 | — | ML model files |

---

## 8. Data Flow Diagrams

### 8.1 Image Upload & Disease Detection Flow

```
Farmer                Gateway        Crop Service      MinIO       Kafka       AI Client       AI Service      Notification
  │                     │               │                │           │              │               │               │
  │── POST /upload ────►│               │                │           │              │               │               │
  │                     │──route──────►│                │           │              │               │               │
  │                     │               │──validate──────│           │              │               │               │
  │                     │               │──store image──►│           │              │               │               │
  │                     │               │◄──imageUrl─────│           │              │               │               │
  │                     │               │──save record───│           │              │               │               │
  │                     │               │──publish──────►│           │              │               │               │
  │                     │               │                │           │              │               │               │
  │◄── 202 Accepted ───│◄──────────────│                │           │              │               │               │
  │                     │               │                │           │              │               │               │
  │                     │               │                │   ┌──consume──┐          │               │               │
  │                     │               │                │   │AI Client  │──POST───►│               │               │
  │                     │               │                │   │Service    │◄─result──│               │               │
  │                     │               │                │   │           │          │               │               │
  │                     │               │◄──update result│   └──────────┘          │               │               │
  │                     │               │                │   │──publish─────────────────────────────────────────────►│
  │                     │               │                │           │              │               │               │
  │◄── notification ────────────────────────────────────────────────────────────────────────────────────────────────│
```

### 8.2 Weather Data Flow

```
Farmer ──► Gateway ──► Weather Service ──► Redis (check cache)
                                              │
                                     miss ────┤──── hit → return cached
                                              │
                                              ▼
                                     External Weather API
                                              │
                                              ▼
                                     Store in Redis (TTL 3h)
                                              │
                                              ▼
                                     Return to farmer
```

---

## 9. Service Responsibilities

| Service | Single Responsibility |
|---------|----------------------|
| `gateway-service` | HTTP entry point, auth filter, rate limiting, routing |
| `eureka-server` | Service registration and discovery |
| `auth-service` | Authentication, token issuance/validation, role management |
| `user-service` | User profile CRUD, farmer data |
| `crop-service` | Image upload, storage orchestration, analysis record management |
| `ai-client-service` | Bridge between Java backend and Python AI service; retry/circuit-breaker |
| `weather-service` | Weather data retrieval, caching, alert generation |
| `advisory-service` | Advisory CRUD, crop/season/region filtering |
| `notification-service` | Kafka consumer, notification storage, delivery |
| `blog-service` | Blog CMS, slug generation, full-text search |
| `admin-service` | Aggregated admin APIs, analytics, user management proxy |
| `ai-service` | ML model inference, disease classification |

---

## 10. Failure Scenarios & Mitigation

| Scenario | Impact | Mitigation |
|----------|--------|------------|
| **AI Service down** | Disease detection fails | Return `PROCESSING` status; retry via Kafka; circuit breaker opens after 5 failures |
| **MinIO down** | Image upload fails | Return 503; store retry event in DB; alert ops team |
| **Kafka broker down** | Events not published | Retry publish with backoff; fallback to DB event table (outbox pattern) |
| **Redis down** | Cache miss, rate limiting disabled | Fallback to DB/direct API calls; log warning |
| **PostgreSQL down** | Full service outage | Connection pool retry; read replicas for reads; PgBouncer |
| **External Weather API down** | Stale weather data | Serve from Redis cache (even if expired); log warning |
| **Gateway overloaded** | All traffic blocked | Rate limiting; horizontal scaling; circuit breaker |
| **Token theft** | Unauthorized access | Short-lived tokens (15 min); refresh token rotation; token blacklist in Redis |

---

## 11. Security Requirements

### 11.1 Authentication & Authorization
- JWT-based stateless authentication
- Access token: 15 min TTL, signed with RS256
- Refresh token: 7 days, rotated on use
- Role-based access control: `FARMER`, `EXPERT`, `ADMIN`
- Method-level security via `@PreAuthorize`

### 11.2 Input Validation
- Bean Validation (JSR 380) on all DTOs
- SQL injection prevention via parameterized queries (JPA)
- XSS prevention via output encoding
- CORS restricted to known origins

### 11.3 File Security
- Validate MIME type via magic bytes (Apache Tika)
- Reject files > 10 MB
- Reject executable content types
- Store with randomized filenames (UUID prefix)

### 11.4 Rate Limiting
- Redis-backed token bucket per API key/IP
- Default: 100 requests/minute per user
- Upload endpoint: 10 requests/minute per user

### 11.5 Secrets Management
- No secrets in source code
- Environment variables for all credentials
- `.env.template` with placeholder values

---

## 12. Scaling Strategy

### 12.1 Horizontal Scaling
- All services are stateless → scale via container replicas
- Gateway: 2+ instances behind load balancer
- Crop service: scale based on upload volume
- AI service: GPU-aware scaling (future)

### 12.2 Database Scaling
- Connection pooling via HikariCP
- Read replicas for read-heavy services (blog, advisory)
- Database-per-service for independent scaling

### 12.3 Kafka Scaling
- Partition topics by `userId` for ordered processing
- Consumer groups per service for parallel consumption
- Topic partitions: min 3 per topic

### 12.4 Caching Strategy
- Weather: Redis, 3h TTL
- User tokens: Redis, 15m/7d TTL
- Blog list: Redis, 10m TTL
- Advisory list: Redis, 1h TTL

---

## 13. API Standards

### 13.1 URL Convention
```
/api/v1/{resource}
/api/v1/{resource}/{id}
/api/v1/{resource}/{id}/{sub-resource}
```

### 13.2 Standard Response Envelope
```json
{
  "success": true,
  "data": { ... },
  "message": "Operation completed successfully",
  "timestamp": "2026-03-26T10:00:00Z",
  "traceId": "abc-123-def"
}
```

### 13.3 Error Response
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input",
    "details": [
      { "field": "email", "message": "must be a valid email" }
    ]
  },
  "timestamp": "2026-03-26T10:00:00Z",
  "traceId": "abc-123-def"
}
```

### 13.4 Pagination
```json
{
  "success": true,
  "data": { "content": [...], "page": 0, "size": 10, "totalElements": 100, "totalPages": 10 },
  "timestamp": "..."
}
```

### 13.5 HTTP Status Codes
| Code | Usage |
|------|-------|
| 200 | Success |
| 201 | Created |
| 202 | Accepted (async) |
| 400 | Validation error |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not found |
| 409 | Conflict (duplicate) |
| 429 | Rate limited |
| 500 | Internal error |
| 503 | Service unavailable |

### 13.6 Versioning
- URL-based: `/api/v1/...`
- No breaking changes within same version

---

## 14. Kafka Event Architecture

### 14.1 Topics

| Topic | Producer | Consumer | Payload |
|-------|----------|----------|---------|
| `crop-analysis-topic` | `crop-service` | `ai-client-service` | `{analysisId, imageUrl, userId}` |
| `analysis-result-topic` | `ai-client-service` | `crop-service`, `notification-service` | `{analysisId, result, status}` |
| `notification-topic` | Multiple | `notification-service` | `{userId, type, title, body}` |
| `audit-topic` | All services | Audit consumer (future) | `{action, userId, service, timestamp}` |

### 14.2 Event Schema Standard
```json
{
  "eventId": "uuid",
  "eventType": "CROP_ANALYSIS_REQUESTED",
  "timestamp": "ISO-8601",
  "source": "crop-service",
  "payload": { ... }
}
```

---

## 15. Logging Standard

### 15.1 Log Format
```
{
  "timestamp": "2026-03-26T10:00:00.000Z",
  "level": "INFO",
  "service": "crop-service",
  "traceId": "abc-123",
  "spanId": "def-456",
  "message": "Image uploaded successfully",
  "userId": "user-uuid",
  "metadata": { ... }
}
```

### 15.2 Log Levels
| Level | Usage |
|-------|-------|
| `ERROR` | Unrecoverable failures, exceptions |
| `WARN` | Recoverable issues, fallbacks triggered |
| `INFO` | Business events (upload, login, analysis) |
| `DEBUG` | Technical details (SQL, HTTP calls) |

---

## 16. Database Design Principles

- **UUID** primary keys (never auto-increment for distributed systems)
- **`created_at`**, **`updated_at`** on every table (auto-managed)
- **Soft delete** via `deleted_at` timestamp (nullable; non-null = deleted)
- **Indexes** on all foreign keys, frequently queried columns, and unique constraints
- **Flyway** versioned migrations: `V{version}__{description}.sql`
- **Database-per-service**: each service owns its schema exclusively

---

## 17. Observability

| Concern | Tool |
|---------|------|
| Structured logging | Logback + JSON encoder |
| Correlation ID | MDC filter on gateway, propagated via headers |
| Health checks | Spring Boot Actuator `/actuator/health` |
| Metrics (future) | Micrometer + Prometheus |
| Tracing (future) | OpenTelemetry + Jaeger |

---

## 18. Frontend Architecture

### 18.1 Tech Stack

| Technology | Purpose |
|------------|---------|
| Next.js 14 | Framework (App Router, SSR/SSG) |
| TypeScript | Type safety |
| Tailwind CSS | Utility-first styling |
| ShadCN UI | Component library |
| React Query (TanStack) | Server state management |
| Axios | HTTP client |
| React Hook Form | Form handling |
| Zod | Schema validation |
| React Context | Auth state |

### 18.2 Frontend Applications

#### A. Public Website (SSR/SSG — SEO optimized)

| Page | Route | Rendering |
|------|-------|-----------|
| Home | `/` | SSG |
| Features | `/features` | SSG |
| How It Works | `/how-it-works` | SSG |
| About | `/about` | SSG |
| Contact | `/contact` | SSG |
| Blog Listing | `/blog` | ISR |
| Blog Detail | `/blog/[slug]` | ISR |
| FAQ | `/faq` | SSG |
| Privacy Policy | `/privacy` | SSG |
| Terms | `/terms` | SSG |

#### B. Product Dashboard (CSR — authenticated)

| Page | Route |
|------|-------|
| Login | `/login` |
| Register | `/register` |
| Dashboard | `/dashboard` |
| Upload Crop Image | `/dashboard/upload` |
| Analysis Result | `/dashboard/analysis/[id]` |
| History | `/dashboard/history` |
| Profile | `/dashboard/profile` |
| Notifications | `/dashboard/notifications` |
| Language Settings | `/dashboard/settings/language` |

#### C. Admin Dashboard (CSR — admin role only)

| Page | Route |
|------|-------|
| Admin Login | `/admin/login` |
| User Management | `/admin/users` |
| Blog CMS | `/admin/blogs` |
| Blog Editor | `/admin/blogs/[id]/edit` |
| Advisory CMS | `/admin/advisories` |
| Crop Data Management | `/admin/crops` |
| AI Usage Dashboard | `/admin/analytics` |

### 18.3 UI Requirements

- Responsive design (mobile, tablet, desktop)
- Loading skeletons on all data-fetched pages
- Error boundary components with retry
- Image upload progress indicator (percentage)
- Toast notifications (success, error, info)
- Pagination on all list views
- Search and filter controls
- Modern SaaS aesthetic (dark mode support ready)

### 18.4 API Integration

- Centralized Axios instance with base URL, JWT interceptor, error transformer
- Service files: `authService`, `userService`, `cropService`, `blogService`, `advisoryService`, `weatherService`, `notificationService`, `adminService`
- React Query for caching, deduplication, background refresh
- Auth context: stores JWT, user info, roles; wraps protected routes

### 18.5 SEO Requirements

- `<title>`, `<meta description>`, OpenGraph tags on every page
- Canonical URLs
- Structured data (JSON-LD) for blog articles
- `sitemap.xml` (auto-generated)
- `robots.txt`

---

## 19. Documentation Deliverables

| Document | Purpose |
|----------|---------|
| `PRODUCT_REQUIREMENTS.md` | This document — full feature & system design |
| `ARCHITECTURE.md` | Detailed architecture diagrams & decisions |
| `MICROSERVICES.md` | Per-service API contracts & internal design |
| `DATABASE_DESIGN.md` | Entity schemas, migrations, indexes |
| `EVENT_FLOW.md` | Kafka topics, event schemas, flow diagrams |
| `API_STANDARDS.md` | REST conventions, error codes, pagination |
| `FRONTEND_ARCHITECTURE.md` | Frontend apps, components, routing, state |
| `LOCAL_SETUP.md` | Developer onboarding guide |
| `DEPLOYMENT.md` | Docker, CI/CD, environment setup |

---

## 19. Implementation Phases

| Phase | Scope | Deliverables |
|-------|-------|-------------|
| **1** | Documentation | All `.md` design documents |
| **2** | Project Structure | Maven parent POM, service directories, shared-lib |
| **3** | Core Infra | eureka-server, gateway-service, auth-service |
| **4** | Business Services | user, crop, weather, advisory, blog, notification, admin, ai-client, ai-service |
| **5** | Integration | Kafka wiring, Redis caching, MinIO integration |
| **6** | Security | JWT filter, RBAC, rate limiting, file validation |
| **7** | Testing | JUnit scaffolding, integration test structure |
| **8** | Docker Infra | `docker-compose.yml`, `.env` templates |
| **9** | Frontend — Public Website | Next.js SSG pages, SEO optimization |
| **10** | Frontend — Product Dashboard | Login, Register, Upload, Analysis, History, Profile, Notifications |
| **11** | Frontend — Admin Dashboard | User mgmt, Blog CMS, Advisory CMS, Analytics |
| **12** | Deployment | Startup scripts, health checks |

---

## 21. Technology Stack Summary

| Layer | Technology |
|-------|------------|
| Backend Framework | Java 17, Spring Boot 3, Spring Cloud |
| Security | Spring Security, JWT (jjwt) |
| Database | PostgreSQL 15+ |
| ORM | Spring Data JPA (Hibernate) |
| Migrations | Flyway |
| Cache | Redis 7+ |
| Message Broker | Apache Kafka |
| Object Storage | MinIO |
| AI/ML Service | Python 3.11, FastAPI, Uvicorn |
| API Gateway | Spring Cloud Gateway |
| Service Discovery | Netflix Eureka |
| Resilience | Resilience4j |
| Frontend Framework | Next.js 14 (App Router) |
| Frontend Language | TypeScript |
| UI Library | ShadCN UI |
| Styling | Tailwind CSS |
| State | React Query (TanStack), React Context |
| Forms | React Hook Form + Zod |
| HTTP Client | Axios |
| Containerization | Docker, Docker Compose |
| Code Quality | Lombok, MapStruct, Jakarta Validation, OpenAPI/Swagger |

---

*End of Product Requirements Document*
