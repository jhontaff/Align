
-- 1. Unicidad de email a nivel BD.
--    User declara @UniqueConstraint(name = "uk_user_email"), pero V2 solo creo un
--    CREATE INDEX no-unico. Hoy la unicidad depende solo del existsByEmail() de
--    AuthServiceImpl: dos registros concurrentes con el mismo email pasan los dos.
ALTER TABLE users
    ADD CONSTRAINT uk_user_email UNIQUE (email);


-- 2. created_at / updated_at como timestamptz NOT NULL en las dos tablas mas viejas.
--    BaseEntity mapea ambos campos como Instant (UTC) con nullable = false, y desde
--    V2 todas las tablas usan TIMESTAMP WITH TIME ZONE NOT NULL. V1 (tasks) y V4
--    (conversation_histories) quedaron con TIMESTAMP pelado y nullable.
--    El USING ... AT TIME ZONE 'UTC' interpreta el wall-clock guardado como UTC,
--    que es como Hibernate escribe un Instant en una columna sin zona.
ALTER TABLE tasks
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE conversation_histories
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at SET NOT NULL;


-- 3. Tabla example: scaffold del Spring Initializr en V1, ninguna entidad la mapea.
DROP TABLE IF EXISTS example;
