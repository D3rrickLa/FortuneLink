CREATE TABLE IF NOT EXISTS account_valuation_snapshots (
    id                    UUID          NOT NULL DEFAULT gen_random_uuid(),
    account_id            UUID          NOT NULL,
    snapshot_date         DATE          NOT NULL,
    total_value           NUMERIC(19,4) NOT NULL,
    total_cost_basis      NUMERIC(19,4) NOT NULL,
    unrealized_gain_loss  NUMERIC(19,4) NOT NULL,
    gain_loss_percent     NUMERIC(10,4) NOT NULL,
    cash_balance          NUMERIC(19,4) NOT NULL,
    invested_value        NUMERIC(19,4) NOT NULL,
    currency              VARCHAR(3)    NOT NULL,
    has_stale_data        BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_account_valuation_snapshots PRIMARY KEY (id),
    CONSTRAINT uq_account_snapshot_date UNIQUE (account_id, snapshot_date),
    CONSTRAINT fk_account_snapshot_account
        FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

CREATE INDEX idx_account_snapshots_account_date
    ON account_valuation_snapshots(account_id, snapshot_date DESC);