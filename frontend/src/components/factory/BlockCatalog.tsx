'use client';

import type { FC } from 'react';
import { BLOCK_TEMPLATES } from '@/services/factory';
import type { BlockType } from '@/services/factory';

interface Props {
  onAddBlock: (type: BlockType) => void;
}

export const BlockCatalog: FC<Props> = ({ onAddBlock }) => {
  const entries = Object.entries(BLOCK_TEMPLATES) as [BlockType, typeof BLOCK_TEMPLATES[BlockType]][];

  return (
    <div className="space-y-0.5">
      <div className="f-mono text-label uppercase tracking-widest text-ink-3 px-3 py-2">Blocs</div>
      {entries.map(([type, tpl]) => (
        <button
          key={type}
          onClick={() => onAddBlock(type)}
          className="w-full flex items-center gap-3 px-3 py-2 text-left text-xs text-ink-1 hover:text-ink-0 hover:bg-accent-subtle transition rounded"
        >
          <span className="w-6 h-6 flex items-center justify-center text-accent-light text-sm font-bold">{tpl.icon}</span>
          <span className="f-mono text-caption uppercase tracking-wider">{tpl.label}</span>
        </button>
      ))}
    </div>
  );
};
