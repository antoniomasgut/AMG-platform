'use client';

import { useTranslations } from 'next-intl';
import { AMGButton } from '@/components/ui/button';
import type { ServiceWizardConfig } from '@/config/service-wizards';

interface WizardWelcomeProps {
  serviceName: string;
  config: ServiceWizardConfig;
  onStart: () => void;
  onCancel: () => void;
}

export function WizardWelcome({ serviceName, config, onStart, onCancel }: WizardWelcomeProps) {
  const t = useTranslations();
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="text-center space-y-2">
        <div className="w-14 h-14 rounded-full bg-accent-muted flex items-center justify-center mx-auto">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#FF6B00" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="8" width="18" height="12" rx="2" />
            <path d="M12 8V4M8 4h8" />
            <circle cx="9" cy="14" r="1" />
            <circle cx="15" cy="14" r="1" />
          </svg>
        </div>
        <div>
          <div className="f-display font-bold text-lg">{t(config.titleKey)}</div>
          <p className="text-sm text-ink-2 mt-1">{serviceName}</p>
        </div>
      </div>

      {/* Description */}
      <div className="amg-card card-clip p-5 space-y-3">
        <div className="f-display font-bold text-sm">Què farem?</div>
        <p className="text-sm text-ink-2 leading-relaxed">{t(config.descriptionKey)}</p>

        <div className="f-display font-bold text-sm mt-4">Necessitaràs:</div>
        <p className="text-sm text-ink-2 leading-relaxed">{t(config.prerequisitesKey)}</p>
      </div>

      {/* Steps overview */}
      <div className="space-y-2">
        <div className="f-mono uppercase text-label text-ink-3 text-xs tracking-widest">
          Passos ({config.steps.length})
        </div>
        <div className="space-y-2">
          {config.steps.map((step, i) => (
            <div key={step.id} className="flex items-center gap-3 p-3 rounded border border-border-base bg-[#1a1a2e]/50">
              <div className="w-7 h-7 rounded-full border border-ink-3 flex items-center justify-center f-mono text-[10px] text-ink-3 flex-shrink-0">
                {i + 1}
              </div>
              <div className="min-w-0">
                <div className="f-display font-bold text-sm">{t(step.titleKey)}</div>
                <div className="f-mono text-[10px] text-ink-3 uppercase">{step.type}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Actions */}
      <div className="flex gap-3 pt-2">
        <AMGButton onClick={onStart} className="flex-1 justify-center">
          Començar
        </AMGButton>
        <AMGButton variant="outline" onClick={onCancel}>
          Configurar més tard
        </AMGButton>
      </div>
    </div>
  );
}
