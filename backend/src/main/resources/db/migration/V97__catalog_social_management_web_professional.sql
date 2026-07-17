-- Serveis nous al catàleg: gestió mensual xarxes socials (3 nivells) + Web Professional (2 mides)
INSERT INTO catalog_services (id, slug, name, description, type, cost, sale_price, monthly_price, is_addon, phase_id, profile_id, sort_order, version, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'social-management-basic',   'Gestió Xarxes Bàsica',   '4 publicacions/mes, 1-2 xarxes, contingut IA en català',                                       'OTHER',   50.00,   0.00,  69.00, true, NULL, NULL, NULL, 0, NOW(), NOW()),
    (gen_random_uuid(), 'social-management-gestio',  'Gestió Xarxes Gestió',   '8 publicacions/mes, multi-xarxa (IG+FB+Google), gestió comentaris/DMs, informe mensual',        'OTHER',  100.00,   0.00, 139.00, true, NULL, NULL, NULL, 0, NOW(), NOW()),
    (gen_random_uuid(), 'social-management-premium', 'Gestió Xarxes Premium',  '12 publicacions/mes, totes les xarxes + campanyes estacionals',                                  'OTHER',  150.00,   0.00, 199.00, true, NULL, NULL, NULL, 0, NOW(), NOW()),
    (gen_random_uuid(), 'web-professional-standard', 'Web Professional Estàndard', 'Web a mida en català, multi-secció, contingut redactat, SEO, formulari + WhatsApp. Sense agent IA.', 'LANDING', 150.00, 390.00,  15.00, true, NULL, NULL, NULL, 0, NOW(), NOW()),
    (gen_random_uuid(), 'web-professional-plus',     'Web Professional Plus',   'Multi-pàgina (5-8 pàgines), més contingut, galeria, SEO treballat. Sense agent IA.',            'LANDING', 200.00, 590.00,  15.00, true, NULL, NULL, NULL, 0, NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;
