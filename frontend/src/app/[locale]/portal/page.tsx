'use client';

import { useState, useEffect, useCallback } from 'react';
import { useTranslations } from 'next-intl';
import { Link } from '@/i18n/navigation';
import { useAuth } from '@/lib/auth-context';
import { useApiErrorHandler } from '@/lib/use-api-error';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { IconSet } from '@/components/ui/icons';
import {
  fetchBillingDashboard, fetchInvoices, fetchLandings, fetchWorkflows,
  type BillingDashboard, type Invoice, type LandingSummary, type WorkflowSummary,
} from '@/services/dashboard';
import { listTenants, getTenantSetup, toggleTenantService, type TenantSetup } from '@/services/admin';
import { getLeadStats, type LeadStats } from '@/services/leads';
import { getOpsDashboard, type OpsDashboard } from '@/services/ops';
import { getInfraStatus, type InfraStatus } from '@/services/infraops';
import { getPaymentDashboard, type PaymentDashboard } from '@/services/payments';
import { getGlobalFinOpsDashboard, type FinOpsGlobalDashboard } from '@/services/finops';
import { getGlobalChannelUsageStats, type ChannelUsageStats } from '@/services/agents-conversational';
import { OnboardingGuide } from '@/components/portal/OnboardingGuide';
import { GuidesSection } from '@/components/guides/GuidesSection';
import { ServiceDeliveryAlert } from '@/components/guides/ServiceDeliveryAlert';

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
  if (href) return <Link href={href}>{content}</Link>;
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
  payments: PaymentDashboard | null;
  channelStats: ChannelUsageStats | null;
  finops: FinOpsGlobalDashboard | null;
}

