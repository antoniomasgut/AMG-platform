'use client';

import { Suspense, useState } from 'react';
import { useRouter, useSearchParams, useParams } from 'next/navigation';
import { useMutation } from '@tanstack/react-query';
import { getCurrentUser } from '@/services/auth';
import { createLanding } from '@/services/factory';
import { TemplateSelector } from '@/components/factory/TemplateSelector';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { I } from '@/components/ui/icons';

function slugify(text: string): string {
  return text.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
}

function NewLandingForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const params = useParams();
  const locale = params.locale as string;
  const [step, setStep] = useState<'template' | 'details'>('template');
  const [templateId, setTemplateId] = useState('');
  const [title, setTitle] = useState('');
  const [slug, setSlug] = useState('');

  const user = getCurrentUser();
  const urlTenantId = searchParams.get('tenantId');

  const [errorMsg, setErrorMsg] = useState('');

  const createMutation = useMutation({
    mutationFn: async () => {
      setErrorMsg('');
      const tid = urlTenantId || user?.tenantId;
      if (!tid) throw new Error('No tenant');
      return createLanding(tid, title, slug, templateId || undefined);
    },
    onSuccess: (landing) => {
      router.push(`/portal/landings/${landing.id}/edit`);
    },
    onError: (err: Error) => {
      setErrorMsg(err.message || 'Error en crear la landing');
    },
  });

  const handleSelectTemplate = (id: string) => {
    setTemplateId(id);
    setStep('details');
    // Derive default name from template — will load from API in details step
    setTitle('');
    setSlug('');
  };

  if (!user) {
    router.replace('/login');
    return null;
  }

  if (step === 'template') {
    return (
      <PortalShell breadcrumb="landings / nova" backHref={`/${locale}/portal/landings`}>
        <div className="p-4 sm:p-8 space-y-6">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / landings / nova /</span>
            <div className="f-display font-bold text-xl mt-1">Selecciona una plantilla</div>
            <p className="text-sm text-ink-1 mt-1">Tria una plantilla per començar</p>
          </div>
          <TemplateSelector onSelect={handleSelectTemplate} />
        </div>
      </PortalShell>
    );
  }

  return (
    <PortalShell breadcrumb="landings / nova" backHref={`/${locale}/portal/landings`}>
    <div className="p-4 sm:p-8 max-w-lg space-y-6">
      <div>
        <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / landings / nova /</span>
        <div className="f-display font-bold text-xl mt-1">Configura la landing</div>
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
          <AMGButton variant="ghost" onClick={() => setStep('template')}>
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
        {errorMsg && (
          <p className="f-mono text-caption text-warning mt-2">{errorMsg}</p>
        )}
      </div>
    </div>
    </PortalShell>
  );
}

export default function NewLandingPage() {
  return (
    <Suspense fallback={
      <div className="flex items-center justify-center min-h-[50vh]">
        <span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
      </div>
    }>
      <NewLandingForm />
    </Suspense>
  );
}
