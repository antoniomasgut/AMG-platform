'use client';

import { useState, useCallback, type FC, type ReactNode } from 'react';
import { useEditorStore } from '@/store/editor';
import { createVersion, updateVersion, publishLanding } from '@/services/factory';
import { BlockCatalog } from './BlockCatalog';
import { BlockProperties } from './BlockProperties';
import { PageStylesPanel } from './PageStylesPanel';
import { FactoryCanvas } from './FactoryCanvas';
import { PreviewToolbar } from './PreviewToolbar';
import { BlockRenderer } from './BlockRenderer';
import type { BlockType } from '@/services/factory';

type SidebarTab = 'blocks' | 'properties' | 'styles';

interface Props {
  landingId: string;
  children?: ReactNode;
}

export const FactoryLayout: FC<Props> = ({ landingId }) => {
  const [sidebarTab, setSidebarTab] = useState<SidebarTab>('blocks');
  const [statusMsg, setStatusMsg] = useState('');
  const [showPreview, setShowPreview] = useState(false);

  const landing = useEditorStore((s) => s.landing);
  const versionId = useEditorStore((s) => s.versionId);
  const content = useEditorStore((s) => s.content);
  const styles = useEditorStore((s) => s.styles);
  const isDirty = useEditorStore((s) => s.isDirty);
  const isSaving = useEditorStore((s) => s.isSaving);
  const markClean = useEditorStore((s) => s.markClean);
  const markSaving = useEditorStore((s) => s.markSaving);
  const addBlock = useEditorStore((s) => s.addBlock);
  const reset = useEditorStore((s) => s.reset);

  const handleSave = useCallback(async () => {
    if (!versionId) return;
    markSaving(true);
    try {
      await updateVersion(landingId, versionId, content, styles);
      markClean();
      setStatusMsg('Desat');
      setTimeout(() => setStatusMsg(''), 2000);
    } catch {
      setStatusMsg('Error en desar');
    } finally {
      markSaving(false);
    }
  }, [landingId, versionId, content, styles, markSaving, markClean]);

  const handlePublish = useCallback(async () => {
    markSaving(true);
    try {
      if (isDirty) {
        if (versionId) await updateVersion(landingId, versionId, content, styles);
        else await createVersion(landingId, content, styles);
      }
      await publishLanding(landingId);
      setStatusMsg('Publicat!');
      setTimeout(() => setStatusMsg(''), 3000);
    } catch {
      setStatusMsg('Error en publicar');
    } finally {
      markSaving(false);
    }
  }, [landingId, versionId, content, styles, isDirty, markSaving]);

  const handleAddBlock = useCallback((type: BlockType) => {
    addBlock(type);
    setSidebarTab('properties');
  }, [addBlock]);

  const sidebarContent = {
    blocks: <BlockCatalog onAddBlock={handleAddBlock} />,
    properties: <BlockProperties />,
    styles: <PageStylesPanel />,
  };

  const tabs: Array<{ key: SidebarTab; label: string }> = [
    { key: 'blocks', label: 'Blocs' },
    { key: 'properties', label: 'Propietats' },
    { key: 'styles', label: 'Estils' },
  ];

  return (
    <div className="flex flex-col h-full">
      {/* Topbar */}
      <div className="h-12 shrink-0 bg-[#0d0d1a] border-b border-[rgba(255,107,0,0.12)] flex items-center px-4 gap-3">
        <span className="f-mono text-xs uppercase tracking-wider text-[#e2e8f0] truncate">
          {landing?.title || 'Editor'}
        </span>
        <span className="f-mono text-[10px] text-[#64748b] uppercase">
          {landing?.status === 'PUBLISHED' ? 'Publicada' : 'Esborrany'}
        </span>
        <div className="flex-1" />
        <PreviewToolbar />
        {statusMsg && (
          <span className="f-mono text-[10px] text-[#FF9A3C]">{statusMsg}</span>
        )}
        <button
          onClick={handleSave}
          disabled={!isDirty || isSaving}
          className="f-mono text-[10px] uppercase px-3 h-8 rounded bg-[rgba(255,107,0,0.12)] text-[#FF9A3C] hover:bg-[rgba(255,107,0,0.2)] disabled:opacity-40 transition"
        >
          {isSaving ? 'Guardant...' : isDirty ? 'Guardar' : 'Desat'}
        </button>
        <button
          onClick={handlePublish}
          disabled={isSaving}
          className="f-mono text-[10px] uppercase px-4 h-8 rounded bg-[#FF6B00] text-black font-semibold hover:bg-[#FF9A3C] disabled:opacity-50 transition"
        >
          Publicar
        </button>
        <button
          onClick={() => setShowPreview(!showPreview)}
          className="f-mono text-[10px] uppercase px-3 h-8 rounded text-[#94a3b8] hover:text-white transition"
        >
          Previsualitzar
        </button>
      </div>

      {/* Main area */}
      <div className="flex flex-1 min-h-0">
        {/* Left sidebar */}
        <aside className="w-[220px] shrink-0 bg-[#13132a] border-r border-[rgba(255,107,0,0.12)] flex flex-col">
          <div className="flex border-b border-[rgba(255,107,0,0.12)]">
            {tabs.map(({ key, label }) => (
              <button
                key={key}
                onClick={() => setSidebarTab(key)}
                className={`flex-1 h-9 f-mono text-[10px] uppercase tracking-wider transition ${
                  sidebarTab === key
                    ? 'text-[#FF9A3C] border-b-2 border-[#FF6B00] bg-[rgba(255,107,0,0.05)]'
                    : 'text-[#64748b] hover:text-[#94a3b8]'
                }`}
              >
                {label}
              </button>
            ))}
          </div>
          <div className="flex-1 overflow-auto">
            {sidebarContent[sidebarTab]}
          </div>
        </aside>

        {/* Canvas */}
        <FactoryCanvas />
      </div>

      {/* Preview overlay */}
      {showPreview && (
        <div className="fixed inset-0 z-40 bg-black/90 flex items-center justify-center" onClick={() => setShowPreview(false)}>
          <div className="w-full max-w-4xl max-h-screen overflow-auto m-8" onClick={(e) => e.stopPropagation()}>
            <div className="bg-white shadow-2xl" style={{ fontFamily: styles.fontFamily }}>
              {content.blocks.map((block) => (
                <BlockRenderer key={block.id} block={block} styles={styles} preview />
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
