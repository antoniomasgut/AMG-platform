'use client';

import { useParams, useRouter } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getTenant, getTenantPhaseHealth, getTenantPhaseHistory, goLiveTenant, suspendTenant, getTenantReadiness, PhaseActivationRecord, PhaseHealthItem, TenantReadiness } from '@/services/admin';
import { getNexeConfigs, getAgendaMode, getQuoteMode } from '@/services/nexe-configs';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { IconSet } from '@/components/ui/icons';

// ── Helpers ────────────────────────────────────────────────────────────────────

const PHASE_CONFIG_KEY: Record<string, string> = {
  F2: 'AGENDA',
  F3: 'PRESSUPOSTOS',
  F4: 'FIDELITZACIO',
  F5: 'EQUIP',
};

function isPhaseConfigured(configs: Record<string, string>, phase: string): boolean {
  const key = PHASE_CONFIG_KEY[phase];
  if (!key) return false; // F1 no té config key — detecció manual
  const val = configs[key];
  if (!val || val === '{}' || val === 'null') return false;
  try {
    const obj = JSON.parse(val);
    return typeof obj === 'object' && Object.keys(obj).length > 0;
  } catch {
    return false;
  }
}

function isEquipFullyConfigured(configs: Record<string, string>): boolean {
  const val = configs['EQUIP'];
  if (!val) return false;
  try {
    const obj = JSON.parse(val);
    return !!(obj?.telegram_group_id);
  } catch {
    return false;
  }
}

// Retorna títol + descripció sector-específics per cada fase
function getPhaseInfo(phase: string, sector?: string | null) {
  const agendaMode = getAgendaMode(sector);
  const quoteMode  = getQuoteMode(sector);

  switch (phase) {
    case 'F1':
      return {
        label: 'F1',
        title: 'Captació',
        desc: 'Captura leads des de la web, WhatsApp i email. L\'agent classifica i crea fitxes automàticament.',
        icon: IconSet.Globe,
        href: (tenantId: string, locale: string) => `/${locale}/portal/landings`,
        ctaLabel: 'Anar a Landings',
      };
    case 'F2':
      return {
        label: 'F2',
        title: agendaMode === 'inspection' ? 'Agenda — Visites'
             : agendaMode === 'vehicle'    ? 'Agenda — Vehicles'
             : agendaMode === 'meeting'    ? 'Agenda — Reunions'
             : 'Agenda',
        desc: agendaMode === 'inspection' ? 'Confirma visites, envia recordatoris i gestiona absències automàticament.'
            : agendaMode === 'vehicle'    ? 'Recepciona vehicles, estima recollida i elimina trucades innecessàries.'
            : agendaMode === 'meeting'    ? 'Confirma reunions, envia recordatoris i redueix no-shows.'
            : 'Confirma cites, envia recordatoris automàtics i redueix cites perdudes.',
        icon: IconSet.Calendar,
        href: (tenantId: string, locale: string) => `/${locale}/portal/admin/tenants/${tenantId}/nexe/agenda`,
        ctaLabel: 'Configurar Agenda',
      };
    case 'F3':
      return {
        label: 'F3',
        title: quoteMode === 'pricelist' ? 'Pressupostos — Catàleg' : 'Pressupostos',
        desc: quoteMode === 'pricelist'
          ? 'Publica el catàleg de serveis i preus que l\'agent mostrarà als clients.'
          : 'L\'agent genera pressupostos i fa seguiment dels que queden sense resposta.',
        icon: IconSet.Receipt,
        href: (tenantId: string, locale: string) => `/${locale}/portal/admin/tenants/${tenantId}/nexe/pressupostos`,
        ctaLabel: quoteMode === 'pricelist' ? 'Configurar Catàleg' : 'Configurar Pressupostos',
      };
    case 'F4':
      return {
        label: 'F4',
        title: 'Seguiment',
        desc: 'Cap client oblidat: recordatoris automàtics, sol·licitud de ressenyes i reactivació de clients inactius.',
        icon: IconSet.Heart,
        href: (tenantId: string, locale: string) => `/${locale}/portal/admin/tenants/${tenantId}/nexe/fidelitzacio`,
        ctaLabel: 'Configurar Seguiment',
      };
    case 'F5':
      return {
        label: 'F5',
        title: 'Alertes & Equip',
        desc: 'Alertes proactives al grup de Telegram: leads sense resposta, cites pendents i informe diari.',
        icon: IconSet.Users,
        href: (tenantId: string, locale: string) => `/${locale}/portal/admin/tenants/${tenantId}/nexe/equip`,
        ctaLabel: 'Configurar Alertes',
      };
    default:
      return null;
  }
}

