CREATE TABLE user_memories (
                               id         UUID PRIMARY KEY,
                               user_id    UUID NOT NULL,
                               content    TEXT NOT NULL,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                               CONSTRAINT fk_user_memories_user
                                   FOREIGN KEY (user_id) REFERENCES users(id)
);
