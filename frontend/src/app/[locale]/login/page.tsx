'use client';

import { useState, FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useTranslations, useLocale } from 'next-intl';
import { useAuth } from '@/lib/auth-context';
import { forgotPassword } from '@/services/auth';
import { AMGLogo } from '@/components/ui/AMGLogo';

type IconProps = { size?: number; stroke?: string; className?: string };

const Icon = ({ d, size = 16, stroke = 'currentColor', children }: { d?: string; size?: number; stroke?: string; children?: React.ReactNode }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={stroke} strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    {children || (d && <path d={d} />)}
  </svg>
);

const I = {
  Mail: (p: IconProps) => <Icon {...p}><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-10 6L2 7"/></Icon>,
  Lock: (p: IconProps) => <Icon {...p}><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></Icon>,
  ArrowRight: (p: IconProps) => <Icon {...p}><path d="M5 12h14M13 5l7 7-7 7"/></Icon>,
  ChevDown: (p: IconProps) => <Icon {...p}><path d="m6 9 6 6 6-6"/></Icon>,
  Clock: (p: IconProps) => <Icon {...p}><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></Icon>,
  Eye: (p: IconProps) => <Icon {...p}><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="3"/></Icon>,
  EyeOff: (p: IconProps) => <Icon {...p}><path d="M17.94 17.94A10.07 10.07 0 0 1 12 19c-7 0-10-7-10-7a18.37 18.37 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 3c7 0 10 7 10 7a18.5 18.5 0 0 1-2.16 3.19M14.12 14.12a3 3 0 1 1-4.24-4.24M1 1l22 22"/></Icon>,
  AlertCircle: (p: IconProps) => <Icon {...p}><circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/></Icon>,
};

type PageState = 'login' | 'magic' | 'sent';

