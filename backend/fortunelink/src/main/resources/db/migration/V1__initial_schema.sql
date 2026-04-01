-- ============================================================
-- FortuneLink — V1 Initial Schema
-- Aligned to domain model v8 (ACB MVP)
--
-- Design notes:
--   - TIMESTAMPTZ everywhere: all timestamps are UTC-aware
--   - NUMERIC(20, 10) for financial amounts: 10 decimal places
--     gives enough headroom for crypto (8 dp) and FOREX (6 dp)
--   - No 'assets' table: positions are normalized per account/symbol
--   - Realized gains are append-only but owned by account lifecycle
--   - Transactions are immutable except for exclusion state
--   - auth.users FK assumes Supabase auth schema; adjust if self-hosting
-- ============================================================
 
-- ============================================================
-- PORTFOLIOS — aggregate root
-- ============================================================
CREATE TABLE portfolios (
    id                      UUID            PRIMARY KEY,
    user_id                 UUID            NOT NULL, -- one active portfolio per user (MVP)
    name                    VARCHAR(255)    NOT NULL,
    description             VARCHAR(500),
    display_currency_code   VARCHAR(3)      NOT NULL,        -- ISO-4217: CAD, USD, etc.
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMPTZ,
    deleted_by              UUID,                            -- UserId of who deleted it
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                 INTEGER         NOT NULL DEFAULT 0, -- optimistic locking
 
    CONSTRAINT fk_portfolio_user FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE,
 
    CONSTRAINT chk_portfolio_soft_delete CHECK (
        (deleted = FALSE AND deleted_at IS NULL  AND deleted_by IS NULL) OR
        (deleted = TRUE  AND deleted_at IS NOT NULL)
    )
);
 
COMMENT ON TABLE portfolios IS
    'Aggregate root. One active portfolio per user at MVP. Soft-delete via deleted flag.';
 
COMMENT ON COLUMN portfolios.display_currency_code IS
    'ISO-4217 code used to aggregate multi-currency accounts into a single display value.';
 
-- ============================================================
-- ACCOUNTS — child of portfolio
-- ============================================================
CREATE TABLE accounts (
    id                      UUID            PRIMARY KEY,
    portfolio_id            UUID            NOT NULL,
    name                    VARCHAR(255)    NOT NULL,
    account_type            VARCHAR(50)     NOT NULL,        -- AccountType enum
    base_currency_code      VARCHAR(3)      NOT NULL,        -- ISO-4217
    position_strategy       VARCHAR(30)     NOT NULL DEFAULT 'ACB',
    lifecycle_state         VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    health_status           VARCHAR(20)     NOT NULL DEFAULT 'HEALTHY',
    cash_balance_amount     NUMERIC(20, 10) NOT NULL DEFAULT 0,
    cash_balance_currency   VARCHAR(3)      NOT NULL,        -- must equal base_currency_code
    closed_date             TIMESTAMPTZ,
    created_date            TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_on         TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                 INTEGER         NOT NULL DEFAULT 0,
 
    CONSTRAINT fk_account_portfolio
        FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE,
 
    CONSTRAINT chk_account_type CHECK (
        account_type IN (
            'TFSA','RRSP','RESP','FHSA',
            'ROTH_IRA','SOLO_401K',
            'CHEQUING','SAVINGS','MARGIN',
            'TAXABLE_INVESTMENT','NON_REGISTERED_INVESTMENT'
        )
    ),
    CONSTRAINT chk_position_strategy CHECK (
        position_strategy IN ('ACB', 'FIFO', 'LIFO', 'SPECIFIC_ID')
    ),
    CONSTRAINT chk_lifecycle_state CHECK (
        lifecycle_state IN ('ACTIVE', 'REPLAYING', 'CLOSED')
    ),
    CONSTRAINT chk_health_status CHECK (
        health_status IN ('HEALTHY', 'STALE', 'ERROR')
    ),
    CONSTRAINT chk_cash_currency CHECK (
        cash_balance_currency = base_currency_code
    ),
    CONSTRAINT chk_closed_date CHECK (
        (lifecycle_state != 'CLOSED' AND closed_date IS NULL) OR
        (lifecycle_state  = 'CLOSED' AND closed_date IS NOT NULL)
    )
);
 
