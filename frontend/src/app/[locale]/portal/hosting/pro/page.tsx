'use client';

import { useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { listSites, requestContainerSite, exportSite, type WebSiteResponse } from '@/services/hosting';

const STATUS_LABEL: Record<string, { text: string; color: string }> = {
  PENDING_REVIEW: { text: 'Pendent de revisió AMG (fins 48h)', color: 'text-yellow-400' },
  APPROVED:       { text: 'Aprovada — desplegament manual AMG', color: 'text-blue-400' },
  DEPLOYING:      { text: 'Desplegant…',                        color: 'text-blue-300' },
  ACTIVE:         { text: 'Activa',                             color: 'text-green-400' },
  SUSPENDED:      { text: 'Suspesa',                            color: 'text-orange-400' },
  REJECTED:       { text: 'Rebutjada',                          color: 'text-red-400' },
};

export default function HostingProPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();
  const composeRef = useRef<HTMLInputElement>(null);
  const envRef = useRef<HTMLInputElement>(null);

  const [domain, setDomain] = useState('');
  const [description, setDescription] = useState('');
  const [composeFile, setComposeFile] = useState<File | null>(null);
  const [envFile, setEnvFile] = useState<File | null>(null);

  const tenantId = user?.tenantId ?? '';

  const { data: allSites = [] } = useQuery({
    queryKey: ['sites', tenantId],
    queryFn: () => listSites(tenantId),
    enabled: !!tenantId,
  });

  const proSites = allSites.filter(s => s.type === 'CONTAINER');
  const canRequest = !proSites.some(s =>
    ['PENDING_REVIEW', 'APPROVED', 'DEPLOYING', 'ACTIVE'].includes(s.status)
  );

  const { mutate: doRequest, isPending: requesting } = useMutation({
    mutationFn: () => requestContainerSite(tenantId, composeFile!, envFile ?? undefined, domain, description),
    onSuccess: () => {
      toast('success', 'Sol·licitud enviada. AMG la revisarà en un termini de 48 hores.');
      setComposeFile(null);
      setEnvFile(null);
      setDomain('');
      setDescription('');
      qc.invalidateQueries({ queryKey: ['sites', tenantId] });
    },
    onError: (e: Error) => toast('error', e.message),
  });

  return (
    <PortalShell breadcrumb="Allotjament Pro" backHref="../hosting">
      <div className="max-w-3xl space-y-8">

        {/* Estat actual */}
        {proSites.length > 0 && (
          <section className="border border-border-base rounded p-5 space-y-4">
            <h2 className="f-display font-bold text-ink-0">La teva web Pro</h2>
            {proSites.map((site: WebSiteResponse) => (
              <div key={site.id} className="space-y-1">
                <p className="text-ui text-ink-0">{site.domain ?? '—'}</p>
                <p className={`text-data f-mono ${STATUS_LABEL[site.status]?.color ?? 'text-ink-3'}`}>
                  {STATUS_LABEL[site.status]?.text ?? site.status}
                </p>
                {site.reviewNotes && (
                  <p className="text-caption text-ink-3 italic">Nota AMG: {site.reviewNotes}</p>
                )}
                {site.clientNotes && (
                  <p className="text-caption text-ink-3">Descripció: {site.clientNotes}</p>
                )}
              </div>
            ))}
          </section>
        )}

        {/* Explicació del servei */}
        <section className="border border-border-base rounded p-5 space-y-4">
          <div className="flex items-start gap-3">
            <span className="bg-accent/10 text-accent-light f-mono text-caption px-2 py-0.5 rounded shrink-0">F5</span>
            <div>
              <h2 className="f-display font-bold text-ink-0 mb-1">Import Pro — Web amb contenidors</h2>
              <p className="text-data text-ink-2">
                Per a webs complexes amb backend, base de dades o múltiples serveis (WordPress, Laravel, Node.js + BD, etc.)
                empaquetades amb Docker Compose. AMG les revisa, configura i desplega al servidor.
              </p>
            </div>
          </div>
        </section>

        {/* Com preparar el docker-compose */}
        <section className="border border-border-base rounded p-5 space-y-5">
          <h2 className="f-display font-bold text-ink-0">Com preparar el docker-compose.yml</h2>

          {/* Estructura bàsica */}
          <div className="space-y-2">
            <p className="f-mono text-caption uppercase tracking-widest text-accent-light">Estructura mínima</p>
            <div className="bg-bg-1 border border-border-subtle rounded p-4 f-mono text-caption text-ink-2 leading-6 overflow-x-auto">
              <pre>{`services:
  web:
    image: nginx:alpine          # imatge de Docker Hub
    restart: unless-stopped
    networks:
      - app_net
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.maweb.rule=Host(\`exemple.com\`)"
      - "traefik.http.routers.maweb.tls.certresolver=letsencrypt"
      - "traefik.http.services.maweb.loadbalancer.server.port=80"
    mem_limit: 256m              # límit de RAM obligatori
    cpus: "0.5"                  # límit de CPU obligatori

networks:
  app_net:`}</pre>
            </div>
          </div>

          {/* Exemple amb BD */}
          <div className="space-y-2">
            <p className="f-mono text-caption uppercase tracking-widest text-accent-light">Exemple amb base de dades</p>
            <div className="bg-bg-1 border border-border-subtle rounded p-4 f-mono text-caption text-ink-2 leading-6 overflow-x-auto">
              <pre>{`services:
  app:
    image: wordpress:6.5
    restart: unless-stopped
    environment:
      WORDPRESS_DB_HOST: db
      WORDPRESS_DB_NAME: \${DB_NAME}
      WORDPRESS_DB_USER: \${DB_USER}
      WORDPRESS_DB_PASSWORD: \${DB_PASSWORD}
    volumes:
      - wp_uploads:/var/www/html/wp-content/uploads
    networks:
      - app_net
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.maweb.rule=Host(\`exemple.com\`)"
      - "traefik.http.routers.maweb.tls.certresolver=letsencrypt"
      - "traefik.http.services.maweb.loadbalancer.server.port=80"
    mem_limit: 256m
    cpus: "0.5"

  db:
    image: mysql:8
    restart: unless-stopped
    environment:
      MYSQL_DATABASE: \${DB_NAME}
      MYSQL_USER: \${DB_USER}
      MYSQL_PASSWORD: \${DB_PASSWORD}
      MYSQL_ROOT_PASSWORD: \${DB_ROOT_PASSWORD}
    volumes:
      - db_data:/var/lib/mysql
    networks:
      - app_net
    mem_limit: 256m
    cpus: "0.5"

volumes:
  wp_uploads:
  db_data:

networks:
  app_net:`}</pre>
            </div>
            <p className="text-caption text-ink-3">
              Les variables d&apos;entorn (<code className="bg-bg-3 px-1 rounded">$&#123;DB_NAME&#125;</code>) han d&apos;estar definides al fitxer <code className="bg-bg-3 px-1 rounded">.env.example</code> que pujaràs juntament amb el compose.
            </p>
          </div>

          {/* Fitxer .env.example */}
          <div className="space-y-2">
            <p className="f-mono text-caption uppercase tracking-widest text-accent-light">Fitxer .env.example</p>
            <div className="bg-bg-1 border border-border-subtle rounded p-4 f-mono text-caption text-ink-2 leading-6">
              <pre>{`# Variables d'entorn necessàries (sense valors reals)
DB_NAME=nom_base_de_dades
DB_USER=usuari_bd
DB_PASSWORD=
DB_ROOT_PASSWORD=`}</pre>
            </div>
            <p className="text-caption text-ink-3">
              Mai posis les contrasenyes reals aquí. AMG les configurarà de forma segura al servidor un cop aprovada la sol·licitud.
            </p>
          </div>
        </section>

        {/* Requisits obligatoris del compose */}
        <section className="border border-border-base rounded p-5 space-y-5">
          <h2 className="f-display font-bold text-ink-0">Requisits obligatoris del docker-compose.yml</h2>

          {/* Obligatoris ✓ */}
          <div className="space-y-3">
            <p className="f-mono text-caption uppercase tracking-widest text-green-400">Ha de complir</p>
            <ul className="space-y-3">
              {[
                {
                  ok: true,
                  title: 'Límits de RAM i CPU en cada servei',
                  detail: 'Cada contenidor ha de declarar mem_limit i cpus. Per defecte AMG assigna 512 MB RAM i 0.5 CPU per tenant (màxim 2 GB / 2 CPU sota petició).',
                  code: 'mem_limit: 256m\ncpus: "0.5"',
                },
                {
                  ok: true,
                  title: 'Enrutament via labels de Traefik (sense ports exposats)',
                  detail: 'El contenidor web no ha d\'exposar ports (ports:). Tot el tràfic entra per Traefik. Cal afegir les labels correctes al servei frontend.',
                  code: 'labels:\n  - "traefik.enable=true"\n  - "traefik.http.routers.NOM.rule=Host(`domini.com`)"\n  - "traefik.http.routers.NOM.tls.certresolver=letsencrypt"\n  - "traefik.http.services.NOM.loadbalancer.server.port=PORT"',
                },
                {
                  ok: true,
                  title: 'Imatges de fonts conegudes',
                  detail: 'Només s\'accepten imatges de Docker Hub oficial (nginx, mysql, wordpress, node, python…) o GitHub Container Registry (ghcr.io). No s\'accepten registres privats desconeguts.',
                  code: 'image: wordpress:6.5      ✓\nimage: node:20-alpine     ✓\nimage: ghcr.io/user/app   ✓',
                },
                {
                  ok: true,
                  title: 'Volums nomenats (sense paths absoluts del host)',
                  detail: 'Els volums han d\'usar noms (volumes:), no rutes absolutes del servidor. AMG gestiona la persistència.',
                  code: 'volumes:\n  - db_data:/var/lib/mysql   ✓\n\n# Al final del compose:\nvolumes:\n  db_data:',
                },
                {
                  ok: true,
                  title: 'Reinici automàtic definit',
                  detail: 'Tots els serveis han d\'incloure restart: unless-stopped per garantir la disponibilitat.',
                  code: 'restart: unless-stopped',
                },
                {
                  ok: true,
                  title: 'Variables sensibles via .env.example',
                  detail: 'Les contrasenyes i claus API han d\'estar parametritzades amb ${VARIABLE}. Puja un .env.example amb els noms de les variables (sense valors reals). AMG les configurarà de forma segura.',
                  code: 'environment:\n  DB_PASSWORD: ${DB_PASSWORD}   ✓\n  # No fer mai:\n  DB_PASSWORD: "la_meva_clau"   ✗',
                },
              ].map(({ ok, title, detail, code }, i) => (
                <li key={i} className="border border-border-subtle rounded p-4 space-y-2">
                  <div className="flex gap-2 items-start">
                    <span className="text-green-400 shrink-0 mt-0.5">✓</span>
                    <p className="text-ui font-semibold text-ink-0">{title}</p>
                  </div>
                  <p className="text-data text-ink-2 pl-5">{detail}</p>
                  {code && (
                    <pre className="pl-5 f-mono text-caption text-ink-3 bg-bg-1 border border-border-subtle rounded p-3 overflow-x-auto leading-5">{code}</pre>
                  )}
                </li>
              ))}
            </ul>
          </div>

          {/* Prohibicions ✗ */}
          <div className="space-y-3">
            <p className="f-mono text-caption uppercase tracking-widest text-red-400">No s&apos;accepta</p>
            <ul className="space-y-2">
              {[
                { text: 'Mode privilegiat', code: 'privileged: true   ✗' },
                { text: 'Muntar directoris del servidor host', code: 'volumes:\n  - /etc:/host-etc    ✗\n  - /var/run/docker.sock:/var/run/docker.sock   ✗' },
                { text: 'Exposar ports directament a internet', code: 'ports:\n  - "80:80"    ✗  (usa labels de Traefik)' },
                { text: 'Imatges sense tag de versió concret', code: 'image: wordpress:latest   ✗\nimage: wordpress:6.5      ✓' },
                { text: 'Accés a la xarxa interna d\'AMG (amg_net)', code: 'networks:\n  - amg_net   ✗  (AMG connecta la xarxa un cop aprovat)' },
              ].map(({ text, code }, i) => (
                <li key={i} className="border border-red-500/20 bg-red-500/5 rounded p-3 space-y-1.5">
                  <div className="flex gap-2 items-center">
                    <span className="text-red-400 shrink-0">✗</span>
                    <p className="text-data text-ink-1">{text}</p>
                  </div>
                  <pre className="pl-5 f-mono text-caption text-red-400/70 bg-bg-1 border border-red-500/20 rounded p-2 overflow-x-auto leading-5">{code}</pre>
                </li>
              ))}
            </ul>
          </div>

          <div className="border border-amber-500/30 bg-amber-500/5 rounded p-4">
            <p className="text-data text-ink-2">
              <span className="text-amber-400 font-semibold">Nota: </span>
              Si el compose no compleix algun d&apos;aquests requisits, AMG rebutjarà la sol·licitud indicant els canvis necessaris.
              El procés de revisió té un termini màxim de <strong className="text-ink-1">48 hores laborables</strong>.
            </p>
          </div>
        </section>

        {/* Procés */}
        <section className="border border-border-base rounded p-5 space-y-3">
          <p className="f-mono text-caption uppercase tracking-widest text-accent-light">Procés de desplegament</p>
          <ol className="space-y-2">
            {[
              { n: '1', text: 'Envies el compose + .env.example + descripció del projecte' },
              { n: '2', text: 'AMG revisa el codi (termini màxim 48h laborables)' },
              { n: '3', text: 'Si s\'aprova, AMG configura les variables d\'entorn reals de forma segura i fa el desplegament manual' },
              { n: '4', text: 'La web s\'activa al domini indicat amb SSL automàtic' },
              { n: '5', text: 'Backup automàtic nocturn dels volums i base de dades a GCS' },
            ].map(({ n, text }) => (
              <li key={n} className="flex gap-3 text-data text-ink-2">
                <span className="w-5 h-5 rounded-full bg-accent/20 text-accent-light f-mono text-caption flex items-center justify-center shrink-0 mt-0.5">{n}</span>
                <span>{text}</span>
              </li>
            ))}
          </ol>
        </section>

        {/* Formulari */}
        {canRequest && (
          <section className="border border-border-base rounded p-5 space-y-5">
            <h2 className="f-display font-bold text-ink-0">Enviar sol·licitud</h2>

            <div className="space-y-1.5">
              <label className="f-mono text-caption uppercase tracking-widest text-ink-3">Domini</label>
              <input
                type="text"
                placeholder="exemple.com"
                value={domain}
                onChange={e => setDomain(e.target.value)}
                className="w-full bg-bg-2 border border-border-base rounded px-3 py-2 text-ui text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-accent"
              />
            </div>

            <div className="space-y-1.5">
              <label className="f-mono text-caption uppercase tracking-widest text-ink-3">
                Descripció del projecte
              </label>
              <textarea
                rows={3}
                placeholder="Explica breument de quina web es tracta, quines tecnologies usa i si necessites algun requisit especial…"
                value={description}
                onChange={e => setDescription(e.target.value)}
                className="w-full bg-bg-2 border border-border-base rounded px-3 py-2 text-ui text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-accent resize-none"
              />
            </div>

            {/* Fitxer compose */}
            <div className="space-y-1.5">
              <label className="f-mono text-caption uppercase tracking-widest text-ink-3">
                docker-compose.yml <span className="text-red-400">*</span>
              </label>
              <div
                className="border-2 border-dashed border-border-base rounded p-4 text-center cursor-pointer hover:border-accent transition-colors"
                onClick={() => composeRef.current?.click()}
              >
                {composeFile ? (
                  <p className="text-ui text-ink-0">{composeFile.name}</p>
                ) : (
                  <p className="text-data text-ink-2">Fes clic per seleccionar el docker-compose.yml</p>
                )}
              </div>
              <input
                ref={composeRef}
                type="file"
                accept=".yml,.yaml"
                className="hidden"
                onChange={e => setComposeFile(e.target.files?.[0] ?? null)}
              />
            </div>

            {/* Fitxer .env.example */}
            <div className="space-y-1.5">
              <label className="f-mono text-caption uppercase tracking-widest text-ink-3">
                .env.example <span className="text-ink-3">(recomanat)</span>
              </label>
              <div
                className="border-2 border-dashed border-border-base rounded p-4 text-center cursor-pointer hover:border-accent transition-colors"
                onClick={() => envRef.current?.click()}
              >
                {envFile ? (
                  <p className="text-ui text-ink-0">{envFile.name}</p>
                ) : (
                  <p className="text-data text-ink-2">Fes clic per seleccionar el .env.example</p>
                )}
              </div>
              <input
                ref={envRef}
                type="file"
                accept=".env,.example,.txt"
                className="hidden"
                onChange={e => setEnvFile(e.target.files?.[0] ?? null)}
              />
            </div>

            <AMGButton
              onClick={() => doRequest()}
              disabled={!composeFile || !domain.trim() || requesting}
              loading={requesting}
            >
              Enviar sol·licitud (revisió 48h)
            </AMGButton>
          </section>
        )}
      </div>
    </PortalShell>
  );
}
