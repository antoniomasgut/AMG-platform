'use client';

import { useState, useEffect } from 'react';
import { useSearchParams } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import { useParams } from 'next/navigation';
import { getPayments, getPaymentDashboard, refundPayment, type Payment } from '@/services/payments';
import {
  getGoCardlessMandate, initiateGoCardlessMandate, completeGoCardlessMandate, cancelGoCardlessMandate,
  type GoCardlessMandate,
} from '@/services/admin';
import {
  getSavedPaymentMethod, createSetupSession, completeSetupSession, removeSavedPaymentMethod,
  type SavedPaymentMethod,
} from '@/services/payments';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGStat } from '@/components/ui/stat';
import { IconSet } from '@/components/ui/icons';

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

const PAGE_SIZE = 20;

function StripeSetupCard({ tenantId }: { tenantId: string }) {
  const { toast } = useToast();
  const qc = useQueryClient();
  const params = useParams();
  const locale = (params.locale as string) ?? 'ca';
  const searchParams = useSearchParams();

  const { data: pm, isLoading } = useQuery<SavedPaymentMethod | null>({
    queryKey: ['stripe-pm', tenantId],
    queryFn: () => getSavedPaymentMethod(tenantId),
  });

  // Completar setup quan Stripe redirigeix amb ?session_id=XXX
  const sessionId = searchParams.get('session_id');
  useEffect(() => {
    if (!sessionId || !tenantId) return;
    completeSetupSession(tenantId, sessionId)
      .then(() => {
        qc.invalidateQueries({ queryKey: ['stripe-pm', tenantId] });
        toast('success', 'Targeta configurada correctament');
        window.history.replaceState({}, '', window.location.pathname);
      })
      .catch(() => toast('error', 'Error guardant la targeta'));
  }, [sessionId, tenantId, qc, toast]);

  const { mutate: initSetup, isPending: setting } = useMutation({
    mutationFn: () => {
      const base = `${window.location.origin}/${locale}/portal/payments`;
      return createSetupSession(tenantId, base, base);
    },
    onSuccess: (data) => { window.location.href = data.url; },
    onError: () => toast('error', 'Error iniciant la configuració de pagament'),
  });

  const { mutate: remove, isPending: removing } = useMutation({
    mutationFn: () => removeSavedPaymentMethod(tenantId),
    onSuccess: () => {
      toast('success', 'Mètode de pagament eliminat');
      qc.invalidateQueries({ queryKey: ['stripe-pm', tenantId] });
    },
    onError: () => toast('error', 'Error eliminant el mètode de pagament'),
  });

  const brandIcon: Record<string, string> = {
    visa: '💳 Visa', mastercard: '💳 Mastercard', amex: '💳 Amex',
    sepa_debit: '🏦 SEPA', card: '💳',
  };

  return (
    <div className={`amg-card card-clip p-4 sm:p-5 border ${pm ? 'border-green-500/30' : 'border-border-base'}`}>
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-3">
          <div className={`mt-0.5 p-2 rounded ${pm ? 'bg-green-500/10' : 'bg-[rgba(255,255,255,0.04)]'}`}>
            <IconSet.CreditCard size={16} stroke={pm ? '#39d353' : '#64748b'} />
          </div>
          <div>
            <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3 mb-0.5">
              Mètode de pagament principal · Stripe
            </div>
            {isLoading ? (
              <div className="f-display font-bold text-sm text-ink-2">Carregant...</div>
            ) : pm ? (
              <>
                <div className="f-display font-bold text-sm text-green-400">
                  {brandIcon[pm.brand] ?? pm.brand}{pm.lastFour ? ` ····${pm.lastFour}` : ''}
                </div>
                {pm.expMonth && pm.expYear && (
                  <div className="f-mono text-xs text-ink-2 mt-0.5">
                    Caduca {String(pm.expMonth).padStart(2, '0')}/{pm.expYear}
                  </div>
                )}
                <div className="f-mono text-[10px] text-green-500/70 mt-1">
                  Les factures es cobren automàticament
                </div>
              </>
            ) : (
              <>
                <div className="f-display font-bold text-sm">Sense targeta guardada</div>
                <div className="f-mono text-xs text-ink-2 mt-0.5">
                  Afegeix una targeta o compte SEPA per a cobraments automàtics.
                </div>
              </>
            )}
          </div>
        </div>
        <div className="shrink-0 flex gap-2">
          {pm ? (
            <>
              <AMGButton size="sm" variant="secondary" disabled={setting} loading={setting} onClick={() => initSetup()}>
                Canviar
              </AMGButton>
              <AMGButton size="sm" variant="ghost" disabled={removing} loading={removing}
                onClick={() => { if (confirm('Eliminar el mètode de pagament guardat?')) remove(); }}>
                Eliminar
              </AMGButton>
            </>
          ) : (
            <AMGButton size="sm" disabled={setting} loading={setting} onClick={() => initSetup()}>
              Afegir targeta / SEPA →
            </AMGButton>
          )}
        </div>
      </div>
    </div>
  );
}

