# FarmRakshak — Local Setup Guide

> **Version:** 1.0 | **Date:** 2026-03-26

---

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Docker + Docker Compose | 24+ | Container runtime |
| Java | 17 | Backend services |
| Maven | 3.9+ | Build tool |
| Node.js | 18+ | Frontend |
| Python | 3.11+ | AI service |
| Git | Latest | Version control |

---

## Quick Start

### 1. Clone & navigate
```bash
git clone <repo-url>
cd FarmRakshak
```

### 2. Copy environment file
```bash
cp .env.template .env
```

### 3. Start infrastructure
```bash
docker-compose up -d postgres redis kafka zookeeper minio eureka-server
```

### 4. Build backend
```bash
mvn clean package -DskipTests
```

### 5. Start backend services
```bash
docker-compose up -d gateway-service auth-service user-service crop-service \
  ai-client-service weather-service advisory-service notification-service \
  blog-service admin-service
```

### 6. Start AI service
```bash
cd ai-service
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8090
```

### 7. Start frontend
```bash
cd farmrakshak-web
npm install
npm run dev
```

---

## Service URLs

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Gateway | http://localhost:8080 |
| Eureka | http://localhost:8761 |
| MinIO Console | http://localhost:9001 |
| Kafka | localhost:9092 |
| PostgreSQL | localhost:5432 |
| Redis | localhost:6379 |

---

## Default Credentials

| Service | Username | Password |
|---------|----------|----------|
| PostgreSQL | `farmrakshak` | `farmrakshak_dev` |
| MinIO | `minioadmin` | `minioadmin` |
| Redis | — | — (no auth in dev) |

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Port conflict | Check `docker ps` and stop conflicting containers |
| Eureka not ready | Wait 30s for Eureka to start before launching services |
| Kafka not connecting | Ensure Zookeeper is up first |
| MinIO bucket missing | Buckets are auto-created on service startup |
