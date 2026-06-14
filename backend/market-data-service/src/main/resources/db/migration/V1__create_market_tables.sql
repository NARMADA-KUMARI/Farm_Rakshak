-- Market Intelligence Schema
-- V1: Core tables for mandi prices, crop master, price alerts

-- Mandi master table (Indian agricultural market yards)
CREATE TABLE mandis (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(150) NOT NULL,
    state           VARCHAR(50) NOT NULL,
    district        VARCHAR(100) NOT NULL,
    latitude        DECIMAL(10, 7),
    longitude       DECIMAL(10, 7),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mandis_state ON mandis(state);
CREATE INDEX idx_mandis_district ON mandis(district);

-- Crop master database with local/regional names
CREATE TABLE crops_master (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    crop_name       VARCHAR(100) NOT NULL UNIQUE,
    category        VARCHAR(50) NOT NULL,
    scientific_name VARCHAR(200),
    unit            VARCHAR(30) NOT NULL DEFAULT 'kg',
    local_names     TEXT[] DEFAULT '{}',
    synonyms        TEXT[] DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_crops_master_name ON crops_master(crop_name);
CREATE INDEX idx_crops_master_category ON crops_master(category);
CREATE INDEX idx_crops_master_local ON crops_master USING GIN(local_names);
CREATE INDEX idx_crops_master_synonyms ON crops_master USING GIN(synonyms);

-- Daily crop price records
CREATE TABLE crop_prices (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    crop_name         VARCHAR(100) NOT NULL,
    mandi_id          UUID NOT NULL REFERENCES mandis(id),
    price_min         DECIMAL(12, 2),
    price_max         DECIMAL(12, 2),
    price_modal       DECIMAL(12, 2) NOT NULL,
    arrival_quantity   DECIMAL(12, 2),
    price_date        DATE NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(crop_name, mandi_id, price_date)
);

CREATE INDEX idx_crop_prices_crop ON crop_prices(crop_name);
CREATE INDEX idx_crop_prices_mandi ON crop_prices(mandi_id);
CREATE INDEX idx_crop_prices_date ON crop_prices(price_date DESC);
CREATE INDEX idx_crop_prices_composite ON crop_prices(crop_name, mandi_id, price_date DESC);

-- User price alert subscriptions
CREATE TABLE price_alerts (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL,
    crop_name        VARCHAR(100) NOT NULL,
    mandi_id         UUID REFERENCES mandis(id),
    threshold_price  DECIMAL(12, 2) NOT NULL,
    direction        VARCHAR(10) NOT NULL DEFAULT 'ABOVE',
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_price_alerts_user ON price_alerts(user_id);
CREATE INDEX idx_price_alerts_crop ON price_alerts(crop_name);
CREATE INDEX idx_price_alerts_active ON price_alerts(is_active) WHERE is_active = TRUE;

-- Alert trigger history
CREATE TABLE alert_history (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alert_id         UUID NOT NULL REFERENCES price_alerts(id),
    triggered_price  DECIMAL(12, 2) NOT NULL,
    mandi_name       VARCHAR(150),
    triggered_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alert_history_alert ON alert_history(alert_id);
CREATE INDEX idx_alert_history_time ON alert_history(triggered_at DESC);