export default function LoginPage() {
  const router = useRouter();
  const locale = useLocale();
  const t = useTranslations('auth.login');
  const { login, isAuthenticated } = useAuth();
  const [state, setState] = useState<PageState>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successEmail, setSuccessEmail] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (isAuthenticated) {
    router.replace(`/${locale}/portal`);
  }

  const handleLogin = async (e: FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password) return;
    setSubmitting(true);
    setErrorMsg('');
    try {
      await login({ email, password });
      router.push(`/${locale}/portal`);
    } catch (err: unknown) {
      const apiErr = err as { status?: number; message?: string };
      if (apiErr.status === 401) {
        setErrorMsg(t('errors.incorrectCredentials'));
      } else if (apiErr.status === 423) {
        setErrorMsg(t('errors.accountLocked'));
      } else if (apiErr.status === 429) {
        setErrorMsg(t('errors.tooManyAttempts'));
      } else {
        setErrorMsg(apiErr.message || t('errors.loginError'));
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleMagicLink = async (e: FormEvent) => {
    e.preventDefault();
    if (!email.trim()) return;
    setSubmitting(true);
    setErrorMsg('');
    try {
      await forgotPassword({ email });
      setSuccessEmail(email);
      setState('sent');
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : t('errors.sendError'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="relative w-full min-h-dvh bg-bg-0 overflow-hidden flex">
      <div className="fixed inset-0 amg-grid-sm pointer-events-none" />
      <div className="fixed inset-0 pointer-events-none" style={{
        background: 'radial-gradient(ellipse at 30% 50%, rgba(255,107,0,0.18), transparent 45%), radial-gradient(ellipse at 80% 80%, rgba(255,154,60,0.10), transparent 40%)',
      }} />

      {/* Left brand panel */}
      <div className="relative hidden lg:flex w-[48%] p-16 flex-col justify-between z-10 border-r border-border-base">
        <Link href={`/${locale}`}>
          <AMGLogo className="h-10 w-auto" />
        </Link>

        <div>
          <h1 className="f-display font-black text-5xl leading-[1.05]">
            UN SOL PARTNER<br />
            PER A <span className="text-accent-light">TOT EL TEU</span><br />
            NEGOCI DIGITAL.
          </h1>
          <p className="text-lg text-ink-1 mt-4 max-w-md">
            Webs, automatitzacions, manteniment i suport — tot gestionat i visible al teu portal.
          </p>
        </div>

        <div className="flex items-center gap-4 f-mono text-label uppercase text-ink-3">
          <span className="flex items-center gap-2">
            <span className="w-1.5 h-1.5 rounded-full bg-[#39d353] amg-blink" />
            SISTEMA OPERATIU
          </span>
          <span>· &lt; 4h resposta</span>
          <span>· 8+ pimes actives</span>
        </div>
      </div>

      {/* Right: login card */}
      <div className="relative flex-1 flex items-center justify-center p-6 sm:p-10 z-10">
        <div className="w-full max-w-[420px]">
          <div className="amg-card card-clip p-6 sm:p-8">
            <div className="flex items-center gap-2 mb-1">
              <div className="w-1.5 h-1.5 bg-accent" />
              <span className="f-mono text-label uppercase tracking-widest text-accent-light">
                {state === 'sent' ? t('linkSent') : t('title')}
              </span>
            </div>

            {state === 'sent' ? (
              <>
                <div className="w-16 h-16 bg-accent-muted border border-accent flex items-center justify-center mb-5 mx-auto">
                  <I.Mail size={24} stroke="#FF9A3C" />
                </div>
                <h2 className="f-display font-black text-2xl text-center">{t('checkEmail')}</h2>
                <p className="text-ui text-ink-1 mt-2 text-center">{t('linkSentTo')}</p>
                <div className="f-mono text-sm text-accent-light text-center mt-1">{successEmail}</div>

                <div className="mt-6 p-3 border-l-2 border-l-[#58a6ff] bg-[rgba(88,166,255,0.05)] flex gap-3">
                  <I.Clock size={14} stroke="#58a6ff" className="shrink-0 mt-0.5" />
                  <div className="text-data text-ink-1">
                    {t('linkExpires')} <span className="text-info f-mono">{t('minutes')}</span>. {t('checkSpam')}
                  </div>
                </div>

                <button onClick={() => setState('login')} className="mt-6 w-full h-10 f-mono text-xs uppercase btn-clip bg-bg-2 hover:bg-bg-3 text-ink-0 border border-border-strong transition-all">
                  {t('useOtherEmail')}
                </button>
              </>
            ) : state === 'magic' ? (
              <>
                <h2 className="f-display font-black text-2xl mb-2">{t('title')}</h2>
                <p className="text-ui text-ink-1 mb-6">{t('subtitle')}</p>

                <form onSubmit={handleMagicLink} className="space-y-3">
                  <label className="block">
                    <span className="block f-mono uppercase text-label tracking-label text-ink-1 mb-1.5">{t('emailLabel')}</span>
                    <div className="relative flex items-center h-10 bg-bg-2/80 border border-border-base focus-within:border-accent transition">
                      <div className="pl-3 text-ink-3"><I.Mail size={14} /></div>
                      <input
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        placeholder={t('emailPlaceholder')}
                        autoFocus
                        autoComplete="email"
                        className="flex-1 bg-transparent outline-none px-3 text-sm text-ink-0 placeholder:text-ink-3"
                      />
                    </div>
                  </label>

                  {errorMsg && (
                    <div className="flex items-start gap-2 p-3 border-l-2 border-l-danger bg-[rgba(255,68,68,0.05)]">
                      <span className="text-data text-danger-light">{errorMsg}</span>
                    </div>
                  )}

                  <button
                    type="submit"
                    disabled={submitting || !email.trim()}
                    className="w-full h-10 f-mono text-xs uppercase btn-clip bg-accent hover:bg-accent-light text-black font-semibold transition-all disabled:opacity-40 disabled:cursor-not-allowed inline-flex items-center justify-center gap-2"
                  >
                    {submitting ? <span className="w-3 h-3 border-2 border-black/60 border-t-transparent rounded-full animate-spin" /> : <I.ArrowRight size={14} />}
                    {submitting ? t('sending') : t('sendMagicLink')}
                  </button>
                </form>

                <button type="button" onClick={() => setState('login')} className="mt-4 w-full f-mono text-caption uppercase text-ink-3 hover:text-accent-light transition">
                  {t('back')}
                </button>
              </>
            ) : (
              <>
                <h2 className="f-display font-black text-2xl mb-2">{t('passwordTitle')}</h2>
                <p className="text-ui text-ink-1 mb-6">{t('subtitle')}</p>

                <form onSubmit={handleLogin} className="space-y-4">
                  <label className="block">
                    <span className="block f-mono uppercase text-label tracking-label text-ink-1 mb-1.5">{t('emailLabel')}</span>
                    <div className="relative flex items-center h-10 bg-bg-2/80 border border-border-base focus-within:border-accent transition">
                      <div className="pl-3 text-ink-3"><I.Mail size={14} /></div>
                      <input
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        placeholder={t('emailPlaceholder')}
                        autoFocus
                        autoComplete="email"
                        className="flex-1 bg-transparent outline-none px-3 text-sm text-ink-0 placeholder:text-ink-3"
                      />
                    </div>
                  </label>

                  <label className="block">
                    <span className="block f-mono uppercase text-label tracking-label text-ink-1 mb-1.5">{t('passwordLabel')}</span>
                    <div className="relative flex items-center h-10 bg-bg-2/80 border border-border-base focus-within:border-accent transition">
                      <div className="pl-3 text-ink-3"><I.Lock size={14} /></div>
                      <input
                        type={showPassword ? 'text' : 'password'}
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="••••••••"
                        autoComplete="current-password"
                        className="flex-1 bg-transparent outline-none px-3 text-sm text-ink-0 placeholder:text-ink-3"
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        aria-label={showPassword ? 'Ocultar contrasenya' : 'Mostrar contrasenya'}
                        className="pr-3 text-ink-2 hover:text-ink-0 transition"
                      >
                        {showPassword ? <I.EyeOff size={14} /> : <I.Eye size={14} />}
                      </button>
                    </div>
                  </label>

                  {errorMsg && (
                    <div className="flex items-start gap-2 p-3 border-l-2 border-l-danger bg-[rgba(255,68,68,0.05)]">
                      <I.AlertCircle size={14} stroke="#ff6666" className="shrink-0 mt-0.5" />
                      <span className="text-data text-danger-light">{errorMsg}</span>
                    </div>
                  )}

                  <button
                    type="submit"
                    disabled={submitting || !email.trim() || !password}
                    className="w-full h-10 f-mono text-xs uppercase btn-clip bg-accent hover:bg-accent-light text-black font-semibold transition-all disabled:opacity-40 disabled:cursor-not-allowed inline-flex items-center justify-center gap-2"
                  >
                    {submitting ? <span className="w-3 h-3 border-2 border-black/60 border-t-transparent rounded-full animate-spin" /> : <I.ArrowRight size={14} />}
                    {submitting ? t('entering') : t('enter')}
                  </button>

                  <div className="flex items-center justify-between pt-1">
                    <button
                      type="button"
                      onClick={() => setState('magic')}
                      className="f-mono text-caption uppercase text-ink-3 hover:text-accent-light transition"
                    >
                      {t('sendMagicLink')}
                    </button>
                    <Link href={`/${locale}/forgot-password`} className="f-mono text-caption uppercase text-accent-light hover:text-accent transition">
                      {t('forgotPassword')}
                    </Link>
                  </div>
                </form>
              </>
            )}
          </div>

          <div className="mt-6 text-center f-mono text-label uppercase text-ink-3 tracking-wider">
            <Link href={`/${locale}/legal/avis-legal`} className="hover:text-accent-light transition">TERMES</Link>
            <span className="mx-2">·</span>
            <Link href={`/${locale}/legal/politica-privacitat`} className="hover:text-accent-light transition">PRIVACITAT</Link>
            <span className="mx-2">·</span>
            <a href="mailto:hola@amg.digital" className="hover:text-accent-light transition">SUPORT</a>
          </div>
        </div>
      </div>
    </div>
  );
}
