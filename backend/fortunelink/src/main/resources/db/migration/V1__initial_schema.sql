/* V1__Create_Portfolio_Management_Schema.sql
  
  Notes:
  - Uses TIMESTAMPTZ for global consistency.
  - JSONB columns default to '{}' to prevent null-pointer issues in Java Records.
  - primary_id serves as the 'id' for Cash, Ticker for Stocks, and Address/ID for Crypto.
*/

-- Portfolios table (The Aggregate Root)
CREATE TABLE portfolios (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    portfolio_currency_preference VARCHAR(3) NOT NULL,
    created_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE
);

-- Accounts table
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
    CONSTRAINT fk_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE,
    CONSTRAINT chk_closed_date CHECK (
        (is_active = true AND closed_date IS NULL) OR 
        (is_active = false AND closed_date IS NOT NULL)
    ),
    CONSTRAINT chk_cash_currency CHECK (cash_balance_currency = base_currency)
);

-- Assets table (Polymorphic Storage)
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

    CONSTRAINT fk_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT chk_positive_quantity CHECK (quantity >= 0),
    
    -- Specific Validation Rules
    CONSTRAINT chk_market_crypto_logic CHECK (
        identifier_type = 'CASH' OR (asset_type IS NOT NULL AND name IS NOT NULL)
    )
);

-- Transactions table (Immutable History)
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL,
    account_id UUID NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    
    -- Denormalized Identifier Snapshot
    identifier_type VARCHAR(50), 
    primary_id VARCHAR(100),
    secondary_ids JSONB DEFAULT '{}'::jsonb,
    asset_type VARCHAR(50),
    name VARCHAR(255),
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

    CONSTRAINT fk_transaction_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

-- Separate Fee breakdown for complex transactions
CREATE TABLE transaction_fees (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    fee_amount NUMERIC(20, 2) NOT NULL,
    fee_currency VARCHAR(3) NOT NULL,
    fee_type VARCHAR(50),
    description TEXT,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_transaction_fee FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
);

-- Performance Indexes
CREATE INDEX idx_portfolios_user_id ON portfolios(user_id);
CREATE INDEX idx_accounts_portfolio_id ON accounts(portfolio_id);
CREATE INDEX idx_assets_account_id ON assets(account_id);
CREATE INDEX idx_assets_identity ON assets(identifier_type, primary_id);
CREATE INDEX idx_assets_secondary_ids ON assets USING GIN (secondary_ids);
CREATE INDEX idx_transactions_portfolio_date ON transactions(portfolio_id, transaction_date DESC);
CREATE INDEX idx_transactions_account_id ON transactions(account_id);

-- Documentation
COMMENT ON TABLE assets IS 'Stores polymorphic asset holdings. primary_id contains Ticker for Stocks or ISO Currency Code for Cash.';