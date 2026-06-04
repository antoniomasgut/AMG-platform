'use client';

import { useParams } from 'next/navigation';
import { useState } from 'react';
import { useAuth } from '@/lib/auth-context';
import { useTenantFeatures } from '@/lib/tenant-features';
import { PortalShell } from '@/components/portal/PortalShell';
import { PhaseGuard } from '@/components/ui/PhaseGuard';
import { AMGButton } from '@/components/ui/button';
import { parseDocument, type ParsedDocumentResponse } from '@/services/analytics';
import { useToast } from '@/lib/toast-context';

export default function DocumentsPage() {
  const params   = useParams<{ locale: string }>();
  const { user } = useAuth();
  const { toast } = useToast();
  const tenantId = (user as any)?.tenantId as string | undefined;
  const locale   = params.locale;
  const features = useTenantFeatures();

  const [file,            setFile]            = useState<File | null>(null);
  const [autoCreate,      setAutoCreate]      = useState(false);
  const [loading,         setLoading]         = useState(false);
  const [result,          setResult]          = useState<ParsedDocumentResponse | null>(null);

  if (!tenantId) {
    return (
      <PortalShell breadcrumb="documents" backHref={`/${locale}/portal`}>
        <div className="p-8 text-sm text-ink-2">No tens un tenant associat.</div>
      </PortalShell>
    );
  }

  const handleUpload = async () => {
    if (!file) return;
    setLoading(true);
    setResult(null);
    try {
      const res = await parseDocument(tenantId, file, autoCreate);
      setResult(res);
      if (res.leadCreated) toast('success', `Lead creat: ${res.extractedName ?? res.leadId}`);
    } catch {
      toast('error', 'Error processant el document');
    } finally {
      setLoading(false);
    }
  };

  return (
    <PortalShell breadcrumb="documents" backHref={`/${locale}/portal`}>
      <PhaseGuard
        allowed={features.canAccessDocuments}
        loading={features.isLoading}
        phaseName="F1 o F3"
        phaseLabel="Captació o Pressupostos"
        description="Importa PDFs i documents per extreure dades de contacte amb IA."
      >
      <div className="p-4 sm:p-8 space-y-6 max-w-2xl">

        <div>
          <h1 className="text-lg font-semibold text-ink-0">Importar Document</h1>
          <p className="text-sm text-ink-2 mt-0.5">
            Puja un PDF o text pla. L'IA n'extraurà nom, email, telèfon i notes.
          </p>
        </div>

        <div className="bg-surface-overlay border border-border-base rounded-md p-5 space-y-4">
          <div>
            <label className="block text-xs text-ink-2 mb-1">Fitxer (PDF o .txt)</label>
            <input
              type="file"
              accept=".pdf,.txt,text/plain,application/pdf"
              className="text-sm text-ink-1 file:mr-3 file:py-1 file:px-3 file:rounded file:border file:border-border-base file:bg-surface-base file:text-ink-1 file:text-xs file:cursor-pointer"
              onChange={e => setFile(e.target.files?.[0] ?? null)}
            />
          </div>

          <label className="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" checked={autoCreate} onChange={e => setAutoCreate(e.target.checked)}
              className="w-4 h-4 accent-orange-500" />
            <span className="text-sm text-ink-1">Crear lead automàticament des de les dades extretes</span>
          </label>

          <AMGButton onClick={handleUpload} disabled={!file || loading} loading={loading}>
            {loading ? 'Processant…' : 'Processar document'}
          </AMGButton>
        </div>

        {result && (
          <div className="space-y-4">
            <div className="bg-surface-overlay border border-border-base rounded-md overflow-hidden">
              <div className="border-b border-border-base px-4 py-2.5">
                <span className="f-mono text-xs font-semibold text-ink-1 uppercase tracking-wider">
                  Dades extretes — {result.fileName}
                </span>
              </div>
              <div className="p-4 space-y-2">
                {[
                  { label: 'Nom',     value: result.extractedName },
                  { label: 'Email',   value: result.extractedEmail },
                  { label: 'Telèfon', value: result.extractedPhone },
                  { label: 'Notes',   value: result.extractedNotes },
                ].map(({ label, value }) => (
                  value && (
                    <div key={label} className="flex gap-3 text-sm">
                      <span className="text-ink-3 w-20 shrink-0">{label}</span>
                      <span className="text-ink-1">{value}</span>
                    </div>
                  )
                ))}
                {result.leadCreated && (
                  <div className="mt-2 text-sm text-green-400">✓ Lead creat (ID: {result.leadId})</div>
                )}
              </div>
            </div>

            {result.rawText && (
              <details className="bg-surface-overlay border border-border-base rounded-md">
                <summary className="px-4 py-2.5 cursor-pointer text-xs f-mono text-ink-3 uppercase tracking-wider">
                  Text extret complet
                </summary>
                <pre className="p-4 text-xs text-ink-2 whitespace-pre-wrap max-h-60 overflow-y-auto">
                  {result.rawText}
                </pre>
              </details>
            )}
          </div>
        )}
      </div>
      </PhaseGuard>
    </PortalShell>
  );
}
