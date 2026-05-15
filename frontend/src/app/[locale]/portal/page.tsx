'use client';

import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '@/lib/auth-context';
import { useApiErrorHandler } from '@/lib/use-api-error';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGSectionTitle } from '@/components/ui/stat';
import { I } from '@/components/ui/icons';
import { AMGLogo } from '@/components/ui/AMGLogo';
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
      <aside className="hidden lg:flex w-[240px] shrink-0 bg-[#13132a] border-r border-border-base flex-col p-4 space-y-4">
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
  return new Date(iso).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

function formatEur(cents: number | null): string {
  if (cents == null) return '€0,00';
  return new Intl.NumberFormat('ca-ES', { style: 'currency', currency: 'EUR' }).format(cents);
}

const BADGE_TONE: Record<string, 'success' | 'warning' | 'danger' | 'accent'> = {
  PAID: 'success', PAGAT: 'success', COMPLETED: 'success',
  PENDING: 'warning', OVERDUE: 'danger', CANCELLED: 'danger', FAILED: 'danger',
  DRAFT: 'accent',
};

/* ─────────── PortalSidebar ─────────── */
interface SidebarProps {
  userName: string;
  userEmail: string;
  userRole: string;
  isSuperAdmin: boolean;
  isAdmin: boolean;
  initial: string;
}

