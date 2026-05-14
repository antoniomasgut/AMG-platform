import { useTranslations } from 'next-intl';

export function ProblemSection() {
  const t = useTranslations('landing.problem');
  const items = t.raw('items') as Array<{ icon: string; text: string }>;

  return (
    <section className="py-24 px-6 border-t border-border-subtle">
      <div className="max-w-4xl mx-auto">
        <div className="flex items-center gap-2 mb-6">
          <div className="w-1.5 h-1.5 bg-accent" />
          <span className="f-mono text-label uppercase tracking-widest text-accent-light">{t('badge')}</span>
        </div>

        <h2 className="f-display font-black text-3xl sm:text-4xl lg:text-5xl leading-[1.1] tracking-display mb-8 whitespace-pre-line">
          {t('title')}
        </h2>

        <div className="grid md:grid-cols-2 gap-8 items-start">
          <p className="text-ui text-ink-1 leading-relaxed">{t('description')}</p>

          <div className="space-y-4">
            {items.map((item, i) => (
              <div key={i} className="flex items-center gap-4 p-4 border border-border-subtle bg-bg-1">
                <span className="text-2xl shrink-0">{item.icon}</span>
                <span className="text-ui text-ink-1">{item.text}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
