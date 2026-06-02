'use client';

interface OnboardingStepProps {
  num: number;
  title: string;
  description: string;
  cta: string;
  href: string;
  done: boolean;
  indeterminate?: boolean;
}

export function OnboardingStep({ num, title, description, cta, href, done, indeterminate }: OnboardingStepProps) {
  const content = (
    <div
      aria-label={`${num}. ${title}`}
      className={`amg-card card-clip p-5 flex flex-col gap-3 transition-all duration-500 ${
        done
          ? 'border-emerald-500/60 bg-emerald-900/10'
          : indeterminate
            ? 'border-slate-500/30 bg-[#1a1a2e]'
            : 'border-amber-500/40 bg-[#1a1a2e] hover:border-[#FF6B00]/60 cursor-pointer'
      }`}
    >
      <div className="flex items-center gap-3">
        <div
          className={`w-8 h-8 flex items-center justify-center text-sm font-bold f-mono ${
            done
              ? 'bg-emerald-900/40 text-emerald-400 border border-emerald-500/40'
              : indeterminate
                ? 'bg-slate-800/40 text-slate-400 border border-slate-500/30'
                : 'bg-accent-muted text-accent-light border border-border-strong'
          }`}
        >
          {done ? (
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#4ade80" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="20 6 9 17 4 12" />
            </svg>
          ) : indeterminate ? (
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
          ) : (
            <span>{num}</span>
          )}
        </div>
        <span className="f-display font-bold text-sm">{title}</span>
      </div>
      <p className="text-data text-ink-2">{description}</p>
      {!done && !indeterminate && (
        <span className="f-mono text-label uppercase text-accent-light mt-auto">{cta} →</span>
      )}
    </div>
  );

  if (done || indeterminate) {
    return content;
  }

  return (
    <a href={href} className="block">
      {content}
    </a>
  );
}
