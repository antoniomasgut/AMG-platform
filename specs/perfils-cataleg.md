# Catàleg de Perfils: Negocis Automatitzables

> Document de referència per crear perfils, fases i serveis a la plataforma AMG.
> Versió: 1.0 — 2026-05-12

---

## Catàleg de serveis reutilitzables

Cada servei està definit amb nom, slug, tipus, cost i preu de venda orientatiu.

| Servei | Slug | Tipus | Cost | Venda | Descripció |
|--------|------|-------|------|-------|------------|
| WhatsApp Business | whatsapp-business | CREDENTIALS | 10€ | 50€ | ApiKey + Phone ID per enviar missatges automàtics |
| SMTP Corporatiu | smtp-corporatiu | CREDENTIALS | 5€ | 30€ | Host + port + user + password per enviar emails |
| Landing Pro | landing-pro | LANDING | 30€ | 80€ | Landing page amb fotos i formulari de contacte |
| Landing extra | landing-extra | LANDING | 25€ | 60€ | Landing addicional (add-on) |
| Agenda online | agenda-online | AUTOMATION | 15€ | 40€ | Calendari per reservar visites online |
| Calculadora pressupost | calc-pressupost | OTHER | 10€ | 35€ | Formulari que genera pressupost automàtic |
| Recordatori 24h | recordatori-24h | AUTOMATION | 5€ | 20€ | WhatsApp automàtic 24h abans de la cita |
| Reenganxament clients | reenganxament | AUTOMATION | 5€ | 25€ | WhatsApp automàtic cada 6 mesos a inactius |
| Ressenya Google | ressenya-google | AUTOMATION | 3€ | 15€ | Demanar ressenya Google automàticament |
| CRM + Pipeline | crm-pipeline | OTHER | 20€ | 50€ | Seguiment de leads (nou → pressupostat → tancat) |
| Cobrament targeta | cobrament-targeta | BILLING | 0€ | 20€ | Acceptar pagaments amb Stripe (comissió a part) |
| Qüestionari salut | qüestionari-salut | OTHER | 5€ | 15€ | Formulari pre-visita per professionals sanitaris |
| Historial mèdic | historial-medic | OTHER | 5€ | 15€ | Fitxa de pacient amb historial |
| Horari especialitat | horari-especialitat | OTHER | 5€ | 10€ | Horari setmanal per especialitat (fisio, etc.) |
| Galeria fotos | galeria-fotos | OTHER | 5€ | 20€ | Galeria de fotos de feines realitzades |
| Xat web | xat-web | CREDENTIALS | 5€ | 15€ | Widget de xat al web (integració) |
| Formulari leads | formulari-leads | OTHER | 3€ | 10€ | Formulari de contacte que alimenta el CRM |
| Facturació | facturacio | BILLING | 15€ | 40€ | Generació i enviament de factures |

---

## Perfil A: Professionals amb cita prèvia

**Perfils:** Fisioterapeuta, dentista, perruqueria, podòleg, entrenador personal, massatgista

### Fases

| Fase | Serveis | Total venda |
|------|---------|-------------|
| F1: Configuració bàsica | WhatsApp Business (50€) + SMTP (30€) | 80€ |
| F2: Landing | Landing Pro (80€) + Galeria fotos (20€) | 100€ |
| F3: Agenda online | Agenda online (40€) + Recordatori 24h (20€) | 60€ |
| F4: CRM i seguiment | CRM + Pipeline (50€) + Reenganxament (25€) | 75€ |
| F5: Fidelització | Ressenya Google (15€) | 15€ |

**Total pla:** 330€  
**Add-ons:** Landing extra (60€), Qüestionari salut (15€), Historial mèdic (15€), Xat web (15€)

---

## Perfil B: Negocis amb pressupost

**Perfils:** Pintor, lampista, jardiner, paleta, electricista, reformes, fuster

### Fases

| Fase | Serveis | Total venda |
|------|---------|-------------|
| F1: Configuració bàsica | WhatsApp Business (50€) + SMTP (30€) | 80€ |
| F2: Landing | Landing Pro (80€) + Galeria fotos (20€) | 100€ |
| F3: Pressupost | Calculadora pressupost (35€) | 35€ |
| F4: CRM i seguiment | CRM + Pipeline (50€) + Reenganxament (25€) | 75€ |
| F5: Fidelització | Ressenya Google (15€) | 15€ |

**Total pla:** 305€  
**Add-ons:** Landing extra (60€), Xat web (15€), Agenda online (40€)

---

## Perfil C: Serveis per subscripció

