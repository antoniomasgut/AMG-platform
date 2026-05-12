# Mòdul 01: Auth (JWT + RBAC)

> **Versió:** 1.0
> **Data:** 2026-05-12
> **Autor:** [per determinar]
> **Dependències:** Cap (mòdul fonacional)

---

## 1. Objectius

- Proporcionar autenticació segura via JWT (access token + refresh token) per a tota la plataforma.
- Implementar control d'accés basat en rols (RBAC) amb tres rols: `SUPER_ADMIN`, `ADMIN` i `CLIENT`.
- Gestionar el cicle de vida de les sessions: login, refresh, logout, bloqueig per intents fallits i reset de contrasenya.
- Suport multi-tenant amb esquema per tenant a PostgreSQL.

---

## 2. Abast

### 2.1 Funcionalitats incloses

- Login amb email + contrasenya
- Generació i rotació de JWT access token (curta durada) + refresh token (llarga durada)
- Logout amb invalidació de refresh token
- Refresh d'access token
- Bloqueig de compte després de 3 intents fallits consecutius
- Desbloqueig manual per part de SUPER_ADMIN
- Reset de contrasenya via email (token temporal)
- Creació i gestió d'usuaris per part de SUPER_ADMIN
- Creació i gestió de tenants per part de SUPER_ADMIN
- Rate limiting al endpoint de login

### 2.2 Funcionalitats excloses

- OAuth social (Google, LinkedIn, etc.) — no previst en aquesta versió
- Registre d'usuaris autoservei (tots els usuaris els crea SUPER_ADMIN)
- Autenticació via API Key (es podria afegir en un mòdul futur)
- 2FA / MFA

### 2.3 Actors

| Actor | Descripció | Permisos |
|-------|-----------|----------|
| SUPER_ADMIN | Propietari de la plataforma. Gestiona admins, tenants i usuaris clients. | Accés total a totes les operacions de la plataforma |
| ADMIN | Personal operatiu de la plataforma. Gestiona clients i configuració de tenants. | Gestionar usuaris CLIENT, assignar perfils i configurar credencials, veure i modificar tenants. NO pot crear/modificar ADMINs ni eliminar tenants. |
| CLIENT | Usuari final d'un tenant. Accés només de lectura a la seva informació i la del seu tenant. | Veure el seu perfil i dades del seu tenant. Sense permisos d'escriptura ni accés a altres tenants |

---

## 3. Model de dades

### 3.1 Entitats (PostgreSQL)

#### Tenant

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | Identificador únic del tenant |
| name | String(100) | @Column(nullable=false) | Nom del negoci/empresa |
| slug | String(60) | @Column(unique, nullable=false) | Identificador URL-friendly del tenant |
| email | String(150) | @Column | Email de contacte del tenant |
| phone | String(20) | @Column | Telèfon de contacte |
| address | String(255) | @Column | Adreça fiscal |
| isActive | Boolean | @Column(nullable=false) | Si el tenant està actiu |
| createdAt | Instant | @CreatedDate | Data de creació |
| updatedAt | Instant | @LastModifiedDate | Data de darrera modificació |

#### User

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | Identificador únic de l'usuari |
| tenantId | UUID | @Column (nullable=true) | Tenant al qual pertany. NULL per a SUPER_ADMIN (usuari global) |
| email | String(150) | @Column(unique, nullable=false) | Email d'inici de sessió |
| passwordHash | String(255) | @Column(nullable=false) | Hash BCrypt de la contrasenya |
| name | String(100) | @Column(nullable=false) | Nom complet de l'usuari |
| role | Enum | @Enumerated(STRING) @Column(nullable=false) | Rol RBAC: `SUPER_ADMIN` o `CLIENT` |
| isActive | Boolean | @Column(nullable=false) | Si l'usuari està actiu (pot iniciar sessió) |
| isBlocked | Boolean | @Column(nullable=false) | Si el compte està blocat per intents fallits |
| failedAttempts | Integer | @Column(nullable=false) | Nombre d'intents fallits consecutius (0-3) |
| lastLoginAt | Instant | @Column | Data del darrer inici de sessió correcte |
| passwordChangedAt | Instant | @Column | Data del darrer canvi de contrasenya |
| createdAt | Instant | @CreatedDate | Data de creació |
| updatedAt | Instant | @LastModifiedDate | Data de darrera modificació |

