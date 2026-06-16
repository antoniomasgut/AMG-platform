'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import { useTranslations } from 'next-intl';
import { getSystemConfig, getSystemConfigDetail, setSystemConfig, deleteSystemConfig, testSystemConfig, getAuditLog, TESTABLE_KEYS, type ConfigStatus } from '@/services/sysconfig';
import { listTenants } from '@/services/admin';
import { getModelPrices, recalculateModelPrices } from '@/services/agents-conversational';
import { getDriveAMGStatus, shareDriveAMGFolder } from '@/services/google';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { IconSet } from '@/components/ui/icons';

// Claus que esperen un UUID de tenant — mostren un select en comptes d'un input de text
const TENANT_UUID_KEYS = new Set(['PLATFORM_TENANT_ID']);

// Claus amb opcions fixes — mostren un select en comptes d'un input de text
const KEY_OPTIONS: Record<string, { value: string; label: string }[]> = {
  DEFAULT_LOCALE: [
    { value: 'ca', label: 'Català (ca)' },
    { value: 'es', label: 'Español (es)' },
    { value: 'en', label: 'English (en)' },
    { value: 'de', label: 'Deutsch (de)' },
  ],
  DEFAULT_CURRENCY: [
    { value: 'EUR', label: 'EUR — Euro €' },
    { value: 'USD', label: 'USD — Dòlar $' },
    { value: 'GBP', label: 'GBP — Lliura £' },
  ],
};

// Phase-based grouping: each phase lists its categories in order
const PHASE_GROUPS: { id: string; label: string; description: string; categories: string[] }[] = [
  { id: 'F0', label: 'Plataforma (base)', description: 'Configuració essencial de la plataforma: identitat, límits, models IA i emmagatzematge.', categories: ['MAINTENANCE', 'GENERAL', 'AI_MODELS', 'STORAGE'] },
  { id: 'F1', label: 'F1 · Agent IA i canals', description: 'Claus per als agents conversacionals i els canals de comunicació (Telegram, Email, WhatsApp).', categories: ['AGENTS'] },
  { id: 'VENDES', label: 'Vendes · Notificacions', description: 'Telegram de vendes (chat ID) i activació individual de cada tipus de notificació comercial.', categories: ['VENDES'] },
  { id: 'F2', label: 'F2 · Agenda', description: 'Integració amb Google Calendar per crear i compartir agendes dels clients.', categories: ['CALENDAR'] },
  { id: 'F3', label: 'F3 · Pressupostos i Pagaments', description: 'Passerella de pagament Stripe i integració Holded per a facturació.', categories: ['PAYMENTS', 'FINOPS'] },
  { id: 'F5', label: 'F5 · Infraops i Alertes', description: 'Monitorització del servidor i alertes d\'infraestructura.', categories: ['INFRAOPS', 'BACKUP'] },
  { id: 'AUT', label: 'Automatitzacions (n8n)', description: 'Connexió amb la instància n8n per a workflows automàtics.', categories: ['AUTOMATIONS'] },
  { id: 'META', label: 'Meta Ads', description: 'Webhooks de Meta Lead Ads, tokens de pàgina i gestió de campanyes.', categories: ['META_ADS', 'IMAGE_GEN'] },
  { id: 'PROS', label: 'Prospecció', description: 'Google Places per a cerques de negocis potencials.', categories: ['PROSPECTING'] },
  { id: 'DOM', label: 'Dominis', description: 'Reseller OpenProvider per a registre i gestió de dominis.', categories: ['DOMAINS'] },
  { id: 'EMAIL', label: 'Email Inbound', description: 'Cloudflare Email Worker per rebre emails dels agents.', categories: ['EMAIL_INBOUND'] },
];

