'use client';

import { useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import {
  getDocument, type DocumentResponse,
} from '@/services/documents';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { I } from '@/components/ui/icons';

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Esborrany',
  FINALIZED: 'Finalitzat',
  SENT: 'Enviat',
  PAID: 'Pagat',
  CANCELLED: 'Cancel·lat',
};

export default function ViewDocumentPage() {
  const params = useParams();
  const id = params.id as string;

  const { data: doc, isLoading } = useQuery({
    queryKey: ['generated-document', id],
    queryFn: () => getDocument(id),
    enabled: !!id,
  });

  if (isLoading) {
    return (
      <PortalShell breadcrumb="admin · documents · view" backHref="/portal/admin/documents/list">
        <div className="flex justify-center py-20">
          <span className="w-5 h-5 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
        </div>
      </PortalShell>
    );
  }

  if (!doc) {
    return (
      <PortalShell breadcrumb="admin · documents · view" backHref="/portal/admin/documents/list">
        <div className="p-8 text-center">
          <I.FileText size={28} stroke="#64748b" className="mx-auto mb-3" />
          <div className="f-display font-bold text-sm">Document no trobat</div>
        </div>
      </PortalShell>
    );
  }

  return (
    <PortalShell breadcrumb={`admin · documents · ${doc.number}`} backHref="/portal/admin/documents/list">
      <div className="p-4 sm:p-8 space-y-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / documents / {doc.number} /</span>
            <div className="flex items-center gap-3 mt-1">
              <div className="f-display font-bold text-xl">{doc.number}</div>
              <AMGBadge tone={doc.status === 'DRAFT' ? 'neutral' : doc.status === 'FINALIZED' ? 'info' : doc.status === 'SENT' ? 'success' : doc.status === 'PAID' ? 'success' : 'danger'}>
                {STATUS_LABELS[doc.status] || doc.status}
              </AMGBadge>
            </div>
          </div>
          <div className="flex gap-2">
            {doc.pdfUrl && (
              <AMGButton size="sm" icon={I.Download} onClick={() => window.open(doc.pdfUrl!, '_blank')}>
                Descarregar PDF
              </AMGButton>
            )}
          </div>
        </div>

        <div className="amg-card card-clip p-4 sm:p-6">
          {doc.htmlContent ? (
            <div className="max-w-[800px] mx-auto bg-white text-black rounded shadow-lg overflow-hidden">
              <iframe
                srcDoc={doc.htmlContent}
                className="w-full min-h-[800px]"
                title={doc.number}
                sandbox="allow-same-origin"
              />
            </div>
          ) : (
            <div className="text-center py-12 text-ink-2">
              <I.FileText size={24} stroke="currentColor" className="mx-auto mb-3" />
              <p className="f-mono text-xs">Aquest document no té contingut HTML disponible</p>
            </div>
          )}
        </div>
      </div>
    </PortalShell>
  );
}
