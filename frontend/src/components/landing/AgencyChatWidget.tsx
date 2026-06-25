'use client';

import { useState, useRef, useEffect } from 'react';
import { useTranslations, useLocale } from 'next-intl';

interface Message {
  role: 'user' | 'assistant';
  text: string;
}

const API_BASE  = process.env.NEXT_PUBLIC_API_URL ?? '';
const BRAND     = '#FF6B00';
const WA_NUMBER = '34654048164';

export function AgencyChatWidget() {
  const t      = useTranslations('widget');
  const locale = useLocale();
  const [open, setOpen]           = useState(false);
  const [step, setStep]           = useState<'prechat' | 'chat'>('prechat');
  const [name, setName]           = useState('');
  const [phone, setPhone]         = useState('');
  const [messages, setMessages]   = useState<Message[]>([]);
  const [input, setInput]         = useState('');
  const [loading, setLoading]     = useState(false);
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [enabled, setEnabled]     = useState<boolean | null>(null);
  const endRef                    = useRef<HTMLDivElement>(null);

  useEffect(() => {
    fetch(`${API_BASE}/api/v1/chat/agency/status`)
      .then(r => r.json())
      .then(d => setEnabled(!!d.enabled))
      .catch(() => setEnabled(false));
  }, []);

  useEffect(() => {
    const stored = localStorage.getItem('amg_agency_sid');
    if (stored) setSessionId(stored);
  }, []);

  useEffect(() => {
    if (open && endRef.current) {
      endRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, open]);

  async function startChat() {
    if (!name.trim() || !phone.trim()) return;
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/v1/chat/agency/sessions`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ contactName: name.trim(), contactPhone: phone.trim(), locale }),
      });
      if (!res.ok) throw new Error();
      const data = await res.json();
      setSessionId(data.sessionId);
      localStorage.setItem('amg_agency_sid', data.sessionId);
      const hist: Message[] = (data.history ?? []).map((m: { role: string; content: string }) => ({
        role: m.role as 'user' | 'assistant',
        text: m.content,
      }));
      setMessages([...hist, { role: 'assistant', text: data.greeting }]);
      setStep('chat');
    } catch {
      setMessages([{ role: 'assistant', text: t('errorStart') }]);
      setStep('chat');
    } finally {
      setLoading(false);
    }
  }

  async function sendMessage() {
    const text = input.trim();
    if (!text || !sessionId || loading) return;
    setInput('');
    setMessages(prev => [...prev, { role: 'user', text }]);
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/v1/chat/sessions/${sessionId}/messages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: text }),
      });
      if (!res.ok) throw new Error();
      const data = await res.json();
      setMessages(prev => [...prev, { role: 'assistant', text: data.reply }]);
      if (data.terminated) {
        localStorage.removeItem('amg_agency_sid');
        setSessionId(null);
      }
    } catch {
      setMessages(prev => [...prev, { role: 'assistant', text: t('errorSend') }]);
    } finally {
      setLoading(false);
    }
  }

  function reset() {
    localStorage.removeItem('amg_agency_sid');
    setSessionId(null);
    setMessages([]);
    setStep('prechat');
    setName('');
    setPhone('');
    setInput('');
  }

  if (!enabled) return null;

  return (
    <>
      {/* WhatsApp personal button */}
      <a
        href={`https://wa.me/${WA_NUMBER}`}
        target="_blank"
        rel="noopener noreferrer"
        aria-label="WhatsApp"
        style={{
          position: 'fixed', bottom: 96, right: 24, zIndex: 9999,
          width: 52, height: 52, borderRadius: '50%',
          background: '#25D366',
          color: '#fff', border: 'none', cursor: 'pointer',
          boxShadow: '0 4px 16px rgba(37,211,102,0.4)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          textDecoration: 'none',
          transition: 'transform 0.2s, box-shadow 0.2s',
        }}
        onMouseEnter={e => { e.currentTarget.style.transform = 'scale(1.1)'; e.currentTarget.style.boxShadow = '0 6px 24px rgba(37,211,102,0.5)'; }}
        onMouseLeave={e => { e.currentTarget.style.transform = 'scale(1)'; e.currentTarget.style.boxShadow = '0 4px 16px rgba(37,211,102,0.4)'; }}
      >
        <svg width="26" height="26" viewBox="0 0 24 24" fill="#fff">
          <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
        </svg>
      </a>

      {/* Floating chat button */}
      <button
        onClick={() => setOpen(o => !o)}
        aria-label={t('ariaLabel')}
        style={{
          position: 'fixed', bottom: 24, right: 24, zIndex: 9999,
          width: 60, height: 60, borderRadius: '50%',
          background: BRAND,
          color: '#fff', border: 'none', cursor: 'pointer',
          boxShadow: '0 4px 20px rgba(255,107,0,0.4)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          transition: 'transform 0.2s, box-shadow 0.2s',
        }}
        onMouseEnter={e => { e.currentTarget.style.transform = 'scale(1.1)'; e.currentTarget.style.boxShadow = '0 6px 28px rgba(255,107,0,0.5)'; }}
        onMouseLeave={e => { e.currentTarget.style.transform = 'scale(1)'; e.currentTarget.style.boxShadow = '0 4px 20px rgba(255,107,0,0.4)'; }}
      >
        {open
          ? <svg width="20" height="20" viewBox="0 0 24 24" fill="#fff"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
          : <svg width="26" height="26" viewBox="0 0 24 24" fill="#fff"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/></svg>
        }
      </button>

      {/* Chat panel */}
      {open && (
        <div style={{
          position: 'fixed', bottom: 96, right: 24, zIndex: 9998,
          width: 340, maxHeight: 520,
          background: '#fff', borderRadius: 16,
          boxShadow: '0 8px 40px rgba(0,0,0,0.15)',
          display: 'flex', flexDirection: 'column', overflow: 'hidden',
          fontFamily: "'Inter', system-ui, sans-serif",
          border: '1px solid #f0f0f0',
        }}>
          {/* Header */}
          <div style={{
            background: BRAND,
            padding: '14px 16px', color: '#fff',
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            flexShrink: 0,
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div style={{
                width: 36, height: 36, borderRadius: '50%',
                background: 'rgba(255,255,255,0.2)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: 18,
              }}>🤖</div>
              <div>
                <div style={{ fontWeight: 700, fontSize: 14 }}>AMG Digitalitzacions</div>
                <div style={{ fontSize: 11, opacity: 0.85 }}>{t('onlineStatus')}</div>
              </div>
            </div>
            {step === 'chat' && (
              <button onClick={reset} style={{
                background: 'rgba(255,255,255,0.2)', border: 'none', color: '#fff',
                borderRadius: 8, padding: '4px 8px', cursor: 'pointer', fontSize: 11,
                fontFamily: 'inherit',
              }}>{t('newChat')}</button>
            )}
          </div>

          {/* Pre-chat form */}
          {step === 'prechat' && (
            <div style={{ padding: '20px 16px', display: 'flex', flexDirection: 'column', gap: 12 }}>
              <p style={{ margin: 0, fontSize: 14, color: '#374151', lineHeight: 1.5 }}>
                {t('greeting')}
              </p>
              <input
                type="text"
                placeholder={t('namePlaceholder')}
                value={name}
                onChange={e => setName(e.target.value)}
                autoFocus
                style={{
                  padding: '10px 12px', borderRadius: 8,
                  border: '1px solid #e5e7eb',
                  fontSize: 14, outline: 'none',
                  color: '#111827', fontFamily: 'inherit',
                }}
              />
              <input
                type="tel"
                placeholder={t('phonePlaceholder')}
                value={phone}
                onChange={e => setPhone(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && startChat()}
                style={{
                  padding: '10px 12px', borderRadius: 8,
                  border: '1px solid #e5e7eb',
                  fontSize: 14, outline: 'none',
                  color: '#111827', fontFamily: 'inherit',
                }}
              />
              <button
                onClick={startChat}
                disabled={loading || !name.trim() || !phone.trim()}
                style={{
                  background: loading || !name.trim() ? '#fed7aa' : BRAND,
                  color: '#fff', border: 'none', borderRadius: 8,
                  padding: '10px 0', fontSize: 14, fontWeight: 600,
                  cursor: loading || !name.trim() ? 'default' : 'pointer',
                  fontFamily: 'inherit', transition: 'background 0.15s',
                }}
              >
                {loading ? t('connecting') : t('startChat')}
              </button>
            </div>
          )}

          {/* Chat messages */}
          {step === 'chat' && (
            <>
              <div style={{
                flex: 1, overflowY: 'auto', padding: '14px 14px 8px',
                display: 'flex', flexDirection: 'column', gap: 10,
                minHeight: 200, maxHeight: 360,
              }}>
                {messages.map((m, i) => (
                  <div key={i} style={{ display: 'flex', justifyContent: m.role === 'user' ? 'flex-end' : 'flex-start' }}>
                    <div style={{
                      maxWidth: '80%', padding: '9px 13px', borderRadius: 12,
                      background: m.role === 'user' ? BRAND : '#f3f4f6',
                      color: m.role === 'user' ? '#fff' : '#111827',
                      fontSize: 13, lineHeight: 1.5,
                      borderBottomRightRadius: m.role === 'user' ? 2 : 12,
                      borderBottomLeftRadius: m.role === 'assistant' ? 2 : 12,
                    }}>
                      {m.text}
                    </div>
                  </div>
                ))}
                {loading && (
                  <div style={{ display: 'flex', justifyContent: 'flex-start' }}>
                    <div style={{
                      background: '#f3f4f6', borderRadius: 12, borderBottomLeftRadius: 2,
                      padding: '10px 14px', display: 'flex', gap: 4, alignItems: 'center',
                    }}>
                      {[0, 1, 2].map(i => (
                        <span key={i} style={{
                          width: 7, height: 7, borderRadius: '50%', background: '#9ca3af',
                          display: 'inline-block',
                          animation: `amgBounce 1.2s ${i * 0.2}s infinite`,
                        }} />
                      ))}
                    </div>
                  </div>
                )}
                <div ref={endRef} />
              </div>
              <div style={{
                padding: '10px 12px', borderTop: '1px solid #f0f0f0',
                display: 'flex', gap: 8, flexShrink: 0,
              }}>
                <input
                  type="text"
                  placeholder={t('messagePlaceholder')}
                  value={input}
                  onChange={e => setInput(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && sendMessage()}
                  maxLength={500}
                  style={{
                    flex: 1, padding: '9px 12px', borderRadius: 8,
                    border: '1px solid #e5e7eb', fontSize: 13, outline: 'none',
                    color: '#111827', background: '#fff',
                    fontFamily: 'inherit',
                  }}
                />
                <button
                  onClick={sendMessage}
                  disabled={loading || !input.trim()}
                  style={{
                    background: loading || !input.trim() ? '#fed7aa' : BRAND,
                    color: '#fff',
                    border: 'none', borderRadius: 8, padding: '0 14px',
                    cursor: loading || !input.trim() ? 'default' : 'pointer',
                    fontSize: 18, transition: 'background 0.15s',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    flexShrink: 0,
                  }}
                >
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
                  </svg>
                </button>
              </div>
            </>
          )}
        </div>
      )}

      <style>{`
        @keyframes amgBounce {
          0%, 80%, 100% { transform: translateY(0); }
          40% { transform: translateY(-5px); }
        }
      `}</style>
    </>
  );
}
