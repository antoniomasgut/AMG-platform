'use client';

import { useEffect, useRef, useState } from 'react';
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
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  // Strip the locale prefix to get the locale-independent path
  const pathWithoutLocale = fullPathname.replace(/^\/(ca|es|en|de)/, '') || '/';

  const current = LOCALES.find(l => l.code === locale) ?? LOCALES[0];

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setOpen(v => !v)}
        className="f-mono text-label uppercase text-ink-2 hover:text-ink-0 border border-border-base hover:border-border-medium px-2.5 h-7 flex items-center gap-1.5 transition-colors"
        aria-label="Canviar idioma"
        aria-expanded={open}
      >
        {current.label}
        <svg width="8" height="8" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="m6 9 6 6 6-6" />
        </svg>
      </button>

      {open && (
        <div className="absolute right-0 top-full mt-1 bg-bg-1 border border-border-base shadow-lg z-[100] min-w-[72px]">
          {LOCALES.map(l => (
            <Link
              key={l.code}
              href={`/${l.code}${pathWithoutLocale === '/' ? '' : pathWithoutLocale}`}
              onClick={() => setOpen(false)}
              className={`block px-3 py-2 f-mono text-label uppercase transition-colors hover:bg-bg-2 ${
                l.code === locale ? 'text-accent-light' : 'text-ink-2'
              }`}
            >
              {l.label}
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
