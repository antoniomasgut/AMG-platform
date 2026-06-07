'use client';

import { useQuery } from '@tanstack/react-query';
import {
  listDocuments, type DocumentResponse,
} from '@/services/documents';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGSectionTitle } from '@/components/ui/stat';
import { IconSet } from '@/components/ui/icons';

const STATUS_TONES: Record<string, 'neutral' | 'info' | 'success' | 'warning' | 'danger'> = {
  DRAFT: 'neutral',
  FINALIZED: 'info',
  SENT: 'success',
  PAID: 'success',
  CANCELLED: 'danger',
};

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Esborrany',
  FINALIZED: 'Finalitzat',
  SENT: 'Enviat',
  PAID: 'Pagat',
  CANCELLED: 'Cancel·lat',
};

function fmtDate(d: string) {
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

export default function GeneratedDocumentsPage() {
  const { data: docs, isLoading } = useQuery({
    queryKey: ['generated-documents'],
    queryFn: () => listDocuments(),
  });

  return (
    <PortalShell breadcrumb="admin · documents · generated" backHref="/portal/admin/documents">
      <div className="p-4 sm:p-8 space-y-6">
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / documents / list /</span>
          <div className="f-display font-bold text-xl mt-1">Documents generats</div>
        </div>

        <div className="flex gap-3 border-b border-border-base pb-3">
          <a href="/portal/admin/documents" className="f-mono text-label uppercase text-ink-2 hover:text-ink-0 pb-2">
            Plantilles
          </a>
          <a href="/portal/admin/documents/list" className="f-mono text-label uppercase text-accent-light border-b-2 border-[#FF6B00] pb-2">
            Documents generats
          </a>
        </div>

        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base">
            <AMGSectionTitle eyebrow="Històric" title="Documents generats" />
          </div>

          {isLoading ? (
            <div className="flex justify-center py-12">
              <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
            </div>
          ) : !docs || docs.length === 0 ? (
            <div className="p-8 text-center">
              <IconSet.Receipt size={28} stroke="#64748b" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Cap document generat</div>
              <p className="f-mono text-xs text-ink-2 mb-4">Genera el primer document des d&apos;una plantilla</p>
              <AMGButton size="sm" onClick={() => window.location.href = '/portal/admin/documents'}>
                Veure plantilles
              </AMGButton>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[580px]">
                <thead>
                  <tr className="border-b border-border-base">
                    {['Número', 'Versió', 'Estat', 'Generat', 'Accions'].map((h) => (
                      <th key={h} className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {docs.map((d: DocumentResponse) => (
                    <tr key={d.id} className="border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,255,255,0.02)] transition-colors">
                      <td className="px-4 sm:px-5 py-3">
                        <div className="f-display font-bold text-sm">{d.number}</div>
                      </td>
                      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-2">v{d.templateVersion}</td>
                      <td className="px-4 sm:px-5 py-3">
                        <AMGBadge tone={STATUS_TONES[d.status] || 'neutral'}>{STATUS_LABELS[d.status] || d.status}</AMGBadge>
                      </td>
                      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{fmtDate(d.generatedAt)}</td>
                      <td className="px-4 sm:px-5 py-3">
                        <div className="flex gap-2 items-center">
                          <AMGButton
                            size="sm"
                            variant="secondary"
                            onClick={() => window.location.href = `/portal/admin/documents/view/${d.id}`}
                          >
                            <IconSet.Eye size={14} />
                            Veure
                          </AMGButton>
                          {d.pdfUrl && (
                            <AMGButton size="sm" variant="secondary" onClick={() => window.open(d.pdfUrl!, '_blank')}>
                              <IconSet.Download size={14} />
                              PDF
                            </AMGButton>
                          )}
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
    </PortalShell>
  );
}
