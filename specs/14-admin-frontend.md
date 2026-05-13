# Mòdul 14: Admin Frontend (CRUD usuaris i tenants)

> **Versió:** 1.0
> **Data:** 2026-05-12
> **Autor:** [per determinar]
> **Dependències:** Mòdul 01 (Auth — backend d'usuaris i tenants)

---

## 1. Objectius

- Proporcionar interfície d'administració d'usuaris des del portal frontend.
- Proporcionar interfície d'administració de tenants des del portal frontend.
- Respectar els permisos per rol (SUPER_ADMIN / ADMIN / CLIENT).
- Validació de camps als formularis abans d'enviar al backend.
- Confirmació en accions destructives (eliminar usuari).
- Feedback visual d'operacions (indicador de càrrega, missatge d'èxit/error).

---

## 2. Abast

### 2.1 Funcionalitats incloses

- Llistat paginat d'usuaris amb cerca i filtre per rol.
- Crear usuari (SUPER_ADMIN: qualsevol rol, ADMIN: només CLIENT).
- Editar usuari (email, nom, rol, tenant, estat actiu/inactiu).
- Eliminar usuari (desactivació lògica — només SUPER_ADMIN).
- Desbloquejar usuari blocat (SUPER_ADMIN i ADMIN).
- Llistat paginat de tenants amb cerca.
- Crear tenant (només SUPER_ADMIN).
- Editar tenant (SUPER_ADMIN i ADMIN).
- Sidebar del portal amb enllaços d'admin (només visible per SUPER_ADMIN/ADMIN).
- Totes les pantalles en els 4 idiomes (ca, es, en, de).

### 2.2 Funcionalitats excloses

- Eliminació de tenants (el backend no té endpoint DELETE).
- Canvi de contrasenya d'un altre usuari des de la interfície (l'usuari es canvia la seva pròpia contrasenya via forgot-password).
- Gestió de permisos granulars més enllà dels 3 rols existents.
- Exportació CSV/PDF de llistats.
- Notificacions push/email en crear/eliminar usuaris.

### 2.3 Actors

| Actor | Descripció | Permisos admin |
|-------|-----------|----------------|
| SUPER_ADMIN | Propietari de la plataforma | Accés total: crear/editar/eliminar usuaris i tenants, desbloquejar |
| ADMIN | Personal operatiu | Crear/editar usuaris (només CLIENT), editar tenants, desbloquejar. NO pot eliminar ni crear tenants. |
| CLIENT | Usuari final del tenant | Sense accés a la secció d'admin. Redirigit al dashboard. |

---

## 3. Rutes del frontend

```
/[locale]/portal/admin/users              → Llistat d'usuaris
/[locale]/portal/admin/users/new           → Formulari crear usuari
/[locale]/portal/admin/users/[id]          → Formulari editar usuari + detall
/[locale]/portal/admin/tenants             → Llistat de tenants
/[locale]/portal/admin/tenants/new         → Formulari crear tenant
/[locale]/portal/admin/tenants/[id]        → Formulari editar tenant
```

Totes les rutes estan dins del grup `[locale]/portal/admin/` i hereten el layout del portal (sidebar + topbar).

### 3.1 Protecció de rutes

- Un `AdminLayout` embolica totes les rutes d'admin i verifica que l'usuari tingui rol SUPER_ADMIN o ADMIN.
- Si l'usuari és CLIENT, es redirigeix a `/[locale]/portal` (dashboard).
- Si l'usuari no està autenticat, es redirigeix a `/[locale]/login`.

---

## 4. Components del frontend

### 4.1 Serveis API

#### `src/services/userService.ts`

| Funció | Mètode | Path | Descripció |
|--------|--------|------|-----------|
| `listUsers(params)` | GET | `/users?page=&size=&role=&tenantId=&search=` | Llistar usuaris paginats |
| `getUser(id)` | GET | `/users/{id}` | Obtenir usuari per ID |
| `createUser(data)` | POST | `/users` | Crear usuari |
| `updateUser(id, data)` | PUT | `/users/{id}` | Actualitzar usuari |
| `deleteUser(id)` | DELETE | `/users/{id}` | Desactivar usuari |
| `unlockUser(id)` | POST | `/users/{id}/unlock` | Desbloquejar usuari |

