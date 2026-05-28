'use client';

import { useQueries } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useRouter, useParams } from 'next/navigation';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGBadge } from '@/components/ui/badge';
import { AMGButton } from '@/components/ui/button';
import { I } from '@/components/ui/icons';
import { getSystemConfig } from '@/services/sysconfig';
import { getLeadStats } from '@/services/leads';
import { getCampaigns } from '@/services/prospecting';
import { listTenants } from '@/services/admin';
import { getBackupDashboard } from '@/services/backup';
import { getOpsDashboard } from '@/services/ops';
import { getInfraStatus } from '@/services/infraops';

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
  if (s === 'ok') return { bar: 'bg-success', badge: 'success' as const, dot: '#39d353' };
  if (s === 'attention') return { bar: 'bg-warning', badge: 'warning' as const, dot: '#f0b429' };
  if (s === 'blocked') return { bar: 'bg-danger', badge: 'danger' as const, dot: '#ff4444' };
  return { bar: 'bg-ink-3', badge: 'neutral' as const, dot: '#64748b' };
}

function StatusDot({ status }: { status: StepStatus }) {
  const c = statusColor(status);
  return (
    <span className="w-2 h-2 rounded-full shrink-0 mt-1" style={{ background: c.dot }} />
  );
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
              <StatusDot status={item.ok === false ? 'blocked' : item.ok === true ? 'ok' : 'ok'} />
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
      { queryKey: ['system-config'], queryFn: getSystemConfig, enabled: !!user && isSuperAdmin },
      { queryKey: ['lead-stats'], queryFn: getLeadStats, enabled: !!user },
      { queryKey: ['campaigns'], queryFn: getCampaigns, enabled: !!user && isSuperAdmin },
      { queryKey: ['tenants-all'], queryFn: () => listTenants({ page: 0, size: 100, isActive: true }), enabled: !!user && isSuperAdmin },
      { queryKey: ['backup-dashboard'], queryFn: getBackupDashboard, enabled: !!user && isSuperAdmin },
      { queryKey: ['ops-dashboard'], queryFn: getOpsDashboard, enabled: !!user && isSuperAdmin },
      { queryKey: ['infra-status'], queryFn: getInfraStatus, enabled: !!user && isSuperAdmin, refetchInterval: 30000 },
    ],
  });

  if (!user) return null;

  const [cfgR, leadsR, campR, tenantsR, backupR, opsR, infraR] = results;
  const loading = results.some((r) => r.isLoading);

  // ── derived data ──────────────────────────────────────────────────────────
  const cfgList = (cfgR.data as any[] | undefined) ?? [];
  const missingKeys = cfgList.filter((c: any) => !c.configured).length;
  const configuredKeys = cfgList.filter((c: any) => c.configured).length;

  const leadStats = leadsR.data as any;
  const totalLeads = leadStats?.total ?? 0;
  const newLeads = leadStats?.byStage?.NEW ?? 0;
  const proposalLeads = (leadStats?.byStage?.PROPOSAL ?? 0) + (leadStats?.byStage?.NEGOTIATION ?? 0);
  const wonLeads = leadStats?.byStage?.WON ?? 0;

  const campaigns = (campR.data as any[] | undefined) ?? [];
  const activeCamps = campaigns.filter((c: any) => c.status === 'RUNNING').length;
  const completedCamps = campaigns.filter((c: any) => c.status === 'COMPLETED').length;
  const totalProspects = campaigns.reduce((sum: number, c: any) => sum + (c.prospectsFound ?? 0), 0);

  const allTenants = ((tenantsR.data as any)?.content ?? []) as import('@/services/admin').TenantResponse[];
  const tenantsTotal = (tenantsR.data as any)?.totalElements ?? allTenants.length;
  const tenantsWithPhases = allTenants.filter(t => t.contractedPhases && t.contractedPhases.length > 0);

  const backup = backupR.data as any;
  const lastBackupOk = backup?.lastBackupStatus === 'COMPLETED';
  const lastBackupDate = backup?.lastBackup
    ? new Date(backup.lastBackup).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short' })
    : 'Mai';

  const ops = opsR.data as any;
  const servicesUp = ops?.currentStatus?.up ?? 0;
  const servicesTotal = ops?.currentStatus?.services ?? 0;
  const openIncidents = ops?.openIncidents ?? 0;

  const infra = infraR.data as any;
  const infraOk = infra?.overallStatus === 'OK';

  // ── step definitions ──────────────────────────────────────────────────────
  const steps: StepCard[] = [
    {
      num: 1,
      title: 'Configuració inicial',
      desc: 'Configura les API keys, el catàleg de serveis i les plantilles de landing.',
      icon: I.Key,
      status: loading ? 'loading' : missingKeys > 5 ? 'blocked' : missingKeys > 0 ? 'attention' : 'ok',
      items: [
        { label: 'API Keys', value: `${configuredKeys} / ${cfgList.length} configurades`, ok: missingKeys === 0 },
        { label: 'Catàleg', value: 'Perfils i serveis', ok: true },
        { label: 'Plantilles de landing', value: 'Disponibles', ok: true },
      ],
      note: missingKeys > 0 ? `${missingKeys} claus pendents. El sistema pot fallar sense elles.` : undefined,
      actions: [
        { label: 'API Keys', href: '/portal/admin/config', primary: missingKeys > 0 },
        { label: 'Catàleg', href: '/portal/admin/vault' },
        { label: 'Plantilles', href: '/portal/admin/templates' },
      ],
    },
    {
      num: 2,
      title: 'Prospecció',
      desc: 'Cerca empreses per sector i localitat amb Google Maps o Pàgines Grogues.',
      icon: I.Search,
      status: loading ? 'loading' : campaigns.length === 0 ? 'blocked' : activeCamps > 0 ? 'attention' : 'ok',
      items: [
        { label: 'Campanyes totals', value: campaigns.length },
        { label: 'En execució', value: activeCamps },
        { label: 'Completades', value: completedCamps },
        { label: 'Prospects trobats', value: totalProspects },
      ],
      note: 'Executa una campanya → selecciona prospects → exporta a Leads CRM.',
      actions: [
        { label: 'Nova campanya', href: '/portal/prospecting', primary: campaigns.length === 0 },
        { label: 'Veure campanyes', href: '/portal/prospecting' },
      ],
    },
    {
      num: 3,
      title: 'Leads CRM',
      desc: 'Gestiona el pipeline de leads des de Nou fins a Guanyat o Perdut.',
      icon: I.Users,
      status: loading ? 'loading' : totalLeads === 0 ? 'blocked' : proposalLeads > 0 ? 'attention' : 'ok',
      items: [
        { label: 'Total leads', value: totalLeads },
        { label: 'Nous (sense contactar)', value: newLeads, ok: newLeads === 0 },
        { label: 'En proposta / negociació', value: proposalLeads, ok: proposalLeads === 0 },
        { label: 'Guanyats', value: wonLeads },
      ],
      note: 'Pipeline: Nou → Contactat → Qualificat → Proposta → Negociació → Guanyat.',
      actions: [
        { label: 'Veure leads', href: '/portal/leads', primary: newLeads > 0 },
        { label: 'Nou lead', href: '/portal/leads/new' },
      ],
    },
    {
      num: 4,
      title: 'Crear Tenant',
      desc: 'Quan un lead és guanyat, crea el seu compte (tenant) i assigna-li un perfil de serveis.',
      icon: I.Building,
      status: loading ? 'loading' : tenantsTotal === 0 ? 'blocked' : 'ok',
      items: [
        { label: 'Tenants actius', value: tenantsTotal },
        { label: 'Pas 1', value: 'Admin → Tenants → Crear nou', ok: true },
        { label: 'Pas 2', value: 'Obrir tenant → Assignar perfil', ok: true },
        { label: 'Pas 3', value: 'Crear usuari client i enviar credencials', ok: true },
      ],
      note: 'El perfil determina quins serveis i fases es pressupostaran automàticament.',
      actions: [
        { label: 'Tenants', href: '/portal/admin/tenants', primary: true },
        { label: 'Crear tenant', href: '/portal/admin/tenants/new' },
      ],
    },
    {
      num: 5,
      title: 'Pressupost',
      desc: 'Genera el pressupost per fases, envia\'l al client i espera l\'acceptació.',
      icon: I.Receipt,
      status: loading ? 'loading' : 'ok',
      items: [
        { label: 'Pas 1', value: 'Tenant → Pressupostos → Crear per perfil', ok: true },
        { label: 'Pas 2', value: 'Revisar línies i aplicar descomptes', ok: true },
        { label: 'Pas 3', value: 'Enviar → el client rep email amb link', ok: true },
        { label: 'Pas 4', value: 'Acceptat → botó "Posar en marxa" al pressupost', ok: true },
      ],
      note: 'Un cop acceptat, fes clic a "Posar en marxa" per iniciar el wizard d\'activació de fases.',
      actions: [
        { label: 'Veure pressupostos', href: '/portal/billing', primary: true },
        { label: 'Programes comercials', href: '/portal/billing/programs' },
      ],
    },
    {
      num: 6,
      title: 'Implementació de serveis',
      desc: 'Configura cada fase contractada amb el wizard "Posar en marxa" per tenant.',
      icon: I.Layers,
      status: loading ? 'loading' : tenantsWithPhases.length === 0 ? 'ok' : 'attention',
      items: [
        { label: 'Tenants amb fases contractades', value: tenantsWithPhases.length, ok: tenantsWithPhases.length === 0 },
        { label: 'Wizard F1', value: 'Landing → Factory → publicar', ok: true },
        { label: 'Wizard F2–F5', value: 'Agenda, Pressupostos, Fidelització, Equip', ok: true },
        { label: 'Bot IA', value: 'Agents → canals + base de coneixement', ok: true },
      ],
      note: 'Obre la fitxa de cada tenant → "Posar en marxa" per veure totes les fases i el seu estat.',
      actions: [
        { label: 'Tenants', href: '/portal/admin/tenants', primary: tenantsWithPhases.length > 0 },
        { label: 'Landings', href: '/portal/landings' },
        { label: 'Agents', href: '/portal/agents' },
      ],
    },
    {
      num: 7,
      title: 'Agents & Automatitzacions',
      desc: 'Activa els agents de comunicació IA i les automatitzacions n8n.',
      icon: I.Bot,
      status: loading ? 'loading' : 'ok',
      items: [
        { label: 'Mode agent', value: 'AUTO / HYBRID / MANUAL', ok: true },
        { label: 'Canals disponibles', value: 'Telegram, WhatsApp, Email', ok: true },
        { label: 'n8n workflows', value: 'Assignar des de catàleg', ok: true },
      ],
      note: 'Mode HYBRID: el bot suggereix respostes per aprovar. AUTO: respon sol. MANUAL: solo notificació.',
      actions: [
        { label: 'Agents', href: '/portal/agents', primary: true },
        { label: 'Automatitzacions', href: '/portal/automations' },
      ],
    },
    {
      num: 8,
      title: 'Manteniment i monitorització',
      desc: 'Supervisa serveis, backups i infraestructura. Respon incidents ràpidament.',
      icon: I.Activity,
      status: loading ? 'loading' : !infraOk || openIncidents > 0 ? 'attention' : !lastBackupOk ? 'attention' : 'ok',
      items: [
        {
          label: 'Serveis',
          value: servicesTotal > 0 ? `${servicesUp}/${servicesTotal} actius` : 'Sense dades',
          ok: servicesTotal > 0 && servicesUp === servicesTotal,
        },
        { label: 'Incidents oberts', value: openIncidents, ok: openIncidents === 0 },
        { label: 'Infraestructura', value: infra?.overallStatus ?? '—', ok: infraOk },
        { label: 'Últim backup', value: lastBackupDate, ok: lastBackupOk },
      ],
      actions: [
        { label: 'Ops & Health', href: '/portal/ops', primary: openIncidents > 0 },
        { label: 'InfraOps', href: '/portal/admin/infraops' },
        { label: 'Backup', href: '/portal/admin/backup' },
      ],
    },
  ];

  // ── summary blockers ──────────────────────────────────────────────────────
  const blockers = steps.filter((s) => s.status === 'blocked' || s.status === 'attention');

  return (
    <PortalShell breadcrumb="procés">
      <div className="p-4 sm:p-8 space-y-6">

        {/* Header */}
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / procés /</span>
          <div className="f-display font-bold text-xl mt-1">Flux de treball</div>
          <p className="f-mono text-label text-ink-2 mt-1">
            Guia pas a pas: des de la configuració inicial fins al manteniment dels clients.
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
              <I.Check size={14} stroke="#39d353" />
              <span className="f-mono text-label text-xs text-success">Tot el flux de treball operatiu. Cap acció pendent.</span>
            </div>
          </div>
        )}

        {/* Flow diagram (visual) */}
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
                        background: s.status === 'ok' ? 'rgba(57,211,83,0.08)' : s.status === 'attention' ? 'rgba(240,180,41,0.08)' : s.status === 'blocked' ? 'rgba(255,68,68,0.08)' : 'rgba(100,116,139,0.08)',
                      }}
                    >
                      {s.num}
                    </div>
                    <span className="f-mono text-[9px] text-ink-2 text-center max-w-[56px] leading-tight">{s.title}</span>
                  </div>
                  {i < steps.length - 1 && (
                    <div className="w-8 h-px bg-border-base mx-1 shrink-0" />
                  )}
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

        {/* Tenants amb fases contractades */}
        {isSuperAdmin && tenantsWithPhases.length > 0 && (
          <div className="amg-card card-clip p-5 space-y-4">
            <div className="flex items-center gap-2">
              <I.Zap size={14} className="text-accent-light" />
              <span className="f-display font-bold text-sm">Fases contractades per activar</span>
              <AMGBadge tone="warning">{tenantsWithPhases.length}</AMGBadge>
            </div>
            <p className="f-mono text-label text-xs text-ink-2">
              Tenants amb fases NexeLocal contractades. Obre el wizard per configurar cada fase.
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
                    <I.Zap size={11} /> Posar en marxa
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
