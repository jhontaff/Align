-- V1__init.sql: initial migration for Align project
-- Flyway will auto-create flyway_schema_history; this creates an example table

CREATE TABLE IF NOT EXISTS example (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
);
