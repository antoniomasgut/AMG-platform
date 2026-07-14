'use client';

import { type FC } from 'react';
import { useEditorStore } from '@/store/editor';

interface Props {
  landingId: string;
}

// El prompt del bot ja no s'edita aquí: és el mateix agent IA per a tots els canals
// (correu, WhatsApp, xat web), configurat a /portal/agents. Aquí només es controla
// si el widget flotant es mostra a la landing.
export const ChatContextPanel: FC<Props> = ({ landingId: _landingId }) => {
  const styles = useEditorStore((s) => s.styles);
  const setStyles = useEditorStore((s) => s.setStyles);
  const chatEnabled = styles.chatEnabled ?? false;

  return (
    <div className="flex flex-col gap-3 p-3">
      {/* Header */}
      <div className="flex items-center gap-2">
        <span className="text-[#FF6B00]">💬</span>
        <span className="f-mono text-[9px] uppercase tracking-wider text-ink-1">Bot de xat</span>
      </div>

      {/* Toggle widget visible */}
      <label className="flex items-center gap-3 cursor-pointer group">
        <div
          onClick={() => setStyles({ chatEnabled: !chatEnabled })}
          className={`relative w-10 h-5 rounded-full transition-colors shrink-0 ${chatEnabled ? 'bg-[#FF6B00]' : 'bg-border-medium'}`}
        >
          <span
            className={`absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white shadow transition-transform ${chatEnabled ? 'translate-x-5' : ''}`}
          />
        </div>
        <span className="text-xs text-ink-1 group-hover:text-ink-0 transition leading-tight">
          Mostra el widget de xat flotant a la landing
        </span>
      </label>

      {/* Info: prompt unificat entre canals */}
      <div className="bg-[#0d0d1a] border border-border-base rounded p-2.5 text-xs text-ink-3 leading-relaxed">
        El bot respon amb el <span className="text-ink-1">mateix agent IA</span> configurat a{' '}
        <span className="text-accent-light">Agents IA</span> (<span className="font-mono">/portal/agents</span>).
        La personalitat, els serveis i el coneixement s&apos;editen allà i s&apos;apliquen a{' '}
        <span className="text-ink-1">tots els canals</span>: xat web, correu i WhatsApp.
      </div>
    </div>
  );
};
