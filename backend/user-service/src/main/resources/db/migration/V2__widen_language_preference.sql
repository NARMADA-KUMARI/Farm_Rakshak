-- Widen language_preference to support full language names (e.g. "Marathi", "Hindi")
ALTER TABLE user_profiles ALTER COLUMN language_preference TYPE VARCHAR(30);
ALTER TABLE user_profiles ALTER COLUMN language_preference DROP NOT NULL;
ALTER TABLE user_profiles ALTER COLUMN language_preference SET DEFAULT 'en';
