'use client';

import { useEffect, useRef } from 'react';
import { useLocale, useTranslations } from 'next-intl';
import { OnboardingStep } from './OnboardingStep';
import { OnboardingComplete } from './OnboardingComplete';

interface OnboardingGuideProps {
  tenantId: string;
  userName: string;
  assignedServices: Array<{ type: string; isAddon?: boolean }>;
  landings: any[];
  workflows: any[];
  invoices: any[];
  vaultPending?: boolean;
  onSkip: () => void;
  onComplete: () => void;
}

interface StepDef {
  id: 'landing' | 'automation' | 'billing';
  step: number;
  title: string;
  description: string;
  cta: string;
  href: string;
  done: boolean;
}

export function OnboardingGuide({
  tenantId,
  userName,
  assignedServices,
  landings,
  workflows,
  invoices,
  vaultPending = false,
  onSkip,
  onComplete,
}: OnboardingGuideProps) {
  const locale = useLocale();
  const t = useTranslations('onboarding');

  const activeTypes = new Set(
    assignedServices
      .filter(s => !s.isAddon)
      .map(s => s.type)
  );

  const steps: StepDef[] = [];
  let stepNum = 1;

  if (activeTypes.has('LANDING')) {
    steps.push({
      id: 'landing', step: stepNum++,
      title: t('steps.landing_title'),
      description: t('steps.landing_desc'),
      cta: t('steps.landing_cta'),
      href: '/portal/landings/new',
      done: landings.length > 0,
    });
  }
  if (activeTypes.has('AUTOMATION')) {
    steps.push({
      id: 'automation', step: stepNum++,
      title: t('steps.automation_title'),
      description: t('steps.automation_desc'),
      cta: t('steps.automation_cta'),
      href: '/portal/automations',
      done: workflows.length > 0,
    });
  }
  if (activeTypes.has('BILLING')) {
    steps.push({
      id: 'billing', step: stepNum++,
      title: t('steps.billing_title'),
      description: t('steps.billing_desc'),
      cta: t('steps.billing_cta'),
      href: '/portal/billing',
      done: invoices.length > 0,
    });
  }

  const completedCount = steps.filter(s => s.done).length;
  const totalCount = steps.length;
  const prevCompletedRef = useRef(completedCount);

  useEffect(() => {
    if (totalCount > 0 && completedCount === totalCount && completedCount > prevCompletedRef.current) {
      onComplete();
    }
    prevCompletedRef.current = completedCount;
  }, [completedCount, totalCount, onComplete]);

  if (totalCount > 0 && completedCount === totalCount) {
    return (
      <OnboardingComplete
        userName={userName}
        onGoToDashboard={onComplete}
      />
    );
  }

  const colsClass = totalCount === 1
    ? 'grid-cols-1 max-w-sm'
    : totalCount === 2
      ? 'grid-cols-1 md:grid-cols-2 max-w-2xl'
      : 'grid-cols-1 md:grid-cols-3';

  return (
    <div className="space-y-6 animate-slide-up" role="region" aria-label={t('title', { name: userName })}>
      <div>
        <div className="f-mono text-label uppercase tracking-widest text-accent-light mb-1">{t('eyebrow')}</div>
        <div className="f-display font-bold text-xl">{t('title', { name: userName })}</div>
        <p className="text-ink-2 text-sm mt-1">{t('subtitle')}</p>
      </div>

      <div>
        <div className="flex justify-between f-mono text-label uppercase text-ink-2 mb-1.5">
          <span>{t('eyebrow')}</span>
          <span>{t('progress', { completed: completedCount, total: totalCount })}</span>
        </div>
        <div className="h-1.5 bg-[#212140] overflow-hidden">
          <div
            className="h-full bg-gradient-to-r from-[#FF6B00] to-[#FF9A3C] transition-all duration-500"
            style={{ width: totalCount > 0 ? `${Math.round((completedCount / totalCount) * 100)}%` : '0%' }}
          />
        </div>
      </div>

      <div className={`grid ${colsClass} gap-4`}>
        {steps.map(step => (
          <OnboardingStep
            key={step.id}
            step={step.step}
            title={step.title}
            description={step.description}
            cta={step.cta}
            href={step.href}
            done={step.done}
          />
        ))}
      </div>

      {vaultPending && (
        <div className="flex items-start gap-3 p-4 border border-amber-500/30 bg-amber-900/10">
          <span className="text-amber-400 text-lg shrink-0">⚠</span>
          <div className="flex-1 min-w-0">
            <div className="f-mono text-[10px] uppercase tracking-wider text-amber-400 mb-0.5">{t('vault_pending')}</div>
            <p className="text-sm text-ink-2">{t('vault_pending_desc')}</p>
          </div>
          <a
            href={`/${locale}/portal/admin/vault`}
            className="f-mono text-label uppercase text-accent-light hover:text-accent border border-border-base hover:border-accent px-3 h-8 flex items-center text-[10px] shrink-0 transition-colors"
          >
            {t('vault_pending_cta')}
          </a>
        </div>
      )}

      <div className="flex justify-end">
        <button
          onClick={onSkip}
          aria-label={t('skip')}
          className="f-mono text-label uppercase text-ink-2 hover:text-ink-0 transition"
        >
          {t('skip')}
        </button>
      </div>
    </div>
  );
}
