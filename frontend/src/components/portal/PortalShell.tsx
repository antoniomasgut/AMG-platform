'use client';

import { useAuth } from '@/lib/auth-context';
import { usePathname } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { I } from '@/components/ui/icons';
import { AMGLogo } from '@/components/ui/AMGLogo';
import { useState } from 'react';
import type { ReactNode } from 'react';

const LOCALES = ['ca', 'es', 'en', 'de'];

type NavItem = { label: string; icon: (p: { size?: number }) => ReactNode; href: string };
type NavGroup = { label: string; items: NavItem[] };
type T = ReturnType<typeof useTranslations<'portalNav'>>;

// ─────────────────────────────────────────────────────────────────────────────
// CLIENT: veu els seus serveis i el seu compte
// ─────────────────────────────────────────────────────────────────────────────
function clientGroups(t: T): NavGroup[] {
  return [
    {
      label: t('groups.summary'),
      items: [
        { label: t('items.dashboard'), icon: I.Dashboard, href: '/portal' },
      ],
    },
    {
      label: t('groups.myServices'),
      items: [
        { label: t('items.myWebs'), icon: I.Globe, href: '/portal/landings' },
        { label: t('items.automations'), icon: I.Zap, href: '/portal/automations' },
        { label: t('items.agentAI'), icon: I.Bot, href: '/portal/agents' },
        { label: t('items.inbox'), icon: I.Mail, href: '/portal/agents/inbox' },
        { label: t('items.files'), icon: I.Image, href: '/portal/assets' },
      ],
    },
    {
      label: t('groups.clientsLeads'),
      items: [
        { label: t('items.leadsCRM'), icon: I.Users, href: '/portal/leads' },
      ],
    },
    {
      label: t('groups.finances'),
      items: [
        { label: t('items.quotes'), icon: I.Receipt, href: '/portal/billing' },
        { label: t('items.invoices'), icon: I.Trending, href: '/portal/finops' },
        { label: t('items.payments'), icon: I.CreditCard, href: '/portal/payments' },
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
        { label: t('items.dashboard'), icon: I.Dashboard, href: '/portal' },
        { label: t('items.process'), icon: I.Flow, href: '/portal/process' },
      ],
    },
    {
      label: t('groups.acquisition'),
      items: [
        { label: t('items.prospecting'), icon: I.Search, href: '/portal/prospecting' },
        { label: t('items.leadsCRM'), icon: I.Users, href: '/portal/leads' },
      ],
    },
    {
      label: t('groups.commercial'),
      items: [
        { label: t('items.quotes'), icon: I.Receipt, href: '/portal/billing' },
        { label: t('items.payments'), icon: I.CreditCard, href: '/portal/payments' },
        { label: t('items.invoices'), icon: I.Trending, href: '/portal/finops' },
      ],
    },
    {
      label: t('groups.services'),
      items: [
        { label: t('items.landings'), icon: I.Globe, href: '/portal/landings' },
        { label: t('items.domains'), icon: I.Link, href: '/portal/admin/domains' },
        { label: t('items.assets'), icon: I.Image, href: '/portal/assets' },
        { label: t('items.automations'), icon: I.Zap, href: '/portal/automations' },
        { label: t('items.agentsAI'), icon: I.Bot, href: '/portal/agents' },
        { label: t('items.inbox'), icon: I.Mail, href: '/portal/agents/inbox' },
      ],
    },
    {
      label: t('groups.operations'),
      items: [
        { label: t('items.opsHealth'), icon: I.Activity, href: '/portal/ops' },
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
        { label: t('items.dashboard'), icon: I.Dashboard, href: '/portal' },
        { label: t('items.process'), icon: I.Flow, href: '/portal/process' },
      ],
    },
    {
      label: t('groups.acquisition'),
      items: [
        { label: t('items.prospecting'), icon: I.Search, href: '/portal/prospecting' },
        { label: t('items.leadsCRM'), icon: I.Users, href: '/portal/leads' },
      ],
    },
    {
      label: t('groups.commercial'),
      items: [
        { label: t('items.quotes'), icon: I.Receipt, href: '/portal/billing' },
        { label: t('items.payments'), icon: I.CreditCard, href: '/portal/payments' },
        { label: t('items.invoices'), icon: I.Trending, href: '/portal/finops' },
      ],
    },
    {
      label: t('groups.services'),
      items: [
        { label: t('items.landings'), icon: I.Globe, href: '/portal/landings' },
        { label: t('items.domains'), icon: I.Link, href: '/portal/admin/domains' },
        { label: t('items.assets'), icon: I.Image, href: '/portal/assets' },
        { label: t('items.automations'), icon: I.Zap, href: '/portal/automations' },
        { label: t('items.agentsAI'), icon: I.Bot, href: '/portal/agents' },
        { label: t('items.inbox'), icon: I.Mail, href: '/portal/agents/inbox' },
      ],
    },
    {
      label: t('groups.operations'),
      items: [
        { label: t('items.opsHealth'), icon: I.Activity, href: '/portal/ops' },
      ],
    },
    {
      label: t('groups.clients'),
      items: [
        { label: t('items.tenants'), icon: I.Building, href: '/portal/admin/tenants' },
        { label: t('items.users'), icon: I.Shield, href: '/portal/admin/users' },
        { label: t('items.programs'), icon: I.Sparkles, href: '/portal/billing/programs' },
      ],
    },
    {
      label: t('groups.system'),
      items: [
        { label: t('items.catalog'), icon: I.Box, href: '/portal/admin/vault' },
        { label: t('items.templates'), icon: I.Layers, href: '/portal/admin/templates' },
        { label: t('items.hosting'), icon: I.Globe, href: '/portal/admin/hosting' },
        { label: t('items.backup'), icon: I.Database, href: '/portal/admin/backup' },
        { label: t('items.infraOps'), icon: I.Server, href: '/portal/admin/infraops' },
        { label: t('items.apiKeys'), icon: I.Key, href: '/portal/admin/config' },
      ],
    },
  ];
}

