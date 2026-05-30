'use client';

import type { FC } from 'react';
import { useEditorStore } from '@/store/editor';

export const PreviewToolbar: FC = () => {
  const previewMode = useEditorStore((s) => s.previewMode);
  const setPreviewMode = useEditorStore((s) => s.setPreviewMode);

  const modes: Array<{ key: typeof previewMode; icon: string; title: string }> = [
    { key: 'desktop', icon: '🖥', title: 'Escriptori (amplada completa)' },
    { key: 'tablet',  icon: '▬', title: 'Tauleta (768px)' },
    { key: 'mobile',  icon: '📱', title: 'Mòbil (375px)' },
  ];

  return (
    <div className="flex items-center gap-0.5 bg-[#13132a] rounded p-0.5 border border-border-base" title="Previsualització responsiva">
      {modes.map(({ key, icon, title }) => (
        <button
          key={key}
          onClick={() => setPreviewMode(key)}
          title={title}
          className={`w-8 h-7 flex items-center justify-center text-sm rounded transition ${
            previewMode === key
              ? 'bg-[#FF6B00] text-black'
              : 'text-ink-3 hover:text-ink-1'
          }`}
        >
          {icon}
        </button>
      ))}
    </div>
  );
};
