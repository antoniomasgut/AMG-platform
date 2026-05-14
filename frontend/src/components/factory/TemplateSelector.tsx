'use client';

import type { FC } from 'react';
import { LANDING_TEMPLATES } from '@/services/factory';

interface Props {
  onSelect: (templateId: string) => void;
}

export const TemplateSelector: FC<Props> = ({ onSelect }) => {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {LANDING_TEMPLATES.map((tpl) => (
        <button
          key={tpl.id}
          onClick={() => onSelect(tpl.id)}
          className="amg-card card-clip p-5 text-left hover:ring-1 hover:ring-[#FF6B00] transition group"
        >
          <div className="f-display font-bold text-sm mb-1 text-ink-0 group-hover:text-accent-light transition">
            {tpl.label}
          </div>
          <div className="f-mono text-caption text-ink-3 mb-3">{tpl.desc}</div>
          <div className="flex flex-wrap gap-1">
            {tpl.blocks.map((blockType) => (
              <span key={blockType} className="text-label px-2 py-0.5 bg-[rgba(255,107,0,0.1)] text-accent-light rounded">
                {blockType}
              </span>
            ))}
          </div>
        </button>
      ))}
    </div>
  );
};
