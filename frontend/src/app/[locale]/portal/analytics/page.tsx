'use client';

import { useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useTenantFeatures } from '@/lib/tenant-features';
import { PortalShell } from '@/components/portal/PortalShell';
import { PhaseGuard } from '@/components/ui/PhaseGuard';
import { getAnalytics } from '@/services/analytics';
import { getTenant } from '@/services/admin';

const STAGE_LABELS: Record<string, string> = {
  NEW: 'Nous', CONTACTED: 'Contactats', QUALIFIED: 'Qualificats',
  PROPOSAL: 'Proposta', NEGOTIATION: 'Negociació', WON: 'Guanyats', LOST: 'Perduts',
};

const CHANNEL_LABELS: Record<string, string> = {
  WHATSAPP: 'WhatsApp', WHATSAPP_META: 'WhatsApp Meta',
  EMAIL: 'Email', WIDGET: 'Chat Widget', TELEGRAM: 'Telegram',
};

const SOURCE_LABELS: Record<string, string> = {
  LANDING_FORM: 'Formulari web', WHATSAPP: 'WhatsApp', EMAIL: 'Email',
  CHAT_WIDGET: 'Chat Widget', MANUAL: 'Manual', OTHER: 'Altres',
  GOOGLE: 'Google', REFERRAL: 'Referència',
};

function KpiCard({ label, value, sub }: { label: string; value: string | number; sub?: string }) {
  return (
    <div className="bg-surface-overlay border border-border-base rounded-md p-4">
      <div className="f-mono text-[10px] uppercase tracking-widest text-ink-3 mb-1">{label}</div>
      <div className="f-display font-bold text-2xl text-accent-light">{value}</div>
      {sub && <div className="text-xs text-ink-3 mt-0.5">{sub}</div>}
    </div>
  );
}

function BarRow({ label, value, max }: { label: string; value: number; max: number }) {
  const pct = max > 0 ? Math.round((value / max) * 100) : 0;
  return (
    <div className="flex items-center gap-3">
      <span className="text-xs text-ink-2 w-32 shrink-0 truncate">{label}</span>
      <div className="flex-1 bg-surface-base rounded-full h-2">
        <div className="bg-accent-light h-2 rounded-full transition-all" style={{ width: `${pct}%` }} />
      </div>
      <span className="f-mono text-xs text-ink-1 w-8 text-right">{value}</span>
    </div>
  );
}

