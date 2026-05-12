import { getTranslations } from 'next-intl/server';
import { Link } from '@/i18n/navigation';

export default async function PoliticaPrivacitatPage() {
  const t = await getTranslations('legal.privacitat');

  const sections: Array<{ key: string }> = [
    { key: 'controller' },
    { key: 'dataCollected' },
    { key: 'purpose' },
    { key: 'legalBasis' },
    { key: 'rights' },
    { key: 'security' },
  ];

  return (
    <>
      <Link href="/" className="f-mono text-xs uppercase text-accent hover:text-accent-light transition inline-flex items-center gap-1.5 mb-8">
        ← {t('backToHome')}
      </Link>
      <h1 className="f-display font-black text-3xl text-white mb-8">{t('title')}</h1>
      <div className="space-y-8">
        {sections.map(({ key }) => (
          <section key={key}>
            <h2 className="f-display font-bold text-lg text-white mb-3">{t(`sections.${key}.title`)}</h2>
            <p className="text-sm text-ink-2 leading-relaxed">{t(`sections.${key}.content`)}</p>
          </section>
        ))}
      </div>
      <div className="mt-12 pt-8 border-t border-white/5">
        <p className="text-xs text-ink-2/50">{t('lastUpdated')}: 01/05/2026</p>
      </div>
    </>
  );
}
