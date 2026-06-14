CREATE TABLE disease_alerts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_user_id    UUID NOT NULL,
    crop_name           VARCHAR(100) NOT NULL,
    disease_name        VARCHAR(200) NOT NULL,
    description         TEXT,
    treatment           TEXT,
    prevention          TEXT,
    latitude            DECIMAL(10, 7),
    longitude           DECIMAL(10, 7),
    village             VARCHAR(100),
    district            VARCHAR(100),
    state               VARCHAR(50),
    radius_km           INT NOT NULL DEFAULT 100,
    farmers_notified    INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_disease_alerts_crop ON disease_alerts(crop_name);
CREATE INDEX idx_disease_alerts_date ON disease_alerts(created_at);
CREATE INDEX idx_disease_alerts_location ON disease_alerts(district, state);
CREATE INDEX idx_disease_alerts_dedup ON disease_alerts(disease_name, district, created_at);
