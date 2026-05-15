'use client';

import { useState, useCallback } from 'react';
import { getWizardConfig, type WizardStep, type ServiceWizardConfig } from '@/config/service-wizards';
import { WizardProgress } from './WizardProgress';
import { WizardWelcome } from './WizardWelcome';
import { WizardStepRenderer } from './WizardStepRenderer';
import { WizardComplete } from './WizardComplete';
import { AMGButton } from '@/components/ui/button';
import { setCredential, verifyService } from '@/services/vault';

type WizardState = 'loading' | 'welcome' | 'active' | 'complete' | 'cancelled';

interface ServiceWizardProps {
  tenantId: string;
  serviceId: string;
  slug: string;
  serviceType: string;
  serviceName: string;
  onComplete: () => void;
  onCancel: () => void;
}

export function ServiceWizard({
  tenantId,
  serviceId,
  slug,
  serviceType,
  serviceName,
  onComplete,
  onCancel,
}: ServiceWizardProps) {
  const config = getWizardConfig(slug, serviceType);
  const [wizardState, setWizardState] = useState<WizardState>(config ? 'welcome' : 'loading');
  const [currentStep, setCurrentStep] = useState(0);
  const [formData, setFormData] = useState<Record<string, string>>({});
  const [completedSteps, setCompletedSteps] = useState<Set<string>>(new Set());
  const [errorSteps, setErrorSteps] = useState<Set<string>>(new Set());

  const handleFieldChange = useCallback((fieldId: string, value: string) => {
    setFormData((prev) => ({ ...prev, [fieldId]: value }));
  }, []);

  if (!config) {
    return (
      <div className="amg-card card-clip p-8 text-center">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#ff6666" strokeWidth="1.6" className="mx-auto mb-3">
          <circle cx="12" cy="12" r="10" />
          <path d="m15 9-6 6M9 9l6 6" />
        </svg>
        <div className="f-display font-bold text-sm mb-1">Guia no disponible</div>
        <p className="f-mono text-xs text-ink-2 mb-4">
          No hi ha un assistent de configuració per a aquest tipus de servei ({serviceType}).
        </p>
        <AMGButton size="sm" onClick={onCancel}>Tornar</AMGButton>
      </div>
    );
  }

  const steps = config.steps;
  const isFirstStep = currentStep === 0;
  const isLastStep = currentStep === steps.length - 1;

  const getStepStatus = (step: WizardStep, index: number) => {
    if (completedSteps.has(step.id)) return 'done' as const;
    if (errorSteps.has(step.id)) return 'error' as const;
    if (index === currentStep) return 'active' as const;
    return 'pending' as const;
  };

  const handleVerify = async (): Promise<boolean> => {
    try {
      const data = await verifyService(tenantId, serviceId);
      return data.verified === true;
    } catch {
      return false;
    }
  };

  const saveStepCredentials = async (step: WizardStep) => {
    if (!step.fields) return;
    for (const field of step.fields) {
      const value = formData[field.id];
      if (value && field.id !== 'webhook_url' && field.type !== 'select') {
        try {
          // Try to find fieldId from existing credential fields
          // In real flow, this would come from the service setup data
          await setCredential(tenantId, serviceId, field.id, value);
        } catch {
          // Silently fail — credentials are best-effort during wizard
        }
      }
    }
  };

  const handleNext = async () => {
    const step = steps[currentStep];

    // Mark current step as done
    setCompletedSteps((prev) => new Set(prev).add(step.id));

    // Save credentials if applicable
    if (step.type === 'credentials') {
      await saveStepCredentials(step);
    }

    if (isLastStep) {
      setWizardState('complete');
    } else {
      setCurrentStep((prev) => prev + 1);
    }
  };

  const handlePrev = () => {
    if (!isFirstStep) {
      setCurrentStep((prev) => prev - 1);
    }
  };

  const handleStart = () => {
    setWizardState('active');
  };

  // Welcome screen
  if (wizardState === 'welcome') {
    return (
      <div className="max-w-2xl mx-auto">
        <WizardWelcome
          serviceName={serviceName}
          config={config}
          onStart={handleStart}
          onCancel={onCancel}
        />
      </div>
    );
  }

  // Completion screen
  if (wizardState === 'complete') {
    return (
      <div className="max-w-lg mx-auto">
        <WizardComplete
          serviceName={serviceName}
          steps={steps.map((s) => ({
            id: s.id,
            titleKey: s.titleKey,
            done: completedSteps.has(s.id),
          }))}
          onFinish={onComplete}
        />
      </div>
    );
  }

  // Active step
  const step = steps[currentStep];
  const progressSteps = steps.map((s, i) => ({
    id: s.id,
    titleKey: s.titleKey,
    status: getStepStatus(s, i),
  }));

  return (
    <div className="max-w-2xl mx-auto">
      {/* Progress */}
      <WizardProgress steps={progressSteps} currentStep={currentStep} />

      {/* Step content */}
      <WizardStepRenderer
        step={step}
        stepIndex={currentStep}
        totalSteps={steps.length}
        formData={formData}
        onFieldChange={handleFieldChange}
        onVerify={handleVerify}
      />

      {/* Navigation */}
      <div className="flex items-center justify-between mt-8 pt-4 border-t border-border-base">
        <AMGButton
          variant="ghost"
          onClick={isFirstStep ? onCancel : handlePrev}
        >
          {isFirstStep ? 'Configurar més tard' : 'Anterior'}
        </AMGButton>

        {step.type === 'verify' ? (
          // For verify steps, let the user finish from the verify button
          <AMGButton onClick={handleNext} disabled={false}>
            {isLastStep ? 'Finalitzar' : 'Següent'}
          </AMGButton>
        ) : (
          <AMGButton onClick={handleNext}>
            {isLastStep ? 'Finalitzar' : 'Següent'}
          </AMGButton>
        )}
      </div>
    </div>
  );
}
