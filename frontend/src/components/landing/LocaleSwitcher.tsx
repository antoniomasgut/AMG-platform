'use client';

import { usePathname, useRouter } from '@/i18n/navigation';
import { useTransition } from 'react';
import { routing } from '@/i18n/routing';

const flags: Record<string, string> = {
  ca: '🇦🇩',
  es: '🇪🇸',
  en: '🇬🇧',
  de: '🇩🇪',
};

export function LocaleSwitcher() {
  const pathname = usePathname();
  const router = useRouter();
  const [isPending, startTransition] = useTransition();

  function switchLocale(next: string) {
    startTransition(() => {
      router.replace(pathname, { locale: next });
    });
  }

  return (
    <div className="flex items-center gap-1">
      {routing.locales.map((locale) => (
        <button
          key={locale}
          onClick={() => switchLocale(locale)}
          disabled={isPending}
          className="px-1.5 py-0.5 text-xs font-medium rounded transition-colors hover:text-accent disabled:opacity-50"
          title={locale.toUpperCase()}
        >
          {flags[locale] || locale.toUpperCase()}
        </button>
      ))}
    </div>
  );
}
