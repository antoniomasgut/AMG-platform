# Spec 40 — Google Workspace Integration (Multi-Tenant OAuth)

**Versió:** 1.0
**Data:** 2026-06-06
**Estat:** Proposat
**Depèn de:** Spec 02 (Vault), Spec 38 (Tenant Storage), Spec 25 (Omnichannel Inbox), Spec 28 (NexeLocal Service Configs)

---

## 1. Visió general

Permetre que cada tenant connecti el seu propi compte de Google (Workspace o personal) mitjançant OAuth 2.0 per utilitzar els serveis de Google com a extensions natives de la plataforma.

La plataforma utilitza una **única aplicació OAuth de Google** propietat del SaaS. Cada tenant autoritza individualment l'accés als seus recursos. Mai s'emmagatzemen credencials Google del usuari en text pla — només tokens OAuth xifrats via Vault (Spec 02).

### Abast v1

| Servei | Proposat | Estat actual |
|--------|----------|-------------|
| Google Drive | Pujar i gestionar documents generats | No existeix (Spec 38 té `StorageProvider` interface, GoogleDriveStorageProvider no implementat) |
| Gmail | Enviar correus des del compte del tenant | No existeix (només Brevo per email transaccional) |
| Google Calendar | Crear events, sincronitzar agenda | Existeix parcialment a `GoogleCalendarService` (SA + OAuth), s'ha d'unificar |
| Google Sheets | Llegir catàlegs, tarifes, configuracions | No existeix |

---

## 2. Relació amb specs existents

| Spec | Compatibilitat | Canvi requerit |
|------|---------------|----------------|
| **02 Vault** | ✅ `VaultEncryption` (AES-256-GCM) serveix per xifrar tokens. No cal canvi | — |
| **38 Tenant Storage** | ✅ `StorageProvider` interface definida. Google Drive n'és una implementació | Afegir `GoogleDriveStorageProvider` |
| **25 Omnichannel Inbox** | ⚠️ Menciona `EmailChannel` però no detalla. Gmail n'és el backend real | Afegir Gmail com a canal d'enviament |
| **28 NexeLocal Configs** | ✅ `calendarIntegration` ja accepta `google_calendar` | — |
| **39 System Config** | ✅ `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET` ja existeixen | — |
| **33 Absence Reschedule** | ⚠️ Cancel·la tasques internes, no events de Calendar | Opcional: sincronitzar cancel·lació a Calendar |
| **26 RAG Knowledge Base** | 🔮 Google Sheets com a font de dades externa (futur) | — |

---

## 3. Aplicació OAuth Centralitzada

Una sola aplicació OAuth de Google Cloud:

```
Google Cloud Project
├── OAuth 2.0 Client (web)
│   ├── GOOGLE_OAUTH_CLIENT_ID
│   └── GOOGLE_OAUTH_CLIENT_SECRET
├── Drive API               (habilitada)
├── Gmail API               (habilitada)
├── Calendar API            (habilitada)
└── Sheets API              (habilitada)
```

**Tots els tenants** utilitzen aquesta aplicació. NO es crea una app per tenant.

Les credencials OAuth es configuren a System Config (Spec 39):

| Key | Tipus | Descripció |
|-----|-------|-----------|
| `GOOGLE_OAUTH_CLIENT_ID` | secret | Client ID de l'app OAuth |
| `GOOGLE_OAUTH_CLIENT_SECRET` | secret | Client Secret |

---

## 4. Model de dades

### 4.1 Taula `google_connections`

Crea un nou fitxer de migració `V22__google_workspace.sql`:

```sql
CREATE TABLE google_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    google_account_email VARCHAR(255) NOT NULL,
    google_user_id VARCHAR(255) NOT NULL,
    encrypted_access_token TEXT NOT NULL,
    encrypted_refresh_token TEXT NOT NULL,
    token_expires_at TIMESTAMPTZ NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_google_connections_tenant ON google_connections(tenant_id);
CREATE INDEX idx_google_connections_active ON google_connections(is_active);
```

**Notes:**
- `encrypted_access_token` i `encrypted_refresh_token` s'emmagatzemen xifrats amb `VaultEncryption.encrypt()` (AES-256-GCM)
- Mai en text pla
- Un sol tenant = una connexió (no múltiples comptes)

