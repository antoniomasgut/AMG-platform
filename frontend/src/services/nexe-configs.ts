import { apiFetch } from './api';

export const getNexeConfigs = (tenantId: string) =>
  apiFetch<Record<string, string>>(`/nexe/tenants/${tenantId}/configs`);

export const getNexeConfig = (tenantId: string, serviceKey: string) =>
  apiFetch<string>(`/nexe/tenants/${tenantId}/configs/${serviceKey}`);

export const saveNexeConfig = (tenantId: string, serviceKey: string, config: object) =>
  apiFetch<void>(`/nexe/tenants/${tenantId}/configs/${serviceKey}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(config),
  });

export const provisionCalendar = (tenantId: string) =>
  apiFetch<{ calendarId: string; message: string }>(`/nexe/tenants/${tenantId}/calendar/provision`, {
    method: 'POST',
  });

export const getCalendarOAuthUrl = (tenantId: string) =>
  apiFetch<{ url: string }>(`/nexe/tenants/${tenantId}/calendar/oauth-url`);

export const getCalendarSaEmail = (tenantId: string) =>
  apiFetch<{ email: string; configured: string }>(`/nexe/tenants/${tenantId}/calendar/sa-email`);

export const validateCalendarId = (tenantId: string, calendarId: string) =>
  apiFetch<{ valid: boolean; message?: string; error?: string }>(`/nexe/tenants/${tenantId}/calendar/validate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ calendarId }),
  });

// ── Sector groups ──────────────────────────────────────────────

export type AgendaMode = 'appointment' | 'inspection' | 'vehicle' | 'meeting';
export type QuoteMode = 'formal' | 'pricelist';

const INSPECTION_SECTORS = ['PINTOR', 'ELECTRICISTA', 'FONTANER', 'JARDINER', 'NETEJA'];
const PRICELIST_SECTORS  = ['FISIOTERAPEUTA', 'PSICOLEG', 'NUTRICIONISTA', 'PERRUQUERIA', 'ESTETICA', 'PERRUQUERIA_CANINA', 'VETERINARI'];

export function getAgendaMode(sector?: string | null): AgendaMode {
  if (!sector) return 'appointment';
  if (INSPECTION_SECTORS.includes(sector)) return 'inspection';
  if (sector === 'TALLER_MECANIC') return 'vehicle';
  if (sector === 'GESTORIA') return 'meeting';
  return 'appointment';
}

export function getQuoteMode(sector?: string | null): QuoteMode {
  if (!sector) return 'formal';
  return PRICELIST_SECTORS.includes(sector) ? 'pricelist' : 'formal';
}

// ── Agenda (F2) ────────────────────────────────────────────────

export interface WorkDayConfig {
  enabled: boolean;
  start: string;
  end: string;
}

export interface ClientQuestion {
  id: string;
  question: string;
  required: boolean;
}

export interface AgendaConfig {
  calendar_type: 'google' | 'google_oauth' | 'calendly' | 'manual';
  google_calendar_id: string;
  google_refresh_token?: string;
  slot_duration_minutes: number;
  buffer_minutes: number;
  max_days_advance: number;
  min_notice_hours: number;
  service_zone?: string;
  estimated_pickup_days?: number;
  working_hours: Record<string, WorkDayConfig>;
  holidays: string[];
  client_questions: ClientQuestion[];
  confirmation_template: string;
}

const BASE_WORKING_HOURS: Record<string, WorkDayConfig> = {
  monday:    { enabled: true,  start: '09:00', end: '18:00' },
  tuesday:   { enabled: true,  start: '09:00', end: '18:00' },
  wednesday: { enabled: true,  start: '09:00', end: '18:00' },
  thursday:  { enabled: true,  start: '09:00', end: '18:00' },
  friday:    { enabled: true,  start: '09:00', end: '17:00' },
  saturday:  { enabled: false, start: '09:00', end: '13:00' },
  sunday:    { enabled: false, start: '09:00', end: '13:00' },
};

function q(question: string, required = false): ClientQuestion {
  return { id: crypto.randomUUID(), question, required };
}

const BASE_AGENDA: Omit<AgendaConfig, 'slot_duration_minutes' | 'buffer_minutes' | 'client_questions' | 'confirmation_template'> = {
  calendar_type: 'manual',
  google_calendar_id: '',
  max_days_advance: 30,
  min_notice_hours: 2,
  working_hours: BASE_WORKING_HOURS,
  holidays: [],
};

