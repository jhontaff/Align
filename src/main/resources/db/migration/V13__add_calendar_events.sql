CREATE TABLE calendar_events (
                                 id                      UUID PRIMARY KEY,
                                 user_id                 UUID NOT NULL,
                                 title                   VARCHAR(255) NOT NULL,
                                 description             TEXT,
                                 start_at                TIMESTAMP NOT NULL,
                                 end_at                  TIMESTAMP,
                                 location                VARCHAR(255),
                                 reminder_minutes_before INTEGER,
                                 reminder_at             TIMESTAMP,
                                 reminder_sent           BOOLEAN NOT NULL DEFAULT FALSE,
                                 created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
                                 updated_at              TIMESTAMP WITH TIME ZONE NOT NULL,

                                 CONSTRAINT fk_calendar_events_user
                                     FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

                                 CONSTRAINT chk_calendar_events_end_after_start
                                     CHECK (end_at IS NULL OR end_at > start_at)
);

CREATE INDEX idx_calendar_events_user_start
    ON calendar_events (user_id, start_at);

CREATE INDEX idx_calendar_events_reminder_due
    ON calendar_events (reminder_at)
    WHERE reminder_at IS NOT NULL AND reminder_sent = FALSE;
