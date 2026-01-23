-- Phase 4: Restore simulation from encrypted shadow tables
-- Production-safe policy:
--  - If key exists and not destroyed -> decrypt and restore plaintext
--  - If key missing/destroyed -> restore UNIQUE, NON-PII tombstones
-- Also: suppress operational triggers during restore.

CREATE OR REPLACE FUNCTION restore_users_from_shadow()
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
  v_rows BIGINT;
BEGIN
  -- Suppress user triggers during restore (prevents key recreation and shadow rewrites)
  PERFORM set_config('session_replication_role', 'replica', true);

  INSERT INTO users(user_id, full_name, email, phone, created_at)
  SELECT
    s.user_id,

    -- full_name is NOT NULL: tombstone must be non-null
    CASE
      WHEN ks_name.destroyed_at IS NULL AND ks_name.key_material <> 'DESTROYED'
        THEN sym_decrypt_nullable(s.full_name_enc, ks_name.key_material)
      ELSE '[REDACTED user_id=' || s.user_id::text || ']'
    END AS full_name,

    -- email is UNIQUE NOT NULL: tombstone must be non-null AND UNIQUE
    CASE
      WHEN ks_email.destroyed_at IS NULL AND ks_email.key_material <> 'DESTROYED'
        THEN sym_decrypt_nullable(s.email_enc, ks_email.key_material)
      ELSE 'deleted+' || s.user_id::text || '@example.invalid'
    END AS email,

    -- phone is nullable: can be NULL on redaction
    CASE
      WHEN ks_phone.destroyed_at IS NULL AND ks_phone.key_material <> 'DESTROYED'
        THEN sym_decrypt_nullable(s.phone_enc, ks_phone.key_material)
      ELSE NULL
    END AS phone,

    now() AS created_at
  FROM users_shadow s
  LEFT JOIN key_store ks_name  ON ks_name.key_id  = s.key_id_name
  LEFT JOIN key_store ks_email ON ks_email.key_id = s.key_id_email
  LEFT JOIN key_store ks_phone ON ks_phone.key_id = s.key_id_phone
  ON CONFLICT (user_id) DO UPDATE SET
    full_name  = EXCLUDED.full_name,
    email      = EXCLUDED.email,
    phone      = EXCLUDED.phone,
    created_at = EXCLUDED.created_at;

  GET DIAGNOSTICS v_rows = ROW_COUNT;

  -- Restore trigger behavior
  PERFORM set_config('session_replication_role', 'origin', true);

  RETURN v_rows;

EXCEPTION WHEN OTHERS THEN
  -- Always restore trigger behavior even if something fails
  PERFORM set_config('session_replication_role', 'origin', true);
  RAISE;
END;
$$;
