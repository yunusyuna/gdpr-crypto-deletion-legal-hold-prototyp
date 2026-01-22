-- Phase 2: Triggers that encrypt sensitive columns into users_shadow

-- 1) Trigger function: encrypt users row into shadow table
CREATE OR REPLACE FUNCTION trg_users_encrypt_to_shadow()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
  v_bucket DATE;
  v_key_id_name  BIGINT; v_key_name  TEXT;
  v_key_id_email BIGINT; v_key_email TEXT;
  v_key_id_phone BIGINT; v_key_phone TEXT;
BEGIN
  -- bucket key by current date (simple policy for prototype)
  v_bucket := CURRENT_DATE;

  -- get or create keys for each protected field
  SELECT key_id, key_material INTO v_key_id_name, v_key_name
  FROM get_or_create_key(NEW.user_id, 'name', v_bucket);

  SELECT key_id, key_material INTO v_key_id_email, v_key_email
  FROM get_or_create_key(NEW.user_id, 'email', v_bucket);

  SELECT key_id, key_material INTO v_key_id_phone, v_key_phone
  FROM get_or_create_key(NEW.user_id, 'phone', v_bucket);

  -- upsert encrypted row into users_shadow
  INSERT INTO users_shadow(
    user_id,
    full_name_enc, email_enc, phone_enc,
    key_id_name, key_id_email, key_id_phone,
    enc_version, updated_at
  )
  VALUES (
    NEW.user_id,
    sym_encrypt_nullable(NEW.full_name, v_key_name),
    sym_encrypt_nullable(NEW.email, v_key_email),
    sym_encrypt_nullable(NEW.phone, v_key_phone),
    v_key_id_name, v_key_id_email, v_key_id_phone,
    1, now()
  )
  ON CONFLICT (user_id) DO UPDATE SET
    full_name_enc = EXCLUDED.full_name_enc,
    email_enc     = EXCLUDED.email_enc,
    phone_enc     = EXCLUDED.phone_enc,
    key_id_name   = EXCLUDED.key_id_name,
    key_id_email  = EXCLUDED.key_id_email,
    key_id_phone  = EXCLUDED.key_id_phone,
    enc_version   = EXCLUDED.enc_version,
    updated_at    = now();

  RETURN NEW;
END;
$$;

-- 2) Attach triggers to users table
DROP TRIGGER IF EXISTS users_encrypt_shadow_ins ON users;
DROP TRIGGER IF EXISTS users_encrypt_shadow_upd ON users;

CREATE TRIGGER users_encrypt_shadow_ins
AFTER INSERT ON users
FOR EACH ROW
EXECUTE FUNCTION trg_users_encrypt_to_shadow();

CREATE TRIGGER users_encrypt_shadow_upd
AFTER UPDATE OF full_name, email, phone ON users
FOR EACH ROW
EXECUTE FUNCTION trg_users_encrypt_to_shadow();
