import { apiFetch } from './api';

export interface EarlyAdopterStatus {
  id: string;
  maxSlots: number;
  usedSlots: number;
  availableSlots: number;
  setupDiscountPct: number;
  monthlyDiscountPct: number;
  commitmentMonths: number;
  active: boolean;
  updatedAt: string;
}

export interface ReferralCode {
  id: string;
  ownerTenantId: string;
  code: string;
  usedByTenantId: string | null;
  usedAt: string | null;
  creditApplied: boolean;
  referrerCreditMonths: number;
  createdAt: string;
}

export const getEarlyAdopterStatus = () =>
  apiFetch<EarlyAdopterStatus>('/billing/programs/early-adopter');

export const updateEarlyAdopter = (data: Partial<EarlyAdopterStatus>) =>
  apiFetch<EarlyAdopterStatus>('/billing/programs/early-adopter', {
    method: 'PUT',
    body: JSON.stringify(data),
  });

export const applyEarlyAdopter = (tenantId: string) =>
  apiFetch<void>(`/billing/programs/early-adopter/apply/${tenantId}`, {
    method: 'POST',
  });

export const applyAnnualContract = (tenantId: string) =>
  apiFetch<void>(`/billing/programs/annual-contract/apply/${tenantId}`, {
    method: 'POST',
  });

export const getAllReferrals = () =>
  apiFetch<ReferralCode[]>('/billing/programs/referrals');

export const getMyReferralCode = () =>
  apiFetch<ReferralCode | null>('/billing/programs/referrals/my');

export const applyReferralCode = (code: string, newTenantId: string) =>
  apiFetch<ReferralCode>('/billing/programs/referrals/apply', {
    method: 'POST',
    body: JSON.stringify({ code, newTenantId }),
  });
