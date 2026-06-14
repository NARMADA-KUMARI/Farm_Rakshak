# FarmRakshak — New Feature Requirements

> **Version:** 2.0
> **Date:** 2026-03-28
> **Status:** Approved
> **Author:** Product & Engineering Team

---

## 1. Overview

This document defines the next phase of features for FarmRakshak. These features are prioritized based on direct impact to farmer revenue, daily engagement, accessibility, and platform stickiness. The goal is to transform FarmRakshak from a disease detection tool into a **daily-use farming companion**.

---

## 2. Priority Framework

| Tier | Label | Criteria |
|------|-------|----------|
| **T1** | Game Changers | Direct revenue/yield impact, high daily engagement |
| **T2** | Strong Differentiators | Unique value, strong retention drivers |
| **T3** | Engagement & Retention | Increase DAU, reduce churn |
| **T4** | Future Vision | Long-term roadmap, depends on T1-T3 |

---

## 3. Tier 1 — Game Changers

### 3.1 Mandi Price Tracker (Market Prices)

**Priority:** P0
**Impact:** Direct farmer revenue — farmers lose 20-40% selling at wrong time/place
**Effort:** Medium

#### Problem

Farmers sell crops without knowing current market rates. They depend on middlemen who exploit information asymmetry. A farmer growing tomatoes in Nashik has no easy way to compare prices across Pune, Mumbai, and Ahmednagar mandis.

#### Requirements

**User Stories:**

- As a farmer, I want to see today's price of my crops at nearby mandis so I can decide where and when to sell.
- As a farmer, I want price trend charts so I can predict if prices will rise or fall.
- As a farmer, I want alerts when my crop crosses a target price so I don't miss the best selling window.

**Functional Requirements:**

| ID | Requirement | Priority |
|----|-------------|----------|
| MP-01 | Show real-time crop prices from nearest 5 APMC mandis | P0 |
| MP-02 | Price trend charts: 7-day, 30-day, 90-day | P0 |
| MP-03 | AI-powered "Best time to sell" recommendation | P1 |
| MP-04 | Price alerts: notify when crop crosses target price | P0 |
| MP-05 | Filter by crop, mandi, date range | P0 |
| MP-06 | Auto-detect nearest mandis from user's location | P1 |
| MP-07 | Compare prices across mandis side-by-side | P1 |
| MP-08 | Historical price data for seasonal planning | P2 |

**Data Sources:**

- Primary: `agmarknet.gov.in` APMC daily prices API
- Secondary: `data.gov.in` commodity prices dataset
- Fallback: Manual data entry by admin for unconnected mandis

**Technical Design:**

```
New Service: mandi-service (port 8090)

Database: farmrakshak_mandi
Tables:
  - mandi_master (id, name, district, state, lat, lon)
  - price_records (id, mandi_id, crop_name, variety, min_price, max_price, modal_price, unit, arrival_qty, recorded_date)
  - price_alerts (id, user_id, crop_name, target_price, direction, mandi_id, is_active)

APIs:
  GET  /api/v1/mandi/prices?crop=tomato&state=Maharashtra&days=7
  GET  /api/v1/mandi/nearby?lat=19.07&lon=72.87&radius=50
  GET  /api/v1/mandi/trends?crop=tomato&mandi_id=123&period=30d
  POST /api/v1/mandi/alerts  { crop, targetPrice, direction: "above"|"below", mandiId }
  GET  /api/v1/mandi/alerts

Scheduled Jobs:
  - Daily price fetcher (cron: 0 18 * * *) — scrapes agmarknet after market hours
  - Alert checker — runs after price fetch, sends notifications via Kafka
```

**Frontend Pages:**

```
/mandi                → Price dashboard with search
/mandi/[crop]         → Crop-specific price details with charts
/mandi/alerts         → Manage price alerts
```

**UI Wireframe:**

