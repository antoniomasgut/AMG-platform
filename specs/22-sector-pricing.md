# Spec 22 — Sector Pricing (NexeLocal Model)

## Context de negoci

NexeLocal és el model de preus basat en sector professional i mida d'empresa. Els clients contracten serveis digitals en **fases lliures** (qualsevol combinació de F1–F5). El preu mensual depèn del sector, la mida del negoci, i el **nombre** de fases contractades — no de quines fases específiques.

**Principi clau:** El preu d'una primera fase sempre és priceF1, d'una segona fase priceF2, etc. — independentment de si el client contracta F1+F3 o F1+F4.

### Fases de servei

| Fase | Nom | Contingut |
|------|-----|-----------|
| F1 | Captació | Agent IA per WhatsApp/Email/Xat, creació automàtica de Leads, FAQ i info del negoci |
| F2 | Agenda | Booking per xat/WA/email, Google Calendar, recordatoris automàtics |
| F3 | Pressupostos | Llistat de preus (pricelist) o generació de pressupost formal per l'agent |
| F4 | Seguiment | Sol·licitud de ressenyes Google, seguiment postvenda, reactivació clients |
| F5 | Alertes & Equip | Notificacions grup Telegram intern, escalada humana, informes diaris |

> **Landing i domini:** serveis independents de les fases, amb setup i mensual propis.
> El chat widget (canal web de F1) requereix **Landing Pro** — no disponible a Micro-landing.
> Veure model de preus a `CLAUDE.md`.

Les fases **no són acumulatives**: F1+F3 és vàlid sense F2. El mensual total = suma de `priceF{n}` per cada fase contractada, usant els tiers en ordre (1a fase = priceF1, 2a fase = priceF2...).

### Mides d'empresa

| Valor | Descripció |
|-------|-----------|
| AUTONOMO | 1 persona |
| PETIT | 2-3 persones |
| MITJA | 3-5+ persones |

---

## Model de dades

### Camps a `Tenant`

```java
BusinessSector sector;          // nullable, EnumType.STRING, length 30
BusinessSize   businessSize;    // nullable, column: business_size, length 20
String contractedPhases;        // comma-separated, e.g. "F1", "F1,F3", "F2,F4,F5"
```

### Entitat `SectorPricing`

Taula: `sector_pricing`
Constraint única: `(sector, business_size)`

| Camp | Tipus | Restricció |
|------|-------|-----------|
| id | UUID | PK, generada |
| sector | BusinessSector | NOT NULL |
| businessSize | BusinessSize | NOT NULL |
| setupPrice | BigDecimal(10,2) | NOT NULL |
| priceF1 | BigDecimal(10,2) | DEFAULT 0 (col: price_f1) |
| priceF2 | BigDecimal(10,2) | DEFAULT 0 (col: price_f2) |
| priceF3 | BigDecimal(10,2) | DEFAULT 0 (col: price_f3) |
| priceF4 | BigDecimal(10,2) | DEFAULT 0 (col: price_f4) |
| priceF5 | BigDecimal(10,2) | DEFAULT 0 (col: price_f5) |
| createdAt | Instant | @CreatedDate |
| updatedAt | Instant | @LastModifiedDate |

**Mètode de domini:**
```java
BigDecimal totalMonthly(Collection<String> phases)
// Ordena les fases, suma els tiers en ordre: 1a fase → priceF1, 2a → priceF2, etc.
```

---

## Enums

### `BusinessSector`
```
PINTOR, ELECTRICISTA, FONTANER, JARDINER, NETEJA,
FISIOTERAPEUTA, PSICOLEG, NUTRICIONISTA,
PERRUQUERIA, ESTETICA,
GESTORIA, ACADEMIA,
TALLER_MECANIC, VETERINARI, PERRUQUERIA_CANINA
```

### `BusinessSize`
```
AUTONOMO, PETIT, MITJA
```

### `ServicePhase`
```
F1, F2, F3, F4, F5
```

---

## Matriu de preus (NexeLocal)

Els preus de la columna F2–F5 són **increments per fase addicional**. El mensual total = priceF1 + priceF2 per a 2 fases, + priceF3 per a 3 fases, etc.

### Serveis a la llar

| Sector | Mida | Setup | F1 | +F2 | +F3 | +F4 | +F5 |
|--------|------|------|----|-----|-----|-----|-----|
| PINTOR | AUTONOMO | 150 | 59 | 20 | 20 | 30 | 20 |
| PINTOR | PETIT | 250 | 79 | 20 | 30 | 30 | 20 |
| ELECTRICISTA | AUTONOMO | 150 | 59 | 20 | 20 | 30 | 20 |
| ELECTRICISTA | PETIT | 250 | 79 | 20 | 30 | 30 | 20 |
| FONTANER | AUTONOMO | 150 | 59 | 20 | 20 | 30 | 20 |
| JARDINER | AUTONOMO | 150 | 59 | 20 | 20 | 30 | 20 |
| NETEJA | AUTONOMO | 150 | 59 | 20 | 20 | 30 | 20 |
| NETEJA | MITJA | 300 | 89 | 30 | 30 | 30 | 20 |

### Salut i benestar

