-- V3__align_schema_with_domain_model.sql
-- Closes the gap between the domain model and what V1 actually stored.
-- Run AFTER V2. Every ALTER is additive — no destructive changes.
 
-- ============================================================
-- ACCOUNTS: missing domain fields
-- ============================================================
 
-- AccountLifecycleState (ACTIVE | REPLAYING | CLOSED).
-- V1 used is_active BOOLEAN, which cannot represent REPLAYING.
ALTER TABLE accounts
    ADD COLUMN IF NOT EXISTS lifecycle_state VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (lifecycle_state IN ('ACTIVE', 'REPLAYING', 'CLOSED'));
 
-- Backfill from the old column so existing rows are consistent.
UPDATE accounts SET lifecycle_state = CASE WHEN is_active THEN 'ACTIVE' ELSE 'CLOSED' END;
 
-- Keep is_active for any existing queries; treat it as a derived view going forward.
-- Drop only after all read paths migrate to lifecycle_state.
 
-- HealthStatus (HEALTHY | STALE | ERROR)
ALTER TABLE accounts
    ADD COLUMN IF NOT EXISTS health_status VARCHAR(20) NOT NULL DEFAULT 'HEALTHY'
        CHECK (health_status IN ('HEALTHY', 'STALE', 'ERROR'));
 
-- PositionStrategy (ACB | FIFO | LIFO | SPECIFIC_ID)
ALTER TABLE accounts
    ADD COLUMN IF NOT EXISTS position_strategy VARCHAR(30) NOT NULL DEFAULT 'ACB'
        CHECK (position_strategy IN ('ACB', 'FIFO', 'LIFO', 'SPECIFIC_ID'));
 
-- ============================================================
-- TRANSACTIONS: missing domain fields
-- ============================================================
 
-- cashDelta — the signed net cash impact stored on every transaction.
-- Required for full-account replay (replayFullTransaction).
ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS cash_delta_amount  NUMERIC(20, 10),
    ADD COLUMN IF NOT EXISTS cash_delta_currency VARCHAR(3);
 
-- Exclusion lifecycle (replaces ad-hoc metadata handling)
ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS excluded          BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS excluded_at       TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS excluded_by       UUID,
    ADD COLUMN IF NOT EXISTS excluded_reason   TEXT;
 
-- Audit / source tracking from TransactionMetadata
ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS metadata_source   VARCHAR(50) NOT NULL DEFAULT 'MANUAL';
 
-- Stock split ratio (TransactionType.SPLIT requires Ratio on the record)
ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS split_numerator   INTEGER,
    ADD COLUMN IF NOT EXISTS split_denominator INTEGER;
 
-- Self-referential link (reversals, linked trades)
ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS related_transaction_id UUID
        REFERENCES transactions(id) ON DELETE SET NULL;
 
-- ============================================================
-- REALIZED_GAINS: new table (RealizedGainRecord on Account)
-- ============================================================
 
CREATE TABLE IF NOT EXISTS realized_gains (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      UUID        NOT NULL,
    symbol          VARCHAR(20) NOT NULL,
    realized_gain_loss_amount   NUMERIC(20, 10) NOT NULL,
    realized_gain_loss_currency VARCHAR(3)      NOT NULL,
    cost_basis_sold_amount      NUMERIC(20, 10) NOT NULL,
    cost_basis_sold_currency    VARCHAR(3)      NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_realized_gains_account FOREIGN KEY (account_id)
        REFERENCES accounts(id) ON DELETE CASCADE
);
 
CREATE INDEX IF NOT EXISTS idx_realized_gains_account ON realized_gains(account_id);
CREATE INDEX IF NOT EXISTS idx_realized_gains_symbol  ON realized_gains(account_id, symbol);
 
-- ============================================================
-- ASSETS: ACB-specific fields missing from V1
-- ============================================================
 
-- firstAcquiredAt is tracked separately from acquired_date for ACB.
-- V1 acquired_date maps to this; rename for clarity in the mapper.
-- We do NOT rename (breaking) — add a dedicated column and migrate.
ALTER TABLE assets
    ADD COLUMN IF NOT EXISTS last_modified_at TIMESTAMPTZ;
 
-- Backfill from existing interaction timestamp
UPDATE assets SET last_modified_at = last_system_interaction WHERE last_modified_at IS NULL;
 
-- ============================================================
-- INDEXES for new columns
-- ============================================================
 
CREATE INDEX IF NOT EXISTS idx_transactions_excluded
    ON transactions(account_id) WHERE excluded = FALSE;
 
CREATE INDEX IF NOT EXISTS idx_transactions_related
    ON transactions(related_transaction_id) WHERE related_transaction_id IS NOT NULL;