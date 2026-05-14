'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import {
  listBudgets, getBillingDashboard, sendBudget, cancelBudget,
  type BudgetResponse,
} from '@/services/billing';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGStat, AMGSectionTitle } from '@/components/ui/stat';
import { I } from '@/components/ui/icons';

const BADGE_TONE: Record<string, 'accent' | 'info' | 'success' | 'danger' | 'warning' | 'neutral'> = {
  DRAFT: 'accent', SENT: 'info', ACCEPTED: 'success',
  REJECTED: 'danger', CANCELLED: 'danger', EXPIRED: 'warning',
};

const LABEL: Record<string, string> = {
  DRAFT: 'Esborrany', SENT: 'Enviat', ACCEPTED: 'Acceptat',
  REJECTED: 'Rebutjat', CANCELLED: 'Cancel·lat', EXPIRED: 'Caducat',
};

function fmt(n: number) {
  return new Intl.NumberFormat('ca-ES', { style: 'currency', currency: 'EUR' }).format(n);
}

function fmtDate(d: string | null) {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

const STATUS_FILTERS = ['', 'DRAFT', 'SENT', 'ACCEPTED', 'REJECTED'];

export default function BillingPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();
  const [statusFilter, setStatusFilter] = useState('');

  const tenantId = user?.tenantId ?? '';

  const { data: dashboard } = useQuery({
    queryKey: ['billing-dashboard', tenantId],
    queryFn: () => getBillingDashboard(tenantId),
    enabled: !!tenantId,
  });

  const { data: budgets = [], isLoading } = useQuery({
    queryKey: ['budgets', tenantId],
    queryFn: () => listBudgets(tenantId),
    enabled: !!tenantId,
  });

  const { mutate: doSend, isPending: sending } = useMutation({
    mutationFn: (id: string) => sendBudget(id),
    onSuccess: () => {
      toast('success', 'Pressupost enviat');
      qc.invalidateQueries({ queryKey: ['budgets'] });
      qc.invalidateQueries({ queryKey: ['billing-dashboard'] });
    },
    onError: () => toast('error', 'Error enviant el pressupost'),
  });

  const { mutate: doCancel } = useMutation({
    mutationFn: (id: string) => cancelBudget(id),
    onSuccess: () => {
      toast('success', 'Pressupost cancel·lat');
      qc.invalidateQueries({ queryKey: ['budgets'] });
      qc.invalidateQueries({ queryKey: ['billing-dashboard'] });
    },
    onError: () => toast('error', 'Error cancel·lant el pressupost'),
  });

  const filtered = statusFilter
    ? budgets.filter((b: BudgetResponse) => b.status === statusFilter)
    : budgets;

  return (
    <PortalShell breadcrumb="billing">
      <div className="p-4 sm:p-8 space-y-6">
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / billing /</span>
          <div className="f-display font-bold text-xl mt-1">Pressupostos i facturació</div>
        </div>

        {dashboard && (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
            <AMGStat label="Total gastat" value={fmt(dashboard.totalSpent)} icon={I.CreditCard} tone="accent" />
            <AMGStat
              label="Pendents aprovació"
              value={String(dashboard.pendingBudgets)}
              icon={I.Clock}
              tone={dashboard.pendingBudgets > 0 ? 'danger' : 'success'}
            />
            {dashboard.recentPhases.slice(0, 2).map((ph, i) => (
              <AMGStat key={i} label={ph.name} value={fmt(ph.amount)} tone="info" />
            ))}
          </div>
        )}

        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex flex-wrap items-center gap-3">
            <AMGSectionTitle eyebrow="Historial" title="Pressupostos" />
            <div className="flex gap-2 ml-auto flex-wrap">
              {STATUS_FILTERS.map((s) => (
                <button
                  key={s}
                  onClick={() => setStatusFilter(s)}
                  className={`f-mono text-label uppercase px-3 h-7 border transition-colors ${
                    statusFilter === s
                      ? 'border-[#FF6B00] text-accent-light bg-accent-muted'
                      : 'border-border-base text-ink-2 hover:text-ink-1'
                  }`}
                >
                  {s ? LABEL[s] : 'TOTS'}
                </button>
              ))}
            </div>
          </div>

          {isLoading ? (
            <div className="flex justify-center py-12">
              <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
            </div>
          ) : filtered.length === 0 ? (
            <div className="p-8 text-center">
              <I.CreditCard size={28} stroke="#64748b" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Cap pressupost</div>
              <p className="text-ui text-ink-2">Contacta amb el teu tècnic per sol·licitar un pressupost</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[560px]">
                <thead>
                  <tr className="border-b border-border-base">
                    {['Número', 'Estat', 'Total', 'Vàlid fins', 'Accions'].map((h) => (
                      <th key={h} className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((b) => (
                    <tr key={b.id} className="border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,255,255,0.02)] transition-colors">
                      <td className="px-4 sm:px-5 py-3 f-mono text-accent-light text-xs">{b.budgetNumber}</td>
                      <td className="px-4 sm:px-5 py-3">
                        <AMGBadge tone={BADGE_TONE[b.status] ?? 'neutral'}>{LABEL[b.status] ?? b.status}</AMGBadge>
                      </td>
                      <td className="px-4 sm:px-5 py-3 f-display font-bold">{fmt(b.total)}</td>
                      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{fmtDate(b.validUntil)}</td>
                      <td className="px-4 sm:px-5 py-3">
                        <div className="flex gap-2">
                          {b.status === 'DRAFT' && (
                            <AMGButton size="sm" icon={I.Mail} disabled={sending} onClick={() => doSend(b.id)}>
                              Enviar
                            </AMGButton>
                          )}
                          {(b.status === 'DRAFT' || b.status === 'SENT') && (
                            <AMGButton size="sm" variant="ghost" icon={I.Trash} onClick={() => doCancel(b.id)}>
                              Cancel·lar
                            </AMGButton>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </PortalShell>
  );
}
