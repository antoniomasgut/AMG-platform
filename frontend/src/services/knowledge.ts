import { apiFetch } from './api';

export interface KnowledgeEntry {
  id: string;
  category: string;
  key: string;
  content: string;
  sortOrder: number;
}

export interface KnowledgeDocument {
  id: string;
  filename: string;
  uploadedAt: string;
}

export interface KnowledgeBase {
  id: string;
  tenantId: string;
  isActive: boolean;
  version: number;
  entriesByCategory: Record<string, KnowledgeEntry[]>;
  documents: KnowledgeDocument[];
}

export function getKnowledge(tenantId: string): Promise<KnowledgeBase> {
  return apiFetch(`/agents/knowledge/${tenantId}`);
}

export function updateEntries(
  tenantId: string,
  category: string,
  entries: { key?: string; content: string; sortOrder?: number }[]
): Promise<void> {
  return apiFetch(`/agents/knowledge/${tenantId}/entries/${category}`, {
    method: 'PUT',
    body: JSON.stringify({ entries }),
  });
}

export function addDocument(
  tenantId: string,
  filename: string,
  content: string
): Promise<KnowledgeDocument> {
  return apiFetch(`/agents/knowledge/${tenantId}/documents`, {
    method: 'POST',
    body: JSON.stringify({ filename, content }),
  });
}

export function deleteDocument(tenantId: string, docId: string): Promise<void> {
  return apiFetch(`/agents/knowledge/${tenantId}/documents/${docId}`, {
    method: 'DELETE',
  });
}

export async function previewPromptBlock(tenantId: string): Promise<string> {
  const res = await fetch(`/api/v1/agents/knowledge/${tenantId}/preview`, {
    headers: {
      Authorization: `Bearer ${sessionStorage.getItem('access_token') ?? ''}`,
    },
  });
  if (!res.ok) throw new Error('Error carregant previsualització');
  return res.text();
}
