'use client';

import { useTranslations, useLocale } from 'next-intl';
import Link from 'next/link';

const SECTORS = [
  { key: 'citaPrevia', path: '/cita-previa' },
  { key: 'pressupostos', path: '/pressupostos' },
  { key: 'despatxos', path: '/despatxos' },
] as const;

export function SectorsSection() {
  const t = useTranslations('sectors');
  const locale = useLocale();

  return (
    <section className="py-20 sm:py-24 px-6 border-t border-border-subtle">
      <div className="max-w-5xl mx-auto">
        <div className="flex items-center gap-2 mb-5">
          <div className="w-2 h-2 bg-accent shrink-0" />
          <span className="f-mono text-label uppercase tracking-widest text-ink-2">
            {t('subtitle')}
          </span>
        </div>
        <h2 className="font-bold text-2xl sm:text-3xl lg:text-4xl uppercase leading-tight tracking-tight mb-10 sm:mb-12 text-ink-0">
          {t('title')}
        </h2>
        <div
          className="grid grid-cols-1 sm:grid-cols-3 border border-border-subtle"
          style={{ gap: '1px', background: 'var(--color-border-subtle)' }}
        >
          {SECTORS.map(({ key, path }) => (
            <Link
              key={key}
              href={`/${locale}${path}`}
              className="group bg-bg-0 p-6 sm:p-8 hover:bg-bg-1 transition-colors flex flex-col gap-4"
            >
              <h3 className="font-semibold text-lg sm:text-xl text-ink-0 group-hover:text-accent transition-colors">
                {t(`${key}.label`)}
              </h3>
              <p className="text-md text-ink-1 leading-relaxed flex-1">
                {t(`${key}.desc`)}
              </p>
              <span className="f-mono text-label text-accent mt-auto">
                {t('cta')}
              </span>
            </Link>
          ))}
        </div>
      </div>
    </section>
  );
}