export const DEFAULT_AGENDA: AgendaConfig = {
  ...BASE_AGENDA,
  slot_duration_minutes: 60,
  buffer_minutes: 0,
  holidays: [],
  client_questions: [],
  confirmation_template: "Cita confirmada per al {{data}} a les {{hora}}. T'esperem!",
};

const SECTOR_AGENDA_DEFAULTS: Record<string, Partial<AgendaConfig>> = {
  FISIOTERAPEUTA: {
    slot_duration_minutes: 60, buffer_minutes: 15,
    client_questions: [q('Motiu de la consulta?', true), q('Ets pacient nou?', true), q('Tens alguna patologia o lesió prèvia rellevant?')],
    confirmation_template: "Consulta confirmada per al {{data}} a les {{hora}}. Si has d'anul·lar, avisa amb 24h d'antelació.",
  },
  PSICOLEG: {
    slot_duration_minutes: 60, buffer_minutes: 15,
    client_questions: [q('Motiu de la consulta?', true), q('Ets pacient nou?', true), q('Prefereixes sessió presencial o online?')],
    confirmation_template: "Sessió confirmada per al {{data}} a les {{hora}}. La confidencialitat és total.",
  },
  NUTRICIONISTA: {
    slot_duration_minutes: 45, buffer_minutes: 15,
    client_questions: [q('Objectiu principal (pèrdua de pes, guany muscular, salut general)?', true), q('Ets pacient nou?', true), q("Tens alguna al·lèrgia o intolerància alimentària?")],
    confirmation_template: "Consulta confirmada per al {{data}} a les {{hora}}. Porta si pots un registre alimentari dels últims 3 dies.",
  },
  PERRUQUERIA: {
    slot_duration_minutes: 45, buffer_minutes: 10,
    client_questions: [q('Quin servei vols (tall, tint, mecxes, pentinat...)?', true), q('Tens el cabell llarg, mig o curt?', true)],
    confirmation_template: "Cita confirmada per al {{data}} a les {{hora}}. T'esperem!",
  },
  ESTETICA: {
    slot_duration_minutes: 60, buffer_minutes: 15,
    client_questions: [q('Quin tractament vols?', true), q('Ets client nova?'), q('Tens alguna al·lèrgia a cosmétics?')],
    confirmation_template: "Tractament confirmat per al {{data}} a les {{hora}}. Recorda arribar 5 minuts abans.",
  },
  PERRUQUERIA_CANINA: {
    slot_duration_minutes: 60, buffer_minutes: 30,
    client_questions: [q('Raça del gos?', true), q('Mida aproximada (petit/mig/gran)?', true), q('Quin servei necessites (bany, talla, bany+talla)?', true)],
    confirmation_template: "Cita pel teu gos confirmada per al {{data}} a les {{hora}}!",
  },
  VETERINARI: {
    slot_duration_minutes: 30, buffer_minutes: 10,
    client_questions: [q('Espècie (gos, gat, ocell...)?', true), q('Raça i edat de l\'animal?', true), q('Motiu de la visita?', true)],
    confirmation_template: "Visita veterinària confirmada per al {{data}} a les {{hora}}. Porta el carnet de vacunes si el tens.",
  },
  MARE_DE_DIA: {
    slot_duration_minutes: 45, buffer_minutes: 0, max_days_advance: 30, min_notice_hours: 24,
    client_questions: [q('Nom i edat de l\'infant?', true), q('Jornada desitjada (completa / mitja)?', true), q('Data aproximada d\'incorporació?', true)],
    confirmation_template: "Visita de coneixença confirmada per al {{data}} a les {{hora}}. Us esperem per conèixer la família!",
  },
  ACADEMIA: {
    slot_duration_minutes: 60, buffer_minutes: 5,
    client_questions: [q('Assignatura o matèria?', true), q('Nivell actual (primària, ESO, Batxillerat...)?', true), q('Prefereixes classe presencial o online?')],
    confirmation_template: "Classe confirmada per al {{data}} a les {{hora}}. Porta el material de l'assignatura.",
  },
  // Inspection sectors
  PINTOR: {
    slot_duration_minutes: 60, buffer_minutes: 0, max_days_advance: 14, min_notice_hours: 4,
    service_zone: '',
    client_questions: [q("Adreça on cal fer la feina?", true), q('Tipus de treball (pintura interior, exterior, impermeabilització...)?', true), q('Superfície aproximada en m² (si ho saps)?'), q('Urgència (normal / urgent)?')],
    confirmation_template: "Visita d'inspecció confirmada per al {{data}} a les {{hora}}. Us enviarem el pressupost el mateix dia.",
  },
  ELECTRICISTA: {
    slot_duration_minutes: 60, buffer_minutes: 0, max_days_advance: 14, min_notice_hours: 2,
    service_zone: '',
    client_questions: [q("Adreça on cal fer la feina?", true), q('Descripció del treball o problema?', true), q('Urgència (hi ha risc elèctric immediat)?')],
    confirmation_template: "Visita confirmada per al {{data}} a les {{hora}}. Si és urgent truca'ns directament.",
  },
  FONTANER: {
    slot_duration_minutes: 60, buffer_minutes: 0, max_days_advance: 7, min_notice_hours: 1,
    service_zone: '',
    client_questions: [q("Adreça?", true), q('Descripció de l\'avaria?', true), q('Urgència (perd aigua, inundació)?', true)],
    confirmation_template: "Visita confirmada per al {{data}} a les {{hora}}. En cas d'urgència extrema truca'ns.",
  },
  JARDINER: {
    slot_duration_minutes: 90, buffer_minutes: 0, max_days_advance: 21, min_notice_hours: 24,
    service_zone: '',
    client_questions: [q("Adreça?", true), q('Tipus de treball (manteniment, tala, disseny, reg...)?', true), q('Superfície aproximada del jardí (m²)?')],
    confirmation_template: "Visita d'inspecció confirmada per al {{data}} a les {{hora}}.",
  },
  NETEJA: {
    slot_duration_minutes: 60, buffer_minutes: 0, max_days_advance: 14, min_notice_hours: 24,
    service_zone: '',
    client_questions: [q("Adreça?", true), q('Tipus d\'espai (habitatge, oficina, local comercial)?', true), q('Superfície aproximada (m²)?'), q('Freqüència desitjada (una vegada, setmanal, quinzenal)?')],
    confirmation_template: "Visita de valoració confirmada per al {{data}} a les {{hora}}.",
  },
  // Vehicle
  TALLER_MECANIC: {
    slot_duration_minutes: 30, buffer_minutes: 0, max_days_advance: 14, min_notice_hours: 2,
    estimated_pickup_days: 2,
    client_questions: [q('Marca i model del vehicle?', true), q('Any del vehicle?', true), q('Km actuals?'), q('Descripció del problema o servei que necessites?', true), q('Matrícula (opcional)?')],
    confirmation_template: "Deixada del vehicle confirmada per al {{data}} a les {{hora}}. Estimem recollida en {{dies_recollida}} dies.",
  },
  // Meeting
  GESTORIA: {
    slot_duration_minutes: 30, buffer_minutes: 10, max_days_advance: 30, min_notice_hours: 24,
    client_questions: [q('Motiu de la reunió?', true), q('Tipus de gestió (fiscal, laboral, comptabilitat, altres)?', true), q('Aportes documentació?')],
    confirmation_template: "Reunió confirmada per al {{data}} a les {{hora}}. Si cal preparar documentació específica, t'avisarem.",
  },
};