```
┌─────────────────────────────────────┐
│ Mandi Prices          [Search crop] │
├─────────────────────────────────────┤
│ 🍅 Tomato        ₹35/kg  ↑12%     │
│    Nashik Mandi   Today             │
│    [7d chart ━━━━━━╱━━━]           │
│    Pune: ₹32  Mumbai: ₹38          │
│    [Set Price Alert]                │
├─────────────────────────────────────┤
│ 🌾 Wheat         ₹2,150/qtl ↓3%   │
│    Indore Mandi   Today             │
│    ...                              │
└─────────────────────────────────────┘
```

**Success Metrics:**

- 60% of active farmers check prices weekly
- Average farmer visits mandi page 4+ times/week
- 30% set at least one price alert

---

### 3.2 Government Scheme Finder

**Priority:** P0
**Impact:** Financial inclusion — most farmers miss schemes worth ₹6,000-₹2,00,000/year
**Effort:** Medium

#### Problem

India has 100+ government schemes for farmers (PM-KISAN, PMFBY, KCC, state subsidies). Most farmers don't know which schemes they qualify for, how to apply, or when deadlines are.

#### Requirements

**User Stories:**

- As a farmer, I want to see government schemes I'm eligible for based on my profile so I don't miss financial benefits.
- As a farmer, I want step-by-step application guidance in my language so I can apply without help.
- As a farmer, I want deadline alerts so I don't miss enrollment windows.

**Functional Requirements:**

| ID | Requirement | Priority |
|----|-------------|----------|
| GS-01 | Auto-match schemes based on user profile (state, crops, land size, category) | P0 |
| GS-02 | Scheme detail page: benefits, eligibility, documents required, how to apply | P0 |
| GS-03 | Step-by-step application guide in user's language | P0 |
| GS-04 | Deadline alerts via notification | P0 |
| GS-05 | Filter schemes by: category, state, crop, benefit type | P1 |
| GS-06 | "You're eligible for X schemes" badge on dashboard | P1 |
| GS-07 | Track application status (applied, pending, approved, rejected) | P2 |
| GS-08 | Admin can add/edit scheme data | P0 |

**Scheme Categories:**

- Income Support (PM-KISAN, state pensions)
- Crop Insurance (PMFBY, WBCIS)
- Credit (KCC, interest subvention)
- Input Subsidy (seeds, fertilizer, equipment)
- Irrigation (PMKSY, drip/sprinkler subsidy)
- Organic Farming (Paramparagat Krishi)
- Market Support (e-NAM, MSP)

**Technical Design:**

```
Table: government_schemes (in admin-service or new scheme-service)
  - id, name, description, benefits, eligibility_criteria (JSONB)
  - documents_required (TEXT[]), how_to_apply (TEXT)
  - category, applicable_states (TEXT[]), applicable_crops (TEXT[])
  - min_land_area, max_land_area, farmer_category
  - enrollment_start, enrollment_end, is_active
  - website_url, helpline

Table: user_scheme_matches (materialized or computed)
  - user_id, scheme_id, match_score, matched_on

APIs:
  GET  /api/v1/schemes/eligible          → auto-matched for current user
  GET  /api/v1/schemes?state=MH&category=insurance
  GET  /api/v1/schemes/{id}              → full detail
  POST /api/v1/schemes/{id}/track        → mark as applied
```

**Success Metrics:**

- 50% of users view scheme recommendations within first week
- 20% click "How to Apply" on at least one scheme
- 10% report successfully applying through the guide

---

### 3.3 Voice Input & Voice Response

**Priority:** P0
**Impact:** Accessibility — unlocks platform for 40%+ semi-literate farmers
**Effort:** Medium-High

#### Problem

Many Indian farmers have limited reading/writing ability. Typing questions in Hindi or Telugu is difficult. Voice is the most natural interface for rural users.

#### Requirements

**User Stories:**

- As a farmer, I want to speak my question to the AI assistant instead of typing so I can use the app easily.
- As a farmer, I want the AI to read its response aloud so I don't have to read long text.
- As a farmer, I want voice to work in my language (Hindi, Telugu, Marathi, Kannada).

