'use client';

import { useState, useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import {
  listBudgets, listAllBudgets, getBillingDashboard,
  sendBudget, cancelBudget, updateBudget, createBudget,
  type BudgetResponse, type CreateBudgetRequest,
} from '@/services/billing';
import { getTenantSetup, type TenantSetup } from '@/services/admin';
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

// ── Budget detail / edit modal ────────────────────────────────────────────────

function BudgetDetailModal({ budget, onClose, onRefresh }: {
  budget: BudgetResponse;
  onClose: () => void;
  onRefresh: () => void;
}) {
  const { toast } = useToast();
  const [mode, setMode] = useState<'view' | 'edit'>('view');
  const [setup, setSetup] = useState<TenantSetup | null>(null);
  const [loadingSetup, setLoadingSetup] = useState(false);

  const [sending, setSending] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [cloning, setCloning] = useState(false);
  const [acceptanceUrl, setAcceptanceUrl] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const [editProfileId, setEditProfileId] = useState(budget.profileId ?? '');
  const [editPhaseIds, setEditPhaseIds] = useState<Set<string>>(new Set(budget.phaseIds ?? []));
  const [editNotes, setEditNotes] = useState(budget.notes ?? '');
  const [editClientNotes, setEditClientNotes] = useState(budget.clientNotes ?? '');
  const [editValidUntil, setEditValidUntil] = useState(budget.validUntil ? budget.validUntil.slice(0, 10) : '');

  const isDraft = budget.status === 'DRAFT';
  const statusTone = BADGE_TONE[budget.status] ?? 'neutral';

  const enterEdit = () => {
    if (!budget.tenantId) return;
    setLoadingSetup(true);
    getTenantSetup(budget.tenantId)
      .then(setSetup)
      .catch(() => toast('error', 'No s\'ha pogut carregar la configuració del tenant'))
      .finally(() => { setLoadingSetup(false); setMode('edit'); });
  };

  const toggleEditPhase = (phaseId: string) => {
    setEditPhaseIds(prev => {
      const next = new Set(prev);
      if (next.has(phaseId)) next.delete(phaseId);
      else next.add(phaseId);
      return next;
    });
  };

  const handleSend = async () => {
    setSending(true);
    try {
      const res = await sendBudget(budget.id);
      if (res?.acceptanceUrl) setAcceptanceUrl(res.acceptanceUrl);
      toast('success', 'Pressupost enviat — copia l\'enllaç per compartir-lo');
      onRefresh();
    } catch (err: unknown) {
      toast('error', `Error enviant: ${err instanceof Error ? err.message : ''}`);
    } finally { setSending(false); }
  };

  const handleCancel = async () => {
    if (!confirm('Segur que vols eliminar aquest pressupost?')) return;
    setCancelling(true);
    try {
      await cancelBudget(budget.id);
      toast('success', 'Pressupost eliminat');
      onRefresh();
      onClose();
    } catch (err: unknown) {
      toast('error', `Error eliminant: ${err instanceof Error ? err.message : ''}`);
    } finally { setCancelling(false); }
  };

  const handleClone = async () => {
    if (!budget.tenantId || !budget.profileId || !budget.phaseIds?.length) {
      toast('error', 'No es pot clonar: dades de perfil no disponibles');
      return;
    }
    setCloning(true);
    try {
      await createBudget(budget.tenantId, {
        profileId: budget.profileId,
        phaseIds: budget.phaseIds,
        notes: budget.notes ?? undefined,
        clientNotes: budget.clientNotes ?? undefined,
      });
      toast('success', 'Pressupost clonat com a DRAFT');
      onRefresh();
      onClose();
    } catch (err: unknown) {
      toast('error', `Error clonant: ${err instanceof Error ? err.message : ''}`);
    } finally { setCloning(false); }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editProfileId) { toast('error', 'Selecciona un perfil'); return; }
    if (editPhaseIds.size === 0) { toast('error', 'Selecciona almenys una fase'); return; }
    setSaving(true);
    try {
      await updateBudget(budget.id, {
        profileId: editProfileId,
        phaseIds: Array.from(editPhaseIds),
        notes: editNotes || undefined,
        clientNotes: editClientNotes || undefined,
        validUntil: editValidUntil || undefined,
      } as CreateBudgetRequest);
      toast('success', 'Pressupost actualitzat');
      onRefresh();
      onClose();
    } catch (err: unknown) {
      toast('error', `Error desant: ${err instanceof Error ? err.message : ''}`);
    } finally { setSaving(false); }
  };

  const handlePrint = () => {
    const el = document.getElementById(`budget-print-${budget.id}`);
    if (!el) return;
    const win = window.open('', '_blank', 'width=800,height=900');
    if (!win) return;
    win.document.write(`<!DOCTYPE html><html><head><title>Pressupost ${budget.budgetNumber}</title>
      <style>
        body{font-family:sans-serif;color:#111;padding:32px;max-width:700px;margin:0 auto}
        h1{font-size:22px;margin-bottom:4px}.meta{color:#666;font-size:13px;margin-bottom:24px}
        .section-title{font-size:11px;text-transform:uppercase;letter-spacing:.08em;color:#888;font-weight:700;margin:20px 0 8px}
        .phase-header{display:flex;justify-content:space-between;background:#f5f5f5;padding:8px 12px;font-weight:700;font-size:14px}
        .line{display:flex;justify-content:space-between;padding:6px 12px;border-bottom:1px solid #eee;font-size:13px}
        .prices{display:flex;gap:24px;color:#444}
        .totals{border:1px solid #ddd;border-radius:6px;padding:12px;margin-top:24px}
        .total-row{display:flex;justify-content:space-between;padding:4px 0;font-size:13px}
        .total-row.bold{font-weight:700;font-size:16px;border-top:1px solid #ddd;padding-top:8px;margin-top:4px}
      </style></head><body>${el.innerHTML}</body></html>`);
    win.document.close();
    win.focus();
    setTimeout(() => win.print(), 400);
  };

  const row = (label: string, value: string, bold = false) => (
    <div className="flex items-center justify-between py-1.5 border-b border-border-base last:border-0">
      <span className="text-xs text-ink-3 f-mono">{label}</span>
      <span className={`text-sm f-mono ${bold ? 'font-bold text-white' : 'text-ink-1'}`}>{value}</span>
    </div>
  );

  const inputCls = 'w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm text-ink-1 focus:outline-none focus:border-[#FF6B00]';
  const labelCls = 'f-mono text-[10px] uppercase tracking-wider text-ink-3 block mb-1';
  const profiles = setup?.profiles ?? [];
  const editProfile = profiles.find(p => p.profile.id === editProfileId);

  return (
    <>
      <div id={`budget-print-${budget.id}`} style={{ display: 'none' }}>
        <h1>Pressupost {budget.budgetNumber}</h1>
        <div className="meta">Creat: {fmtDate(budget.createdAt)} · Vàlid fins: {fmtDate(budget.validUntil)} · Estat: {budget.status}{budget.tenantName ? ` · Client: ${budget.tenantName}` : ''}</div>
        {budget.phases.map((phase, pi) => (
          <div key={pi}>
            <div className="section-title">Fase</div>
            <div className="phase-header"><span>{phase.name}</span><span>{phase.phaseTotal.toFixed(2)} €</span></div>
            {phase.lines.map((line, li) => (
              <div key={li} className="line"><span>{line.serviceName}</span><div className="prices"><span>{line.setupPrice.toFixed(2)} €</span><span>{line.monthlyPrice.toFixed(2)} €/mes</span></div></div>
            ))}
          </div>
        ))}
        {budget.addons.length > 0 && (
          <div>
            <div className="section-title">Addons</div>
            {budget.addons.map((a, i) => <div key={i} className="line"><span>{a.serviceName}</span><span>{a.unitPrice.toFixed(2)} €</span></div>)}
          </div>
        )}
        <div className="totals">
          <div className="total-row"><span>Subtotal</span><span>{budget.subtotal.toFixed(2)} €</span></div>
          {budget.discountTotal > 0 && <div className="total-row"><span>Descompte</span><span>-{budget.discountTotal.toFixed(2)} €</span></div>}
          <div className="total-row bold"><span>Total</span><span>{budget.total.toFixed(2)} €</span></div>
        </div>
      </div>

      <div className="fixed inset-0 bg-black/70 z-50 flex items-center justify-center p-4" onClick={onClose}>
        <div className="amg-card card-clip w-full max-w-lg max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>

          {/* Header */}
          <div className="flex items-center justify-between p-5 border-b border-border-base sticky top-0 bg-[var(--surface-card)] z-10">
            <div className="flex items-center gap-2 min-w-0">
              <AMGBadge tone={statusTone}>{LABEL[budget.status] ?? budget.status}</AMGBadge>
              <span className="f-mono font-bold text-white truncate">{budget.budgetNumber}</span>
              {budget.tenantName && <span className="f-mono text-xs text-ink-3 truncate">· {budget.tenantName}</span>}
              {mode === 'edit' && <AMGBadge tone="warning">Editant</AMGBadge>}
            </div>
            <div className="flex items-center gap-1 shrink-0 ml-2">
              {isDraft && mode === 'view' && (
                <button title="Editar" onClick={enterEdit} disabled={loadingSetup}
                  className="p-1.5 rounded text-ink-2 hover:text-white hover:bg-[rgba(255,255,255,0.08)] transition disabled:opacity-40">
                  {loadingSetup ? <span className="w-3.5 h-3.5 border border-[#FF6B00] border-t-transparent rounded-full animate-spin block" /> : <I.Edit size={15} />}
                </button>
              )}
              <button title="Imprimir / PDF" onClick={handlePrint}
                className="p-1.5 rounded text-ink-2 hover:text-white hover:bg-[rgba(255,255,255,0.08)] transition">
                <I.Download size={15} />
              </button>
              <button title="Clonar" onClick={handleClone} disabled={cloning}
                className="p-1.5 rounded text-ink-2 hover:text-white hover:bg-[rgba(255,255,255,0.08)] transition disabled:opacity-40">
                <I.Copy size={15} />
              </button>
              {isDraft && mode === 'view' && (
                <button title="Enviar al client" onClick={handleSend} disabled={sending}
                  className="p-1.5 rounded text-ink-2 hover:text-white hover:bg-[rgba(255,255,255,0.08)] transition disabled:opacity-40">
                  <I.ArrowRight size={15} />
                </button>
              )}
              <button title="Eliminar" onClick={handleCancel} disabled={cancelling}
                className="p-1.5 rounded text-red-400 hover:text-red-300 hover:bg-[rgba(239,68,68,0.12)] transition disabled:opacity-40">
                <I.Trash size={15} />
              </button>
              <button onClick={mode === 'edit' ? () => setMode('view') : onClose}
                className="p-1.5 ml-1 rounded text-ink-2 hover:text-ink-0">
                <I.X size={17} />
              </button>
            </div>
          </div>

          {mode === 'view' ? (
            <div className="p-5 space-y-5">

              {/* Enllaç d'acceptació */}
              {acceptanceUrl && (
                <div className="rounded-lg bg-green-500/10 border border-green-500/30 p-4 space-y-2">
                  <div className="text-green-400 text-xs font-semibold uppercase tracking-wider">Enllaç per al client</div>
                  <div className="flex items-center gap-2">
                    <input readOnly value={acceptanceUrl}
                      className="flex-1 bg-[rgba(255,255,255,0.05)] border border-border-base rounded px-3 py-1.5 text-xs text-ink-1 f-mono truncate focus:outline-none" />
                    <button
                      onClick={() => { navigator.clipboard.writeText(acceptanceUrl); toast('success', 'Enllaç copiat'); }}
                      className="shrink-0 px-3 py-1.5 bg-green-500/20 hover:bg-green-500/30 border border-green-500/40 text-green-400 text-xs rounded transition">
                      Copiar
                    </button>
                  </div>
                  <p className="text-ink-3 text-xs">Comparteix aquest enllaç amb el client perquè pugui revisar i acceptar la proposta.</p>
                </div>
              )}

              <div className="space-y-0">
                {row('Creat', fmtDate(budget.createdAt))}
                {row('Vàlid fins', fmtDate(budget.validUntil))}
                {budget.sentAt && row('Enviat', fmtDate(budget.sentAt))}
                {budget.acceptedAt && row('Acceptat', fmtDate(budget.acceptedAt))}
                {budget.rejectedAt && row('Rebutjat', fmtDate(budget.rejectedAt))}
                {budget.notes && row('Notes', budget.notes)}
                {budget.clientNotes && row('Notes client', budget.clientNotes)}
              </div>

              {budget.phases.length > 0 && (
                <div className="space-y-3">
                  <div className="f-display font-bold text-xs text-ink-3 uppercase tracking-wider">Fases</div>
                  {budget.phases.map((phase, pi) => (
                    <div key={pi} className="rounded border border-border-base overflow-hidden">
                      <div className="flex items-center justify-between px-4 py-2 bg-[rgba(255,255,255,0.04)]">
                        <span className="f-display font-bold text-sm text-white">{phase.name}</span>
                        <span className="f-mono text-sm text-white">{phase.phaseTotal.toFixed(2)} €</span>
                      </div>
                      <div className="divide-y divide-border-base">
                        {phase.lines.map((line, li) => (
                          <div key={li} className="flex items-center justify-between px-4 py-2 gap-4">
                            <span className="text-xs text-ink-2 flex-1">{line.serviceName}</span>
                            <div className="flex items-center gap-3 shrink-0">
                              <span className="text-xs f-mono text-ink-3">{line.setupPrice.toFixed(2)} €</span>
                              <span className="text-xs f-mono text-white">{line.monthlyPrice.toFixed(2)} €/mes</span>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {budget.addons.length > 0 && (
                <div className="space-y-2">
                  <div className="f-display font-bold text-xs text-ink-3 uppercase tracking-wider">Addons</div>
                  <div className="rounded border border-border-base divide-y divide-border-base">
                    {budget.addons.map((addon, ai) => (
                      <div key={ai} className="flex items-center justify-between px-4 py-2">
                        <span className="text-xs text-ink-2">{addon.serviceName}</span>
                        <span className="text-xs f-mono text-ink-1">{addon.unitPrice.toFixed(2)} €</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              <div className="rounded border border-border-base p-4 bg-[rgba(255,255,255,0.02)] space-y-0">
                {row('Setup (únic)', `${budget.subtotal.toFixed(2)} €`)}
                {budget.discountTotal > 0 && row('Descompte', `-${budget.discountTotal.toFixed(2)} €`)}
                {row('Total setup', `${budget.total.toFixed(2)} €`, true)}
                <div className="border-t border-border-base mt-3 pt-3">
                  {row('Mensual recurrent', `${(budget.monthlyTotal ?? 0).toFixed(2)} €/mes`, true)}
                </div>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSave} className="p-5 space-y-4">
              <div>
                <label className={labelCls}>Perfil</label>
                {profiles.length === 0 ? (
                  <p className="text-sm text-ink-3">Cap perfil assignat al tenant.</p>
                ) : (
                  <div className="space-y-2">
                    {profiles.map((p) => (
                      <button key={p.profile.id} type="button"
                        onClick={() => { setEditProfileId(p.profile.id); setEditPhaseIds(new Set()); }}
                        className={`w-full text-left p-3 border rounded transition text-sm ${
                          editProfileId === p.profile.id ? 'border-[#FF6B00] bg-accent-muted' : 'border-border-base hover:border-ink-2'
                        }`}>
                        <span className="font-semibold">{p.profile.name}</span>
                        <span className="text-ink-3 ml-2 text-xs">{p.phases.length} fases</span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
              {editProfile && (
                <div>
                  <label className={labelCls}>Fases a incloure</label>
                  <div className="space-y-2">
                    {editProfile.phases.map((ph) => (
                      <label key={ph.phase.id} className="flex items-center gap-3 p-3 border border-border-base rounded cursor-pointer hover:border-ink-2 transition">
                        <input type="checkbox" checked={editPhaseIds.has(ph.phase.id)}
                          onChange={() => toggleEditPhase(ph.phase.id)} className="accent-[#FF6B00]" />
                        <span className="text-sm flex-1">{ph.phase.name}</span>
                        <span className="f-mono text-xs text-ink-3">{ph.services.length} serveis</span>
                      </label>
                    ))}
                  </div>
                </div>
              )}
              <div>
                <label className={labelCls}>Notes internes</label>
                <textarea value={editNotes} onChange={e => setEditNotes(e.target.value)} rows={2} className={`${inputCls} resize-none`} />
              </div>
              <div>
                <label className={labelCls}>Notes per al client</label>
                <textarea value={editClientNotes} onChange={e => setEditClientNotes(e.target.value)} rows={2} className={`${inputCls} resize-none`} />
              </div>
              <div>
                <label className={labelCls}>Vàlid fins</label>
                <input type="date" value={editValidUntil} onChange={e => setEditValidUntil(e.target.value)} className={inputCls} />
              </div>
              <div className="flex gap-3 pt-2 border-t border-border-base">
                <AMGButton type="submit" disabled={saving} loading={saving} className="flex-1 justify-center">Desar canvis</AMGButton>
                <AMGButton type="button" variant="outline" onClick={() => setMode('view')}>Cancel·lar</AMGButton>
              </div>
            </form>
          )}
        </div>
      </div>
    </>
  );
}

// ── Main page ─────────────────────────────────────────────────────────────────

export default function BillingPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();
  const [statusFilter, setStatusFilter] = useState('');
  const [selectedBudget, setSelectedBudget] = useState<BudgetResponse | null>(null);

  const isAdmin = user?.role === 'SUPER_ADMIN' || user?.role === 'ADMIN';
  const tenantId = user?.tenantId ?? '';

  const { data: dashboard } = useQuery({
    queryKey: ['billing-dashboard', tenantId],
    queryFn: () => getBillingDashboard(tenantId),
    enabled: !!tenantId && !isAdmin,
  });

  const { data: budgets = [], isLoading, refetch: refetchBudgets } = useQuery({
    queryKey: isAdmin ? ['budgets-all', statusFilter] : ['budgets', tenantId, statusFilter],
    queryFn: () => isAdmin
      ? listAllBudgets(statusFilter || undefined)
      : listBudgets(tenantId, statusFilter || undefined),
    enabled: isAdmin || !!tenantId,
  });

  const handleRefresh = () => {
    refetchBudgets();
    qc.invalidateQueries({ queryKey: ['billing-dashboard'] });
  };

  const filtered = budgets as BudgetResponse[];

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
            <AMGSectionTitle eyebrow={isAdmin ? 'Tots els tenants' : 'Historial'} title="Pressupostos" />
            <div className="flex gap-2 ml-auto flex-wrap">
              {STATUS_FILTERS.map((s) => (
                <button key={s} onClick={() => setStatusFilter(s)}
                  className={`f-mono text-label uppercase px-3 h-7 border transition-colors ${
                    statusFilter === s
                      ? 'border-[#FF6B00] text-accent-light bg-accent-muted'
                      : 'border-border-base text-ink-2 hover:text-ink-1'
                  }`}>
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
              <p className="text-ui text-ink-2">
                {isAdmin ? 'No hi ha pressupostos creats' : 'Contacta amb el teu tècnic per sol·licitar un pressupost'}
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[560px]">
                <thead>
                  <tr className="border-b border-border-base">
                    {(isAdmin
                      ? ['Client', 'Número', 'Estat', 'Total', 'Vàlid fins']
                      : ['Número', 'Estat', 'Total', 'Vàlid fins']
                    ).map((h) => (
                      <th key={h} className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((b) => (
                    <tr key={b.id}
                      onClick={() => setSelectedBudget(b)}
                      className="border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,107,0,0.04)] hover:border-[rgba(255,107,0,0.2)] transition-colors cursor-pointer">
                      {isAdmin && (
                        <td className="px-4 sm:px-5 py-3 text-xs text-ink-2 max-w-[140px] truncate">
                          {b.tenantName ?? '—'}
                        </td>
                      )}
                      <td className="px-4 sm:px-5 py-3 f-mono text-accent-light text-xs">{b.budgetNumber}</td>
                      <td className="px-4 sm:px-5 py-3">
                        <AMGBadge tone={BADGE_TONE[b.status] ?? 'neutral'}>{LABEL[b.status] ?? b.status}</AMGBadge>
                      </td>
                      <td className="px-4 sm:px-5 py-3 f-display font-bold">{fmt(b.total)}</td>
                      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{fmtDate(b.validUntil)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {selectedBudget && (
        <BudgetDetailModal
          budget={selectedBudget}
          onClose={() => setSelectedBudget(null)}
          onRefresh={() => { handleRefresh(); setSelectedBudget(null); }}
        />
      )}
    </PortalShell>
  );
}
