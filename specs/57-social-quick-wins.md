# Mòdul 57 · Social Quick Wins

**Estat:** En desenvolupament
**Dependències:** Mòdul 40 (Google Workspace), Mòdul 54 (Google Reviews → Telegram), Mòdul 55 (Social Engagement), Mòdul 33 (Absence & Reschedule), Mòdul 52 (Social Publisher)

Tres funcionalitats petites d'alt valor que reutilitzen la infraestructura social ja configurada (tokens Google `business.manage`, tokens Meta, fluxos Telegram existents). Cap requereix permisos externs nous ni migracions de BD.

---

## F1 · Festius i absències F2 → horaris especials de Google Business

Quan el tenant marca `/festiu` o `/absencia` per Telegram, la plataforma publica automàticament els dies com a **tancat** al seu perfil de Google Business (special hours), perquè els clients ho vegin a Google Maps/Cerca.

### Comportament

- **Trigger:** `AbsenceRescheduleService.handleHolidayCommand` i `handleAbsenceCommand` (data única i rang).
- **Gate:** el tenant té `GoogleModuleConfig.businessEnabled=true` **i** `businessLocationId` configurat. Si no, no es fa res (silenciós).
- **No bloquejant:** un error a l'API de Google mai fa fallar la comanda; només es loga. Si té èxit, el resum de la comanda afegeix la línia `• 🗓️ Dies publicats com a tancat a Google`.

### API

`GoogleBusinessHoursService.markClosed(tenantId, startDate, endDate)` → `boolean`

1. `GET https://mybusinessbusinessinformation.googleapis.com/v1/locations/{id}?readMask=specialHours` — llegeix els períodes actuals.
2. Fusiona: conserva els períodes existents amb `startDate >= avui` que no coincideixin amb el rang nou; afegeix un període `{startDate, closed: true}` per cada dia del rang.
3. `PATCH .../locations/{id}?updateMask=specialHours` amb el resultat.

El `businessLocationId` es normalitza a `locations/{id}` (accepta valor cru o camí complet `accounts/x/locations/y`).

### QA

- Fusió: períodes passats es descarten, els futurs es conserven, el dia nou s'afegeix sense duplicats.
- Config absent o `businessEnabled=false` → retorna `false`, cap crida HTTP.
- Error HTTP → retorna `false`, la comanda Telegram respon igualment el resum normal.

---

## F2 · Flux de ressenya negativa (≤3★)

Una ressenya de 3★ o menys és un risc de reputació: l'avís ha de ser diferenciat i, si el tenant no respon, cal recordar-l'hi.

### Comportament

- **Avís diferenciat:** a `notifyNewReviews`, si `rating <= 3` el missatge Telegram usa capçalera `🚨 Ressenya negativa` amb recomanació de respondre aviat (respondre bé una crítica és visible per a tothom). Botons: `✍️ Respondre` + `🤖 Suggerir resposta` (existents). El botó `📢 Compartir` només apareix per 5★ (comportament actual, sense canvi).
- **Recordatori 48h:** scheduler diari a les 09:45 (`remindUnansweredNegativeReviews`): ressenyes amb `rating <= 3`, `reply IS NULL` i `notifiedAt < ara-48h` → un únic recordatori Telegram amb els mateixos botons. Dedupe via `followup_logs` amb `type=NEGREV_48H`, `entityId=review.id`.

### QA

- Ressenya rating 2 → missatge amb `🚨` i sense botó de compartir.
- Recordatori: només s'envia un cop (segona execució → dedupe), i mai si ja té `reply`.

---

## F3 · Post mensual de social proof

El dia 1 de cada mes, els tenants amb el toggle `auto_post_reviews` actiu reben una proposta de post amb la seva valoració agregada («⭐ 4,9/5 amb 32 ressenyes») per publicar a xarxes amb un clic.

### Comportament

- **Scheduler:** `SocialProofPostScheduler`, dia 1 de cada mes a les 11:00.
- **Gate:** config `SOCIAL_PUBLISHER` existent + toggle `auto_post_reviews=true` + xat Telegram vinculat + **mínim 5 ressenyes** + **mitjana ≥ 4.0** (no es presumeix d'una mitjana fluixa).
- **Flux:** genera el caption amb IA (`SocialContentGeneratorService.generateCaption`, fallback determinista si la IA falla) → crea `SocialPost` DRAFT (FACEBOOK/TEXT) → envia previsualització Telegram amb botó `✅ Publicar` (`callback_data=gpub:<postId>`, flux de publicació existent del Mòdul 55).
- **Dedupe:** `followup_logs` amb `type=SOCPROOF_yyyyMM`, `entityId=tenantId` (un post proposat per tenant i mes).

### QA

- Menys de 5 ressenyes o mitjana < 4.0 → no s'envia res.
- IA caiguda → caption determinista, el flux no falla.
- Segona execució el mateix mes → dedupe, cap missatge.

---

## Seguretat

- Cap endpoint HTTP nou: tot són schedulers interns i comandes Telegram ja autenticades (secret token del webhook).
- Els tokens Google es continuen llegint via `GoogleTokenService` (xifrats al Vault, refresh automàtic).
- El `callback_data` de Telegram només porta identificadors (reviewId/postId); la resolució del tenant sempre es fa via `TenantChatLink` del chatId, mai des del payload.
