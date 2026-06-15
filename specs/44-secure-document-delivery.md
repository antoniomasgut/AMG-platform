# Mòdul 44 — Secure Document Delivery (Lliurament Segur de Documents)

> **Versió:** 1.0
> **Data:** 2026-06-15
> **Estat:** Esborrany
> **Dependències:** Mòdul 37 (Document Builder), Mòdul 38 (Tenant Storage), Mòdul 43 (Communication Templates), Mòdul 01 (Auth)

---

## 1. Objectiu

Proporcionar un mecanisme segur per lliurar documents als clients finals sense enviar el fitxer directament per email. El sistema envia una **notificació per email amb un link temporal i autenticat** que permet descarregar el document des d'un endpoint protegit.

Obligatori per a documents de categoria **SENSITIVE** (dades de salut, dades legals). Recomanat per a documents de categoria **STANDARD** (factures, pressupostos).

---

## 2. Classificació de sensibilitat

| Nivell | Tipus de document | Fonament RGPD | TTL | Descàrregues |
|--------|------------------|---------------|-----|--------------|
| `STANDARD` | Factures, pressupostos, albarans, contractes comercials | Art. 6 — Base legítima | 7 dies | Il·limitades |
| `SENSITIVE` | Informes mèdics, resultats diagnòstics, dades de salut, dades biomètriques | Art. 9 — Categoria especial | 72 hores | 1 (un sol ús) |

La classificació la defineix el tenant al crear el document. El sistema aplica les restriccions automàticament.

---

## 3. Model de dades

### Taula: `secure_document_tokens`

