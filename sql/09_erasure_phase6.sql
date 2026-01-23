-- ============================================================
-- Phase 3 + 6: Cryptographic deletion with audit logging
-- ============================================================

CREATE OR REPLACE FUNCTION destroy_user_keys(
  p_user_id BIGINT,
  p_purpose TEXT DEFAULT NULL
) RETURNS INT
LANGUAGE plpgsql
AS $$
DECLARE
  v_count INT;
  v_hold  BOOLEAN;
BEGIN
  v_hold := is_user_on_legal_hold(p_user_id);

  -- Log the attempt
  INSERT INTO deletion_audit(
    user_id, action, reason, legal_hold_active
  ) VALUES (
    p_user_id,
    'CRYPTO_ERASE_ATTEMPT',
    COALESCE(p_purpose, 'ALL'),
    v_hold
  );

  -- Block if legal hold is active
  IF v_hold THEN
    INSERT INTO deletion_audit(
      user_id, action, reason, legal_hold_active
    ) VALUES (
      p_user_id,
      'CRYPTO_ERASE_BLOCKED',
      'Active legal hold',
      true
    );

    RAISE EXCEPTION
      'Deletion blocked: user % is under active legal hold',
      p_user_id
      USING ERRCODE = '45000';
  END IF;

  -- Perform cryptographic deletion
  UPDATE key_store
  SET
    destroyed_at = now(),
    key_material = 'DESTROYED'
  WHERE user_id = p_user_id
    AND destroyed_at IS NULL
    AND (p_purpose IS NULL OR purpose = p_purpose);

  GET DIAGNOSTICS v_count = ROW_COUNT;

  -- Log success
  INSERT INTO deletion_audit(
    user_id, action, reason, legal_hold_active, keys_affected
  ) VALUES (
    p_user_id,
    'CRYPTO_ERASE_SUCCESS',
    COALESCE(p_purpose, 'ALL'),
    false,
    v_count
  );

  RETURN v_count;
END;
$$;
