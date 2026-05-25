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

// ── Result screen ─────────────────────────────────────────────────────────────

function ResultScreen({ status, message }: { status: 'ACCEPTED' | 'REJECTED' | 'ERROR'; message: string }) {
  const isAccepted = status === 'ACCEPTED';
  const isError = status === 'ERROR';
  return (
    <div className="min-h-dvh bg-[#fafafa] flex items-center justify-center p-6">
      <div className="max-w-md w-full text-center space-y-6">
        <AMGLogo className="h-8 w-auto mx-auto" />
        <div className={`w-16 h-16 rounded-full flex items-center justify-center mx-auto ${
          isAccepted ? 'bg-green-100' : isError ? 'bg-amber-100' : 'bg-red-100'
        }`}>
          <span className="text-3xl">{isAccepted ? '✓' : isError ? '!' : '✕'}</span>
        </div>
        <div>
          <h1 className={`text-2xl font-bold mb-2 ${
            isAccepted ? 'text-green-700' : isError ? 'text-amber-700' : 'text-red-700'
          }`}>
            {isAccepted ? 'Proposta acceptada' : isError ? 'Error' : 'Proposta rebutjada'}
          </h1>
          <p className="text-gray-500 text-sm">{message}</p>
        </div>
        {isAccepted && (
          <p className="text-gray-400 text-xs">
            El nostre equip es posarà en contacte amb tu ben aviat per iniciar la implementació.
          </p>
        )}
      </div>
    </div>
  );
}

// ── Phase card ────────────────────────────────────────────────────────────────

