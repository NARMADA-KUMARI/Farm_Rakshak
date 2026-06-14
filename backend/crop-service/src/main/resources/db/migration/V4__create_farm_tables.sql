-- ============================================================
-- FARM MANAGEMENT MODULE — Multi-tenant farm → crop ownership
-- ============================================================

-- 1. Farms — User-owned farm entities
CREATE TABLE farms (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    farm_name       VARCHAR(150) NOT NULL,
    village         VARCHAR(100),
    district        VARCHAR(100),
    state           VARCHAR(50),
    latitude        DECIMAL(10,7),
    longitude       DECIMAL(10,7),
    total_area      DECIMAL(10,2),
    area_unit       VARCHAR(20) DEFAULT 'acres',
    soil_type       VARCHAR(50),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_farms_user_id ON farms(user_id);
CREATE INDEX idx_farms_user_active ON farms(user_id) WHERE deleted = FALSE;

-- 2. Farm Crops — Crops owned by a specific farm
CREATE TABLE farm_crops (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    farm_id             UUID NOT NULL REFERENCES farms(id),
    crop_name           VARCHAR(100) NOT NULL,
    variety             VARCHAR(100),
    sowing_date         DATE,
    expected_harvest    DATE,
    crop_stage          VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    area_allocated      DECIMAL(10,2),
    irrigation_type     VARCHAR(50),
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted             BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_farm_crops_farm_id ON farm_crops(farm_id);
CREATE INDEX idx_farm_crops_farm_active ON farm_crops(farm_id) WHERE deleted = FALSE;
CREATE INDEX idx_farm_crops_stage ON farm_crops(crop_stage);

-- 3. Constraint: crop_stage must be a valid lifecycle value
ALTER TABLE farm_crops ADD CONSTRAINT chk_crop_stage
    CHECK (crop_stage IN ('PLANNED', 'SOWN', 'GERMINATION', 'VEGETATIVE', 'FLOWERING', 'FRUITING', 'HARVEST_READY', 'HARVESTED', 'FAILED'));
