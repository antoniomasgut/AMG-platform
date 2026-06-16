import { apiFetch } from './api';

export interface MeetingSettings {
  tenantId: string;
  workingDays: string;
  startTime: string;
  endTime: string;
  slotDurationMinutes: number;
  bufferMinutes: number;
  minNoticeHours: number;
  maxAdvanceDays: number;
  calendarId: string | null;
}

export interface BookingToken {
  id: string;
  token: string;
  leadId: string;
  leadName: string;
  leadEmail: string | null;
  confirmed: boolean;
  meetingAt: string | null;
  meetLink: string | null;
  expiresAt: string;
  createdAt: string;
}

export interface TokenInfo {
  leadName: string;
  slotDurationMinutes: number;
  workingDays: string;
  bookingLabel?: string;
}

export interface DocumentPreferences {
  tenantId: string;
  language: string;
  standardDocChannel: string;
  sensitiveDocChannel: string;
  autoBookingOnAccept: boolean;
}

export interface BookingResult {
  meetingAt: string;
  meetLink?: string;
}

const BASE = process.env.NEXT_PUBLIC_API_URL || '';

// ── Admin endpoints (authenticated) ──────────────────────────

export const getDocumentPreferences = (tenantId?: string) =>
  apiFetch<DocumentPreferences>(`/documents/preferences${tenantId ? `?tenantId=${tenantId}` : ''}`);

export const updateDocumentPreferences = (data: Partial<DocumentPreferences>, tenantId?: string) =>
  apiFetch<DocumentPreferences>(
    `/documents/preferences${tenantId ? `?tenantId=${tenantId}` : ''}`,
    { method: 'PUT', body: JSON.stringify(data) }
  );

export const getBookingSettings = () =>
  apiFetch<MeetingSettings>('/booking/settings');

export const updateBookingSettings = (data: Partial<MeetingSettings>) =>
  apiFetch<MeetingSettings>('/booking/settings', { method: 'PUT', body: JSON.stringify(data) });

export const createBookingToken = (leadId: string) =>
  apiFetch<{ token: string; expiresAt: string }>(`/booking/tokens?leadId=${leadId}`, { method: 'POST' });

export const getTokensForLead = (leadId: string) =>
  apiFetch<BookingToken[]>(`/booking/tokens/lead/${leadId}`);

// ── Public endpoints (no auth) ────────────────────────────────

async function publicFetch<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}/api/v1${path}`);
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || res.statusText);
  }
  return res.json();
}

async function publicPost<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${BASE}/api/v1${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || res.statusText);
  }
  return res.json();
}

export const getTokenInfo = (token: string) =>
  publicFetch<TokenInfo>(`/booking/${token}`);

export const getAvailableDays = (token: string) =>
  publicFetch<string[]>(`/booking/${token}/days`);

export const getSlotsForDay = (token: string, date: string) =>
  publicFetch<string[]>(`/booking/${token}/slots?date=${date}`);

export const confirmBooking = (token: string, slotDatetime: string) =>
  publicPost<BookingResult>(`/booking/${token}/confirm`, { slotDatetime });
