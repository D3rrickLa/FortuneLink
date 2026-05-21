-- dropping columns full name and base currency form users
ALTER TABLE users DROP COLUMN IF EXISTS  full_name;
ALTER TABLE users DROP COLUMN IF EXISTS  base_currency;

-- create new table for preferences
CREATE TABLE user_preferences (
  user_id UUID PRIMARY KEY,

  base_currency VARCHAR(3) NOT NULL DEFAULT 'CAD',
  email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
  price_alerts BOOLEAN NOT NULL DEFAULT TRUE,
  date_format TEXT NOT NULL DEFAULT 'MM/DD/YYYY',

  CONSTRAINT fk_user_preferences_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE
);
