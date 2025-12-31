-- Portfolios table
CREATE TABLE portfolios (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    created_date TIMESTAMP NOT NULL,
    last_updated TIMESTAMP NOT NULL,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES auth.users(id)
);

-- Accounts table
CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    base_currency VARCHAR(3) NOT NULL,
    created_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE
);

-- Assets table
CREATE TABLE assets (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    asset_type VARCHAR(50) NOT NULL,
    quantity NUMERIC(20, 8) NOT NULL,
    cost_basis_amount NUMERIC(20, 2) NOT NULL,
    cost_basis_currency VARCHAR(3) NOT NULL,
    acquired_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

-- Transactions table
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL,
    account_id UUID NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    symbol VARCHAR(20),
    quantity NUMERIC(20, 8),
    price_amount NUMERIC(20, 2),
    price_currency VARCHAR(3),
    fee_amount NUMERIC(20, 2),
    fee_currency VARCHAR(3),
    transaction_date TIMESTAMP NOT NULL,
    notes TEXT,
    created_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_transaction_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_portfolios_user_id ON portfolios(user_id);
CREATE INDEX idx_accounts_portfolio_id ON accounts(portfolio_id);
CREATE INDEX idx_assets_account_id ON assets(account_id);
CREATE INDEX idx_assets_symbol ON assets(symbol);
CREATE INDEX idx_transactions_portfolio_id ON transactions(portfolio_id);
CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_date ON transactions(transaction_date);
CREATE INDEX idx_transactions_symbol ON transactions(symbol);