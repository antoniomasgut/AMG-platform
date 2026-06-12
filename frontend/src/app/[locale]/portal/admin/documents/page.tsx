'use client';
import { Link, useRouter } from '@/i18n/navigation';

import { useRef, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@/lib/toast-context';
import {
  listTemplates, deleteTemplate, duplicateTemplate,
  exportTemplateToDrive, exportTemplateToDriveAMG, importTemplateFromPdf,
  type TemplateResponse, type DocumentType,
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

function ImportPdfModal({ onClose, onImported, tenantId }: {
  onClose: () => void;
  onImported: () => void;
  tenantId?: string;
}) {
  const { toast } = useToast();
  const fileRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [name, setName] = useState('');
  const [docType, setDocType] = useState<DocumentType>('quote');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) return;
    setLoading(true);
    try {
      await importTemplateFromPdf(file, docType, name || file.name.replace('.pdf', ''), tenantId);
      toast('success', 'Plantilla generada des del PDF');
      onImported();
      onClose();
    } catch {
      toast('error', 'Error processant el PDF');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-md p-6 space-y-4" onClick={e => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Importar plantilla des de PDF</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><IconSet.X size={18} /></button>
        </div>
        <p className="f-mono text-xs text-ink-2">
          Puja un pressupost o factura en PDF. La IA analitzarà l'estructura i generarà una plantilla editable.
        </p>
        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="block f-mono text-xs text-ink-2 mb-1 uppercase tracking-wide">Fitxer PDF</label>
            <input ref={fileRef} type="file" accept=".pdf,application/pdf" className="hidden"
              onChange={e => setFile(e.target.files?.[0] ?? null)} />
            <div
              className="border border-dashed border-border-base rounded p-4 text-center cursor-pointer hover:border-accent transition-colors"
              onClick={() => fileRef.current?.click()}
            >
              {file ? (
                <span className="f-mono text-xs text-ink-1">{file.name}</span>
              ) : (
                <span className="f-mono text-xs text-ink-3">Fes clic per seleccionar un PDF</span>
              )}
            </div>
          </div>
          <div>
            <label className="block f-mono text-xs text-ink-2 mb-1 uppercase tracking-wide">Nom de la plantilla</label>
            <input
              className="w-full bg-surface-base border border-border-base rounded px-3 py-2 text-sm text-ink-0 focus:outline-none focus:border-accent"
              placeholder="Ex: Pressupost pintura 2026"
              value={name} onChange={e => setName(e.target.value)}
            />
          </div>
          <div>
            <label className="block f-mono text-xs text-ink-2 mb-1 uppercase tracking-wide">Tipus de document</label>
            <select
              className="w-full bg-surface-base border border-border-base rounded px-3 py-2 text-sm text-ink-0 focus:outline-none focus:border-accent"
              value={docType} onChange={e => setDocType(e.target.value as DocumentType)}
            >
              {DOCUMENT_TYPES.map(dt => <option key={dt.value} value={dt.value}>{dt.label}</option>)}
            </select>
          </div>
          <div className="flex gap-3 pt-1">
            <AMGButton type="submit" disabled={!file} loading={loading} className="flex-1 justify-center">
              Generar plantilla
            </AMGButton>
            <AMGButton type="button" variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function AdminDocumentsPage() {
  const { toast } = useToast();
  const router = useRouter();
  const qc = useQueryClient();
  const searchParams = useSearchParams();
  const forTenantId = searchParams.get('tenantId') ?? undefined;
  const [deleteTarget, setDeleteTarget] = useState<TemplateResponse | null>(null);
  const [showImportPdf, setShowImportPdf] = useState(false);
  const [exportingId, setExportingId] = useState<string | null>(null);
  const [exportingAMGId, setExportingAMGId] = useState<string | null>(null);

  const { data: templates, isLoading } = useQuery({
    queryKey: ['document-templates', forTenantId],
    queryFn: () => listTemplates(forTenantId),
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ['document-templates', forTenantId] });

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

  const handleExportDrive = async (templateId: string) => {
    setExportingId(templateId);
    try {
      const result = await exportTemplateToDrive(templateId, forTenantId);
      toast('success', 'Plantilla exportada al Drive del tenant');
      if (result.webViewLink) window.open(result.webViewLink, '_blank');
    } catch {
      toast('error', 'Error exportant. Comprova que Google Drive del tenant està connectat.');
    } finally {
      setExportingId(null);
    }
  };

  const handleExportDriveAMG = async (templateId: string) => {
    setExportingAMGId(templateId);
    try {
      const result = await exportTemplateToDriveAMG(templateId, forTenantId);
      toast('success', 'Plantilla exportada al Drive d\'AMG (Tenants/' + (forTenantId ?? '') + ')');
      if (result.webViewLink) window.open(result.webViewLink, '_blank');
    } catch {
      toast('error', 'Error exportant al Drive d\'AMG. Comprova GOOGLE_CALENDAR_SA_JSON.');
    } finally {
      setExportingAMGId(null);
    }
  };

  return (
    <PortalShell breadcrumb="admin · documents">
      <div className="p-4 sm:p-8 space-y-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / documents /</span>
            <div className="f-display font-bold text-xl mt-1">Plantilles de documents</div>
          </div>
          <div className="flex gap-2">
            <AMGButton size="sm" variant="secondary" icon={IconSet.Upload} onClick={() => setShowImportPdf(true)}>
              Importar PDF
            </AMGButton>
            <AMGButton size="sm" icon={IconSet.Plus} onClick={() => router.push(`/portal/admin/documents/new${forTenantId ? `?tenantId=${forTenantId}` : ''}`)}>
              Nova plantilla
            </AMGButton>
          </div>
        </div>

        <div className="flex gap-3 border-b border-border-base pb-3">
          <Link href="/portal/admin/documents" className="f-mono text-label uppercase text-accent-light border-b-2 border-[#FF6B00] pb-2">
            Plantilles
            </Link>
          <Link href="/portal/admin/documents/list" className="f-mono text-label uppercase text-ink-2 hover:text-ink-0 pb-2">
            Documents generats
            </Link>
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
              <AMGButton size="sm" onClick={() => router.push(`/portal/admin/documents/new${forTenantId ? `?tenantId=${forTenantId}` : ''}`)}>
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
                            onClick={() => router.push(`/portal/admin/documents/${t.id}/edit${forTenantId ? `?tenantId=${forTenantId}` : ''}`)}
                          >
                            Editar
                          </AMGButton>
                          <AMGButton
                            size="sm"
                            variant="secondary"
                            onClick={() => router.push(`/portal/admin/documents/generate/${t.id}${forTenantId ? `?tenantId=${forTenantId}` : ''}`)}
                          >
                            Generar
                          </AMGButton>
                          <AMGButton size="sm" variant="ghost" onClick={() => doDuplicate(t.id)}>
                            <IconSet.Copy size={14} />
                          </AMGButton>
                          <AMGButton
                            size="sm" variant="ghost"
                            loading={exportingId === t.id}
                            onClick={() => handleExportDrive(t.id)}
                          >
                            <IconSet.Link size={14} />
                          </AMGButton>
                          <AMGButton
                            size="sm" variant="ghost"
                            loading={exportingAMGId === t.id}
                            onClick={() => handleExportDriveAMG(t.id)}
                          >
                            <IconSet.Database size={14} />
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
      {showImportPdf && (
        <ImportPdfModal
          tenantId={forTenantId}
          onClose={() => setShowImportPdf(false)}
          onImported={invalidate}
        />
      )}
    </PortalShell>
  );
}
