'use client';

import { useState, useEffect, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { AMGLogo } from '@/components/ui/AMGLogo';
import {
  previewBudget, acceptBudgetPhases, rejectBudget,
  type BudgetResponse, type BudgetPhase,
} from '@/services/billing';

function fmt(n: number) {
  return new Intl.NumberFormat('ca-ES', { style: 'currency', currency: 'EUR' }).format(n);
}

function fmtDate(d: string | null) {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'long', year: 'numeric' });
}

const SECTOR_LABELS: Record<string, string> = {
  PINTOR: 'Pintura i reforma', ELECTRICISTA: 'Electricitat', FONTANER: 'Lampisteria',
  JARDINER: 'Jardineria', NETEJA: 'Neteja', TALLER_MECANIC: 'Taller mecànic',
  FISIOTERAPEUTA: 'Fisioteràpia', PSICOLEG: 'Psicologia', NUTRICIONISTA: 'Nutrició',
  RESTAURANTE: 'Restaurant', ACADEMIA: 'Acadèmia', VETERINARI: 'Veterinari',
  PERRUQUERIA_CANINA: 'Perruqueria canina', PERRUQUERIA: 'Perruqueria',
  ESTETICA: 'Centre d\'estètica', GESTORIA: 'Gestoria', INMOBILIARIA: 'Immobiliària',
  AGENCIA_IA: 'Agència IA',
};

// ── Result screen ─────────────────────────────────────────────────────────────

function ResultScreen({ status, message }: { status: 'ACCEPTED' | 'REJECTED' | 'ERROR'; message: string }) {
  const isAccepted = status === 'ACCEPTED';
  const isError = status === 'ERROR';
  return (
    <div className="min-h-dvh bg-gray-50 flex items-center justify-center p-6">
      <div className="max-w-md w-full text-center space-y-6">
        <AMGLogo className="h-8 w-auto mx-auto" />
        <div className={`w-20 h-20 rounded-full flex items-center justify-center mx-auto ${
          isAccepted ? 'bg-green-100' : isError ? 'bg-amber-100' : 'bg-red-100'
        }`}>
          {isAccepted ? (
            <svg className="w-9 h-9 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
            </svg>
          ) : (
            <svg className="w-9 h-9 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01M12 3a9 9 0 100 18A9 9 0 0012 3z" />
            </svg>
          )}
        </div>
        <div>
          <h1 className={`text-2xl font-bold mb-2 ${
            isAccepted ? 'text-green-700' : isError ? 'text-amber-700' : 'text-red-700'
          }`}>
            {isAccepted ? 'Proposta acceptada' : isError ? 'Error' : 'Proposta rebutjada'}
          </h1>
          <p className="text-gray-500 text-sm leading-relaxed">{message}</p>
        </div>
        {isAccepted && (
          <div className="bg-green-50 border border-green-100 rounded-xl p-4 text-left space-y-2">
            <p className="text-green-800 text-xs font-semibold uppercase tracking-wider">Propers passos</p>
            <ul className="space-y-1">
              {['Et contactarem en menys de 24h per confirmar els detalls', 'Configurarem el sistema en 48h', 'Rebràs accés al teu panel de gestió'].map(s => (
                <li key={s} className="flex items-start gap-2 text-sm text-green-700">
                  <svg className="w-4 h-4 text-green-500 mt-0.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                  {s}
                </li>
              ))}
            </ul>
          </div>
        )}
        <p className="text-gray-400 text-xs">
          AMG Digitalitzacions · <a href="mailto:info@amgdl.com" className="hover:text-gray-600">info@amgdl.com</a>
        </p>
      </div>
    </div>
  );
}

// ── Phase card ────────────────────────────────────────────────────────────────

