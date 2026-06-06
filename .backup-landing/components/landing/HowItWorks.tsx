import { getTranslations } from 'next-intl/server';

export async function HowItWorks() {
  const t = await getTranslations('howItWorks');
  const steps = t.raw('steps') as { number: string; title: string; desc: string }[];

  return (
    <section id="com-funciona" className="relative py-24 bg-bg-1">
      <div className="max-w-5xl mx-auto px-4">
        <h2 className="f-display text-3xl sm:text-4xl font-black text-white text-center mb-16">
          {t('title')}
        </h2>

        <div className="grid md:grid-cols-3 gap-8 relative">
          {/* Connecting line (desktop) */}
          <div className="hidden md:block absolute top-12 left-[16.66%] right-[16.66%] h-0.5 bg-gradient-to-r from-accent/40 via-accent to-accent/40" />

          {steps.map((step, i) => (
            <div key={i} className="relative flex flex-col items-center text-center">
              <div className="w-16 h-16 rounded-full bg-accent flex items-center justify-center mb-6 relative z-10">
                <span className="text-white font-black text-xl">{step.number}</span>
              </div>
              <h3 className="f-display text-lg font-bold text-white mb-3">{step.title}</h3>
              <p className="text-ink-2 text-sm leading-relaxed max-w-xs">{step.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
