ALTER TABLE user_preferences
ADD COLUMN time_zone VARCHAR(50) NOT NULL DEFAULT 'America/Toronto';

-- V16__remove_snapshot_date

-- Drop indexes depending on snapshot_date (if any exist)
DROP INDEX IF EXISTS idx_account_snapshots_account_date;
DROP INDEX IF EXISTS idx_portfolio_snapshots_user_date;

-- Drop constraint if still tied to snapshot_date
ALTER TABLE account_valuation_snapshots
DROP CONSTRAINT IF EXISTS uq_account_snapshot_date;

ALTER TABLE portfolio_valuation_snapshots
DROP CONSTRAINT IF EXISTS uq_portfolio_user_date;

-- Drop column
ALTER TABLE account_valuation_snapshots
DROP COLUMN IF EXISTS snapshot_date;

ALTER TABLE portfolio_valuation_snapshots
DROP COLUMN IF EXISTS snapshot_date;