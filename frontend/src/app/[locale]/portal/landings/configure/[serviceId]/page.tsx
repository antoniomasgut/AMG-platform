'use client';

import { useState } from 'react';
import { useParams, useRouter, useSearchParams } from 'next/navigation';
import { PortalShell } from '@/components/portal/PortalShell';
import { useQuery, useMutation } from '@tanstack/react-query';
import { useToast } from '@/lib/toast-context';
import { listTemplates, getTemplate } from '@/services/templates';
import { createLandingFromTemplate } from '@/services/factory';
import { apiFetch } from '@/services/api';
import { SectionContentFillerList } from '@/components/factory/SectionContentFiller';
import { AMGButton } from '@/components/ui/button';
import { I } from '@/components/ui/icons';

type Step = 'select-template' | 'fill-content' | 'confirm';

function ConfigureLandingForm() {
  const rawParams = useParams<{ serviceId: string; locale: string }>();
  const serviceId = rawParams.serviceId;
  const locale = rawParams.locale as string;
  const searchParams = useSearchParams();
  const router = useRouter();
  const { toast } = useToast();
  const tenantId = searchParams.get('tenantId');

  const [step, setStep] = useState<Step>('select-template');
  const [selectedTemplateId, setSelectedTemplateId] = useState<string | null>(null);
  const [title, setTitle] = useState('');
  const [slug, setSlug] = useState('');
  const [filledSections, setFilledSections] = useState<Record<string, Record<string, unknown>>>({});

  const { data: templates, isLoading: loadingTemplates } = useQuery({
    queryKey: ['templates'],
    queryFn: () => listTemplates(),
  });

  const { data: templateDetail, isLoading: loadingDetail } = useQuery({
    queryKey: ['template', selectedTemplateId],
    queryFn: () => getTemplate(selectedTemplateId!),
    enabled: !!selectedTemplateId,
  });

  const { mutate: doCreate, isPending: creating } = useMutation({
    mutationFn: async () => {
      if (!tenantId) throw new Error('Falta tenantId');
      if (!selectedTemplateId) throw new Error('Falta plantilla');

      const landing = await createLandingFromTemplate(tenantId, {
        title,
        slug,
        serviceId,
        templateId: selectedTemplateId,
        filledSections,
      });

      // Mark service as VERIFIED after landing creation
      await apiFetch<void>(`/vault/tenants/${tenantId}/services/${serviceId}/status`, {
        method: 'PUT',
        body: JSON.stringify({ status: 'VERIFIED' }),
      });

      return landing;
    },
    onSuccess: (landing) => {
      toast('success', 'Landing creada i servei completat');
      router.push(`/portal/landings/${landing.id}/edit`);
    },
    onError: (err: Error) => {
      toast('error', err.message || 'Error en crear la landing');
    },
  });

  const handleSelectTemplate = (templateId: string) => {
    setSelectedTemplateId(templateId);
    setStep('fill-content');
  };

  const handleSectionChange = (blockType: string, values: Record<string, unknown>) => {
    setFilledSections((prev) => ({ ...prev, [blockType]: values }));
  };

  const canCreate = title && slug && selectedTemplateId && templateDetail;

  if (!tenantId) {
    return (
      <PortalShell breadcrumb="landings / configurar" backHref={`/${locale}/portal/landings`}>
        <div className="p-4 sm:p-8 text-center py-12">
          <I.AlertCircle size={28} stroke="#ff6666" className="mx-auto mb-3" />
          <div className="f-display font-bold text-sm mb-1">Falta tenantId</div>
          <p className="f-mono text-xs text-ink-2">Aquesta pàgina requereix un tenantId com a paràmetre</p>
        </div>
      </PortalShell>
    );
  }

  return (
    <PortalShell breadcrumb="landings / configurar" backHref={`/${locale}/portal/landings`}>
    <div className="p-4 sm:p-8 space-y-6">
      {/* Header */}
      <div>
        <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / landings / configurar /</span>
        <div className="f-display font-bold text-xl mt-1">Configuració de landing</div>
        <p className="text-sm text-ink-1 mt-1">
          {step === 'select-template' && 'Selecciona una plantilla per a la landing'}
          {step === 'fill-content' && 'Omple els continguts de cada secció'}
        </p>
      </div>

      {/* Step: select template */}
      {step === 'select-template' && (
        <>
          {loadingTemplates ? (
            <div className="flex justify-center py-12">
              <span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
            </div>
          ) : !templates || templates.length === 0 ? (
            <div className="amg-card card-clip p-8 text-center">
              <I.Box size={28} stroke="#64748b" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Cap plantilla disponible</div>
              <p className="f-mono text-xs text-ink-2">Crea una plantilla des del panell d&apos;admin</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {templates.map((tpl) => (
                <button
                  key={tpl.id}
                  onClick={() => handleSelectTemplate(tpl.id)}
                  className="amg-card card-clip p-5 text-left hover:ring-1 hover:ring-[#FF6B00] transition group"
                >
                  <div className="f-display font-bold text-sm mb-1 text-ink-0 group-hover:text-accent-light transition">
                    {tpl.name}
                  </div>
                  <div className="f-mono text-caption text-ink-3 mb-3">{tpl.description}</div>
                  <div className="f-mono text-xs text-ink-3">{tpl.sectionCount} seccions</div>
                </button>
              ))}
            </div>
          )}
        </>
      )}

      {/* Step: fill content */}
      {step === 'fill-content' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 space-y-4">
            {loadingDetail ? (
              <div className="flex justify-center py-12">
                <span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
              </div>
            ) : templateDetail ? (
              <SectionContentFillerList
                sections={templateDetail.sections}
                filledSections={filledSections}
                onSectionChange={handleSectionChange}
              />
            ) : (
              <div className="amg-card card-clip p-8 text-center">
                <I.AlertCircle size={28} stroke="#ff6666" className="mx-auto mb-3" />
                <div className="f-display font-bold text-sm mb-1">Error carregant la plantilla</div>
              </div>
            )}
          </div>

          {/* Sidebar: landing info */}
          <div className="space-y-4">
            <div className="amg-card card-clip p-5 space-y-4">
              <div className="f-display font-bold text-sm">Informació de la landing</div>

              <div>
                <label className="f-mono text-label uppercase text-ink-2 block mb-1 text-xs">Títol</label>
                <input
                  type="text"
                  required
                  placeholder="Títol de la landing"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  className="w-full bg-[#0d0d1a] border border-border-base px-3 h-9 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]"
                />
              </div>
              <div>
                <label className="f-mono text-label uppercase text-ink-2 block mb-1 text-xs">Slug</label>
                <input
                  type="text"
                  required
                  placeholder="nom-de-la-landing"
                  value={slug}
                  onChange={(e) => setSlug(e.target.value)}
                  className="w-full bg-[#0d0d1a] border border-border-base px-3 h-9 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]"
                />
              </div>

              {/* Template info */}
              {templateDetail && (
                <div className="border-t border-border-base pt-3">
                  <div className="f-mono text-label uppercase text-ink-2 text-xs mb-2">Plantilla seleccionada</div>
                  <div className="flex items-center gap-2">
                    <span className="f-display font-bold text-sm">{templateDetail.name}</span>
                    <span className="f-mono text-[10px] text-ink-3">{templateDetail.sections.length} seccions</span>
                  </div>
                </div>
              )}

              <div className="flex gap-2 pt-2">
                <AMGButton
                  onClick={() => doCreate()}
                  disabled={!canCreate || creating}
                  loading={creating}
                  className="flex-1 justify-center"
                >
                  Crear landing
                </AMGButton>
                <AMGButton variant="ghost" onClick={() => setStep('select-template')}>
                  Canviar plantilla
                </AMGButton>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
    </PortalShell>
  );
}

export default function ConfigureLandingPage() {
  return <ConfigureLandingForm />;
}
