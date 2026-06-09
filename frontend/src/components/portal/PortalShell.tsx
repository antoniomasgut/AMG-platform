'use client';

import { useAuth } from '@/lib/auth-context';
import { useTenantFeatures } from '@/lib/tenant-features';
import { useTheme } from '@/lib/theme-context';
import { usePathname } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useQuery } from '@tanstack/react-query';
import { IconSet } from '@/components/ui/icons';
import { AMGLogo } from '@/components/ui/AMGLogo';
import { useState } from 'react';
import type { ReactNode } from 'react';
import { getPendingCount } from '@/services/agents-conversational';

const LOCALES = ['ca', 'es', 'en', 'de'];

type NavItem = { label: string; icon: (p: { size?: number }) => ReactNode; href: string; locked?: boolean };
type NavGroup = { label: string; items: NavItem[] };
type T = ReturnType<typeof useTranslations<'portalNav'>>;

// ─────────────────────────────────────────────────────────────────────────────
// CLIENT: veu els seus serveis i el seu compte
// ─────────────────────────────────────────────────────────────────────────────
function clientGroups(t: T, features: { canAccessAnalytics: boolean; canAccessDocuments: boolean; isLoading: boolean }): NavGroup[] {
  const analyticsLocked  = !features.isLoading && !features.canAccessAnalytics;
  const documentsLocked  = !features.isLoading && !features.canAccessDocuments;

  return [
    {
      label: t('groups.summary'),
      items: [
        { label: t('items.dashboard'), icon: IconSet.Dashboard, href: '/portal' },
      ],
    },
    {
      label: t('groups.myServices'),
      items: [
        { label: t('items.myWebs'), icon: IconSet.Globe, href: '/portal/landings' },
        { label: t('items.hosting'), icon: IconSet.Server, href: '/portal/hosting' },
        { label: t('items.automations'), icon: IconSet.Zap, href: '/portal/automations' },
        { label: t('items.agentAI'), icon: IconSet.Bot, href: '/portal/agents' },
        { label: t('items.inbox'), icon: IconSet.Mail, href: '/portal/agents/inbox' },
        { label: t('items.files'), icon: IconSet.Image, href: '/portal/assets' },
        { label: t('items.serveis'), icon: IconSet.Book, href: '/portal/serveis' },
      ],
    },
    {
      label: t('groups.clientsLeads'),
      items: [
        { label: t('items.leadsCRM'), icon: IconSet.Users, href: '/portal/leads' },
        { label: t('items.analytics'), icon: IconSet.Trending, href: '/portal/analytics', locked: analyticsLocked },
        { label: t('items.documents'), icon: IconSet.FileText, href: '/portal/documents', locked: documentsLocked },
      ],
    },
    {
      label: t('groups.finances'),
      items: [
        { label: t('items.quotes'), icon: IconSet.Receipt, href: '/portal/billing' },
        { label: t('items.invoices'), icon: IconSet.Trending, href: '/portal/finops' },
        { label: t('items.payments'), icon: IconSet.CreditCard, href: '/portal/payments' },
      ],
    },
  ];
}