**Relacions:**
- Un `Tenant` té molts `User` (1:N)
- Un `User` pot pertànyer a zero o un `Tenant` (N:1) — si el rol és SUPER_ADMIN, `tenantId` és NULL

### 3.2 Entitats (Redis — dades volatils)

Les següents entitats NO es persisteixen a PostgreSQL, només a Redis perquè tenen cicles de vida curts i requisits de baixa latència.

#### RefreshToken (Redis)

| Camp | Tipus | Descripció |
|------|-------|-----------|
| id | String (UUID) | Identificador únic del token |
| userId | UUID | Usuari propietari del token |
| tokenHash | String | Hash del refresh token (no es guarda en clar) |
| expiresAt | Long (timestamp ms) | Data d'expiració (7 dies) |

- TTL a Redis: 7 dies (configurable)
- `key pattern`: `refresh_token:{id}`

#### LoginAttempt (Redis — rate limiting)

| Camp | Tipus | Descripció |
|------|-------|-----------|
| ip | String | IP del sol·licitant |
| email | String | Email de l'intent |
| timestamp | Long (timestamp ms) | Moment de l'intent |

- `key pattern`: `login_attempt:{email}`
- TTL: 15 minuts

#### PasswordResetToken (PostgreSQL + cache Redis opcional)

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | Identificador únic |
| userId | UUID | @Column(nullable=false) | Usuari que sol·licita el reset |
| tokenHash | String | @Column(nullable=false) | Hash del token de reset |
| expiresAt | Instant | @Column(nullable=false) | Expiració (30 minuts) |
| used | Boolean | @Column(nullable=false) | Si ja s'ha utilitzat |
| createdAt | Instant | @CreatedDate | Data de sol·licitud |

### 3.3 Schema per tenant

El sistema multi-tenant s'implementa amb **schema-per-tenant** a PostgreSQL:

- `public` — taules globals: `Tenant`, `User` (inclou SUPER_ADMIN i CLIENT amb `tenant_id`)
- Enfocament: **taula compartida amb tenantId** (shared table + discriminator column), no esquemes separats, per simplificar manteniment donat el nombre baix d'entitats i la poca aïllació requerida.
- `SUPER_ADMIN` no té `tenant_id` (NULL) i pot operar a través de tots els tenants.
- `CLIENT` té `tenant_id` assignat i només veu les dades del seu tenant.

---

## 4. API REST

### 4.1 Endpoints d'autenticació

#### `POST /api/v1/auth/login` — Inici de sessió

**Autenticació:** Pública (no requereix JWT)
**Rate limiting:** Sí, basat en email + IP

**Request:**
```json
{
  "email": "string",
  "password": "string"
}
```

**Response 200:**
```json
{
  "accessToken": "string (JWT)",
  "refreshToken": "string",
  "expiresIn": 900,
  "tokenType": "Bearer",
  "user": {
    "id": "uuid",
    "email": "string",
    "name": "string",
    "role": "SUPER_ADMIN | CLIENT",
    "tenant": { "id": "uuid", "name": "string" }
  }
}
```

**Errors:**
| Codi | Situació |
|------|---------|
| 401 | Email o contrasenya incorrectes |
| 403 | Compte blocat (3 intents fallits) |
| 403 | Compte inactiu |
| 429 | Massa intents (rate limiting) |

**Lògica interna:**
1. Comprovar si l'email existeix a la base de dades
2. Si el compte està blocat → retornar 403
3. Si el compte està inactiu → retornar 403
4. Verificar contrasenya amb BCrypt
5. Si contrasenya incorrecta → incrementar `failedAttempts`
6. Si `failedAttempts >= 3` → blocar el compte
7. Si contrasenya correcta → resetjar `failedAttempts = 0`, actualitzar `lastLoginAt`
8. Generar access token (15 min) + refresh token (7 dies)
9. Emmagatzemar refresh token a Redis

---

#### `POST /api/v1/auth/refresh` — Refrescar access token

**Autenticació:** Refresh token (al body)

**Request:**
```json
{
  "refreshToken": "string"
}
```

**Response 200:**
```json
{
  "accessToken": "string (nou JWT)",
  "refreshToken": "string (nou refresh token, rotació)",
  "expiresIn": 900,
  "tokenType": "Bearer"
}
```

