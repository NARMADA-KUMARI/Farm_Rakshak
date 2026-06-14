# FarmRakshak — Deployment Guide

> **Version:** 1.0 | **Date:** 2026-03-26

---

## Docker Compose Deployment

### Full Stack Start
```bash
cp .env.template .env
# Edit .env with production values
docker-compose up -d
```

### Service Startup Order
1. PostgreSQL, Redis, Zookeeper
2. Kafka, MinIO
3. Eureka Server
4. Gateway Service
5. All business services
6. AI Service
7. Frontend

---

## Environment Variables

All services read from `.env`. Key variables:

| Variable | Example | Used By |
|----------|---------|---------|
| `POSTGRES_HOST` | `postgres` | All DB services |
| `POSTGRES_PORT` | `5432` | All DB services |
| `REDIS_HOST` | `redis` | gateway, auth, weather |
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | crop, ai-client, notification |
| `MINIO_ENDPOINT` | `http://minio:9000` | crop-service |
| `MINIO_ACCESS_KEY` | `minioadmin` | crop-service |
| `MINIO_SECRET_KEY` | `minioadmin` | crop-service |
| `JWT_SECRET` | `<random-256-bit>` | auth, gateway |
| `AI_SERVICE_URL` | `http://ai-service:8090` | ai-client-service |
| `EUREKA_URL` | `http://eureka-server:8761/eureka` | all services |

---

## Health Checks

Each Spring Boot service exposes:
```
GET /actuator/health
```

AI service exposes:
```
GET /health
```

---

## Scaling

```bash
# Scale crop-service to 3 instances
docker-compose up -d --scale crop-service=3
```

---

## Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f crop-service
```
