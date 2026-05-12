'use client';

import { useState, FormEvent, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { Link } from '@/i18n/navigation';
import { useTranslations } from 'next-intl';

function ResetForm() {
  const t = useTranslations('auth.resetPassword');
  const searchParams = useSearchParams();
  const token = searchParams.get('token');

  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [success, setSuccess] = useState(false);

  if (!token) {
    return (
      <div className="text-center">
        <div className="w-16 h-16 bg-[rgba(255,68,68,0.12)] border border-[#ff4444] flex items-center justify-center mx-auto mb-5">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#ff6666" strokeWidth="1.6">
            <circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/>
          </svg>
        </div>
        <h1 className="f-display font-black text-2xl text-white">{t('invalidLinkTitle')}</h1>
        <p className="text-[13px] text-[#94a3b8] mt-2">{t('invalidLinkDesc')}</p>
        <Link href="/login" className="mt-6 inline-block f-mono text-xs uppercase text-[#FF9A3C] hover:text-[#FF6B00] transition">
          ← {t('goToLogin')}
        </Link>
      </div>
    );
  }

  if (success) {
    return (
      <div className="text-center">
        <div className="w-16 h-16 bg-[rgba(57,211,83,0.12)] border border-[#39d353] flex items-center justify-center mx-auto mb-5">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#39d353" strokeWidth="1.6">
            <path d="M20 6L9 17l-5-5"/>
          </svg>
        </div>
        <h1 className="f-display font-black text-2xl text-white">{t('successTitle')}</h1>
        <p className="text-[13px] text-[#94a3b8] mt-2">{t('successDesc')}</p>
      </div>
    );
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setErrorMsg('');

    if (password.length < 4) {
      setErrorMsg(t('errorMinLength'));
      return;
    }
    if (password !== confirm) {
      setErrorMsg(t('errorMismatch'));
      return;
    }

    setSubmitting(true);
    try {
      setSuccess(true);
    } catch (_err) {
      setErrorMsg(t('errorGeneral'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <div className="flex items-center gap-2 mb-1">
        <div className="w-1.5 h-1.5 bg-[#FF6B00]"></div>
        <span className="f-mono text-[10px] uppercase tracking-[0.2em] text-[#FF9A3C]">{t('title')}</span>
      </div>
      <h1 className="f-display font-black text-2xl text-white">{t('title')}</h1>
      <p className="text-[13px] text-[#94a3b8] mb-6">{t('subtitle')}</p>

      <form onSubmit={handleSubmit} className="space-y-4">
        <label className="block">
          <span className="block f-mono uppercase text-[10px] tracking-[0.14em] text-[#94a3b8] mb-1.5">{t('newPasswordLabel')}</span>
          <div className="relative flex items-center h-10 bg-[#1a1a2e]/80 border border-[rgba(255,107,0,0.14)] focus-within:border-[#FF6B00] transition">
            <div className="pl-3 text-[#64748b]">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
                <rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
            </div>
            <input
              type={showPassword ? 'text' : 'password'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              autoFocus
              className="flex-1 bg-transparent outline-none px-3 text-sm text-[#e2e8f0] placeholder:text-[#64748b]"
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="pr-3 text-[#64748b] hover:text-[#e2e8f0]"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
                {showPassword
                  ? <><path d="M17.94 17.94A10.07 10.07 0 0 1 12 19c-7 0-10-7-10-7a18.37 18.37 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 3c7 0 10 7 10 7a18.5 18.5 0 0 1-2.16 3.19M14.12 14.12a3 3 0 1 1-4.24-4.24M1 1l22 22"/></>
                  : <><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="3"/></>
                }
              </svg>
            </button>
          </div>
        </label>

        <label className="block">
          <span className="block f-mono uppercase text-[10px] tracking-[0.14em] text-[#94a3b8] mb-1.5">{t('confirmLabel')}</span>
          <div className="relative flex items-center h-10 bg-[#1a1a2e]/80 border border-[rgba(255,107,0,0.14)] focus-within:border-[#FF6B00] transition">
            <div className="pl-3 text-[#64748b]">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
                <rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
            </div>
            <input
              type={showPassword ? 'text' : 'password'}
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              placeholder="••••••••"
              className="flex-1 bg-transparent outline-none px-3 text-sm text-[#e2e8f0] placeholder:text-[#64748b]"
            />
          </div>
        </label>

        {errorMsg && (
          <div className="text-[12px] text-[#ff6666]">{errorMsg}</div>
        )}

        <button
          type="submit"
          disabled={submitting || !password || !confirm}
          className="w-full h-10 f-mono text-xs uppercase btn-clip bg-[#FF6B00] hover:bg-[#FF9A3C] text-black font-semibold transition-all disabled:opacity-40 disabled:cursor-not-allowed"
        >
          {submitting ? t('updating') : t('update')}
        </button>
      </form>
    </>
  );
}

export default function ResetPasswordPage() {
  return (
    <div className="relative w-full min-h-dvh bg-[#0d0d1a] overflow-hidden flex items-center justify-center p-4">
      <div className="fixed inset-0 amg-grid-sm pointer-events-none"></div>
      <div className="w-full max-w-[420px] amg-card card-clip p-8 z-10">
        <Suspense fallback={<div className="text-ink-2 text-sm">Loading...</div>}>
          <ResetForm />
        </Suspense>
      </div>
    </div>
  );
}
