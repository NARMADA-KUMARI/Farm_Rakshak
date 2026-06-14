# FarmRakshak — Architecture Document

> **Version:** 1.0 | **Date:** 2026-03-26

---

## 1. System Overview

FarmRakshak is a distributed microservices platform. The system is composed of 11 Java/Spring Boot services, 1 Python AI service, and a Next.js frontend — all orchestrated via Docker Compose.

```
                        ┌─────────────┐
                        │   Internet   │
                        └──────┬──────┘
                               │
                    ┌──────────▼──────────┐
                    │  Next.js Frontend   │ :3000
                    │  (Public + Dashboard │
                    │   + Admin)          │
                    └──────────┬──────────┘
                               │ API calls
                    ┌──────────▼──────────┐
                    │   Gateway Service   │ :8080
                    │   Spring Cloud GW   │
                    │  ┌───────────────┐  │
                    │  │ JWT Filter    │  │
                    │  │ Rate Limiter  │  │
                    │  │ Route Config  │  │
                    │  └───────────────┘  │
                    └──────────┬──────────┘
                               │ Eureka Discovery
              ┌────────────────┼────────────────┐
              │                │                │
    ┌─────────▼───┐  ┌────────▼────┐  ┌────────▼────┐
    │ Auth Service │  │User Service │  │Crop Service │
    │    :8081     │  │   :8082     │  │   :8083     │
    └──────┬──────┘  └──────┬─────┘  └──────┬──────┘
           │                │               │
    ┌──────▼──────┐  ┌──────▼─────┐  ┌──────▼──────┐
    │  auth_db    │  │  user_db   │  │  crop_db    │
    └─────────────┘  └────────────┘  └─────────────┘

    ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
    │  Weather    │  │  Advisory   │  │  Blog       │
    │  Service    │  │  Service    │  │  Service    │
    │   :8085     │  │   :8086     │  │   :8088     │
    └──────┬──────┘  └──────┬─────┘  └──────┬──────┘
           │                │               │
    ┌──────▼──────┐  ┌──────▼─────┐  ┌──────▼──────┐
    │   Redis     │  │advisory_db │  │  blog_db    │
    └─────────────┘  └────────────┘  └─────────────┘

    ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
    │ AI Client   │  │Notification │  │  Admin      │
    │ Service     │  │  Service    │  │  Service    │
    │   :8084     │  │   :8087     │  │   :8089     │
    └──────┬──────┘  └──────┬─────┘  └─────────────┘
           │                │
    ┌──────▼──────┐  ┌──────▼─────┐
    │ AI Service  │  │notif_db    │
    │ (FastAPI)   │  └────────────┘
    │   :8090     │
    └─────────────┘

    ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
    │   Kafka     │  │   MinIO     │  │   Eureka    │
    │   :9092     │  │   :9000     │  │   :8761     │
    └─────────────┘  └─────────────┘  └─────────────┘
```

---

## 2. Communication Patterns

| Pattern | Where Used |
|---------|------------|
| Synchronous REST | Gateway → all services; AI Client → AI Service |
| Async Kafka events | Crop → AI Client (analysis); AI Client → Notification |
| Redis cache | Weather cache, token blacklist, rate limits |
| Service discovery | All services register with Eureka; Gateway resolves routes |

---

## 3. Gateway Routing Table

| Path Pattern | Target Service | Auth Required |
|-------------|----------------|---------------|
| `/api/v1/auth/**` | auth-service | No |
| `/api/v1/users/**` | user-service | Yes |
| `/api/v1/crops/**` | crop-service | Yes |
| `/api/v1/weather/**` | weather-service | Yes |
| `/api/v1/advisories/**` | advisory-service | Yes |
| `/api/v1/blogs/**` | blog-service | No (public read) |
| `/api/v1/notifications/**` | notification-service | Yes |
| `/api/v1/admin/**` | admin-service | Yes (ADMIN) |
| `/api/v1/i18n/**` | user-service | No |

---

## 4. Database-per-Service

| Service | Database | Schema Owner |
|---------|----------|-------------|
| auth-service | `farmrakshak_auth` | auth tables only |
| user-service | `farmrakshak_user` | user profiles |
| crop-service | `farmrakshak_crop` | uploads, analyses |
| advisory-service | `farmrakshak_advisory` | advisories |
| notification-service | `farmrakshak_notification` | notifications |
| blog-service | `farmrakshak_blog` | blog posts |

---

## 5. Key Architectural Patterns

| Pattern | Application |
|---------|-------------|
| API Gateway | Centralized entry, auth, rate limiting |
| Service Discovery | Eureka for dynamic routing |
| Event-Driven | Kafka for async image analysis pipeline |
| Circuit Breaker | Resilience4j on AI client calls |
| Cache-Aside | Redis for weather, tokens |
| Database-per-Service | Independent data ownership |
| Outbox Pattern | Fallback when Kafka unavailable |
| CQRS-ready | Admin reads via aggregation service |

---

## 6. Deployment Architecture

```
docker-compose.yml
├── Infrastructure
│   ├── postgres:15
│   ├── redis:7-alpine
│   ├── zookeeper
│   ├── kafka
│   ├── minio
│   └── eureka-server
├── Backend Services
│   ├── gateway-service
│   ├── auth-service
│   ├── user-service
│   ├── crop-service
│   ├── ai-client-service
│   ├── weather-service
│   ├── advisory-service
│   ├── notification-service
│   ├── blog-service
│   └── admin-service
├── AI
│   └── ai-service (FastAPI)
└── Frontend
    └── farmrakshak-web (Next.js)
```

All services are containerized as individual Docker images. Infrastructure services use official images.
