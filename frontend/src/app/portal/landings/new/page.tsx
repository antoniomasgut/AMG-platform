'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useMutation } from '@tanstack/react-query';
import { getCurrentUser } from '@/services/auth';
import { createLanding } from '@/services/factory';
import { parseColorSchemes, type TemplateSummary, type ColorScheme } from '@/services/templates';
import { TemplateSelector } from '@/components/factory/TemplateSelector';
import { AMGButton } from '@/components/ui/button';

function slugify(text: string): string {
  return text.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
}

export default function NewLandingPage() {
  const router = useRouter();
  const [step, setStep] = useState<'template' | 'scheme' | 'details'>('template');
  const [selectedTemplate, setSelectedTemplate] = useState<TemplateSummary | null>(null);
  const [selectedScheme, setSelectedScheme] = useState<ColorScheme | null>(null);
  const [title, setTitle] = useState('');
  const [slug, setSlug] = useState('');

  const user = getCurrentUser();

  const createMutation = useMutation({
    mutationFn: async () => {
      if (!user?.tenantId) throw new Error('No tenant');
      const schemeStyles = selectedScheme ? {
        primaryColor: selectedScheme.primary,
        accentColor: selectedScheme.accent,
        fontHeading: selectedScheme.fontHeading,
        fontBody: selectedScheme.fontBody,
        bgColor: selectedScheme.bg,
        textColor: selectedScheme.text,
      } : undefined;
      return createLanding(user.tenantId, title, slug, selectedTemplate?.id || undefined, schemeStyles);
    },
    onSuccess: (landing) => {
      router.push(`/portal/landings/${landing.id}/edit`);
    },
  });

  const handleSelectTemplate = (tpl: TemplateSummary) => {
    setSelectedTemplate(tpl);
    setSelectedScheme(null);
    setTitle('');
    setSlug('');
    const schemes = parseColorSchemes(tpl.colorSchemes);
    setStep(schemes.length > 0 ? 'scheme' : 'details');
  };

  if (!user) {
    router.replace('/login');
    return null;
  }

  if (step === 'template') {
    return (
      <div className="p-4 sm:p-8 space-y-6">
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / landings / nova /</span>
          <div className="f-display font-bold text-xl mt-1">Selecciona una plantilla</div>
          <p className="text-sm text-ink-1 mt-1">Tria una plantilla per començar</p>
        </div>
        <TemplateSelector onSelect={handleSelectTemplate} />
      </div>
    );
  }

  if (step === 'scheme') {
    const schemes = parseColorSchemes(selectedTemplate?.colorSchemes);
    return (
      <div className="p-4 sm:p-8 space-y-6">
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / landings / nova /</span>
          <div className="f-display font-bold text-xl mt-1">Tria l&apos;estil visual</div>
          <p className="text-sm text-ink-1 mt-1">Selecciona una paleta de colors per a la teva landing</p>
        </div>
        <div className="grid grid-cols-2 gap-4 max-w-lg">
          {schemes.map((scheme, i) => (
            <button
              key={i}
              onClick={() => { setSelectedScheme(scheme); setStep('details'); }}
              className="amg-card card-clip p-4 text-left hover:ring-1 hover:ring-[#FF6B00] transition group"
            >
              <div className="flex gap-2 mb-3">
                <div className="w-8 h-8 rounded-md" style={{ background: scheme.primary }} />
                <div className="w-8 h-8 rounded-md" style={{ background: scheme.accent }} />
                <div className="w-8 h-8 rounded-md border border-white/10" style={{ background: scheme.bg }} />
              </div>
              <div className="f-mono text-xs font-bold text-ink-0 mb-0.5">{scheme.name}</div>
              <div className="f-mono text-[9px] text-ink-3" style={{ fontFamily: scheme.fontHeading }}>
                {scheme.fontHeading.split(',')[0]}
              </div>
            </button>
          ))}
        </div>
        <div className="flex gap-3">
          <AMGButton variant="ghost" onClick={() => setStep('template')}>← Enrere</AMGButton>
          <AMGButton variant="ghost" onClick={() => { setSelectedScheme(null); setStep('details'); }}>Ometre</AMGButton>
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-8 max-w-lg space-y-6">
      <div>
        <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / landings / nova /</span>
        <div className="f-display font-bold text-xl mt-1">Configura la landing</div>
        {selectedScheme && (
          <div className="flex items-center gap-2 mt-2">
            <div className="w-3 h-3 rounded-full" style={{ background: selectedScheme.primary }} />
            <div className="w-3 h-3 rounded-full" style={{ background: selectedScheme.accent }} />
            <span className="f-mono text-[10px] text-ink-3">{selectedScheme.name}</span>
            <button onClick={() => setStep('scheme')} className="f-mono text-[9px] text-accent-light underline ml-1">
              canviar
            </button>
          </div>
        )}
      </div>

      <div className="amg-card card-clip p-5 space-y-4">
        <div>
          <label className="f-mono text-label uppercase text-ink-3 block mb-1">Títol</label>
          <input
            value={title}
            onChange={(e) => {
              setTitle(e.target.value);
              if (!slug) setSlug(slugify(e.target.value));
            }}
            className="w-full bg-[#0d0d1a] border border-border-medium rounded p-2 text-sm text-ink-0"
            placeholder="Nom de la landing"
          />
        </div>
        <div>
          <label className="f-mono text-label uppercase text-ink-3 block mb-1">Slug (URL)</label>
          <input
            value={slug}
            onChange={(e) => setSlug(e.target.value)}
            className="w-full bg-[#0d0d1a] border border-border-medium rounded p-2 text-sm text-ink-0"
            placeholder="nom-de-la-landing"
          />
          <p className="f-mono text-[9px] text-ink-3 mt-1">/{slug}</p>
        </div>

        <div className="flex gap-3 pt-2">
          <AMGButton variant="ghost" onClick={() => setStep(parseColorSchemes(selectedTemplate?.colorSchemes).length > 0 ? 'scheme' : 'template')}>
            ← Enrere
          </AMGButton>
          <AMGButton
            onClick={() => createMutation.mutate()}
            disabled={!title || !slug || createMutation.isPending}
            loading={createMutation.isPending}
          >
            Crear landing
          </AMGButton>
        </div>
      </div>
    </div>
  );
}
