CREATE TABLE blog_posts (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title            VARCHAR(200) NOT NULL,
    slug             VARCHAR(250) NOT NULL UNIQUE,
    content          TEXT NOT NULL,
    excerpt          VARCHAR(300),
    cover_image_url  TEXT,
    seo_title        VARCHAR(60),
    seo_description  VARCHAR(160),
    tags             TEXT,
    author_id        UUID NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ
);
CREATE UNIQUE INDEX idx_blog_posts_slug ON blog_posts(slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_blog_posts_status ON blog_posts(status) WHERE deleted_at IS NULL;