**Functional Requirements:**

| ID | Requirement | Priority |
|----|-------------|----------|
| VO-01 | Voice-to-text input in AI chat (all 5 languages) | P0 |
| VO-02 | Text-to-speech for AI responses | P0 |
| VO-03 | Microphone button in ChatWidget and AI Assistant page | P0 |
| VO-04 | Speaker button on each AI response message | P0 |
| VO-05 | Visual feedback during recording (waveform animation) | P1 |
| VO-06 | Auto-detect spoken language | P2 |
| VO-07 | Voice commands for navigation: "Show my crops", "Open weather" | P2 |

**Technical Approach:**

```
Voice-to-Text:
  Option A: Web Speech API (free, browser-native, good Hindi/Marathi support)
  Option B: OpenAI Whisper API (better accuracy, costs per minute)
  Recommendation: Web Speech API first, Whisper as fallback

Text-to-Speech:
  Option A: Web Speech API (SpeechSynthesis — free, built-in)
  Option B: Google Cloud TTS (better quality for Indian languages)
  Recommendation: Web Speech API with Google TTS for premium quality

Frontend:
  - New component: VoiceInput.tsx (mic button with recording state)
  - New component: SpeakButton.tsx (speaker icon on AI messages)
  - Integration points: ChatWidget, AI Assistant page, search bars
  - Language mapping: user's i18n language → speech recognition lang code

No backend changes needed — voice processing happens client-side.
```

**UI:**

```
┌──────────────────────────────┐
│  Ask about your crops...  🎤 │  ← Mic button next to send
└──────────────────────────────┘

When recording:
┌──────────────────────────────┐
│  🔴 Listening...  ~~~~ [✕]  │  ← Waveform + cancel
└──────────────────────────────┘

On AI response:
┌──────────────────────────────┐
│ 🤖 Your tomato needs...     │
│    [🔊 Listen]              │  ← TTS button
└──────────────────────────────┘
```

---

### 3.4 Crop Calendar & Smart Task Reminders

**Priority:** P0
**Impact:** Yield improvement — timely actions increase yield 15-25%
**Effort:** Medium

#### Problem

Farmers miss critical farming windows (fertilizer timing, pest spraying, irrigation schedules). The existing task system generates tasks but doesn't push reminders through channels farmers actually check.

#### Requirements

**User Stories:**

- As a farmer, I want a visual calendar showing what to do each day/week for my crops.
- As a farmer, I want reminders via WhatsApp/SMS so I don't miss important actions.
- As a farmer, I want tasks to auto-adjust based on weather (e.g., delay spraying if rain expected).

**Functional Requirements:**

| ID | Requirement | Priority |
|----|-------------|----------|
| CT-01 | Visual crop calendar (day/week/month views) | P0 |
| CT-02 | Auto-generated tasks from crop lifecycle stages | P0 (exists, enhance) |
| CT-03 | Weather-aware task adjustment ("Rain tomorrow, delay spraying") | P0 |
| CT-04 | WhatsApp reminders for upcoming tasks | P1 |
| CT-05 | SMS fallback for farmers without WhatsApp | P1 |
| CT-06 | Daily morning summary: "Today's tasks for your 3 crops" | P0 |
| CT-07 | Task completion tracking with streak gamification | P2 |
| CT-08 | Push notifications (browser/PWA) | P1 |

**Technical Design:**

```
Enhancements to existing crop-service:

New Table: task_reminders
  - id, task_id, user_id, channel (WHATSAPP|SMS|PUSH|IN_APP)
  - scheduled_at, sent_at, status

New Table: daily_summaries
  - id, user_id, summary_text, language, sent_at, channel

Scheduled Jobs:
  - Morning summary generator (cron: 0 6 * * *)
    → Fetch user's crops + pending tasks + weather
    → Generate summary in user's language
    → Send via preferred channel

  - Weather-based task adjuster (cron: 0 5 * * *)
    → Check weather forecast for next 48h
    → Auto-postpone outdoor tasks if heavy rain/storm expected
    → Notify user: "Spraying postponed due to expected rain"

WhatsApp Integration:
  - WhatsApp Business API (Meta Cloud API)
  - Template messages for task reminders
  - Interactive buttons: "Done ✅" / "Remind Later 🔔"

Frontend:
  /calendar → New calendar page with day/week views
  Dashboard → "Today's Tasks" card
```

