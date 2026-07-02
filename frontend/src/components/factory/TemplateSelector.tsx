'use client';

import { useQuery } from '@tanstack/react-query';
import type { FC } from 'react';
import { listTemplates, parseColorSchemes, type TemplateSummary } from '@/services/templates';

interface Props {
  onSelect: (template: TemplateSummary) => void;
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
      {templates.map((tpl) => {
        const schemes = parseColorSchemes(tpl.colorSchemes);
        return (
          <button
            key={tpl.id}
            onClick={() => onSelect(tpl)}
            className="amg-card card-clip p-5 text-left hover:ring-1 hover:ring-[#FF6B00] transition group"
          >
            <div className="f-display font-bold text-sm mb-1 text-ink-0 group-hover:text-accent-light transition">
              {tpl.name}
            </div>
            <div className="f-mono text-caption text-ink-3 mb-3">{tpl.description}</div>
            <div className="flex items-center gap-2">
              {schemes.length > 0 ? (
                <div className="flex gap-1">
                  {schemes.map((s, i) => (
                    <div
                      key={i}
                      title={s.name}
                      className="w-3.5 h-3.5 rounded-full border border-white/10"
                      style={{ background: s.primary }}
                    />
                  ))}
                </div>
              ) : (
                <div className="flex flex-wrap gap-1">
                  {Array.from({ length: tpl.sectionCount }).map((_, i) => (
                    <span key={i} className="text-label px-2 py-0.5 bg-[rgba(255,107,0,0.1)] text-accent-light rounded">
                      ●
                    </span>
                  ))}
                </div>
              )}
              <span className="f-mono text-[9px] text-ink-3 ml-auto">{tpl.sectionCount} blocs</span>
            </div>
          </button>
        );
      })}
    </div>
  );
};
