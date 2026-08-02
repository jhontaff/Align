CREATE TABLE conversation_histories (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    history_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_conversation_histories_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_conversation_histories_user
        UNIQUE (user_id)
);