**Errors:**
| Codi | Situació |
|------|---------|
| 401 | Refresh token invàlid o expirat |
| 403 | Refresh token ja utilitzat (possible robatori de token) |

**Lògica interna:**
1. Recuperar el refresh token de Redis pel seu ID
2. Verificar hash i expiració
3. Eliminar el token vell de Redis (rotació: single-use)
4. Generar nou parell access + refresh token
5. Emmagatzemar el nou refresh token a Redis

---

#### `POST /api/v1/auth/logout` — Tancar sessió

**Autenticació:** Bearer JWT

**Request:**
```json
{
  "refreshToken": "string"
}
```

**Response 200:**
```json
{}
```

**Errors:**
| Codi | Situació |
|------|---------|
| 401 | JWT invàlid |
| 404 | Refresh token no trobat |

**Lògica interna:**
1. Eliminar el refresh token de Redis
2. (Opcional) Afegir l'access token a una blacklist Redis amb TTL fins la seva expiració natural

---

#### `POST /api/v1/auth/forgot-password` — Sol·licitar reset de contrasenya

**Autenticació:** Pública

**Request:**
```json
{
  "email": "string"
}
```

**Response 200 (sempre el mateix per no revelar emails existents):**
```json
{
  "message": "S'ha enviat un enllaç de recuperació al teu email"
}
```