COMMENT ON TABLE accounts IS
    'A named sub-portfolio within a Portfolio. Holds cash and positions. '
    'lifecycle_state replaces the old is_active boolean to support REPLAYING state.';
 
COMMENT ON COLUMN accounts.position_strategy IS
    'ACB is the only supported strategy at MVP. FIFO stub exists for future use.';
 
COMMENT ON COLUMN accounts.health_status IS
    'STALE indicates a failed position recalculation. HEALTHY is the normal state.';
 
-- ============================================================
-- POSITIONS — normalized per (account, symbol)
-- Replaces the old polymorphic 'assets' table entirely.
-- ============================================================
CREATE TABLE positions (
    id                      UUID            PRIMARY KEY,
    account_id              UUID            NOT NULL,
    symbol                  VARCHAR(20)     NOT NULL,        -- AssetSymbol.symbol()
    identifier_type         VARCHAR(20)     NOT NULL,        -- 'MARKET' | 'CRYPTO' | 'CASH'
    asset_type              VARCHAR(50)     NOT NULL,        -- AssetType enum
    quantity                NUMERIC(20, 8)  NOT NULL CHECK (quantity >= 0),
    cost_basis_amount       NUMERIC(20, 10) NOT NULL,        -- AcbPosition.totalCostBasis
    cost_basis_currency     VARCHAR(3)      NOT NULL,
    first_acquired_at       TIMESTAMPTZ     NOT NULL,        -- AcbPosition.firstAcquiredAt
    last_modified_at        TIMESTAMPTZ     NOT NULL,        -- AcbPosition.lastModifiedAt
    version                 INTEGER         NOT NULL DEFAULT 0,
 
    CONSTRAINT fk_position_account
        FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
 
    -- One row per symbol per account. The mapper relies on this uniqueness
    -- to find the existing row UUID and avoid delete/insert churn on update.
    CONSTRAINT uq_position_account_symbol
        UNIQUE (account_id, symbol)
);
 
COMMENT ON TABLE positions IS
    'Current open positions. One row per (account, symbol). '
    'Rebuilt by PositionRecalculationService when transactions are added/excluded. '
    'MVP: AcbPosition only. FIFO support requires a separate tax_lots table (future).';
 
COMMENT ON COLUMN positions.identifier_type IS
    'Discriminator used by the domain mapper: MARKET (stocks/ETFs/bonds), CRYPTO, CASH.';
 
-- ============================================================
-- TRANSACTIONS — immutable ledger
-- ============================================================
CREATE TABLE transactions (
    id                      UUID            PRIMARY KEY,
    portfolio_id            UUID            NOT NULL,        -- denormalized for efficient joins
    account_id              UUID            NOT NULL,
    transaction_type        VARCHAR(50)     NOT NULL,        -- TransactionType enum
 
    -- TradeExecution (null for DEPOSIT, WITHDRAWAL, FEE, INTEREST, DIVIDEND, etc.)
    execution_symbol        VARCHAR(20),
    execution_quantity      NUMERIC(20, 8),
    execution_price_amount  NUMERIC(20, 10),
    execution_price_currency VARCHAR(3),
 
    -- Stock split ratio (null unless transaction_type = 'SPLIT')
    split_numerator         INTEGER,
    split_denominator       INTEGER,
 
    -- Net signed cash impact on the account (+deposit, -withdrawal, -buy, +sell)
    cash_delta_amount       NUMERIC(20, 10) NOT NULL,
    cash_delta_currency     VARCHAR(3)      NOT NULL,
 
    -- Metadata
    asset_type              VARCHAR(50),                     -- from TransactionMetadata
    metadata_source         VARCHAR(50)     NOT NULL DEFAULT 'MANUAL',
    additional_data         JSONB           NOT NULL DEFAULT '{}'::jsonb,
 
    -- Exclusion lifecycle (transactions are immutable except for this)
    excluded                BOOLEAN         NOT NULL DEFAULT FALSE,
    excluded_at             TIMESTAMPTZ,
    excluded_by             UUID,
    excluded_reason         TEXT,
 
    notes                   TEXT,
    occurred_at             TIMESTAMPTZ     NOT NULL,
    related_transaction_id  UUID            REFERENCES transactions(id) ON DELETE SET NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                 INTEGER         NOT NULL DEFAULT 0,
 
    CONSTRAINT fk_tx_portfolio
        FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE,
    CONSTRAINT fk_tx_account
        FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
 
    -- Split requires both ratio fields or neither
    CONSTRAINT chk_split_ratio CHECK (
        (split_numerator IS NULL AND split_denominator IS NULL) OR
        (split_numerator IS NOT NULL AND split_denominator IS NOT NULL
            AND split_numerator > 0 AND split_denominator > 0)
    ),
    -- Exclusion: either fully excluded with metadata, or not excluded at all
    CONSTRAINT chk_exclusion_state CHECK (
        (excluded = FALSE AND excluded_at IS NULL AND excluded_by IS NULL) OR
        (excluded = TRUE  AND excluded_at IS NOT NULL AND excluded_by IS NOT NULL)
    )
);
 
