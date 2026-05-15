'use client';

import { useState } from 'react';
import { AMGButton } from '@/components/ui/button';
import { I } from '@/components/ui/icons';
import type { TemplateSectionView, TemplateSectionRequest } from '@/services/templates';
import { blockTypeFromBackend } from '@/services/templates';
import { BLOCK_TEMPLATES, type BlockType } from '@/services/factory';

interface Props {
  sections: TemplateSectionView[];
  onAdd: (data: TemplateSectionRequest) => void | Promise<void>;
  onUpdate: (sectionId: string, data: TemplateSectionRequest) => void | Promise<void>;
  onRemove: (sectionId: string) => void | Promise<void>;
  loading?: boolean;
}

const BLOCK_TYPE_OPTIONS = Object.entries(BLOCK_TEMPLATES).map(([type, meta]) => ({
  value: type.replace(/-/g, '_').toUpperCase(),
  label: meta.label,
  icon: meta.icon,
}));

export function TemplateSectionEditor({ sections, onAdd, onUpdate, onRemove, loading }: Props) {
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState<string | null>(null);
  const [blockType, setBlockType] = useState('HERO');
  const [sortOrder, setSortOrder] = useState(sections.length + 1);

  const resetForm = () => {
    setBlockType('HERO');
    setSortOrder(sections.length + 1);
    setShowForm(false);
    setEditId(null);
  };

  const handleSubmit = async () => {
    const template = BLOCK_TEMPLATES[blockTypeFromBackend(blockType)];

    const sectionData: TemplateSectionRequest = {
      blockType,
      sortOrder,
      propsSchema: JSON.stringify(buildPropsSchema(blockTypeFromBackend(blockType))),
      defaultProps: JSON.stringify(template?.defaultProps ?? {}),
    };

    if (editId) {
      await onUpdate(editId, sectionData);
    } else {
      await onAdd(sectionData);
    }
    resetForm();
  };

  const handleEdit = (section: TemplateSectionView) => {
    setEditId(section.id);
    setBlockType(section.blockType);
    setSortOrder(section.sortOrder);
    setShowForm(true);
  };

  const handleMoveUp = (index: number) => {
    if (index === 0) return;
    const current = sections[index];
    const prev = sections[index - 1];
    onUpdate(current.id, { blockType: current.blockType, sortOrder: prev.sortOrder, propsSchema: current.propsSchema, defaultProps: current.defaultProps });
    onUpdate(prev.id, { blockType: prev.blockType, sortOrder: current.sortOrder, propsSchema: prev.propsSchema, defaultProps: prev.defaultProps });
  };

  const handleMoveDown = (index: number) => {
    if (index >= sections.length - 1) return;
    const current = sections[index];
    const next = sections[index + 1];
    onUpdate(current.id, { blockType: current.blockType, sortOrder: next.sortOrder, propsSchema: current.propsSchema, defaultProps: current.defaultProps });
    onUpdate(next.id, { blockType: next.blockType, sortOrder: current.sortOrder, propsSchema: next.propsSchema, defaultProps: next.defaultProps });
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="f-display font-bold text-sm">Seccions ({sections.length})</h3>
        {!showForm && (
          <AMGButton size="sm" onClick={() => setShowForm(true)}>
            <I.Plus size={14} className="mr-1" /> Afegir secció
          </AMGButton>
        )}
      </div>

      {showForm && (
        <div className="border border-border-base rounded p-4 space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="f-mono text-label uppercase text-ink-2 block mb-1">Tipus de bloc</label>
              <select
                value={blockType}
                onChange={(e) => setBlockType(e.target.value)}
                className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 focus:outline-none focus:border-[#FF6B00]"
              >
                {BLOCK_TYPE_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>{opt.icon} {opt.label}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="f-mono text-label uppercase text-ink-2 block mb-1">Ordre</label>
              <input
                type="number"
                min={1}
                value={sortOrder}
                onChange={(e) => setSortOrder(parseInt(e.target.value) || 1)}
                className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 focus:outline-none focus:border-[#FF6B00]"
              />
            </div>
          </div>
          <div className="flex gap-2 pt-1">
            <AMGButton size="sm" onClick={handleSubmit} disabled={loading}>
              {editId ? 'Actualitzar' : 'Afegir'}
            </AMGButton>
            <AMGButton size="sm" variant="ghost" onClick={resetForm}>Cancel·lar</AMGButton>
          </div>
        </div>
      )}

      {sections.length === 0 && !showForm ? (
        <div className="p-6 text-center">
          <I.Box size={24} stroke="#64748b" className="mx-auto mb-2" />
          <p className="f-mono text-xs text-ink-2">Cap secció definida</p>
        </div>
      ) : (
        <div className="space-y-2">
          {sections
            .sort((a, b) => a.sortOrder - b.sortOrder)
            .map((section, index) => {
              const blockMeta = BLOCK_TEMPLATES[blockTypeFromBackend(section.blockType)];
              return (
                <div key={section.id} className="flex items-center gap-2 border border-border-base rounded p-3">
                  <div className="flex flex-col gap-0.5">
                    <button onClick={() => handleMoveUp(index)} disabled={index === 0} className="text-ink-2 hover:text-ink-0 disabled:opacity-30">
                      <I.ArrowUp size={14} />
                    </button>
                    <button onClick={() => handleMoveDown(index)} disabled={index >= sections.length - 1} className="text-ink-2 hover:text-ink-0 disabled:opacity-30">
                      <I.ChevDown size={14} />
                    </button>
                  </div>
                  <span className="f-mono text-[10px] text-ink-3 w-5">{section.sortOrder}</span>
                  <span className="text-sm">{blockMeta?.icon ?? '?'}</span>
                  <span className="text-sm text-ink-0 flex-1">{blockMeta?.label ?? section.blockType}</span>
                  <span className="f-mono text-[10px] text-ink-3 uppercase">{section.blockType}</span>
                  <button onClick={() => handleEdit(section)} className="text-ink-2 hover:text-accent-light p-1">
                    <I.Edit size={14} />
                  </button>
                  <button onClick={() => onRemove(section.id)} className="text-ink-2 hover:text-warning p-1">
                    <I.Trash size={14} />
                  </button>
                </div>
              );
            })}
        </div>
      )}
    </div>
  );
}

function buildPropsSchema(blockType: BlockType): Record<string, { type: string; label: string }> {
  const defaults = BLOCK_TEMPLATES[blockType]?.defaultProps ?? {};
  return Object.fromEntries(
    Object.keys(defaults).map((key) => [
      key,
      { type: typeof defaults[key] === 'string' ? 'text' : typeof defaults[key] === 'number' ? 'number' : typeof defaults[key] === 'boolean' ? 'boolean' : 'text', label: key },
    ])
  );
}
