-- V104: Microsoft Clarity Project ID global de plataforma
INSERT INTO system_settings (key, encrypted_value, is_secret, description, value_type, sort_order, updated_at)
VALUES ('CLARITY_ID', 'xpysgbditz', false, 'Microsoft Clarity Project ID — heatmaps i gravació de sessions per a totes les landings', 'string', 15, NOW())
ON CONFLICT (key) DO NOTHING;
