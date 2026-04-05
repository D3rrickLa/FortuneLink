-- Create the schema if it doesn't exist (H2 or Postgres Test Container)
CREATE SCHEMA IF NOT EXISTS auth;
CREATE TABLE IF NOT EXISTS auth.users (id UUID PRIMARY KEY, email VARCHAR(255));

-- Insert a constant UUID we can use in all tests
INSERT INTO auth.users (id, email) 
VALUES ('00000000-0000-0000-0000-000000000000', 'test@example.com')
ON CONFLICT DO NOTHING;