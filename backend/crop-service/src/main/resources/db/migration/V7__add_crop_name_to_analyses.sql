-- Add crop_name column to crop_analyses for AI-detected crop identification
ALTER TABLE crop_analyses ADD COLUMN crop_name VARCHAR(100);

-- Add farm_crop_id to link analysis to the matched farm crop
ALTER TABLE crop_analyses ADD COLUMN farm_crop_id UUID;

CREATE INDEX idx_crop_analyses_crop_name ON crop_analyses(crop_name);
CREATE INDEX idx_crop_analyses_farm_crop ON crop_analyses(farm_crop_id);
