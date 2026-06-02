'use client';

import Link from 'next/link';
import { useLocale, useTranslations } from 'next-intl';

interface OnboardingCompleteProps {
  userName: string;
  onGoToDashboard: () => void;
}

export function OnboardingComplete({ userName, onGoToDashboard }: OnboardingCompleteProps) {
  const locale = useLocale();
  const t = useTranslations('onboarding');

  return (
    <div className="amg-card card-clip p-8 text-center animate-slide-up" role="region" aria-label={t('complete_title')}>
      <div className="w-16 h-16 bg-emerald-900/30 border border-emerald-500/40 flex items-center justify-center mx-auto mb-4">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#4ade80" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
          <polyline points="20 6 9 17 4 12" />
        </svg>
      </div>
      <div className="f-display font-bold text-2xl mb-2">{t('complete_title')}, {userName.toUpperCase()}!</div>
      <p className="text-ink-2 text-sm mb-6">{t('complete_subtitle')}</p>
      <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
        <button
          onClick={onGoToDashboard}
          className="f-mono text-label uppercase btn-clip bg-accent hover:bg-accent-light text-black font-semibold px-6 h-10 transition-colors"
        >
          {t('complete_cta')} →
        </button>
        <Link
          href={`/${locale}/portal/admin/vault`}
          className="f-mono text-label uppercase text-ink-2 hover:text-ink-0 border border-border-base hover:border-border-medium px-6 h-10 flex items-center transition-colors"
        >
          {t('vault_pending')}
        </Link>
      </div>
    </div>
  );
}
