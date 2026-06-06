# Mòdul 39 — System Config (Configuració Global de l'Aplicació)

## Objectiu

Millorar el sistema de configuració global (`SystemSetting` / `SystemConfigService`) afegint tipus, validació, audit log i una UI amb cerca i categories, mantenint la prioritat env var → DB xifrada.

L'abast és **estrictament global** — cap configuració per tenant. Les claus afecten tots els tenants per igual.

---

## Estat actual

El sistema funciona però té mancances:

| Aspecte | Com està ara |
|---------|-------------|
| Tipus | Tot és string — no distingeix `boolean`, `number`, `url` |
| Validació | Cap — pots guardar "hola" a `N8N_API_URL` |
| Categoria | `KNOWN_KEYS` té categories però el frontend no les ordena |
| Audit | No es registra qui va canviar què ni quan |
| Frontend cerca | No n'hi ha — totes les claus en una llista plana |
| Valors per defecte | No existeixen — si no està configurat, retorna null |
| Dependències | No es pot expressar "X necessita Y per funcionar" |

---

## Millores proposades

### 1. Tipus per clau

Cada `KnownKey` guanya un camp `type`:

| Tipus | Input UI | Exemple |
|-------|----------|---------|
| `secret` | Password amb toggle | `ANTHROPIC_API_KEY` |
| `string` | Text input | `BREVO_SENDER_EMAIL` |
| `url` | URL input amb validació | `N8N_API_URL` |
| `number` | Number input | `AGENTS_MAX_TOKENS` (nou) |
| `boolean` | Toggle switch | `MAINTENANCE_MODE` (nou) |
| `json` | Textarea monospace | `GOOGLE_CALENDAR_SA_JSON` |

### 2. Validació en guardar

El backend valida el valor abans de persistir:

```
secret → no buit, mínim 8 chars
url    → ha de començar per http:// o https://
number → dins d'un rang configurable [min, max]
boolean→ "true" o "false"
json   → ha de ser JSON parsejable
```

### 3. Audit log

Nova entitat `SystemConfigAuditLog` (taula `system_config_audit_log`):

| Camp | Tipus |
|------|-------|
| `id` | UUID PK |
| `key` | VARCHAR(80) |
| `action` | `SET`, `DELETE` |
| `previousValue` | TEXT (encriptat, opcional) |
| `userId` | UUID |
| `userEmail` | VARCHAR(150) |
| `ip` | VARCHAR(45) |
| `changedAt` | TIMESTAMPTZ |

### 4. Frontend millorat

Components nous per tipus:
- `ConfigSecretInput` — camp password amb reveal
- `ConfigUrlInput` — URL amb validació visual
- `ConfigToggle` — per booleans
- `ConfigNumberInput` — amb min/max
- `ConfigJsonEditor` — textarea amb syntax highlight

Layout:
- Barra de cerca (filtra per key, label, category, description)
- Categories plegables amb ordre definit
- Badge per font (`ENV` / `DB` / `MISSING`)
- Botó "Test" per claus testejables
- Indicador de "canviat per X fa Y minuts"

### 5. Metadades noves a SystemSetting

Ampliar `system_settings`:

| Columna nova | Tipus | Descripció |
|-------------|-------|------------|
| `value_type` | VARCHAR(20) | `secret`, `string`, `url`, `number`, `boolean`, `json` |
| `default_value` | TEXT | Valor per defecte (opcional) |
| `validation_rules` | TEXT | JSON amb regles (min, max, pattern, required) |
| `sort_order` | INTEGER | Per ordenar dins la categoria |

### 6. Categoria "MAINTENANCE" (nova)

Claus proposades:

| Clau | Tipus | Defecte | Descripció |
|------|-------|---------|------------|
| `MAINTENANCE_MODE` | boolean | `false` | Bloqueja l'accés d'usuaris no-admin |
| `MAINTENANCE_MESSAGE` | string | — | Missatge a mostrar durant manteniment |
| `PLATFORM_NAME` | string | `AMG Digitalització` | Nom de la plataforma |
| `PLATFORM_LOGO_URL` | url | — | URL del logo global |
| `DEFAULT_LOCALE` | string | `ca` | Locale per defecte |
| `DEFAULT_CURRENCY` | string | `EUR` | Moneda per defecte |
| `AGENTS_MAX_TOKENS` | number | `4096` | Límit global de tokens per agent |

### 7. Categoria "AI MODELS" (global defaults per tasca)

L'admin defineix **quin model d'IA s'usa per defecte per a cada tasca**. Aquest valor afecta tots els tenants. Si un tenant té `TenantAIConfig.preferredModel` configurat, overrideja aquest default.