// ─────────────────────────────────────────────────────────────────────────────
// Shell
// ─────────────────────────────────────────────────────────────────────────────
export function PortalShell({ children, breadcrumb, backHref }: { children: ReactNode; breadcrumb: string; backHref?: string }) {
  const { user, isSuperAdmin, isAdmin } = useAuth();
  const pathname = usePathname();
  const t = useTranslations('portalNav');
  const [mobileOpen, setMobileOpen] = useState(false);

  if (!user) return null;

  const initial = (user.name || user.email)[0].toUpperCase();
  const segs = pathname.split('/').filter(Boolean);
  const normalized = LOCALES.includes(segs[0]) ? '/' + segs.slice(1).join('/') : pathname;

  const isActive = (href: string) =>
    href === '/portal' ? normalized === '/portal' : normalized.startsWith(href);

  const groups = isSuperAdmin ? superAdminGroups(t) : isAdmin ? adminGroups(t) : clientGroups(t);

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
            {group.items.map(({ label, icon: Icon, href }) => {
              const active = isActive(href);
              return (
                <a
                  key={href}
                  href={href}
                  onClick={() => setMobileOpen(false)}
                  className={`relative flex items-center gap-3 px-4 h-9 f-mono text-xs uppercase tracking-wider transition-colors ${
                    active
                      ? 'bg-accent-muted text-accent-light'
                      : 'text-ink-1 hover:text-ink-0 hover:bg-[rgba(255,255,255,0.03)]'
                  }`}
                >
                  {active && <span className="absolute left-0 top-0 bottom-0 w-[2px] bg-[#FF6B00]" />}
                  <Icon size={13} />
                  {label}
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
        </div>
      </div>
    </>
  );

  return (
    <div className="flex w-full min-h-dvh bg-[#0d0d1a] overflow-hidden">

      {/* Desktop sidebar */}
      <aside
        aria-label="Navegació del portal"
        className="hidden lg:flex w-[210px] shrink-0 bg-[#13132a] border-r border-border-base flex-col"
      >
        <NavContent />
      </aside>

      {/* Mobile overlay */}
      {mobileOpen && (
        <div className="fixed inset-0 z-40 lg:hidden" onClick={() => setMobileOpen(false)}>
          <div className="absolute inset-0 bg-black/60" />
          <aside
            className="absolute left-0 top-0 bottom-0 w-[210px] bg-[#13132a] border-r border-border-base flex flex-col z-50"
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
            <I.Menu size={20} />
          </button>
          {backHref && (
            <a
              href={backHref}
              className="flex items-center gap-1 f-mono text-label text-ink-2 hover:text-accent-light transition-colors shrink-0"
            >
              <I.ArrowRight size={13} className="rotate-180" />
              Tornar
            </a>
          )}
          <span className="f-mono text-label uppercase text-accent-light tracking-widest text-xs">
            / portal / {breadcrumb} /
          </span>
        </div>
        <main aria-label="Contingut principal" className="flex-1 overflow-auto">
          {children}
        </main>
      </div>
    </div>
  );
}
