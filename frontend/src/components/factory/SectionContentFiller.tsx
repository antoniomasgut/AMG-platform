'use client';

import { useState, useEffect } from 'react';
import { AMGButton } from '@/components/ui/button';
import { I } from '@/components/ui/icons';
import { blockTypeFromBackend, type TemplateSectionView } from '@/services/templates';
import { BLOCK_TEMPLATES } from '@/services/factory';

interface SectionFillerProps {
  section: TemplateSectionView;
  values: Record<string, unknown>;
  onChange: (values: Record<string, unknown>) => void;
}

function parseSchema(schemaStr: string): Record<string, { type: string; label: string }> {
  try {
    return JSON.parse(schemaStr);
  } catch {
    return {};
  }
}

function parseDefaultProps(propsStr: string): Record<string, unknown> {
  try {
    return JSON.parse(propsStr);
  } catch {
    return {};
  }
}

function ArrayFieldEditor({
  label, value, onChange,
}: {
  label: string;
  value: unknown[];
  onChange: (v: unknown[]) => void;
}) {
  const addItem = () => onChange([...value, {}]);
  const updateItem = (index: number, item: unknown) => {
    const next = [...value];
    next[index] = item;
    onChange(next);
  };
  const removeItem = (index: number) => {
    onChange(value.filter((_, i) => i !== index));
  };

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <label className="f-mono text-label uppercase text-ink-2 text-xs">{label}</label>
        <button type="button" onClick={addItem} className="text-accent-light hover:text-accent text-xs flex items-center gap-1">
          <I.Plus size={12} /> Afegir
        </button>
      </div>
      {value.length === 0 ? (
        <p className="f-mono text-[10px] text-ink-3">Cap element</p>
      ) : (
        <div className="space-y-2">
          {value.map((item, i) => (
            <div key={i} className="flex items-start gap-2 border border-border-base rounded p-2">
              <div className="flex-1 space-y-1">
                {typeof item === 'object' && item !== null
                  ? Object.entries(item as Record<string, unknown>).map(([k, v]) => (
                      <div key={k} className="flex items-center gap-2">
                        <span className="f-mono text-[9px] text-ink-3 w-14 uppercase">{k}</span>
                        <input
                          type="text"
                          value={String(v ?? '')}
                          onChange={(e) => updateItem(i, { ...(item as Record<string, unknown>), [k]: e.target.value })}
                          className="flex-1 bg-[#0d0d1a] border border-border-base px-2 h-7 text-xs text-ink-0"
                        />
                      </div>
                    ))
                  : <input
                      type="text"
                      value={String(item ?? '')}
                      onChange={(e) => updateItem(i, e.target.value)}
                      className="w-full bg-[#0d0d1a] border border-border-base px-2 h-7 text-xs text-ink-0"
                    />
                }
              </div>
              <button type="button" onClick={() => removeItem(i)} className="text-ink-2 hover:text-warning mt-1">
                <I.X size={12} />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function FieldInput({
  type, label, value, onChange,
}: {
  type: string;
  label: string;
  value: unknown;
  onChange: (v: unknown) => void;
}) {
  const strValue = String(value ?? '');

  switch (type) {
    case 'richtext':
      return (
        <div>
          <label className="f-mono text-label uppercase text-ink-2 block mb-1 text-xs">{label}</label>
          <textarea
            rows={4}
            value={strValue}
            onChange={(e) => onChange(e.target.value)}
            className="w-full bg-[#0d0d1a] border border-border-base px-3 py-2 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]"
          />
        </div>
      );

    case 'number':
      return (
        <div>
          <label className="f-mono text-label uppercase text-ink-2 block mb-1 text-xs">{label}</label>
          <input
            type="number"
            value={strValue}
            onChange={(e) => onChange(Number(e.target.value))}
            className="w-full bg-[#0d0d1a] border border-border-base px-3 h-9 text-sm text-ink-0 focus:outline-none focus:border-[#FF6B00]"
          />
        </div>
      );

    case 'boolean':
      return (
        <label className="flex items-center gap-2 cursor-pointer">
          <input
            type="checkbox"
            checked={Boolean(value)}
            onChange={(e) => onChange(e.target.checked)}
            className="accent-[#FF6B00]"
          />
          <span className="f-mono text-label uppercase text-ink-2 text-xs">{label}</span>
        </label>
      );

    default:
      return (
        <div>
          <label className="f-mono text-label uppercase text-ink-2 block mb-1 text-xs">{label}</label>
          <input
            type={type === 'url' ? 'url' : 'text'}
            value={strValue}
            placeholder={label}
            onChange={(e) => onChange(e.target.value)}
            className="w-full bg-[#0d0d1a] border border-border-base px-3 h-9 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]"
          />
        </div>
      );
  }
}

export function SectionContentFiller({ section, values, onChange }: SectionFillerProps) {
  const schema = parseSchema(section.propsSchema);
  const defaults = parseDefaultProps(section.defaultProps);
  const blockMeta = BLOCK_TEMPLATES[blockTypeFromBackend(section.blockType)];

  // Initialize with defaults merged with any existing values
  useEffect(() => {
    const merged: Record<string, unknown> = {};
    for (const key of Object.keys(schema)) {
      merged[key] = key in values ? values[key] : defaults[key] ?? '';
    }
    // Only call onChange if values differ
    const hasChanges = Object.keys(merged).some((k) => merged[k] !== values[k]);
    if (hasChanges && Object.keys(values).length === 0) {
      onChange(merged);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const fieldKeys = Object.keys(schema);
  if (fieldKeys.length === 0) return null;

  return (
    <div className="border border-border-base rounded p-4 space-y-3">
      <div className="flex items-center gap-2 mb-2">
        <span className="text-sm">{blockMeta?.icon ?? '?'}</span>
        <span className="f-display font-bold text-sm">{blockMeta?.label ?? section.blockType}</span>
        <span className="f-mono text-[10px] text-ink-3 uppercase ml-auto">{section.blockType}</span>
      </div>
      {fieldKeys.map((key) => {
        const field = schema[key];
        const val = key in values ? values[key] : defaults[key] ?? '';

        if (field.type === 'array') {
          return (
            <ArrayFieldEditor
              key={key}
              label={field.label}
              value={Array.isArray(val) ? val : []}
              onChange={(v) => onChange({ ...values, [key]: v })}
            />
          );
        }

        return (
          <FieldInput
            key={key}
            type={field.type}
            label={field.label}
            value={val}
            onChange={(v) => onChange({ ...values, [key]: v })}
          />
        );
      })}
    </div>
  );
}

interface SectionContentFillerListProps {
  sections: TemplateSectionView[];
  filledSections: Record<string, Record<string, unknown>>;
  onSectionChange: (blockType: string, values: Record<string, unknown>) => void;
}

export function SectionContentFillerList({ sections, filledSections, onSectionChange }: SectionContentFillerListProps) {
  if (sections.length === 0) {
    return (
      <div className="p-8 text-center">
        <I.Box size={28} stroke="#64748b" className="mx-auto mb-3" />
        <p className="f-mono text-xs text-ink-2">Aquesta plantilla no té seccions</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {sections.map((section) => (
        <SectionContentFiller
          key={section.id}
          section={section}
          values={filledSections[section.blockType] ?? {}}
          onChange={(values) => onSectionChange(section.blockType, values)}
        />
      ))}
    </div>
  );
}
