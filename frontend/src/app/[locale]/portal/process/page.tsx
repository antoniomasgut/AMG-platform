'use client';

import { useQueries } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useRouter, useParams } from 'next/navigation';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGBadge } from '@/components/ui/badge';
import { AMGButton } from '@/components/ui/button';
import { IconSet } from '@/components/ui/icons';
import { getSystemConfig } from '@/services/sysconfig';
import { getLeadStats } from '@/services/leads';
import { getCampaigns } from '@/services/prospecting';
import { listTenants } from '@/services/admin';
import { getBackupDashboard } from '@/services/backup';
import { getOpsDashboard } from '@/services/ops';
import { getInfraStatus } from '@/services/infraops';
import { listAllBudgets } from '@/services/billing';

// ─── types ────────────────────────────────────────────────────────────────────

type StepStatus = 'ok' | 'attention' | 'blocked' | 'loading';

interface StepCard {
  num: number;
  title: string;
  desc: string;
  icon: (p: { size?: number }) => React.ReactNode;
  status: StepStatus;
  items: { label: string; value: string | number; ok?: boolean }[];
  actions: { label: string; href: string; primary?: boolean }[];
  note?: string;
}

// ─── helpers ──────────────────────────────────────────────────────────────────

function statusColor(s: StepStatus) {
  if (s === 'ok')        return { bar: 'bg-success',  badge: 'success' as const, dot: '#39d353' };
  if (s === 'attention') return { bar: 'bg-warning',  badge: 'warning' as const, dot: '#f0b429' };
  if (s === 'blocked')   return { bar: 'bg-danger',   badge: 'danger'  as const, dot: '#ff4444' };
  return                        { bar: 'bg-ink-3',    badge: 'neutral' as const, dot: '#64748b' };
}

function StatusDot({ status }: { status: StepStatus }) {
  const c = statusColor(status);
  return <span className="w-2 h-2 rounded-full shrink-0 mt-1" style={{ background: c.dot }} />;
}

function StepCard({ step, locale }: { step: StepCard; locale: string }) {
  const router = useRouter();
  const c = statusColor(step.status);

  return (
    <div className="amg-card card-clip overflow-hidden">
      <div className={`h-0.5 ${c.bar}`} />
      <div className="p-5">
        <div className="flex items-start gap-3 mb-4">
          <div className="w-7 h-7 shrink-0 flex items-center justify-center bg-accent-muted text-accent-light f-mono font-bold text-xs">
            {step.num}
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-0.5">
              <span className="f-display font-bold text-sm">{step.title}</span>
              <AMGBadge tone={c.badge}>
                {step.status === 'ok' ? 'OK' : step.status === 'attention' ? 'Atenció' : step.status === 'blocked' ? 'Pendent' : '…'}
              </AMGBadge>
            </div>
            <p className="f-mono text-label text-ink-2 text-xs">{step.desc}</p>
          </div>
          <step.icon size={16} />
        </div>

        <div className="space-y-1 mb-4">
          {step.items.map((item) => (
            <div key={item.label} className="flex items-center gap-2">
              <StatusDot status={item.ok === false ? 'blocked' : 'ok'} />
              <span className="f-mono text-label text-ink-2 text-xs">{item.label}:</span>
              <span className={`f-mono text-label text-xs font-semibold ${item.ok === false ? 'text-danger' : 'text-ink-0'}`}>
                {item.value}
              </span>
            </div>
          ))}
        </div>

        {step.note && (
          <p className="f-mono text-label text-xs text-ink-3 mb-3 border-l border-border-base pl-2">{step.note}</p>
        )}

        <div className="flex flex-wrap gap-2">
          {step.actions.map((a) => (
            <AMGButton
              key={a.label}
              size="sm"
              variant={a.primary ? 'primary' : 'secondary'}
              onClick={() => router.push(`/${locale}${a.href}`)}
            >
              {a.label}
            </AMGButton>
          ))}
        </div>
      </div>
    </div>
  );
}

// ─── page ─────────────────────────────────────────────────────────────────────

