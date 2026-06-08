-- Mòdul 42: Meta Cloud API OAuth Onboarding
ALTER TABLE whatsapp_waba_configs
    ADD COLUMN IF NOT EXISTS meta_user_id VARCHAR(50);