COMMENT ON TABLE transactions IS
    'Immutable ledger of all financial events. Only exclusion state changes post-creation. '
    'cash_delta_amount is signed: positive = cash in (SELL, DEPOSIT, DIVIDEND), '
    'negative = cash out (BUY, WITHDRAWAL, FEE).';
 
COMMENT ON COLUMN transactions.portfolio_id IS
    'Denormalized from account.portfolio_id. Avoids a join in the common '
    'findByPortfolioIdAndAccountId query used by the recalculation engine.';
 
COMMENT ON COLUMN transactions.additional_data IS
    'Free-form key/value audit pairs from TransactionMetadata.additionalData. '
    'Examples: symbol for DIVIDEND/INTEREST, feeType for standalone FEE transactions.';
 
-- ============================================================
-- TRANSACTION FEES — multi-currency fee breakdown per transaction
-- ============================================================
CREATE TABLE transaction_fees (
    id                          UUID            PRIMARY KEY,
    transaction_id              UUID            NOT NULL,
    fee_type                    VARCHAR(50)     NOT NULL,    -- FeeType enum
 
    -- The fee in the currency it was originally charged (e.g. USD commission)
    native_amount               NUMERIC(20, 10) NOT NULL CHECK (native_amount >= 0),
    native_currency             VARCHAR(3)      NOT NULL,
 
    -- The fee converted to the account's base currency (null if no conversion needed)
    account_amount              NUMERIC(20, 10),
    account_amount_currency     VARCHAR(3),
 
    -- The exchange rate used for the conversion (null if same currency)
    exchange_rate               NUMERIC(20, 10),
    rate_from_currency          VARCHAR(3),
    rate_to_currency            VARCHAR(3),
    exchange_rate_date          TIMESTAMPTZ,
 
    occurred_at                 TIMESTAMPTZ     NOT NULL,
    version                     INTEGER         NOT NULL DEFAULT 0,
 
    CONSTRAINT fk_fee_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
 
    -- If conversion fields are present, all must be present
    CONSTRAINT chk_conversion_completeness CHECK (
        (account_amount IS NULL AND account_amount_currency IS NULL
            AND exchange_rate IS NULL AND rate_from_currency IS NULL AND rate_to_currency IS NULL) OR
        (account_amount IS NOT NULL AND account_amount_currency IS NOT NULL
            AND exchange_rate IS NOT NULL AND rate_from_currency IS NOT NULL AND rate_to_currency IS NOT NULL)
    )
);
 
COMMENT ON TABLE transaction_fees IS
    'Itemized fee breakdown for a transaction. '
    'Fee.nativeAmount is the charge in its original currency. '
    'Fee.accountAmount is the converted value in account base currency (CRA ACB inclusion).';
 
