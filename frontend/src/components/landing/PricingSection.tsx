import { useTranslations } from 'next-intl';

export function PricingSection() {
  const t = useTranslations('landing.pricing');
  const items = t.raw('items') as Array<{ service: string; price: string }>;

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
        <p className="text-ui text-ink-1 mb-10">{t('subtitle')}</p>

        <div className="amg-card card-clip overflow-hidden">
          {items.map((item, i) => (
            <div
              key={i}
              className={`flex items-center justify-between px-6 py-4 ${
                i < items.length - 1 ? 'border-b border-border-subtle' : ''
              } hover:bg-bg-2 transition-colors`}
            >
              <span className="text-ui text-ink-1">{item.service}</span>
              <span className="f-mono text-data text-accent-light font-semibold">{item.price}</span>
            </div>
          ))}
        </div>

        <p className="mt-4 f-mono text-label text-ink-3 text-center">{t('note')}</p>
      </div>
    </section>
  );
}
