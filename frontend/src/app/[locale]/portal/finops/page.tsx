'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import {
  listInvoices, getFinOpsDashboard, getGlobalFinOpsDashboard, cancelInvoice,
  type InvoiceResponse,
} from '@/services/finops';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGStat, AMGSectionTitle } from '@/components/ui/stat';
import { IconSet } from '@/components/ui/icons';

const BADGE_TONE: Record<string, 'success' | 'warning' | 'danger' | 'accent' | 'neutral'> = {
  PAID: 'success', PENDING: 'warning', OVERDUE: 'danger',
  CANCELLED: 'neutral', DRAFT: 'accent',
};

const LABEL: Record<string, string> = {
  PAID: 'Pagada', PENDING: 'Pendent', OVERDUE: 'Vençuda',
  CANCELLED: 'Cancel·lada', DRAFT: 'Esborrany',
};

function fmt(n: number) {
  return new Intl.NumberFormat('ca-ES', { style: 'currency', currency: 'EUR' }).format(n);
}

function fmtDate(d: string | null) {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

const STATUS_FILTERS = ['', 'PENDING', 'PAID', 'OVERDUE', 'CANCELLED'];
const PAGE_SIZE = 20;

export default function FinOpsPage() {
  const { user, isSuperAdmin } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);

  const tenantId = user?.tenantId ?? '';

  const { data: dashboard } = useQuery({
    queryKey: ['finops-dashboard', tenantId],
    queryFn: () => (isSuperAdmin ? getGlobalFinOpsDashboard() : getFinOpsDashboard(tenantId)),
    enabled: isSuperAdmin || !!tenantId,
  });

  const { data: invoicePage, isLoading } = useQuery({
    queryKey: ['invoices', tenantId, statusFilter, page],
    queryFn: () => listInvoices(
      isSuperAdmin ? undefined : tenantId,
      statusFilter || undefined,
      page,
      PAGE_SIZE,
    ),
    enabled: isSuperAdmin || !!tenantId,
  });

  const invoices = invoicePage?.content ?? [];
  const totalPages = invoicePage?.totalPages ?? 1;

  const { mutate: doCancel } = useMutation({
    mutationFn: (id: string) => cancelInvoice(id),
    onSuccess: () => {
      toast('success', 'Factura cancel·lada');
      qc.invalidateQueries({ queryKey: ['invoices'] });
      qc.invalidateQueries({ queryKey: ['finops-dashboard'] });
    },
    onError: () => toast('error', 'Error cancel·lant la factura'),
  });

  return (
    <PortalShell breadcrumb="finops">
      <div className="p-4 sm:p-8 space-y-6">
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / finops /</span>
          <div className="f-display font-bold text-xl mt-1">
            {isSuperAdmin ? 'FinOps Global · Factures' : 'Les meves factures'}
          </div>
        </div>

        {dashboard && (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
            <AMGStat label="Total facturat" value={fmt(dashboard.totalInvoiced)} icon={IconSet.Receipt} tone="accent" />
            <AMGStat label="Total pagat" value={fmt(dashboard.totalPaid)} icon={IconSet.Check} tone="success" />
            <AMGStat label="Pendent" value={fmt(dashboard.totalPending)} icon={IconSet.Clock} tone={dashboard.totalPending > 0 ? 'danger' : 'success'} />
            <AMGStat label="Vençut" value={fmt(dashboard.totalOverdue)} icon={IconSet.AlertCircle} tone={dashboard.totalOverdue > 0 ? 'danger' : 'success'} />
          </div>
        )}

        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex flex-wrap items-center gap-3">
            <AMGSectionTitle eyebrow="Registre" title="Factures" />
            <div className="flex gap-2 ml-auto flex-wrap">
              {STATUS_FILTERS.map((s) => (
                <button
                  key={s}
                  onClick={() => { setStatusFilter(s); setPage(0); }}
                  className={`f-mono text-label uppercase px-3 h-7 border transition-colors ${
                    statusFilter === s
                      ? 'border-[#FF6B00] text-accent-light bg-accent-muted'
                      : 'border-border-base text-ink-2 hover:text-ink-1'
                  }`}
                >
                  {s ? LABEL[s] : 'TOTES'}
                </button>
              ))}
            </div>
          </div>

          {isLoading ? (
            <div className="flex justify-center py-12">
              <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
            </div>
          ) : invoices.length === 0 ? (
            <div className="p-8 text-center">
              <IconSet.Receipt size={28} stroke="#64748b" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Cap factura trobada</div>
              <p className="text-ui text-ink-2">Les factures apareixeran aquí quan es generin</p>
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[620px]">
                  <thead>
                    <tr className="border-b border-border-base">
                      {['Número', 'Estat', 'Import', 'IVA', 'Venciment', 'PDF', 'Accions'].map((h) => (
                        <th key={h} className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">
                          {h}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {invoices.map((inv: InvoiceResponse) => (
                      <tr key={inv.id} className="border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,255,255,0.02)] transition-colors">
                        <td className="px-4 sm:px-5 py-3 f-mono text-accent-light text-xs">{inv.invoiceNumber}</td>
                        <td className="px-4 sm:px-5 py-3">
                          <AMGBadge tone={BADGE_TONE[inv.status] ?? 'neutral'}>{LABEL[inv.status] ?? inv.status}</AMGBadge>
                        </td>
                        <td className="px-4 sm:px-5 py-3 f-display font-bold">{fmt(inv.amount)}</td>
                        <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{fmt(inv.taxAmount)}</td>
                        <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{fmtDate(inv.dueDate)}</td>
                        <td className="px-4 sm:px-5 py-3">
                          {inv.invoicePdfUrl ? (
                            <a href={inv.invoicePdfUrl} target="_blank" rel="noopener" className="text-accent-light hover:text-ink-0 transition-colors">
                              <IconSet.Download size={14} />
                            </a>
                          ) : (
                            <span className="text-ink-3 opacity-40"><IconSet.Download size={14} /></span>
                          )}
                        </td>
                        <td className="px-4 sm:px-5 py-3">
                          {inv.status === 'PENDING' && (
                            <AMGButton size="sm" variant="ghost" icon={IconSet.Trash} onClick={() => doCancel(inv.id)}>
                              Cancel·lar
                            </AMGButton>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {totalPages > 1 && (
                <div className="p-4 flex items-center justify-center gap-3 border-t border-border-base">
                  <AMGButton
                    size="sm" variant="outline"
                    disabled={page === 0}
                    onClick={() => setPage((p) => p - 1)}
                  >
                    ← Anterior
                  </AMGButton>
                  <span className="f-mono text-label text-ink-2">{page + 1} / {totalPages}</span>
                  <AMGButton
                    size="sm" variant="outline"
                    disabled={page >= totalPages - 1}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Següent →
                  </AMGButton>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </PortalShell>
  );
}