-- ============================================================
-- REALIZED GAINS — append-only record of closed positions
-- ============================================================
CREATE TABLE realized_gains (
    id                          UUID            PRIMARY KEY,
    account_id                  UUID            NOT NULL,
    symbol                      VARCHAR(20)     NOT NULL,
    gain_loss_amount            NUMERIC(20, 10) NOT NULL,   -- signed: positive=gain, negative=loss
    gain_loss_currency          VARCHAR(3)      NOT NULL,
    cost_basis_sold_amount      NUMERIC(20, 10) NOT NULL,
    cost_basis_sold_currency    VARCHAR(3)      NOT NULL,
    occurred_at                 TIMESTAMPTZ     NOT NULL,
 
    CONSTRAINT fk_realized_gain_account
        FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);
 
COMMENT ON TABLE realized_gains IS
    'Capital gains/losses realized when positions are closed (SELL, ROC excess). '
    'Rebuilt from the domain on every account save. See the note on UUID stability '
    'in RealizedGainRecord — the domain needs a stable ID to avoid DELETE+INSERT churn.';
 
-- ============================================================
-- MARKET ASSET INFO — cached metadata, NOT financial data
-- ============================================================
CREATE TABLE market_asset_info (
    symbol              VARCHAR(20)     PRIMARY KEY,         -- AssetSymbol.symbol()
    name                VARCHAR(255)    NOT NULL,
    asset_type          VARCHAR(50)     NOT NULL,
    exchange            VARCHAR(100),
    trading_currency    VARCHAR(3)      NOT NULL,
    sector              VARCHAR(100),
    description         TEXT,
    fetched_at          TIMESTAMPTZ     NOT NULL,
    expires_at          TIMESTAMPTZ     NOT NULL             -- TTL managed by TransactionPurgeService
);
 
COMMENT ON TABLE market_asset_info IS
    'DB-side cache for slow-changing asset metadata (name, sector, exchange). '
    'Market quotes (prices) are NOT stored here — they live in Redis only. '
    'Purged weekly by TransactionPurgeService on a Sunday 3am cron.';
 
-- ============================================================
-- INDEXES
-- ============================================================
 
-- Portfolios: active lookup by user is the hot path
CREATE UNIQUE INDEX idx_portfolios_active_user
    ON portfolios (user_id)
    WHERE deleted = FALSE;
 
CREATE INDEX idx_portfolios_user_id
    ON portfolios (user_id);
 
-- Accounts
CREATE INDEX idx_accounts_portfolio_id
    ON accounts (portfolio_id);
 
-- Positions: the two query patterns used by PositionRecalculationService
CREATE INDEX idx_positions_account_id
    ON positions (account_id);
 
CREATE INDEX idx_positions_account_symbol
    ON positions (account_id, symbol);
 
-- Transactions: primary read patterns
CREATE INDEX idx_transactions_account_occurred
    ON transactions (account_id, occurred_at DESC);
 
CREATE INDEX idx_transactions_portfolio_account
    ON transactions (portfolio_id, account_id);
 
-- Symbol filter used by findByAccountIdAndSymbol (recalculation + history)
CREATE INDEX idx_transactions_account_symbol
    ON transactions (account_id, execution_symbol)
    WHERE execution_symbol IS NOT NULL;
 
-- Recalculation replays only non-excluded transactions
CREATE INDEX idx_transactions_active
    ON transactions (account_id, occurred_at)
    WHERE excluded = FALSE;
 
-- Purge job: find expired excluded transactions by date
CREATE INDEX idx_transactions_excluded_purge
    ON transactions (excluded_at)
    WHERE excluded = TRUE;
 
-- Transaction fees
CREATE INDEX idx_fees_transaction_id
    ON transaction_fees (transaction_id);
 
-- Realized gains: capital gains report by account + symbol
CREATE INDEX idx_realized_gains_account
    ON realized_gains (account_id);
 
CREATE INDEX idx_realized_gains_account_symbol
    ON realized_gains (account_id, symbol);
 
CREATE INDEX idx_realized_gains_occurred
    ON realized_gains (account_id, occurred_at DESC);
 
-- Market asset info: purge job
CREATE INDEX idx_market_asset_info_expires
    ON market_asset_info (expires_at);
 