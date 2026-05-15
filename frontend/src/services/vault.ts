import { apiFetch } from './api';

export function setCredential(tenantId: string, serviceId: string, fieldId: string, value: string) {
  return apiFetch<any>(
    `/vault/tenants/${tenantId}/services/${serviceId}/fields/${fieldId}`,
    { method: 'PUT', body: JSON.stringify({ value }) }
  );
}

export function verifyService(tenantId: string, serviceId: string) {
  return apiFetch<{ verified: boolean; message: string }>(
    `/vault/tenants/${tenantId}/services/${serviceId}/verify`,
    { method: 'POST' }
  );
}

export function getTenantSetup(tenantId: string) {
  return apiFetch<any>(`/vault/tenants/${tenantId}/setup`);
}
