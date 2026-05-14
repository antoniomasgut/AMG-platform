'use client';

import { useState, FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/lib/auth-context';
import { forgotPassword } from '@/services/auth';

/* ─────────── SVG Icons (inline, no dependency) ─────────── */
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

/* ─────────── Types ─────────── */
type PageState = 'form' | 'password' | 'sent';

/* ─────────── Login Page ─────────── */
export default function LoginPage() {
  const router = useRouter();
  const { login, isAuthenticated } = useAuth();
  const [state, setState] = useState<PageState>('form');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successEmail, setSuccessEmail] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Redirect if already logged in
  if (isAuthenticated) {
    router.replace('/portal');
  }

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
      const msg = err instanceof Error ? err.message : 'Error en enviar l\'enllaç';
      setErrorMsg(msg);
      setState('form');
    } finally {
      setSubmitting(false);
    }
  };

  const handlePasswordLogin = async (e: FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password) return;
    setSubmitting(true);
    setErrorMsg('');
    try {
      await login({ email, password });
      router.push('/portal');
    } catch (err: unknown) {
      const apiErr = err as { status?: number; message?: string };
      if (apiErr.status === 401) {
        setErrorMsg('Credencials incorrectes');
      } else if (apiErr.status === 423) {
        setErrorMsg('Compte blocat. Intenta-ho més tard o recupera la contrasenya.');
      } else if (apiErr.status === 429) {
        setErrorMsg('Massa intents. Torna-ho a provar en uns minuts.');
      } else {
        setErrorMsg(apiErr.message || 'Error d\'inici de sessió');
      }
      setState('password');
    } finally {
      setSubmitting(false);
    }
  };

  const isBusy = submitting;
  return (
    <div className="relative w-full min-h-dvh bg-[#0d0d1a] overflow-hidden flex">
      {/* Animated grid bg */}
      <div className="fixed inset-0 amg-grid-sm pointer-events-none"></div>
      <div className="fixed inset-0 pointer-events-none" style={{
        background: 'radial-gradient(ellipse at 30% 50%, rgba(255,107,0,0.18), transparent 45%), radial-gradient(ellipse at 80% 80%, rgba(255,154,60,0.10), transparent 40%)',
      }}></div>

      {/* Left: brand panel (hidden on mobile) */}
      <div className="relative hidden lg:flex w-[48%] p-16 flex-col justify-between z-10 border-r border-border-base">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 bg-[#FF6B00] btn-clip flex items-center justify-center">
            <span className="f-display font-black text-black text-xl">A</span>
          </div>
          <div>
            <div className="f-display font-black text-lg tracking-wider">AMG</div>
            <div className="f-mono text-label text-accent-light tracking-widest">ENGINYERIA DIGITAL</div>
          </div>
        </div>

        <div>
          <div className="f-mono text-caption uppercase tracking-widest text-accent-light mb-4">PORTAL v2.14.0</div>
          <h1 className="f-display font-black text-5xl leading-[1.05]">
            PYMES <span className="text-accent-light">AUTOMATITZADES</span><br />
            EN MENYS DE 48 HORES.
          </h1>
          <p className="text-lg text-ink-1 mt-4 max-w-md">
            Bots, landings, workflows i facturació — un sol portal administrat.
          </p>
        </div>

        <div className="flex items-center gap-4 f-mono text-label uppercase text-ink-3">
          <span className="flex items-center gap-2">
            <span className="w-1.5 h-1.5 rounded-full bg-[#39d353] amg-blink"></span>
            SISTEMA OPERATIU
          </span>
          <span>· 99.98% uptime</span>
          <span>· 48 pimes actives</span>
        </div>
      </div>

      {/* Right: login card */}
      <div className="relative flex-1 flex items-center justify-center p-6 sm:p-10 z-10">
        <div className="w-full max-w-[420px]">
          <div className="amg-card card-clip p-6 sm:p-8">
            {/* Header */}
            <div className="flex items-center gap-2 mb-1">
              <div className="w-1.5 h-1.5 bg-[#FF6B00]"></div>
              <span className="f-mono text-label uppercase tracking-widest text-accent-light">
                {state === 'sent' ? 'Enllaç enviat' : 'Iniciar sessió'}
              </span>
            </div>

            {state === 'sent' ? (
              /* ─── SENT STATE ─── */
              <>
                <div className="w-16 h-16 bg-accent-muted border border-[#FF6B00] flex items-center justify-center mb-5 mx-auto">
                  <I.Mail size={24} stroke="#FF9A3C" />
                </div>
                <h2 className="f-display font-black text-2xl text-center">COMPROVA EL TEU EMAIL</h2>
                <p className="text-ui text-ink-1 mt-2 text-center">Hem enviat un enllaç d&#39;accés a</p>
                <div className="f-mono text-sm text-accent-light text-center mt-1">{successEmail}</div>

                <div className="mt-6 p-3 border-l-2 border-l-[#58a6ff] bg-[rgba(88,166,255,0.05)] flex gap-3">
                  <I.Clock size={14} stroke="#58a6ff" className="shrink-0 mt-0.5" />
                  <div className="text-data text-ink-1">
                    L&#39;enllaç caduca en <span className="text-info f-mono">15 min</span>. Revisa la carpeta de spam si no el veus.
                  </div>
                </div>

                <button className="mt-6 w-full h-10 f-mono text-xs uppercase btn-clip bg-[#1a1a2e] hover:bg-[#212140] text-ink-0 border border-border-strong transition-all">
                  REENVIAR ENLLAÇ
                </button>
                <button onClick={() => setState('form')} className="w-full mt-3 f-mono text-caption uppercase text-ink-3 hover:text-accent-light transition">
                  ← USAR UN ALTRE EMAIL
                </button>
              </>
            ) : state === 'password' ? (
              /* ─── PASSWORD STATE ─── */
              <>
                <h2 className="f-display font-black text-2xl mb-2">CONTRASENYA</h2>
                <p className="text-ui text-ink-1 mb-6">Introdueix la teva contrasenya per a {email}</p>

                <form onSubmit={handlePasswordLogin} className="space-y-4">
                  <label className="block">
                    <span className="block f-mono uppercase text-label tracking-label text-ink-1 mb-1.5">Contrasenya</span>
                    <div className="relative flex items-center h-10 bg-[#1a1a2e]/80 border border-border-base focus-within:border-[#FF6B00] transition">
                      <div className="pl-3 text-ink-3"><I.Lock size={14} /></div>
                      <input
                        type={showPassword ? 'text' : 'password'}
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="••••••••"
                        autoFocus
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
                    <div className="flex items-start gap-2 p-3 border-l-2 border-l-[#ff4444] bg-[rgba(255,68,68,0.05)]">
                      <I.AlertCircle size={14} stroke="#ff6666" className="shrink-0 mt-0.5" />
                      <span className="text-data text-danger-light">{errorMsg}</span>
                    </div>
                  )}

                  <button
                    type="submit"
                    disabled={isBusy || !password}
                    className="w-full h-10 f-mono text-xs uppercase btn-clip bg-[#FF6B00] hover:bg-[#FF9A3C] text-black font-semibold transition-all disabled:opacity-40 disabled:cursor-not-allowed inline-flex items-center justify-center gap-2"
                  >
                    {isBusy ? (
                      <span className="w-3 h-3 border-2 border-black/60 border-t-transparent rounded-full animate-spin"></span>
                    ) : (
                      <I.ArrowRight size={14} />
                    )}
                    {isBusy ? 'ENTRANT...' : 'ENTRAR'}
                  </button>

                  <div className="flex items-center justify-between">
                    <button type="button" onClick={() => setState('form')} className="f-mono text-caption uppercase text-ink-3 hover:text-accent-light transition">
                      ← TORNAR
                    </button>
                    <Link href="/forgot-password" className="f-mono text-caption uppercase text-accent-light hover:text-accent transition">
                      HAS OBLIDAT LA CONTRASENYA?
                    </Link>
                  </div>
                </form>
              </>
            ) : (
              /* ─── FORM STATE (magic link) ─── */
              <>
                <h2 className="f-display font-black text-2xl mb-2">ACCÉS AL PORTAL</h2>
                <p className="text-ui text-ink-1 mb-6">T&#39;enviem un enllaç màgic per email. Sense contrasenyes.</p>

                <form onSubmit={handleMagicLink} className="space-y-3">
                  <label className="block">
                    <span className="block f-mono uppercase text-label tracking-label text-ink-1 mb-1.5">Email corporatiu</span>
                    <div className="relative flex items-center h-10 bg-[#1a1a2e]/80 border border-border-base focus-within:border-[#FF6B00] transition">
                      <div className="pl-3 text-ink-3"><I.Mail size={14} /></div>
                      <input
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        placeholder="tu@empresa.com"
                        autoFocus
                        autoComplete="email"
                        className="flex-1 bg-transparent outline-none px-3 text-sm text-ink-0 placeholder:text-ink-3"
                      />
                    </div>
                  </label>

                  <button
                    type="submit"
                    disabled={isBusy || !email.trim()}
                    className="w-full h-10 f-mono text-xs uppercase btn-clip bg-[#FF6B00] hover:bg-[#FF9A3C] text-black font-semibold transition-all disabled:opacity-40 disabled:cursor-not-allowed inline-flex items-center justify-center gap-2"
                  >
                    {isBusy ? (
                      <span className="w-3 h-3 border-2 border-black/60 border-t-transparent rounded-full animate-spin"></span>
                    ) : (
                      <I.ArrowRight size={14} />
                    )}
                    {isBusy ? 'ENVIANT...' : 'ENVIAR MAGIC LINK'}
                  </button>
                </form>

                <div className="flex items-center gap-3 my-5">
                  <div className="flex-1 h-[1px] bg-[rgba(226,232,240,0.08)]"></div>
                  <span className="f-mono text-label uppercase text-ink-3 tracking-wider">o amb contrasenya</span>
                  <div className="flex-1 h-[1px] bg-[rgba(226,232,240,0.08)]"></div>
                </div>

                <button
                  onClick={() => { if (email.trim()) setState('password'); else setErrorMsg('Introdueix primer el teu email'); }}
                  className="w-full h-10 bg-[#1a1a2e] border border-border-base flex items-center justify-between px-3 text-sm hover:border-[#FF6B00] transition"
                >
                  <span className="flex items-center gap-2"><I.Lock size={14} stroke="#94a3b8" />Usar contrasenya</span>
                  <I.ChevDown size={12} className="text-ink-3" />
                </button>

                {errorMsg && (
                  <div className="flex items-start gap-2 p-3 border-l-2 border-l-[#f0b429] bg-[rgba(240,180,41,0.05)] mt-3">
                    <span className="text-data text-warning">{errorMsg}</span>
                  </div>
                )}
              </>
            )}
          </div>

          {/* Footer */}
          <div className="mt-6 text-center f-mono text-label uppercase text-ink-3 tracking-wider">
            <Link href="/legal/avis-legal" className="hover:text-accent-light transition">TERMES</Link>
            <span className="mx-2">·</span>
            <Link href="/legal/privacitat" className="hover:text-accent-light transition">PRIVACITAT</Link>
            <span className="mx-2">·</span>
            <a href="mailto:hola@amg.digital" className="hover:text-accent-light transition">SUPORT</a>
            <span className="mx-2">·</span>
            <a href="tel:+34971000000" className="hover:text-accent-light transition">971 000 000</a>
          </div>
        </div>
      </div>
    </div>
  );
}
