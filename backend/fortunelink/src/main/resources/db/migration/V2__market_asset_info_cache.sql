CREATE TABLE market_asset_info (
    symbol VARCHAR(20) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    asset_type VARCHAR(50) NOT NULL,
    exchange VARCHAR(100),
    trading_currency VARCHAR(3) NOT NULL,
    sector VARCHAR(100),
    description TEXT,
    fetched_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_market_asset_info_expires ON market_asset_info(expires_at);