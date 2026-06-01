'use client';

import { useRef, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import {
  listSites,
  requestStaticSite,
  updateStaticSite,
  exportSite,
  type WebSiteResponse,
} from '@/services/hosting';

const STATUS_LABEL: Record<string, { text: string; color: string }> = {
  PENDING_REVIEW: { text: 'Pendent de revisió', color: 'text-yellow-400' },
  APPROVED:       { text: 'Aprovada',           color: 'text-blue-400' },
  DEPLOYING:      { text: 'Desplegant…',        color: 'text-blue-300' },
  ACTIVE:         { text: 'Activa',             color: 'text-green-400' },
  SUSPENDED:      { text: 'Suspesa',            color: 'text-orange-400' },
  REJECTED:       { text: 'Rebutjada',          color: 'text-red-400' },
};

function fmtSize(bytes: number | null) {
  if (!bytes) return '—';
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export default function HostingPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();
  const fileRef = useRef<HTMLInputElement>(null);
  const updateRef = useRef<HTMLInputElement>(null);

  const [domain, setDomain] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [updatingSiteId, setUpdatingSiteId] = useState<string | null>(null);

  const tenantId = user?.tenantId ?? '';

  const { data: sites = [], isLoading } = useQuery({
    queryKey: ['sites', tenantId],
    queryFn: () => listSites(tenantId),
    enabled: !!tenantId,
  });

  const { mutate: doRequest, isPending: requesting } = useMutation({
    mutationFn: () => requestStaticSite(tenantId, selectedFile!, domain),
    onSuccess: () => {
      toast('success', 'Sol·licitud enviada. AMG revisarà el ZIP en breu.');
      setSelectedFile(null);
      setDomain('');
      qc.invalidateQueries({ queryKey: ['sites', tenantId] });
    },
    onError: (e: Error) => toast('error', e.message),
  });

  const { mutate: doUpdate, isPending: updating } = useMutation({
    mutationFn: ({ siteId, file }: { siteId: string; file: File }) =>
      updateStaticSite(tenantId, siteId, file),
    onSuccess: () => {
      toast('success', 'Web actualitzada correctament.');
      setUpdatingSiteId(null);
      qc.invalidateQueries({ queryKey: ['sites', tenantId] });
    },
    onError: (e: Error) => toast('error', e.message),
  });

  const activeSite = sites.find(s => s.status === 'ACTIVE' || s.status === 'PENDING_REVIEW' || s.status === 'APPROVED' || s.status === 'DEPLOYING');
  const canRequest = !activeSite;

  return (
    <PortalShell breadcrumb="Allotjament web">
      <div className="max-w-3xl space-y-8">

        {/* Estat actual */}
        {sites.length > 0 && (
          <section className="border border-border-base rounded p-5 space-y-4">
            <h2 className="f-display font-bold text-ink-0">La teva web</h2>
            {sites.map(site => (
              <div key={site.id} className="flex flex-col sm:flex-row sm:items-center gap-3 justify-between">
                <div className="space-y-0.5">
                  <p className="text-ui text-ink-0">{site.domain ?? '—'}</p>
                  <p className={`text-data f-mono ${STATUS_LABEL[site.status]?.color ?? 'text-ink-3'}`}>
                    {STATUS_LABEL[site.status]?.text ?? site.status}
                  </p>
                  {site.reviewNotes && (
                    <p className="text-caption text-ink-3 italic">Nota AMG: {site.reviewNotes}</p>
                  )}
                  <p className="text-caption text-ink-3">Mida: {fmtSize(site.storageBytes)}</p>
                </div>
                <div className="flex gap-2">
                  {site.status === 'ACTIVE' && (
                    <>
                      <AMGButton
                        variant="secondary"
                        size="sm"
                        onClick={() => {
                          setUpdatingSiteId(site.id);
                          updateRef.current?.click();
                        }}
                      >
                        Actualitzar web
                      </AMGButton>
                      <AMGButton
                        variant="ghost"
                        size="sm"
                        onClick={() => exportSite(tenantId, site.id).catch(e => toast('error', e.message))}
                      >
                        Descarregar ZIP
                      </AMGButton>
                    </>
                  )}
                </div>
              </div>
            ))}
          </section>
        )}

        {/* Input ocult per actualitzar */}
        <input
          ref={updateRef}
          type="file"
          accept=".zip"
          className="hidden"
          onChange={e => {
            const file = e.target.files?.[0];
            if (file && updatingSiteId) {
              doUpdate({ siteId: updatingSiteId, file });
            }
            e.target.value = '';
          }}
        />

        {/* Formulari de nova sol·licitud */}
        {canRequest && (
          <section className="border border-border-base rounded p-5 space-y-6">
            <h2 className="f-display font-bold text-ink-0">Sol·licitar allotjament</h2>

            {/* Instruccions ZIP */}
            <div className="bg-bg-2 border border-border-subtle rounded p-4 space-y-3">
              <p className="f-mono text-caption uppercase tracking-widest text-accent-light">
                Com preparar el teu ZIP
              </p>
              <div className="space-y-2 text-data text-ink-2">
                <p>El fitxer ZIP ha de complir aquests requisits:</p>
                <ul className="space-y-1.5 pl-4">
                  <li className="flex gap-2">
                    <span className="text-green-400 shrink-0">✓</span>
                    <span>Ha de contenir un fitxer <code className="bg-bg-3 px-1 rounded text-accent-light">index.html</code> directament a l'arrel del ZIP (no dins d'una carpeta)</span>
                  </li>
                  <li className="flex gap-2">
                    <span className="text-green-400 shrink-0">✓</span>
                    <span>Pot incloure subcarpetes: <code className="bg-bg-3 px-1 rounded text-ink-2">/css</code>, <code className="bg-bg-3 px-1 rounded text-ink-2">/js</code>, <code className="bg-bg-3 px-1 rounded text-ink-2">/images</code>, <code className="bg-bg-3 px-1 rounded text-ink-2">/assets</code></span>
                  </li>
                  <li className="flex gap-2">
                    <span className="text-green-400 shrink-0">✓</span>
                    <span>Mida màxima: <strong className="text-ink-1">50 MB</strong></span>
                  </li>
                  <li className="flex gap-2">
                    <span className="text-red-400 shrink-0">✗</span>
                    <span>No s'accepten fitxers <code className="bg-bg-3 px-1 rounded text-ink-2">.php</code>, <code className="bg-bg-3 px-1 rounded text-ink-2">.htaccess</code>, scripts de servidor ni executables</span>
                  </li>
                </ul>
              </div>

              {/* Exemple d'estructura */}
              <div className="border border-border-subtle rounded p-3 bg-bg-1 f-mono text-caption text-ink-3 leading-6">
                <p className="text-accent-light mb-1">Estructura correcta:</p>
                <p>📦 web.zip</p>
                <p className="pl-4">📄 <span className="text-green-400">index.html</span></p>
                <p className="pl-4">📁 css/</p>
                <p className="pl-8">📄 style.css</p>
                <p className="pl-4">📁 js/</p>
                <p className="pl-8">📄 main.js</p>
                <p className="pl-4">📁 images/</p>
                <p className="pl-8">🖼 logo.png</p>
                <p className="mt-2 text-red-400">Estructura incorrecta:</p>
                <p>📦 web.zip</p>
                <p className="pl-4">📁 web/  ← carpeta extra</p>
                <p className="pl-8">📄 <span className="text-red-400">index.html</span>  ← no és a l'arrel</p>
              </div>

              <p className="text-caption text-ink-3">
                AMG revisarà el contingut manualment per verificar la seguretat. El procés sol tardar menys de 24 hores.
                Un cop aprovat, la web s'activarà automàticament al domini indicat.
              </p>
            </div>

            {/* Camp domini */}
            <div className="space-y-1.5">
              <label className="f-mono text-caption uppercase tracking-widest text-ink-3">Domini</label>
              <input
                type="text"
                placeholder="exemple.com"
                value={domain}
                onChange={e => setDomain(e.target.value)}
                className="w-full bg-bg-2 border border-border-base rounded px-3 py-2 text-ui text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-accent"
              />
              <p className="text-caption text-ink-3">Escriu el domini on es publicarà la web (sense https://)</p>
            </div>

            {/* Selecció fitxer */}
            <div className="space-y-2">
              <label className="f-mono text-caption uppercase tracking-widest text-ink-3">Fitxer ZIP</label>
              <div
                className="border-2 border-dashed border-border-base rounded p-6 text-center cursor-pointer hover:border-accent transition-colors"
                onClick={() => fileRef.current?.click()}
              >
                {selectedFile ? (
                  <div className="space-y-1">
                    <p className="text-ui text-ink-0">{selectedFile.name}</p>
                    <p className="text-caption text-ink-3">{fmtSize(selectedFile.size)}</p>
                  </div>
                ) : (
                  <div className="space-y-1">
                    <p className="text-ui text-ink-2">Fes clic per seleccionar el ZIP</p>
                    <p className="text-caption text-ink-3">Màxim 50 MB · Només fitxers .zip</p>
                  </div>
                )}
              </div>
              <input
                ref={fileRef}
                type="file"
                accept=".zip"
                className="hidden"
                onChange={e => setSelectedFile(e.target.files?.[0] ?? null)}
              />
            </div>

            <AMGButton
              onClick={() => doRequest()}
              disabled={!selectedFile || !domain.trim() || requesting}
              loading={requesting}
            >
              Enviar sol·licitud
            </AMGButton>
          </section>
        )}

        {isLoading && (
          <p className="text-data text-ink-3">Carregant…</p>
        )}
      </div>
    </PortalShell>
  );
}
