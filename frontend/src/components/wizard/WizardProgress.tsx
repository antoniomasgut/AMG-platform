'use client';

import { useTranslations } from 'next-intl';

interface StepSummary {
  id: string;
  titleKey: string;
  status: 'pending' | 'active' | 'done' | 'error';
}

export function WizardProgress({ steps, currentStep }: { steps: StepSummary[]; currentStep: number }) {
  const t = useTranslations();
  return (
    <div className="flex items-center gap-2 mb-6">
      {steps.map((step, i) => {
        const isLast = i === steps.length - 1;
        const isActive = step.status === 'active';
        const isDone = step.status === 'done';
        const isError = step.status === 'error';

        return (
          <div key={step.id} className="flex items-center gap-2 flex-1 min-w-0">
            <div className="flex items-center gap-2 min-w-0">
              {/* Circle */}
              <div
                className={`w-6 h-6 rounded-full flex items-center justify-center flex-shrink-0 transition-all text-[10px] font-bold ${
                  isDone
                    ? 'bg-emerald-500 text-black'
                    : isError
                    ? 'bg-red-500 text-white'
                    : isActive
                    ? 'bg-[#FF6B00] text-black'
                    : 'bg-[#1a1a2e] text-ink-3 border border-border-base'
                }`}
                title={t(step.titleKey)}
              >
                {isDone ? (
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
                    <path d="M20 6 9 17l-5-5" />
                  </svg>
                ) : isError ? (
                  <span>!</span>
                ) : (
                  <span>{i + 1}</span>
                )}
              </div>
              {/* Label — només visible si és el pas actiu */}
              <span
                className={`f-mono text-[10px] uppercase truncate hidden sm:block ${
                  isActive ? 'text-accent-light' : 'text-ink-3'
                }`}
              >
                {t(step.titleKey)}
              </span>
            </div>
            {/* Connector line */}
            {!isLast && (
              <div
                className={`flex-1 h-[2px] min-w-[12px] ${
                  isDone ? 'bg-emerald-500/60' : 'bg-border-base'
                }`}
              />
            )}
          </div>
        );
      })}
    </div>
  );
}
