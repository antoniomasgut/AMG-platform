'use client';

import { useParams, useRouter } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@/lib/toast-context';
import {
  getTemplate, updateTemplate, addTemplateSection,
  updateTemplateSection, removeTemplateSection,
} from '@/services/templates';
import { TemplateForm } from '@/components/admin/TemplateForm';
import { TemplateSectionEditor } from '@/components/admin/TemplateSectionEditor';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { I } from '@/components/ui/icons';
import type { CreateTemplateRequest, TemplateSectionRequest } from '@/services/templates';

export default function EditTemplatePage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const { toast } = useToast();
  const qc = useQueryClient();

  const { data: template, isLoading, error } = useQuery({
    queryKey: ['template', id],
    queryFn: () => getTemplate(id),
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ['template', id] });

  const { mutate: doUpdate, isPending: updating } = useMutation({
    mutationFn: (data: CreateTemplateRequest) => updateTemplate(id, data),
    onSuccess: () => {
      toast('success', 'Plantilla actualitzada');
      invalidate();
    },
    onError: () => toast('error', 'Error actualitzant la plantilla'),
  });

  const { mutate: doAddSection, isPending: addingSection } = useMutation({
    mutationFn: (data: TemplateSectionRequest) => addTemplateSection(id, data),
    onSuccess: () => {
      toast('success', 'Secció afegida');
      invalidate();
    },
    onError: () => toast('error', 'Error afegint la secció'),
  });

  const { mutate: doUpdateSection, isPending: updatingSection } = useMutation({
    mutationFn: ({ sectionId, data }: { sectionId: string; data: TemplateSectionRequest }) =>
      updateTemplateSection(id, sectionId, data),
    onSuccess: () => {
      toast('success', 'Secció actualitzada');
      invalidate();
    },
    onError: () => toast('error', 'Error actualitzant la secció'),
  });

  const { mutate: doRemoveSection, isPending: removingSection } = useMutation({
    mutationFn: (sectionId: string) => removeTemplateSection(id, sectionId),
    onSuccess: () => {
      toast('success', 'Secció eliminada');
      invalidate();
    },
    onError: () => toast('error', 'Error eliminant la secció'),
  });

  if (isLoading) {
    return (
      <PortalShell breadcrumb="admin · plantilles · carregant">
        <div className="p-4 sm:p-8 space-y-6">
          <div className="flex justify-center py-12">
            <span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          </div>
        </div>
      </PortalShell>
    );
  }

  if (error || !template) {
    return (
      <PortalShell breadcrumb="admin · plantilles · error">
        <div className="p-4 sm:p-8 text-center py-12">
          <I.AlertCircle size={28} stroke="#ff6666" className="mx-auto mb-3" />
          <div className="f-display font-bold text-sm mb-1">Error carregant la plantilla</div>
          <p className="f-mono text-xs text-ink-2 mb-4">No s'ha pogut carregar la informació</p>
          <AMGButton size="sm" onClick={() => router.push('/portal/admin/templates')}>
            Tornar al llistat
          </AMGButton>
        </div>
      </PortalShell>
    );
  }

  return (
    <PortalShell breadcrumb={`admin · plantilles · ${template.name}`}>
      <div className="p-4 sm:p-8 space-y-6">
        {/* Header */}
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / plantilles /</span>
          <div className="flex items-center gap-3 mt-1">
            <div className="f-display font-bold text-xl">{template.name}</div>
            {template.isActive ? (
              <AMGBadge tone="success">Activa</AMGBadge>
            ) : (
              <AMGBadge tone="neutral">Inactiva</AMGBadge>
            )}
          </div>
          {template.description && (
            <p className="text-sm text-ink-2 mt-1">{template.description}</p>
          )}
          <div className="f-mono text-xs text-ink-3 mt-1">/{template.slug} · {template.sections.length} seccions</div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Info bàsica */}
          <div className="amg-card card-clip p-5 space-y-4">
            <div className="f-display font-bold text-sm">Informació bàsica</div>
            <TemplateForm
              initial={{ name: template.name, slug: template.slug, description: template.description }}
              onSave={(data) => doUpdate(data)}
              onCancel={() => router.push('/portal/admin/templates')}
              loading={updating}
            />
          </div>

          {/* Seccions */}
          <div className="amg-card card-clip p-5 space-y-4">
            <TemplateSectionEditor
              sections={template.sections}
              onAdd={(data) => doAddSection(data)}
              onUpdate={(sectionId, data) => doUpdateSection({ sectionId, data })}
              onRemove={(sectionId) => doRemoveSection(sectionId)}
              loading={addingSection || updatingSection || removingSection}
            />
          </div>
        </div>
      </div>
    </PortalShell>
  );
}
