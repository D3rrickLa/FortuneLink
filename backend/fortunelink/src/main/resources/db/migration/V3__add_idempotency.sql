ALTER TABLE transactions 
    ADD COLUMN idempotency_key VARCHAR(36);

CREATE UNIQUE INDEX idx_transactions_idempotency_key 
    ON transactions (idempotency_key)
    WHERE idempotency_key IS NOT NULL;