# Spec 42 — Meta Cloud API OAuth Onboarding (per-tenant WABA connect)

## Objectiu

Permetre que cada tenant connecti el seu compte WhatsApp Business (WABA) directament des del portal, sense intervenció manual de l'administrador. Utilitza el flux OAuth de Meta (Facebook Login for Business).

---

## Model de facturació: dues opcions

### Opció B — Tenant posa el seu WABA (recomanada ara) ✅

El tenant connecta el seu propi WABA via OAuth. **Meta factura al tenant directament** (al seu Facebook Business Manager). AMG no veu la factura de Meta dels clients.

- No requereix permisos especials de Meta
- El tenant gestiona el seu pressupost Meta
- AMG cobra la quota de plataforma (F1-F5) independentment del consum Meta
- **Infraestructura de tracking ja existent:** `channel_usage_logs` registra tots els missatges enviats per cada tenant — preparada per a quan es necessiti (vegeu Opció A futura)

### Opció A — AMG posa els WABAs (futura, quan es sigui BSP/Tech Provider)

AMG crea tots els WABAs sota el seu Facebook Business Manager. **Meta factura a AMG** per la suma de tots els tenants. AMG repercuteix el cost als tenants amb markup.

- Requereix registre com a **Meta Tech Provider** (formulari + aprovació, pot trigar setmanes)
- AMG té control total i pot fer markup per conversa
- **Threshold per activar:** quan la plataforma superi **20 tenants amb WhatsApp actiu**
- Quan arribi el moment: `monthly_message_budget` + `overage_message_increment` (ja implementats al Mòdul 41) s'activen per controlar i auto-incrementar quotes per tenant

**Arquitectura Opció A (referència futura):**
```
Meta Cloud API
      ↓
AMG WhatsApp Gateway (routing multi-tenant)
      ↓  ↓  ↓
Tenant A  Tenant B  Tenant C
(WABA A)  (WABA B)  (WABA C)  ← tots sota BM d'AMG
```

---

## Prerequisits SaaS (1 sola vegada, per l'administrador AMG)

1. Crear una **Meta Developer App** de tipus **Business** a developers.facebook.com
2. Afegir el producte **WhatsApp Cloud API**
3. Configurar el **Webhook global** apuntant a `https://api.amgdl.com/webhooks/meta/whatsapp`
   - Esdeveniments: `messages`, `message_status`, `account_update`
4. Afegir les variables de sistema:
   - `META_APP_ID` — ID de l'app Meta
   - `META_APP_SECRET` — Secret de l'app (ja existent per a HMAC de webhooks)
   - `META_SYSTEM_USER_TOKEN` — Token del System User amb permisos globals (per enviar missatges des de qualsevol WABA)

---

## Flux d'onboarding per tenant (Opció B)

```
Portal tenant: Integracions → WhatsApp → "Connecta WhatsApp Business"
  ↓
GET /api/v1/meta/whatsapp/connect (backend genera state + redirect URL)
  ↓
Redirecció a facebook.com/dialog/oauth?
  client_id=META_APP_ID
  &redirect_uri=https://api.amgdl.com/oauth/meta/whatsapp/callback
  &scope=whatsapp_business_management,whatsapp_business_messaging,business_management
  &state={JWT signat amb tenantId}
  ↓
Usuari autoritza → Meta callback
  ↓
GET /oauth/meta/whatsapp/callback?code=...&state=...
  ↓
Backend: intercanvi code → access_token (POST graph.facebook.com/oauth/access_token)
  ↓
Backend: GET graph.facebook.com/me/accounts → llista WABA
  ↓
Si 1 WABA: auto-selecciona. Si N: retorna llista al frontend per seleccionar
  ↓
Backend: GET graph.facebook.com/{waba_id}/phone_numbers → llista números
  ↓
Frontend: usuari selecciona número → POST /api/v1/meta/whatsapp/confirm
  ↓
Backend: guarda wabaId + phoneNumberId + access_token (encriptat AES-256) a whatsapp_waba_configs
  ↓
Backend: subscriu el número al webhook global de l'app
```

---

## Regles de Meta a tenir en compte

- **Finestra 24h:** dins la finestra (usuari ha escrit en les darreres 24h) → missatges lliures. Fora → només templates aprovats per Meta.
- **Templates:** cal gestionar-los i demanar aprovació a Meta per idioma.
- **Rate limits:** per número, per tenant i per app.
- **Preu per conversa** (referència UE, juny 2025):

| Tipus | Preu aprox. |
|-------|-------------|
| Servei (usuari inicia) | Gratuït fins 1.000/mes per WABA, ~0,04€ després |
| Utilitat (recordatoris, confirmacions) | ~0,06€/conversa |
| Marketing (promocions) | ~0,11€/conversa |
| Autenticació (OTPs) | ~0,04€/conversa |

Una "conversa" = finestra de 24h, **no per missatge individual.**

---

## Entitat existent (Mòdul 27): `WhatsappWabaConfig`

Ja té: `tenantId`, `wabaId`, `phoneNumberId`, `accessToken` (encriptat), `webhookStatus`

Cal afegir: `metaUserId` (per revocar permisos), `connectedAt`

---

## Endpoints nous

| Mètode | Path | Descripció |
|--------|------|------------|
| GET | `/api/v1/meta/whatsapp/connect` | Genera URL OAuth i state JWT |
| GET | `/oauth/meta/whatsapp/callback` | Processa callback Meta |
| POST | `/api/v1/meta/whatsapp/confirm` | Confirma selecció de número |
| DELETE | `/api/v1/meta/whatsapp/disconnect` | Revoca permisos i neteja config |

---

## Seguretat

- El `state` és un JWT de curta durada (10 min) signat amb `JWT_SECRET`, conté `tenantId`
- El `access_token` es guarda xifrat amb AES-256 (Jasypt)
- Validació HMAC del webhook existent (Mòdul 27) segueix funcionant sense canvis
- No s'emmagatzemen refresh tokens de Meta; el System User Token és l'alternativa per a enviaments

---

## Notes d'implementació

- El System User Token no caduca, però l'`access_token` d'usuari sí (60 dies). Usar el System User Token per enviar i guardar l'accés del tenant només per a gestió de WABA.
- Subscripció al webhook: `POST /{phone-number-id}/subscribed_apps` amb el token del sistema.
- Entorn de proves: Meta proporciona un test phone number gratuït.

---

## Prioritat

**Baixa-Mitjana.** La connexió manual (Mòdul 27) ja cobreix el cas d'ús actual. Aquest mòdul millora l'experiència d'onboarding però no és bloquejant. Implementar quan hi hagi demanda real de tenants que vulguin auto-connectar-se.
