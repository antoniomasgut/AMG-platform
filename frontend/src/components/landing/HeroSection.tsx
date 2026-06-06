'use client';

import { useTranslations, useLocale } from 'next-intl';
import Link from 'next/link';

export function HeroSection() {
  const t = useTranslations('landing.hero');
  const locale = useLocale();

  return (
    <section className="relative min-h-dvh flex flex-col items-center justify-center px-6 pt-20 pb-16 text-center overflow-hidden">
      {/* Subtle gradient background */}
      <div className="absolute inset-0 pointer-events-none" style={{
        background: 'radial-gradient(ellipse at 50% 40%, rgba(255,107,0,0.08), transparent 55%), radial-gradient(ellipse at 80% 80%, rgba(255,154,60,0.04), transparent 40%)',
      }} />

      <div className="relative z-10 max-w-4xl mx-auto">
        {/* Badge */}
        <div className="inline-flex items-center gap-2 border border-border-medium bg-accent-subtle px-4 py-1.5 mb-8">
          <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="#FF6B00" strokeWidth="2.5" strokeLinecap="round">
            <path d="M20 6L9 17l-5-5" />
          </svg>
          <span className="f-mono text-label uppercase tracking-widest text-accent-light">{t('badge')}</span>
        </div>

        {/* Headline */}
        <h1 className="font-bold text-4xl sm:text-5xl lg:text-6xl leading-[1.1] tracking-tight mb-6">
          {t('title')}<br />
          <span className="text-accent-light">{t('titleAccent')}</span>
        </h1>

        {/* Subtitle */}
        <p className="text-lg sm:text-xl text-ink-1 max-w-2xl mx-auto mb-10 leading-relaxed">
          {t('subtitle')}
        </p>

        {/* CTAs */}
        <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
          <Link
            href={`/${locale}/login`}
            className="bg-accent hover:bg-accent-light text-black font-semibold f-mono text-xs uppercase px-8 h-12 flex items-center gap-2 transition-colors"
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
        <div className="mt-16 grid grid-cols-3 max-w-md mx-auto gap-px bg-border-subtle overflow-hidden">
          {(
            [
              { value: t('stats.uptimeValue'), label: t('stats.uptimeLabel') },
              { value: t('stats.clientsValue'), label: t('stats.clientsLabel') },
              { value: t('stats.automationsValue'), label: t('stats.automationsLabel') },
            ] as const
          ).map((stat, i) => (
            <div key={i} className="bg-bg-0 py-4 px-2 flex flex-col items-center">
              <span className="font-bold text-xl sm:text-2xl text-accent-light">{stat.value}</span>
              <span className="f-mono text-data text-ink-3 uppercase tracking-wider mt-1">{stat.label}</span>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
