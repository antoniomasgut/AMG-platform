ALTER TABLE stripe_configs
    ADD COLUMN IF NOT EXISTS stripe_customer_id    VARCHAR(100),
    ADD COLUMN IF NOT EXISTS stripe_payment_method_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS pm_last_four          VARCHAR(10),
    ADD COLUMN IF NOT EXISTS pm_brand              VARCHAR(30),
    ADD COLUMN IF NOT EXISTS pm_exp_month          INTEGER,
    ADD COLUMN IF NOT EXISTS pm_exp_year           INTEGER;
