-- Credencial de LLM por usuario (BYOK: cada usuario trae su propia API key).
-- La key nunca se guarda en claro: encrypted_key es AES-GCM (IV + ciphertext)
-- en Base64, cifrado con align.crypto.secret.
CREATE TABLE llm_credentials (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL,
    encrypted_key TEXT NOT NULL,
    last_four     VARCHAR(4) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Si se borra el usuario, su credencial se va con él. ddl-auto=validate
    -- no verifica reglas ON DELETE, así que tiene que declararse acá.
    CONSTRAINT fk_llm_credentials_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- Una sola key por usuario, garantizado por la base y no solo por el
    -- servicio: validate tampoco verifica unique constraints.
    CONSTRAINT uk_llm_credentials_user
        UNIQUE (user_id)
);
