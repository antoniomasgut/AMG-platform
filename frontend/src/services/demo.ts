import { apiFetch } from './api';

export interface DemoSession {
  token: string;
  url: string;
  demoEmail: string;
  expiresAt: string;
}

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

export const createDemoSession = (
  prospectEmail: string,
  companyName: string,
  agentContext: string,
) =>
  apiFetch<DemoSession>('/admin/demo/inbox', {
    method: 'POST',
    body: JSON.stringify({ prospectEmail, companyName, agentContext }),
  });

export const updateDemoSession = (
  token: string,
  companyName: string,
  agentContext: string,
) =>
  apiFetch<DemoSession>(`/admin/demo/inbox/${token}`, {
    method: 'PATCH',
    body: JSON.stringify({ companyName, agentContext }),
  });

export const getDemoInbox = (token: string) =>
  apiFetch<DemoInbox>(`/demo/inbox/${token}`, { skipAuth: true });

export const sendDemoReply = (token: string, text: string) =>
  apiFetch<{ status?: string; blocked?: string; reason?: string }>(
    `/demo/inbox/${token}/reply`,
    {
      method: 'POST',
      body: JSON.stringify({ text }),
      skipAuth: true,
    },
  );
