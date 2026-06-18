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

export async function getPipeline(): Promise<PipelineView> {
  return apiFetch<PipelineView>('/pipeline');
}
