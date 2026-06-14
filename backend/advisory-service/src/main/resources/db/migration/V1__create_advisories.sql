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
CREATE INDEX idx_advisories_language ON advisories(language) WHERE deleted_at IS NULL;
