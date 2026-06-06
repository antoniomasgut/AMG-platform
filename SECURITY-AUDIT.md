# Auditoria de Seguretat — AMG Portal

**Data:** 2026-06-06
**Abast:** Backend (Spring Boot 3), Frontend (Next.js 14), Infra (Traefik, Docker)
**Estat:** 17 vulnerabilitats corregides, 3 pendents

---

## Corregit (17)

### 🔴 CRÍTIC (5)

| ID | Issue | Fix | Commit |
|----|-------|-----|--------|
| C-1 | `/communication/{id}/respond` sense auth | Afegides validacions d'estat SENT + expiry + `@Valid` | `0ef4b89` |
| C-2 | `anyRequest().permitAll()` | Cambiat a `.authenticated()` | `0ef4b89` |
| C-3 | `JWT_SECRET` buit per defecte | `JwtProvider` llança `IllegalStateException` si no configurat | `0ef4b89` |
| C-4 | `VAULT_MASTER_KEY` genera clau aleatòria | `VaultEncryption` llança `IllegalStateException` si no configurat | `0ef4b89` |
| C-6 | `meta-ads.ts` / `hosting.ts` llegeixen token de localStorage | Canviats a `sessionStorage.getItem('access_token')` | `0ef4b89` |

### 🟠 ALT (5)

| ID | Issue | Fix | Commit |
|----|-------|-----|--------|
| H-1 | Swagger UI públic | Canviat a `.authenticated()` | `0ef4b89` |
| H-2 | `test-email` bypass de rate limiting | Eliminat `LoginAttemptService.testEmail` | `0ef4b89` |
| H-5 | Nonce CSP generat amb UUID | Millorat a `crypto.getRandomValues()` (256-bit) | `0ef4b89` |
| H-8 | Security headers no aplicats als routers Traefik | Afegits `secureHeaders` + `rateLimit` a tots els routers | `0ef4b89` |
| H-9 | Contrasenyes hardcodejades al `docker-compose.deploy.yml` | Substituïdes per `${ENV_VAR}` | `0ef4b89` |

### 🟡 MITJÀ (5)

| ID | Issue | Fix | Commit |
|----|-------|-----|--------|
| M-2 | `@Valid` faltant a `CommunicationRespondRequest` | Afegit `@NotBlank` + `@Size(max=5000)` al DTO | `0ef4b89` |
| M-3 | Password mínim 4 caràcters | Canviat a 8 | `0ef4b89` |
| M-7 | `innerHTML` capturat sense sanititzar a `BlockRenderer` | Afegit `DOMPurify.sanitize()` al `onBlur` | `0ef4b89` |
| M-8 | `object-src` no definit al CSP | Afegit `object-src 'none'` | `0ef4b89` |
| M-9 | `document.write` amb `innerHTML` al billing print | Afegit `DOMPurify.sanitize()` | `0ef4b89` |

### 🟢 BAIX (2)

| ID | Issue | Fix | Commit |
|----|-------|-----|--------|
| L-1 | CSRF deshabilitat sense documentació | Afegit comentari explicatiu | `0ef4b89` |
| L-7 | Manca HSTS i Referrer-Policy al backend | Afegits al `SecurityConfig.headers()` | `0ef4b89` |

---

## Pendents (3)

| ID | Severitat | Issue | Bloquejant |
|----|-----------|-------|------------|
| C-5 | 🔴 CRÍTIC | JWT tokens a `sessionStorage` — cal migrar a `httpOnly` cookies | Requereix canvis a backend (Set-Cookie a login) + frontend (llegir de cookie) |
| C-7 | 🔴 CRÍTIC | Socket Docker muntat *writable* al backend (`docker-compose.yml:113`) | Per avaluar si Engine realment necessita RW |
| H-6 | 🟠 ALT | Credential masking mostra últims 4 caràcters | Canvi de UX a `****` fix |

Veure `SECURITY-AUDIT-FULL.md` per a l' informe complet d'auditoria.
