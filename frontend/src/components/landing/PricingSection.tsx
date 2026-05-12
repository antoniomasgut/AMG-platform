import { getTranslations } from 'next-intl/server';

export async function PricingSection() {
  const t = await getTranslations('pricing');
  const plans = t.raw('plans') as {
    name: string;
    price: string;
    period: string;
    features: string[];
    cta: string;
    highlighted?: boolean;
  }[];

  return (
    <section id="preus" className="relative py-24">
      <div className="max-w-5xl mx-auto px-4">
        <h2 className="f-display text-3xl sm:text-4xl font-black text-white text-center mb-16">
          {t('title')}
        </h2>

        <div className="grid md:grid-cols-3 gap-6 items-start">
          {plans.map((plan, i) => (
            <div
              key={i}
              className={`amg-card p-8 border transition-all duration-300 ${
                plan.highlighted
                  ? 'border-accent/40 bg-accent/5 scale-105 md:scale-110 relative'
                  : 'border-white/5 hover:border-accent/20'
              }`}
            >
              {plan.highlighted && (
                <div className="absolute -top-3 left-1/2 -translate-x-1/2 px-3 py-1 rounded-full bg-accent text-white text-[10px] font-bold tracking-widest">
                  MÉS POPULAR
                </div>
              )}

              <h3 className="f-display text-xl font-bold text-white mb-2">{plan.name}</h3>
              <div className="mb-6">
                <span className="text-3xl font-black text-white">{plan.price}</span>
                <span className="text-ink-2 text-sm">{plan.period}</span>
              </div>
              <ul className="space-y-3 mb-8">
                {plan.features.map((feat, j) => (
                  <li key={j} className="flex items-start gap-2 text-sm text-ink-2">
                    <svg className="w-4 h-4 mt-0.5 text-accent shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M20 6L9 17l-5-5" />
                    </svg>
                    {feat}
                  </li>
                ))}
              </ul>
              <a
                href="#cta"
                className={`block w-full text-center py-3 text-xs font-bold tracking-widest btn-clip transition-all ${
                  plan.highlighted
                    ? 'bg-accent text-white hover:bg-accent-light'
                    : 'border border-accent/30 text-accent hover:bg-accent hover:text-white'
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
