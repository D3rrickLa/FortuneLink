-- Fix portfolios unique constraint
ALTER TABLE portfolios DROP CONSTRAINT portfolios_user_id_key;
-- The partial index already handles uniqueness for active portfolios


-- Concurrent check as creating an account with the same name can happen technically
ALTER TABLE accounts ADD CONSTRAINT uq_account_name_portfolio 
    UNIQUE (portfolio_id, name);