#### `src/services/tenantService.ts`

| Funció | Mètode | Path | Descripció |
|--------|--------|------|-----------|
| `listTenants(params)` | GET | `/tenants?page=&size=&search=` | Llistar tenants paginats |
| `getTenant(id)` | GET | `/tenants/{id}` | Obtenir tenant per ID |
| `createTenant(data)` | POST | `/tenants` | Crear tenant |
| `updateTenant(id, data)` | PUT | `/tenants/{id}` | Actualitzar tenant |

### 4.2 Components d'UI reutilitzables

#### `src/components/admin/DataTable.tsx`

Taula reutilitzable amb:
- Configuració de columnes (label, render, width opcional)
- Paginació (navegació anterior/següent, indicador de pàgina)
- Estat de càrrega (skeleton/spinner)
- Estat buit (missatge "no results")
- Suport per a accions per fila (botons)

**Props:**
```typescript
interface Column<T> {
  label: string;
  render: (item: T) => React.ReactNode;
  className?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  loading?: boolean;
  emptyMessage?: string;
}
```

#### `src/components/admin/UserStatusBadge.tsx`

Badge que mostra l'estat d'un usuari amb colors:
- **Actiu** (`isActive && !isBlocked`): verd (success)
- **Blocat** (`isBlocked`): vermell (danger)
- **Inactiu** (`!isActive`): gris (neutral)

Utilitza `AMGBadge` internament.

### 4.3 Components de pàgina

