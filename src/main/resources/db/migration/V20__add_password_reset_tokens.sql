-- Recuperación de contraseña por email. Se guarda el hash del token (SHA-256),
-- nunca el token crudo: si se filtra la base, los hashes no sirven para
-- restablecer nada. El valor crudo solo viaja en el link del mail.
CREATE TABLE password_reset_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at    TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Si se borra el usuario, sus tokens se van con él. ddl-auto=validate no
    -- verifica reglas ON DELETE, así que tiene que declararse acá.
    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- El reset busca por hash: el UNIQUE da el índice para esa consulta y
    -- descarta colisiones por construcción.
    CONSTRAINT uk_password_reset_tokens_token_hash
        UNIQUE (token_hash)
);

-- Un solo token activo (no usado) por usuario a la vez, garantizado por la
-- base y no solo por el servicio: pedir un segundo reset reemplaza al
-- anterior en vez de dejar dos links vivos. validate tampoco verifica índices
-- únicos parciales.
CREATE UNIQUE INDEX uk_password_reset_tokens_active_user
    ON password_reset_tokens (user_id)
    WHERE used_at IS NULL;
