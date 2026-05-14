'use client';

import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '@/lib/auth-context';
import { useApiErrorHandler } from '@/lib/use-api-error';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGSectionTitle } from '@/components/ui/stat';
import { I } from '@/components/ui/icons';
import {
  fetchBillingDashboard, fetchInvoices, fetchLandings, fetchWorkflows,
  type BillingDashboard, type Invoice, type LandingSummary, type WorkflowSummary,
} from '@/services/dashboard';

/* ─────────── Skeleton ─────────── */
function Skeleton({ className = '' }: { className?: string }) {
  return <div className={`animate-pulse bg-[#212140] rounded ${className}`} />;
}

function DashboardSkeleton() {
  return (
    <div className="flex w-full min-h-dvh bg-[#0d0d1a] overflow-hidden">
      <aside className="hidden lg:flex w-[240px] shrink-0 bg-[#13132a] border-r border-[rgba(255,107,0,0.12)] flex-col p-4 space-y-4">
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-8 w-3/4" />
        <Skeleton className="h-8 w-3/4" />
        <Skeleton className="h-8 w-3/4" />
        <div className="flex-1" />
        <Skeleton className="h-14 w-full" />
      </aside>
      <main className="flex-1 p-8 space-y-6">
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-40 w-full" />
        <Skeleton className="h-48 w-full" />
      </main>
    </div>
  );
}

/* ─────────── Helpers ─────────── */
function formatDate(iso: string | null): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

function formatEur(cents: number | null): string {
  if (cents == null) return '€0,00';
  return new Intl.NumberFormat('ca-ES', { style: 'currency', currency: 'EUR' }).format(cents);
}

function daysUntil(dateStr: string | null): string {
  if (!dateStr) return '—';
  const diff = Math.ceil((new Date(dateStr).getTime() - Date.now()) / (1000 * 60 * 60 * 24));
  if (diff <= 0) return 'vençut';
  if (diff === 1) return 'demà';
  return `en ${diff} dies`;
}

const BADGE_TONE: Record<string, 'success' | 'warning' | 'error' | 'accent'> = {
  PAID: 'success', PAGAT: 'success', COMPLETED: 'success',
  PENDING: 'warning', OVERDUE: 'error', CANCELLED: 'error', FAILED: 'error',
  DRAFT: 'accent',
};

