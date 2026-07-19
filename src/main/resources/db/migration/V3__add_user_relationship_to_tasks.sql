ALTER TABLE tasks
    ADD COLUMN user_id UUID NOT NULL;

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_user
        FOREIGN KEY (user_id)
            REFERENCES users(id);