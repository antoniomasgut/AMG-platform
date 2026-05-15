'use client';

import { useQuery } from '@tanstack/react-query';
import type { FC } from 'react';
import { listTemplates, blockTypeFromBackend } from '@/services/templates';
import { BLOCK_TEMPLATES } from '@/services/factory';

interface Props {
  onSelect: (templateId: string) => void;
}

export const TemplateSelector: FC<Props> = ({ onSelect }) => {
  const { data: templates, isLoading } = useQuery({
    queryKey: ['templates'],
    queryFn: () => listTemplates(),
  });

  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!templates || templates.length === 0) {
    return (
      <div className="text-center py-8">
        <p className="f-mono text-xs text-ink-2">No hi ha plantilles disponibles</p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {templates.map((tpl) => (
        <button
          key={tpl.id}
          onClick={() => onSelect(tpl.id)}
          className="amg-card card-clip p-5 text-left hover:ring-1 hover:ring-[#FF6B00] transition group"
        >
          <div className="f-display font-bold text-sm mb-1 text-ink-0 group-hover:text-accent-light transition">
            {tpl.name}
          </div>
          <div className="f-mono text-caption text-ink-3 mb-3">{tpl.description}</div>
          <div className="flex flex-wrap gap-1">
            {Array.from({ length: tpl.sectionCount }).map((_, i) => (
              <span key={i} className="text-label px-2 py-0.5 bg-[rgba(255,107,0,0.1)] text-accent-light rounded">
                ●
              </span>
            ))}
          </div>
        </button>
      ))}
    </div>
  );
};
