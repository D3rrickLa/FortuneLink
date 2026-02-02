-- ============================================================================
-- Portfolio Management Bounded Context - Schema Migration
-- ============================================================================
-- This migration follows Domain-Driven Design principles:
-- - Portfolio is the Aggregate Root
-- - Account and Transaction are entities within the aggregate
-- - Denormalized fields (portfolio_id in transactions) exist for query performance
-- - Foreign keys only exist between parent-child entities in the aggregate
-- ============================================================================

-- Portfolios table (The Aggregate Root)
CREATE TABLE portfolios (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,  -- Reference to Supabase auth.users (no FK - external context)
    portfolio_name VARCHAR(255) NOT NULL,
    portfolio_currency_preference VARCHAR(3) NOT NULL,
    portfolio_description VARCHAR(255),
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0
);

-- Accounts table (Entity within Portfolio aggregate)
CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    base_currency VARCHAR(3) NOT NULL,
    cash_balance_amount NUMERIC(20, 2) NOT NULL DEFAULT 0,
    cash_balance_currency VARCHAR(3) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    closed_date TIMESTAMPTZ,
    created_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_system_interaction TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    
    -- FK to aggregate root (parent entity)
    CONSTRAINT fk_account_portfolio 
        FOREIGN KEY (portfolio_id) 
        REFERENCES portfolios(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT chk_closed_date CHECK (
        (is_active = true AND closed_date IS NULL) OR 
        (is_active = false AND closed_date IS NOT NULL)
    ),
    CONSTRAINT chk_cash_currency CHECK (cash_balance_currency = base_currency)
);

-- Assets table (Entity within Portfolio aggregate, child of Account)
CREATE TABLE assets (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    
    -- Discriminator: Maps to Java implementation classes
    identifier_type VARCHAR(50) NOT NULL, -- 'MARKET', 'CRYPTO', 'CASH'
    
    -- Polymorphic Identity
    primary_id VARCHAR(100) NOT NULL, -- "USD", "AAPL", or "BTC"
    secondary_ids JSONB NOT NULL DEFAULT '{}'::jsonb, 
    asset_type VARCHAR(50), -- STOCK, ETF, CRYPTO (NULL for CASH)
    name VARCHAR(255),
    unit_of_trade VARCHAR(50), 
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,

    -- Financial Totals
    quantity NUMERIC(20, 8) NOT NULL,
    cost_basis_amount NUMERIC(20, 2) NOT NULL,
    cost_basis_currency VARCHAR(3) NOT NULL,
    
    acquired_date TIMESTAMPTZ NOT NULL,
    last_system_interaction TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,

    -- FK to parent entity (Account)
    CONSTRAINT fk_asset_account 
        FOREIGN KEY (account_id) 
        REFERENCES accounts(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT chk_positive_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_market_crypto_logic CHECK (
        identifier_type = 'CASH' OR (asset_type IS NOT NULL AND name IS NOT NULL)
    )
);

-- Transactions table (Immutable History - Entity within Portfolio aggregate)
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    
    -- Direct relationship to parent entity (Account)
    account_id UUID NOT NULL,
    
    -- Denormalized for query performance (NO FK - derived from account.portfolio_id)
    -- This is a materialized/cached value, not a relationship
    portfolio_id UUID NOT NULL,
    
    transaction_type VARCHAR(50) NOT NULL,
    
    -- Denormalized Identifier Snapshot (point-in-time data)
    identifier_type VARCHAR(50), 
    primary_id VARCHAR(100),
    secondary_ids JSONB DEFAULT '{}'::jsonb,
    asset_type VARCHAR(50),
    display_name VARCHAR(255),
    unit_of_trade VARCHAR(50),
    metadata JSONB DEFAULT '{}'::jsonb,

    -- Transaction Values
    quantity NUMERIC(20, 8),
    price_amount NUMERIC(20, 2),
    price_currency VARCHAR(3),
    dividend_amount NUMERIC(20, 2),
    dividend_currency VARCHAR(3),
    fee_amount NUMERIC(20, 2),
    fee_currency VARCHAR(3),
    
    is_drip BOOLEAN NOT NULL DEFAULT false,
    transaction_date TIMESTAMPTZ NOT NULL,
    notes TEXT,
    created_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,

    -- Only FK to Account (immediate parent in aggregate)
    -- NO FK to portfolio - it's denormalized/cached data for queries
    CONSTRAINT fk_transaction_account 
        FOREIGN KEY (account_id) 
        REFERENCES accounts(id) 
        ON DELETE CASCADE
);