export function getAgendaDefaults(sector?: string | null): AgendaConfig {
  const overrides = sector ? (SECTOR_AGENDA_DEFAULTS[sector] ?? {}) : {};
  return { ...DEFAULT_AGENDA, ...overrides };
}

// ── Pressupostos (F3) ──────────────────────────────────────────

export interface ServiceItem {
  id: string;
  name: string;
  price: number;
  unit: string;
  duration?: string;
  description: string;
}

export interface PressupostosConfig {
  quote_validity_days: number;
  quote_header: string;
  quote_footer: string;
  services_catalog: ServiceItem[];
  quote_followup_enabled: boolean;
  quote_followup_days: number;
  quote_followup_message: string;
  /** Mòdul 53: cobrament online amb l'Stripe del tenant en acceptar el document */
  online_payment_mode?: 'OFF' | 'FULL' | 'DEPOSIT';
  deposit_percent?: number;
}

export const DEFAULT_PRESSUPOSTOS: PressupostosConfig = {
  online_payment_mode: 'OFF',
  deposit_percent: 30,
  quote_validity_days: 30,
  quote_header: '',
  quote_footer: 'Gràcies per confiar en nosaltres.',
  services_catalog: [],
  quote_followup_enabled: false,
  quote_followup_days: 3,
  quote_followup_message: "Hola {{nom}}! Et volem recordar que fa uns dies et vam enviar informació sobre el nostre servei. Estem aquí si tens alguna pregunta. 😊",
};