Cada pàgina es un `'use client'` component que:
- Carrega dades via `useEffect` + servei corresponent
- Mostra `loading` state (spinner)
- Mostra `error` state (missatge d'error amb botó de reintentar)
- Mostra `empty` state si no hi ha dades
- Configuració de cerca/filtre via query params de URL

#### Pàgina: Llistat d'usuaris

URL: `/[locale]/portal/admin/users/page.tsx`

- Input de cerca (debounce 300ms) que filtra per email/nom
- Selector de filtre per rol (tots / SUPER_ADMIN / ADMIN / CLIENT)
- `DataTable` amb columnes: email, nom, rol (badge), tenant, estat (UserStatusBadge), última connexió, accions
- Accions per fila: Editar (sempre), Desbloquejar (només si blocat), Eliminar (només SUPER_ADMIN)
- Botó "Nou usuari" a dalt a la dreta (visible per SUPER_ADMIN/ADMIN)
- `search` es passa com a query param del backend

#### Pàgina: Formulari d'usuari (crear/editar)

URLs:
- `/[locale]/portal/admin/users/new/page.tsx` (crear)
- `/[locale]/portal/admin/users/[id]/page.tsx` (editar)

- **Crear:** `UserForm` amb camps buits, valida abans d'enviar
- **Editar:** Carrega dades de l'usuari, `UserForm` prepopulat
- Camps:
  - Email (required, email validation)
  - Contrasenya (required només en crear, opcional en editar, min 4 chars)
  - Nom (required)
  - Rol (select: SUPER_ADMIN / ADMIN / CLIENT — només visible si l'usuari actual és SUPER_ADMIN; ADMIN veu select només CLIENT)
  - Tenant (select amb llistat de tenants — obligatori si rol=CLIENT, ocult si SUPER_ADMIN/ADMIN)
  - Actiu (checkbox/toggle — només en editar)
- Botons: "Guardar" (primary) + "Cancel·lar" (ghost, torna al llistat)
- En crear: `POST /api/v1/users` → redirect a llistat amb missatge èxit
- En editar: `PUT /api/v1/users/{id}` → redirect a llistat amb missatge èxit
- Errors del backend es mostren inline (email duplicat, etc.)

#### Pàgina: Llistat de tenants

URL: `/[locale]/portal/admin/tenants/page.tsx`

- Input de cerca (debounce 300ms) que filtra per nom/slug
- `DataTable` amb columnes: nom, slug, email, telèfon, estat (actiu/inactiu), creat, accions
- Accions per fila: Editar (sempre)
- Botó "Nou tenant" a dalt a la dreta (només visible per SUPER_ADMIN)

#### Pàgina: Formulari de tenant (crear/editar)

URLs:
- `/[locale]/portal/admin/tenants/new/page.tsx` (crear — només SUPER_ADMIN)
- `/[locale]/portal/admin/tenants/[id]/page.tsx` (editar)

- Camps: nom (required), slug (required, URL-friendly), email, telèfon, adreça
- En crear: `POST /api/v1/tenants` → redirect a llistat
- En editar: `PUT /api/v1/tenants/{id}` → redirect a llistat
- Si un ADMIN intenta accedir a `/new`, es redirigeix al llistat
- El llistat d'admin/tenants no mostra botó "Nou tenant" per ADMIN

### 4.4 Sidebar del portal

Al sidebar del portal, s'afegeix una secció "Administració" (després de "El meu compte"):

- **Usuaris** (icona Users) → `/[locale]/portal/admin/users` — només SUPER_ADMIN/ADMIN
- **Tenants** (icona Building) → `/[locale]/portal/admin/tenants` — només SUPER_ADMIN/ADMIN

Aquests enllaços es mostren condicionalment basant-se en el rol de l'usuari loguejat.

---

## 5. Confirmacions i feedback

### 5.1 Confirmació d'eliminació

En clicar "Eliminar usuari":
1. Es mostra un diàleg de confirmació: "Estàs segur d'eliminar aquest usuari? Aquesta acció desactivarà el seu compte."
2. Botons: "Cancel·lar" (ghost) + "Eliminar" (danger)
3. En confirmar: `DELETE /api/v1/users/{id}` → actualitza el llistat

### 5.2 Feedback d'operacions

- Operació exitosa: missatge de confirmació breu a la part superior de la pàgina, amb fons verd
- Error de validació: missatge d'error inline al camp corresponent
- Error del backend (400/403/409): missatge d'error a la part superior
- Error de xarxa/server (500): missatge genèric + botó de reintentar

---

## 6. Traduccions

S'afegeix la secció `"admin"` als fitxers `messages/{ca,es,en,de}.json`:

```json
{
  "admin": {
    "title": "Administració",
    "users": {
      "title": "Usuaris",
      "new": "Nou usuari",
      "edit": "Editar usuari",
      "delete": "Eliminar usuari",
      "deleteConfirm": "Estàs segur d'eliminar aquest usuari? Aquesta acció desactivarà el seu compte.",
      "deleteCancel": "Cancel·lar",
      "deleteConfirmBtn": "Eliminar",
      "unlock": "Desbloquejar",
      "unlockSuccess": "Usuari desblocat correctament",
      "created": "Usuari creat correctament",
      "updated": "Usuari actualitzat correctament",
      "deleted": "Usuari eliminat correctament",
      "fields": {
        "email": "Email",
        "name": "Nom",
        "role": "Rol",
        "tenant": "Tenant",
        "password": "Contrasenya",
        "isActive": "Actiu"
      },
      "status": {
        "active": "Actiu",
        "blocked": "Blocat",
        "inactive": "Inactiu"
      },
      "roles": {
        "SUPER_ADMIN": "Super Admin",
        "ADMIN": "Admin",
        "CLIENT": "Client"
      },
      "search": "Cercar usuaris...",
      "noResults": "No s'han trobat usuaris",
      "lastLogin": "Última connexió",
      "never": "Mai"
    },
    "tenants": {
      "title": "Tenants",
      "new": "Nou tenant",
      "edit": "Editar tenant",
      "created": "Tenant creat correctament",
      "updated": "Tenant actualitzat correctament",
      "fields": {
        "name": "Nom",
        "slug": "Slug",
        "email": "Email",
        "phone": "Telèfon",
        "address": "Adreça",
        "isActive": "Actiu"
      },
      "search": "Cercar tenants...",
      "noResults": "No s'han trobat tenants"
    },
    "sidebar": {
      "users": "Usuaris",
      "tenants": "Tenants"
    },
    "save": "Guardar",
    "cancel": "Cancel·lar",
    "creating": "Creant...",
    "saving": "Guardant..."
  }
}
```

---

## 7. Fix API prefix

Canviar `API_BASE` de `'/api'` a `'/api/v1'` a `src/services/api.ts` perquè les crides al backend coincideixin amb els paths reals dels controllers.

**Impacte:** Totes les crides existents (`/auth/login`, `/auth/refresh`, etc.) passaran de `/api/auth/login` a `/api/v1/auth/login`, que és el path correcte del backend.

---

## 8. Tests (QA)

### 8.1 Test cases funcionals

| # | Cas | Acció | Resultat esperat |
|---|-----|-------|-----------------|
| 1 | Llistat usuaris | Navegar a /ca/portal/admin/users | Taula amb usuaris, paginació visible |
| 2 | Cerca usuaris | Escriure email a la cerca | La taula es filtra (debounce 300ms) |
| 3 | Crear usuari CLIENT | Omplir formulari correcte + submit | Redirigeix a llistat, missatge èxit |
| 4 | Crear usuari email duplicat | Email que ja existeix | Error inline al camp email |
| 5 | Crear usuari sense tenant (rol CLIENT) | No seleccionar tenant | Error: tenant requerit per CLIENT |
| 6 | Editar usuari | Canviar nom + submit | Redirigeix a llistat, missatge èxit |
| 7 | Eliminar usuari (SUPER_ADMIN) | Clic eliminar + confirmar | Usuari desactivat, missatge èxit |
| 8 | ADMIN no veu botó eliminar | Login com ADMIN | Botó eliminar no visible |
| 9 | ADMIN no veu botó Nou Tenant | Login com ADMIN | Botó no visible, /new redirigeix |
| 10 | Desbloquejar usuari | Clic desbloquejar en usuari blocat | Missatge èxit, estat canvia |
| 11 | Llistat tenants | Navegar a /ca/portal/admin/tenants | Taula amb tenants, paginació visible |
| 12 | Crear tenant (SUPER_ADMIN) | Omplir formulari + submit | Redirigeix a llistat, missatge èxit |
| 13 | Crear tenant slug duplicat | Slug que ja existeix | Error inline |
| 14 | CLIENT no veu secció admin | Login com CLIENT | Sidebar sense enllaços admin, /admin redirigeix |
| 15 | Pàgina admin en 4 idiomes | ca/es/en/de | Texts traduïts, formularis funcionen |
| 16 | Error de xarxa | Aturar backend, fer petició | Missatge error + botó reintentar |
| 17 | Llistat buit | Cerca sense resultats | Missatge "No s'han trobat usuaris" |

---

## 9. Dependències entre mòduls

| Mòdul | Dependència | Tipus |
|-------|-----------|-------|
| Mòdul 14 (Admin Frontend) | Mòdul 01 (Auth backend) | Forta — necessita endpoints d'usuaris i tenants |
| Mòdul 14 (Admin Frontend) | Mòdul 13 (i18n) | Forta — necessita next-intl i estructura de rutes `/[locale]/` |

---

## 10. Obert / Pendents

- [ ] Decidir si les notificacions d'èxit/error es fan amb un component toast o missatge inline al capçal de la pàgina
- [ ] Confirmar si ADMIN pot editar el rol d'un usuari CLIENT (backend ho permet? o el força a CLIENT?)
- [ ] Confirmar si ADMIN pot veure el botó "Desbloquejar" (backend: sí, POST /users/{id}/unlock permet ADMIN)
- [ ] Confirmar el comportament exacte de `sessionStorage.getItem('user')` per obtenir el rol (format de l'objecte guardat al login)
