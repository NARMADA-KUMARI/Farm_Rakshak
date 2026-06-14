# FarmRakshak — Microservices Design

> **Version:** 1.0 | **Date:** 2026-03-26

---

## 1. eureka-server

**Port:** 8761  
**Purpose:** Service discovery and registration.

No business logic. All services register on startup and query Eureka for downstream service URLs.

---

## 2. gateway-service

**Port:** 8080  
**Purpose:** API gateway, auth filter, rate limiting, routing.

### Filters
- `JwtAuthenticationFilter` — validates JWT on protected routes, extracts userId/roles into headers
- `RateLimitFilter` — Redis-backed token bucket (100 req/min default)
- `CorrelationIdFilter` — generates/propagates `X-Trace-Id` header

### Config
- Route definitions in `application.yml` mapping path patterns to Eureka service IDs.

---

## 3. auth-service

**Port:** 8081 | **DB:** `farmrakshak_auth`

### Endpoints
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/auth/register` | No | Register user |
| POST | `/api/v1/auth/login` | No | Login, returns tokens |
| POST | `/api/v1/auth/refresh` | No | Refresh access token |
| POST | `/api/v1/auth/logout` | Yes | Blacklist token |
| GET | `/api/v1/auth/validate` | Internal | Validate token (gateway) |

### Entities
- `AuthUser`: id, email, mobile, passwordHash, role, enabled, createdAt, updatedAt, deletedAt
- `RefreshToken`: id, userId, tokenHash, expiresAt, createdAt

---

## 4. user-service

**Port:** 8082 | **DB:** `farmrakshak_user`

### Endpoints
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/users/me` | FARMER+ | Get own profile |
| PUT | `/api/v1/users/me` | FARMER+ | Update profile |
| GET | `/api/v1/users/{id}` | ADMIN | Get user by id |
| GET | `/api/v1/users` | ADMIN | List users (paginated) |

### Entities
- `UserProfile`: id, authUserId, name, mobile, email, village, district, state, primaryCrops, languagePreference, profileImageUrl, createdAt, updatedAt, deletedAt

---

## 5. crop-service

**Port:** 8083 | **DB:** `farmrakshak_crop`

### Endpoints
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/crops/upload` | FARMER | Upload crop image |
| GET | `/api/v1/crops/analysis/{id}` | FARMER+ | Get analysis result |
| GET | `/api/v1/crops/history` | FARMER | Get upload history |

### Entities
- `CropUpload`: id, userId, imageUrl, originalFilename, fileSize, mimeType, status, createdAt
- `CropAnalysis`: id, uploadId, diseaseName, confidence, description, treatment, prevention, analyzedAt, createdAt

### Kafka
- **Produces:** `crop-analysis-topic` on upload
- **Consumes:** `analysis-result-topic` to store results

---

## 6. ai-client-service

**Port:** 8084 | **DB:** None

### Purpose
Bridge between Kafka events and the Python AI service.

### Kafka
- **Consumes:** `crop-analysis-topic`
- **Produces:** `analysis-result-topic`, `notification-topic`

### Integration
- REST call to `ai-service` at `POST /api/v1/analyze`
- Resilience4j circuit breaker: opens after 5 failures, half-open after 30s
- Retry: 3 attempts, exponential backoff
- Timeout: 30 seconds

---

## 7. weather-service

**Port:** 8085 | **DB:** None

### Endpoints
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/weather?lat={}&lon={}` | FARMER+ | Get weather data |
| GET | `/api/v1/weather/alerts?state={}` | FARMER+ | Get weather alerts |

### Caching
- Redis key: `weather:{lat}:{lon}`
- TTL: 3 hours
- Cache-aside pattern

---

## 8. advisory-service

**Port:** 8086 | **DB:** `farmrakshak_advisory`

### Endpoints
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/advisories` | FARMER+ | List advisories (filtered) |
| GET | `/api/v1/advisories/{id}` | FARMER+ | Get advisory detail |
| POST | `/api/v1/advisories` | EXPERT/ADMIN | Create advisory |
| PUT | `/api/v1/advisories/{id}` | EXPERT/ADMIN | Update advisory |
| DELETE | `/api/v1/advisories/{id}` | ADMIN | Soft-delete advisory |

### Entities
- `Advisory`: id, title, content, crop, season, region, language, type, authorId, createdAt, updatedAt, deletedAt

---

## 9. notification-service

**Port:** 8087 | **DB:** `farmrakshak_notification`

### Endpoints
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/notifications` | FARMER+ | List notifications |
| PUT | `/api/v1/notifications/{id}/read` | FARMER+ | Mark as read |
| PUT | `/api/v1/notifications/read-all` | FARMER+ | Mark all as read |

### Kafka
- **Consumes:** `notification-topic`

### Entities
- `Notification`: id, userId, type, title, body, read, createdAt

---

## 10. blog-service

**Port:** 8088 | **DB:** `farmrakshak_blog`

### Endpoints
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/blogs` | Public | List blogs (paginated) |
| GET | `/api/v1/blogs/{slug}` | Public | Get blog by slug |
| POST | `/api/v1/blogs` | ADMIN | Create blog |
| PUT | `/api/v1/blogs/{id}` | ADMIN | Update blog |
| DELETE | `/api/v1/blogs/{id}` | ADMIN | Soft-delete blog |

### Entities
- `BlogPost`: id, title, slug, content, excerpt, coverImageUrl, seoTitle, seoDescription, tags, authorId, status, publishedAt, createdAt, updatedAt, deletedAt

---

## 11. admin-service

**Port:** 8089 | **DB:** None (aggregates from other services)

### Endpoints
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/admin/dashboard` | ADMIN | Dashboard stats |
| GET | `/api/v1/admin/users` | ADMIN | Users list (proxied) |
| GET | `/api/v1/admin/analytics/ai` | ADMIN | AI usage stats |
| GET | `/api/v1/admin/analytics/uploads` | ADMIN | Upload stats |

---

## 12. ai-service (Python FastAPI)

**Port:** 8090

### Endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/analyze` | Analyze crop image |
| GET | `/health` | Health check |

### Response
```json
{
  "disease": "Late Blight",
  "confidence": 0.94,
  "description": "...",
  "treatment": ["..."],
  "prevention": ["..."]
}
```

### Internal Structure
```
ai-service/
├── app/
│   ├── main.py
│   ├── api/
│   │   └── routes.py
│   ├── services/
│   │   └── classifier.py
│   ├── models/
│   │   └── schemas.py
│   └── core/
│       └── config.py
├── requirements.txt
└── Dockerfile
```
