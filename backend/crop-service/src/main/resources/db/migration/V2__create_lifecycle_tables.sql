-- ============================================================
-- DYNAMIC CROP LIFECYCLE ENGINE — Schema + Seed Data
-- All lifecycle logic is data-driven. Zero hardcoded constants.
-- ============================================================

-- 1. Crop Master — Defines crop types & growth ranges
CREATE TABLE crop_master (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    crop_name           VARCHAR(100) NOT NULL UNIQUE,
    crop_category       VARCHAR(50) NOT NULL,
    avg_growth_days     INT NOT NULL,
    min_growth_days     INT NOT NULL,
    max_growth_days     INT NOT NULL,
    region              VARCHAR(100),
    seed_variety        VARCHAR(100),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. Crop Stage Master — Percentage-based lifecycle stages
CREATE TABLE crop_stage_master (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    crop_id                 UUID NOT NULL REFERENCES crop_master(id) ON DELETE CASCADE,
    stage_name              VARCHAR(100) NOT NULL,
    stage_order             INT NOT NULL,
    start_day_percentage    DECIMAL(5,2) NOT NULL,
    end_day_percentage      DECIMAL(5,2) NOT NULL,
    description             TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(crop_id, stage_order)
);
CREATE INDEX idx_stage_master_crop ON crop_stage_master(crop_id);

-- 3. User Crops — Farmer's active crops
CREATE TABLE user_crops (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                     UUID NOT NULL,
    crop_id                     UUID NOT NULL REFERENCES crop_master(id),
    sowing_date                 DATE NOT NULL,
    expected_harvest_date       DATE NOT NULL,
    current_stage_id            UUID REFERENCES crop_stage_master(id),
    land_area                   DECIMAL(10,2),
    land_area_unit              VARCHAR(20) DEFAULT 'acres',
    soil_type                   VARCHAR(50),
    irrigation_type             VARCHAR(50),
    growth_adjustment_factor    DECIMAL(4,2) DEFAULT 1.00,
    status                      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_user_crops_user ON user_crops(user_id);
CREATE INDEX idx_user_crops_status ON user_crops(status);

-- 4. Crop Stage History — Tracks stage transitions
CREATE TABLE crop_stage_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_crop_id    UUID NOT NULL REFERENCES user_crops(id) ON DELETE CASCADE,
    stage_id        UUID NOT NULL REFERENCES crop_stage_master(id),
    start_date      DATE NOT NULL,
    end_date        DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_stage_history_user_crop ON crop_stage_history(user_crop_id);

-- 5. Crop Task Templates — Admin-configurable tasks per stage
CREATE TABLE crop_task_templates (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    crop_id                 UUID NOT NULL REFERENCES crop_master(id) ON DELETE CASCADE,
    stage_id                UUID NOT NULL REFERENCES crop_stage_master(id) ON DELETE CASCADE,
    task_title              VARCHAR(200) NOT NULL,
    task_description        TEXT,
    days_after_stage_start  INT NOT NULL DEFAULT 0,
    priority                VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_task_templates_crop ON crop_task_templates(crop_id);
CREATE INDEX idx_task_templates_stage ON crop_task_templates(stage_id);

-- 6. Crop Tasks — Auto-generated tasks for user crops
CREATE TABLE crop_tasks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_crop_id    UUID NOT NULL REFERENCES user_crops(id) ON DELETE CASCADE,
    template_id     UUID REFERENCES crop_task_templates(id),
    task_title      VARCHAR(200) NOT NULL,
    task_description TEXT,
    due_date        DATE,
    priority        VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_crop_tasks_user_crop ON crop_tasks(user_crop_id);
CREATE INDEX idx_crop_tasks_status ON crop_tasks(status);

-- ============================================================
-- SEED DATA — Tomato Lifecycle
-- ============================================================

-- Tomato Master
INSERT INTO crop_master (id, crop_name, crop_category, avg_growth_days, min_growth_days, max_growth_days)
VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Tomato', 'Vegetable', 120, 90, 150);

-- Tomato Stages (percentage-based)
INSERT INTO crop_stage_master (id, crop_id, stage_name, stage_order, start_day_percentage, end_day_percentage, description) VALUES
('11000001-0000-0000-0000-000000000001', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Germination',    1,  0,  10, 'Seed sprouting and initial root development'),
('11000001-0000-0000-0000-000000000002', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Vegetative',     2, 10,  35, 'Stem and leaf growth, building plant structure'),
('11000001-0000-0000-0000-000000000003', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Flowering',      3, 35,  55, 'Flower bud formation and pollination'),
('11000001-0000-0000-0000-000000000004', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Fruiting',       4, 55,  80, 'Fruit development and maturation'),
('11000001-0000-0000-0000-000000000005', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Harvest Ready',  5, 80, 100, 'Fruits are mature and ready for harvest');

-- Tomato Task Templates
INSERT INTO crop_task_templates (crop_id, stage_id, task_title, task_description, days_after_stage_start, priority) VALUES
('a1b2c3d4-e5f6-7890-abcd-ef1234567890', '11000001-0000-0000-0000-000000000001', 'Ensure adequate moisture', 'Keep soil consistently moist but not waterlogged during germination.', 0, 'HIGH'),
('a1b2c3d4-e5f6-7890-abcd-ef1234567890', '11000001-0000-0000-0000-000000000002', 'Apply nitrogen fertilizer', 'Apply 20-20-20 NPK fertilizer at recommended dose for vegetative growth.', 10, 'HIGH'),
('a1b2c3d4-e5f6-7890-abcd-ef1234567890', '11000001-0000-0000-0000-000000000002', 'Stake plants', 'Install support stakes or cages for plant structure.', 15, 'MEDIUM'),
('a1b2c3d4-e5f6-7890-abcd-ef1234567890', '11000001-0000-0000-0000-000000000003', 'Monitor pollination', 'Check for proper flower pollination. Consider hand pollination if needed.', 5, 'MEDIUM'),
('a1b2c3d4-e5f6-7890-abcd-ef1234567890', '11000001-0000-0000-0000-000000000003', 'Apply phosphorus boost', 'Switch to high-phosphorus fertilizer to support flowering.', 3, 'HIGH'),
('a1b2c3d4-e5f6-7890-abcd-ef1234567890', '11000001-0000-0000-0000-000000000004', 'Monitor for pests', 'Inspect fruits for pest damage. Apply organic pesticide if needed.', 7, 'HIGH'),
('a1b2c3d4-e5f6-7890-abcd-ef1234567890', '11000001-0000-0000-0000-000000000004', 'Apply calcium spray', 'Prevent blossom end rot with calcium chloride foliar spray.', 5, 'MEDIUM'),
('a1b2c3d4-e5f6-7890-abcd-ef1234567890', '11000001-0000-0000-0000-000000000005', 'Begin harvest', 'Pick ripe fruits when they reach full color and slight softness.', 0, 'HIGH');

-- Additional Crop: Rice
INSERT INTO crop_master (id, crop_name, crop_category, avg_growth_days, min_growth_days, max_growth_days)
VALUES ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'Rice', 'Cereal', 140, 110, 170);

INSERT INTO crop_stage_master (crop_id, stage_name, stage_order, start_day_percentage, end_day_percentage, description) VALUES
('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'Nursery',         1,  0,  15, 'Seedling nursery and transplant preparation'),
('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'Tillering',       2, 15,  40, 'Active tiller production and root expansion'),
('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'Panicle Init',    3, 40,  60, 'Panicle initiation and booting'),
('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'Grain Filling',   4, 60,  85, 'Grain development and filling'),
('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'Maturity',        5, 85, 100, 'Grain maturity and harvest readiness');

-- Additional Crop: Wheat
INSERT INTO crop_master (id, crop_name, crop_category, avg_growth_days, min_growth_days, max_growth_days)
VALUES ('c3d4e5f6-a7b8-9012-cdef-123456789012', 'Wheat', 'Cereal', 130, 100, 160);

INSERT INTO crop_stage_master (crop_id, stage_name, stage_order, start_day_percentage, end_day_percentage, description) VALUES
('c3d4e5f6-a7b8-9012-cdef-123456789012', 'Germination',    1,  0,  10, 'Seed emergence and coleoptile growth'),
('c3d4e5f6-a7b8-9012-cdef-123456789012', 'Tillering',      2, 10,  30, 'Tiller development and crown root formation'),
('c3d4e5f6-a7b8-9012-cdef-123456789012', 'Stem Extension', 3, 30,  50, 'Internode elongation and flag leaf emergence'),
('c3d4e5f6-a7b8-9012-cdef-123456789012', 'Heading',        4, 50,  70, 'Ear emergence and anthesis'),
('c3d4e5f6-a7b8-9012-cdef-123456789012', 'Grain Filling',  5, 70, 100, 'Kernel development through physiological maturity');
