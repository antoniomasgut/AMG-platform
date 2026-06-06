import { useTranslations } from 'next-intl';

export function HowItWorksSection() {
  const t = useTranslations('landing.howItWorks');
  const steps = t.raw('steps') as Array<{ number: string; title: string; description: string }>;

  return (
    <section id="how-it-works" className="py-24 px-6 border-t border-border-subtle">
      <div className="max-w-4xl mx-auto">
        <div className="flex items-center gap-2 mb-6">
          <div className="w-1.5 h-1.5 bg-accent" />
          <span className="f-mono text-label uppercase tracking-widest text-accent-light">{t('badge')}</span>
        </div>

        <h2 className="f-display font-black text-3xl sm:text-4xl lg:text-5xl leading-[1.1] tracking-display mb-16 whitespace-pre-line">
          {t('title')}
        </h2>

        <div className="space-y-0">
          {steps.map((step, i) => (
            <div key={i} className="flex gap-8 relative">
              {/* Connector line */}
              {i < steps.length - 1 && (
                <div className="absolute left-7 top-16 bottom-0 w-px bg-border-base" />
              )}

              {/* Number */}
              <div className="shrink-0 w-14 h-14 border border-border-medium flex items-center justify-center">
                <span className="f-display font-black text-sm text-accent-light">{step.number}</span>
              </div>

              {/* Content */}
              <div className="pb-16">
                <h3 className="f-display font-black text-xl mb-2">{step.title}</h3>
                <p className="text-ui text-ink-1 leading-relaxed">{step.description}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
