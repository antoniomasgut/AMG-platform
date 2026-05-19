'use client';

import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '@/lib/auth-context';
import { useApiErrorHandler } from '@/lib/use-api-error';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { I } from '@/components/ui/icons';
import {
  fetchBillingDashboard, fetchInvoices, fetchLandings, fetchWorkflows,
  type BillingDashboard, type Invoice, type LandingSummary, type WorkflowSummary,
} from '@/services/dashboard';
import { listTenants } from '@/services/admin';
import { getLeadStats, type LeadStats } from '@/services/leads';
import { getOpsDashboard, type OpsDashboard } from '@/services/ops';
import { getInfraStatus, type InfraStatus } from '@/services/infraops';
import { OnboardingGuide } from '@/components/portal/OnboardingGuide';

function formatDate(iso: string | null): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

function formatEur(cents: number | null): string {
  if (cents == null) return '€0';
  return new Intl.NumberFormat('ca-ES', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(cents);
}

const BADGE_TONE: Record<string, 'success' | 'warning' | 'danger' | 'accent'> = {
  PAID: 'success', PAGAT: 'success', COMPLETED: 'success',
  PENDING: 'warning', OVERDUE: 'danger', CANCELLED: 'danger', FAILED: 'danger',
  DRAFT: 'accent',
};

/* ─── KPI card ─── */
function KpiCard({ label, value, sub, tone = 'default', icon: Icon, href }: {
  label: string; value: string | number; sub?: string;
  tone?: 'default' | 'ok' | 'warn' | 'crit'; icon?: React.FC<{ size?: number }>;
  href?: string;
}) {
  const bar = tone === 'ok' ? 'bg-[#39d353]' : tone === 'warn' ? 'bg-[#f0b429]' : tone === 'crit' ? 'bg-[#f85149]' : 'bg-[#FF6B00]';
  const content = (
    <div className={`amg-card card-clip p-4 relative overflow-hidden ${href ? 'cursor-pointer hover:border-border-strong transition-colors' : ''}`}>
      <div className={`absolute top-0 left-0 right-0 h-[2px] ${bar}`} />
      <div className="flex items-start justify-between gap-2">
        <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3 mt-1">{label}</div>
        {Icon && <Icon size={13} />}
      </div>
      <div className="f-display font-black text-2xl mt-2 leading-none">{value}</div>
      {sub && <div className="f-mono text-[10px] text-ink-2 mt-1 uppercase">{sub}</div>}
    </div>
  );
  if (href) return <a href={href}>{content}</a>;
  return content;
}

/* ─── Gauge bar ─── */
function Gauge({ label, pct }: { label: string; pct: number }) {
  const color = pct >= 90 ? 'bg-[#f85149]' : pct >= 75 ? 'bg-[#f0b429]' : 'bg-[#39d353]';
  return (
    <div>
      <div className="flex justify-between mb-1">
        <span className="f-mono text-[10px] uppercase text-ink-2">{label}</span>
        <span className={`f-mono text-[10px] font-bold ${pct >= 90 ? 'text-[#f85149]' : pct >= 75 ? 'text-[#f0b429]' : 'text-ink-1'}`}>{pct}%</span>
      </div>
      <div className="h-1 bg-[#212140] overflow-hidden rounded-full">
        <div className={`h-full ${color} transition-all`} style={{ width: `${Math.min(pct, 100)}%` }} />
      </div>
    </div>
  );
}

/* ─────────────────────────────────────────────
   SUPER_ADMIN / ADMIN dashboard
───────────────────────────────────────────── */
interface AdminData {
  totalTenants: number;
  leads: LeadStats | null;
  ops: OpsDashboard | null;
  infra: InfraStatus | null;
}

function AdminDashboard({ data, loading }: { data: AdminData; loading: boolean }) {
  const { leads, ops, infra, totalTenants } = data;

  const sysOk = ops ? ops.currentStatus.up === ops.currentStatus.services && ops.openIncidents === 0 : null;
  const infraTone = infra
    ? (infra.cpu.percent >= 90 || infra.ram.percent >= 90 || infra.disk.percent >= 90 ? 'crit'
      : infra.cpu.percent >= 75 || infra.ram.percent >= 75 || infra.disk.percent >= 75 ? 'warn' : 'ok')
    : 'default';

  const STAGES = ['PROSPECTING', 'CONTACT', 'PROPOSAL', 'NEGOTIATION', 'WON', 'LOST'];
  const STAGE_LABEL: Record<string, string> = {
    PROSPECTING: 'Prospecció', CONTACT: 'Contacte', PROPOSAL: 'Proposta',
    NEGOTIATION: 'Negociació', WON: 'Guanyat', LOST: 'Perdut',
  };

  if (loading) {
    return (
      <div className="p-6 space-y-4">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="h-24 animate-pulse bg-[#212140] rounded" />
        ))}
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6 space-y-6">

      {/* KPIs principals */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <KpiCard
          label="Tenants actius"
          value={totalTenants}
          sub="clients"
          icon={I.Building}
          href="/portal/admin/tenants"
        />
        <KpiCard
          label="Leads actius"
          value={leads ? (leads.total - (leads.byStage?.LOST ?? 0) - (leads.byStage?.WON ?? 0)) : '—'}
          sub={leads ? `${leads.total} total · ${Math.round(leads.conversionRate ?? 0)}% conv.` : ''}
          icon={I.Users}
          href="/portal/leads"
        />
        <KpiCard
          label="Serveis monitorats"
          value={ops ? `${ops.currentStatus.up}/${ops.currentStatus.services}` : '—'}
          sub={ops ? (ops.openIncidents > 0 ? `${ops.openIncidents} incidents oberts` : 'Sense incidents') : ''}
          tone={sysOk === true ? 'ok' : sysOk === false ? 'crit' : 'default'}
          icon={I.Activity}
          href="/portal/ops"
        />
        <KpiCard
          label="Infraestructura"
          value={infra ? `${infra.cpu.percent}% CPU` : '—'}
          sub={infra ? `RAM ${infra.ram.percent}% · Disk ${infra.disk.percent}%` : ''}
          tone={infraTone}
          icon={I.Server}
          href="/portal/admin/infraops"
        />
      </div>

      {/* Leads pipeline + Infra */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">

        {/* Pipeline de leads */}
        <div className="amg-card card-clip p-4 sm:p-5">
          <div className="flex items-center justify-between mb-4">
            <div>
              <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3">Captació</div>
              <div className="f-display font-bold text-sm mt-0.5">Pipeline de leads</div>
            </div>
            <a href="/portal/leads" className="f-mono text-[10px] uppercase text-accent-light hover:underline">VEURE TOT →</a>
          </div>
          {leads ? (
            <div className="space-y-2">
              {STAGES.filter(s => s !== 'LOST').map(stage => {
                const count = leads.byStage?.[stage] ?? 0;
                const total = leads.total || 1;
                return (
                  <div key={stage} className="flex items-center gap-3">
                    <span className="f-mono text-[10px] uppercase text-ink-2 w-24 shrink-0">{STAGE_LABEL[stage]}</span>
                    <div className="flex-1 h-1.5 bg-[#212140] overflow-hidden">
                      <div
                        className={`h-full ${stage === 'WON' ? 'bg-[#39d353]' : 'bg-[#FF6B00]'}`}
                        style={{ width: `${Math.round((count / total) * 100)}%` }}
                      />
                    </div>
                    <span className="f-mono text-[11px] font-bold w-6 text-right">{count}</span>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="py-6 text-center f-mono text-[10px] uppercase text-ink-3">Sense dades</div>
          )}
        </div>

        {/* Recursos del servidor */}
        <div className="amg-card card-clip p-4 sm:p-5">
          <div className="flex items-center justify-between mb-4">
            <div>
              <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3">Servidor</div>
              <div className="f-display font-bold text-sm mt-0.5">Recursos</div>
            </div>
            <a href="/portal/admin/infraops" className="f-mono text-[10px] uppercase text-accent-light hover:underline">DETALL →</a>
          </div>
          {infra ? (
            <div className="space-y-4">
              <Gauge label="CPU" pct={infra.cpu.percent} />
              <Gauge label="RAM" pct={infra.ram.percent} />
              <Gauge label="Disc" pct={infra.disk.percent} />
              <Gauge label="Connexions DB" pct={infra.database.percent} />
              {infra.tenants && (
                <div className="pt-2 border-t border-border-subtle flex justify-between">
                  <span className="f-mono text-[10px] uppercase text-ink-3">Tenants n8n</span>
                  <span className="f-mono text-[10px] font-bold">{infra.tenants.active}</span>
                </div>
              )}
            </div>
          ) : (
            <div className="py-6 text-center f-mono text-[10px] uppercase text-ink-3">Sense dades de servidor</div>
          )}
        </div>
      </div>

      {/* Accions ràpides */}
      <div className="amg-card card-clip p-4 sm:p-5">
        <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3 mb-3">Accions ràpides</div>
        <div className="flex flex-wrap gap-2">
          <a href="/portal/process">
            <AMGButton size="sm" icon={I.Flow}>PROCÉS COMPLET</AMGButton>
          </a>
          <a href="/portal/leads/new">
            <AMGButton size="sm" variant="outline" icon={I.Plus}>NOU LEAD</AMGButton>
          </a>
          <a href="/portal/admin/tenants">
            <AMGButton size="sm" variant="outline" icon={I.Building}>TENANTS</AMGButton>
          </a>
          <a href="/portal/admin/config">
            <AMGButton size="sm" variant="outline" icon={I.Key}>API KEYS</AMGButton>
          </a>
          <a href="/portal/admin/backup">
            <AMGButton size="sm" variant="outline" icon={I.Database}>BACKUP</AMGButton>
          </a>
        </div>
      </div>

    </div>
  );
}

/* ─────────────────────────────────────────────
   CLIENT dashboard
───────────────────────────────────────────── */
interface ClientData {
  billing: BillingDashboard | null;
  invoices: Invoice[];
  landings: LandingSummary[];
  workflows: WorkflowSummary[];
}

function ClientDashboard({ data, loading, userName, onboardingSkipped, onboardingComplete, onSkip, onComplete }: {
  data: ClientData; loading: boolean; userName: string;
  onboardingSkipped: boolean; onboardingComplete: boolean;
  onSkip: () => void; onComplete: () => void;
}) {
  const { billing, invoices, landings, workflows } = data;
  const activeLandings = landings.filter(l => l.status === 'PUBLISHED' || l.status === 'ACTIVE').length;
  const activeWorkflows = workflows.filter(w => w.status === 'ACTIVE').length;

  const pendingSteps = [
    ...(landings.length === 0 ? ['landing' as const] : []),
    ...(workflows.length === 0 ? ['automation' as const] : []),
    ...(invoices.length === 0 ? ['billing' as const] : []),
  ];
  const showOnboarding = !onboardingSkipped && !onboardingComplete && pendingSteps.length > 0;

  if (loading) {
    return (
      <div className="p-6 space-y-4">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="h-24 animate-pulse bg-[#212140] rounded" />
        ))}
      </div>
    );
  }

  if (showOnboarding) {
    return (
      <div className="p-4 sm:p-6">
        <OnboardingGuide
          userName={userName}
          landingsCount={landings.length}
          workflowsCount={workflows.length}
          invoicesCount={invoices.length}
          onSkip={onSkip}
          onComplete={onComplete}
        />
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6 space-y-6">

      {/* Hero billing */}
      <div className="amg-card card-clip p-4 sm:p-6 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-[3px] h-16 bg-[#FF6B00]" />
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div>
            <span className="f-mono text-[10px] uppercase tracking-widest text-ink-3">Total invertit</span>
            <div className="f-display font-black text-2xl mt-1">{formatEur(billing?.totalSpent ?? 0)}</div>
            <AMGBadge tone={billing && billing.pendingBudgets > 0 ? 'warning' : 'success'} className="mt-2">
              {billing && billing.pendingBudgets > 0 ? `${billing.pendingBudgets} pendents` : 'AL DIA'}
            </AMGBadge>
          </div>
          <div>
            <div className="f-mono text-[10px] uppercase text-ink-3">Últim pressupost</div>
            <div className="f-display font-bold text-lg mt-1">{billing?.lastBudget?.budgetNumber ?? 'Cap'}</div>
            <div className="f-mono text-[10px] text-ink-2 mt-0.5">{formatDate(billing?.lastBudget?.sentAt ?? null)}</div>
          </div>
          <div>
            <div className="f-mono text-[10px] uppercase text-ink-3">Import</div>
            <div className="f-display font-bold text-lg mt-1 text-accent-light">{billing?.lastBudget ? formatEur(billing.lastBudget.total) : '€0'}</div>
            <div className="f-mono text-[10px] text-ink-2 mt-0.5">{billing?.lastBudget?.status ?? '—'}</div>
          </div>
          <div className="grid grid-cols-2 gap-2 content-start">
            <KpiCard label="Webs" value={activeLandings} sub={`${landings.length} total`} />
            <KpiCard label="Workflows" value={activeWorkflows} sub={`${workflows.length} total`} />
          </div>
        </div>
      </div>

      {/* Últimes factures */}
      <div className="amg-card card-clip p-4 sm:p-5">
        <div className="flex items-center justify-between mb-4">
          <div>
            <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3">Historial</div>
            <div className="f-display font-bold text-sm mt-0.5">Últimes factures</div>
          </div>
          <a href="/portal/finops" className="f-mono text-[10px] uppercase text-accent-light hover:underline">VEURE TOTES →</a>
        </div>
        {invoices.length === 0 ? (
          <div className="py-8 text-center">
            <I.Receipt size={24} className="mx-auto mb-2 opacity-30" />
            <p className="f-mono text-[10px] uppercase text-ink-3">Cap factura encara</p>
          </div>
        ) : (
          <div className="space-y-0">
            {invoices.slice(0, 5).map(inv => (
              <div key={inv.id}
                className="grid grid-cols-[1fr_80px_80px_20px] gap-3 h-10 items-center border-b border-[rgba(226,232,240,0.04)] last:border-0">
                <span className="f-mono text-[11px] text-ink-1">{formatDate(inv.createdAt)}</span>
                <span className="f-mono text-[11px] font-bold">{formatEur(inv.amount)}</span>
                <AMGBadge tone={BADGE_TONE[inv.status] || 'accent'}>{inv.status}</AMGBadge>
                <a href={inv.invoicePdfUrl || '#'} target="_blank" rel="noopener"
                  className={`text-ink-2 hover:text-accent-light ${!inv.invoicePdfUrl ? 'opacity-30 pointer-events-none' : ''}`}>
                  <I.Download size={12} />
                </a>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Ajuda */}
      <div className="amg-card card-clip p-4 sm:p-5 flex items-start gap-4">
        <I.Sparkles size={20} className="text-accent shrink-0 mt-0.5" />
        <div className="flex-1">
          <div className="f-display font-bold text-sm">NECESSITES AJUDA?</div>
          <p className="text-ui text-ink-1 mt-1 text-sm">El teu tècnic assignat està disponible per respondre els teus dubtes.</p>
        </div>
        <AMGButton size="sm" icon={I.Mail}>CONTACTE</AMGButton>
      </div>

    </div>
  );
}

/* ─────────────────────────────────────────────
   Page
───────────────────────────────────────────── */
export default function PortalPage() {
  const { user, isSuperAdmin, isAdmin } = useAuth();
  const handleApiError = useApiErrorHandler();
  const isStaff = isSuperAdmin || isAdmin;

  const [loading, setLoading] = useState(true);

  // Admin data
  const [adminData, setAdminData] = useState<AdminData>({
    totalTenants: 0, leads: null, ops: null, infra: null,
  });

  // Client data
  const [clientData, setClientData] = useState<ClientData>({
    billing: null, invoices: [], landings: [], workflows: [],
  });

  const [onboardingSkipped, setOnboardingSkipped] = useState(false);
  const [onboardingComplete, setOnboardingComplete] = useState(false);

  useEffect(() => {
    if (user?.tenantId) {
      setOnboardingSkipped(localStorage.getItem(`amg_onboarding_skipped_${user.tenantId}`) === 'true');
    }
  }, [user?.tenantId]);

  const loadDashboard = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    try {
      if (isStaff) {
        const [tenants, leads, ops, infra] = await Promise.all([
          listTenants({ size: 1 }).catch(() => ({ totalElements: 0 })),
          getLeadStats().catch(() => null),
          getOpsDashboard().catch(() => null),
          getInfraStatus().catch(() => null),
        ]);
        setAdminData({
          totalTenants: (tenants as { totalElements: number }).totalElements,
          leads,
          ops,
          infra,
        });
      } else {
        const tid = user.tenantId;
        const [bill, inv, lnd, wf] = await Promise.all([
          tid ? fetchBillingDashboard(tid).catch(() => null) : Promise.resolve(null),
          fetchInvoices().catch(() => [] as Invoice[]),
          tid ? fetchLandings(tid).catch(() => [] as LandingSummary[]) : Promise.resolve([]),
          tid ? fetchWorkflows(tid).catch(() => [] as WorkflowSummary[]) : Promise.resolve([]),
        ]);
        setClientData({ billing: bill, invoices: inv, landings: lnd, workflows: wf });
      }
    } catch (err: unknown) {
      handleApiError(err, 'Dashboard');
    } finally {
      setLoading(false);
    }
  }, [user, isStaff, handleApiError]);

  useEffect(() => { loadDashboard(); }, [loadDashboard]);

  const handleSkipOnboarding = () => {
    if (user?.tenantId) localStorage.setItem(`amg_onboarding_skipped_${user.tenantId}`, 'true');
    setOnboardingSkipped(true);
  };

  if (!user) return null;

  const firstName = user.name?.split(' ')[0] || 'usuari';

  return (
    <PortalShell breadcrumb="dashboard">
      {/* Topbar greeting */}
      <div className="h-10 flex items-center px-4 sm:px-6 border-b border-border-subtle">
        <span className="f-mono text-[10px] uppercase text-ink-3">
          Bon dia, {firstName}
        </span>
      </div>

      {isStaff ? (
        <AdminDashboard data={adminData} loading={loading} />
      ) : (
        <ClientDashboard
          data={clientData}
          loading={loading}
          userName={firstName}
          onboardingSkipped={onboardingSkipped}
          onboardingComplete={onboardingComplete}
          onSkip={handleSkipOnboarding}
          onComplete={() => setOnboardingComplete(true)}
        />
      )}
    </PortalShell>
  );
}
