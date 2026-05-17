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
  totalCollected: number;
  totalPending: number;
  totalRefunded: number;
  recentPayments: Payment[];
}

export const getPayments = (tenantId?: string) => {
  const params = tenantId ? `?tenantId=${tenantId}` : '';
  return apiFetch<Payment[]>(`/payments${params}`);
};

export const getPayment = (id: string) =>
  apiFetch<Payment>(`/payments/${id}`);

export const getPaymentDashboard = () =>
  apiFetch<PaymentDashboard>('/payments/dashboard');

export const refundPayment = (id: string) =>
  apiFetch<Payment>(`/payments/${id}/refund`, { method: 'POST' });
