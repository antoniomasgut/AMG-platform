'use client';

import { useTranslations } from 'next-intl';

interface Testimonial {
  name: string;
  role: string;
  text: string;
}

export function TestimonialsSection() {
  const t = useTranslations('landing.testimonials');
  const items = t.raw('items') as Testimonial[];

  return (
    <section id="testimonials" className="py-24 px-6 border-t border-border-subtle">
      <div className="max-w-6xl mx-auto">
        <div className="flex items-center gap-2 mb-6">
          <div className="w-1.5 h-1.5 bg-accent" />
          <span className="f-mono text-label uppercase tracking-widest text-accent-light">{t('badge')}</span>
        </div>

        <div className="mb-12">
          <h2 className="f-display font-black text-3xl sm:text-4xl lg:text-5xl leading-[1.1] tracking-display whitespace-pre-line">
            {t('title')}
          </h2>
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {items.map((item, i) => (
            <div
              key={i}
              className="amg-card card-clip p-6 flex flex-col gap-4 hover:border-border-medium transition-colors group"
            >
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" className="text-accent-light shrink-0">
                <path d="M3 21C3 21 5 13 11 13V21H3Z" fill="currentColor" opacity="0.3"/>
                <path d="M13 21C13 21 15 13 21 13V21H13Z" fill="currentColor" opacity="0.3"/>
              </svg>
              <p className="text-ui text-ink-2 leading-relaxed flex-1">
                &ldquo;{item.text}&rdquo;
              </p>
              <div className="pt-4 border-t border-border-subtle">
                <p className="f-display font-bold text-sm text-ink-0">{item.name}</p>
                <p className="f-mono text-caption text-ink-3 uppercase tracking-caption">{item.role}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