// ── Status chip ────────────────────────────────────────────────────────────────

function StatusChip({ done }: { done: boolean }) {
  if (done) {
    return (
      <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 f-mono text-label text-xs bg-success/10 text-success border border-success/20">
        <IconSet.Check size={10} />
        Configurat
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 f-mono text-label text-xs bg-warning/10 text-warning border border-warning/20">
      <IconSet.Clock size={10} />
      Pendent
    </span>
  );
}

// ── Phase card ─────────────────────────────────────────────────────────────────

function PhaseCard({
  phase,
  sector,
  configured,
  tenantId,
  locale,
}: {
  phase: string;
  sector?: string | null;
  configured: boolean;
  tenantId: string;
  locale: string;
}) {
  const router = useRouter();
  const info = getPhaseInfo(phase, sector);
  if (!info) return null;
  const Icon = info.icon;

  return (
    <div className={`amg-card card-clip overflow-hidden transition-all ${configured ? 'opacity-90' : ''}`}>
      <div className={`h-0.5 ${configured ? 'bg-success' : 'bg-warning'}`} />
      <div className="p-5">
        <div className="flex items-start gap-4">
          {/* Phase badge */}
          <div className={`w-10 h-10 shrink-0 flex items-center justify-center f-mono font-bold text-sm border-2 transition-colors ${
            configured ? 'border-success text-success bg-success/10' : 'border-accent text-accent bg-accent/10'
          }`}>
            {info.label}
          </div>

          {/* Content */}
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1 flex-wrap">
              <span className="f-display font-bold text-sm">{info.title}</span>
              <StatusChip done={configured} />
            </div>
            <p className="f-mono text-label text-xs text-ink-2 mb-4">{info.desc}</p>

            <div className="flex items-center gap-2">
              <Icon size={13} className="text-ink-3" />
              <AMGButton
                size="sm"
                variant={configured ? 'secondary' : 'primary'}
                onClick={() => router.push(info.href(tenantId, locale))}
              >
                {configured ? 'Revisar →' : info.ctaLabel + ' →'}
              </AMGButton>
            </div>
          </div>

          {/* Done indicator */}
          {configured && (
            <div className="shrink-0 w-6 h-6 bg-success/20 flex items-center justify-center">
              <IconSet.Check size={12} stroke="#39d353" />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// ── Phase health section ───────────────────────────────────────────────────────

function PhaseHealthSection({ results }: { results: PhaseHealthItem[] }) {
  const withProblems = results.filter(r => !r.healthy || r.warnings.length > 0);
  if (withProblems.length === 0) {
    return (
      <div className="amg-card card-clip p-4 flex items-center gap-3">
        <IconSet.Check size={14} stroke="#39d353" />
        <span className="f-mono text-label text-xs text-success">Totes les fases passen el diagnòstic tècnic</span>
      </div>
    );
  }
  return (
    <div className="space-y-2">
      {results.map(item => (
        <div key={item.phase} className="amg-card card-clip p-4">
          <div className="flex items-center gap-2 mb-2">
            <span className={`f-mono font-bold text-xs ${item.healthy ? 'text-success' : 'text-error'}`}>{item.phase}</span>
            {item.healthy && item.warnings.length === 0
              ? <span className="text-success text-xs">✓ Correcte</span>
              : !item.healthy
              ? <span className="text-error text-xs">Té problemes</span>
              : <span className="text-warning text-xs">Avisos</span>
            }
          </div>
          {item.issues.map((msg, i) => (
            <div key={i} className="flex items-start gap-2 mt-1">
              <IconSet.AlertTriangle size={11} className="text-error mt-0.5 shrink-0" />
              <span className="f-mono text-label text-xs text-error">{msg}</span>
            </div>
          ))}
          {item.warnings.map((msg, i) => (
            <div key={i} className="flex items-start gap-2 mt-1">
              <IconSet.Clock size={11} className="text-warning mt-0.5 shrink-0" />
              <span className="f-mono text-label text-xs text-warning">{msg}</span>
            </div>
          ))}
        </div>
      ))}
    </div>
  );
}

// ── Phase history section ──────────────────────────────────────────────────────

function PhaseHistorySection({ records }: { records: PhaseActivationRecord[] }) {
  if (records.length === 0) {
    return (
      <div className="f-mono text-label text-xs text-ink-3 py-2">Sense registres d'activació</div>
    );
  }
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-xs f-mono text-label">
        <thead>
          <tr className="border-b border-border-base text-ink-3 text-left">
            <th className="pb-1.5 pr-4 font-normal">Fase</th>
            <th className="pb-1.5 pr-4 font-normal">Origen</th>
            <th className="pb-1.5 pr-4 font-normal">Data activació</th>
            <th className="pb-1.5 font-normal">Desactivada</th>
          </tr>
        </thead>
        <tbody>
          {records.map(r => (
            <tr key={r.id} className="border-b border-border-base/40 text-ink-1">
              <td className="py-1.5 pr-4 font-bold text-accent">{r.phase}</td>
              <td className="py-1.5 pr-4 text-ink-2">{r.source}</td>
              <td className="py-1.5 pr-4">{new Date(r.activatedAt).toLocaleString('ca')}</td>
              <td className="py-1.5 text-ink-3">
                {r.deactivatedAt ? new Date(r.deactivatedAt).toLocaleString('ca') : '—'}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// ── Bot step ───────────────────────────────────────────────────────────────────

function BotCard({ tenantId, locale }: { tenantId: string; locale: string }) {
  const router = useRouter();
  return (
    <div className="amg-card card-clip overflow-hidden">
      <div className="h-0.5 bg-accent" />
      <div className="p-5">
        <div className="flex items-start gap-4">
          <div className="w-10 h-10 shrink-0 flex items-center justify-center f-mono font-bold text-sm border-2 border-accent text-accent bg-accent/10">
            IA
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <span className="f-display font-bold text-sm">Bot IA & Canals</span>
              <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 f-mono text-label text-xs bg-accent/10 text-accent-light border border-accent/20">
                Sempre necessari
              </span>
            </div>
            <p className="f-mono text-label text-xs text-ink-2 mb-4">
              Configura el canal principal del bot (WhatsApp, Telegram), el mode de resposta (AUTO/HYBRID/MANUAL) i la base de coneixement.
            </p>
            <div className="flex items-center gap-2">
              <IconSet.Bot size={13} className="text-ink-3" />
              <AMGButton size="sm" variant="primary" onClick={() => router.push(`/${locale}/portal/admin/tenants/${tenantId}#section-agent-config`)}>
                Configurar Agent →
              </AMGButton>
              <AMGButton size="sm" variant="secondary" onClick={() => router.push(`/${locale}/portal/agents`)}>
                Coneixement KB →
              </AMGButton>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── Page ───────────────────────────────────────────────────────────────────────

export default function ActivateWizardPage() {
  const params  = useParams();
  const router  = useRouter();
  const tenantId = params.id as string;
  const locale  = (params.locale as string) ?? 'ca';

  const { data: tenant, isLoading: loadingTenant } = useQuery({
    queryKey: ['tenant', tenantId],
    queryFn: () => getTenant(tenantId),
    enabled: !!tenantId,
  });

  const { data: configs = {}, isLoading: loadingConfigs } = useQuery({
    queryKey: ['nexe-configs', tenantId],
    queryFn: () => getNexeConfigs(tenantId),
    enabled: !!tenantId,
  });

  const { data: phaseHealth } = useQuery({
    queryKey: ['phase-health', tenantId],
    queryFn: () => getTenantPhaseHealth(tenantId),
    enabled: !!tenantId,
  });

  const { data: phaseHistory = [] } = useQuery({
    queryKey: ['phase-history', tenantId],
    queryFn: () => getTenantPhaseHistory(tenantId),
    enabled: !!tenantId,
  });

  const queryClient = useQueryClient();
  const { data: readiness } = useQuery({
    queryKey: ['tenant-readiness', tenantId],
    queryFn: () => getTenantReadiness(tenantId),
    enabled: !!tenantId,
    refetchInterval: 30_000,
  });

  const goLiveMutation = useMutation({
    mutationFn: () => goLiveTenant(tenantId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenant', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['tenant-readiness', tenantId] });
    },
  });
  const suspendMutation = useMutation({
    mutationFn: () => suspendTenant(tenantId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tenant', tenantId] }),
  });

  const loading = loadingTenant || loadingConfigs;

  const phases: string[] = tenant?.contractedPhases ?? [];
  const sector = tenant?.sector;

  const configuredCount = phases.filter(p =>
    p === 'F1' ? false /* F1 no auto-detecta */ : isPhaseConfigured(configs, p)
  ).length + (isEquipFullyConfigured(configs) ? 0 : 0); // just for F5 special check

  // Recompute with F5 special logic
  const configuredPhases = phases.filter(p => {
    if (p === 'F1') return false;
    if (p === 'F5') return isEquipFullyConfigured(configs);
    return isPhaseConfigured(configs, p);
  });

  const totalConfigured  = configuredPhases.length;
  const totalPhases      = phases.length;
  const pct = totalPhases > 0 ? Math.round((totalConfigured / totalPhases) * 100) : 0;

  const sectorLabel: Record<string, string> = {
    PINTOR: 'Pintor', ELECTRICISTA: 'Electricista', FONTANER: 'Fontaner',
    JARDINER: 'Jardiner', NETEJA: 'Neteja', TALLER_MECANIC: 'Taller Mecànic',
    GESTORIA: 'Gestoria', FISIOTERAPEUTA: 'Fisioterapeuta', PSICOLEG: 'Psicòleg',
    NUTRICIONISTA: 'Nutricionista', PERRUQUERIA: 'Perruqueria', ESTETICA: 'Estètica',
    PERRUQUERIA_CANINA: 'Perruqueria Canina', VETERINARI: 'Veterinari',
    RESTAURANT: 'Restaurant', PASTISSERIA: 'Pastisseria', FARMACIA: 'Farmàcia',
    OPTICA: 'Òptica', DENTISTA: 'Dentista', CLINICA: 'Clínica',
  };

  return (
    <PortalShell breadcrumb="activació" backHref={`/${locale}/portal/admin/tenants/${tenantId}`}>
      <div className="p-4 sm:p-8 max-w-3xl space-y-6">

        {/* Header */}
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest text-xs">
            / portal / admin / tenants / activació /
          </span>
          <div className="f-display font-bold text-xl mt-1">
            Posar en marxa: {tenant?.name ?? '…'}
          </div>
          {tenant && (
            <p className="f-mono text-label text-xs text-ink-2 mt-1">
              {sectorLabel[sector ?? ''] ?? sector ?? 'Sector no definit'}
              {tenant.businessSize && ` · ${tenant.businessSize}`}
              {' · '}
              {phases.length} fase{phases.length !== 1 ? 's' : ''} contractada{phases.length !== 1 ? 'es' : ''}
            </p>
          )}
        </div>

        {/* Progress */}
        {!loading && phases.length > 0 && (
          <div className="amg-card card-clip p-5">
            <div className="flex items-center justify-between mb-2">
              <span className="f-mono text-label text-xs text-ink-2">Progrés de configuració</span>
              <span className="f-mono text-label text-xs font-semibold text-ink-0">
                {totalConfigured} / {totalPhases} fases configurades
              </span>
            </div>
            <div className="h-1.5 bg-surface-2 overflow-hidden">
              <div
                className="h-full bg-gradient-to-r from-accent to-accent-light transition-all duration-700"
                style={{ width: `${pct}%` }}
              />
            </div>
            {pct === 100 && (
              <div className="flex items-center gap-2 mt-3">
                <IconSet.Check size={13} stroke="#39d353" />
                <span className="f-mono text-label text-xs text-success">
                  Totes les fases configurades. Recorda activar el Bot IA!
                </span>
              </div>
            )}
          </div>
        )}

        {/* No phases */}
        {!loading && phases.length === 0 && (
          <div className="amg-card card-clip p-6 text-center">
            <IconSet.AlertTriangle size={24} className="mx-auto mb-3 text-ink-3" />
            <div className="f-display font-bold text-sm mb-1">Sense fases contractades</div>
            <p className="f-mono text-label text-xs text-ink-2 mb-4">
              Aquest tenant no té fases NexeLocal actives. Accepta un pressupost primer.
            </p>
            <AMGButton size="sm" variant="primary" onClick={() => router.push(`/${locale}/portal/billing`)}>
              Veure pressupostos →
            </AMGButton>
          </div>
        )}

        {/* Loading skeleton */}
        {loading && (
          <div className="space-y-3">
            {[1, 2, 3].map(i => (
              <div key={i} className="amg-card card-clip h-24 animate-pulse bg-surface-1" />
            ))}
          </div>
        )}

        {/* Phase steps */}
        {!loading && phases.length > 0 && (
          <div className="space-y-3">
            <div className="f-mono text-label text-xs text-ink-3 uppercase tracking-widest">Fases contractades</div>
            {phases.map(phase => (
              <PhaseCard
                key={phase}
                phase={phase}
                sector={sector}
                configured={
                  phase === 'F5'
                    ? isEquipFullyConfigured(configs)
                    : isPhaseConfigured(configs, phase)
                }
                tenantId={tenantId}
                locale={locale}
              />
            ))}
          </div>
        )}

        {/* Bot IA — always shown */}
        {!loading && (
          <div className="space-y-3">
            <div className="f-mono text-label text-xs text-ink-3 uppercase tracking-widest">
              Configuració de l&apos;agent
            </div>
            <BotCard tenantId={tenantId} locale={locale} />
          </div>
        )}

        {/* Phase health diagnostic */}
        {!loading && phaseHealth && phaseHealth.results.length > 0 && (
          <div className="space-y-3">
            <div className="f-mono text-label text-xs text-ink-3 uppercase tracking-widest">
              Diagnòstic tècnic de fases
            </div>
            <PhaseHealthSection results={phaseHealth.results} />
          </div>
        )}

        {/* Phase activation history */}
        {!loading && phaseHistory.length > 0 && (
          <div className="space-y-3">
            <div className="f-mono text-label text-xs text-ink-3 uppercase tracking-widest">
              Historial d&apos;activació
            </div>
            <div className="amg-card card-clip p-4">
              <PhaseHistorySection records={phaseHistory} />
            </div>
          </div>
        )}

        {/* Go-Live panel */}
        {!loading && phases.length > 0 && (
          <div className="amg-card card-clip p-5 space-y-4">
            <div className="flex items-start justify-between gap-4">
              <div>
                <div className="f-mono text-label text-xs text-ink-3 uppercase tracking-widest mb-1">
                  Estat de l&apos;agent
                </div>
                {tenant?.activePhases && tenant.activePhases.length > 0 ? (
                  <div className="flex items-center gap-2">
                    <span className="w-2 h-2 rounded-full bg-success inline-block" />
                    <span className="f-display font-bold text-sm text-success">En Marxa</span>
                    <span className="f-mono text-label text-xs text-ink-2">
                      · fases actives: {tenant.activePhases.join(', ')}
                    </span>
                  </div>
                ) : (
                  <div className="flex items-center gap-2">
                    <span className="w-2 h-2 rounded-full bg-amber-400 inline-block" />
                    <span className="f-display font-bold text-sm text-amber-500">Pendent d&apos;activació</span>
                    <span className="f-mono text-label text-xs text-ink-2">
                      · l&apos;agent no respon fins que activis
                    </span>
                  </div>
                )}
              </div>
            </div>
            <p className="f-mono text-label text-xs text-ink-2">
              Fases contractades (pagades): <strong>{phases.join(', ')}</strong>
              {tenant?.activePhases && tenant.activePhases.length > 0
                ? ' — totes actives'
                : ' — pendents de posada en marxa'}
            </p>
            {/* Checklist de prerequisites go-live */}
            {(!tenant?.activePhases || tenant.activePhases.length === 0) && readiness && (
              <div className="mb-4 border border-[#222] p-4 space-y-2">
                <div className="f-mono text-label text-[10px] uppercase tracking-widest text-ink-3 mb-3">
                  Prerequisits per activar
                </div>
                {(
                  [
                    { key: 'intakeCompleted', label: 'Setup wizard completat pel client', actionHref: `/${locale}/portal/admin/tenants/${tenantId}/wizard`, actionLabel: 'Veure wizard' },
                    { key: 'hasLanding',      label: 'Landing publicada (estat ACTIVA)',  actionHref: `/${locale}/portal/hosting`,                             actionLabel: 'Gestionar landings' },
                    { key: 'hasKbEntries',    label: 'Knowledge base amb contingut',      actionHref: `/${locale}/portal/agents`,                              actionLabel: 'Afegir contingut' },
                    { key: 'hasChannel',      label: 'Canal de comunicació configurat',   actionHref: `/${locale}/portal/admin/tenants/${tenantId}`,           actionLabel: 'Configurar canals' },
                  ] as { key: keyof TenantReadiness; label: string; actionHref: string; actionLabel: string }[]
                ).map(({ key, label, actionHref, actionLabel }) => {
                  const ok = readiness[key] === true;
                  return (
                    <div key={key} className="flex items-center gap-2">
                      <div className={`w-4 h-4 flex items-center justify-center shrink-0 ${ok ? 'bg-[#ff6b00]' : 'border border-[#444]'}`}>
                        {ok
                          ? <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="black" strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                          : <span className="w-1.5 h-1.5 bg-[#555] rounded-full" />
                        }
                      </div>
                      <span className={`text-xs font-mono ${ok ? 'text-ink-1' : 'text-ink-3'}`}>{label}</span>
                      {!ok && (
                        <button
                          onClick={() => router.push(actionHref)}
                          className="ml-auto text-[10px] font-mono text-accent hover:text-accent-light underline underline-offset-2 shrink-0"
                        >
                          {actionLabel} →
                        </button>
                      )}
                    </div>
                  );
                })}
              </div>
            )}

            <div className="flex gap-3">
              {(!tenant?.activePhases || tenant.activePhases.length === 0) ? (
                <AMGButton
                  size="sm"
                  variant="primary"
                  onClick={() => goLiveMutation.mutate()}
                  disabled={goLiveMutation.isPending || (readiness != null && !readiness.ready)}
                >
                  {goLiveMutation.isPending ? 'Activant…' : 'Posar en marxa'}
                </AMGButton>
              ) : (
                <AMGButton
                  size="sm"
                  variant="secondary"
                  onClick={() => suspendMutation.mutate()}
                  disabled={suspendMutation.isPending}
                >
                  {suspendMutation.isPending ? 'Suspenent…' : 'Suspendre agent'}
                </AMGButton>
              )}
            </div>
          </div>
        )}

        {/* Context note */}
        {!loading && phases.length > 0 && (
          <div className="amg-card card-clip p-4 border-l-2 border-l-border-base">
            <div className="f-mono text-label text-xs text-ink-2 space-y-1">
              <p>
                <span className="text-ink-0 font-semibold">Captació (F1)</span> — la detecció és manual: comprova que hi hagi almenys una landing publicada a{' '}
                <button onClick={() => router.push(`/${locale}/portal/landings`)} className="text-accent hover:underline">
                  Landings
                </button>.
              </p>
              <p>
                <span className="text-ink-0 font-semibold">Agenda–Alertes (F2–F5)</span> — el sistema detecta automàticament si la configuració s'ha desat.
              </p>
              <p>
                Les opcions de cada fase s'adapten al sector{sector ? ` (${sectorLabel[sector] ?? sector})` : ''} i la mida del negoci.
              </p>
            </div>
          </div>
        )}

      </div>
    </PortalShell>
  );
}
