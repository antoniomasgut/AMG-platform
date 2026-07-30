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

type Snapshot = { content: PageContent; styles: PageStyles };

function snap(state: EditorState): { history: Snapshot[]; historyIndex: number } {
  const newHistory = [
    ...state.history.slice(0, state.historyIndex + 1),
    { content: state.content, styles: state.styles },
  ].slice(-30);
  return { history: newHistory, historyIndex: newHistory.length - 1 };
}

export interface EditorState {
  landing: LandingDetail | null;
  versionId: string | null;
  currentLocale: string;
  content: PageContent;
  styles: PageStyles;
  selectedBlockId: string | null;
  isDirty: boolean;
  isSaving: boolean;
  previewMode: 'desktop' | 'tablet' | 'mobile';
  history: Snapshot[];
  historyIndex: number;

  loadLanding: (landing: LandingDetail, versionId: string, locale?: string) => void;
  setLocale: (locale: string) => void;
  setBlocks: (blocks: Block[]) => void;
  addBlock: (type: Block['type'], index?: number) => void;
  removeBlock: (blockId: string) => void;
  duplicateBlock: (blockId: string) => void;
  moveBlock: (blockId: string, newIndex: number) => void;
  updateBlockProps: (blockId: string, props: Partial<Record<string, unknown>>) => void;
  setStyles: (styles: Partial<PageStyles>) => void;
  selectBlock: (blockId: string | null) => void;
  setPreviewMode: (mode: 'desktop' | 'tablet' | 'mobile') => void;
  undo: () => void;
  redo: () => void;
  markClean: () => void;
  markSaving: (saving: boolean) => void;
  reset: () => void;
}

export const useEditorStore = create<EditorState>((set) => ({
  landing: null,
  versionId: null,
  currentLocale: 'ca',
  content: { blocks: [] },
  styles: DEFAULT_STYLES,
  selectedBlockId: null,
  isDirty: false,
  isSaving: false,
  previewMode: 'desktop',
  history: [],
  historyIndex: -1,

  loadLanding: (landing, versionId, locale) => {
    const version = landing.versions.find((v) => v.id === versionId);
    const initialContent = version?.content || { blocks: [] };
    const initialStyles = version?.styles || DEFAULT_STYLES;
    set({
      landing,
      versionId,
      currentLocale: locale ?? version?.locale ?? 'ca',
      content: initialContent,
      styles: initialStyles,
      selectedBlockId: null,
      isDirty: false,
      history: [{ content: initialContent, styles: initialStyles }],
      historyIndex: 0,
    });
  },

  setLocale: (locale) => set({ currentLocale: locale }),

  setBlocks: (blocks) =>
    set((state) => ({ ...snap(state), content: { blocks }, isDirty: true })),

  addBlock: (type, index) =>
    set((state) => {
      const snapData = snap(state);
      const newBlock: Block = { id: crypto.randomUUID(), type, props: {} };
      const blocks = [...state.content.blocks];
      if (index !== undefined && index >= 0 && index <= blocks.length) {
        blocks.splice(index, 0, newBlock);
      } else {
        blocks.push(newBlock);
      }
      return { ...snapData, content: { blocks }, isDirty: true, selectedBlockId: newBlock.id };
    }),

  removeBlock: (blockId) =>
    set((state) => ({
      ...snap(state),
      content: { blocks: state.content.blocks.filter((b) => b.id !== blockId) },
      isDirty: true,
      selectedBlockId: state.selectedBlockId === blockId ? null : state.selectedBlockId,
    })),

  duplicateBlock: (blockId) =>
    set((state) => {
      const snapData = snap(state);
      const blocks = [...state.content.blocks];
      const idx = blocks.findIndex((b) => b.id === blockId);
      if (idx === -1) return state;
      const original = blocks[idx];
      const clone: Block = {
        id: crypto.randomUUID(),
        type: original.type,
        props: JSON.parse(JSON.stringify(original.props)),
      };
      blocks.splice(idx + 1, 0, clone);
      return { ...snapData, content: { blocks }, isDirty: true, selectedBlockId: clone.id };
    }),

  moveBlock: (blockId, newIndex) =>
    set((state) => {
      const snapData = snap(state);
      const blocks = [...state.content.blocks];
      const oldIndex = blocks.findIndex((b) => b.id === blockId);
      if (oldIndex === -1) return state;
      const [block] = blocks.splice(oldIndex, 1);
      blocks.splice(newIndex, 0, block);
      return { ...snapData, content: { blocks }, isDirty: true };
    }),

  updateBlockProps: (blockId, props) =>
    set((state) => ({
      ...snap(state),
      content: {
        blocks: state.content.blocks.map((b) =>
          b.id === blockId ? { ...b, props: { ...b.props, ...props } } : b
        ),
      },
      isDirty: true,
    })),

  setStyles: (styles) =>
    set((state) => ({ ...snap(state), styles: { ...state.styles, ...styles }, isDirty: true })),

  undo: () =>
    set((state) => {
      const idx = state.historyIndex;
      if (idx <= 0) return state;
      const prev = state.history[idx - 1];
      return { content: prev.content, styles: prev.styles, historyIndex: idx - 1, isDirty: true };
    }),

  redo: () =>
    set((state) => {
      const idx = state.historyIndex;
      if (idx >= state.history.length - 1) return state;
      const next = state.history[idx + 1];
      return { content: next.content, styles: next.styles, historyIndex: idx + 1, isDirty: true };
    }),

  selectBlock: (blockId) => set({ selectedBlockId: blockId }),
  setPreviewMode: (previewMode) => set({ previewMode }),
  markClean: () => set({ isDirty: false }),
  markSaving: (isSaving) => set({ isSaving }),
  reset: () => set({
    landing: null, versionId: null, currentLocale: 'ca', content: { blocks: [] }, styles: DEFAULT_STYLES,
    selectedBlockId: null, isDirty: false, history: [], historyIndex: -1,
  }),
}));