function svc(name: string, price: number, unit: string, description = '', duration?: string): ServiceItem {
  return { id: crypto.randomUUID(), name, price, unit, description, duration };
}

const SECTOR_QUOTE_DEFAULTS: Record<string, Partial<PressupostosConfig>> = {
  // Formal quotes (oficis)
  PINTOR: {
    quote_validity_days: 30,
    services_catalog: [
      svc('Pintura interior (per m²)', 8, 'm²', 'Inclou preparació i dues mans de pintura'),
      svc('Pintura exterior (per m²)', 12, 'm²', 'Inclou imprimació i pintura exterior'),
      svc('Impermeabilització (per m²)', 15, 'm²'),
      svc('Hores de mà d\'obra', 25, 'hora'),
    ],
  },
  ELECTRICISTA: {
    quote_validity_days: 30,
    services_catalog: [
      svc('Punt de llum', 80, 'unitat'),
      svc('Endoll o interruptor', 35, 'unitat'),
      svc('Quadre elèctric (substitució)', 300, 'unitat'),
      svc('Hores de mà d\'obra', 50, 'hora'),
    ],
  },
  FONTANER: {
    quote_validity_days: 15,
    services_catalog: [
      svc('Reparació d\'avaria', 60, 'hora', 'Sense materials inclosos'),
      svc('Instal·lació d\'aixeta', 120, 'unitat', 'Inclou mà d\'obra, sense material'),
      svc('Desembussament', 80, 'servei'),
      svc('Instal·lació sanitari', 200, 'unitat'),
    ],
  },
  JARDINER: {
    quote_validity_days: 30,
    services_catalog: [
      svc('Manteniment jardí (per hora)', 25, 'hora'),
      svc('Tala d\'arbres', 150, 'unitat'),
      svc('Disseny jardí', 500, 'projecte'),
      svc('Instal·lació sistema de reg', 400, 'projecte'),
    ],
  },
  NETEJA: {
    quote_validity_days: 15,
    services_catalog: [
      svc('Neteja habitatge (per hora)', 18, 'hora'),
      svc('Neteja profunda', 25, 'hora'),
      svc('Neteja post-obra (per m²)', 8, 'm²'),
      svc('Neteja comercial/oficina (per hora)', 20, 'hora'),
    ],
  },
  TALLER_MECANIC: {
    quote_validity_days: 15,
    services_catalog: [
      svc('Canvi oli i filtres', 60, 'servei'),
      svc('Revisió general (30 punts)', 80, 'servei'),
      svc('Canvi pastilles de fre (per eix)', 120, 'eix'),
      svc('Hores de mà d\'obra', 55, 'hora'),
    ],
  },
  GESTORIA: {
    quote_validity_days: 30,
    services_catalog: [
      svc('Declaració de la renda', 80, 'persona'),
      svc('Comptabilitat mensual', 150, 'mes'),
      svc('Constitució empresa', 500, 'gestió'),
      svc('Nòmina mensual (per treballador)', 30, 'treballador/mes'),
    ],
  },
  ACADEMIA: {
    quote_validity_days: 30,
    services_catalog: [
      svc('Classe individual', 25, 'hora'),
      svc('Classe grupal (fins 4 alumnes)', 15, 'hora/alumne'),
      svc('Pack 10 classes individuals', 220, 'pack'),
    ],
  },
  // Price lists (salut/estètica)
  FISIOTERAPEUTA: {
    services_catalog: [
      svc('Sessió de fisioteràpia', 45, 'sessió', '', '1h'),
      svc('Drenatge limfàtic', 55, 'sessió', '', '1h'),
      svc('Acupuntura', 40, 'sessió', '', '45min'),
      svc('Pilates terapèutic', 35, 'sessió', 'Grup reduït, màx 6 persones', '50min'),
    ],
  },
  PSICOLEG: {
    services_catalog: [
      svc('Consulta individual', 65, 'sessió', '', '1h'),
      svc('Teràpia de parella', 90, 'sessió', '', '1h 15min'),
      svc('Consulta online', 55, 'sessió', '', '1h'),
    ],
  },
  NUTRICIONISTA: {
    services_catalog: [
      svc('Primera consulta', 70, 'sessió', 'Inclou anàlisi completa i pla nutricional', '1h 15min'),
      svc('Revisió i seguiment', 40, 'sessió', '', '30min'),
      svc('Pack 5 revisions', 175, 'pack'),
    ],
  },
  PERRUQUERIA: {
    services_catalog: [
      svc('Tall de cabell (cabellera curta/mig)', 18, 'servei', '', '30min'),
      svc('Tall de cabell (cabellera llarga)', 25, 'servei', '', '45min'),
      svc('Tint bàsic', 45, 'servei', '', '1h 30min'),
      svc('Mecxes', 60, 'servei', '', '2h'),
      svc('Pentinat per a ocasió', 35, 'servei', '', '1h'),
    ],
  },
  ESTETICA: {
    services_catalog: [
      svc('Neteja facial completa', 45, 'servei', '', '1h'),
      svc('Depilació cera cames senceres', 30, 'servei', '', '45min'),
      svc('Depilació cera axil·les', 10, 'servei', '', '15min'),
      svc('Manicura', 20, 'servei', '', '45min'),
      svc('Pedicura', 25, 'servei', '', '1h'),
    ],
  },
  PERRUQUERIA_CANINA: {
    services_catalog: [
      svc('Bany i secat (gos petit)', 25, 'servei', '', '1h'),
      svc('Bany i secat (gos gran)', 40, 'servei', '', '1h 30min'),
      svc('Talla completa (gos petit)', 40, 'servei', '', '1h 30min'),
      svc('Talla completa (gos gran)', 60, 'servei', '', '2h 30min'),
    ],
  },
  VETERINARI: {
    services_catalog: [
      svc('Consulta general', 35, 'visita', '', '30min'),
      svc('Vacunació', 25, 'vacuna', ''),
      svc('Desparasitació', 15, 'tractament'),
      svc('Esterilització gat', 180, 'cirurgia'),
      svc('Esterilització gos (petit)', 250, 'cirurgia'),
    ],
  },
  MARE_DE_DIA: {
    quote_validity_days: 30,
    services_catalog: [
      svc('Jornada completa (mensual)', 450, 'mes', 'Inclou àpats i material didàctic, de 7:30h a 17:00h'),
      svc('Mitja jornada matí (mensual)', 280, 'mes', 'De 7:30h a 13:00h, inclou àpat del migdia'),
      svc('Matrícula / Inscripció', 150, 'únic', 'Quota única d\'alta al servei'),
    ],
  },
};

