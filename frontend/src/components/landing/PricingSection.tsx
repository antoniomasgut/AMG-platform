import { useTranslations } from 'next-intl';

export function PricingSection() {
  const t = useTranslations('landing.service');
  const pillars = t.raw('pillars') as Array<{ icon: string; title: string; description: string }>;

  return (
    <section id="pricing" className="py-24 px-6 border-t border-border-subtle">
      <div className="max-w-4xl mx-auto">
        <div className="flex items-center gap-2 mb-6">
          <div className="w-1.5 h-1.5 bg-accent" />
          <span className="f-mono text-label uppercase tracking-widest text-accent-light">{t('badge')}</span>
        </div>

        <h2 className="f-display font-black text-3xl sm:text-4xl lg:text-5xl leading-[1.1] tracking-display mb-3 whitespace-pre-line">
          {t('title')}
        </h2>
        <p className="text-ui text-ink-1 mb-12 max-w-2xl">{t('subtitle')}</p>

        <div className="grid sm:grid-cols-2 gap-4">
          {pillars.map((pillar, i) => (
            <div key={i} className="amg-card card-clip p-6 flex gap-4">
              <span className="text-2xl shrink-0 mt-0.5">{pillar.icon}</span>
              <div>
                <h3 className="f-display font-black text-md mb-2">{pillar.title}</h3>
                <p className="text-ui text-ink-2 leading-relaxed">{pillar.description}</p>
              </div>
            </div>
          ))}
        </div>

        <p className="mt-8 f-mono text-label text-ink-3 text-center">{t('note')}</p>
      </div>
    </section>
  );
}
