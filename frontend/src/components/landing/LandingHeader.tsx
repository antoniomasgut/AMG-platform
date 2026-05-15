'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useTranslations, useLocale } from 'next-intl';
import { LocaleSwitcher } from './LocaleSwitcher';
import { AMGLogo } from '@/components/ui/AMGLogo';

export function LandingHeader() {
  const t = useTranslations('nav');
  const locale = useLocale();
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <header className="fixed top-0 left-0 right-0 z-50 border-b border-border-base bg-bg-0/90 backdrop-blur-sm">
      <div className="max-w-6xl mx-auto px-6 h-14 flex items-center justify-between">
        {/* Logo */}
        <Link href={`/${locale}`} className="flex items-center">
          <AMGLogo className="h-10 w-auto" />
        </Link>

        {/* Desktop nav */}
        <nav className="hidden lg:flex items-center gap-6" aria-label="Navegació principal">
          {[
            { key: 'services', href: '#services' },
            { key: 'pricing', href: '#pricing' },
            { key: 'howItWorks', href: '#how-it-works' },
            { key: 'contact', href: '#contact' },
          ].map(({ key, href }) => (
            <a
              key={key}
              href={href}
              className="f-mono text-caption uppercase text-ink-2 hover:text-ink-0 tracking-caption transition-colors"
            >
              {t(key as 'services' | 'pricing' | 'howItWorks' | 'contact')}
            </a>
          ))}
        </nav>

        {/* Right side */}
        <div className="flex items-center gap-3">
          <LocaleSwitcher />
          <Link
            href={`/${locale}/login`}
            className="f-mono text-caption uppercase btn-clip bg-accent hover:bg-accent-light text-black font-semibold px-4 h-8 flex items-center transition-colors"
          >
            {t('login')}
          </Link>

          {/* Mobile hamburger */}
          <button
            className="lg:hidden w-8 h-8 flex flex-col items-center justify-center gap-1.5"
            onClick={() => setMenuOpen(!menuOpen)}
            aria-label="Obrir menú"
          >
            <span className={`w-5 h-0.5 bg-ink-1 transition-all ${menuOpen ? 'rotate-45 translate-y-2' : ''}`} />
            <span className={`w-5 h-0.5 bg-ink-1 transition-all ${menuOpen ? 'opacity-0' : ''}`} />
            <span className={`w-5 h-0.5 bg-ink-1 transition-all ${menuOpen ? '-rotate-45 -translate-y-2' : ''}`} />
          </button>
        </div>
      </div>

      {/* Mobile menu */}
      {menuOpen && (
        <div className="lg:hidden border-t border-border-base bg-bg-1 px-6 py-4 flex flex-col gap-4">
          {[
            { key: 'services', href: '#services' },
            { key: 'pricing', href: '#pricing' },
            { key: 'howItWorks', href: '#how-it-works' },
            { key: 'contact', href: '#contact' },
          ].map(({ key, href }) => (
            <a
              key={key}
              href={href}
              onClick={() => setMenuOpen(false)}
              className="f-mono text-caption uppercase text-ink-1 hover:text-ink-0 tracking-caption transition-colors"
            >
              {t(key as 'services' | 'pricing' | 'howItWorks' | 'contact')}
            </a>
          ))}
        </div>
      )}
    </header>
  );
}
