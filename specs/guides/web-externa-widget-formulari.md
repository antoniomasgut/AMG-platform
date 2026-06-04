# Guia: Widget i Formulari en Webs Externes

Quan un client ja té la seva pròpia web (WordPress, Wix, web pròpia, etc.) i no la hostegem nosaltres, podem igualment activar-hi el **chat widget** i/o el **formulari de contacte** d'AMG.

---

## Pas 1 — Registrar la web externa al portal

Des del portal AMG (com a SUPER_ADMIN o com el propi tenant):

```
POST /api/v1/hosting/tenants/{tenantId}/sites/external
Content-Type: application/x-www-form-urlencoded

domain=www.clientxyz.com
contactEmail=info@clientxyz.com          (opcional; si no, s'usa el email del tenant)
contactRedirectUrl=https://www.clientxyz.com/gracies  (opcional)
```

La resposta retorna el `siteId` (UUID). **Desa'l**, és el que s'usa a tots els snippets.

El site es crea directament amb estat `ACTIVE` — no cal revisió ni Docker.

---

## Pas 2A — Afegir el chat widget

Enganxa aquest snippet just abans del `</body>` de totes les pàgines:

```html
<script src="https://api.amgdl.com/api/v1/widget/{SITE_ID}/loader" defer></script>
```

Substitueix `{SITE_ID}` pel UUID obtingut al Pas 1.

**Prerequisits** perquè el widget aparegui:
- El tenant ha de tenir l'agent activat (Mòdul 20 — `TenantAIConfig` existent)
- El `TenantChatLink` ha de tenir `isActive = true`
- Si no hi ha agent actiu, el widget mostrarà el botó de WhatsApp si el tenant té número configurat

---

## Pas 2B — Redirigir el formulari de contacte

### Opció A — Formulari HTML estàndard

Canvia l'atribut `action` del formulari:

```html
<form action="https://api.amgdl.com/api/v1/widget/{SITE_ID}/contact" method="POST">
  <input type="text"  name="name"    placeholder="Nom">
  <input type="email" name="email"   placeholder="Email">
  <input type="tel"   name="phone"   placeholder="Telèfon" required="false">
  <textarea           name="message" placeholder="Missatge"></textarea>
  <button type="submit">Enviar</button>
</form>
```

Camps acceptats: `name`, `email`, `phone` (opcional), `message`.

Si s'ha configurat un `contactRedirectUrl` al Pas 1, després d'enviar el formulari el navegador redirigeix automàticament a aquella URL (pàgina de "gràcies"). Si no, retorna JSON `{"ok":true}`.

### Opció B — Fetch (JS) per a webs SPA/React/Vue

```javascript
async function sendForm(name, email, phone, message) {
  const params = new URLSearchParams({ name, email, phone, message });
  const res = await fetch(
    'https://api.amgdl.com/api/v1/widget/{SITE_ID}/contact',
    { method: 'POST', body: params }
  );
  const data = await res.json();
  if (data.ok) { /* mostra missatge d'èxit */ }
}
```

---

## On arriba el missatge

El sistema envia un email via Brevo a l'adreça configurada (`contactEmail` del site, o l'email del tenant si no se n'ha especificat cap).

Format del correu:
```
Assumpte: Nou missatge del formulari web — {NomTenant}

Has rebut un missatge des del formulari web de {NomTenant}:

Nom:     Joan Pérez
Email:   joan@example.com
Telèfon: 600 123 456

Missatge:
Voldria informació sobre els vostres serveis...
```

---

## Modificar la configuració posteriorment

Per canviar el `contactEmail` o el `contactRedirectUrl` d'un site existent, usa la BD directament o afegeix un endpoint `PATCH` quan sigui necessari.

Per consultar tots els sites d'un tenant (incloent EXTERNAL):
```
GET /api/v1/hosting/tenants/{tenantId}/sites
```

---

## Resum ràpid (xuleta)

| Objectiu | Acció |
|----------|-------|
| Registrar web externa | `POST .../sites/external` amb `domain` + `contactEmail` |
| Obtenir `siteId` | De la resposta del registre (camp `id`) |
| Activar widget | `<script src=".../widget/{siteId}/loader" defer>` |
| Redirigir formulari | `<form action=".../widget/{siteId}/contact" method="POST">` |
| Pàgina de gràcies | Configura `contactRedirectUrl` al registre |
| Canviar email destí | Edita `contact_email` a la taula `websites` |