-- Transaction Fees table (Value Object / Entity associated with Transaction)
CREATE TABLE transaction_fees (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    fee_type VARCHAR(50),
    fee_amount NUMERIC(20, 2) NOT NULL,
    fee_currency VARCHAR(3) NOT NULL,
    
    -- Exchange Rate Components
    rate NUMERIC(20, 10),
    from_currency VARCHAR(3),
    to_currency VARCHAR(3),
    exchange_rate_date TIMESTAMPTZ,
    rate_source VARCHAR(100),
    
    -- Metadata
    metadata JSONB,
    fee_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description TEXT,
    version INTEGER NOT NULL DEFAULT 0,
    
    CONSTRAINT fk_transaction_fee 
        FOREIGN KEY (transaction_id) 
        REFERENCES transactions(id) 
        ON DELETE CASCADE
);

-- ============================================================================
-- Indexes for Performance
-- ============================================================================

-- Portfolio indexes
CREATE INDEX idx_portfolios_user_id ON portfolios(user_id);
CREATE UNIQUE INDEX idx_portfolios_user_id_unique 
    ON portfolios(user_id) 
    WHERE deleted = FALSE;

-- Account indexes
CREATE INDEX idx_accounts_portfolio_id ON accounts(portfolio_id);
CREATE INDEX idx_accounts_active ON accounts(portfolio_id, is_active) 
    WHERE is_active = TRUE;

-- Asset indexes
CREATE INDEX idx_assets_account_id ON assets(account_id);
CREATE INDEX idx_assets_identity ON assets(identifier_type, primary_id);
CREATE INDEX idx_assets_secondary_ids ON assets USING GIN (secondary_ids);

-- Transaction indexes (optimized for common query patterns)
CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_portfolio_date 
    ON transactions(portfolio_id, transaction_date DESC);
CREATE INDEX idx_transactions_portfolio_type 
    ON transactions(portfolio_id, transaction_type);
CREATE INDEX idx_transactions_date ON transactions(transaction_date);

-- Transaction fee indexes
CREATE INDEX idx_transaction_fees_transaction_id ON transaction_fees(transaction_id);

-- ============================================================================
-- Comments / Documentation
-- ============================================================================

COMMENT ON TABLE portfolios IS 
    'Aggregate Root: Contains all financial accounts and transaction history for a user';

COMMENT ON TABLE accounts IS 
    'Entity within Portfolio aggregate: Represents different account types (TFSA, RRSP, etc.)';

COMMENT ON TABLE assets IS 
    'Entity within Portfolio aggregate: Polymorphic storage for stocks, ETFs, crypto, and cash holdings';

COMMENT ON TABLE transactions IS 
    'Entity within Portfolio aggregate: Immutable transaction history. portfolio_id is denormalized for query performance';

COMMENT ON COLUMN transactions.portfolio_id IS 
    'Denormalized field copied from account.portfolio_id at creation time. Used for fast queries, NOT a foreign key relationship';

COMMENT ON COLUMN assets.primary_id IS 
    'Contains Ticker Symbol for stocks/ETFs, Currency Code for cash (e.g., "AAPL", "USD"), or Crypto Symbol (e.g., "BTC")';

COMMENT ON COLUMN assets.secondary_ids IS 
    'JSON array of alternative identifiers: ISIN, CUSIP, exchange-specific symbols, etc.';