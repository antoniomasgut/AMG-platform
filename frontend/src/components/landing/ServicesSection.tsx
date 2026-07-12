import { useTranslations } from 'next-intl';

export function ServicesSection() {
  const t = useTranslations('landing.services');
  const items = t.raw('items') as Array<{ icon: string; title: string; description: string; tag: string }>;

  return (
    <section id="services" className="py-24 px-6 border-t border-border-subtle">
      <div className="max-w-6xl mx-auto">
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
          {items.map((item, i) => (
            <div
              key={i}
              className="amg-card card-clip p-6 flex flex-col gap-4 hover:border-border-medium transition-colors group"
            >
              <span className="text-3xl">{item.icon}</span>
              <div>
                <h3 className="f-display font-black text-md mb-1 group-hover:text-accent-light transition-colors">
                  {item.title}
                </h3>
                <p className="text-data text-ink-2 leading-relaxed">{item.description}</p>
              </div>
              <div className="mt-auto pt-4 border-t border-border-subtle">
                <span className="f-mono text-caption text-ink-3 uppercase tracking-caption">{item.tag}</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