**Lògica interna:**
1. Buscar usuari per email (si no existeix, retornar 200 igualment)
2. Generar token de reset (UUID aleatori, 30 min d'expiració)
3. Emmagatzemar `PasswordResetToken` a PostgreSQL
4. Enviar email amb enllaç: `https://{tenant-domain}/reset-password?token={token}`
5. Si l'usuari no existeix, no enviar email (però retornar 200)

---

#### `POST /api/v1/auth/reset-password` — Resetjar contrasenya

**Autenticació:** Token de reset (al body)

**Request:**
```json
{
  "token": "string",
  "newPassword": "string (min 4 chars)"
}
```

**Response 200:**
```json
{
  "message": "Contrasenya actualitzada correctament"
}
```

**Errors:**
| Codi | Situació |
|------|---------|
| 400 | Token invàlid o expirat |
| 400 | Token ja utilitzat |
| 400 | Contrasenya no compleix requisits |

**Lògica interna:**
1. Buscar `PasswordResetToken` pel hash del token rebut
2. Verificar expiració i que no estigui marcat com `used`
3. Hashejar nova contrasenya amb BCrypt
4. Actualitzar `passwordHash` i `passwordChangedAt` de l'usuari
5. Marcar el `PasswordResetToken` com a `used`
6. Invalidar tots els refresh tokens existents de l'usuari a Redis (logout forçat)

---

### 4.2 Endpoints de gestió d'usuaris (SUPER_ADMIN només)

#### `POST /api/v1/users` — Crear usuari (SUPER_ADMIN o CLIENT)

**Autenticació:** Bearer JWT
**Rols permesos:** SUPER_ADMIN (qualsevol rol), ADMIN (només CLIENT)

**Request:**
```json
{
  "email": "string",
  "password": "string (min 4 chars)",
  "name": "string",
  "role": "SUPER_ADMIN | ADMIN | CLIENT",
  "tenantId": "uuid (obligatori si role=CLIENT, ignorat si role=SUPER_ADMIN o ADMIN)"
}
```

**Lògica interna:**
- Si el creador és ADMIN, el camp `role` es força a CLIENT (encara que enviïn un altre valor)
- Si el creador és SUPER_ADMIN, permet crear qualsevol rol
```

**Response 201:**
```json
{
  "id": "uuid",
  "email": "string",
  "name": "string",
  "role": "SUPER_ADMIN | CLIENT",
  "tenant": { "id": "uuid", "name": "string" },
  "isActive": true,
  "createdAt": "instant"
}
```

**Errors:**
| Codi | Situació |
|------|---------|
| 400 | Email ja existeix |
| 400 | Contrasenya no compleix requisits |
| 400 | Tenant no especificat per a rol CLIENT |
| 401 | JWT invàlid |
| 403 | No tens permís (no ets SUPER_ADMIN) |

---

#### `GET /api/v1/users` — Llistar usuaris

**Autenticació:** Bearer JWT
**Rols permesos:** SUPER_ADMIN, ADMIN

**Response 200:**
```json
{
  "content": [
    {
      "id": "uuid",
      "email": "string",
      "name": "string",
      "role": "SUPER_ADMIN | CLIENT",
      "tenant": { "id": "uuid", "name": "string" },
      "isActive": true,
      "isBlocked": false,
      "lastLoginAt": "instant",
      "createdAt": "instant"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

---

#### `GET /api/v1/users/{id}` — Veure usuari

**Autenticació:** Bearer JWT
**Rols permesos:** SUPER_ADMIN (qualsevol usuari), ADMIN (qualsevol usuari), CLIENT (només el seu propi perfil)

---

#### `PUT /api/v1/users/{id}` — Actualitzar usuari

**Autenticació:** Bearer JWT
**Rols permesos:** SUPER_ADMIN (qualsevol usuari, pot canviar el rol), ADMIN (només CLIENT, no pot canviar el rol), CLIENT (només el seu propi nom/email)

**Request (SUPER_ADMIN):**
```json
{
  "email": "string",
  "name": "string",
  "role": "SUPER_ADMIN | ADMIN | CLIENT",
  "tenantId": "uuid",
  "isActive": true
}
```

**Request (ADMIN):**
```json
{
  "email": "string",
  "name": "string",
  "isActive": true
}
```

**Request (CLIENT):**
```json
{
  "email": "string",
  "name": "string"
}
```

---

#### `DELETE /api/v1/users/{id}` — Eliminar usuari (desactivació lògica)

**Autenticació:** Bearer JWT
**Rols permesos:** SUPER_ADMIN

**Lògica interna:** No s'elimina el registre. Es marca `isActive = false` i s'invaliden els refresh tokens.

---

#### `POST /api/v1/users/{id}/unlock` — Desblocar usuari

**Autenticació:** Bearer JWT
**Rols permesos:** SUPER_ADMIN, ADMIN

**Response 200:**
```json
{
  "message": "Usuari desblocat correctament"
}
```

**Lògica interna:** `failedAttempts = 0`, `isBlocked = false`

---

### 4.3 Endpoints de gestió de tenants (SUPER_ADMIN només)

#### `POST /api/v1/tenants` — Crear tenant

**Autenticació:** Bearer JWT
**Rols permesos:** SUPER_ADMIN

**Request:**
```json
{
  "name": "string",
  "slug": "string",
  "email": "string (opcional)",
  "phone": "string (opcional)",
  "address": "string (opcional)"
}
```

**Response 201:**
```json
{
  "id": "uuid",
  "name": "string",
  "slug": "string",
  "email": "string",
  "phone": "string",
  "address": "string",
  "isActive": true,
  "createdAt": "instant"
}
```

---

#### `GET /api/v1/tenants` — Llistar tenants

**Autenticació:** Bearer JWT
**Rols permesos:** SUPER_ADMIN, ADMIN

**Query params:** `page`, `size`, `search` (per nom o slug)

---

#### `GET /api/v1/tenants/{id}` — Veure tenant

**Autenticació:** Bearer JWT
**Rols permesos:** SUPER_ADMIN (qualsevol), CLIENT (només el seu propi tenant)

---

#### `PUT /api/v1/tenants/{id}` — Actualitzar tenant

**Autenticació:** Bearer JWT
**Rols permesos:** SUPER_ADMIN, ADMIN

---

### 4.4 Mapa complet d'endpoints

| Mètode | Ruta | Descripció | Auth | Rols |
|--------|------|-----------|------|------|
| POST | /api/v1/auth/login | Inici de sessió | Pública | — |
| POST | /api/v1/auth/refresh | Refrescar token | Refresh token | — |
| POST | /api/v1/auth/logout | Tancar sessió | JWT | Tots |
| GET | /api/v1/auth/me | Perfil actual | JWT | Tots |
| POST | /api/v1/auth/forgot-password | Sol·licitar reset | Pública | — |
| POST | /api/v1/auth/reset-password | Resetjar contrasenya | Reset token | — |
| POST | /api/v1/users | Crear usuari | JWT | SUPER_ADMIN / ADMIN (només CLIENT) |
| GET | /api/v1/users | Llistar usuaris | JWT | SUPER_ADMIN, ADMIN |
| GET | /api/v1/users/{id} | Veure usuari | JWT | SUPER_ADMIN / ADMIN / CLIENT (propi) |
| PUT | /api/v1/users/{id} | Actualitzar usuari | JWT | SUPER_ADMIN / ADMIN (només CLIENT) / CLIENT (propi) |
| DELETE | /api/v1/users/{id} | Desactivar usuari | JWT | SUPER_ADMIN |
| POST | /api/v1/users/{id}/unlock | Desblocar usuari | JWT | SUPER_ADMIN, ADMIN |
| POST | /api/v1/tenants | Crear tenant | JWT | SUPER_ADMIN |
| GET | /api/v1/tenants | Llistar tenants | JWT | SUPER_ADMIN, ADMIN |
| GET | /api/v1/tenants/{id} | Veure tenant | JWT | SUPER_ADMIN / ADMIN / CLIENT (propi) |
| PUT | /api/v1/tenants/{id} | Actualitzar tenant | JWT | SUPER_ADMIN, ADMIN |

---

## 5. Seguretat

### 5.1 Autenticació

- **JWT access token:** Signat amb HMAC-SHA256 (secret configurat via variable d'entorn `JWT_SECRET`). Expiració: 15 minuts.
- **Refresh token:** UUID aleatori de 128 bits. Hash emmagatzemat a Redis. Expiració: 7 dies.
- **Rotació de refresh token:** Cada cop que es refresca un access token, es genera un nou refresh token i s'invalida l'anterior (protecció contra robatori de tokens).
- **Contrasenyes:** Hashades amb BCrypt (strength 12).

### 5.2 Autorització (RBAC)

- **SUPER_ADMIN:** Accés total a tots els endpoints de la plataforma. Pot gestionar usuaris (qualsevol rol), tenants i configuracions globals. Sense restriccions de tenant.
- **ADMIN:** Accés operatiu. Pot gestionar usuaris CLIENT (crear, modificar, desbloquejar), veure i modificar tenants, assignar perfils i configurar credencials. NO pot crear/modificar ADMINs ni eliminar tenants.
- **CLIENT:** Accés només de lectura a la seva informació. Pot veure el seu perfil, el seu tenant i les dades associades al seu tenant. No pot crear, modificar ni eliminar recursos.
- Implementat mitjançant `@PreAuthorize` amb `hasRole()` i `hasAnyRole()` de Spring Security.

### 5.3 Protecció de dades

- **Rate limiting:** Redis-based sliding window al endpoint `/api/v1/auth/login` (màxim 5 intents per email cada 15 minuts, màxim 20 per IP cada 15 minuts).
- **CSRF:** Protecció habilitada per a endpoints d'escriptura amb tokens CSRF.
- **XSS:** Headers de seguretat a totes les respostes (`X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `X-XSS-Protection: 1; mode=block`).
- **Validació d'input:** Zod a frontend, Bean Validation (`@Valid`, `@NotBlank`, `@Email`, etc.) a backend. Totes les entrades es sanitizen.
- **CORS:** Configurat per acceptar només els dominis dels tenants i el domini d'admin.

---

## 6. RGPD / LSSI

- **Dades personals:** email, nom, adreça, telèfon, IP (logs de login), hash de contrasenya
- **Base legal:** Execució del contracte de servei (interès legítim). Les dades són necessàries per a la prestació del servei.
- **Conservació:** Les dades d'usuari es conserven mentre el compte estigui actiu. En desactivar-lo, es conserven 3 anys per obligacions fiscals/llegals. Els tokens de sessió s'eliminen en logout o al vèncer.
- **Portabilitat:** El CLIENT pot sol·licitar exportació de les seves dades via SUPER_ADMIN (endpoint GET /api/v1/users/{id} + GET /api/v1/tenants/{id}).
- **Supressió (right to be forgotten):** SUPER_ADMIN pot sol·licitar l'eliminació completa d'un usuari. Es fa anonimització irreversible (email → `deleted-{uuid}@anonymized`, nom → `Usuari eliminat`), no eliminació física, per preservar la integritat referencial.
- **Registre d'accessos (LSSI):** Es registren tots els accessos a endpoints protegits amb timestamp, IP, userId i endpoint.

---

## 7. Tests (QA)

### 7.1 Test cases funcionals

| # | Cas | Entrada | Resultat esperat |
|---|-----|---------|-----------------|
| 1 | Login correcte | email + password vàlids | 200 + JWT + refresh token |
| 2 | Login incorrecte | email correcte + password incorrecte | 401, failedAttempts incrementat |
| 3 | Bloqueig per intents | 3 intents fallits consecutius | 403 "Compte blocat", isBlocked=true |
| 4 | Login després de bloqueig | email + password correctes però compte blocat | 403 "Compte blocat" |
| 5 | Desbloqueig per SUPER_ADMIN | POST /users/{id}/unlock | 200, isBlocked=false, failedAttempts=0 |
| 6 | Refresh token vàlid | refresh token vàlid | 200 + nou parell access+refresh |
| 7 | Refresh token expirat | refresh token > 7 dies | 401 |
| 8 | Refresh token reutilitzat | refresh token ja usat | 401 (possible robatori) |
| 9 | Logout | refresh token | 200, token eliminat de Redis |
| 10 | Forgot password (email existent) | email vàlid | 200 + email enviat |
| 11 | Forgot password (email inexistent) | email desconegut | 200 (no revela existència) |
| 12 | Reset password correcte | token vàlid + nova password | 200, login funciona amb nova password |
| 13 | Reset password expirat | token > 30 min | 400 "Token expirat" |
| 14 | Reset password token usat | token ja usat | 400 "Token ja utilitzat" |
| 15 | Crear usuari CLIENT (SUPER_ADMIN) | body complet amb tenantId | 201 + usuari creat |
| 16 | Crear usuari CLIENT sense tenantId | body sense tenantId | 400 camp obligatori |
| 17 | CLIENT intenta crear usuari | endpoint POST /users | 403 |
| 18 | CLIENT veu el seu propi perfil | GET /users/{el-seu-id} | 200 |
| 19 | CLIENT veu perfil d'altre user | GET /users/{altre-id} | 403 |
| 20 | CLIENT veu el seu propi tenant | GET /tenants/{el-seu-tenant-id} | 200 |
| 21 | ADMIN llista usuaris | GET /users | 200 + llista |
| 22 | ADMIN desbloqueja CLIENT | POST /users/{id}/unlock | 200 |
| 23 | ADMIN crea un altre ADMIN | POST /users amb role=ADMIN | 403 (el rol es força a CLIENT) |
| 24 | ADMIN modifica tenant | PUT /tenants/{id} | 200 |

### 7.2 Test cases de seguretat

| # | Cas | Entrada | Resultat esperat |
|---|-----|---------|-----------------|
| 1 | Rate limiting superat | 6 intents de login en < 15 min des del mateix email | 429 |
| 2 | JWT manipulat | access token amb signatura invàlida | 401 |
| 3 | JWT expirat | access token > 15 min | 401 |
| 4 | Accés sense token | endpoint protegit sense header Authorization | 401 |
| 5 | SQL injection | email: `' OR 1=1 --` | 401 (no injectable) |
| 6 | Password massa curta | "ab" (min 4 chars) | 400 |
| 7 | Email mal format | "not-an-email" | 400 |
| 8 | Injecció a reset token | token: `' OR '1'='1` | 400 |

### 7.3 Test cases de límits

| # | Cas | Entrada | Resultat esperat |
|---|-----|---------|-----------------|
| 1 | Contrasenya exactament 4 caràcters | "abcd" | 200 (login correcte) |
| 2 | Email de 254 caràcters | email límit RFC | 200 o 400 si no compleix format |
| 3 | Usuari sense tenant | tenantId = null | Permès només si role=SUPER_ADMIN |
| 4 | Múltiples refresh tokens simultanis | 5 refresh tokens actius per al mateix usuari | Tots vàlids, rotació independent |

---

## 8. Dependències entre mòduls

| Mòdul | Dependència | Tipus |
|-------|-----------|-------|
| Tots | Mòdul 01 (Auth) | Forta — tots els mòduls requereixen autenticació i autorització |

---

## 9. Obert / Pendents

- [ ] Decidir si els refresh tokens van a Redis o PostgreSQL (Redis per rendiment, però cal considerar persistència si Redis es reinicia)
- [ ] Decidir si el reset de password via email necessita integració amb un servei de correu (SendGrid, SES, SMTP local) o es tracta en un mòdul separat de notificacions
- [ ] Confirmar si SUPER_ADMIN pot tenir múltiples usuaris o només un (el compte propietari de la plataforma)
- [ ] Definir dominis CORS per a entorns de desenvolupament vs producció
- [ ] Definir política de contrasenyes de SUPER_ADMIN (pot ser superior a la de CLIENT?)
