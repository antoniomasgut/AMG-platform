'use client';

import type { FC } from 'react';
import { useEditorStore } from '@/store/editor';

const GOOGLE_FONTS = [
  'Inter, sans-serif',
  'Roboto, sans-serif',
  'Open Sans, sans-serif',
  'Lato, sans-serif',
  'Montserrat, sans-serif',
  'Poppins, sans-serif',
  'Playfair Display, serif',
  'Merriweather, serif',
  'Source Serif 4, serif',
  'DM Sans, sans-serif',
];

const COLOR_FIELDS: Array<{ key: 'primaryColor' | 'secondaryColor' | 'bgColor' | 'textColor'; label: string }> = [
  { key: 'primaryColor', label: 'Color primari' },
  { key: 'secondaryColor', label: 'Color secundari' },
  { key: 'bgColor', label: 'Color de fons' },
  { key: 'textColor', label: 'Color text' },
];

export const PageStylesPanel: FC = () => {
  const styles = useEditorStore((s) => s.styles);
  const setStyles = useEditorStore((s) => s.setStyles);

  return (
    <div className="p-3 space-y-4">
      <div className="f-mono text-label uppercase tracking-widest text-ink-3">Estils globals</div>

      {/* Color fields */}
      {COLOR_FIELDS.map(({ key, label }) => (
        <div key={key}>
          <label className="f-mono text-label uppercase text-ink-3 block mb-1">{label}</label>
          <div className="flex gap-2 items-center">
            <input
              type="color"
              value={styles[key]}
              onChange={(e) => setStyles({ [key]: e.target.value })}
              className="w-8 h-8 p-0 border-0 cursor-pointer rounded shrink-0"
            />
            <input
              type="text"
              value={styles[key]}
              onChange={(e) => setStyles({ [key]: e.target.value })}
              className="flex-1 bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0 font-mono"
            />
          </div>
        </div>
      ))}

      {/* Font selector */}
      <div>
        <label className="f-mono text-label uppercase text-ink-3 block mb-1">Font</label>
        <select
          value={styles.fontFamily}
          onChange={(e) => setStyles({ fontFamily: e.target.value })}
          className="w-full bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0"
        >
          {GOOGLE_FONTS.map((f) => (
            <option key={f} value={f}>{f.split(',')[0]}</option>
          ))}
        </select>
        <p className="mt-1 f-mono text-[9px] text-ink-3" style={{ fontFamily: styles.fontFamily }}>
          Abc 123 — previsualització
        </p>
      </div>

      {/* Border radius */}
      <div>
        <label className="f-mono text-label uppercase text-ink-3 block mb-1">Cantonades</label>
        <div className="flex gap-2 items-center">
          <input
            type="range"
            min={0}
            max={24}
            value={parseInt(styles.borderRadius) || 0}
            onChange={(e) => setStyles({ borderRadius: `${e.target.value}px` })}
            className="flex-1 accent-[#FF6B00]"
          />
          <span className="f-mono text-label text-ink-1 w-10 text-right">{styles.borderRadius}</span>
        </div>
      </div>
    </div>
  );
};
