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
          <h2 className="f-display font-black text-3xl sm:text-4xl lg:text-5xl leading-[1.1] tracking-display">
            {t('title')}
          </h2>
          <p className="text-ui text-ink-2 lg:text-right max-w-sm">{t('subtitle')}</p>
        </div>

        <div className="grid lg:grid-cols-2 gap-8 items-start">
          {/* Feature list */}
          <div className="flex flex-col gap-4">
            {features.map((f, i) => (
              <div key={i} className="amg-card p-5 flex gap-4">
                <span className="text-2xl shrink-0 mt-0.5">{f.icon}</span>
                <div>
                  <h3 className="font-bold text-md mb-1">{f.title}</h3>
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
          <div className="amg-card bg-bg-1 border border-border-medium overflow-hidden">
            {/* Mockup header */}
            <div className="flex items-center justify-between px-5 py-3 border-b border-border-subtle bg-bg-2">
              <div className="flex items-center gap-2">
                <AMGIcon className="h-5 w-5" />
                <span className="f-mono text-label text-ink-2 uppercase tracking-widest">Portal</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-[#39d353] amg-blink" />
                <span className="f-mono text-label text-ink-3">Online</span>
              </div>
            </div>

            {/* Mockup stat cards */}
            <div className="grid grid-cols-2 gap-3 p-5 border-b border-border-subtle">
              {[
                { label: 'Missatges gestionats', value: '1.247', sub: 'aquest mes', color: 'text-accent-light' },
                { label: 'Reserves', value: '48', sub: 'confirmades', color: 'text-[#39d353]' },
                { label: 'Pressupostos', value: '12', sub: 'enviats', color: 'text-ink-1' },
                { label: 'Temps d\'activitat', value: '99.9%', sub: 'uptime', color: 'text-[#39d353]' },
              ].map((card) => (
                <div key={card.label} className="border border-border-subtle p-3">
                  <div className="f-mono text-label text-ink-3 uppercase mb-1">{card.label}</div>
                  <div className={`font-bold text-xl ${card.color}`}>{card.value}</div>
                  <div className="f-mono text-label text-ink-3">{card.sub}</div>
                </div>
              ))}
            </div>

            {/* Mockup conversation feed */}
            <div className="p-5">
              <div className="f-mono text-label text-ink-3 uppercase tracking-widest mb-3">Converses recents</div>
              <div className="flex flex-col gap-0">
                {[
                  { name: 'Clínica Rosselló', preview: 'Bon dia, volia demanar hora per a una revisió...', time: 'Fa 5 min', read: true },
                  { name: 'Fullana Reformes', preview: 'Em poden fer un pressupost per a una reforma de bany?', time: 'Fa 23 min', read: true },
                  { name: 'Aiguabella Assessors', preview: 'Quins documents necessito per a la declaració...', time: 'Fa 1h', read: false },
                ].map((row, i) => (
                  <div key={i} className="flex items-start justify-between py-3 border-b border-border-subtle last:border-0 gap-3">
                    <div className="flex items-start gap-3 min-w-0">
                      <div className={`w-7 h-7 rounded-full flex items-center justify-center shrink-0 ${row.read ? 'bg-bg-3' : 'bg-accent-muted'}`}>
                        <span className={`f-mono text-[10px] font-bold ${row.read ? 'text-ink-3' : 'text-accent-light'}`}>
                          {row.name.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase()}
                        </span>
                      </div>
                      <div className="min-w-0">
                        <div className={`text-sm ${row.read ? 'text-ink-2' : 'text-ink-0 font-semibold'}`}>{row.name}</div>
                        <div className="f-mono text-label text-ink-3 truncate max-w-[200px]">{row.preview}</div>
                      </div>
                    </div>
                    <span className="f-mono text-label text-ink-3 shrink-0">{row.time}</span>
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
