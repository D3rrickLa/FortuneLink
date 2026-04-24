-- ============================================================
-- V4__net_worth_snapshots.sql
-- ============================================================
CREATE TABLE net_worth_snapshots (
    id                       UUID            PRIMARY KEY,
    user_id                  UUID            NOT NULL,
    total_assets_amount      NUMERIC(20, 10) NOT NULL,
    total_assets_currency    VARCHAR(3)      NOT NULL,
    total_liabilities_amount NUMERIC(20, 10) NOT NULL DEFAULT 0,
    total_liab_currency      VARCHAR(3)      NOT NULL,
    net_worth_amount         NUMERIC(20, 10) NOT NULL,
    net_worth_currency       VARCHAR(3)      NOT NULL,
    display_currency_code    VARCHAR(3)      NOT NULL,
    has_stale_data           BOOLEAN         NOT NULL DEFAULT FALSE,
    snapshot_date            TIMESTAMPTZ     NOT NULL,
    created_at               TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
 
    CONSTRAINT fk_snapshot_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
 
-- The primary access pattern: "give me 90 days of snapshots for user X"
CREATE INDEX idx_snapshots_user_date
    ON net_worth_snapshots (user_id, snapshot_date DESC);
 
-- Prevent duplicate snapshots on the same calendar day per user
-- (snapshotDate is truncated to midnight UTC by the service)
CREATE UNIQUE INDEX idx_snapshots_user_day
    ON net_worth_snapshots (user_id, CAST(snapshot_date AT TIME ZONE 'UTC' AS DATE));
 
COMMENT ON TABLE net_worth_snapshots IS
    'Daily net worth snapshots for FIRE progress tracking. '
    'Written by NetWorthSnapshotService at 02:00 UTC. '
    'totalLiabilities is zero at MVP , field exists for Loan Management context.';
 