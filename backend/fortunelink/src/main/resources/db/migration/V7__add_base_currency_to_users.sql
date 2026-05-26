-- V14__add_base_currency_to_users.sql
--
-- Adds an explicit base currency preference to the user record.
-- This replaces the hardcoded Currency.CAD fallback in ValuationApplicationService.
-- 'CAD' is a safe default, FortuneLink is Canadian-focused.
-- The column is NOT NULL to avoid null checks scattered across the codebase.

ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS base_currency VARCHAR (3) NOT NULL DEFAULT 'CAD';

-- Populate any existing rows with the default (the DEFAULT clause only fires
-- for INSERT going forward; ALTER TABLE with DEFAULT back-fills for Postgres 11+
-- using the fast-path, so this is safe on a small dataset).
-- Explicit UPDATE is left here for documentation/clarity purposes.
UPDATE public.users
SET base_currency = 'CAD'
WHERE base_currency IS NULL;

COMMENT
ON COLUMN public.users.base_currency IS
    'ISO-4217 3-letter currency code the user has chosen as their reporting currency. '
    'All cross-portfolio aggregations (ValuationApplicationService) normalise to this currency.';