/* ─────────── Main ─────────── */
export default function PortalPage() {
  const { user, logout, isSuperAdmin } = useAuth();
  const handleApiError = useApiErrorHandler();
  const [loggingOut, setLoggingOut] = useState(false);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [billing, setBilling] = useState<BillingDashboard | null>(null);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [landings, setLandings] = useState<LandingSummary[]>([]);
  const [workflows, setWorkflows] = useState<WorkflowSummary[]>([]);

  const loadDashboard = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      const tid = user.tenantId!;
      const [bill, inv, lnd, wf] = await Promise.all([
        fetchBillingDashboard(tid).catch(() => null),
        fetchInvoices().catch(() => [] as Invoice[]),
        fetchLandings(tid).catch(() => [] as LandingSummary[]),
        fetchWorkflows(tid).catch(() => [] as WorkflowSummary[]),
      ]);
      setBilling(bill);
      setInvoices(inv);
      setLandings(lnd);
      setWorkflows(wf);
    } catch (err: any) {
      handleApiError(err, 'Dashboard');
      setError('No s\'ha pogut carregar el dashboard');
    } finally {
      setLoading(false);
    }
  }, [user, handleApiError]);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  const handleLogout = async () => {
    setLoggingOut(true);
    try { await logout(); } finally { window.location.href = '/login'; }
  };

  if (!user) return null;
  if (loading) return <DashboardSkeleton />;

  const initial = (user.name || user.email)[0].toUpperCase();
  const activeLandings = landings.filter(l => l.status === 'PUBLISHED' || l.status === 'ACTIVE').length;
  const activeWorkflows = workflows.filter(w => w.status === 'ACTIVE').length;
  const lastInvoice = invoices[0];

  const serviceCards = [
    { name: 'Landings', icon: I.Globe, used: activeLandings, total: landings.length || 1, unit: 'actives' },
    { name: 'Workflows', icon: I.Zap, used: activeWorkflows, total: workflows.length || 1, unit: 'actius' },
    { name: 'Factures', icon: I.Receipt, used: invoices.length, total: Math.max(invoices.length, 1), unit: 'emeses' },
    { name: 'Pressupostos', icon: I.CreditCard, used: billing?.pendingBudgets ?? 0, total: Math.max(billing?.pendingBudgets ?? 0, 1), unit: 'pendents' },
  ];

  return (
    <div className="flex w-full min-h-dvh bg-[#0d0d1a] overflow-hidden">
      {/* Sidebar */}
      <aside className="hidden lg:flex w-[240px] shrink-0 bg-[#13132a] border-r border-[rgba(255,107,0,0.12)] flex-col">
        <div className="h-16 border-b border-[rgba(255,107,0,0.12)] flex items-center px-5 gap-3">
          <div className="w-9 h-9 bg-[#FF6B00] btn-clip flex items-center justify-center shrink-0">
            <span className="f-display font-black text-black text-sm">A</span>
          </div>
          <div className="flex flex-col leading-tight">
            <span className="f-display font-bold text-sm">AMG</span>
            <span className="f-mono text-[9px] text-[#FF9A3C] tracking-[0.2em]">PORTAL · GROWTH</span>
          </div>
        </div>
        <nav className="flex-1 p-3 space-y-1">
          <div className="f-mono text-[9px] uppercase tracking-[0.2em] text-[#64748b] px-3 py-2">El meu compte</div>
          {([
            { label: 'Dashboard', icon: I.Dashboard, active: true, href: '/portal' },
            { label: 'Landings', icon: I.Globe, active: false, href: '/portal/landings' },
            { label: 'Serveis', icon: I.Box, active: false, href: '/portal' },
            { label: 'Factures', icon: I.Receipt, active: false, href: '/portal' },
            { label: 'Suport', icon: I.Bell, active: false, href: '/portal' },
            ...(isSuperAdmin ? [{ label: 'Admin', icon: I.Settings, active: false, href: '/portal' }] : []),
          ] as const).map(({ label, icon: Icon, active, href }) => (
            <a key={label} href={href}
              className={`relative flex items-center gap-3 px-3 h-10 f-mono text-xs uppercase tracking-wider cursor-pointer ${
                active ? 'bg-[rgba(255,107,0,0.10)] text-[#FF9A3C]' : 'text-[#94a3b8] hover:text-[#e2e8f0]'
              }`}>
              {active && <span className="absolute left-0 top-0 bottom-0 w-[2px] bg-[#FF6B00]"></span>}
              <Icon size={14} />
              {label}
            </a>
          ))}
        </nav>
        <div className="p-4 border-t border-[rgba(255,107,0,0.12)]">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 bg-gradient-to-br from-[#58a6ff] to-[#FF9A3C] btn-clip flex items-center justify-center text-black font-bold text-xs">{initial}</div>
            <div className="flex-1 min-w-0">
              <div className="text-sm font-semibold truncate">{user.name}</div>
              <div className="f-mono text-[10px] text-[#64748b] truncate">{user.email}</div>
            </div>
          </div>
        </div>
      </aside>

      {/* Main */}
      <main className="flex-1 flex flex-col min-w-0">
        <div className="h-16 border-b border-[rgba(255,107,0,0.12)] flex items-center px-4 sm:px-8 gap-4">
          <button className="lg:hidden text-[#94a3b8]"><I.Menu size={20} /></button>
          <div className="flex-1 hidden sm:block">
            <span className="f-mono text-[10px] uppercase text-[#FF9A3C] tracking-[0.2em]">/ portal /</span>
            <div className="f-display font-bold text-lg leading-tight mt-0.5">
              Bon dia, {user.name?.split(' ')[0] || 'usuari'}
            </div>
          </div>
          <AMGButton variant="outline" size="sm" icon={I.Globe}>VER LANDING</AMGButton>
          <AMGButton size="sm" icon={I.Bell}>SUPORT</AMGButton>
        </div>

        <div className="flex-1 overflow-auto amg-grid p-4 sm:p-8 space-y-6">
          {/* Error banner */}
          {error && (
            <div className="flex items-center gap-3 p-4 border-l-[3px] border-l-[#f0b429] amg-card card-clip">
              <I.AlertCircle size={16} stroke="#f0b429" />
              <span className="text-[13px] text-[#f0b429] flex-1">{error}</span>
              <button onClick={loadDashboard} className="f-mono text-[11px] uppercase text-[#FF9A3C] hover:underline">REINTENTAR</button>
            </div>
          )}

          {/* Billing hero */}
          <div className="amg-card card-clip p-4 sm:p-6 relative overflow-hidden">
            <div className="absolute top-0 right-0 w-[3px] h-16 bg-[#FF6B00]"></div>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 items-center">
              <div>
                <span className="f-mono text-[10px] uppercase tracking-[0.2em] text-[#FF9A3C]">Total gastat</span>
                <div className="f-display font-black text-2xl sm:text-3xl mt-1">{formatEur(billing?.totalSpent ?? 0)}</div>
                <div className="flex items-center gap-2 mt-2">
                  <AMGBadge tone={billing && billing.pendingBudgets > 0 ? 'warning' : 'success'}>
                    <span className={`w-1 h-1 rounded-full ${billing && billing.pendingBudgets > 0 ? 'bg-[#f0b429]' : 'bg-[#39d353]'}`}></span>
                    {billing && billing.pendingBudgets > 0 ? `${billing.pendingBudgets} pendents` : 'AL DIA'}
                  </AMGBadge>
                </div>
              </div>
              <div>
                <div className="f-mono text-[10px] uppercase text-[#64748b]">Últim pressupost</div>
                <div className="f-display font-bold text-lg sm:text-xl mt-1">
                  {billing?.lastBudget ? billing.lastBudget.budgetNumber : 'Cap'}
                </div>
                <div className="f-mono text-[11px] text-[#94a3b8] mt-0.5">
                  {billing?.lastBudget ? formatDate(billing.lastBudget.sentAt) : '—'}
                </div>
              </div>
              <div>
                <div className="f-mono text-[10px] uppercase text-[#64748b]">Import</div>
                <div className="f-display font-bold text-lg sm:text-xl mt-1 text-[#FF9A3C]">
                  {billing?.lastBudget ? formatEur(billing.lastBudget.total) : '€0'}
                </div>
                <div className="f-mono text-[11px] text-[#94a3b8] mt-0.5">
                  {billing?.lastBudget?.status?.toLowerCase() === 'accepted' ? 'Acceptat' : billing?.lastBudget?.status ?? '—'}
                </div>
              </div>
              <div>
                <div className="f-mono text-[10px] uppercase text-[#64748b]">Rol</div>
                <div className="flex items-center gap-2 mt-2">
                  <AMGBadge tone="accent">{user.role === 'SUPER_ADMIN' ? 'SUPER ADMIN' : user.role === 'ADMIN' ? 'ADMIN' : 'CLIENT'}</AMGBadge>
                </div>
              </div>
            </div>
          </div>

          {/* Services */}
          <div>
            <AMGSectionTitle eyebrow="Resum" title="Serveis">
              <span className="f-mono text-[11px] text-[#64748b] uppercase">{landings.length} landings · {workflows.length} workflows</span>
            </AMGSectionTitle>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {serviceCards.map((s, i) => {
                const Icon = s.icon;
                const pct = Math.min(Math.round((s.used / s.total) * 100), 100);
                const warn = pct > 80;
                return (
                  <div key={i} className="amg-card card-clip p-4 sm:p-5">
                    <div className="flex items-center gap-3 mb-4">
                      <div className="w-10 h-10 bg-[rgba(255,107,0,0.12)] border border-[rgba(255,107,0,0.35)] flex items-center justify-center">
                        <Icon size={16} stroke="#FF9A3C" />
                      </div>
                      <div className="flex-1">
                        <div className="f-display font-bold text-sm">{s.name.toUpperCase()}</div>
                        <div className="f-mono text-[10px] text-[#64748b] uppercase flex items-center gap-1.5">
                          <span className={`w-1.5 h-1.5 rounded-full ${s.used > 0 ? 'bg-[#39d353] amg-blink' : 'bg-[#64748b]'}`}></span>
                          {s.used > 0 ? 'OPERATIU' : 'SENSE DADES'}
                        </div>
                      </div>
                    </div>
                    <div className="flex items-baseline justify-between mb-1.5">
                      <span className="f-mono text-[11px] text-[#94a3b8] uppercase">{s.used} / {s.total} {s.unit}</span>
                      <span className={`f-mono text-[11px] ${warn ? 'text-[#f0b429]' : 'text-[#64748b]'}`}>{pct}%</span>
                    </div>
                    <div className="h-1.5 bg-[#212140] overflow-hidden">
                      <div className={`h-full ${warn ? 'bg-[#f0b429]' : 'bg-[#FF6B00]'}`} style={{ width: `${pct}%` }}></div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Invoices + CTA */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
            <div className="lg:col-span-2 amg-card card-clip p-4 sm:p-5">
              <AMGSectionTitle eyebrow="Historial" title="Últimes factures">
                <a className="f-mono text-[10px] uppercase text-[#FF9A3C] cursor-pointer">VEURE TOTES →</a>
              </AMGSectionTitle>
              {invoices.length === 0 ? (
                <div className="py-8 text-center">
                  <I.Receipt size={24} stroke="#64748b" className="mx-auto mb-2" />
                  <p className="f-mono text-[11px] text-[#64748b] uppercase">Cap factura encara</p>
                </div>
              ) : (
                <div className="space-y-0">
                  {invoices.slice(0, 5).map((inv, i) => (
                    <div key={inv.id}
                      className="grid grid-cols-[80px_1fr_80px_80px_24px] gap-2 sm:gap-3 px-2 h-11 items-center border-b border-[rgba(226,232,240,0.04)] text-sm last:border-b-0">
                      <span className="f-mono text-[#FF9A3C] text-xs">#{inv.invoiceNumber || '---'}</span>
                      <span className="f-mono text-[12px] text-[#94a3b8]">{formatDate(inv.createdAt)}</span>
                      <span className="f-mono text-[#e2e8f0]">{formatEur(inv.amount)}</span>
                      <span><AMGBadge tone={BADGE_TONE[inv.status] || 'accent'}>{inv.status}</AMGBadge></span>
                      <a href={inv.invoicePdfUrl || '#'} target="_blank" rel="noopener"
                        className={`text-[#94a3b8] hover:text-[#FF9A3C] ${!inv.invoicePdfUrl ? 'opacity-30 pointer-events-none' : ''}`}>
                        <I.Download size={12} />
                      </a>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="amg-card card-clip p-4 sm:p-5 flex flex-col">
              <I.Sparkles size={20} stroke="#FF9A3C" />
              <div className="f-display font-bold text-base mt-3">NECESSITES AJUDA?</div>
              <p className="text-[13px] text-[#94a3b8] mt-1 flex-1">
                El teu tècnic assignat està disponible per respondre els teus dubtes.
              </p>
              <div className="space-y-2 mt-4">
                <AMGButton size="sm" icon={I.Mail} className="w-full justify-center">ESCRIURE AL EQUIP</AMGButton>
                <AMGButton variant="outline" size="sm" icon={I.Play} className="w-full justify-center">VEURE TUTORIALS</AMGButton>
              </div>
            </div>
          </div>

          {/* Logout mobile */}
          <div className="flex justify-center pt-4 pb-8 lg:hidden">
            <AMGButton variant="ghost" onClick={handleLogout} disabled={loggingOut}>
              {loggingOut ? 'SORTINT...' : 'TANCAR SESSIÓ'}
            </AMGButton>
          </div>
        </div>
      </main>
    </div>
  );
}
