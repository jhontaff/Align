-- V1__init.sql: initial migration for Align project
-- Flyway will auto-create flyway_schema_history; this creates an example table

CREATE TABLE IF NOT EXISTS example (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
);

CREATE TABLE tasks (
                       id UUID PRIMARY KEY,
                       title VARCHAR(255) NOT NULL,
                       description TEXT,
                       created_at TIMESTAMP,
                       updated_at TIMESTAMP,
                       priority VARCHAR(50) NOT NULL,
                       status VARCHAR(50) NOT NULL,
                       due_date DATE
);