### 4.2 Taula `google_module_configs`

Per tenir una configuració per tenant dels mòduls activats:

```sql
CREATE TABLE google_module_configs (
    tenant_id UUID NOT NULL PRIMARY KEY REFERENCES tenants(id),
    drive_enabled BOOLEAN NOT NULL DEFAULT false,
    gmail_enabled BOOLEAN NOT NULL DEFAULT false,
    calendar_enabled BOOLEAN NOT NULL DEFAULT false,
    sheets_enabled BOOLEAN NOT NULL DEFAULT false,
    drive_folder_id VARCHAR(255),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### 4.3 Taula `oauth_states` (millora de seguretat)

L'actual implementació de Calendar (Spec 28) passa el `tenantId` com a `state` sense protecció CSRF. Aquesta nova taula ho corregeix:

```sql
CREATE TABLE oauth_states (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    state_token VARCHAR(64) NOT NULL UNIQUE,
    redirect_uri VARCHAR(500) NOT NULL,
    requested_scopes TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_oauth_states_token ON oauth_states(state_token);
```

---

## 5. Scopes

Es construeixen dinàmicament segons els mòduls seleccionats pel tenant:

| Mòdul | Scope | Permís |
|-------|-------|--------|
| Drive | `https://www.googleapis.com/auth/drive.file` | Només fitxers creats per l'app |
| Gmail | `https://www.googleapis.com/auth/gmail.send` | Només enviar, no llegir |
| Calendar | `https://www.googleapis.com/auth/calendar` | Llegir i crear events |
| Sheets | `https://www.googleapis.com/auth/spreadsheets` | Llegir i escriure fulls |

L'usuari selecciona quins mòduls vol activar abans de l'autorització OAuth.

---

## 6. Flux OAuth

### 6.1 Inici

```
[Frontend]                              [Backend]                         [Google]
    │                                       │                                │
    │  1. Selecciona mòduls                 │                                │
    │  (Drive, Gmail, Calendar, Sheets)     │                                │
    │─────────────────────►                 │                                │
    │                                       │                                │
    │  2. POST /api/v1/google/auth-url      │                                │
    │  { tenantId, modules: [...] }         │                                │
    │─────────────────────►                 │                                │
    │                                       │ 3. Genera state token           │
    │                                       │    (UUID aleatori)              │
    │                                       │ 4. Guarda oauth_states          │
    │                                       │ 5. Construeix URL OAuth         │
    │                                       │    amb scopes seleccionats      │
    │  6. { authUrl }                       │                                │
    │◄─────────────────────                 │                                │
    │                                       │                                │
    │  7. Redirigeix usuari a Google        │                                │
    │──────────────────────────────────────────────────────────►             │
    │                                       │                                │
    │                                       │  8. Usuari autoritza           │
    │                                       │                                │
    │                                       │  9. Callback amb code + state  │
    │                                       │◄────────────────────────────────│
    │                                       │                                │
    │                                       │ 10. Valida state token          │
    │                                       │ 11. Intercanvia code → tokens   │
    │                                       │ 12. Obté user info (email)      │
    │                                       │ 13. Xifra tokens amb Vault     │
    │                                       │ 14. Guarda google_connections  │
    │                                       │ 15. Activa mòduls seleccionats  │
    │                                       │                                │
    │ 16. Redirigeix al frontend            │                                │
    │◄──────────────────────────────────────│                                │
```

### 6.2 State Security (millora respecte a l'existent)

El `state` és un **UUID aleatori** emmagatzemat a `oauth_states` amb expiració (10 minuts). Al callback:
- Es verifica que el `state_token` existeixi i no hagi expirat
- S'elimina el registre (one-time use)
- S'extreu el `tenantId` associat

Això corregeix la vulnerabilitat actual on el `tenantId` es passa com a `state` en clar.

### 6.3 Renovació de tokens

Abans de qualsevol crida a una API de Google:
1. Verificar `token_expires_at`
2. Si ha expirat o expirarà en menys de 5 minuts:
   - Fer refresh amb `refresh_token`
   - Actualitzar `encrypted_access_token` + `token_expires_at`

Procés completament transparent per al sistema.

---

## 7. Google Drive (`GoogleDriveStorageProvider`)

Implementa la interfície `StorageProvider` definida a **Spec 38** (`shared/storage/StorageProvider.java`).

```
shared/storage/
├── StorageProvider.java          ← Interface existent
├── LocalStorageProvider.java     ← Implementació existent
├── S3StorageProvider.java        ← Implementació existent
├── GoogleDriveStorageProvider.java  ← NOVA
└── StorageProviderRouter.java    ← Router existent (ja resol per tenant)
```

### 7.1 Dependències Maven

```xml
<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
    <version>2.7.2</version>
</dependency>
<dependency>
    <groupId>com.google.apis</groupId>
    <artifactId>google-api-services-drive</artifactId>
    <version>v3-rev20250115-2.0.0</version>
</dependency>
<dependency>
    <groupId>com.google.auth</groupId>
    <artifactId>google-auth-library-oauth2-http</artifactId>
    <version>1.33.1</version>
</dependency>
```

### 7.2 Implementació

```java
public class GoogleDriveStorageProvider implements StorageProvider {

    private final Drive driveService;
    private final String rootFolderId;

    public GoogleDriveStorageProvider(String accessToken, String rootFolderId) {
        var credentials = new GoogleCredentials(accessToken);
        this.driveService = new Drive.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            new HttpCredentialsAdapter(credentials))
            .setApplicationName("AMG Digitalització")
            .build();
        this.rootFolderId = rootFolderId;
    }

    @Override
    public StorageFile upload(InputStream data, String fileName, String mimeType) {
        var file = new File();
        file.setName(fileName);
        file.setMimeType(mimeType);
        file.setParents(List.of(rootFolderId));

        var content = new InputStreamContent(mimeType, data);
        var uploaded = driveService.files().create(file, content)
            .setFields("id, name, mimeType, size")
            .execute();

        return new StorageFile(uploaded.getId(), uploaded.getName(),
            uploaded.getMimeType(), uploaded.getSize(), "google_drive");
    }

    @Override
    public InputStream download(String fileId) {
        var content = driveService.files().get(fileId).executeMedia();
        return content.getContentInputStream();
    }

    @Override
    public void delete(String fileId) {
        driveService.files().delete(fileId).execute();
    }

    @Override
    public boolean exists(String fileId) {
        try {
            driveService.files().get(fileId).setFields("id").execute();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getSignedUrl(String fileId, Duration expiry) {
        // Google Drive no té signed URLs.
        // Es retorna un link de visualització temporal via webViewLink
        try {
            var file = driveService.files().get(fileId)
                .setFields("webViewLink").execute();
            return file.getWebViewLink();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public FileMetadata getMetadata(String fileId) {
        var file = driveService.files().get(fileId)
            .setFields("id, name, mimeType, size, createdTime")
            execute();
        return new FileMetadata(file.getId(), file.getName(),
            file.getMimeType(), file.getSize(),
            Instant.parse(file.getCreatedTime().toStringRfc3339()), null);
    }

    @Override
    public String getProviderName() { return "google_drive"; }
}
```

### 7.3 Estructura de carpetes

En connectar Google Drive, es crea automàticament:

```
/AMG Digitalització/
├── Documents/
├── Factures/
├── Pressupostos/
└── Contractes/
```

La carpeta arrel (`rootFolderId`) es guarda a `google_module_configs.drive_folder_id`.

### 7.4 Integració amb StorageProviderRouter

El `StorageProviderRouter` existent ja llegeix `storage_provider_configs` per `tenant_id + provider_key`. Per a Google Drive, cal:

1. Obtenir el `access_token` desxifrat de `google_connections`
2. Construir el `GoogleDriveStorageProvider` amb el token
3. Retornar-lo com a provider actiu

---

## 8. Gmail (`GoogleMailProvider`)

### 8.1 Interface (nova)

```java
public interface MailProvider {
    void send(String to, String subject, String body, String attachmentUrl);
    String getProviderName();
}
```

Paquet: `shared/mail/`

### 8.2 Implementació

```java
public class GoogleMailProvider implements MailProvider {
    // Usa Gmail API: users.messages.send()
    // Construeix MIME message amb attachment
    // Adjunta PDF des de Drive o storage local
}
```

### 8.3 Casos d'ús

- Enviar pressupost → client
- Enviar factura → client
- Enviar contracte → client
- Enviar notificacions des del compte del tenant

### 8.4 Relació amb Brevo

| Canal | Ús |
|-------|-----|
| **Brevo** | Email transaccional del SaaS (notificacions, reset password, alerts) |
| **Gmail** | Email **des del compte del tenant** a clients (pressupostos, factures) |

No es substitueix Brevo. Gmail és un canal addicional per a comunicacions sortints amb la marca del tenant.

---

## 9. Google Calendar

### 9.1 Estat actual

Ja existeix `GoogleCalendarService` amb:
- OAuth flow per calendar
- Creació d'events
- Autenticació via Service Account + OAuth

### 9.2 Millores proposades

1. **Unificar OAuth**: Reutilitzar el flux OAuth centralitzat (secció 6) en lloc del flux independent actual
2. **State amb nonce**: Substituir `state=tenantId` per `state=UUID` amb `oauth_states`
3. **Integrar amb Spec 33**: En una cancel·lació per absència, opcionalment actualitzar l'event de Calendar

---

## 10. Google Sheets

### 10.1 Interface (futur)

```java
public interface SpreadsheetProvider {
    List<Map<String, Object>> readRows(String spreadsheetId, String range);
    void writeRows(String spreadsheetId, String range, List<Map<String, Object>> rows);
}
```

### 10.2 Casos d'ús inicials (v1 opcional)

- Catàleg de productes des de Google Sheets
- Tarifes i preus editables pel tenant sense tocar codi
- Configuracions externes

---

## 11. Renovació de tokens

`GoogleTokenService` (servei compartit):

```java
@Service
@RequiredArgsConstructor
public class GoogleTokenService {

    private final GoogleConnectionRepository repo;
    private final VaultEncryption vault;

    public GoogleCredentials getValidCredentials(UUID tenantId) {
        var conn = repo.findByTenantIdAndIsActiveTrue(tenantId)
            .orElseThrow(() -> new RuntimeException("Google no connectat"));

        if (conn.getTokenExpiresAt().isBefore(Instant.now().plus(5, ChronoUnit.MINUTES))) {
            conn = refreshTokens(conn);
        }

        var accessToken = vault.decrypt(conn.getEncryptedAccessToken());
        return new GoogleCredentials(accessToken);
    }

    private GoogleConnection refreshTokens(GoogleConnection conn) {
        var refreshToken = vault.decrypt(conn.getEncryptedRefreshToken());
        // POST a https://oauth2.googleapis.com/token
        //   grant_type=refresh_token
        //   client_id=...
        //   client_secret=...
        //   refresh_token=...
        // Guarda nous tokens xifrats
        // Retorna connexió actualitzada
    }
}
```

---

## 12. API REST

### 12.1 OAuth

| Mètode | Path | Descripció |
|--------|------|-----------|
| `POST` | `/api/v1/google/auth-url` | Genera URL OAuth amb scopes seleccionats |
| `GET` | `/api/v1/google/callback` | Callback OAuth de Google (públic) |
| `POST` | `/api/v1/google/connect` | Desa el code i completa la connexió |
| `DELETE` | `/api/v1/google/disconnect` | Revoca tokens i desconnecta |

### 12.2 Configuració per tenant

| Mètode | Path | Descripció |
|--------|------|-----------|
| `GET` | `/api/v1/tenants/{tenantId}/google` | Estat de la connexió |
| `GET` | `/api/v1/tenants/{tenantId}/google/modules` | Mòduls activats |
| `PUT` | `/api/v1/tenants/{tenantId}/google/modules` | Activa/desactiva mòduls |
| `POST` | `/api/v1/tenants/{tenantId}/google/test` | Prova la connexió |

### 12.3 Drive (via StorageProvider)

Es reutilitzen els endpoints de **Spec 38** (`/api/v1/tenants/{tenantId}/storage/...`).
Quan el provider actiu és `google_drive`, els uploads van a Google Drive automàticament.

### 12.4 Gmail

| Mètode | Path | Descripció |
|--------|------|-----------|
| `POST` | `/api/v1/tenants/{tenantId}/google/send` | Envia un correu via Gmail |

---

## 13. Seguretat

### 13.1 Tokens

- Mai s'exposen tokens al frontend
- Mai s'emmagatzemen en text pla
- Xifratge AES-256-GCM via `VaultEncryption`
- Rate limiting a tots els endpoints

### 13.2 Crides a Google

- Totes les crides a API de Google es fan des del backend
- El frontend només rep dades dessensibilitzades

### 13.3 Revocació

En desconnectar:
1. `POST https://oauth2.googleapis.com/revoke?token={accessToken}`
2. Eliminar `encrypted_access_token` i `encrypted_refresh_token`
3. Marcar `is_active = false`
4. Desactivar tots els mòduls

### 13.4 RGPD

- Consentiment explícit a l'inici del flux OAuth
- Dret a l'oblit: en eliminar tenant, revocar tokens i eliminar referències
- Minimització: només es guarden tokens (no dades dels usuaris de Google)

---

## 14. Frontend

### 14.1 Ruta

```
Configuració → Integracions → Google Workspace
```

Ruta: `/portal/admin/integrations/google`

### 14.2 Components

**Estat de connexió:**
- No connectat: botó "Connecta Google"
- Connectat: email del compte + estat dels mòduls + botó "Desconnecta"

**Selector de mòduls:**
```
☑ Google Drive     — Desa documents al teu Drive
☑ Gmail            — Envia correus des del teu compte
☐ Google Calendar  — Crea events al teu calendari
☐ Google Sheets    — Llegeix dades dels teus fulls
```

**Accions:**
- `[Connecta Google]` — Inicia flux OAuth
- `[Prova connexió]` — Testeja que els tokens funcionen
- `[Desconnecta]` — Revoca i elimina

---

## 15. Migració Flyway

Fitxer: `V22__google_workspace.sql`

```sql
-- Taula de connexions Google OAuth per tenant
CREATE TABLE google_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    google_account_email VARCHAR(255) NOT NULL,
    google_user_id VARCHAR(255) NOT NULL,
    encrypted_access_token TEXT NOT NULL,
    encrypted_refresh_token TEXT NOT NULL,
    token_expires_at TIMESTAMPTZ NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_google_connections_tenant ON google_connections(tenant_id);
CREATE INDEX idx_google_connections_active ON google_connections(is_active);

-- Taula de configuració de mòduls per tenant
CREATE TABLE google_module_configs (
    tenant_id UUID NOT NULL PRIMARY KEY REFERENCES tenants(id),
    drive_enabled BOOLEAN NOT NULL DEFAULT false,
    gmail_enabled BOOLEAN NOT NULL DEFAULT false,
    calendar_enabled BOOLEAN NOT NULL DEFAULT false,
    sheets_enabled BOOLEAN NOT NULL DEFAULT false,
    drive_folder_id VARCHAR(255),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Taula d'estats OAuth (nonce, one-time use, 10 min expiry)
CREATE TABLE oauth_states (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    state_token VARCHAR(64) NOT NULL UNIQUE,
    redirect_uri VARCHAR(500) NOT NULL,
    requested_scopes TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_oauth_states_token ON oauth_states(state_token);
```

---

## 16. Resum de canvis respecte al PRD original

| Aspecte | PRD original | Spec 40 alineada |
|---------|-------------|------------------|
| Xifrat de tokens | "AES-256 o mecanisme equivalent" | ✅ `VaultEncryption` existent (AES-256-GCM) |
| State OAuth | "token aleatori" genèric | ✅ Taula `oauth_states` amb expiració + one-time use |
| Storage Drive | Interface pròpia `StorageProvider` | ✅ Reutilitza interface de Spec 38 |
| Gmail vs Brevo | No esmenta coexistència | ✅ Clar: Brevo = transaccional, Gmail = tenant outbound |
| Calendar | Nou | ✅ Reutilitza `GoogleCalendarService` existent |
| Provider interfaces | Defineix 4 interfaces pròpies | ✅ Drive usa StorageProvider (Spec 38). MailProvider és nova. Sheets ajornada |
| Mòdul | Sense número | ✅ **Spec 40** dins la numeració del monorepo |
