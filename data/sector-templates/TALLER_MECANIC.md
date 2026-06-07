# Taller mecànic

## Prospecting

### Email fred

**Assumpte:** Manteniment de flota / Revisió {TEMPORADA} per a {NOM_NEGOCI}

Hola {NOM_CONTACTE},

Soc {TEU_NOM} de {EMPRESA}. He vist que {NOM_NEGOCI} treballa amb vehicles {TIPUS_VEHICLE}.

Aquesta temporada estem ajudant tallers a {CIUTAT} a omplir els forats de booking amb un sistema de recordatori automàtic per WhatsApp. Els clients reben un avís quan toca revisió i contesten directament per reservar.

Resultat típic: +30% reserves preventives, menys dies morts.

Funciona per a qualsevol taller: la gent no recorda quan va fer la ITV o el canvi d'oli, però respon a un WhatsApp.

Vols que t'ho ensenyi en 5 minuts?

{TEU_NOM}

---

**To:** Tècnic, directe, fiable
**CTA:** Baixa fricció
**Consell:** Esmenta la temporada (hivern = pneumàtics, estiu = aire condicionat)

### Seguiment

**Assumpte:** 3 tallers a {CIUTAT} ja ho fan

Hola {NOM_CONTACTE},

Només un recordatori: 3 tallers a {CIUTAT} ja estan usant el sistema de recordatoris per WhatsApp. El primer mes van recuperar la inversió només amb les revisions que haurien perdut.

El cost és 20€/mes. S'activa en 24h.

Vols que t'ho configuri?

{TEU_NOM}

---

## Meta Ads

### Objectiu: Leads (crida telefònica o formulari)

**Estructura:**

| Nivell | % | Audiència | Creativitat |
|--------|---|-----------|-------------|
| Captació | 60% | Radi {RADI}km, Homeowners, 25-65 | Abans/després reparació + oferta revisió |
| Retargeting | 30% | Visitants web + engatgers | Testimonial + recordatori |
| Notorietat | 10% | Radi ampli | Vídeo taller en acció |

### Creativitats:

**Revisió temporada:**
- Text: "Abans que arribi l'estiu: revisió aire condicionat 29€. Inclou gas + diagnosi."
- Imatge: Tècnic treballant + cotxe

**ITV:**
- Text: "La teva ITV caduca aviat? Porta'l a revisió prèvia i vine tranquil."
- Imatge: Calendari + cotxe

**Vídeo testimoni:**
- Escena: Client recollint cotxe + somriure
- Text: "«Em van dir 3 dies i ho van tenir en 2. I van deixar el cotxe net.»"

---

## Agent IA (Prompt)

```
Ets l'assistent virtual de {NOM_NEGOCI}, un taller mecànic a {CIUTAT} (Mallorca).
Respons sempre en català o castellà, de forma breu i natural — és WhatsApp.

SERVEIS: {SERVEIS}

ROL PRINCIPAL:
1. Gestionar entrades de vehicle (recepció)
2. Recollir dades: marca, model, any, matrícula, descripció avaria, km
3. Confirmar cita d'entrega i data estimada de recollida
4. Informar l'estat de la reparació
5. Notificar quan el vehicle està llest

FLUX:
- Client demana cita → recollir dades → confirmar horari
- Vehicle al taller → diagnosi → pressupost → aprovació client → reparació
- Acabat → avís al client → recollida

URGÈNCIES: Si el vehicle no pot circular, oferir grua i avisar propietari.

HORARI: {HORARI}
TANCAT: {DIA_TANCAMENT}
```
