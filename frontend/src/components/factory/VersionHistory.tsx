'use client';

import { useMemo, type FC } from 'react';
import { useEditorStore } from '@/store/editor';
import type { VersionResponse } from '@/services/factory';

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Esborrany',
  PUBLISHED: 'Publicada',
  ARCHIVED: 'Arxivada',
};

const STATUS_CLASSES: Record<string, string> = {
  PUBLISHED: 'bg-green-900/40 text-green-400',
  ARCHIVED: 'bg-gray-800 text-gray-500',
};

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('ca-ES', { dateStyle: 'short', timeStyle: 'short' });
}

export const VersionHistory: FC = () => {
  const landing = useEditorStore((s) => s.landing);
  const versionId = useEditorStore((s) => s.versionId);
  const loadLanding = useEditorStore((s) => s.loadLanding);

  const sorted = useMemo(
    () => landing ? [...landing.versions].sort((a, b) => b.versionNumber - a.versionNumber) : [],
    [landing]
  );

  if (!landing) return null;

  function restore(version: VersionResponse) {
    if (!landing) return;
    if (!confirm(`Restaurar versió ${version.versionNumber}? Es perdran els canvis no desats.`)) return;
    loadLanding(landing, version.id);
  }

  return (
    <div className="p-3 space-y-2">
      <div className="f-mono text-label uppercase tracking-widest text-ink-3">Historial de versions</div>
      {sorted.map((v) => {
        const isCurrent = v.id === versionId;
        const statusClass = STATUS_CLASSES[v.status] ?? 'bg-accent-muted text-accent-light';
        return (
          <div
            key={v.id}
            className={`rounded p-2 border transition ${
              isCurrent
                ? 'border-[#FF6B00] bg-accent-subtle'
                : 'border-border-base hover:border-[rgba(255,107,0,0.3)]'
            }`}
          >
            <div className="flex items-center justify-between gap-2">
              <span className="f-mono text-caption text-ink-0">v{v.versionNumber}</span>
              <span className={`f-mono text-[9px] uppercase px-1.5 py-0.5 rounded ${statusClass}`}>
                {STATUS_LABELS[v.status] ?? v.status}
              </span>
            </div>
            <div className="f-mono text-[9px] text-ink-3 mt-0.5">{formatDate(v.createdAt)}</div>
            {isCurrent ? (
              <div className="mt-1 f-mono text-[9px] text-accent">↑ Versió activa</div>
            ) : (
              <button
                onClick={() => restore(v)}
                className="mt-2 w-full f-mono text-[9px] uppercase py-1 rounded border border-border-medium text-accent-light hover:bg-[rgba(255,107,0,0.1)] transition"
              >
                Restaurar
              </button>
            )}
          </div>
        );
      })}
    </div>
  );
};
