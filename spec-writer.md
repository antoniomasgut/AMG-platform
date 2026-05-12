---
name: spec-writer
description: "Genera documents d'especificació (specs) per als mòduls de la plataforma AMG. Crea un fitxer specs/NN-nom.md complet amb objectius, model de dades, API, seguretat, RGPD i tests. S'activa amb frases com: 'genera un spec', 'defineix el mòdul', 'especifica el mòdul', 'crea l'spec de', 'escriu l'spec per al mòdul', 'especificació de mòdul', 'spec per al mòdul NN', 'vull definir el mòdul', 'documenta el mòdul', 'fes l'spec', 'genera especificació', 'crea documentació de mòdul', 'write module specification', 'generate module spec'."
---

# Spec-Writer — Generador d'specs per a la plataforma AMG

Genera documents d'especificació complets per als mòduls de la plataforma AMG Digitalització. Llegeix el context del projecte des de `CLAUDE.md` i crea un fitxer `specs/NN-nom.md` seguint el format estàndard.

**Regla fonamental:** No inventis res. Si l'spec requereix informació que no està al CLAUDE.md o que l'usuari no ha proporcionat, pregunta-ho abans d'assumir-ho.

---

## Pas 1 — Determinar el mòdul

Quan l'usuari demani generar un spec:

1. Llegeix `CLAUDE.md` de l'arrel del projecte per obtenir el stack tecnològic, la llista de mòduls, les regles de treball i el context general.
2. Identifica el número de mòdul i nom que l'usuari vol especificar.
3. Si l'usuari no l'especifica, mostra la llista de mòduls pendents i pregunta quin vol definir.
4. Si l'spec ja existeix a `specs/`, avisa'n i pregunta si vol sobreescriure'l.

Extreu del `CLAUDE.md`:
- Stack backend (Spring Boot, Java, JPA, seguretat, base de dades)
- Stack frontend (Next.js, Tailwind, i18n, state management)
- Stack infra (Hetzner, Docker, Traefik)
- Model de preus (per mòduls de billing)
- Regles de treball (tests, deepseek, commits)

---

## Pas 2 — Recollir informació específica del mòdul

Pregunta a l'usuari de forma conversacional (agrupat, no interrogatori):

**Bloc A — Abast i objectius:**
- Quins són els objectius principals del mòdul?
- Quins actors/usuaris hi intervenen? (admin, client, visitant...)
- Quines són les funcionalitats clau?

**Bloc B — Model de dades:**
- Quines entitats principals té el mòdul? (si l'usuari no ho sap, proposa'n)
- Quines relacions tenen entre elles?
- Quins camps clau hauria de tenir cada entitat?

**Bloc C — Integracions:**
- Es connecta amb algun altre mòdul de la plataforma? (ex: Auth, Vault, Billing)
- Necessita APIs externes? (Stripe, Holded, n8n...)
- Té requirements d'emmagatzematge (assets, fitxers)?

**Bloc D — RGPD / Legal:**
- Tracta dades personals? Si sí, quines?
- Quina base legal té el tractament?
- Quant de temps es conserven les dades?

**Bloc E — QA:**
- Quins casos d'ús principals cal testejar?
- Hi ha casos límit o errors previstos?

Si l'usuari ja ha proporcionat prou informació a la seva sol·licitud inicial, omet les preguntes i dissenya directament.

---

## Pas 3 — Generar l'spec

Escriu el fitxer `specs/NN-nom.md` (on NN és el número de mòdul i nom el seu identificador) amb aquesta estructura:

