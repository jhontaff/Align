CREATE TABLE users (
                       id UUID PRIMARY KEY,

                       email VARCHAR(150) NOT NULL,
                       password VARCHAR(255) NOT NULL,

                       first_name VARCHAR(100) NOT NULL,
                       last_name VARCHAR(100) NOT NULL,

                       role VARCHAR(20) NOT NULL,
                       enabled BOOLEAN NOT NULL DEFAULT TRUE,

                       created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                       updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_user_email
    ON users(email);