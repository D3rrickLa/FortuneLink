-- ============================================================
-- V4__portfolio_valuation_snapshots.sql
-- ============================================================
CREATE TABLE IF NOT EXISTS portfolio_valuation_snapshots
(
    id                            UUID PRIMARY KEY,
    user_id                       UUID            NOT NULL,

    -- Net Worth
    total_value_amount            NUMERIC(20, 10) NOT NULL,
    total_value_currency          VARCHAR(3)      NOT NULL,

    -- Cost Basis
    total_cost_basis_amount       NUMERIC(20, 10) NOT NULL,
    total_cost_basis_currency     VARCHAR(3)      NOT NULL,

    -- Unrealized
    unrealized_gain_loss_amount   NUMERIC(20, 10) NOT NULL,
    unrealized_gain_loss_currency VARCHAR(3)      NOT NULL,
    gain_loss_percent             NUMERIC(12, 4),

    -- Cash & Invested
    total_cash_balance_amount     NUMERIC(20, 10) NOT NULL,
    total_cash_balance_currency   VARCHAR(3)      NOT NULL,
    total_invested_value_amount   NUMERIC(20, 10) NOT NULL,
    total_invested_value_currency VARCHAR(3)      NOT NULL,

    display_currency_code         VARCHAR(3)      NOT NULL,
    has_stale_data                BOOLEAN         NOT NULL DEFAULT FALSE,
    snapshot_date                 TIMESTAMPTZ     NOT NULL,
    created_at                    TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_snapshot_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- The primary access pattern: "give me 90 days of snapshots for user X"
CREATE INDEX idx_snapshots_user_date
    ON portfolio_valuation_snapshots (user_id, snapshot_date DESC);

-- Prevent duplicate snapshots on the same calendar day per user
-- (snapshotDate is truncated to midnight UTC by the service)
CREATE UNIQUE INDEX idx_snapshots_user_day
    ON portfolio_valuation_snapshots (user_id, CAST(snapshot_date AT TIME ZONE 'UTC' AS DATE));

COMMENT
ON TABLE portfolio_valuation_snapshots IS
    'Daily net worth snapshots for FIRE progress tracking. '
    'Written by NetWorthSnapshotService at 02:00 UTC. '
    'totalLiabilities is zero at MVP , field exists for Loan Management context.';
 