-- Initialize the database with required extensions
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create shared sequence for entity IDs
CREATE SEQUENCE IF NOT EXISTS base_sequence START WITH 1 INCREMENT BY 1;

-- Grant permissions to the application user
GRANT ALL PRIVILEGES ON DATABASE clinicos_db TO clinicos_user;
GRANT ALL ON SCHEMA public TO clinicos_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO clinicos_user;

