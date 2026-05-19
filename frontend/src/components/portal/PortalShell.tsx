'use client';

import { useAuth } from '@/lib/auth-context';
import { usePathname } from 'next/navigation';
import { I } from '@/components/ui/icons';
import { AMGLogo } from '@/components/ui/AMGLogo';
import { useState } from 'react';
import type { ReactNode } from 'react';

const LOCALES = ['ca', 'es', 'en', 'de'];

type NavItem = { label: string; icon: (p: { size?: number }) => ReactNode; href: string };
type NavGroup = { label: string; items: NavItem[] };

// ─────────────────────────────────────────────────────────────────────────────
// CLIENT: veu els seus serveis i el seu compte
// ─────────────────────────────────────────────────────────────────────────────
function clientGroups(): NavGroup[] {
  return [
    {
      label: 'Resum',
      items: [
        { label: 'Dashboard', icon: I.Dashboard, href: '/portal' },
      ],
    },
    {
      label: 'Els meus serveis',
      items: [
        { label: 'Les meves webs', icon: I.Globe, href: '/portal/landings' },
        { label: 'Automatitzacions', icon: I.Zap, href: '/portal/automations' },
        { label: 'Agent IA', icon: I.Bot, href: '/portal/agents' },
        { label: 'Arxius', icon: I.Image, href: '/portal/assets' },
      ],
    },
    {
      label: 'Clients i leads',
      items: [
        { label: 'Leads CRM', icon: I.Users, href: '/portal/leads' },
      ],
    },
    {
      label: 'Finances',
      items: [
        { label: 'Pressupostos', icon: I.Receipt, href: '/portal/billing' },
        { label: 'Factures', icon: I.Trending, href: '/portal/finops' },
        { label: 'Pagaments', icon: I.CreditCard, href: '/portal/payments' },
      ],
    },
  ];
}

// ─────────────────────────────────────────────────────────────────────────────
// ADMIN: gestiona el flux complet (captació → implementació) sense infra
// ─────────────────────────────────────────────────────────────────────────────
function adminGroups(): NavGroup[] {
  return [
    {
      label: 'Visió general',
      items: [
        { label: 'Dashboard', icon: I.Dashboard, href: '/portal' },
        { label: 'Procés', icon: I.Flow, href: '/portal/process' },
      ],
    },
    {
      label: '1. Captació',
      items: [
        { label: 'Prospecció', icon: I.Search, href: '/portal/prospecting' },
        { label: 'Leads CRM', icon: I.Users, href: '/portal/leads' },
      ],
    },
    {
      label: '2. Comercial',
      items: [
        { label: 'Pressupostos', icon: I.Receipt, href: '/portal/billing' },
        { label: 'Pagaments', icon: I.CreditCard, href: '/portal/payments' },
        { label: 'Factures', icon: I.Trending, href: '/portal/finops' },
      ],
    },
    {
      label: '3. Serveis',
      items: [
        { label: 'Landings', icon: I.Globe, href: '/portal/landings' },
        { label: 'Assets', icon: I.Image, href: '/portal/assets' },
        { label: 'Automatitzacions', icon: I.Zap, href: '/portal/automations' },
        { label: 'Agents IA', icon: I.Bot, href: '/portal/agents' },
      ],
    },
    {
      label: '4. Operacions',
      items: [
        { label: 'Ops & Health', icon: I.Activity, href: '/portal/ops' },
      ],
    },
  ];
}

// ─────────────────────────────────────────────────────────────────────────────
// SUPER_ADMIN: tot + configuració del sistema + infraestructura
// ─────────────────────────────────────────────────────────────────────────────
function superAdminGroups(): NavGroup[] {
  return [
    {
      label: 'Visió general',
      items: [
        { label: 'Dashboard', icon: I.Dashboard, href: '/portal' },
        { label: 'Procés', icon: I.Flow, href: '/portal/process' },
      ],
    },
    {
      label: '1. Captació',
      items: [
        { label: 'Prospecció', icon: I.Search, href: '/portal/prospecting' },
        { label: 'Leads CRM', icon: I.Users, href: '/portal/leads' },
      ],
    },
    {
      label: '2. Comercial',
      items: [
        { label: 'Pressupostos', icon: I.Receipt, href: '/portal/billing' },
        { label: 'Pagaments', icon: I.CreditCard, href: '/portal/payments' },
        { label: 'Factures', icon: I.Trending, href: '/portal/finops' },
      ],
    },
    {
      label: '3. Serveis',
      items: [
        { label: 'Landings', icon: I.Globe, href: '/portal/landings' },
        { label: 'Assets', icon: I.Image, href: '/portal/assets' },
        { label: 'Automatitzacions', icon: I.Zap, href: '/portal/automations' },
        { label: 'Agents IA', icon: I.Bot, href: '/portal/agents' },
      ],
    },
    {
      label: '4. Operacions',
      items: [
        { label: 'Ops & Health', icon: I.Activity, href: '/portal/ops' },
      ],
    },
    {
      label: 'Clients',
      items: [
        { label: 'Tenants', icon: I.Building, href: '/portal/admin/tenants' },
        { label: 'Usuaris', icon: I.Shield, href: '/portal/admin/users' },
        { label: 'Programes', icon: I.Sparkles, href: '/portal/billing/programs' },
      ],
    },
    {
      label: 'Sistema',
      items: [
        { label: 'Catàleg', icon: I.Box, href: '/portal/admin/vault' },
        { label: 'Plantilles', icon: I.Layers, href: '/portal/admin/templates' },
        { label: 'Backup', icon: I.Database, href: '/portal/admin/backup' },
        { label: 'InfraOps', icon: I.Server, href: '/portal/admin/infraops' },
        { label: 'API Keys', icon: I.Key, href: '/portal/admin/config' },
      ],
    },
  ];
}

// ─────────────────────────────────────────────────────────────────────────────
// Shell
// ─────────────────────────────────────────────────────────────────────────────
export function PortalShell({ children, breadcrumb }: { children: ReactNode; breadcrumb: string }) {
  const { user, isSuperAdmin, isAdmin } = useAuth();
  const pathname = usePathname();
  const [mobileOpen, setMobileOpen] = useState(false);

  if (!user) return null;

  const initial = (user.name || user.email)[0].toUpperCase();
  const segs = pathname.split('/').filter(Boolean);
  const normalized = LOCALES.includes(segs[0]) ? '/' + segs.slice(1).join('/') : pathname;

  const isActive = (href: string) =>
    href === '/portal' ? normalized === '/portal' : normalized.startsWith(href);

  const groups = isSuperAdmin ? superAdminGroups() : isAdmin ? adminGroups() : clientGroups();

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
