import { apiFetch } from './api';

export interface CommTemplate {
  id: string;
  sector: string;
  channel: 'EMAIL' | 'WHATSAPP';
  action: string;
  language: string;
  subject: string | null;
  body: string;
  isActive: boolean;
  sortOrder: number;
}

export interface CommSendRequest {
  channel: 'EMAIL' | 'WHATSAPP';
  to: string;
  action: string;
  sector?: string;
  language?: string;
  variables: Record<string, string>;
}

export const LANGUAGES: { value: string; label: string }[] = [
  { value: 'ca', label: 'Català' },
  { value: 'es', label: 'Castellà' },
  { value: 'en', label: 'English' },
  { value: 'de', label: 'Deutsch' },
];

export const ACTIONS: { value: string; label: string; variables: string[] }[] = [
  {
    value: 'DEMO_SEND',
    label: 'Enviar demo',
    variables: ['NOM_NEGOCI', 'SECTOR', 'URL_DEMO', 'HORES_VALIDA'],
  },
  {
    value: 'BUDGET_ACCEPTED',
    label: 'Pressupost acceptat',
    variables: ['NOM_NEGOCI', 'NOM_CLIENT', 'NUM_PRESSUPOST', 'IMPORT', 'DATA_ACCEPTACIO'],
  },
  {
    value: 'APPOINTMENT_CONFIRM',
    label: 'Cita confirmada',
    variables: ['NOM_NEGOCI', 'NOM_CLIENT', 'DATA_CITA', 'HORA_CITA', 'SERVEI'],
  },
  {
    value: 'APPOINTMENT_REMINDER',
    label: 'Recordatori cita',
    variables: ['NOM_NEGOCI', 'NOM_CLIENT', 'DATA_CITA', 'HORA_CITA', 'SERVEI'],
  },
  {
    value: 'APPOINTMENT_CANCEL',
    label: 'Cita cancel·lada',
    variables: ['NOM_NEGOCI', 'NOM_CLIENT', 'DATA_CITA', 'HORA_CITA', 'MOTIU'],
  },
  {
    value: 'DOCUMENT_STANDARD_DELIVERY',
    label: 'Lliurament document estàndard',
    variables: ['RECIPIENT_NAME', 'DOCUMENT_NAME', 'VIEW_URL', 'EXPIRES_AT', 'DOCUMENT_NUMBER'],
  },
  {
    value: 'DOCUMENT_SENSITIVE_DELIVERY',
    label: 'Lliurament document sensible',
    variables: ['RECIPIENT_NAME', 'DOCUMENT_NAME', 'VIEW_URL', 'EXPIRES_AT', 'DOCUMENT_NUMBER'],
  },
];

export const listCommTemplates = () =>
  apiFetch<CommTemplate[]>('/admin/comm/templates');

export const createCommTemplate = (body: Partial<CommTemplate>) =>
  apiFetch<CommTemplate>('/admin/comm/templates', {
    method: 'POST',
    body: JSON.stringify(body),
  });

export const updateCommTemplate = (id: string, body: Partial<CommTemplate>) =>
  apiFetch<CommTemplate>(`/admin/comm/templates/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  });

export const deleteCommTemplate = (id: string) =>
  apiFetch<void>(`/admin/comm/templates/${id}`, { method: 'DELETE' });

export const sendComm = (req: CommSendRequest) =>
  apiFetch<{ sent: boolean; renderedBody: string }>('/admin/comm/send', {
    method: 'POST',
    body: JSON.stringify(req),
  });
