DROP INDEX IF EXISTS idx_snapshots_user_day;

CREATE UNIQUE INDEX idx_snapshots_user_day
ON portfolio_valuation_snapshots (user_id, snapshot_day);