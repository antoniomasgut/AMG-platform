-- P46: biblioteca d'hashtags per tenant (desats per reutilitzar als posts d'Instagram)
ALTER TABLE social_meta_configs
    ADD COLUMN IF NOT EXISTS hashtag_presets TEXT;
