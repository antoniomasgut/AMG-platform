'use client';

import { useState, useEffect, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import Image from 'next/image';
import {
  getTemplate, updateTemplate, previewTemplate, listVersions, restoreVersion, applyAiOperations,
  type TemplateResponse, type LayoutBlock, type DocumentType,
  BLOCK_LIBRARY, defaultLayout,
} from '@/services/documents';
import { listAssets, type AssetResponse } from '@/services/assets';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { IconSet } from '@/components/ui/icons';

const BLOCK_TYPE_LABELS: Record<string, string> = {};
for (const cat of BLOCK_LIBRARY) {
  for (const b of cat.blocks) {
    BLOCK_TYPE_LABELS[b.id] = b.label;
  }
}

function AiModal({
  open, onClose, onApply, templateId,
}: {
  open: boolean;
  onClose: () => void;
  onApply: (prompt: string) => Promise<void>;
  templateId: string;
}) {
  const [prompt, setPrompt] = useState('');
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();

  if (!open) return null;

  const handleApply = async () => {
    if (!prompt.trim()) return;
    setLoading(true);
    try {
      await onApply(prompt.trim());
      toast('success', 'Operació IA aplicada');
      onClose();
    } catch {
      toast('error', 'Error aplicant operació IA');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-lg p-6 space-y-4" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">IA - Modificar plantilla</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><IconSet.X size={18} /></button>
        </div>
        <p className="text-sm text-ink-1">Descriu amb text què vols canviar de la plantilla (ex: &ldquo;Posa el logo a l&apos;esquerra&rdquo;).</p>
        <textarea
          value={prompt}
          onChange={(e) => setPrompt(e.target.value)}
          placeholder="Ex: Afegeix una secció de signatura al final"
          className="w-full h-24 bg-[#0d0d1a] border border-border-base rounded px-3 py-2 text-sm text-ink-0 placeholder:text-ink-3 resize-none"
        />
        <div className="flex gap-3">
          <AMGButton onClick={handleApply} disabled={loading || !prompt.trim()}>
            {loading ? 'Aplicant...' : 'Aplicar'}
          </AMGButton>
          <AMGButton variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
        </div>
      </div>
    </div>
  );
}

function VersionHistoryModal({
  open, onClose, templateId, onRestore,
}: {
  open: boolean;
  onClose: () => void;
  templateId: string;
  onRestore: (version: number) => void;
}) {
  const { data: versions } = useQuery({
    queryKey: ['template-versions', templateId],
    queryFn: () => listVersions(templateId),
    enabled: open && !!templateId,
  });

  if (!open) return null;

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-lg p-6 space-y-4" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Historial de versions</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><IconSet.X size={18} /></button>
        </div>
        {!versions ? (
          <div className="flex justify-center py-8">
            <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
          </div>
        ) : versions.length === 0 ? (
          <p className="text-sm text-ink-2">Sense versions anteriors</p>
        ) : (
          <div className="space-y-2 max-h-64 overflow-y-auto">
            {versions.map((v) => (
              <div key={v.id} className="flex items-center justify-between p-3 bg-[#0d0d1a] rounded">
                <div>
                  <span className="f-mono text-xs text-accent-light">v{v.version}</span>
                  {v.notes && <p className="text-xs text-ink-2 mt-0.5">{v.notes}</p>}
                  <p className="text-[10px] text-ink-3 mt-0.5">{new Date(v.createdAt).toLocaleString()}</p>
                </div>
                <AMGButton size="sm" variant="secondary" onClick={() => onRestore(v.version)}>
                  Restaurar
                </AMGButton>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function ImagePickerModal({
  open, onClose, onSelect, tenantId,
}: {
  open: boolean;
  onClose: () => void;
  onSelect: (url: string) => void;
  tenantId: string;
}) {
  const { data: assets = [] } = useQuery({
    queryKey: ['assets', tenantId],
    queryFn: () => listAssets(tenantId),
    enabled: open && !!tenantId,
  });

  if (!open) return null;

  const images = (assets as AssetResponse[]).filter((a) => a.mimeType.startsWith('image/'));

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-2xl p-6 space-y-4 max-h-[80vh] flex flex-col" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between shrink-0">
          <div className="f-display font-bold text-base">Triar imatge</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><IconSet.X size={18} /></button>
        </div>
        {images.length === 0 ? (
          <div className="text-center py-8">
            <IconSet.Image size={28} stroke="#64748b" className="mx-auto mb-3" />
            <p className="text-sm text-ink-2">Cap imatge pujada. Ves a Assets per pujar-ne.</p>
          </div>
        ) : (
          <div className="grid grid-cols-3 sm:grid-cols-4 gap-2 overflow-y-auto flex-1">
            {images.map((asset) => (
              <button
                key={asset.id}
                onClick={() => { onSelect(asset.url); onClose(); }}
                className="aspect-square relative rounded overflow-hidden border border-border-base hover:border-accent transition-colors"
              >
                <Image
                  src={asset.thumbnailUrl ?? asset.url}
                  alt={asset.originalName}
                  fill
                  sizes="150px"
                  style={{ objectFit: 'cover' }}
                />
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default function EditDocumentTemplatePage() {
  const params = useParams();
  const router = useRouter();
  const { user } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();
  const id = params.id as string;

  const [name, setName] = useState('');
  const [blocks, setBlocks] = useState<LayoutBlock[]>([]);
  const [selectedBlockId, setSelectedBlockId] = useState<string | null>(null);
  const [dirty, setDirty] = useState(false);
  const [showAi, setShowAi] = useState(false);
  const [showVersions, setShowVersions] = useState(false);
  const [showImagePicker, setShowImagePicker] = useState(false);
  const [htmlPreview, setHtmlPreview] = useState<string | null>(null);

  const { data: template, isLoading } = useQuery({
    queryKey: ['document-template', id],
    queryFn: () => getTemplate(id),
    enabled: !!id,
  });

  useEffect(() => {
    if (template) {
      setName(template.name);
      try {
        const parsed = JSON.parse(template.layout);
        setBlocks(Array.isArray(parsed) ? parsed : defaultLayout());
      } catch {
        setBlocks(defaultLayout());
      }
      setDirty(false);
    }
  }, [template]);

  const { mutate: doSave, isPending: saving } = useMutation({
    mutationFn: (data: { name: string; layout: string }) =>
      updateTemplate(id, { name: data.name, documentType: template!.documentType, layout: data.layout }),
    onSuccess: () => {
      toast('success', 'Plantilla desada');
      setDirty(false);
      qc.invalidateQueries({ queryKey: ['document-template', id] });
    },
    onError: () => toast('error', 'Error desant la plantilla'),
  });

  const { mutate: doRestoreVersion } = useMutation({
    mutationFn: (version: number) => restoreVersion(id, version),
    onSuccess: () => {
      toast('success', 'Versió restaurada');
      qc.invalidateQueries({ queryKey: ['document-template', id] });
      qc.invalidateQueries({ queryKey: ['template-versions', id] });
      setShowVersions(false);
    },
    onError: () => toast('error', 'Error restaurant la versió'),
  });

  const applyAi = useCallback(async (prompt: string) => {
    const result = await applyAiOperations(id, prompt);
    if (!result.operations) return;
    const newBlocks = [...blocks];
    for (const op of result.operations) {
      if (op.action === 'add_block' && op.blockId) {
        newBlocks.push({
          id: op.blockId,
          type: op.blockId,
          x: op.x ?? 0, y: op.y ?? 0, w: op.w ?? 4, h: op.h ?? 2,
          visible: true,
          config: { style: {} },
          dataBinding: null,
        });
      } else if (op.action === 'remove_block' && op.blockId) {
        const idx = newBlocks.findIndex(b => b.id === op.blockId);
        if (idx >= 0) newBlocks.splice(idx, 1);
      } else if ((op.action === 'move' || op.action === 'resize') && op.blockId) {
        const block = newBlocks.find(b => b.id === op.blockId);
        if (block) {
          if (op.x !== undefined) block.x = op.x;
          if (op.y !== undefined) block.y = op.y;
          if (op.w !== undefined) block.w = op.w;
          if (op.h !== undefined) block.h = op.h;
        }
      } else if (op.action === 'update_style' && op.blockId && op.style) {
        const block = newBlocks.find(b => b.id === op.blockId);
        if (block) {
          block.config.style = { ...block.config.style, ...op.style };
        }
      } else if (op.action === 'set_visibility' && op.blockId) {
        const block = newBlocks.find(b => b.id === op.blockId);
        if (block) block.visible = op.visible ?? true;
      }
    }
    setBlocks(newBlocks);
    setDirty(true);
  }, [id, blocks]);

  const addBlock = (type: string) => {
    const maxY = blocks.reduce((max, b) => Math.max(max, b.y + b.h), 0);
    const newBlock: LayoutBlock = {
      id: `${type}_${Date.now()}`,
      type,
      x: 0, y: maxY, w: 4, h: 2,
      visible: true,
      config: { style: {} },
      dataBinding: null,
    };
    setBlocks([...blocks, newBlock]);
    setSelectedBlockId(newBlock.id);
    setDirty(true);
  };

  const removeBlock = (blockId: string) => {
    setBlocks(blocks.filter(b => b.id !== blockId));
    if (selectedBlockId === blockId) setSelectedBlockId(null);
    setDirty(true);
  };

  const toggleVisibility = (blockId: string) => {
    setBlocks(blocks.map(b => b.id === blockId ? { ...b, visible: !b.visible } : b));
    setDirty(true);
  };

  const updateBlockConfig = (blockId: string, updates: Partial<LayoutBlock>) => {
    setBlocks(blocks.map(b => b.id === blockId ? { ...b, ...updates } : b));
    setDirty(true);
  };

  const updateBlockStyle = (blockId: string, style: Record<string, unknown>) => {
    setBlocks(blocks.map(b => {
      if (b.id !== blockId) return b;
      return { ...b, config: { ...b.config, style: { ...b.config.style, ...style } } };
    }));
    setDirty(true);
  };

  const updateDataBinding = (blockId: string, source: string, field: string) => {
    setBlocks(blocks.map(b => {
      if (b.id !== blockId) return b;
      return { ...b, dataBinding: source ? { source, field } : null };
    }));
    setDirty(true);
  };

  const save = () => {
    doSave({ name, layout: JSON.stringify(blocks) });
  };

  const loadPreview = async () => {
    try {
      const html = await previewTemplate(id);
      setHtmlPreview(html);
    } catch {
      toast('error', 'Error carregant la previsualització');
    }
  };

  const selectedBlock = blocks.find(b => b.id === selectedBlockId);

  if (isLoading) {
    return (
      <PortalShell breadcrumb="admin · documents · edit">
        <div className="flex justify-center py-20">
          <span className="w-5 h-5 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
        </div>
      </PortalShell>
    );
  }

  if (!template) {
    return (
      <PortalShell breadcrumb="admin · documents · edit">
        <div className="p-8 text-center">
          <IconSet.FileText size={28} stroke="#64748b" className="mx-auto mb-3" />
          <div className="f-display font-bold text-sm">Plantilla no trobada</div>
        </div>
      </PortalShell>
    );
  }

  return (
    <PortalShell breadcrumb={`admin · documents · ${template.name}`} backHref="/portal/admin/documents">
      <div className="flex flex-col h-[calc(100dvh-3.5rem)]">
        {/* Top toolbar */}
        <div className="flex items-center justify-between px-4 sm:px-6 py-3 border-b border-border-base shrink-0">
          <div className="flex items-center gap-3">
            <input
              value={name}
              onChange={(e) => { setName(e.target.value); setDirty(true); }}
              className="bg-transparent border-none f-display font-bold text-lg text-ink-0 outline-none w-64"
            />
            <AMGBadge tone="info">{template.documentType}</AMGBadge>
          </div>
          <div className="flex items-center gap-2">
            <AMGButton size="sm" variant="ghost" onClick={() => setShowVersions(true)}>
              <IconSet.Clock size={14} />
            </AMGButton>
            <AMGButton size="sm" variant="ghost" onClick={() => window.open(`/portal/admin/documents/${id}/preview`, '_blank')}>
              <IconSet.Eye size={14} />
            </AMGButton>
            <AMGButton size="sm" variant="secondary" onClick={() => setShowAi(true)}>
              <IconSet.Sparkles size={14} />
              IA
            </AMGButton>
            <AMGButton size="sm" disabled={!dirty || saving} onClick={save}>
              {saving ? 'Desant...' : 'Desar'}
            </AMGButton>
          </div>
        </div>

        {/* Main editor area */}
        <div className="flex flex-1 overflow-hidden">
          {/* Left panel: palette + config */}
          <div className="w-64 lg:w-72 border-r border-border-base overflow-y-auto shrink-0 bg-[#0d0d1a]">
            {selectedBlock ? (
              <div className="p-4 space-y-4">
                <div className="flex items-center justify-between">
                  <div className="f-display font-bold text-sm">{BLOCK_TYPE_LABELS[selectedBlock.type] || selectedBlock.type}</div>
                  <button onClick={() => setSelectedBlockId(null)} className="text-ink-2 hover:text-ink-0">
                    <IconSet.X size={14} />
                  </button>
                </div>

                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="f-mono text-[10px] uppercase text-ink-2">Visible</span>
                    <button
                      onClick={() => toggleVisibility(selectedBlock.id)}
                      className={`w-8 h-5 rounded-full transition-colors ${selectedBlock.visible ? 'bg-[#FF6B00]' : 'bg-ink-3'}`}
                    >
                      <div className={`w-3.5 h-3.5 bg-white rounded-full transition-transform ${selectedBlock.visible ? 'translate-x-4' : 'translate-x-0.5'}`} />
                    </button>
                  </div>

                  <div>
                    <label className="f-mono text-[10px] uppercase text-ink-2 block mb-1">Posició X</label>
                    <input
                      type="number" min={0} max={11}
                      value={selectedBlock.x}
                      onChange={(e) => updateBlockConfig(selectedBlock.id, { x: parseInt(e.target.value) || 0 })}
                      className="w-full bg-[#0d0d1a] border border-border-base rounded px-2 py-1 text-xs text-ink-0"
                    />
                  </div>
                  <div>
                    <label className="f-mono text-[10px] uppercase text-ink-2 block mb-1">Posició Y</label>
                    <input
                      type="number" min={0}
                      value={selectedBlock.y}
                      onChange={(e) => updateBlockConfig(selectedBlock.id, { y: parseInt(e.target.value) || 0 })}
                      className="w-full bg-[#0d0d1a] border border-border-base rounded px-2 py-1 text-xs text-ink-0"
                    />
                  </div>
                  <div>
                    <label className="f-mono text-[10px] uppercase text-ink-2 block mb-1">Amplada (cols)</label>
                    <input
                      type="number" min={1} max={12}
                      value={selectedBlock.w}
                      onChange={(e) => updateBlockConfig(selectedBlock.id, { w: parseInt(e.target.value) || 1 })}
                      className="w-full bg-[#0d0d1a] border border-border-base rounded px-2 py-1 text-xs text-ink-0"
                    />
                  </div>
                  <div>
                    <label className="f-mono text-[10px] uppercase text-ink-2 block mb-1">Altura (rows)</label>
                    <input
                      type="number" min={1}
                      value={selectedBlock.h}
                      onChange={(e) => updateBlockConfig(selectedBlock.id, { h: parseInt(e.target.value) || 1 })}
                      className="w-full bg-[#0d0d1a] border border-border-base rounded px-2 py-1 text-xs text-ink-0"
                    />
                  </div>

                  <div className="border-t border-border-base pt-3">
                    <div className="f-mono text-[10px] uppercase text-ink-2 mb-2">Estils</div>
                    <div>
                      <label className="f-mono text-[10px] uppercase text-ink-2 block mb-1">Mida font</label>
                      <input
                        type="number"
                        value={(selectedBlock.config.style.fontSize as number) || 12}
                        onChange={(e) => updateBlockStyle(selectedBlock.id, { fontSize: parseInt(e.target.value) || 12 })}
                        className="w-full bg-[#0d0d1a] border border-border-base rounded px-2 py-1 text-xs text-ink-0"
                      />
                    </div>
                    <div className="mt-2">
                      <label className="f-mono text-[10px] uppercase text-ink-2 block mb-1">Alineació</label>
                      <div className="flex gap-1">
                        {(['left', 'center', 'right'] as const).map(a => (
                          <button
                            key={a}
                            onClick={() => updateBlockStyle(selectedBlock.id, { alignment: a })}
                            className={`px-2 py-1 text-[10px] uppercase rounded border transition-colors ${
                              (selectedBlock.config.style.alignment as string) === a
                                ? 'border-[#FF6B00] text-[#FF6B00]'
                                : 'border-border-base text-ink-2'
                            }`}
                          >
                            {a}
                          </button>
                        ))}
                      </div>
                    </div>
                    <div className="mt-2">
                      <label className="f-mono text-[10px] uppercase text-ink-2 block mb-1">Negreta</label>
                      <button
                        onClick={() => {
                          const curr = selectedBlock.config.style.fontWeight as string;
                          updateBlockStyle(selectedBlock.id, { fontWeight: curr === 'bold' ? 'normal' : 'bold' });
                        }}
                        className={`px-2 py-1 text-[10px] font-bold uppercase rounded border transition-colors ${
                          selectedBlock.config.style.fontWeight === 'bold'
                            ? 'border-[#FF6B00] text-[#FF6B00]'
                            : 'border-border-base text-ink-2'
                        }`}
                      >
                        B
                      </button>
                    </div>
                  </div>

                  {selectedBlock.type === 'image' && (
                    <div className="border-t border-border-base pt-3 space-y-2">
                      <div className="f-mono text-[10px] uppercase text-ink-2">Imatge</div>
                      <div className="flex gap-1">
                        <input
                          value={(selectedBlock.config.src as string) || ''}
                          onChange={(e) => updateBlockConfig(selectedBlock.id, { config: { ...selectedBlock.config, src: e.target.value } })}
                          placeholder="URL de la imatge"
                          className="flex-1 bg-[#0d0d1a] border border-border-base rounded px-2 py-1 text-xs text-ink-0 placeholder:text-ink-3"
                        />
                        <button
                          onClick={() => setShowImagePicker(true)}
                          className="px-2 py-1 text-[10px] bg-[rgba(255,107,0,0.1)] text-accent border border-accent/30 rounded hover:bg-[rgba(255,107,0,0.2)] transition-colors"
                        >
                          Assets
                        </button>
                      </div>
                      {(selectedBlock.config.src as string) && (
                        <div className="relative aspect-video bg-[#0d0d1a] rounded overflow-hidden">
                          <Image
                            src={selectedBlock.config.src as string}
                            alt="preview"
                            fill
                            style={{ objectFit: 'contain' }}
                          />
                        </div>
                      )}
                    </div>
                  )}

                  <div className="border-t border-border-base pt-3">
                    <div className="f-mono text-[10px] uppercase text-ink-2 mb-2">Data Binding</div>
                    <div>
                      <label className="f-mono text-[10px] uppercase text-ink-2 block mb-1">Font</label>
                      <select
                        value={selectedBlock.dataBinding?.source || ''}
                        onChange={(e) => {
                          const s = e.target.value;
                          updateDataBinding(selectedBlock.id, s, s ? selectedBlock.dataBinding?.field || '' : '');
                        }}
                        className="w-full bg-[#0d0d1a] border border-border-base rounded px-2 py-1 text-xs text-ink-0"
                      >
                        <option value="">Cap</option>
                        <option value="company">Empresa</option>
                        <option value="customer">Client</option>
                        <option value="document">Document</option>
                        <option value="variables">Variables</option>
                        <option value="calculated">Calculats</option>
                      </select>
                    </div>
                    {selectedBlock.dataBinding?.source && (
                      <div className="mt-2">
                        <label className="f-mono text-[10px] uppercase text-ink-2 block mb-1">Camp</label>
                        <input
                          value={selectedBlock.dataBinding.field}
                          onChange={(e) => updateDataBinding(selectedBlock.id, selectedBlock.dataBinding!.source, e.target.value)}
                          className="w-full bg-[#0d0d1a] border border-border-base rounded px-2 py-1 text-xs text-ink-0"
                        />
                      </div>
                    )}
                  </div>

                  <div className="border-t border-border-base pt-3">
                    <AMGButton size="sm" variant="danger" onClick={() => removeBlock(selectedBlock.id)}>
                      <IconSet.Trash size={14} />
                      Eliminar bloc
                    </AMGButton>
                  </div>
                </div>
              </div>
            ) : (
              <div className="p-4 space-y-4">
                <div className="f-display font-bold text-sm">Blocs disponibles</div>
                <div className="flex gap-2 flex-wrap">
                  <AMGButton size="sm" variant="ghost" onClick={() => setShowAi(true)}>
                    <IconSet.Sparkles size={14} />
                    Pregunta a la IA
                  </AMGButton>
                </div>
                {BLOCK_LIBRARY.map((cat) => (
                  <div key={cat.category}>
                    <div className="f-mono text-[10px] uppercase text-ink-3 mb-1.5">{cat.category}</div>
                    <div className="space-y-1">
                      {cat.blocks.map((b) => (
                        <button
                          key={b.id}
                          onClick={() => addBlock(b.id)}
                          className="w-full text-left px-3 py-2 rounded text-xs text-ink-1 hover:text-ink-0 hover:bg-[rgba(255,255,255,0.03)] border border-border-base transition-colors"
                        >
                          + {b.label}
                        </button>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Canvas + Preview */}
          <div className="flex-1 flex flex-col">
            {/* Canvas tool switcher (tabs) */}
            <div className="flex gap-1 px-4 pt-3 pb-2 border-b border-border-base bg-[#0d0d1a]">
              <button
                onClick={() => setHtmlPreview(null)}
                className={`px-3 py-1 text-[10px] uppercase f-mono rounded ${
                  !htmlPreview ? 'bg-[rgba(255,107,0,0.1)] text-[#FF6B00]' : 'text-ink-2 hover:text-ink-1'
                }`}
              >
                Editor visual
              </button>
              <button
                onClick={loadPreview}
                className={`px-3 py-1 text-[10px] uppercase f-mono rounded ${
                  htmlPreview ? 'bg-[rgba(255,107,0,0.1)] text-[#FF6B00]' : 'text-ink-2 hover:text-ink-1'
                }`}
              >
                Previsualització HTML
              </button>
            </div>

            <div className="flex-1 overflow-auto p-4 sm:p-6 bg-[#0d0d1a]">
              {htmlPreview ? (
                <div className="max-w-[800px] mx-auto bg-white text-black rounded shadow-lg overflow-hidden">
                  <iframe
                    srcDoc={htmlPreview}
                    className="w-full min-h-[600px]"
                    title="Preview"
                    sandbox="allow-same-origin"
                  />
                </div>
              ) : (
                <div className="max-w-[800px] mx-auto bg-white rounded shadow-lg">
                  <div className="p-6 sm:p-8">
                    <div className="text-xs text-gray-400 mb-3 f-mono">Document preview · {blocks.length} blocs</div>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(12, 1fr)', gap: '4px' }}>
                      {blocks.filter(b => b.visible).map((block) => (
                        <div
                          key={block.id}
                          onClick={() => setSelectedBlockId(block.id)}
                          style={{
                            gridColumn: `${block.x + 1} / span ${block.w}`,
                            gridRow: `${block.y + 1} / span ${block.h}`,
                            fontSize: (block.config.style.fontSize as number) || 12,
                            fontWeight: (block.config.style.fontWeight as string) || 'normal',
                            textAlign: (block.config.style.alignment as 'left' | 'center' | 'right') || 'left',
                          }}
                          className={`relative border transition-colors p-2 cursor-pointer text-black ${
                            selectedBlockId === block.id
                              ? 'border-[#FF6B00] ring-1 ring-[#FF6B00]'
                              : 'border-gray-200 hover:border-gray-400'
                          }`}
                        >
                          <span className="text-[10px] text-gray-500 f-mono opacity-50">
                            [{block.type}]
                          </span>
                          <div className="text-xs mt-1" style={{ fontSize: (block.config.style.fontSize as number) || 12 }}>
                            {block.type === 'image' && block.config.src ? (
                              <img
                                src={block.config.src as string}
                                alt="imatge"
                                style={{ maxWidth: '100%', maxHeight: '80px', objectFit: 'contain' }}
                              />
                            ) : renderBlockPlaceholder(block)}
                          </div>
                          <button
                            onClick={(e) => { e.stopPropagation(); removeBlock(block.id); }}
                            className="absolute -top-2 -right-2 w-4 h-4 bg-red-500 rounded-full flex items-center justify-center opacity-0 hover:opacity-100 transition-opacity"
                          >
                            <IconSet.X size={8} />
                          </button>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      <AiModal open={showAi} onClose={() => setShowAi(false)} onApply={applyAi} templateId={id} />
      <VersionHistoryModal open={showVersions} onClose={() => setShowVersions(false)} templateId={id} onRestore={(v) => doRestoreVersion(v)} />
      <ImagePickerModal
        open={showImagePicker}
        onClose={() => setShowImagePicker(false)}
        tenantId={user?.tenantId ?? ''}
        onSelect={(url) => {
          if (selectedBlockId) {
            const block = blocks.find(b => b.id === selectedBlockId);
            if (block) updateBlockConfig(selectedBlockId, { config: { ...block.config, src: url } });
          }
        }}
      />
    </PortalShell>
  );
}

function renderBlockPlaceholder(block: LayoutBlock): string {
  if (block.type === 'image' && block.config.src) return '';
  const map: Record<string, string> = {
    logo: '[Logo empresa]',
    company_info: 'Nom Empresa SL · CIF B-12345678',
    customer_info: 'Client: Joan Pérez',
    document_title: 'Títol del document',
    document_number: 'DOC-2026-0001',
    document_date: '01/06/2026',
    document_validity: '15/06/2026',
    products_table: '[Taula de productes]',
    services_table: '[Taula de serveis]',
    subtotal: 'Subtotal: 0.00 €',
    discount: 'Descompte: 0%',
    tax: 'IVA 21%: 0.00 €',
    total: 'Total: 0.00 €',
    text: 'Text lliure',
    rich_text: 'Text enriquit',
    image: '[Imatge]',
    separator: '────────────────────',
    page_break: '[Salt de pàgina]',
    terms: 'Termes i condicions del document...',
    conditions: 'Condicions generals...',
    signature: '[Signatura]',
    qr_code: '[Codi QR]',
    barcode: '[Codi barres]',
    payment_link: '[Enllaç de pagament]',
  };
  return map[block.type] || `[${block.type}]`;
}
