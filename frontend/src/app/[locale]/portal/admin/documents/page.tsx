'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@/lib/toast-context';
import {
  listTemplates, deleteTemplate, duplicateTemplate,
  type TemplateResponse,
  DOCUMENT_TYPES,
} from '@/services/documents';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGSectionTitle } from '@/components/ui/stat';
import { IconSet } from '@/components/ui/icons';

function fmtDate(d: string) {
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

function docTypeLabel(type: string) {
  return DOCUMENT_TYPES.find(t => t.value === type)?.label ?? type;
}

function ConfirmDeleteModal({
  template, onClose, onConfirm, loading,
}: {
  template: TemplateResponse;
  onClose: () => void;
  onConfirm: () => void;
  loading: boolean;
}) {
  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-sm p-6 space-y-4" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Eliminar plantilla</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><IconSet.X size={18} /></button>
        </div>
        <p className="text-sm text-ink-1">
          Estàs segur d'eliminar la plantilla <strong>{template.name}</strong>? Aquesta acció no es pot desfer.
        </p>
        <div className="flex gap-3">
          <AMGButton onClick={onConfirm} disabled={loading} className="flex-1 justify-center">
            {loading ? 'Eliminant...' : 'Eliminar'}
          </AMGButton>
          <AMGButton variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
        </div>
      </div>
    </div>
  );
}

export default function AdminDocumentsPage() {
  const { toast } = useToast();
  const qc = useQueryClient();
  const [deleteTarget, setDeleteTarget] = useState<TemplateResponse | null>(null);

  const { data: templates, isLoading } = useQuery({
    queryKey: ['document-templates'],
    queryFn: () => listTemplates(),
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ['document-templates'] });

  const { mutate: doDelete, isPending: deleting } = useMutation({
    mutationFn: (id: string) => deleteTemplate(id),
    onSuccess: () => {
      toast('success', 'Plantilla eliminada');
      setDeleteTarget(null);
      invalidate();
    },
    onError: () => toast('error', 'Error eliminant la plantilla'),
  });

  const { mutate: doDuplicate } = useMutation({
    mutationFn: (id: string) => duplicateTemplate(id),
    onSuccess: () => {
      toast('success', 'Plantilla duplicada');
      invalidate();
    },
    onError: () => toast('error', 'Error duplicant la plantilla'),
  });

  return (
    <PortalShell breadcrumb="admin · documents">
      <div className="p-4 sm:p-8 space-y-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / documents /</span>
            <div className="f-display font-bold text-xl mt-1">Plantilles de documents</div>
          </div>
          <AMGButton size="sm" icon={IconSet.Plus} onClick={() => window.location.href = '/portal/admin/documents/new'}>
            Nova plantilla
          </AMGButton>
        </div>

        <div className="flex gap-3 border-b border-border-base pb-3">
          <a href="/portal/admin/documents" className="f-mono text-label uppercase text-accent-light border-b-2 border-[#FF6B00] pb-2">
            Plantilles
          </a>
          <a href="/portal/admin/documents/list" className="f-mono text-label uppercase text-ink-2 hover:text-ink-0 pb-2">
            Documents generats
          </a>
        </div>

        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base">
            <AMGSectionTitle eyebrow="Documents" title="Totes les plantilles" />
          </div>

          {isLoading ? (
            <div className="flex justify-center py-12">
              <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
            </div>
          ) : !templates || templates.length === 0 ? (
            <div className="p-8 text-center">
              <IconSet.FileText size={28} stroke="#64748b" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Cap plantilla</div>
              <p className="f-mono text-xs text-ink-2 mb-4">Crea la primera plantilla de document per començar</p>
              <AMGButton size="sm" onClick={() => window.location.href = '/portal/admin/documents/new'}>
                Crear plantilla
              </AMGButton>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[580px]">
                <thead>
                  <tr className="border-b border-border-base">
                    {['Plantilla', 'Tipus', 'Versió', 'Estat', 'Creat', 'Accions'].map((h) => (
                      <th key={h} className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {templates.map((t: TemplateResponse) => (
                    <tr key={t.id} className="border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,255,255,0.02)] transition-colors">
                      <td className="px-4 sm:px-5 py-3">
                        <div className="f-display font-bold text-sm">{t.name}</div>
                      </td>
                      <td className="px-4 sm:px-5 py-3">
                        <AMGBadge tone="info">{docTypeLabel(t.documentType)}</AMGBadge>
                      </td>
                      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-2">v{t.version}</td>
                      <td className="px-4 sm:px-5 py-3">
                        {t.active ? (
                          <AMGBadge tone="success">Activa</AMGBadge>
                        ) : (
                          <AMGBadge tone="neutral">Inactiva</AMGBadge>
                        )}
                      </td>
                      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{fmtDate(t.createdAt)}</td>
                      <td className="px-4 sm:px-5 py-3">
                        <div className="flex gap-2 items-center flex-wrap">
                          <AMGButton
                            size="sm"
                            variant="secondary"
                            onClick={() => window.location.href = `/portal/admin/documents/${t.id}/edit`}
                          >
                            Editar
                          </AMGButton>
                          <AMGButton
                            size="sm"
                            variant="secondary"
                            onClick={() => window.location.href = `/portal/admin/documents/generate/${t.id}`}
                          >
                            Generar
                          </AMGButton>
                          <AMGButton size="sm" variant="ghost" onClick={() => doDuplicate(t.id)}>
                            <IconSet.Copy size={14} />
                          </AMGButton>
                          <AMGButton size="sm" variant="ghost" onClick={() => setDeleteTarget(t)}>
                            <IconSet.Trash size={14} />
                          </AMGButton>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {deleteTarget && (
        <ConfirmDeleteModal
          template={deleteTarget}
          onClose={() => setDeleteTarget(null)}
          onConfirm={() => doDelete(deleteTarget.id)}
          loading={deleting}
        />
      )}
    </PortalShell>
  );
}
