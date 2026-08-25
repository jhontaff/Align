CREATE TABLE pending_actions (
                                 id             UUID PRIMARY KEY,
                                 user_id        UUID NOT NULL,
                                 tool_name      VARCHAR(100) NOT NULL,
                                 arguments_json TEXT NOT NULL,
                                 status         VARCHAR(20) NOT NULL,
                                 created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
                                 updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,

                                 CONSTRAINT fk_pending_actions_user
                                     FOREIGN KEY (user_id) REFERENCES users(id)
);
