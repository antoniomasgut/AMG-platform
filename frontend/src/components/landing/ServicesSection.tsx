import { useTranslations } from 'next-intl';

interface Profile {
  name: string;
  tagline: string;
  price: string;
  features: string[];
}

export function ServicesSection() {
  const t = useTranslations('services');
  const profiles = t.raw('profiles') as Profile[];

  return (
    <section id="services" className="py-24 px-6 border-t border-border-subtle">
      <div className="max-w-6xl mx-auto">
        <div className="flex flex-col lg:flex-row lg:items-end lg:justify-between gap-4 mb-12">
          <h2 className="f-display font-black text-3xl sm:text-4xl lg:text-5xl leading-[1.1] tracking-display whitespace-pre-line">
            {t('title')}
          </h2>
          <p className="text-ui text-ink-2 lg:text-right max-w-xs">{t('subtitle')}</p>
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {profiles.map((p, i) => (
            <div
              key={i}
              className="amg-card card-clip p-6 flex flex-col gap-3 hover:border-border-medium transition-colors group"
            >
              <div>
                <h3 className="f-display font-black text-md mb-1 group-hover:text-accent-light transition-colors">
                  {p.name}
                </h3>
                <p className="text-data text-ink-3 leading-relaxed">{p.tagline}</p>
              </div>

              <div className="space-y-1.5">
                {p.features.map((f, j) => (
                  <div key={j} className="flex items-start gap-2">
                    <span className="text-accent shrink-0 text-xs mt-0.5">✓</span>
                    <span className="text-data text-ink-2 text-[11px] leading-snug">{f}</span>
                  </div>
                ))}
              </div>

              <div className="mt-auto pt-4 border-t border-border-subtle">
                <span className="f-mono text-caption text-accent-light uppercase tracking-caption">{p.price}</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
