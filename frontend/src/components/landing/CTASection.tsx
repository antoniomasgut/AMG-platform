import { getTranslations } from 'next-intl/server';

export async function CTASection() {
  const t = await getTranslations('cta');

  return (
    <section id="cta" className="relative py-24">
      <div className="amg-grid-sm" />
      <div className="absolute inset-0 bg-gradient-radial from-accent/10 via-transparent to-transparent" />

      <div className="max-w-3xl mx-auto px-4 text-center relative z-10">
        <h2 className="f-display text-3xl sm:text-4xl font-black text-white mb-6">
          {t('title')}
        </h2>
        <p className="text-lg text-ink-2 mb-10 max-w-xl mx-auto leading-relaxed">
          {t('subtitle')}
        </p>
        <a
          href="mailto:hola@amg.cat"
          className="inline-block px-10 py-4 text-sm font-bold tracking-widest bg-accent text-white btn-clip hover:bg-accent-light transition-all hover:scale-105"
        >
          {t('button')}
        </a>
        <p className="text-ink-2/50 text-xs mt-4">o escriu-nos a hola@amg.cat</p>
      </div>
    </section>
  );
}
