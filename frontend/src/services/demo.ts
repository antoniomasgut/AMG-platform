import { apiFetch } from './api';

export interface DemoSession {
  token: string;
  landingUrl: string;
  sector: string;
  companyName: string;
  demoEmail: string;
  locale: string;
  expiresAt: string;
  active: boolean;
}

export interface DemoCreateRequest {
  sector: string;
  companyName: string;
  agentContext?: string;
  prospectEmail?: string;
  locale?: string;
}

export const createDemoSession = (req: DemoCreateRequest) =>
  apiFetch<DemoSession>('/admin/demo/sessions', {
    method: 'POST',
    body: JSON.stringify(req),
  });

export const listDemoSessions = () =>
  apiFetch<DemoSession[]>('/admin/demo/sessions');

export const deactivateDemoSession = (token: string) =>
  apiFetch<void>(`/admin/demo/sessions/${token}`, { method: 'DELETE' });

export const updateDemoSession = (
  token: string,
  companyName: string,
  agentContext: string,
) =>
  apiFetch<DemoSession>(`/admin/demo/sessions/${token}`, {
    method: 'PATCH',
    body: JSON.stringify({ companyName, agentContext }),
  });

export const getDemoSession = (token: string) =>
  apiFetch<DemoSession>(`/demo/sessions/${token}`, { skipAuth: true });

// Legacy inbox types (V1 — kept for the demo inbox page)
export interface DemoMessage {
  id: number;
  customerIdentifier: string;
  channel: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  pendingApproval: boolean;
  createdAt: string;
}

export interface DemoInbox {
  token: string;
  prospectEmail: string;
  demoEmail: string;
  companyName: string | null;
  agentContext: string | null;
  expiresAt: string;
  blocked: boolean;
  blockReason: string | null;
  thread: DemoMessage[];
}

export const getDemoInbox = (token: string) =>
  apiFetch<DemoInbox>(`/demo/inbox/${token}`, { skipAuth: true });

export const sendDemoReply = (token: string, text: string) =>
  apiFetch<{ status?: string; blocked?: string; reason?: string }>(
    `/demo/inbox/${token}/reply`,
    { method: 'POST', body: JSON.stringify({ text }), skipAuth: true },
  );
