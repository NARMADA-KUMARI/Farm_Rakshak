# FarmRakshak — Event Flow Architecture

> **Version:** 1.0 | **Date:** 2026-03-26

---

## Topics

| Topic | Partitions | Producer | Consumer(s) |
|-------|-----------|----------|-------------|
| `crop-analysis-topic` | 3 | crop-service | ai-client-service |
| `analysis-result-topic` | 3 | ai-client-service | crop-service, notification-service |
| `notification-topic` | 3 | multiple | notification-service |
| `audit-topic` | 3 | all services | (future audit consumer) |

---

## Event Schema

All events follow this envelope:

```json
{
  "eventId": "UUID",
  "eventType": "string",
  "source": "service-name",
  "timestamp": "ISO-8601",
  "payload": { }
}
```

---

## Flow 1: Image Analysis Pipeline

```
crop-service                    ai-client-service              ai-service (FastAPI)
     │                                │                              │
     │── publish ──────────────────►  │                              │
     │   topic: crop-analysis-topic   │                              │
     │   payload: {                   │                              │
     │     analysisId, imageUrl,      │                              │
     │     userId                     │                              │
     │   }                            │                              │
     │                                │── POST /api/v1/analyze ────► │
     │                                │◄── {disease,confidence,...}── │
     │                                │                              │
     │◄── publish ────────────────── │                              │
     │   topic: analysis-result-topic │                              │
     │   payload: {                   │                              │
     │     analysisId, disease,       │                              │
     │     confidence, treatment,     │                              │
     │     prevention, status         │                              │
     │   }                            │                              │
```

---

## Flow 2: Notification Pipeline

```
ai-client-service (or any)     notification-service
     │                                │
     │── publish ──────────────────►  │
     │   topic: notification-topic    │
     │   payload: {                   │
     │     userId, type, title, body  │
     │   }                            │
     │                                │── save to DB
     │                                │── (future: push via FCM)
```

---

## Consumer Groups

| Service | Group ID |
|---------|----------|
| ai-client-service | `ai-client-group` |
| crop-service | `crop-result-group` |
| notification-service | `notification-group` |

---

## Failure Handling

| Failure | Mitigation |
|---------|------------|
| Consumer crash | Kafka redelivers (at-least-once) |
| AI service timeout | Publish failure event, retry with backoff |
| Kafka unavailable | Outbox pattern: store event in DB, poll & publish |
