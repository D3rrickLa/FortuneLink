-- Fix portfolios unique constraint
ALTER TABLE portfolios DROP CONSTRAINT portfolios_user_id_key;
-- The partial index already handles uniqueness for active portfolios

-- Fix realized_gains column names (if they were created wrong)
ALTER TABLE realized_gains 
    RENAME COLUMN gain_loss_amount TO gain_loss_amount; -- only if needed after entity fix
-- Actually if the entity was mapping to "realized_gain_loss_amount" and the 
-- DB column is "gain_loss_amount", you fix the entity, not the DB.
-- The V1 SQL is the source of truth. Fix the entities to match V1.

-- Concurrent check as creating an account with the same name can happen technically
ALTER TABLE accounts ADD CONSTRAINT uq_account_name_portfolio 
    UNIQUE (portfolio_id, name);