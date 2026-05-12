import { getTranslations } from 'next-intl/server';

export async function ProblemSection() {
  const t = await getTranslations('problem');

  const items = t.raw('items') as string[];

  return (
    <section className="relative py-24 bg-bg-1">
      <div className="max-w-5xl mx-auto px-4">
        <h2 className="f-display text-3xl sm:text-4xl font-black text-white text-center mb-16">
          {t('title')}
        </h2>

        <div className="grid sm:grid-cols-2 gap-6">
          {items.map((item: string, i: number) => (
            <div
              key={i}
              className="amg-card p-6 flex items-start gap-4 border border-white/5 hover:border-accent/20 transition-colors group"
            >
              <div className="w-10 h-10 rounded-full bg-accent/10 flex items-center justify-center shrink-0 group-hover:bg-accent/20 transition-colors">
                <span className="text-accent font-bold text-sm">{i + 1}</span>
              </div>
              <p className="text-ink-2 text-base leading-relaxed">{item}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
