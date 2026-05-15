import { useTranslations, useLocale } from 'next-intl';
import Link from 'next/link';
import { AMGIcon } from '@/components/ui/AMGLogo';

export function PortalSection() {
  const t = useTranslations('landing.portalPreview');
  const locale = useLocale();
  const features = t.raw('features') as Array<{ icon: string; title: string; description: string }>;

  return (
    <section id="portal" className="py-24 px-6 border-t border-border-subtle">
      <div className="max-w-6xl mx-auto">
        <div className="flex items-center gap-2 mb-6">
          <div className="w-1.5 h-1.5 bg-accent" />
          <span className="f-mono text-label uppercase tracking-widest text-accent-light">{t('badge')}</span>
        </div>

        <div className="flex flex-col lg:flex-row lg:items-end lg:justify-between gap-4 mb-12">
          <h2 className="f-display font-black text-3xl sm:text-4xl lg:text-5xl leading-[1.1] tracking-display whitespace-pre-line">
            {t('title')}
          </h2>
          <p className="text-ui text-ink-2 lg:text-right max-w-sm">{t('subtitle')}</p>
        </div>

        <div className="grid lg:grid-cols-2 gap-8 items-start">
          {/* Feature list */}
          <div className="flex flex-col gap-4">
            {features.map((f, i) => (
              <div key={i} className="amg-card card-clip p-5 flex gap-4">
                <span className="text-2xl shrink-0 mt-0.5">{f.icon}</span>
                <div>
                  <h3 className="f-display font-black text-md mb-1">{f.title}</h3>
                  <p className="text-ui text-ink-2 leading-relaxed">{f.description}</p>
                </div>
              </div>
            ))}

            <Link
              href={`/${locale}/login`}
              className="mt-2 f-mono text-caption uppercase text-accent-light hover:text-accent border border-accent/30 hover:border-accent/60 px-6 h-10 flex items-center gap-2 transition-colors self-start"
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M5 12h14M13 5l7 7-7 7" />
              </svg>
              {t('badge')}
            </Link>
          </div>

          {/* Dashboard mockup */}
          <div className="amg-card card-clip bg-bg-1 border border-border-medium overflow-hidden">
            {/* Mockup header */}
            <div className="flex items-center justify-between px-5 py-3 border-b border-border-subtle bg-bg-2">
              <div className="flex items-center gap-2">
                <AMGIcon className="h-5 w-5" />
                <span className="f-mono text-label text-ink-2 uppercase tracking-widest">Portal</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-[#39d353] amg-blink" />
                <span className="f-mono text-label text-ink-3">Operatiu</span>
              </div>
            </div>

            {/* Mockup stat cards */}
            <div className="grid grid-cols-2 gap-3 p-5 border-b border-border-subtle">
              {[
                { label: 'Landings', value: '3', sub: 'actives', color: 'text-[#39d353]' },
                { label: 'Automatitzacions', value: '5', sub: 'actives', color: 'text-accent-light' },
                { label: 'Factures', value: '12', sub: 'emeses', color: 'text-ink-1' },
                { label: 'Sistema', value: '99.9%', sub: 'uptime', color: 'text-[#39d353]' },
              ].map((card) => (
                <div key={card.label} className="border border-border-subtle p-3">
                  <div className="f-mono text-label text-ink-3 uppercase mb-1">{card.label}</div>
                  <div className={`f-display font-black text-xl ${card.color}`}>{card.value}</div>
                  <div className="f-mono text-label text-ink-3">{card.sub}</div>
                </div>
              ))}
            </div>

            {/* Mockup invoice rows */}
            <div className="p-5">
              <div className="f-mono text-label text-ink-3 uppercase tracking-widest mb-3">Factures recents</div>
              <div className="flex flex-col gap-2">
                {[
                  { id: '#0024', concept: 'Landing Pro · Fase 1', amount: '80€', status: 'Pagada', ok: true },
                  { id: '#0023', concept: 'Automatització WhatsApp', amount: '60€', status: 'Pagada', ok: true },
                  { id: '#0022', concept: 'Manteniment mensual', amount: '45€', status: 'Pendent', ok: false },
                ].map((row) => (
                  <div key={row.id} className="flex items-center justify-between py-2 border-b border-border-subtle last:border-0">
                    <div className="flex items-center gap-3">
                      <span className="f-mono text-label text-ink-3">{row.id}</span>
                      <span className="f-mono text-label text-ink-1 hidden sm:block">{row.concept}</span>
                    </div>
                    <div className="flex items-center gap-3">
                      <span className="f-mono text-label text-ink-1 font-semibold">{row.amount}</span>
                      <span className={`f-mono text-label px-2 py-0.5 border ${row.ok ? 'border-[#39d353]/30 text-[#39d353]' : 'border-amber-500/30 text-amber-400'}`}>
                        {row.status}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
