'use client';

import { useTranslations } from 'next-intl';

interface Testimonial {
  name: string;
  role: string;
  text: string;
}

function initials(name: string): string {
  return name.split(' ').slice(0, 2).map(w => w[0]).join('').toUpperCase();
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
          <h2 className="f-display font-black text-3xl sm:text-4xl lg:text-5xl leading-[1.1] tracking-display">
            {t('title')}
          </h2>
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {items.map((item, i) => (
            <div
              key={i}
              className="amg-card p-6 flex flex-col gap-4 hover:border-border-medium transition-colors group"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-accent-muted border border-accent/20 flex items-center justify-center">
                  <span className="f-mono text-xs font-bold text-accent-light">{initials(item.name)}</span>
                </div>
                <div>
                  <p className="font-bold text-sm text-ink-0">{item.name}</p>
                  <p className="f-mono text-caption text-ink-3 uppercase tracking-caption">{item.role}</p>
                </div>
              </div>
              <p className="text-ui text-ink-2 leading-relaxed flex-1">
                &ldquo;{item.text}&rdquo;
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
