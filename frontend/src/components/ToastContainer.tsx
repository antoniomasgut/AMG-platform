'use client';

import { useToast } from '@/lib/toast-context';
import { I } from '@/components/ui/icons';

const COLORS = {
  success: { border: '#39d353', bg: 'rgba(57,211,83,0.1)', icon: '#39d353' },
  error: { border: '#ff4444', bg: 'rgba(255,68,68,0.1)', icon: '#ff6666' },
  warning: { border: '#f0b429', bg: 'rgba(240,180,41,0.1)', icon: '#f0b429' },
  info: { border: '#58a6ff', bg: 'rgba(88,166,255,0.1)', icon: '#58a6ff' },
};

const ICONS = {
  success: I.Check,
  error: I.AlertCircle,
  warning: I.AlertCircle,
  info: I.Bell,
};

export function ToastContainer() {
  const { toasts, dismiss } = useToast();

  return (
    <div className="fixed bottom-6 right-6 z-[999] flex flex-col gap-2 max-w-[380px] w-full pointer-events-none">
      {toasts.map(t => {
        const colors = COLORS[t.type];
        const Icon = ICONS[t.type];
        return (
          <div
            key={t.id}
            className="pointer-events-auto animate-slide-up amg-card card-clip p-4 border-l-[3px] shadow-2xl"
            style={{ borderLeftColor: colors.border, background: '#13132a' }}
          >
            <div className="flex items-start gap-3">
              <div className="w-8 h-8 rounded flex items-center justify-center shrink-0" style={{ background: colors.bg }}>
                <Icon size={14} stroke={colors.icon} />
              </div>
              <div className="flex-1 min-w-0">
                <div className="f-display font-bold text-sm">{t.title}</div>
                {t.message && <div className="text-data text-ink-1 mt-0.5">{t.message}</div>}
              </div>
              <button onClick={() => dismiss(t.id)} className="text-ink-3 hover:text-ink-0 transition shrink-0">
                <I.X size={12} />
              </button>
            </div>
          </div>
        );
      })}
    </div>
  );
}
