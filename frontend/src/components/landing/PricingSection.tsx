import { useTranslations } from 'next-intl';

interface Fase {
  code: string;
  name: string;
  price: string;
  description: string;
  services: string[];
  highlight: boolean;
}

export function PricingSection() {
  const t = useTranslations('landing.pricing');
  const fases = t.raw('fases') as Fase[];

  return (
    <section id="pricing" className="py-24 px-6 border-t border-border-subtle">
      <div className="max-w-5xl mx-auto">
        <div className="flex items-center gap-2 mb-6">
          <div className="w-1.5 h-1.5 bg-accent" />
          <span className="f-mono text-label uppercase tracking-widest text-accent-light">{t('badge')}</span>
        </div>

        <div className="flex flex-col lg:flex-row lg:items-end lg:justify-between gap-4 mb-12">
          <h2 className="f-display font-black text-3xl sm:text-4xl lg:text-5xl leading-[1.1] tracking-display whitespace-pre-line">
            {t('title')}
          </h2>
          <p className="text-ui text-ink-2 lg:text-right max-w-xs">{t('subtitle')}</p>
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {fases.map((fase, i) => (
            <div
              key={i}
              className={`relative card-clip flex flex-col gap-5 p-6 border transition-colors ${
                fase.highlight
                  ? 'bg-accent-muted border-accent'
                  : 'bg-bg-1 border-border-base hover:border-border-medium'
              }`}
            >
              {fase.highlight && (
                <div className="absolute top-0 left-0 right-0 h-[2px] bg-accent" />
              )}
              {fase.highlight && (
                <span className="absolute -top-3 left-1/2 -translate-x-1/2 f-mono text-[9px] uppercase tracking-widest bg-accent text-black px-3 py-1 whitespace-nowrap">
                  {t('popular')}
                </span>
              )}

              {/* Phase code + name */}
              <div>
                <span className="f-mono text-[10px] uppercase tracking-widest text-accent-light">{fase.code}</span>
                <h3 className="f-display font-black text-md mt-1">{fase.name}</h3>
                <p className="text-data text-ink-2 leading-relaxed mt-1">{fase.description}</p>
              </div>

              {/* Pricing */}
              <div className="border-t border-border-subtle pt-4">
                <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3 mb-0.5">{t('fromLabel')}</div>
                <div className="flex items-baseline gap-0.5">
                  <span className={`f-display font-black text-2xl ${fase.highlight ? 'text-accent-light' : 'text-ink-0'}`}>
                    {fase.price}
                  </span>
                  <span className="f-mono text-xs text-ink-3">{t('monthLabel')}</span>
                </div>
              </div>

              {/* Services list */}
              <div className="space-y-2">
                <span className="f-mono text-[9px] uppercase tracking-widest text-ink-3">{t('included')}</span>
                {fase.services.map((service, j) => (
                  <div key={j} className="flex items-start gap-2">
                    <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" className="text-accent shrink-0 mt-1">
                      <path d="M20 6L9 17l-5-5" />
                    </svg>
                    <span className="text-data text-ink-2 text-[12px] leading-snug">{service}</span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>

        <div className="mt-8 space-y-2">
          <p className="f-mono text-[10px] uppercase tracking-widest text-ink-3">{t('setupNote')}</p>
          <p className="f-mono text-[10px] text-ink-3 opacity-60">{t('ctaNote')}</p>
        </div>
      </div>
    </section>
  );
}
