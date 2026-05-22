'use client';

import { useState, useEffect } from 'react';
import { useTranslations, useLocale } from 'next-intl';
import Link from 'next/link';

const STORAGE_KEY = 'cookie_consent';

export function CookieConsentBanner() {
  const t = useTranslations('cookies');
  const locale = useLocale();
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    if (!localStorage.getItem(STORAGE_KEY)) {
      setVisible(true);
    }
  }, []);

  const accept = () => {
    localStorage.setItem(STORAGE_KEY, 'all');
    setVisible(false);
  };

  const reject = () => {
    localStorage.setItem(STORAGE_KEY, 'necessary');
    setVisible(false);
  };

  if (!visible) return null;

  return (
    <div
      role="dialog"
      aria-label="Consentiment de cookies"
      className="fixed bottom-0 left-0 right-0 z-50 border-t border-border-base bg-bg-1/95 backdrop-blur-sm p-4 sm:p-6"
    >
      <div className="max-w-5xl mx-auto flex flex-col sm:flex-row items-start sm:items-center gap-4 justify-between">
        <p className="text-data text-ink-1 max-w-2xl">
          {t('message')}{' '}
          <Link href={`/${locale}/legal/cookies`} className="underline hover:text-accent-light transition-colors">
            {t('moreInfo')}
          </Link>
        </p>
        <div className="flex items-center gap-3 shrink-0">
          <button
            onClick={reject}
            className="f-mono text-caption uppercase text-ink-3 hover:text-ink-1 transition-colors"
          >
            {t('reject')}
          </button>
          <button
            onClick={accept}
            className="btn-clip bg-accent hover:bg-accent-light text-black font-semibold f-mono text-caption uppercase px-5 h-9 flex items-center transition-colors"
          >
            {t('accept')}
          </button>
        </div>
      </div>
    </div>
  );
}
