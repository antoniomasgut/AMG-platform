export type SectorTemplateType = 'prospecting' | 'meta-ads' | 'agent-prompt';

export interface SectorTemplate {
  key: string;
  label: string;
  templates: Record<SectorTemplateType, TemplateBlock[]>;
}

export interface TemplateBlock {
  title: string;
  body: string;  // amb {PLACEHOLDERS}
}

export type TemplateVariables = Record<string, string>;

export const SECTOR_TEMPLATES: SectorTemplate[] = [
  {
    key: 'RESTAURANTE',
    label: 'Restaurant / Bar',
    templates: {
      prospecting: [
        {
          title: 'Email fred (principal)',
          body: `Assumpte: Una pregunta ràpida sobre {NOM_NEGOCI}

Hola {NOM_CONTACTE},

He vist que {NOM_NEGOCI} té {NUM_REVIEWS} ressenyes a Google amb {ESTRELLES} estrelles — els últims comentaris parlen de {PROBLEMA_OBSERVAT}.

Ajudem restaurants a {CIUTAT} a millorar la seua presència digital i omplir les hores baixes.

A {CLIENT_REF} vam aconseguir +23% reserves entre setmana en 90 dies.

Vols que t'enviï un resum de com ho fem?

{TEU_NOM}

---
To: Casual, eficient | Millor dia: Dijous | Hora: 15-17h | CTA: Baixa fricció`,
        },
        {
          title: 'Seguiment (5-7 dies)',
          body: `Assumpte: {NOM_NEGOCI} — idea ràpida

Hola {NOM_CONTACTE},

Només un segon missatge. Molts restaurants ens diuen que el problema no és tenir clients, sinó que repeteixin entre setmana.

La solució: un programa de fidelització per WhatsApp. Punts per visita, descompte al cinquè cop.

Costa 20€ al mes i es configura en un dia.

Vols que t'ho expliqui en 5 minuts per telèfon?

{TEU_NOM}`,
        },
      ],
      'meta-ads': [
        {
          title: 'Campanya de captació (60% pressupost)',
          body: `Objectiu: Leads (formulari nadiu o landing)

Radi: {RADI} km
Edat: {EDAT_MIN}-{EDAT_MAX}
Interessos: Foodies, Dining Out

Creativitats:
1. Imatge — Oferta horabaixa: "De dilluns a dijous, 2a consumició gratis"
2. Imatge — Menú del dia: "Menú complet {PREU_MENU}€"
3. Vídeo 15s: Cuina en acció + plat servit + somriures
4. Testimonial: Client real al restaurant

Landing: 1 oferta + 1 CTA (Reserva taula) + mòbil-first`,
        },
        {
          title: 'Retargeting (30% pressupost)',
          body: `Audiència: Visitants web + engatgers darrers 30 dies
Creativitat: Testimonials + oferta especial

Mètriques objectiu:
- Cost per lead: 5-15€
- Taxa conversió landing: 12-18%
- ROAS objectiu: 3x+`,
        },
      ],
      'agent-prompt': [
        {
          title: 'Prompt base per a l\'assistent',
          body: `Ets l'assistent virtual de {NOM_NEGOCI}, un restaurant a {CIUTAT} (Mallorca).

SERVEIS: {SERVEIS}

ROL PRINCIPAL:
1. Gestionar reserves de taula
2. Informar sobre la carta, menú del dia i especialitats
3. Gestionar al·lèrgies i intoleràncies
4. Gestionar grups (+8 persones → derivar)
5. Promocionar ofertes especials

PER A RESERVA: dia/hora, comensals, al·lèrgies, ocasió, nom i telèfon

HORARI: {HORARI_DINAR} / {HORARI_SOPAR} | Tancat: {DIA_TANCAMENT}`,
        },
      ],
    },
  },

  {
    key: 'TALLER_MECANIC',
    label: 'Taller mecànic',
    templates: {
      prospecting: [
        {
          title: 'Email fred',
          body: `Assumpte: Manteniment per a {NOM_NEGOCI}

Hola {NOM_CONTACTE},

Soc {TEU_NOM} de {EMPRESA}. Ajudem tallers a {CIUTAT} a omplir els forats de calendari amb recordatoris automàtics per WhatsApp.

Els clients reben un avís quan toca revisió (ITV, canvi d'oli, pneumàtics) i contesten per reservar directament.

Resultat: +30% reserves preventives, menys dies morts.

Vols que t'ho ensenyi en 5 minuts?

{TEU_NOM}`,
        },
      ],
      'meta-ads': [
        {
          title: 'Campanya de captació',
          body: `Objectiu: Leads (crida o formulari)
Radi: {RADI}km | Edat: 25-65 | Homeowners

Creativitats:
1. "Revisió aire condicionat {PREU_REVISIO}€ — inclou gas + diagnosi"
2. "ITV propera? Revisió prèvia i vine tranquil"
3. Vídeo testimoni: Client content recollint el cotxe`,
        },
      ],
      'agent-prompt': [
        {
          title: 'Prompt base',
          body: `Ets l'assistent virtual de {NOM_NEGOCI}, un taller a {CIUTAT} (Mallorca).

SERVEIS: {SERVEIS}

FLUX:
1. Client demana cita → recollir: marca, model, any, matrícula, avaria, km
2. Vehicle al taller → diagnosi → pressupost → aprovació → reparació
3. Acabat → avís al client → recollida

URGÈNCIES: vehicle immobilitzat → oferir grua i avisar propietari

HORARI: {HORARI} | Tancat: {DIA_TANCAMENT}`,
        },
      ],
    },
  },

  {
    key: 'ASSESSORIA',
    label: 'Assessoria fiscal i laboral',
    templates: {
      prospecting: [
        {
          title: 'Email fred',
          body: `Assumpte: Terminis fiscals {MES} | {NOM_NEGOCI}

Hola {NOM_CONTACTE},

Recorda que el {DATA_TERMINI} acaba el termini de {IVA_RENDA}.

Oferim primera consulta gratuïta de 30min per revisar la teva situació fiscal.

Sense compromís.

Vols que t'expliqui com funcionem?

{TEU_NOM}

---
To: Formal, precís | CTA: Consulta gratuïta`,
        },
      ],
      'meta-ads': [
        {
          title: 'Campanya de captació',
          body: `Radi: {RADI}km | Small Business Owners | 30-60

Creativitats:
1. "Autònom a {CIUTAT}? Primera consulta fiscal gratuïta. Des de {PREU_AUTONOM}€/mes"
2. "La declaració acaba el 30 de juny. Encara no l'has presentada?"
3. Vídeo: "Què inclou 65€ al mes? T'ho explico en 30s"`,
        },
      ],
      'agent-prompt': [
        {
          title: 'Prompt base',
          body: `Ets l'assistent virtual de {NOM_NEGOCI}, una assessoria a {CIUTAT} (Mallorca).

SERVEIS: {SERVEIS}

ROL: Gestionar consultes, recollir info per pressupost, recordar terminis fiscals.

TERMINIS CLAU:
- IVA trimestral: 20 {MES_IVA}
- Declaració renda: 1 abr - 30 juny
- IS: 1-25 juliol

TARIFES: Autònom des de {PREU_AUTONOM}€/mes | SL des de {PREU_SL}€/mes

IMPORTANT: Mai donis consells fiscals o legals per xat. Derivar al gestor.`,
        },
      ],
    },
  },

  {
    key: 'FISIOTERAPEUTA',
    label: 'Fisioterapeuta / Clínica',
    templates: {
      prospecting: [
        {
          title: 'Email fred',
          body: `Assumpte: Pacients nous per a {NOM_NEGOCI}

Hola {NOM_CONTACTE},

Ajudem clíniques de fisioteràpia a {CIUTAT} a captar pacients nous amb un assistent virtual per WhatsApp.

Respon dubtes sobre tractaments, preus i agenda cites automàticament.

Una clínica a {CIUTAT_REF} va passar de 5 a 15 nous pacients al mes en 60 dies.

Vols que t'ho expliqui?

{TEU_NOM}`,
        },
      ],
      'meta-ads': [
        {
          title: 'Campanya',
          body: `Radi: {RADI}km | 25-65 | Interessos: fisioteràpia, salut, benestar

Creativitats:
1. "Mal d'esquena? Sessió de prova per {PREU_PROVA}€"
2. "3 mesos de rehabilitació: de no poder caminar a tornar a córrer"
3. Vídeo testimoni: pacient + fisio`,
        },
      ],
      'agent-prompt': [
        {
          title: 'Prompt base',
          body: `Ets l'assistent de {NOM_NEGOCI}, clínica de fisioteràpia a {CIUTAT}.

SERVEIS: {SERVEIS}

ROL: Cites, explicar tractaments, preus, assegurances, seguiment.

PER CITA NOVA: nom, motiu consulta, assegurança, disponibilitat

IMPORTANT: Mai donis diagnòstics ni consells mèdics. Derivar al fisioterapeuta.

HORARI: {HORARI}`,
        },
      ],
    },
  },

  {
    key: 'INMOBILIARIA',
    label: 'Immobiliària',
    templates: {
      prospecting: [
        {
          title: 'Captar propietats',
          body: `Assumpte: Vendre a {CIUTAT}? Valoració gratuïta

Hola {NOM_CONTACTE},

Oferim valoració gratuïta sense compromís per a propietats a {ZONA}.

En 30 minuts tens preu orientatiu i pla de màrqueting.

Quin dia et va bé?

{TEU_NOM}`,
        },
      ],
      'meta-ads': [
        {
          title: 'Campanya',
          body: `Captació propietaris (50%): Radi {RADI}km, 35-65
Captació compradors (30%): 25-55, interessos immobiliària
Retargeting (20%)

Creativitats:
1. "Vens el teu pis? Valoració gratuïta en 24h"
2. "{HABITACIONS} hab, {ZONA}, {PREU}€"
3. Vídeo tour 30s per una propietat`,
        },
      ],
      'agent-prompt': [
        {
          title: 'Prompt base',
          body: `Ets l'assistent de {NOM_NEGOCI}, immobiliària a {CIUTAT}.

SERVEIS: {SERVEIS}

ROL: Qualificar compradors, presentar propietats, gestionar visites, captar propietats.

PER COMPRADOR: tipus, zona, pressupost, habitacions, termini

PER PROPIETARI: agendar valoració gratuïta

IMPORTANT: Mai donis valoracions sense visita prèvia.`,
        },
      ],
    },
  },

  {
    key: 'ADVOCATS',
    label: 'Advocats / Despatx jurídic',
    templates: {
      prospecting: [
        {
          title: 'Email fred',
          body: `Assumpte: Consulta gratuïta 30min

Hola {NOM_CONTACTE},

Primera consulta gratuïta de 30min (presencial o online).

Sense compromís. T'explicarem com treballem i honoraris orientatius.

Àrees: immobiliari, laboral, mercantil, família.

Quan et va bé?

{TEU_NOM}`,
        },
      ],
      'meta-ads': [
        {
          title: 'Campanya',
          body: `Radi {RADI}km | 30-65 | Small Business Owners

1. "Problema legal? 30min de consulta gratuïta"
2. "Herències? Lloguers? Acomiadaments? T'ho expliquem a la primera visita"`,
        },
      ],
      'agent-prompt': [
        {
          title: 'Prompt base',
          body: `Ets l'assistent de {NOM_NEGOCI}, despatx d'advocats a {CIUTAT}.

SERVEIS: {SERVEIS}

ROL: Gestionar consulta inicial gratuïta, derivar al advocat adequat.

PER CONSULTA: nom, tipus assumpte, breu descripció, modalitat

HONORARIS: consulta gratuïta | hora des de {PREU_HORA}€/h | tancat des de {PREU_TANCAT}€

IMPORTANT: Mai donis consells legals per xat. Confidencialitat total.`,
        },
      ],
    },
  },

  {
    key: 'PERRUQUERIA',
    label: 'Perruqueria / Estètica capilar',
    templates: {
      prospecting: [
        {
          title: 'WhatsApp fred',
          body: `Hola {NOM_CONTACTE}! Soc {TEU_NOM}.

Tinc un sistema que recorda les cites per WhatsApp i omple forats d'última hora.

Les clientes reben "Toca repassar el color?" i reserven directament.

Resultat: -80% no-shows, +30% reserves entre setmana.

Vols que t'expliqui en 2 minuts?`,
        },
      ],
      'meta-ads': [
        {
          title: 'Campanya',
          body: `Radi {RADI}km | Dones 20-65 | Perruqueria, estètica, bellesa

1. "Primera visita: 20% descompte en tall i color"
2. "Color esvaeix? Repassa'l des de {PREU}€"
3. Vídeo time-lapse transformació 15s`,
        },
      ],
      'agent-prompt': [
        {
          title: 'Prompt base',
          body: `Ets l'assistent de {NOM_NEGOCI}, perruqueria a {CIUTAT}.

SERVEIS: {SERVEIS}

ROL: Cites, informar serveis/preus, cancel·lacions (24h), recordatoris.

PER CITA: servei, data/hora, estilista preferit, client nou/habitual

HORARI: {HORARI} | Tancat: {DIA_TANCAMENT}`,
        },
      ],
    },
  },

  {
    key: 'ELECTRICISTA',
    label: 'Electricista',
    templates: {
      prospecting: [
        {
          title: 'Email fred',
          body: `Assumpte: Electricistes a {CIUTAT} — clients urgents

Hola {NOM_CONTACTE},

El 40% de les trucades dels electricistes són urgències (sense llum, curtcircuit). Els clients busquen qui vingui JA.

Nosaltres t'ajudem a ser el primer que trobin. Assistència per WhatsApp: classifica urgències i t'avisa automàticament.

Vols que t'expliqui?

{TEU_NOM}`,
        },
      ],
      'meta-ads': [
        {
          title: 'Campanya',
          body: `1. "Sense llum a {CIUTAT}? Sortim d'urgència"
2. "Revisió elèctrica per a cases velles. Des de {PREU}€"`,
        },
      ],
      'agent-prompt': [
        {
          title: 'Prompt base',
          body: `Ets l'assistent de {NOM_NEGOCI}, electricista a {CIUTAT}.

SERVEIS: {SERVEIS}

URGÈNCIA: curtcircuit, sense llum, espurnes → avisar propietari IMMEDIAT + 112 si cal
PLANIFICAT: agendar visita de diagnòstic o pressupost`,
        },
      ],
    },
  },

  {
    key: 'FONTANER',
    label: 'Lampista / Fontaner',
    templates: {
      prospecting: [
        {
          title: 'Email fred',
          body: `Assumpte: Urgències de fontaneria a {CIUTAT}

Hola {NOM_CONTACTE},

Ajudem lampistes a captar clients sense Google Ads.

El client descriu l'avaria per WhatsApp, el sistema classifica urgència i t'avisa automàticament.

Un lampista a {CIUTAT_REF} va passar de 4 a 12 serveis setmanals.

Vols que t'ho expliqui?

{TEU_NOM}`,
        },
      ],
      'meta-ads': [
        {
          title: 'Campanya',
          body: `1. "Fuita d'aigua? Resposta en 30 minuts. {CIUTAT}"
2. "Caldera avariada? Sortim avui."`,
        },
      ],
      'agent-prompt': [
        {
          title: 'Prompt base',
          body: `Ets l'assistent de {NOM_NEGOCI}, lampista a {CIUTAT}.

SERVEIS: {SERVEIS}

URGÈNCIA: fuita activa, inundació, sense aigua → avisar propietari
PLANIFICAT: agendar visita de diagnòstic

PER VISITA: tipus problema, adreça, disponibilitat`,
        },
      ],
    },
  },

  {
    key: 'VETERINARI',
    label: 'Clínica veterinària',
    templates: {
      prospecting: [
        {
          title: 'Email fred',
          body: `Assumpte: Pacients nous per a {NOM_NEGOCI}

Hola {NOM_CONTACTE},

Ajudem clíniques veterinàries a gestionar cites i recordatoris per WhatsApp.

Vacunes pendents? Revisió anual? El sistema avisa els propietaris i ells reserven.

Una clínica va reduir un 70% els no-shows i va augmentar un 25% les visites de rutina.

Vols que t'ho ensenyi?

{TEU_NOM}`,
        },
      ],
      'meta-ads': [
        {
          title: 'Campanya',
          body: `1. "Vacunes al dia? Recordatori automàtic per WhatsApp"
2. "Primera visita {PREU}€. Cuida'l com es mereix"`,
        },
      ],
      'agent-prompt': [
        {
          title: 'Prompt base',
          body: `Ets l'assistent de {NOM_NEGOCI}, clínica veterinària a {CIUTAT}.

SERVEIS: {SERVEIS}

ROL: Cites, classificar urgència, vacunes, recordatoris.

PER CITA: nom animal, espècie, raça, edat, motiu

URGÈNCIES: no respira, traumatisme, convulsions, tòxic → avisar immediatament`,
        },
      ],
    },
  },

  {
    key: 'MARE_DE_DIA',
    label: 'Mare de dia',
    templates: {
      prospecting: [
        {
          title: 'Email fred',
          body: `Assumpte: Gestió de places i comunicació amb les famílies per a {NOM_NEGOCI}

Hola {NOM_CONTACTE},

Ajudem mares de dia a gestionar sol·licituds de plaça, comunicar disponibilitat i enviar recordatoris a les famílies per WhatsApp.

Menys trucades, més tranquil·litat. Les famílies reserven i confirmen sense haver de trucar.

Vols que t'ho ensenyi?

{TEU_NOM}`,
        },
      ],
      'meta-ads': [
        {
          title: 'Campanya',
          body: `1. "Places disponibles per a {EDAT} mesos – {CIUTAT}. Entorn familiar i proper."
2. "Mare de dia a {CIUTAT}. Màxim {MAX_INFANTS} infants. Sol·licita informació."`,
        },
      ],
      'agent-prompt': [
        {
          title: 'Prompt base',
          body: `Ets l'assistent de {NOM_NEGOCI}, mare de dia a {CIUTAT}.

SERVEIS: {SERVEIS}

ROL: Informar sobre places disponibles, horaris, preus i documentació necessària per a la matrícula.

PER SOL·LICITAR PLAÇA: nom de l'infant, data de naixement, jornada desitjada (completa/mitja), data d'incorporació prevista

HORARI: {HORARI}

IMPORTANT:
- Màxim 6 infants simultanis (normativa vigent)
- Edats acceptades: 4 mesos fins a 3 anys
- Si la família insisteix en edats fora del rang → explica la normativa amb amabilitat
- Per urgències o incidències amb un infant → avisar directament a {NOM_NEGOCI}`,
        },
      ],
    },
  },

  {
    key: 'PSICOLEG',
    label: 'Psicòleg / Psicòloga',
    templates: {
      prospecting: [
        {
          title: 'Contacte discret',
          body: `Hola {NOM_CONTACTE},

Ajudem professionals de la psicologia a gestionar cites de forma discreta i reduir no-shows.

El sistema envia recordatoris per WhatsApp i els pacients confirmen o cancel·len sense trucar.

Resultat: -70% no-shows.

Vols que t'expliqui?

{TEU_NOM}

---
Nota: To molt discret. Mai "vendre" sinó "ajudar".`,
        },
      ],
      'meta-ads': [
        {
          title: 'Campanya',
          body: `"Primera visita {PREU}€. Presencial o online. Sense llista d'espera."

Imatge: entorn tranquil, mai cares de patiment`,
        },
      ],
      'agent-prompt': [
        {
          title: 'Prompt base',
          body: `Ets l'assistent de {NOM_NEGOCI}, centre de psicologia a {CIUTAT}.

SERVEIS: {SERVEIS}

ROL: Cites discretes, informar modalitats, explicar primera visita.

PER CITA: nom, modalitat, disponibilitat, per a adult/parella/menor

MOLT IMPORTANT:
- Mai demanis detalls del motiu de consulta per escrit
- Si expressa crisis → 024 + avisar propietari
- Tracte empàtic, mai jutgis`,
        },
      ],
    },
  },
];

export function getSectorTemplate(key: string): SectorTemplate | undefined {
  return SECTOR_TEMPLATES.find(t => t.key === key);
}

export function renderTemplate(body: string, vars: TemplateVariables): string {
  let result = body;
  for (const [key, value] of Object.entries(vars)) {
    result = result.replaceAll(`{${key}}`, value);
  }
  return result;
}

export function extractPlaceholders(body: string): string[] {
  const matches = body.match(/\{(\w+)\}/g);
  if (!matches) return [];
  return Array.from(new Set(matches.map(m => m.slice(1, -1))));
}
