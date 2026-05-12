'use client';

import { useTranslations } from 'next-intl';
import { useEffect, useState } from 'react';
import { useRouter } from '@/i18n/navigation';
import { useParams } from 'next/navigation';
import { getCurrentUser } from '@/services/auth';
import { AMGSectionTitle } from '@/components/ui/stat';
import { AMGBadge } from '@/components/ui/badge';
import { AMGButton } from '@/components/ui/button';
import { I } from '@/components/ui/icons';

type User = { name: string; email: string; role: string; tenantId: string };

export default function PortalPage() {
  const t = useTranslations('portal');
  const router = useRouter();
  const params = useParams();
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [loggingOut, setLoggingOut] = useState(false);

  useEffect(() => {
    const u = getCurrentUser() as User | null;
    if (!u) { router.push('/login'); return; }
    setUser(u);
    setLoading(false);
  }, [router]);

  const handleLogout = async () => {
    setLoggingOut(true);
    sessionStorage.clear();
    router.push('/login');
  };

  if (loading) {
    return (
      <div className="min-h-dvh bg-[#0d0d1a] flex items-center justify-center">
        <span className="w-6 h-6 border-2 border-accent border-t-transparent rounded-full animate-spin"></span>
      </div>
    );
  }

  if (!user) return null;

  const initials = user.name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);

  return (
    <div className="min-h-dvh bg-[#0d0d1a] flex">
      {/* Sidebar */}
      <aside className="hidden lg:flex w-60 flex-col border-r border-white/5 bg-bg-1">
        <div className="p-4 border-b border-white/5 flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-accent flex items-center justify-center text-black font-black f-display">
            A
          </div>
          <div>
            <div className="f-display font-black text-sm">AMG</div>
            <div className="f-mono text-[9px] text-accent tracking-widest uppercase">PORTAL</div>
          </div>
        </div>

        <nav className="flex-1 p-3 space-y-1">
          <div className="f-mono text-[9px] uppercase tracking-widest text-ink-2/50 px-3 pt-4 pb-2">{t('sidebar.myAccount')}</div>
          <a className="flex items-center gap-3 px-3 py-2 rounded text-sm bg-accent/10 text-accent font-semibold">
            <I.Dashboard size={16} /> {t('sidebar.dashboard')}
          </a>
          <a className="flex items-center gap-3 px-3 py-2 rounded text-sm text-ink-2 hover:text-white hover:bg-white/5 transition-colors">
            <I.Box size={16} /> {t('sidebar.services')}
          </a>
          <a className="flex items-center gap-3 px-3 py-2 rounded text-sm text-ink-2 hover:text-white hover:bg-white/5 transition-colors">
            <I.Receipt size={16} /> {t('sidebar.invoices')}
          </a>
          <a className="flex items-center gap-3 px-3 py-2 rounded text-sm text-ink-2 hover:text-white hover:bg-white/5 transition-colors">
            <I.Globe size={16} /> {t('sidebar.publicLanding')}
          </a>
          <a className="flex items-center gap-3 px-3 py-2 rounded text-sm text-ink-2 hover:text-white hover:bg-white/5 transition-colors">
            <I.Settings size={16} /> {t('sidebar.support')}
          </a>

          {(user.role === 'SUPER_ADMIN' || user.role === 'ADMIN') && (
            <>
              <div className="f-mono text-[9px] uppercase tracking-widest text-ink-2/50 px-3 pt-4 pb-2">{t('sidebar.admin')}</div>
              <a href={`/${params.locale}/portal/admin/users`} className="flex items-center gap-3 px-3 py-2 rounded text-sm text-ink-2 hover:text-white hover:bg-white/5 transition-colors">
                <I.Users size={16} /> {t('sidebar.users')}
              </a>
              <a href={`/${params.locale}/portal/admin/tenants`} className="flex items-center gap-3 px-3 py-2 rounded text-sm text-ink-2 hover:text-white hover:bg-white/5 transition-colors">
                <I.Building size={16} /> {t('sidebar.tenants')}
              </a>
            </>
          )}
        </nav>

        <div className="p-3 border-t border-white/5">
          <div className="flex items-center gap-3 px-3 py-2">
            <div className="w-8 h-8 rounded-full bg-bg-2 flex items-center justify-center text-xs font-bold text-ink-2">
              {initials}
            </div>
            <div className="text-xs">
              <div className="text-white font-medium truncate max-w-[120px]">{user.name}</div>
              <div className="text-ink-2/50 text-[10px]">{user.email}</div>
            </div>
          </div>
        </div>
      </aside>

      {/* Main */}
      <div className="flex-1 flex flex-col">
        {/* Top bar */}
        <header className="h-16 border-b border-white/5 flex items-center justify-between px-4 sm:px-8">
          <h1 className="f-display font-black text-lg">
            {t('greeting', { name: user.name.split(' ')[0] })}
          </h1>
          <div className="flex items-center gap-3">
            <AMGButton variant="outline" size="sm">
              <I.Globe size={14} className="mr-1.5" /> VER LANDING
            </AMGButton>
            <AMGButton variant="ghost" size="sm">
              <I.Smartphone size={14} className="mr-1.5" /> SUPORT
            </AMGButton>
            <AMGButton variant="danger" size="sm" onClick={handleLogout} disabled={loggingOut}>
              {loggingOut ? t('loggingOut') : t('logout')}
            </AMGButton>
          </div>
        </header>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-8 space-y-6">
          {/* Subscription card */}
          <div className="amg-card card-clip p-6 sm:p-8 border border-accent/20 bg-gradient-to-br from-accent/5 to-transparent">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <AMGBadge tone="success">{t('subscription.badge')}</AMGBadge>
                <h2 className="f-display font-black text-2xl text-white mt-3">
                  {t('subscription.plan', { name: 'AVANÇAT' })}
                </h2>
                <div className="flex items-center gap-4 mt-2 text-sm text-ink-2">
                  <span className="flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-success amg-blink" />
                    {t('subscription.active')}
                  </span>
                  <span>{t('subscription.nextBilling')}: 01/06/2026</span>
                  <span>{t('subscription.monthlyAmount')}: <span className="text-white font-bold">99,00 €</span></span>
                </div>
              </div>
              <div className="text-right">
                <AMGBadge tone="accent" mono>{t('subscription.upToDate')}</AMGBadge>
                <div className="text-xs text-ink-2/50 mt-2">
                  {t('subscription.role')}: ADMIN
                </div>
              </div>
            </div>
          </div>

          {/* Service cards */}
          <div>
            <AMGSectionTitle eyebrow={t('services.eyebrow')} title={t('services.title')} />
            <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4 mt-4">
              {[
                { name: 'WhatsApp Bot', usage: 42, total: 100 },
                { name: 'AI Chat', usage: 78, total: 100 },
                { name: 'Landing Page', usage: 100, total: 100 },
                { name: 'Workflows', usage: 15, total: 50 },
              ].map((svc, i) => (
                <div key={i} className="amg-card p-4 border border-white/5">
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-sm text-white font-semibold">{svc.name}</span>
                    <AMGBadge tone={svc.usage >= svc.total ? 'success' : 'neutral'} mono>
                      {t('services.operational')}
                    </AMGBadge>
                  </div>
                  <div className="h-1.5 rounded-full bg-bg-2 overflow-hidden">
                    <div
                      className="h-full rounded-full bg-accent transition-all"
                      style={{ width: `${(svc.usage / svc.total) * 100}%` }}
                    />
                  </div>
                  <div className="flex items-center justify-between mt-2">
                    <span className="text-xs text-ink-2">{svc.usage}/{svc.total} %</span>
                    <button className="text-xs text-accent hover:text-accent-light transition-colors">{t('services.manage')}</button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Invoices */}
          <div>
            <div className="flex items-center justify-between">
              <AMGSectionTitle eyebrow={t('invoices.eyebrow')} title={t('invoices.title')} />
              <button className="text-xs text-accent hover:text-accent-light transition-colors">{t('invoices.viewAll')}</button>
            </div>
            <div className="mt-4 amg-card border border-white/5 overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-white/5 text-ink-2/50 f-mono text-[10px] uppercase tracking-widest">
                    <th className="text-left p-3 font-normal">#</th>
                    <th className="text-left p-3 font-normal">Data</th>
                    <th className="text-left p-3 font-normal">Concepte</th>
                    <th className="text-right p-3 font-normal">Import</th>
                    <th className="text-right p-3 font-normal">Estat</th>
                  </tr>
                </thead>
                <tbody>
                  {[
                    { num: '2026-001', date: '01/04/2026', concept: 'Serveis mensuals Abril', amount: '99,00 €', status: 'PAGAT' },
                    { num: '2026-002', date: '01/03/2026', concept: 'Serveis mensuals Març', amount: '99,00 €', status: 'PAGAT' },
                    { num: '2026-003', date: '01/02/2026', concept: 'Serveis mensuals Febrer', amount: '99,00 €', status: 'PAGAT' },
                  ].map((inv, i) => (
                    <tr key={i} className="border-b border-white/5 last:border-none text-ink-2 hover:bg-white/5 transition-colors">
                      <td className="p-3 f-mono text-xs">#{inv.num}</td>
                      <td className="p-3">{inv.date}</td>
                      <td className="p-3">{inv.concept}</td>
                      <td className="p-3 text-right font-semibold text-white">{inv.amount}</td>
                      <td className="p-3 text-right">
                        <AMGBadge tone="success" mono>{inv.status}</AMGBadge>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Help */}
          <div className="amg-card p-6 border border-white/5 bg-gradient-to-r from-accent/5 to-transparent">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div>
                <h3 className="f-display font-bold text-lg text-white">{t('help.title')}</h3>
                <p className="text-sm text-ink-2 mt-1">{t('help.description')}</p>
              </div>
              <div className="flex items-center gap-3">
                <AMGButton variant="primary" size="sm"><I.Mail size={14} className="mr-1.5" />{t('help.contactTeam')}</AMGButton>
                <AMGButton variant="outline" size="sm"><I.Play size={14} className="mr-1.5" />{t('help.viewTutorials')}</AMGButton>
              </div>
            </div>
          </div>

          {/* Mobile logout */}
          <div className="lg:hidden">
            <AMGButton variant="danger" className="w-full" onClick={handleLogout} disabled={loggingOut}>
              {loggingOut ? t('loggingOut') : t('logout')}
            </AMGButton>
          </div>
        </div>
      </div>
    </div>
  );
}
