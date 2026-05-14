import { useTranslations, useLocale } from 'next-intl';
import Link from 'next/link';

export function CTASection() {
  const t = useTranslations('landing.cta');
  const locale = useLocale();

  return (
    <section id="contact" className="py-24 px-6 border-t border-border-subtle">
      <div className="max-w-3xl mx-auto text-center">
        <div className="amg-grid p-12 sm:p-16 border border-border-medium relative overflow-hidden">
          <div className="absolute inset-0 pointer-events-none" style={{
            background: 'radial-gradient(ellipse at 50% 0%, rgba(255,107,0,0.15), transparent 60%)',
          }} />

          <div className="relative z-10">
            <h2 className="f-display font-black text-3xl sm:text-4xl lg:text-5xl leading-[1.1] tracking-display mb-4 whitespace-pre-line">
              {t('title')}
            </h2>
            <p className="text-ui text-ink-1 mb-10">{t('subtitle')}</p>

            <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
              <a
                href="mailto:hola@amg.digital"
                className="btn-clip bg-accent hover:bg-accent-light text-black font-semibold f-mono text-xs uppercase px-8 h-12 flex items-center gap-2 transition-colors"
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="2" y="4" width="20" height="16" rx="2" />
                  <path d="m22 7-10 6L2 7" />
                </svg>
                {t('button')}
              </a>
              <Link
                href={`/${locale}/login`}
                className="f-mono text-xs uppercase text-ink-2 hover:text-ink-0 border border-border-base hover:border-border-medium px-8 h-12 flex items-center transition-colors"
              >
                {t('buttonSecondary')}
              </Link>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
