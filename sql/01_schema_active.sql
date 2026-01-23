-- Active tables (plaintext, used by the application)

DROP TABLE IF EXISTS events CASCADE;
DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users (
  user_id      BIGSERIAL PRIMARY KEY,

  -- In production, you commonly keep these NOT NULL.
  full_name    TEXT NOT NULL,

  -- Production-realistic: email is usually unique.
  email        TEXT NOT NULL UNIQUE,

  -- phone often not unique; can be NULL
  phone        TEXT,

  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE events (
  event_id     BIGSERIAL PRIMARY KEY,
  user_id      BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  action       TEXT NOT NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_user_time ON events(user_id, created_at);