---

## 4. Tier 2 — Strong Differentiators

### 4.1 Community Forum / Farmer Network

**Priority:** P1
**Impact:** Engagement + peer learning — farmers trust other farmers
**Effort:** Medium

#### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| CF-01 | Post questions with text + photos | P0 |
| CF-02 | Upvote/downvote answers | P0 |
| CF-03 | Tag by crop, region, problem type | P0 |
| CF-04 | "Farmers near you growing same crop" discovery | P1 |
| CF-05 | Expert-verified answer badge | P1 |
| CF-06 | Report/moderate inappropriate content | P0 |
| CF-07 | Trending topics/questions | P2 |
| CF-08 | All content in user's selected language | P0 |

**Technical Design:**

```
New Service: community-service (port 8091)

Database: farmrakshak_community
Tables:
  - posts (id, user_id, title, content, images[], crop_tags[], region, language, upvotes, created_at)
  - answers (id, post_id, user_id, content, is_expert_verified, upvotes, created_at)
  - votes (id, user_id, target_id, target_type, vote_type)
  - reports (id, reporter_id, target_id, target_type, reason, status)

Frontend:
  /community            → Feed of questions
  /community/[id]       → Question detail with answers
  /community/ask        → Post a question
  /community/my-posts   → User's posts
```

---

### 4.2 Soil Health Dashboard

**Priority:** P1
**Impact:** Long-term yield improvement — soil health is foundational
**Effort:** Low-Medium

#### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| SH-01 | Input soil test report values (pH, N, P, K, organic carbon, EC) | P0 |
| SH-02 | AI interpretation: "Your soil is nitrogen-deficient for wheat" | P0 |
| SH-03 | Fertilizer calculator: exact quantity per acre based on soil + crop | P0 |
| SH-04 | Track soil health over multiple seasons | P1 |
| SH-05 | Nearest soil testing lab locator | P1 |
| SH-06 | Recommended soil amendments | P0 |
| SH-07 | Soil health score (0-100) with improvement tips | P2 |

**Technical Design:**

```
New tables in crop-service or user-service:

  soil_reports (id, user_id, sample_date, ph, nitrogen, phosphorus, potassium,
                organic_carbon, ec, soil_texture, lab_name, report_image_url)

  fertilizer_recommendations (id, soil_report_id, crop_id, urea_kg_per_acre,
                              dap_kg_per_acre, mop_kg_per_acre, micronutrients, notes)

Frontend:
  /soil              → Soil dashboard
  /soil/add-report   → Input soil test values
  /soil/calculator   → Fertilizer calculator
```

---

### 4.3 Yield Estimation & Harvest Planner

**Priority:** P1
**Impact:** Revenue optimization through planning
**Effort:** Medium

#### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| YE-01 | AI yield prediction based on crop health + weather + disease history | P0 |
| YE-02 | Estimated harvest date with confidence range | P0 |
| YE-03 | "Expected: 12 quintals of tomato in 18 days" on crop detail | P0 |
| YE-04 | Storage recommendations (cold storage vs immediate sale) | P1 |
| YE-05 | Connect with mandi prices for optimal sell timing | P1 |
| YE-06 | Season-over-season yield comparison | P2 |

---

### 4.4 Expense & Profit Tracker

**Priority:** P1
**Impact:** Financial awareness — most farmers don't track true costs
**Effort:** Low-Medium

#### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| EP-01 | Log expenses by category: seeds, fertilizer, pesticide, labor, irrigation, transport | P0 |
| EP-02 | Per-crop expense tracking (link expense to user_crop) | P0 |
| EP-03 | Revenue logging (quantity sold, price, mandi) | P0 |
| EP-04 | Auto-calculate profit/loss per crop per season | P0 |
| EP-05 | Season comparison: "You spent ₹3,200 more on cotton vs last year" | P1 |
| EP-06 | Expense pie chart and trend visualization | P1 |
| EP-07 | Export report as PDF (for loan/insurance applications) | P2 |

**Technical Design:**

```
New tables in user-service or new finance-service:

  farm_expenses (id, user_id, user_crop_id, category, amount, description, expense_date, created_at)
  farm_revenue (id, user_id, user_crop_id, quantity, unit, price_per_unit, buyer, sale_date, mandi_name)

Frontend:
  /finance           → Overview dashboard with profit/loss
  /finance/add       → Quick expense/revenue entry
  /finance/[cropId]  → Per-crop breakdown
```

---

## 5. Tier 3 — Engagement & Retention

### 5.1 Offline Mode (PWA)

**Priority:** P1
**Impact:** Enables usage in low-connectivity rural areas
**Effort:** Medium

#### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| OF-01 | Service worker for caching static assets and translation files | P0 |
| OF-02 | Cache last weather data, advisory, and crop info for offline viewing | P0 |
| OF-03 | Queue disease scan uploads for when connectivity returns | P0 |
| OF-04 | "Offline" indicator banner when disconnected | P0 |
| OF-05 | Auto-sync queued actions when back online | P0 |
| OF-06 | Install as PWA on Android home screen | P1 |

**Technical Approach:**

```
- next-pwa or Serwist for service worker generation
- IndexedDB for local data caching (crop data, weather, tasks)
- Background sync API for queued uploads
- Manifest.json for PWA install prompt
```

---

### 5.2 WhatsApp Bot Integration

**Priority:** P1
**Impact:** Reach farmers where they already are (500M+ WhatsApp users in India)
**Effort:** High

#### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| WA-01 | Send crop photo via WhatsApp → receive disease diagnosis | P0 |
| WA-02 | Daily weather + task summary via WhatsApp | P0 |
| WA-03 | Price alerts via WhatsApp | P1 |
| WA-04 | AI chat via WhatsApp (text questions) | P1 |
| WA-05 | Language selection via WhatsApp menu | P0 |
| WA-06 | Registration/linking via WhatsApp number | P0 |

**Technical Design:**

```
New Service: whatsapp-service (port 8092)

Integration: WhatsApp Business API (Meta Cloud API)
  - Webhook for incoming messages
  - Template messages for outbound notifications
  - Media API for receiving/sending images

Flow:
  Farmer sends photo → webhook → ai-client-service → response → WhatsApp reply
  Daily cron → generate summary → send via WhatsApp template
```

---

### 5.3 Pest & Disease Early Warning System

**Priority:** P1
**Impact:** Preventive action — save crops before damage occurs
**Effort:** Medium

#### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| EW-01 | Aggregate disease scan data by region (anonymized) | P0 |
| EW-02 | Detect disease outbreak patterns: "8 leaf blight cases within 15km" | P0 |
| EW-03 | Alert nearby farmers: "Leaf blight spreading in your area" | P0 |
| EW-04 | Disease hotspot map visualization | P1 |
| EW-05 | Preventive advisory before disease reaches their farm | P0 |
| EW-06 | Seasonal pest forecasting based on weather patterns | P2 |

---

### 5.4 Water Management / Irrigation Scheduler

**Priority:** P2
**Impact:** Water efficiency — most critical resource in Indian agriculture
**Effort:** Medium

#### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| WM-01 | Calculate daily crop water requirement (growth stage + weather + soil) | P0 |
| WM-02 | "Your rice needs 4cm irrigation today" notification | P0 |
| WM-03 | Rainfall utilization tracking: "Yesterday's 12mm rain covers 3 days" | P1 |
| WM-04 | Drip vs flood vs sprinkler efficiency comparison | P2 |
| WM-05 | Weekly irrigation schedule generation | P1 |

