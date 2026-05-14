'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useTranslations, useLocale } from 'next-intl';
import { I } from '@/components/ui/icons';
import { forgotPassword } from '@/services/auth';

export default function ForgotPasswordPage() {
  const t = useTranslations('auth.forgotPassword');
  const locale = useLocale();
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim()) return;
    setLoading(true);
    setError('');
    try {
      await forgotPassword({ email });
      setSent(true);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : t('error'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full min-h-dvh bg-bg-0 overflow-hidden flex items-center justify-center p-6 sm:p-10 relative">
      <div className="fixed inset-0 amg-grid-sm pointer-events-none" />
      <div className="fixed inset-0 pointer-events-none" style={{
        background: 'radial-gradient(ellipse at 30% 50%, rgba(255,107,0,0.18), transparent 45%)',
      }} />

      <div className="relative z-10 w-full max-w-[420px]">
        <div className="amg-card card-clip p-6 sm:p-8">
          <div className="flex items-center gap-2 mb-1">
            <div className="w-1.5 h-1.5 bg-accent" />
            <span className="f-mono text-label uppercase tracking-widest text-accent-light">
              {sent ? t('linkSentBadge') : t('badge')}
            </span>
          </div>

          {sent ? (
            <>
              <div className="w-16 h-16 bg-[rgba(57,211,83,0.12)] border border-[#39d353] flex items-center justify-center mb-5 mx-auto">
                <I.Check size={24} stroke="#39d353" />
              </div>
              <h2 className="f-display font-black text-2xl text-center">{t('checkEmail')}</h2>
              <p className="text-ui text-ink-1 mt-2 text-center">{t('emailSentTo')}</p>
              <div className="f-mono text-sm text-accent-light text-center mt-1">{email}</div>

              <div className="mt-6 p-3 border-l-2 border-l-[#58a6ff] bg-[rgba(88,166,255,0.05)] flex gap-3">
                <I.Clock size={14} stroke="#58a6ff" className="shrink-0 mt-0.5" />
                <div className="text-data text-ink-1">
                  {t('linkExpires')} <span className="text-info f-mono">{t('minutes')}</span>.{' '}
                  {t('checkSpam')}
                </div>
              </div>

              <Link
                href={`/${locale}/login`}
                className="mt-6 block w-full text-center f-mono text-caption uppercase text-ink-3 hover:text-accent-light transition"
              >
                {t('backToLogin')}
              </Link>
            </>
          ) : (
            <>
              <h2 className="f-display font-black text-2xl mb-2">{t('title')}</h2>
              <p className="text-ui text-ink-1 mb-6">{t('subtitle')}</p>

              <form onSubmit={handleSubmit} className="space-y-4">
                <label className="block">
                  <span className="block f-mono uppercase text-label tracking-label text-ink-1 mb-1.5">{t('emailLabel')}</span>
                  <div className="relative flex items-center h-10 bg-bg-2/80 border border-border-base focus-within:border-accent transition">
                    <div className="pl-3 text-ink-3"><I.Mail size={14} /></div>
                    <input
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="tu@empresa.com"
                      autoFocus
                      className="flex-1 bg-transparent outline-none px-3 text-sm text-ink-0 placeholder:text-ink-3"
                    />
                  </div>
                </label>

                {error && (
                  <div className="flex items-start gap-2 p-3 border-l-2 border-l-danger bg-[rgba(255,68,68,0.05)]">
                    <span className="text-data text-danger-light">{error}</span>
                  </div>
                )}

                <button
                  type="submit"
                  disabled={loading || !email.trim()}
                  className="w-full h-10 f-mono text-xs uppercase btn-clip bg-accent hover:bg-accent-light text-black font-semibold transition-all disabled:opacity-40 disabled:cursor-not-allowed inline-flex items-center justify-center gap-2"
                >
                  {loading ? (
                    <span className="w-3 h-3 border-2 border-black/60 border-t-transparent rounded-full animate-spin" />
                  ) : (
                    <I.ArrowRight size={14} />
                  )}
                  {loading ? t('sending') : t('sendLink')}
                </button>
              </form>

              <Link
                href={`/${locale}/login`}
                className="mt-4 block w-full text-center f-mono text-caption uppercase text-ink-3 hover:text-accent-light transition"
              >
                {t('backToLogin')}
              </Link>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