function MandateCard({ tenantId }: { tenantId: string }) {
  const { toast } = useToast();
  const qc = useQueryClient();
  const params = useParams();
  const locale = (params.locale as string) ?? 'ca';
  const searchParams = useSearchParams();

  const { data: mandate, isLoading } = useQuery<GoCardlessMandate | null>({
    queryKey: ['gc-mandate', tenantId],
    queryFn: () => getGoCardlessMandate(tenantId).catch(() => null),
  });

  // Completar mandat quan GoCardless redirigeix de tornada amb ?redirect_flow_id=XXX
  const redirectFlowId = searchParams.get('redirect_flow_id');
  useEffect(() => {
    if (!redirectFlowId || !tenantId) return;
    completeGoCardlessMandate(tenantId, redirectFlowId)
      .then(() => {
        qc.invalidateQueries({ queryKey: ['gc-mandate', tenantId] });
        toast('success', 'Domiciliació bancària configurada correctament');
        // Netejar els params de la URL sense recarregar
        window.history.replaceState({}, '', window.location.pathname);
      })
      .catch(() => toast('error', 'Error completant el mandat bancari'));
  }, [redirectFlowId, tenantId, qc, toast]);

  const { mutate: initiate, isPending: initiating } = useMutation({
    mutationFn: () => {
      const returnUrl = `${window.location.origin}/${locale}/portal/payments`;
      return initiateGoCardlessMandate(tenantId, returnUrl);
    },
    onSuccess: (data) => { window.location.href = data.redirectUrl; },
    onError: () => toast('error', 'Error iniciant la configuració del mandat'),
  });

  const { mutate: cancel, isPending: cancelling } = useMutation({
    mutationFn: () => cancelGoCardlessMandate(tenantId),
    onSuccess: () => {
      toast('success', 'Mandat cancel·lat');
      qc.invalidateQueries({ queryKey: ['gc-mandate', tenantId] });
    },
    onError: () => toast('error', 'Error cancel·lant el mandat'),
  });

  const status = mandate?.status ?? null;
  const isActive = status === 'ACTIVE';
  const isPending = status === 'PENDING_SUBMISSION' || status === 'PENDING';

  return (
    <div className={`amg-card card-clip p-4 sm:p-5 border ${isActive ? 'border-green-500/30' : isPending ? 'border-yellow-500/30' : 'border-border-base'}`}>
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-3">
          <div className={`mt-0.5 p-2 rounded ${isActive ? 'bg-green-500/10' : isPending ? 'bg-yellow-500/10' : 'bg-[rgba(255,255,255,0.04)]'}`}>
            <IconSet.CreditCard size={16} stroke={isActive ? '#39d353' : isPending ? '#f0b429' : '#64748b'} />
          </div>
          <div>
            <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3 mb-0.5">Mètode de pagament</div>
            {isLoading ? (
              <div className="f-display font-bold text-sm text-ink-2">Carregant...</div>
            ) : isActive ? (
              <>
                <div className="f-display font-bold text-sm text-green-400">Domiciliació bancària activa</div>
                <div className="f-mono text-xs text-ink-2 mt-0.5">
                  {mandate?.bankName ?? 'Banc'}{mandate?.lastFourDigits ? ` · ····${mandate.lastFourDigits}` : ''}
                  {mandate?.accountHolderName ? ` · ${mandate.accountHolderName}` : ''}
                </div>
                <div className="f-mono text-[10px] text-green-500/70 mt-1">
                  Les factures mensuals es cobren automàticament
                </div>
              </>
            ) : isPending ? (
              <>
                <div className="f-display font-bold text-sm text-yellow-400">Configuració pendent</div>
                <div className="f-mono text-xs text-ink-2 mt-0.5">
                  El mandat bancari no s&apos;ha completat. Torna a iniciar el procés.
                </div>
              </>
            ) : (
              <>
                <div className="f-display font-bold text-sm">Sense mètode de pagament automàtic</div>
                <div className="f-mono text-xs text-ink-2 mt-0.5">
                  Configura una domiciliació bancària SEPA perquè les factures es paguin soles.
                </div>
              </>
            )}
          </div>
        </div>

        <div className="shrink-0 flex gap-2">
          {isActive ? (
            <AMGButton
              size="sm" variant="ghost"
              disabled={cancelling}
              loading={cancelling}
              onClick={() => { if (confirm('Segur que vols cancel·lar la domiciliació?')) cancel(); }}
            >
              Cancel·lar
            </AMGButton>
          ) : (
            <AMGButton
              size="sm"
              disabled={initiating}
              loading={initiating}
              onClick={() => initiate()}
            >
              {isPending ? 'Reintentar configuració' : 'Configurar domiciliació →'}
            </AMGButton>
          )}
        </div>
      </div>
    </div>
  );
}

