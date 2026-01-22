-- Shadow table (encrypted copy that is backed up)

DROP TABLE IF EXISTS users_shadow CASCADE;

CREATE TABLE users_shadow (
  user_id         BIGINT PRIMARY KEY,

  -- encrypted fields
  full_name_enc   BYTEA,
  email_enc       BYTEA,
  phone_enc       BYTEA,

  -- key references (which key was used)
  key_id_name     BIGINT,
  key_id_email    BIGINT,
  key_id_phone    BIGINT,

  enc_version     INT NOT NULL DEFAULT 1,
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_shadow_updated ON users_shadow(updated_at);