function AdminDashboard({ data, loading, isSuperAdmin }: { data: AdminData; loading: boolean; isSuperAdmin: boolean }) {
  const { leads, ops, infra, totalTenants, payments, channelStats, finops } = data;

  const sysOk = ops ? ops.currentStatus.up === ops.currentStatus.services && ops.openIncidents === 0 : null;
  const infraTone = infra
    ? (infra.cpu.percent >= 90 || infra.ram.percent >= 90 || infra.disk.percent >= 90 ? 'crit'
      : infra.cpu.percent >= 75 || infra.ram.percent >= 75 || infra.disk.percent >= 75 ? 'warn' : 'ok')
    : 'default';

  const t = useTranslations('portalDashboard');
  const STAGES = ['NEW', 'CONTACTED', 'QUALIFIED', 'PROPOSAL', 'NEGOTIATION', 'NURTURING', 'WON'];

  if (loading) {
    return (
      <div className="p-6 space-y-4">
        {[...Array(3)].map((_, idx) => (
          <div key={idx} className="h-24 animate-pulse bg-[#212140] rounded" />
        ))}
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6 space-y-6">

      {/* Alerta factures vençudes */}
      {finops && finops.totalOverdue > 0 && (
        <div className="rounded border border-red-500/40 bg-red-500/10 p-4 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <IconSet.AlertCircle size={18} stroke="#f85149" />
            <div>
              <span className="f-display font-bold text-sm text-red-400">
                {finops.overdueCount} factura{finops.overdueCount !== 1 ? 'es' : ''} vençuda{finops.overdueCount !== 1 ? 'es' : ''}
              </span>
              <span className="f-mono text-xs text-ink-2 ml-2">
                · {new Intl.NumberFormat('ca-ES', { style: 'currency', currency: 'EUR' }).format(finops.totalOverdue)} sense cobrar
              </span>
            </div>
          </div>
          <Link href="/portal/finops?status=OVERDUE">
            <AMGButton size="sm" variant="outline">Veure factures →</AMGButton>
          </Link>
        </div>
      )}

      {/* KPIs principals */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        {isSuperAdmin && (
          <KpiCard
            label={t('admin.kpi.tenantsActive')}
            value={totalTenants}
            sub={t('admin.kpi.clients')}
            icon={IconSet.Building}
            href="/portal/admin/tenants"
          />
        )}
        <KpiCard
          label={t('admin.kpi.leadsActive')}
          value={leads ? (leads.total - (leads.byStage?.LOST ?? 0) - (leads.byStage?.WON ?? 0)) : '—'}
          sub={leads ? t('admin.kpi.convRateSub', { total: leads.total, rate: Math.round(leads.conversionRate ?? 0) }) : ''}
          icon={IconSet.Users}
          href="/portal/leads"
        />
        <KpiCard
          label={t('admin.kpi.servicesMonitored')}
          value={ops ? `${ops.currentStatus.up}/${ops.currentStatus.services}` : '—'}
          sub={ops ? (ops.openIncidents > 0 ? t('admin.kpi.incidents', { count: ops.openIncidents }) : t('admin.kpi.noIncidents')) : ''}
          tone={sysOk === true ? 'ok' : sysOk === false ? 'crit' : 'default'}
          icon={IconSet.Activity}
          href="/portal/ops"
        />
        {isSuperAdmin ? (
          <KpiCard
            label={t('admin.kpi.infra')}
            value={infra ? `${infra.cpu.percent}% CPU` : '—'}
            sub={infra ? `RAM ${infra.ram.percent}% · Disk ${infra.disk.percent}%` : ''}
            tone={infraTone}
            icon={IconSet.Server}
            href="/portal/admin/infraops"
          />
        ) : (
          <KpiCard
            label={t('admin.kpi.paymentsPending')}
            value={payments ? payments.pendingCount : '—'}
            sub={payments ? t('admin.kpi.completed', { count: payments.completedCount }) : ''}
            icon={IconSet.CreditCard}
          />
        )}
      </div>

      {/* Leads pipeline + Infra (SUPER_ADMIN only per la columna dreta) */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">

        {/* Pipeline de leads */}
        <div className="amg-card card-clip p-4 sm:p-5">
          <div className="flex items-center justify-between mb-4">
            <div>
              <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3">{t('admin.pipeline.section')}</div>
              <div className="f-display font-bold text-sm mt-0.5">{t('admin.pipeline.title')}</div>
            </div>
            <Link href="/portal/leads" className="f-mono text-[10px] uppercase text-accent-light hover:underline">{t('seeAll')}</Link>
          </div>
          {leads ? (
            <div className="space-y-2">
              {STAGES.map(stage => {
                const count = leads.byStage?.[stage] ?? 0;
                const total = leads.total || 1;
                return (
                  <div key={stage} className="flex items-center gap-3">
                    <span className="f-mono text-[10px] uppercase text-ink-2 w-24 shrink-0">{t((`admin.pipeline.stages.${stage}`) as any)}</span>
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
            <div className="py-6 text-center f-mono text-[10px] uppercase text-ink-3">{t('noData')}</div>
          )}
        </div>

        {/* Recursos del servidor — SUPER_ADMIN only */}
        {isSuperAdmin ? (
          <div className="amg-card card-clip p-4 sm:p-5">
            <div className="flex items-center justify-between mb-4">
              <div>
                <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3">{t('admin.resources.section')}</div>
                <div className="f-display font-bold text-sm mt-0.5">{t('admin.resources.title')}</div>
              </div>
              <Link href="/portal/admin/infraops" className="f-mono text-[10px] uppercase text-accent-light hover:underline">{t('detail')}</Link>
            </div>
            {infra ? (
              <div className="space-y-4">
                <Gauge label="CPU" pct={infra.cpu.percent} />
                <Gauge label="RAM" pct={infra.ram.percent} />
                <Gauge label={t('admin.resources.disc')} pct={infra.disk.percent} />
                <Gauge label={t('admin.resources.dbConnections')} pct={infra.database.percent} />
                {infra.tenants && (
                  <div className="pt-2 border-t border-border-subtle flex justify-between">
                    <span className="f-mono text-[10px] uppercase text-ink-3">{t('admin.resources.n8nTenants')}</span>
                    <span className="f-mono text-[10px] font-bold">{infra.tenants.active}</span>
                  </div>
                )}
              </div>
            ) : (
              <div className="py-6 text-center f-mono text-[10px] uppercase text-ink-3">{t('admin.resources.noData')}</div>
            )}
          </div>
        ) : (
          <div className="amg-card card-clip p-4 sm:p-5">
            <div className="flex items-center justify-between mb-4">
              <div>
                <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3">{t('admin.payments.section')}</div>
                <div className="f-display font-bold text-sm mt-0.5">{t('admin.payments.title')}</div>
              </div>
              <Link href="/portal/payments" className="f-mono text-[10px] uppercase text-accent-light hover:underline">{t('seeAll')}</Link>
            </div>
            {payments ? (
              <div className="space-y-3">
                <div className="grid grid-cols-2 gap-3">
                  <div className="bg-bg-1 border border-border-base p-3">
                    <div className="f-mono text-[9px] uppercase text-ink-3">{t('admin.payments.pending')}</div>
                    <div className={`f-display font-bold text-2xl mt-1 ${payments.pendingCount > 0 ? 'text-warning' : 'text-ink-0'}`}>
                      {payments.pendingCount}
                    </div>
                  </div>
                  <div className="bg-bg-1 border border-border-base p-3">
                    <div className="f-mono text-[9px] uppercase text-ink-3">{t('admin.payments.completed')}</div>
                    <div className="f-display font-bold text-2xl mt-1 text-success">{payments.completedCount}</div>
                  </div>
                </div>
              </div>
            ) : (
              <div className="py-6 text-center f-mono text-[10px] uppercase text-ink-3">{t('noData')}</div>
            )}
          </div>
        )}
      </div>

      {/* Activitat de canals — últims 30 dies (SUPER_ADMIN only) */}
      {isSuperAdmin && (
        <div className="amg-card card-clip p-4 sm:p-5">
          <div className="flex items-center justify-between mb-4">
            <div>
              <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3">{t('admin.channels.period')}</div>
              <div className="f-display font-bold text-sm mt-0.5">{t('admin.channels.title')}</div>
            </div>
            <Link href="/portal/admin/tenants?tab=canals" className="f-mono text-[10px] uppercase text-accent-light hover:underline">{t('admin.channels.byTenant')}</Link>
          </div>
          {channelStats ? (
            <div className="grid grid-cols-3 sm:grid-cols-6 gap-2">
              {[
                { label: 'WA Twilio', value: channelStats.whatsappMessages,     icon: '📱' },
                { label: 'WA Meta',   value: channelStats.whatsappMetaMessages,  icon: '💚' },
                { label: 'Telegram',  value: channelStats.telegramMessages,      icon: '✈️' },
                { label: 'Email',     value: channelStats.emailMessages,         icon: '✉️' },
                { label: 'Xat web',   value: channelStats.chatMessages,          icon: '💬' },
                { label: t('admin.channels.tokensAI'), value: channelStats.aiTokens.toLocaleString('ca-ES'), icon: '🤖' },
              ].map(({ label, value, icon }) => (
                <div key={label} className="bg-[#0d0d1a] border border-border-base rounded p-2.5 text-center">
                  <div className="text-sm mb-1">{icon}</div>
                  <div className="f-mono text-base font-bold text-ink-0">{value}</div>
                  <div className="f-mono text-[8px] uppercase tracking-wider text-ink-3 mt-0.5">{label}</div>
                </div>
              ))}
            </div>
          ) : (
            <div className="py-4 text-center f-mono text-[10px] uppercase text-ink-3">{t('admin.channels.noData')}</div>
          )}
        </div>
      )}

      {/* Accions ràpides */}
      <div className="amg-card card-clip p-4 sm:p-5">
        <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3 mb-3">{t('admin.actions.title')}</div>
        <div className="flex flex-wrap gap-2">
          <Link href="/portal/process">
            <AMGButton size="sm" icon={IconSet.Flow}>{t('admin.actions.process')}</AMGButton>
          </Link>
          <Link href="/portal/leads/new">
            <AMGButton size="sm" variant="outline" icon={IconSet.Plus}>{t('admin.actions.newLead')}</AMGButton>
          </Link>
          {isSuperAdmin && (
            <>
              <Link href="/portal/admin/tenants">
                <AMGButton size="sm" variant="outline" icon={IconSet.Building}>{t('admin.actions.tenants')}</AMGButton>
              </Link>
              <Link href="/portal/admin/config">
                <AMGButton size="sm" variant="outline" icon={IconSet.Key}>{t('admin.actions.apiKeys')}</AMGButton>
              </Link>
              <Link href="/portal/admin/backup">
                <AMGButton size="sm" variant="outline" icon={IconSet.Database}>{t('admin.actions.backup')}</AMGButton>
              </Link>
            </>
          )}
        </div>
      </div>

    </div>
  );
}

/* ─────────────────────────────────────────────
   Client services toggle section
───────────────────────────────────────────── */
function ClientServicesCard({ tenantId }: { tenantId: string }) {
  const t = useTranslations('portalDashboard');
  const [setup, setSetup] = useState<TenantSetup | null>(null);
  const [toggling, setToggling] = useState<string | null>(null);

  useEffect(() => {
    getTenantSetup(tenantId).then(setSetup).catch(() => {});
  }, [tenantId]);

  const allServices = setup?.profiles.flatMap(p =>
    p.phases.flatMap(ph => ph.services)
  ) ?? [];

  if (allServices.length === 0) return null;

  const handleToggle = async (serviceId: string, tenantServiceId: string) => {
    setToggling(serviceId);
    try {
      const { isEnabled } = await toggleTenantService(tenantId, serviceId);
      setSetup(prev => prev ? {
        ...prev,
        profiles: prev.profiles.map(p => ({
          ...p,
          phases: p.phases.map(ph => ({
            ...ph,
            services: ph.services.map(svc =>
              svc.service.id === serviceId ? { ...svc, isEnabled } : svc
            ),
          })),
        })),
      } : prev);
    } finally {
      setToggling(null);
    }
  };

  return (
    <div className="amg-card card-clip p-4 sm:p-5">
      <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3 mb-4">{t('client.myServices.title')}</div>
      <div className="space-y-2">
        {allServices.map((svc) => {
          const isLoading = toggling === svc.service.id;
          return (
            <div key={svc.service.id}
              className="flex items-center justify-between gap-3 py-2 border-b border-[rgba(226,232,240,0.04)] last:border-0">
              <div className="flex items-center gap-2 min-w-0">
                <span className={`w-1.5 h-1.5 rounded-full flex-shrink-0 ${svc.isEnabled ? 'bg-[#39d353]' : 'bg-ink-3'}`} />
                <span className="text-sm text-ink-1 truncate">{svc.service.name}</span>
              </div>
              <button
                type="button"
                disabled={isLoading}
                onClick={() => handleToggle(svc.service.id, svc.tenantServiceId)}
                className={`flex-shrink-0 w-10 h-5 rounded-full transition-colors relative ${
                  svc.isEnabled ? 'bg-[#FF6B00]' : 'bg-[rgba(255,255,255,0.12)]'
                } ${isLoading ? 'opacity-50 cursor-not-allowed' : ''}`}
              >
                <div className={`absolute top-0.5 w-4 h-4 rounded-full bg-white shadow transition-all ${svc.isEnabled ? 'left-5' : 'left-0.5'}`} />
              </button>
            </div>
          );
        })}
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
  tenantSetup: TenantSetup | null;
}

function ClientDashboard({ data, loading, userName, tenantId, onboardingSkipped, onboardingComplete, loadError, onSkip, onComplete }: {
  data: ClientData; loading: boolean; userName: string; tenantId: string | null;
  onboardingSkipped: boolean; onboardingComplete: boolean; loadError: string | null;
  onSkip: () => void; onComplete: () => void;
}) {
  const t = useTranslations('portalDashboard');
  const ot = useTranslations('onboarding');
  const { billing, invoices, landings, workflows, tenantSetup } = data;
  const activeLandings = landings.filter(l => l.status === 'PUBLISHED' || l.status === 'ACTIVE').length;
  const activeWorkflows = workflows.filter(w => w.status === 'ACTIVE').length;

  // Assigned services from Vault
  const assignedServices = (tenantSetup?.profiles ?? [])
    .flatMap(p => p.phases)
    .flatMap(ph => ph.services)
    .map(s => ({ type: s.service.type, isAddon: (s as any).isAddon ?? false }));
  const assignedServiceTypes = new Set(assignedServices.map(s => s.type));
  // Vault pending: any phase not yet APPROVED
  const vaultPending = (tenantSetup?.profiles ?? [])
    .flatMap(p => p.phases)
    .some(ph => ph.approvalStatus !== 'APPROVED');

  // Only show onboarding if Vault has relevant assigned services and at least one is pending
  const hasOnboardingServices = assignedServiceTypes.has('LANDING') || assignedServiceTypes.has('AUTOMATION') || assignedServiceTypes.has('BILLING');
  const hasPendingStep =
    (assignedServiceTypes.has('LANDING') && landings.length === 0) ||
    (assignedServiceTypes.has('AUTOMATION') && workflows.length === 0) ||
    (assignedServiceTypes.has('BILLING') && invoices.length === 0);

  // Fallback: if vault data unavailable, use simple check (all 3 resources empty)
  const fallbackPending = !tenantSetup && landings.length === 0 && workflows.length === 0 && invoices.length === 0;
  const showOnboarding = !onboardingSkipped && !onboardingComplete && !loading && !loadError &&
    (hasOnboardingServices ? hasPendingStep : fallbackPending);

  if (loading) {
    return (
      <div className="p-6 space-y-4">
        {[...Array(3)].map((_, idx) => (
          <div key={idx} className="h-24 animate-pulse bg-[#212140] rounded" />
        ))}
      </div>
    );
  }

  if (showOnboarding) {
    return (
      <div className="p-4 sm:p-6">
        <OnboardingGuide
          tenantId={tenantId ?? ''}
          userName={userName}
          assignedServices={assignedServices}
          landings={landings}
          workflows={workflows}
          invoices={invoices}
          vaultPending={vaultPending}
          onSkip={onSkip}
          onComplete={onComplete}
        />
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6 space-y-6">

      {tenantId && <ServiceDeliveryAlert tenantId={tenantId} />}

      {tenantId && <GuidesSection tenantId={tenantId} />}

      {/* Alerta factures vençudes */}
      {(() => {
        const overdue = invoices.filter(i => i.status === 'OVERDUE');
        if (overdue.length === 0) return null;
        const totalOverdue = overdue.reduce((sum, i) => sum + i.amount, 0);
        return (
          <div className="rounded border border-red-500/40 bg-red-500/10 p-4 flex items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <IconSet.AlertCircle size={18} stroke="#f85149" />
              <div>
                <span className="f-display font-bold text-sm text-red-400">
                  Tens {overdue.length} factura{overdue.length !== 1 ? 'es' : ''} vençuda{overdue.length !== 1 ? 'es' : ''}
                </span>
                <span className="f-mono text-xs text-ink-2 ml-2">
                  · {new Intl.NumberFormat('ca-ES', { style: 'currency', currency: 'EUR' }).format(totalOverdue)} pendents de pagament
                </span>
              </div>
            </div>
            <Link href="/portal/finops">
              <AMGButton size="sm" variant="outline">Veure factures →</AMGButton>
            </Link>
          </div>
        );
      })()}

      {/* Hero billing */}
      <div className="amg-card card-clip p-4 sm:p-6 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-[3px] h-16 bg-[#FF6B00]" />
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div>
            <span className="f-mono text-[10px] uppercase tracking-widest text-ink-3">{t('client.billing.totalInvested')}</span>
            <div className="f-display font-black text-2xl mt-1">{formatEur(billing?.totalSpent ?? 0)}</div>
            <AMGBadge tone={billing && billing.pendingBudgets > 0 ? 'warning' : 'success'} className="mt-2">
              {billing && billing.pendingBudgets > 0 ? t('client.billing.pending', { count: billing.pendingBudgets }) : t('client.billing.upToDate')}
            </AMGBadge>
          </div>
          <div>
            <div className="f-mono text-[10px] uppercase text-ink-3">{t('client.billing.lastQuote')}</div>
            <div className="f-display font-bold text-lg mt-1">{billing?.lastBudget?.budgetNumber ?? t('client.billing.none')}</div>
            <div className="f-mono text-[10px] text-ink-2 mt-0.5">{formatDate(billing?.lastBudget?.sentAt ?? null)}</div>
          </div>
          <div>
            <div className="f-mono text-[10px] uppercase text-ink-3">{t('client.billing.amount')}</div>
            <div className="f-display font-bold text-lg mt-1 text-accent-light">{billing?.lastBudget ? formatEur(billing.lastBudget.total) : '€0'}</div>
            <div className="f-mono text-[10px] text-ink-2 mt-0.5">{billing?.lastBudget?.status ?? '—'}</div>
          </div>
          <div className="grid grid-cols-2 gap-2 content-start">
            <KpiCard label={t('client.kpi.webs')} value={activeLandings} sub={t('client.kpi.total', { count: landings.length })} />
            <KpiCard label="Workflows" value={activeWorkflows} sub={t('client.kpi.total', { count: workflows.length })} />
          </div>
        </div>
      </div>

      {/* Serveis contractats */}
      {tenantId && <ClientServicesCard tenantId={tenantId} />}

      {/* Últimes factures */}
      <div className="amg-card card-clip p-4 sm:p-5">
        <div className="flex items-center justify-between mb-4">
          <div>
            <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3">{t('client.invoices.section')}</div>
            <div className="f-display font-bold text-sm mt-0.5">{t('client.invoices.title')}</div>
          </div>
          <Link href="/portal/finops" className="f-mono text-[10px] uppercase text-accent-light hover:underline">{t('client.invoices.seeAll')}</Link>
        </div>
        {invoices.length === 0 ? (
          <div className="py-8 text-center">
            <IconSet.Receipt size={24} className="mx-auto mb-2 opacity-30" />
            <p className="f-mono text-[10px] uppercase text-ink-3">{t('client.invoices.empty')}</p>
          </div>
        ) : (
          <div className="space-y-0">
            {invoices.slice(0, 5).map(inv => (
              <div key={inv.id}
                className="grid grid-cols-[1fr_80px_80px_20px] gap-3 h-10 items-center border-b border-[rgba(226,232,240,0.04)] last:border-0">
                <span className="f-mono text-[11px] text-ink-1">{formatDate(inv.createdAt)}</span>
                <span className="f-mono text-[11px] font-bold">{formatEur(inv.amount)}</span>
                <AMGBadge tone={BADGE_TONE[inv.status] || 'accent'}>{t(`client.invoiceStatus.${inv.status}` as any) || inv.status}</AMGBadge>
                <a href={inv.invoicePdfUrl || '#'} target="_blank" rel="noopener"
                  className={`text-ink-2 hover:text-accent-light ${!inv.invoicePdfUrl ? 'opacity-30 pointer-events-none' : ''}`}>
                  <IconSet.Download size={12} />
                </a>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Ajuda */}
      <div className="amg-card card-clip p-4 sm:p-5 flex items-start gap-4">
        <IconSet.Sparkles size={20} className="text-accent shrink-0 mt-0.5" />
        <div className="flex-1">
          <div className="f-display font-bold text-sm">{t('client.help.title')}</div>
          <p className="text-ui text-ink-1 mt-1 text-sm">{t('client.help.subtitle')}</p>
        </div>
        <a href="mailto:info@amgdl.com">
          <AMGButton size="sm" icon={IconSet.Mail}>{t('client.help.contact')}</AMGButton>
        </a>
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
  const t = useTranslations('portalDashboard');
  const ot = useTranslations('onboarding');
  const isStaff = isSuperAdmin || isAdmin;

  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  // Admin data
  const [adminData, setAdminData] = useState<AdminData>({
    totalTenants: 0, leads: null, ops: null, infra: null, payments: null, channelStats: null, finops: null,
  });

  // Client data
  const [clientData, setClientData] = useState<ClientData>({
    billing: null, invoices: [], landings: [], workflows: [], tenantSetup: null,
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
    setLoadError(null);
    try {
      if (isStaff) {
        const [tenants, leads, ops, infra, payments, channelStats, finops] = await Promise.all([
          listTenants({ size: 1 }).catch(() => ({ totalElements: 0 })),
          getLeadStats().catch(() => null),
          getOpsDashboard().catch(() => null),
          isSuperAdmin ? getInfraStatus().catch(() => null) : Promise.resolve(null),
          !isSuperAdmin ? getPaymentDashboard().catch(() => null) : Promise.resolve(null),
          isSuperAdmin ? getGlobalChannelUsageStats().catch(() => null) : Promise.resolve(null),
          isSuperAdmin ? getGlobalFinOpsDashboard().catch(() => null) : Promise.resolve(null),
        ]);
        setAdminData({
          totalTenants: (tenants as { totalElements: number }).totalElements,
          leads,
          ops,
          infra,
          payments,
          channelStats,
          finops,
        });
      } else {
        const tid = user.tenantId;
        const [bill, inv, lnd, wf, setup] = await Promise.all([
          tid ? fetchBillingDashboard(tid).catch(() => null) : Promise.resolve(null),
          fetchInvoices().catch(() => [] as Invoice[]),
          tid ? fetchLandings(tid).catch(() => [] as LandingSummary[]) : Promise.resolve([]),
          tid ? fetchWorkflows(tid).catch(() => [] as WorkflowSummary[]) : Promise.resolve([]),
          tid ? getTenantSetup(tid).catch(() => null) : Promise.resolve(null),
        ]);
        setClientData({ billing: bill, invoices: inv, landings: lnd, workflows: wf, tenantSetup: setup });
      }
    } catch (err: unknown) {
      handleApiError(err, 'Dashboard');
      if (err instanceof Error) setLoadError(err.message);
      else setLoadError('Error loading dashboard');
    } finally {
      setLoading(false);
    }
  }, [user, isStaff, isSuperAdmin, handleApiError]);

  useEffect(() => { loadDashboard(); }, [loadDashboard]);

  /* Refresc automàtic cada 30s mentre l'onboarding client és actiu */
  useEffect(() => {
    if (isStaff || onboardingSkipped || onboardingComplete) return;
    const interval = setInterval(() => { loadDashboard(); }, 30_000);
    return () => clearInterval(interval);
  }, [isStaff, onboardingSkipped, onboardingComplete, loadDashboard]);

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
          {t('greeting', { name: firstName })}
        </span>
      </div>

      {loadError && !isStaff && !onboardingSkipped && !onboardingComplete && (
        <div className="p-4 sm:p-6">
          <div className="amg-card card-clip p-6 text-center border border-[#f85149]/40 bg-[#f85149]/5">
            <IconSet.AlertCircle size={24} className="mx-auto mb-3 text-[#f85149]" />
            <div className="f-display font-bold text-sm mb-1">{ot('error_title')}</div>
            <p className="text-ink-2 text-sm mb-4">{ot('error_desc')}</p>
            <button
              onClick={loadDashboard}
              className="f-mono text-label uppercase btn-clip bg-accent hover:bg-accent-light text-black font-semibold px-5 h-10 transition-colors"
            >
              {ot('error_retry')}
            </button>
          </div>
        </div>
      )}

      {isStaff ? (
        <AdminDashboard data={adminData} loading={loading} isSuperAdmin={isSuperAdmin} />
      ) : !loadError ? (
        <ClientDashboard
          data={clientData}
          loading={loading}
          userName={firstName}
          tenantId={user.tenantId ?? null}
          onboardingSkipped={onboardingSkipped}
          onboardingComplete={onboardingComplete}
          loadError={loadError}
          onSkip={handleSkipOnboarding}
          onComplete={() => setOnboardingComplete(true)}
        />
      ) : null}
    </PortalShell>
  );
}
