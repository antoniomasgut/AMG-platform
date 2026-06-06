-- Modul 40 — Google Workspace Integration (Multi-Tenant OAuth)

CREATE TABLE google_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    google_account_email VARCHAR(255) NOT NULL,
    google_user_id VARCHAR(255) NOT NULL,
    encrypted_access_token TEXT NOT NULL,
    encrypted_refresh_token TEXT NOT NULL,
    token_expires_at TIMESTAMPTZ NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_google_connections_tenant ON google_connections(tenant_id);
CREATE INDEX idx_google_connections_active ON google_connections(is_active);

CREATE TABLE google_module_configs (
    tenant_id UUID NOT NULL PRIMARY KEY REFERENCES tenants(id),
    drive_enabled BOOLEAN NOT NULL DEFAULT false,
    gmail_enabled BOOLEAN NOT NULL DEFAULT false,
    calendar_enabled BOOLEAN NOT NULL DEFAULT false,
    sheets_enabled BOOLEAN NOT NULL DEFAULT false,
    drive_folder_id VARCHAR(255),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE oauth_states (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    state_token VARCHAR(64) NOT NULL UNIQUE,
    redirect_uri VARCHAR(500) NOT NULL,
    requested_scopes TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_oauth_states_token ON oauth_states(state_token);
