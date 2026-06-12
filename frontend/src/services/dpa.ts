import { apiFetch } from './api';

export interface DpaStatus {
  status: 'NOT_SENT' | 'PENDING' | 'SIGNED' | 'EXPIRED';
  signerName?: string;
  signerPosition?: string;
  signerEmail?: string;
  signedAt?: string;
  documentVersion?: string;
  token?: string;
}

export interface DpaDocument {
  token: string;
  documentVersion: string;
  signed: boolean;
  signedAt?: string;
  tenantName: string;
  tenantEmail: string;
  signerName?: string;
  signerPosition?: string;
}

export const getDpaStatus = (tenantId: string) =>
  apiFetch<DpaStatus>(`/legal/tenants/${tenantId}/dpa`);

export const sendDpaRequest = (tenantId: string) =>
  apiFetch<DpaStatus>(`/legal/tenants/${tenantId}/dpa/send`, { method: 'POST' });

export const getDpaDocument = (token: string) =>
  fetch(`${process.env.NEXT_PUBLIC_API_URL ?? ''}/api/v1/public/dpa/${token}`)
    .then(r => { if (!r.ok) throw new Error('Document no trobat'); return r.json() as Promise<DpaDocument>; });

export const signDpa = (token: string, signerName: string, signerPosition: string) =>
  fetch(`${process.env.NEXT_PUBLIC_API_URL ?? ''}/api/v1/public/dpa/${token}/sign`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ signerName, signerPosition }),
  }).then(async r => {
    const text = await r.text();
    if (!r.ok) throw new Error(text);
    return text;
  });
