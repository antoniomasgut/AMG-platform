'use client';

import type { FC } from 'react';
import { useEditorStore } from '@/store/editor';

export const PageStylesPanel: FC = () => {
  const styles = useEditorStore((s) => s.styles);
  const setStyles = useEditorStore((s) => s.setStyles);

  const fields: Array<{ key: keyof typeof styles; label: string; type: string }> = [
    { key: 'primaryColor', label: 'Color primari', type: 'color' },
    { key: 'secondaryColor', label: 'Color secundari', type: 'color' },
    { key: 'bgColor', label: 'Color de fons', type: 'color' },
    { key: 'textColor', label: 'Color text', type: 'color' },
    { key: 'fontFamily', label: 'Font', type: 'text' },
    { key: 'borderRadius', label: 'Cantonades', type: 'text' },
  ];

  return (
    <div className="p-3 space-y-3">
      <div className="f-mono text-[10px] uppercase tracking-[0.2em] text-[#64748b]">Estils globals</div>
      {fields.map(({ key, label, type }) => (
        <div key={key}>
          <label className="f-mono text-[10px] uppercase text-[#64748b] block mb-1">{label}</label>
          <div className="flex gap-2 items-center">
            {type === 'color' && (
              <input
                type="color"
                value={styles[key] as string}
                onChange={(e) => setStyles({ [key]: e.target.value })}
                className="w-8 h-8 p-0 border-0 cursor-pointer rounded"
              />
            )}
            <input
              type={type}
              value={styles[key] as string}
              onChange={(e) => setStyles({ [key]: e.target.value })}
              className="flex-1 bg-[#0d0d1a] border border-[rgba(255,107,0,0.2)] rounded p-2 text-xs text-[#e2e8f0]"
            />
          </div>
        </div>
      ))}
    </div>
  );
};
