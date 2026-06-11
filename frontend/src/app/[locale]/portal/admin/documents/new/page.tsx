'use client';

import { useState } from 'react';
import { useRouter } from '@/i18n/navigation';
import { useSearchParams } from 'next/navigation';
import { useToast } from '@/lib/toast-context';
import {
  createTemplate, defaultLayout, amgQuoteLayout,
  type DocumentType,
  DOCUMENT_TYPES,
} from '@/services/documents';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGInput } from '@/components/ui/input';
import { IconSet } from '@/components/ui/icons';

export default function NewDocumentTemplatePage() {
  const router = useRouter();
  const { toast } = useToast();
  const searchParams = useSearchParams();
  const forTenantId = searchParams.get('tenantId') ?? undefined;
  const [name, setName] = useState('');
  const [docType, setDocType] = useState<DocumentType>('quote');
  const [useAmgPreset, setUseAmgPreset] = useState(true);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const validate = () => {
    const e: Record<string, string> = {};
    if (!name.trim()) e.name = 'El nom és obligatori';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;
    setLoading(true);
    try {
      const layout = docType === 'quote' && useAmgPreset ? amgQuoteLayout() : defaultLayout();
      const result = await createTemplate({
        name: name.trim(),
        documentType: docType,
        layout: JSON.stringify(layout),
        dataBindings: '{}',
        styles: '{}',
      }, forTenantId);
      toast('success', 'Plantilla creada');
      router.push(`/portal/admin/documents/${result.id}/edit`);
    } catch (err: any) {
      toast('error', err?.body?.message || 'Error creant la plantilla');
    } finally {
      setLoading(false);
    }
  };

  return (
    <PortalShell breadcrumb="admin · documents · new" backHref="/portal/admin/documents">
      <div className="p-4 sm:p-8 space-y-6 max-w-2xl">
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / documents / new /</span>
          <div className="f-display font-bold text-xl mt-1">Nova plantilla de document</div>
        </div>

        <form onSubmit={handleSubmit} className="amg-card card-clip p-6 space-y-5">
          <AMGInput
            label="Nom de la plantilla"
            placeholder="Ex: Pressupost estàndard"
            value={name}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setName(e.target.value)}
            error={errors.name}
          />

          <div>
            <label className="f-mono text-label uppercase text-ink-2 mb-2 block">Tipus de document</label>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {DOCUMENT_TYPES.map(({ value, label }) => (
                <button
                  key={value}
                  type="button"
                  onClick={() => setDocType(value)}
                  className={`px-3 py-2 rounded text-xs f-mono uppercase tracking-wider border transition-colors ${
                    docType === value
                      ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.1)] text-[#FF6B00]'
                      : 'border-border-base text-ink-2 hover:border-ink-1'
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>
          </div>

          {docType === 'quote' && (
            <div
              className="flex items-center gap-3 p-3 rounded border border-[rgba(255,107,0,0.3)] bg-[rgba(255,107,0,0.05)] cursor-pointer"
              onClick={() => setUseAmgPreset(!useAmgPreset)}
            >
              <div className={`w-8 h-5 rounded-full transition-colors shrink-0 ${useAmgPreset ? 'bg-[#FF6B00]' : 'bg-ink-3'}`}>
                <div className={`w-3.5 h-3.5 bg-white rounded-full mt-0.5 transition-transform ${useAmgPreset ? 'translate-x-4' : 'translate-x-0.5'}`} />
              </div>
              <div>
                <div className="f-mono text-xs text-accent-light font-semibold">Plantilla AMG Pressupost</div>
                <div className="text-[11px] text-ink-2 mt-0.5">Capçalera empresa + client + taula serveis + totals IVA + condicions pagament + signatures</div>
              </div>
            </div>
          )}

          <div className="flex gap-3 pt-4">
            <AMGButton type="submit" disabled={loading} icon={IconSet.Plus}>
              {loading ? 'Creant...' : 'Crear plantilla'}
            </AMGButton>
            <AMGButton type="button" variant="outline" onClick={() => router.push('/portal/admin/documents')}>
              Cancel·lar
            </AMGButton>
          </div>
        </form>
      </div>
    </PortalShell>
  );
}
