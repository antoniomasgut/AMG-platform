import { useTranslations } from 'next-intl';

interface Plan {
  icon: string;
  name: string;
  price: string;
  pricePrefix?: string;
  description: string;
  features: string[];
  highlight: boolean;
}

export function PricingSection() {
  const t = useTranslations('landing.pricing');
  const plans = t.raw('plans') as Plan[];

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

        <div className="grid sm:grid-cols-3 gap-4">
          {plans.map((plan, i) => (
            <div
              key={i}
              className={`relative card-clip flex flex-col gap-5 p-6 border transition-colors ${
                plan.highlight
                  ? 'bg-accent-muted border-accent'
                  : 'bg-bg-1 border-border-base hover:border-border-medium'
              }`}
            >
              {plan.highlight && (
                <div className="absolute top-0 left-0 right-0 h-[2px] bg-accent" />
              )}
              {plan.highlight && (
                <span className="absolute -top-3 left-1/2 -translate-x-1/2 f-mono text-[9px] uppercase tracking-widest bg-accent text-black px-3 py-1">
                  {t('popular')}
                </span>
              )}

              <div className="flex items-start justify-between">
                <span className="text-3xl">{plan.icon}</span>
              </div>

              <div>
                <h3 className="f-display font-black text-md mb-1">{plan.name}</h3>
                <p className="text-data text-ink-2 leading-relaxed">{plan.description}</p>
              </div>

              <div className="mt-auto">
                <div className="flex items-baseline gap-1 mb-1">
                  {plan.pricePrefix && (
                    <span className="f-mono text-xs text-ink-3 uppercase">{t('from')}</span>
                  )}
                  <span className={`f-display font-black text-3xl ${plan.highlight ? 'text-accent-light' : 'text-ink-0'}`}>
                    {plan.price}
                  </span>
                </div>
                <span className="f-mono text-[10px] uppercase tracking-widest text-ink-3">{t('oneTime')}</span>
              </div>

              <div className="border-t border-border-subtle pt-4 space-y-2">
                <span className="f-mono text-[9px] uppercase tracking-widest text-ink-3">{t('included')}</span>
                {plan.features.map((feature, j) => (
                  <div key={j} className="flex items-center gap-2">
                    <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" className="text-accent shrink-0">
                      <path d="M20 6L9 17l-5-5" />
                    </svg>
                    <span className="text-data text-ink-2 text-[12px]">{feature}</span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>

        <div className="mt-8 text-center">
          <p className="f-mono text-[10px] uppercase tracking-widest text-ink-3">{t('note')}</p>
        </div>
      </div>
    </section>
  );
}
