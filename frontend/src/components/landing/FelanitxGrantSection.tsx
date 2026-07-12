import { useTranslations } from 'next-intl';

export function FelanitxGrantSection() {
  const t = useTranslations('felanitxGrant');
  const services = t.raw('services') as string[];

  return (
    <section id="ajut-felanitx" className="py-16 px-6 border-t border-border-subtle bg-accent/5">
      <div className="max-w-5xl mx-auto">
        <div className="flex items-center gap-2 mb-4">
          <div className="w-1.5 h-1.5 bg-accent" />
          <span className="f-mono text-label uppercase tracking-widest text-accent-light">{t('badge')}</span>
        </div>

        <h2 className="f-display font-black text-2xl sm:text-3xl lg:text-4xl leading-tight mb-3">
          {t('title')}
        </h2>
        <p className="f-mono text-sm text-accent-light font-semibold mb-5">⏳ {t('deadline')}</p>

        <p className="text-ui text-ink-2 max-w-2xl mb-6">{t('intro')}</p>

        <ul className="grid sm:grid-cols-2 gap-x-8 gap-y-2 mb-6 max-w-2xl">
          {services.map((s, i) => (
            <li key={i} className="flex items-start gap-2 text-data text-ink-2">
              <span className="text-accent font-bold">✓</span>
              <span>{s}</span>
            </li>
          ))}
        </ul>

        <p className="text-data text-ink-3 max-w-2xl mb-7">{t('covers')} {t('eligibility')}</p>

        <div className="flex flex-wrap items-center gap-3">
          <a
            href="tel:+34614492062"
            className="inline-flex items-center gap-2 amg-card card-clip px-5 py-3 f-mono text-sm font-semibold hover:border-border-medium transition-colors"
          >
            📞 {t('phone')}
          </a>
          <a
            href="https://wa.me/34654048164"
            className="inline-flex items-center gap-2 amg-card card-clip px-5 py-3 f-mono text-sm font-semibold hover:border-border-medium transition-colors"
          >
            💬 WhatsApp {t('whatsapp')}
          </a>
        </div>
      </div>
    </section>
  );
}
