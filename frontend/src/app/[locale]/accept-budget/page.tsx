'use client';

import { useState, useEffect, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
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
    <div className="min-h-dvh bg-[#0a0a0a] flex items-center justify-center p-6">
      <div className="max-w-md w-full text-center space-y-6">
        <div className={`w-16 h-16 rounded-full flex items-center justify-center mx-auto ${
          isAccepted ? 'bg-green-500/20' : isError ? 'bg-yellow-500/20' : 'bg-red-500/20'
        }`}>
          <span className="text-3xl">{isAccepted ? '✓' : isError ? '!' : '✕'}</span>
        </div>
        <div>
          <h1 className={`text-2xl font-bold mb-2 ${
            isAccepted ? 'text-green-400' : isError ? 'text-yellow-400' : 'text-red-400'
          }`}>
            {isAccepted ? 'Fases acceptades' : isError ? 'Error' : 'Pressupost rebutjat'}
          </h1>
          <p className="text-[#94a3b8] text-sm">{message}</p>
        </div>
        {isAccepted && (
          <p className="text-[#64748b] text-xs">
            El nostre equip es posarà en contacte amb tu per iniciar la implementació dels serveis acceptats.
          </p>
        )}
      </div>
    </div>
  );
}

// ── Phase card ────────────────────────────────────────────────────────────────

