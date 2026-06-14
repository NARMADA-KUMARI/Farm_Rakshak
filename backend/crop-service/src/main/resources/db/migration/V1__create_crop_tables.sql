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

CREATE TABLE crop_analyses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    upload_id       UUID NOT NULL REFERENCES crop_uploads(id),
    disease_name    VARCHAR(200),
    confidence      DECIMAL(5,4),
    description     TEXT,
    treatment       TEXT,
    prevention      TEXT,
    analyzed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_crop_analyses_upload ON crop_analyses(upload_id);
