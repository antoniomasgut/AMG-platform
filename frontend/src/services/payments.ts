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

export const getPayments = (tenantId?: string) => {
  const params = tenantId ? `?tenantId=${tenantId}&size=200` : '?size=200';
  return apiFetch<{ content: Payment[] }>(`/payments${params}`).then(r => r.content);
};

export const getPayment = (id: string) =>
  apiFetch<Payment>(`/payments/${id}`);

export const getPaymentDashboard = () =>
  apiFetch<PaymentDashboard>('/payments/dashboard');

export const refundPayment = (id: string) =>
  apiFetch<Payment>(`/payments/${id}/refund`, { method: 'POST' });