export default function ProcessPage() {
  const { user, isSuperAdmin } = useAuth();
  const router = useRouter();
  const params = useParams();
  const locale = (params.locale as string) ?? 'ca';

  const results = useQueries({
    queries: [
      { queryKey: ['system-config'],    queryFn: getSystemConfig,                                                    enabled: !!user && isSuperAdmin },
      { queryKey: ['lead-stats'],       queryFn: getLeadStats,                                                       enabled: !!user },
      { queryKey: ['campaigns'],        queryFn: getCampaigns,                                                       enabled: !!user && isSuperAdmin },
      { queryKey: ['tenants-process'],  queryFn: () => listTenants({ page: 0, size: 100, isActive: true }),          enabled: !!user && isSuperAdmin },
      { queryKey: ['all-budgets'],      queryFn: () => listAllBudgets(undefined, 0, 100),                            enabled: !!user && isSuperAdmin },
      { queryKey: ['ops-dashboard'],    queryFn: getOpsDashboard,                                                    enabled: !!user && isSuperAdmin },
      { queryKey: ['infra-status'],     queryFn: getInfraStatus,    refetchInterval: 30000,                          enabled: !!user && isSuperAdmin },
      { queryKey: ['backup-dashboard'], queryFn: getBackupDashboard,                                                 enabled: !!user && isSuperAdmin },
    ],
  });

  if (!user) return null;

  const [cfgR, leadsR, campR, tenantsR, budgetsR, opsR, infraR, backupR] = results;
  const loading = results.some((r) => r.isLoading);

  // ── step 1: configuració ──────────────────────────────────────────────────
  const cfgList   = (cfgR.data as any[] | undefined) ?? [];
  const missingKeys     = cfgList.filter((c: any) => !c.configured).length;
  const configuredKeys  = cfgList.filter((c: any) => c.configured).length;

  // ── step 2: captació ──────────────────────────────────────────────────────
  const campaigns      = (campR.data as any[] | undefined) ?? [];
  const activeCamps    = campaigns.filter((c: any) => c.status === 'RUNNING').length;
  const completedCamps = campaigns.filter((c: any) => c.status === 'COMPLETED').length;
  const totalProspects = campaigns.reduce((sum: number, c: any) => sum + (c.totalFound ?? 0), 0);

  // ── step 3: qualificació ──────────────────────────────────────────────────
  const leadStats     = leadsR.data as any;
  const totalLeads    = leadStats?.total ?? 0;
  const newLeads      = leadStats?.byStage?.NEW ?? 0;
  const contactedLeads= leadStats?.byStage?.CONTACTED ?? 0;
  const proposalLeads = (leadStats?.byStage?.PROPOSAL ?? 0) + (leadStats?.byStage?.NEGOTIATION ?? 0);
  const wonLeads      = leadStats?.byStage?.WON ?? 0;

  // ── step 4: pressupost ────────────────────────────────────────────────────
  const allBudgets  = (budgetsR.data as any[] | undefined) ?? [];
  const bDraft      = allBudgets.filter((b: any) => b.status === 'DRAFT').length;
  const bSent       = allBudgets.filter((b: any) => b.status === 'SENT').length;
  const bAccepted   = allBudgets.filter((b: any) => b.status === 'ACCEPTED').length;
  const bRejected   = allBudgets.filter((b: any) => ['REJECTED', 'CANCELLED'].includes(b.status)).length;

  // ── step 5: implementació ─────────────────────────────────────────────────
  const allTenants       = ((tenantsR.data as any)?.content ?? []) as import('@/services/admin').TenantResponse[];
  const tenantsTotal     = (tenantsR.data as any)?.totalElements ?? allTenants.length;
  const tenantsWithPhases = allTenants.filter(t => t.contractedPhases && t.contractedPhases.length > 0);

  // ── step 6: monitoratge ───────────────────────────────────────────────────
  const ops          = opsR.data as any;
  const servicesUp   = ops?.currentStatus?.up ?? 0;
  const servicesTotal= ops?.currentStatus?.services ?? 0;
  const openIncidents= ops?.openIncidents ?? 0;

  const infra  = infraR.data as any;
  const infraOk= infra?.overallStatus === 'OK';

  const backup       = backupR.data as any;
  const lastBackupOk = backup?.lastBackupStatus === 'COMPLETED';
  const lastBackupDate = backup?.lastBackup
    ? new Date(backup.lastBackup).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short' })
    : 'Mai';

  // ── step definitions ──────────────────────────────────────────────────────
  const steps: StepCard[] = [
    {
      num: 1,
      title: 'Configuració del sistema',
      desc: 'API keys, canals de comunicació i plantilles. Les mancances crítiques bloquegen el funcionament.',
      icon: IconSet.Key,
      status: loading ? 'loading' : missingKeys > 5 ? 'blocked' : missingKeys > 0 ? 'attention' : 'ok',
      items: [
        { label: 'API Keys',    value: `${configuredKeys} / ${cfgList.length} configurades`, ok: missingKeys === 0 },
        { label: 'Catàleg',     value: 'Sectors, perfils i preus',                           ok: true },
        { label: 'Plantilles',  value: 'Landings i documents',                               ok: true },
      ],
      note: missingKeys > 0 ? `${missingKeys} claus pendents. Alguns serveis poden no funcionar.` : undefined,
      actions: [
        { label: 'API Keys',   href: '/portal/admin/config',     primary: missingKeys > 0 },
        { label: 'Catàleg',    href: '/portal/admin/vault' },
        { label: 'Plantilles', href: '/portal/admin/templates' },
      ],
    },
    {
      num: 2,
      title: 'Captació de clients',
      desc: 'Prospecció manual per sector/localitat i campanyes Meta Ads que injecten leads via landing.',
      icon: IconSet.Search,
      status: loading ? 'loading' : campaigns.length === 0 ? 'blocked' : activeCamps > 0 ? 'attention' : 'ok',
      items: [
        { label: 'Campanyes prospecció', value: campaigns.length,  ok: campaigns.length > 0 },
        { label: 'En execució',          value: activeCamps },
        { label: 'Completades',          value: completedCamps },
        { label: 'Prospects trobats',    value: totalProspects,    ok: totalProspects > 0 },
        { label: 'Meta Ads',             value: 'Configura per tenant → Agents & IA', ok: true },
      ],
      note: campaigns.length === 0 ? 'Inicia una campanya de prospecció o activa Meta Ads per captar leads.' : undefined,
      actions: [
        { label: 'Nova campanya',   href: '/portal/prospecting', primary: campaigns.length === 0 },
        { label: 'Prospecció',      href: '/portal/prospecting' },
        { label: 'Meta Ads',        href: '/portal/admin/tenants' },
      ],
    },
    {
      num: 3,
      title: 'Qualificació del lead',
      desc: 'Contacta per WhatsApp o email amb una demo del sector, qualifica i agenda reunió.',
      icon: IconSet.Users,
      status: loading ? 'loading' : totalLeads === 0 ? 'blocked' : newLeads > 0 ? 'attention' : 'ok',
      items: [
        { label: 'Total leads',              value: totalLeads,       ok: totalLeads > 0 },
        { label: 'Nous (sense contactar)',   value: newLeads,         ok: newLeads === 0 },
        { label: 'Contactats',               value: contactedLeads },
        { label: 'En proposta/negociació',   value: proposalLeads,    ok: proposalLeads === 0 },
        { label: 'Guanyats',                 value: wonLeads },
      ],
      note: newLeads > 0
        ? `${newLeads} lead${newLeads > 1 ? 's' : ''} sense contactar. Envia la demo del sector i agenda reunió.`
        : undefined,
      actions: [
        { label: 'Veure leads',  href: '/portal/leads', primary: newLeads > 0 },
        { label: 'Nou lead',     href: '/portal/leads/new' },
        { label: 'Demos',        href: '/portal/admin/demos' },
      ],
    },
    {
      num: 4,
      title: 'Pressupost i tancament',
      desc: 'Crea el pressupost per fases segons el que ha demanat el client a la reunió i espera l\'acceptació.',
      icon: IconSet.Receipt,
      status: loading ? 'loading' : bSent > 0 ? 'attention' : 'ok',
      items: [
        { label: 'Esborranys',              value: bDraft,    ok: bDraft === 0 },
        { label: 'Enviats (sense resposta)', value: bSent,    ok: bSent === 0 },
        { label: 'Acceptats',               value: bAccepted, ok: bAccepted > 0 || allBudgets.length === 0 },
        { label: 'Rebutjats/cancel·lats',   value: bRejected },
      ],
      note: bSent > 0
        ? `${bSent} pressupost${bSent > 1 ? 's' : ''} enviats esperant resposta del client.`
        : bDraft > 0
        ? `${bDraft} pressupost${bDraft > 1 ? 's' : ''} en esborrany. Revisa i envia al client.`
        : undefined,
      actions: [
        { label: 'Pressupostos', href: '/portal/billing', primary: bSent > 0 || bDraft > 0 },
        { label: 'Nou pressupost', href: '/portal/billing' },
      ],
    },
    {
      num: 5,
      title: 'Implementació',
      desc: 'El tenant es crea amb les fases contractades. Configura-les una a una amb el wizard.',
      icon: IconSet.Layers,
      status: loading ? 'loading' : tenantsTotal === 0 ? 'blocked' : tenantsWithPhases.length > 0 ? 'attention' : 'ok',
      items: [
        { label: 'Tenants actius',               value: tenantsTotal,               ok: tenantsTotal > 0 },
        { label: 'Fases pendents de configurar', value: tenantsWithPhases.length,   ok: tenantsWithPhases.length === 0 },
        { label: 'Landings',                     value: 'Factory → publicar',        ok: true },
        { label: 'Agents IA',                    value: 'Canals + base coneixement', ok: true },
      ],
      note: tenantsWithPhases.length > 0
        ? `${tenantsWithPhases.length} tenant${tenantsWithPhases.length > 1 ? 's' : ''} amb fases per configurar. Obre el wizard "Posar en marxa".`
        : undefined,
      actions: [
        { label: 'Tenants',    href: '/portal/admin/tenants', primary: tenantsWithPhases.length > 0 },
        { label: 'Landings',   href: '/portal/landings' },
        { label: 'Agents',     href: '/portal/agents' },
      ],
    },
    {
      num: 6,
      title: 'Monitoratge i backup',
      desc: 'Supervisa serveis i infraestructura. Els backups permeten recuperar dades si cal.',
      icon: IconSet.Activity,
      status: loading ? 'loading'
        : openIncidents > 0 || !infraOk ? 'attention'
        : !lastBackupOk ? 'attention'
        : 'ok',
      items: [
        {
          label: 'Serveis',
          value: servicesTotal > 0 ? `${servicesUp}/${servicesTotal} actius` : 'Sense dades',
          ok: servicesTotal === 0 || servicesUp === servicesTotal,
        },
        { label: 'Incidents oberts',  value: openIncidents,             ok: openIncidents === 0 },
        { label: 'Infraestructura',   value: infra?.overallStatus ?? '—', ok: infraOk },
        { label: 'Últim backup',      value: lastBackupDate,             ok: lastBackupOk },
      ],
      note: openIncidents > 0
        ? `${openIncidents} incident${openIncidents > 1 ? 's' : ''} obert${openIncidents > 1 ? 's' : ''}. Revisa Ops & Health.`
        : !lastBackupOk && backup ? 'El darrer backup no ha completat correctament.'
        : undefined,
      actions: [
        { label: 'Ops & Health', href: '/portal/ops',            primary: openIncidents > 0 },
        { label: 'InfraOps',     href: '/portal/admin/infraops' },
        { label: 'Backup',       href: '/portal/admin/backup' },
      ],
    },
  ];

  const blockers = steps.filter((s) => s.status === 'blocked' || s.status === 'attention');

  return (
    <PortalShell breadcrumb="procés">
      <div className="p-4 sm:p-8 space-y-6">

        {/* Header */}
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / procés /</span>
          <div className="f-display font-bold text-xl mt-1">Flux de treball</div>
          <p className="f-mono text-label text-ink-2 mt-1">
            Els 6 passos del cicle complet: des de la configuració fins al monitoratge dels clients.
          </p>
        </div>

        {/* Blockers banner */}
        {!loading && blockers.length > 0 && (
          <div className="amg-card card-clip p-4 border-l-2 border-l-warning bg-warning/5">
            <div className="f-mono text-label text-xs text-warning uppercase tracking-widest mb-2">Accions pendents</div>
            <div className="space-y-1">
              {blockers.map((s) => (
                <div key={s.num} className="flex items-center gap-2 f-mono text-xs text-ink-1">
                  <span className="text-warning">→</span>
                  <span className="font-semibold">{s.num}. {s.title}:</span>
                  <span>{s.note ?? s.desc}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {!loading && blockers.length === 0 && (
          <div className="amg-card card-clip p-4 border-l-2 border-l-success bg-success/5">
            <div className="flex items-center gap-2">
              <IconSet.Check size={14} stroke="#39d353" />
              <span className="f-mono text-label text-xs text-success">Tot el flux operatiu. Cap acció pendent.</span>
            </div>
          </div>
        )}

        {/* Flow diagram */}
        <div className="amg-card card-clip p-4 overflow-x-auto">
          <div className="f-mono text-label text-xs text-ink-2 uppercase tracking-widest mb-3">Cicle complet</div>
          <div className="flex items-center gap-0 min-w-max">
            {steps.map((s, i) => {
              const c = statusColor(s.status);
              return (
                <div key={s.num} className="flex items-center">
                  <div className="flex flex-col items-center gap-1">
                    <div
                      className="w-8 h-8 flex items-center justify-center f-mono text-xs font-bold border-2 transition-colors"
                      style={{
                        borderColor: c.dot,
                        color: c.dot,
                        background: s.status === 'ok'        ? 'rgba(57,211,83,0.08)'
                                  : s.status === 'attention' ? 'rgba(240,180,41,0.08)'
                                  : s.status === 'blocked'   ? 'rgba(255,68,68,0.08)'
                                  : 'rgba(100,116,139,0.08)',
                      }}
                    >
                      {s.num}
                    </div>
                    <span className="f-mono text-[9px] text-ink-2 text-center max-w-[56px] leading-tight">{s.title}</span>
                  </div>
                  {i < steps.length - 1 && <div className="w-8 h-px bg-border-base mx-1 shrink-0" />}
                </div>
              );
            })}
          </div>
        </div>

        {/* Step cards */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {steps.map((s) => (
            <StepCard key={s.num} step={s} locale={locale} />
          ))}
        </div>

        {/* Tenants amb fases pendents */}
        {isSuperAdmin && tenantsWithPhases.length > 0 && (
          <div className="amg-card card-clip p-5 space-y-4">
            <div className="flex items-center gap-2">
              <IconSet.Zap size={14} className="text-accent-light" />
              <span className="f-display font-bold text-sm">Fases pendents de configurar</span>
              <AMGBadge tone="warning">{tenantsWithPhases.length}</AMGBadge>
            </div>
            <p className="f-mono text-label text-xs text-ink-2">
              Obriu el wizard &quot;Posar en marxa&quot; per configurar les fases contractades de cada tenant.
            </p>
            <div className="space-y-2">
              {tenantsWithPhases.map((t) => (
                <div key={t.id} className="flex items-center justify-between gap-3 py-2 border-b border-border-base last:border-0">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="f-display font-semibold text-sm text-ink-0 truncate">{t.name}</span>
                      {t.sector && (
                        <span className="f-mono text-[10px] text-ink-3 border border-border-base px-1.5 py-0.5 uppercase tracking-wide">
                          {t.sector}
                        </span>
                      )}
                    </div>
                    <div className="flex items-center gap-1.5 mt-0.5 flex-wrap">
                      {t.contractedPhases!.map(p => (
                        <span key={p} className="f-mono text-[10px] text-accent-light border border-accent-muted px-1.5 py-0.5">{p}</span>
                      ))}
                    </div>
                  </div>
                  <AMGButton
                    size="sm"
                    variant="primary"
                    onClick={() => router.push(`/${locale}/portal/admin/tenants/${t.id}/activate`)}
                  >
                    <IconSet.Zap size={11} /> Posar en marxa
                  </AMGButton>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </PortalShell>
  );
}
