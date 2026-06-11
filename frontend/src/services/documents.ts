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
    text?: string;
    html?: string;
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

export function amgQuoteLayout(): LayoutBlock[] {
  return [
    {
      id: 'document_title', type: 'document_title',
      x: 0, y: 0, w: 7, h: 2, visible: true,
      config: { style: { fontSize: 28, fontWeight: 'bold' }, text: 'PRESSUPOST' },
      dataBinding: null,
    },
    {
      id: 'company_info', type: 'company_info',
      x: 7, y: 0, w: 5, h: 3, visible: true,
      config: { style: { alignment: 'right', fontSize: 11 } },
      dataBinding: { source: 'company', field: 'name' },
    },
    {
      id: 'sep1', type: 'separator',
      x: 0, y: 3, w: 12, h: 1, visible: true,
      config: { style: {} }, dataBinding: null,
    },
    {
      id: 'document_number', type: 'document_number',
      x: 0, y: 4, w: 4, h: 1, visible: true,
      config: { style: { fontSize: 12 } },
      dataBinding: { source: 'document', field: 'number' },
    },
    {
      id: 'document_date', type: 'document_date',
      x: 4, y: 4, w: 4, h: 1, visible: true,
      config: { style: { fontSize: 12 } },
      dataBinding: { source: 'document', field: 'date' },
    },
    {
      id: 'document_validity', type: 'document_validity',
      x: 8, y: 4, w: 4, h: 1, visible: true,
      config: { style: { alignment: 'right', fontSize: 12 } },
      dataBinding: { source: 'document', field: 'validUntil' },
    },
    {
      id: 'customer_info', type: 'customer_info',
      x: 0, y: 5, w: 6, h: 3, visible: true,
      config: { style: { fontSize: 12 } },
      dataBinding: { source: 'customer', field: 'name' },
    },
    {
      id: 'intro_text', type: 'rich_text',
      x: 6, y: 5, w: 6, h: 3, visible: true,
      config: {
        style: { fontSize: 11 },
        html: '<p style="text-align:right;color:#888;font-size:11px;">Benvolgut/da client,</p><p style="text-align:right;color:#888;font-size:11px;">A continuació trobareu el detall dels serveis de digitalització sol·licitats. No dubteu a contactar-nos per a qualsevol consulta.</p>',
      },
      dataBinding: null,
    },
    {
      id: 'sep2', type: 'separator',
      x: 0, y: 8, w: 12, h: 1, visible: true,
      config: { style: {} }, dataBinding: null,
    },
    {
      id: 'services_label', type: 'rich_text',
      x: 0, y: 9, w: 12, h: 1, visible: true,
      config: {
        style: {},
        html: '<p style="font-size:10px;font-weight:600;text-transform:uppercase;letter-spacing:1px;color:#888;margin:4px 0;">Detall de serveis</p>',
      },
      dataBinding: null,
    },
    {
      id: 'services_table', type: 'services_table',
      x: 0, y: 10, w: 12, h: 5, visible: true,
      config: { style: {} }, dataBinding: null,
    },
    {
      id: 'sep3', type: 'separator',
      x: 0, y: 15, w: 12, h: 1, visible: true,
      config: { style: {} }, dataBinding: null,
    },
    {
      id: 'subtotal', type: 'subtotal',
      x: 6, y: 16, w: 6, h: 1, visible: true,
      config: { style: { alignment: 'right', fontSize: 13 } },
      dataBinding: { source: 'calculated', field: 'subtotal' },
    },
    {
      id: 'tax', type: 'tax',
      x: 6, y: 17, w: 6, h: 1, visible: true,
      config: { style: { alignment: 'right', fontSize: 13 } },
      dataBinding: { source: 'calculated', field: 'tax' },
    },
    {
      id: 'total', type: 'total',
      x: 6, y: 18, w: 6, h: 1, visible: true,
      config: { style: { alignment: 'right', fontSize: 16, fontWeight: 'bold' } },
      dataBinding: { source: 'calculated', field: 'total' },
    },
    {
      id: 'sep4', type: 'separator',
      x: 0, y: 19, w: 12, h: 1, visible: true,
      config: { style: {} }, dataBinding: null,
    },
    {
      id: 'payment_text', type: 'rich_text',
      x: 0, y: 20, w: 12, h: 2, visible: true,
      config: {
        style: { fontSize: 11 },
        html: '<p><strong>Condicions de pagament:</strong> 50% a la signatura del pressupost · 50% a l\'entrega del projecte. Transferència bancària o targeta de crèdit.</p><p><strong>Termini d\'execució:</strong> S\'especificarà per escrit a la confirmació de la comanda.</p>',
      },
      dataBinding: null,
    },
    {
      id: 'terms', type: 'terms',
      x: 0, y: 22, w: 12, h: 2, visible: true,
      config: {
        style: { fontSize: 10 },
        text: 'Els preus no inclouen IVA (21%). Pressupost vàlid 30 dies. Qualsevol modificació de l\'abast acordada durant el projecte es pressupostarà de forma addicional. La signatura implica l\'acceptació de les Condicions Generals disponibles a amgdigitalitzacio.com/condicions.',
      },
      dataBinding: null,
    },
    {
      id: 'sig_client', type: 'rich_text',
      x: 0, y: 24, w: 5, h: 2, visible: true,
      config: {
        style: {},
        html: '<div style="margin-top:24px;"><p>_________________________</p><p style="font-size:11px;color:#888;">Signatura del client · Data</p></div>',
      },
      dataBinding: null,
    },
    {
      id: 'sig_amg', type: 'rich_text',
      x: 7, y: 24, w: 5, h: 2, visible: true,
      config: {
        style: {},
        html: '<div style="margin-top:24px;text-align:right;"><p>_________________________</p><p style="font-size:11px;color:#888;">AMG Digitalització</p></div>',
      },
      dataBinding: null,
    },
  ];
}

