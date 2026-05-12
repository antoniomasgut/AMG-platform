'use client';

import { useTranslations } from 'next-intl';
import { useState, useEffect } from 'react';

const STORAGE_KEY = 'amg_cookies_consent';

export function CookieConsentBanner() {
  const t = useTranslations('legal.cookies');
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const consent = localStorage.getItem(STORAGE_KEY);
    if (!consent) {
      setVisible(true);
    }
  }, []);

  function acceptAll() {
    localStorage.setItem(STORAGE_KEY, 'all');
    setVisible(false);
  }

  if (!visible) return null;

  return (
    <div className="fixed bottom-0 left-0 right-0 z-50 p-4">
      <div className="max-w-3xl mx-auto bg-bg-2 border border-white/10 rounded-lg p-4 sm:p-6 backdrop-blur-md shadow-2xl">
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4">
          <div className="flex-1">
            <p className="text-white font-semibold text-sm mb-1">{t('title')}</p>
            <p className="text-ink-2 text-xs leading-relaxed">{t('description')}</p>
          </div>
          <div className="flex items-center gap-3 shrink-0">
            <button
              onClick={acceptAll}
              className="px-4 py-2 text-xs font-bold tracking-wider bg-accent text-white btn-clip hover:bg-accent-light transition-colors"
            >
              {t('acceptAll')}
            </button>
            <button
              onClick={acceptAll}
              className="px-4 py-2 text-xs font-semibold text-ink-2 border border-white/10 rounded hover:border-accent/30 hover:text-accent transition-colors"
            >
              {t('accept')}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
