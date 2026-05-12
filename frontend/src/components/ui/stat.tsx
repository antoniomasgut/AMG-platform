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
    accent: 'text-[#FF9A3C]',
    success: 'text-[#39d353]',
    danger: 'text-[#ff6666]',
    info: 'text-[#58a6ff]',
  };
  return (
    <div className="card-clip amg-card p-5 relative overflow-hidden">
      <div className="flex items-start justify-between">
        <span className="f-mono uppercase text-[10px] tracking-[0.18em] text-[#64748b]">{label}</span>
        {Ico && <div className={`${colors[tone]} opacity-80`}><Ico size={16} /></div>}
      </div>
      <div className="mt-3 flex items-baseline gap-2">
        <span className={`f-display font-bold text-3xl ${colors[tone]}`}>{value}</span>
      </div>
      {delta && (
        <div className="mt-2 flex items-center gap-1.5 text-[11px]">
          <span className={delta.startsWith('+') ? 'text-[#39d353]' : 'text-[#ff6666]'}>{delta}</span>
          <span className="f-mono text-[#64748b] uppercase tracking-wider">vs mes anterior</span>
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
        {eyebrow && <span className="f-mono uppercase text-[10px] tracking-[0.2em] text-[#FF9A3C]">{eyebrow}</span>}
        <h3 className="f-display text-lg font-bold tracking-wider text-[#e2e8f0] mt-1">{title}</h3>
      </div>
      {children}
    </div>
  );
}
