-- Add Firebase OAuth columns to auth_users table
ALTER TABLE auth_users ADD COLUMN IF NOT EXISTS firebase_uid VARCHAR(128) UNIQUE;
ALTER TABLE auth_users ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(20) DEFAULT 'local';

-- Make password_hash nullable (Firebase users don't have local passwords)
ALTER TABLE auth_users ALTER COLUMN password_hash DROP NOT NULL;
