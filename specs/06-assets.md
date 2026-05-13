# Mòdul 06: Assets — Gestió d'Imatges i Fitxers

> **Versió:** 1.0
> **Data:** 2026-05-13
> **Dependències:** Mòdul 01 (Auth) — tots els endpoints requereixen JWT

---

## 1. Objectius

- Gestionar la pujada, emmagatzematge i lliurament d'imatges i fitxers per a landings
- Suportar els formats d'imatge més comuns (JPEG, PNG, WebP, AVIF)
- Generar miniatures i versions redimensionades automàticament
- Aïllar fitxers per tenant (cada tenant només veu els seus fitxers)
- Integrar-se amb el Mòdul 05 Factory per al selector d'imatges
- Servir imatges amb cache i CDN per a render ràpid

---

## 2. Abast

### 2.1 Funcionalitats incloses

- Pujada d'imatges (fins a 5 MB per fitxer)
- Llistat d'imatges per tenant
- Eliminació d'imatges
- Generació automàtica de miniatures (thumbnails)
- Suport per als formats: JPEG, PNG, WebP, AVIF, GIF, SVG
- Servir imatges públiques via URL directa
- Aïllament per tenant (cada tenant veu només les seves imatges)

### 2.2 Funcionalitats excloses

- Edició d'imatges (retall, filtres, etc.) — ho fa el client abans de pujar
- Processament de vídeo o àudio
- CDN / cache avanzada (es pot afegir després amb CloudFront / Cloudflare)
- Backup i versionat de fitxers (es fa a nivell d'infra)

### 2.3 Actors

| Actor | Descripció | Permisos |
|-------|-----------|----------|
| SUPER_ADMIN | Accés a tots els fitxers de tots els tenants | CRUD complet |
| ADMIN | Accés als fitxers dels seus tenants | CRUD complet |
| CLIENT | Accés als fitxers del seu propi tenant | Pujar, llistar, eliminar els seus fitxers |

---

## 3. Model de dades

### 3.1 Emmagatzematge físic

Els fitxers s'emmagatzemen al **sistema de fitxers local** dins d'un directori configurable, organitzat per tenant:

```
{STORAGE_PATH}/
├── {tenantId}/
│   ├── abc123.jpg
│   ├── def456.png
│   └── thumbs/
│       ├── abc123_200x200.jpg
│       └── def456_200x200.png
```

En producció, `STORAGE_PATH` pot ser un volum Docker persistent o, en el futur, un bucket S3 compatible (MinIO / AWS S3).

### 3.2 Entitat Asset (PostgreSQL)

Metadades de cada fitxer pujat.

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantId | UUID | @Column(nullable=false) | Propietari del fitxer |
| originalName | String(255) | @Column(nullable=false) | Nom original del fitxer |
| mimeType | String(100) | @Column(nullable=false) | Tipus MIME (image/jpeg, etc.) |
| size | Long | @Column(nullable=false) | Mida en bytes |
| width | Integer | @Column | Amplada en píxels (si és imatge) |
| height | Integer | @Column | Alçada en píxels (si és imatge) |
| storagePath | String(500) | @Column(nullable=false) | Ruta relativa dins del storage |
| thumbnailPath | String(500) | @Column | Ruta de la miniatura |
| url | String(500) | @Column(nullable=false) | URL pública del fitxer |
| thumbnailUrl | String(500) | @Column | URL pública de la miniatura |
| isActive | Boolean | @Column(nullable=false) | Si el fitxer està actiu (no s'ha eliminat) |
| createdAt | Instant | @CreatedDate | |

### 3.3 URLs públiques

```
GET /api/v1/assets/{assetId}/file        → Fitxer original
GET /api/v1/assets/{assetId}/thumbnail   → Miniatura (200x200)
```

---

## 4. API REST

Tots els endpoints requereixen `Authorization: Bearer JWT` excepte els de servei de fitxers (que són públics perquè les landings els carreguen).

Prefix base: `/api/v1/assets`

### 4.1 Gestió de fitxers

#### `POST /api/v1/assets/upload` — Pujar fitxer

**Autenticació:** JWT (tots els rols)

**Request:** `multipart/form-data`
- `file`: El fitxer (màxim 5 MB)
- Tipus permesos: `image/jpeg`, `image/png`, `image/webp`, `image/avif`, `image/gif`, `image/svg+xml`

**Response 201:**
```json
{
  "id": "uuid",
  "originalName": "logo-restaurant.png",
  "mimeType": "image/png",
  "size": 245000,
  "width": 800,
  "height": 600,
  "url": "/api/v1/assets/uuid/file",
  "thumbnailUrl": "/api/v1/assets/uuid/thumbnail",
  "createdAt": "2026-05-13T10:00:00Z"
}
```

**Lògica:**
1. Validar tipus MIME i mida
2. Llegir el `tenantId` del JWT
3. Guardar el fitxer a `{STORAGE_PATH}/{tenantId}/{uuid}.{ext}`
4. Crear registre a la taula `Asset`
5. Generar miniatura (200x200) si és imatge rasteritzada
6. Retornar metadades

#### `GET /api/v1/assets/tenant/{tenantId}` — Llistar fitxers del tenant

**Autenticació:** JWT (SUPER_ADMIN, ADMIN, CLIENT propi)

**Response 200:**
```json
[
  {
    "id": "uuid",
    "originalName": "logo-restaurant.png",
    "mimeType": "image/png",
    "size": 245000,
    "width": 800,
    "height": 600,
    "url": "/api/v1/assets/uuid/file",
    "thumbnailUrl": "/api/v1/assets/uuid/thumbnail",
    "createdAt": "2026-05-13T10:00:00Z"
  }
]
```

#### `GET /api/v1/assets/{assetId}/file` — Servir fitxer original

**Autenticació:** Pública (les landings el carreguen)

Retorna el fitxer amb el Content-Type i cache headers adequats.

**Headers de resposta:**
- `Content-Type: image/png`
- `Cache-Control: public, max-age=31536000, immutable`
- `Content-Length: 245000`

#### `GET /api/v1/assets/{assetId}/thumbnail` — Servir miniatura

**Autenticació:** Pública

Retorna la miniatura (200x200). Si no existeix, retorna 404.

#### `DELETE /api/v1/assets/{assetId}` — Eliminar fitxer

**Autenticació:** JWT (SUPER_ADMIN, ADMIN, CLIENT propietari)

Eliminació lògica (marca `isActive = false`). El fitxer físic s'elimina si ja no hi ha referències.

---

## 5. Configuració

```yaml
# application.yml
app:
  storage:
    path: /data/assets          # Directori arrel de fitxers
    max-file-size: 5MB          # Mida màxima per fitxer
    thumbnail-width: 200        # Amplada de miniatures
    thumbnail-height: 200       # Alçada de miniatures
    allowed-types:
      - image/jpeg
      - image/png
      - image/webp
      - image/avif
      - image/gif
      - image/svg+xml
```

---

## 6. Seguretat

- **Validació de tipus MIME**: Es valida tant l'extensió com el contingut (magic bytes)
- **Mida màxima**: 5 MB per fitxer (configurable)
- **Aïllament per tenant**: El `tenantId` s'extreu del JWT, no del body de la petició
- **CLIENT**: Només veu/elimina els seus propis fitxers
- **Path traversal**: Es neteja el nom del fitxer per evitar `../../../etc/passwd`

---

## 7. Tests (QA)

### 7.1 Funcionals

| # | Cas | Resultat |
|---|-----|---------|
| 1 | Pujar imatge PNG vàlida | 201, URL accessible |
| 2 | Pujar fitxer massa gran (>5MB) | 400 |
| 3 | Pujar tipus no permès (.exe) | 400 |
| 4 | Llistar fitxers del tenant | 200, només fitxers del tenant |
| 5 | CLIENT llista fitxers d'un altre tenant | 403 |
| 6 | Accedir a fitxer per ID | 200, Content-Type correcte |
| 7 | Accedir a miniatura | 200, mida 200x200 |
| 8 | Eliminar fitxer | 204, desactivat lògicament |
| 9 | Accedir a fitxer eliminat | 404 |

---

## 8. Dependències

| Mòdul | Dependència | Tipus |
|-------|-----------|-------|
| Mòdul 01 (Auth) | Autenticació JWT | Forta |
| Mòdul 05 (Factory) | Consumeix Assets per al selector d'imatges | Forta |

---

## 9. Obert / Pendents

- [ ] Decidir si es fa servir sistema de fitxers local o MinIO des del principi
- [ ] Implementar generació de miniatures amb Thumbnailator o similar
- [ ] Afegir neteja programada de fitxers orfes (Assets eliminats fa >30 dies)
- [ ] Afegir suport per a PDF (factures, documents contractuals)
