'use client';

import { apiFetch } from './api';

export type DocumentType = 'quote' | 'invoice' | 'delivery_note' | 'contract' | 'report' | 'proposal' | 'custom';
export type DocumentStatus = 'DRAFT' | 'FINALIZED' | 'SENT' | 'PAID' | 'CANCELLED';

export interface TemplateResponse {
  id: string;
  tenantId: string;
  name: string;
  documentType: DocumentType;
  version: number;
  active: boolean;
  layout: string;
  dataBindings: string;
  styles: string;
  createdAt: string;
  updatedAt: string;
}

export interface TemplateRequest {
  name: string;
  documentType: DocumentType;
  layout?: string;
  dataBindings?: string;
  styles?: string;
}

export interface VersionResponse {
  id: string;
  templateId: string;
  version: number;
  layout: string;
  dataBindings: string;
  styles: string;
  notes: string | null;
  createdAt: string;
}

export interface ArticleLine {
  description: string;
  quantity: number;
  unitPrice: number;
}

export interface GenerateRequest {
  templateId: string;
  customerId?: string | null;
  customerData?: Record<string, string> | null;
  variables?: Record<string, unknown> | null;
  articles?: ArticleLine[] | null;
}

export interface DocumentResponse {
  id: string;
  tenantId: string;
  templateId: string;
  templateVersion: number;
  number: string;
  status: DocumentStatus;
  htmlContent: string | null;
  pdfUrl: string | null;
  generatedAt: string;
  createdAt: string;
}

export interface AIApplyResponse {
  operations: AIOperation[];
}

export interface AIOperation {
  action: string;
  blockId?: string;
  x?: number;
  y?: number;
  w?: number;
  h?: number;
  style?: Record<string, unknown>;
  visible?: boolean;
}

export interface LayoutBlock {
  id: string;
  type: string;
  x: number;
  y: number;
  w: number;
  h: number;
  visible: boolean;
  config: {
    style: Record<string, unknown>;
    src?: string;
  };
  dataBinding: {
    source: string;
    field: string;
  } | null;
}

export const DOCUMENT_TYPES: { value: DocumentType; label: string }[] = [
  { value: 'quote', label: 'Pressupost' },
  { value: 'invoice', label: 'Factura' },
  { value: 'delivery_note', label: 'Albarà' },
  { value: 'contract', label: 'Contracte' },
  { value: 'report', label: 'Informe' },
  { value: 'proposal', label: 'Proposta' },
  { value: 'custom', label: 'Personalitzat' },
];

export const BLOCK_LIBRARY: { category: string; blocks: { id: string; label: string }[] }[] = [
  {
    category: 'Identitat',
    blocks: [
      { id: 'logo', label: 'Logo' },
      { id: 'company_info', label: 'Info empresa' },
      { id: 'customer_info', label: 'Info client' },
    ],
  },
  {
    category: 'Document',
    blocks: [
      { id: 'document_title', label: 'Títol' },
      { id: 'document_number', label: 'Número' },
      { id: 'document_date', label: 'Data' },
      { id: 'document_validity', label: 'Validesa' },
    ],
  },
  {
    category: 'Comercial',
    blocks: [
      { id: 'products_table', label: 'Taula productes' },
      { id: 'services_table', label: 'Taula serveis' },
      { id: 'subtotal', label: 'Subtotal' },
      { id: 'discount', label: 'Descompte' },
      { id: 'tax', label: 'Impost' },
      { id: 'total', label: 'Total' },
    ],
  },
  {
    category: 'Contingut',
    blocks: [
      { id: 'text', label: 'Text' },
      { id: 'rich_text', label: 'Text enriquit' },
      { id: 'image', label: 'Imatge' },
      { id: 'separator', label: 'Separador' },
      { id: 'page_break', label: 'Salt de pàgina' },
    ],
  },
  {
    category: 'Legal',
    blocks: [
      { id: 'terms', label: 'Termes' },
      { id: 'conditions', label: 'Condicions' },
      { id: 'signature', label: 'Signatura' },
    ],
  },
  {
    category: 'Integracions',
    blocks: [
      { id: 'qr_code', label: 'Codi QR' },
      { id: 'barcode', label: 'Codi barres' },
      { id: 'payment_link', label: 'Enllaç pagament' },
    ],
  },
];

