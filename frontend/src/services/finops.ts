import { apiFetch } from './api';

export interface InvoiceResponse {
  id: string; tenantId: string; budgetId: string;
  holdedInvoiceId: string; invoiceNumber: string;
  status: string; amount: number; taxAmount: number;
  currency: string; verifactuStatus: string;
  invoicePdfUrl: string | null;
  dueDate: string | null; paidAt: string | null;
  errorMessage: string | null; createdAt: string;
}

export interface FinOpsDashboard {
  totalInvoiced: number; totalPaid: number;
  totalPending: number; totalOverdue: number;
  invoiceCount: number; paidCount: number;
  pendingCount: number; overdueCount: number;
}

export interface FinOpsGlobalDashboard extends FinOpsDashboard {
  tenantCount: number;
}

export interface PageResponse<T> {
  content: T[]; totalPages: number; totalElements: number;
  number: number; size: number;
}

export const getFinOpsDashboard = (tenantId: string) =>
  apiFetch<FinOpsDashboard>(`/finops/dashboard?tenantId=${tenantId}`);

export const getClientFinOpsDashboard = () =>
  apiFetch<FinOpsDashboard>('/finops/dashboard/client');

export const getGlobalFinOpsDashboard = () =>
  apiFetch<FinOpsGlobalDashboard>('/finops/dashboard/global');

export const listInvoices = (tenantId?: string, status?: string, page = 0, size = 20) => {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (tenantId) params.set('tenantId', tenantId);
  if (status) params.set('status', status);
  return apiFetch<PageResponse<InvoiceResponse>>(`/finops/invoices?${params}`);
};

export const getInvoice = (id: string) =>
  apiFetch<InvoiceResponse>(`/finops/invoices/${id}`);

export const getInvoicePdfUrl = (id: string) =>
  apiFetch<string>(`/finops/invoices/${id}/pdf`);

export const cancelInvoice = (id: string) =>
  apiFetch<InvoiceResponse>(`/finops/invoices/${id}/cancel`, { method: 'POST' });

export interface MonthlyInvoiceResponse {
  id: string; tenantId: string; period: string;
  invoiceNumber: string; amount: number; status: string;
  sepaCollectionDate: string | null; sepaCollected: boolean;
  invoicePdfUrl: string | null; createdAt: string;
  tenantName: string | null; tenantNif: string | null;
  tenantAddress: string | null; tenantEmail: string | null;
}

export const generateMonthlyInvoiceForTenant = (tenantId: string, period?: string) => {
  const params = new URLSearchParams();
  if (period) params.set('period', period);
  const query = params.toString() ? `?${params}` : '';
  return apiFetch<MonthlyInvoiceResponse>(
    `/finops/tenants/${tenantId}/invoices/generate-monthly${query}`,
    { method: 'POST' },
  );
};
