# Configuració Google Drive AMG (Service Account)

Aquesta guia descriu el procés complet per connectar el Drive d'AMG mitjançant un Service Account (SA), que permet exportar plantilles de documents a una carpeta centralitzada `Tenants/` al Drive d'AMG.

---

## Cas A — Ja tens `GOOGLE_CALENDAR_SA_JSON` configurat

El SA del Calendar i el de Drive són el mateix. Només cal habilitar una API addicional.

**1. Habilita Drive API al Google Cloud**

```
console.cloud.google.com
→ Selecciona el projecte existent (el mateix del Calendar)
→ APIs i serveis → Biblioteca
→ Cerca: "Google Drive API"
→ Habilitar
```

**2. Comprova al portal AMG**

```
Portal AMG → Configuració del sistema
→ Verifica que GOOGLE_CALENDAR_SA_JSON té valor
```

**3. Fes la primera exportació**

```
Portal AMG → Documents → qualsevol plantilla → botó 🗄
```

El sistema crearà automàticament la carpeta `Tenants/` al Drive del SA i desarà l'ID a `GOOGLE_DRIVE_AMG_TENANTS_FOLDER_ID` a la configuració del sistema.

---

## Cas B — Setup des de zero (sense SA)

### Pas 1 — Crea el projecte a Google Cloud

```
console.cloud.google.com
→ Selector de projectes (dalt a l'esquerra) → Nou projecte
→ Nom: "AMG Platform"
→ Crear
```

### Pas 2 — Habilita les APIs necessàries

```
APIs i serveis → Biblioteca → Habilita una per una:
  ✓ Google Calendar API
  ✓ Google Drive API
```

### Pas 3 — Crea el Service Account

```
IAM i administració → Comptes de servei
→ + Crear compte de servei
→ Nom: "amg-platform-sa"
→ ID: amg-platform-sa (s'omple sol)
→ Descripció: "SA per a Calendar i Drive d'AMG Digitalització"
→ Continuar

→ Rol: no cal assignar cap rol (el SA accedeix al seu propi Drive)
→ Continuar → Fet
```

### Pas 4 — Descarrega la clau JSON

```
Clica sobre el SA creat (amg-platform-sa@...)
→ Pestanya "Claus"
→ Afegir clau → Crear clau nova → JSON
→ Es descarrega automàticament: amg-platform-sa-xxxx.json
```

El fitxer té aquesta forma:
```json
{
  "type": "service_account",
  "project_id": "amg-platform",
  "private_key_id": "...",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "amg-platform-sa@amg-platform.iam.gserviceaccount.com",
  "client_id": "...",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token"
}
```

### Pas 5 — Configura al portal AMG

```
Portal AMG → Configuració del sistema
→ GOOGLE_CALENDAR_SA_JSON → enganxa tot el contingut del fitxer JSON
→ Desar
```

---

## Compartir la carpeta Tenants/ amb el teu compte Google

El Drive del SA és invisible per defecte. Per veure'l des del teu Gmail/Drive personal:

**Opció 1 — Via portal AMG (recomanat)**

```
Portal AMG → Configuració del sistema → secció "Drive AMG — Accés a la carpeta Tenants/"
→ Introdueix l'email Google de l'usuari
→ Tria el permís (Llegir / Editar)
→ Compartir
```

**Opció 2 — Manual (1 vegada)**

```
1. Fes una primera exportació des del portal
   → Anota l'ID de GOOGLE_DRIVE_AMG_TENANTS_FOLDER_ID (Configuració del sistema)

2. Obre Google Drive del SA via API Explorer:
   developers.google.com/drive/api/reference/rest/v3/permissions/create

   → Try this method:
     fileId: {GOOGLE_DRIVE_AMG_TENANTS_FOLDER_ID}
     role: reader  (o writer si vols editar)
     type: user
     emailAddress: el-teu-email@gmail.com

3. A partir d'ara apareix a drive.google.com → "Compartit amb mi"
```

**Opció 3 — Via cURL**

```bash
# Primer obtén un access token del SA (o usa el portal per exportar i copia el token dels logs)
ACCESS_TOKEN="..."
FOLDER_ID="{GOOGLE_DRIVE_AMG_TENANTS_FOLDER_ID}"
YOUR_EMAIL="el-teu-email@gmail.com"

curl -X POST \
  "https://www.googleapis.com/drive/v3/files/${FOLDER_ID}/permissions" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"role":"reader","type":"user","emailAddress":"'"${YOUR_EMAIL}"'"}'
```

---

## Estructura resultant al Drive d'AMG

```
Drive del SA (amg-platform-sa@...)
└── Tenants/                          ← ID desat a GOOGLE_DRIVE_AMG_TENANTS_FOLDER_ID
    ├── Restaurant Can Pep/           ← creat automàticament en la primera exportació
    │   ├── Pressupost — plantilla    ← Google Docs editable
    │   └── Factura — plantilla
    ├── Perruqueria Maria/
    │   └── Pressupost — plantilla
    └── ...
```

---

## Variables de configuració relacionades

| Clau | Descripció |
|------|-----------|
| `GOOGLE_CALENDAR_SA_JSON` | JSON complet del Service Account (Calendar + Drive) |
| `GOOGLE_DRIVE_AMG_TENANTS_FOLDER_ID` | ID de la carpeta arrel `Tenants/` (s'omple automàticament en la primera exportació) |
| `GOOGLE_OAUTH_CLIENT_ID` | Per a OAuth de tenants (Drive/Calendar/Gmail/Sheets del client) |
| `GOOGLE_OAUTH_CLIENT_SECRET` | Per a OAuth de tenants |

---

## Solució de problemes

**Error: "SA Drive token error"**
→ Drive API no habilitada al projecte Google Cloud. Segueix el Pas 2.

**Error: "GOOGLE_CALENDAR_SA_JSON no configurat"**
→ Afegeix el JSON del SA a Configuració del sistema.

**La carpeta Tenants/ no apareix al meu Drive**
→ El Drive del SA és privat. Comparteix la carpeta seguint la secció anterior.

**Les exportacions van a llocs inesperats**
→ Esborra `GOOGLE_DRIVE_AMG_TENANTS_FOLDER_ID` de system_settings perquè es torni a crear.
