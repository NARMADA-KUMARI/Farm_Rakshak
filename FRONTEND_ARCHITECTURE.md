# FarmRakshak — Frontend Architecture

> **Version:** 1.0 | **Date:** 2026-03-26

---

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Next.js | 14 | App Router, SSR/SSG/ISR |
| TypeScript | 5.x | Type safety |
| Tailwind CSS | 3.x | Utility-first styling |
| ShadCN UI | latest | Accessible component library |
| React Query | 5.x | Server state, caching |
| Axios | 1.x | HTTP client |
| React Hook Form | 7.x | Form handling |
| Zod | 3.x | Schema validation |
| React Context | — | Auth state |

---

## Project Structure

```
farmrakshak-web/
├── src/
│   ├── app/
│   │   ├── (public)/          # Public website routes
│   │   │   ├── page.tsx       # Home
│   │   │   ├── features/
│   │   │   ├── how-it-works/
│   │   │   ├── about/
│   │   │   ├── contact/
│   │   │   ├── blog/
│   │   │   ├── faq/
│   │   │   ├── privacy/
│   │   │   └── terms/
│   │   ├── (auth)/
│   │   │   ├── login/
│   │   │   └── register/
│   │   ├── dashboard/         # Product dashboard (auth required)
│   │   │   ├── page.tsx
│   │   │   ├── upload/
│   │   │   ├── analysis/[id]/
│   │   │   ├── history/
│   │   │   ├── profile/
│   │   │   ├── notifications/
│   │   │   └── settings/language/
│   │   ├── admin/             # Admin dashboard (admin role)
│   │   │   ├── login/
│   │   │   ├── users/
│   │   │   ├── blogs/
│   │   │   ├── advisories/
│   │   │   ├── crops/
│   │   │   └── analytics/
│   │   ├── layout.tsx
│   │   └── globals.css
│   ├── components/
│   │   ├── ui/                # ShadCN components
│   │   ├── layout/            # Header, Footer, Sidebar
│   │   ├── forms/             # Login, Register, Upload forms
│   │   ├── dashboard/         # Dashboard widgets
│   │   └── admin/             # Admin-specific components
│   ├── lib/
│   │   ├── api/
│   │   │   ├── client.ts      # Axios instance + interceptors
│   │   │   ├── auth.ts
│   │   │   ├── crops.ts
│   │   │   ├── blogs.ts
│   │   │   ├── weather.ts
│   │   │   ├── advisories.ts
│   │   │   ├── notifications.ts
│   │   │   └── admin.ts
│   │   ├── hooks/
│   │   │   ├── useAuth.ts
│   │   │   ├── useCrops.ts
│   │   │   └── useNotifications.ts
│   │   ├── context/
│   │   │   └── AuthContext.tsx
│   │   ├── utils/
│   │   │   ├── cn.ts
│   │   │   └── formatters.ts
│   │   └── validations/
│   │       ├── auth.ts
│   │       └── crop.ts
│   └── types/
│       ├── api.ts
│       ├── user.ts
│       ├── crop.ts
│       └── blog.ts
├── public/
│   ├── robots.txt
│   └── images/
├── next.config.js
├── tailwind.config.ts
├── tsconfig.json
└── package.json
```

---

## API Integration Pattern

```typescript
// lib/api/client.ts
const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL,
  headers: { 'Content-Type': 'application/json' }
});

// JWT interceptor
apiClient.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Error interceptor — auto-refresh on 401
apiClient.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    if (error.response?.status === 401) {
      // attempt token refresh
    }
    return Promise.reject(error);
  }
);
```

---

## State Management

| State Type | Solution |
|-----------|----------|
| Server state (API data) | React Query |
| Auth state (JWT, user) | React Context |
| Form state | React Hook Form |
| UI state (modals, toasts) | Local state / Context |

---

## SEO Strategy

| Technique | Implementation |
|-----------|---------------|
| Meta tags | Next.js `metadata` export per page |
| OpenGraph | `og:title`, `og:description`, `og:image` |
| Canonical | `<link rel="canonical">` via metadata |
| Structured data | JSON-LD for blog articles |
| Sitemap | `next-sitemap` package |
| robots.txt | Static file in `/public` |
