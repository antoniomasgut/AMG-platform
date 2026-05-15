'use client';

import { apiFetch } from './api';
import type { BlockType } from './factory';

// --- Types ---

export interface TemplateSummary {
  id: string;
  name: string;
  slug: string;
  description: string;
  sectionCount: number;
  isActive: boolean;
  createdAt: string;
}

export interface TemplateSectionView {
  id: string;
  blockType: string;
  sortOrder: number;
  propsSchema: string;
  defaultProps: string;
}

export interface TemplateDetail {
  id: string;
  name: string;
  slug: string;
  description: string;
  isActive: boolean;
  sections: TemplateSectionView[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateTemplateRequest {
  name: string;
  slug: string;
  description: string;
}

export interface TemplateSectionRequest {
  blockType: string;
  sortOrder: number;
  propsSchema: string;
  defaultProps: string;
}

// --- Block type mapping ---

/** Convert frontend kebab-case (e.g. 'contact-form') to backend UPPER_SNAKE_CASE (e.g. 'CONTACT_FORM') */
export function blockTypeToBackend(type: BlockType): string {
  return type.toUpperCase().replace(/-/g, '_');
}

/** Convert backend UPPER_SNAKE_CASE to frontend kebab-case */
export function blockTypeFromBackend(type: string): BlockType {
  return type.toLowerCase().replace(/_/g, '-') as BlockType;
}

// --- API functions ---

export async function listTemplates(): Promise<TemplateSummary[]> {
  return apiFetch<TemplateSummary[]>('/engine/templates');
}

export async function getTemplate(id: string): Promise<TemplateDetail> {
  return apiFetch<TemplateDetail>(`/engine/templates/${id}`);
}

export async function createTemplate(data: CreateTemplateRequest): Promise<TemplateDetail> {
  return apiFetch<TemplateDetail>('/engine/templates', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function updateTemplate(id: string, data: CreateTemplateRequest): Promise<TemplateDetail> {
  return apiFetch<TemplateDetail>(`/engine/templates/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function deleteTemplate(id: string): Promise<void> {
  return apiFetch<void>(`/engine/templates/${id}`, { method: 'DELETE' });
}

// --- Sections ---

export async function addTemplateSection(templateId: string, data: TemplateSectionRequest): Promise<TemplateDetail> {
  return apiFetch<TemplateDetail>(`/engine/templates/${templateId}/sections`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function updateTemplateSection(templateId: string, sectionId: string, data: TemplateSectionRequest): Promise<TemplateDetail> {
  return apiFetch<TemplateDetail>(`/engine/templates/${templateId}/sections/${sectionId}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function removeTemplateSection(templateId: string, sectionId: string): Promise<void> {
  return apiFetch<void>(`/engine/templates/${templateId}/sections/${sectionId}`, {
    method: 'DELETE',
  });
}