**Perfils:** Gimnàs, escola d'idiomes, assessoria fiscal, neteja de llars, centre d'estètica

### Fases

| Fase | Serveis | Total venda |
|------|---------|-------------|
| F1: Configuració bàsica | WhatsApp Business (50€) + SMTP (30€) | 80€ |
| F2: Landing | Landing Pro (80€) | 80€ |
| F3: Cobraments | Cobrament targeta (20€) | 20€ |
| F4: CRM i retenció | CRM + Pipeline (50€) + Recordatori renovació (20€) + Reenganxament (25€) | 95€ |
| F5: Fidelització | Ressenya Google (15€) | 15€ |

**Total pla:** 290€  
**Add-ons:** Landing extra (60€), Agenda online (40€), Xat web (15€)

---

## Perfil D: Comerços locals

**Perfils:** Restaurant, botiga, taller mecànic, bar, perruqueria canina

### Fases

| Fase | Serveis | Total venda |
|------|---------|-------------|
| F1: Configuració bàsica | WhatsApp Business (50€) + SMTP (30€) | 80€ |
| F2: Landing | Landing Pro (80€) + Galeria fotos (20€) | 100€ |
| F3: Captació | Formulari leads (10€) + Xat web (15€) | 25€ |
| F4: Fidelització | Ressenya Google (15€) + Reenganxament (25€) | 40€ |

**Total pla:** 245€  
**Add-ons:** Landing extra (60€), CRM + Pipeline (50€), Agenda online (40€)

---

## Perfils verticals addicionals

### Perfil E: Assessoria / Gestoria

| Fase | Serveis | Total |
|------|---------|-------|
| F1: Configuració | WhatsApp (50€) + SMTP (30€) | 80€ |
| F2: Landing | Landing Pro (80€) | 80€ |
| F3: CRM clients | CRM (50€) + Pipeline (inclòs) | 50€ |
| F4: Cobraments + Factures | Cobrament (20€) + Facturació (40€) | 60€ |
| F5: Fidelització | Reenganxament (25€) + Ressenya (15€) | 40€ |

**Total:** 310€

### Perfil F: Immobiliària / Agent immobiliari

| Fase | Serveis | Total |
|------|---------|-------|
| F1: Configuració | WhatsApp (50€) + SMTP (30€) | 80€ |
| F2: Landing | Landing Pro (80€) + Galeria fotos (20€) | 100€ |
| F3: Captació leads | Formulari leads (10€) + CRM (50€) | 60€ |
| F4: Visites | Agenda online (40€) + Recordatori (20€) | 60€ |
| F5: Seguiment | Reenganxament (25€) + Ressenya (15€) | 40€ |

**Total:** 340€

### Perfil G: Taller mecànic

| Fase | Serveis | Total |
|------|---------|-------|
| F1: Configuració | WhatsApp (50€) + SMTP (30€) | 80€ |
| F2: Landing | Landing Pro (80€) | 80€ |
| F3: Reserva visites | Agenda online (40€) + Recordatori (20€) | 60€ |
| F4: CRM + Historial | CRM (50€) + Historial vehicles (15€, per crear) | 65€ |
| F5: Fidelització | Reenganxament (25€) + Ressenya (15€) | 40€ |

**Total:** 325€

---

## Quadre resum

| Perfil | Fases | Serveis | Preu total | Add-ons disponibles |
|--------|-------|---------|------------|-------------------|
| A: Professionals cita | 5 | 9 | 330€ | Qüestionari, Historial, Xat, Landing extra |
| B: Negocis pressupost | 5 | 8 | 305€ | Landing extra, Xat, Agenda |
| C: Subscripció | 5 | 7 | 290€ | Landing extra, Agenda, Xat |
| D: Comerç local | 4 | 7 | 245€ | Landing extra, CRM, Agenda |
| E: Assessoria | 5 | 8 | 310€ | Xat, Agenda |
| F: Immobiliària | 5 | 9 | 340€ | Landing extra |
| G: Taller mecànic | 5 | 8 | 325€ | Xat, Landing extra |

---

## Notes per implementació

1. **Serveis base obligatoris:** WhatsApp Business + SMTP haurien d'estar a tots els perfils (són la infraestructura de comunicació)
2. **Landing Pro** és el segon servei més comú — només canvia el contingut (fotos + text)
3. **Reenganxament** és servei de baix cost i alt valor — posar-lo per defecte
4. **Add-ons:** Landing extra és l'add-on més venut (negocis amb múltiples línies de servei)
5. **Preus orientatius:** Cal revisar marges reals abans de posar en producció
