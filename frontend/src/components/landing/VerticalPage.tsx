import Link from 'next/link';
import { VerticalFAQ } from './VerticalFAQ';

const WHATSAPP_URL = 'https://wa.me/34654048164';

function WhatsAppIcon({ size = 16 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M12.04 2C6.58 2 2.13 6.45 2.13 11.91c0 1.75.46 3.45 1.32 4.95L2 22l5.25-1.38c1.45.79 3.08 1.21 4.79 1.21 5.46 0 9.91-4.45 9.91-9.91C21.95 6.45 17.5 2 12.04 2zm0 18.15c-1.52 0-3.01-.41-4.31-1.18l-.31-.18-3.12.82.83-3.04-.2-.31a8.2 8.2 0 0 1-1.26-4.35c0-4.54 3.7-8.23 8.24-8.23 4.54 0 8.23 3.69 8.23 8.23 0 4.54-3.69 8.24-8.23 8.24zm4.52-6.16c-.25-.12-1.47-.72-1.69-.81-.23-.08-.39-.12-.56.13-.16.25-.64.81-.79.97-.14.17-.29.19-.54.06-.25-.12-1.05-.39-2-1.23-.74-.66-1.23-1.48-1.38-1.73-.14-.25-.01-.38.11-.51.11-.11.25-.29.37-.43.13-.14.17-.25.25-.41.08-.17.04-.31-.02-.43-.06-.12-.56-1.34-.76-1.84-.2-.48-.41-.42-.56-.42l-.48-.01c-.17 0-.43.06-.66.31-.23.25-.86.85-.86 2.07s.89 2.4 1.01 2.56c.12.17 1.75 2.67 4.23 3.74.59.26 1.05.41 1.41.52.59.19 1.13.16 1.56.1.48-.07 1.47-.6 1.67-1.18.21-.58.21-1.07.14-1.18-.06-.1-.22-.16-.47-.28z" />
    </svg>
  );
}

interface Problem {
  num: string;
  title: string;
  desc: string;
}

interface Solution {
  num: string;
  title: string;
  desc: string;
  pills: string[];
}

interface FAQItem {
  q: string;
  a: string;
}

interface VerticalPageProps {
  locale: string;
  badge: string;
  h1: string;
  h1Accent: string;
  subtitle: string;
  problemLabel: string;
  problemHeadline: string;
  problems: Problem[];
  solutionsLabel: string;
  solutionsHeadline: string;
  solutions: Solution[];
  forWhoLabel: string;
  forWho: string[];
  faqTitle: string;
  faqSectionLabel: string;
  faqs: FAQItem[];
  testimonialQuote: string;
  testimonialName: string;
  testimonialRole: string;
  ctaTitle: string;
  ctaSubtitle: string;
  ctaButton: string;
  ctaContact: string;
  consultaGratuita: string;
  veureFunciona: string;
  portalLink: string;
}

