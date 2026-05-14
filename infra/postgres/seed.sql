-- =============================================================
-- AMG Platform — Seed de demo (entorn local)
-- Executa amb: make seed
-- ATENCIÓ: Esborra i recrea les dades de demo
-- =============================================================

-- Tenant demo
INSERT INTO tenants (id, name, slug, email, phone, address, is_active, created_at)
VALUES (
  '00000000-0000-0000-0000-000000000001',
  'Demo Client SL',
  'demo-client',
  'info@democlient.com',
  '+34 971 000 001',
  'Carrer de la Demo, 1, 07001 Palma',
  true,
  NOW()
) ON CONFLICT (id) DO NOTHING;

-- Usuari SUPER_ADMIN (password: Admin1234!)
-- Hash BCrypt de "Admin1234!"
INSERT INTO users (id, email, name, password_hash, role, tenant_id, is_active, is_blocked, failed_attempts, created_at)
VALUES (
  '00000000-0000-0000-0000-000000000010',
  'admin@amg.digital',
  'Antonio Mas',
  '$2b$12$NmUH.LcsgodoE7W0xxQlkO8oua28Im75KY5ilendxMP7KlFXuSbFC',
  'SUPER_ADMIN',
  NULL,
  true,
  false,
  0,
  NOW()
) ON CONFLICT (id) DO NOTHING;

-- Usuari CLIENT lligat al tenant demo (password: Client1234!)
INSERT INTO users (id, email, name, password_hash, role, tenant_id, is_active, is_blocked, failed_attempts, created_at)
VALUES (
  '00000000-0000-0000-0000-000000000011',
  'client@democlient.com',
  'Joan Client',
  '$2b$12$u0S/YwtdaqZc113uJ.kU7OGLkGn4TyJ8Y/0u.C/emi5h9YtGd40ua',
  'CLIENT',
  '00000000-0000-0000-0000-000000000001',
  true,
  false,
  0,
  NOW()
) ON CONFLICT (id) DO NOTHING;

-- Nota: els hashes BCrypt anteriors corresponen a "Admin1234!" i "Client1234!"
-- Canvia les contrasenyes després del primer login
