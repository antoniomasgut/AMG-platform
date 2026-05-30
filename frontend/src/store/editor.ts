'use client';

import { create } from 'zustand';
import type { Block, PageContent, PageStyles, LandingDetail } from '@/services/factory';

const DEFAULT_STYLES: PageStyles = {
  fontHeading: 'Montserrat, sans-serif',
  fontBody: 'Open Sans, sans-serif',
  primaryColor: '#FF6B00',
  accentColor: '#1e293b',
  bgColor: '#ffffff',
  textColor: '#1e293b',
  borderRadius: '8px',
};

export interface EditorState {
  landing: LandingDetail | null;
  versionId: string | null;
  content: PageContent;
  styles: PageStyles;
  selectedBlockId: string | null;
  isDirty: boolean;
  isSaving: boolean;
  previewMode: 'desktop' | 'tablet' | 'mobile';

  loadLanding: (landing: LandingDetail, versionId: string) => void;
  setBlocks: (blocks: Block[]) => void;
  addBlock: (type: Block['type'], index?: number) => void;
  removeBlock: (blockId: string) => void;
  moveBlock: (blockId: string, newIndex: number) => void;
  updateBlockProps: (blockId: string, props: Partial<Record<string, unknown>>) => void;
  setStyles: (styles: Partial<PageStyles>) => void;
  selectBlock: (blockId: string | null) => void;
  setPreviewMode: (mode: 'desktop' | 'tablet' | 'mobile') => void;
  markClean: () => void;
  markSaving: (saving: boolean) => void;
  reset: () => void;
}

export const useEditorStore = create<EditorState>((set) => ({
  landing: null,
  versionId: null,
  content: { blocks: [] },
  styles: DEFAULT_STYLES,
  selectedBlockId: null,
  isDirty: false,
  isSaving: false,
  previewMode: 'desktop',

  loadLanding: (landing, versionId) =>
    set({
      landing,
      versionId,
      content: landing.versions.find((v) => v.id === versionId)?.content || { blocks: [] },
      styles: landing.versions.find((v) => v.id === versionId)?.styles || DEFAULT_STYLES,
      selectedBlockId: null,
      isDirty: false,
    }),

  setBlocks: (blocks) =>
    set({ content: { blocks }, isDirty: true }),

  addBlock: (type, index) =>
    set((state) => {
      const newBlock: Block = {
        id: crypto.randomUUID(),
        type,
        props: {},
      };
      const blocks = [...state.content.blocks];
      if (index !== undefined && index >= 0 && index <= blocks.length) {
        blocks.splice(index, 0, newBlock);
      } else {
        blocks.push(newBlock);
      }
      return { content: { blocks }, isDirty: true, selectedBlockId: newBlock.id };
    }),

  removeBlock: (blockId) =>
    set((state) => ({
      content: { blocks: state.content.blocks.filter((b) => b.id !== blockId) },
      isDirty: true,
      selectedBlockId: state.selectedBlockId === blockId ? null : state.selectedBlockId,
    })),

  moveBlock: (blockId, newIndex) =>
    set((state) => {
      const blocks = [...state.content.blocks];
      const oldIndex = blocks.findIndex((b) => b.id === blockId);
      if (oldIndex === -1) return state;
      const [block] = blocks.splice(oldIndex, 1);
      blocks.splice(newIndex, 0, block);
      return { content: { blocks }, isDirty: true };
    }),

  updateBlockProps: (blockId, props) =>
    set((state) => ({
      content: {
        blocks: state.content.blocks.map((b) =>
          b.id === blockId ? { ...b, props: { ...b.props, ...props } } : b
        ),
      },
      isDirty: true,
    })),

  setStyles: (styles) =>
    set((state) => ({ styles: { ...state.styles, ...styles }, isDirty: true })),

  selectBlock: (blockId) => set({ selectedBlockId: blockId }),
  setPreviewMode: (previewMode) => set({ previewMode }),
  markClean: () => set({ isDirty: false }),
  markSaving: (isSaving) => set({ isSaving }),
  reset: () => set({ landing: null, versionId: null, content: { blocks: [] }, styles: DEFAULT_STYLES, selectedBlockId: null, isDirty: false }),
}));