// Importance per key: level + one-liner impact if missing
type Importance = { level: 'critical' | 'important' | 'optional'; impact: string };
const KEY_IMPORTANCE: Record<string, Importance> = {
  ANTHROPIC_API_KEY:         { level: 'critical',  impact: 'Els agents IA no poden respondre' },
  BREVO_API_KEY:             { level: 'critical',  impact: 'No s\'envien emails (notificacions, reset de contrasenya, demos...)' },
  TELEGRAM_BOT_TOKEN:        { level: 'critical',  impact: 'No s\'envien notificacions ni alertes via Telegram' },
  WHATSAPP_WEBHOOK_SECRET:   { level: 'critical',  impact: 'El webhook de WhatsApp no es valida — canal WhatsApp inoperatiu' },
  META_WEBHOOK_VERIFY_TOKEN: { level: 'critical',  impact: 'El webhook de Meta Lead Ads no es valida — no arriben leads' },
  STRIPE_API_KEY:            { level: 'critical',  impact: 'El mòdul de pagaments no funciona' },
  N8N_API_KEY:               { level: 'critical',  impact: 'Les automatitzacions n8n no es poden gestionar' },
  GOOGLE_CALENDAR_SA_JSON:   { level: 'critical',  impact: 'No es poden crear ni compartir calendaris (F2 Agenda)' },
  MAINTENANCE_MODE:          { level: 'important', impact: 'Sense configurar, el mode manteniment queda desactivat per defecte' },
  PLATFORM_TENANT_ID:        { level: 'important', impact: 'Els leads del formulari web no s\'assignen a cap tenant' },
  DEEPSEEK_API_KEY:          { level: 'important', impact: 'DeepSeek no disponible com a alternativa als models Claude' },
  TWILIO_ACCOUNT_SID:        { level: 'important', impact: 'Canal WhatsApp via Twilio no disponible' },
  TWILIO_AUTH_TOKEN:         { level: 'important', impact: 'Canal WhatsApp via Twilio no disponible' },
  TWILIO_WHATSAPP_FROM:      { level: 'important', impact: 'Canal WhatsApp via Twilio no disponible' },
  META_PAGE_ACCESS_TOKEN:    { level: 'important', impact: 'Meta Ads sense token de pàgina de fallback' },
  META_APP_SECRET:           { level: 'important', impact: 'Les signatures HMAC dels webhooks Meta no es verifiquen' },
  META_ADS_MANAGEMENT_TOKEN: { level: 'important', impact: 'No es poden crear ni gestionar campanyes Meta des del portal' },
  HOLDED_API_KEY:            { level: 'important', impact: 'Integració FinOps/Holded no disponible' },
  GCS_PROJECT_ID:            { level: 'important', impact: 'Backups automàtics a Google Cloud Storage no funcionen' },
  GCS_BUCKET_NAME:           { level: 'important', impact: 'Backups automàtics a Google Cloud Storage no funcionen' },
  GOOGLE_PLACES_API_KEY:     { level: 'important', impact: 'El mòdul de prospecció de negocis no funciona' },
  EMAIL_INBOUND_DOMAIN:      { level: 'important', impact: 'Els emails entrants no s\'enruten correctament als agents' },
  N8N_API_URL:               { level: 'important', impact: 'La instància n8n no és accessible des del portal' },
  GOOGLE_OAUTH_CLIENT_ID:    { level: 'optional',  impact: 'Els clients no podran connectar el seu propi Google Calendar' },
  GOOGLE_OAUTH_CLIENT_SECRET:{ level: 'optional',  impact: 'Els clients no podran connectar el seu propi Google Calendar' },
  OPENAI_API_KEY:            { level: 'optional',  impact: 'Generació d\'imatges DALL·E no disponible per a Meta Ads' },
  TELEGRAM_CHAT_ID:          { level: 'optional',  impact: 'Alertes d\'infraestructura no s\'envien al Telegram' },
  OPENPROVIDER_USERNAME:     { level: 'optional',  impact: 'Registre de dominis via OpenProvider no disponible' },
  OPENPROVIDER_PASSWORD:     { level: 'optional',  impact: 'Registre de dominis via OpenProvider no disponible' },
  AMG_SALES_CHAT_ID:         { level: 'important', impact: 'Les notificacions comercials (leads, pressupostos, pagaments) no s\'envien' },
  AMG_NOTIFY_LEAD_CREATED:   { level: 'optional',  impact: 'Sense activar, no arriben alertes de nous leads a Telegram' },
  AMG_NOTIFY_BUDGET_SENT:    { level: 'optional',  impact: 'Sense activar, no arriben alertes de pressupostos enviats' },
  AMG_NOTIFY_BUDGET_ACCEPTED:{ level: 'optional',  impact: 'Sense activar, no arriben alertes de pressupostos acceptats' },
  AMG_NOTIFY_PAYMENT_RECEIVED:{ level: 'optional', impact: 'Sense activar, no arriben alertes de pagaments confirmats' },
  AMG_NOTIFY_AGENT_ERROR:    { level: 'optional',  impact: 'Sense activar, no arriben alertes d\'errors crítics d\'agent' },
  AMG_NOTIFY_CONTACT_FORM:   { level: 'optional',  impact: 'Sense activar, no arriben alertes de formularis de contacte' },
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

const IMPORTANCE_TONE = { critical: 'danger', important: 'warning', optional: 'neutral' } as const;
const IMPORTANCE_LABEL = { critical: 'Imprescindible', important: 'Recomanat', optional: 'Opcional' };

function ImportanceBadge({ keyName }: { keyName: string }) {
  const imp = KEY_IMPORTANCE[keyName];
  if (!imp) return null;
  return (
    <AMGBadge tone={IMPORTANCE_TONE[imp.level]}>
      {IMPORTANCE_LABEL[imp.level]}
    </AMGBadge>
  );
}

function TenantSelect({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  const { data } = useQuery({
    queryKey: ['tenants-all'],
    queryFn: () => listTenants({ size: 200, sort: 'name,asc' }),
  });
  const tenants = data?.content ?? [];
  return (
    <select
      value={value}
      onChange={e => onChange(e.target.value)}
      className="flex-1 bg-bg-1 border border-accent text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none"
      autoFocus
    >
      <option value="">— Selecciona un tenant —</option>
      {tenants.map(t => (
        <option key={t.id} value={t.id}>
          {t.name} {t.isOwner ? '(propietari)' : ''}
        </option>
      ))}
    </select>
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
  const [loadingEdit, setLoadingEdit] = useState(false);
  const [value, setValue] = useState('');
  const [show, setShow] = useState(false);
  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState<{ ok: boolean; message: string } | null>(null);
  const [showAudit, setShowAudit] = useState(false);
  const [auditLog, setAuditLog] = useState<{ id: string; action: string; userEmail: string | null; changedAt: string }[]>([]);
  const qc = useQueryClient();

  const handleOpenEdit = async () => {
    setValue('');
    setEditing(true);
    if (!item.secret && item.source !== 'ENV' && item.configured) {
      setLoadingEdit(true);
      try {
        const detail = await getSystemConfigDetail(item.key);
        if (detail?.currentValue != null) setValue(detail.currentValue);
      } catch {}
      setLoadingEdit(false);
    }
  };

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
            <ImportanceBadge keyName={item.key} />
          </div>
          <p className="f-mono text-label text-ink-2 text-xs mt-0.5">{item.description}</p>
          <p className="f-mono text-label text-ink-3 text-xs mt-0.5 opacity-60">{item.key}</p>
          {!item.configured && KEY_IMPORTANCE[item.key] && KEY_IMPORTANCE[item.key].level !== 'optional' && (
            <p className="f-mono text-[10px] mt-1 text-danger/80">
              ⚠ Sense aquesta clau: {KEY_IMPORTANCE[item.key].impact}
            </p>
          )}
        </div>

        <div className="flex gap-2 shrink-0 flex-wrap justify-end">
          {item.configured && TESTABLE_KEYS.has(item.key) && (
            <AMGButton size="sm" variant="secondary" onClick={handleTest} disabled={testing}>
              {testing ? t('loadingTest') : t('btnTest')}
            </AMGButton>
          )}
          {!item.configured && (
            <AMGButton size="sm" icon={IconSet.Plus} onClick={handleOpenEdit}>
              {t('btnConfigure')}
            </AMGButton>
          )}
          {item.configured && item.source !== 'ENV' && (
            <>
              <AMGButton size="sm" variant="secondary" icon={IconSet.Edit} onClick={handleOpenEdit} disabled={loadingEdit}>
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
          ) : KEY_OPTIONS[item.key] ? (
            <div className="flex gap-2 items-start">
              <select
                value={value}
                onChange={e => setValue(e.target.value)}
                className="flex-1 bg-bg-1 border border-accent text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none"
                autoFocus
              >
                <option value="">— Selecciona —</option>
                {KEY_OPTIONS[item.key].map(o => (
                  <option key={o.value} value={o.value}>{o.label}</option>
                ))}
              </select>
              <AMGButton size="sm" icon={IconSet.Check} disabled={!value.trim()} onClick={() => { onSave(item.key, value.trim()); setEditing(false); setValue(''); }}>
                {t('btnSave')}
              </AMGButton>
              <AMGButton size="sm" variant="ghost" onClick={() => { setEditing(false); setValue(''); }}>
                {t('btnCancel')}
              </AMGButton>
            </div>
          ) : TENANT_UUID_KEYS.has(item.key) ? (
            <div className="flex gap-2 items-start">
              <TenantSelect value={value} onChange={setValue} />
              <AMGButton size="sm" icon={IconSet.Check} disabled={!value.trim()} onClick={() => { onSave(item.key, value.trim()); setEditing(false); setValue(''); }}>
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

function DriveAMGSection() {
  const { toast } = useToast();
  const [email, setEmail] = useState('');
  const [role, setRole] = useState<'reader' | 'writer'>('reader');
  const [loading, setLoading] = useState(false);

  const { data: status } = useQuery({
    queryKey: ['drive-amg-status'],
    queryFn: getDriveAMGStatus,
    retry: false,
  });

  const handleShare = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim()) return;
    setLoading(true);
    try {
      const res = await shareDriveAMGFolder(email.trim(), role);
      toast('success', res.message);
      setEmail('');
    } catch (err: unknown) {
      const msg = err && typeof err === 'object' && 'body' in err
        ? (err as { body: { error?: string } }).body?.error ?? 'Error desconegut'
        : 'Error desconegut';
      toast('error', msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="amg-card card-clip p-5 mt-6 space-y-4">
      <div className="flex items-center gap-3 border-b border-border-base pb-3">
        <IconSet.Database size={16} stroke="#FF6B00" />
        <div>
          <div className="f-display font-bold text-sm">Drive AMG — Accés a la carpeta Tenants/</div>
          <p className="f-mono text-xs text-ink-3 mt-0.5">
            Comparteix la carpeta centralitzada del Drive d'AMG amb membres de l'equip.
          </p>
        </div>
        <div className="ml-auto">
          {status?.saConfigured
            ? <span className="f-mono text-xs text-green-400">SA configurada</span>
            : <span className="f-mono text-xs text-yellow-400">SA no configurada</span>}
        </div>
      </div>

      {!status?.saConfigured ? (
        <p className="f-mono text-xs text-ink-2">
          Configura <code className="bg-surface-base px-1 rounded">GOOGLE_CALENDAR_SA_JSON</code> i habilita
          la <strong>Google Drive API</strong> al Google Cloud per poder compartir.
        </p>
      ) : (
        <>
          <div className="bg-surface-base rounded p-3 space-y-1">
            <p className="f-mono text-xs text-ink-2">Estructura al Drive del SA:</p>
            <pre className="f-mono text-[10px] text-ink-3 leading-relaxed">
{`Drive AMG (service account)/
└── Tenants/          ← carpeta que es comparteix
    ├── Restaurant Can Pep/
    ├── Perruqueria Maria/
    └── ...`}
            </pre>
          </div>

          <form onSubmit={handleShare} className="flex gap-2 items-end flex-wrap">
            <div className="flex-1 min-w-48">
              <label className="block f-mono text-xs text-ink-2 mb-1 uppercase tracking-wide">Email Google</label>
              <input
                type="email"
                required
                placeholder="nom@gmail.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
                className="w-full bg-surface-base border border-border-base rounded px-3 py-2 text-sm text-ink-0 focus:outline-none focus:border-accent"
              />
            </div>
            <div>
              <label className="block f-mono text-xs text-ink-2 mb-1 uppercase tracking-wide">Permís</label>
              <select
                value={role}
                onChange={e => setRole(e.target.value as 'reader' | 'writer')}
                className="bg-surface-base border border-border-base rounded px-3 py-2 text-sm text-ink-0 focus:outline-none focus:border-accent"
              >
                <option value="reader">Llegir</option>
                <option value="writer">Editar</option>
              </select>
            </div>
            <AMGButton type="submit" size="sm" loading={loading} disabled={!email.trim()}>
              Compartir
            </AMGButton>
          </form>
        </>
      )}
    </div>
  );
}

function ModelPricingSection({ queryClient }: { queryClient: ReturnType<typeof useQueryClient> }) {
  const [markup, setMarkup] = useState(20);

  const { data: prices = [], isLoading } = useQuery({
    queryKey: ['model-prices'],
    queryFn: getModelPrices,
  });

  const recalcMutation = useMutation({
    mutationFn: () => recalculateModelPrices(markup),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['model-prices'] }),
  });

  const fmtEur = (micros: number) => `€${(micros / 1_000_000).toFixed(6)}`;
  const fmtMTok = (micros: number) => `€${(micros / 10_000).toFixed(4)}/MTok`;

  return (
    <div className="mt-10 space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="f-display font-bold text-ink-1">Taula de preus IA</h2>
          <p className="text-xs text-ink-3 mt-0.5">Cost base (Anthropic/DeepSeek) + markup → preu client. Recàlcul automàtic el dia 1 de cada mes.</p>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm text-ink-2">Markup</span>
          <input
            type="number"
            min={0}
            max={500}
            value={markup}
            onChange={e => setMarkup(parseInt(e.target.value) || 0)}
            className="w-20 p-2 bg-bg-1 border border-border-base rounded text-sm text-ink-1 text-center"
          />
          <span className="text-sm text-ink-2">%</span>
          <AMGButton
            onClick={() => recalcMutation.mutate()}
            disabled={recalcMutation.isPending}
            size="sm"
          >
            {recalcMutation.isPending ? 'Calculant...' : 'Recalcular i desar'}
          </AMGButton>
        </div>
      </div>

      {isLoading && <p className="text-sm text-ink-3">Carregant...</p>}
      {!isLoading && prices.length === 0 && (
        <div className="amg-card card-clip p-6 text-center text-sm text-ink-3">
          Cap preu configurat. Prem &ldquo;Recalcular i desar&rdquo; per inicialitzar.
        </div>
      )}
      {prices.length > 0 && (
        <div className="amg-card card-clip overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border-base text-left">
                <th className="px-4 py-3 f-mono text-xs uppercase text-ink-3 tracking-wider">Model</th>
                <th className="px-4 py-3 f-mono text-xs uppercase text-ink-3 tracking-wider">Proveïdor</th>
                <th className="px-4 py-3 f-mono text-xs uppercase text-ink-3 tracking-wider text-right">Cost input</th>
                <th className="px-4 py-3 f-mono text-xs uppercase text-ink-3 tracking-wider text-right">Cost output</th>
                <th className="px-4 py-3 f-mono text-xs uppercase text-ink-3 tracking-wider text-center">Markup</th>
                <th className="px-4 py-3 f-mono text-xs uppercase text-ink-3 tracking-wider text-right">Preu client input</th>
                <th className="px-4 py-3 f-mono text-xs uppercase text-ink-3 tracking-wider text-right">Preu client output</th>
                <th className="px-4 py-3 f-mono text-xs uppercase text-ink-3 tracking-wider text-right">Actualitzat</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border-base">
              {prices.map(p => (
                <tr key={p.modelName} className="hover:bg-bg-1">
                  <td className="px-4 py-3 font-medium text-ink-1 f-mono text-xs">{p.modelName}</td>
                  <td className="px-4 py-3 text-ink-2">{p.provider}</td>
                  <td className="px-4 py-3 text-right text-ink-2 f-mono text-xs">
                    <div>{fmtEur(p.inputCostMicros)}</div>
                    <div className="text-ink-3">{fmtMTok(p.inputCostMicros)}</div>
                  </td>
                  <td className="px-4 py-3 text-right text-ink-2 f-mono text-xs">
                    <div>{fmtEur(p.outputCostMicros)}</div>
                    <div className="text-ink-3">{fmtMTok(p.outputCostMicros)}</div>
                  </td>
                  <td className="px-4 py-3 text-center">
                    <AMGBadge tone="neutral">+{p.markupPercent}%</AMGBadge>
                  </td>
                  <td className="px-4 py-3 text-right text-ink-1 font-medium f-mono text-xs">
                    <div>{fmtEur(p.clientInputMicros)}</div>
                    <div className="text-ink-3">{fmtMTok(p.clientInputMicros)}</div>
                  </td>
                  <td className="px-4 py-3 text-right text-ink-1 font-medium f-mono text-xs">
                    <div>{fmtEur(p.clientOutputMicros)}</div>
                    <div className="text-ink-3">{fmtMTok(p.clientOutputMicros)}</div>
                  </td>
                  <td className="px-4 py-3 text-right text-ink-3 text-xs">
                    {new Date(p.updatedAt).toLocaleDateString('ca-ES')}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
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

  // Build phase groups with their items (skip empty phases)
  const allCategoriesInPhases = new Set(PHASE_GROUPS.flatMap(p => p.categories));
  const orphanCategories = Object.keys(byCategory).filter(c => !allCategoriesInPhases.has(c));

  const phaseGroupsWithItems = [
    ...PHASE_GROUPS.map(phase => ({
      ...phase,
      items: phase.categories.flatMap(cat => byCategory[cat] ?? []),
    })).filter(p => p.items.length > 0),
    ...(orphanCategories.length > 0 ? [{
      id: 'OTHER', label: 'Altres', description: 'Claus no assignades a cap fase.', categories: orphanCategories,
      items: orphanCategories.flatMap(cat => byCategory[cat] ?? []),
    }] : []),
  ];

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
          phaseGroupsWithItems.map((phase) => {
            const phaseMissing = phase.items.filter(i => !i.configured && KEY_IMPORTANCE[i.key]?.level === 'critical').length;
            const phaseConfigured = phase.items.filter(i => i.configured).length;
            // Group items by category within phase (maintain category sub-headers for help panels)
            const phaseByCategory: Record<string, ConfigStatus[]> = {};
            for (const cat of phase.categories) {
              const catItems = byCategory[cat];
              if (catItems?.length) phaseByCategory[cat] = catItems;
            }
            return (
              <div key={phase.id} className="space-y-0">
                {/* Phase header */}
                <div className="flex items-center gap-3 pt-2 pb-1 px-1">
                  <span className="f-mono text-xs font-bold text-accent-light bg-accent/10 border border-accent/30 px-2 py-0.5 rounded">
                    {phase.id}
                  </span>
                  <div>
                    <span className="f-display font-bold text-sm text-ink-0">{phase.label}</span>
                    <span className="f-mono text-xs text-ink-3 ml-2">{phase.description}</span>
                  </div>
                  <div className="ml-auto flex gap-2 shrink-0">
                    {phaseMissing > 0 && (
                      <AMGBadge tone="danger">{phaseMissing} crític{phaseMissing > 1 ? 's' : ''} sense config</AMGBadge>
                    )}
                    <AMGBadge tone={phaseConfigured === phase.items.length ? 'success' : 'neutral'}>
                      {phaseConfigured}/{phase.items.length}
                    </AMGBadge>
                  </div>
                </div>

                {/* Category cards within phase */}
                {Object.entries(phaseByCategory).map(([category, items]) => (
                  <div key={category} className="amg-card card-clip mb-3">
                    {Object.keys(phaseByCategory).length > 1 && (
                      <div className="px-4 sm:px-5 py-2 border-b border-border-base">
                        <span className="f-mono text-[10px] uppercase text-ink-3 tracking-widest">
                          {t(`categories.${category}`) ?? category}
                        </span>
                      </div>
                    )}
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
                ))}
              </div>
            );
          })
        )}
      </div>
      {/* Drive AMG */}
      <DriveAMGSection />
      {/* Taula de preus IA */}
      <ModelPricingSection queryClient={qc} />
    </PortalShell>
  );
}
