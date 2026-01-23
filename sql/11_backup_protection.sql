-- ============================================================
-- Phase 5+ : Backup protection under legal hold
--
-- Purpose:
--   When a user is under active legal hold, identify backups
--   that contain that user's data and mark ONLY those backups
--   as protected (do not delete).
--
-- Important:
--   This does NOT resurrect deleted data. It only prevents
--   deletion of impacted backups.
-- ============================================================

-- 1) Add protection columns to backup_runs (idempotent)
ALTER TABLE backup_runs
ADD COLUMN IF NOT EXISTS protected_by_hold BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE backup_runs
ADD COLUMN IF NOT EXISTS protected_reason TEXT;

ALTER TABLE backup_runs
ADD COLUMN IF NOT EXISTS protected_at TIMESTAMPTZ;

-- 2) Optional: protection audit (recommended)
CREATE TABLE IF NOT EXISTS backup_protection_audit (
  audit_id     BIGSERIAL PRIMARY KEY,
  user_id      BIGINT NOT NULL,
  hold_id      BIGINT,
  backup_id    BIGINT NOT NULL,
  action       TEXT NOT NULL CHECK (action IN ('PROTECT')),
  reason       TEXT NOT NULL,
  occurred_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_backup_protection_audit_user_time
ON backup_protection_audit(user_id, occurred_at);


-- 3) Function: protect backups for a held user and return protected backups
CREATE OR REPLACE FUNCTION protect_backups_for_legal_hold(p_user_id BIGINT)
RETURNS TABLE(backup_id BIGINT, backup_type TEXT, started_at TIMESTAMPTZ, ended_at TIMESTAMPTZ, protected_reason TEXT)
LANGUAGE plpgsql
AS $$
DECLARE
  v_hold_id BIGINT;
  v_reason  TEXT;
BEGIN
  -- Ensure there is an active legal hold
  SELECT hold_id, hold_reason
  INTO v_hold_id, v_reason
  FROM legal_holds
  WHERE user_id = p_user_id
    AND released_at IS NULL
  ORDER BY created_at DESC
  LIMIT 1;

  IF v_hold_id IS NULL THEN
    RAISE EXCEPTION 'No active legal hold for user %', p_user_id
      USING ERRCODE = '45000';
  END IF;

  -- Protect ONLY backups that contain this user
  UPDATE backup_runs b
  SET
    protected_by_hold = true,
    protected_reason  = 'Legal hold ' || v_hold_id::text || ' for user_id=' || p_user_id::text || ' (' || v_reason || ')',
    protected_at      = now()
  FROM backup_user_index bi
  WHERE bi.backup_id = b.backup_id
    AND bi.user_id = p_user_id;

  -- Write protection audit entries for each impacted backup
  INSERT INTO backup_protection_audit(user_id, hold_id, backup_id, action, reason)
  SELECT
    p_user_id,
    v_hold_id,
    bi.backup_id,
    'PROTECT',
    'Marked backup as protected due to active legal hold'
  FROM backup_user_index bi
  WHERE bi.user_id = p_user_id
  ON CONFLICT DO NOTHING;

  -- Return the protected backups
  RETURN QUERY
  SELECT
    b.backup_id,
    b.backup_type,
    b.started_at,
    b.ended_at,
    b.protected_reason
  FROM backup_runs b
  JOIN backup_user_index bi ON bi.backup_id = b.backup_id
  WHERE bi.user_id = p_user_id
  ORDER BY b.backup_id;
END;
$$;
