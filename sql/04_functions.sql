-- Helper function: get or create a key for (user, purpose, bucket_date)
CREATE OR REPLACE FUNCTION get_or_create_key(
  p_user_id BIGINT,
  p_purpose TEXT,
  p_bucket DATE
) RETURNS TABLE(key_id BIGINT, key_material TEXT)
LANGUAGE plpgsql
AS $$
BEGIN
  -- Try to fetch existing key
  RETURN QUERY
  SELECT ks.key_id, ks.key_material
  FROM key_store ks
  WHERE ks.user_id = p_user_id
    AND ks.purpose = p_purpose
    AND ks.bucket_date = p_bucket
    AND ks.destroyed_at IS NULL;

  IF FOUND THEN
    RETURN;
  END IF;

  -- Create a new key (prototype: random bytes base64)
  INSERT INTO key_store(user_id, purpose, bucket_date, key_material)
  VALUES (p_user_id, p_purpose, p_bucket, encode(gen_random_bytes(32), 'base64'))
  RETURNING key_store.key_id, key_store.key_material
  INTO key_id, key_material;

  RETURN NEXT;
END;
$$;

-- Helper: encrypt nullable text with symmetric key
CREATE OR REPLACE FUNCTION sym_encrypt_nullable(p_plain TEXT, p_key TEXT)
RETURNS BYTEA
LANGUAGE sql
AS $$
  SELECT CASE
    WHEN p_plain IS NULL THEN NULL
    ELSE pgp_sym_encrypt(p_plain, p_key)
  END;
$$;

-- Helper: decrypt nullable bytea with symmetric key
CREATE OR REPLACE FUNCTION sym_decrypt_nullable(p_cipher BYTEA, p_key TEXT)
RETURNS TEXT
LANGUAGE sql
AS $$
  SELECT CASE
    WHEN p_cipher IS NULL THEN NULL
    ELSE pgp_sym_decrypt(p_cipher, p_key)::TEXT
  END;
$$;
