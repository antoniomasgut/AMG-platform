-- V16__tenant_billing_lifecycle.sql
ALTER TABLE tenants ADD COLUMN billing_start_date DATE;
ALTER TABLE tenants ADD COLUMN implementation_delivered_at TIMESTAMPTZ;
ALTER TABLE tenants ADD COLUMN onboarding_completed_at TIMESTAMPTZ;
