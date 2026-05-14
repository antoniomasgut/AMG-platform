'use client';

import { useAuth } from '@/lib/auth-context';
import { usePathname } from 'next/navigation';
import { I } from '@/components/ui/icons';
import type { ReactNode } from 'react';

const LOCALES = ['ca', 'es', 'en', 'de'];

export function PortalShell({ children, breadcrumb }: { children: ReactNode; breadcrumb: string }) {
  const { user, isSuperAdmin, isAdmin } = useAuth();
  const pathname = usePathname();

  if (!user) return null;

  const initial = (user.name || user.email)[0].toUpperCase();
  const segs = pathname.split('/').filter(Boolean);
  const normalized = LOCALES.includes(segs[0]) ? '/' + segs.slice(1).join('/') : pathname;

  const isActive = (href: string) =>
    href === '/portal' ? normalized === '/portal' : normalized.startsWith(href);

  const navItems: { label: string; icon: (p: { size?: number }) => ReactNode; href: string }[] = [
    { label: 'Dashboard', icon: I.Dashboard, href: '/portal' },
    { label: 'Landings', icon: I.Globe, href: '/portal/landings' },
    { label: 'Billing', icon: I.CreditCard, href: '/portal/billing' },
    { label: 'FinOps', icon: I.Receipt, href: '/portal/finops' },
    { label: 'Automatitzacions', icon: I.Zap, href: '/portal/automations' },
    ...(isAdmin ? [{ label: 'Ops & Health', icon: I.Activity, href: '/portal/ops' }] : []),
    ...(isSuperAdmin ? [{ label: 'Admin', icon: I.Settings, href: '/portal/admin/users' }] : []),
  ];

  return (
    <div className="flex w-full min-h-dvh bg-[#0d0d1a] overflow-hidden">
      <aside aria-label="Navegació del portal" className="hidden lg:flex w-[240px] shrink-0 bg-[#13132a] border-r border-border-base flex-col">
        <div className="h-16 border-b border-border-base flex items-center px-5 gap-3">
          <a href="/portal" className="w-9 h-9 bg-[#FF6B00] btn-clip flex items-center justify-center shrink-0">
            <span className="f-display font-black text-black text-sm">A</span>
          </a>
          <div className="flex flex-col leading-tight">
            <span className="f-display font-bold text-sm">AMG</span>
            <span className="f-mono text-[9px] text-accent-light tracking-widest">PORTAL · GROWTH</span>
          </div>
        </div>
        <nav aria-label="Menú principal" className="flex-1 p-3 space-y-1 overflow-y-auto">
          <div className="f-mono text-[9px] uppercase tracking-widest text-ink-2 px-3 py-2">El meu compte</div>
          {navItems.map(({ label, icon: Icon, href }) => {
            const active = isActive(href);
            return (
              <a key={label} href={href}
                className={`relative flex items-center gap-3 px-3 h-10 f-mono text-xs uppercase tracking-wider transition-colors ${
                  active ? 'bg-accent-muted text-accent-light' : 'text-ink-1 hover:text-ink-0'
                }`}>
                {active && <span className="absolute left-0 top-0 bottom-0 w-[2px] bg-[#FF6B00]" />}
                <Icon size={14} />
                {label}
              </a>
            );
          })}
        </nav>
        <div className="p-4 border-t border-border-base">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 bg-gradient-to-br from-[#58a6ff] to-[#FF9A3C] btn-clip flex items-center justify-center text-black font-bold text-xs">
              {initial}
            </div>
            <div className="flex-1 min-w-0">
              <div className="text-sm font-semibold truncate">{user.name ?? ''}</div>
              <div className="f-mono text-label text-ink-2 truncate">{user.email}</div>
            </div>
          </div>
        </div>
      </aside>

      <div className="flex-1 flex flex-col min-w-0">
        <div className="h-16 border-b border-border-base flex items-center px-4 sm:px-8 gap-3">
          <button aria-label="Obrir menú" className="lg:hidden text-ink-1"><I.Menu size={20} /></button>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">
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