function PortalSidebar({ userName, userEmail, userRole: _userRole, isSuperAdmin, isAdmin, initial }: SidebarProps) {
  return (
    <aside aria-label="Navegació del portal" className="hidden lg:flex w-[240px] shrink-0 bg-[#13132a] border-r border-border-base flex-col">
      <div className="h-16 border-b border-border-base flex items-center px-4">
        <a href="/portal">
          <AMGLogo className="h-9 w-auto" />
        </a>
      </div>
      <nav aria-label="Menú principal" className="flex-1 p-3 space-y-1 overflow-y-auto">
        <div className="f-mono text-[9px] uppercase tracking-widest text-ink-2 px-3 py-2">El meu compte</div>
        {([
          { label: 'Dashboard', icon: I.Dashboard, active: true, href: '/portal' },
          { label: 'Landings', icon: I.Globe, active: false, href: '/portal/landings' },
          { label: 'Billing', icon: I.CreditCard, active: false, href: '/portal/billing' },
          { label: 'FinOps', icon: I.Receipt, active: false, href: '/portal/finops' },
          { label: 'Automatitzacions', icon: I.Zap, active: false, href: '/portal/automations' },
          ...(isAdmin ? [{ label: 'Ops & Health', icon: I.Activity, active: false, href: '/portal/ops' }] : []),
          ...(isSuperAdmin ? [{ label: 'Admin', icon: I.Settings, active: false, href: '/portal/admin/users' }] : []),
        ]).map(({ label, icon: Icon, active, href }) => (
          <a key={label} href={href}
            className={`relative flex items-center gap-3 px-3 h-10 f-mono text-xs uppercase tracking-wider cursor-pointer ${
              active ? 'bg-accent-muted text-accent-light' : 'text-ink-1 hover:text-ink-0'
            }`}>
            {active && <span className="absolute left-0 top-0 bottom-0 w-[2px] bg-[#FF6B00]"></span>}
            <Icon size={14} />
            {label}
          </a>
        ))}
      </nav>
      <div className="p-4 border-t border-border-base">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 bg-gradient-to-br from-[#58a6ff] to-[#FF9A3C] btn-clip flex items-center justify-center text-black font-bold text-xs">{initial}</div>
          <div className="flex-1 min-w-0">
            <div className="text-sm font-semibold truncate">{userName}</div>
            <div className="f-mono text-label text-ink-2 truncate">{userEmail}</div>
          </div>
        </div>
      </div>
    </aside>
  );
}

/* ─────────── ServiceCards ─────────── */
interface ServiceCard {
  name: string;
  icon: React.FC<{ size?: number; stroke?: string }>;
  used: number;
  total: number;
  unit: string;
}

function ServiceCards({ cards, landingCount, workflowCount }: { cards: ServiceCard[]; landingCount: number; workflowCount: number }) {
  return (
    <div>
      <AMGSectionTitle eyebrow="Resum" title="Serveis">
        <span className="f-mono text-caption text-ink-2 uppercase">{landingCount} landings · {workflowCount} workflows</span>
      </AMGSectionTitle>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {cards.map((s, i) => {
          const Icon = s.icon;
          const pct = Math.min(Math.round((s.used / s.total) * 100), 100);
          const warn = pct > 80;
          return (
            <div key={i} className="amg-card card-clip p-4 sm:p-5">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-10 h-10 bg-accent-muted border border-border-strong flex items-center justify-center">
                  <Icon size={16} stroke="#FF9A3C" />
                </div>
                <div className="flex-1">
                  <div className="f-display font-bold text-sm">{s.name.toUpperCase()}</div>
                  <div className="f-mono text-label text-ink-2 uppercase flex items-center gap-1.5">
                    <span className={`w-1.5 h-1.5 rounded-full ${s.used > 0 ? 'bg-[#39d353] amg-blink' : 'bg-[#8896aa]'}`}></span>
                    {s.used > 0 ? 'OPERATIU' : 'SENSE DADES'}
                  </div>
                </div>
              </div>
              <div className="flex items-baseline justify-between mb-1.5">
                <span className="f-mono text-caption text-ink-1 uppercase">{s.used} / {s.total} {s.unit}</span>
                <span className={`f-mono text-caption ${warn ? 'text-warning' : 'text-ink-2'}`}>{pct}%</span>
              </div>
              <div className="h-1.5 bg-[#212140] overflow-hidden">
                <div className={`h-full ${warn ? 'bg-[#f0b429]' : 'bg-[#FF6B00]'}`} style={{ width: `${pct}%` }}></div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

/* ─────────── InvoicesTable ─────────── */
function InvoicesTable({ invoices }: { invoices: Invoice[] }) {
  return (
    <div className="amg-card card-clip p-4 sm:p-5">
      <AMGSectionTitle eyebrow="Historial" title="Últimes factures">
        <a className="f-mono text-label uppercase text-accent-light cursor-pointer">VEURE TOTES →</a>
      </AMGSectionTitle>
      {invoices.length === 0 ? (
        <div className="py-8 text-center">
          <I.Receipt size={24} stroke="#8896aa" className="mx-auto mb-2" />
          <p className="f-mono text-caption text-ink-2 uppercase">Cap factura encara</p>
        </div>
      ) : (
        <div className="overflow-x-auto -mx-4 sm:mx-0">
          <div className="min-w-[340px] space-y-0">
            {invoices.slice(0, 5).map((inv) => (
              <div key={inv.id}
                className="grid grid-cols-[1fr_72px_24px] sm:grid-cols-[80px_1fr_80px_80px_24px] gap-2 sm:gap-3 px-4 sm:px-2 h-11 items-center border-b border-[rgba(226,232,240,0.04)] text-sm last:border-b-0">
                <span className="hidden sm:block f-mono text-accent-light text-xs">#{inv.invoiceNumber || '---'}</span>
                <span className="f-mono text-data text-ink-1">{formatDate(inv.createdAt)}</span>
                <span className="f-mono text-ink-0">{formatEur(inv.amount)}</span>
                <span className="hidden sm:block"><AMGBadge tone={BADGE_TONE[inv.status] || 'accent'}>{inv.status}</AMGBadge></span>
                <a href={inv.invoicePdfUrl || '#'} target="_blank" rel="noopener"
                  className={`text-ink-1 hover:text-accent-light ${!inv.invoicePdfUrl ? 'opacity-30 pointer-events-none' : ''}`}>
                  <I.Download size={12} />
                </a>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

/* ─────────── OnboardingGuide ─────────── */
interface OnboardingGuideProps {
  userName: string;
  landingCount: number;
  workflowCount: number;
  invoiceCount: number;
  onSkip: () => void;
}

function OnboardingGuide({ userName, landingCount, workflowCount, invoiceCount, onSkip }: OnboardingGuideProps) {
  const steps = [
    {
      num: 1,
      title: 'Crea la teva primera landing',
      desc: "Publica la teva web en menys de 5 minuts amb l'editor visual.",
      href: '/portal/landings/new',
      done: landingCount > 0,
    },
    {
      num: 2,
      title: 'Connecta una automatització',
      desc: 'Connecta n8n per enviar emails, WhatsApp o rebre notificacions.',
      href: '/portal/automations',
      done: workflowCount > 0,
    },
    {
      num: 3,
      title: 'Genera el teu primer pressupost',
      desc: "Crea un pressupost personalitzat i envia'l al client en PDF.",
      href: '/portal/billing/new',
      done: invoiceCount > 0,
    },
  ];

  const completedCount = steps.filter((s) => s.done).length;

  if (completedCount === 3) {
    return (
      <div className="amg-card card-clip p-8 text-center animate-slide-up" role="region" aria-label="Guia d'inici completada">
        <div className="w-16 h-16 bg-emerald-900/30 border border-emerald-500/40 flex items-center justify-center mx-auto mb-4">
          <I.Check size={28} stroke="#4ade80" />
        </div>
        <div className="f-display font-bold text-2xl mb-2">ESTÀS A PUNT!</div>
        <p className="text-ink-2 text-sm">Ja tens tot configurat. El teu portal està llest.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-slide-up" role="region" aria-label="Guia d'inici">
      <div>
        <div className="f-mono text-label uppercase tracking-widest text-accent-light mb-1">PRIMERS PASSOS</div>
        <div className="f-display font-bold text-xl">Benvingut, {userName}! Comencem.</div>
        <p className="text-ink-2 text-sm mt-1">Completa els 3 passos per treure el màxim profit del portal.</p>
      </div>

      <div>
        <div className="flex justify-between f-mono text-label uppercase text-ink-2 mb-1.5">
          <span>Progrés</span>
          <span>{completedCount} / 3 passos</span>
        </div>
        <div className="h-1.5 bg-[#212140] overflow-hidden">
          <div
            className="h-full bg-gradient-to-r from-[#FF6B00] to-[#FF9A3C] transition-all duration-500"
            style={{ width: `${Math.round((completedCount / 3) * 100)}%` }}
          />
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {steps.map((step) => (
          <a
            key={step.num}
            href={step.done ? undefined : step.href}
            aria-label={`Pas ${step.num}: ${step.title} — ${step.done ? 'completat' : 'pendent'}`}
            className={`amg-card card-clip p-5 flex flex-col gap-3 transition-all ${
              step.done
                ? 'border-emerald-500/40 opacity-70 pointer-events-none'
                : 'hover:border-[#FF6B00]/60 cursor-pointer'
            }`}
          >
            <div className="flex items-center gap-3">
              <div className={`w-8 h-8 flex items-center justify-center text-sm font-bold f-mono ${
                step.done
                  ? 'bg-emerald-900/40 text-emerald-400 border border-emerald-500/40'
                  : 'bg-accent-muted text-accent-light border border-border-strong'
              }`}>
                {step.done ? <I.Check size={14} stroke="#4ade80" /> : step.num}
              </div>
              <span className="f-display font-bold text-sm">{step.title.toUpperCase()}</span>
            </div>
            <p className="text-data text-ink-2">{step.desc}</p>
            {!step.done && (
              <span className="f-mono text-label uppercase text-accent-light mt-auto">COMENÇA →</span>
            )}
          </a>
        ))}
      </div>

      <div className="flex justify-end">
        <button
          onClick={onSkip}
          aria-label="Salta la guia d'inici i ves al dashboard"
          className="f-mono text-label uppercase text-ink-2 hover:text-ink-0 transition"
        >
          Salta l&#39;onboarding
        </button>
      </div>
    </div>
  );
}

/* ─────────── Main ─────────── */
export default function PortalPage() {
  const { user, logout, isSuperAdmin, isAdmin } = useAuth();
  const handleApiError = useApiErrorHandler();
  const [loggingOut, setLoggingOut] = useState(false);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [billing, setBilling] = useState<BillingDashboard | null>(null);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [landings, setLandings] = useState<LandingSummary[]>([]);
  const [workflows, setWorkflows] = useState<WorkflowSummary[]>([]);

  const [onboardingSkipped, setOnboardingSkipped] = useState(false);

  useEffect(() => {
    if (user?.tenantId) {
      setOnboardingSkipped(
        localStorage.getItem(`amg_onboarding_skipped_${user.tenantId}`) === 'true'
      );
    }
  }, [user?.tenantId]);

  const loadDashboard = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      const tid = user.tenantId;
      const [bill, inv, lnd, wf] = await Promise.all([
        tid ? fetchBillingDashboard(tid).catch(() => null) : Promise.resolve(null),
        fetchInvoices().catch(() => [] as Invoice[]),
        tid ? fetchLandings(tid).catch(() => [] as LandingSummary[]) : Promise.resolve([]),
        tid ? fetchWorkflows(tid).catch(() => [] as WorkflowSummary[]) : Promise.resolve([]),
      ]);
      setBilling(bill);
      setInvoices(inv);
      setLandings(lnd);
      setWorkflows(wf);
    } catch (err: unknown) {
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

  const handleSkipOnboarding = () => {
    if (user?.tenantId) {
      localStorage.setItem(`amg_onboarding_skipped_${user.tenantId}`, 'true');
    }
    setOnboardingSkipped(true);
  };

  if (!user) return null;
  if (loading) return <DashboardSkeleton />;

  const initial = (user.name || user.email)[0].toUpperCase();
  const activeLandings = landings.filter((l) => l.status === 'PUBLISHED' || l.status === 'ACTIVE').length;
  const activeWorkflows = workflows.filter((w) => w.status === 'ACTIVE').length;

  const serviceCards: ServiceCard[] = [
    { name: 'Landings', icon: I.Globe, used: activeLandings, total: landings.length || 1, unit: 'actives' },
    { name: 'Workflows', icon: I.Zap, used: activeWorkflows, total: workflows.length || 1, unit: 'actius' },
    { name: 'Factures', icon: I.Receipt, used: invoices.length, total: Math.max(invoices.length, 1), unit: 'emeses' },
    { name: 'Pressupostos', icon: I.CreditCard, used: billing?.pendingBudgets ?? 0, total: Math.max(billing?.pendingBudgets ?? 0, 1), unit: 'pendents' },
  ];

  const showOnboarding =
    !onboardingSkipped &&
    landings.length === 0 &&
    workflows.length === 0 &&
    invoices.length === 0;

  return (
    <div className="flex w-full min-h-dvh bg-[#0d0d1a] overflow-hidden">
      <PortalSidebar
        userName={user.name ?? ''}
        userEmail={user.email}
        userRole={user.role}
        isSuperAdmin={isSuperAdmin}
        isAdmin={isAdmin}
        initial={initial}
      />

      <main aria-label="Contingut principal" className="flex-1 flex flex-col min-w-0">
        {/* Topbar */}
        <div className="h-16 border-b border-border-base flex items-center px-4 sm:px-8 gap-4">
          <button aria-label="Obrir menú" className="lg:hidden text-ink-1"><I.Menu size={20} /></button>
          <div className="flex-1 hidden sm:block">
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal /</span>
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
              <span className="text-ui text-warning flex-1">{error}</span>
              <button onClick={loadDashboard} className="f-mono text-caption uppercase text-accent-light hover:underline">REINTENTAR</button>
            </div>
          )}

          {showOnboarding ? (
            <OnboardingGuide
              userName={user.name?.split(' ')[0] || 'usuari'}
              landingCount={landings.length}
              workflowCount={workflows.length}
              invoiceCount={invoices.length}
              onSkip={handleSkipOnboarding}
            />
          ) : (
            <>
              {/* Billing hero */}
              <div className="amg-card card-clip p-4 sm:p-6 relative overflow-hidden">
                <div className="absolute top-0 right-0 w-[3px] h-16 bg-[#FF6B00]"></div>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 items-center">
                  <div>
                    <span className="f-mono text-label uppercase tracking-widest text-accent-light">Total gastat</span>
                    <div className="f-display font-black text-2xl sm:text-3xl mt-1">{formatEur(billing?.totalSpent ?? 0)}</div>
                    <div className="flex items-center gap-2 mt-2">
                      <AMGBadge tone={billing && billing.pendingBudgets > 0 ? 'warning' : 'success'}>
                        <span className={`w-1 h-1 rounded-full ${billing && billing.pendingBudgets > 0 ? 'bg-[#f0b429]' : 'bg-[#39d353]'}`}></span>
                        {billing && billing.pendingBudgets > 0 ? `${billing.pendingBudgets} pendents` : 'AL DIA'}
                      </AMGBadge>
                    </div>
                  </div>
                  <div>
                    <div className="f-mono text-label uppercase text-ink-2">Últim pressupost</div>
                    <div className="f-display font-bold text-lg sm:text-xl mt-1">
                      {billing?.lastBudget ? billing.lastBudget.budgetNumber : 'Cap'}
                    </div>
                    <div className="f-mono text-caption text-ink-1 mt-0.5">
                      {billing?.lastBudget ? formatDate(billing.lastBudget.sentAt) : '—'}
                    </div>
                  </div>
                  <div>
                    <div className="f-mono text-label uppercase text-ink-2">Import</div>
                    <div className="f-display font-bold text-lg sm:text-xl mt-1 text-accent-light">
                      {billing?.lastBudget ? formatEur(billing.lastBudget.total) : '€0'}
                    </div>
                    <div className="f-mono text-caption text-ink-1 mt-0.5">
                      {billing?.lastBudget?.status?.toLowerCase() === 'accepted' ? 'Acceptat' : billing?.lastBudget?.status ?? '—'}
                    </div>
                  </div>
                  <div>
                    <div className="f-mono text-label uppercase text-ink-2">Rol</div>
                    <div className="flex items-center gap-2 mt-2">
                      <AMGBadge tone="accent">{user.role === 'SUPER_ADMIN' ? 'SUPER ADMIN' : user.role === 'ADMIN' ? 'ADMIN' : 'CLIENT'}</AMGBadge>
                    </div>
                  </div>
                </div>
              </div>

              <ServiceCards cards={serviceCards} landingCount={landings.length} workflowCount={workflows.length} />

              {/* Invoices + CTA */}
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
                <div className="lg:col-span-2">
                  <InvoicesTable invoices={invoices} />
                </div>
                <div className="amg-card card-clip p-4 sm:p-5 flex flex-col">
                  <I.Sparkles size={20} stroke="#FF9A3C" />
                  <div className="f-display font-bold text-base mt-3">NECESSITES AJUDA?</div>
                  <p className="text-ui text-ink-1 mt-1 flex-1">
                    El teu tècnic assignat està disponible per respondre els teus dubtes.
                  </p>
                  <div className="space-y-2 mt-4">
                    <AMGButton size="sm" icon={I.Mail} className="w-full justify-center">ESCRIURE AL EQUIP</AMGButton>
                    <AMGButton variant="outline" size="sm" icon={I.Play} className="w-full justify-center">VEURE TUTORIALS</AMGButton>
                  </div>
                </div>
              </div>
            </>
          )}

          {/* Logout mobile */}
          <div className="flex justify-center pt-4 lg:hidden">
            <AMGButton variant="ghost" onClick={handleLogout} disabled={loggingOut}>
              {loggingOut ? 'SORTINT...' : 'TANCAR SESSIÓ'}
            </AMGButton>
          </div>

          {/* Legal footer */}
          <div className="flex flex-wrap items-center justify-center gap-x-4 gap-y-1 pt-6 pb-8 border-t border-border-subtle">
            {[
              { label: 'Avís Legal', href: '/legal/avis-legal' },
              { label: 'Privacitat', href: '/legal/privacitat' },
              { label: 'Cookies', href: '/legal/cookies' },
              { label: 'Suport', href: 'mailto:hola@amg.digital' },
            ].map(({ label, href }) => (
              <a
                key={label}
                href={href}
                className="f-mono text-label text-ink-3 hover:text-ink-2 tracking-caption transition-colors"
              >
                {label.toUpperCase()}
              </a>
            ))}
            <span className="f-mono text-label text-ink-3 tracking-caption ml-auto hidden sm:block">
              © {new Date().getFullYear()} AMG DIGITALITZACIÓ
            </span>
          </div>
        </div>
      </main>
    </div>
  );
}
