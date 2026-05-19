'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import { getPayments, getPaymentDashboard, refundPayment, type Payment } from '@/services/payments';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGStat } from '@/components/ui/stat';
import { I } from '@/components/ui/icons';

const STATUS_TONE: Record<string, 'neutral' | 'info' | 'success' | 'danger' | 'warning' | 'accent'> = {
  PENDING: 'warning',
  SUCCEEDED: 'success',
  FAILED: 'danger',
  REFUNDED: 'neutral',
  CANCELLED: 'danger',
};

const STATUS_LABEL: Record<string, string> = {
  PENDING: 'Pendent',
  SUCCEEDED: 'Completat',
  FAILED: 'Fallat',
  REFUNDED: 'Reemborsat',
  CANCELLED: 'Cancel·lat',
};

function fmt(n: number, currency = 'EUR') {
  return new Intl.NumberFormat('ca-ES', { style: 'currency', currency }).format(n / 100);
}

function fmtDate(d: string) {
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

export default function PaymentsPage() {
  const { user, isAdmin } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();

  const tenantId = user?.tenantId ?? undefined;

  const { data: dashboard } = useQuery({
    queryKey: ['payments-dashboard'],
    queryFn: getPaymentDashboard,
    enabled: !!user && isAdmin,
  });

  const { data: payments = [], isLoading } = useQuery({
    queryKey: ['payments', tenantId],
    queryFn: () => getPayments(tenantId),
    enabled: !!user,
  });

  const { mutate: doRefund, isPending: refunding } = useMutation({
    mutationFn: (id: string) => refundPayment(id),
    onSuccess: () => {
      toast('success', 'Reembors iniciat correctament');
      qc.invalidateQueries({ queryKey: ['payments'] });
      qc.invalidateQueries({ queryKey: ['payments-dashboard'] });
    },
    onError: () => toast('error', 'Error iniciant el reembors'),
  });

  if (!user) return null;

  return (
    <PortalShell breadcrumb="payments">
      <div className="p-4 sm:p-8 space-y-6">
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / payments /</span>
          <div className="f-display font-bold text-xl mt-1">Pagaments</div>
        </div>

        {dashboard && (
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <AMGStat
              label="Total cobrat"
              value={String(dashboard.completedCount)}
              icon={I.CreditCard}
              tone="success"
            />
            <AMGStat
              label="Pendent"
              value={String(dashboard.pendingCount)}
              icon={I.Clock}
              tone={dashboard.pendingCount > 0 ? 'danger' : 'accent'}
            />
            <AMGStat
              label="Fallits"
              value={String(dashboard.failedCount)}
              icon={I.ArrowRight}
              tone="info"
            />
          </div>
        )}

        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base">
            <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Historial de pagaments</div>
          </div>

          {isLoading ? (
            <div className="flex justify-center py-12">
              <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
            </div>
          ) : (payments as Payment[]).length === 0 ? (
            <div className="p-8 text-center">
              <I.CreditCard size={28} stroke="#64748b" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Cap pagament</div>
              <p className="f-mono text-label text-ink-2">No hi ha pagaments registrats</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[560px]">
                <thead>
                  <tr className="border-b border-border-base">
                    {['ID', 'Proveïdor', 'Import', 'Estat', 'Data', ...(isAdmin ? ['Accions'] : [])].map((h) => (
                      <th key={h} className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {(payments as Payment[]).map((p) => (
                    <tr key={p.id} className="border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,255,255,0.02)] transition-colors">
                      <td className="px-4 sm:px-5 py-3 f-mono text-accent-light text-xs font-semibold">{p.id.slice(0, 8)}…</td>
                      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1 uppercase">{p.provider}</td>
                      <td className="px-4 sm:px-5 py-3 f-display font-bold">{fmt(p.amount, p.currency)}</td>
                      <td className="px-4 sm:px-5 py-3">
                        <AMGBadge tone={STATUS_TONE[p.status] ?? 'neutral'}>
                          {STATUS_LABEL[p.status] ?? p.status}
                        </AMGBadge>
                      </td>
                      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{fmtDate(p.createdAt)}</td>
                      {isAdmin && (
                        <td className="px-4 sm:px-5 py-3">
                          {p.status === 'SUCCEEDED' && (
                            <AMGButton
                              size="sm"
                              variant="ghost"
                              icon={I.ArrowRight}
                              disabled={refunding}
                              onClick={() => {
                                if (confirm('Iniciar reembors?')) doRefund(p.id);
                              }}
                            >
                              Reembors
                            </AMGButton>
                          )}
                        </td>
                      )}
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
