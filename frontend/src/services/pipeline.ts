import { apiFetch } from './api';

export interface PipelineCard {
  id: string;
  tenantId: string | null;
  name: string;
  sector: string | null;
  contact: string | null;
  value: string | null;
  date: string | null;
  actionUrl: string;
  stage: string | null; // present only for lead cards
}

export interface PipelineColumn {
  stage: string;
  label: string;
  cards: PipelineCard[];
}

export interface PipelineView {
  columns: PipelineColumn[];
  total: number;
}

export const LEAD_STAGE_LABELS: Record<string, string> = {
  NEW:         'Nou',
  CONTACTED:   'Contactat',
  QUALIFIED:   'Qualificat',
  PROPOSAL:    'Proposta',
  NEGOTIATION: 'Negociació',
  WON:         'Guanyat',
  LOST:        'Perdut',
  NURTURING:   'Nurturing',
};

export async function getPipeline(): Promise<PipelineView> {
  return apiFetch<PipelineView>('/pipeline');
}

export async function updateLeadStage(leadId: string, stage: string, notes?: string): Promise<void> {
  await apiFetch<void>(`/pipeline/${leadId}/stage`, {
    method: 'PUT',
    body: JSON.stringify({ stage, notes: notes ?? null }),
  });
}
