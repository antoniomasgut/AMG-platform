'use client';

import { useRef, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import { useRouter, Link } from '@/i18n/navigation';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import {
  listSites,
  requestStaticSite,
  updateStaticSite,
  exportSite,
  getWidgetConfig,
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

const AMG_SERVER_IP = '65.108.148.62';

function CopyButton({ value }: { value: string }) {
  const [copied, setCopied] = useState(false);
  return (
    <button
      onClick={() => { navigator.clipboard.writeText(value); setCopied(true); setTimeout(() => setCopied(false), 2000); }}
      className="f-mono text-[10px] uppercase px-2 py-0.5 border border-border-base rounded text-ink-2 hover:text-ink-0 hover:border-ink-1 transition-colors shrink-0"
    >
      {copied ? 'Copiat!' : 'Copiar'}
    </button>
  );
}

type StepStatus = 'done' | 'pending' | 'optional';

function Step({ status, title, children }: { status: StepStatus; title: string; children: React.ReactNode }) {
  const dot = status === 'done'
    ? 'bg-green-500'
    : status === 'pending'
    ? 'bg-yellow-400 animate-pulse'
    : 'bg-border-base';
  return (
    <div className="flex gap-3">
      <div className="flex flex-col items-center">
        <div className={`w-3 h-3 rounded-full mt-1 shrink-0 ${dot}`} />
        <div className="w-px flex-1 bg-border-base mt-1" />
      </div>
      <div className="pb-5 flex-1 min-w-0">
        <p className={`text-sm font-semibold mb-1 ${status === 'done' ? 'text-ink-0' : status === 'pending' ? 'text-yellow-400' : 'text-ink-2'}`}>{title}</p>
        <div className="text-xs text-ink-2 space-y-1.5">{children}</div>
      </div>
    </div>
  );
}

function ActivationGuide({ site }: { site: WebSiteResponse }) {
  const router = useRouter();
  const { data: cfg } = useQuery({
    queryKey: ['widget-config', site.id],
    queryFn: () => getWidgetConfig(site.id),
  });

  const isActive = site.status === 'ACTIVE';
  const domain = site.domain ?? '';
  const isSubdomain = domain.split('.').length > 2;

  return (
    <section className="border border-border-base rounded p-5">
      <p className="f-mono text-[10px] uppercase tracking-widest text-accent-light mb-5">Guia de posada en marxa</p>

      <Step status={isActive ? 'done' : 'pending'} title="Web desplegada">
        {isActive
          ? <p>La web és activa i accessible. Per actualitzar el contingut, puja un nou ZIP.</p>
          : <p>En curs — AMG està revisant i desplegant la web. Sol tardar menys de 24 hores.</p>
        }
      </Step>

      <Step status={isActive ? 'pending' : 'optional'} title="Apunta el domini al servidor">
        <p>Al panell del teu registrador de dominis (GoDaddy, Namecheap, 1&1…) afegeix el registre DNS:</p>
        <div className="mt-2 bg-bg-2 border border-border-subtle rounded p-3 space-y-2">
          <div className="grid grid-cols-[80px_1fr_auto] gap-2 items-center f-mono text-[11px]">
            <span className="text-ink-3 uppercase">Tipus</span>
            <span className="text-ink-3 uppercase">Valor</span>
            <span />
          </div>
          {isSubdomain ? (
            <>
              <div className="grid grid-cols-[80px_1fr_auto] gap-2 items-center f-mono text-[11px]">
                <span className="text-accent-light">CNAME</span>
                <span className="text-ink-0 truncate">amgdl.com.</span>
                <CopyButton value="amgdl.com." />
              </div>
              <p className="text-ink-3 text-[10px]">Si el teu proveïdor no admet CNAME, usa un registre A apuntant a {AMG_SERVER_IP}</p>
            </>
          ) : (
            <div className="grid grid-cols-[80px_1fr_auto] gap-2 items-center f-mono text-[11px]">
              <span className="text-accent-light">A</span>
              <span className="text-ink-0">{AMG_SERVER_IP}</span>
              <CopyButton value={AMG_SERVER_IP} />
            </div>
          )}
        </div>
        <p className="mt-1.5 text-ink-3">La propagació DNS pot trigar fins a 24–48 hores. L'SSL s'activa automàticament un cop propagat.</p>
      </Step>

      <Step status="done" title="SSL automàtic (HTTPS)">
        <p>El certificat SSL es genera automàticament via Let&apos;s Encrypt quan el domini apunta al servidor. No cal fer res.</p>
      </Step>

      <Step status={cfg?.chatEnabled ? 'done' : 'optional'} title="Widget de xat IA">
        {cfg?.chatEnabled ? (
          <p>El widget de xat IA ja és actiu i s'injecta automàticament a totes les pàgines de la web.</p>
        ) : (
          <>
            <p>El widget de xat IA s'injectarà automàticament un cop activis i configuris l'agent.</p>
            <button onClick={() => router.push('/portal/agents')} className="text-accent-light underline">
              Configurar l'agent IA →
            </button>
          </>
        )}
      </Step>

      <Step status={cfg?.waNumber ? 'done' : 'optional'} title="Botó de WhatsApp">
        {cfg?.waNumber ? (
          <p>El botó de WhatsApp <span className="text-ink-0 font-medium">{cfg.waNumber}</span> ja apareix a la web.</p>
        ) : (
          <>
            <p>Pots afegir un botó flotant de WhatsApp a la web configurant el número de contacte.</p>
            <button onClick={() => router.push('/portal/agents')} className="text-accent-light underline">
              Configurar canals →
            </button>
          </>
        )}
      </Step>

      <Step status="done" title="Actualitzar la web">
        <p>Per modificar el contingut, prepara un nou ZIP amb els canvis i puja&apos;l amb el botó <strong className="text-ink-1">Actualitzar web</strong>. Els widgets es reinjjectaran automàticament.</p>
      </Step>
    </section>
  );
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
                  <p className="text-ui text-ink-0 font-medium">{site.domain ?? '—'}</p>
                  <p className={`text-data f-mono text-sm ${STATUS_LABEL[site.status]?.color ?? 'text-ink-3'}`}>
                    {STATUS_LABEL[site.status]?.text ?? site.status}
                  </p>
                  {site.reviewNotes && (
                    <p className="text-caption text-ink-3 italic">Nota AMG: {site.reviewNotes}</p>
                  )}
                  <p className="text-caption text-ink-3">Mida: {fmtSize(site.storageBytes)}</p>
                </div>
                {site.status === 'ACTIVE' && (
                  <div className="flex gap-2 flex-wrap shrink-0">
                    <AMGButton variant="secondary" size="sm"
                      onClick={() => { setUpdatingSiteId(site.id); updateRef.current?.click(); }}>
                      Actualitzar web
                    </AMGButton>
                    <AMGButton variant="ghost" size="sm"
                      onClick={() => exportSite(tenantId, site.id).catch(e => toast('error', e.message))}>
                      Descarregar ZIP
                    </AMGButton>
                  </div>
                )}
              </div>
            ))}
          </section>
        )}

        {/* Guia d'activació */}
        {activeSite && activeSite.status !== 'REJECTED' && (
          <ActivationGuide site={activeSite} />
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

        {/* Pro hosting CTA */}
        <section className="border border-accent/20 bg-accent/5 rounded p-5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <p className="f-mono text-label uppercase tracking-widest text-accent-light mb-1">Allotjament Pro — Web amb Docker</p>
            <p className="text-data text-ink-2">
              Tens una aplicació web amb Docker (WordPress, Node.js, Laravel…)?
              Puja un <code className="bg-bg-3 px-1 rounded text-ink-2">docker-compose.yml</code> i AMG la desplegarà en un contenidor dedicat amb SSL automàtic.
            </p>
          </div>
          <Link
            href="/portal/hosting/pro"
            className="f-mono text-xs uppercase text-accent-light border border-accent/40 hover:border-accent hover:bg-accent/10 px-5 h-9 flex items-center shrink-0 transition-colors rounded whitespace-nowrap"
          >
            Import Pro →
          </Link>
        </section>
      </div>
    </PortalShell>
  );
}
