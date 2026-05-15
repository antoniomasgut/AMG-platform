import { useTranslations, useLocale } from 'next-intl';
import Link from 'next/link';
import { AMGLogo } from '@/components/ui/AMGLogo';

export function LandingFooter() {
  const t = useTranslations('landing.footer');
  const locale = useLocale();
  const year = new Date().getFullYear();

  return (
    <footer className="border-t border-border-base py-10 px-6">
      <div className="max-w-6xl mx-auto">
        <div className="flex flex-col sm:flex-row items-start justify-between gap-8">
          {/* Brand */}
          <div>
            <Link href={`/${locale}`} className="inline-block mb-3">
              <AMGLogo className="h-8 w-auto" />
            </Link>
            <p className="f-mono text-label text-ink-3 max-w-[200px]">{t('description')}</p>
          </div>

          {/* Legal links */}
          <div>
            <div className="f-mono text-label uppercase text-ink-3 mb-3 tracking-widest">{t('legal')}</div>
            <div className="flex flex-col gap-2">
              {[
                { key: 'avisLegal', href: `/${locale}/legal/avis-legal` },
                { key: 'privacitat', href: `/${locale}/legal/politica-privacitat` },
                { key: 'cookies', href: `/${locale}/legal/cookies` },
                { key: 'termes', href: `/${locale}/legal/termes-servei` },
              ].map(({ key, href }) => (
                <Link
                  key={key}
                  href={href}
                  className="f-mono text-label text-ink-3 hover:text-ink-1 transition-colors"
                >
                  {t(`links.${key}` as 'links.avisLegal' | 'links.privacitat' | 'links.cookies' | 'links.termes')}
                </Link>
              ))}
            </div>
          </div>

          {/* Contact */}
          <div>
            <div className="f-mono text-label uppercase text-ink-3 mb-3 tracking-widest">{t('contact')}</div>
            <div className="flex flex-col gap-2">
              <a href="mailto:hola@amg.digital" className="f-mono text-label text-ink-3 hover:text-ink-1 transition-colors">
                hola@amg.digital
              </a>
              <a href="tel:+34971000000" className="f-mono text-label text-ink-3 hover:text-ink-1 transition-colors">
                971 000 000
              </a>
            </div>
          </div>
        </div>

        <div className="mt-8 pt-6 border-t border-border-subtle f-mono text-label text-ink-3 text-center">
          © {year} {t('copyright')}
        </div>
      </div>
    </footer>
  );
}
