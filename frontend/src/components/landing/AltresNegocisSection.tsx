'use client';

import { useTranslations } from 'next-intl';

const WHATSAPP_URL = 'https://wa.me/34654048164';

export function AltresNegocisSection() {
  const t = useTranslations('altresNegocis');
  const sectors = t.raw('sectors') as string[];

  return (
    <section className="py-20 sm:py-24 px-6 border-t border-border-subtle">
      <div className="max-w-5xl mx-auto">
        <div className="border border-border-medium bg-bg-1 p-8 sm:p-12 relative overflow-hidden">
          <div
            className="absolute inset-0 pointer-events-none"
            style={{ background: 'radial-gradient(ellipse at 80% 50%, rgba(255,107,0,0.08), transparent 60%)' }}
          />
          <div className="relative z-10 flex flex-col lg:flex-row lg:items-center gap-8 lg:gap-16">
            <div className="flex-1">
              <div className="flex items-center gap-2 mb-4">
                <div className="w-2 h-2 bg-accent shrink-0" />
                <span className="f-mono text-label uppercase tracking-widest text-ink-2">{t('label')}</span>
              </div>
              <h2 className="font-bold text-2xl sm:text-3xl uppercase leading-tight tracking-tight mb-4 text-ink-0">
                {t('title')}
              </h2>
              <p className="text-md text-ink-1 leading-relaxed mb-6 max-w-lg">
                {t('subtitle')}
              </p>
              <a
                href={WHATSAPP_URL}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-2 btn-clip bg-accent hover:bg-accent-light text-black font-semibold f-mono text-xs uppercase px-8 h-11 transition-colors"
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347z" />
                  <path d="M12 0C5.373 0 0 5.373 0 12c0 2.123.554 4.117 1.522 5.847L0 24l6.335-1.502A11.938 11.938 0 0012 24c6.627 0 12-5.373 12-12S18.627 0 12 0zm0 21.882a9.875 9.875 0 01-5.034-1.378l-.361-.214-3.741.981.999-3.648-.235-.374A9.845 9.845 0 012.118 12C2.118 6.533 6.533 2.118 12 2.118c5.467 0 9.882 4.415 9.882 9.882 0 5.467-4.415 9.882-9.882 9.882z" />
                </svg>
                {t('cta')}
              </a>
            </div>
            <div className="flex-1">
              <div className="f-mono text-label text-xs uppercase tracking-widest text-ink-3 mb-4">
                {t('examplesLabel')}
              </div>
              <div className="flex flex-wrap gap-2">
                {sectors.map((sector) => (
                  <span
                    key={sector}
                    className="border border-border-base text-ink-2 f-mono text-xs px-3 py-1.5"
                  >
                    {sector}
                  </span>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
