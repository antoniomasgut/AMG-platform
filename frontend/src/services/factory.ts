'use client';

import { apiFetch } from './api';

// --- Types ---

export type BlockType =
  | 'header' | 'hero' | 'stats' | 'trust-bar' | 'steps'
  | 'text' | 'services' | 'gallery' | 'contact-form'
  | 'faq' | 'testimonials' | 'cta' | 'footer' | 'map' | 'opening-hours'
  | 'pricing' | 'team' | 'video' | 'reviews' | 'chat-cta' | 'before-after';

export interface Block {
  id: string;
  type: BlockType;
  props: Record<string, unknown>;
}

export interface PageContent {
  blocks: Block[];
}

export interface PageStyles {
  fontHeading: string;
  fontBody: string;
  primaryColor: string;
  accentColor: string;
  bgColor: string;
  textColor: string;
  borderRadius: string;
  // Analytics
  gaId?: string;
  // CSS personalitzat
  customCss?: string;
  // Contacte ràpid
  whatsappNumber?: string;
  phone?: string;
  address?: string;
  businessType?: string;
  // Widget de xat flotant
  chatEnabled?: boolean;
  chatBusinessName?: string;
  /** @deprecated use fontHeading/fontBody */
  fontFamily?: string;
  /** @deprecated use accentColor */
  secondaryColor?: string;
}

export const BUSINESS_TYPES: Array<{ value: string; label: string }> = [
  { value: 'LocalBusiness',           label: 'Negoci local (genèric)' },
  { value: 'Restaurant',              label: 'Restaurant / Bar' },
  { value: 'AutoRepair',              label: 'Taller / Automoció' },
  { value: 'HealthAndBeautyBusiness', label: 'Salut i Bellesa' },
  { value: 'MedicalBusiness',         label: 'Clínica / Salut' },
  { value: 'RealEstateAgent',         label: 'Immobiliària' },
  { value: 'EducationalOrganization', label: 'Academia / Formació' },
  { value: 'ProfessionalService',     label: 'Serveis professionals' },
  { value: 'HomeAndConstructionBusiness', label: 'Construcció / Reformes' },
  { value: 'Store',                   label: 'Comerç / Botiga' },
];

export const TEMPLATE_STYLES: Record<string, Partial<PageStyles>> = {
  'restaurant':   { fontHeading: 'Playfair Display, serif', fontBody: 'Lato, sans-serif',        primaryColor: '#c8423a', accentColor: '#f5a623', bgColor: '#fffbf5', textColor: '#1a1a1a' },
  'profesional':  { fontHeading: 'Montserrat, sans-serif',  fontBody: 'Open Sans, sans-serif',    primaryColor: '#1a365d', accentColor: '#3182ce', bgColor: '#f8fafc', textColor: '#1e293b' },
  'comercio':     { fontHeading: 'Oswald, sans-serif',       fontBody: 'Source Sans Pro, sans-serif', primaryColor: '#2d3748', accentColor: '#e53e3e', bgColor: '#ffffff', textColor: '#2d3748' },
  'serveis':      { fontHeading: 'Raleway, sans-serif',      fontBody: 'Lato, sans-serif',        primaryColor: '#276749', accentColor: '#f6ad55', bgColor: '#f7faf7', textColor: '#1a1a1a' },
  'salut':        { fontHeading: 'DM Sans, sans-serif',      fontBody: 'Inter, sans-serif',       primaryColor: '#553c9a', accentColor: '#9f7aea', bgColor: '#fdfaff', textColor: '#1a202c' },
  'taller':       { fontHeading: 'Roboto Slab, serif',       fontBody: 'Roboto, sans-serif',      primaryColor: '#1a202c', accentColor: '#f6b94b', bgColor: '#f8f8f8', textColor: '#1a202c' },
  'immobiliaria': { fontHeading: 'Cormorant Garamond, serif',fontBody: 'Source Sans Pro, sans-serif', primaryColor: '#2c5282', accentColor: '#c8a951', bgColor: '#fafaf8', textColor: '#1a202c' },
  'evento':       { fontHeading: 'Raleway, sans-serif',      fontBody: 'Open Sans, sans-serif',   primaryColor: '#702459', accentColor: '#f687b3', bgColor: '#fff5f7', textColor: '#1a202c' },
  'basica':       { fontHeading: 'Inter, sans-serif',        fontBody: 'Inter, sans-serif',       primaryColor: '#FF6B00', accentColor: '#1e293b', bgColor: '#ffffff', textColor: '#1e293b' },
};

