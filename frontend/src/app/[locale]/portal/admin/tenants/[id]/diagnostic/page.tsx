'use client';

import { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { getTenant, getTenantSetup, SECTOR_LABELS, SIZE_LABELS, SECTORS, SIZES, type TenantSetup } from '@/services/admin';
import { createBudget, listDiscounts, type DiscountResponse } from '@/services/billing';
import { calculatePrice, getSectorCategoryLabel, F_PHASE_MONTHLY, SIZE_FACTORS } from '@/services/pricing';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { I } from '@/components/ui/icons';
import { useToast } from '@/lib/toast-context';

// ── Dades diagnòstic per sector ───────────────────────────────────────────────

interface PainItem { id: string; label: string; phases: number[] }
interface SectorData {
  pains: PainItem[];
  priorityPhases: number[];
  recommendedPhases: number[];
  message: string;
}

const PHASE_LABELS: Record<number, string> = {
  1: 'F1 — Captació i agent IA',
  2: 'F2 — Agenda i cites',
  3: 'F3 — Pressupostos i facturació',
  4: 'F4 — Fidelització i seguiment',
  5: 'F5 — Equip i alertes',
};

const SECTOR_DATA: Record<string, SectorData> = {
  FISIOTERAPEUTA: {
    pains: [
      { id: 'missed_calls',   label: 'Trucades perdudes mentre treballa',  phases: [1] },
      { id: 'no_shows',       label: 'Cites oblidades (no-shows)',          phases: [1, 2] },
      { id: 'no_return',      label: 'Pacients que no tornen',              phases: [4] },
      { id: 'admin_overload', label: 'Poc temps per a tasques administratives', phases: [5] },
    ],
    priorityPhases: [1, 2],
    recommendedPhases: [4, 3],
    message: 'Reduïm les absències, millorem la gestió de cites i ajudem a que els pacients tornin amb més freqüència.',
  },
  PSICOLEG: {
    pains: [
      { id: 'after_hours',    label: 'Sol·licituds fora d\'horari',         phases: [1] },
      { id: 'agenda_manual',  label: 'Gestió d\'agenda manual i lenta',     phases: [2] },
      { id: 'abandonment',    label: 'Pacients que abandonen el tractament', phases: [4] },
    ],
    priorityPhases: [1, 2],
    recommendedPhases: [4, 5],
    message: 'Menys temps gestionant cites i més temps dedicat als pacients.',
  },
  ELECTRICISTA: {
    pains: [
      { id: 'calls_working',  label: 'Trucades mentre treballa en obra',    phases: [1] },
      { id: 'lost_quotes',    label: 'Pressupostos oblidats sense seguiment', phases: [3] },
      { id: 'lost_jobs',      label: 'Pèrdua de feina per manca de resposta', phases: [1, 3] },
    ],
    priorityPhases: [1, 3],
    recommendedPhases: [3, 5],
    message: 'Evita perdre feines per manca de resposta i millora el seguiment de pressupostos.',
  },
  PINTOR: {
    pains: [
      { id: 'whatsapp_chaos', label: 'Sol·licituds per WhatsApp sense gestionar', phases: [1] },
      { id: 'quote_time',     label: 'Molt temps preparant pressupostos',   phases: [3] },
      { id: 'silent_clients', label: 'Clients que no responen al pressupost', phases: [3, 4] },
    ],
    priorityPhases: [1, 3],
    recommendedPhases: [3],
    message: 'Converteix més pressupostos en feines reals mitjançant seguiments automàtics.',
  },
  FONTANER: {
    pains: [
      { id: 'urgencies',      label: 'Urgències sense atendre',             phases: [1] },
      { id: 'after_hours',    label: 'Trucades fora d\'horari',             phases: [1] },
      { id: 'disorganized',   label: 'Falta d\'organització interna',       phases: [5] },
    ],
    priorityPhases: [1, 5],
    recommendedPhases: [3],
    message: 'Gestiona millor les sol·licituds urgents i redueix el temps dedicat a trucades i coordinació.',
  },
  GESTORIA: {
    pains: [
      { id: 'doc_overload',   label: 'Massa documents i papers',            phases: [3] },
      { id: 'client_followup', label: 'Seguiment manual de clients',        phases: [4] },
      { id: 'fiscal_reminders', label: 'Recordatoris fiscals perduts',      phases: [4, 5] },
    ],
    priorityPhases: [5, 3],
    recommendedPhases: [4, 3],
    message: 'Redueix tasques administratives repetitives i millora la gestió documental.',
  },
  ACADEMIA: {
    pains: [
      { id: 'info_requests',  label: 'Sol·licituds d\'informació sense gestionar', phases: [1] },
      { id: 'manual_enroll',  label: 'Matrícules manuals i lentes',         phases: [1, 2] },
      { id: 'no_renewals',    label: 'Renovació de matrícules perdudes',    phases: [4] },
    ],
    priorityPhases: [1, 4],
    recommendedPhases: [3],
    message: 'Facilita la captació d\'alumnes i millora la renovació de matrícules.',
  },
  INMOBILIARIA: {
    pains: [
      { id: 'many_leads',     label: 'Molts leads, seguiment irregular',    phases: [1, 4] },
      { id: 'visits',         label: 'Coordinació de visites desorganitzada', phases: [2] },
      { id: 'disappear',      label: 'Clients que desapareixen',            phases: [3, 4] },
    ],
    priorityPhases: [1, 3],
    recommendedPhases: [4, 3],
    message: 'Automatitzem el seguiment de compradors i venedors per augmentar les oportunitats tancades.',
  },
  TALLER_MECANIC: {
    pains: [
      { id: 'missed_calls',   label: 'Trucades perdudes a taller',          phases: [1] },
      { id: 'appt_management', label: 'Gestió de cita prèvia manual',       phases: [2] },
      { id: 'no_return',      label: 'Clients que no tornen per revisió',   phases: [4] },
      { id: 'quote_chaos',    label: 'Pressupostos de reparació descontrolats', phases: [3] },
    ],
    priorityPhases: [1, 2],
    recommendedPhases: [3, 4],
    message: 'Automatitza l\'agenda, redueix trucades i recupera clients per a les pròximes revisions.',
  },
  PERRUQUERIA: {
    pains: [
      { id: 'missed_calls',   label: 'Trucades perdudes mentre atén clients', phases: [1] },
      { id: 'no_shows',       label: 'Cites que no apareixen',               phases: [1, 2] },
      { id: 'no_return',      label: 'Clients que no tornen',                phases: [4] },
    ],
    priorityPhases: [1, 2],
    recommendedPhases: [4],
    message: 'Gestiona les cites automàticament i fidelitza als clients amb recordatoris personalitzats.',
  },
  ESTETICA: {
    pains: [
      { id: 'missed_calls',   label: 'Trucades perdudes durant tractaments', phases: [1] },
      { id: 'no_shows',       label: 'Cites oblidades',                      phases: [1, 2] },
      { id: 'upsell',         label: 'Dificultat per oferir tractaments addicionals', phases: [4] },
    ],
    priorityPhases: [1, 2],
    recommendedPhases: [4, 3],
    message: 'Automatitza les cites i fidelitza als clients amb propostes personalitzades de tractaments.',
  },
};

const DEFAULT_SECTOR_DATA: SectorData = {
  pains: [
    { id: 'missed_calls',   label: 'Trucades perdudes',                   phases: [1] },
    { id: 'no_followup',    label: 'Manca de seguiment de clients',       phases: [3, 4] },
    { id: 'disorganized',   label: 'Poca organització interna',           phases: [5] },
    { id: 'manual_quotes',  label: 'Pressupostos manuals i lents',        phases: [3] },
    { id: 'no_return',      label: 'Clients que no tornen',               phases: [4] },
  ],
  priorityPhases: [1],
  recommendedPhases: [3, 4],
  message: 'Automatitzem les comunicacions i millorem la gestió per fer créixer el teu negoci.',
};

function getRecommendedPhases(sector: string, checkedPains: Set<string>): number[] {
  const data = SECTOR_DATA[sector] ?? DEFAULT_SECTOR_DATA;
  if (checkedPains.size === 0) {
    return Array.from(new Set(data.priorityPhases.concat(data.recommendedPhases)));
  }
  const phases = new Set<number>();
  data.pains.forEach(p => {
    if (checkedPains.has(p.id)) p.phases.forEach(ph => phases.add(ph));
  });
  // always include priority phases
  data.priorityPhases.forEach(p => phases.add(p));
  return Array.from(phases).sort();
}

// ── Components ────────────────────────────────────────────────────────────────

function StepIndicator({ current, total }: { current: number; total: number }) {
  return (
    <div className="flex items-center gap-1">
      {Array.from({ length: total }).map((_, i) => (
        <div key={i} className={`h-1 rounded-full transition-all ${i < current ? 'bg-[#FF6B00]' : i === current ? 'bg-[#FF6B00] w-6' : 'bg-border-medium'} ${i === current ? 'w-6' : 'w-3'}`} />
      ))}
    </div>
  );
}

// ── Main ─────────────────────────────────────────────────────────────────────

export default function DiagnosticPage() {
  const params = useParams<{ locale: string; id: string }>();
  const router = useRouter();
  const { toast } = useToast();
  const tenantId = params.id;
  const locale = params.locale;

  const [step, setStep] = useState(0); // 0=diagnòstic 1=recomanació 2=pressupost
  const [sector, setSector] = useState('');
  const [size, setSize] = useState('');
  const [checkedPains, setCheckedPains] = useState<Set<string>>(new Set());
  const [selectedPhaseNums, setSelectedPhaseNums] = useState<Set<number>>(new Set());
  const [selectedProfileId, setSelectedProfileId] = useState('');
  const [selectedPhaseIds, setSelectedPhaseIds] = useState<Set<string>>(new Set());
  const [discountCode, setDiscountCode] = useState('');
  const [selectedDiscountId, setSelectedDiscountId] = useState('');
  const [notes, setNotes] = useState('');
  const [clientNotes, setClientNotes] = useState('');
  const [validDays, setValidDays] = useState('30');
  const [creating, setCreating] = useState(false);

  const { data: tenant } = useQuery({
    queryKey: ['tenant', tenantId],
    queryFn: () => getTenant(tenantId),
  });

  useEffect(() => {
    if (tenant?.sector && !sector) setSector(tenant.sector);
    if (tenant?.businessSize && !size) setSize(tenant.businessSize);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tenant]);

  const { data: setup } = useQuery({
    queryKey: ['tenant-setup', tenantId],
    queryFn: () => getTenantSetup(tenantId),
  });

  const { data: discounts = [] } = useQuery({
    queryKey: ['discounts'],
    queryFn: () => listDiscounts(undefined, true),
  });

  const sectorData = SECTOR_DATA[sector] ?? DEFAULT_SECTOR_DATA;
  const profiles = setup?.profiles ?? [];
  const selectedProfile = profiles.find(p => p.profile.id === selectedProfileId);

  // ── Step 0 → 1: compute recommendation ───────────────────────────────────
  const handleDiagnosticNext = () => {
    const recommended = getRecommendedPhases(sector, checkedPains);
    setSelectedPhaseNums(new Set(recommended));
    setStep(1);
  };

  // ── Step 1 → 2: map phase numbers to profile phase IDs ───────────────────
  const handleRecommendationNext = () => {
    if (profiles.length === 1 && !selectedProfileId) {
      const profileId = profiles[0].profile.id;
      setSelectedProfileId(profileId);
      const matching = profiles[0].phases
        .filter(ph => selectedPhaseNums.has(ph.phase.sortOrder))
        .map(ph => ph.phase.id);
      setSelectedPhaseIds(new Set(matching));
    }
    setStep(2);
  };

  // ── Step 2: when profile changes, auto-select matching phases ─────────────
  const handleProfileSelect = (profileId: string) => {
    setSelectedProfileId(profileId);
    const profile = profiles.find(p => p.profile.id === profileId);
    if (!profile) return;
    const matching = profile.phases
      .filter(ph => selectedPhaseNums.has(ph.phase.sortOrder))
      .map(ph => ph.phase.id);
    setSelectedPhaseIds(new Set(matching));
  };

  const togglePhaseId = (id: string) => {
    setSelectedPhaseIds(prev => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  // ── Create budget ─────────────────────────────────────────────────────────
  const handleCreate = async () => {
    if (!selectedProfileId) { toast('error', 'Selecciona un perfil'); return; }
    if (selectedPhaseIds.size === 0) { toast('error', 'Selecciona almenys una fase'); return; }
    setCreating(true);
    try {
      const validUntil = new Date();
      validUntil.setDate(validUntil.getDate() + parseInt(validDays || '30'));
      const pricingNote = (sector && size && selectedPhaseNums.size > 0)
        ? (() => {
            const p = calculatePrice(Array.from(selectedPhaseNums).sort(), sector, size);
            return `Estimació NexeLocal: ${p.monthlyTotal}€/mes (setup ${p.setupTotal}€) · ${getSectorCategoryLabel(sector)} ×${p.sectorFactor} · ${size} ×${p.sizeFactor}`;
          })()
        : '';
      await createBudget(tenantId, {
        profileId: selectedProfileId,
        phaseIds: Array.from(selectedPhaseIds),
        sector: sector || undefined,
        businessSize: size || undefined,
        recommendation: sectorData.message,
        recommendedPhaseIds: Array.from(selectedPhaseIds),
        phaseNumbers: Array.from(selectedPhaseNums),
        notes: [notes, pricingNote].filter(Boolean).join('\n') || undefined,
        clientNotes: clientNotes || undefined,
        discountIds: selectedDiscountId ? [selectedDiscountId] : undefined,
        validUntil: validUntil.toISOString().slice(0, 10),
      });
      toast('success', 'Pressupost creat correctament');
      router.push(`/${locale}/portal/billing`);
    } catch (e: unknown) {
      toast('error', `Error: ${e instanceof Error ? e.message : ''}`);
    } finally { setCreating(false); }
  };

  // ── Render ────────────────────────────────────────────────────────────────

  const inputCls = 'w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm text-ink-1 focus:outline-none focus:border-[#FF6B00] transition';
  const labelCls = 'f-mono text-[10px] uppercase tracking-wider text-ink-3 block mb-1.5';

  return (
    <PortalShell breadcrumb={`tenants / ${tenant?.name ?? tenantId} / diagnòstic`} backHref={`/${locale}/portal/admin/tenants/${tenantId}`}>
      <div className="p-4 sm:p-8 max-w-2xl space-y-6">

        {/* Header */}
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-lg font-semibold text-ink-0">Diagnòstic comercial</h1>
            <p className="text-sm text-ink-2 mt-0.5">{tenant?.name}</p>
          </div>
          <StepIndicator current={step} total={3} />
        </div>

        {/* ── STEP 0: Diagnòstic ─────────────────────────────────────────────── */}
        {step === 0 && (
          <div className="space-y-5">
            <div className="bg-surface-overlay border border-border-base rounded-md p-5 space-y-4">

              {/* Sector */}
              <div>
                <label className={labelCls}>Sector del negoci</label>
                <select
                  value={sector}
                  onChange={e => { setSector(e.target.value); setCheckedPains(new Set()); }}
                  className={inputCls}
                >
                  <option value="">— Selecciona un sector —</option>
                  {SECTORS.map(s => (
                    <option key={s} value={s}>{SECTOR_LABELS[s] ?? s}</option>
                  ))}
                </select>
              </div>

              {/* Grandària */}
              <div>
                <label className={labelCls}>Grandària del negoci</label>
                <div className="grid grid-cols-2 gap-2">
                  {SIZES.map(s => (
                    <button key={s} type="button"
                      onClick={() => setSize(s)}
                      className={`p-3 rounded border text-left transition ${
                        size === s
                          ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.08)] text-accent-light'
                          : 'border-border-base text-ink-2 hover:border-ink-2'
                      }`}
                    >
                      <div className="text-xs font-semibold">{SIZE_LABELS[s]}</div>
                    </button>
                  ))}
                </div>
              </div>

              {/* Dolors */}
              {sector && (
                <div>
                  <label className={labelCls}>Dolors detectats (marca els que li preocupen)</label>
                  <div className="space-y-2">
                    {sectorData.pains.map(pain => (
                      <label key={pain.id} className={`flex items-start gap-3 p-3 rounded border cursor-pointer transition ${
                        checkedPains.has(pain.id) ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.06)]' : 'border-border-base hover:border-ink-3'
                      }`}>
                        <input
                          type="checkbox"
                          checked={checkedPains.has(pain.id)}
                          onChange={() => setCheckedPains(prev => {
                            const next = new Set(prev);
                            next.has(pain.id) ? next.delete(pain.id) : next.add(pain.id);
                            return next;
                          })}
                          className="mt-0.5 accent-[#FF6B00]"
                        />
                        <div className="flex-1">
                          <span className="text-sm text-ink-0">{pain.label}</span>
                          <div className="flex gap-1 mt-1">
                            {pain.phases.map(ph => (
                              <span key={ph} className="f-mono text-[9px] px-1.5 py-0.5 rounded bg-[rgba(255,107,0,0.1)] text-accent-light">
                                F{ph}
                              </span>
                            ))}
                          </div>
                        </div>
                      </label>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <AMGButton
              onClick={handleDiagnosticNext}
              disabled={!sector || !size}
              icon={I.ArrowRight}
            >
              Veure recomanació
            </AMGButton>
          </div>
        )}

        {/* ── STEP 1: Recomanació ────────────────────────────────────────────── */}
        {step === 1 && (
          <div className="space-y-5">
            {/* Missatge */}
            <div className="bg-[rgba(255,107,0,0.06)] border border-[rgba(255,107,0,0.2)] rounded-md p-4">
              <div className="f-mono text-[10px] uppercase tracking-widest text-accent-light mb-2">
                ✦ Proposta per a {SECTOR_LABELS[sector] ?? sector} · {SIZE_LABELS[size] ?? size}
              </div>
              <p className="text-sm text-ink-1 leading-relaxed">{sectorData.message}</p>
            </div>

            {/* Dolors seleccionats */}
            {checkedPains.size > 0 && (
              <div className="bg-surface-overlay border border-border-base rounded-md p-4">
                <div className="f-mono text-[10px] uppercase tracking-widest text-ink-3 mb-3">Dolors identificats</div>
                <div className="space-y-1">
                  {sectorData.pains.filter(p => checkedPains.has(p.id)).map(p => (
                    <div key={p.id} className="flex items-center gap-2 text-sm text-ink-1">
                      <I.Check size={12} className="text-[#FF6B00] shrink-0" />
                      {p.label}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Fases recomanades */}
            <div className="bg-surface-overlay border border-border-base rounded-md p-4">
              <div className="f-mono text-[10px] uppercase tracking-widest text-ink-3 mb-3">Fases recomanades</div>
              <div className="space-y-2">
                {[1, 2, 3, 4, 5].map(num => {
                  const isRecommended = selectedPhaseNums.has(num);
                  const isPriority = sectorData.priorityPhases.includes(num);
                  return (
                    <label key={num} className={`flex items-center gap-3 p-3 rounded border cursor-pointer transition ${
                      isRecommended ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.08)]' : 'border-border-base opacity-50 hover:opacity-80'
                    }`}>
                      <input
                        type="checkbox"
                        checked={isRecommended}
                        onChange={() => setSelectedPhaseNums(prev => {
                          const next = new Set(prev);
                          next.has(num) ? next.delete(num) : next.add(num);
                          return next;
                        })}
                        className="accent-[#FF6B00]"
                      />
                      <div className="flex-1">
                        <span className={`text-sm font-medium ${isRecommended ? 'text-ink-0' : 'text-ink-2'}`}>
                          {PHASE_LABELS[num]}
                        </span>
                      </div>
                      {isPriority && isRecommended && (
                        <span className="f-mono text-[9px] px-2 py-0.5 rounded-full bg-[#FF6B00] text-black font-bold">
                          Prioritat
                        </span>
                      )}
                    </label>
                  );
                })}
              </div>
            </div>

            {/* Estimació de preus */}
            {selectedPhaseNums.size > 0 && sector && size && (() => {
              const pricing = calculatePrice(Array.from(selectedPhaseNums).sort(), sector, size);
              return (
                <div className="bg-[rgba(255,107,0,0.04)] border border-[rgba(255,107,0,0.15)] rounded-md p-4">
                  <div className="f-mono text-[10px] uppercase tracking-widest text-accent-light mb-3">
                    Estimació de preus · {getSectorCategoryLabel(sector)} · ×{pricing.sectorFactor} · {SIZE_FACTORS[size] ? `×${SIZE_FACTORS[size]}` : ''}
                  </div>
                  <div className="space-y-1 mb-3">
                    {Array.from(selectedPhaseNums).sort().map(num => (
                      <div key={num} className="flex justify-between text-xs text-ink-2">
                        <span>F{num} — {['Captació + Agent IA', 'Agenda i cites', 'Pressupostos + Cobraments', 'Fidelització', 'Equip + Documentació'][num - 1]}</span>
                        <span className="f-mono">{F_PHASE_MONTHLY[num]}€</span>
                      </div>
                    ))}
                    <div className="flex justify-between text-xs text-ink-3 pt-1 border-t border-border-base">
                      <span>Base ({pricing.baseTotal}€) × {pricing.sectorFactor} sector × {pricing.sizeFactor} mida</span>
                      <span className="f-mono">= {pricing.monthlyTotal}€/mes</span>
                    </div>
                  </div>
                  <div className="flex items-end justify-between">
                    <div>
                      <div className="f-mono text-[10px] text-ink-3 uppercase tracking-wider">Setup estimat</div>
                      <div className="text-xl font-bold text-ink-0 f-mono">{pricing.setupTotal}€</div>
                    </div>
                    <div className="text-right">
                      <div className="f-mono text-[10px] text-ink-3 uppercase tracking-wider">Mensual</div>
                      <div className="text-2xl font-bold text-accent-light f-mono">{pricing.monthlyTotal}€<span className="text-sm font-normal text-ink-3">/mes</span></div>
                    </div>
                  </div>
                </div>
              );
            })()}

            <div className="flex gap-3">
              <AMGButton variant="outline" onClick={() => setStep(0)} icon={I.ArrowRight} className="[&_svg]:rotate-180">
                Enrere
              </AMGButton>
              <AMGButton
                onClick={handleRecommendationNext}
                disabled={selectedPhaseNums.size === 0}
                icon={I.ArrowRight}
              >
                Crear pressupost
              </AMGButton>
            </div>
          </div>
        )}

        {/* ── STEP 2: Pressupost ─────────────────────────────────────────────── */}
        {step === 2 && (
          <div className="space-y-5">

            {/* Perfil */}
            {profiles.length === 0 ? (
              <div className="bg-surface-overlay border border-border-base rounded-md p-5 text-sm text-ink-2">
                Aquest tenant no té cap perfil de facturació assignat. Ves a la pàgina del tenant i assigna un perfil primer.
              </div>
            ) : (
              <>
                {profiles.length > 1 && (
                  <div className="bg-surface-overlay border border-border-base rounded-md p-4">
                    <label className={labelCls}>Perfil de facturació</label>
                    <div className="space-y-2">
                      {profiles.map(p => (
                        <button key={p.profile.id} type="button"
                          onClick={() => handleProfileSelect(p.profile.id)}
                          className={`w-full text-left p-3 rounded border transition text-sm ${
                            selectedProfileId === p.profile.id ? 'border-[#FF6B00] bg-accent-muted' : 'border-border-base hover:border-ink-2'
                          }`}
                        >
                          <span className="font-semibold">{p.profile.name}</span>
                          <span className="text-ink-3 ml-2 text-xs">{p.phases.length} fases</span>
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                {/* Fases del perfil seleccionat */}
                {selectedProfile && (
                  <div className="bg-surface-overlay border border-border-base rounded-md overflow-hidden">
                    <div className="px-4 py-3 border-b border-border-base">
                      <div className="f-mono text-[10px] uppercase tracking-widest text-ink-3">Fases — {selectedProfile.profile.name}</div>
                    </div>
                    <div className="divide-y divide-border-base">
                      {selectedProfile.phases.map(ph => {
                        const isSelected = selectedPhaseIds.has(ph.phase.id);
                        const isRecommended = selectedPhaseNums.has(ph.phase.sortOrder);
                        return (
                          <label key={ph.phase.id} className={`flex items-start gap-3 px-4 py-3 cursor-pointer transition ${
                            isSelected ? 'bg-[rgba(255,107,0,0.05)]' : 'opacity-60 hover:opacity-90'
                          }`}>
                            <input
                              type="checkbox"
                              checked={isSelected}
                              onChange={() => togglePhaseId(ph.phase.id)}
                              className="mt-1 accent-[#FF6B00]"
                            />
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center gap-2 flex-wrap">
                                <span className="text-sm font-medium text-ink-0">{ph.phase.name}</span>
                                {isRecommended && (
                                  <span className="f-mono text-[9px] px-1.5 py-0.5 rounded bg-[rgba(255,107,0,0.15)] text-accent-light">
                                    Recomanada
                                  </span>
                                )}
                              </div>
                              <div className="text-xs text-ink-3 mt-0.5">{ph.services.length} serveis</div>
                            </div>
                          </label>
                        );
                      })}
                    </div>
                    {sector && size && selectedPhaseNums.size > 0 && (() => {
                      const pricing = calculatePrice(Array.from(selectedPhaseNums).sort(), sector, size);
                      return (
                        <div className="px-4 py-3 border-t border-border-base bg-[rgba(255,107,0,0.03)]">
                          <div className="flex items-center justify-between">
                            <span className="f-mono text-[10px] text-ink-3 uppercase tracking-wider">
                              {selectedPhaseIds.size} fase{selectedPhaseIds.size !== 1 ? 's' : ''} · estimació fórmula
                            </span>
                            <div className="flex items-center gap-4">
                              <div className="text-right">
                                <div className="f-mono text-[9px] text-ink-3 uppercase">Setup</div>
                                <div className="f-mono text-sm font-semibold text-ink-1">{pricing.setupTotal}€</div>
                              </div>
                              <div className="text-right">
                                <div className="f-mono text-[9px] text-ink-3 uppercase">Mensual</div>
                                <div className="f-mono text-sm font-semibold text-accent-light">{pricing.monthlyTotal}€/mes</div>
                              </div>
                            </div>
                          </div>
                        </div>
                      );
                    })()}
                  </div>
                )}

                {/* Descompte */}
                {(discounts as DiscountResponse[]).length > 0 && (
                  <div className="bg-surface-overlay border border-border-base rounded-md p-4">
                    <label className={labelCls}>Descompte (opcional)</label>
                    <select
                      value={selectedDiscountId}
                      onChange={e => setSelectedDiscountId(e.target.value)}
                      className={inputCls}
                    >
                      <option value="">— Sense descompte —</option>
                      {(discounts as DiscountResponse[]).map(d => (
                        <option key={d.id} value={d.id}>
                          {d.code} — {d.type === 'PERCENTAGE' ? `${d.value}%` : `${d.value} €`} {d.description ? `· ${d.description}` : ''}
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                {/* Detalls finals */}
                <div className="bg-surface-overlay border border-border-base rounded-md p-4 space-y-3">
                  <div>
                    <label className={labelCls}>Nota per al client (apareix al pressupost)</label>
                    <textarea
                      value={clientNotes}
                      onChange={e => setClientNotes(e.target.value)}
                      rows={2}
                      placeholder={sectorData.message}
                      className={`${inputCls} resize-none`}
                    />
                  </div>
                  <div>
                    <label className={labelCls}>Nota interna</label>
                    <textarea value={notes} onChange={e => setNotes(e.target.value)} rows={2} className={`${inputCls} resize-none`} />
                  </div>
                  <div>
                    <label className={labelCls}>Vàlid durant (dies)</label>
                    <input type="number" value={validDays} onChange={e => setValidDays(e.target.value)} min="1" max="365" className={inputCls} />
                  </div>
                </div>

                <div className="flex gap-3">
                  <AMGButton variant="outline" onClick={() => setStep(1)} icon={I.ArrowRight} className="[&_svg]:rotate-180">
                    Enrere
                  </AMGButton>
                  <AMGButton
                    onClick={handleCreate}
                    disabled={creating || !selectedProfileId || selectedPhaseIds.size === 0}
                    loading={creating}
                    icon={I.Receipt}
                    className="flex-1 justify-center"
                  >
                    Crear pressupost
                  </AMGButton>
                </div>
              </>
            )}
          </div>
        )}
      </div>
    </PortalShell>
  );
}
