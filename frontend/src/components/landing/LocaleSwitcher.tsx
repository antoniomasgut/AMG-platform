'use client';

import { useLocale } from 'next-intl';
import { usePathname } from 'next/navigation';
import Link from 'next/link';

const LOCALES = [
  { code: 'ca', label: 'CAT' },
  { code: 'es', label: 'ES' },
  { code: 'en', label: 'EN' },
  { code: 'de', label: 'DE' },
];

export function LocaleSwitcher() {
  const locale = useLocale();
  const fullPathname = usePathname();
  const path = fullPathname.replace(/^\/(ca|es|en|de)/, '') || '';

  return (
    <div className="flex items-center gap-1">
      {LOCALES.map((l, i) => (
        <span key={l.code} className="flex items-center gap-1">
          {i > 0 && <span className="text-border-medium f-mono text-label">·</span>}
          <Link
            href={`/${l.code}${path}`}
            className={`f-mono text-label uppercase transition-colors ${
              l.code === locale
                ? 'text-accent-light'
                : 'text-ink-3 hover:text-ink-1'
            }`}
          >
            {l.label}
          </Link>
        </span>
      ))}
    </div>
  );
}
