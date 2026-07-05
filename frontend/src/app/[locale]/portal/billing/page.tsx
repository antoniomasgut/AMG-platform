'use client';

import { useState, useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useRouter, useParams } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import {
  listBudgets, listAllBudgets, getBillingDashboard,
  sendBudget, cancelBudget, updateBudget, createBudget,
  type BudgetResponse, type CreateBudgetRequest, type CustomLineRequest,
} from '@/services/billing';
import { getTenantSetup, listTenants, type TenantSetup, type TenantResponse } from '@/services/admin';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGStat, AMGSectionTitle } from '@/components/ui/stat';
import { IconSet } from '@/components/ui/icons';

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

// ── Create budget modal (línies lliures) ───────────────────────────────────────

interface LineItem { description: string; quantity: string; unitPrice: string; monthlyPrice: string; }

function CreateBudgetModal({ onClose, onCreated, isSuperAdmin, defaultTenantId }: {
  onClose: () => void;
  onCreated: () => void;
  isSuperAdmin: boolean;
  defaultTenantId?: string;
}) {
  const { toast } = useToast();
  const [tenantId, setTenantId] = useState(defaultTenantId ?? '');
  const [tenantSearch, setTenantSearch] = useState('');
  const [tenantDropdown, setTenantDropdown] = useState(false);
  const [tenantName, setTenantName] = useState('');
  const [notes, setNotes] = useState('');
  const [clientNotes, setClientNotes] = useState('');
  const [validUntil, setValidUntil] = useState('');
  const [lines, setLines] = useState<LineItem[]>([
    { description: '', quantity: '1', unitPrice: '', monthlyPrice: '' },
  ]);
  const [saving, setSaving] = useState(false);

  const { data: tenants } = useQuery({
    queryKey: ['tenants-search-create', tenantSearch],
    queryFn: () => listTenants({ search: tenantSearch || undefined, size: 30 }),
    enabled: isSuperAdmin,
  });

  const addLine = () => setLines(prev => [...prev, { description: '', quantity: '1', unitPrice: '', monthlyPrice: '' }]);
  const removeLine = (i: number) => setLines(prev => prev.filter((_, idx) => idx !== i));
  const updateLine = (i: number, field: keyof LineItem, val: string) =>
    setLines(prev => prev.map((l, idx) => idx === i ? { ...l, [field]: val } : l));

  const subtotal = lines.reduce((sum, l) => {
    const qty = parseFloat(l.quantity) || 1;
    const up = parseFloat(l.unitPrice) || 0;
    return sum + qty * up;
  }, 0);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const validLines = lines.filter(l => l.description.trim() && l.unitPrice.trim());
    if (validLines.length === 0) { toast('error', 'Afegeix almenys una línia'); return; }
    if (isSuperAdmin && !tenantId) { toast('error', 'Selecciona un client'); return; }
    setSaving(true);
    try {
      const customLines: CustomLineRequest[] = validLines.map(l => ({
        description: l.description.trim(),
        quantity: parseInt(l.quantity) || 1,
        unitPrice: parseFloat(l.unitPrice),
        monthlyPrice: l.monthlyPrice ? parseFloat(l.monthlyPrice) : undefined,
      }));
      await createBudget(tenantId, {
        customLines,
        notes: notes || undefined,
        clientNotes: clientNotes || undefined,
        validUntil: validUntil || undefined,
      } as CreateBudgetRequest);
      toast('success', 'Pressupost creat');
      onCreated();
      onClose();
    } catch {
      toast('error', 'Error creant el pressupost');
    } finally {
      setSaving(false);
    }
  };

  const labelCls = 'block f-mono text-label uppercase text-ink-2 text-xs mb-1';
  const inputCls = 'w-full px-3 py-2 text-sm bg-transparent border border-border-base text-ink-1 focus:outline-none focus:border-[#FF6B00]';

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-2xl max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
        <form onSubmit={handleSubmit}>
          <div className="p-5 border-b border-border-base flex items-center justify-between">
            <div className="f-display font-bold text-base">Nou pressupost</div>
            <button type="button" onClick={onClose} className="text-ink-2 hover:text-ink-0"><IconSet.X size={18} /></button>
          </div>
          <div className="p-5 space-y-5">
            {isSuperAdmin && (
              <div>
                <label className={labelCls}>Client *</label>
                <div className="relative">
                  <input
                    type="text"
                    placeholder="Cerca tenant..."
                    value={tenantName || tenantSearch}
                    onChange={e => { setTenantSearch(e.target.value); setTenantId(''); setTenantName(''); setTenantDropdown(true); }}
                    onFocus={() => setTenantDropdown(true)}
                    onBlur={() => setTimeout(() => setTenantDropdown(false), 150)}
                    className={inputCls}
                  />
                  {tenantDropdown && tenants && tenants.content.length > 0 && (
                    <div className="absolute top-full left-0 z-20 w-full bg-bg-0 border border-border-base shadow-xl max-h-40 overflow-y-auto">
                      {tenants.content.map((t: TenantResponse) => (
                        <button key={t.id} type="button" onMouseDown={() => { setTenantId(t.id); setTenantName(t.name); setTenantSearch(''); setTenantDropdown(false); }}
                          className="w-full text-left px-3 py-2 text-xs text-ink-1 hover:bg-accent-muted truncate block">
                          {t.name}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            )}

            <div>
              <div className="flex items-center justify-between mb-2">
                <label className={labelCls}>Línies *</label>
                <button type="button" onClick={addLine} className="f-mono text-label text-xs text-accent-light hover:underline">+ Afegir línia</button>
              </div>
              <div className="space-y-2">
                <div className="grid grid-cols-12 gap-2 f-mono text-label text-xs text-ink-3 uppercase px-1">
                  <span className="col-span-5">Descripció</span>
                  <span className="col-span-2 text-right">Qtty</span>
                  <span className="col-span-2 text-right">Preu setup</span>
                  <span className="col-span-2 text-right">Preu/mes</span>
                  <span className="col-span-1" />
                </div>
                {lines.map((line, i) => (
                  <div key={i} className="grid grid-cols-12 gap-2">
                    <input value={line.description} onChange={e => updateLine(i, 'description', e.target.value)}
                      placeholder="Descripció del servei" className={`col-span-5 ${inputCls}`} />
                    <input value={line.quantity} onChange={e => updateLine(i, 'quantity', e.target.value)}
                      type="number" min="1" className={`col-span-2 ${inputCls} text-right`} />
                    <input value={line.unitPrice} onChange={e => updateLine(i, 'unitPrice', e.target.value)}
                      type="number" min="0" step="0.01" placeholder="0.00" className={`col-span-2 ${inputCls} text-right`} />
                    <input value={line.monthlyPrice} onChange={e => updateLine(i, 'monthlyPrice', e.target.value)}
                      type="number" min="0" step="0.01" placeholder="0.00" className={`col-span-2 ${inputCls} text-right`} />
                    <button type="button" onClick={() => removeLine(i)} disabled={lines.length === 1}
                      className="col-span-1 flex items-center justify-center text-ink-3 hover:text-red-400 disabled:opacity-30">
                      <IconSet.X size={14} />
                    </button>
                  </div>
                ))}
              </div>
              <div className="mt-3 text-right f-mono text-sm">
                <span className="text-ink-2">Subtotal: </span>
                <span className="font-bold text-ink-0">{fmt(subtotal)}</span>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className={labelCls}>Notes internes</label>
                <textarea value={notes} onChange={e => setNotes(e.target.value)} rows={2} className={`${inputCls} resize-none`} />
              </div>
              <div>
                <label className={labelCls}>Notes per al client</label>
                <textarea value={clientNotes} onChange={e => setClientNotes(e.target.value)} rows={2} className={`${inputCls} resize-none`} />
              </div>
            </div>

            <div>
              <label className={labelCls}>Vàlid fins</label>
              <input type="date" value={validUntil} onChange={e => setValidUntil(e.target.value)} className={inputCls} />
            </div>
          </div>
          <div className="p-5 border-t border-border-base flex gap-3">
            <AMGButton type="submit" disabled={saving} loading={saving} className="flex-1 justify-center">Crear pressupost</AMGButton>
            <AMGButton type="button" variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
          </div>
        </form>
      </div>
    </div>
  );
}

// ── Budget detail / edit modal ────────────────────────────────────────────────

function BudgetDetailModal({ budget, onClose, onRefresh }: {
  budget: BudgetResponse;
  onClose: () => void;
  onRefresh: () => void;
}) {
  const { toast } = useToast();
  const qc = useQueryClient();
  const router = useRouter();
  const params = useParams();
  const locale = (params.locale as string) ?? 'ca';
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
      // Refresca les dades però NO tanca el modal (l'usuari ha de copiar l'URL)
      qc.invalidateQueries({ queryKey: ['budgets-all'] });
      qc.invalidateQueries({ queryKey: ['budgets'] });
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
    const win = window.open('', '_blank', 'width=860,height=1000');
    if (!win) return;

    const fmtEur = (n: number) =>
      new Intl.NumberFormat('ca-ES', { style: 'currency', currency: 'EUR' }).format(n);

    const phasesHtml = budget.phases.map(phase => `
      <div class="phase-block">
        <div class="phase-header">
          <span>${phase.name}</span>
          <span>${fmtEur(phase.phaseTotal)}</span>
        </div>
        <table class="lines-table">
          <thead>
            <tr>
              <th class="tl">Servei</th>
              <th class="tr">Setup</th>
              <th class="tr">Mensual</th>
            </tr>
          </thead>
          <tbody>
            ${phase.lines.map(l => `
              <tr>
                <td>${l.serviceName}</td>
                <td class="tr mono">${fmtEur(l.setupPrice)}</td>
                <td class="tr mono">${l.monthlyPrice > 0 ? fmtEur(l.monthlyPrice) + '/mes' : '—'}</td>
              </tr>`).join('')}
          </tbody>
        </table>
      </div>`).join('');

    const addonsHtml = budget.addons.length > 0 ? `
      <div class="section-label">Addons</div>
      <table class="lines-table">
        <tbody>
          ${budget.addons.map(a => `
            <tr>
              <td>${a.serviceName}</td>
              <td class="tr mono" colspan="2">${fmtEur(a.unitPrice)}</td>
            </tr>`).join('')}
        </tbody>
      </table>` : '';

    const customLinesHtml = (budget.customLines?.length ?? 0) > 0 ? `
      <div class="section-label">Serveis addicionals</div>
      <table class="lines-table">
        <thead><tr><th class="tl">Descripció</th><th class="tr">Qty</th><th class="tr">Preu u.</th><th class="tr">Mensual</th><th class="tr">Total</th></tr></thead>
        <tbody>
          ${(budget.customLines ?? []).map(cl => `
            <tr>
              <td>${cl.description}</td>
              <td class="tr mono">${cl.quantity}</td>
              <td class="tr mono">${fmtEur(cl.unitPrice)}</td>
              <td class="tr mono">${cl.monthlyPrice > 0 ? fmtEur(cl.monthlyPrice) : '—'}</td>
              <td class="tr mono bold">${fmtEur(cl.total)}</td>
            </tr>`).join('')}
        </tbody>
      </table>` : '';

    const discountRow = budget.discountTotal > 0
      ? `<tr><td>Descompte aplicat</td><td class="tr mono">−${fmtEur(budget.discountTotal)}</td></tr>` : '';

    const notesHtml = budget.clientNotes
      ? `<div class="notes-box"><div class="section-label" style="margin-bottom:6px">Observacions</div><p style="margin:0;font-size:13px;color:#444">${budget.clientNotes}</p></div>` : '';

    win.document.write(`<!DOCTYPE html>
<html lang="ca"><head>
<meta charset="utf-8">
<title>Pressupost ${budget.budgetNumber}${budget.tenantName ? ' — ' + budget.tenantName : ''}</title>
<style>
  @page { size: A4; margin: 18mm 16mm; }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: 'Helvetica Neue', Arial, sans-serif; color: #111; font-size: 13px; line-height: 1.5; }
  /* ── CAPÇALERA ── */
  .header { display: flex; justify-content: space-between; align-items: flex-start; padding-bottom: 20px; border-bottom: 3px solid #FF6B00; margin-bottom: 24px; }
  .brand { font-size: 22px; font-weight: 900; letter-spacing: -0.5px; color: #111; }
  .brand span { color: #FF6B00; }
  .brand-sub { font-size: 10px; color: #888; letter-spacing: .08em; text-transform: uppercase; margin-top: 2px; }
  .contact-block { text-align: right; font-size: 11px; color: #555; line-height: 1.7; }
  /* ── TÍTOL DOCUMENT ── */
  .doc-title { font-size: 18px; font-weight: 800; letter-spacing: -.3px; margin-bottom: 16px; }
  .meta-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; background: #f8f8f8; padding: 14px 16px; border-left: 3px solid #FF6B00; margin-bottom: 24px; }
  .meta-item { }
  .meta-label { font-size: 10px; text-transform: uppercase; letter-spacing: .08em; color: #999; font-weight: 700; }
  .meta-value { font-size: 13px; font-weight: 600; color: #111; }
  /* ── FASES ── */
  .section-label { font-size: 10px; text-transform: uppercase; letter-spacing: .08em; color: #999; font-weight: 700; margin: 20px 0 8px; }
  .phase-block { margin-bottom: 16px; border: 1px solid #e5e5e5; }
  .phase-header { display: flex; justify-content: space-between; background: #111; color: #fff; padding: 8px 14px; font-weight: 700; font-size: 13px; }
  .lines-table { width: 100%; border-collapse: collapse; }
  .lines-table th { font-size: 10px; text-transform: uppercase; letter-spacing: .06em; color: #888; font-weight: 700; padding: 6px 14px; border-bottom: 1px solid #eee; }
  .lines-table td { padding: 7px 14px; border-bottom: 1px solid #f0f0f0; font-size: 13px; color: #222; }
  .lines-table tr:last-child td { border-bottom: none; }
  .tl { text-align: left; }
  .tr { text-align: right; }
  .mono { font-variant-numeric: tabular-nums; }
  .bold { font-weight: 700; }
  /* ── TOTALS ── */
  .totals-box { border: 1px solid #e5e5e5; margin-top: 24px; }
  .totals-box table { width: 100%; border-collapse: collapse; }
  .totals-box td { padding: 8px 16px; font-size: 13px; }
  .totals-box tr:not(:last-child) td { border-bottom: 1px solid #f0f0f0; }
  .totals-box .total-setup { font-weight: 700; font-size: 15px; background: #111; color: #fff; }
  .totals-box .total-monthly { font-weight: 700; font-size: 14px; background: #FF6B00; color: #fff; }
  /* ── NOTES ── */
  .notes-box { border: 1px solid #e5e5e5; border-left: 3px solid #FF6B00; padding: 14px 16px; margin-top: 20px; }
  /* ── SIGNATURES ── */
  .signatures { display: grid; grid-template-columns: 1fr 1fr; gap: 40px; margin-top: 48px; padding-top: 12px; border-top: 1px solid #ddd; }
  .sig-block { }
  .sig-line { border-bottom: 1px solid #aaa; height: 36px; margin-bottom: 6px; }
  .sig-label { font-size: 10px; color: #888; text-transform: uppercase; letter-spacing: .06em; }
  /* ── PEU ── */
  .footer { margin-top: 32px; padding-top: 10px; border-top: 1px solid #e5e5e5; font-size: 10px; color: #aaa; display: flex; justify-content: space-between; }
  @media print { body { -webkit-print-color-adjust: exact; print-color-adjust: exact; } }
</style>
</head><body>

<div class="header">
  <div>
    <div class="brand">AMG<span>DL</span></div>
    <div class="brand-sub">Digitalització de negocis locals</div>
  </div>
  <div class="contact-block">
    amgdl.com · info@amgdl.com<br>
    +34 654 048 164 (WhatsApp)<br>
    +34 614 492 062 (Trucades)
  </div>
</div>

<div class="doc-title">Proposta de serveis digitals</div>

<div class="meta-grid">
  <div class="meta-item">
    <div class="meta-label">Número de pressupost</div>
    <div class="meta-value">${budget.budgetNumber}</div>
  </div>
  <div class="meta-item">
    <div class="meta-label">Data d'emissió</div>
    <div class="meta-value">${fmtDate(budget.createdAt)}</div>
  </div>
  <div class="meta-item">
    <div class="meta-label">Client</div>
    <div class="meta-value">${budget.tenantName ?? '—'}</div>
  </div>
  <div class="meta-item">
    <div class="meta-label">Vàlid fins</div>
    <div class="meta-value">${fmtDate(budget.validUntil)}</div>
  </div>
</div>

${budget.phases.length > 0 ? '<div class="section-label">Serveis contractats</div>' + phasesHtml : ''}
${addonsHtml}
${customLinesHtml}

<div class="totals-box">
  <table>
    <tr><td>Subtotal</td><td class="tr mono">${fmtEur(budget.subtotal)}</td></tr>
    ${discountRow}
    <tr class="total-setup"><td>Total d'inversió inicial (setup)</td><td class="tr mono">${fmtEur(budget.total)}</td></tr>
    <tr class="total-monthly"><td>Quota mensual recurrent</td><td class="tr mono">${fmtEur(budget.monthlyTotal ?? 0)}/mes</td></tr>
  </table>
</div>

${notesHtml}

<div class="signatures">
  <div class="sig-block">
    <div class="sig-line"></div>
    <div class="sig-label">AMG Digitalització · Data i signatura</div>
  </div>
  <div class="sig-block">
    <div class="sig-line"></div>
    <div class="sig-label">Client · Conforme i acceptació</div>
  </div>
</div>

<div class="footer">
  <span>AMGDL · Antoni Mas Gut · info@amgdl.com</span>
  <span>Pressupost ${budget.budgetNumber} · Emès ${fmtDate(budget.createdAt)}</span>
</div>

</body></html>`);
    win.document.close();
    win.focus();
    setTimeout(() => win.print(), 500);
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
                  {loadingSetup ? <span className="w-3.5 h-3.5 border border-[#FF6B00] border-t-transparent rounded-full animate-spin block" /> : <IconSet.Edit size={15} />}
                </button>
              )}
              <button title="Imprimir / PDF" onClick={handlePrint}
                className="p-1.5 rounded text-ink-2 hover:text-white hover:bg-[rgba(255,255,255,0.08)] transition">
                <IconSet.Download size={15} />
              </button>
              <button title="Clonar" onClick={handleClone} disabled={cloning}
                className="p-1.5 rounded text-ink-2 hover:text-white hover:bg-[rgba(255,255,255,0.08)] transition disabled:opacity-40">
                <IconSet.Copy size={15} />
              </button>
              {isDraft && mode === 'view' && (
                <button title="Enviar al client" onClick={handleSend} disabled={sending}
                  className="p-1.5 rounded text-ink-2 hover:text-white hover:bg-[rgba(255,255,255,0.08)] transition disabled:opacity-40">
                  <IconSet.ArrowRight size={15} />
                </button>
              )}
              <button title="Eliminar" onClick={handleCancel} disabled={cancelling}
                className="p-1.5 rounded text-red-400 hover:text-red-300 hover:bg-[rgba(239,68,68,0.12)] transition disabled:opacity-40">
                <IconSet.Trash size={15} />
              </button>
              <button onClick={mode === 'edit' ? () => setMode('view') : onClose}
                className="p-1.5 ml-1 rounded text-ink-2 hover:text-ink-0">
                <IconSet.X size={17} />
              </button>
            </div>
          </div>

          {mode === 'view' ? (
            <div className="p-5 space-y-5">

              {/* Enllaç d'acceptació */}
              {(budget.acceptanceUrl || acceptanceUrl) && (
                <div className="rounded-lg bg-green-500/10 border border-green-500/30 p-4 space-y-2">
                  <div className="text-green-400 text-xs font-semibold uppercase tracking-wider">Enllaç per al client</div>
                  <div className="flex items-center gap-2">
                    <input readOnly value={budget.acceptanceUrl ?? acceptanceUrl ?? ''}
                      className="flex-1 bg-[rgba(255,255,255,0.05)] border border-border-base rounded px-3 py-1.5 text-xs text-ink-1 f-mono truncate focus:outline-none" />
                    <button
                      onClick={() => { navigator.clipboard.writeText(budget.acceptanceUrl ?? acceptanceUrl ?? ''); toast('success', 'Enllaç copiat'); }}
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

              {budget.customLines && budget.customLines.length > 0 && (
                <div className="space-y-2">
                  <div className="f-display font-bold text-xs text-ink-3 uppercase tracking-wider">Línies</div>
                  <div className="rounded border border-border-base overflow-hidden">
                    <div className="grid grid-cols-12 gap-2 px-4 py-2 bg-[rgba(255,255,255,0.04)] f-mono text-label text-xs text-ink-3 uppercase">
                      <span className="col-span-5">Descripció</span>
                      <span className="col-span-1 text-right">Qty</span>
                      <span className="col-span-2 text-right">Preu u.</span>
                      <span className="col-span-2 text-right">Mensual</span>
                      <span className="col-span-2 text-right">Total</span>
                    </div>
                    <div className="divide-y divide-border-base">
                      {budget.customLines.map((cl, i) => (
                        <div key={i} className="grid grid-cols-12 gap-2 px-4 py-2">
                          <span className="col-span-5 text-xs text-ink-1">{cl.description}</span>
                          <span className="col-span-1 text-xs f-mono text-ink-2 text-right">{cl.quantity}</span>
                          <span className="col-span-2 text-xs f-mono text-ink-2 text-right">{cl.unitPrice.toFixed(2)} €</span>
                          <span className="col-span-2 text-xs f-mono text-ink-2 text-right">{cl.monthlyPrice > 0 ? `${cl.monthlyPrice.toFixed(2)} €` : '—'}</span>
                          <span className="col-span-2 text-xs f-mono text-white font-bold text-right">{cl.total.toFixed(2)} €</span>
                        </div>
                      ))}
                    </div>
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

              {/* Botó "Posar en marxa" — només per pressupostos acceptats amb tenant */}
              {budget.status === 'ACCEPTED' && budget.tenantId && (
                <div className="rounded border border-success/30 bg-success/5 p-4 space-y-3">
                  <div className="flex items-center gap-2">
                    <IconSet.Check size={13} stroke="#39d353" />
                    <span className="f-mono text-label text-xs text-success font-semibold uppercase tracking-wider">
                      Pressupost acceptat
                    </span>
                  </div>
                  <p className="f-mono text-label text-xs text-ink-2">
                    El client ha acceptat les fases. Ara cal configurar cada fase per activar els serveis contractats.
                  </p>
                  <AMGButton
                    size="sm"
                    variant="primary"
                    onClick={() => {
                      onClose();
                      router.push(`/${locale}/portal/admin/tenants/${budget.tenantId}/setup`);
                    }}
                  >
                    Posar en marxa →
                  </AMGButton>
                </div>
              )}
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

const PAGE_SIZE = 20;

export default function BillingPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [tenantFilter, setTenantFilter] = useState<string>('');
  const [tenantSearch, setTenantSearch] = useState('');
  const [tenantDropdown, setTenantDropdown] = useState(false);
  const [selectedBudget, setSelectedBudget] = useState<BudgetResponse | null>(null);
  const [showCreate, setShowCreate] = useState(false);

  const isAdmin = user?.role === 'SUPER_ADMIN' || user?.role === 'ADMIN';
  const isSuperAdmin = user?.role === 'SUPER_ADMIN';
  const tenantId = user?.tenantId ?? '';

  const { data: dashboard } = useQuery({
    queryKey: ['billing-dashboard', tenantId],
    queryFn: () => getBillingDashboard(tenantId),
    enabled: !!tenantId && !isAdmin,
  });

  const { data: tenants } = useQuery({
    queryKey: ['tenants-search', tenantSearch],
    queryFn: () => listTenants({ search: tenantSearch || undefined, size: 30 }),
    enabled: isAdmin,
  });

  const { data: budgetPage, isLoading, refetch: refetchBudgets } = useQuery({
    queryKey: isAdmin ? ['budgets-all', statusFilter, tenantFilter, page] : ['budgets', tenantId, statusFilter, page],
    queryFn: () => isAdmin
      ? listAllBudgets(statusFilter || undefined, tenantFilter || undefined, page, PAGE_SIZE)
      : listBudgets(tenantId, statusFilter || undefined, page, PAGE_SIZE),
    enabled: isAdmin || !!tenantId,
  });

  const handleRefresh = () => {
    refetchBudgets();
    qc.invalidateQueries({ queryKey: ['billing-dashboard'] });
  };

  const filtered = budgetPage?.content ?? [];
  const totalPages = budgetPage?.totalPages ?? 1;

  const selectedTenantName = tenants?.content?.find((t: TenantResponse) => t.id === tenantFilter)?.name ?? tenantSearch;

  return (
    <PortalShell breadcrumb="billing">
      <div className="p-4 sm:p-8 space-y-6">
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / billing /</span>
          <div className="f-display font-bold text-xl mt-1">Pressupostos i facturació</div>
        </div>

        {dashboard && (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
            <AMGStat label="Total gastat" value={fmt(dashboard.totalSpent)} icon={IconSet.CreditCard} tone="accent" />
            <AMGStat
              label="Pendents aprovació"
              value={String(dashboard.pendingBudgets)}
              icon={IconSet.Clock}
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
            <div className="flex gap-2 ml-auto flex-wrap items-center">
              {isAdmin && (
                <AMGButton size="sm" icon={IconSet.Plus} onClick={() => setShowCreate(true)}>
                  Nou pressupost
                </AMGButton>
              )}
              {isAdmin && (
                <div className="relative">
                  <input
                    type="text"
                    placeholder="Cerca client..."
                    value={tenantFilter ? (selectedTenantName || tenantSearch) : tenantSearch}
                    onChange={e => {
                      setTenantSearch(e.target.value);
                      setTenantFilter('');
                      setTenantDropdown(true);
                      setPage(0);
                    }}
                    onFocus={() => setTenantDropdown(true)}
                    onBlur={() => setTimeout(() => setTenantDropdown(false), 150)}
                    className="h-7 px-2 text-xs f-mono border border-border-base bg-transparent text-ink-1 placeholder-ink-3 focus:outline-none focus:border-[#FF6B00] w-36"
                  />
                  {tenantFilter && (
                    <button onClick={() => { setTenantFilter(''); setTenantSearch(''); setPage(0); }}
                      className="absolute right-1.5 top-1/2 -translate-y-1/2 text-ink-3 hover:text-ink-1">
                      <IconSet.X size={10} />
                    </button>
                  )}
                  {tenantDropdown && tenants && tenants.content.length > 0 && (
                    <div className="absolute top-8 left-0 z-20 w-56 bg-bg-0 border border-border-base shadow-xl">
                      {tenants.content.map((t: TenantResponse) => (
                        <button key={t.id} onMouseDown={() => { setTenantFilter(t.id); setTenantSearch(t.name); setTenantDropdown(false); setPage(0); }}
                          className="w-full text-left px-3 py-2 text-xs text-ink-1 hover:bg-accent-muted truncate block">
                          {t.name}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              )}
              {STATUS_FILTERS.map((s) => (
                <button key={s} onClick={() => { setStatusFilter(s); setPage(0); }}
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
              <IconSet.CreditCard size={28} stroke="#64748b" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Cap pressupost</div>
              <p className="text-ui text-ink-2">
                {isAdmin ? 'No hi ha pressupostos creats' : 'Contacta amb el teu tècnic per sol·licitar un pressupost'}
              </p>
            </div>
          ) : (
            <>
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

      {selectedBudget && (
        <BudgetDetailModal
          budget={selectedBudget}
          onClose={() => setSelectedBudget(null)}
          onRefresh={() => { handleRefresh(); setSelectedBudget(null); }}
        />
      )}

      {showCreate && (
        <CreateBudgetModal
          isSuperAdmin={isSuperAdmin}
          defaultTenantId={!isSuperAdmin ? tenantId : undefined}
          onClose={() => setShowCreate(false)}
          onCreated={() => { handleRefresh(); setShowCreate(false); }}
        />
      )}
    </PortalShell>
  );
}
