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
    neutral: 'bg-[#212140] text-[#94a3b8] border border-[rgba(226,232,240,0.1)]',
    accent: 'bg-[rgba(255,107,0,0.12)] text-[#FF9A3C] border border-[rgba(255,107,0,0.35)]',
    success: 'bg-[rgba(57,211,83,0.12)] text-[#39d353] border border-[rgba(57,211,83,0.35)]',
    danger: 'bg-[rgba(255,68,68,0.12)] text-[#ff6666] border border-[rgba(255,68,68,0.35)]',
    info: 'bg-[rgba(88,166,255,0.12)] text-[#58a6ff] border border-[rgba(88,166,255,0.35)]',
    warning: 'bg-[rgba(240,180,41,0.12)] text-[#f0b429] border border-[rgba(240,180,41,0.35)]',
  };
  return (
    <span className={`${mono ? 'f-mono' : ''} inline-flex items-center gap-1.5 px-2 h-[22px] text-[10px] uppercase font-semibold tracking-wider ${tones[tone]} ${className}`}>
      {children}
    </span>
  );
}
