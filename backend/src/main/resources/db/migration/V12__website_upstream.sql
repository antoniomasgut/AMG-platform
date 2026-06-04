-- Webs complexes (CONTAINER): upstream per al proxy Nginx amb widget injection
ALTER TABLE websites
    ADD COLUMN IF NOT EXISTS upstream_container VARCHAR(100),
    ADD COLUMN IF NOT EXISTS upstream_port      INTEGER;
