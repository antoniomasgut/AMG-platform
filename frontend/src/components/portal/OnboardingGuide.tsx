'use client';

import { useEffect, useRef } from 'react';
import { useLocale } from 'next-intl';
import { OnboardingStep } from './OnboardingStep';
import { OnboardingComplete } from './OnboardingComplete';

interface OnboardingGuideProps {
  userName: string;
  landingsCount: number;
  workflowsCount: number;
  invoicesCount: number;
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
  landingsCount: number,
  workflowsCount: number,
  invoicesCount: number,
  locale: string,
): StepDef[] {
  return [
    {
      id: 'landing',
      num: 1,
      title: 'Crea la teva primera landing',
      description: "Publica la teva web en menys de 5 minuts amb l'editor visual.",
      href: `/${locale}/portal/landings/new`,
      done: landingsCount > 0,
    },
    {
      id: 'automation',
      num: 2,
      title: 'Connecta una automatització',
      description: 'Connecta n8n per enviar emails, WhatsApp o rebre notificacions.',
      href: `/${locale}/portal/automations`,
      done: workflowsCount > 0,
    },
    {
      id: 'billing',
      num: 3,
      title: 'Genera el teu primer pressupost',
      description: "Crea un pressupost personalitzat i envia'l al client en PDF.",
      href: `/${locale}/portal/billing`,
      done: invoicesCount > 0,
    },
  ];
}

export function OnboardingGuide({
  userName,
  landingsCount,
  workflowsCount,
  invoicesCount,
  onSkip,
  onComplete,
}: OnboardingGuideProps) {
  const locale = useLocale();
  const steps = buildSteps(landingsCount, workflowsCount, invoicesCount, locale);
  const completedCount = steps.filter((s) => s.done).length;
  const totalCount = steps.length;
  const prevCompletedRef = useRef(completedCount);

  // Notify parent when all steps become complete
  useEffect(() => {
    if (completedCount === totalCount && completedCount > prevCompletedRef.current) {
      onComplete();
    }
    prevCompletedRef.current = completedCount;
  }, [completedCount, totalCount, onComplete]);

  if (completedCount === totalCount) {
    return (
      <OnboardingComplete
        userName={userName}
        onGoToDashboard={onComplete}
      />
    );
  }

  return (
    <div className="space-y-6 animate-slide-up" role="region" aria-label="Guia d'inici ràpid">
      <div>
        <div className="f-mono text-label uppercase tracking-widest text-accent-light mb-1">PRIMERS PASSOS</div>
        <div className="f-display font-bold text-xl">Benvingut, {userName}! Comencem.</div>
        <p className="text-ink-2 text-sm mt-1">
          Completa el{totalCount > 1 ? 's ' + totalCount : ''} pas{totalCount > 1 ? 'os' : ''} per treure el màxim profit del portal.
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
            style={{ width: `${Math.round((completedCount / totalCount) * 100)}%` }}
          />
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
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