function PhaseCard({ phase, phaseNum, selected, recommended, onToggle }: {
  phase: BudgetPhase;
  phaseNum: number;
  selected: boolean;
  recommended: boolean;
  onToggle: () => void;
}) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div
      onClick={onToggle}
      className={`rounded-xl border-2 transition-all duration-150 cursor-pointer overflow-hidden ${
        selected
          ? 'border-[#FF6B00] shadow-[0_2px_16px_rgba(255,107,0,0.12)]'
          : 'border-gray-200 hover:border-gray-300 hover:shadow-sm'
      }`}
    >
      <div className="flex items-start gap-4 p-5">
        {/* Phase badge */}
        <div className={`w-11 h-11 rounded-lg flex items-center justify-center flex-shrink-0 font-bold text-sm transition-colors ${
          selected ? 'bg-[#FF6B00] text-white' : 'bg-gray-100 text-gray-500'
        }`}>
          {selected ? (
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
            </svg>
          ) : (
            <span className="text-sm font-bold">{phaseNum}</span>
          )}
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap mb-2">
            <span className="font-bold text-gray-900">{phase.name}</span>
            {recommended && (
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-amber-50 text-amber-700 border border-amber-200 font-bold uppercase tracking-wide">
                ★ Recomanada
              </span>
            )}
          </div>

          <div className="flex items-stretch gap-2.5">
            <div className="flex-1 rounded-lg bg-gray-50 border border-gray-100 px-3 py-2.5 text-center">
              <div className="text-gray-400 text-[10px] font-semibold uppercase tracking-wide mb-0.5">Setup</div>
              <div className="text-gray-900 font-bold text-base">{fmt(phase.phaseTotal)}</div>
              <div className="text-gray-400 text-[10px]">pagament únic</div>
            </div>
            <div className="flex-1 rounded-lg bg-[rgba(255,107,0,0.05)] border border-[rgba(255,107,0,0.15)] px-3 py-2.5 text-center">
              <div className="text-[#FF6B00] text-[10px] font-semibold uppercase tracking-wide mb-0.5">Mensual</div>
              <div className="text-[#FF6B00] font-bold text-base">{fmt(phase.phaseMonthlyTotal)}</div>
              <div className="text-gray-400 text-[10px]">recurrent</div>
            </div>
          </div>
        </div>

        <button
          type="button"
          onClick={e => { e.stopPropagation(); setExpanded(!expanded); }}
          className="text-gray-300 hover:text-gray-500 transition p-1 mt-0.5 shrink-0"
          title="Veure serveis inclosos"
        >
          <svg className={`w-5 h-5 transition-transform duration-200 ${expanded ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </button>
      </div>

      {expanded && (
        <div className="border-t border-gray-100" onClick={e => e.stopPropagation()}>
          <div className="px-5 pt-3 pb-1 text-gray-400 text-[10px] font-bold uppercase tracking-widest">Serveis inclosos</div>
          <div className="pb-3">
            {phase.lines.map((line, i) => (
              <div key={i} className="flex items-center justify-between px-5 py-2 odd:bg-gray-50/50">
                <span className="text-gray-600 text-sm flex-1 pr-4">{line.serviceName}</span>
                <div className="flex items-center gap-4 text-xs shrink-0 tabular-nums">
                  <span className="text-gray-400">{fmt(line.setupPrice)}</span>
                  <span className="text-[#FF6B00] font-medium w-20 text-right">{fmt(line.monthlyPrice)}/mes</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// ── Value strip ───────────────────────────────────────────────────────────────

const VALUE_ITEMS = [
  { icon: '🤖', label: 'Agent IA 24h' },
  { icon: '💬', label: 'WhatsApp Business' },
  { icon: '📊', label: 'Panel de gestió' },
  { icon: '🇪🇸', label: 'Suport en català' },
];

// ── Main acceptance page ──────────────────────────────────────────────────────

function AcceptBudgetContent() {
  const searchParams = useSearchParams();
  const token = searchParams.get('token') ?? '';

  const [budget, setBudget] = useState<BudgetResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedPhaseKeys, setSelectedPhaseKeys] = useState<Set<string>>(new Set());
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<{ status: 'ACCEPTED' | 'REJECTED' | 'ERROR'; message: string } | null>(null);
  const [rejectMode, setRejectMode] = useState(false);
  const [rejectReason, setRejectReason] = useState('');

  useEffect(() => {
    if (!token) { setError('Token no vàlid'); setLoading(false); return; }
    previewBudget(token)
      .then(b => {
        setBudget(b);
        const rec = b.recommendedPhaseIds ?? [];
        const keys = b.phases.map(p => p.phaseKey ?? p.phaseId).filter(Boolean) as string[];
        const recKeys = rec.length > 0
          ? rec.map(id => b.phases.find(p => p.phaseId === id)?.phaseKey ?? id).filter(Boolean) as string[]
          : keys;
        setSelectedPhaseKeys(new Set(recKeys));
      })
      .catch(() => setError('El pressupost no s\'ha trobat o el token ha caducat.'))
      .finally(() => setLoading(false));
  }, [token]);

  const togglePhase = (key: string) => {
    setSelectedPhaseKeys(prev => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key); else next.add(key);
      return next;
    });
  };

  const handleAccept = async () => {
    if (!budget || selectedPhaseKeys.size === 0) return;
    setSubmitting(true);
    try {
      await acceptBudgetPhases(token, Array.from(selectedPhaseKeys));
      setResult({ status: 'ACCEPTED', message: `Has acceptat ${selectedPhaseKeys.size} fase${selectedPhaseKeys.size !== 1 ? 's' : ''}. Gràcies per la teva confiança!` });
    } catch {
      setResult({ status: 'ERROR', message: 'Hi ha hagut un error en processar l\'acceptació. Contacta\'ns directament.' });
    } finally { setSubmitting(false); }
  };

  const handleReject = async () => {
    setSubmitting(true);
    try {
      await rejectBudget(token, rejectReason || undefined);
      setResult({ status: 'REJECTED', message: 'Ens posarem en contacte amb tu per entendre millor les teves necessitats.' });
    } catch {
      setResult({ status: 'ERROR', message: 'Hi ha hagut un error. Contacta\'ns directament.' });
    } finally { setSubmitting(false); }
  };

  if (result) return <ResultScreen status={result.status} message={result.message} />;

  if (loading) {
    return (
      <div className="min-h-dvh bg-gray-50 flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (error || !budget) {
    return (
      <div className="min-h-dvh bg-gray-50 flex items-center justify-center p-6">
        <div className="max-w-md text-center space-y-4">
          <AMGLogo className="h-8 w-auto mx-auto" />
          <div className="w-16 h-16 rounded-full bg-amber-100 flex items-center justify-center mx-auto">
            <svg className="w-7 h-7 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01M12 3a9 9 0 100 18A9 9 0 0012 3z" />
            </svg>
          </div>
          <h1 className="text-xl font-bold text-gray-800">Proposta no disponible</h1>
          <p className="text-gray-500 text-sm">{error ?? 'No s\'ha trobat el pressupost.'}</p>
          <a href="mailto:info@amgdl.com" className="inline-block text-sm text-[#FF6B00] hover:underline">info@amgdl.com</a>
        </div>
      </div>
    );
  }

  const allPhaseKeys = budget.phases.map(p => p.phaseKey ?? p.phaseId).filter(Boolean) as string[];
  const allSelected = allPhaseKeys.length > 0 && allPhaseKeys.every(k => selectedPhaseKeys.has(k));
  const noneSelected = selectedPhaseKeys.size === 0;
  const recIds = new Set(budget.recommendedPhaseIds ?? []);
  const isNexeLocal = !!(budget.phaseNumbers?.length);

  const selectedPhases = budget.phases.filter(p => selectedPhaseKeys.has(p.phaseKey ?? p.phaseId ?? ''));
  const selSetupTotal = selectedPhases.reduce((s, p) => s + p.phaseTotal, 0);
  const selMonthlyTotal = selectedPhases.reduce((s, p) => s + p.phaseMonthlyTotal, 0);
  const addonsSetup = budget.addons.reduce((s, a) => s + a.unitPrice, 0);

  const sectorLabel = budget.sector ? (SECTOR_LABELS[budget.sector] ?? budget.sector) : null;

  return (
    <div className="min-h-dvh bg-gray-50">

      {/* Sticky header */}
      <header className="bg-white border-b border-gray-100 sticky top-0 z-20 shadow-sm">
        <div className="max-w-2xl mx-auto px-5 py-3.5 flex items-center justify-between">
          <AMGLogo className="h-6 w-auto" />
          <div className="flex items-center gap-3">
            <span className="hidden sm:block text-gray-400 text-xs">Ref. {budget.budgetNumber}</span>
            <span className="text-xs text-gray-500 bg-gray-50 border border-gray-200 rounded-full px-3 py-1">
              Vàlid fins {fmtDate(budget.validUntil)}
            </span>
          </div>
        </div>
      </header>

      <div className="max-w-2xl mx-auto px-5 py-8 space-y-6 pb-32">

        {/* Cover card */}
        <div className="bg-white rounded-2xl shadow-sm overflow-hidden border border-gray-100">
          <div className="h-1.5 bg-gradient-to-r from-[#FF6B00] to-[#ff9a00]" />
          <div className="p-7 sm:p-8">
            <div className="flex items-start justify-between gap-4 mb-5">
              <div>
                {sectorLabel && (
                  <div className="inline-flex items-center gap-1.5 text-[11px] font-bold uppercase tracking-widest text-[#FF6B00] mb-2">
                    <span className="w-3.5 h-0.5 bg-[#FF6B00] inline-block rounded" />
                    {sectorLabel}
                  </div>
                )}
                <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 leading-tight">
                  {budget.tenantName
                    ? <>Proposta per a<br /><span className="text-[#FF6B00]">{budget.tenantName}</span></>
                    : 'La teva proposta personalitzada'}
                </h1>
              </div>
              <div className="shrink-0 text-right hidden sm:block">
                <div className="text-[10px] text-gray-400 uppercase tracking-wider mb-0.5">Referència</div>
                <div className="font-mono text-sm font-bold text-gray-700">{budget.budgetNumber}</div>
              </div>
            </div>

            <p className="text-gray-500 text-sm leading-relaxed mb-6">
              {isNexeLocal
                ? `Hem dissenyat aquesta proposta amb les fases de digitalització que millor s'adapten al teu negoci${sectorLabel ? ` del sector de ${sectorLabel.toLowerCase()}` : ''}. Revisa cada fase, selecciona les que vols activar i accepta per posar-ho en marxa.`
                : 'Hem preparat aquesta proposta tenint en compte el teu negoci i les teves necessitats. Revisa les fases disponibles i accepta-les per començar.'}
            </p>

            {/* Value strip */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {VALUE_ITEMS.map(item => (
                <div key={item.label} className="flex items-center gap-2 bg-gray-50 rounded-lg px-3 py-2.5">
                  <span className="text-base leading-none">{item.icon}</span>
                  <span className="text-xs font-medium text-gray-600">{item.label}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Technician recommendation */}
        {budget.recommendation && (
          <div className="bg-amber-50 rounded-2xl border border-amber-100 p-5">
            <div className="flex items-start gap-3">
              <div className="w-8 h-8 rounded-full bg-amber-100 flex items-center justify-center flex-shrink-0 mt-0.5">
                <svg className="w-4 h-4 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                </svg>
              </div>
              <div>
                <div className="text-amber-800 font-semibold text-xs uppercase tracking-wider mb-1">Recomanació del tècnic</div>
                <p className="text-amber-900 text-sm leading-relaxed">{budget.recommendation}</p>
              </div>
            </div>
          </div>
        )}

        {/* Client notes */}
        {budget.clientNotes && (
          <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5">
            <div className="text-gray-400 text-[10px] font-bold uppercase tracking-widest mb-2">Informació addicional</div>
            <p className="text-gray-600 text-sm leading-relaxed">{budget.clientNotes}</p>
          </div>
        )}

        {/* Phases */}
        {budget.phases.length > 0 && (
          <div>
            <div className="flex items-end justify-between mb-3">
              <div>
                <h2 className="text-base font-bold text-gray-900">Fases disponibles</h2>
                <p className="text-gray-400 text-xs mt-0.5">Selecciona les que vols activar</p>
              </div>
              {budget.phases.length > 1 && (
                <button type="button"
                  onClick={() => allSelected
                    ? setSelectedPhaseKeys(new Set())
                    : setSelectedPhaseKeys(new Set(allPhaseKeys))
                  }
                  className="text-[#FF6B00] text-xs font-semibold hover:underline shrink-0 ml-4 pb-0.5">
                  {allSelected ? 'Deseleccionar tot' : 'Seleccionar tot'}
                </button>
              )}
            </div>
            <div className="space-y-3">
              {budget.phases.map((phase, i) => {
                const key = phase.phaseKey ?? phase.phaseId ?? String(i);
                const phaseNum = isNexeLocal ? (budget.phaseNumbers?.[i] ?? i + 1) : i + 1;
                return (
                  <PhaseCard
                    key={key}
                    phase={phase}
                    phaseNum={phaseNum}
                    selected={selectedPhaseKeys.has(key)}
                    recommended={recIds.has(phase.phaseId ?? '')}
                    onToggle={() => togglePhase(key)}
                  />
                );
              })}
            </div>
          </div>
        )}

        {/* Addons */}
        {budget.addons.length > 0 && (
          <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5">
            <h2 className="font-bold text-gray-900 text-sm mb-3">Serveis addicionals inclosos</h2>
            <div className="space-y-1">
              {budget.addons.map((addon, i) => (
                <div key={i} className="flex items-center justify-between py-2 border-b border-gray-50 last:border-0">
                  <span className="text-gray-600 text-sm flex-1">{addon.serviceName}</span>
                  <span className="text-gray-800 text-sm font-semibold tabular-nums">{fmt(addon.unitPrice)}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Pricing summary */}
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
          <div className="px-5 py-3.5 border-b border-gray-50 flex items-center justify-between">
            <h2 className="font-bold text-gray-900 text-sm">Resum de la teva selecció</h2>
            <span className="text-xs text-gray-400">{selectedPhaseKeys.size} fase{selectedPhaseKeys.size !== 1 ? 's' : ''} seleccionada{selectedPhaseKeys.size !== 1 ? 's' : ''}</span>
          </div>
          <div className="grid grid-cols-2 divide-x divide-gray-50">
            <div className="p-6 text-center">
              <div className="text-gray-400 text-[10px] font-semibold uppercase tracking-widest mb-2">Inversió inicial</div>
              <div className="text-gray-900 font-bold text-3xl tabular-nums">{fmt(selSetupTotal + addonsSetup)}</div>
              {budget.discountTotal > 0 && (
                <div className="text-green-600 text-xs mt-1.5 font-medium">−{fmt(budget.discountTotal)} descompte</div>
              )}
              <div className="text-gray-400 text-xs mt-2">pagament únic · implementació inclosa</div>
            </div>
            <div className="p-6 text-center bg-[rgba(255,107,0,0.02)]">
              <div className="text-[#FF6B00] text-[10px] font-semibold uppercase tracking-widest mb-2">Quota mensual</div>
              <div className="text-[#FF6B00] font-bold text-3xl tabular-nums">
                {fmt(selMonthlyTotal)}<span className="text-base font-normal text-gray-400">/mes</span>
              </div>
              <div className="text-gray-400 text-xs mt-2">agent IA · suport · manteniment</div>
            </div>
          </div>
        </div>

        {/* Reject form */}
        {rejectMode && (
          <div className="bg-white rounded-2xl border border-gray-200 p-6 space-y-4">
            <h3 className="font-bold text-gray-900">Motiu del rebuig <span className="text-gray-400 font-normal text-sm">(opcional)</span></h3>
            <textarea
              value={rejectReason}
              onChange={e => setRejectReason(e.target.value)}
              placeholder="Explica'ns per què no t'encaixa o quines eren les teves expectatives. Ens ajudarà a millorar la proposta."
              rows={3}
              className="w-full bg-gray-50 border border-gray-200 rounded-xl px-4 py-3 text-sm text-gray-800 placeholder-gray-400 focus:outline-none focus:border-gray-400 resize-none"
            />
            <div className="flex gap-3">
              <button type="button" onClick={handleReject} disabled={submitting}
                className={`flex-1 py-3 rounded-xl bg-red-50 border border-red-200 text-red-600 font-semibold text-sm transition hover:bg-red-100 ${submitting ? 'opacity-60' : ''}`}>
                {submitting ? 'Processant...' : 'Confirmar rebuig'}
              </button>
              <button type="button" onClick={() => setRejectMode(false)}
                className="flex-1 py-3 rounded-xl border border-gray-200 text-gray-500 hover:text-gray-700 transition text-sm">
                Tornar enrere
              </button>
            </div>
          </div>
        )}

        {/* Footer */}
        <footer className="pt-4 pb-4 text-center space-y-1.5">
          <AMGLogo className="h-5 w-auto mx-auto opacity-40" />
          <p className="text-gray-400 text-xs">
            AMG Digitalitzacions · <a href="mailto:info@amgdl.com" className="hover:text-gray-600">info@amgdl.com</a>
          </p>
          <p className="text-gray-300 text-[10px]">
            Ref. {budget.budgetNumber} · Creat el {fmtDate(budget.createdAt)}
          </p>
        </footer>

      </div>

      {/* Sticky action bar */}
      {!rejectMode && (
        <div className="fixed bottom-0 left-0 right-0 z-30 bg-white/95 backdrop-blur border-t border-gray-100 shadow-[0_-4px_20px_rgba(0,0,0,0.08)]">
          <div className="max-w-2xl mx-auto px-5 py-4 flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
            {!noneSelected && (
              <div className="hidden sm:block shrink-0 text-right">
                <div className="text-[10px] text-gray-400 uppercase tracking-wider">Total selecció</div>
                <div className="text-sm font-bold text-gray-900 tabular-nums">
                  {fmt(selSetupTotal + addonsSetup)} + {fmt(selMonthlyTotal)}/mes
                </div>
              </div>
            )}
            <button type="button" onClick={handleAccept} disabled={submitting || noneSelected}
              className={`flex-1 py-3.5 rounded-xl font-bold text-sm transition ${
                noneSelected
                  ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                  : 'bg-[#FF6B00] hover:bg-[#e55a00] text-white shadow-[0_4px_14px_rgba(255,107,0,0.3)]'
              } ${submitting ? 'opacity-60' : ''}`}
            >
              {submitting ? 'Processant...'
                : noneSelected ? 'Selecciona almenys una fase'
                : `Acceptar ${selectedPhaseKeys.size} fase${selectedPhaseKeys.size !== 1 ? 's' : ''}`}
            </button>
            <button type="button" onClick={() => setRejectMode(true)} disabled={submitting}
              className="py-3.5 sm:px-5 rounded-xl border border-gray-200 text-gray-400 hover:text-gray-600 hover:border-gray-300 transition text-sm font-medium">
              No m&apos;interessa
            </button>
          </div>
        </div>
      )}

    </div>
  );
}

export default function AcceptBudgetPage() {
  return (
    <Suspense fallback={
      <div className="min-h-dvh bg-gray-50 flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
      </div>
    }>
      <AcceptBudgetContent />
    </Suspense>
  );
}
