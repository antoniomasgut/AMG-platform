'use client';

import { useState } from 'react';

interface FAQItem {
  q: string;
  a: string;
}

interface VerticalFAQProps {
  title: string;
  sectionLabel: string;
  items: FAQItem[];
}

export function VerticalFAQ({ title, sectionLabel, items }: VerticalFAQProps) {
  const [open, setOpen] = useState<number>(0);

  return (
    <section id="faq" className="py-24 px-6 border-t border-border-subtle">
      <div className="max-w-3xl mx-auto">
        <div className="flex items-center gap-2 mb-5">
          <div className="w-2 h-2 bg-accent shrink-0" />
          <span className="f-mono text-label uppercase tracking-widest text-ink-2">
            {sectionLabel}
          </span>
        </div>
        <h2 className="font-bold text-3xl sm:text-4xl leading-tight tracking-tight mb-10 text-ink-0">
          {title}
        </h2>
        <div className="border-t border-border-subtle">
          {items.map((item, i) => (
            <div key={i} className="border-b border-border-subtle">
              <button
                onClick={() => setOpen(open === i ? -1 : i)}
                className="w-full flex items-center justify-between gap-5 py-6 text-left bg-transparent"
              >
                <span className="font-semibold text-md text-ink-0">{item.q}</span>
                <span className="shrink-0 f-mono text-xl text-accent w-5 text-center">
                  {open === i ? '−' : '+'}
                </span>
              </button>
              {open === i && (
                <div className="pb-6 pr-10">
                  <p className="text-md text-ink-1 leading-relaxed">{item.a}</p>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