---

## 6. Tier 4 — Future Vision

### 6.1 Drone/Satellite Field Mapping

**Priority:** P2
**Effort:** High

| ID | Requirement |
|----|-------------|
| DM-01 | NDVI vegetation health maps from Sentinel-2 satellite imagery (free) |
| DM-02 | Identify stressed zones before visible symptoms |
| DM-03 | Plot boundary detection from satellite |
| DM-04 | Time-lapse field health over season |
| DM-05 | Drone image analysis for large farms |

---

### 6.2 Input Marketplace

**Priority:** P2
**Effort:** High

| ID | Requirement |
|----|-------------|
| IM-01 | Browse seeds, fertilizers, pesticides by crop |
| IM-02 | Price comparison across brands and sellers |
| IM-03 | Verified quality ratings and reviews |
| IM-04 | Cash-on-delivery for rural areas |
| IM-05 | AI-recommended products based on crop/soil |

---

### 6.3 Crop Insurance Claim Assistant

**Priority:** P2
**Effort:** Medium

| ID | Requirement |
|----|-------------|
| CI-01 | Auto-document crop damage with timestamped photos |
| CI-02 | Generate claim reports in required PMFBY format |
| CI-03 | GPS-tagged damage evidence |
| CI-04 | Track claim status with insurer |
| CI-05 | AI estimates damage percentage for claim amount |

---

## 7. Implementation Roadmap

```
Phase 1 (Month 1-2): Foundation
├── Mandi Price Tracker (backend + frontend)
├── Government Scheme Finder (data + matching)
├── Voice Input in AI Chat
└── PWA / Offline Mode basics

Phase 2 (Month 2-3): Engagement
├── Smart Task Reminders (WhatsApp integration)
├── Crop Calendar UI
├── Expense & Profit Tracker
└── Soil Health Dashboard

Phase 3 (Month 3-4): Community
├── Community Forum
├── Pest & Disease Early Warning
├── Yield Estimation
└── Water Management

Phase 4 (Month 5+): Scale
├── WhatsApp Bot (full)
├── Satellite Mapping
├── Input Marketplace
└── Insurance Claim Assistant
```

---

## 8. Technical Dependencies

| Feature | Depends On |
|---------|------------|
| Mandi Prices | New mandi-service, agmarknet API access |
| Government Schemes | Admin data entry, scheme database |
| Voice Input | Web Speech API (no backend needed) |
| WhatsApp Bot | Meta Business API approval, phone number |
| Offline Mode | Service worker, IndexedDB |
| Community Forum | New community-service, moderation |
| Satellite Maps | Sentinel Hub API or Google Earth Engine |
| Marketplace | Payment gateway, seller onboarding |

---

## 9. Success Metrics (Overall)

| Metric | Current | Target (6 months) |
|--------|---------|-------------------|
| Daily Active Users | - | 5,000+ |
| Avg. sessions/user/week | 2 | 5+ |
| Feature adoption (Mandi) | 0% | 60% |
| Scheme applications started | 0 | 1,000+ |
| Voice input usage | 0% | 30% |
| WhatsApp bot users | 0 | 3,000+ |
| Farmer retention (30-day) | - | 65%+ |
| NPS Score | - | 50+ |

---

## 10. Non-Functional Requirements

| Requirement | Specification |
|-------------|---------------|
| Language Support | All new features must support en, hi, te, mr, kn |
| Mobile Performance | Page load < 2s on 3G, interaction < 100ms |
| Offline | Core features usable without internet |
| Accessibility | Voice input, large touch targets, high contrast |
| Data Privacy | Farmer data encrypted, location anonymized for aggregation |
| Scalability | Support 100K concurrent users |
| API Response Time | p95 < 500ms for all read APIs |

---

*This document will be updated as features move through development. Each feature will have its own technical design document before implementation begins.*