```markdown
# Mòdul NN: Nom del Mòdul

> **Versió:** 1.0
> **Data:** YYYY-MM-DD
> **Autor:** [per determinar]
> **Dependències:** Mòduls #X, #Y

---

## 1. Objectius

- [Objectiu 1]
- [Objectiu 2]

---

## 2. Abast

### 2.1 Funcionalitats incloses

- [Funcionalitat 1]
- [Funcionalitat 2]

### 2.2 Funcionalitats excloses

- [Funcionalitat que NO cobreix aquest mòdul]

### 2.3 Actors

| Actor | Descripció | Permisos |
|-------|-----------|----------|
| Admin | ... | ... |
| Client | ... | ... |

---

## 3. Model de dades

### 3.1 Entitats

#### [Entitat1]

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | Long (PK) | @Id @GeneratedValue | Identificador únic |
| ... | ... | ... | ... |

**Relacions:**
- `entitat2`: @OneToMany, cascade ALL, lazy

#### [Entitat2]

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| ... | ... | ... | ... |

### 3.2 Schema per tenant

[Explicació de com es gestiona el multi-tenant per aquest mòdul]

---

## 4. API REST

### 4.1 Endpoints

#### `[METHOD] /api/v1/[ruta]` — [Descripció]

**Autenticació:** [Bearer JWT / API Key / Pública]
**Rols permesos:** [Rol1, Rol2]

**Request:**
```json
{
  "camp": "tipus (descripció)"
}
```

**Response [codi]:**
```json
{
  "camp": "valor"
}
```

**Errors:**
| Codi | Situació |
|------|---------|
| 400 | [descripció] |
| 401 | [descripció] |
| 403 | [descripció] |
| 404 | [descripció] |

### 4.2 Mapa complet d'endpoints

| Mètode | Ruta | Descripció | Auth | Rols |
|--------|------|-----------|------|------|
| GET | ... | ... | JWT | ... |
| POST | ... | ... | JWT | ... |
| PUT | ... | ... | JWT | ... |
| DELETE | ... | ... | JWT | ... |

---

## 5. Seguretat

### 5.1 Autenticació
[Com es protegeix l'accés]

### 5.2 Autorització (RBAC)
[Permisos granulars per rol]

### 5.3 Protecció de dades
[XSS, CSRF, rate limiting, validació d'input]

---

## 6. RGPD / LSSI

- **Dades personals:** [llista de camps amb dades personals]
- **Base legal:** [consentiment/contracte/interès legítim]
- **Conservació:** [termini d'emmagatzematge]
- **Portabilitat:** [si aplica]
- **Supressió:** [mecanisme de right to be forgotten]

---

## 7. Tests (QA)

### 7.1 Test cases funcionals

| # | Cas | Entrada | Resultat esperat |
|---|-----|---------|-----------------|
| 1 | ... | ... | ... |

### 7.2 Test cases de seguretat

| # | Cas | Entrada | Resultat esperat |
|---|-----|---------|-----------------|
| 1 | ... | ... | ... |

### 7.3 Test cases de límits

| # | Cas | Entrada | Resultat esperat |
|---|-----|---------|-----------------|
| 1 | ... | ... | ... |

---

## 8. Dependències entre mòduls

| Mòdul | Dependència | Tipus |
|-------|-----------|-------|
| #X | ... | Forta/Dèbil |

---

## 9. Obert / Pendents

- [Decisions pendents]
- [Dubtes per resoldre]
```

Adapta cada secció al mòdul concret. Si alguna secció no aplica (ex: "Integracions" en un mòdul sense API externa), pots simplificar-la o ometre-la.

Aplica el stack del `CLAUDE.md` als exemples de codi:
- Java 21 + Spring Boot 3 per entitats JPA
- PostgreSQL per persistència
- Redis per cache/rate-limiting
- JWT amb RBAC per endpoints
- Next.js 14 App Router per frontend
- `next-intl` per i18n
- Zod + Zustand + React Query per frontend
- MinIO per assets

---

## Pas 4 — Validar l'spec

Després d'escriure l'spec, verifica:

1. Té tots els apartats obligatoris? (Objectius, Abast, Model de dades, API, Seguretat, RGPD, Tests)
2. És coherent amb altres specs existents? (Llegeix specs anteriors per comprovar solapaments)
3. Té el format correcte de ruta? (`specs/NN-nom.md`)
4. Les relacions entre entitats són consistents?
5. La nomenclatura dels endpoints segueix el patró `/api/v1/...`?

Si detectes algun problema, corregeix-lo automàticament.

---

## Pas 5 — Presentar el resultat

Mostra:
1. Ruta del fitxer generat (`specs/NN-nom.md`)
2. Resum de l'estructura: quantes entitats, endpoints, casos de test
3. Menciona si detectes dependències amb altres mòduls que caldria coordinar
4. Pregunta si vol ajustar o afegir alguna cosa

No mostris preus suggerits ni consells de venda.
