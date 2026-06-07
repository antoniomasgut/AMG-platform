'use client';

import { useState, useCallback, useEffect, useRef, type FC } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useEditorStore } from '@/store/editor';
import { createVersion, updateVersion, publishLanding } from '@/services/factory';
import { BlockCatalog } from './BlockCatalog';
import { BlockProperties } from './BlockProperties';
import { PageStylesPanel } from './PageStylesPanel';
import { ChatContextPanel } from './ChatContextPanel';
import { FactoryCanvas } from './FactoryCanvas';
import { PreviewToolbar } from './PreviewToolbar';
import { BlockRenderer } from './BlockRenderer';
import { VersionHistory } from './VersionHistory';
import { IconSet } from '@/components/ui/icons';
import type { BlockType } from '@/services/factory';

type SidebarTab = 'blocks' | 'properties' | 'styles' | 'chat' | 'versions';

interface Props {
  landingId: string;
}

const LOCALES = ['ca', 'es', 'en', 'de'];

export const FactoryLayout: FC<Props> = ({ landingId }) => {
  const router = useRouter();
  const pathname = usePathname();
  const locale = LOCALES.find((l) => pathname.startsWith(`/${l}/`)) ?? '';
  const localePrefix = locale ? `/${locale}` : '';
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

  const selectedBlockId = useEditorStore((s) => s.selectedBlockId);

  const isDirtyRef = useRef(isDirty);
  useEffect(() => { isDirtyRef.current = isDirty; }, [isDirty]);

  // Auto-switch to Props tab when a block is selected in the canvas
  useEffect(() => {
    if (selectedBlockId) setSidebarTab('properties');
  }, [selectedBlockId]);

  useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => {
      if (isDirtyRef.current) {
        e.preventDefault();
        e.returnValue = '';
      }
    };
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, []);

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

  const sidebarContent: Record<SidebarTab, JSX.Element> = {
    blocks:     <BlockCatalog onAddBlock={handleAddBlock} />,
    properties: <BlockProperties />,
    styles:     <PageStylesPanel />,
    chat:       <ChatContextPanel landingId={landingId} />,
    versions:   <VersionHistory />,
  };

  const tabs: Array<{ key: SidebarTab; label: string }> = [
    { key: 'blocks',     label: 'Blocs' },
    { key: 'properties', label: 'Props' },
    { key: 'styles',     label: 'Estils' },
    { key: 'chat',       label: 'Chat' },
    { key: 'versions',   label: 'Vers.' },
  ];

  return (
    <div className="flex flex-col h-full">
      {/* Topbar */}
      <div className="h-12 shrink-0 bg-[#0d0d1a] border-b border-border-base flex items-center px-4 gap-3">
        <button
          onClick={() => router.push(`${localePrefix}/portal/landings`)}
          className="flex items-center gap-1 text-ink-2 hover:text-ink-0 transition shrink-0 f-mono text-xs"
          title="Tornar a Landings"
        >
          <IconSet.Chevron size={16} className="rotate-180" />
        </button>
        <span className="f-mono text-xs uppercase tracking-wider text-ink-0 truncate max-w-[180px]">
          {landing?.title || 'Editor'}
        </span>
        <span className="f-mono text-label text-ink-3 uppercase">
          {landing?.status === 'PUBLISHED' ? 'Publicada' : 'Esborrany'}
        </span>
        {isDirty && (
          <span className="w-2 h-2 rounded-full bg-[#FF6B00] shrink-0" title="Canvis sense desar" />
        )}
        <div className="flex-1" />
        <PreviewToolbar />
        {statusMsg && (
          <span className="f-mono text-label text-accent-light">{statusMsg}</span>
        )}
        <button
          onClick={handleSave}
          disabled={!isDirty || isSaving}
          className="f-mono text-label uppercase px-3 h-8 rounded bg-accent-muted text-accent-light hover:bg-[rgba(255,107,0,0.2)] disabled:opacity-40 transition"
        >
          {isSaving ? 'Guardant...' : isDirty ? 'Guardar' : 'Desat'}
        </button>
        <button
          onClick={handlePublish}
          disabled={isSaving}
          className="f-mono text-label uppercase px-4 h-8 rounded bg-[#FF6B00] text-black font-semibold hover:bg-[#FF9A3C] disabled:opacity-50 transition"
        >
          Publicar
        </button>
        <button
          onClick={() => setShowPreview(!showPreview)}
          className="f-mono text-label uppercase px-3 h-8 rounded text-ink-1 hover:text-white transition"
        >
          Preview
        </button>
      </div>

      {/* Main area */}
      <div className="flex flex-1 min-h-0">
        {/* Left sidebar */}
        <aside className="w-[220px] shrink-0 bg-[#13132a] border-r border-border-base flex flex-col">
          <div className="flex border-b border-border-base">
            {tabs.map(({ key, label }) => (
              <button
                key={key}
                onClick={() => setSidebarTab(key)}
                className={`flex-1 h-9 f-mono text-[9px] uppercase tracking-wider transition ${
                  sidebarTab === key
                    ? 'text-accent-light border-b-2 border-[#FF6B00] bg-[rgba(255,107,0,0.05)]'
                    : 'text-ink-3 hover:text-ink-1'
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
        <div
          className="fixed inset-0 z-40 bg-black/90 flex items-center justify-center"
          onClick={() => setShowPreview(false)}
        >
          <div
            className="w-full max-w-4xl max-h-screen overflow-auto m-8"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="bg-white shadow-2xl" style={{ fontFamily: styles.fontBody || styles.fontHeading }}>
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
