CREATE TABLE push_subscriptions (
                                    id         UUID PRIMARY KEY,
                                    user_id    UUID NOT NULL,
                                    endpoint   TEXT NOT NULL UNIQUE,
                                    p256dh     VARCHAR(255) NOT NULL,
                                    auth       VARCHAR(255) NOT NULL,
                                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                    CONSTRAINT fk_push_subscriptions_user
                                        FOREIGN KEY (user_id) REFERENCES users(id)
);
