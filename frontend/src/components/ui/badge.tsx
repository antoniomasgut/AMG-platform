import React from 'react';
import type { ReactNode } from 'react';

type BadgeTone = 'neutral' | 'accent' | 'success' | 'danger' | 'info' | 'warning';

interface BadgeProps {
  tone?: BadgeTone;
  mono?: boolean;
  children?: ReactNode;
  className?: string;
}

export function AMGBadge({ tone = 'neutral', mono = true, children, className = '' }: BadgeProps) {
  const tones: Record<string, string> = {
    neutral: 'bg-[#212140] text-ink-1 border border-[rgba(226,232,240,0.1)]',
    accent: 'bg-accent-muted text-accent-light border border-border-strong',
    success: 'bg-[rgba(57,211,83,0.12)] text-success border border-[rgba(57,211,83,0.35)]',
    danger: 'bg-[rgba(255,68,68,0.12)] text-danger-light border border-[rgba(255,68,68,0.35)]',
    info: 'bg-[rgba(88,166,255,0.12)] text-info border border-[rgba(88,166,255,0.35)]',
    warning: 'bg-[rgba(240,180,41,0.12)] text-warning border border-[rgba(240,180,41,0.35)]',
  };
  return (
    <span className={`${mono ? 'f-mono' : ''} inline-flex items-center gap-1.5 px-2 h-[22px] text-label uppercase font-semibold tracking-wider ${tones[tone]} ${className}`}>
      {children}
    </span>
  );
}
