import { apiFetch } from './api';

export type DomainScenario =
  | 'OPENPROVIDER_NEW'
  | 'OPENPROVIDER_TRANSFER'
  | 'EXTERNAL_OWN'
  | 'EXTERNAL_SUBDOMAIN'
  | 'AMG_SUBDOMAIN';

export interface ManagedDomainResponse {
  id: string;
  tenantId: string;
  domainName: string;
  tld: string;
  status: string;
  scenario: DomainScenario;
  purchasePrice: number | null;
  salePrice: number | null;
  registeredAt: string | null;
  expiresAt: string | null;
  autoRenew: boolean;
  dnsConfigured: boolean;
  dnsVerified: boolean;
  dnsVerifiedAt: string | null;
  provider: string;
  dnsRecords: DnsRecordResponse[];
}

export interface DnsVerifyResponse {
  verified: boolean;
  resolvedIp: string | null;
  expectedIp: string;
  checkedAt: string;
}

export interface DnsRecordResponse {
  id: string;
  type: string;
  name: string;
  value: string;
  ttl: number;
  priority: number | null;
}

export interface DomainCheckResponse {
  domainName: string;
  available: boolean;
  saleRegister: number;
  saleRenew: number;
  alternatives: string[];
}

export interface TldPricingResponse {
  tld: string;
  costRegister: number;
  costRenew: number;
  saleRegister: number;
  saleRenew: number;
  isActive: boolean;
}

export interface RegisterDomainRequest {
  tenantId: string;
  domainName: string;
  landingId?: string;
  autoRenew: boolean;
  registrantName?: string;
  registrantEmail?: string;
  registrantPhone?: string;
  registrantNif?: string;
}

export interface DnsRecordRequest {
  type: string;
  name: string;
  value: string;
  ttl: number;
  priority?: number;
}

export const checkDomainAvailability = (domainName: string) =>
  apiFetch<DomainCheckResponse>(`/domains/check`, {
    method: 'POST',
    body: JSON.stringify({ domainName }),
  });

export const listAllDomains = (page = 0, size = 20) =>
  apiFetch<{ content: ManagedDomainResponse[]; totalElements: number; totalPages: number }>(
    `/domains?page=${page}&size=${size}`
  );

export const listDomainsByTenant = (tenantId: string) =>
  apiFetch<ManagedDomainResponse[]>(`/domains/tenant/${tenantId}`);

export const getDomain = (id: string) =>
  apiFetch<ManagedDomainResponse>(`/domains/${id}`);

export const registerDomain = (data: RegisterDomainRequest) =>
  apiFetch<ManagedDomainResponse>('/domains/register', {
    method: 'POST',
    body: JSON.stringify(data),
  });

export const renewDomain = (id: string) =>
  apiFetch<ManagedDomainResponse>(`/domains/${id}/renew`, { method: 'POST' });

export const configureDns = (id: string) =>
  apiFetch<ManagedDomainResponse>(`/domains/${id}/configure-dns`, { method: 'POST' });

export const updateDnsRecords = (id: string, records: DnsRecordRequest[]) =>
  apiFetch<ManagedDomainResponse>(`/domains/${id}/dns`, {
    method: 'PUT',
    body: JSON.stringify(records),
  });

export const cancelDomain = (id: string) =>
  apiFetch<void>(`/domains/${id}`, { method: 'DELETE' });

export const listTldPricing = () =>
  apiFetch<TldPricingResponse[]>('/domains/admin/tld-pricing');

export const listExpiringDomains = (days = 30) =>
  apiFetch<ManagedDomainResponse[]>(`/domains/admin/expiring?days=${days}`);

export const listMyDomains = () =>
  apiFetch<ManagedDomainResponse[]>('/domains/my');

export const addExternalDomain = (data: {
  tenantId: string;
  domainName: string;
  landingId?: string;
  scenario: 'EXTERNAL_OWN' | 'EXTERNAL_SUBDOMAIN';
}) =>
  apiFetch<ManagedDomainResponse>('/domains/external', {
    method: 'POST',
    body: JSON.stringify(data),
  });

export const assignAmgSubdomain = (data: {
  tenantId: string;
  subdomain: string;
  landingId?: string;
}) =>
  apiFetch<ManagedDomainResponse>('/domains/amg-subdomain', {
    method: 'POST',
    body: JSON.stringify(data),
  });

export const transferDomain = (data: {
  tenantId: string;
  domainName: string;
  eppCode: string;
  landingId?: string;
  autoRenew: boolean;
  registrantName?: string;
  registrantEmail?: string;
  registrantPhone?: string;
  registrantNif?: string;
}) =>
  apiFetch<ManagedDomainResponse>('/domains/transfer', {
    method: 'POST',
    body: JSON.stringify(data),
  });

export const verifyDns = (id: string) =>
  apiFetch<DnsVerifyResponse>(`/domains/${id}/verify-dns`);

export const SCENARIO_LABELS: Record<string, string> = {
  OPENPROVIDER_NEW:      'Registre OpenProvider',
  OPENPROVIDER_TRANSFER: 'Transferència OpenProvider',
  EXTERNAL_OWN:          'Domini propi extern',
  EXTERNAL_SUBDOMAIN:    'Subdomini propi',
  AMG_SUBDOMAIN:         'Subdomini AMG',
};

export const SCENARIO_TONE: Record<string, string> = {
  OPENPROVIDER_NEW:      'success',
  OPENPROVIDER_TRANSFER: 'info',
  EXTERNAL_OWN:          'neutral',
  EXTERNAL_SUBDOMAIN:    'neutral',
  AMG_SUBDOMAIN:         'warning',
};

export const STATUS_LABELS: Record<string, string> = {
  PENDING_PURCHASE: 'Pendent de pagament',
  REGISTERING: 'Registrant...',
  ACTIVE: 'Actiu',
  DNS_PENDING: 'DNS pendent',
  TRANSFER_IN: 'Transferència entrant',
  TRANSFER_OUT: 'Transferència sortint',
  EXPIRING_SOON: 'Expira aviat',
  EXPIRED: 'Expirat',
  CANCELLED: 'Cancel·lat',
};

export const STATUS_TONE: Record<string, string> = {
  ACTIVE: 'success',
  DNS_PENDING: 'warning',
  REGISTERING: 'warning',
  PENDING_PURCHASE: 'neutral',
  EXPIRING_SOON: 'error',
  EXPIRED: 'error',
  CANCELLED: 'neutral',
  TRANSFER_IN: 'info',
  TRANSFER_OUT: 'info',
};
