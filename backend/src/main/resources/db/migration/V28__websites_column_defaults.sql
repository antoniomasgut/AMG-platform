-- Mòdul 29: Web Hosting & Import — reforç de defaults de la taula websites (creada a V23)
ALTER TABLE websites ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE websites ALTER COLUMN storage_bytes SET DEFAULT 0;
ALTER TABLE websites ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE websites ALTER COLUMN created_at SET DEFAULT now();
ALTER TABLE websites ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE websites ALTER COLUMN updated_at SET DEFAULT now();
