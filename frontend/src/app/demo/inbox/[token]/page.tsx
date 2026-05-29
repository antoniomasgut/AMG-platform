'use client';

import { useState, useEffect, useRef } from 'react';
import { useParams } from 'next/navigation';
import { getDemoInbox, sendDemoReply, type DemoInbox, type DemoMessage } from '@/services/demo';

function timeLeft(expiresAt: string): string {
  const diff = new Date(expiresAt).getTime() - Date.now();
  if (diff <= 0) return 'Expirada';
  const h = Math.floor(diff / 3_600_000);
  const m = Math.floor((diff % 3_600_000) / 60_000);
  return h > 0 ? `${h}h ${m}min` : `${m}min`;
}

function fmtTime(iso: string): string {
  return new Date(iso).toLocaleTimeString('ca-ES', { hour: '2-digit', minute: '2-digit' });
}

function Message({ msg }: { msg: DemoMessage }) {
  const isAgent = msg.role === 'ASSISTANT';
  return (
    <div className={`flex ${isAgent ? 'justify-start' : 'justify-end'} mb-3`}>
      <div
        className={`max-w-[78%] px-4 py-2.5 text-sm leading-relaxed ${
          isAgent
            ? 'bg-[#1e1e2e] border border-[rgba(226,232,240,0.08)] text-[#e2e8f0]'
            : 'bg-[#6366f1] text-white'
        }`}
        style={{ borderRadius: isAgent ? '0 12px 12px 12px' : '12px 0 12px 12px' }}
      >
        <p style={{ whiteSpace: 'pre-wrap' }}>{msg.content}</p>
        <span className="block text-[10px] mt-1 opacity-50">{fmtTime(msg.createdAt)}</span>
      </div>
    </div>
  );
}

function BlockedBanner({ reason }: { reason: string | null }) {
  return (
    <div className="fixed inset-0 bg-black/70 z-50 flex items-center justify-center p-6">
      <div className="bg-[#1e1e2e] border border-[#ef4444]/40 max-w-sm w-full p-6 text-center">
        <div className="text-3xl mb-3">&#x26A0;&#xFE0F;</div>
        <div className="font-mono text-[#f87171] font-bold text-base mb-2">
          Sessió tancada
        </div>
        <p className="font-mono text-[#94a3b8] text-sm mb-1">
          {reason ?? 'Contingut inadequat detectat.'}
        </p>
        <p className="font-mono text-[#64748b] text-xs mt-3">
          Aquesta sessió ha estat tancada automàticament.
          Posa&apos;t en contacte amb{' '}
          <a href="https://amgdl.com" className="text-[#818cf8] hover:underline">
            AMG Digitalització
          </a>{' '}
          per obtenir una nova demo.
        </p>
      </div>
    </div>
  );
}

