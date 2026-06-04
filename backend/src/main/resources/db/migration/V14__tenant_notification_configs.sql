CREATE TABLE IF NOT EXISTS tenant_notification_configs (
    tenant_id           UUID PRIMARY KEY,
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    tg_contact_form     BOOLEAN NOT NULL DEFAULT TRUE,
    tg_chat_widget_new  BOOLEAN NOT NULL DEFAULT TRUE,
    tg_whatsapp_new     BOOLEAN NOT NULL DEFAULT TRUE,
    tg_email_new        BOOLEAN NOT NULL DEFAULT TRUE,
    tg_lead_created     BOOLEAN NOT NULL DEFAULT FALSE,
    tg_booking          BOOLEAN NOT NULL DEFAULT TRUE,
    em_contact_form     BOOLEAN NOT NULL DEFAULT TRUE,
    em_booking          BOOLEAN NOT NULL DEFAULT TRUE,
    quiet_start         INTEGER,
    quiet_end           INTEGER,
    timezone            VARCHAR(50) NOT NULL DEFAULT 'Europe/Madrid',
    cooldown_minutes    INTEGER NOT NULL DEFAULT 0,
    updated_at          TIMESTAMPTZ
);
