import { useTranslations } from 'next-intl';

export function ProblemSection() {
  const t = useTranslations('problem');
  const items = t.raw('items') as string[];

  return (
    <section className="py-24 px-6 border-t border-border-subtle">
      <div className="max-w-4xl mx-auto">
        <h2 className="f-display font-black text-3xl sm:text-4xl lg:text-5xl leading-[1.1] tracking-display mb-8 whitespace-pre-line">
          {t('title')}
        </h2>

        <div className="space-y-4">
          {items.map((item, i) => (
            <div key={i} className="flex items-center gap-4 p-4 border border-border-subtle bg-bg-1">
              <span className="text-accent shrink-0 text-xl leading-none">•</span>
              <span className="text-ui text-ink-1">{item}</span>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
