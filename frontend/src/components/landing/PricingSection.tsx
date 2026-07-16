import { useTranslations } from 'next-intl';

interface Fase {
  name: string;
  description: string;
  services: string[];
  highlight?: boolean;
}

export function PricingSection() {
  const t = useTranslations('landing.pricing');
  const fases = t.raw('fases') as Fase[];

  return (
    <section id="pricing" className="py-24 px-6 border-t border-border-subtle">
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

        <div className="grid sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-3">
          {fases.map((fase, i) => (
            <div
              key={i}
              className={`relative flex flex-col gap-4 p-5 border transition-colors ${
                fase.highlight
                  ? 'bg-accent-subtle border-accent'
                  : 'bg-bg-1 border-border-base hover:border-border-medium'
              }`}
            >
              {fase.highlight && (
                <div className="absolute top-0 left-0 right-0 h-[2px] bg-accent" />
              )}

              <h3 className="font-bold text-md">{fase.name}</h3>
              <p className="text-data text-ink-2 leading-relaxed">{fase.description}</p>

              <div className="space-y-1.5 mt-auto pt-4 border-t border-border-subtle">
                {fase.services.map((service, j) => (
                  <div key={j} className="flex items-start gap-2">
                    <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" className="text-accent shrink-0 mt-1">
                      <path d="M20 6L9 17l-5-5" />
                    </svg>
                    <span className="text-data text-ink-2 text-[11px] leading-snug">{service}</span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>

        <div className="mt-10 text-center">
          <p className="f-mono text-caption uppercase tracking-widest text-ink-3">{t('setupNote')}</p>
          <a
            href="#contacte"
            className="inline-block mt-4 bg-accent hover:bg-accent-light text-black font-semibold f-mono text-xs uppercase px-8 h-12 leading-[3rem] transition-colors"
          >
            {t('ctaNote')}
          </a>
        </div>
      </div>
    </section>
  );
}
