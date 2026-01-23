-- ============================================================
-- Phase 6: Deletion Audit Log
--
-- Purpose:
--   Provide an immutable evidentiary record of cryptographic
--   deletion events for compliance, audits, and litigation.
--
-- Design principles:
--   - Append-only
--   - No plaintext personal data
--   - Timestamped and contextual
-- ============================================================

DROP TABLE IF EXISTS deletion_audit CASCADE;

CREATE TABLE deletion_audit (
  audit_id      BIGSERIAL PRIMARY KEY,

  user_id       BIGINT NOT NULL,

  action        TEXT NOT NULL
                CHECK (action IN ('CRYPTO_ERASE_ATTEMPT',
                                  'CRYPTO_ERASE_BLOCKED',
                                  'CRYPTO_ERASE_SUCCESS')),

  reason        TEXT NOT NULL,

  legal_hold_active BOOLEAN NOT NULL,

  keys_affected INT NOT NULL DEFAULT 0,

  occurred_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_deletion_audit_user_time
ON deletion_audit(user_id, occurred_at);
