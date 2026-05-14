import { apiFetch } from './api';

export interface BillingDashboard {
  pendingBudgets: number;
  lastBudget: { id: string; budgetNumber: string; total: number; status: string; sentAt: string } | null;
  totalSpent: number;
  recentPhases: { name: string; status: string; amount: number }[];
}

export interface Invoice {
  id: string;
  invoiceNumber: string;
  amount: number;
  status: string;
  dueDate: string | null;
  paidAt: string | null;
  createdAt: string;
  invoicePdfUrl: string | null;
}

export interface LandingSummary {
  id: string;
  name: string;
  status: string;
  publishedUrl: string | null;
}

export interface WorkflowSummary {
  id: string;
  name: string;
  status: string;
}

export interface PortalDashboard {
  billing: BillingDashboard | null;
  invoices: Invoice[];
  landings: LandingSummary[];
  workflows: WorkflowSummary[];
  loading: boolean;
  error: string | null;
}

export async function fetchBillingDashboard(tenantId: string): Promise<BillingDashboard> {
  return apiFetch<BillingDashboard>(`/billing/tenants/${tenantId}/dashboard`);
}

export async function fetchInvoices(): Promise<Invoice[]> {
  return apiFetch<Invoice[]>('/finops/invoices');
}

export async function fetchLandings(tenantId: string): Promise<LandingSummary[]> {
  return apiFetch<LandingSummary[]>(`/engine/tenants/${tenantId}/landings`);
}

export async function fetchWorkflows(tenantId: string): Promise<WorkflowSummary[]> {
  return apiFetch<WorkflowSummary[]>(`/automations/tenants/${tenantId}/workflows`);
}
