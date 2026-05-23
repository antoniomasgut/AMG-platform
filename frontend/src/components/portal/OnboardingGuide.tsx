'use client';

import { useEffect, useRef } from 'react';
import { useLocale } from 'next-intl';
import { OnboardingStep } from './OnboardingStep';
import { OnboardingComplete } from './OnboardingComplete';

interface OnboardingGuideProps {
  userName: string;
  assignedServiceTypes: Set<string>;
  landingsCount: number;
  workflowsCount: number;
  invoicesCount: number;
  vaultPending?: boolean;
  onSkip: () => void;
  onComplete: () => void;
}

interface StepDef {
  id: 'landing' | 'automation' | 'billing';
  num: number;
  title: string;
  description: string;
  href: string;
  done: boolean;
}

function buildSteps(
  assignedTypes: Set<string>,
  landingsCount: number,
  workflowsCount: number,
  invoicesCount: number,
  locale: string,
): StepDef[] {
  const steps: StepDef[] = [];
  let num = 1;

  if (assignedTypes.has('LANDING')) {
    steps.push({
      id: 'landing', num: num++,
      title: 'Crea la teva primera landing',
      description: "Publica la teva web en menys de 5 minuts amb l'editor visual.",
      href: `/${locale}/portal/landings/new`,
      done: landingsCount > 0,
    });
  }
  if (assignedTypes.has('AUTOMATION')) {
    steps.push({
      id: 'automation', num: num++,
      title: 'Connecta una automatització',
      description: 'Connecta n8n per enviar emails, WhatsApp o rebre notificacions.',
      href: `/${locale}/portal/automations`,
      done: workflowsCount > 0,
    });
  }
  if (assignedTypes.has('BILLING')) {
    steps.push({
      id: 'billing', num: num++,
      title: 'Genera el teu primer pressupost',
      description: "Crea un pressupost personalitzat i envia'l al client en PDF.",
      href: `/${locale}/portal/billing`,
      done: invoicesCount > 0,
    });
  }

  return steps;
}

export function OnboardingGuide({
  userName,
  assignedServiceTypes,
  landingsCount,
  workflowsCount,
  invoicesCount,
  vaultPending = false,
  onSkip,
  onComplete,
}: OnboardingGuideProps) {
  const locale = useLocale();
  const steps = buildSteps(assignedServiceTypes, landingsCount, workflowsCount, invoicesCount, locale);
  const completedCount = steps.filter((s) => s.done).length;
  const totalCount = steps.length;
  const prevCompletedRef = useRef(completedCount);

  useEffect(() => {
    if (completedCount === totalCount && totalCount > 0 && completedCount > prevCompletedRef.current) {
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
    <div className="space-y-6 animate-slide-up" role="region" aria-label="Guia d'inici ràpid">
      <div>
        <div className="f-mono text-label uppercase tracking-widest text-accent-light mb-1">PRIMERS PASSOS</div>
        <div className="f-display font-bold text-xl">Benvingut, {userName}! Comencem.</div>
        <p className="text-ink-2 text-sm mt-1">
          Completa el{totalCount > 1 ? 's ' + totalCount : ''} pas{totalCount > 1 ? 'sos' : ''} per treure el màxim profit del portal.
        </p>
      </div>

      <div>
        <div className="flex justify-between f-mono text-label uppercase text-ink-2 mb-1.5">
          <span>Progrés</span>
          <span>{completedCount} / {totalCount} passos</span>
        </div>
        <div className="h-1.5 bg-[#212140] overflow-hidden">
          <div
            className="h-full bg-gradient-to-r from-[#FF6B00] to-[#FF9A3C] transition-all duration-500"
            style={{ width: totalCount > 0 ? `${Math.round((completedCount / totalCount) * 100)}%` : '0%' }}
          />
        </div>
      </div>

      <div className={`grid ${colsClass} gap-4`}>
        {steps.map((step) => (
          <OnboardingStep
            key={step.id}
            num={step.num}
            title={step.title}
            description={step.description}
            href={step.href}
            done={step.done}
          />
        ))}
      </div>

      {vaultPending && (
        <div className="flex items-start gap-3 p-4 border border-amber-500/30 bg-amber-900/10">
          <span className="text-amber-400 text-lg shrink-0">⚠</span>
          <div className="flex-1 min-w-0">
            <div className="f-mono text-[10px] uppercase tracking-wider text-amber-400 mb-0.5">Configuració pendent</div>
            <p className="text-sm text-ink-2">El teu compte té serveis pendents de configurar. Revisa'ls per activar-los.</p>
          </div>
          <a
            href={`/${locale}/portal/admin/vault`}
            className="f-mono text-label uppercase text-accent-light hover:text-accent border border-border-base hover:border-accent px-3 h-8 flex items-center text-[10px] shrink-0 transition-colors"
          >
            Revisar →
          </a>
        </div>
      )}

      <div className="flex justify-end">
        <button
          onClick={onSkip}
          aria-label="Salta la guia d'inici i ves al dashboard"
          className="f-mono text-label uppercase text-ink-2 hover:text-ink-0 transition"
        >
          Salta l&#39;onboarding
        </button>
      </div>
    </div>
  );
}
