'use client';

import type { ReactNode } from 'react';
import { IconSet } from './icons';

interface PhaseGuardProps {
  allowed: boolean;
  loading?: boolean;
  phaseName: string;
  phaseLabel: string;
  description?: string;
  children: ReactNode;
}

export function PhaseGuard({ allowed, loading, phaseName, phaseLabel, description, children }: PhaseGuardProps) {
  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <span className="w-5 h-5 border-2 border-accent-light border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!allowed) {
    return (
      <div className="flex flex-col items-center justify-center py-24 gap-5 text-center px-4">
        <div className="w-14 h-14 rounded-full bg-surface-overlay border border-border-base flex items-center justify-center">
          <IconSet.Lock size={22} className="text-ink-3" />
        </div>
        <div className="space-y-1 max-w-xs">
          <div className="text-sm font-semibold text-ink-1">Funcionalitat no disponible</div>
          <div className="text-xs text-ink-2">
            Requereix contractar la fase{' '}
            <span className="text-accent-light font-semibold">{phaseName} — {phaseLabel}</span>
          </div>
          {description && (
            <div className="text-xs text-ink-3 mt-1">{description}</div>
          )}
        </div>
        <a href="/portal" className="text-xs text-accent-light hover:underline">
          Tornar al portal
        </a>
      </div>
    );
  }

  return <>{children}</>;
}