function PhaseCard({ phase, index, selected, recommended, onToggle }: {
  phase: BudgetPhase;
  index: number;
  selected: boolean;
  recommended: boolean;
  onToggle: () => void;
}) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div
      onClick={onToggle}
      className={`rounded-2xl border-2 transition-all cursor-pointer ${
        selected
          ? 'border-[#FF6B00] shadow-[0_0_0_4px_rgba(255,107,0,0.08)]'
          : 'border-gray-200 hover:border-gray-300'
      }`}
    >
      <div className="flex items-start gap-4 p-5 sm:p-6">
        {/* Number + checkbox */}
        <div className={`w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 font-bold text-sm transition-colors ${
          selected ? 'bg-[#FF6B00] text-white' : 'bg-gray-100 text-gray-500'
        }`}>
          {selected ? (
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
            </svg>
          ) : (
            <span>{index + 1}</span>
          )}
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap mb-1">
            <span className="font-bold text-gray-900 text-lg leading-tight">{phase.name}</span>
            {recommended && (
              <span className="text-xs px-2.5 py-0.5 rounded-full bg-amber-50 text-amber-700 border border-amber-200 font-semibold">
                ★ Recomanada
              </span>
            )}
          </div>
          <div className="text-gray-400 text-sm mb-4">{phase.lines.length} servei{phase.lines.length !== 1 ? 's' : ''} inclòs{phase.lines.length !== 1 ? 'os' : ''}</div>

          {/* Preus */}
          <div className="flex items-stretch gap-3">
            <div className="flex-1 rounded-xl bg-gray-50 border border-gray-100 px-4 py-3 text-center">
              <div className="text-gray-400 text-xs font-medium uppercase tracking-wide mb-1">Inversió inicial</div>
              <div className="text-gray-900 font-bold text-xl">{fmt(phase.phaseTotal)}</div>
              <div className="text-gray-400 text-xs mt-0.5">pagament únic</div>
            </div>
            <div className="flex-1 rounded-xl bg-[rgba(255,107,0,0.04)] border border-[rgba(255,107,0,0.15)] px-4 py-3 text-center">
              <div className="text-[#FF6B00] text-xs font-medium uppercase tracking-wide mb-1">Quota mensual</div>
              <div className="text-[#FF6B00] font-bold text-xl">{fmt(phase.phaseMonthlyTotal)}</div>
              <div className="text-gray-400 text-xs mt-0.5">recurrent</div>
            </div>
          </div>
        </div>

        {/* Expand */}
        <button
          type="button"
          onClick={e => { e.stopPropagation(); setExpanded(!expanded); }}
          className="text-gray-300 hover:text-gray-600 transition p-1 mt-1 shrink-0"
          title="Veure detall"
        >
          <svg className={`w-5 h-5 transition-transform ${expanded ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </button>
      </div>

      {/* Expanded service list */}
      {expanded && (
        <div className="border-t border-gray-100" onClick={e => e.stopPropagation()}>
          <div className="px-6 py-3 text-gray-400 text-xs font-semibold uppercase tracking-wider">Serveis inclosos</div>
          <div className="divide-y divide-gray-50 pb-3">
            {phase.lines.map((line, i) => (
              <div key={i} className="flex items-center justify-between px-6 py-2.5">
                <span className="text-gray-600 text-sm flex-1">{line.serviceName}</span>
                <div className="flex items-center gap-4 text-xs shrink-0 ml-4">
                  <span className="text-gray-400">{fmt(line.setupPrice)}</span>
                  <span className="text-[#FF6B00] font-medium">{fmt(line.monthlyPrice)}/mes</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

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
      if (next.has(key)) next.delete(key);
      else next.add(key);
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
      <div className="min-h-dvh bg-[#fafafa] flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (error || !budget) {
    return (
      <div className="min-h-dvh bg-[#fafafa] flex items-center justify-center p-6">
        <div className="max-w-md text-center space-y-4">
          <AMGLogo className="h-8 w-auto mx-auto" />
          <div className="w-16 h-16 rounded-full bg-amber-100 flex items-center justify-center mx-auto">
            <span className="text-3xl text-amber-600">!</span>
          </div>
          <h1 className="text-xl font-bold text-gray-800">Proposta no disponible</h1>
          <p className="text-gray-500 text-sm">{error ?? 'No s\'ha trobat el pressupost.'}</p>
        </div>
      </div>
    );
  }

  const allPhaseKeys = budget.phases.map(p => p.phaseKey ?? p.phaseId).filter(Boolean) as string[];
  const allSelected = allPhaseKeys.length > 0 && allPhaseKeys.every(k => selectedPhaseKeys.has(k));
  const noneSelected = selectedPhaseKeys.size === 0;
  const recIds = new Set(budget.recommendedPhaseIds ?? []);

  const selectedPhases = budget.phases.filter(p => selectedPhaseKeys.has(p.phaseKey ?? p.phaseId ?? ''));
  const selSetupTotal = selectedPhases.reduce((s, p) => s + p.phaseTotal, 0);
  const selMonthlyTotal = selectedPhases.reduce((s, p) => s + p.phaseMonthlyTotal, 0);
  const addonsSetup = budget.addons.reduce((s, a) => s + a.unitPrice, 0);

  return (
    <div className="min-h-dvh bg-[#fafafa]">

      {/* Header */}
      <header className="bg-white border-b border-gray-100 sticky top-0 z-10 shadow-sm">
        <div className="max-w-3xl mx-auto px-6 py-4 flex items-center justify-between">
          <AMGLogo className="h-7 w-auto" />
          <div className="text-right">
            <div className="text-gray-400 text-xs">Vàlid fins al</div>
            <div className="text-gray-700 text-sm font-semibold">{fmtDate(budget.validUntil)}</div>
          </div>
        </div>
      </header>

      <div className="max-w-3xl mx-auto px-6 py-10 space-y-10">

        {/* Hero */}
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-8">
          <div className="text-[#FF6B00] text-xs font-bold uppercase tracking-widest mb-3">Proposta de serveis digitals</div>
          <h1 className="text-3xl font-bold text-gray-900 mb-2">La teva proposta personalitzada</h1>
          <p className="text-gray-500 text-base leading-relaxed mb-6">
            Hem preparat aquesta proposta tenint en compte el teu negoci i les teves necessitats.
            Revisa les fases disponibles, selecciona les que t&apos;interessen i accepta-les per començar.
          </p>
          <div className="flex items-center gap-3 flex-wrap">
            <span className="inline-flex items-center gap-1.5 text-xs text-gray-500 bg-gray-50 border border-gray-100 rounded-full px-3 py-1.5">
              <span className="w-1.5 h-1.5 rounded-full bg-gray-400 inline-block" />
              Ref. {budget.budgetNumber}
            </span>
            <span className="inline-flex items-center gap-1.5 text-xs text-gray-500 bg-gray-50 border border-gray-100 rounded-full px-3 py-1.5">
              <span className="w-1.5 h-1.5 rounded-full bg-green-400 inline-block" />
              {budget.phases.length} fase{budget.phases.length !== 1 ? 's' : ''} disponibles
            </span>
            {budget.tenantName && (
              <span className="inline-flex items-center gap-1.5 text-xs text-gray-500 bg-gray-50 border border-gray-100 rounded-full px-3 py-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-[#FF6B00] inline-block" />
                {budget.tenantName}
              </span>
            )}
          </div>
        </div>

        {/* Recomanació del tècnic */}
        {budget.recommendation && (
          <div className="bg-amber-50 rounded-2xl border border-amber-100 p-6">
            <div className="flex items-start gap-3">
              <div className="w-9 h-9 rounded-full bg-amber-100 flex items-center justify-center flex-shrink-0 mt-0.5">
                <svg className="w-4 h-4 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                </svg>
              </div>
              <div>
                <div className="text-amber-800 font-bold text-sm mb-1.5">Recomanació del tècnic</div>
                <p className="text-amber-900 text-sm leading-relaxed">{budget.recommendation}</p>
              </div>
            </div>
          </div>
        )}

        {/* Notes per al client */}
        {budget.clientNotes && (
          <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
            <div className="text-gray-400 text-xs font-bold uppercase tracking-wider mb-2">Informació addicional</div>
            <p className="text-gray-600 text-sm leading-relaxed">{budget.clientNotes}</p>
          </div>
        )}

        {/* Fases */}
        {budget.phases.length > 0 && (
          <div>
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-xl font-bold text-gray-900">Fases del projecte</h2>
                <p className="text-gray-400 text-sm mt-0.5">Fes clic sobre cada fase per seleccionar-la o desseleccionar-la</p>
              </div>
              {budget.phases.length > 1 && (
                <button type="button"
                  onClick={() => allSelected
                    ? setSelectedPhaseKeys(new Set())
                    : setSelectedPhaseKeys(new Set(allPhaseKeys))
                  }
                  className="text-[#FF6B00] text-xs font-semibold hover:underline shrink-0 ml-4">
                  {allSelected ? 'Deseleccionar tot' : 'Seleccionar tot'}
                </button>
              )}
            </div>
            <div className="space-y-4 bg-white rounded-2xl border border-gray-100 shadow-sm p-4 sm:p-6">
              {budget.phases.map((phase, i) => {
                const key = phase.phaseKey ?? phase.phaseId ?? String(i);
                return (
                  <PhaseCard
                    key={key}
                    phase={phase}
                    index={i}
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
          <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
            <h2 className="font-bold text-gray-900 mb-4">Serveis addicionals inclosos</h2>
            <div className="space-y-2">
              {budget.addons.map((addon, i) => (
                <div key={i} className="flex items-center justify-between py-2 border-b border-gray-50 last:border-0">
                  <span className="text-gray-600 text-sm">{addon.serviceName}</span>
                  <span className="text-gray-800 text-sm font-semibold">{fmt(addon.unitPrice)}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Resum dinàmic */}
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
          <div className="px-6 py-4 border-b border-gray-50">
            <h2 className="font-bold text-gray-900">
              Resum de la teva selecció
              <span className="ml-2 text-sm font-normal text-gray-400">
                {selectedPhaseKeys.size} fase{selectedPhaseKeys.size !== 1 ? 's' : ''} seleccionada{selectedPhaseKeys.size !== 1 ? 's' : ''}
              </span>
            </h2>
          </div>
          <div className="grid grid-cols-2 divide-x divide-gray-50">
            <div className="p-6 text-center">
              <div className="text-gray-400 text-xs font-medium uppercase tracking-wide mb-2">Inversió inicial (únic)</div>
              <div className="text-gray-900 font-bold text-3xl">
                {fmt(selSetupTotal + addonsSetup)}
              </div>
              {budget.discountTotal > 0 && (
                <div className="text-green-600 text-xs mt-1.5 font-medium">−{fmt(budget.discountTotal)} descompte aplicat</div>
              )}
              <div className="text-gray-400 text-xs mt-1">pagament únic per posar en marxa els serveis</div>
            </div>
            <div className="p-6 text-center bg-[rgba(255,107,0,0.02)]">
              <div className="text-[#FF6B00] text-xs font-medium uppercase tracking-wide mb-2">Quota mensual</div>
              <div className="text-[#FF6B00] font-bold text-3xl">
                {fmt(selMonthlyTotal)}<span className="text-base font-normal text-gray-400">/mes</span>
              </div>
              <div className="text-gray-400 text-xs mt-1">inclou manteniment, suport i agent IA 24h</div>
            </div>
          </div>
        </div>

        {/* Accions */}
        {!rejectMode ? (
          <div className="space-y-3 pb-4">
            <button
              type="button"
              onClick={handleAccept}
              disabled={submitting || noneSelected}
              className={`w-full py-4 rounded-2xl font-bold text-base transition shadow-sm ${
                noneSelected
                  ? 'bg-gray-100 text-gray-400 cursor-not-allowed shadow-none'
                  : 'bg-[#FF6B00] hover:bg-[#e55a00] text-white shadow-[0_4px_14px_rgba(255,107,0,0.35)]'
              } ${submitting ? 'opacity-60' : ''}`}
            >
              {submitting ? 'Processant...'
                : noneSelected ? 'Selecciona almenys una fase per continuar'
                : `Acceptar proposta (${selectedPhaseKeys.size} fase${selectedPhaseKeys.size !== 1 ? 's' : ''})`}
            </button>
            <button
              type="button"
              onClick={() => setRejectMode(true)}
              disabled={submitting}
              className="w-full py-3 rounded-2xl border border-gray-200 text-gray-400 hover:text-gray-600 hover:border-gray-300 transition text-sm font-medium"
            >
              No m&apos;interessa de moment
            </button>
          </div>
        ) : (
          <div className="space-y-4 bg-white rounded-2xl border border-gray-200 p-6 mb-4">
            <h3 className="font-bold text-gray-900">Motiu del rebuig <span className="text-gray-400 font-normal text-sm">(opcional)</span></h3>
            <textarea
              value={rejectReason}
              onChange={e => setRejectReason(e.target.value)}
              placeholder="Explica'ns per què no t'encaixa o quines eren les teves expectatives. Ens ajudarà a millorar la proposta."
              rows={3}
              className="w-full bg-gray-50 border border-gray-200 rounded-xl px-4 py-3 text-sm text-gray-800 placeholder-gray-400 focus:outline-none focus:border-gray-400 resize-none"
            />
            <div className="flex gap-3">
              <button
                type="button"
                onClick={handleReject}
                disabled={submitting}
                className={`flex-1 py-3 rounded-xl bg-red-50 border border-red-200 text-red-600 font-semibold text-sm transition hover:bg-red-100 ${submitting ? 'opacity-60' : ''}`}
              >
                {submitting ? 'Processant...' : 'Confirmar rebuig'}
              </button>
              <button
                type="button"
                onClick={() => setRejectMode(false)}
                className="flex-1 py-3 rounded-xl border border-gray-200 text-gray-500 hover:text-gray-700 transition text-sm"
              >
                Tornar enrere
              </button>
            </div>
          </div>
        )}

        {/* Footer */}
        <footer className="border-t border-gray-100 pt-6 pb-10 text-center space-y-3">
          <AMGLogo className="h-6 w-auto mx-auto opacity-50" />
          <p className="text-gray-400 text-xs">
            AMG Digitalitzacions · <a href="mailto:hola@amgdl.com" className="hover:text-gray-600">hola@amgdl.com</a>
          </p>
          <p className="text-gray-300 text-xs">
            Ref. {budget.budgetNumber} · Creat el {fmtDate(budget.createdAt)}
          </p>
        </footer>

      </div>
    </div>
  );
}

export default function AcceptBudgetPage() {
  return (
    <Suspense fallback={
      <div className="min-h-dvh bg-[#fafafa] flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
      </div>
    }>
      <AcceptBudgetContent />
    </Suspense>
  );
}
