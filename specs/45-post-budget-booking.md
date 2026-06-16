# Spec 45 — Post-Budget Booking (F3 → F2 Integration)

**Versió**: 1.0  
**Estat**: Aprovat  
**Mòdul**: 45  
**Depèn de**: Mòdul 28 (NexeLocal Service Configs), Mòdul 37 (Document Builder), Mòdul 44 (Secure Document Delivery), Booking existent (`BookingService`, `BookingToken`)

---

## 1. Problema

Quan un client accepta un pressupost (F3), el sistema notifica el tenant per Telegram però no hi ha cap acció automàtica cap al client. El pas natural és concretar quan es fa el treball — una cita, una entrega de vehicle, una visita d'inici d'obra, o una reunió de formalització.

Cada sector té un mode d'agenda diferent (F2), però el flux és uniformitzable: **acceptació de pressupost → invitació de reserva automàtica**.

---

## 2. Flux principal

```
Client accepta pressupost a /documents/view/{token}
              ↓
DocumentViewService.accept()
              ↓
  [si tenant té F2 activa + autoBookingOnAccept=true]
              ↓
BookingService.createTokenFromDocument(tenantId, recipientEmail, recipientPhone, recipientName, sourceDocumentId)
              ↓
Envia invitació de reserva al client (WhatsApp si disponible, Email sempre)
  Plantilla: BOOKING_INVITATION · idioma del tenant
  Variables: {RECIPIENT_NAME}, {BUSINESS_NAME}, {BOOKING_URL}, {DOCUMENT_NAME}, {BOOKING_LABEL}
              ↓
Telegram al tenant:
  "✅ {nom} ha acceptat el pressupost {num}.
   📅 S'ha enviat l'enllaç de reserva automàticament."
```

Si `autoBookingOnAccept=false`, el tenant pot enviar manualment des del CRM (`POST /api/v1/booking/tokens` existent).

---

## 3. Modes per sector

El `{BOOKING_LABEL}` de la plantilla i el títol de la pàgina de reserva s'adapten al mode AGENDA del tenant:

| Mode AGENDA | Sectors representatius | Label reserva | Descripció slot |
|-------------|----------------------|---------------|-----------------|
| `appointment` | Fisioterapeuta, Perruqueria, Esteticista, Veterinari | "Reserva la teva cita" | Dia + hora |
| `vehicle` | Taller mecànic | "Programa l'entrega del vehicle" | Dia + hora |
| `inspection` | Pintor, Electricista, Fontaner, Jardiner, Neteja | "Concreta el dia d'inici" | Dia + hora (slot de visita tècnica) |
| `meeting` | Gestoria | "Concerta una reunió" | Dia + hora |

La lògica de disponibilitat és la mateixa en tots els casos (`MeetingSettings`). Únicament canvien els textos.

---

## 4. Canvis de model de dades

### 4.1 `booking_tokens` (ALTER)

```sql
ALTER TABLE booking_tokens
    ADD COLUMN source_document_id UUID,     -- SecureDocumentToken.id
    ADD COLUMN recipient_phone    VARCHAR(30),
    ADD COLUMN recipient_name     VARCHAR(255),
    ADD COLUMN booking_label      VARCHAR(100);  -- label calculat en el moment de creació
```

`BookingToken` ja té `leadId`, però en el context post-pressupost el client pot no ser un Lead existent al CRM. `leadId` passa a ser nullable; `recipient_phone` i `recipient_name` cobreixen el cas sense Lead.

### 4.2 `tenant_document_preferences` (ALTER)

```sql
ALTER TABLE tenant_document_preferences
    ADD COLUMN auto_booking_on_accept BOOLEAN NOT NULL DEFAULT true;
```

---

## 5. Canvis de backend

### 5.1 `BookingToken` — camps nous

```java
@Column private UUID sourceDocumentId;
@Column private String recipientPhone;
@Column private String recipientName;
@Column private String bookingLabel;
// leadId passa a nullable
```

### 5.2 `BookingService` — nou mètode

```java
@Transactional
public BookingToken createTokenFromDocument(
        UUID tenantId,
        String recipientEmail,
        String recipientPhone,
        String recipientName,
        UUID sourceDocumentId) {

    String label = resolveBookingLabel(tenantId);  // llegeix mode AGENDA de nexe_service_configs

    var token = new BookingToken();
    token.setTenantId(tenantId);
    token.setToken(generateToken());
    token.setLeadEmail(recipientEmail);
    token.setRecipientPhone(recipientPhone);
    token.setRecipientName(recipientName);
    token.setLeadName(recipientName);   // compatibilitat pàgina existent
    token.setSourceDocumentId(sourceDocumentId);
    token.setBookingLabel(label);
    token.setExpiresAt(Instant.now().plus(Duration.ofDays(14)));
    return tokenRepo.save(token);
}

private String resolveBookingLabel(UUID tenantId) {
    // llegeix nexe_service_configs AGENDA → mode
    // retorna el label corresponent (default: "Reserva la teva cita")
}
```

### 5.3 `DocumentViewService.accept()` — extensió

Després de `tokenRepo.save(token)` i `audit(...)`:

```java
if (prefs.isAutoBookingOnAccept() && agendaIsActive(token.getTenantId())) {
    var bookingToken = bookingService.createTokenFromDocument(
        token.getTenantId(),
        token.getRecipientEmail(),
        token.getRecipientPhone(),
        token.getRecipientName(),
        token.getId()
    );
    sendBookingInvitation(token, bookingToken);
}
```

