'use client';

import type { FC } from 'react';
import { useEditorStore } from '@/store/editor';
import { BLOCK_TEMPLATES } from '@/services/factory';

export const BlockProperties: FC = () => {
  const selectedBlockId = useEditorStore((s) => s.selectedBlockId);
  const block = useEditorStore((s) => s.content.blocks.find((b) => b.id === selectedBlockId));
  const updateBlockProps = useEditorStore((s) => s.updateBlockProps);

  if (!block) {
    return (
      <div className="p-3 text-ink-3 text-xs text-center">
        Selecciona un bloc per editar les seves propietats
      </div>
    );
  }

  const tpl = BLOCK_TEMPLATES[block.type];

  const handleChange = (key: string, value: unknown) => {
    updateBlockProps(block.id, { [key]: value });
  };

  const renderField = (key: string, value: unknown) => {
    if (key === 'body') {
      return (
        <div key={key} className="mb-2">
          <label className="f-mono text-label uppercase text-ink-3 block mb-1">{key}</label>
          <textarea
            value={String(value || '')}
            onChange={(e) => handleChange(key, e.target.value)}
            className="w-full bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0 h-20 font-mono"
          />
        </div>
      );
    }
    if (key === 'items' && Array.isArray(value)) {
      const templateItems = tpl?.defaultProps?.items;
      const blankItem = Array.isArray(templateItems) && templateItems.length > 0
        ? Object.fromEntries(Object.keys(templateItems[0] as object).map((k) => [k, '']))
        : { name: '', desc: '' };
      return (
        <div key={key} className="mb-2">
          <label className="f-mono text-label uppercase text-ink-3 block mb-1">{key}</label>
          {value.map((item: Record<string, unknown>, i: number) => (
            <div key={i} className="bg-[#0d0d1a] p-2 rounded mb-1 border border-[rgba(255,107,0,0.1)]">
              {Object.entries(item).map(([k, v]) => (
                <input
                  key={k}
                  value={String(v || '')}
                  onChange={(e) => {
                    const newItems = [...value];
                    newItems[i] = { ...newItems[i], [k]: e.target.value };
                    handleChange(key, newItems);
                  }}
                  placeholder={k}
                  className="w-full bg-transparent text-xs text-ink-0 p-1 mb-1 border-b border-[rgba(255,107,0,0.1)]"
                />
              ))}
              <button
                onClick={() => {
                  const newItems = [...value];
                  newItems.splice(i, 1);
                  handleChange(key, newItems);
                }}
                className="text-red-400 text-label mt-1"
              >
                Eliminar
              </button>
            </div>
          ))}
          <button
            onClick={() => handleChange(key, [...value, blankItem])}
            className="text-accent-light text-label mt-1"
          >
            + Afegir
          </button>
        </div>
      );
    }
    if (key === 'images' && Array.isArray(value)) {
      return (
        <div key={key} className="mb-2">
          <label className="f-mono text-label uppercase text-ink-3 block mb-1">Imatges</label>
          <button className="text-accent-light text-label">Pujar imatge</button>
        </div>
      );
    }
    return (
      <div key={key} className="mb-2">
        <label className="f-mono text-label uppercase text-ink-3 block mb-1">{key}</label>
        <input
          value={String(value || '')}
          onChange={(e) => handleChange(key, e.target.value)}
          className="w-full bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0"
        />
      </div>
    );
  };

  return (
    <div className="p-3">
      <div className="f-mono text-label uppercase tracking-widest text-accent-light mb-3">
        {tpl?.label || block.type}
      </div>
      {Object.keys(tpl?.defaultProps || {}).map((key) => {
        const value = key in (block.props || {}) ? block.props[key] : (tpl?.defaultProps || {})[key];
        return renderField(key, value);
      })}
    </div>
  );
};
