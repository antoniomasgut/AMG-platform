'use client';

import { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { useParams } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import {
  getEarlyAdopterStatus,
  updateEarlyAdopter,
  applyEarlyAdopter,
  applyAnnualContract,
  getAllReferrals,
  applyReferralCode,
  type EarlyAdopterStatus,
  type ReferralCode,
} from '@/services/commercial-programs';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGBadge } from '@/components/ui/badge';
import { I } from '@/components/ui/icons';

export default function CommercialProgramsPage() {
  const { user, isSuperAdmin } = useAuth();
  const params = useParams();
  const locale = params.locale as string;
  const [tab, setTab] = useState<'early-adopter' | 'annual-contract' | 'referrals'>('early-adopter');

  // Early Adopter
  const [editMode, setEditMode] = useState(false);
  const [earlyAdopterForm, setEarlyAdopterForm] = useState<Partial<EarlyAdopterStatus>>({});
  const [tenantIdToApply, setTenantIdToApply] = useState('');

  // Annual Contract
  const [tenantIdAnnual, setTenantIdAnnual] = useState('');

  // Referrals
  const [referralCode, setReferralCode] = useState('');
  const [newTenantId, setNewTenantId] = useState('');

  const { data: earlyAdopterStatus, isLoading: loadingEarly, refetch: refetchEarly } = useQuery({
    queryKey: ['early-adopter-status'],
    queryFn: getEarlyAdopterStatus,
    enabled: !!user && isSuperAdmin,
  });

  const { data: referrals = [], isLoading: loadingReferrals, refetch: refetchReferrals } = useQuery({
    queryKey: ['all-referrals'],
    queryFn: getAllReferrals,
    enabled: !!user && isSuperAdmin && tab === 'referrals',
  });

  const updateEarlyAdopterMutation = useMutation({
    mutationFn: () => updateEarlyAdopter(earlyAdopterForm),
    onSuccess: () => {
      refetchEarly();
      setEditMode(false);
      setEarlyAdopterForm({});
    },
  });

  const applyEarlyAdopterMutation = useMutation({
    mutationFn: () => applyEarlyAdopter(tenantIdToApply),
    onSuccess: () => {
      refetchEarly();
      setTenantIdToApply('');
    },
  });

  const applyAnnualMutation = useMutation({
    mutationFn: () => applyAnnualContract(tenantIdAnnual),
    onSuccess: () => {
      setTenantIdAnnual('');
    },
  });

  const applyReferralMutation = useMutation({
    mutationFn: () => applyReferralCode(referralCode, newTenantId),
    onSuccess: () => {
      refetchReferrals();
      setReferralCode('');
      setNewTenantId('');
    },
  });

  if (!user || !isSuperAdmin) {
    return (
      <PortalShell breadcrumb="billing / programes" backHref={`/${locale}/portal/billing`}>
        <div className="p-4 sm:p-8">
          <div className="p-4 border-l-2 border-l-danger bg-danger/5">
            <span className="f-mono text-label text-danger-light">Accés denegat. Només SUPER_ADMIN.</span>
          </div>
        </div>
      </PortalShell>
    );
  }

  const progress = earlyAdopterStatus
    ? (earlyAdopterStatus.usedSlots / earlyAdopterStatus.maxSlots) * 100
    : 0;

  return (
    <PortalShell breadcrumb="billing / programes" backHref={`/${locale}/portal/billing`}>
      <div className="p-4 sm:p-8 space-y-6">
        {/* Header */}
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / billing / programes /</span>
          <h1 className="f-display font-bold text-xl mt-1">Programes Comercials</h1>
        </div>

        {/* Tabs */}
        <div className="flex gap-2 border-b border-border-base">
          {(['early-adopter', 'annual-contract', 'referrals'] as const).map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`px-4 py-2 f-mono text-label uppercase tracking-widest transition-colors ${
                tab === t
                  ? 'border-b-2 border-b-accent text-accent-light'
                  : 'text-ink-2 hover:text-ink-1'
              }`}
            >
              {t === 'early-adopter'
                ? 'Early Adopter'
                : t === 'annual-contract'
                  ? 'Contracte Anual'
                  : 'Referits'}
            </button>
          ))}
        </div>

        {/* Early Adopter Tab */}
        {tab === 'early-adopter' && (
          <div className="amg-card card-clip p-6 space-y-6">
            {loadingEarly ? (
              <div className="flex justify-center py-12">
                <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
              </div>
            ) : earlyAdopterStatus ? (
              <>
                <div className="flex items-center justify-between">
                  <div>
                    <h2 className="f-display font-bold text-lg mb-1">Early Adopter</h2>
                    <p className="text-sm text-ink-2">Setup gratuït + 30% mensual de per vida</p>
                  </div>
                  <AMGBadge tone={earlyAdopterStatus.active ? 'success' : 'danger'}>
                    {earlyAdopterStatus.active ? 'ACTIU' : 'TANCAT'}
                  </AMGBadge>
                </div>

                {/* Progress Bar */}
                <div>
                  <div className="flex justify-between items-center mb-2">
                    <span className="f-mono text-label text-ink-2 uppercase tracking-wider">Slots usats</span>
                    <span className="f-mono text-label text-ink-0 font-semibold">
                      {earlyAdopterStatus.usedSlots} / {earlyAdopterStatus.maxSlots}
                    </span>
                  </div>
                  <div className="h-2 bg-bg-1 rounded-full overflow-hidden">
                    <div
                      className={`h-full transition-all ${
                        earlyAdopterStatus.availableSlots > 0 ? 'bg-success' : 'bg-warning'
                      }`}
                      style={{ width: `${Math.min(progress, 100)}%` }}
                    />
                  </div>
                  {earlyAdopterStatus.availableSlots > 0 && (
                    <div className="mt-2">
                      <AMGBadge tone="success">{earlyAdopterStatus.availableSlots} places disponibles</AMGBadge>
                    </div>
                  )}
                </div>

                {/* Config Form */}
                {editMode ? (
                  <div className="border-t border-border-base pt-6 space-y-4">
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <div>
                        <label className="f-mono text-label text-ink-2 uppercase tracking-wider mb-2 block">
                          Max slots
                        </label>
                        <input
                          type="number"
                          value={earlyAdopterForm.maxSlots ?? earlyAdopterStatus.maxSlots}
                          onChange={(e) =>
                            setEarlyAdopterForm({
                              ...earlyAdopterForm,
                              maxSlots: parseInt(e.target.value),
                            })
                          }
                          className="w-full px-3 py-2 border border-border-base rounded bg-bg-1 text-ink-0 f-mono text-sm"
                        />
                      </div>
                      <div>
                        <label className="f-mono text-label text-ink-2 uppercase tracking-wider mb-2 block">
                          Setup discount %
                        </label>
                        <input
                          type="number"
                          step="0.01"
                          value={
                            earlyAdopterForm.setupDiscountPct ?? earlyAdopterStatus.setupDiscountPct
                          }
                          onChange={(e) =>
                            setEarlyAdopterForm({
                              ...earlyAdopterForm,
                              setupDiscountPct: parseFloat(e.target.value),
                            })
                          }
                          className="w-full px-3 py-2 border border-border-base rounded bg-bg-1 text-ink-0 f-mono text-sm"
                        />
                      </div>
                      <div>
                        <label className="f-mono text-label text-ink-2 uppercase tracking-wider mb-2 block">
                          Monthly discount %
                        </label>
                        <input
                          type="number"
                          step="0.01"
                          value={
                            earlyAdopterForm.monthlyDiscountPct ??
                            earlyAdopterStatus.monthlyDiscountPct
                          }
                          onChange={(e) =>
                            setEarlyAdopterForm({
                              ...earlyAdopterForm,
                              monthlyDiscountPct: parseFloat(e.target.value),
                            })
                          }
                          className="w-full px-3 py-2 border border-border-base rounded bg-bg-1 text-ink-0 f-mono text-sm"
                        />
                      </div>
                      <div>
                        <label className="f-mono text-label text-ink-2 uppercase tracking-wider mb-2 block">
                          Commitment months
                        </label>
                        <input
                          type="number"
                          value={
                            earlyAdopterForm.commitmentMonths ??
                            earlyAdopterStatus.commitmentMonths
                          }
                          onChange={(e) =>
                            setEarlyAdopterForm({
                              ...earlyAdopterForm,
                              commitmentMonths: parseInt(e.target.value),
                            })
                          }
                          className="w-full px-3 py-2 border border-border-base rounded bg-bg-1 text-ink-0 f-mono text-sm"
                        />
                      </div>
                    </div>

                    <div className="flex gap-3">
                      <button
                        onClick={() => updateEarlyAdopterMutation.mutate()}
                        disabled={updateEarlyAdopterMutation.isPending}
                        className="px-4 py-2 bg-accent text-white f-mono text-label uppercase tracking-wider rounded hover:bg-accent/90 disabled:opacity-50"
                      >
                        Guardar
                      </button>
                      <button
                        onClick={() => {
                          setEditMode(false);
                          setEarlyAdopterForm({});
                        }}
                        className="px-4 py-2 border border-border-base text-ink-1 f-mono text-label uppercase tracking-wider rounded hover:bg-bg-1"
                      >
                        Cancelar
                      </button>
                    </div>
                  </div>
                ) : (
                  <button
                    onClick={() => setEditMode(true)}
                    className="px-4 py-2 border border-border-base text-ink-1 f-mono text-label uppercase tracking-wider rounded hover:bg-bg-1"
                  >
                    Editar configuració
                  </button>
                )}

                <div className="border-t border-border-base pt-6">
                  <h3 className="f-display font-bold text-base mb-4">Aplicar a tenant</h3>
                  <div className="flex gap-3">
                    <input
                      type="text"
                      placeholder="ID del tenant"
                      value={tenantIdToApply}
                      onChange={(e) => setTenantIdToApply(e.target.value)}
                      className="flex-1 px-3 py-2 border border-border-base rounded bg-bg-1 text-ink-0 f-mono text-sm"
                    />
                    <button
                      onClick={() => applyEarlyAdopterMutation.mutate()}
                      disabled={
                        !tenantIdToApply || applyEarlyAdopterMutation.isPending
                      }
                      className="px-4 py-2 bg-accent text-white f-mono text-label uppercase tracking-wider rounded hover:bg-accent/90 disabled:opacity-50"
                    >
                      Aplicar
                    </button>
                  </div>
                  {applyEarlyAdopterMutation.isError && (
                    <p className="mt-2 text-sm text-danger">
                      No es pot aplicar: no hi ha slots disponibles
                    </p>
                  )}
                </div>
              </>
            ) : null}
          </div>
        )}

        {/* Annual Contract Tab */}
        {tab === 'annual-contract' && (
          <div className="amg-card card-clip p-6 space-y-6">
            <div>
              <h2 className="f-display font-bold text-lg mb-1">Contracte Anual</h2>
              <p className="text-sm text-ink-2">Setup gratuït + Compromís de 12 mesos</p>
            </div>

            <div className="border-t border-border-base pt-6">
              <h3 className="f-display font-bold text-base mb-4">Aplicar a tenant</h3>
              <div className="flex gap-3">
                <input
                  type="text"
                  placeholder="ID del tenant"
                  value={tenantIdAnnual}
                  onChange={(e) => setTenantIdAnnual(e.target.value)}
                  className="flex-1 px-3 py-2 border border-border-base rounded bg-bg-1 text-ink-0 f-mono text-sm"
                />
                <button
                  onClick={() => applyAnnualMutation.mutate()}
                  disabled={!tenantIdAnnual || applyAnnualMutation.isPending}
                  className="px-4 py-2 bg-accent text-white f-mono text-label uppercase tracking-wider rounded hover:bg-accent/90 disabled:opacity-50"
                >
                  Aplicar
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Referrals Tab */}
        {tab === 'referrals' && (
          <div className="space-y-6">
            {/* Apply Referral */}
            <div className="amg-card card-clip p-6 space-y-4">
              <h3 className="f-display font-bold text-base">Aplicar codi referit</h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <input
                  type="text"
                  placeholder="Codi referit (ex: AMG-ABC12345)"
                  value={referralCode}
                  onChange={(e) => setReferralCode(e.target.value.toUpperCase())}
                  className="px-3 py-2 border border-border-base rounded bg-bg-1 text-ink-0 f-mono text-sm"
                />
                <input
                  type="text"
                  placeholder="ID del tenant referit"
                  value={newTenantId}
                  onChange={(e) => setNewTenantId(e.target.value)}
                  className="px-3 py-2 border border-border-base rounded bg-bg-1 text-ink-0 f-mono text-sm"
                />
              </div>
              <button
                onClick={() => applyReferralMutation.mutate()}
                disabled={!referralCode || !newTenantId || applyReferralMutation.isPending}
                className="px-4 py-2 bg-accent text-white f-mono text-label uppercase tracking-wider rounded hover:bg-accent/90 disabled:opacity-50"
              >
                Aplicar codi
              </button>
              {applyReferralMutation.isError && (
                <p className="text-sm text-danger">
                  {applyReferralMutation.error instanceof Error
                    ? applyReferralMutation.error.message
                    : 'Error aplicant el codi'}
                </p>
              )}
            </div>

            {/* Referrals List */}
            <div className="amg-card card-clip">
              <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
                <h3 className="f-mono text-label uppercase text-ink-2 tracking-widest">Codis referits</h3>
                {referrals.length > 0 && (
                  <AMGBadge tone="info">{referrals.length} codis</AMGBadge>
                )}
              </div>

              {loadingReferrals ? (
                <div className="flex justify-center py-8">
                  <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
                </div>
              ) : referrals.length === 0 ? (
                <div className="p-8 text-center">
                  <I.Box size={28} stroke="#6c757d" className="mx-auto mb-3" />
                  <div className="f-display font-bold text-sm mb-1 text-ink-2">Cap codi referit</div>
                  <p className="f-mono text-label text-ink-2">No hi ha referits aún</p>
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead className="border-b border-border-base">
                      <tr>
                        <th className="px-4 py-3 text-left f-mono text-label text-ink-2 uppercase tracking-widest">
                          Referidor
                        </th>
                        <th className="px-4 py-3 text-left f-mono text-label text-ink-2 uppercase tracking-widest">
                          Codi
                        </th>
                        <th className="px-4 py-3 text-left f-mono text-label text-ink-2 uppercase tracking-widest">
                          Referit
                        </th>
                        <th className="px-4 py-3 text-left f-mono text-label text-ink-2 uppercase tracking-widest">
                          Data ús
                        </th>
                        <th className="px-4 py-3 text-left f-mono text-label text-ink-2 uppercase tracking-widest">
                          Crèdit
                        </th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-border-base">
                      {referrals.map((ref) => (
                        <tr key={ref.id}>
                          <td className="px-4 py-3 text-ink-1">
                            <code className="text-xs">{ref.ownerTenantId.slice(0, 8)}...</code>
                          </td>
                          <td className="px-4 py-3 f-mono text-label font-semibold text-accent">
                            {ref.code}
                          </td>
                          <td className="px-4 py-3 text-ink-1">
                            {ref.usedByTenantId ? (
                              <code className="text-xs">{ref.usedByTenantId.slice(0, 8)}...</code>
                            ) : (
                              <span className="text-ink-2">–</span>
                            )}
                          </td>
                          <td className="px-4 py-3 text-ink-2 text-xs">
                            {ref.usedAt ? new Date(ref.usedAt).toLocaleDateString('ca-ES') : '–'}
                          </td>
                          <td className="px-4 py-3">
                            <AMGBadge
                              tone={ref.creditApplied ? 'success' : 'warning'}
                            >
                              {ref.creditApplied ? 'Aplicat' : 'Pendent'}
                            </AMGBadge>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </PortalShell>
  );
}