export default function DemoInboxPage() {
  const params = useParams();
  const token = params.token as string;

  const [inbox, setInbox] = useState<DemoInbox | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [expired, setExpired] = useState(false);
  const [text, setText] = useState('');
  const [sending, setSending] = useState(false);
  const [ttl, setTtl] = useState('');
  const bottomRef = useRef<HTMLDivElement>(null);

  const load = async () => {
    try {
      const data = await getDemoInbox(token);
      if (new Date(data.expiresAt).getTime() < Date.now()) {
        setExpired(true);
        return;
      }
      setInbox(data);
      setTtl(timeLeft(data.expiresAt));
    } catch {
      setLoadError('Demo no disponible o expirada.');
    }
  };

  useEffect(() => {
    load();
    const poll = setInterval(load, 5000);
    return () => clearInterval(poll);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [inbox?.thread.length]);

  const send = async () => {
    if (!text.trim() || sending) return;
    const msg = text.trim();
    setText('');
    setSending(true);
    try {
      const res = await sendDemoReply(token, msg);
      if (res.blocked === 'true') {
        // Reload to get blocked state from server
        await load();
      } else {
        setTimeout(load, 1500);
      }
    } catch {
      setText(msg); // restore text so user can retry
    } finally {
      setSending(false);
    }
  };

  if (expired || loadError) {
    return (
      <div className="min-h-screen bg-[#0d0d1a] flex items-center justify-center p-6" style={{ fontFamily: 'monospace' }}>
        <div className="text-center max-w-sm">
          <div className="text-4xl mb-4">&#x23F1;</div>
          <div className="text-[#e2e8f0] text-lg font-bold mb-2">Demo expirada</div>
          <p className="text-[#94a3b8] text-sm">
            {loadError ?? "Aquesta demo ja no és accessible. Posa't en contacte amb AMG Digitalització per obtenir un nou accés."}
          </p>
          <a href="https://amgdl.com" className="inline-block mt-6 text-xs text-[#818cf8] hover:underline">
            amgdl.com
          </a>
        </div>
      </div>
    );
  }

  if (!inbox) {
    return (
      <div className="min-h-screen bg-[#0d0d1a] flex items-center justify-center">
        <span className="w-6 h-6 border-2 border-[#6366f1] border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  const isBlocked = inbox.blocked;

  return (
    <div className="min-h-screen bg-[#0d0d1a] flex flex-col" style={{ fontFamily: "'JetBrains Mono', 'Fira Code', monospace" }}>
      {isBlocked && <BlockedBanner reason={inbox.blockReason} />}

      {/* Header */}
      <div className="border-b border-[rgba(226,232,240,0.08)] px-4 py-3 flex items-center justify-between shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-[#6366f1] flex items-center justify-center text-white text-xs font-bold shrink-0">
            AMG
          </div>
          <div>
            <div className="text-[#e2e8f0] text-sm font-bold">Inbox Demo · AMG Digitalització</div>
            <div className="text-[#64748b] text-[10px] uppercase tracking-wider">Agent IA · Canal Email</div>
          </div>
        </div>
        <div className="text-right shrink-0">
          <div className="text-[#818cf8] text-xs">&#x23F1; {ttl}</div>
          <div className="text-[#475569] text-[10px]">Expira en</div>
        </div>
      </div>

      {/* Company info card */}
      {(inbox.companyName || inbox.agentContext) && (
        <div className="bg-[#1a1a2e] border-b border-[rgba(99,102,241,0.15)] px-4 py-3">
          <div className="text-[#818cf8] text-[10px] uppercase tracking-widest mb-1">Empresa</div>
          {inbox.companyName && (
            <div className="text-[#e2e8f0] text-sm font-bold">{inbox.companyName}</div>
          )}
          {inbox.agentContext && (
            <p className="text-[#94a3b8] text-xs mt-0.5 leading-relaxed">{inbox.agentContext}</p>
          )}
        </div>
      )}

      {/* Email banner */}
      <div className="bg-[#1e1e2e] border-b border-[rgba(226,232,240,0.06)] px-4 py-2 flex items-center gap-2">
        <span className="text-[#818cf8]">&#x1F4E7;</span>
        <span className="text-[#94a3b8] text-xs">
          Envia un correu a{' '}
          <span className="text-[#818cf8] font-bold">{inbox.demoEmail}</span>
          {' '}o escriu aquí baix.
        </span>
      </div>

      {/* Thread */}
      <div className="flex-1 overflow-y-auto px-4 py-4">
        {inbox.thread.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-center py-16">
            <div className="text-4xl mb-4">&#x2709;&#xFE0F;</div>
            <p className="text-[#64748b] text-sm">Cap missatge encara.</p>
            <p className="text-[#475569] text-xs mt-1">
              Envia un email a{' '}
              <span className="text-[#818cf8]">{inbox.demoEmail}</span>{' '}
              o escriu un missatge aquí baix.
            </p>
          </div>
        ) : (
          inbox.thread.map((msg) => <Message key={msg.id} msg={msg} />)
        )}
        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <div className="border-t border-[rgba(226,232,240,0.08)] p-4 flex gap-3 shrink-0">
        <textarea
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(); } }}
          placeholder={isBlocked ? 'Sessió tancada' : 'Escriu un missatge... (Enter per enviar)'}
          disabled={isBlocked}
          rows={2}
          className="flex-1 bg-[#1e1e2e] border border-[rgba(226,232,240,0.1)] text-[#e2e8f0] text-sm px-3 py-2 resize-none focus:outline-none focus:border-[#6366f1] placeholder-[#475569] disabled:opacity-40"
          style={{ borderRadius: 4 }}
        />
        <button
          onClick={send}
          disabled={!text.trim() || sending || isBlocked}
          className="px-4 bg-[#6366f1] text-white text-sm font-bold disabled:opacity-40 hover:bg-[#4f46e5] transition-colors shrink-0"
          style={{ borderRadius: 4 }}
        >
          {sending ? '...' : '&#x2192;'}
        </button>
      </div>

      {/* Footer */}
      <div className="px-4 py-2 text-center border-t border-[rgba(226,232,240,0.04)]">
        <span className="text-[#334155] text-[10px]">
          Demo ·{' '}
          <a href="https://amgdl.com" className="hover:text-[#818cf8] transition-colors">
            AMG Digitalització
          </a>{' '}
          · 654 048 164
        </span>
      </div>
    </div>
  );
}
