CREATE TABLE ai_suggestions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    crop_name   VARCHAR(100),
    crop_stage  VARCHAR(50),
    suggestions JSONB NOT NULL,
    weather_snapshot JSONB,
    disease_context  JSONB,
    source      VARCHAR(20) NOT NULL DEFAULT 'RULE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ
);
CREATE INDEX idx_ai_sugg_user ON ai_suggestions(user_id, created_at DESC);
CREATE INDEX idx_ai_sugg_expires ON ai_suggestions(expires_at) WHERE expires_at IS NOT NULL;
