'use client';

import { Suspense, useState } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { useTranslations, useLocale } from 'next-intl';
import Link from 'next/link';
import { AMGButton } from '@/components/ui/button';
import { AMGLogo } from '@/components/ui/AMGLogo';
import { I } from '@/components/ui/icons';
import { resetPassword } from '@/services/auth';

function ResetPasswordForm() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const locale = useLocale();
  const t = useTranslations('auth.resetPassword');
  const token = searchParams.get('token');

  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!token) { setError(t('errors.invalidToken')); return; }
    if (password.length < 4) { setError(t('errors.minLength')); return; }
    if (password !== confirm) { setError(t('errors.mismatch')); return; }

    setLoading(true);
    try {
      await resetPassword({ token, password });
      setSuccess(true);
      setTimeout(() => router.push(`/${locale}/login`), 3000);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : t('errors.generic'));
    } finally {
      setLoading(false);
    }
  };

  if (!token) {
    return (
      <div className="w-full min-h-dvh bg-bg-0 flex items-center justify-center p-6">
        <div className="amg-card card-clip p-8 max-w-[420px] w-full text-center">
          <div className="w-16 h-16 bg-[rgba(255,68,68,0.12)] border border-[rgba(255,68,68,0.35)] flex items-center justify-center mx-auto mb-5">
            <I.AlertCircle size={24} stroke="#ff6666" />
          </div>
          <h2 className="f-display font-black text-2xl">{t('invalidLink')}</h2>
          <p className="text-ui text-ink-1 mt-2">{t('invalidLinkText')}</p>
          <AMGButton onClick={() => router.push(`/${locale}/login`)} className="mt-6">{t('backToLogin')}</AMGButton>
        </div>
      </div>
    );
  }

  if (success) {
    return (
      <div className="w-full min-h-dvh bg-bg-0 flex items-center justify-center p-6">
        <div className="amg-card card-clip p-8 max-w-[420px] w-full text-center">
          <div className="w-16 h-16 bg-[rgba(57,211,83,0.12)] border border-[#39d353] flex items-center justify-center mx-auto mb-5">
            <I.Check size={24} stroke="#39d353" />
          </div>
          <h2 className="f-display font-black text-2xl">{t('successTitle')}</h2>
          <p className="text-ui text-ink-1 mt-2">{t('successText')}</p>
          <AMGButton onClick={() => router.push(`/${locale}/login`)} className="mt-6">{t('goToLogin')}</AMGButton>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full min-h-dvh bg-bg-0 overflow-hidden flex items-center justify-center p-6 sm:p-10 relative">
      <div className="fixed inset-0 amg-grid-sm pointer-events-none" />
      <div className="fixed inset-0 pointer-events-none" style={{
        background: 'radial-gradient(ellipse at 30% 50%, rgba(255,107,0,0.18), transparent 45%)',
      }} />

      <div className="relative z-10 w-full max-w-[420px]">
        <div className="flex justify-center mb-8">
          <Link href={`/${locale}`}>
            <AMGLogo className="h-9 w-auto" />
          </Link>
        </div>
        <div className="amg-card card-clip p-6 sm:p-8">
          <div className="flex items-center gap-2 mb-1">
            <div className="w-1.5 h-1.5 bg-accent" />
            <span className="f-mono text-label uppercase tracking-widest text-accent-light">{t('badge')}</span>
          </div>

          <h2 className="f-display font-black text-2xl mb-2">{t('title')}</h2>
          <p className="text-ui text-ink-1 mb-6">{t('subtitle')}</p>

          <form onSubmit={handleSubmit} className="space-y-4">
            <label className="block">
              <span className="block f-mono uppercase text-label tracking-label text-ink-1 mb-1.5">{t('newPasswordLabel')}</span>
              <div className="relative flex items-center h-10 bg-bg-2/80 border border-border-base focus-within:border-accent transition">
                <div className="pl-3 text-ink-3"><I.Lock size={14} /></div>
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  autoFocus
                  className="flex-1 bg-transparent outline-none px-3 text-sm text-ink-0 placeholder:text-ink-3"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="pr-3 text-ink-3 hover:text-ink-0 transition"
                >
                  {showPassword ? <I.EyeOff size={14} /> : <I.Eye size={14} />}
                </button>
              </div>
            </label>

            <label className="block">
              <span className="block f-mono uppercase text-label tracking-label text-ink-1 mb-1.5">{t('confirmLabel')}</span>
              <div className="relative flex items-center h-10 bg-bg-2/80 border border-border-base focus-within:border-accent transition">
                <div className="pl-3 text-ink-3"><I.Lock size={14} /></div>
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={confirm}
                  onChange={(e) => setConfirm(e.target.value)}
                  placeholder="••••••••"
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
              disabled={loading || !password || !confirm}
              className="w-full h-10 f-mono text-xs uppercase btn-clip bg-accent hover:bg-accent-light text-black font-semibold transition-all disabled:opacity-40 disabled:cursor-not-allowed inline-flex items-center justify-center gap-2"
            >
              {loading ? (
                <span className="w-3 h-3 border-2 border-black/60 border-t-transparent rounded-full animate-spin" />
              ) : (
                <I.ArrowRight size={14} />
              )}
              {loading ? t('updating') : t('update')}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={
      <div className="w-full min-h-dvh bg-bg-0 flex items-center justify-center">
        <span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
      </div>
    }>
      <ResetPasswordForm />
    </Suspense>
  );
}
