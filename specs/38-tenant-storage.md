# Mòdul 38 — Tenant Storage (Sistema d'Emmagatzematge Multi-Tenant)

## Objectiu

Implementar una capa d'emmagatzematge abstracta, multi-tenant i RGPD que externalitzi els arxius generats (PDFs, imatges) fora de PostgreSQL, aprofitant **MinIO** (ja desplegat) i **Google Drive** com a proveïdors inicials.

---

## Principis de Disseny

**Dades de negoci** (metadades, config, plantilles, variables, fórmules, històric) → PostgreSQL.
**Arxius** (PDFs, logos, imatges, adjunts) → Storage Provider extern.

---

## Storage Provider Interface

Capa abstracta al paquet `shared/storage/`:

```java
public interface StorageProvider {
    StorageFile upload(InputStream data, String fileName, String mimeType);
    InputStream download(String fileId);
    void delete(String fileId);
    boolean exists(String fileId);
    String getSignedUrl(String fileId, Duration expiry);
    FileMetadata getMetadata(String fileId);
    String getProviderName();
}
```

Resolució del provider per tenant via `StorageProviderRouter`, que llegeix la configuració del tenant i retorna la implementació correcta.

---

## Proveïdors inicials (v1)

### 1. MinIO (S3-compatible)

Implementació: `S3StorageProvider` — usa el SDK d'Amazon S3 apuntant al MinIO local.

```json
{
  "provider": "minio",
  "endpoint": "http://minio:9000",
  "bucket": "amg-documents",
  "region": "eu-central-1"
}
```

El tenant pot alternar entre MinIO (servidor propi) i AWS S3 canviant endpoint/region.

### 2. Google Drive

Implementació: `GoogleDriveStorageProvider` — autenticació OAuth2, permisos mínims.

Crea automàticament:
```
/AMG Digitalització/
/AMG Digitalització/Documents/
/AMG Digitalització/Contracts/
/AMG Digitalització/Invoices/
/AMG Digitalització/Quotes/
```

El tenant pot seleccionar una carpeta arrel personalitzada.

```json
{
  "provider": "google_drive",
  "folderId": "root_folder_id_optional",
  "credentialsRef": "uuid-del-credential-al-vault"
}
```

---

## Proveïdors futurs

Disseny preparat per afegir sense canviar la interfície:
- Amazon S3 (directe, sense MinIO intermedi)
- OneDrive (Microsoft OAuth)
- Dropbox

---

## Xifrat de credencials

Les credencials OAuth (access token, refresh token, client secret) s'emmagatzemen al **Vault** (AES-256-GCM via `VaultEncryption.java`). Mai en text pla.

`tenant_storage_settings` guarda **referències** al credential del Vault.

---

## Esquema de dades

### Taula `storage_provider_configs`

Seguint el patró de `nexe_service_configs` (clau composta tenant_id + service_key):

| Columna | Tipus | Descripció |
|---------|-------|------------|
| `tenant_id` | UUID (PK) | |
| `provider_key` | VARCHAR(30) (PK) | `minio`, `google_drive` |
| `config_json` | TEXT | Configuració JSON del proveïdor |
| `is_active` | BOOLEAN DEFAULT false | Actiu o no |
| `updated_at` | TIMESTAMPTZ | |

`config_json` conté (exemple per Google Drive):
```json
{
  "folderId": "...",
  "credentialsRef": "uuid-del-credential-al-vault",
  "autoCreateFolders": true
}
```

### Taula `documents` (metadades persistents)

Ampliar `generated_documents` amb camps de storage:

| Columna | Tipus | Descripció |
|---------|-------|------------|
| `storage_provider` | VARCHAR(30) | `minio`, `google_drive` |
| `storage_file_id` | VARCHAR(255) | ID del fitxer al proveïdor |
| `storage_path` | VARCHAR(500) | Ruta dins del proveïdor |
| `pdf_url` | VARCHAR(255) | Es manté, ara conté URL signed real |

La resta de camps (`htmlContent`, `customerData`, `variables`, `articles`, `calculated`) **es queden a PostgreSQL** — són dades de negoci, no arxius.

---

## Flux de generació de documents

### Amb storage extern actiu

```
1. Genera HTML (ja existeix)
2. Genera PDF (Flying Saucer / OpenPDF - NO és stub)
3. Obté StorageProvider del tenant
4. Puja el PDF al proveïdor extern
5. Obté fileId + path del proveïdor
6. Guarda metadades + referències a generated_documents
7. Signed URL efímera (24h) per descàrrega
8. Esborra temporal local
```

### Sense storage extern (fallback)

```
1. Genera HTML
2. Genera PDF
3. Guarda HTML + PDF path local a generated_documents
4. Serveix el PDF via endpoint intern
```

---

## Migració de l'Assets module (Spec 06)

L'actual `AssetOrchestrator` guarda al disc local (`/data/assets/`). S'ha de migrar a `StorageProvider`:

- `AssetOrchestrator` passa a usar `StorageProviderRouter.getProvider(tenantId)`
- Els logos d'empresa s'emmagatzemen via StorageProvider (no a BD)
- Retrocompatibilitat: els assets existents al disc local es serveixen igual

---

## Política de retenció local

- El fitxer temporal s'elimina després de pujar al proveïdor
- Si el proveïdor cau, el PDF es guarda a PostgreSQL com a base64 (fallback, màxim 7 dies)
- No hi ha "24h de límit" — el temporal dura el que trigui la pujada

---

## Descàrrega de documents

`GET /api/v1/documents/{id}/pdf`:
1. Consulta `generated_documents` per `id`
2. Si té `storage_provider`, obté `StorageProvider` → `getSignedUrl()` → redirect
3. Si no té provider extern, serveix el PDF emmagatzemat internament

Mai s'exposen tokens OAuth al client.

---

## Complement RGPD

- **Consentiment**: en connectar Google Drive, es mostra: *"Els documents generats s'emmagatzemaran al vostre Google Drive."*
- **Dret a l'oblit**: en eliminar tenant, es permet: (a) eliminar només referències, (b) eliminar referències + arxius al proveïdor
- **Minimització**: a PostgreSQL només hi ha metadades i HTML (text)

---

## UI de configuració

Ruta: `Configuració → Emmagatzematge`

Selecció de proveïdor:
- `○ Local (per defecte)`
- `○ MinIO / S3`
- `○ Google Drive`

Botons:
- `[Connecta]` / `[Desconnecta]`
- `[Prova connexió]`
- `[Canvia proveïdor]`

Indicadors:
- Proveïdor actiu
- Estat connexió
- Espai utilitzat (si el provider ho suporta)

---

## Integració amb Mòdul 37 (Document Builder)

- El `DocumentBuilderService` rep un `StorageProviderRouter` per injecció
- En generar document, si el tenant té storage actiu → puja PDF automàticament
- Si no → comportament actual (sense PDF real, o fallback local)
- Cap canvi a les plantilles ni als blocs del layout

---

## Extensions futures

- **Migració entre proveïdors**: relliga tots els documents d'un tenant del provider A al B
- **Google Shared Drives**
- **SharePoint / OneDrive**
- **Azure Blob Storage / Backblaze B2 / Wasabi**

---

## Resultat esperat

- MinIO aprofita la infraestructura existent (ja desplegat)
- Google Drive permet als tenants veure els documents al seu Drive
- Els PDFs surten de PostgreSQL → menys BD, més disc econòmic
- Si un tenant no configura storage extern, tot funciona com ara
- La interfície `StorageProvider` permet afegir nous proveïdors sense tocar la resta del sistema