| Clau | Tipus | Defecte | Descripció |
|------|-------|---------|------------|
| `AI_MODEL_CHAT` | string | `claude-sonnet-4-20250514` | Model per al xat general amb l'agent |
| `AI_MODEL_QUOTES` | string | `claude-sonnet-4-20250514` | Model per generar pressupostos |
| `AI_MODEL_CONTRACTS` | string | `claude-sonnet-4-20250514` | Model per generar contractes |
| `AI_MODEL_EXTRACTION` | string | `gemini-2.5-pro` | Model per extracció de dades de documents |
| `AI_MODEL_CLASSIFICATION` | string | `gpt-5-mini` | Model per classificació documental |
| `AI_MODEL_OCR` | string | `gemini-2.5-pro` | Model per OCR d'imatges i PDFs |
| `AI_MODEL_SUMMARIES` | string | `gpt-5-mini` | Model per resums automàtics |
| `AI_MODEL_IMAGE_GEN` | string | `dall-e-3` | Model per generació d'imatges |

Els valors per defecte són raonables per començar, però l'admin pot canviar-los en qualsevol moment segons disponibilitat de models, cost o rendiment.

**Flux de resolució del model**:
```
1. TenantAIConfig té preferredModel? → usa aquell
2. SystemConfig té AI_MODEL_<TASK>? → usa aquell
3. Fallback: claude-sonnet-4-20250514
```

### 8. Categoria "STORAGE" (default global)

Defineix **on s'emmagatzemen els fitxers per defecte** (MinIO intern o storage del client). Si el tenant no té cap storage connectat (Mòdul 38), s'usa aquest default.

| Clau | Tipus | Defecte | Descripció |
|------|-------|---------|------------|
| `STORAGE_DEFAULT_PROVIDER` | string | `minio` | Proveïdor per defecte: `minio`, `google_drive` |
| `STORAGE_DOCUMENTS` | string | `minio` | On es guarden PDFs de documents generats |
| `STORAGE_LOGOS` | string | `minio` | On es guarden logos d'empresa |
| `STORAGE_IMAGES` | string | `minio` | On es guarden imatges pujades (Assets) |
| `STORAGE_BACKUPS` | string | `minio` | On es guarden còpies de seguretat |
| `STORAGE_MAX_FILE_SIZE_MB` | number | `50` | Mida màxima de fitxer en MB |
| `STORAGE_RETENTION_DAYS` | number | `365` | Dies que es conserven els fitxers |

**Com funciona**: si `STORAGE_DEFAULT_PROVIDER = minio`, tots els fitxers van a MinIO. Si es canvia a `google_drive`, els fitxers nous van a Google Drive (els existents no es mouen). Quan un tenant connecti el seu propi storage (Mòdul 38), overrideja aquest default global.

---

## Endpoints API

Tots sota `/api/v1/admin/system-config`, requereixen `SUPER_ADMIN`.

| Mètode | Path | Descripció |
|--------|------|------------|
| `GET` | `/` | Llista amb estat i metadades |
| `GET` | `/{key}` | Detall d'una clau (valor + metadades) |
| `PUT` | `/{key}` | Guarda valor (amb validació + audit) |
| `DELETE` | `/{key}` | Elimina de la DB (deixa de fer override a l'env var) |
| `POST` | `/{key}/test` | Testeja connexió (només per claus testejables) |
| `GET` | `/audit` | Històric de canvis (pagina't) |

---

## Migració Flyway

`V19__system_config_enhance.sql`:

```sql
ALTER TABLE system_settings
  ADD COLUMN IF NOT EXISTS value_type VARCHAR(20) NOT NULL DEFAULT 'secret',
  ADD COLUMN IF NOT EXISTS default_value TEXT,
  ADD COLUMN IF NOT EXISTS validation_rules TEXT,
  ADD COLUMN IF NOT EXISTS sort_order INTEGER NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS system_config_audit_log (
    id UUID PRIMARY KEY,
    config_key VARCHAR(80) NOT NULL,
    action VARCHAR(10) NOT NULL,
    previous_value TEXT,
    user_id UUID,
    user_email VARCHAR(150),
    ip VARCHAR(45),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_scal_config_key ON system_config_audit_log(config_key);
CREATE INDEX idx_scal_changed_at ON system_config_audit_log(changed_at);
```

---

## No inclou

- **Configuració per tenant** — això és un altre sistema (Mòdul 38 storage, TenantAIConfig, etc.)
- **Feature flags** — ja existeix el Mòdul 31 (Agent Feature Toggles)
- **Panell d'usuari final** — només SUPER_ADMIN pot tocar system config

---

## Resultat esperat

- L'admin pot configurar la plataforma des d'una UI amb tipus, validació i cerca
- Cada canvi queda registrat (qui, quan, valor anterior)
- Les claus noves (maintenance mode, nom plataforma) són immediatament funcionals
- El backend valida abans de guardar → menys errors de configuració
- Les variables d'entorn segueixen tenint prioritat absoluta
