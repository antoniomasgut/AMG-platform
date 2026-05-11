-- Extensió per a UUIDs
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Extensió per a cerca de text (leads, CRM)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Rol de lectura per a backups/monitoratge
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'amg_readonly') THEN
    CREATE ROLE amg_readonly;
  END IF;
END
$$;

GRANT CONNECT ON DATABASE amg_platform TO amg_readonly;
GRANT USAGE ON SCHEMA public TO amg_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO amg_readonly;
