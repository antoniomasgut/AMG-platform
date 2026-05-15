'use client';

import { apiFetch } from './api';

// --- Types ---

export type BlockType =
  | 'hero' | 'text' | 'services' | 'gallery' | 'contact-form'
  | 'faq' | 'testimonials' | 'cta' | 'footer' | 'map';

export interface Block {
  id: string;
  type: BlockType;
  props: Record<string, unknown>;
}

export interface PageContent {
  blocks: Block[];
}

export interface PageStyles {
  fontFamily: string;
  primaryColor: string;
  secondaryColor: string;
  bgColor: string;
  textColor: string;
  borderRadius: string;
}

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

// --- Engine API ---

export async function listLandings(tenantId: string, page = 0, size = 20): Promise<LandingSummary[]> {
  return apiFetch<LandingSummary[]>(`/engine/tenants/${tenantId}/landings?page=${page}&size=${size}`);
}

export async function getLanding(tenantId: string, landingId: string): Promise<LandingDetail> {
  return apiFetch<LandingDetail>(`/engine/tenants/${tenantId}/landings/${landingId}`);
}

export async function createLanding(tenantId: string, title: string, slug: string, templateId?: string): Promise<LandingDetail> {
  return apiFetch<LandingDetail>(`/engine/tenants/${tenantId}/landings`, {
    method: 'POST',
    body: JSON.stringify({ title, slug, templateId }),
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

// --- Block templates ---

export const BLOCK_TEMPLATES: Record<BlockType, { label: string; icon: string; defaultProps: Record<string, unknown> }> = {
  hero: {
    label: 'Hero',
    icon: '⊞',
    defaultProps: { title: 'Títol principal', subtitle: 'Subtítol', ctaText: 'Contacta', ctaLink: '#contact', bgImage: '' },
  },
  text: {
    label: 'Text',
    icon: '¶',
    defaultProps: { title: 'Sobre nosaltres', body: '<p>Text de contingut</p>' },
  },
  services: {
    label: 'Serveis',
    icon: '◆',
    defaultProps: { title: 'Els nostres serveis', items: [{ name: 'Servei 1', desc: 'Descripció' }] },
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
    defaultProps: { title: 'Preguntes freqüents', items: [{ q: 'Pregunta?', a: 'Resposta' }] },
  },
  testimonials: {
    label: 'Testimonis',
    icon: '★',
    defaultProps: { title: 'Què diuen els clients', items: [{ name: 'Client', text: 'Testimoni', rating: 5 }] },
  },
  cta: {
    label: 'CTA',
    icon: '➤',
    defaultProps: { title: 'Crida a l\'acció', ctaText: 'Contacta' },
  },
  footer: {
    label: 'Footer',
    icon: '⌂',
    defaultProps: { copyright: '© 2026 Tots els drets reservats' },
  },
  map: {
    label: 'Mapa',
    icon: '⊕',
    defaultProps: { address: 'Carrer, Ciutat', lat: 39.5696, lng: 2.6502 },
  },
};
