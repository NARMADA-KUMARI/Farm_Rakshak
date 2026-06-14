CREATE TABLE user_profiles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auth_user_id        UUID NOT NULL UNIQUE,
    name                VARCHAR(100) NOT NULL,
    mobile              VARCHAR(20),
    email               VARCHAR(255),
    village             VARCHAR(100),
    district            VARCHAR(100),
    state               VARCHAR(50),
    primary_crops       TEXT,
    language_preference VARCHAR(5) NOT NULL DEFAULT 'en',
    profile_image_url   TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ
);

CREATE INDEX idx_user_profiles_auth_id ON user_profiles(auth_user_id);
CREATE INDEX idx_user_profiles_state ON user_profiles(state) WHERE deleted_at IS NULL;