export interface LandingSummary {
  id: string;
  title: string;
  slug: string;
  status: string;
  publicUrl: string;
  domainVerified: boolean;
  createdAt: string;
}

export interface VersionResponse {
  id: string;
  versionNumber: number;
  status: string;
  content: PageContent;
  styles: PageStyles | null;
  publishedAt: string | null;
  createdAt: string;
}

export interface LandingDetail {
  id: string;
  title: string;
  slug: string;
  status: string;
  publicUrl: string;
  customDomain: string | null;
  domainVerified: boolean;
  metaDescription?: string | null;
  ogImageUrl?: string | null;
  previewToken?: string | null;
  versions: VersionResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface PublishResponse {
  versionNumber: number;
  status: string;
  publicUrl: string;
  publishedAt: string;
}

export interface LandingStats {
  viewCount: number;
  contactCount: number;
}

export interface GenerateBlockResult {
  props: Record<string, unknown>;
  model: string;
}

export async function generateBlock(
  blockType: string,
  businessName: string,
  sector: string,
  model: string,
  language = 'ca'
): Promise<GenerateBlockResult> {
  return apiFetch<GenerateBlockResult>('/engine/generate-block', {
    method: 'POST',
    body: JSON.stringify({ blockType, businessName, sector, language, model }),
  });
}

export interface AIModelInfo {
  id: string;
  label: string;
  provider: string;
  requiresApiKey: boolean;
}

export async function listAIModels(): Promise<AIModelInfo[]> {
  return apiFetch<AIModelInfo[]>('/agents/models');
}

// --- Engine API ---

export async function getLandingStats(tenantId: string, landingId: string): Promise<LandingStats> {
  return apiFetch<LandingStats>(`/engine/tenants/${tenantId}/landings/${landingId}/stats`);
}

export interface LandingsGlobalSummary { total: number; published: number; }
export async function getLandingsGlobalSummary(): Promise<LandingsGlobalSummary> {
  return apiFetch<LandingsGlobalSummary>('/engine/admin/landings/summary');
}

export async function listLandings(tenantId: string, page = 0, size = 20): Promise<LandingSummary[]> {
  return apiFetch<LandingSummary[]>(`/engine/tenants/${tenantId}/landings?page=${page}&size=${size}`);
}

export async function getLanding(tenantId: string, landingId: string): Promise<LandingDetail> {
  return apiFetch<LandingDetail>(`/engine/tenants/${tenantId}/landings/${landingId}`);
}

export async function createLanding(
  tenantId: string,
  title: string,
  slug: string,
  templateId?: string,
  styles?: Record<string, string>
): Promise<LandingDetail> {
  return apiFetch<LandingDetail>(`/engine/tenants/${tenantId}/landings`, {
    method: 'POST',
    body: JSON.stringify({ title, slug, templateId, styles: styles ? JSON.stringify(styles) : undefined }),
  });
}

export async function createLandingFromTemplate(
  tenantId: string,
  data: {
    title: string;
    slug: string;
    metaDescription?: string;
    serviceId: string;
    templateId: string;
    filledSections: Record<string, Record<string, unknown>>;
    styles?: Record<string, unknown>;
  }
): Promise<LandingDetail> {
  return apiFetch<LandingDetail>(`/engine/tenants/${tenantId}/landings/from-template`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function createVersion(landingId: string, content: PageContent, styles?: PageStyles): Promise<VersionResponse> {
  return apiFetch<VersionResponse>(`/engine/landings/${landingId}/versions`, {
    method: 'POST',
    body: JSON.stringify({ content, styles }),
  });
}

export async function updateVersion(landingId: string, versionId: string, content: PageContent, styles?: PageStyles): Promise<VersionResponse> {
  return apiFetch<VersionResponse>(`/engine/landings/${landingId}/versions/${versionId}`, {
    method: 'PUT',
    body: JSON.stringify({ content, styles }),
  });
}

export async function publishLanding(landingId: string): Promise<PublishResponse> {
  return apiFetch<PublishResponse>(`/engine/landings/${landingId}/publish`, { method: 'POST' });
}

export async function unpublishLanding(landingId: string): Promise<void> {
  return apiFetch<void>(`/engine/landings/${landingId}/unpublish`, { method: 'POST' });
}

export async function deleteLanding(tenantId: string, landingId: string): Promise<void> {
  return apiFetch<void>(`/engine/tenants/${tenantId}/landings/${landingId}`, { method: 'DELETE' });
}

export async function generatePreviewToken(tenantId: string, landingId: string): Promise<{ token: string; previewUrl: string }> {
  return apiFetch<{ token: string; previewUrl: string }>(
    `/engine/tenants/${tenantId}/landings/${landingId}/preview-token`,
    { method: 'POST' }
  );
}

export async function updateLandingMeta(
  tenantId: string,
  landingId: string,
  data: { metaDescription?: string; ogImageUrl?: string }
): Promise<void> {
  await apiFetch<unknown>(`/engine/tenants/${tenantId}/landings/${landingId}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function duplicateLanding(tenantId: string, landingId: string): Promise<LandingDetail> {
  return apiFetch<LandingDetail>(
    `/engine/tenants/${tenantId}/landings/${landingId}/duplicate`,
    { method: 'POST' }
  );
}

// --- Chat context ---

export interface ChatContext {
  landingId: string;
  businessName: string;
  sector: string | null;
  systemPrompt: string;
  profanityAction: string;
  updatedAt: string;
}

export async function getChatContext(landingId: string): Promise<ChatContext> {
  return apiFetch<ChatContext>(`/engine/landings/${landingId}/chat`);
}

export async function saveChatContext(landingId: string, data: {
  businessName: string;
  sector?: string;
  systemPrompt: string;
  profanityAction?: string;
}): Promise<ChatContext> {
  return apiFetch<ChatContext>(`/engine/landings/${landingId}/chat`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function deleteChatContext(landingId: string): Promise<void> {
  return apiFetch<void>(`/engine/landings/${landingId}/chat`, { method: 'DELETE' });
}

// --- Landing defaults (auto-fill) ---

export interface LandingDefaults {
  businessName: string;
  phone: string;
  email: string;
  address: string;
  city: string;
  sector: string;
  whatsappNumber: string;
  heroSuggestions: { title?: string; subtitle?: string };
  contactSuggestions: { phone?: string; email?: string; address?: string };
}

export async function getLandingDefaults(tenantId: string): Promise<LandingDefaults> {
  return apiFetch<LandingDefaults>(`/engine/tenants/${tenantId}/landing-defaults`);
}

// --- Block templates ---

export const BLOCK_TEMPLATES: Record<BlockType, { label: string; icon: string; defaultProps: Record<string, unknown> }> = {
  header: {
    label: 'Capçalera',
    icon: '☰',
    defaultProps: {
      logoText: 'El Negoci',
      logo: '',
      links: [
        { label: 'Serveis', href: '#services' },
        { label: 'Sobre nosaltres', href: '#about' },
        { label: 'Contacte', href: '#contact' },
      ],
      phone: '',
      ctaText: 'Reserva cita',
      ctaLink: '#contact',
    },
  },
  hero: {
    label: 'Hero',
    icon: '⊞',
    defaultProps: { title: 'Títol principal', subtitle: 'Subtítol', ctaText: 'Contacta\'ns', ctaLink: '#contact', bgImage: '', badgeText: '', secondaryCtaText: '', secondaryCtaLink: '#services' },
  },
  stats: {
    label: 'Estadístiques',
    icon: '📊',
    defaultProps: {
      title: '',
      items: [
        { number: '500+', label: 'Clients satisfets', icon: '👥' },
        { number: '15', label: 'Anys d\'experiència', icon: '🏆' },
        { number: '4.9★', label: 'Valoració Google', icon: '⭐' },
      ],
    },
  },
  'trust-bar': {
    label: 'Confiança',
    icon: '🛡',
    defaultProps: {
      title: 'Empreses que confien en nosaltres',
      items: [
        { name: 'Certificat ISO', icon: '📋' },
        { name: 'Col·legi Professional', icon: '🏛' },
        { name: 'Google Partner', icon: '🔍' },
        { name: '5 Estrelles', icon: '⭐' },
      ],
    },
  },
  steps: {
    label: 'Passos',
    icon: '→',
    defaultProps: {
      title: 'Com treballem',
      items: [
        { number: '1', title: 'Primera consulta', description: 'Et contactem per entendre les teves necessitats.' },
        { number: '2', title: 'Proposta personalitzada', description: 'Preparem una solució adaptada al teu negoci.' },
        { number: '3', title: 'Posada en marxa', description: 'Implementem i t\'acompanyem en tot el procés.' },
      ],
    },
  },
  text: {
    label: 'Text',
    icon: '¶',
    defaultProps: { title: 'Sobre nosaltres', body: '<p>Som un negoci local amb anys d\'experiència al sector. La nostra missió és oferir el millor servei amb la màxima qualitat i atenció personalitzada a cada client.</p>' },
  },
  services: {
    label: 'Serveis',
    icon: '◆',
    defaultProps: {
      title: 'Els nostres serveis',
      items: [
        { name: 'Servei professional', desc: 'Oferim el millor servei adaptat a les necessitats del teu negoci. Qualitat garantida.', icon: '⭐' },
        { name: 'Atenció personalitzada', desc: 'Tracte directe i proper amb cada client. Respondrem les teves preguntes en menys de 24h.', icon: '💬' },
        { name: 'Resultats garantits', desc: 'El nostre compromís és el teu èxit. Si no estàs satisfet, ho arreglem sense cost addicional.', icon: '✓' },
      ],
    },
  },
  gallery: {
    label: 'Galeria',
    icon: '▣',
    defaultProps: { title: 'Galeria', images: [] },
  },
  'contact-form': {
    label: 'Contacte',
    icon: '✉',
    defaultProps: { title: 'Contacta amb nosaltres', email: '', phone: '' },
  },
  faq: {
    label: 'FAQ',
    icon: '?',
    defaultProps: {
      title: 'Preguntes freqüents',
      items: [
        { question: 'Com puc demanar un pressupost?', answer: 'Pots contactar-nos per telèfon, WhatsApp o el formulari de contacte. T\'enviarem el pressupost detallat en menys de 24 hores.' },
        { question: 'Quant tarda a estar llest?', answer: 'Depèn de la feina. Les tasques senzilles les resolem el mateix dia; les més complexes en 2-5 dies hàbils. Et mantindrem informat en tot moment.' },
        { question: 'Doneu garantia del servei?', answer: 'Sí, tots els treballs inclouen garantia. Si no estàs satisfet amb el resultat, ho arreglem sense cap cost addicional.' },
      ],
    },
  },
  testimonials: {
    label: 'Testimonis',
    icon: '★',
    defaultProps: {
      title: 'El que diuen els clients',
      items: [
        { name: 'Maria G.', role: 'Client habitual', text: 'Excel·lent servei i tracte molt professional. Ho recoman a tothom. El millor que he provat a la zona.', rating: 5 },
        { name: 'Joan P.', role: 'Client des de 2022', text: 'El resultat va superar les expectatives. Ràpids, eficients i amb un preu molt raonable. Repetiré sense dubte.', rating: 5 },
        { name: 'Anna M.', role: 'Primera visita', text: 'Vaig venir per recomanació i estic molt contenta. Atenen molt bé i expliquen tot amb detall. 100% recomanable.', rating: 5 },
      ],
    },
  },
  cta: {
    label: 'CTA',
    icon: '➤',
    defaultProps: { title: 'Crida a l\'acció', ctaText: 'Contacta' },
  },
  footer: {
    label: 'Footer',
    icon: '⌂',
    defaultProps: {
      businessName: '',
      tagline: '',
      address: '',
      phone: '',
      email: '',
      links: [
        { label: 'Serveis', href: '#services' },
        { label: 'Sobre nosaltres', href: '#about' },
        { label: 'Contacte', href: '#contact' },
      ],
      socialLinks: [],
      copyright: '',
    },
  },
  map: {
    label: 'Mapa',
    icon: '⊕',
    defaultProps: { address: 'Carrer, Ciutat', lat: 39.5696, lng: 2.6502 },
  },
  'opening-hours': {
    label: 'Horaris',
    icon: '🕐',
    defaultProps: {
      title: "Horaris d'atenció",
      hours: [
        { day: 'Dilluns',   open: '09:00', close: '18:00', closed: false },
        { day: 'Dimarts',   open: '09:00', close: '18:00', closed: false },
        { day: 'Dimecres',  open: '09:00', close: '18:00', closed: false },
        { day: 'Dijous',    open: '09:00', close: '18:00', closed: false },
        { day: 'Divendres', open: '09:00', close: '18:00', closed: false },
        { day: 'Dissabte',  open: '10:00', close: '14:00', closed: false },
        { day: 'Diumenge',  open: '',      close: '',      closed: true  },
      ],
    },
  },
  pricing: {
    label: 'Preus',
    icon: '€',
    defaultProps: {
      title: 'Les nostres tarifes',
      items: [
        { name: 'Bàsic', description: 'Per a autònoms i particulars', price: '29', period: 'mes', features: ['Consulta inicial gratuïta', 'Resposta en 48h', 'Servei adaptat a les teves necessitats'], highlighted: false },
        { name: 'Pro', description: 'Per a empreses i pimes', price: '59', period: 'mes', features: ['Tot del pla Bàsic', 'Atenció prioritària en 24h', 'Visita presencial inclosa', 'Suport mensual il·limitat'], highlighted: true },
      ],
    },
  },
  team: {
    label: 'Equip',
    icon: '👥',
    defaultProps: {
      title: 'El nostre equip',
      items: [
        { name: 'Andreu Mas', role: 'Director i fundador', bio: 'Expert en el sector amb més de 10 anys d\'experiència a Mallorca. Especialista en solucions per a negocis locals.', photo: '' },
        { name: 'Maria Vidal', role: 'Especialista tècnica', bio: 'Formació especialitzada i atenció personalitzada a cada client. La seva prioritat: que surts satisfet.', photo: '' },
      ],
    },
  },
  video: {
    label: 'Vídeo',
    icon: '▶',
    defaultProps: { title: '', videoUrl: '', caption: '' },
  },
  reviews: {
    label: 'Ressenyes',
    icon: '⭐',
    defaultProps: {
      title: 'El que diuen de nosaltres',
      source: 'manual',
      minRating: 4,
      maxItems: 6,
      googleMapsUrl: '',
      items: [
        { name: 'Maria G.', rating: 5, text: 'Excel·lent servei, molt professionals!', date: '2026-01-15' },
        { name: 'Joan P.',  rating: 5, text: 'Molt contents amb el resultat final.', date: '2026-02-03' },
      ],
    },
  },
  'chat-cta': {
    label: 'Chat CTA',
    icon: '💬',
    defaultProps: {
      title: 'Reserva la teva cita ara',
      subtitle: 'Xateja amb el nostre assistent i respon en menys d\'1 minut',
      buttonText: 'Parla amb nosaltres',
      accentColor: '',
    },
  },
  'before-after': {
    label: 'Abans / Després',
    icon: '⇆',
    defaultProps: {
      title: 'El nostre treball',
      beforeImage: '',
      afterImage: '',
      beforeLabel: 'Abans',
      afterLabel: 'Després',
      aspectRatio: '56.25',
    },
  },
};
