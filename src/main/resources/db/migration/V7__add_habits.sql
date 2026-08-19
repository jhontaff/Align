CREATE TABLE habits (
                        id         UUID PRIMARY KEY,
                        user_id    UUID NOT NULL,
                        name       VARCHAR(100) NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                        CONSTRAINT fk_habits_user
                            FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE habit_completions (
                                   id           UUID PRIMARY KEY,
                                   habit_id     UUID NOT NULL,
                                   date         DATE NOT NULL,
                                   created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
                                   updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,

                                   CONSTRAINT fk_habit_completions_habit
                                       FOREIGN KEY (habit_id) REFERENCES habits(id),

                                   CONSTRAINT uq_habit_completions_habit_date
                                       UNIQUE (habit_id, date)
);

CREATE INDEX idx_habit_completions_habit_date
    ON habit_completions (habit_id, date DESC);
