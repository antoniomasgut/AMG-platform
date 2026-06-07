# Restaurant / Bar

## Prospecting

### Email fred (6-8 frases)

**Assumpte:** Una pregunta ràpida sobre {NOM_NEGOCI}

Hola {NOM_CONTACTE},

He vist que {NOM_NEGOCI} té {NUM_REVIEWS} ressenyes a Google amb {ESTRELLES} estrelles — els últims comentaris parlen de {PROBLEMA_OBSERVAT} (cues, espera, qualitat). Aquest patró sol costar un 10-15% de nous clients potencials.

Ajudem restaurants a {CIUTAT} a millorar la seua presència digital i omplir les hores baixes (dilluns-dimecres migdia).

A {CLIENT_REF} vam aconseguir +23% reserves entre setmana en 90 dies.

Vols que t'enviï un resum de com ho fem?

{TEU_NOM}

---

**To:** Casual, eficient
**Millor dia:** Dijous
**Millor hora:** 15-17h (entre serveis)
**CTA:** Baixa fricció ("Vols que t'enviï un resum?" no "Agenda una demo")

### Seguiment (5-7 dies després, angle diferent)

**Assumpte:** {NOM_NEGOCI} — idea ràpida

Hola {NOM_CONTACTE},

Només un segon missatge. Molts restaurants ens diuen que el problema no és tenir clients, sinó que els clients repetixin entre setmana.

La solució més senzilla que funciona: un programa de fidelització senzill per WhatsApp. Res complicat — punts per visita, descompte al cinquè cop.

Costa 20€ al mes i es configura en un dia.

Vols que t'ho expliqui en 5 minuts per telèfon?

{TEU_NOM}

---

## Meta Ads

### Objectiu: Leads (formulari nadiu o landing)

**Estructura de campanya:**

| Nivell | % Pressupost | Audiència | Creació |
|--------|-------------|-----------|---------|
| Captació | 60% | Radi {RADI}km, edat {EDAT_MIN}-{EDAT_MAX}, Foodies/Dining Out | 4 imatges + 2 vídeos |
| Retargeting | 30% | Visitants web + engatgers darrers 30 dies | Testimonials + oferta |
| Notorietat | 10% | Radi {RADI}km, ampli | Vídeo 15-30s (cuina/ambient) |

### Creativitats que funcionen:

**Imatge 1 — Oferta horabaixa (15-18h):**
- Text: "De dilluns a dijous, 2a consumició gratis. Només avui."
- Imatge: Plat estrella amb bona llum

**Imatge 2 — Menú del dia:**
- Text: "Menú complet 12,90€. Saps què menjaràs avui?"
- Imatge: Foto real del menú (no stock photo)

**Vídeo 15s — Ambient:**
- Escena: Cuina en acció + plat servit + somriures
- Text: "Això passa cada dia a {NOM_NEGOCI}. I tu encara no has vingut?"

**Testimonial:**
- Text: "Vaig venir de casualitat i ara vinc cada setmana" — {CLIENT}
- Imatge: Client real al restaurant

### Landings:

No enviïs mai a la homepage. Crea una landing per campanya:
- 1 oferta
- 1 CTA ("Reserva taula" / "Demana el menú")
- Sense menú de navegació
- Mòbil-first

### Mètriques clau:
- Cost per lead: 5-15€
- Taxa conversió landing: 12-18%
- ROAS objectiu: 3x+

---

## Agent IA (Prompt)

```
Ets l'assistent virtual de {NOM_NEGOCI}, un restaurant a {CIUTAT} (Mallorca).
Respons sempre en català o castellà (adapta't a l'idioma del client), de forma breu i natural — és WhatsApp, no un email.
Mai inventis preus ni dates concretes si no els tens.

SERVEIS: {SERVEIS}

ROL PRINCIPAL:
1. Gestionar reserves de taula
2. Informar sobre la carta, menú del dia i especialitats
3. Gestionar al·lèrgies i intoleràncies alimentàries
4. Gestionar grups i esdeveniments privats
5. Promocionar el menú del dia i ofertes especials

PER A RESERVA:
- Dia i hora
- Nombre de comensals
- Al·lèrgies o intoleràncies?
- Ocasio especial? (aniversari, etc.)
- Nom de la reserva i telèfon de contacte

GRUPS: Per a grups de +8 persones → derivar a contacte directe.

HORARI:
- Dinar: {HORARI_DINAR}
- Sopar: {HORARI_SOPAR}
- Tancat: {DIA_TANCAMENT}

REGLE GENERAL:
- Confirma sempre la disponibilitat abans de tancar la reserva
- Si no saps alguna cosa, pregunta al client en lloc d'inventar
- Sigues amable i eficient (els clients de restaurant valoren la rapidesa)
```
