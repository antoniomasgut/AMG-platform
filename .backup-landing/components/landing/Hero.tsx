'use client';

import { useTranslations } from 'next-intl';

export function Hero() {
  const t = useTranslations('hero');

  return (
    <section className="relative min-h-screen flex items-center justify-center overflow-hidden pt-16">
      {/* Grid background */}
      <div className="amg-grid" />

      {/* Radial gradients */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-accent/10 rounded-full blur-[120px]" />
      <div className="absolute bottom-1/4 right-1/4 w-64 h-64 bg-accent/5 rounded-full blur-[100px]" />

      {/* Content */}
      <div className="relative z-10 max-w-5xl mx-auto px-4 text-center">
        <div className="inline-block px-3 py-1 rounded-full border border-accent/20 bg-accent/5 text-accent text-xs font-semibold tracking-wider mb-6">
          Per a pimes de Mallorca
        </div>

        <h1 className="f-display text-4xl sm:text-5xl md:text-6xl lg:text-7xl font-black leading-tight mb-6">
          <span className="text-white">{t('title')}</span>
          <br />
          <span className="text-accent">{t('titleAccent')}</span>
        </h1>

        <p className="text-lg sm:text-xl text-ink-2 max-w-2xl mx-auto mb-10 leading-relaxed">
          {t('subtitle')}
        </p>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
          <a
            href="#cta"
            className="px-8 py-4 text-sm font-bold tracking-widest bg-accent text-white btn-clip hover:bg-accent-light transition-all hover:scale-105"
          >
            {t('cta')}
          </a>
          <a
            href="#serveis"
            className="px-8 py-4 text-sm font-bold tracking-widest border border-ink-2/20 text-ink-2 btn-clip hover:border-accent/50 hover:text-accent transition-all"
          >
            VEURE SERVEIS
          </a>
        </div>

        {/* Scroll indicator */}
        <div className="mt-16 flex flex-col items-center gap-2 text-ink-2/50 animate-bounce">
          <span className="text-xs tracking-widest">{t('scrollDown')}</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M7 13l5 5 5-5M7 6l5 5 5-5" />
          </svg>
        </div>
      </div>
    </section>
  );
}
