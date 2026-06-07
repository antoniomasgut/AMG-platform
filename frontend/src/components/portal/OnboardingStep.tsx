'use client';

import { Link } from '@/i18n/navigation';
import { IconSet } from '@/components/ui/icons';

interface OnboardingStepProps {
  step: number;
  title: string;
  description: string;
  cta: string;
  href: string;
  done: boolean;
  indeterminate?: boolean;
}

export function OnboardingStep({ step, title, description, cta, href, done, indeterminate }: OnboardingStepProps) {
  const label = `Pas ${step}: ${title} — ${done ? 'completat' : indeterminate ? 'no disponible' : 'pendent'}`;

  const card = (
    <div
      aria-label={label}
      className={`col-span-full md:col-span-1 amg-card card-clip p-5 flex flex-col gap-3 transition-all duration-500 ${
        done
          ? 'border-emerald-500/60 bg-emerald-900/10'
          : indeterminate
            ? 'border-slate-500/30 bg-[#1a1a2e]'
            : 'border-amber-500/40 bg-[#1a1a2e]'
      }`}
    >
      <div className="flex items-center gap-3">
        <div
          className={`w-8 h-8 flex items-center justify-center f-mono font-bold ${
            done
              ? 'bg-emerald-900/40 text-emerald-400 border border-emerald-500/40'
              : indeterminate
                ? 'bg-slate-800/40 text-slate-400 border border-slate-500/30'
                : 'bg-accent-muted text-accent-light border border-border-strong'
          }`}
        >
          {done ? (
            <IconSet.Check size={14} stroke="#4ade80" />
          ) : indeterminate ? (
            <IconSet.Clock size={14} stroke="#94a3b8" />
          ) : (
            <span>{step}</span>
          )}
        </div>
        <span className="f-display font-bold text-sm">{title}</span>
      </div>
      <p className="text-data text-ink-2">{description}</p>
      <div className="mt-auto">
        {!done && !indeterminate && (
          <span className="f-mono text-label uppercase btn-clip bg-accent hover:bg-accent-light text-black font-semibold px-3 py-1.5 inline-block text-xs transition-colors">
            {cta} →
          </span>
        )}
        {done && (
          <span className="f-mono text-label uppercase text-emerald-400 font-semibold">
            Fet!
          </span>
        )}
      </div>
    </div>
  );

  if (done || indeterminate) return card;

  return (
    <Link href={href} className="block col-span-full md:col-span-1">
      {card}
    </Link>
  );
}
