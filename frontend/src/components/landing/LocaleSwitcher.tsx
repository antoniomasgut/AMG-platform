'use client';

import { useEffect, useRef, useState } from 'react';
import { useLocale } from 'next-intl';
import { useRouter, usePathname } from '@/i18n/navigation';

const LOCALES = [
  { code: 'ca' as const, label: 'CAT' },
  { code: 'es' as const, label: 'ES' },
  { code: 'en' as const, label: 'EN' },
  { code: 'de' as const, label: 'DE' },
];

export function LocaleSwitcher() {
  const locale = useLocale();
  const router = useRouter();
  const pathname = usePathname();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const switchLocale = (code: 'ca' | 'es' | 'en' | 'de') => {
    router.replace(pathname, { locale: code });
    setOpen(false);
  };

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
        <div className="absolute right-0 top-full mt-1 bg-bg-1 border border-border-base shadow-lg z-50 min-w-[72px]">
          {LOCALES.map(l => (
            <button
              key={l.code}
              onClick={() => switchLocale(l.code)}
              className={`w-full px-3 py-2 text-left f-mono text-label uppercase transition-colors hover:bg-bg-2 ${
                l.code === locale ? 'text-accent-light' : 'text-ink-2'
              }`}
            >
              {l.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
