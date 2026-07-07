# Spec 56 · Social Capabilities (extensió dels Mòduls 52/54/55)

**Estat:** 🔧 En construcció
**Depèn de:** Mòdul 25 (Omnichannel Inbox), Mòdul 42 (Meta OAuth), Mòdul 52 (Social Publisher), Mòdul 54/55 (Reviews & Engagement), Mòdul 20 (Agents Telegram)

---

## 1. Objectiu

Ampliar el que es pot fer amb les xarxes, per a **tots els tenants** (inclòs el tenant propietari AMG via "El meu negoci"), més **LinkedIn només per a AMG**.

## 2. Features

### F1 · Respostes IA suggerides (ressenyes i comentaris) ✅
- A l'avís d'una ressenya (M54) o comentari (M55 F1), botó extra **🤖 Suggerir resposta**.
- Callback genera un esborrany amb `AIProviderRouter` (to proper, idioma del text original, sense placeholders) i el mostra amb botó **✅ Publicar** — mai s'auto-publica sense confirmació.
- Reutilitza els fluxos de publicació existents (`replyToReview`, `replyToComment`).
- Esborrany desat a Redis (TTL 15 min). Per a comentaris, el text del comentari es desa a Redis en notificar (`cmt:text:<id>`, TTL 1h) perquè no es persisteix.

### F2 · DMs Instagram + Messenger → Inbox
- Webhook Meta camp `messages` (IG) i `messaging` (Messenger) → crear/actualitzar conversa al Mòdul 25 Omnichannel Inbox.
- Resposta des de l'Inbox via Graph API (`POST /{page}/messages`).
- Requereix permisos `instagram_manage_messages` / `pages_messaging` (App Review Meta).

### F3 · Sol·licitud automàtica de ressenyes
- En tancar un servei (F2/F3), enviar WhatsApp/Email al client amb enllaç per deixar ressenya a Google.
- Reutilitza plantilles de comunicació (Mòdul 43) + booking/document lifecycle.
- Toggle per tenant; cooldown per no spamejar el mateix client.

### F4 · LinkedIn (només tenant AMG)
- Nou canal del Social Publisher, actiu només per al tenant propietari (`isOwner`).
- OAuth `w_member_social` (perfil personal — assolible) com a primera fase; pàgina d'empresa (`w_organization_social`) requereix Marketing Developer Platform de LinkedIn (fase posterior).
- Publicació via `POST /v2/ugcPosts` o Posts API.

## 3. Ordre d'implementació
1. F1 Respostes IA suggerides (extén M54/M55, risc baix) ✅ — botons `grevai:`/`grevpub:` (ressenyes) i `cmtai:`/`cmtpub:` (comentaris); esborrany IA a Redis, publicació sempre amb confirmació.
2. F3 Sol·licitud automàtica de ressenyes (reutilitza M43)
3. F2 DMs → Inbox (requereix App Review Meta)
4. F4 LinkedIn AMG (nou OAuth)

## 4. Notes de producció
- F2/F4 requereixen aprovacions/permisos externs (Meta App Review, LinkedIn MDP) — el codi queda llest però l'activació real depèn d'això.
