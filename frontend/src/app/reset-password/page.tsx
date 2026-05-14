'use client';

import { Suspense, useState } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { AMGButton } from '@/components/ui/button';
import { I } from '@/components/ui/icons';
import { resetPassword } from '@/services/auth';

function ResetPasswordForm() {
  const searchParams = useSearchParams();
  const router = useRouter();
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

    if (!token) { setError('Enllaç de recuperació invàlid o expirat'); return; }
    if (password.length < 4) { setError('La contrasenya ha de tenir almenys 4 caràcters'); return; }
    if (password !== confirm) { setError('Les contrasenyes no coincideixen'); return; }

    setLoading(true);
    try {
      await resetPassword({ token, password });
      setSuccess(true);
      setTimeout(() => router.push('/login'), 3000);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Error en restablir la contrasenya');
    } finally {
      setLoading(false);
    }
  };

  if (!token) {
    return (
      <div className="w-full min-h-dvh bg-[#0d0d1a] flex items-center justify-center p-6">
        <div className="amg-card card-clip p-8 max-w-[420px] w-full text-center">
          <div className="w-16 h-16 bg-[rgba(255,68,68,0.12)] border border-[rgba(255,68,68,0.35)] flex items-center justify-center mx-auto mb-5">
            <I.AlertCircle size={24} stroke="#ff6666" />
          </div>
          <h2 className="f-display font-black text-2xl">ENLLAÇ INVALID</h2>
          <p className="text-ui text-ink-1 mt-2">Aquest enllaç de recuperació és invàlid o ha expirat.</p>
          <AMGButton onClick={() => router.push('/login')} className="mt-6">TORNAR AL LOGIN</AMGButton>
        </div>
      </div>
    );
  }

  if (success) {
    return (
      <div className="w-full min-h-dvh bg-[#0d0d1a] flex items-center justify-center p-6">
        <div className="amg-card card-clip p-8 max-w-[420px] w-full text-center">
          <div className="w-16 h-16 bg-[rgba(57,211,83,0.12)] border border-[#39d353] flex items-center justify-center mx-auto mb-5">
            <I.Check size={24} stroke="#39d353" />
          </div>
          <h2 className="f-display font-black text-2xl">CONTRASENYA ACTUALITZADA</h2>
          <p className="text-ui text-ink-1 mt-2">Redirigint al login...</p>
          <AMGButton onClick={() => router.push('/login')} className="mt-6">ANAR AL LOGIN</AMGButton>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full min-h-dvh bg-[#0d0d1a] overflow-hidden flex items-center justify-center p-6 sm:p-10 relative">
      <div className="fixed inset-0 amg-grid-sm pointer-events-none"></div>
      <div className="fixed inset-0 pointer-events-none" style={{
        background: 'radial-gradient(ellipse at 30% 50%, rgba(255,107,0,0.18), transparent 45%)',
      }}></div>

      <div className="relative z-10 w-full max-w-[420px]">
        <div className="amg-card card-clip p-6 sm:p-8">
          <div className="flex items-center gap-2 mb-1">
            <div className="w-1.5 h-1.5 bg-[#FF6B00]"></div>
            <span className="f-mono text-label uppercase tracking-widest text-accent-light">Nova contrasenya</span>
          </div>

          <h2 className="f-display font-black text-2xl mb-2">RESTABLIR CONTRASENYA</h2>
          <p className="text-ui text-ink-1 mb-6">Introdueix la teva nova contrasenya.</p>

          <form onSubmit={handleSubmit} className="space-y-4">
            <label className="block">
              <span className="block f-mono uppercase text-label tracking-label text-ink-1 mb-1.5">Nova contrasenya</span>
              <div className="relative flex items-center h-10 bg-[#1a1a2e]/80 border border-border-base focus-within:border-[#FF6B00] transition">
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
              <span className="block f-mono uppercase text-label tracking-label text-ink-1 mb-1.5">Confirmar contrasenya</span>
              <div className="relative flex items-center h-10 bg-[#1a1a2e]/80 border border-border-base focus-within:border-[#FF6B00] transition">
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
              <div className="flex items-start gap-2 p-3 border-l-2 border-l-[#ff4444] bg-[rgba(255,68,68,0.05)]">
                <span className="text-data text-danger-light">{error}</span>
              </div>
            )}

            <button
              type="submit"
              disabled={loading || !password || !confirm}
              className="w-full h-10 f-mono text-xs uppercase btn-clip bg-[#FF6B00] hover:bg-[#FF9A3C] text-black font-semibold transition-all disabled:opacity-40 disabled:cursor-not-allowed inline-flex items-center justify-center gap-2"
            >
              {loading ? (
                <span className="w-3 h-3 border-2 border-black/60 border-t-transparent rounded-full animate-spin"></span>
              ) : (
                <I.ArrowRight size={14} />
              )}
              {loading ? 'ACTUALITZANT...' : 'ACTUALITZAR CONTRASENYA'}
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
      <div className="w-full min-h-dvh bg-[#0d0d1a] flex items-center justify-center">
        <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin"></span>
      </div>
    }>
      <ResetPasswordForm />
    </Suspense>
  );
}
