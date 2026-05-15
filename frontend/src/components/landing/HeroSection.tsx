import { useTranslations, useLocale } from 'next-intl';
import Link from 'next/link';

export function HeroSection() {
  const t = useTranslations('landing.hero');
  const locale = useLocale();

  return (
    <section className="relative min-h-dvh flex flex-col items-center justify-center px-6 pt-20 pb-16 text-center overflow-hidden">
      {/* Background */}
      <div className="absolute inset-0 amg-grid-sm pointer-events-none" />
      <div className="absolute inset-0 pointer-events-none" style={{
        background: 'radial-gradient(ellipse at 50% 40%, rgba(255,107,0,0.20), transparent 55%), radial-gradient(ellipse at 80% 80%, rgba(255,154,60,0.08), transparent 40%)',
      }} />

      <div className="relative z-10 max-w-4xl mx-auto">
        {/* Badge */}
        <div className="inline-flex items-center gap-2 border border-border-medium bg-accent-subtle px-4 py-1.5 mb-8">
          <span className="w-1.5 h-1.5 rounded-full bg-[#39d353] amg-blink" />
          <span className="f-mono text-label uppercase tracking-widest text-accent-light">{t('badge')}</span>
        </div>

        {/* Headline */}
        <h1 className="f-display font-black text-4xl sm:text-6xl lg:text-7xl leading-[1.0] tracking-display mb-4">
          {t('title')}<br />
          <span className="text-accent-light">{t('titleAccent')}</span>
        </h1>

        {/* Subtitle */}
        <p className="text-ui sm:text-md text-ink-1 max-w-2xl mx-auto mb-10 leading-relaxed">
          {t('subtitle')}
        </p>

        {/* CTAs */}
        <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
          <Link
            href={`/${locale}/login`}
            className="btn-clip bg-accent hover:bg-accent-light text-black font-semibold f-mono text-xs uppercase px-8 h-12 flex items-center gap-2 transition-colors"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M5 12h14M13 5l7 7-7 7" />
            </svg>
            {t('cta')}
          </Link>
          <a
            href="#services"
            className="f-mono text-xs uppercase text-ink-2 hover:text-ink-0 border border-border-base hover:border-border-medium px-8 h-12 flex items-center transition-colors"
          >
            {t('ctaSecondary')}
          </a>
        </div>

        {/* Stats */}
        <div className="mt-16 grid grid-cols-3 divide-x divide-border-subtle max-w-md mx-auto">
          <div className="flex flex-col items-center gap-1 px-4">
            <span className="flex items-center gap-1.5">
              <span className="w-1.5 h-1.5 rounded-full bg-[#39d353] shrink-0" />
              <span className="f-display font-black text-2xl sm:text-3xl text-ink-0">{t('stats.uptimeValue')}</span>
            </span>
            <span className="f-mono text-label uppercase text-ink-3 tracking-widest">{t('stats.uptimeLabel')}</span>
          </div>
          <div className="flex flex-col items-center gap-1 px-4">
            <span className="f-display font-black text-2xl sm:text-3xl text-ink-0">{t('stats.clientsValue')}</span>
            <span className="f-mono text-label uppercase text-ink-3 tracking-widest">{t('stats.clientsLabel')}</span>
          </div>
          <div className="flex flex-col items-center gap-1 px-4">
            <span className="f-display font-black text-2xl sm:text-3xl text-ink-0">{t('stats.automationsValue')}</span>
            <span className="f-mono text-label uppercase text-ink-3 tracking-widest">{t('stats.automationsLabel')}</span>
          </div>
        </div>
      </div>
    </section>
  );
}
