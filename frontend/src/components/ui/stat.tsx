import React from 'react';
import type { FC, ReactNode } from 'react';

type StatTone = 'accent' | 'success' | 'danger' | 'info';

interface StatProps {
  label: string;
  value: string;
  delta?: string;
  tone?: StatTone;
  icon?: FC<{ size?: number; stroke?: string; className?: string }>;
}

export function AMGStat({ label, value, delta, tone = 'accent', icon: Ico }: StatProps) {
  const colors: Record<string, string> = {
    accent: 'text-accent-light',
    success: 'text-success',
    danger: 'text-danger-light',
    info: 'text-info',
  };
  return (
    <div className="card-clip amg-card p-5 relative overflow-hidden">
      <div className="flex items-start justify-between">
        <span className="f-mono uppercase text-label tracking-[0.18em] text-ink-3">{label}</span>
        {Ico && <div className={`${colors[tone]} opacity-80`}><Ico /></div>}
      </div>
      <div className="mt-3 flex items-baseline gap-2">
        <span className={`f-display font-bold text-3xl ${colors[tone]}`}>{value}</span>
      </div>
      {delta && (
        <div className="mt-2 flex items-center gap-1.5 text-caption">
          <span className={delta.startsWith('+') ? 'text-success' : 'text-danger-light'}>{delta}</span>
          <span className="f-mono text-ink-3 uppercase tracking-wider">vs mes anterior</span>
        </div>
      )}
      <div className="absolute top-0 right-0 w-[2px] h-6 bg-[#FF6B00]" />
    </div>
  );
}

export function AMGSectionTitle({ eyebrow, title, children }: { eyebrow?: string; title: string; children?: ReactNode }) {
  return (
    <div className="flex items-end justify-between mb-4">
      <div>
        {eyebrow && <span className="f-mono uppercase text-label tracking-widest text-accent-light">{eyebrow}</span>}
        <h3 className="f-display text-lg font-bold tracking-wider text-ink-0 mt-1">{title}</h3>
      </div>
      {children}
    </div>
  );
}
