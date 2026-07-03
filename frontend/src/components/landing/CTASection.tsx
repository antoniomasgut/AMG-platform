'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useTranslations, useLocale } from 'next-intl';

export function CTASection() {
  const t = useTranslations('landing.cta');
  const locale = useLocale();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');
  const [state, setState] = useState<'idle' | 'sending' | 'ok' | 'error'>('idle');
  const [utms, setUtms] = useState<{ utmSource?: string; utmMedium?: string; utmCampaign?: string }>({});

  useEffect(() => {
    const p = new URLSearchParams(window.location.search);
    const captured = {
      utmSource:   p.get('utm_source')   ?? undefined,
      utmMedium:   p.get('utm_medium')   ?? undefined,
      utmCampaign: p.get('utm_campaign') ?? undefined,
    };
    setUtms(captured);
    // Persisteix a sessionStorage per si l'usuari navega per la pàgina
    if (captured.utmSource) sessionStorage.setItem('utm_source',   captured.utmSource);
    if (captured.utmMedium) sessionStorage.setItem('utm_medium',   captured.utmMedium);
    if (captured.utmCampaign) sessionStorage.setItem('utm_campaign', captured.utmCampaign);
  }, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setState('sending');
    // Recupera UTMs de sessionStorage si no vénen de la URL actual
    const source   = utms.utmSource   ?? sessionStorage.getItem('utm_source')   ?? undefined;
    const medium   = utms.utmMedium   ?? sessionStorage.getItem('utm_medium')   ?? undefined;
    const campaign = utms.utmCampaign ?? sessionStorage.getItem('utm_campaign') ?? undefined;
    try {
      const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? '';
      const res = await fetch(`${apiUrl}/api/v1/contact`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, email, message, utmSource: source, utmMedium: medium, utmCampaign: campaign }),
      });
      setState(res.ok ? 'ok' : 'error');
    } catch {
      setState('error');
    }
  }

  return (
    <section id="contact" className="py-24 px-6 border-t border-border-subtle">
      <div className="max-w-3xl mx-auto">
        <div className="p-8 sm:p-12 border border-border-medium relative overflow-hidden bg-bg-1">
          <div className="absolute inset-0 pointer-events-none" style={{
            background: 'radial-gradient(ellipse at 50% 0%, rgba(255,107,0,0.15), transparent 60%)',
          }} />

          <div className="relative z-10">
            <h2 className="f-display font-black text-3xl sm:text-4xl lg:text-5xl leading-[1.1] tracking-display mb-4 whitespace-pre-line text-center">
              {t('title')}
            </h2>
            <p className="text-ui text-ink-1 mb-8 text-center">{t('subtitle')}</p>

            {/* Quick CTAs */}
            <div className="flex flex-col sm:flex-row items-center justify-center gap-3 mb-10">
              <a
                href="https://wa.me/34654048164"
                target="_blank"
                rel="noopener noreferrer"
                className="btn-clip bg-accent hover:bg-accent-light text-black font-semibold f-mono text-xs uppercase px-8 h-11 flex items-center gap-2 transition-colors"
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347z" />
                  <path d="M12 0C5.373 0 0 5.373 0 12c0 2.123.554 4.117 1.522 5.847L0 24l6.335-1.502A11.938 11.938 0 0012 24c6.627 0 12-5.373 12-12S18.627 0 12 0zm0 21.882a9.875 9.875 0 01-5.034-1.378l-.361-.214-3.741.981.999-3.648-.235-.374A9.845 9.845 0 012.118 12C2.118 6.533 6.533 2.118 12 2.118c5.467 0 9.882 4.415 9.882 9.882 0 5.467-4.415 9.882-9.882 9.882z" />
                </svg>
                {t('button')}
              </a>
              <Link
                href={`/${locale}/login`}
                className="f-mono text-xs uppercase text-ink-2 hover:text-ink-0 border border-border-base hover:border-border-medium px-8 h-11 flex items-center transition-colors"
              >
                {t('buttonSecondary')}
              </Link>
            </div>

            {/* Inline contact form */}
            <div className="border-t border-border-subtle pt-8">
              <div className="f-mono text-label text-xs uppercase text-ink-3 tracking-widest text-center mb-6">
                {t('formTitle')}
              </div>

              {state === 'ok' ? (
                <div className="text-center py-6">
                  <div className="w-10 h-10 bg-success/10 border border-success/30 flex items-center justify-center mx-auto mb-3">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#39d353" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                      <polyline points="20 6 9 17 4 12" />
                    </svg>
                  </div>
                  <div className="f-display font-bold text-sm">{t('formSuccess')}</div>
                  <p className="f-mono text-label text-xs text-ink-2 mt-1">{t('formSuccessDesc')}</p>
                </div>
              ) : (
                <form onSubmit={handleSubmit} className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-w-lg mx-auto">
                  <input
                    type="text"
                    required
                    placeholder={t('formName')}
                    value={name}
                    onChange={e => setName(e.target.value)}
                    className="bg-bg-0 border border-border-base text-ink-0 text-sm px-3 h-10 focus:outline-none focus:border-accent f-mono placeholder:text-ink-3"
                  />
                  <input
                    type="email"
                    required
                    placeholder={t('formEmail')}
                    value={email}
                    onChange={e => setEmail(e.target.value)}
                    className="bg-bg-0 border border-border-base text-ink-0 text-sm px-3 h-10 focus:outline-none focus:border-accent f-mono placeholder:text-ink-3"
                  />
                  <textarea
                    required
                    placeholder={t('formMessage')}
                    value={message}
                    onChange={e => setMessage(e.target.value)}
                    rows={3}
                    className="sm:col-span-2 bg-bg-0 border border-border-base text-ink-0 text-sm px-3 py-2 focus:outline-none focus:border-accent f-mono placeholder:text-ink-3 resize-none"
                  />
                  {state === 'error' && (
                    <p className="sm:col-span-2 f-mono text-xs text-danger">{t('formError')}</p>
                  )}
                  <button
                    type="submit"
                    disabled={state === 'sending'}
                    className="sm:col-span-2 bg-bg-2 border border-border-medium hover:border-accent text-ink-1 hover:text-ink-0 font-semibold f-mono text-xs uppercase h-10 flex items-center justify-center transition-colors disabled:opacity-50"
                  >
                    {state === 'sending' ? '…' : t('formSubmit')}
                  </button>
                </form>
              )}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
