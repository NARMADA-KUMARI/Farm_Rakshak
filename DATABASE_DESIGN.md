# FarmRakshak — Database Design

> **Version:** 1.0 | **Date:** 2026-03-26

---

## Design Principles

- UUID primary keys (`gen_random_uuid()`)
- `created_at TIMESTAMPTZ DEFAULT NOW()`
- `updated_at TIMESTAMPTZ DEFAULT NOW()` (trigger-managed)
- Soft delete via `deleted_at TIMESTAMPTZ NULL`
- Flyway migrations: `V{N}__{description}.sql`
- Database-per-service isolation

---

## 1. farmrakshak_auth

### auth_users
```sql
CREATE TABLE auth_users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) UNIQUE,
    mobile          VARCHAR(20) UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'FARMER',
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_auth_users_email ON auth_users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_auth_users_mobile ON auth_users(mobile) WHERE deleted_at IS NULL;
```

### refresh_tokens
```sql
CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES auth_users(id),
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);
```

---

## 2. farmrakshak_user

### user_profiles
```sql
CREATE TABLE user_profiles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auth_user_id        UUID NOT NULL UNIQUE,
    name                VARCHAR(100) NOT NULL,
    mobile              VARCHAR(20),
    email               VARCHAR(255),
    village             VARCHAR(100),
    district            VARCHAR(100),
    state               VARCHAR(50),
    primary_crops       TEXT[],
    language_preference VARCHAR(5) NOT NULL DEFAULT 'en',
    profile_image_url   TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ
);

CREATE INDEX idx_user_profiles_auth_id ON user_profiles(auth_user_id);
CREATE INDEX idx_user_profiles_state ON user_profiles(state) WHERE deleted_at IS NULL;
```

---

## 3. farmrakshak_crop

### crop_uploads
```sql
CREATE TABLE crop_uploads (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL,
    image_url         TEXT NOT NULL,
    original_filename VARCHAR(255),
    file_size         BIGINT,
    mime_type         VARCHAR(50),
    status            VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_crop_uploads_user_id ON crop_uploads(user_id);
CREATE INDEX idx_crop_uploads_status ON crop_uploads(status);
CREATE INDEX idx_crop_uploads_created ON crop_uploads(created_at DESC);
```

### crop_analyses
```sql
CREATE TABLE crop_analyses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    upload_id       UUID NOT NULL REFERENCES crop_uploads(id),
    disease_name    VARCHAR(200),
    confidence      DECIMAL(5,4),
    description     TEXT,
    treatment       TEXT[],
    prevention      TEXT[],
    analyzed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_crop_analyses_upload ON crop_analyses(upload_id);
```

---

## 4. farmrakshak_advisory

### advisories
```sql
CREATE TABLE advisories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(200) NOT NULL,
    content     TEXT NOT NULL,
    crop        VARCHAR(100),
    season      VARCHAR(50),
    region      VARCHAR(100),
    language    VARCHAR(5) NOT NULL DEFAULT 'en',
    type        VARCHAR(50) NOT NULL,
    author_id   UUID NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX idx_advisories_crop ON advisories(crop) WHERE deleted_at IS NULL;
CREATE INDEX idx_advisories_season ON advisories(season) WHERE deleted_at IS NULL;
CREATE INDEX idx_advisories_language ON advisories(language) WHERE deleted_at IS NULL;
```

---

## 5. farmrakshak_notification

### notifications
```sql
CREATE TABLE notifications (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    type        VARCHAR(50) NOT NULL,
    title       VARCHAR(200) NOT NULL,
    body        TEXT,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_created ON notifications(created_at DESC);
```

---

## 6. farmrakshak_blog

### blog_posts
```sql
CREATE TABLE blog_posts (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title            VARCHAR(200) NOT NULL,
    slug             VARCHAR(250) NOT NULL UNIQUE,
    content          TEXT NOT NULL,
    excerpt          VARCHAR(300),
    cover_image_url  TEXT,
    seo_title        VARCHAR(60),
    seo_description  VARCHAR(160),
    tags             TEXT[],
    author_id        UUID NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_blog_posts_slug ON blog_posts(slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_blog_posts_status ON blog_posts(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_blog_posts_published ON blog_posts(published_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_blog_posts_tags ON blog_posts USING GIN(tags);
```

---

## 7. Translation Table (user-service)

### translations
```sql
CREATE TABLE translations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key         VARCHAR(255) NOT NULL,
    locale      VARCHAR(5) NOT NULL,
    value       TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(key, locale)
);

CREATE INDEX idx_translations_locale ON translations(locale);
```
