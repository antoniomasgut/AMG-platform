'use client';

import { useTranslations } from 'next-intl';
import { Link } from '@/i18n/navigation';
import { LocaleSwitcher } from './LocaleSwitcher';

export function Footer() {
  const t = useTranslations('footer');
  const navT = useTranslations('nav');
  const year = new Date().getFullYear();

  return (
    <footer id="contacte" className="border-t border-white/5 bg-bg-0">
      <div className="max-w-6xl mx-auto px-4 py-12">
        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-8 mb-8">
          {/* Brand */}
          <div className="lg:col-span-1">
            <div className="flex items-center gap-2 mb-3">
              <span className="w-8 h-8 rounded-full bg-accent flex items-center justify-center text-white font-bold text-sm">
                A
              </span>
              <span className="f-display text-lg font-bold text-white">
                AMG<span className="text-accent">.</span>
              </span>
            </div>
            <p className="text-ink-2 text-sm leading-relaxed">
              {t('description')}
            </p>
          </div>

          {/* Services */}
          <div>
            <h4 className="text-white font-bold text-sm mb-4">{t('services')}</h4>
            <ul className="space-y-2 text-sm text-ink-2">
              <li><a href="#serveis" className="hover:text-accent transition-colors">Web Bàsica</a></li>
              <li><a href="#serveis" className="hover:text-accent transition-colors">Landing Premium</a></li>
              <li><a href="#serveis" className="hover:text-accent transition-colors">Automatització</a></li>
              <li><a href="#serveis" className="hover:text-accent transition-colors">Assistant IA</a></li>
              <li><a href="#serveis" className="hover:text-accent transition-colors">Transformació Digital</a></li>
            </ul>
          </div>

          {/* Legal */}
          <div>
            <h4 className="text-white font-bold text-sm mb-4">{t('legal')}</h4>
            <ul className="space-y-2 text-sm text-ink-2">
              <li>
                <Link href="/legal/avis-legal" className="hover:text-accent transition-colors">
                  {t('avisLegal')}
                </Link>
              </li>
              <li>
                <Link href="/legal/politica-privacitat" className="hover:text-accent transition-colors">
                  {t('privacitat')}
                </Link>
              </li>
              <li>
                <Link href="/legal/termes-servei" className="hover:text-accent transition-colors">
                  {t('termes')}
                </Link>
              </li>
            </ul>
          </div>

          {/* Locale & Contact */}
          <div>
            <h4 className="text-white font-bold text-sm mb-4">{t('contacte')}</h4>
            <ul className="space-y-2 text-sm text-ink-2 mb-4">
              <li>info@amgdl.com</li>
              <li>Mallorca, Illes Balears</li>
            </ul>
            <LocaleSwitcher />
          </div>
        </div>

        <div className="border-t border-white/5 pt-6 text-center text-xs text-ink-2/50">
          &copy; {year} {t('copyright')}
        </div>
      </div>
    </footer>
  );
}
