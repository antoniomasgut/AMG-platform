import { apiFetch } from './api';

export interface WabaConfig {
  id: string;
  tenantId: string;
  wabaId: string;
  phoneNumberId: string;
  displayPhoneNumber: string;
  businessName: string | null;
  status: 'CONNECTED' | 'DISCONNECTED' | 'ERROR';
  webhookRegistered: boolean;
  connectedAt: string | null;
}

export interface WabaChoice {
  wabaId: string;
  businessName: string;
  phones: PhoneChoice[];
}

export interface PhoneChoice {
  phoneNumberId: string;
  displayPhone: string;
  verifiedName: string;
}

export interface OAuthSession {
  sessionToken: string;
  wabas: WabaChoice[];
}

export const getWhatsAppConfig = (tenantId: string) =>
  apiFetch<WabaConfig>(`/whatsapp/tenants/${tenantId}/config`);

export const getConnectUrl = (tenantId: string) =>
  apiFetch<{ oauthUrl: string }>(`/meta/whatsapp/connect?tenantId=${tenantId}`);

export const getOAuthSession = (sessionToken: string) =>
  apiFetch<OAuthSession>(`/meta/whatsapp/oauth-session/${sessionToken}`, { skipAuth: true });

export const confirmConnection = (sessionToken: string, wabaId: string, phoneNumberId: string) =>
  apiFetch<void>('/meta/whatsapp/confirm', {
    method: 'POST',
    body: JSON.stringify({ sessionToken, wabaId, phoneNumberId }),
  });

export const disconnectWhatsApp = (tenantId: string) =>
  apiFetch<void>(`/meta/whatsapp/disconnect/${tenantId}`, { method: 'DELETE' });
