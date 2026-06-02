'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import { getSystemConfig, setSystemConfig, deleteSystemConfig, testSystemConfig, TESTABLE_KEYS, type ConfigStatus } from '@/services/sysconfig';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { I } from '@/components/ui/icons';

const CATEGORY_LABEL: Record<string, string> = {
  AGENTS: 'Agents IA',
  INFRAOPS: 'InfraOps',
  PROSPECTING: 'Prospecció',
  PAYMENTS: 'Pagaments',
  FINOPS: 'FinOps',
  BACKUP: 'Backup',
  AUTOMATIONS: 'Automatitzacions',
  CALENDAR: 'Calendari',
  EMAIL_INBOUND: 'Email Inbound',
};

type HelpStep = { n: number; text: React.ReactNode };

const CATEGORY_HELP: Record<string, { title: string; steps: HelpStep[] }[]> = {
  EMAIL_INBOUND: [
    {
      title: 'Configuració del Cloudflare Email Worker (una sola vegada)',
      steps: [
        { n: 1, text: <>Ves a <strong>dash.cloudflare.com → amgdl.com → Email → Email Routing</strong> i assegura&apos;t que Email Routing està activat.</> },
        { n: 2, text: <>Ves a <strong>DNS</strong> i afegeix els registres MX per al subdomini <code className="bg-surface-overlay px-1 rounded text-[10px]">inbound.amgdl.com</code>:<br/><code className="bg-surface-overlay px-1 rounded text-[10px] block mt-1">inbound  MX  10  route1.mx.cloudflare.net</code><code className="bg-surface-overlay px-1 rounded text-[10px] block mt-0.5">inbound  MX  20  route2.mx.cloudflare.net</code><code className="bg-surface-overlay px-1 rounded text-[10px] block mt-0.5">inbound  MX  50  route3.mx.cloudflare.net</code></> },
        { n: 3, text: <>Ves a <strong>Workers & Pages → Crear aplicació → Worker</strong>. Anomena&apos;l <code className="bg-surface-overlay px-1 rounded text-[10px]">amg-email-worker</code>. Enganxa el contingut del fitxer <code className="bg-surface-overlay px-1 rounded text-[10px]">infra/cloudflare-email-worker/worker.js</code> del repositori.</> },
        { n: 4, text: <>Torna a <strong>Email → Email Routing → Routes</strong>. Crea una nova regla: <em>Catch-all</em> <code className="bg-surface-overlay px-1 rounded text-[10px]">*@inbound.amgdl.com</code> → acció <strong>Send to a Worker</strong> → selecciona <code className="bg-surface-overlay px-1 rounded text-[10px]">amg-email-worker</code>.</> },
        { n: 5, text: <>Prova enviant un email a <code className="bg-surface-overlay px-1 rounded text-[10px]">test@inbound.amgdl.com</code> i comprova els logs del Worker a <strong>Workers → amg-email-worker → Logs</strong>. Ha d&apos;aparèixer "Email enrutat".</> },
        { n: 6, text: <>Ja funciona. A partir d&apos;ara, cada tenant pot posar qualsevol adreça <code className="bg-surface-overlay px-1 rounded text-[10px]">@inbound.amgdl.com</code> a la seva configuració d&apos;agents sense cap tràmit addicional.</> },
      ],
    },
  ],
  CALENDAR: [
    {
      title: 'Fase 1 — AMG crea i gestiona el calendari (GOOGLE_CALENDAR_SA_JSON)',
      steps: [
        { n: 1, text: <>Ves a <a href="https://console.cloud.google.com" target="_blank" rel="noreferrer" className="text-accent-light underline">console.cloud.google.com</a> → crea un projecte (ex: <code className="bg-surface-overlay px-1 rounded text-[10px]">amg-calendar</code>).</> },
        { n: 2, text: <>Activa l'API: <strong>APIs i serveis → Biblioteca → cerca "Google Calendar API" → Activa</strong>.</> },
        { n: 3, text: <>Crea el Service Account: <strong>IAM i administrador → Comptes de servei → Crea</strong>. El nom pot ser <code className="bg-surface-overlay px-1 rounded text-[10px]">amg-calendar-bot</code>. No cal assignar cap rol.</> },
        { n: 4, text: <>Dins del Service Account creat: <strong>Claus → Afegir clau → Crear nova clau → JSON → Crea</strong>. Es descarregarà un fitxer <code className="bg-surface-overlay px-1 rounded text-[10px]">.json</code>.</> },
        { n: 5, text: <>Obre el fitxer JSON, copia tot el contingut i enganxa'l al camp <strong>GOOGLE_CALENDAR_SA_JSON</strong> d'aquí a baix.</> },
        { n: 6, text: <>Des d'ara, al formulari <strong>F2 Agenda</strong> de cada tenant podràs clicar <em>"Crear calendari AMG"</em> i es crearà i compartirà automàticament.</> },
      ],
    },
    {
      title: 'Fase 2 — Client connecta el seu propi Google (GOOGLE_OAUTH_CLIENT_ID + SECRET) — Opcional',
      steps: [
        { n: 1, text: <>Al mateix projecte de Google Cloud: <strong>APIs i serveis → Credencials → Crear credencials → ID de client OAuth 2.0</strong>.</> },
        { n: 2, text: <>Tipus d'aplicació: <strong>Aplicació web</strong>. Afegeix als <em>URI de redireccionament autoritzats</em>: <code className="bg-surface-overlay px-1 rounded text-[10px] break-all">https://api.amgdl.com/api/v1/nexe/calendar/oauth-callback</code></> },
        { n: 3, text: <>Copia el <strong>Client ID</strong> → camp GOOGLE_OAUTH_CLIENT_ID, i el <strong>Client Secret</strong> → camp GOOGLE_OAUTH_CLIENT_SECRET.</> },
        { n: 4, text: <>Al formulari F2 Agenda del tenant, selecciona <em>"Google Calendar — Client connecta el seu compte"</em> i clica <em>"Connecta Google Calendar →"</em>.</> },
      ],
    },
  ],
};

function CategoryHelp({ category }: { category: string }) {
  const [open, setOpen] = useState(false);
  const sections = CATEGORY_HELP[category];
  if (!sections) return null;
  return (
    <div className="border-b border-border-base">
      <button
        type="button"
        onClick={() => setOpen(o => !o)}
        className="w-full flex items-center gap-2 px-4 sm:px-5 py-2.5 text-left hover:bg-accent/5 transition-colors"
      >
        <I.AlertCircle size={13} className="text-accent-light flex-shrink-0" />
        <span className="f-mono text-xs text-accent-light">Com configurar — instruccions pas a pas</span>
        <I.ChevDown size={13} className={`ml-auto text-ink-3 transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>
      {open && (
        <div className="px-4 sm:px-5 pb-5 space-y-6 bg-[rgba(255,107,0,0.03)]">
          {sections.map((section, si) => (
            <div key={si} className="space-y-3">
              <p className="f-mono text-xs font-semibold text-accent-light uppercase tracking-wide pt-3">{section.title}</p>
              <div className="space-y-2">
                {section.steps.map(step => (
                  <div key={step.n} className="flex gap-3">
                    <span className="flex-shrink-0 w-5 h-5 rounded-full bg-accent-subtle border border-[rgba(255,107,0,0.4)] text-accent-light f-mono text-[10px] font-bold flex items-center justify-center mt-0.5">{step.n}</span>
                    <span className="f-mono text-xs text-ink-1 leading-relaxed">{step.text}</span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function StatusBadge({ status }: { status: ConfigStatus }) {
  if (status.configured && status.source === 'ENV') {
    return <AMGBadge tone="success">Variable d'entorn</AMGBadge>;
  }
  if (status.configured && status.source === 'DB') {
    return <AMGBadge tone="info">Configurat (DB)</AMGBadge>;
  }
  return <AMGBadge tone="danger">No configurat</AMGBadge>;
}

function KeyRow({ item, onSave, onDelete }: {
  item: ConfigStatus;
  onSave: (key: string, value: string) => void;
  onDelete: (key: string) => void;
}) {
  const [editing, setEditing] = useState(false);
  const [value, setValue] = useState('');
  const [show, setShow] = useState(false);
  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState<{ ok: boolean; message: string } | null>(null);

  const handleTest = async () => {
    setTesting(true);
    setTestResult(null);
    try {
      const res = await testSystemConfig(item.key);
      setTestResult(res);
    } catch {
      setTestResult({ ok: false, message: 'Error connectant amb el servidor.' });
    } finally {
      setTesting(false);
    }
  };

  return (
    <div className={`px-4 sm:px-5 py-4 border-b border-border-base last:border-0 ${!item.configured ? 'bg-danger/[0.03]' : ''}`}>
      <div className="flex items-start gap-3">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap mb-0.5">
            <span className="f-display font-bold text-sm">{item.label}</span>
            <AMGBadge tone="neutral">{CATEGORY_LABEL[item.category] ?? item.category}</AMGBadge>
            <StatusBadge status={item} />
          </div>
          <p className="f-mono text-label text-ink-2 text-xs mt-0.5">{item.description}</p>
          <p className="f-mono text-label text-ink-3 text-xs mt-0.5 opacity-60">{item.key}</p>
        </div>

        <div className="flex gap-2 shrink-0 flex-wrap justify-end">
          {item.configured && TESTABLE_KEYS.has(item.key) && (
            <AMGButton size="sm" variant="secondary" onClick={handleTest} disabled={testing}>
              {testing ? '…' : 'Provar'}
            </AMGButton>
          )}
          {!item.configured && (
            <AMGButton size="sm" icon={I.Plus} onClick={() => setEditing(true)}>
              Configurar
            </AMGButton>
          )}
          {item.configured && item.source === 'DB' && (
            <>
              <AMGButton size="sm" variant="secondary" icon={I.Edit} onClick={() => setEditing(true)}>
                Editar
              </AMGButton>
              <AMGButton
                size="sm"
                variant="ghost"
                icon={I.Trash}
                onClick={() => { if (confirm(`Eliminar la clau ${item.key}?`)) onDelete(item.key); }}
              />
            </>
          )}
          {item.configured && item.source === 'ENV' && (
            <span className="f-mono text-label text-xs text-ink-3 self-center">Llegida d'entorn</span>
          )}
        </div>
      </div>

      {testResult && (
        <div className={`mt-3 p-3 rounded text-xs f-mono ${testResult.ok ? 'bg-success/10 text-success border border-success/30' : 'bg-danger/10 text-danger border border-danger/30'}`}>
          {testResult.ok ? '✓ ' : '✗ '}{testResult.message}
        </div>
      )}

      {editing && (
        <div className="mt-3 flex gap-2 items-start">
          <div className="flex-1 relative">
            <input
              type={item.secret && !show ? 'password' : 'text'}
              value={value}
              onChange={(e) => setValue(e.target.value)}
              placeholder={`Introdueix ${item.label}…`}
              className="w-full bg-bg-1 border border-accent text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none pr-9"
              autoFocus
            />
            {item.secret && (
              <button
                type="button"
                onClick={() => setShow(!show)}
                className="absolute right-2 top-1/2 -translate-y-1/2 text-ink-3 hover:text-ink-1"
              >
                {show ? <I.EyeOff size={14} /> : <I.Eye size={14} />}
              </button>
            )}
          </div>
          <AMGButton
            size="sm"
            icon={I.Check}
            disabled={!value.trim()}
            onClick={() => { onSave(item.key, value.trim()); setEditing(false); setValue(''); }}
          >
            Desar
          </AMGButton>
          <AMGButton size="sm" variant="ghost" onClick={() => { setEditing(false); setValue(''); }}>
            Cancel·lar
          </AMGButton>
        </div>
      )}
    </div>
  );
}

export default function SystemConfigPage() {
  const { user, isSuperAdmin } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();

  const { data: configs = [], isLoading } = useQuery({
    queryKey: ['system-config'],
    queryFn: getSystemConfig,
    enabled: !!user && isSuperAdmin,
  });

  const { mutate: doSave } = useMutation({
    mutationFn: ({ key, value }: { key: string; value: string }) => setSystemConfig(key, value),
    onSuccess: (_, { key }) => {
      toast('success', `Clau ${key} desada correctament`);
      qc.invalidateQueries({ queryKey: ['system-config'] });
    },
    onError: () => toast('error', 'Error desant la clau'),
  });

  const { mutate: doDelete } = useMutation({
    mutationFn: (key: string) => deleteSystemConfig(key),
    onSuccess: (_, key) => {
      toast('success', `Clau ${key} eliminada`);
      qc.invalidateQueries({ queryKey: ['system-config'] });
    },
    onError: () => toast('error', 'Error eliminant la clau'),
  });

  if (!user || !isSuperAdmin) return null;

  const list = configs as ConfigStatus[];
  const missing = list.filter((c) => !c.configured).length;

  // Group by category
  const byCategory = list.reduce<Record<string, ConfigStatus[]>>((acc, c) => {
    const cat = c.category;
    if (!acc[cat]) acc[cat] = [];
    acc[cat].push(c);
    return acc;
  }, {});

  return (
    <PortalShell breadcrumb="admin / config">
      <div className="p-4 sm:p-8 space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / config /</span>
            <div className="flex items-center gap-3 mt-1">
              <div className="f-display font-bold text-xl">API Keys del Sistema</div>
              {missing > 0 && <AMGBadge tone="danger">{missing} no configurades</AMGBadge>}
              {missing === 0 && list.length > 0 && <AMGBadge tone="success">Tot configurat</AMGBadge>}
            </div>
          </div>
        </div>

        <div className="amg-card card-clip p-4 border-l-2 border-l-accent bg-accent/5">
          <p className="f-mono text-label text-ink-1 text-xs leading-relaxed">
            Les claus d'API s'emmagatzemen xifrades (AES-256) a la base de dades.
            Les variables d'entorn tenen prioritat sobre les claus de la BD.
            Canviar una clau aquí no requereix reiniciar el servidor.
          </p>
        </div>

        {isLoading ? (
          <div className="flex justify-center py-12">
            <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          </div>
        ) : (
          Object.entries(byCategory).map(([category, items]) => (
            <div key={category} className="amg-card card-clip">
              <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
                <div className="f-mono text-label uppercase text-ink-2 tracking-widest">
                  {CATEGORY_LABEL[category] ?? category}
                </div>
                <AMGBadge tone={items.every((i) => i.configured) ? 'success' : 'warning'}>
                  {items.filter((i) => i.configured).length}/{items.length}
                </AMGBadge>
              </div>
              <div>
                <CategoryHelp category={category} />
                {items.map((item) => (
                  <KeyRow
                    key={item.key}
                    item={item}
                    onSave={(key, value) => doSave({ key, value })}
                    onDelete={(key) => doDelete(key)}
                  />
                ))}
              </div>
            </div>
          ))
        )}
      </div>
    </PortalShell>
  );
}
