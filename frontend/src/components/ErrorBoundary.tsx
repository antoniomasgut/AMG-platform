'use client';

import { Component, type ReactNode, type ErrorInfo } from 'react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[ErrorBoundary]', error, info.componentStack);
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) return this.props.fallback;
      return (
        <div className="w-full min-h-dvh bg-[#0d0d1a] flex items-center justify-center p-8">
          <div className="amg-card card-clip p-8 max-w-lg text-center">
            <div className="w-16 h-16 bg-[rgba(255,68,68,0.1)] border border-[#ff4444] flex items-center justify-center mx-auto mb-4">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#ff6666" strokeWidth="1.6">
                <circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/>
              </svg>
            </div>
            <h2 className="f-display font-black text-xl">ERROR INESPERAT</h2>
            <p className="text-ui text-ink-1 mt-2">Ha ocorregut un error. Torna a carregar la pàgina.</p>
            <p className="f-mono text-caption text-ink-3 mt-3 truncate">{this.state.error?.message}</p>
            <button
              onClick={() => window.location.reload()}
              className="mt-6 h-10 px-6 f-mono text-xs uppercase btn-clip bg-[#FF6B00] hover:bg-[#FF9A3C] text-black font-semibold transition"
            >
              RECARREGAR
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
