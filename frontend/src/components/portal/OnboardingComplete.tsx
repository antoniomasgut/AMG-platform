'use client';

interface OnboardingCompleteProps {
  userName: string;
  onGoToDashboard: () => void;
}

export function OnboardingComplete({ userName, onGoToDashboard }: OnboardingCompleteProps) {
  return (
    <div className="amg-card card-clip p-8 text-center animate-slide-up" role="region" aria-label="Guia d'inici completada">
      <div className="w-16 h-16 bg-emerald-900/30 border border-emerald-500/40 flex items-center justify-center mx-auto mb-4">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#4ade80" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
          <polyline points="20 6 9 17 4 12" />
        </svg>
      </div>
      <div className="f-display font-bold text-2xl mb-2">ESTÀS A PUNT, {userName.toUpperCase()}!</div>
      <p className="text-ink-2 text-sm mb-6">Ja tens tot configurat. El teu portal està llest.</p>
      <button
        onClick={onGoToDashboard}
        className="f-mono text-label uppercase btn-clip bg-accent hover:bg-accent-light text-black font-semibold px-6 h-10 transition-colors"
      >
        ANAR AL DASHBOARD →
      </button>
    </div>
  );
}
