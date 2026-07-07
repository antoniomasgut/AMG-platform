-- Mòdul 56 F4 · Connexió LinkedIn (només tenant propietari AMG)
CREATE TABLE IF NOT EXISTS linkedin_connections (
    tenant_id                UUID PRIMARY KEY,
    person_urn               VARCHAR(120) NOT NULL,
    display_name             VARCHAR(255),
    encrypted_access_token   TEXT NOT NULL,
    token_expires_at         TIMESTAMPTZ,
    is_active                BOOLEAN NOT NULL DEFAULT true,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now()
);