// ─────────────────────────────────────────────────────────────────────────────
// ADMIN: gestiona el flux complet (captació → implementació) sense infra
// ─────────────────────────────────────────────────────────────────────────────
function adminGroups(t: T): NavGroup[] {
  return [
    {
      label: t('groups.overview'),
      items: [
        { label: t('items.dashboard'), icon: IconSet.Dashboard, href: '/portal' },
        { label: t('items.process'), icon: IconSet.Flow, href: '/portal/process' },
      ],
    },
    {
      label: t('groups.acquisition'),
      items: [
        { label: t('items.prospecting'), icon: IconSet.Search, href: '/portal/prospecting' },
        { label: t('items.leadsCRM'), icon: IconSet.Users, href: '/portal/leads' },
        { label: t('items.analytics'), icon: IconSet.Trending, href: '/portal/analytics' },
        { label: t('items.documents'), icon: IconSet.FileText, href: '/portal/documents' },
      ],
    },
    {
      label: t('groups.commercial'),
      items: [
        { label: t('items.quotes'), icon: IconSet.Receipt, href: '/portal/billing' },
        { label: t('items.payments'), icon: IconSet.CreditCard, href: '/portal/payments' },
        { label: t('items.invoices'), icon: IconSet.Trending, href: '/portal/finops' },
      ],
    },
    {
      label: t('groups.webServices'),
      items: [
        { label: t('items.landings'), icon: IconSet.Globe, href: '/portal/landings' },
        { label: t('items.domains'), icon: IconSet.Link, href: '/portal/admin/domains' },
        { label: t('items.assets'), icon: IconSet.Image, href: '/portal/assets' },
      ],
    },
    {
      label: t('groups.agentsAI'),
      items: [
        { label: t('items.agentsAI'), icon: IconSet.Bot, href: '/portal/agents' },
        { label: t('items.inbox'), icon: IconSet.Mail, href: '/portal/agents/inbox' },
        { label: t('items.automations'), icon: IconSet.Zap, href: '/portal/automations' },
        { label: t('items.integrations'), icon: IconSet.Key, href: '/portal/admin/integrations' },
      ],
    },
    {
      label: t('groups.operations'),
      items: [
        { label: t('items.opsHealth'), icon: IconSet.Activity, href: '/portal/ops' },
        { label: t('items.knowledge'), icon: IconSet.Book, href: '/portal/admin/knowledge' },
        { label: t('items.documentTemplates'), icon: IconSet.FileText, href: '/portal/admin/documents' },
        { label: t('items.generatedDocs'), icon: IconSet.Receipt, href: '/portal/admin/documents/list' },
        { label: t('items.meetings'), icon: IconSet.Calendar, href: '/portal/admin/booking' },
      ],
    },
  ];
}

// ─────────────────────────────────────────────────────────────────────────────
// SUPER_ADMIN: tot + configuració del sistema + infraestructura
// ─────────────────────────────────────────────────────────────────────────────
function superAdminGroups(t: T): NavGroup[] {
  return [
    {
      label: t('groups.overview'),
      items: [
        { label: t('items.dashboard'), icon: IconSet.Dashboard, href: '/portal' },
        { label: t('items.process'), icon: IconSet.Flow, href: '/portal/process' },
      ],
    },
    {
      label: t('groups.acquisition'),
      items: [
        { label: t('items.prospecting'), icon: IconSet.Search, href: '/portal/prospecting' },
        { label: t('items.leadsCRM'), icon: IconSet.Users, href: '/portal/leads' },
        { label: t('items.analytics'), icon: IconSet.Trending, href: '/portal/analytics' },
        { label: t('items.documents'), icon: IconSet.FileText, href: '/portal/documents' },
      ],
    },
    {
      label: t('groups.commercial'),
      items: [
        { label: t('items.quotes'), icon: IconSet.Receipt, href: '/portal/billing' },
        { label: t('items.payments'), icon: IconSet.CreditCard, href: '/portal/payments' },
        { label: t('items.invoices'), icon: IconSet.Trending, href: '/portal/finops' },
      ],
    },
    {
      label: t('groups.webServices'),
      items: [
        { label: t('items.landings'), icon: IconSet.Globe, href: '/portal/landings' },
        { label: t('items.domains'), icon: IconSet.Link, href: '/portal/admin/domains' },
        { label: t('items.assets'), icon: IconSet.Image, href: '/portal/assets' },
        { label: t('items.hosting'), icon: IconSet.Server, href: '/portal/admin/hosting' },
      ],
    },
    {
      label: t('groups.agentsAI'),
      items: [
        { label: t('items.agentsAI'), icon: IconSet.Bot, href: '/portal/agents' },
        { label: t('items.inbox'), icon: IconSet.Mail, href: '/portal/agents/inbox' },
        { label: t('items.automations'), icon: IconSet.Zap, href: '/portal/automations' },
        { label: t('items.integrations'), icon: IconSet.Key, href: '/portal/admin/integrations' },
      ],
    },
    {
      label: t('groups.operations'),
      items: [
        { label: t('items.opsHealth'), icon: IconSet.Activity, href: '/portal/ops' },
        { label: t('items.knowledge'), icon: IconSet.Book, href: '/portal/admin/knowledge' },
        { label: t('items.documentTemplates'), icon: IconSet.FileText, href: '/portal/admin/documents' },
        { label: t('items.generatedDocs'), icon: IconSet.Receipt, href: '/portal/admin/documents/list' },
        { label: t('items.meetings'), icon: IconSet.Calendar, href: '/portal/admin/booking' },
      ],
    },
    {
      label: t('groups.clients'),
      items: [
        { label: t('items.tenants'), icon: IconSet.Building, href: '/portal/admin/tenants' },
        { label: t('items.users'), icon: IconSet.Shield, href: '/portal/admin/users' },
        { label: t('items.programs'), icon: IconSet.Sparkles, href: '/portal/billing/programs' },
      ],
    },
    {
      label: t('groups.platform'),
      items: [
        { label: t('items.platformSummary'), icon: IconSet.Zap,       href: '/portal/platform' },
        { label: t('items.metaAds'),         icon: IconSet.Trending,  href: '/portal/platform/meta-ads' },
        { label: t('items.myBusiness'),      icon: IconSet.Briefcase, href: '/portal/my-business' },
        { label: t('items.config'),          icon: IconSet.Settings,  href: '/portal/platform/config' },
      ],
    },
    {
      label: t('groups.system'),
      items: [
        { label: t('items.catalog'), icon: IconSet.Box, href: '/portal/admin/vault' },
        { label: t('items.templates'), icon: IconSet.Layers, href: '/portal/admin/templates' },
        { label: t('items.backup'), icon: IconSet.Database, href: '/portal/admin/backup' },
        { label: t('items.infraOps'), icon: IconSet.Server, href: '/portal/admin/infraops' },
        { label: t('items.apiKeys'), icon: IconSet.Key, href: '/portal/admin/config' },
      ],
    },
  ];
}

