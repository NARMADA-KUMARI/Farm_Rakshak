-- AI-generated lifecycle tasks for farm crops
CREATE TABLE farm_crop_tasks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    farm_crop_id    UUID NOT NULL REFERENCES farm_crops(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL,
    stage           VARCHAR(30),
    title           VARCHAR(300) NOT NULL,
    description     TEXT,
    due_date        DATE,
    day_number      INT NOT NULL DEFAULT 0,
    priority        VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fct_farm_crop_id ON farm_crop_tasks(farm_crop_id);
CREATE INDEX idx_fct_due_date ON farm_crop_tasks(due_date);
CREATE INDEX idx_fct_status ON farm_crop_tasks(status);
CREATE INDEX idx_fct_user_due ON farm_crop_tasks(user_id, due_date) WHERE status = 'PENDING';
