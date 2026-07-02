'use client';

import { useState, useCallback, useEffect, useRef, type FC } from 'react';
import { useRouter } from '@/i18n/navigation';
import { useEditorStore } from '@/store/editor';
import { createVersion, updateVersion, publishLanding, unpublishLanding, generatePreviewToken } from '@/services/factory';
import { getCurrentUser } from '@/services/auth';
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

export const FactoryLayout: FC<Props> = ({ landingId }) => {
  const router = useRouter();
  const user = getCurrentUser();
  const [sidebarTab, setSidebarTab] = useState<SidebarTab>('blocks');
  const [statusMsg, setStatusMsg] = useState('');
  const [showPreview, setShowPreview] = useState(false);
  const [previewDevice, setPreviewDevice] = useState<'desktop' | 'mobile'>('desktop');
  const [shareOpen, setShareOpen] = useState(false);
  const [shareUrl, setShareUrl] = useState('');
  const [shareCopied, setShareCopied] = useState(false);
  const [shareLoading, setShareLoading] = useState(false);

  const landing = useEditorStore((s) => s.landing);
  const versionId = useEditorStore((s) => s.versionId);
  const content = useEditorStore((s) => s.content);
  const styles = useEditorStore((s) => s.styles);
  const isDirty = useEditorStore((s) => s.isDirty);
  const isSaving = useEditorStore((s) => s.isSaving);
  const markClean = useEditorStore((s) => s.markClean);
  const markSaving = useEditorStore((s) => s.markSaving);
  const addBlock = useEditorStore((s) => s.addBlock);
  const undo = useEditorStore((s) => s.undo);
  const redo = useEditorStore((s) => s.redo);
  const history = useEditorStore((s) => s.history);
  const historyIndex = useEditorStore((s) => s.historyIndex);
  const canUndo = historyIndex > 0;
  const canRedo = historyIndex < history.length - 1;

  const selectedBlockId = useEditorStore((s) => s.selectedBlockId);

  const isDirtyRef = useRef(isDirty);
  useEffect(() => { isDirtyRef.current = isDirty; }, [isDirty]);

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

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;
      if ((e.ctrlKey || e.metaKey) && e.key === 'z' && !e.shiftKey) {
        e.preventDefault();
        undo();
      } else if ((e.ctrlKey || e.metaKey) && (e.key === 'y' || (e.key === 'z' && e.shiftKey))) {
        e.preventDefault();
        redo();
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [undo, redo]);

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

  const handleUnpublish = useCallback(async () => {
    markSaving(true);
    try {
      await unpublishLanding(landingId);
      setStatusMsg('Desactivada');
      setTimeout(() => router.push('/portal/landings'), 1500);
    } catch {
      setStatusMsg('Error en desactivar');
    } finally {
      markSaving(false);
    }
  }, [landingId, markSaving, router]);

  const handleShare = useCallback(async () => {
    if (!user?.tenantId) return;
    setShareLoading(true);
    setShareOpen(true);
    try {
      const { previewUrl } = await generatePreviewToken(user.tenantId, landingId);
      setShareUrl(previewUrl);
    } catch {
      setShareUrl('');
    } finally {
      setShareLoading(false);
    }
  }, [user?.tenantId, landingId]);

  const handleCopyShare = useCallback(() => {
    if (!shareUrl) return;
    navigator.clipboard.writeText(shareUrl).then(() => {
      setShareCopied(true);
      setTimeout(() => setShareCopied(false), 2000);
    });
  }, [shareUrl]);

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
          onClick={() => router.push('/portal/landings')}
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

        {/* Undo / Redo */}
        <button
          onClick={undo}
          disabled={!canUndo}
          title="Desfer (Ctrl+Z)"
          className="w-7 h-7 flex items-center justify-center rounded text-ink-2 hover:text-white hover:bg-white/10 disabled:opacity-30 disabled:cursor-not-allowed transition text-base"
        >
          ↩
        </button>
        <button
          onClick={redo}
          disabled={!canRedo}
          title="Refer (Ctrl+Y)"
          className="w-7 h-7 flex items-center justify-center rounded text-ink-2 hover:text-white hover:bg-white/10 disabled:opacity-30 disabled:cursor-not-allowed transition text-base"
        >
          ↪
        </button>

        <div className="flex-1" />
        <PreviewToolbar />
        {statusMsg && (
          <span className="f-mono text-label text-accent-light">{statusMsg}</span>
        )}

        {/* Share preview link */}
        <button
          onClick={handleShare}
          title="Compartir previsualització amb el client"
          className="f-mono text-label uppercase px-3 h-8 rounded border border-border-base text-ink-1 hover:text-accent-light hover:border-accent-light transition"
        >
          ↗ Preview
        </button>

        <button
          onClick={handleSave}
          disabled={!isDirty || isSaving}
          className="f-mono text-label uppercase px-3 h-8 rounded bg-accent-muted text-accent-light hover:bg-[rgba(255,107,0,0.2)] disabled:opacity-40 transition"
        >
          {isSaving ? 'Guardant...' : isDirty ? 'Guardar' : 'Desat'}
        </button>
        {landing?.status === 'PUBLISHED' ? (
          <button
            onClick={handleUnpublish}
            disabled={isSaving}
            className="f-mono text-label uppercase px-3 h-8 rounded border border-border-base text-ink-1 hover:text-warning hover:border-warning disabled:opacity-50 transition"
          >
            Desactivar
          </button>
        ) : (
          <button
            onClick={handlePublish}
            disabled={isSaving}
            className="f-mono text-label uppercase px-4 h-8 rounded bg-[#FF6B00] text-black font-semibold hover:bg-[#FF9A3C] disabled:opacity-50 transition"
          >
            Publicar
          </button>
        )}
        <button
          onClick={() => { setPreviewDevice('desktop'); setShowPreview(true); }}
          title="Preview escriptori"
          className="f-mono text-label uppercase px-2 h-8 rounded text-ink-1 hover:text-white transition"
        >
          🖥
        </button>
        <button
          onClick={() => { setPreviewDevice('mobile'); setShowPreview(true); }}
          title="Preview mòbil"
          className="f-mono text-label uppercase px-2 h-8 rounded text-ink-1 hover:text-white transition"
        >
          📱
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
          className="fixed inset-0 z-40 bg-black/90 flex flex-col items-center justify-start pt-6 overflow-auto"
          onClick={() => setShowPreview(false)}
        >
          <div className="flex justify-center gap-2 mb-4 shrink-0" onClick={(e) => e.stopPropagation()}>
            <button
              onClick={() => setPreviewDevice('desktop')}
              className={`px-3 py-1.5 rounded f-mono text-xs transition ${previewDevice === 'desktop' ? 'bg-[#FF6B00] text-black font-bold' : 'bg-white/10 text-white hover:bg-white/20'}`}
            >
              🖥 Escriptori
            </button>
            <button
              onClick={() => setPreviewDevice('mobile')}
              className={`px-3 py-1.5 rounded f-mono text-xs transition ${previewDevice === 'mobile' ? 'bg-[#FF6B00] text-black font-bold' : 'bg-white/10 text-white hover:bg-white/20'}`}
            >
              📱 Mòbil
            </button>
            <button
              onClick={() => setShowPreview(false)}
              className="px-3 py-1.5 rounded f-mono text-xs bg-white/10 text-white hover:bg-white/20 transition ml-4"
            >
              ✕ Tancar
            </button>
          </div>
          <div
            className={previewDevice === 'mobile'
              ? 'w-[390px] max-h-[844px] overflow-auto rounded-[40px] border-4 border-[#2d2d2d] shadow-2xl mb-8'
              : 'w-full max-w-4xl overflow-auto mx-8 mb-8'}
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

      {/* Share preview modal */}
      {shareOpen && (
        <div
          className="fixed inset-0 z-50 bg-black/70 flex items-center justify-center"
          onClick={() => setShareOpen(false)}
        >
          <div
            className="bg-[#13132a] border border-border-base rounded-xl p-6 w-full max-w-md shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="f-mono text-sm uppercase tracking-wider text-ink-0 mb-1">
              Previsualització per al client
            </h3>
            <p className="text-ink-3 text-xs mb-4">
              Comparteix aquest enllaç perquè el client pugui veure la landing sense publicar-la.
              Caduca als 7 dies.
            </p>
            {shareLoading ? (
              <div className="h-10 flex items-center justify-center text-ink-3 text-sm">
                Generant enllaç...
              </div>
            ) : shareUrl ? (
              <div className="flex gap-2">
                <input
                  readOnly
                  value={shareUrl}
                  className="flex-1 bg-[#0d0d1a] border border-border-base rounded px-3 py-2 text-xs text-ink-1 f-mono truncate"
                />
                <button
                  onClick={handleCopyShare}
                  className={`px-3 py-2 rounded f-mono text-xs font-semibold transition ${
                    shareCopied
                      ? 'bg-green-600 text-white'
                      : 'bg-[#FF6B00] text-black hover:bg-[#FF9A3C]'
                  }`}
                >
                  {shareCopied ? '✓ Copiat' : 'Copiar'}
                </button>
              </div>
            ) : (
              <p className="text-red-400 text-xs">Error en generar l&apos;enllaç. Torna-ho a provar.</p>
            )}
            <button
              onClick={() => setShareOpen(false)}
              className="mt-4 w-full py-2 rounded border border-border-base text-ink-2 f-mono text-xs hover:text-ink-0 transition"
            >
              Tancar
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
