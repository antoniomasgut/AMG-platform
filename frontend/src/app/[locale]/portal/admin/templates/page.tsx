'use client';
import { Link } from '@/i18n/navigation';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@/lib/toast-context';
import {
  listTemplates, deleteTemplate,
  type TemplateSummary,
} from '@/services/templates';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGSectionTitle } from '@/components/ui/stat';
import { IconSet } from '@/components/ui/icons';

function fmtDate(d: string) {
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

function ConfirmDeleteModal({
  template, onClose, onConfirm, loading,
}: {
  template: TemplateSummary;
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

export default function AdminTemplatesPage() {
  const { toast } = useToast();
  const qc = useQueryClient();
  const [deleteTarget, setDeleteTarget] = useState<TemplateSummary | null>(null);

  const { data: templates, isLoading } = useQuery({
    queryKey: ['templates'],
    queryFn: () => listTemplates(),
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ['templates'] });

  const { mutate: doDelete, isPending: deleting } = useMutation({
    mutationFn: (id: string) => deleteTemplate(id),
    onSuccess: () => {
      toast('success', 'Plantilla eliminada');
      setDeleteTarget(null);
      invalidate();
    },
    onError: () => toast('error', 'Error eliminant la plantilla'),
  });

  return (
    <PortalShell breadcrumb="admin · plantilles">
      <div className="p-4 sm:p-8 space-y-6">
        {/* Header */}
        <div className="flex items-start justify-between gap-4">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / plantilles /</span>
            <div className="f-display font-bold text-xl mt-1">Gestió de plantilles</div>
          </div>
          <AMGButton size="sm" icon={IconSet.Plus} onClick={() => window.location.href = '/portal/admin/templates/new'}>
            Nova plantilla
          </AMGButton>
        </div>

        {/* Admin sub-nav */}
        <div className="flex gap-3 border-b border-border-base pb-3">
          <Link href="/portal/admin/users" className="f-mono text-label uppercase text-ink-2 hover:text-ink-0 pb-2">
            Usuaris
            </Link>
          <Link href="/portal/admin/tenants" className="f-mono text-label uppercase text-ink-2 hover:text-ink-0 pb-2">
            Tenants
            </Link>
          <Link href="/portal/admin/templates" className="f-mono text-label uppercase text-accent-light border-b-2 border-[#FF6B00] pb-2">
            Plantilles
            </Link>
        </div>

        {/* List */}
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base">
            <AMGSectionTitle eyebrow="Catàleg" title="Plantilles de landing" />
          </div>

          {isLoading ? (
            <div className="flex justify-center py-12">
              <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
            </div>
          ) : !templates || templates.length === 0 ? (
            <div className="p-8 text-center">
              <IconSet.Box size={28} stroke="#64748b" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Cap plantilla</div>
              <p className="f-mono text-xs text-ink-2 mb-4">Crea la primera plantilla per començar</p>
              <AMGButton size="sm" onClick={() => window.location.href = '/portal/admin/templates/new'}>
                Crear plantilla
              </AMGButton>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[580px]">
                <thead>
                  <tr className="border-b border-border-base">
                    {['Plantilla', 'Slug', 'Seccions', 'Estat', 'Creat', 'Accions'].map((h) => (
                      <th key={h} className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {templates.map((t: TemplateSummary) => (
                    <tr key={t.id} className="border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,255,255,0.02)] transition-colors">
                      <td className="px-4 sm:px-5 py-3">
                        <div className="f-display font-bold text-sm">{t.name}</div>
                        {t.description && <div className="f-mono text-xs text-ink-3 mt-0.5">{t.description}</div>}
                      </td>
                      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-2">{t.slug}</td>
                      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{t.sectionCount}</td>
                      <td className="px-4 sm:px-5 py-3">
                        {t.isActive ? (
                          <AMGBadge tone="success">Activa</AMGBadge>
                        ) : (
                          <AMGBadge tone="neutral">Inactiva</AMGBadge>
                        )}
                      </td>
                      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{fmtDate(t.createdAt)}</td>
                      <td className="px-4 sm:px-5 py-3">
                        <div className="flex gap-2 items-center">
                          <AMGButton
                            size="sm"
                            variant="secondary"
                            onClick={() => window.location.href = `/portal/admin/templates/${t.id}`}
                          >
                            Gestionar
                          </AMGButton>
                          <AMGButton
                            size="sm"
                            variant="ghost"
                            onClick={() => setDeleteTarget(t)}
                          >
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
