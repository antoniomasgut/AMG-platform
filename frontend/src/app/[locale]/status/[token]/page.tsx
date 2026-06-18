'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { AMGLogo } from '@/components/ui/AMGLogo';
import { getProjectStatus, type ProjectStatus } from '@/services/projectStatus';

const WHATSAPP_URL = 'https://wa.me/34614492062';

function fmtDate(iso: string | null): string {
  if (!iso) return '';
  return new Date(iso).toLocaleDateString('ca-ES', { day: 'numeric', month: 'long', year: 'numeric' });
}

type StepState = 'done' | 'in_progress' | 'pending';

interface Step {
  key: string;
  label: string;
  sublabel?: string;
  state: StepState;
  date?: string | null;
}

function buildSteps(s: ProjectStatus): Step[] {
  return [
    {
      key: 'config',
      label: 'Configuració rebuda',
      sublabel: 'El teu pressupost ha estat processat.',
      state: s.configReceived ? 'done' : 'pending',
      date: s.configReceivedAt,
    },
    {
      key: 'payment',
      label: 'Pagament confirmat',
      sublabel: 'El servei és actiu a nivell administratiu.',
      state: s.paymentConfirmed ? 'done' : (s.configReceived ? 'in_progress' : 'pending'),
      date: s.paymentConfirmedAt,
    },
    {
      key: 'setup',
      label: 'Setup del negoci completat',
      sublabel: 'Hem rebut les dades del teu negoci.',
      state: s.setupCompleted ? 'done' : (s.paymentConfirmed ? 'in_progress' : 'pending'),
      date: s.setupCompletedAt,
    },
    {
      key: 'implementing',
      label: 'Implementació tècnica',
      sublabel: 'El nostre equip configura el teu agent i canals.',
      state: s.active ? 'done' : (s.implementing ? 'in_progress' : 'pending'),
    },
    {
      key: 'active',
      label: 'Agent actiu',
      sublabel: s.active ? 'El teu agent ja respon als clients.' : 'Estimat: 24-48h des del setup completat.',
      state: s.active ? 'done' : 'pending',
    },
  ];
}

function StepIcon({ state }: { state: StepState }) {
  if (state === 'done') {
    return (
      <div className="w-8 h-8 bg-[#ff6b00] flex items-center justify-center shrink-0">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="black" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
          <polyline points="20 6 9 17 4 12" />
        </svg>
      </div>
    );
  }
  if (state === 'in_progress') {
    return (
      <div className="w-8 h-8 border-2 border-[#ff6b00] flex items-center justify-center shrink-0">
        <span className="w-2 h-2 bg-[#ff6b00] rounded-full animate-pulse" />
      </div>
    );
  }
  return (
    <div className="w-8 h-8 border border-[#333] flex items-center justify-center shrink-0">
      <span className="w-2 h-2 bg-[#333] rounded-full" />
    </div>
  );
}

export default function ProjectStatusPage() {
  const { token } = useParams<{ token: string }>();
  const [status, setStatus] = useState<ProjectStatus | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    if (!token) return;
    getProjectStatus(token)
      .then(setStatus)
      .catch(() => setError(true));
  }, [token]);

  return (
    <div className="min-h-screen bg-[#0a0a0a] text-[#f0f0f0] flex flex-col">
      {/* Header */}
      <header className="border-b border-[#1a1a1a] px-6 py-4 flex items-center gap-3">
        <div className="w-1.5 h-6 bg-[#ff6b00]" />
        <AMGLogo className="h-6" />
        <span className="font-mono text-xs text-[#555] uppercase tracking-widest ml-2">
          Estat del projecte
        </span>
      </header>

      <main className="flex-1 flex items-start justify-center px-6 py-12">
        <div className="w-full max-w-xl">
          {error ? (
            <div className="border border-[#333] p-8 text-center">
              <div className="font-mono text-xs text-[#555] uppercase tracking-widest mb-3">Enllaç no vàlid</div>
              <p className="text-sm text-[#888]">No hem pogut trobar l&apos;estat d&apos;aquest projecte.</p>
              <a href={WHATSAPP_URL} target="_blank" rel="noopener noreferrer"
                className="inline-block mt-6 font-mono text-xs uppercase tracking-wider text-[#ff6b00] hover:text-orange-300 transition-colors">
                Contacta&apos;ns per WhatsApp →
              </a>
            </div>
          ) : !status ? (
            <div className="flex justify-center py-16">
              <span className="w-5 h-5 border-2 border-[#ff6b00] border-t-transparent rounded-full animate-spin" />
            </div>
          ) : (
            <>
              {/* Capçalera del projecte */}
              <div className="mb-10">
                <div className="font-mono text-xs text-[#555] uppercase tracking-widest mb-2">
                  {status.sector ?? 'Projecte'}
                </div>
                <h1 className="font-black text-3xl uppercase tracking-tight leading-tight">
                  {status.tenantName ?? 'El teu projecte'}
                </h1>
              </div>

              {/* Steps */}
              <div className="space-y-0 border border-[#1a1a1a]">
                {buildSteps(status).map((step, i, arr) => (
                  <div
                    key={step.key}
                    className={`flex items-start gap-4 p-5 ${i < arr.length - 1 ? 'border-b border-[#1a1a1a]' : ''} ${
                      step.state === 'in_progress' ? 'bg-[#111]' : ''
                    }`}
                  >
                    <StepIcon state={step.state} />
                    <div className="flex-1 min-w-0">
                      <div className={`font-bold text-sm ${step.state === 'pending' ? 'text-[#555]' : 'text-[#f0f0f0]'}`}>
                        {step.label}
                      </div>
                      {step.sublabel && (
                        <div className="font-mono text-[11px] text-[#666] mt-0.5">{step.sublabel}</div>
                      )}
                    </div>
                    {step.date && (
                      <div className="font-mono text-[10px] text-[#555] shrink-0 text-right">
                        {fmtDate(step.date)}
                      </div>
                    )}
                    {step.state === 'in_progress' && !step.date && (
                      <div className="font-mono text-[10px] text-[#ff6b00] shrink-0">En curs</div>
                    )}
                  </div>
                ))}
              </div>

              {/* CTA si actiu */}
              {status.active && (
                <div className="mt-8 border border-[#ff6b00]/30 bg-[#ff6b00]/5 p-6 text-center">
                  <div className="font-bold text-sm mb-1">El teu agent ja està actiu 🎉</div>
                  <p className="font-mono text-xs text-[#888] mb-4">Accedeix al teu panel de gestió.</p>
                  <a href={status.portalUrl}
                    className="inline-block bg-[#ff6b00] text-black font-semibold font-mono text-xs uppercase px-8 py-3 hover:bg-orange-400 transition-colors">
                    Accedir al portal →
                  </a>
                </div>
              )}

              {/* Peu */}
              <div className="mt-10 pt-6 border-t border-[#1a1a1a] flex items-center justify-between">
                <span className="font-mono text-[10px] text-[#444] uppercase tracking-widest">
                  Tens dubtes?
                </span>
                <a href={WHATSAPP_URL} target="_blank" rel="noopener noreferrer"
                  className="font-mono text-[11px] text-[#ff6b00] hover:text-orange-300 transition-colors uppercase tracking-wider">
                  WhatsApp +34 614 492 062 →
                </a>
              </div>
            </>
          )}
        </div>
      </main>
    </div>
  );
}