| Sector | Mida | Setup | F1 | +F2 | +F3 | +F4 | +F5 |
|--------|------|------|----|-----|-----|-----|-----|
| FISIOTERAPEUTA | AUTONOMO | 175 | 69 | 20 | 20 | 30 | 20 |
| FISIOTERAPEUTA | PETIT | 275 | 99 | 30 | 40 | 30 | 20 |
| FISIOTERAPEUTA | MITJA | 375 | 129 | 40 | 60 | 50 | 20 |
| PSICOLEG | AUTONOMO | 175 | 69 | 20 | 20 | 30 | 20 |
| PSICOLEG | PETIT | 300 | 109 | 40 | 40 | 40 | 20 |
| NUTRICIONISTA | AUTONOMO | 175 | 59 | 20 | 20 | 30 | 20 |

### Estètica

| Sector | Mida | Setup | F1 | +F2 | +F3 | +F4 | +F5 |
|--------|------|------|----|-----|-----|-----|-----|
| PERRUQUERIA | AUTONOMO | 150 | 59 | 20 | 20 | 30 | 20 |
| PERRUQUERIA | PETIT | 300 | 99 | 40 | 40 | 40 | 20 |
| ESTETICA | AUTONOMO | 150 | 59 | 20 | 20 | 30 | 20 |
| ESTETICA | MITJA | 350 | 109 | 40 | 40 | 40 | 20 |

### Professionals

| Sector | Mida | Setup | F1 | +F2 | +F3 | +F4 | +F5 |
|--------|------|------|----|-----|-----|-----|-----|
| GESTORIA | AUTONOMO | 200 | 69 | 20 | 20 | 30 | 20 |
| GESTORIA | MITJA | 400 | 109 | 40 | 50 | 50 | 20 |
| ACADEMIA | AUTONOMO | 175 | 59 | 20 | 20 | 30 | 20 |
| ACADEMIA | MITJA | 300 | 99 | 40 | 30 | 40 | 20 |

### Automoció i mascotes

| Sector | Mida | Setup | F1 | +F2 | +F3 | +F4 | +F5 |
|--------|------|------|----|-----|-----|-----|-----|
| TALLER_MECANIC | PETIT | 150 | 59 | 20 | 20 | 30 | 20 |
| TALLER_MECANIC | MITJA | 275 | 89 | 30 | 30 | 30 | 20 |
| VETERINARI | AUTONOMO | 175 | 69 | 20 | 20 | 30 | 20 |
| VETERINARI | PETIT | 325 | 109 | 40 | 40 | 40 | 20 |
| PERRUQUERIA_CANINA | AUTONOMO | 150 | 49 | 20 | 20 | 30 | 20 |

---

## API

### `GET /api/v1/pricing`
- Auth: authenticated
- Retorna: llista de tots els `SectorPricingResponse` ordenats per sector i mida

### `GET /api/v1/pricing/lookup?sector=FISIOTERAPEUTA&size=AUTONOMO`
- Auth: authenticated
- Retorna: `SectorPricingResponse` o 404 si no trobat

### `SectorPricingResponse`
```java
record SectorPricingResponse(
    String sector, String businessSize,
    BigDecimal setupPrice,
    BigDecimal priceF1, BigDecimal priceF2, BigDecimal priceF3, BigDecimal priceF4, BigDecimal priceF5
) {
    PhaseLookup forPhaseCount(int count);   // suma els N primers tiers
    PhaseLookup forPhases(Collection<String> phases); // compta les fases i delega a forPhaseCount
}
```

---

## Regla de facturació

```
mensual = SectorPricing.totalMonthly(tenant.contractedPhases)

Exemples (PERRUQUERIA AUTONOMO):
  F1        → 59€
  F1 + F3   → 59 + 20 = 79€   (2 fases → priceF1 + priceF2)
  F1 + F4   → 59 + 20 = 79€   (2 fases → priceF1 + priceF2)
  F1+F2+F3  → 59 + 20 + 20 = 99€
  F1+F3+F5  → 59 + 20 + 20 = 99€   (3 fases)
  F1+F2+F3+F4 → 59+20+20+30 = 129€
```

---

## Checklist d'implementació

- [x] Enum `BusinessSector` (15 valors)
- [x] Enum `BusinessSize` (AUTONOMO, PETIT, MITJA)
- [x] Enum `ServicePhase` (F1-F5)
- [x] Entitat JPA `SectorPricing` amb priceF1–priceF5 individuals
- [x] `SectorPricing.totalMonthly(phases)` — suma per nombre de fases
- [x] `SectorPricingRepository` amb `findBySectorAndBusinessSize` i `findAllByOrderBySectorAscBusinessSizeAsc`
- [x] `SectorPricingSeeder` @Order(2) @Profile("!test") — 27 entrades amb preus individuals
- [x] DTO `SectorPricingResponse` amb `forPhaseCount` i `forPhases`
- [x] `PricingController` GET /pricing i GET /pricing/lookup
- [x] `Tenant.contractedPhases` — String comma-separated, suporta qualsevol combinació de fases
- [x] `TenantResponse` +sector, +businessSize, +contractedPhases
- [x] `CreateTenantRequest` +sector, +businessSize, +contractedPhases
- [x] `UpdateTenantRequest` +sector, +businessSize, +contractedPhases
- [x] `TenantService` mapeja els nous camps a create, update i toResponse
- [ ] Migració Flyway (V22__add_sector_pricing.sql)
- [ ] Tests d'integració per a PricingController
- [ ] Frontend: selector de sector/mida i fases a la creació/edició de tenant