export function amgInvoiceLayout(): LayoutBlock[] {
  return [
    {
      id: 'document_title', type: 'document_title',
      x: 0, y: 0, w: 7, h: 2, visible: true,
      config: { style: { fontSize: 28, fontWeight: 'bold' }, text: 'FACTURA' },
      dataBinding: null,
    },
    {
      id: 'company_info', type: 'company_info',
      x: 7, y: 0, w: 5, h: 3, visible: true,
      config: { style: { alignment: 'right', fontSize: 11 } },
      dataBinding: { source: 'company', field: 'name' },
    },
    { id: 'sep1', type: 'separator', x: 0, y: 3, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    {
      id: 'document_number', type: 'document_number',
      x: 0, y: 4, w: 3, h: 1, visible: true,
      config: { style: { fontSize: 12 } },
      dataBinding: { source: 'document', field: 'number' },
    },
    {
      id: 'document_date', type: 'document_date',
      x: 3, y: 4, w: 3, h: 1, visible: true,
      config: { style: { fontSize: 12 } },
      dataBinding: { source: 'document', field: 'date' },
    },
    {
      id: 'due_date', type: 'document_validity',
      x: 6, y: 4, w: 3, h: 1, visible: true,
      config: { style: { fontSize: 12 } },
      dataBinding: { source: 'document', field: 'validUntil' },
    },
    {
      id: 'payment_method', type: 'rich_text',
      x: 9, y: 4, w: 3, h: 1, visible: true,
      config: { style: { alignment: 'right' }, html: '<p style="font-size:11px;color:#555;text-align:right;"><strong>Forma de pagament:</strong> Transferència bancària</p>' },
      dataBinding: null,
    },
    {
      id: 'customer_info', type: 'customer_info',
      x: 0, y: 5, w: 6, h: 3, visible: true,
      config: { style: { fontSize: 12 } },
      dataBinding: { source: 'customer', field: 'name' },
    },
    {
      id: 'bank_info', type: 'rich_text',
      x: 6, y: 5, w: 6, h: 3, visible: true,
      config: {
        style: { alignment: 'right' },
        html: '<p style="text-align:right;font-size:11px;color:#555;"><strong>Compte bancari:</strong></p><p style="text-align:right;font-size:11px;color:#555;">IBAN: ES00 0000 0000 0000 0000 0000</p><p style="text-align:right;font-size:11px;color:#555;">BIC/SWIFT: XXXXXXXX</p>',
      },
      dataBinding: null,
    },
    { id: 'sep2', type: 'separator', x: 0, y: 8, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    {
      id: 'services_label', type: 'rich_text',
      x: 0, y: 9, w: 12, h: 1, visible: true,
      config: { style: {}, html: '<p style="font-size:10px;font-weight:600;text-transform:uppercase;letter-spacing:1px;color:#888;margin:4px 0;">Detall de serveis facturats</p>' },
      dataBinding: null,
    },
    { id: 'services_table', type: 'services_table', x: 0, y: 10, w: 12, h: 5, visible: true, config: { style: {} }, dataBinding: null },
    { id: 'sep3', type: 'separator', x: 0, y: 15, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    { id: 'subtotal', type: 'subtotal', x: 6, y: 16, w: 6, h: 1, visible: true, config: { style: { alignment: 'right', fontSize: 13 } }, dataBinding: { source: 'calculated', field: 'subtotal' } },
    { id: 'tax', type: 'tax', x: 6, y: 17, w: 6, h: 1, visible: true, config: { style: { alignment: 'right', fontSize: 13 } }, dataBinding: { source: 'calculated', field: 'tax' } },
    { id: 'total', type: 'total', x: 6, y: 18, w: 6, h: 1, visible: true, config: { style: { alignment: 'right', fontSize: 16, fontWeight: 'bold' } }, dataBinding: { source: 'calculated', field: 'total' } },
    { id: 'sep4', type: 'separator', x: 0, y: 19, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    {
      id: 'terms', type: 'terms',
      x: 0, y: 20, w: 12, h: 2, visible: true,
      config: {
        style: { fontSize: 10 },
        text: 'Factura emesa d\'acord amb la Llei 37/1992 de l\'IVA. Els preus inclouen l\'IVA indicat. En cas de retard en el pagament s\'aplicaran interessos de demora. Reclamacions en un termini de 15 dies des de la recepció.',
      },
      dataBinding: null,
    },
    {
      id: 'sig_client', type: 'rich_text',
      x: 0, y: 22, w: 5, h: 2, visible: true,
      config: { style: {}, html: '<div style="margin-top:24px;"><p>_________________________</p><p style="font-size:11px;color:#888;">Rebut conforme · Data</p></div>' },
      dataBinding: null,
    },
    {
      id: 'sig_amg', type: 'rich_text',
      x: 7, y: 22, w: 5, h: 2, visible: true,
      config: { style: {}, html: '<div style="margin-top:24px;text-align:right;"><p>_________________________</p><p style="font-size:11px;color:#888;">AMG Digitalització</p></div>' },
      dataBinding: null,
    },
  ];
}

export function amgDeliveryNoteLayout(): LayoutBlock[] {
  return [
    {
      id: 'document_title', type: 'document_title',
      x: 0, y: 0, w: 7, h: 2, visible: true,
      config: { style: { fontSize: 28, fontWeight: 'bold' }, text: 'ALBARÀ' },
      dataBinding: null,
    },
    {
      id: 'company_info', type: 'company_info',
      x: 7, y: 0, w: 5, h: 3, visible: true,
      config: { style: { alignment: 'right', fontSize: 11 } },
      dataBinding: { source: 'company', field: 'name' },
    },
    { id: 'sep1', type: 'separator', x: 0, y: 3, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    {
      id: 'document_number', type: 'document_number',
      x: 0, y: 4, w: 4, h: 1, visible: true,
      config: { style: { fontSize: 12 } },
      dataBinding: { source: 'document', field: 'number' },
    },
    {
      id: 'document_date', type: 'document_date',
      x: 4, y: 4, w: 4, h: 1, visible: true,
      config: { style: { fontSize: 12 } },
      dataBinding: { source: 'document', field: 'date' },
    },
    {
      id: 'customer_info', type: 'customer_info',
      x: 0, y: 5, w: 6, h: 3, visible: true,
      config: { style: { fontSize: 12 } },
      dataBinding: { source: 'customer', field: 'name' },
    },
    {
      id: 'delivery_address', type: 'rich_text',
      x: 6, y: 5, w: 6, h: 3, visible: true,
      config: {
        style: { alignment: 'right' },
        html: '<p style="text-align:right;font-size:11px;"><strong>Adreça de lliurament:</strong></p><p style="text-align:right;font-size:11px;color:#555;">(mateixa adreça del client)</p>',
      },
      dataBinding: null,
    },
    { id: 'sep2', type: 'separator', x: 0, y: 8, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    {
      id: 'items_label', type: 'rich_text',
      x: 0, y: 9, w: 12, h: 1, visible: true,
      config: { style: {}, html: '<p style="font-size:10px;font-weight:600;text-transform:uppercase;letter-spacing:1px;color:#888;margin:4px 0;">Detall de lliurament</p>' },
      dataBinding: null,
    },
    { id: 'services_table', type: 'services_table', x: 0, y: 10, w: 12, h: 6, visible: true, config: { style: {} }, dataBinding: null },
    { id: 'sep3', type: 'separator', x: 0, y: 16, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    {
      id: 'note', type: 'rich_text',
      x: 0, y: 17, w: 12, h: 2, visible: true,
      config: {
        style: {},
        html: '<p style="font-size:11px;color:#555;"><strong>Nota:</strong> La signatura d\'aquest albarà implica la conformitat amb els articles lliurats. Qualsevol discrepància ha de comunicar-se en un termini de 48 hores.</p>',
      },
      dataBinding: null,
    },
    {
      id: 'sig_client', type: 'rich_text',
      x: 0, y: 19, w: 5, h: 2, visible: true,
      config: { style: {}, html: '<div style="margin-top:24px;"><p>_________________________</p><p style="font-size:11px;color:#888;">Signatura del receptor · Data</p></div>' },
      dataBinding: null,
    },
    {
      id: 'sig_amg', type: 'rich_text',
      x: 7, y: 19, w: 5, h: 2, visible: true,
      config: { style: {}, html: '<div style="margin-top:24px;text-align:right;"><p>_________________________</p><p style="font-size:11px;color:#888;">AMG Digitalització · Transportista</p></div>' },
      dataBinding: null,
    },
  ];
}

export function amgContractLayout(): LayoutBlock[] {
  return [
    {
      id: 'document_title', type: 'document_title',
      x: 0, y: 0, w: 12, h: 2, visible: true,
      config: { style: { fontSize: 22, fontWeight: 'bold', alignment: 'center' }, text: 'CONTRACTE DE SERVEIS DE DIGITALITZACIÓ' },
      dataBinding: null,
    },
    {
      id: 'contract_date', type: 'rich_text',
      x: 0, y: 2, w: 12, h: 1, visible: true,
      config: { style: { alignment: 'center' }, html: '<p style="text-align:center;font-size:11px;color:#555;">Palma de Mallorca, a __ de ________ de 2026</p>' },
      dataBinding: null,
    },
    { id: 'sep1', type: 'separator', x: 0, y: 3, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    {
      id: 'parties_label', type: 'rich_text',
      x: 0, y: 4, w: 12, h: 1, visible: true,
      config: { style: {}, html: '<p style="font-size:10px;font-weight:600;text-transform:uppercase;letter-spacing:1px;color:#888;">Les parts</p>' },
      dataBinding: null,
    },
    {
      id: 'company_info', type: 'company_info',
      x: 0, y: 5, w: 5, h: 3, visible: true,
      config: { style: { fontSize: 11 } },
      dataBinding: { source: 'company', field: 'name' },
    },
    {
      id: 'party_label', type: 'rich_text',
      x: 5, y: 5, w: 2, h: 3, visible: true,
      config: { style: { alignment: 'center' }, html: '<p style="text-align:center;font-size:11px;color:#aaa;padding-top:12px;">(Prestador)</p>' },
      dataBinding: null,
    },
    {
      id: 'customer_info', type: 'customer_info',
      x: 7, y: 5, w: 5, h: 3, visible: true,
      config: { style: { alignment: 'right', fontSize: 11 } },
      dataBinding: { source: 'customer', field: 'name' },
    },
    { id: 'sep2', type: 'separator', x: 0, y: 8, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    {
      id: 'object_label', type: 'rich_text',
      x: 0, y: 9, w: 12, h: 1, visible: true,
      config: { style: {}, html: '<p style="font-size:10px;font-weight:600;text-transform:uppercase;letter-spacing:1px;color:#888;">1. Objecte del contracte</p>' },
      dataBinding: null,
    },
    {
      id: 'object_text', type: 'rich_text',
      x: 0, y: 10, w: 12, h: 3, visible: true,
      config: {
        style: {},
        html: '<p style="font-size:11px;color:#333;">El prestador es compromet a prestar els serveis de digitalització descrits a l\'annex de serveis, d\'acord amb les condicions establertes en el present contracte.</p>',
      },
      dataBinding: null,
    },
    { id: 'sep3', type: 'separator', x: 0, y: 13, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    {
      id: 'services_label', type: 'rich_text',
      x: 0, y: 14, w: 12, h: 1, visible: true,
      config: { style: {}, html: '<p style="font-size:10px;font-weight:600;text-transform:uppercase;letter-spacing:1px;color:#888;">2. Serveis contractats</p>' },
      dataBinding: null,
    },
    { id: 'services_table', type: 'services_table', x: 0, y: 15, w: 12, h: 4, visible: true, config: { style: {} }, dataBinding: null },
    { id: 'sep4', type: 'separator', x: 0, y: 19, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    {
      id: 'economic_label', type: 'rich_text',
      x: 0, y: 20, w: 12, h: 1, visible: true,
      config: { style: {}, html: '<p style="font-size:10px;font-weight:600;text-transform:uppercase;letter-spacing:1px;color:#888;">3. Condicions econòmiques</p>' },
      dataBinding: null,
    },
    { id: 'subtotal', type: 'subtotal', x: 6, y: 21, w: 6, h: 1, visible: true, config: { style: { alignment: 'right' } }, dataBinding: { source: 'calculated', field: 'subtotal' } },
    { id: 'tax', type: 'tax', x: 6, y: 22, w: 6, h: 1, visible: true, config: { style: { alignment: 'right' } }, dataBinding: { source: 'calculated', field: 'tax' } },
    { id: 'total', type: 'total', x: 6, y: 23, w: 6, h: 1, visible: true, config: { style: { alignment: 'right', fontSize: 14, fontWeight: 'bold' } }, dataBinding: { source: 'calculated', field: 'total' } },
    {
      id: 'payment_cond', type: 'rich_text',
      x: 0, y: 21, w: 6, h: 3, visible: true,
      config: {
        style: {},
        html: '<p style="font-size:11px;"><strong>Forma de pagament:</strong> 50% inici · 50% lliurament.</p><p style="font-size:11px;"><strong>Durada:</strong> Fins a la finalització del projecte.</p>',
      },
      dataBinding: null,
    },
    { id: 'sep5', type: 'separator', x: 0, y: 24, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    {
      id: 'terms', type: 'terms',
      x: 0, y: 25, w: 12, h: 2, visible: true,
      config: {
        style: { fontSize: 10 },
        text: '4. Condicions generals — El prestador no serà responsable de retards causats per força major. La propietat intel·lectual dels lliurables es transfereix al client en el moment del pagament íntegre. Llei aplicable: dret espanyol. Jurisdicció: Jutjats de Palma de Mallorca.',
      },
      dataBinding: null,
    },
    {
      id: 'sig_client', type: 'rich_text',
      x: 0, y: 27, w: 5, h: 2, visible: true,
      config: { style: {}, html: '<div style="margin-top:24px;"><p>_________________________</p><p style="font-size:11px;color:#888;">Client · NIF · Data</p></div>' },
      dataBinding: null,
    },
    {
      id: 'sig_amg', type: 'rich_text',
      x: 7, y: 27, w: 5, h: 2, visible: true,
      config: { style: {}, html: '<div style="margin-top:24px;text-align:right;"><p>_________________________</p><p style="font-size:11px;color:#888;">AMG Digitalització · Data</p></div>' },
      dataBinding: null,
    },
  ];
}

export function amgReportLayout(): LayoutBlock[] {
  return [
    {
      id: 'company_info', type: 'company_info',
      x: 7, y: 0, w: 5, h: 3, visible: true,
      config: { style: { alignment: 'right', fontSize: 11 } },
      dataBinding: { source: 'company', field: 'name' },
    },
    {
      id: 'document_title', type: 'document_title',
      x: 0, y: 0, w: 7, h: 2, visible: true,
      config: { style: { fontSize: 26, fontWeight: 'bold' }, text: 'INFORME' },
      dataBinding: null,
    },
    { id: 'sep1', type: 'separator', x: 0, y: 3, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    {
      id: 'document_number', type: 'document_number',
      x: 0, y: 4, w: 4, h: 1, visible: true,
      config: { style: { fontSize: 11 } },
      dataBinding: { source: 'document', field: 'number' },
    },
    {
      id: 'document_date', type: 'document_date',
      x: 4, y: 4, w: 4, h: 1, visible: true,
      config: { style: { fontSize: 11 } },
      dataBinding: { source: 'document', field: 'date' },
    },
    {
      id: 'customer_info', type: 'customer_info',
      x: 8, y: 4, w: 4, h: 2, visible: true,
      config: { style: { alignment: 'right', fontSize: 11 } },
      dataBinding: { source: 'customer', field: 'name' },
    },
    { id: 'sep2', type: 'separator', x: 0, y: 6, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    {
      id: 'summary_label', type: 'rich_text',
      x: 0, y: 7, w: 12, h: 1, visible: true,
      config: { style: {}, html: '<p style="font-size:10px;font-weight:600;text-transform:uppercase;letter-spacing:1px;color:#888;">Resum executiu</p>' },
      dataBinding: null,
    },
    {
      id: 'summary_text', type: 'rich_text',
      x: 0, y: 8, w: 12, h: 4, visible: true,
      config: {
        style: {},
        html: '<p style="font-size:12px;color:#333;line-height:1.7;">Introduïu aquí el resum executiu de l\'informe. Descriviu els objectius, el context i els principals resultats obtinguts de forma breu i clara.</p>',
      },
      dataBinding: null,
    },
    { id: 'sep3', type: 'separator', x: 0, y: 12, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    {
      id: 'content_label', type: 'rich_text',
      x: 0, y: 13, w: 12, h: 1, visible: true,
      config: { style: {}, html: '<p style="font-size:10px;font-weight:600;text-transform:uppercase;letter-spacing:1px;color:#888;">Contingut de l\'informe</p>' },
      dataBinding: null,
    },
    {
      id: 'content_text', type: 'rich_text',
      x: 0, y: 14, w: 12, h: 6, visible: true,
      config: {
        style: {},
        html: '<p style="font-size:12px;color:#333;line-height:1.7;">Desenvolupeu aquí el contingut principal de l\'informe. Podeu afegir seccions, taules de dades, anàlisis i qualsevol informació rellevant per al client.</p>',
      },
      dataBinding: null,
    },
    { id: 'sep4', type: 'separator', x: 0, y: 20, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    {
      id: 'conclusions_label', type: 'rich_text',
      x: 0, y: 21, w: 12, h: 1, visible: true,
      config: { style: {}, html: '<p style="font-size:10px;font-weight:600;text-transform:uppercase;letter-spacing:1px;color:#888;">Conclusions i recomanacions</p>' },
      dataBinding: null,
    },
    {
      id: 'conclusions_text', type: 'rich_text',
      x: 0, y: 22, w: 12, h: 3, visible: true,
      config: {
        style: {},
        html: '<p style="font-size:12px;color:#333;line-height:1.7;">Introduïu aquí les principals conclusions de l\'anàlisi i les recomanacions per a l\'empresa client.</p>',
      },
      dataBinding: null,
    },
    { id: 'sep5', type: 'separator', x: 0, y: 25, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    {
      id: 'sig_amg', type: 'rich_text',
      x: 7, y: 26, w: 5, h: 2, visible: true,
      config: { style: {}, html: '<div style="margin-top:24px;text-align:right;"><p>_________________________</p><p style="font-size:11px;color:#888;">AMG Digitalització · Data</p></div>' },
      dataBinding: null,
    },
  ];
}

export function amgProposalLayout(): LayoutBlock[] {
  return [
    {
      id: 'document_title', type: 'document_title',
      x: 0, y: 0, w: 7, h: 2, visible: true,
      config: { style: { fontSize: 24, fontWeight: 'bold' }, text: 'PROPOSTA COMERCIAL' },
      dataBinding: null,
    },
    {
      id: 'company_info', type: 'company_info',
      x: 7, y: 0, w: 5, h: 3, visible: true,
      config: { style: { alignment: 'right', fontSize: 11 } },
      dataBinding: { source: 'company', field: 'name' },
    },
    { id: 'sep1', type: 'separator', x: 0, y: 3, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    {
      id: 'customer_info', type: 'customer_info',
      x: 0, y: 4, w: 6, h: 3, visible: true,
      config: { style: { fontSize: 12 } },
      dataBinding: { source: 'customer', field: 'name' },
    },
    {
      id: 'proposal_meta', type: 'rich_text',
      x: 6, y: 4, w: 6, h: 3, visible: true,
      config: {
        style: { alignment: 'right' },
        html: '<p style="text-align:right;font-size:11px;color:#555;"><strong>Data proposta:</strong> {{document.date}}</p><p style="text-align:right;font-size:11px;color:#555;"><strong>Referència:</strong> {{document.number}}</p><p style="text-align:right;font-size:11px;color:#555;"><strong>Validesa:</strong> 30 dies</p>',
      },
      dataBinding: null,
    },
    { id: 'sep2', type: 'separator', x: 0, y: 7, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    {
      id: 'intro_label', type: 'rich_text',
      x: 0, y: 8, w: 12, h: 1, visible: true,
      config: { style: {}, html: '<p style="font-size:10px;font-weight:600;text-transform:uppercase;letter-spacing:1px;color:#888;">Introducció</p>' },
      dataBinding: null,
    },
    {
      id: 'intro_text', type: 'rich_text',
      x: 0, y: 9, w: 12, h: 3, visible: true,
      config: {
        style: {},
        html: '<p style="font-size:12px;color:#333;line-height:1.7;">Estimat client, presentem a continuació la nostra proposta per impulsar la digitalització del vostre negoci. Hem analitzat les vostres necessitats i dissenyat una solució adaptada que us permetrà créixer, automatitzar i captar més clients.</p>',
      },
      dataBinding: null,
    },
    { id: 'sep3', type: 'separator', x: 0, y: 12, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    {
      id: 'services_label', type: 'rich_text',
      x: 0, y: 13, w: 12, h: 1, visible: true,
      config: { style: {}, html: '<p style="font-size:10px;font-weight:600;text-transform:uppercase;letter-spacing:1px;color:#888;">Solució proposada</p>' },
      dataBinding: null,
    },
    { id: 'services_table', type: 'services_table', x: 0, y: 14, w: 12, h: 5, visible: true, config: { style: {} }, dataBinding: null },
    { id: 'sep4', type: 'separator', x: 0, y: 19, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    { id: 'subtotal', type: 'subtotal', x: 6, y: 20, w: 6, h: 1, visible: true, config: { style: { alignment: 'right', fontSize: 13 } }, dataBinding: { source: 'calculated', field: 'subtotal' } },
    { id: 'tax', type: 'tax', x: 6, y: 21, w: 6, h: 1, visible: true, config: { style: { alignment: 'right', fontSize: 13 } }, dataBinding: { source: 'calculated', field: 'tax' } },
    { id: 'total', type: 'total', x: 6, y: 22, w: 6, h: 1, visible: true, config: { style: { alignment: 'right', fontSize: 16, fontWeight: 'bold' } }, dataBinding: { source: 'calculated', field: 'total' } },
    { id: 'sep5', type: 'separator', x: 0, y: 23, w: 12, h: 1, visible: true, config: { style: {} }, dataBinding: null },
    {
      id: 'next_steps_label', type: 'rich_text',
      x: 0, y: 24, w: 12, h: 1, visible: true,
      config: { style: {}, html: '<p style="font-size:10px;font-weight:600;text-transform:uppercase;letter-spacing:1px;color:#888;">Propers passos</p>' },
      dataBinding: null,
    },
    {
      id: 'next_steps', type: 'rich_text',
      x: 0, y: 25, w: 12, h: 2, visible: true,
      config: {
        style: {},
        html: '<p style="font-size:11px;color:#333;">1. Confirmació de la proposta · 2. Signatura del contracte · 3. Pagament del 50% inicial · 4. Inici del projecte en un termini de 5 dies hàbils.</p>',
      },
      dataBinding: null,
    },
    {
      id: 'terms', type: 'terms',
      x: 0, y: 27, w: 12, h: 2, visible: true,
      config: {
        style: { fontSize: 10 },
        text: 'Proposta vàlida 30 dies. Els preus no inclouen IVA (21%). L\'acceptació d\'aquesta proposta implica l\'acceptació de les Condicions Generals disponibles a amgdigitalitzacio.com/condicions.',
      },
      dataBinding: null,
    },
    {
      id: 'sig_client', type: 'rich_text',
      x: 0, y: 29, w: 5, h: 2, visible: true,
      config: { style: {}, html: '<div style="margin-top:24px;"><p>_________________________</p><p style="font-size:11px;color:#888;">Acceptació client · Data</p></div>' },
      dataBinding: null,
    },
    {
      id: 'sig_amg', type: 'rich_text',
      x: 7, y: 29, w: 5, h: 2, visible: true,
      config: { style: {}, html: '<div style="margin-top:24px;text-align:right;"><p>_________________________</p><p style="font-size:11px;color:#888;">AMG Digitalització</p></div>' },
      dataBinding: null,
    },
  ];
}

export async function listTemplates(tenantId?: string): Promise<TemplateResponse[]> {
  const qs = tenantId ? `?tenantId=${tenantId}` : '';
  return apiFetch<TemplateResponse[]>(`/documents/templates${qs}`);
}

export async function getTemplate(id: string): Promise<TemplateResponse> {
  return apiFetch<TemplateResponse>(`/documents/templates/${id}`);
}

export async function createTemplate(data: TemplateRequest, tenantId?: string): Promise<TemplateResponse> {
  const qs = tenantId ? `?tenantId=${tenantId}` : '';
  return apiFetch<TemplateResponse>(`/documents/templates${qs}`, {
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

export async function listDocuments(tenantId?: string): Promise<DocumentResponse[]> {
  const qs = tenantId ? `?tenantId=${tenantId}` : '';
  return apiFetch<DocumentResponse[]>(`/documents/list${qs}`);
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