export default function AnalyticsPage() {
  const params  = useParams<{ locale: string }>();
  const { user } = useAuth();
  const tenantId = (user as any)?.tenantId as string | undefined;
  const features = useTenantFeatures();

  const { data: tenant } = useQuery({
    queryKey: ['tenant', tenantId],
    queryFn: () => getTenant(tenantId!),
    enabled: !!tenantId,
  });

  const { data: analytics, isLoading } = useQuery({
    queryKey: ['analytics', tenantId],
    queryFn: () => getAnalytics(tenantId!),
    enabled: !!tenantId,
  });

  const locale = params.locale;

  if (!tenantId) {
    return (
      <PortalShell breadcrumb="analítica" backHref={`/${locale}/portal`}>
        <div className="p-8 text-sm text-ink-2">No tens un tenant associat.</div>
      </PortalShell>
    );
  }

  const stageEntries   = Object.entries(analytics?.leadsByStage ?? {}).sort((a, b) => b[1] - a[1]);
  const channelEntries = Object.entries(analytics?.conversationsByChannel ?? {}).sort((a, b) => b[1] - a[1]);
  const sourceEntries  = Object.entries(analytics?.leadsBySource ?? {}).sort((a, b) => b[1] - a[1]);
  const maxStage   = Math.max(...stageEntries.map(e => e[1]), 1);
  const maxChannel = Math.max(...channelEntries.map(e => e[1]), 1);
  const maxSource  = Math.max(...sourceEntries.map(e => e[1]), 1);

  return (
    <PortalShell breadcrumb={`analítica · ${tenant?.name ?? ''}`} backHref={`/${locale}/portal`}>
      <PhaseGuard
        allowed={features.canAccessAnalytics}
        loading={features.isLoading}
        phaseName="F4"
        phaseLabel="Seguiment"
        description="Accedeix a mètriques de leads, converses, pressupostos i informes automàtics."
      >
      <div className="p-4 sm:p-8 space-y-6 max-w-5xl">

        <div>
          <h1 className="text-lg font-semibold text-ink-0">Analítica</h1>
          <p className="text-sm text-ink-2 mt-0.5">{tenant?.name}</p>
        </div>

        {isLoading ? (
          <div className="flex items-center gap-2 text-sm text-ink-3">
            <span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
            Carregant dades...
          </div>
        ) : !analytics ? (
          <div className="text-sm text-ink-3">No hi ha dades disponibles.</div>
        ) : (
          <>
            {/* KPIs */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <KpiCard label="Leads totals"     value={analytics.totalLeads} />
              <KpiCard label="Leads (7 dies)"   value={analytics.newLeads7d} sub={`${analytics.newLeads30d} el darrer mes`} />
              <KpiCard label="Taxa conversió"   value={`${analytics.conversionRate.toFixed(1)}%`} sub={`${analytics.wonLeads} guanyats`} />
              <KpiCard label="Converses"        value={analytics.totalConversations} sub={`${analytics.newConversations7d} noves (7d)`} />
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <KpiCard label="Pressupostos"     value={analytics.totalBudgets} />
              <KpiCard label="Enviats"          value={analytics.sentBudgets} />
              <KpiCard label="Acceptats"        value={analytics.acceptedBudgets} sub={`${analytics.budgetConversionRate.toFixed(1)}%`} />
              <KpiCard label="Pend. aprovació"  value={analytics.pendingApproval} />
            </div>

            {/* Distribucions */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              {stageEntries.length > 0 && (
                <div className="bg-surface-overlay border border-border-base rounded-md p-4 space-y-3">
                  <div className="f-mono text-[10px] uppercase tracking-widest text-ink-3">Leads per etapa</div>
                  {stageEntries.map(([k, v]) => (
                    <BarRow key={k} label={STAGE_LABELS[k] ?? k} value={v} max={maxStage} />
                  ))}
                </div>
              )}

              {channelEntries.length > 0 && (
                <div className="bg-surface-overlay border border-border-base rounded-md p-4 space-y-3">
                  <div className="f-mono text-[10px] uppercase tracking-widest text-ink-3">Converses per canal</div>
                  {channelEntries.map(([k, v]) => (
                    <BarRow key={k} label={CHANNEL_LABELS[k] ?? k} value={v} max={maxChannel} />
                  ))}
                </div>
              )}

              {sourceEntries.length > 0 && (
                <div className="bg-surface-overlay border border-border-base rounded-md p-4 space-y-3">
                  <div className="f-mono text-[10px] uppercase tracking-widest text-ink-3">Leads per origen</div>
                  {sourceEntries.map(([k, v]) => (
                    <BarRow key={k} label={SOURCE_LABELS[k] ?? k} value={v} max={maxSource} />
                  ))}
                </div>
              )}
            </div>

            {/* Resums */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="bg-surface-overlay border border-border-base rounded-md p-4">
                <div className="f-mono text-[10px] uppercase tracking-widest text-ink-3 mb-2">Resum diari</div>
                <pre className="text-xs text-ink-1 whitespace-pre-wrap font-mono">{analytics.dailyReport}</pre>
              </div>
              <div className="bg-surface-overlay border border-border-base rounded-md p-4">
                <div className="f-mono text-[10px] uppercase tracking-widest text-ink-3 mb-2">Resum setmanal</div>
                <pre className="text-xs text-ink-1 whitespace-pre-wrap font-mono">{analytics.weeklyReport}</pre>
              </div>
            </div>
          </>
        )}
      </div>
      </PhaseGuard>
    </PortalShell>
  );
}