export default function PaymentsPage() {
  const { user, isAdmin } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const searchParams = useSearchParams();

  const tenantId = user?.tenantId ?? undefined;


  const { data: dashboard } = useQuery({
    queryKey: ['payments-dashboard'],
    queryFn: getPaymentDashboard,
    enabled: !!user && isAdmin,
  });

  const { data: paymentPage, isLoading } = useQuery({
    queryKey: ['payments', tenantId, page],
    queryFn: () => getPayments(tenantId, page, PAGE_SIZE),
    enabled: !!user,
  });

  const payments = paymentPage?.content ?? [];
  const totalPages = paymentPage?.totalPages ?? 1;

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

        {/* Mètodes de pagament automàtic — només per a clients */}
        {!isAdmin && tenantId && <StripeSetupCard tenantId={tenantId} />}
        {!isAdmin && tenantId && (
          <details className="group">
            <summary className="cursor-pointer f-mono text-[10px] uppercase text-ink-3 hover:text-ink-2 transition-colors list-none flex items-center gap-1">
              <IconSet.Chevron size={10} className="group-open:rotate-90 transition-transform" />
              Alternativa: domiciliació bancària GoCardless
            </summary>
            <div className="mt-2">
              <MandateCard tenantId={tenantId} />
            </div>
          </details>
        )}

        {dashboard && (
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <AMGStat
              label="Total cobrat"
              value={String(dashboard.completedCount)}
              icon={IconSet.CreditCard}
              tone="success"
            />
            <AMGStat
              label="Pendent"
              value={String(dashboard.pendingCount)}
              icon={IconSet.Clock}
              tone={dashboard.pendingCount > 0 ? 'danger' : 'accent'}
            />
            <AMGStat
              label="Fallits"
              value={String(dashboard.failedCount)}
              icon={IconSet.ArrowRight}
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
          ) : payments.length === 0 ? (
            <div className="p-8 text-center">
              <IconSet.CreditCard size={28} stroke="#64748b" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Cap pagament</div>
              <p className="f-mono text-label text-ink-2">No hi ha pagaments registrats</p>
            </div>
          ) : (
            <>
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
                    {payments.map((p: Payment) => (
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
                                icon={IconSet.ArrowRight}
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
              {totalPages > 1 && (
                <div className="p-4 flex items-center justify-center gap-3 border-t border-border-base">
                  <AMGButton size="sm" variant="outline" disabled={page === 0} onClick={() => setPage(p => p - 1)}>
                    ← Anterior
                  </AMGButton>
                  <span className="f-mono text-label text-ink-2">{page + 1} / {totalPages}</span>
                  <AMGButton size="sm" variant="outline" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>
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
