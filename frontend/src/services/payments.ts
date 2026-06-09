import { apiFetch } from './api';

export interface Payment {
  id: string;
  tenantId: string;
  budgetId: string;
  amount: number;
  currency: string;
  status: string;
  provider: string;
  createdAt: string;
}

export interface PaymentDashboard {
  totalPayments: number;
  completedCount: number;
  pendingCount: number;
  failedCount: number;
  totalAmount: number;
  totalCompleted: number;
}

export interface PaymentPage { content: Payment[]; totalPages: number; totalElements: number; }

export const getPayments = (tenantId?: string, page = 0, size = 20) => {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (tenantId) params.set('tenantId', tenantId);
  return apiFetch<PaymentPage>(`/payments?${params}`);
};

export const getPayment = (id: string) =>
  apiFetch<Payment>(`/payments/${id}`);

export const getPaymentDashboard = () =>
  apiFetch<PaymentDashboard>('/payments/dashboard');

export const refundPayment = (id: string) =>
  apiFetch<Payment>(`/payments/${id}/refund`, { method: 'POST' });

export interface SavedPaymentMethod {
  paymentMethodId: string;
  brand: string;
  lastFour: string | null;
  expMonth: number | null;
  expYear: number | null;
}

export const getSavedPaymentMethod = (tenantId: string) =>
  apiFetch<SavedPaymentMethod | null>(`/payments/tenants/${tenantId}/payment-method`).catch(() => null);

export const createSetupSession = (tenantId: string, successUrl: string, cancelUrl: string) => {
  const params = new URLSearchParams({ successUrl, cancelUrl });
  return apiFetch<{ url: string }>(`/payments/tenants/${tenantId}/setup?${params}`, { method: 'POST' });
};

export const completeSetupSession = (tenantId: string, sessionId: string) =>
  apiFetch<SavedPaymentMethod>(`/payments/tenants/${tenantId}/setup/complete?session_id=${encodeURIComponent(sessionId)}`, { method: 'POST' });

export const removeSavedPaymentMethod = (tenantId: string) =>
  apiFetch<void>(`/payments/tenants/${tenantId}/payment-method`, { method: 'DELETE' });
