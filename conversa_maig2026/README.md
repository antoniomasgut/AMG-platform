# NexeLocal — Documentació del Projecte

> Plataforma multitenant d'agents IA per a petits negocis locals  
> Antonio Mas Gutiérrez · Guillem Mas · Rebecca Buhk Quesada

---

## Índex de documents

| Document | Descripció |
|---|---|
| [01-arquitectura-tecnica.md](./01-arquitectura-tecnica.md) | Spring Boot, base de dades, estructura del projecte |
| [02-agents-per-sector.md](./02-agents-per-sector.md) | Configuració d'agents per cada sector de negoci |
| [03-integracio-canals.md](./03-integracio-canals.md) | WhatsApp (Twilio), Telegram, Email |
| [04-infraestructura-hetzner.md](./04-infraestructura-hetzner.md) | Servidors, desplegament, monitorització |
| [05-model-de-negoci.md](./05-model-de-negoci.md) | Preus, fases, descomptes, projecció |
| [06-operacions-i-onboarding.md](./06-operacions-i-onboarding.md) | Alta de clients, manteniment, rols de l'equip |

---

## Resum del projecte

NexeLocal és una aplicació SaaS multitenant que desplega agents d'IA conversacionals per a petits negocis (pintors, fisioterapeutes, perruqueries, gestories...). Cada client (tenant) té el seu propi agent configurat amb la informació del seu negoci, que respon automàticament per WhatsApp, Telegram i email.

### Tecnologia principal

```
Backend:      Spring Boot 3.x (Java 21)
Base de dades: PostgreSQL 16
Caché:        Redis 7
IA principal: Claude Haiku 4.5 (Anthropic)
IA secundària: Mistral Small 3.1 (tasques sense dades personals)
Canals:       Twilio (WhatsApp) · Telegram Bot API · Resend (Email)
Infraestructura: Hetzner Cloud (Falkenstein, Alemanya — RGPD)
```

### Principis de disseny

- **Multitenant per `tenant_id`** — un sol servidor per a tots els clients
- **Sense instàncies per client** — l'agent és configuració a la BD, no un procés
- **RGPD complert** — dades processades a Europa, mai a servidors xinesos
- **Preu per fases** — el client paga el que implementa, no un pla genèric
- **Mode híbrid** — el dueño pot prendre el control de les converses en qualsevol moment

### Equip

| Persona | Rol |
|---|---|
| **Antonio** | Tècnic — Desenvolupament, infraestructura, monitorització |
| **Guillem** | Comercial — Captació, demos, pressupostos, onboarding |
| **Rebecca** | Administrativa — Contractes, facturació, suport bàsic |

---

## Estat del projecte

- [x] Disseny d'arquitectura
- [x] Definició de sectors i agents
- [x] Model de negoci i tarifes
- [x] Comparativa competència
- [ ] Implementació Spring Boot
- [ ] Integració Twilio WhatsApp
- [ ] Integració Telegram
- [ ] Integració Email (Resend)
- [ ] Panel d'administració (onboarding tenants)
- [ ] Panel del client (mode híbrid)
- [ ] Desplegament Hetzner producció
- [ ] Primers clients piloto

---

## Contractes a signar

| Servei | Ús | Cost inicial |
|---|---|---|
| Hetzner Cloud | Servidor CX32 | ~€7/mes |
| Anthropic API | Claude Haiku 4.5 | Per ús |
| Twilio | WhatsApp Business API | Per ús |
| Resend | Email sortint | Gratis fins 3K/mes |
| cdmon | Domini nexelocal.com/.cat/.es | ~€40/any |
| Google Workspace | Email corporatiu (3 usuaris) | ~€18/mes |
