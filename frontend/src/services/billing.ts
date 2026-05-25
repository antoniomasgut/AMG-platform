import { apiFetch } from './api';

export interface BudgetLine { serviceName: string; setupPrice: number; monthlyPrice: number; }
export interface BudgetPhase { name: string; sortOrder: number; lines: BudgetLine[]; phaseTotal: number; phaseMonthlyTotal: number; phaseId: string; }
export interface BudgetAddon { serviceName: string; unitPrice: number; }
export interface BudgetResponse {
  id: string; budgetNumber: string; status: string;
  phases: BudgetPhase[]; addons: BudgetAddon[];
  subtotal: number; discountTotal: number; total: number; monthlyTotal: number;
  sentAt: string | null; acceptedAt: string | null; rejectedAt: string | null;
  rejectionUrl: string | null; acceptanceUrl: string | null; validUntil: string; createdAt: string;
  profileId: string | null; phaseIds: string[]; notes: string | null; clientNotes: string | null;
  tenantId: string | null; tenantName: string | null;
  recommendation: string | null; recommendedPhaseIds: string[];
}

export interface BudgetSummary { id: string; budgetNumber: string; total: number; status: string; sentAt: string | null; }
export interface PhaseSummary { name: string; status: string; amount: number; }
export interface BillingDashboard {
  pendingBudgets: number; lastBudget: BudgetSummary | null;
  totalSpent: number; recentPhases: PhaseSummary[];
}

export interface DiscountResponse {
  id: string; code: string; description: string;
  type: 'PERCENTAGE' | 'FIXED'; value: number;
  isActive: boolean; validFrom: string | null; validUntil: string | null;
  appliesTo: string | null;
}

export const getBillingDashboard = (tenantId: string) =>
  apiFetch<BillingDashboard>(`/billing/tenants/${tenantId}/dashboard`);

export const listBudgets = (tenantId: string, status?: string, page = 0, size = 20) => {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) params.set('status', status);
  return apiFetch<BudgetResponse[]>(`/billing/tenants/${tenantId}/budgets?${params}`);
};

export const getBudget = (tenantId: string, id: string) =>
  apiFetch<BudgetResponse>(`/billing/tenants/${tenantId}/budgets/${id}`);

export const sendBudget = (id: string) =>
  apiFetch<{ acceptanceUrl: string; expiresAt: string }>(`/billing/budgets/${id}/send`, { method: 'POST' });

export const cancelBudget = (id: string) =>
  apiFetch<void>(`/billing/budgets/${id}`, { method: 'DELETE' });

export const listDiscounts = (tenantId?: string, isActive?: boolean) => {
  const params = new URLSearchParams();
  if (tenantId) params.set('tenantId', tenantId);
  if (isActive !== undefined) params.set('isActive', String(isActive));
  return apiFetch<DiscountResponse[]>(`/billing/discounts?${params}`);
};

export interface CreateBudgetRequest {
  profileId?: string;
  phaseIds?: string[];
  addonIds?: string[];
  notes?: string;
  clientNotes?: string;
  discountIds?: string[];
  validUntil?: string;
  recommendation?: string;
  recommendedPhaseIds?: string[];
}

export const createBudget = (tenantId: string, data: CreateBudgetRequest) =>
  apiFetch<BudgetResponse>(`/billing/tenants/${tenantId}/budgets`, {
    method: 'POST',
    body: JSON.stringify(data),
  });

export const updateBudget = (id: string, data: CreateBudgetRequest) =>
  apiFetch<BudgetResponse>(`/billing/budgets/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });

export const listAllBudgets = (status?: string, page = 0, size = 50) => {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) params.set('status', status);
  return apiFetch<BudgetResponse[]>(`/billing/budgets?${params}`);
};

// Públic (sense auth) — per a la pàgina d'acceptació del client
export const previewBudget = (token: string) =>
  apiFetch<BudgetResponse>(`/billing/budgets/preview?token=${encodeURIComponent(token)}`);

export const acceptBudgetFull = (token: string) =>
  apiFetch<{ status: string; message: string }>(`/billing/budgets/accept?token=${encodeURIComponent(token)}`, { method: 'POST' });

export const acceptBudgetPhases = (token: string, phaseIds: string[]) =>
  apiFetch<{ status: string; message: string }>(`/billing/budgets/accept-phases?token=${encodeURIComponent(token)}`, {
    method: 'POST',
    body: JSON.stringify({ phaseIds }),
  });

export const rejectBudget = (token: string, reason?: string) =>
  apiFetch<{ status: string; message: string }>(`/billing/budgets/reject?token=${encodeURIComponent(token)}`, {
    method: 'POST',
    body: JSON.stringify({ reason: reason ?? '' }),
  });