// ─────────────────────────────────────────────────────────────────────────────
// Shell
// ─────────────────────────────────────────────────────────────────────────────
function ThemeToggleIcon() {
  const { theme, toggle } = useTheme();
  return (
    <button
      aria-label={theme === 'dark' ? 'Activar mode clar' : 'Activar mode fosc'}
      onClick={toggle}
      className="text-ink-2 hover:text-ink-0 transition-colors p-1 shrink-0"
    >
      {theme === 'dark' ? <IconSet.Sun size={18} /> : <IconSet.Moon size={18} />}
    </button>
  );
}

export function PortalShell({ children, breadcrumb, backHref }: { children: ReactNode; breadcrumb: string; backHref?: string }) {
  const { user, isSuperAdmin, isAdmin } = useAuth();
  const { theme, toggle } = useTheme();
  const features = useTenantFeatures();
  const pathname = usePathname();
  const t = useTranslations('portalNav');
  const [mobileOpen, setMobileOpen] = useState(false);

  const tenantId = user?.tenantId ?? null;
  const { data: pendingData } = useQuery({
    queryKey: ['pendingCount', tenantId],
    queryFn: () => getPendingCount(tenantId!),
    enabled: !!tenantId && (isAdmin || isSuperAdmin),
    refetchInterval: 30000,
  });
  const pendingCount = pendingData?.count ?? 0;

  if (!user) return null;

  const initial = (user.name || user.email)[0].toUpperCase();
  const segs = pathname.split('/').filter(Boolean);
  const normalized = LOCALES.includes(segs[0]) ? '/' + segs.slice(1).join('/') : pathname;

  const isActive = (href: string) =>
    href === '/portal' ? normalized === '/portal' : normalized.startsWith(href);

  const groups = isSuperAdmin ? superAdminGroups(t) : isAdmin ? adminGroups(t) : clientGroups(t, features);

  const NavContent = () => (
    <>
      <div className="h-14 border-b border-border-base flex items-center px-4 shrink-0">
        <a href="/portal" onClick={() => setMobileOpen(false)}>
          <AMGLogo className="h-8 w-auto" />
        </a>
      </div>

      <nav aria-label="Menú principal" className="flex-1 py-2 overflow-y-auto">
        {groups.map((group) => (
          <div key={group.label} className="mb-1">
            <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3 px-4 pt-3 pb-1">
              {group.label}
            </div>
            {group.items.map(({ label, icon: Icon, href, locked }) => {
              const active = isActive(href);
              return (
                <a
                  key={href}
                  href={href}
                  onClick={() => setMobileOpen(false)}
                  className={`relative flex items-center gap-3 px-4 h-9 f-mono text-xs uppercase tracking-wider transition-colors ${
                    active
                      ? 'bg-accent-muted text-accent-light'
                      : locked
                      ? 'text-ink-3 hover:text-ink-2 hover:bg-[rgba(255,255,255,0.02)]'
                      : 'text-ink-1 hover:text-ink-0 hover:bg-[rgba(255,255,255,0.03)]'
                  }`}
                >
                  {active && !locked && <span className="absolute left-0 top-0 bottom-0 w-[2px] bg-[#FF6B00]" />}
                  <Icon size={16} />
                  <span className="flex-1">{label}</span>
                  {href === '/portal/agents/inbox' && pendingCount > 0 && (
                    <span className="flex items-center justify-center min-w-[20px] h-[20px] px-1.5 rounded-full bg-[#FF6B00] text-[10px] font-bold text-white leading-none">
                      {pendingCount > 99 ? '99+' : pendingCount}
                    </span>
                  )}
                  {locked && <IconSet.Lock size={12} className="shrink-0" />}
                </a>
              );
            })}
          </div>
        ))}
      </nav>

      <div className="p-3 border-t border-border-base shrink-0">
        <div className="flex items-center gap-2.5">
          <div className="w-7 h-7 bg-gradient-to-br from-[#58a6ff] to-[#FF9A3C] btn-clip flex items-center justify-center text-black font-bold text-[10px] shrink-0">
            {initial}
          </div>
          <div className="flex-1 min-w-0">
            <div className="text-xs font-semibold truncate leading-tight">{user.name ?? ''}</div>
            <div className="f-mono text-[9px] text-ink-3 truncate leading-tight">
              {isSuperAdmin ? 'Super Admin' : isAdmin ? 'Admin' : 'Client'} · {user.email}
            </div>
          </div>
          <ThemeToggleIcon />
        </div>
      </div>
    </>
  );

  return (
    <div className="flex w-full min-h-dvh bg-[var(--surface-page)] overflow-hidden">

      {/* Desktop sidebar */}
      <aside
        aria-label="Navegació del portal"
        className="hidden lg:flex w-[210px] shrink-0 bg-[var(--surface-sidebar)] border-r border-border-base flex-col"
      >
        <NavContent />
      </aside>

      {/* Mobile overlay */}
      {mobileOpen && (
        <div className="fixed inset-0 z-40 lg:hidden" onClick={() => setMobileOpen(false)}>
          <div className="absolute inset-0 bg-black/60" />
          <aside
            className="absolute left-0 top-0 bottom-0 w-[210px] bg-[var(--surface-sidebar)] border-r border-border-base flex flex-col z-50"
            onClick={(e) => e.stopPropagation()}
          >
            <NavContent />
          </aside>
        </div>
      )}

      {/* Main content */}
      <div className="flex-1 flex flex-col min-w-0">
        <div className="h-14 border-b border-border-base flex items-center px-4 sm:px-6 gap-3 shrink-0">
          <button
            aria-label="Obrir menú"
            className="lg:hidden text-ink-1 hover:text-ink-0 transition-colors"
            onClick={() => setMobileOpen(true)}
          >
            <IconSet.Menu size={20} />
          </button>
          {backHref && (
            <a
              href={backHref}
              className="flex items-center gap-1 f-mono text-label text-ink-2 hover:text-accent-light transition-colors shrink-0"
            >
              <IconSet.ArrowRight size={16} className="rotate-180" />
              Tornar
            </a>
          )}
          <span className="f-mono text-label uppercase text-accent-light tracking-widest text-xs">
            / portal / {breadcrumb} /
          </span>
          <button
            aria-label={theme === 'dark' ? 'Activar mode clar' : 'Activar mode fosc'}
            onClick={toggle}
            className="ml-auto text-ink-2 hover:text-ink-0 transition-colors p-1"
          >
            {theme === 'dark' ? <IconSet.Sun size={18} /> : <IconSet.Moon size={18} />}
          </button>
        </div>
        <main aria-label="Contingut principal" className="flex-1 overflow-auto">
          {children}
        </main>
      </div>
    </div>
  );
}
