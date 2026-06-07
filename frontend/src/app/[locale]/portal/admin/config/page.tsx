'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import { useTranslations } from 'next-intl';
import { getSystemConfig, setSystemConfig, deleteSystemConfig, testSystemConfig, getAuditLog, TESTABLE_KEYS, type ConfigStatus } from '@/services/sysconfig';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { IconSet } from '@/components/ui/icons';

const CATEGORY_ORDER = [
  'MAINTENANCE',
  'AI_MODELS',
  'STORAGE',
  'AGENTS',
  'INFRAOPS',
  'PROSPECTING',
  'PAYMENTS',
  'FINOPS',
  'BACKUP',
  'AUTOMATIONS',
  'CALENDAR',
  'EMAIL_INBOUND',
  'META_ADS',
  'IMAGE_GEN',
  'GENERAL',
];

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
        <IconSet.AlertCircle size={13} className="text-accent-light flex-shrink-0" />
        <span className="f-mono text-xs text-accent-light">Com configurar — instruccions pas a pas</span>
        <IconSet.ChevDown size={13} className={`ml-auto text-ink-3 transition-transform ${open ? 'rotate-180' : ''}`} />
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

function SourceBadge({ status, t }: { status: ConfigStatus; t: (key: string) => string }) {
  if (status.source === 'ENV') return <AMGBadge tone="success">{t('envSource')}</AMGBadge>;
  if (status.source === 'DB') return <AMGBadge tone="info">{t('dbSource')}</AMGBadge>;
  if (status.source === 'DEFAULT') return <AMGBadge tone="neutral">{t('defaultSource')}</AMGBadge>;
  return <AMGBadge tone="danger">{t('missingSource')}</AMGBadge>;
}

function TypeBadge({ type, t }: { type: string; t: (key: string) => string }) {
  const label = t(`types.${type}`) ?? type;
  return <AMGBadge tone="neutral">{label}</AMGBadge>;
}

