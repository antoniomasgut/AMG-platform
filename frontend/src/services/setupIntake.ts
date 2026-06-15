const BASE = `${process.env.NEXT_PUBLIC_API_URL ?? ''}/api/v1`;

export interface IntakeResponse {
  id: string;
  token: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETE';
  budgetId: string;
  tenantName: string | null;
  sector: string | null;
  phaseNumbers: number[] | null;
  intakeData: string | null; // JSON string
  intakeUrl: string;
}

export interface IntakeData {
  // AGENT (sempre)
  businessName?: string;
  whatsappPhone?: string;
  schedule?: string;
  location?: string;
  mainServices?: string;
  // WEB/LANDING (sempre)
  hasExistingWeb?: boolean;
  existingWebUrl?: string;
  wantsLanding?: 'none' | 'micro' | 'pro';
  wantsChatWidget?: boolean;
  wantsWhatsappButton?: boolean;
  domainPreference?: 'existing' | 'new' | 'none';
  domainName?: string;
  // PRESSUPOSTOS
  budgetValidityDays?: number;
  catalogItems?: Array<{ name: string; unitPrice: number; unit: string }>;
  paymentTerms?: string;
  // AGENDA
  visitDuration?: number;
  googleCalendarEmail?: string;
  scheduleNotes?: string;
  // FIDELITZACIO
  googleReviewsUrl?: string;
  followUpDays?: number;
  // General
  notes?: string;
}

export const createIntake = (budgetId: string): Promise<IntakeResponse> =>
  fetch(`${BASE}/billing/budgets/${budgetId}/intake`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${getToken()}`,
    },
  }).then(r => r.ok ? r.json() as Promise<IntakeResponse> : Promise.reject(r));

export const getIntakeByBudget = (budgetId: string): Promise<IntakeResponse | null> =>
  fetch(`${BASE}/billing/budgets/${budgetId}/intake`, {
    headers: { 'Authorization': `Bearer ${getToken()}` },
  }).then(r => {
    if (r.ok) return r.json() as Promise<IntakeResponse>;
    if (r.status === 404) return null;
    return Promise.reject(r);
  });

export const getIntakePublic = (token: string): Promise<IntakeResponse> =>
  fetch(`${BASE}/billing/intake/${token}`)
    .then(r => r.ok ? r.json() : Promise.reject(r));

export const saveIntake = (token: string, intakeData: IntakeData, complete: boolean): Promise<IntakeResponse> =>
  fetch(`${BASE}/billing/intake/${token}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ intakeData: JSON.stringify(intakeData), complete }),
  }).then(r => r.ok ? r.json() : Promise.reject(r));

function getToken(): string {
  if (typeof window === 'undefined') return '';
  return localStorage.getItem('amg_token') ?? '';
}