| Camp | Tipus | Descripció |
|------|-------|-----------|
| `id` | UUID PK | Identificador intern |
| `token` | VARCHAR(64) UNIQUE NOT NULL | Token criptogràfic segur (32 bytes hex) |
| `tenant_id` | UUID NOT NULL | Tenant propietari del document |
| `storage_file_id` | VARCHAR(500) NOT NULL | Referència al fitxer al proveïdor d'emmagatzematge (MinIO/GDrive) |
| `file_name` | VARCHAR(255) NOT NULL | Nom original del fitxer (per a la descàrrega) |
| `mime_type` | VARCHAR(100) NOT NULL | Tipus MIME del fitxer |
| `sensitivity` | VARCHAR(20) NOT NULL | `STANDARD` o `SENSITIVE` |
| `recipient_email` | VARCHAR(255) NOT NULL | Email del destinatari |
| `recipient_name` | VARCHAR(255) | Nom del destinatari (per personalitzar l'email) |
| `description` | VARCHAR(500) | Descripció breu del document per a l'email |
| `expires_at` | TIMESTAMPTZ NOT NULL | Data/hora d'expiració del token |
| `max_downloads` | INTEGER | Màxim de descàrregues permeses (`NULL` = il·limitat) |
| `download_count` | INTEGER NOT NULL DEFAULT 0 | Nombre de descàrregues realitzades |
| `first_downloaded_at` | TIMESTAMPTZ | Primera descàrrega |
| `last_downloaded_at` | TIMESTAMPTZ | Darrera descàrrega |
| `email_sent_at` | TIMESTAMPTZ | Quan s'ha enviat l'email de notificació |
| `revoked` | BOOLEAN NOT NULL DEFAULT false | Revocat manualment per l'admin |
| `revoked_at` | TIMESTAMPTZ | Quan s'ha revocat |
| `created_by` | UUID | Usuari del portal que ha creat el token |
| `created_at` | TIMESTAMPTZ NOT NULL | Data de creació |

**Índexs:** `token` (UNIQUE), `(tenant_id, created_at)`, `expires_at` (per a neteja periòdica).

### Taula: `secure_document_audit`

Registre immutable de cada intent d'accés (RGPD Art. 9 requereix traçabilitat per a categories especials).

| Camp | Tipus | Descripció |
|------|-------|-----------|
| `id` | UUID PK | |
| `token_id` | UUID NOT NULL FK | Referència al token |
| `tenant_id` | UUID NOT NULL | Desnormalitzat per a queries d'auditoria |
| `event_type` | VARCHAR(30) NOT NULL | `EMAIL_SENT`, `DOWNLOAD_OK`, `DOWNLOAD_EXPIRED`, `DOWNLOAD_EXHAUSTED`, `DOWNLOAD_REVOKED`, `DOWNLOAD_ERROR` |
| `ip_address` | VARCHAR(45) | IP del client (IPv4 o IPv6) |
| `user_agent` | VARCHAR(500) | User-Agent del navegador |
| `occurred_at` | TIMESTAMPTZ NOT NULL | Moment de l'event |

---

## 4. Flux complet

```
Tenant (portal) → crea document (Mòdul 37)
         ↓
SecureDocumentService.issue(tenantId, fileId, fileName, mimeType,
                             recipientEmail, recipientName,
                             description, sensitivity)
         ↓
[1] Genera token (SecureRandom 32 bytes → hex)
[2] Calcula expiresAt i maxDownloads segons sensitivity
[3] Guarda SecureDocumentToken a PostgreSQL
[4] Envia email via Brevo:
      Assumpte: "El teu document està disponible"
      Cos: nom destinatari + descripció + botó [Descarregar document]
      Link: https://{domain}/api/v1/documents/download/{token}
[5] Registra event EMAIL_SENT a secure_document_audit
         ↓
Client (email) → clica el link
         ↓
GET /api/v1/documents/download/{token}  (endpoint públic, sense JWT)
         ↓
SecureDocumentService.validateAndStream(token, ipAddress, userAgent)
         ↓
[1] Cerca token per valor (no per ID)
[2] Comprova: token existeix, no revocat, no expirat, downloads < max
[3] Si KO → registra event tipus corresponent → retorna 410 Gone
[4] Si OK:
      - Incrementa download_count
      - Actualitza first/last_downloaded_at
      - Recupera fitxer de MinIO/GDrive via StorageProvider
      - Registra event DOWNLOAD_OK a secure_document_audit
      - Retorna stream del fitxer amb headers:
            Content-Disposition: attachment; filename="{fileName}"
            Content-Type: {mimeType}
            Cache-Control: no-store
            X-Content-Type-Options: nosniff
```

---

## 5. API

### Endpoints del portal (autenticats, rol ADMIN o CLIENT)

```
POST   /api/v1/documents/tokens
       Body: { storageFileId, fileName, mimeType, sensitivity,
               recipientEmail, recipientName, description }
       → 201 { tokenId, expiresAt, downloadUrl }

GET    /api/v1/documents/tokens?page=0&size=20
       → Llista tokens del tenant (paginada, ordenada per created_at DESC)

GET    /api/v1/documents/tokens/{id}
       → Detall d'un token + estadístiques de descàrrega

DELETE /api/v1/documents/tokens/{id}
       → Revoca el token (posa revoked=true, registra event REVOKED)

GET    /api/v1/documents/tokens/{id}/audit
       → Historial d'events d'auditoria d'un token
```

### Endpoint públic (sense autenticació)

```
GET    /api/v1/documents/download/{token}
       → Stream del fitxer si el token és vàlid
       → 410 Gone si expirat, esgotat o revocat
       → 404 Not Found si el token no existeix
```

**L'endpoint de descàrrega NO retorna mai el motiu específic del rebuig** (només 410 genèric) per evitar enumerar l'estat dels tokens.

---

## 6. Email de notificació

Format en text/HTML via Brevo EU. Variables:

| Variable | Valor |
|----------|-------|
| `{RECIPIENT_NAME}` | Nom del destinatari |
| `{DOCUMENT_DESCRIPTION}` | Descripció del document |
| `{TENANT_NAME}` | Nom del negoci (tenant) |
| `{DOWNLOAD_URL}` | URL completa amb token |
| `{EXPIRES_AT}` | Data/hora d'expiració formatada (ex: "17 de juny de 2026 a les 14:32") |
| `{SENSITIVITY_NOTE}` | Missatge addicional per a SENSITIVE: "Aquest document conté informació confidencial. L'enllaç és d'un sol ús." |

L'assumpte de l'email inclou el nom del negoci per evitar ser marcat com a spam:
`[{TENANT_NAME}] El teu document està disponible`

---

## 7. Seguretat

- **Token:** `SecureRandom.generateSeed(32)` → hex string 64 caràcters. Impossible de predir per força bruta.
- **Transport:** Sempre HTTPS (Traefik + Let's Encrypt). El token viatja únicament a l'URL.
- **Emmagatzematge:** El token es guarda en clar a la BD (és com una contrasenya d'un sol ús, no un secret de llarga durada). Si un atacant accedeix a la BD, els tokens ja expirats no serveixen.
- **Rate limiting:** Màxim 10 intents per IP per minut a l'endpoint de descàrrega (Redis + bucket token).
- **No cache:** `Cache-Control: no-store` a la resposta de descàrrega.
- **Logs:** La IP i el User-Agent es registren a `secure_document_audit` per a traçabilitat RGPD.

### Documents SENSITIVE: mesures addicionals

- TTL màxim 72 hores (no configurable per sota).
- Màxim 1 descàrrega (no configurable per sobre a la UI per a SENSITIVE).
- L'email inclou avís explícit: "Aquest document conté informació de salut confidencial."
- El tenant pot revocar el token en qualsevol moment des del portal.
- Retenció dels logs d'auditoria: mínim 3 anys (RGPD Art. 9 + consideracions sanitàries).

---

## 8. Neteja automàtica (Scheduled Job)

Tasca programada diàriament a les 03:00 UTC:

```java
// Elimina tokens expirats fa més de 30 dies (STANDARD)
// Elimina tokens expirats fa més de 90 dies (SENSITIVE, per auditoria)
// NO elimina mai els registres de secure_document_audit
```

Els fitxers al MinIO/GDrive els gestiona el Mòdul 38 independentment.

---

## 9. Frontend (portal)

### `/portal/documents/tokens` (ADMIN)

Taula de tokens emesos amb:
- Nom del fitxer + destinatari
- Sensitivity badge (verd STANDARD / vermell SENSITIVE)
- Estat: ACTIU / EXPIRAT / REVOCAT / ESGOTAT
- Comptador de descàrregues
- Botó "Revocar" per als actius
- Botó "Veure auditoria" → modal amb la llista d'events

### Integració amb Mòdul 37 (Document Builder)

Al generar un document des del portal, botó secundari:
**[Enviar per link segur]** → modal que demana:
- Email destinatari
- Nom destinatari
- Descripció breu (pre-omplerta amb el nom del document)
- Classificació: STANDARD / SENSITIVE (pre-seleccionada per tipus de plantilla)

---

## 10. Casos d'ús principals

### C1: Clínica envia informe mèdic a pacient
1. Metge genera informe des del portal (Mòdul 37).
2. Clica "Enviar per link segur" → selecciona SENSITIVE.
3. El pacient rep email: "El teu informe de visita del 15/06 ja és disponible."
4. Clica el link → descarrega l'informe una sola vegada.
5. Segon clic → 410 Gone ("Aquest document ja ha estat descarregat").
6. El metge pot veure a la taula que el pacient ha descarregat el document (hora + IP).

### C2: Gestoria envia factura a client
1. Comptable genera factura PDF (Mòdul 37 o upload directe).
2. Selecciona STANDARD → TTL 7 dies, descàrregues il·limitades.
3. Client pot descarregar-la múltiples vegades fins que expiri.

### C3: Token expirat
- Client clica un link de 3 dies enrere (SENSITIVE).
- Rep pàgina d'error: "Aquest document ja no és accessible. Contacta amb [Nom negoci] per obtenir-ne un de nou."
- Tenant rep notificació (opcional, configurable) que el client ha intentat accedir a un token expirat.

---

## 11. Consideracions RGPD

| Obligació | Com es compleix |
|-----------|----------------|
| Art. 9 — Categoria especial requereix protecció reforçada | TTL curt + 1 sola descàrrega + audit trail complet |
| Art. 5(f) — Integritat i confidencialitat | Fitxer al MinIO (no adjunt), token d'un sol ús, HTTPS obligatori |
| Art. 17 — Dret a l'oblit | Revocació immediata del token + eliminació del fitxer via Mòdul 38 |
| Art. 30 — Registre d'activitats | `secure_document_audit` cobreix qui ha accedit, quan i des d'on |
| Art. 32 — Seguretat del tractament | Token criptogràfic, rate limiting, no cache, logs immutables |

---

## 12. Fora d'abast (v1)

- Verificació d'identitat del destinatari (DNI, data de naixement) — possible v2 per a SENSITIVE.
- Signatura electrònica del document.
- Xifrat end-to-end del fitxer al MinIO (ja cobert pel Mòdul 38 si el proveïdor ho suporta).
- Enviament per WhatsApp (solo email en v1).
- Preview del document al navegador sense descàrrega.