`agendaIsActive()` comprova que `nexe_service_configs` té entrada AGENDA amb `"enabled": true`.

### 5.4 `sendBookingInvitation()` (nou, a `DocumentViewService`)

Mateix patró que `SecureDocumentService.sendViaWhatsApp()` / `sendNotificationEmail()`:

- Busca plantilla `BOOKING_INVITATION` via `CommunicationTemplateService.resolveForTenant()`
- Variables: `RECIPIENT_NAME`, `BUSINESS_NAME` (nom del tenant), `BOOKING_URL`, `DOCUMENT_NAME`, `BOOKING_LABEL`
- Canal: preferència del tenant (`standardDocChannel`)
- Fallback inline si no hi ha plantilla

### 5.5 `notifyTenantAcceptance()` — text ampliat

```
✅ *{signerName}* ha acceptat el pressupost *{fileName}*.
📅 S'ha enviat l'enllaç de reserva automàticament.
```

Si `autoBookingOnAccept=false`:
```
✅ *{signerName}* ha acceptat el pressupost *{fileName}*.
👉 Envia l'enllaç de reserva des del CRM quan estiguis a punt.
```

### 5.6 `TenantDocumentPreferencesService` — camp nou

`getPreferences()` ja retorna l'entitat; afegir `autoBookingOnAccept` (default `true`).

### 5.7 `BookingController` — endpoint existent reutilitzat

`GET /api/v1/booking/tokens/{token}/info` ja retorna `TokenInfo(leadName, settings)`. Afegir `bookingLabel` a la resposta:

```java
public record TokenInfo(String leadName, MeetingSettings settings, String bookingLabel) {}
```

---

## 6. Canvis de frontend

### 6.1 Pàgina `/book/[token]` — adaptació de títol

La pàgina ja existeix. Llegeix `bookingLabel` de `TokenInfo` i el mostra en lloc del text fix "Reserva una reunió":

```tsx
<h1 className="text-xl font-bold text-white">{info.bookingLabel ?? 'Reserva una reunió'}</h1>
```

### 6.2 Preferències de tenant — camp nou

`TenantDocumentPreferencesController` exposa `autoBookingOnAccept`. Afegir el toggle al portal (secció de preferències de documents o config F2).

---

## 7. Plantilles noves (`BOOKING_INVITATION`)

8 plantilles globals (4 idiomes × 2 canals) inserides via migració:

**WhatsApp ca:**
```
Hola {RECIPIENT_NAME},

has acceptat el pressupost *{DOCUMENT_NAME}*. Gràcies!

Ara pots {BOOKING_LABEL} aquí:
{BOOKING_URL}

L'enllaç és vàlid 14 dies.
```

**Email ca** (subject: `Reserva la teva cita — {DOCUMENT_NAME}`):
```
Hola {RECIPIENT_NAME},

Hem rebut la teva acceptació del pressupost "{DOCUMENT_NAME}". Gràcies per confiar en nosaltres!

Per confirmar la data, pots {BOOKING_LABEL} a continuació:
{BOOKING_URL}

L'enllaç caduca en 14 dies. Si tens cap dubte, posa't en contacte amb nosaltres.
```

_(Equivalent en es/en/de)_

---

## 8. Migració SQL

**V61:**
```sql
ALTER TABLE booking_tokens
    ADD COLUMN source_document_id UUID,
    ADD COLUMN recipient_phone    VARCHAR(30),
    ADD COLUMN recipient_name     VARCHAR(255),
    ADD COLUMN booking_label      VARCHAR(100);

ALTER TABLE tenant_document_preferences
    ADD COLUMN auto_booking_on_accept BOOLEAN NOT NULL DEFAULT true;

-- 8 plantilles BOOKING_INVITATION (ca/es/en/de × EMAIL/WHATSAPP)
INSERT INTO communication_templates (...) VALUES (...);
```

---

## 9. Casos límit

| Cas | Comportament |
|-----|-------------|
| Tenant sense F2 activa | No s'envia invitació; Telegram indica que el tenant ha de contactar manualment |
| Tenant sense `MeetingSettings` | S'usa config per defecte (Dll-Dv 9h-18h, 45 min) |
| Client sense telèfon | Només email |
| Client sense email | Error silenciós; Telegram al tenant avisa que no s'ha pogut enviar |
| Document ja acceptat (reintent) | `accept()` llança excepció; no es creen tokens duplicats |
| `autoBookingOnAccept=false` | No s'envia res; el tenant gestiona manualment |

---

## 10. Perquè no és una nova fase

F3 → F2 és una integració entre dues fases ja contractades. No té cost addicional de setup ni mensual. La condició necessària és tenir **F2 + F3 ambdues actives**; si un tenant té F3 però no F2, el comportament és el de Mòdul 44 sense canvis (acceptació + Telegram, sense booking automàtic).

---

## 11. QA

| Cas | Resultat esperat |
|-----|-----------------|
| Accept pressupost, tenant amb F2+F3 | Client rep WhatsApp/email amb booking URL |
| Accept pressupost, tenant amb F3 però sense F2 | Cap booking; Telegram al tenant |
| Accept pressupost, `autoBookingOnAccept=false` | Cap booking; Telegram al tenant sense menció d'URL |
| Client fa booking → cita creada | Tenant rep Telegram de cita; Google Calendar actualitzat |
| Booking URL expirada (>14 dies) | Pàgina mostra "Enllaç expirat" i suggereix contactar el negoci |
| Tenant canvia `autoBookingOnAccept` a false | Pressupostos futurs no envien booking; els anteriors no s'afecten |
