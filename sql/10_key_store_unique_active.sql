-- Fix: allow new key creation after destruction
-- Enforce uniqueness only for ACTIVE keys (destroyed_at IS NULL)

-- 1) Drop the old constraint (if it exists)
ALTER TABLE key_store DROP CONSTRAINT IF EXISTS uq_user_purpose_bucket;

-- 2) Create a partial unique index: only one ACTIVE key per (user, purpose, bucket_date)
CREATE UNIQUE INDEX IF NOT EXISTS ux_key_store_active
ON key_store(user_id, purpose, bucket_date)
WHERE destroyed_at IS NULL;
