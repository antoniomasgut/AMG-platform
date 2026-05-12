'use client';

import { useState, FormEvent } from 'react';
import { Link } from '@/i18n/navigation';
import { useTranslations } from 'next-intl';

export default function ForgotPasswordPage() {
  const t = useTranslations('auth.forgotPassword');
  const [email, setEmail] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [sent, setSent] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!email.trim()) return;
    setSubmitting(true);
    setErrorMsg('');
    try {
      setSent(true);
    } catch (_err) {
      setErrorMsg(t('error'));
    } finally {
      setSubmitting(false);
    }
  };

  if (sent) {
    return (
      <div className="relative w-full min-h-dvh bg-[#0d0d1a] overflow-hidden flex items-center justify-center p-4">
        <div className="fixed inset-0 amg-grid-sm pointer-events-none"></div>
        <div className="w-full max-w-[420px] amg-card card-clip p-8 text-center z-10">
          <div className="w-16 h-16 bg-[rgba(255,107,0,0.12)] border border-[#FF6B00] flex items-center justify-center mx-auto mb-5">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#FF9A3C" strokeWidth="1.6">
              <rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-10 6L2 7"/>
            </svg>
          </div>
          <h1 className="f-display font-black text-2xl text-white">{t('sentTitle')}</h1>
          <p className="text-[13px] text-[#94a3b8] mt-2">{t('sentDesc')}</p>
          <div className="f-mono text-sm text-[#FF9A3C] mt-1">{email}</div>
          <div className="mt-6 p-3 border-l-2 border-l-[#58a6ff] bg-[rgba(88,166,255,0.05)] text-[12px] text-[#94a3b8] text-left flex gap-2">
            <span className="shrink-0 mt-0.5 text-[#58a6ff]">&#9432;</span>
            {t('sentExpiry', { minutes: 15 })}
          </div>
          <Link href="/login" className="mt-6 inline-block f-mono text-xs uppercase text-[#FF9A3C] hover:text-[#FF6B00] transition">
            ← {t('backToLogin')}
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="relative w-full min-h-dvh bg-[#0d0d1a] overflow-hidden flex items-center justify-center p-4">
      <div className="fixed inset-0 amg-grid-sm pointer-events-none"></div>
      <div className="w-full max-w-[420px] amg-card card-clip p-8 z-10">
        <div className="flex items-center gap-2 mb-1">
          <div className="w-1.5 h-1.5 bg-[#FF6B00]"></div>
          <span className="f-mono text-[10px] uppercase tracking-[0.2em] text-[#FF9A3C]">{t('title')}</span>
        </div>
        <h1 className="f-display font-black text-2xl text-white">{t('title')}</h1>
        <p className="text-[13px] text-[#94a3b8] mb-6">{t('subtitle')}</p>

        <form onSubmit={handleSubmit} className="space-y-3">
          <label className="block">
            <span className="block f-mono uppercase text-[10px] tracking-[0.14em] text-[#94a3b8] mb-1.5">{t('emailLabel')}</span>
            <div className="relative flex items-center h-10 bg-[#1a1a2e]/80 border border-[rgba(255,107,0,0.14)] focus-within:border-[#FF6B00] transition">
              <div className="pl-3 text-[#64748b]">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
                  <rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-10 6L2 7"/>
                </svg>
              </div>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="tu@empresa.com"
                autoFocus
                className="flex-1 bg-transparent outline-none px-3 text-sm text-[#e2e8f0] placeholder:text-[#64748b]"
              />
            </div>
          </label>

          {errorMsg && (
            <div className="text-[12px] text-[#ff6666]">{errorMsg}</div>
          )}

          <button
            type="submit"
            disabled={submitting || !email.trim()}
            className="w-full h-10 f-mono text-xs uppercase btn-clip bg-[#FF6B00] hover:bg-[#FF9A3C] text-black font-semibold transition-all disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {submitting ? t('sending') : t('sendLink')}
          </button>
        </form>

        <Link href="/login" className="mt-4 inline-block f-mono text-[11px] uppercase text-[#64748b] hover:text-[#FF9A3C] transition">
          ← {t('backToLogin')}
        </Link>
      </div>
    </div>
  );
}
