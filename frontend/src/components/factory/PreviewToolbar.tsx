'use client';

import type { FC } from 'react';
import { useEditorStore } from '@/store/editor';

export const PreviewToolbar: FC = () => {
  const previewMode = useEditorStore((s) => s.previewMode);
  const setPreviewMode = useEditorStore((s) => s.setPreviewMode);

  const modes: Array<{ key: typeof previewMode; label: string }> = [
    { key: 'desktop', label: 'Desktop' },
    { key: 'tablet', label: 'Tauleta' },
    { key: 'mobile', label: 'Mòbil' },
  ];

  return (
    <div className="flex items-center gap-1 bg-[#13132a] rounded p-0.5 border border-border-base">
      {modes.map(({ key, label }) => (
        <button
          key={key}
          onClick={() => setPreviewMode(key)}
          className={`px-2 h-6 f-mono text-label uppercase tracking-wider rounded transition ${
            previewMode === key
              ? 'bg-[#FF6B00] text-black font-semibold'
              : 'text-ink-3 hover:text-ink-1'
          }`}
        >
          {label}
        </button>
      ))}
    </div>
  );
};