export function defaultLayout(): LayoutBlock[] {
  return [
    { id: 'logo', type: 'logo', x: 0, y: 0, w: 4, h: 2, visible: true, config: { style: {} }, dataBinding: { source: 'company', field: 'logo' } },
    { id: 'company_info', type: 'company_info', x: 4, y: 0, w: 4, h: 2, visible: true, config: { style: {} }, dataBinding: { source: 'company', field: 'name' } },
    { id: 'document_title', type: 'document_title', x: 0, y: 2, w: 12, h: 1, visible: true, config: { style: { fontSize: 18, fontWeight: 'bold', alignment: 'center' } }, dataBinding: null },
    { id: 'document_number', type: 'document_number', x: 0, y: 3, w: 6, h: 1, visible: true, config: { style: {} }, dataBinding: { source: 'document', field: 'number' } },
    { id: 'document_date', type: 'document_date', x: 6, y: 3, w: 6, h: 1, visible: true, config: { style: { alignment: 'right' } }, dataBinding: { source: 'document', field: 'date' } },
    { id: 'customer_info', type: 'customer_info', x: 0, y: 4, w: 6, h: 2, visible: true, config: { style: {} }, dataBinding: { source: 'customer', field: 'name' } },
    { id: 'services_table', type: 'services_table', x: 0, y: 6, w: 12, h: 4, visible: true, config: { style: {} }, dataBinding: null },
    { id: 'subtotal', type: 'subtotal', x: 0, y: 10, w: 6, h: 1, visible: true, config: { style: {} }, dataBinding: { source: 'calculated', field: 'subtotal' } },
    { id: 'tax', type: 'tax', x: 0, y: 11, w: 6, h: 1, visible: true, config: { style: {} }, dataBinding: { source: 'calculated', field: 'tax' } },
    { id: 'total', type: 'total', x: 0, y: 12, w: 6, h: 1, visible: true, config: { style: { fontSize: 16, fontWeight: 'bold' } }, dataBinding: { source: 'calculated', field: 'total' } },
    { id: 'terms', type: 'terms', x: 0, y: 13, w: 12, h: 2, visible: true, config: { style: { fontSize: 10 } }, dataBinding: null },
    { id: 'signature', type: 'signature', x: 0, y: 15, w: 6, h: 2, visible: true, config: { style: {} }, dataBinding: null },
  ];
}

export async function listTemplates(): Promise<TemplateResponse[]> {
  return apiFetch<TemplateResponse[]>('/documents/templates');
}

export async function getTemplate(id: string): Promise<TemplateResponse> {
  return apiFetch<TemplateResponse>(`/documents/templates/${id}`);
}

export async function createTemplate(data: TemplateRequest): Promise<TemplateResponse> {
  return apiFetch<TemplateResponse>('/documents/templates', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function updateTemplate(id: string, data: TemplateRequest): Promise<TemplateResponse> {
  return apiFetch<TemplateResponse>(`/documents/templates/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function deleteTemplate(id: string): Promise<void> {
  return apiFetch<void>(`/documents/templates/${id}`, { method: 'DELETE' });
}

export async function duplicateTemplate(id: string): Promise<TemplateResponse> {
  return apiFetch<TemplateResponse>(`/documents/templates/${id}/duplicate`, { method: 'POST' });
}

export async function listVersions(id: string): Promise<VersionResponse[]> {
  return apiFetch<VersionResponse[]>(`/documents/templates/${id}/versions`);
}

export async function getVersion(id: string, version: number): Promise<VersionResponse> {
  return apiFetch<VersionResponse>(`/documents/templates/${id}/versions/${version}`);
}

export async function restoreVersion(id: string, version: number): Promise<TemplateResponse> {
  return apiFetch<TemplateResponse>(`/documents/templates/${id}/restore/${version}`, { method: 'POST' });
}

export async function previewTemplate(id: string): Promise<string> {
  return apiFetch<string>(`/documents/templates/${id}/preview`);
}

export async function generateDocument(data: GenerateRequest): Promise<DocumentResponse> {
  return apiFetch<DocumentResponse>('/documents/generate', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function generatePdf(data: GenerateRequest): Promise<DocumentResponse> {
  return apiFetch<DocumentResponse>('/documents/generate/pdf', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function listDocuments(): Promise<DocumentResponse[]> {
  return apiFetch<DocumentResponse[]>('/documents/list');
}

export async function getDocument(id: string): Promise<DocumentResponse> {
  return apiFetch<DocumentResponse>(`/documents/${id}`);
}

export async function applyAiOperations(templateId: string, prompt: string): Promise<AIApplyResponse> {
  return apiFetch<AIApplyResponse>('/documents/ai/apply', {
    method: 'POST',
    body: JSON.stringify({ templateId, prompt }),
  });
}
