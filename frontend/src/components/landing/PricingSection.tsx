import { useTranslations } from 'next-intl';

interface Plan {
  name: string;
  price: string;
  period: string;
  features: string[];
  cta: string;
  highlighted?: boolean;
}

export function PricingSection() {
  const t = useTranslations('pricing');
  const plans = t.raw('plans') as Plan[];

  return (
    <section id="pricing" className="py-24 px-6 border-t border-border-subtle">
      <div className="max-w-6xl mx-auto">
        <h2 className="f-display font-black text-3xl sm:text-4xl lg:text-5xl leading-[1.1] tracking-display mb-12">
          {t('title')}
        </h2>

        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {plans.map((plan, i) => (
            <div
              key={i}
              className={`relative flex flex-col gap-4 p-6 border transition-colors ${
                plan.highlighted
                  ? 'bg-accent-subtle border-accent'
                  : 'bg-bg-1 border-border-base hover:border-border-medium'
              }`}
            >
              {plan.highlighted && <div className="absolute top-0 left-0 right-0 h-[2px] bg-accent" />}

              <h3 className="font-bold text-lg">{plan.name}</h3>
              <div className="flex items-baseline gap-1">
                <span className="f-display font-black text-3xl">{plan.price}</span>
                <span className="text-data text-ink-3">{plan.period}</span>
              </div>

              <div className="space-y-2 mt-2">
                {plan.features.map((f, j) => (
                  <div key={j} className="flex items-start gap-2">
                    <span className="text-accent shrink-0 text-xs mt-0.5">✓</span>
                    <span className="text-data text-ink-2 text-xs leading-snug">{f}</span>
                  </div>
                ))}
              </div>

              <a
                href="#contact"
                className={`mt-auto text-center f-mono text-xs uppercase h-10 leading-10 transition-colors ${
                  plan.highlighted
                    ? 'bg-accent hover:bg-accent-light text-black font-semibold'
                    : 'border border-border-medium hover:border-accent text-ink-1'
                }`}
              >
                {plan.cta}
              </a>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
