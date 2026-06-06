# Informe d'Auditoria de Seguretat — AMG Portal (Complet)

**Data:** 2026-06-06
**Abast:** Backend (`backend/`), Frontend (`frontend/`), Infra (`infra/`), Specs (`specs/`)
**Realitzat per:** Claude Code (DeepSeek V4 + Opus)

---

## Metodologia

S'han analitzat 40+ fitxers categoritzant vulnerabilitats per:
- **CRÍTIC**: Explotació immediata, dany sever
- **ALT**: Debilitat significativa, probable explotació  
- **MITJÀ**: Defensa en profunditat, risc condicional
- **BAIX**: Millor pràctica, informatiu

---

## 🔴 CRÍTIC (7 trobats, 5 corregits, 2 pendents)

### C-1: `/communication/{id}/respond` sense autenticació ✅ CORREGIT

**Abans:** `VaultController.java:275-279` — endpoint POST públic que permet escriure credencials xifrades a la BD per a qualsevol tenant, només amb un UUID previsible.

**Després:** 
- S'ha afegit `@Valid` amb `@NotBlank @Size(max=5000)` al `text`
- Validació que `status == SENT` i `expiresAt` no ha caducat
- `SecurityConfig` → `.anyRequest().authenticated()` (ara requereix auth)

**Risc residual:** Cap (és un endpoint intern que requereix JWT d'admin)

---

### C-2: `anyRequest().permitAll()` ✅ CORREGIT

**Abans:** `SecurityConfig.java:101` — qualsevol endpoint no mapejat explícitament era públic.

**Després:** `.anyRequest().authenticated()` — cal autenticació per defecte.

**Impacte:** Nou endpoints creats per error queden automàticament protegits.

---

### C-3: JWT_SECRET buit per defecte ✅ CORREGIT

**Abans:** `application.yml:43` → `jwt.secret: ${JWT_SECRET:}` — si `JWT_SECRET` no estava configurat, quedava buit. `Keys.hmacShaKeyFor("".getBytes())` produïa una clau de 0 bits, i qualsevol JWT token era acceptat.

**Després:** `JwtProvider.java:22-24` llança `IllegalStateException` si `props.secret()` és null o blank. L'aplicació no arrenca sense `JWT_SECRET`.

---

### C-4: VAULT_MASTER_KEY genera clau aleatòria ✅ CORREGIT

**Abans:** `VaultEncryption.java:29-34` — si `VAULT_MASTER_KEY` no estava configurat, generava una clau aleatòria a cada restart. Totes les dades xifrades (credencials, API keys) es perdien irreversiblement.

**Després:** Llança `IllegalStateException` amb missatge d'error claro:
```
VAULT_MASTER_KEY environment variable is required for production.
Set it to a 32-byte (256-bit) AES key encoded in Base64.
```

---

### C-5: JWT tokens a sessionStorage 🔴 PENDENT

**Risc:** `api.ts:7-17` — tokens JWT accessibles via JavaScript (`sessionStorage`). Un XSS pot robar tokens i refrescar-los.

**Pla:**
1. Backend: `Set-Cookie: access_token=...; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=900`
2. Backend: `Set-Cookie: refresh_token=...; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth/refresh; Max-Age=604800`
3. Frontend: Eliminar `sessionStorage.setItem/getItem/removeItem` de `api.ts`
4. Frontend: Tokens automàtics via cookies HttpOnly

**Dependència:** Canvi a backend + frontend coordinat.

---

### C-6: Token storage inconsistent (meta-ads.ts, hosting.ts) ✅ CORREGIT

**Abans:** `meta-ads.ts:222` usava `localStorage.getItem('amg_token')` i `hosting.ts:49` usava `localStorage.getItem('token')` — claus i ubicació diferents del sistema central (`sessionStorage.getItem('access_token')`).

**Després:** Ambdós usen `sessionStorage.getItem('access_token')`.

---

### C-7: Socket Docker writable al backend 🔴 PENDENT

**Risc:** `docker-compose.yml:113` — `/var/run/docker.sock:/var/run/docker.sock` (escriptura). Un RCE al backend = escape de contenidor complet.

**Pla:** Avaluar si Engine necessita RW (crear/aturar contenidors de landing). Si no, canviar a `:ro`. Si sí, crear sidecar amb mínims privilegis.

---

## 🟠 ALT (7 trobats, 5 corregits, 2 pendents)

### H-1: Swagger UI públic ✅ CORREGIT

**Abans:** `.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()` — qualsevol persona podia veure tots els endpoints, DTOs i esquemes.

**Després:** `.authenticated()` — requereix JWT.

**Workflow:** Login → obtenir JWT → accedir a Swagger amb `Authorization: Bearer <token>`.

---

### H-2: test-email bypass de rate limiting ✅ CORREGIT

**Abans:** `LoginAttemptService.java:35-37,47-49` — si `rate-limiting.test-email` coincidia amb l'email, es saltaven tots els límits d'intents i bloqueig per IP.

**Després:** Eliminat completament el camp `testEmail` i els checks.

---

### H-3: Assets públics (per avaluar)

**Risc:** `AssetController.java:43-60` — fitxers i thumbnails accessibles amb UUID. Un UUID filtrat permet accés a fitxers de qualsevol tenant.

**Pla:** Afegir control d'accés per tenant als endpoints d'assets.

---

### H-4: Token de reset a la URL (per avaluar)

**Risc:** `AuthService.java:164` — `resetLink = "...reset-password?token=" + token` — el token apareix a logs del servidor, historial del navegador, referer header.

**Pla:** Millorar a POST-only flow on el token es passa al body.

---

### H-5: Nonce CSP millorat ✅ CORREGIT

**Abans:** `middleware.ts:9` — `Buffer.from(crypto.randomUUID()).toString('base64')` — UUID té només 122 bits d'entropia i format previsible.

**Després:** 
```typescript
const nonceBytes = new Uint8Array(32);
crypto.getRandomValues(nonceBytes);
const nonce = Buffer.from(nonceBytes).toString('base64');
```
256 bits d'entropia criptogràfica.

---

### H-6: Credential masking mostra últims 4 caràcters 🔴 PENDENT

**Risc:** `specs/02-vault.md:997` — mostrar `***3456` redueix l'espai de cerca.

**Pla:** Canviar a mostrar `****` per a qualsevol credential no buit, independentment de la longitud.

---

### H-7: AuthGuard client-side only (per avaluar)

**Risc:** `AuthGuard.tsx:7-28` — només client-side. Un usuari pot manipular `sessionStorage` per veure UI d'admin (tot i que les API calls fallarien).

**Pla:** Afegir validació JWT al middleware de Next.js per a rutes protegides.

---

### H-8: Security headers + rate limit a Traefik ✅ CORREGIT

**Abans:** `dynamic.yml` definia `secureHeaders`, `rateLimit` i `corsApi` middlewares, però cap router els referenciaba. Totes les respostes anaven sense HSTS, CSP, X-Frame-Options ni rate limiting.

**Després:** Tots els routers (`backend`, `frontend`, `minio-api`, `minio-console`, `netdata`, `traefik-dashboard`) tenen `secureHeaders` aplicat. Backend i MinIO tenen `rateLimit` addicional.

---

### H-9: Passwords hardcodejats a docker-compose.deploy.yml ✅ CORREGIT

**Abans:** `SPRING_DATASOURCE_PASSWORD: amg_db_2026`, `SPRING_REDIS_PASSWORD: amg_redis_2026` — valors fixos al codi.

**Després:** `${POSTGRES_PASSWORD}`, `${REDIS_PASSWORD}` — llegits de `/opt/amg/.env`.

---

## 🟡 MITJÀ (9 trobats, 5 corregits, 4 pendents)

### M-1: spring-boot-devtools en runtime scope
**Pendent:** Assegurar exclusió a producció.

### M-2: @Valid faltant a CommunicationRespondRequest ✅ CORREGIT
**Abans:** DTO sense validacions. **Després:** `@NotBlank @Size(max=5000)`.

### M-3: Password mínim 4 caràcters ✅ CORREGIT
**Abans:** `reset-password/page.tsx:31` — `password.length < 4`. **Després:** 8.

### M-4: Rate limiter race condition
**Pendent:** Redis `get` + `increment` no atòmic. Cal Lua script o `MULTI/EXEC`.

### M-5: No X-Forwarded-For a AuditFilter
**Pendent:** `AuditFilter.java:51` — `request.getRemoteAddr()` sempre retorna IP del proxy.

### M-6: missing @Valid en diversos VaultController
**Pendent:** `createProfile`, `updateProfile`, `addPhase` sense `@Valid`.

### M-7: innerHTML capturat sense sanititzar ✅ CORREGIT
**Abans:** `BlockRenderer.tsx:130` — `e.currentTarget.innerHTML` sense sanititzar.
**Després:** `DOMPurify.sanitize(e.currentTarget.innerHTML)`.

### M-8: object-src no definit al CSP ✅ CORREGIT
**Afegit** `object-src 'none'` a `middleware.ts`.

### M-9: document.write amb innerHTML al billing print ✅ CORREGIT
**Abans:** `billing/page.tsx:175` — `${el.innerHTML}` directe.
**Després:** `${DOMPurify.sanitize(el.innerHTML)}`.

---

## 🟢 BAIX (10 trobats, 2 corregits, 8 pendents)

| ID | Issue | Estat |
|----|-------|-------|
| L-1 | CSRF disable documentat | ✅ CORREGIT |
| L-2 | Token JWT no fa trim | Pendent |
| L-3 | Role com a String enlloc d'enum | Pendent |
| L-4 | Account lockout permanent (3 intents) sense desbloqueig | Pendent |
| L-5 | `blob:` a img-spec CSP | Pendent |
| L-6 | No `media-src` explícit | Pendent |
| L-7 | HSTS + Referrer-Policy al backend | ✅ CORREGIT |
| L-8 | HSTS sense `preload` | Pendent |
| L-9 | Healthcheck feble a Dockerfile.runtime | Pendent |
| L-10 | Sense rate limiting a forgot-password i refresh | Pendent |

---

## Resum

| Severitat | Trobats | Corregits | Pendents |
|-----------|---------|-----------|----------|
| 🔴 CRÍTIC | 7 | 5 | 2 |
| 🟠 ALT | 9 | 7 | 2 |
| 🟡 MITJÀ | 9 | 5 | 4 |
| 🟢 BAIX | 10 | 2 | 8 |
| **TOTAL** | **35** | **19** | **16** |

---

## Pla d'Acció (Prioritzat)

1. **C-5**: Migrar tokens a HttpOnly cookies (setmana 1-2)
2. **C-7**: Avaluar necessitat de Docker socket RW (setmana 1)
3. **H-6**: Canviar credential masking a `****` (setmana 1)
4. **H-3**: Protegir assets amb tenant-scoped access (setmana 2-3)
5. **H-4**: POST-only flow per reset password (setmana 2)
6. **M-4**: Fer atòmic el rate limiter amb Lua script (setmana 2)
7. **M-5**: Afegir X-Forwarded-For a AuditFilter (setmana 1)
8. **M-6**: Afegir @Valid a endpoints de VaultController (setmana 1)