function PhaseCard({ phase, selected, recommended, onToggle }: {
  phase: BudgetPhase;
  selected: boolean;
  recommended: boolean;
  onToggle: () => void;
}) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div
      onClick={onToggle}
      className={`rounded-xl border-2 transition-all cursor-pointer ${
        selected
          ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.06)]'
          : 'border-[#1e293b] bg-[#0f172a] hover:border-[#334155]'
      }`}
    >
      <div className="flex items-start gap-4 p-5">
        {/* Checkbox */}
        <div className={`mt-0.5 w-5 h-5 rounded border-2 flex items-center justify-center flex-shrink-0 transition-colors ${
          selected ? 'border-[#FF6B00] bg-[#FF6B00]' : 'border-[#334155]'
        }`}>
          {selected && (
            <svg className="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
            </svg>
          )}
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="font-semibold text-white text-base">{phase.name}</span>
            {recommended && (
              <span className="text-xs px-2 py-0.5 rounded-full bg-amber-500/15 text-amber-400 border border-amber-500/30 font-medium">
                Recomanada
              </span>
            )}
          </div>
          <div className="text-[#64748b] text-xs mt-0.5">{phase.lines.length} serveis inclosos</div>

          {/* Preus */}
          <div className="flex items-center gap-4 mt-3">
            <div className="text-center">
              <div className="text-[#64748b] text-xs">Setup (únic)</div>
              <div className="text-white font-bold font-mono">{fmt(phase.phaseTotal)}</div>
            </div>
            <div className="w-px h-8 bg-[#1e293b]" />
            <div className="text-center">
              <div className="text-[#64748b] text-xs">Mensual</div>
              <div className="text-[#FF6B00] font-bold font-mono">{fmt(phase.phaseMonthlyTotal)}<span className="text-xs font-normal text-[#64748b]">/mes</span></div>
            </div>
          </div>
        </div>

        {/* Expand button */}
        <button
          type="button"
          onClick={e => { e.stopPropagation(); setExpanded(!expanded); }}
          className="text-[#64748b] hover:text-white transition p-1 mt-1 shrink-0"
          title="Veure serveis inclosos"
        >
          <svg className={`w-4 h-4 transition-transform ${expanded ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </button>
      </div>

      {/* Expanded service list */}
      {expanded && (
        <div className="border-t border-[#1e293b]" onClick={e => e.stopPropagation()}>
          <div className="px-5 py-2 text-[#64748b] text-xs font-mono uppercase tracking-wider">Serveis inclosos</div>
          <div className="divide-y divide-[#0f172a] pb-2">
            {phase.lines.map((line, i) => (
              <div key={i} className="flex items-center justify-between px-5 py-2.5">
                <span className="text-[#94a3b8] text-sm flex-1">{line.serviceName}</span>
                <div className="flex items-center gap-4 text-xs font-mono shrink-0">
                  <span className="text-[#64748b]">{fmt(line.setupPrice)}</span>
                  <span className="text-white">{fmt(line.monthlyPrice)}/mes</span>
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
  const [selectedPhaseIds, setSelectedPhaseIds] = useState<Set<string>>(new Set());
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<{ status: 'ACCEPTED' | 'REJECTED' | 'ERROR'; message: string } | null>(null);
  const [rejectMode, setRejectMode] = useState(false);
  const [rejectReason, setRejectReason] = useState('');

  useEffect(() => {
    if (!token) { setError('Token no vàlid'); setLoading(false); return; }
    previewBudget(token)
      .then(b => {
        setBudget(b);
        // Pre-select recomanades; si no n'hi ha, seleccionar totes
        const rec = b.recommendedPhaseIds ?? [];
        const ids = rec.length > 0 ? new Set(rec) : new Set(b.phases.map(p => p.phaseId).filter(Boolean));
        setSelectedPhaseIds(ids as Set<string>);
      })
      .catch(() => setError('El pressupost no s\'ha trobat o el token ha caducat.'))
      .finally(() => setLoading(false));
  }, [token]);

  const togglePhase = (phaseId: string) => {
    setSelectedPhaseIds(prev => {
      const next = new Set(prev);
      if (next.has(phaseId)) next.delete(phaseId);
      else next.add(phaseId);
      return next;
    });
  };

  const handleAccept = async () => {
    if (!budget || selectedPhaseIds.size === 0) return;
    setSubmitting(true);
    try {
      await acceptBudgetPhases(token, Array.from(selectedPhaseIds));
      setResult({ status: 'ACCEPTED', message: 'Fases acceptades correctament. Gràcies!' });
    } catch {
      setResult({ status: 'ERROR', message: 'Hi ha hagut un error en processar l\'acceptació. Contacta\'ns directament.' });
    } finally { setSubmitting(false); }
  };

  const handleReject = async () => {
    setSubmitting(true);
    try {
      await rejectBudget(token, rejectReason || undefined);
      setResult({ status: 'REJECTED', message: 'Pressupost rebutjat. Ens posarem en contacte amb tu.' });
    } catch {
      setResult({ status: 'ERROR', message: 'Hi ha hagut un error. Contacta\'ns directament.' });
    } finally { setSubmitting(false); }
  };

  if (result) return <ResultScreen status={result.status} message={result.message} />;

  if (loading) {
    return (
      <div className="min-h-dvh bg-[#0a0a0a] flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (error || !budget) {
    return (
      <div className="min-h-dvh bg-[#0a0a0a] flex items-center justify-center p-6">
        <div className="max-w-md text-center space-y-4">
          <div className="w-16 h-16 rounded-full bg-yellow-500/20 flex items-center justify-center mx-auto">
            <span className="text-3xl">!</span>
          </div>
          <h1 className="text-xl font-bold text-yellow-400">Pressupost no disponible</h1>
          <p className="text-[#94a3b8] text-sm">{error ?? 'No s\'ha trobat el pressupost.'}</p>
        </div>
      </div>
    );
  }

  const allPhaseIds = budget.phases.map(p => p.phaseId);
  const allSelected = allPhaseIds.length > 0 && allPhaseIds.every(id => selectedPhaseIds.has(id));
  const noneSelected = selectedPhaseIds.size === 0;
  const recIds = new Set(budget.recommendedPhaseIds ?? []);

  // Totals dinàmics basats en fases seleccionades
  const selectedPhases = budget.phases.filter(p => selectedPhaseIds.has(p.phaseId));
  const selSetupTotal = selectedPhases.reduce((s, p) => s + p.phaseTotal, 0);
  const selMonthlyTotal = selectedPhases.reduce((s, p) => s + p.phaseMonthlyTotal, 0);
  const addonsSetup = budget.addons.reduce((s, a) => s + a.unitPrice, 0);

  return (
    <div className="min-h-dvh bg-[#0a0a0a] text-white">
      {/* Header */}
      <div className="border-b border-[#1e293b] bg-[#0a0a0a] sticky top-0 z-10">
        <div className="max-w-2xl mx-auto px-6 py-4 flex items-center justify-between">
          <div>
            <div className="text-[#FF6B00] text-xs font-mono uppercase tracking-widest">AMG Digitalitzacions</div>
            <div className="font-bold text-white">{budget.budgetNumber}</div>
          </div>
          <div className="text-right">
            <div className="text-[#64748b] text-xs">Vàlid fins</div>
            <div className="text-sm font-mono text-white">{fmtDate(budget.validUntil)}</div>
          </div>
        </div>
      </div>

      <div className="max-w-2xl mx-auto px-6 py-8 space-y-8">

        {/* Títol */}
        <div>
          <h1 className="text-2xl font-bold mb-2">La teva proposta de serveis</h1>
          <p className="text-[#94a3b8] text-sm">
            Revisa les fases proposades, selecciona les que t&apos;interessen i accepta.
            Pots triar les fases que vulguis — no cal acceptar-les totes.
          </p>
        </div>

        {/* Recomanació del tècnic */}
        {budget.recommendation && (
          <div className="rounded-xl bg-amber-500/8 border border-amber-500/25 p-5">
            <div className="flex items-center gap-2 mb-2">
              <svg className="w-4 h-4 text-amber-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
              </svg>
              <span className="text-amber-400 text-xs font-mono uppercase tracking-wider font-semibold">Recomanació del tècnic</span>
            </div>
            <p className="text-[#cbd5e1] text-sm leading-relaxed">{budget.recommendation}</p>
          </div>
        )}

        {/* Notes del client */}
        {budget.clientNotes && (
          <div className="rounded-xl bg-[#0f172a] border border-[#1e293b] p-5">
            <div className="text-[#64748b] text-xs font-mono uppercase tracking-wider mb-2">Informació addicional</div>
            <p className="text-[#94a3b8] text-sm leading-relaxed">{budget.clientNotes}</p>
          </div>
        )}

        {/* Fases */}
        {budget.phases.length > 0 && (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="font-semibold text-white">Fases del projecte</h2>
              {budget.phases.length > 1 && (
                <button type="button"
                  onClick={() => allSelected
                    ? setSelectedPhaseIds(new Set())
                    : setSelectedPhaseIds(new Set(allPhaseIds))
                  }
                  className="text-[#FF6B00] text-xs hover:underline">
                  {allSelected ? 'Deseleccionar tot' : 'Seleccionar tot'}
                </button>
              )}
            </div>
            {budget.phases.map((phase) => (
              <PhaseCard
                key={phase.phaseId}
                phase={phase}
                selected={selectedPhaseIds.has(phase.phaseId)}
                recommended={recIds.has(phase.phaseId)}
                onToggle={() => togglePhase(phase.phaseId)}
              />
            ))}
          </div>
        )}

        {/* Addons */}
        {budget.addons.length > 0 && (
          <div className="rounded-xl bg-[#0f172a] border border-[#1e293b] p-5">
            <h2 className="font-semibold text-white mb-3">Serveis addicionals (inclosos)</h2>
            <div className="space-y-2">
              {budget.addons.map((addon, i) => (
                <div key={i} className="flex items-center justify-between">
                  <span className="text-[#94a3b8] text-sm">{addon.serviceName}</span>
                  <span className="text-white text-sm font-mono">{fmt(addon.unitPrice)}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Resum dinàmic */}
        <div className="rounded-xl bg-[#0f172a] border border-[#1e293b] p-5 space-y-3">
          <div className="text-[#64748b] text-xs font-mono uppercase tracking-wider mb-1">
            Resum de la teva selecció ({selectedPhaseIds.size} fase{selectedPhaseIds.size !== 1 ? 's' : ''})
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="rounded-lg bg-[#0a0a0a] border border-[#1e293b] p-4 text-center">
              <div className="text-[#64748b] text-xs mb-1">Setup (únic)</div>
              <div className="text-white font-bold text-xl font-mono">
                {fmt(selSetupTotal + addonsSetup)}
              </div>
              {budget.discountTotal > 0 && (
                <div className="text-green-400 text-xs mt-1">-{fmt(budget.discountTotal)} descompte</div>
              )}
            </div>
            <div className="rounded-lg bg-[rgba(255,107,0,0.06)] border border-[rgba(255,107,0,0.2)] p-4 text-center">
              <div className="text-[#94a3b8] text-xs mb-1">Mensual recurrent</div>
              <div className="text-[#FF6B00] font-bold text-xl font-mono">
                {fmt(selMonthlyTotal)}<span className="text-sm text-[#94a3b8] font-normal">/mes</span>
              </div>
            </div>
          </div>
        </div>

        {/* Accions */}
        {!rejectMode ? (
          <div className="space-y-3">
            <button
              type="button"
              onClick={handleAccept}
              disabled={submitting || noneSelected}
              className={`w-full py-4 rounded-xl font-bold text-base transition ${
                noneSelected
                  ? 'bg-[#1e293b] text-[#64748b] cursor-not-allowed'
                  : 'bg-[#FF6B00] hover:bg-[#e55a00] text-white'
              } ${submitting ? 'opacity-60' : ''}`}
            >
              {submitting ? 'Processant...'
                : noneSelected ? 'Selecciona almenys una fase'
                : `Acceptar ${selectedPhaseIds.size} fase${selectedPhaseIds.size !== 1 ? 's' : ''}`}
            </button>
            <button
              type="button"
              onClick={() => setRejectMode(true)}
              disabled={submitting}
              className="w-full py-3 rounded-xl border border-[#1e293b] text-[#64748b] hover:text-white hover:border-[#334155] transition text-sm"
            >
              No m&apos;interessa cap fase
            </button>
          </div>
        ) : (
          <div className="space-y-3 rounded-xl bg-[#0f172a] border border-[#1e293b] p-5">
            <h3 className="font-semibold text-white">Motiu del rebuig (opcional)</h3>
            <textarea
              value={rejectReason}
              onChange={e => setRejectReason(e.target.value)}
              placeholder="Explica'ns per què no t'interessa o quines eren les teves expectatives..."
              rows={3}
              className="w-full bg-[#0a0a0a] border border-[#1e293b] rounded-lg px-4 py-3 text-sm text-white placeholder-[#475569] focus:outline-none focus:border-[#334155] resize-none"
            />
            <div className="flex gap-3">
              <button
                type="button"
                onClick={handleReject}
                disabled={submitting}
                className={`flex-1 py-3 rounded-xl bg-red-500/20 border border-red-500/40 text-red-400 font-semibold text-sm transition hover:bg-red-500/30 ${submitting ? 'opacity-60' : ''}`}
              >
                {submitting ? 'Processant...' : 'Confirmar rebuig'}
              </button>
              <button
                type="button"
                onClick={() => setRejectMode(false)}
                className="flex-1 py-3 rounded-xl border border-[#1e293b] text-[#64748b] hover:text-white transition text-sm"
              >
                Cancel·lar
              </button>
            </div>
          </div>
        )}

        {/* Footer */}
        <div className="text-center text-[#475569] text-xs space-y-1 pb-8">
          <p>AMG Digitalitzacions · hola@amgdl.com</p>
          <p>Pressupost {budget.budgetNumber} · Creat el {fmtDate(budget.createdAt)}</p>
        </div>
      </div>
    </div>
  );
}

export default function AcceptBudgetPage() {
  return (
    <Suspense fallback={
      <div className="min-h-dvh bg-[#0a0a0a] flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
      </div>
    }>
      <AcceptBudgetContent />
    </Suspense>
  );
}
