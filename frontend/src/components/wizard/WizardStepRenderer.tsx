'use client';

import { useState } from 'react';
import type { WizardStep } from '@/config/service-wizards';

interface StepRendererProps {
  step: WizardStep;
  stepIndex: number;
  totalSteps: number;
  formData: Record<string, string>;
  onFieldChange: (fieldId: string, value: string) => void;
  onVerify?: () => Promise<boolean>;
}

function CopyField({ value, label }: { value: string; label: string }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(value);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Fallback
      const el = document.createElement('textarea');
      el.value = value;
      document.body.appendChild(el);
      el.select();
      document.execCommand('copy');
      document.body.removeChild(el);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className="space-y-2">
      <div className="f-mono text-[10px] uppercase text-ink-3 tracking-widest">{label}</div>
      <div className="flex gap-2">
        <code className="flex-1 p-3 rounded bg-[#0d0d1a] border border-border-base text-sm text-ink-1 font-mono truncate">
          {value}
        </code>
        <button
          onClick={handleCopy}
          className="px-3 py-2 rounded border border-border-base hover:bg-[#1a1a2e] text-xs f-mono uppercase text-ink-2 hover:text-ink-0 transition flex items-center gap-1.5"
        >
          {copied ? (
            <>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <path d="M20 6 9 17l-5-5" />
              </svg>
              Copiat!
            </>
          ) : (
            <>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="9" y="9" width="13" height="13" rx="2" />
                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
              </svg>
              Copiar
            </>
          )}
        </button>
      </div>
    </div>
  );
}

function CredentialsForm({
  fields,
  formData,
  onFieldChange,
  errors,
}: {
  fields: NonNullable<WizardStep['fields']>;
  formData: Record<string, string>;
  onFieldChange: (id: string, value: string) => void;
  errors: Record<string, string>;
}) {
  return (
    <div className="space-y-4">
      {fields.map((field) => (
        <div key={field.id}>
          <label className="f-mono text-[10px] uppercase text-ink-2 tracking-widest block mb-1.5">
            {field.labelKey}
            {field.required && <span className="text-red-400 ml-1">*</span>}
          </label>
          {field.type === 'select' && field.options && field.options.length > 0 ? (
            <select
              value={formData[field.id] ?? ''}
              onChange={(e) => onFieldChange(field.id, e.target.value)}
              className="w-full p-2.5 rounded bg-[#0d0d1a] border border-border-base text-sm text-ink-0 focus:border-accent-light focus:outline-none transition"
            >
              <option value="">Selecciona...</option>
              {field.options.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.labelKey}
                </option>
              ))}
            </select>
          ) : (
            <input
              type={field.type}
              value={formData[field.id] ?? ''}
              onChange={(e) => onFieldChange(field.id, e.target.value)}
              placeholder={field.placeholderKey}
              className={`w-full p-2.5 rounded bg-[#0d0d1a] border text-sm text-ink-0 focus:outline-none transition ${
                errors[field.id] ? 'border-red-500' : 'border-border-base focus:border-accent-light'
              }`}
            />
          )}
          {field.hintKey && (
            <p className="text-[10px] text-ink-3 mt-1">{field.hintKey}</p>
          )}
          {errors[field.id] && (
            <p className="text-[10px] text-red-400 mt-1">{errors[field.id]}</p>
          )}
        </div>
      ))}
    </div>
  );
}

