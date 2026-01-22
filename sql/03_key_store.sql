-- Key store (must be excluded from backups in a real deployment)

DROP TABLE IF EXISTS key_store CASCADE;

CREATE TABLE key_store (
  key_id        BIGSERIAL PRIMARY KEY,
  user_id       BIGINT NOT NULL,
  purpose       TEXT NOT NULL,      -- e.g. 'name', 'email', 'phone'
  bucket_date   DATE NOT NULL,      -- time bucketing (daily)
  key_material  TEXT NOT NULL,      -- prototype only (use KMS/HSM in production)

  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  destroyed_at  TIMESTAMPTZ,

  CONSTRAINT uq_user_purpose_bucket UNIQUE(user_id, purpose, bucket_date)
);

CREATE INDEX idx_key_store_user ON key_store(user_id);
CREATE INDEX idx_key_store_user_purpose ON key_store(user_id, purpose);
