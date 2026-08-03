CREATE TABLE transactions (
                              id               UUID PRIMARY KEY,
                              user_id          UUID NOT NULL,
                              type             VARCHAR(20) NOT NULL,
                              amount           NUMERIC(15,2) NOT NULL,
                              category         VARCHAR(30) NOT NULL,
                              description      VARCHAR(255),
                              transaction_date DATE NOT NULL,
                              created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
                              updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,

                              CONSTRAINT fk_transactions_user
                                  FOREIGN KEY (user_id) REFERENCES users(id),

                              CONSTRAINT chk_transactions_amount_positive
                                  CHECK (amount > 0)
);

CREATE INDEX idx_transactions_user_date
    ON transactions (user_id, transaction_date DESC);
