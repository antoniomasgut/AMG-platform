'use client';

import { useTranslations } from 'next-intl';
import { AMGButton } from '@/components/ui/button';

interface StepSummary {
  id: string;
  titleKey: string;
  done: boolean;
}

interface WizardCompleteProps {
  serviceName: string;
  steps: StepSummary[];
  onFinish: () => void;
}

export function WizardComplete({ serviceName, steps, onFinish }: WizardCompleteProps) {
  const t = useTranslations();
  return (
    <div className="space-y-6 text-center">
      {/* Checkmark animation */}
      <div className="flex justify-center">
        <div className="w-20 h-20 rounded-full bg-emerald-500/20 flex items-center justify-center">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#10b981" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <path d="M20 6 9 17l-5-5" />
          </svg>
        </div>
      </div>

      <div>
        <div className="f-display font-bold text-xl">Configuració completada!</div>
        <p className="text-sm text-ink-2 mt-2">
          {serviceName} configurat correctament.
        </p>
      </div>

      {/* Steps summary */}
      <div className="amg-card card-clip p-5 max-w-md mx-auto">
        <div className="space-y-3">
          {steps.map((step) => (
            <div key={step.id} className="flex items-center gap-3">
              <div
                className={`w-6 h-6 rounded-full flex items-center justify-center flex-shrink-0 ${
                  step.done ? 'bg-emerald-500 text-black' : 'bg-[#1a1a2e] text-ink-3 border border-border-base'
                }`}
              >
                {step.done ? (
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
                    <path d="M20 6 9 17l-5-5" />
                  </svg>
                ) : (
                  <span className="text-[10px] font-bold">?</span>
                )}
              </div>
              <span className={`text-sm ${step.done ? 'text-ink-0' : 'text-ink-3'}`}>
                {t(step.titleKey)}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Action */}
      <AMGButton onClick={onFinish} className="justify-center">
        Tornar al tenant
      </AMGButton>
    </div>
  );
}