export function VerticalPage({
  locale,
  badge,
  h1,
  h1Accent,
  subtitle,
  problemLabel,
  problemHeadline,
  problems,
  solutionsLabel,
  solutionsHeadline,
  solutions,
  forWhoLabel,
  forWho,
  faqTitle,
  faqSectionLabel,
  faqs,
  testimonialQuote,
  testimonialName,
  testimonialRole,
  ctaTitle,
  ctaSubtitle,
  ctaButton,
  ctaContact,
  consultaGratuita,
  veureFunciona,
  portalLink,
}: VerticalPageProps) {
  const faqJsonLd = {
    '@context': 'https://schema.org',
    '@type': 'FAQPage',
    mainEntity: faqs.map(f => ({
      '@type': 'Question',
      name: f.q,
      acceptedAnswer: { '@type': 'Answer', text: f.a },
    })),
  };

  const showTestimonial = !testimonialQuote.startsWith('[');

  return (
    <>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(faqJsonLd) }}
      />
      {/* ── HERO ── */}
      <section className="relative min-h-dvh flex flex-col items-center justify-center px-6 pt-20 pb-16 text-center overflow-hidden border-b border-border-subtle">
        <div
          className="absolute inset-0 pointer-events-none"
          style={{
            background:
              'radial-gradient(ellipse at 50% 30%, rgba(255,107,0,0.22) 0%, rgba(255,107,0,0.06) 40%, transparent 65%)',
          }}
        />
        <div className="relative z-10 max-w-4xl mx-auto w-full">
          <div className="inline-flex items-center gap-2 mb-8">
            <div className="w-2 h-2 bg-accent shrink-0" />
            <span className="f-mono text-label uppercase tracking-widest text-ink-2">{badge}</span>
          </div>

          <h1 className="font-bold text-4xl sm:text-5xl lg:text-6xl leading-[1.05] tracking-tight mb-6 text-ink-0">
            {h1}{' '}
            <span className="text-accent">{h1Accent}</span>
          </h1>

          <p className="text-lg sm:text-xl text-ink-1 max-w-2xl mx-auto mb-10 leading-relaxed">
            {subtitle}
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <a
              href={WHATSAPP_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="btn-clip bg-accent hover:bg-accent-light text-black font-semibold f-mono text-xs uppercase px-8 h-12 flex items-center gap-2 transition-colors w-full sm:w-auto justify-center"
            >
              <WhatsAppIcon size={16} />
              {consultaGratuita}
            </a>
            <a
              href="#solucions"
              className="f-mono text-xs uppercase text-ink-2 hover:text-ink-0 border border-border-base hover:border-border-medium px-8 h-12 flex items-center transition-colors w-full sm:w-auto justify-center"
            >
              {veureFunciona}
            </a>
          </div>
        </div>
      </section>

      {/* ── EL PROBLEMA ── */}
      <section id="problema" className="py-20 sm:py-24 px-6 border-b border-border-subtle">
        <div className="max-w-5xl mx-auto">
          <div className="flex items-center gap-2 mb-5">
            <div className="w-2 h-2 bg-accent shrink-0" />
            <span className="f-mono text-label uppercase tracking-widest text-ink-2">{problemLabel}</span>
          </div>
          <h2 className="font-bold text-2xl sm:text-3xl lg:text-5xl uppercase leading-tight tracking-tight mb-10 sm:mb-12 text-ink-0 max-w-3xl">
            {problemHeadline}
          </h2>
          <div
            className="grid grid-cols-1 sm:grid-cols-3 border border-border-subtle"
            style={{ gap: '1px', background: 'var(--color-border-subtle)' }}
          >
            {problems.map((p) => (
              <div key={p.num} className="bg-bg-0 p-6 sm:p-8 hover:bg-bg-1 transition-colors">
                <div className="f-mono text-label text-ink-3 mb-5">{p.num}</div>
                <h3 className="font-semibold text-lg sm:text-xl text-ink-0 mb-3">{p.title}</h3>
                <p className="text-md text-ink-1 leading-relaxed">{p.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── SOLUCIONS ── */}
      <section id="solucions" className="py-20 sm:py-24 px-6 border-b border-border-subtle">
        <div className="max-w-5xl mx-auto">
          <div className="flex items-center gap-2 mb-5">
            <div className="w-2 h-2 bg-accent shrink-0" />
            <span className="f-mono text-label uppercase tracking-widest text-ink-2">{solutionsLabel}</span>
          </div>
          <h2 className="font-bold text-2xl sm:text-3xl lg:text-4xl leading-tight tracking-tight mb-10 sm:mb-12 text-ink-0">
            {solutionsHeadline}
          </h2>
          <div
            className="grid grid-cols-1 sm:grid-cols-2 border border-border-subtle"
            style={{ gap: '1px', background: 'var(--color-border-subtle)' }}
          >
            {solutions.map((s) => (
              <div
                key={s.num}
                className="bg-bg-0 p-6 sm:p-8 hover:bg-bg-1 transition-colors flex flex-col gap-4"
              >
                <div className="f-mono text-sm font-semibold text-accent">{s.num}</div>
                <div>
                  <h3 className="font-semibold text-lg sm:text-xl text-ink-0 mb-2">{s.title}</h3>
                  <p className="text-md text-ink-1 leading-relaxed">{s.desc}</p>
                </div>
                <div className="flex flex-wrap gap-2 mt-auto pt-1">
                  {s.pills.map((pill) => (
                    <span
                      key={pill}
                      className="f-mono text-label border border-border-base text-accent px-3 py-1.5"
                    >
                      {pill}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── PER A QUI ── */}
      <section className="py-16 sm:py-20 px-6 border-b border-border-subtle">
        <div className="max-w-5xl mx-auto">
          <div className="flex items-center gap-2 mb-7">
            <div className="w-2 h-2 bg-accent shrink-0" />
            <span className="f-mono text-label uppercase tracking-widest text-ink-2">{forWhoLabel}</span>
          </div>
          <div className="flex flex-wrap gap-3">
            {forWho.map((sector) => (
              <span
                key={sector}
                className="f-mono text-sm text-ink-0 border border-border-base bg-bg-1 px-4 py-2.5"
              >
                {sector}
              </span>
            ))}
          </div>
        </div>
      </section>

      {/* ── FAQ ── */}
      <VerticalFAQ title={faqTitle} sectionLabel={faqSectionLabel} items={faqs} />

      {/* ── TESTIMONI ── */}
      {showTestimonial && <section className="py-20 sm:py-24 px-6 border-t border-border-subtle border-b border-border-subtle">
        <div className="max-w-2xl mx-auto">
          <div className="relative bg-bg-1 border border-border-base p-8 sm:p-12 text-center">
            <div
              className="font-bold text-7xl text-accent select-none"
              style={{ lineHeight: '0.6', height: '32px' }}
            >
              &ldquo;
            </div>
            <p className="font-semibold text-xl sm:text-2xl text-ink-0 leading-snug mt-6 mb-7">
              {testimonialQuote}
            </p>
            <div className="flex justify-center gap-1 mb-4">
              {Array.from({ length: 5 }).map((_, i) => (
                <span key={i} className="text-accent text-lg">★</span>
              ))}
            </div>
            <div className="f-mono text-sm text-ink-0 tracking-wide">{testimonialName}</div>
            <div className="f-mono text-label text-ink-3 mt-1">{testimonialRole}</div>
          </div>
        </div>
      </section>}

      {/* ── CTA FINAL ── */}
      <section
        id="contacte"
        className="relative py-24 sm:py-32 px-6 text-center overflow-hidden border-b border-border-subtle"
      >
        <div
          className="absolute inset-0 pointer-events-none"
          style={{
            background:
              'radial-gradient(ellipse at 50% 50%, rgba(255,107,0,0.20) 0%, rgba(255,107,0,0.05) 42%, transparent 68%)',
          }}
        />
        <div className="relative z-10 max-w-2xl mx-auto">
          <h2 className="font-bold text-3xl sm:text-4xl lg:text-5xl leading-[1.05] tracking-tight mb-5 text-ink-0">
            {ctaTitle}
          </h2>
          <p className="text-lg text-ink-1 mb-10">{ctaSubtitle}</p>
          <a
            href={WHATSAPP_URL}
            target="_blank"
            rel="noopener noreferrer"
            className="btn-clip inline-flex items-center gap-3 bg-accent hover:bg-accent-light text-black font-semibold f-mono text-sm uppercase px-8 sm:px-10 h-14 transition-colors"
          >
            <WhatsAppIcon size={20} />
            {ctaButton}
          </a>
          <p className="f-mono text-label text-ink-3 mt-6">{ctaContact}</p>

          <div className="mt-10 pt-8 border-t border-border-subtle text-sm text-ink-2">
            <Link
              href={`/${locale}/login`}
              className="text-accent hover:text-accent-light underline underline-offset-2 transition-colors"
            >
              {portalLink}
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}
