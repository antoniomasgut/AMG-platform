# Tasques pendents de producció

**Actualitzat:** 2026-07-20
**Entorn:** `ssh root@65.108.148.62` · deploy automàtic via GitHub Actions en push a `main`

---

## 1. Tràmits externs que bloquegen funcionalitat (no és codi)

Auditoria del 2026-07-20 sobre `system_settings` i configs per tenant. Les 19 claus de plataforma estan configurades (Brevo inclosa, des del 2026-07-14).

| Integració | Estat actual | Acció pendent |
|------------|--------------|---------------|
| **Stripe** | Clau configurada, mode **Test** | Completar verificació d'empresa a Stripe i substituir per claus Live (`STRIPE_API_KEY` + `STRIPE_WEBHOOK_SECRET`) |
| **Brevo** | ✅ `BREVO_API_KEY` configurada; remitent default `noreply@amgdl.com` | Confirmar domini/remitent validat al panell de Brevo |
| **WhatsApp Business (WABA)** | 0 tenants configurats (dorment, previst) | Procés d'alta a Meta quan hi hagi client que ho contracti |
| **Pàgines Meta (social)** | 0 pàgines connectades | Meta App Review (feed + DMs) i connectar la pàgina d'AMG |
| **Google OAuth** | Sense `GOOGLE_OAUTH_CLIENT_ID`/`SECRET` enlloc; 0 connexions actives | Crear credencials OAuth a Google Cloud Console (bloqueja Mòduls 40 i 54) |
| **LinkedIn** | Claus no configurades | Crear app LinkedIn + aprovació del producte «Share on LinkedIn» (Mòdul 56) |
| **Webhooks secundaris** | `GOCARDLESS_WEBHOOK_SECRET`, `HOLDED_API_KEY`, `MAILGUN_WEBHOOK_SIGNING_KEY` sense configurar | Configurar quan s'activin aquests fluxos (ara no s'usen) |

---

## 2. Client pilot: Ca na Rebecca (sector MARE_DE_DIA)

Estat a 2026-07-20:

- ✅ Tenant creat a producció (`ca-na-rebecca`)
- ✅ Landing publicada amb SSL: https://canarebecca.webs.amgdl.com
- ✅ Pressupost **BUD-2026-0013** en DRAFT: 365€ setup + 109€/mes (paquet aprovat 2026-06-30, sense F3 ni SMTP)

Passos següents (manuals):
1. Revisar i enviar el pressupost des del portal
2. Crear l'usuari d'accés per a la Rebecca
3. Post-acceptació: fitxa de configuració → pagament → activació F1 + grup de Telegram

---

## 3. Notes d'estat

- **Migracions**: des del 2026-07-20 replicables des de zero (vegeu CLAUDE.md § Notes de producció). No crear mai res a mà a la BD de producció.
- **Routing landings**: `*.webs.amgdl.com` es gestiona sol — publicar una landing genera router + certificat via manifest + agent reconciliador (cron 2 min). `amg_traefik` ja no existeix.
- **Auditoria de seguretat juny 2026**: les 31 correccions estan desplegades des de fa setmanes (històric al git log i a SECURITY-AUDIT-FULL.md).
