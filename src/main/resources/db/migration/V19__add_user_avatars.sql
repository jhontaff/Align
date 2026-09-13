-- Foto de perfil, en tabla aparte y no como columna de users: el filtro JWT carga
-- users en cada request autenticado, y un bytea ahí viajaría con cada llamada.
-- Los bytes solo se leen cuando se pide GET /api/users/me/avatar.
CREATE TABLE user_avatars (
    id           UUID PRIMARY KEY,
    user_id      UUID NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    data         BYTEA NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Si se borra el usuario, su foto se va con él. ddl-auto=validate no verifica
    -- reglas ON DELETE, así que tiene que declararse acá.
    CONSTRAINT fk_user_avatars_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- Una sola foto por usuario, garantizado por la base y no solo por el
    -- servicio: validate tampoco verifica unique constraints.
    CONSTRAINT uk_user_avatars_user
        UNIQUE (user_id)
);
