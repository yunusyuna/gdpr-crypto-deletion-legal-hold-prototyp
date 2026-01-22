-- Phase 3: Cryptographic deletion (key destruction)

-- Destroy all active keys for a user (optionally limited to a purpose)
CREATE OR REPLACE FUNCTION destroy_user_keys(
  p_user_id BIGINT,
  p_purpose TEXT DEFAULT NULL
) RETURNS INT
LANGUAGE plpgsql
AS $$
DECLARE
  v_count INT;
BEGIN
  -- mark keys as destroyed and wipe key_material (prototype behavior)
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