function KeyRow({ item, onSave, onDelete, t }: {
  item: ConfigStatus;
  onSave: (key: string, value: string) => void;
  onDelete: (key: string) => void;
  t: (key: string, opts?: any) => string;
}) {
  const [editing, setEditing] = useState(false);
  const [value, setValue] = useState('');
  const [show, setShow] = useState(false);
  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState<{ ok: boolean; message: string } | null>(null);
  const [showAudit, setShowAudit] = useState(false);
  const [auditLog, setAuditLog] = useState<{ id: string; action: string; userEmail: string | null; changedAt: string }[]>([]);
  const qc = useQueryClient();

  const handleTest = async () => {
    setTesting(true);
    setTestResult(null);
    try {
      const res = await testSystemConfig(item.key);
      setTestResult(res);
    } catch {
      setTestResult({ ok: false, message: t('testError') });
    } finally {
      setTesting(false);
    }
  };

  const handleAudit = async () => {
    if (showAudit) { setShowAudit(false); return; }
    try {
      const log = await getAuditLog(item.key);
      setAuditLog(log);
      setShowAudit(true);
    } catch {}
  };

  const inputMode = item.type || 'secret';

  return (
    <div className={`px-4 sm:px-5 py-4 border-b border-border-base last:border-0 ${!item.configured ? 'bg-danger/[0.03]' : ''}`}>
      <div className="flex items-start gap-3">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap mb-0.5">
            <span className="f-display font-bold text-sm">{item.label}</span>
            <TypeBadge type={inputMode} t={t} />
            <SourceBadge status={item} t={t} />
          </div>
          <p className="f-mono text-label text-ink-2 text-xs mt-0.5">{item.description}</p>
          <p className="f-mono text-label text-ink-3 text-xs mt-0.5 opacity-60">{item.key}</p>
        </div>

        <div className="flex gap-2 shrink-0 flex-wrap justify-end">
          {item.configured && TESTABLE_KEYS.has(item.key) && (
            <AMGButton size="sm" variant="secondary" onClick={handleTest} disabled={testing}>
              {testing ? t('loadingTest') : t('btnTest')}
            </AMGButton>
          )}
          {!item.configured && (
            <AMGButton size="sm" icon={IconSet.Plus} onClick={() => setEditing(true)}>
              {t('btnConfigure')}
            </AMGButton>
          )}
          {item.configured && item.source !== 'ENV' && (
            <>
              <AMGButton size="sm" variant="secondary" icon={IconSet.Edit} onClick={() => setEditing(true)}>
                {t('btnEdit')}
              </AMGButton>
              <AMGButton
                size="sm"
                variant="ghost"
                icon={IconSet.FileText}
                onClick={handleAudit}
              />
              <AMGButton
                size="sm"
                variant="ghost"
                icon={IconSet.Trash}
                onClick={() => { if (confirm(t('confirmDelete', { key: item.key }))) onDelete(item.key); }}
              />
            </>
          )}
          {item.configured && item.source === 'ENV' && (
            <span className="f-mono text-label text-xs text-ink-3 self-center">{t('envRead')}</span>
          )}
        </div>
      </div>

      {testResult && (
        <div className={`mt-3 p-3 rounded text-xs f-mono ${testResult.ok ? 'bg-success/10 text-success border border-success/30' : 'bg-danger/10 text-danger border border-danger/30'}`}>
          {testResult.ok ? '✓ ' : '✗ '}{testResult.message}
        </div>
      )}

      {showAudit && auditLog.length > 0 && (
        <div className="mt-3 p-3 rounded bg-bg-1 border border-border-base text-xs f-mono space-y-1 max-h-40 overflow-y-auto">
          <p className="text-ink-2 font-semibold mb-1">{t('auditTitle')}</p>
          {auditLog.map(e => (
            <div key={e.id} className="flex gap-2 text-ink-3">
              <span className="text-ink-1">{e.action === 'SET' ? '🖊' : '🗑'}</span>
              <span>{new Date(e.changedAt).toLocaleString('ca')}</span>
              {e.userEmail && <span>— {e.userEmail}</span>}
            </div>
          ))}
        </div>
      )}

      {editing && (
        <div className="mt-3">
          {inputMode === 'boolean' ? (
            <div className="flex gap-2 items-center">
              <button
                type="button"
                onClick={() => setValue(value === 'true' ? 'false' : 'true')}
                className={`w-10 h-6 rounded-full transition-colors ${value === 'true' ? 'bg-accent' : 'bg-ink-3'}`}
              >
                <span className={`block w-4 h-4 bg-white rounded-full transition-transform ${value === 'true' ? 'translate-x-5' : 'translate-x-1'}`} />
              </button>
              <span className="f-mono text-xs text-ink-2">{value === 'true' ? t('toggleActive') : t('toggleInactive')}</span>
              <AMGButton size="sm" icon={IconSet.Check} onClick={() => { onSave(item.key, value || 'false'); setEditing(false); }}>
                {t('btnSave')}
              </AMGButton>
              <AMGButton size="sm" variant="ghost" onClick={() => setEditing(false)}>{t('btnCancel')}</AMGButton>
            </div>
          ) : inputMode === 'json' ? (
            <div className="flex gap-2 items-start">
              <textarea
                value={value}
                onChange={(e) => setValue(e.target.value)}
                placeholder={t('placeholderJson')}
                className="flex-1 bg-bg-1 border border-accent text-ink-0 px-3 py-2 f-mono text-xs focus:outline-none min-h-[100px]"
                autoFocus
              />
              <div className="flex gap-2 shrink-0">
                <AMGButton size="sm" icon={IconSet.Check} disabled={!value.trim()} onClick={() => { onSave(item.key, value.trim()); setEditing(false); }}>
                  {t('btnSave')}
                </AMGButton>
                <AMGButton size="sm" variant="ghost" onClick={() => { setEditing(false); setValue(''); }}>
                  {t('btnCancel')}
                </AMGButton>
              </div>
            </div>
          ) : inputMode === 'number' ? (
            <div className="flex gap-2 items-start">
              <input
                type="number"
                value={value}
                onChange={(e) => setValue(e.target.value)}
                placeholder={t('placeholderNumber')}
                className="w-40 bg-bg-1 border border-accent text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none"
                autoFocus
              />
              <AMGButton size="sm" icon={IconSet.Check} disabled={!value.trim()} onClick={() => { onSave(item.key, value.trim()); setEditing(false); }}>
                {t('btnSave')}
              </AMGButton>
              <AMGButton size="sm" variant="ghost" onClick={() => { setEditing(false); setValue(''); }}>
                {t('btnCancel')}
              </AMGButton>
            </div>
          ) : (
            <div className="flex gap-2 items-start">
              <div className="flex-1 relative">
                <input
                  type={inputMode === 'secret' && !show ? 'password' : 'text'}
                  value={value}
                  onChange={(e) => setValue(e.target.value)}
                  placeholder={t('placeholderValue', { label: item.label })}
                  className="w-full bg-bg-1 border border-accent text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none pr-9"
                  autoFocus
                />
                {inputMode === 'secret' && (
                  <button
                    type="button"
                    onClick={() => setShow(!show)}
                    className="absolute right-2 top-1/2 -translate-y-1/2 text-ink-3 hover:text-ink-1"
                  >
                    {show ? <IconSet.EyeOff size={14} /> : <IconSet.Eye size={14} />}
                  </button>
                )}
              </div>
              <AMGButton size="sm" icon={IconSet.Check} disabled={!value.trim()} onClick={() => { onSave(item.key, value.trim()); setEditing(false); setValue(''); }}>
                {t('btnSave')}
              </AMGButton>
              <AMGButton size="sm" variant="ghost" onClick={() => { setEditing(false); setValue(''); }}>
                {t('btnCancel')}
              </AMGButton>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default function SystemConfigPage() {
  const { user, isSuperAdmin } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();
  const [search, setSearch] = useState('');
  const t = useTranslations('admin.config');

  const { data: configs = [], isLoading } = useQuery({
    queryKey: ['system-config'],
    queryFn: getSystemConfig,
    enabled: !!user && isSuperAdmin,
  });

  const { mutate: doSave } = useMutation({
    mutationFn: ({ key, value }: { key: string; value: string }) => setSystemConfig(key, value),
    onSuccess: (res, { key }) => {
      if ('error' in res) {
        toast('error', (res as { error: string }).error);
      } else {
        toast('success', t('toastSaved', { key }));
      }
      qc.invalidateQueries({ queryKey: ['system-config'] });
    },
    onError: () => toast('error', t('toastSaveError')),
  });

  const { mutate: doDelete } = useMutation({
    mutationFn: (key: string) => deleteSystemConfig(key),
    onSuccess: (_, key) => {
      toast('success', t('toastDeleted', { key }));
      qc.invalidateQueries({ queryKey: ['system-config'] });
    },
    onError: () => toast('error', t('toastDeleteError')),
  });

  if (!user || !isSuperAdmin) return null;

  const list = configs as ConfigStatus[];
  const filtered = list.filter(c =>
    !search ||
    c.key.toLowerCase().includes(search.toLowerCase()) ||
    c.label.toLowerCase().includes(search.toLowerCase()) ||
    c.description.toLowerCase().includes(search.toLowerCase())
  );
  const missing = list.filter((c) => !c.configured).length;

  const byCategory = filtered.reduce<Record<string, ConfigStatus[]>>((acc, c) => {
    const cat = c.category;
    if (!acc[cat]) acc[cat] = [];
    acc[cat].push(c);
    return acc;
  }, {});

  const sortedCategories = Object.keys(byCategory).sort(
    (a, b) => (CATEGORY_ORDER.indexOf(a) !== -1 ? CATEGORY_ORDER.indexOf(a) : 999) -
              (CATEGORY_ORDER.indexOf(b) !== -1 ? CATEGORY_ORDER.indexOf(b) : 999)
  );

  return (
    <PortalShell breadcrumb="admin / config">
      <div className="p-4 sm:p-8 space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">{t('breadcrumbDisplay')}</span>
            <div className="flex items-center gap-3 mt-1">
              <div className="f-display font-bold text-xl">{t('title')}</div>
              {missing > 0 && <AMGBadge tone="danger">{t('missing', { n: missing })}</AMGBadge>}
              {missing === 0 && list.length > 0 && <AMGBadge tone="success">{t('allConfigured')}</AMGBadge>}
            </div>
          </div>
        </div>

        <div className="flex gap-3 items-center">
          <div className="relative flex-1 max-w-sm">
            <IconSet.Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-3" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder={t('search')}
              className="w-full bg-bg-1 border border-border-base text-ink-0 pl-9 pr-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
            />
          </div>
          {search && (
            <AMGButton size="sm" variant="ghost" onClick={() => setSearch('')}>
              <IconSet.X size={14} />
            </AMGButton>
          )}
          <span className="f-mono text-xs text-ink-3 ml-auto">
            {t('count', { filtered: filtered.length, total: list.length })}
          </span>
        </div>

        <div className="amg-card card-clip p-4 border-l-2 border-l-accent bg-accent/5">
          <p className="f-mono text-label text-ink-1 text-xs leading-relaxed">
            {t('infoEncrypted')}
            {' '}{t('infoEnvPriority')}
            {' '}{t('infoNoRestart')}
          </p>
        </div>

        {isLoading ? (
          <div className="flex justify-center py-12">
            <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          </div>
        ) : (
          sortedCategories.map((category) => {
            const items = byCategory[category];
            return (
              <div key={category} className="amg-card card-clip">
                <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
                  <div className="f-mono text-label uppercase text-ink-2 tracking-widest">
                    {t(`categories.${category}`) ?? category}
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
                      t={t}
                      onSave={(key, value) => doSave({ key, value })}
                      onDelete={(key) => doDelete(key)}
                    />
                  ))}
                </div>
              </div>
            );
          })
        )}
      </div>
    </PortalShell>
  );
}
