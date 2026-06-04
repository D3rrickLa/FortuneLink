-- ============================================================
-- V14__snapshot_day_refactor.sql
-- ============================================================

-- =========================
-- PORTFOLIO SNAPSHOTS
-- =========================

ALTER TABLE portfolio_valuation_snapshots
ADD COLUMN snapshot_day DATE;

-- backfill safely (NO timezone math needed anymore)
UPDATE portfolio_valuation_snapshots
SET snapshot_day = snapshot_date::date
WHERE snapshot_day IS NULL;

ALTER TABLE portfolio_valuation_snapshots
ALTER COLUMN snapshot_day SET NOT NULL;

-- index for queries
CREATE INDEX IF NOT EXISTS idx_portfolio_snapshots_user_day
ON portfolio_valuation_snapshots (user_id, snapshot_day DESC);

-- enforce uniqueness (this becomes your REAL business constraint)
ALTER TABLE portfolio_valuation_snapshots
ADD CONSTRAINT uq_portfolio_user_day
UNIQUE (user_id, snapshot_day);


-- =========================
-- ACCOUNT SNAPSHOTS
-- =========================

ALTER TABLE account_valuation_snapshots
ADD COLUMN snapshot_day DATE;

UPDATE account_valuation_snapshots
SET snapshot_day = snapshot_date::date
WHERE snapshot_day IS NULL;

ALTER TABLE account_valuation_snapshots
ALTER COLUMN snapshot_day SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_account_snapshots_account_day
ON account_valuation_snapshots (account_id, snapshot_day DESC);

ALTER TABLE account_valuation_snapshots
ADD CONSTRAINT uq_account_day
UNIQUE (account_id, snapshot_day);


-- =========================
-- CLEANUP (IMPORTANT)
-- =========================

-- DO NOT drop snapshot_date yet (backward compatibility)
-- DO NOT change primary keys yet (keep JPA stable)