export function getPressupostosDefaults(sector?: string | null): PressupostosConfig {
  const overrides = sector ? (SECTOR_QUOTE_DEFAULTS[sector] ?? {}) : {};
  return { ...DEFAULT_PRESSUPOSTOS, ...overrides };
}

// ── Seguiment / F4 (service key intern: FIDELITZACIO) ─────────

export interface SeasonalCampaign {
  id: string;
  name: string;
  send_date: string;   // YYYY-MM-DD
  message: string;
}

export interface FidelitzacioConfig {
  google_reviews_url: string;
  followup_days: number;
  followup_template: string;
  reengagement_months: number;
  reengagement_template: string;
  seasonal_campaigns?: SeasonalCampaign[];
}

export const DEFAULT_FIDELITZACIO: FidelitzacioConfig = {
  seasonal_campaigns: [],
  google_reviews_url: '',
  followup_days: 3,
  followup_template: "Hola {{nom}}, com va anar el servei? Si us plau deixa'ns una ressenya: {{url_ressenya}}",
  reengagement_months: 3,
  reengagement_template: "Hola {{nom}}, fa temps que no et veiem! Tenim una oferta especial per a tu.",
};

// ── Equip (F5) ────────────────────────────────────────────────

export interface TeamMember {
  id: string;
  name: string;
  role: string;
  whatsapp: string;
}

export interface EquipConfig {
  telegram_group_id: string;
  telegram_group_name: string;
  members: TeamMember[];
  daily_report_enabled: boolean;
  daily_report_time: string;
  report_template: string;
  unresponded_alert_enabled: boolean;
  unresponded_hours_threshold: number;
}

export const DEFAULT_EQUIP: EquipConfig = {
  telegram_group_id: '',
  telegram_group_name: '',
  members: [],
  daily_report_enabled: false,
  daily_report_time: '18:00',
  report_template: 'Resum del dia {{data}}:\n- Cites: {{num_cites}}\n- Nous contactes: {{nous_contactes}}',
  unresponded_alert_enabled: false,
  unresponded_hours_threshold: 4,
};
