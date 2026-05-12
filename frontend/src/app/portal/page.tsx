'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { getCurrentUser, logout } from '@/services/auth';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGSectionTitle } from '@/components/ui/stat';
import { I } from '@/components/ui/icons';

interface UserInfo {
  id: string; email: string; name: string; role: string; tenantId: string | null;
}

const services = [
  { name: 'WhatsApp Bot AI', icon: I.Bot, used: 1240, total: 2000, unit: 'msgs' },
  { name: 'Landing auto', icon: I.Globe, used: 1, total: 1, unit: 'pàgines' },
  { name: 'Workflow engine', icon: I.Zap, used: 3, total: 5, unit: 'workflows' },
  { name: 'Reserves calendari', icon: I.Calendar, used: 42, total: 200, unit: 'reserves' },
];

export default function PortalPage() {
  const router = useRouter();
  const [user, setUser] = useState<UserInfo | null>(null);
  const [loggingOut, setLoggingOut] = useState(false);

  useEffect(() => {
    const u = getCurrentUser();
    if (!u) {
      router.replace('/login');
    } else {
      setUser(u);
    }
  }, [router]);

  const handleLogout = async () => {
    setLoggingOut(true);
    try {
      await logout();
    } finally {
      router.replace('/login');
    }
  };

  if (!user) {
    return (
      <div className="w-full min-h-dvh bg-[#0d0d1a] flex items-center justify-center">
        <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin"></span>
      </div>
    );
  }

  const initial = (user.name || user.email)[0].toUpperCase();

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
            { label: 'Dashboard', icon: I.Dashboard, active: true },
            { label: 'Serveis', icon: I.Box, active: false },
            { label: 'Factures', icon: I.Receipt, active: false },
            { label: 'Landing pública', icon: I.Globe, active: false },
            { label: 'Suport', icon: I.Bell, active: false },
            { label: 'Configuració', icon: I.Settings, active: false },
          ] as const).map(({ label, icon: Icon, active }) => (
            <a
              key={label}
              className={`relative flex items-center gap-3 px-3 h-10 f-mono text-xs uppercase tracking-wider cursor-pointer ${
                active ? 'bg-[rgba(255,107,0,0.10)] text-[#FF9A3C]' : 'text-[#94a3b8] hover:text-[#e2e8f0]'
              }`}
            >
              {active && <span className="absolute left-0 top-0 bottom-0 w-[2px] bg-[#FF6B00]"></span>}
              <Icon size={14} />
              {label}
            </a>
          ))}
        </nav>
        <div className="p-4 border-t border-[rgba(255,107,0,0.12)]">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 bg-gradient-to-br from-[#58a6ff] to-[#FF9A3C] btn-clip flex items-center justify-center text-black font-bold text-xs">
              {initial}
            </div>
            <div className="flex-1 min-w-0">
              <div className="text-sm font-semibold truncate">{user.name}</div>
              <div className="f-mono text-[10px] text-[#64748b] truncate">{user.email}</div>
            </div>
          </div>
        </div>
      </aside>

      {/* Main content */}
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
          {/* Subscription hero */}
          <div className="amg-card card-clip p-4 sm:p-6 relative overflow-hidden">
            <div className="absolute top-0 right-0 w-[3px] h-16 bg-[#FF6B00]"></div>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 items-center">
              <div>
                <span className="f-mono text-[10px] uppercase tracking-[0.2em] text-[#FF9A3C]">Suscripció activa</span>
                <div className="f-display font-black text-2xl sm:text-3xl mt-1">PLAN AVANÇAT</div>
                <div className="flex items-center gap-2 mt-2">
                  <AMGBadge tone="success">
                    <span className="w-1 h-1 rounded-full bg-[#39d353]"></span>AL DIA
                  </AMGBadge>
                  <span className="f-mono text-[11px] text-[#64748b] uppercase">En actiu</span>
                </div>
              </div>
              <div>
                <div className="f-mono text-[10px] uppercase text-[#64748b]">Pròxim cobro</div>
                <div className="f-display font-bold text-lg sm:text-xl mt-1">12 Jun 2026</div>
                <div className="f-mono text-[11px] text-[#94a3b8] mt-0.5">en 31 dies</div>
              </div>
              <div>
                <div className="f-mono text-[10px] uppercase text-[#64748b]">Import mensual</div>
                <div className="f-display font-bold text-lg sm:text-xl mt-1 text-[#FF9A3C]">€99,00</div>
                <div className="f-mono text-[11px] text-[#94a3b8] mt-0.5">€81,82 + IVA</div>
              </div>
              <div>
                <div className="f-mono text-[10px] uppercase text-[#64748b]">Rol</div>
                <div className="flex items-center gap-2 mt-2">
                  <AMGBadge tone="accent">{user.role === 'SUPER_ADMIN' ? 'SUPER ADMIN' : 'CLIENT'}</AMGBadge>
                </div>
              </div>
            </div>
          </div>

          {/* Services with usage */}
          <div>
            <AMGSectionTitle eyebrow="Ús del mes" title="Serveis actius">
              <span className="f-mono text-[11px] text-[#64748b] uppercase">Cicle 12 Abr → 12 May</span>
            </AMGSectionTitle>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {services.map((s, i) => {
                const Icon = s.icon;
                const pct = (s.used / s.total) * 100;
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
                          <span className="w-1.5 h-1.5 rounded-full bg-[#39d353] amg-blink"></span>OPERATIU
                        </div>
                      </div>
                      <button className="f-mono text-[11px] uppercase text-[#FF9A3C] flex items-center gap-1">
                        GESTIONAR<I.ArrowRight size={10} />
                      </button>
                    </div>
                    <div className="flex items-baseline justify-between mb-1.5">
                      <span className="f-mono text-[11px] text-[#94a3b8] uppercase">
                        {s.used.toLocaleString()} / {s.total.toLocaleString()} {s.unit}
                      </span>
                      <span className={`f-mono text-[11px] ${warn ? 'text-[#f0b429]' : 'text-[#64748b]'}`}>
                        {Math.round(pct)}%
                      </span>
                    </div>
                    <div className="h-1.5 bg-[#212140] overflow-hidden">
                      <div className={`h-full ${warn ? 'bg-[#f0b429]' : 'bg-[#FF6B00]'}`} style={{ width: `${pct}%` }}></div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Recent invoices + CTA */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
            <div className="lg:col-span-2 amg-card card-clip p-4 sm:p-5">
              <AMGSectionTitle eyebrow="Historial" title="Últimes factures">
                <a className="f-mono text-[10px] uppercase text-[#FF9A3C] cursor-pointer">VEURE TOTES →</a>
              </AMGSectionTitle>
              <div className="space-y-0">
                {[
                  ['2026-001', '12 Abr 2026', '€99,00', 'PAGAT', 'success'],
                  ['2026-002', '12 Mar 2026', '€99,00', 'PAGAT', 'success'],
                  ['2026-003', '12 Feb 2026', '€99,00', 'PAGAT', 'success'],
                ].map((r, i) => (
                  <div
                    key={i}
                    className="grid grid-cols-[80px_1fr_80px_80px_24px] gap-2 sm:gap-3 px-2 h-11 items-center border-b border-[rgba(226,232,240,0.04)] text-sm last:border-b-0"
                  >
                    <span className="f-mono text-[#FF9A3C] text-xs">#{r[0]}</span>
                    <span className="f-mono text-[12px] text-[#94a3b8]">{r[1]}</span>
                    <span className="f-mono text-[#e2e8f0]">{r[2]}</span>
                    <span><AMGBadge tone={r[4] as any}>{r[3]}</AMGBadge></span>
                    <button className="text-[#94a3b8] hover:text-[#FF9A3C]"><I.Download size={12} /></button>
                  </div>
                ))}
              </div>
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

          {/* Logout */}
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
