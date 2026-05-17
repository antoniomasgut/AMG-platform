'use client';

import { useTranslations } from 'next-intl';
import { useState } from 'react';
import { Link } from '@/i18n/navigation';
import { LocaleSwitcher } from './LocaleSwitcher';

export function Header() {
  const t = useTranslations('nav');
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <header className="fixed top-0 left-0 right-0 z-50 bg-bg-0/80 backdrop-blur-md border-b border-white/5">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link href="/" className="flex items-center gap-2">
            <span className="w-8 h-8 rounded-full bg-accent flex items-center justify-center text-white font-bold text-sm">
              A
            </span>
            <span className="f-display text-lg font-bold text-white hidden sm:block">
              AMG<span className="text-accent">.</span>
            </span>
          </Link>

          {/* Desktop nav */}
          <nav className="hidden md:flex items-center gap-8 text-sm text-ink-2">
            <a href="#serveis" className="hover:text-accent transition-colors">
              {t('serveis')}
            </a>
            <a href="#com-funciona" className="hover:text-accent transition-colors">
              {t('comFunciona')}
            </a>
            <a href="#preus" className="hover:text-accent transition-colors">
              {t('preus')}
            </a>
            <a href="#contacte" className="hover:text-accent transition-colors">
              {t('contacte')}
            </a>
          </nav>

          {/* Right side */}
          <div className="flex items-center gap-3">
            <LocaleSwitcher />
            <Link
              href="/login"
              className="hidden sm:inline-flex text-xs font-semibold tracking-wider text-ink-2 hover:text-white transition-colors"
            >
              {t('iniciarSessio')}
            </Link>
            <a
              href="#cta"
              className="hidden sm:inline-flex px-4 py-2 text-xs font-bold tracking-wider bg-accent text-white btn-clip hover:bg-accent-light transition-colors"
            >
              SOL·LICITAR DEMO
            </a>

            {/* Mobile hamburger */}
            <button
              onClick={() => setMenuOpen(!menuOpen)}
              className="md:hidden p-2 text-ink-2"
              aria-label="Menu"
            >
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                {menuOpen ? (
                  <path d="M6 6l12 12M6 18L18 6" />
                ) : (
                  <path d="M3 12h18M3 6h18M3 18h18" />
                )}
              </svg>
            </button>
          </div>
        </div>
      </div>

      {/* Mobile menu */}
      {menuOpen && (
        <div className="md:hidden bg-bg-1 border-t border-white/5">
          <div className="px-4 py-4 space-y-3">
            <a href="#serveis" onClick={() => setMenuOpen(false)} className="block text-sm text-ink-2 py-2">
              {t('serveis')}
            </a>
            <a href="#com-funciona" onClick={() => setMenuOpen(false)} className="block text-sm text-ink-2 py-2">
              {t('comFunciona')}
            </a>
            <a href="#preus" onClick={() => setMenuOpen(false)} className="block text-sm text-ink-2 py-2">
              {t('preus')}
            </a>
            <a href="#contacte" onClick={() => setMenuOpen(false)} className="block text-sm text-ink-2 py-2">
              {t('contacte')}
            </a>
            <Link href="/login" onClick={() => setMenuOpen(false)} className="block text-sm font-semibold text-accent py-2">
              {t('iniciarSessio')}
            </Link>
          </div>
        </div>
      )}
    </header>
  );
}