export function WizardStepRenderer({
  step,
  stepIndex,
  totalSteps,
  formData,
  onFieldChange,
  onVerify,
}: StepRendererProps) {
  const [verifyStatus, setVerifyStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle');
  const [verifyMessage, setVerifyMessage] = useState('');

  const handleVerify = async () => {
    if (!onVerify) return;
    setVerifyStatus('loading');
    setVerifyMessage('');
    try {
      const ok = await onVerify();
      setVerifyStatus(ok ? 'success' : 'error');
      setVerifyMessage(ok ? 'Connexió correcta' : 'Verificació fallida');
    } catch {
      setVerifyStatus('error');
      setVerifyMessage('Error de connexió');
    }
  };

  return (
    <div className="space-y-6">
      {/* Step header */}
      <div>
        <div className="f-mono text-[10px] uppercase text-ink-3 tracking-widest">
          Pas {stepIndex + 1} de {totalSteps}
        </div>
        <div className="f-display font-bold text-lg mt-1">{step.titleKey}</div>
        <p className="text-sm text-ink-2 mt-1">{step.descriptionKey}</p>
      </div>

      {/* Step content based on type */}
      {step.type === 'credentials' && step.fields && (
        <div className="amg-card card-clip p-5 space-y-4">
          <CredentialsForm
            fields={step.fields}
            formData={formData}
            onFieldChange={onFieldChange}
            errors={{}}
          />
        </div>
      )}

      {step.type === 'form' && step.fields && step.fields.length > 0 && (
        <div className="amg-card card-clip p-5 space-y-4">
          <CredentialsForm
            fields={step.fields}
            formData={formData}
            onFieldChange={onFieldChange}
            errors={{}}
          />
        </div>
      )}

      {step.type === 'info' && (
        <div className="amg-card card-clip p-5">
          <div className="flex items-start gap-3">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#FF6B00" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" className="flex-shrink-0 mt-0.5">
              <circle cx="12" cy="12" r="10" />
              <path d="M12 16v-4M12 8h.01" />
            </svg>
            <div>
              <div className="f-display font-bold text-sm mb-1">Informació</div>
              <p className="text-sm text-ink-2 leading-relaxed">
                Aquest pas no requereix acció. Revisa la informació i continua al següent pas.
              </p>
            </div>
          </div>
        </div>
      )}

      {step.type === 'copy' && step.fields && (
        <div className="amg-card card-clip p-5 space-y-4">
          {step.fields.map((field) => (
            <CopyField
              key={field.id}
              value={formData[field.id] ?? '---'}
              label={field.labelKey}
            />
          ))}
        </div>
      )}

      {step.type === 'verify' && (
        <div className="amg-card card-clip p-5 space-y-4">
          <div className="text-center py-4 space-y-4">
            <div className="text-sm text-ink-2">
              Prova la connexió per assegurar que tot funciona correctament.
            </div>
            <button
              onClick={handleVerify}
              disabled={verifyStatus === 'loading'}
              className="px-6 py-2.5 rounded btn-clip bg-accent-light text-black f-mono uppercase text-xs font-bold hover:bg-accent transition disabled:opacity-50 inline-flex items-center gap-2"
            >
              {verifyStatus === 'loading' ? (
                <>
                  <span className="w-3 h-3 border-2 border-black/60 border-t-transparent rounded-full animate-spin" />
                  Verificant...
                </>
              ) : (
                <>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                    <path d="m9 11 3 3L22 4" />
                  </svg>
                  Provar connexió
                </>
              )}
            </button>

            {verifyStatus !== 'idle' && (
              <div
                className={`p-3 rounded text-sm ${
                  verifyStatus === 'success'
                    ? 'bg-emerald-900/20 text-emerald-400 border border-emerald-500/30'
                    : verifyStatus === 'error'
                    ? 'bg-red-900/20 text-red-400 border border-red-500/30'
                    : 'bg-amber-900/20 text-amber-400 border border-amber-500/30'
                }`}
              >
                <div className="flex items-center gap-2 justify-center">
                  {verifyStatus === 'success' ? (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                      <path d="M20 6 9 17l-5-5" />
                    </svg>
                  ) : verifyStatus === 'error' ? (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                      <circle cx="12" cy="12" r="10" />
                      <path d="m15 9-6 6M9 9l6 6" />
                    </svg>
                  ) : null}
                  <span>{verifyMessage}</span>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {step.type === 'link' && (
        <div className="amg-card card-clip p-5 space-y-3">
          <p className="text-sm text-ink-2">
            Accedeix a l'enllaç extern per completar aquesta configuració:
          </p>
          <a
            href={formData['external_url'] ?? '#'}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 px-4 py-2 rounded btn-clip bg-accent-light text-black f-mono uppercase text-xs font-bold hover:bg-accent transition"
          >
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
              <path d="M15 3h6v6" />
              <path d="M10 14 21 3" />
            </svg>
            Obrir enllaç extern
          </a>
        </div>
      )}
    </div>
  );
}
