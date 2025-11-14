-- Initialize database with rapidphoto_admin user
-- This runs automatically when PostgreSQL initializes

-- Create user if it doesn't exist
DO
$$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'rapidphoto_admin') THEN
      CREATE ROLE rapidphoto_admin LOGIN PASSWORD 'password' SUPERUSER;
   END IF;
END
$$;

-- Ensure password is set
ALTER USER rapidphoto_admin WITH PASSWORD 'password';

-- Grant all privileges on database
GRANT ALL PRIVILEGES ON DATABASE rapidphoto TO rapidphoto_admin;
