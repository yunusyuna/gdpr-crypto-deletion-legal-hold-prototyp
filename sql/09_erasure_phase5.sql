-- ============================================================
-- Phase 3 (updated): Cryptographic deletion with legal hold guard
-- ============================================================

CREATE OR REPLACE FUNCTION destroy_user_keys(
  p_user_id BIGINT,
  p_purpose TEXT DEFAULT NULL
) RETURNS INT
LANGUAGE plpgsql
AS $$
DECLARE
  v_count INT;
BEGIN
  -- Guard: do not allow deletion if legal hold is active
  IF is_user_on_legal_hold(p_user_id) THEN
    RAISE EXCEPTION 'Deletion blocked: user % is under active legal hold', p_user_id
      USING ERRCODE = '45000';
  END IF;

  UPDATE key_store
  SET
    destroyed_at = now(),
    key_material = 'DESTROYED'
  WHERE user_id = p_user_id
    AND destroyed_at IS NULL
    AND (p_purpose IS NULL OR purpose = p_purpose);

  GET DIAGNOSTICS v_count = ROW_COUNT;
  RETURN v_count;
END;
$$;
