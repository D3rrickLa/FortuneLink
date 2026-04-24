ALTER TABLE accounts          ALTER COLUMN version TYPE BIGINT;
ALTER TABLE portfolios        ALTER COLUMN version TYPE BIGINT;
ALTER TABLE transactions      ALTER COLUMN version TYPE BIGINT;
ALTER TABLE transaction_fees  ALTER COLUMN version TYPE BIGINT;