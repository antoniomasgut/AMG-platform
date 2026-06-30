'use client';

import { useState, useEffect, useRef, type ReactNode } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, useRouter } from 'next/navigation';
import { useToast } from '@/lib/toast-context';
import {
  getTenant, updateTenant,
  getGoCardlessConfig, configureGoCardless, getGoCardlessMandate,
  initiateGoCardlessMandate, cancelGoCardlessMandate, listGoCardlessPayments,
  getPaymentProviders,
  markImplementationDelivered, markOnboardingCompleted, setBillingStartDate,
  getTenantSetup, listCatalogServices, toggleTenantService, removeTenantService,
  listProfiles, assignProfileToTenant, assignPhaseToTenant,
  addStandaloneServiceToTenant,
  getAgentChannels,
  getTelegramConfig, connectTelegram, verifyTelegram, disconnectTelegram,
  getWhatsAppConfig, connectWhatsApp, verifyWhatsApp, disconnectWhatsApp, sendWhatsAppTest,
  lookupSectorPricing, listSectorPhases, calcMonthly,
  checkTenantDeletion, deleteTenant,
  SECTOR_SIZES, SECTOR_LABELS, SIZE_LABELS, PHASE_LABELS, PHASE_UPGRADE_PRICE,
  type TenantResponse, type TenantSetup, type ChannelsConfig,
  type CatalogService, type CatalogProfileResponse, type CatalogPhaseResponse,
  type SectorPhaseResponse, type SectorPricingResponse,
  type GoCardlessConfig, type GoCardlessMandate, type GoCardlessPaymentItem,
  type ProviderSummary, type DeleteTenantCheck,
} from '@/services/admin';
import { createBudget, listBudgets, sendBudget, cancelBudget, updateBudget, type BudgetResponse, type CreateBudgetRequest } from '@/services/billing';
import { getDpaStatus, sendDpaRequest, type DpaStatus } from '@/services/dpa';
import { createIntake, getIntakeByBudget, type IntakeResponse as SetupIntakeResponse } from '@/services/setupIntake';
import { getMetaAdsConfig, saveMetaAdsConfig, syncMetaAds } from '@/services/meta-ads';
import { getChannelUsageStats, type ChannelUsageStats } from '@/services/agents-conversational';
import { SECTOR_CONTEXTS, getSectorContext } from '@/services/sector-contexts';
import { listLandings } from '@/services/factory';
import {
  listSites, requestStaticSite, updateStaticSite, approveSite, exportSite,
  sendSnippetsEmail, requestExternalSite, getWidgetConfig,
  type WebSiteResponse, type WebsiteStatus,
} from '@/services/hosting';
import { getWizardConfig } from '@/config/service-wizards';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGSectionTitle } from '@/components/ui/stat';
import { IconSet } from '@/components/ui/icons';

function fmtDate(d: string | null) {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

function fmt(n: number) {
  return new Intl.NumberFormat('ca-ES', { style: 'currency', currency: 'EUR' }).format(n);
}

const NEXE_PHASE_NAMES: Record<number, string> = {
  1: 'Captació',
  2: 'Agenda',
  3: 'Pressupostos',
  4: 'Seguiment',
  5: 'Alertes & Equip',
};

const WORKER_ADDONS: Record<string, { setup: number; monthly: number }> = {
  AUTONOMO: { setup: 0, monthly: 0 },
  PETIT:    { setup: 99, monthly: 20 },
  MITJA:    { setup: 199, monthly: 35 },
  EMPRESA:  { setup: 299, monthly: 50 },
};

const DEP_BADGE: Record<string, string> = {
  BASE: '🔵', REQUIRED: '🔴', OPTIONAL: '🟡',
};

type SectionStatus = 'active' | 'warning' | 'inactive' | 'neutral';

function CollapsibleSection({
  sectionId, eyebrow, title, status, warning,
  collapsed, onToggle, children,
}: {
  sectionId?: string;
  eyebrow?: string;
  title: string;
  status: SectionStatus;
  warning?: string;
  collapsed: boolean;
  onToggle: () => void;
  children: ReactNode;
}) {
  const dotCls = {
    active:   'bg-[#39d353]',
    warning:  'bg-amber-400',
    inactive: 'bg-[rgba(255,255,255,0.2)]',
    neutral:  'bg-[rgba(255,255,255,0.15)]',
  }[status];

  const labelText = status === 'active' ? 'Actiu'
    : status === 'warning' ? 'Pendent'
    : status === 'inactive' ? 'Inactiu'
    : null;

  const labelCls = status === 'active' ? 'text-[#39d353]'
    : status === 'warning' ? 'text-amber-400'
    : 'text-ink-3';

  return (
    <div id={sectionId}>
      <button
        type="button"
        onClick={onToggle}
        className="w-full flex items-center gap-2 mb-2 group"
      >
        <span className={`w-2 h-2 rounded-full flex-shrink-0 ${dotCls}`} />
        {eyebrow && (
          <span className="f-mono text-[9px] uppercase tracking-wider text-ink-3 shrink-0">{eyebrow}</span>
        )}
        <span className="f-mono text-xs font-semibold text-ink-1 group-hover:text-white transition shrink-0">{title}</span>
        {warning && <span className="f-mono text-[9px] text-amber-400 shrink-0">⚠ {warning}</span>}
        <span className="flex-1 h-px bg-border-base mx-1" />
        {labelText && <span className={`f-mono text-[9px] shrink-0 ${labelCls}`}>{labelText}</span>}
        <IconSet.Chevron
          size={13}
          className={`text-ink-3 transition-transform shrink-0 ${collapsed ? '' : 'rotate-90'}`}
        />
      </button>
      {!collapsed && <div>{children}</div>}
    </div>
  );
}

function statusBadge(status: string, activeLabel: string, inactiveLabel: string) {
  return status === 'APPROVED' || status === 'ACTIVE' || status === 'COMPLETED'
    ? <AMGBadge tone="success">{activeLabel}</AMGBadge>
    : status === 'PENDING' || status === 'REJECTED'
    ? <AMGBadge tone="warning">{status === 'REJECTED' ? 'Rebutjat' : 'Pendent'}</AMGBadge>
    : <AMGBadge tone="neutral">{inactiveLabel}</AMGBadge>;
}

const WEB_STATUS_LABEL: Record<WebsiteStatus, string> = {
  PENDING_REVIEW: 'Pendent revisió', APPROVED: 'Aprovada', DEPLOYING: 'Desplegant',
  ACTIVE: 'Activa', SUSPENDED: 'Suspesa', REJECTED: 'Rebutjada',
};
const WEB_STATUS_TONE: Record<WebsiteStatus, 'warning' | 'success' | 'info' | 'danger' | 'neutral'> = {
  PENDING_REVIEW: 'warning', APPROVED: 'info', DEPLOYING: 'info',
  ACTIVE: 'success', SUSPENDED: 'neutral', REJECTED: 'danger',
};

const ADMIN_API_URL = process.env.NEXT_PUBLIC_API_URL ?? '';

function TenantWebSnippets({ site, tenantEmail }: { site: WebSiteResponse; tenantEmail: string }) {
  const { toast } = useToast();
  const [sending, setSending] = useState(false);
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  const { data: cfg } = useQuery({
    queryKey: ['widget-config', site.id],
    queryFn: () => getWidgetConfig(site.id),
  });

  function copy(key: string, value: string) {
    navigator.clipboard.writeText(value);
    setCopiedKey(key);
    setTimeout(() => setCopiedKey(null), 2000);
  }

  async function handleSendEmail() {
    setSending(true);
    try {
      await sendSnippetsEmail(site.id);
      toast('success', `Snippets enviats a ${tenantEmail}`);
    } catch {
      toast('error', 'Error enviant el correu');
    } finally {
      setSending(false);
    }
  }

  const widgetCode = `<script src="${ADMIN_API_URL}/api/v1/widget/${site.id}/loader" defer></script>`;
  const formCode = `<form action="${ADMIN_API_URL}/api/v1/widget/${site.id}/contact" method="POST">
  <input type="text"  name="name"    placeholder="Nom"      required />
  <input type="email" name="email"   placeholder="Email"    required />
  <input type="tel"   name="phone"   placeholder="Telèfon" />
  <textarea           name="message" placeholder="Missatge" required></textarea>
  <button type="submit">Enviar</button>
</form>`;

  const snippets = [
    { key: 'widget', label: 'Widget (xat IA + WhatsApp)', code: widgetCode },
    { key: 'form',   label: 'Formulari de contacte',      code: formCode   },
  ];

  return (
    <div className="space-y-3 mt-3">
      <div className="flex items-center justify-between">
        <p className="f-mono text-xs uppercase text-ink-2 tracking-widest">Snippets d'integració</p>
        <AMGButton size="sm" variant="secondary" disabled={sending} onClick={handleSendEmail}>
          {sending ? 'Enviant…' : `Enviar per email a ${tenantEmail}`}
        </AMGButton>
      </div>
      {snippets.map(({ key, label, code }) => (
        <div key={key} className="space-y-1">
          <div className="flex items-center justify-between">
            <p className="text-xs text-ink-3">{label}</p>
            <button
              onClick={() => copy(key, code)}
              className="f-mono text-[10px] uppercase px-2 py-0.5 border border-border-base rounded text-ink-2 hover:text-ink-0 transition-colors"
            >
              {copiedKey === key ? 'Copiat!' : 'Copiar'}
            </button>
          </div>
          <pre className="bg-bg-2 border border-border-subtle rounded p-2.5 text-[10px] text-accent-light f-mono overflow-x-auto whitespace-pre-wrap break-all leading-4">{code}</pre>
        </div>
      ))}
      {!cfg?.chatEnabled && (
        <p className="text-[11px] text-yellow-400">⚠ L'agent IA no està activat — el widget de xat no apareixerà fins que s'activi.</p>
      )}
    </div>
  );
}

function TenantWebSection({ tenantId, tenantEmail }: { tenantId: string; tenantEmail: string }) {
  const { toast } = useToast();
  const qc = useQueryClient();
  const fileRef = useRef<HTMLInputElement>(null);
  const updateRef = useRef<HTMLInputElement>(null);
  const [domain, setDomain] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [updatingSiteId, setUpdatingSiteId] = useState<string | null>(null);
  const [extDomain, setExtDomain] = useState('');
  const [extRedirect, setExtRedirect] = useState('');
  const [creatingExt, setCreatingExt] = useState(false);

  const { data: sites = [], isLoading } = useQuery({
    queryKey: ['tenant-sites', tenantId],
    queryFn: () => listSites(tenantId),
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ['tenant-sites', tenantId] });

  const hostedSite = sites.find(s => s.type !== 'EXTERNAL' && ['ACTIVE','PENDING_REVIEW','APPROVED','DEPLOYING'].includes(s.status));
  const externalSite = sites.find(s => s.type === 'EXTERNAL');

  async function handleUpload() {
    if (!file || !domain.trim()) return;
    setUploading(true);
    try {
      const site = await requestStaticSite(tenantId, file, domain.trim());
      await approveSite(site.id);
      toast('success', 'Web pujada i aprovada');
      setFile(null); setDomain('');
      invalidate();
    } catch { toast('error', 'Error pujant la web'); }
    finally { setUploading(false); }
  }

  async function handleUpdate(siteId: string, f: File) {
    try {
      await updateStaticSite(tenantId, siteId, f);
      toast('success', 'Web actualitzada');
      invalidate();
    } catch { toast('error', 'Error actualitzant'); }
  }

  async function handleCreateExternal() {
    if (!extDomain.trim()) return;
    setCreatingExt(true);
    try {
      await requestExternalSite(tenantId, extDomain.trim(), extRedirect.trim() || undefined);
      toast('success', 'Domini extern registrat');
      setExtDomain(''); setExtRedirect('');
      invalidate();
    } catch { toast('error', 'Error registrant el domini'); }
    finally { setCreatingExt(false); }
  }

  return (
    <div className="amg-card card-clip">
      <div className="p-4 sm:p-5 border-b border-border-base">
        <AMGSectionTitle eyebrow="Web" title="Allotjament web" />
      </div>
      <div className="p-4 sm:p-5 space-y-5">
        {isLoading ? (
          <div className="flex justify-center py-6"><span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" /></div>
        ) : (
          <>
            {/* Webs allotjades */}
            {sites.filter(s => s.type !== 'EXTERNAL').length > 0 && (
              <div className="space-y-2">
                <p className="f-mono text-xs uppercase text-ink-3 tracking-widest">Web allotjada per AMG</p>
                {sites.filter(s => s.type !== 'EXTERNAL').map(site => (
                  <div key={site.id} className="flex items-center justify-between gap-4 p-3 border border-border-base rounded">
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <AMGBadge tone={WEB_STATUS_TONE[site.status]}>{WEB_STATUS_LABEL[site.status]}</AMGBadge>
                        <span className="f-mono text-xs text-ink-3">{site.type}</span>
                      </div>
                      <p className="text-sm font-medium">{site.domain ?? '—'}</p>
                      {site.reviewNotes && <p className="text-xs text-ink-3 italic mt-0.5">{site.reviewNotes}</p>}
                    </div>
                    {site.status === 'ACTIVE' && (
                      <div className="flex gap-2 shrink-0">
                        <AMGButton size="sm" variant="secondary" onClick={() => { setUpdatingSiteId(site.id); updateRef.current?.click(); }}>Actualitzar</AMGButton>
                        <AMGButton size="sm" variant="ghost" onClick={() => exportSite(tenantId, site.id).catch(() => toast('error', 'Error'))}>ZIP</AMGButton>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}

            <input ref={updateRef} type="file" accept=".zip" className="hidden"
              onChange={e => { const f = e.target.files?.[0]; if (f && updatingSiteId) handleUpdate(updatingSiteId, f); e.target.value = ''; }} />

            {!hostedSite && (
              <div className="border border-border-base rounded p-4 space-y-3">
                <p className="f-mono text-xs uppercase text-ink-2 tracking-widest">Pujar nova web (ZIP estàtic)</p>
                <div className="flex gap-3 flex-wrap">
                  <input type="text" placeholder="domini.com" value={domain} onChange={e => setDomain(e.target.value)}
                    className="flex-1 min-w-[140px] bg-bg-2 border border-border-base rounded px-3 py-1.5 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-accent" />
                  <AMGButton size="sm" variant="secondary" onClick={() => fileRef.current?.click()}>
                    {file ? file.name.slice(0, 18) + '…' : 'Seleccionar ZIP'}
                  </AMGButton>
                  <input ref={fileRef} type="file" accept=".zip" className="hidden" onChange={e => setFile(e.target.files?.[0] ?? null)} />
                </div>
                <AMGButton size="sm" disabled={!file || !domain.trim() || uploading} loading={uploading} onClick={handleUpload}>Pujar i activar</AMGButton>
              </div>
            )}

            {/* Web externa (snippets) */}
            <div className="border-t border-border-base pt-4">
              <p className="f-mono text-xs uppercase text-ink-2 tracking-widest mb-3">Web externa (integració amb snippets)</p>
              {externalSite ? (
                <>
                  <div className="flex items-center gap-2 mb-2">
                    <AMGBadge tone="info">EXTERNAL</AMGBadge>
                    <span className="text-sm text-ink-1">{externalSite.domain}</span>
                  </div>
                  <TenantWebSnippets site={externalSite} tenantEmail={tenantEmail} />
                </>
              ) : (
                <div className="space-y-2">
                  <p className="text-xs text-ink-3">Registra el domini extern per generar els snippets d'integració i enviar-los per email.</p>
                  <div className="flex gap-2 flex-wrap">
                    <input type="text" placeholder="domini-client.com" value={extDomain} onChange={e => setExtDomain(e.target.value)}
                      className="flex-1 min-w-[140px] bg-bg-2 border border-border-base rounded px-3 py-1.5 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-accent" />
                    <input type="text" placeholder="URL de gràcies (opcional)" value={extRedirect} onChange={e => setExtRedirect(e.target.value)}
                      className="flex-1 min-w-[160px] bg-bg-2 border border-border-base rounded px-3 py-1.5 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-accent" />
                    <AMGButton size="sm" disabled={!extDomain.trim() || creatingExt} loading={creatingExt} onClick={handleCreateExternal}>
                      Generar snippets
                    </AMGButton>
                  </div>
                </div>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function ServiceCatalogTable({ services }: { services: CatalogService[] }) {
  const [query, setQuery] = useState('');
  const [typeFilter, setTypeFilter] = useState<'ALL' | 'RECURRING' | 'ONE_TIME'>('ALL');
  const [addonOnly, setAddonOnly] = useState(false);

  const filtered = services.filter((s) => {
    const q = query.toLowerCase();
    const matchesQuery = !q || s.name.toLowerCase().includes(q) || s.slug.toLowerCase().includes(q);
    const matchesType = typeFilter === 'ALL' || s.type === typeFilter;
    const matchesAddon = !addonOnly || s.isAddon;
    return matchesQuery && matchesType && matchesAddon;
  });

  const filterBtn = (label: string, active: boolean, onClick: () => void) => (
    <button type="button" onClick={onClick}
      className={`px-3 py-1.5 rounded text-xs f-mono transition border ${
        active
          ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.12)] text-white'
          : 'border-border-base text-ink-2 hover:border-ink-2'
      }`}>
      {label}
    </button>
  );

  return (
    <div>
      {/* Toolbar */}
      <div className="flex flex-wrap items-center gap-3 px-4 sm:px-5 py-3 border-b border-border-base">
        <div className="relative flex-1 min-w-[180px] max-w-xs">
          <IconSet.Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-3" />
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Cercar servei..."
            className="w-full pl-8 pr-3 py-1.5 bg-[rgba(255,255,255,0.04)] border border-border-base rounded text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00] placeholder:text-ink-3"
          />
        </div>
        <div className="flex items-center gap-2">
          {filterBtn('Tots', typeFilter === 'ALL', () => setTypeFilter('ALL'))}
          {filterBtn('Recurrent', typeFilter === 'RECURRING', () => setTypeFilter('RECURRING'))}
          {filterBtn('Únic', typeFilter === 'ONE_TIME', () => setTypeFilter('ONE_TIME'))}
          {filterBtn('Addon', addonOnly, () => setAddonOnly(v => !v))}
        </div>
        {(query || typeFilter !== 'ALL' || addonOnly) && (
          <span className="f-mono text-xs text-ink-3">{filtered.length} / {services.length}</span>
        )}
      </div>

      {filtered.length === 0 ? (
        <p className="text-sm text-ink-2 px-5 py-6">Cap servei coincideix amb el filtre.</p>
      ) : (
        <table className="w-full min-w-[500px]">
          <thead>
            <tr className="border-b border-border-base">
              {['Servei', 'Tipus', 'Preu venda', 'Addon'].map((h) => (
                <th key={h} className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {filtered.map((s) => (
              <tr key={s.id} className="border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,255,255,0.02)] transition-colors">
                <td className="px-4 sm:px-5 py-3">
                  <div className="f-display font-bold text-sm">{s.name}</div>
                  <div className="f-mono text-xs text-ink-3 mt-0.5">{s.slug}</div>
                </td>
                <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-2 capitalize">{s.type.toLowerCase()}</td>
                <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{s.salePrice.toFixed(2)} €</td>
                <td className="px-4 sm:px-5 py-3">{s.isAddon ? <AMGBadge tone="info">Addon</AMGBadge> : '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

function ServiceToggle({ tenantId, serviceId, enabled, onToggle }: {
  tenantId: string; serviceId: string; enabled: boolean; onToggle: () => void;
}) {
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();

  const handleToggle = async () => {
    setLoading(true);
    try {
      await toggleTenantService(tenantId, serviceId);
      onToggle();
    } catch {
      toast('error', 'Error canviant l\'estat del servei');
    } finally {
      setLoading(false);
    }
  };

  return (
    <button
      type="button"
      onClick={handleToggle}
      disabled={loading}
      title={enabled ? 'Desactivar servei' : 'Activar servei'}
      className={`flex-shrink-0 w-9 h-5 rounded-full transition-colors relative ${
        enabled ? 'bg-[#FF6B00]' : 'bg-[rgba(255,255,255,0.12)]'
      } ${loading ? 'opacity-50 cursor-not-allowed' : ''}`}
    >
      <div className={`absolute top-0.5 w-4 h-4 rounded-full bg-white shadow transition-all ${enabled ? 'left-4' : 'left-0.5'}`} />
    </button>
  );
}

type NexeServiceItem =
  | { name: string; action: 'scroll'; sectionId: string }
  | { name: string; action: 'navigate'; configKey: string };

const NEXE_PHASE_SERVICES: Record<string, NexeServiceItem[]> = {
  F1: [
    { name: 'Bot IA & Canals', action: 'scroll', sectionId: 'section-agent-config' },
    { name: 'Telegram Bot', action: 'scroll', sectionId: 'section-telegram' },
    { name: 'WhatsApp Business', action: 'scroll', sectionId: 'section-whatsapp' },
  ],
  F2: [
    { name: 'Bot IA & Canals', action: 'scroll', sectionId: 'section-agent-config' },
    { name: 'Telegram Bot', action: 'scroll', sectionId: 'section-telegram' },
    { name: 'Gestió de Cites', action: 'navigate', configKey: 'agenda' },
  ],
  F3: [
    { name: 'Bot IA & Canals', action: 'scroll', sectionId: 'section-agent-config' },
    { name: 'Pressupostos', action: 'scroll', sectionId: 'section-budgets' },
    { name: 'Config Pressupostos', action: 'navigate', configKey: 'pressupostos' },
  ],
  F4: [
    { name: 'Bot IA & Canals', action: 'scroll', sectionId: 'section-agent-config' },
    { name: 'WhatsApp Business', action: 'scroll', sectionId: 'section-whatsapp' },
    { name: 'Seguiment', action: 'navigate', configKey: 'fidelitzacio' },
  ],
  F5: [
    { name: 'Bot IA & Canals', action: 'scroll', sectionId: 'section-agent-config' },
    { name: "Gestió d'Equip", action: 'navigate', configKey: 'equip' },
  ],
};

const FIELD_LABELS: Record<string, string> = {
  api_key: 'API Key', bot_token: 'Bot Token', telegram_bot_token: 'Token Telegram',
  phone_number: 'Número telèfon', account_id: 'Account ID', access_token: 'Access Token',
  phone_number_id: 'Phone Number ID', smtp_host: 'Servidor SMTP', smtp_user: 'Usuari SMTP',
  smtp_password: 'Contrasenya SMTP', username: 'Usuari', bot_username: 'Usuari bot',
  waba_id: 'WABA ID', client_id: 'Client ID', client_secret: 'Client Secret',
  analytics_id: 'Analytics ID', webhook_url: 'Webhook URL',
};

function getRequiredCredentials(slug: string, serviceType: string): string[] {
  const wizard = getWizardConfig(slug, serviceType);
  if (!wizard) return [];
  return wizard.steps
    .filter(s => s.type === 'credentials')
    .flatMap(s => s.fields?.filter(f => f.required) ?? [])
    .map(f => FIELD_LABELS[f.id] ?? f.id.replace(/_/g, ' '));
}

function SetupSection({ setup, tenantId, contractedPhases, activePhases, onRefresh, onRemovePhase, onTogglePhase }: {
  setup: TenantSetup;
  tenantId: string;
  contractedPhases?: string[] | null;
  activePhases?: string[] | null;
  onRefresh: () => void;
  onRemovePhase?: (phase: string) => void;
  onTogglePhase?: (phase: string, enable: boolean) => void;
}) {
  const hasProfiles = setup.profiles.length > 0;
  const hasAddons = setup.addons.length > 0;
  const hasStandalone = (setup.standalone?.length ?? 0) > 0;
  const hasNexePhases = (contractedPhases?.length ?? 0) > 0;
  const { toast } = useToast();
  const router = useRouter();
  const { locale } = useParams<{ locale: string }>();
  const [removingId, setRemovingId] = useState<string | null>(null);
  const [removingPhase, setRemovingPhase] = useState<string | null>(null);

  const handleRemove = async (tenantServiceId: string) => {
    setRemovingId(tenantServiceId);
    try {
      await removeTenantService(tenantId, tenantServiceId);
      onRefresh();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      toast('error', `Error eliminant el servei: ${msg}`);
    } finally {
      setRemovingId(null);
    }
  };

  const handleRemovePhase = async (phase: string) => {
    if (!confirm(`Eliminar la fase ${phase} del contracte? Aquesta acció no es pot desfer.`)) return;
    setRemovingPhase(phase);
    try {
      onRemovePhase?.(phase);
    } finally {
      setRemovingPhase(null);
    }
  };

  if (!hasNexePhases && !hasProfiles && !hasAddons && !hasStandalone) {
    return (
      <div className="p-8 text-center">
        <IconSet.Box size={28} stroke="#64748b" className="mx-auto mb-3" />
        <div className="f-display font-bold text-sm mb-1">Cap servei assignat</div>
        <p className="f-mono text-xs text-ink-2">Aquest tenant encara no té fases ni serveis assignats</p>
      </div>
    );
  }

  const ServiceRow = ({ svc }: { svc: TenantSetup['profiles'][0]['phases'][0]['services'][0] }) => {
    const isPending = svc.status === 'PENDING' || svc.status === 'CONFIGURING' || svc.status === 'AWAITING_CLIENT';
    const wizard = getWizardConfig(svc.service.slug, svc.service.type);
    const requiredCreds = isPending ? getRequiredCredentials(svc.service.slug, svc.service.type) : [];
    return (
      <div className={`p-3 border border-border-base rounded transition-opacity ${!svc.isEnabled ? 'opacity-40' : ''}`}>
        <div className="flex items-center gap-2 flex-wrap">
          <ServiceToggle tenantId={tenantId} serviceId={svc.service.id} enabled={svc.isEnabled} onToggle={onRefresh} />
          <span className="text-sm font-medium text-ink-1 flex-1 min-w-0 truncate">{svc.service.name}</span>
          <span className="f-mono text-[10px] text-ink-3 uppercase">{svc.service.type}</span>
          {statusBadge(svc.status, 'Actiu', 'Inactiu')}
          <div className="flex items-center gap-1.5 ml-auto flex-shrink-0">
            <a
              href={`/portal/admin/tenants/${tenantId}/services/${svc.service.id}/setup`}
              title="Configurar / gestionar"
              className={`flex items-center gap-1 px-2 py-1 rounded text-[10px] f-mono border transition ${
                wizard
                  ? 'border-[rgba(255,107,0,0.5)] text-accent-light hover:bg-[rgba(255,107,0,0.12)]'
                  : 'border-border-base text-ink-2 hover:border-ink-2 hover:text-ink-1'
              }`}
            >
              <IconSet.Settings size={11} />
              {isPending ? 'Configurar' : 'Gestionar'}
            </a>
            <button
              type="button"
              onClick={() => handleRemove(svc.tenantServiceId)}
              disabled={removingId === svc.tenantServiceId}
              title="Eliminar servei"
              className="p-1 rounded text-ink-3 hover:text-red-400 hover:bg-[rgba(239,68,68,0.08)] transition disabled:opacity-40"
            >
              {removingId === svc.tenantServiceId
                ? <span className="w-3 h-3 border border-current border-t-transparent rounded-full animate-spin inline-block" />
                : <IconSet.Trash size={13} />}
            </button>
          </div>
        </div>
        {requiredCreds.length > 0 && (
          <div className="mt-2 flex flex-wrap gap-1.5 pl-1">
            <span className="f-mono text-[10px] text-amber-400 flex items-center gap-1">
              <IconSet.Key size={9} /> Claus pendents:
            </span>
            {requiredCreds.map(c => (
              <span key={c} className="f-mono text-[10px] px-1.5 py-0.5 rounded border border-amber-500/30 bg-amber-500/8 text-amber-400">
                {c}
              </span>
            ))}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="space-y-5 p-5">

      {/* Fases NexeLocal contractades */}
      {hasNexePhases && (
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <div className="f-mono text-label uppercase tracking-widest text-ink-3 text-[10px]">Fases NexeLocal contractades</div>
            <button
              type="button"
              onClick={() => router.push(`/${locale}/portal/admin/tenants/${tenantId}/setup`)}
              className="flex items-center gap-1.5 px-3 py-1 f-mono text-xs font-semibold text-accent-light border border-[rgba(255,107,0,0.5)] bg-[rgba(255,107,0,0.06)] hover:bg-[rgba(255,107,0,0.15)] rounded transition"
            >
              <IconSet.Zap size={11} /> Posar en marxa →
            </button>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {(contractedPhases ?? []).sort().map((phase) => {
              const phaseNum = parseInt(phase.replace('F', ''));
              const phaseName = NEXE_PHASE_NAMES[phaseNum] ?? phase;
              const phaseServices = NEXE_PHASE_SERVICES[phase] ?? [];
              const isActive = (activePhases ?? contractedPhases ?? []).includes(phase);
              return (
                <div key={phase} className={`border rounded p-3 space-y-2 transition-opacity ${isActive ? 'border-border-base' : 'border-border-base opacity-50'}`}>
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <span className={`f-mono text-xs font-bold px-2 py-0.5 rounded border ${isActive ? 'border-[rgba(255,107,0,0.4)] bg-[rgba(255,107,0,0.08)] text-accent-light' : 'border-border-base text-ink-3'}`}>
                        {phase}
                      </span>
                      <span className="text-sm font-medium text-ink-1">{phaseName}</span>
                      {!isActive && <span className="f-mono text-[10px] text-ink-3 uppercase">inactiva</span>}
                    </div>
                    <div className="flex items-center gap-1 flex-shrink-0">
                      {/* Toggle activa/desactiva */}
                      <button
                        type="button"
                        onClick={() => onTogglePhase?.(phase, !isActive)}
                        title={isActive ? `Desactivar ${phase}` : `Activar ${phase}`}
                        className={`p-1 rounded transition ${isActive ? 'text-green-400 hover:text-green-300 hover:bg-green-500/10' : 'text-ink-3 hover:text-green-400 hover:bg-green-500/10'}`}
                      >
                        <IconSet.Power size={13} />
                      </button>
                      {/* Eliminar del contracte */}
                      <button
                        type="button"
                        onClick={() => handleRemovePhase(phase)}
                        disabled={removingPhase === phase}
                        title={`Eliminar ${phase} del contracte`}
                        className="p-1 rounded text-ink-3 hover:text-red-400 hover:bg-[rgba(239,68,68,0.08)] transition disabled:opacity-40"
                      >
                        {removingPhase === phase
                          ? <span className="w-3 h-3 border border-current border-t-transparent rounded-full animate-spin inline-block" />
                          : <IconSet.Trash size={13} />}
                      </button>
                    </div>
                  </div>
                  {phaseServices.length > 0 && (
                    <div className="space-y-1.5">
                      {phaseServices.map(svcItem => (
                        <div key={svcItem.name} className="flex items-center justify-between gap-2 px-2 py-1.5 bg-[rgba(255,255,255,0.02)] border border-border-base rounded">
                          <span className="f-mono text-[10px] text-ink-2">{svcItem.name}</span>
                          <button
                            type="button"
                            onClick={() => {
                              if (svcItem.action === 'scroll') {
                                const el = document.getElementById(svcItem.sectionId);
                                if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
                              } else {
                                router.push(`/portal/admin/tenants/${tenantId}/nexe/${svcItem.configKey}`);
                              }
                            }}
                            className={`flex items-center gap-1 px-2 py-0.5 rounded text-[10px] f-mono border transition flex-shrink-0 ${
                              svcItem.action === 'navigate'
                                ? 'border-[rgba(255,107,0,0.6)] text-accent-light bg-[rgba(255,107,0,0.06)] hover:bg-[rgba(255,107,0,0.15)]'
                                : 'border-[rgba(255,107,0,0.4)] text-accent-light hover:bg-[rgba(255,107,0,0.1)]'
                            }`}
                          >
                            <IconSet.Settings size={10} />
                            {svcItem.action === 'navigate' ? 'Configurar' : 'Gestionar'}
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Serveis de catàleg per perfil */}
      {hasProfiles && (
        <div className="space-y-4">
          {hasNexePhases && <div className="f-mono text-label uppercase tracking-widest text-ink-3 text-[10px]">Serveis del catàleg</div>}
          {setup.profiles.map((p) => (
            <div key={p.profile.id} className="border border-border-base rounded p-4 space-y-3">
              <div className="flex items-center gap-2">
                <IconSet.Box size={14} className="text-accent-light" />
                <span className="f-display font-bold text-sm">{p.profile.name}</span>
              </div>
              {p.phases.map((ph) => (
                <div key={ph.phase.id} className="ml-5 border-l-2 border-border-base pl-4 space-y-2">
                  <div className="flex items-center gap-2">
                    <span className="f-mono text-label uppercase text-ink-3">{ph.phase.name}</span>
                    {statusBadge(ph.approvalStatus, 'Aprovat', 'Pendent')}
                  </div>
                  {ph.services.map((svc) => (
                    <ServiceRow key={svc.tenantServiceId} svc={svc} />
                  ))}
                </div>
              ))}
            </div>
          ))}
        </div>
      )}

      {/* Serveis individuals */}
      {hasStandalone && (
        <div className="space-y-2">
          {(hasNexePhases || hasProfiles) && <div className="f-mono text-label uppercase tracking-widest text-ink-3 text-[10px]">Serveis individuals</div>}
          <div className="border border-border-base rounded p-4 space-y-2">
            <div className="flex items-center gap-2 mb-1">
              <IconSet.Zap size={14} className="text-accent-light" />
              <span className="f-display font-bold text-sm">Serveis individuals</span>
            </div>
            {setup.standalone!.map((svc) => (
              <ServiceRow key={svc.tenantServiceId} svc={svc} />
            ))}
          </div>
        </div>
      )}

      {/* Add-ons */}
      {hasAddons && (
        <div className="border border-border-base rounded p-4 space-y-2">
          <div className="flex items-center gap-2 mb-1">
            <IconSet.Plus size={14} className="text-accent-light" />
            <span className="f-display font-bold text-sm">Add-ons</span>
          </div>
          {setup.addons.map((a) => (
            <div key={a.service.id} className="flex items-center gap-2 px-3 py-2 border border-border-base rounded">
              <span className="w-1.5 h-1.5 rounded-full bg-accent flex-shrink-0" />
              <span className="text-sm text-ink-1 flex-1">{a.service.name}</span>
              {statusBadge(a.approvalStatus, 'Aprovat', 'No aprovat')}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function AssignProfileModal({ tenantId, onClose, onAssigned }: { tenantId: string; onClose: () => void; onAssigned: () => void }) {
  const { toast } = useToast();
  const { data: profiles, isLoading } = useQuery({
    queryKey: ['vault-profiles'],
    queryFn: () => listProfiles(),
  });

  const [selectedProfileId, setSelectedProfileId] = useState('');
  const [assigning, setAssigning] = useState(false);

  const activeProfiles = profiles?.filter(p => p.isActive) ?? [];

  const handleAssign = async () => {
    if (!selectedProfileId) return;
    setAssigning(true);
    try {
      const result = await assignProfileToTenant(tenantId, selectedProfileId);
      toast('success', `Perfil assignat — ${result.phases.length} fases, total ${result.totalPrice.toFixed(2)} €`);
      onAssigned();
      onClose();
    } catch {
      toast('error', 'Error assignant el perfil');
    } finally {
      setAssigning(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-md p-6 space-y-4" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Assignar perfil</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><IconSet.X size={18} /></button>
        </div>
        {isLoading ? (
          <div className="flex justify-center py-8">
            <span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          </div>
        ) : activeProfiles.length === 0 ? (
          <div className="text-center py-6">
            <IconSet.Box size={24} stroke="#64748b" className="mx-auto mb-2" />
            <div className="f-display font-bold text-sm mb-1">Cap perfil disponible</div>
            <p className="f-mono text-xs text-ink-2">Crea perfils des de la secció Catàleg</p>
          </div>
        ) : (
          <div className="space-y-3">
            <p className="text-sm text-ink-2">Selecciona un perfil per assignar-lo al tenant:</p>
            {activeProfiles.map((p: CatalogProfileResponse) => (
              <button key={p.id} onClick={() => setSelectedProfileId(p.id)}
                className={`w-full text-left p-3 border rounded transition ${
                  selectedProfileId === p.id
                    ? 'border-[#FF6B00] bg-accent-muted'
                    : 'border-border-base hover:border-ink-2'
                }`}>
                <div className="f-display font-bold text-sm">{p.name}</div>
                {p.description && <div className="f-mono text-xs text-ink-3 mt-0.5">{p.description}</div>}
                <div className="f-mono text-xs text-ink-2 mt-1">{p.phases?.length ?? 0} fases</div>
              </button>
            ))}
            <div className="flex gap-3 pt-2">
              <AMGButton onClick={handleAssign} disabled={!selectedProfileId || assigning} loading={assigning} className="flex-1 justify-center">
                Assignar perfil
              </AMGButton>
              <AMGButton variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function AddPhaseModal({ tenantId, onClose, onAdded }: { tenantId: string; onClose: () => void; onAdded: () => void }) {
  const { toast } = useToast();
  const { data: profiles, isLoading } = useQuery({
    queryKey: ['vault-profiles'],
    queryFn: () => listProfiles(),
  });
  const [selectedProfileId, setSelectedProfileId] = useState('');
  const [selectedPhaseIds, setSelectedPhaseIds] = useState<Set<string>>(new Set());
  const [adding, setAdding] = useState(false);

  const activeProfiles = profiles?.filter(p => p.isActive) ?? [];
  const selectedProfile = activeProfiles.find(p => p.id === selectedProfileId);

  const togglePhase = (id: string) => setSelectedPhaseIds(prev => {
    const next = new Set(prev);
    next.has(id) ? next.delete(id) : next.add(id);
    return next;
  });

  const handleAdd = async () => {
    if (selectedPhaseIds.size === 0) { toast('error', 'Selecciona almenys una fase'); return; }
    setAdding(true);
    try {
      await Promise.all(Array.from(selectedPhaseIds).map(phaseId => assignPhaseToTenant(tenantId, phaseId)));
      toast('success', `${selectedPhaseIds.size} fase${selectedPhaseIds.size > 1 ? 's' : ''} afegida${selectedPhaseIds.size > 1 ? 's' : ''}`);
      onAdded(); onClose();
    } catch {
      toast('error', 'Error afegint fases');
    } finally {
      setAdding(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-md p-6 space-y-4 max-h-[85vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Afegir fases</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><IconSet.X size={18} /></button>
        </div>
        {isLoading ? (
          <div className="flex justify-center py-8"><span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" /></div>
        ) : activeProfiles.length === 0 ? (
          <p className="text-sm text-ink-3 text-center py-6">Cap perfil disponible al catàleg</p>
        ) : (
          <div className="space-y-4">
            <div>
              <label className="f-mono text-label uppercase text-ink-2 block mb-2">Perfil</label>
              <div className="space-y-2">
                {activeProfiles.map(p => (
                  <button key={p.id} type="button" onClick={() => { setSelectedProfileId(p.id); setSelectedPhaseIds(new Set()); }}
                    className={`w-full text-left p-3 border rounded transition text-sm ${
                      selectedProfileId === p.id ? 'border-[#FF6B00] bg-accent-muted' : 'border-border-base hover:border-ink-2'
                    }`}>
                    <span className="font-semibold">{p.name}</span>
                    <span className="text-ink-3 ml-2 text-xs">{p.phases?.length ?? 0} fases</span>
                  </button>
                ))}
              </div>
            </div>
            {selectedProfile && (
              <div>
                <label className="f-mono text-label uppercase text-ink-2 block mb-2">Fases</label>
                <div className="space-y-2">
                  {selectedProfile.phases.map((ph: CatalogPhaseResponse) => (
                    <label key={ph.id} className="flex items-center gap-3 p-3 border border-border-base rounded cursor-pointer hover:border-ink-2 transition">
                      <input type="checkbox" checked={selectedPhaseIds.has(ph.id)} onChange={() => togglePhase(ph.id)} className="accent-[#FF6B00]" />
                      <div className="flex-1">
                        <span className="text-sm">{ph.name}</span>
                        <span className="f-mono text-xs text-ink-3 ml-2">{ph.services.length} serveis</span>
                      </div>
                    </label>
                  ))}
                </div>
              </div>
            )}
            <div className="flex gap-3 pt-2">
              <AMGButton onClick={handleAdd} disabled={selectedPhaseIds.size === 0 || adding} loading={adding} className="flex-1 justify-center">
                Afegir fases ({selectedPhaseIds.size})
              </AMGButton>
              <AMGButton variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function AddServiceModal({ tenantId, onClose, onAdded }: { tenantId: string; onClose: () => void; onAdded: () => void }) {
  const { toast } = useToast();
  const { data: services, isLoading } = useQuery({
    queryKey: ['catalog-services'],
    queryFn: () => listCatalogServices(),
  });
  const [query, setQuery] = useState('');
  const [selectedId, setSelectedId] = useState('');
  const [adding, setAdding] = useState(false);

  const filtered = (services ?? []).filter(s => !query || s.name.toLowerCase().includes(query.toLowerCase()) || s.slug.toLowerCase().includes(query.toLowerCase()));

  const handleAdd = async () => {
    if (!selectedId) return;
    setAdding(true);
    try {
      await addStandaloneServiceToTenant(tenantId, selectedId);
      const name = services?.find(s => s.id === selectedId)?.name ?? 'Servei';
      toast('success', `${name} afegit`);
      onAdded(); onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error desconegut';
      toast('error', `Error afegint el servei: ${msg}`);
    } finally {
      setAdding(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-md p-6 space-y-4 max-h-[85vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Afegir servei individual</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><IconSet.X size={18} /></button>
        </div>
        {isLoading ? (
          <div className="flex justify-center py-8"><span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" /></div>
        ) : (
          <div className="space-y-3">
            <div className="relative">
              <IconSet.Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-3" />
              <input type="text" value={query} onChange={e => setQuery(e.target.value)}
                placeholder="Cercar servei..."
                className="w-full pl-8 pr-3 py-1.5 bg-[rgba(255,255,255,0.04)] border border-border-base rounded text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00] placeholder:text-ink-3" />
            </div>
            <div className="space-y-1.5 max-h-64 overflow-y-auto">
              {filtered.map(s => (
                <button key={s.id} type="button" onClick={() => setSelectedId(s.id)}
                  className={`w-full text-left p-2.5 border rounded transition ${
                    selectedId === s.id ? 'border-[#FF6B00] bg-accent-muted' : 'border-border-base hover:border-ink-2'
                  }`}>
                  <div className="text-sm font-semibold">{s.name}</div>
                  <div className="f-mono text-[10px] text-ink-3 mt-0.5">{s.slug} · {s.type.toLowerCase()} · {s.salePrice.toFixed(2)} €</div>
                </button>
              ))}
              {filtered.length === 0 && <p className="text-sm text-ink-3 text-center py-4">Cap servei trobat</p>}
            </div>
            <div className="flex gap-3 pt-2">
              <AMGButton onClick={handleAdd} disabled={!selectedId || adding} loading={adding} className="flex-1 justify-center">
                Afegir servei
              </AMGButton>
              <AMGButton variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function ContractSection({ tenant, onRefresh }: { tenant: TenantResponse; onRefresh: () => void }) {
  const { toast } = useToast();
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editSector, setEditSector] = useState(tenant.sector ?? '');
  const [editSize, setEditSize] = useState(tenant.businessSize ?? '');

  const { data: pricing } = useQuery({
    queryKey: ['pricing', tenant.sector, tenant.businessSize],
    queryFn: () => lookupSectorPricing(tenant.sector!, tenant.businessSize!),
    enabled: !!tenant.sector && !!tenant.businessSize,
  });

  const { data: editPricing } = useQuery({
    queryKey: ['pricing', editSector, editSize],
    queryFn: () => lookupSectorPricing(editSector, editSize),
    enabled: !!editSector && !!editSize,
  });

  const phases = tenant.contractedPhases ?? [];
  const phaseCount = phases.length;
  const monthlyPrice = pricing && phaseCount > 0 ? calcMonthly(pricing, phaseCount) : null;

  const handleSave = async () => {
    setSaving(true);
    try {
      await updateTenant(tenant.id, { sector: editSector || null, businessSize: editSize || null });
      toast('success', 'Contracte actualitzat');
      onRefresh();
      setEditing(false);
    } catch {
      toast('error', 'Error desant els canvis');
    } finally {
      setSaving(false);
    }
  };

  const availableSizes = editSector ? (SECTOR_SIZES[editSector] ?? []) : [];
  const lbl = 'f-mono text-[10px] uppercase tracking-wider text-ink-3 block mb-1.5';

  return (
    <div className="amg-card card-clip">
      <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
        <AMGSectionTitle eyebrow="Grandària" title="Contracte" />
        {!editing ? (
          <AMGButton size="sm" variant="ghost" icon={IconSet.Edit} onClick={() => { setEditSector(tenant.sector ?? ''); setEditSize(tenant.businessSize ?? ''); setEditing(true); }}>
            Editar
          </AMGButton>
        ) : (
          <div className="flex gap-2">
            <AMGButton size="sm" variant="ghost" onClick={() => setEditing(false)}>Cancel·lar</AMGButton>
            <AMGButton size="sm" loading={saving} onClick={handleSave}>Desar</AMGButton>
          </div>
        )}
      </div>
      <div className="p-5 space-y-4">
        {editing ? (
          <div className="space-y-4">
            <div>
              <label className={lbl}>Sector</label>
              <div className="grid grid-cols-3 gap-1.5 max-h-48 overflow-y-auto pr-1">
                {(Object.keys(SECTOR_LABELS) as string[]).map(k => (
                  <button key={k} type="button"
                    onClick={() => { setEditSector(k); setEditSize(''); }}
                    className={`px-2 py-2 text-xs border rounded text-center transition leading-tight ${editSector === k ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.12)] text-white font-semibold' : 'border-border-base text-ink-2 hover:border-ink-2'}`}>
                    {SECTOR_LABELS[k]}
                  </button>
                ))}
              </div>
            </div>
            {editSector && availableSizes.length > 0 && (
              <div>
                <label className={lbl}>Mida de negoci</label>
                <div className="flex gap-2 flex-wrap">
                  {availableSizes.map(sz => (
                    <button key={sz} type="button"
                      onClick={() => setEditSize(sz)}
                      className={`px-4 py-2 text-sm border rounded transition ${editSize === sz ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.12)] text-white font-semibold' : 'border-border-base text-ink-2 hover:border-ink-2'}`}>
                      {SIZE_LABELS[sz] ?? sz}
                    </button>
                  ))}
                </div>
              </div>
            )}
            {editPricing && editSector && editSize && (
              <div className="bg-[rgba(255,107,0,0.06)] border border-[rgba(255,107,0,0.2)] rounded p-3 flex gap-6 flex-wrap">
                <div>
                  <div className="f-mono text-[10px] text-ink-3 uppercase">Setup</div>
                  <div className="f-display font-bold text-white">{editPricing.setupPrice} €</div>
                </div>
                <div>
                  <div className="f-mono text-[10px] text-ink-3 uppercase">1 fase/mes</div>
                  <div className="f-display font-bold text-accent-light">{editPricing.priceF1} €</div>
                </div>
                <div>
                  <div className="f-mono text-[10px] text-ink-3 uppercase">2 fases/mes</div>
                  <div className="f-display font-bold text-accent-light">{editPricing.priceF1 + editPricing.priceF2} €</div>
                </div>
              </div>
            )}
          </div>
        ) : (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <div className="f-mono text-label uppercase text-ink-3 mb-1">Sector</div>
                <div className="text-sm text-ink-1 font-semibold">
                  {tenant.sector ? (SECTOR_LABELS[tenant.sector] ?? tenant.sector) : <span className="text-ink-3 italic">Sense sector</span>}
                </div>
              </div>
              <div>
                <div className="f-mono text-label uppercase text-ink-3 mb-1">Mida</div>
                <div className="text-sm text-ink-1 font-semibold">
                  {tenant.businessSize ? (SIZE_LABELS[tenant.businessSize] ?? tenant.businessSize) : <span className="text-ink-3 italic">Sense mida</span>}
                </div>
              </div>
              {phaseCount > 0 && (
                <div>
                  <div className="f-mono text-label uppercase text-ink-3 mb-1">
                    Fases contractades <span className="normal-case">({phaseCount})</span>
                  </div>
                  <div className="flex flex-wrap gap-1.5 mt-1">
                    {phases.sort().map((ph) => (
                      <span key={ph} className="f-mono text-xs px-2 py-0.5 border border-[rgba(255,107,0,0.4)] bg-[rgba(255,107,0,0.08)] text-accent-light rounded">
                        {ph}
                      </span>
                    ))}
                  </div>
                  <div className="f-mono text-[10px] text-ink-3 mt-1">
                    {phases.sort().map(ph => PHASE_LABELS[ph]?.split(' — ')[1]).filter(Boolean).join(' · ')}
                  </div>
                </div>
              )}
            </div>
            {pricing && (
              <div className="border-t border-border-base pt-4 flex gap-8 flex-wrap">
                <div>
                  <div className="f-mono text-label uppercase text-ink-3 mb-1">Setup</div>
                  <div className="f-display font-bold text-lg text-white">{pricing.setupPrice} €</div>
                </div>
                {monthlyPrice !== null && (
                  <div>
                    <div className="f-mono text-label uppercase text-ink-3 mb-1">Mensual ({phaseCount} fase{phaseCount > 1 ? 's' : ''})</div>
                    <div className="f-display font-bold text-lg text-accent-light">{monthlyPrice} €/mes</div>
                  </div>
                )}
                <div className="self-end">
                  <div className="f-mono text-[10px] text-ink-3">Ampliació futura: <span className="text-ink-2 font-semibold">{PHASE_UPGRADE_PRICE} € / fase</span></div>
                </div>
              </div>
            )}
            {tenant.agentSystemPrompt && (
              <div className="border-t border-border-base pt-4">
                <div className="f-mono text-label uppercase text-ink-3 mb-2">Prompt agent IA</div>
                <pre className="text-xs f-mono text-ink-2 whitespace-pre-wrap bg-[rgba(255,255,255,0.02)] border border-border-base rounded p-3 max-h-48 overflow-y-auto">
                  {tenant.agentSystemPrompt}
                </pre>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

function LifecycleSection({ tenant, onRefresh }: { tenant: TenantResponse; onRefresh: () => void }) {
  const { toast } = useToast();
  const [saving, setSaving] = useState(false);
  
  const handleDeliver = async () => {
    if (!confirm('Estàs segur que vols marcar la implementació com a lliurada?')) return;
    setSaving(true);
    try {
      await markImplementationDelivered(tenant.id);
      toast('success', 'Implementació marcada com a lliurada');
      onRefresh();
    } catch {
      toast('error', 'Error al actualitzar estat');
    } finally {
      setSaving(false);
    }
  };

  const handleOnboarding = async () => {
    if (!confirm("Estàs segur que vols marcar l'onboarding com a completat?")) return;
    setSaving(true);
    try {
      await markOnboardingCompleted(tenant.id);
      toast('success', 'Onboarding marcat com a completat');
      onRefresh();
    } catch {
      toast('error', 'Error al actualitzar estat');
    } finally {
      setSaving(false);
    }
  };

  const handleSetBillingDate = async () => {
    const d = prompt("Introdueix la data d'inici de facturació (YYYY-MM-DD):", tenant.billingStartDate?.split('T')[0] ?? new Date().toISOString().split('T')[0]);
    if (!d) return;
    setSaving(true);
    try {
      await setBillingStartDate(tenant.id, d);
      toast('success', 'Data de facturació actualitzada');
      onRefresh();
    } catch {
      toast('error', 'Error al actualitzar la data');
    } finally {
      setSaving(false);
    }
  };

  const fDate = (d: string | null) => d ? new Date(d).toLocaleDateString('ca-ES') : '--';

  return (
    <div className="p-4 sm:p-5">
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="border border-border-base rounded p-4 flex flex-col items-start gap-3 bg-bg-0">
          <div>
            <div className="f-mono text-label uppercase tracking-wider text-ink-3">Data d'inici facturació</div>
            <div className="text-sm font-semibold text-ink-0 mt-1">{fDate(tenant.billingStartDate)}</div>
          </div>
          <AMGButton size="sm" variant="ghost" disabled={saving} onClick={handleSetBillingDate}>Establir data</AMGButton>
        </div>
        
        <div className="border border-border-base rounded p-4 flex flex-col items-start gap-3 bg-bg-0">
          <div>
            <div className="f-mono text-label uppercase tracking-wider text-ink-3">Implementació Lliurada</div>
            <div className="text-sm font-semibold text-ink-0 mt-1">{fDate(tenant.implementationDeliveredAt)}</div>
          </div>
          <AMGButton size="sm" variant={tenant.implementationDeliveredAt ? "ghost" : "primary"} disabled={saving || !!tenant.implementationDeliveredAt} onClick={handleDeliver}>
            {tenant.implementationDeliveredAt ? 'Lliurat' : 'Marcar Lliurat'}
          </AMGButton>
        </div>

        <div className="border border-border-base rounded p-4 flex flex-col items-start gap-3 bg-bg-0">
          <div>
            <div className="f-mono text-label uppercase tracking-wider text-ink-3">Onboarding Completat</div>
            <div className="text-sm font-semibold text-ink-0 mt-1">{fDate(tenant.onboardingCompletedAt)}</div>
          </div>
          <AMGButton size="sm" variant={tenant.onboardingCompletedAt ? "ghost" : "primary"} disabled={saving || !!tenant.onboardingCompletedAt || !tenant.implementationDeliveredAt} onClick={handleOnboarding}>
            {tenant.onboardingCompletedAt ? 'Completat' : 'Marcar Completat'}
          </AMGButton>
        </div>
      </div>
    </div>
  );
}



const WA_STATUS_TONE: Record<string, 'success' | 'warning' | 'danger' | 'neutral'> = {
  CONNECTED: 'success', PENDING: 'warning', ERROR: 'danger', DISCONNECTED: 'neutral',
};
const WA_STATUS_LABEL: Record<string, string> = {
  CONNECTED: 'Connectat', PENDING: 'Pendent', ERROR: 'Error', DISCONNECTED: 'Desconnectat',
};

function TelegramBotCard({ tenantId }: { tenantId: string }) {
  const qc = useQueryClient();
  const { toast } = useToast();
  const [showForm, setShowForm] = useState(false);
  const [botToken, setBotToken] = useState('');
  const [saving, setSaving] = useState(false);
  const [verifying, setVerifying] = useState(false);

  const { data: config, isLoading } = useQuery({
    queryKey: ['tg-config', tenantId],
    queryFn: () => getTelegramConfig(tenantId).catch(() => null),
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ['tg-config', tenantId] });

  const handleConnect = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      await connectTelegram(tenantId, botToken);
      toast('success', 'Bot configurat i webhook registrat');
      setBotToken('');
      setShowForm(false);
      invalidate();
    } catch {
      toast('error', 'Error configurant el bot — comprova el token');
    } finally {
      setSaving(false);
    }
  };

  const handleVerify = async () => {
    setVerifying(true);
    try {
      await verifyTelegram(tenantId);
      toast('success', 'Webhook re-registrat correctament');
      invalidate();
    } catch {
      toast('error', 'Error verificant el bot — comprova que el token és vàlid');
    } finally {
      setVerifying(false);
    }
  };

  const handleDisconnect = async () => {
    if (!confirm('Eliminar la configuració del bot? El webhook deixarà de funcionar.')) return;
    try {
      await disconnectTelegram(tenantId);
      toast('success', 'Bot desconnectat');
      invalidate();
    } catch {
      toast('error', 'Error desconnectant el bot');
    }
  };

  const statusTone: Record<string, 'success' | 'warning' | 'danger' | 'neutral'> = {
    CONNECTED: 'success', PENDING: 'warning', ERROR: 'danger', DISCONNECTED: 'neutral',
  };

  return (
    <div className="amg-card card-clip">
      <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
        <AMGSectionTitle eyebrow="Missatgeria" title="Telegram Bot" />
        <div className="flex items-center gap-2">
          {config && (
            <AMGBadge tone={statusTone[config.status] ?? 'neutral'}>
              {WA_STATUS_LABEL[config.status] ?? config.status}
            </AMGBadge>
          )}
          <AMGButton size="sm" variant="ghost" onClick={() => setShowForm(v => !v)}>
            {config ? 'Editar' : 'Configurar'}
          </AMGButton>
        </div>
      </div>

      <div className="p-5 space-y-5">
        {showForm && (
          <form onSubmit={handleConnect} className="space-y-3 p-4 border border-border-base rounded bg-[rgba(255,255,255,0.02)]">
            <div className="f-mono text-label uppercase text-ink-3 text-xs tracking-widest mb-2">
              Token del bot (de @BotFather)
            </div>
            <div>
              <label className="f-mono text-xs text-ink-2 block mb-1">Bot Token *</label>
              <input
                type="password"
                required
                value={botToken}
                onChange={(e) => setBotToken(e.target.value)}
                placeholder="1234567890:ABCdefGHIjklMNOpqrsTUVwxyz"
                className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00]"
              />
              <p className="f-mono text-[10px] text-ink-3 mt-1">
                Crea el bot amb @BotFather a Telegram i copia el token aquí
              </p>
            </div>
            <div className="flex gap-2">
              <AMGButton type="submit" size="sm" loading={saving}>Desar i registrar webhook</AMGButton>
              <AMGButton type="button" size="sm" variant="ghost" onClick={() => setShowForm(false)}>Cancel·lar</AMGButton>
            </div>
          </form>
        )}

        {!isLoading && config && !showForm && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              {config.botUsername && (
                <div>
                  <div className="f-mono text-label uppercase text-ink-3 text-[10px]">Bot</div>
                  <div className="text-sm text-ink-1 font-semibold">@{config.botUsername}</div>
                </div>
              )}
              <div>
                <div className="f-mono text-label uppercase text-ink-3 text-[10px]">Webhook</div>
                <div className="text-sm text-ink-1">{config.webhookRegistered ? '✓ Registrat' : '✗ No registrat'}</div>
              </div>
              {config.connectedAt && (
                <div>
                  <div className="f-mono text-label uppercase text-ink-3 text-[10px]">Connectat el</div>
                  <div className="text-sm text-ink-1">{fmtDate(config.connectedAt)}</div>
                </div>
              )}
            </div>
            {config.botUsername && (
              <div className="f-mono text-[10px] text-ink-3">
                Enllaç del bot:{' '}
                <a href={`https://t.me/${config.botUsername}`} target="_blank" rel="noopener noreferrer"
                  className="text-accent-light hover:text-accent transition">
                  t.me/{config.botUsername}
                </a>
              </div>
            )}
            <div className="flex items-center gap-2 flex-wrap">
              {(config.status === 'ERROR' || !config.webhookRegistered) && (
                <AMGButton size="sm" icon={IconSet.Zap} onClick={handleVerify} loading={verifying}>
                  Re-registrar webhook
                </AMGButton>
              )}
              <AMGButton size="sm" variant="ghost" onClick={handleDisconnect}>
                Desconnectar
              </AMGButton>
            </div>
          </div>
        )}

        {!isLoading && !config && !showForm && (
          <div className="text-center py-6">
            <IconSet.Smartphone size={28} stroke="#64748b" className="mx-auto mb-3" />
            <p className="text-sm text-ink-2 mb-3">Cap bot de Telegram configurat per aquest tenant.</p>
            <p className="f-mono text-[10px] text-ink-3 mb-4">Crea un bot amb @BotFather i entra el token aquí. El webhook es registrarà automàticament.</p>
            <AMGButton size="sm" onClick={() => setShowForm(true)}>Configurar bot</AMGButton>
          </div>
        )}
      </div>
    </div>
  );
}

type WaProvider = 'TWILIO' | 'META';

function MetaAdsConfigCard({ tenantId }: { tenantId: string }) {
  const qc = useQueryClient();
  const { toast } = useToast();
  const [adAccountId, setAdAccountId] = useState('');
  const [accessToken, setAccessToken] = useState('');
  const [saving, setSaving] = useState(false);
  const [syncing, setSyncing] = useState(false);

  const { data: config, isLoading } = useQuery({
    queryKey: ['meta-ads-config', tenantId],
    queryFn: () => getMetaAdsConfig(tenantId),
    retry: false,
  });

  useEffect(() => {
    if (config) setAdAccountId(config.adAccountId ?? '');
  }, [config]);

  const invalidate = () => qc.invalidateQueries({ queryKey: ['meta-ads-config', tenantId] });

  const handleToggle = async (enabled: boolean) => {
    try {
      await saveMetaAdsConfig(tenantId, { adAccountId: adAccountId || config?.adAccountId || '', enabled });
      invalidate();
      toast('success', enabled ? 'Meta Ads activat' : 'Meta Ads desactivat');
    } catch {
      toast('error', 'Error actualitzant la configuració');
    }
  };

  const handleSave = async () => {
    if (!adAccountId.trim()) { toast('error', 'L\'Ad Account ID és obligatori'); return; }
    setSaving(true);
    try {
      await saveMetaAdsConfig(tenantId, {
        adAccountId: adAccountId.trim(),
        accessToken: accessToken.trim() || undefined,
        enabled: config?.enabled ?? false,
      });
      setAccessToken('');
      invalidate();
      toast('success', 'Configuració Meta Ads desada');
    } catch {
      toast('error', 'Error desant la configuració');
    } finally {
      setSaving(false);
    }
  };

  const handleSync = async () => {
    setSyncing(true);
    try {
      await syncMetaAds(tenantId);
      invalidate();
      toast('success', 'Sincronització completada');
    } catch {
      toast('error', 'Error durant la sincronització');
    } finally {
      setSyncing(false);
    }
  };

  if (isLoading) return <div className="p-5 text-sm text-ink-3">Carregant...</div>;

  return (
    <div className="amg-card card-clip">
      <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
        <AMGSectionTitle eyebrow="Anuncis" title="Meta Ads Analytics" />
        <div className="flex items-center gap-2">
          {config?.enabled && (
            <button
              onClick={handleSync}
              disabled={syncing}
              className="f-mono text-[10px] uppercase text-ink-2 hover:text-accent border border-border-base hover:border-accent px-3 h-7 transition-colors disabled:opacity-50"
            >
              {syncing ? '…' : 'Sync ara'}
            </button>
          )}
          <button
            onClick={() => handleToggle(!config?.enabled)}
            className={`f-mono text-[10px] uppercase px-3 h-7 border transition-colors ${
              config?.enabled
                ? 'bg-success/10 border-success/40 text-success hover:bg-success/20'
                : 'border-border-base text-ink-3 hover:text-ink-1 hover:border-border-medium'
            }`}
          >
            {config?.enabled ? 'Activat' : 'Desactivat'}
          </button>
        </div>
      </div>
      <div className="p-5 space-y-4">
        <p className="text-xs text-ink-2">
          Connecta el compte d&apos;anuncis de Meta per obtenir dades de despesa per campanya
          i calcular el cost per lead (CPL) des de la pàgina d&apos;analítica.
        </p>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="space-y-1">
            <label className="f-mono text-xs text-ink-2">Ad Account ID</label>
            <input
              type="text"
              value={adAccountId}
              onChange={e => setAdAccountId(e.target.value)}
              placeholder="act_123456789 o 123456789"
              className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00]"
            />
            <p className="f-mono text-[10px] text-ink-3">Meta Ads Manager → Comptes d&apos;anuncis → ID</p>
          </div>
          <div className="space-y-1">
            <label className="f-mono text-xs text-ink-2">
              Access Token {config?.hasAccessToken && <span className="text-success ml-1">✓ configurat</span>}
            </label>
            <input
              type="password"
              value={accessToken}
              onChange={e => setAccessToken(e.target.value)}
              placeholder={config?.hasAccessToken ? '(deixa buit per mantenir)' : 'Token d\'accés de llarga durada'}
              className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00]"
            />
            <p className="f-mono text-[10px] text-ink-3">Marketing API: System User token o Page token amb ads_read</p>
          </div>
        </div>

        <div className="flex items-center justify-between">
          <AMGButton size="sm" onClick={handleSave} disabled={saving}>
            {saving ? 'Desant…' : 'Desar configuració'}
          </AMGButton>
          {config?.lastSyncAt && (
            <span className="f-mono text-[10px] text-ink-3">
              Darrera sync: {new Date(config.lastSyncAt).toLocaleString('ca-ES')}
            </span>
          )}
        </div>

        {config?.enabled && (
          <div className="pt-3 border-t border-border-base">
            <a
              href={`meta-ads`}
              className="f-mono text-xs text-accent-light hover:underline"
            >
              Gestionar campanyes →
            </a>
          </div>
        )}
      </div>
    </div>
  );
}

function WhatsAppMetaCard({ tenantId }: { tenantId: string }) {
  const qc = useQueryClient();
  const { toast } = useToast();
  const [provider, setProvider] = useState<WaProvider>('TWILIO');
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ phoneNumberId: '', accessToken: '', wabaId: '' });
  const [saving, setSaving] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [testPhone, setTestPhone] = useState('');
  const [sendingTest, setSendingTest] = useState(false);

  const { data: wabaConfig } = useQuery({
    queryKey: ['wa-config', tenantId],
    queryFn: () => getWhatsAppConfig(tenantId),
    retry: false,
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ['wa-config', tenantId] });

  const handleConnect = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      await connectWhatsApp(tenantId, form);
      toast('success', 'Configuració desada — prem "Verificar" per activar');
      invalidate();
      setShowForm(false);
    } catch {
      toast('error', 'Error desant la configuració');
    } finally {
      setSaving(false);
    }
  };

  const handleVerify = async () => {
    setVerifying(true);
    try {
      await verifyWhatsApp(tenantId);
      toast('success', 'WhatsApp connectat correctament');
      invalidate();
    } catch {
      toast('error', 'Verificació fallida — comprova el token i el Phone Number ID');
    } finally {
      setVerifying(false);
    }
  };

  const handleDisconnect = async () => {
    if (!confirm('Desconnectar WhatsApp Business? Els missatges deixaran d\'arribar.')) return;
    try {
      await disconnectWhatsApp(tenantId);
      toast('success', 'WhatsApp desconnectat');
      invalidate();
    } catch {
      toast('error', 'Error desconnectant');
    }
  };

  const handleTest = async (e: React.FormEvent) => {
    e.preventDefault();
    setSendingTest(true);
    try {
      await sendWhatsAppTest(tenantId, testPhone);
      toast('success', 'Missatge de prova enviat');
    } catch {
      toast('error', 'Error enviant el missatge de prova');
    } finally {
      setSendingTest(false);
    }
  };

  const providerTab = (p: WaProvider, label: string) => (
    <button
      type="button"
      onClick={() => { setProvider(p); setShowForm(false); }}
      className={`px-3 py-1.5 rounded text-xs f-mono transition ${
        provider === p
          ? 'bg-[rgba(255,107,0,0.15)] text-accent-light border border-[rgba(255,107,0,0.4)]'
          : 'text-ink-3 border border-border-base hover:text-ink-1'
      }`}
    >
      {label}
    </button>
  );

  return (
    <div className="amg-card card-clip">
      <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
        <AMGSectionTitle eyebrow="WhatsApp" title="WhatsApp Business" />
        <div className="flex items-center gap-2">
          {provider === 'META' && wabaConfig && (
            <AMGBadge tone={WA_STATUS_TONE[wabaConfig.status] ?? 'neutral'}>
              {WA_STATUS_LABEL[wabaConfig.status] ?? wabaConfig.status}
            </AMGBadge>
          )}
          {provider === 'META' && (
            <AMGButton size="sm" variant="ghost" onClick={() => setShowForm(v => !v)}>
              {wabaConfig ? 'Editar' : 'Configurar'}
            </AMGButton>
          )}
        </div>
      </div>

      <div className="p-5 space-y-5">
        {/* Selector de proveïdor */}
        <div className="flex items-center gap-2">
          <span className="f-mono text-[10px] uppercase text-ink-3 tracking-wider mr-1">Proveïdor:</span>
          {providerTab('TWILIO', 'Twilio')}
          {providerTab('META', 'Meta Business Suite')}
        </div>

        {/* Twilio — gestionat a nivell de plataforma */}
        {provider === 'TWILIO' && (
          <div className="space-y-3">
            <div className="p-4 bg-[rgba(255,255,255,0.02)] border border-border-base rounded text-sm space-y-2">
              <div className="f-mono text-[10px] uppercase text-ink-3 tracking-wider">Twilio (compte AMG)</div>
              <p className="text-ink-2 text-xs">Els missatges WhatsApp s&apos;envien via el compte Twilio d&apos;AMG. Tots els tenants comparteixen el mateix número sender configurat a les claus del sistema.</p>
              <div className="grid grid-cols-2 gap-3 pt-1">
                <div>
                  <div className="f-mono text-[10px] text-ink-3 uppercase">Account SID</div>
                  <div className="f-mono text-xs text-ink-1">Configurat a API Keys</div>
                </div>
                <div>
                  <div className="f-mono text-[10px] text-ink-3 uppercase">From number</div>
                  <div className="f-mono text-xs text-ink-1">Configurat a API Keys</div>
                </div>
              </div>
              <p className="f-mono text-[10px] text-ink-3">Per canviar les credencials Twilio, ves a <span className="text-accent-light">Sistema → API Keys → Twilio</span></p>
            </div>

            {/* Test message via Twilio */}
            <form onSubmit={handleTest} className="flex gap-2 items-end">
              <div className="flex-1">
                <label className="f-mono text-xs text-ink-3 block mb-1">Enviar missatge de prova (E.164)</label>
                <input type="text" value={testPhone}
                  onChange={(e) => setTestPhone(e.target.value)}
                  placeholder="+34612345678"
                  className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00]" />
              </div>
              <AMGButton type="submit" size="sm" variant="secondary" loading={sendingTest}>
                Enviar prova
              </AMGButton>
            </form>
          </div>
        )}

        {/* Meta Business Suite — configuració per tenant */}
        {provider === 'META' && (
          <div className="space-y-5">
            {/* Manual config form */}
            {showForm && (
              <form onSubmit={handleConnect} className="space-y-3 p-4 border border-border-base rounded bg-[rgba(255,255,255,0.02)]">
                <div className="f-mono text-label uppercase text-ink-3 text-xs tracking-widest mb-2">Configuració Meta Cloud API</div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="f-mono text-xs text-ink-2 block mb-1">Phone Number ID *</label>
                    <input type="text" required value={form.phoneNumberId}
                      onChange={(e) => setForm(f => ({ ...f, phoneNumberId: e.target.value }))}
                      placeholder="123456789012345"
                      className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00]" />
                  </div>
                  <div>
                    <label className="f-mono text-xs text-ink-2 block mb-1">WABA ID (opcional)</label>
                    <input type="text" value={form.wabaId}
                      onChange={(e) => setForm(f => ({ ...f, wabaId: e.target.value }))}
                      placeholder="987654321098765"
                      className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00]" />
                  </div>
                  <div className="sm:col-span-2">
                    <label className="f-mono text-xs text-ink-2 block mb-1">Access Token permanent *</label>
                    <input type="password" required value={form.accessToken}
                      onChange={(e) => setForm(f => ({ ...f, accessToken: e.target.value }))}
                      placeholder="EAAxxxxxxxxxx..."
                      className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00]" />
                    <p className="f-mono text-[10px] text-ink-3 mt-1">System User Access Token del Meta Business Manager</p>
                  </div>
                </div>
                <div className="flex gap-2">
                  <AMGButton type="submit" size="sm" loading={saving}>Desar</AMGButton>
                  <AMGButton type="button" size="sm" variant="ghost" onClick={() => setShowForm(false)}>Cancel·lar</AMGButton>
                </div>
              </form>
            )}

            {/* Status details */}
            {wabaConfig && !showForm && (
              <div className="space-y-3">
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                  {wabaConfig.displayPhoneNumber && (
                    <div>
                      <div className="f-mono text-label uppercase text-ink-3 text-[10px]">Número</div>
                      <div className="text-sm text-ink-1 font-semibold">{wabaConfig.displayPhoneNumber}</div>
                    </div>
                  )}
                  {wabaConfig.businessName && (
                    <div>
                      <div className="f-mono text-label uppercase text-ink-3 text-[10px]">Negoci</div>
                      <div className="text-sm text-ink-1">{wabaConfig.businessName}</div>
                    </div>
                  )}
                  {wabaConfig.phoneNumberId && (
                    <div>
                      <div className="f-mono text-label uppercase text-ink-3 text-[10px]">Phone Number ID</div>
                      <div className="f-mono text-xs text-ink-2 truncate">{wabaConfig.phoneNumberId}</div>
                    </div>
                  )}
                </div>
                <div className="flex items-center gap-2 flex-wrap">
                  {wabaConfig.status === 'PENDING' && (
                    <AMGButton size="sm" icon={IconSet.Zap} onClick={handleVerify} loading={verifying}>
                      Verificar connexió
                    </AMGButton>
                  )}
                  {wabaConfig.status === 'ERROR' && (
                    <AMGButton size="sm" icon={IconSet.Zap} onClick={handleVerify} loading={verifying}>
                      Reintentar verificació
                    </AMGButton>
                  )}
                  {wabaConfig.status !== 'DISCONNECTED' && (
                    <AMGButton size="sm" variant="ghost" onClick={handleDisconnect}>
                      Desconnectar
                    </AMGButton>
                  )}
                </div>
                {wabaConfig.status === 'CONNECTED' && (
                  <form onSubmit={handleTest} className="flex gap-2 items-end">
                    <div className="flex-1">
                      <label className="f-mono text-xs text-ink-3 block mb-1">Número de prova (E.164)</label>
                      <input type="text" value={testPhone}
                        onChange={(e) => setTestPhone(e.target.value)}
                        placeholder="+34612345678"
                        className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00]" />
                    </div>
                    <AMGButton type="submit" size="sm" variant="secondary" loading={sendingTest}>
                      Enviar prova
                    </AMGButton>
                  </form>
                )}
              </div>
            )}

            {!wabaConfig && !showForm && (
              <div className="text-center py-6">
                <IconSet.Smartphone size={28} stroke="#64748b" className="mx-auto mb-3" />
                <p className="text-sm text-ink-2 mb-3">Meta Business Suite no configurat per aquest tenant.</p>
                <AMGButton size="sm" onClick={() => setShowForm(true)}>Configurar Meta</AMGButton>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

const MANDATE_STATUS_LABEL: Record<string, string> = {
  PENDING_SUBMISSION: 'Pendent d\'enviament',
  SUBMITTED: 'Enviat al banc',
  ACTIVE: 'Actiu',
  FAILED: 'Fallat',
  CANCELLED: 'Cancel·lat',
  EXPIRED: 'Expirat',
};
const MANDATE_STATUS_TONE: Record<string, 'success' | 'warning' | 'danger' | 'neutral'> = {
  ACTIVE: 'success', SUBMITTED: 'warning', PENDING_SUBMISSION: 'warning',
  FAILED: 'danger', CANCELLED: 'danger', EXPIRED: 'danger',
};

function GoCardlessCard({ tenantId }: { tenantId: string }) {
  const qc = useQueryClient();
  const { toast } = useToast();
  const [showConfigForm, setShowConfigForm] = useState(false);
  const [configForm, setConfigForm] = useState({ apiKeyRef: '', environment: 'SANDBOX' as 'SANDBOX' | 'LIVE', creditorId: '', webhookSecret: '' });
  const [configuring, setConfiguring] = useState(false);
  const [initiating, setInitiating] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [mandateUrl, setMandateUrl] = useState<string | null>(null);

  const { data: gcConfig } = useQuery({
    queryKey: ['gc-config', tenantId],
    queryFn: () => getGoCardlessConfig(tenantId),
  });

  const { data: mandate } = useQuery({
    queryKey: ['gc-mandate', tenantId],
    queryFn: () => getGoCardlessMandate(tenantId),
    enabled: !!gcConfig?.isActive,
  });

  const { data: payments } = useQuery({
    queryKey: ['gc-payments', tenantId],
    queryFn: () => listGoCardlessPayments(tenantId),
    enabled: mandate?.status === 'ACTIVE',
  });

  const { data: providers } = useQuery({
    queryKey: ['gc-providers', tenantId],
    queryFn: () => getPaymentProviders(tenantId),
  });

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['gc-config', tenantId] });
    qc.invalidateQueries({ queryKey: ['gc-mandate', tenantId] });
    qc.invalidateQueries({ queryKey: ['gc-payments', tenantId] });
    qc.invalidateQueries({ queryKey: ['gc-providers', tenantId] });
  };

  const handleConfigure = async (e: React.FormEvent) => {
    e.preventDefault();
    setConfiguring(true);
    try {
      await configureGoCardless(tenantId, configForm);
      toast('success', 'GoCardless configurat');
      invalidate();
      setShowConfigForm(false);
    } catch {
      toast('error', 'Error configurant GoCardless');
    } finally {
      setConfiguring(false);
    }
  };

  const handleInitiate = async () => {
    setInitiating(true);
    try {
      const resp = await initiateGoCardlessMandate(tenantId);
      setMandateUrl(resp.redirectUrl);
      invalidate();
    } catch {
      toast('error', 'Error iniciant el mandat SEPA');
    } finally {
      setInitiating(false);
    }
  };

  const handleCancel = async () => {
    if (!confirm('Cancel·lar el mandat SEPA? Deixarà de funcionar el cobrament automàtic.')) return;
    setCancelling(true);
    try {
      await cancelGoCardlessMandate(tenantId);
      toast('success', 'Mandat cancel·lat');
      invalidate();
    } catch {
      toast('error', 'Error cancel·lant el mandat');
    } finally {
      setCancelling(false);
    }
  };

  return (
    <div className="amg-card card-clip">
      <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
        <AMGSectionTitle eyebrow="Pagament" title="GoCardless — SEPA Directe" />
        <div className="flex items-center gap-2">
          {gcConfig?.isActive
            ? <span className="f-mono text-[10px] px-2 py-1 rounded bg-[rgba(57,211,83,0.12)] text-[#39d353] border border-[rgba(57,211,83,0.3)]">● Configurat</span>
            : <span className="f-mono text-[10px] px-2 py-1 rounded bg-[rgba(255,255,255,0.04)] text-ink-3 border border-border-base">○ No configurat</span>
          }
          {!showConfigForm && (
            <AMGButton size="sm" variant="ghost" onClick={() => setShowConfigForm(true)}>
              {gcConfig ? 'Actualitzar' : 'Configurar'}
            </AMGButton>
          )}
        </div>
      </div>

      <div className="p-5 space-y-5">
        {/* Provider summary */}
        {providers && (
          <div className="flex items-center gap-4 p-3 bg-[rgba(255,255,255,0.02)] border border-border-base rounded text-xs">
            <div>
              <span className="f-mono text-[10px] text-ink-3 uppercase tracking-wider">Proveïdor recurrent</span>
              <div className="f-display font-bold text-sm mt-0.5">{providers.recurring.provider}</div>
            </div>
            {providers.recurring.gcMandateActive && (
              <AMGBadge tone={providers.recurring.gcMandateStatus === 'ACTIVE' ? 'success' : 'warning'}>{providers.recurring.gcMandateStatus}</AMGBadge>
            )}
            {providers.recurring.sepaMandateActive && (
              <AMGBadge tone="success">Mandat SEPA manual actiu</AMGBadge>
            )}
          </div>
        )}

        {/* Config form */}
        {showConfigForm && (
          <form onSubmit={handleConfigure} className="space-y-3 p-4 border border-border-base rounded bg-[rgba(255,255,255,0.02)]">
            <div className="f-mono text-label uppercase text-ink-3 text-xs tracking-widest mb-2">Configuració GoCardless</div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="f-mono text-xs text-ink-2 block mb-1">API Key Ref</label>
                <input type="text" required value={configForm.apiKeyRef}
                  onChange={(e) => setConfigForm(f => ({ ...f, apiKeyRef: e.target.value }))}
                  placeholder="GC_API_KEY_SANDBOX"
                  className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00]" />
              </div>
              <div>
                <label className="f-mono text-xs text-ink-2 block mb-1">Entorn</label>
                <select value={configForm.environment}
                  onChange={(e) => setConfigForm(f => ({ ...f, environment: e.target.value as 'SANDBOX' | 'LIVE' }))}
                  className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm text-ink-1 focus:outline-none focus:border-[#FF6B00]">
                  <option value="SANDBOX">Sandbox (proves)</option>
                  <option value="LIVE">Live (producció)</option>
                </select>
              </div>
              <div>
                <label className="f-mono text-xs text-ink-2 block mb-1">Creditor ID (opcional)</label>
                <input type="text" value={configForm.creditorId}
                  onChange={(e) => setConfigForm(f => ({ ...f, creditorId: e.target.value }))}
                  placeholder="CR000..."
                  className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00]" />
              </div>
              <div>
                <label className="f-mono text-xs text-ink-2 block mb-1">Webhook Secret (opcional)</label>
                <input type="password" value={configForm.webhookSecret}
                  onChange={(e) => setConfigForm(f => ({ ...f, webhookSecret: e.target.value }))}
                  className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00]" />
              </div>
            </div>
            <div className="flex gap-2">
              <AMGButton type="submit" size="sm" loading={configuring}>Guardar</AMGButton>
              <AMGButton type="button" size="sm" variant="ghost" onClick={() => setShowConfigForm(false)}>Cancel·lar</AMGButton>
            </div>
          </form>
        )}

        {/* Mandate status */}
        {gcConfig?.isActive && (
          <div className="space-y-3">
            <div className="f-mono text-label uppercase text-ink-3 text-xs tracking-widest">Mandat SEPA</div>

            {!mandate ? (
              <div className="p-4 bg-[rgba(255,255,255,0.02)] border border-border-base rounded space-y-3">
                <p className="text-sm text-ink-2">Cap mandat actiu. Inicia el flux de domiciliació per al client.</p>
                <AMGButton size="sm" onClick={handleInitiate} loading={initiating} icon={IconSet.Zap}>
                  Iniciar mandat SEPA
                </AMGButton>
              </div>
            ) : (
              <div className="p-4 bg-[rgba(255,255,255,0.02)] border border-border-base rounded space-y-2">
                <div className="flex items-center gap-2">
                  <AMGBadge tone={MANDATE_STATUS_TONE[mandate.status] ?? 'neutral'}>
                    {MANDATE_STATUS_LABEL[mandate.status] ?? mandate.status}
                  </AMGBadge>
                  {mandate.accountHolderName && (
                    <span className="text-sm text-ink-1">{mandate.accountHolderName}</span>
                  )}
                </div>
                {mandate.bankName && (
                  <div className="f-mono text-xs text-ink-3">
                    {mandate.bankName}{mandate.lastFourDigits && ` ····${mandate.lastFourDigits}`}
                  </div>
                )}
                {mandate.status === 'ACTIVE' && (
                  <AMGButton size="sm" variant="ghost" onClick={handleCancel} loading={cancelling}>
                    Cancel·lar mandat
                  </AMGButton>
                )}
              </div>
            )}

            {/* Redirect URL (after initiating) */}
            {mandateUrl && (
              <div className="p-3 bg-[rgba(255,107,0,0.06)] border border-[rgba(255,107,0,0.2)] rounded space-y-2">
                <p className="text-xs text-ink-2">Envia aquesta URL al client per autoritzar la domiciliació:</p>
                <div className="flex items-center gap-2">
                  <code className="f-mono text-xs text-ink-1 bg-[rgba(255,255,255,0.04)] px-2 py-1 rounded flex-1 truncate">
                    {mandateUrl}
                  </code>
                  <button onClick={() => { navigator.clipboard.writeText(mandateUrl); toast('success', 'URL copiada'); }}
                    className="text-xs f-mono text-accent-light hover:text-accent transition flex-shrink-0">
                    Copiar
                  </button>
                </div>
                <button onClick={() => setMandateUrl(null)} className="text-xs text-ink-3 hover:text-ink-1">Tancar</button>
              </div>
            )}
          </div>
        )}

        {/* Recent payments */}
        {payments && payments.content.length > 0 && (
          <div className="space-y-2">
            <div className="f-mono text-label uppercase text-ink-3 text-xs tracking-widest">Últims pagaments</div>
            {payments.content.slice(0, 5).map((p) => (
              <div key={p.id} className="flex items-center justify-between px-3 py-2 bg-[rgba(255,255,255,0.02)] rounded">
                <div className="flex items-center gap-2">
                  <AMGBadge tone={p.status === 'PAID_OUT' ? 'success' : p.status === 'FAILED' ? 'danger' : 'warning'}>
                    {p.status}
                  </AMGBadge>
                  <span className="f-mono text-xs text-ink-3">
                    {p.chargeDate ? new Date(p.chargeDate).toLocaleDateString('ca-ES') : '—'}
                  </span>
                </div>
                <span className="f-mono text-sm text-ink-1">{Number(p.amount).toFixed(2)} €</span>
              </div>
            ))}
          </div>
        )}

        {!gcConfig && !showConfigForm && (
          <div className="text-center py-6">
            <IconSet.CreditCard size={28} stroke="#64748b" className="mx-auto mb-3" />
            <p className="text-sm text-ink-2 mb-3">GoCardless no està configurat per aquest tenant.</p>
            <AMGButton size="sm" onClick={() => setShowConfigForm(true)}>Configurar GoCardless</AMGButton>
          </div>
        )}
      </div>
    </div>
  );
}

function NewBudgetModal({ tenantId, tenant, setup, onClose, onCreated }: {
  tenantId: string;
  tenant?: TenantResponse;
  setup: TenantSetup | null;
  onClose: () => void;
  onCreated: () => void;
}) {
  const { toast } = useToast();

  // Sector/size state — default to tenant values but editable
  const [budgetSector, setBudgetSector] = useState(tenant?.sector ?? '');
  const [budgetSize, setBudgetSize] = useState(tenant?.businessSize ?? '');

  // El mode NexeLocal s'activa quan l'usuari selecciona un sector (no el del tenant)
  const isNexeLocal = !!budgetSector;

  const { data: sectorPhases, isLoading: loadingPhases } = useQuery({
    queryKey: ['sector-phases', budgetSector],
    queryFn: () => listSectorPhases(budgetSector),
    enabled: !!budgetSector,
  });

  // NexeLocal state
  const [selectedPhaseNums, setSelectedPhaseNums] = useState<Set<number>>(new Set());

  // Catalog state
  const [selectedProfileId, setSelectedProfileId] = useState('');
  const [selectedPhaseIds, setSelectedPhaseIds] = useState<Set<string>>(new Set());
  const [recommendedPhaseIds, setRecommendedPhaseIds] = useState<Set<string>>(new Set());

  const [notes, setNotes] = useState('');
  const [clientNotes, setClientNotes] = useState('');
  const [validUntil, setValidUntil] = useState('');
  const [recommendation, setRecommendation] = useState('');
  const [creating, setCreating] = useState(false);

  const profiles = setup?.profiles ?? [];
  const selectedProfile = profiles.find(p => p.profile.id === selectedProfileId);

  const phaseMap = new Map((sectorPhases ?? []).map((p: SectorPhaseResponse) => [p.phaseNumber, p]));
  const selectedPhasesArr = Array.from(selectedPhaseNums).sort((a, b) => a - b);
  const phasesMonthly = selectedPhasesArr.reduce((sum, pn) => sum + (phaseMap.get(pn)?.monthlyPrice ?? 0), 0);
  const phasesSetup = selectedPhasesArr.reduce((sum, pn) => sum + (phaseMap.get(pn)?.setupPrice ?? 0), 0);
  const tierAddon = WORKER_ADDONS[budgetSize] ?? { setup: 0, monthly: 0 };
  const nexeLocalSetup = phasesSetup + tierAddon.setup;
  const nexeLocalMonthly = phasesMonthly + tierAddon.monthly;

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isNexeLocal) {
      if (!budgetSector) { toast('error', 'Selecciona el tipus d\'empresa'); return; }
      if (selectedPhaseNums.size === 0) { toast('error', 'Selecciona almenys una fase'); return; }
    } else {
      if (!selectedProfileId) { toast('error', 'Selecciona un perfil'); return; }
      if (selectedPhaseIds.size === 0) { toast('error', 'Selecciona almenys una fase'); return; }
    }
    setCreating(true);
    try {
      const req: CreateBudgetRequest = isNexeLocal ? {
        phaseNumbers: Array.from(selectedPhaseNums).sort(),
        notes: notes || undefined,
        clientNotes: clientNotes || undefined,
        validUntil: validUntil || undefined,
        recommendation: recommendation || undefined,
        sector: budgetSector || undefined,
        businessSize: budgetSize || undefined,
      } : {
        profileId: selectedProfileId,
        phaseIds: Array.from(selectedPhaseIds),
        notes: notes || undefined,
        clientNotes: clientNotes || undefined,
        validUntil: validUntil || undefined,
        recommendation: recommendation || undefined,
        recommendedPhaseIds: recommendedPhaseIds.size > 0 ? Array.from(recommendedPhaseIds) : undefined,
      };
      await createBudget(tenantId, req);
      toast('success', 'Pressupost creat');
      onCreated();
      onClose();
    } catch {
      toast('error', 'Error creant el pressupost');
    } finally {
      setCreating(false);
    }
  };

  const ta = 'w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm text-ink-1 focus:outline-none focus:border-[#FF6B00] resize-none';
  const lbl = 'f-mono text-[10px] uppercase tracking-wider text-ink-3 block mb-1';
  const sel = 'w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm text-ink-1 focus:outline-none focus:border-[#FF6B00]';

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-lg p-6 space-y-4 max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Nou pressupost</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><IconSet.X size={18} /></button>
        </div>

        <form onSubmit={handleCreate} className="space-y-4">
          {/* Sector i mida — sempre visibles, activen mode NexeLocal quan es seleccionen */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className={lbl}>Sector <span className="text-ink-3 normal-case">(opcional — activa preus NexeLocal)</span></label>
              <select value={budgetSector} onChange={e => { setBudgetSector(e.target.value); setBudgetSize(''); setSelectedPhaseNums(new Set()); }} className={sel}>
                <option value="">— Sense sector (mode catàleg) —</option>
                {(Object.keys(SECTOR_LABELS) as string[]).map(k => (
                  <option key={k} value={k}>{SECTOR_LABELS[k]}</option>
                ))}
              </select>
            </div>
            <div>
              <label className={lbl}>Nombre de treballadors</label>
              <select value={budgetSize} onChange={e => setBudgetSize(e.target.value)} className={sel} disabled={!budgetSector}>
                <option value="">Selecciona mida</option>
                {(SECTOR_SIZES[budgetSector] ?? []).map(sz => (
                  <option key={sz} value={sz}>{SIZE_LABELS[sz] ?? sz}</option>
                ))}
              </select>
            </div>
          </div>

          {isNexeLocal ? (
            /* Mode NexeLocal: fases per sector */
            <>
              <div>
                <label className={lbl}>Fases</label>
                {!budgetSector ? (
                  <p className="text-sm text-ink-3 italic">Selecciona primer el tipus d&apos;empresa</p>
                ) : loadingPhases ? (
                  <p className="text-sm text-ink-3">Carregant fases…</p>
                ) : !sectorPhases?.length ? (
                  <p className="text-sm text-ink-3 italic">Cap fase disponible per a aquest sector</p>
                ) : (
                  <div className="space-y-2">
                    {sectorPhases.map((phase: SectorPhaseResponse) => {
                      const checked = selectedPhaseNums.has(phase.phaseNumber);
                      const badge = DEP_BADGE[phase.dependencyType] ?? '';
                      return (
                        <label key={phase.phaseNumber} className={`flex items-start gap-3 p-3 border rounded cursor-pointer transition ${checked ? 'border-[#FF6B00] bg-accent-muted' : 'border-border-base hover:border-ink-2'}`}>
                          <input type="checkbox" checked={checked}
                            onChange={() => setSelectedPhaseNums(prev => { const s = new Set(prev); s.has(phase.phaseNumber) ? s.delete(phase.phaseNumber) : s.add(phase.phaseNumber); return s; })}
                            className="accent-[#FF6B00] mt-0.5" />
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-1.5">
                              <span className="text-xs leading-none">{badge}</span>
                              <span className="text-sm font-medium">F{phase.phaseNumber} · {phase.name}</span>
                            </div>
                            <div className="text-xs text-ink-3 mt-0.5 line-clamp-2">{phase.description}</div>
                            {checked && (
                              <div className="text-xs text-ink-3 f-mono flex gap-3 mt-1">
                                <span>Setup: {fmt(phase.setupPrice)}</span>
                                <span>{fmt(phase.monthlyPrice)}/mes</span>
                              </div>
                            )}
                          </div>
                        </label>
                      );
                    })}
                    {selectedPhaseNums.size > 0 && (
                      <div className="mt-2 p-3 rounded bg-[rgba(255,107,0,0.08)] border border-[rgba(255,107,0,0.2)] space-y-1.5">
                        {tierAddon.setup > 0 && (
                          <div className="text-xs text-ink-3">Add-on equip ({SIZE_LABELS[budgetSize] ?? budgetSize}): +{fmt(tierAddon.setup)} setup, +{fmt(tierAddon.monthly)}/mes</div>
                        )}
                        <div className="grid grid-cols-2 gap-2">
                          <div>
                            <div className="text-xs text-ink-3">Setup total</div>
                            <div className="text-sm font-bold f-mono text-white">{fmt(nexeLocalSetup)}</div>
                          </div>
                          <div>
                            <div className="text-xs text-ink-3">Mensual</div>
                            <div className="text-sm font-bold f-mono text-[#FF6B00]">{fmt(nexeLocalMonthly)}/mes</div>
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </>
          ) : (
            /* Mode catàleg */
            <>
              <div>
                <label className={lbl}>Perfil</label>
                {profiles.length === 0 ? (
                  <p className="text-sm text-ink-3">Cap perfil assignat. Assigna primer un perfil.</p>
                ) : (
                  <div className="space-y-2">
                    {profiles.map((p) => (
                      <button key={p.profile.id} type="button"
                        onClick={() => { setSelectedProfileId(p.profile.id); setSelectedPhaseIds(new Set()); }}
                        className={`w-full text-left p-3 border rounded transition text-sm ${selectedProfileId === p.profile.id ? 'border-[#FF6B00] bg-accent-muted' : 'border-border-base hover:border-ink-2'}`}>
                        <span className="font-semibold">{p.profile.name}</span>
                        <span className="text-ink-3 ml-2 text-xs">{p.phases.length} fases</span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
              {selectedProfile && (
                <div>
                  <label className={lbl}>Fases a incloure</label>
                  <div className="space-y-2">
                    {selectedProfile.phases.map((ph) => (
                      <div key={ph.phase.id} className="flex items-center gap-2 p-3 border border-border-base rounded">
                        <input type="checkbox" checked={selectedPhaseIds.has(ph.phase.id)}
                          onChange={() => setSelectedPhaseIds(prev => { const s = new Set(prev); s.has(ph.phase.id) ? s.delete(ph.phase.id) : s.add(ph.phase.id); return s; })}
                          className="accent-[#FF6B00]" />
                        <div className="flex-1 min-w-0">
                          <span className="text-sm">{ph.phase.name}</span>
                          <span className="f-mono text-xs text-ink-3 ml-2">{ph.services.length} serveis</span>
                        </div>
                        <label className="flex items-center gap-1.5 cursor-pointer shrink-0">
                          <input type="checkbox" checked={recommendedPhaseIds.has(ph.phase.id)}
                            onChange={() => setRecommendedPhaseIds(prev => { const s = new Set(prev); s.has(ph.phase.id) ? s.delete(ph.phase.id) : s.add(ph.phase.id); return s; })}
                            className="accent-amber-500" />
                          <span className="text-xs text-amber-400">Recomanada</span>
                        </label>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}

          <div>
            <label className={lbl}>Recomanació per al client</label>
            <textarea value={recommendation} onChange={(e) => setRecommendation(e.target.value)} rows={3}
              placeholder="Per a un negoci com el teu, et recomanem les fases F1 i F2 per arrancar..."
              className={ta} />
          </div>
          <div>
            <label className={lbl}>Notes internes</label>
            <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={2} className={ta} />
          </div>
          <div>
            <label className={lbl}>Notes per al client</label>
            <textarea value={clientNotes} onChange={(e) => setClientNotes(e.target.value)} rows={2} className={ta} />
          </div>
          <div>
            <label className={lbl}>Vàlid fins</label>
            <input type="date" value={validUntil} onChange={(e) => setValidUntil(e.target.value)}
              className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm text-ink-1 focus:outline-none focus:border-[#FF6B00]" />
          </div>

          <div className="flex gap-3 pt-2 border-t border-border-base">
            <AMGButton type="submit" disabled={creating} loading={creating} className="flex-1 justify-center">
              Crear pressupost
            </AMGButton>
            <AMGButton type="button" variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
          </div>
        </form>
      </div>
    </div>
  );
}

function BudgetDetailModal({ budget, tenantId, tenant, setup, onClose, onRefresh }: {
  budget: BudgetResponse;
  tenantId: string;
  tenant?: TenantResponse;
  setup: TenantSetup | null;
  onClose: () => void;
  onRefresh: () => void;
}) {
  const { toast } = useToast();
  const qc = useQueryClient();
  const [mode, setMode] = useState<'view' | 'edit'>('view');
  const [sending, setSending] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [cloning, setCloning] = useState(false);
  const [acceptanceUrl, setAcceptanceUrl] = useState<string | null>(null);

  const isNexeLocal = !!(budget.phaseNumbers?.length);

  // Edit state — pre-filled from budget
  const [editPhaseNums, setEditPhaseNums] = useState<Set<number>>(new Set(budget.phaseNumbers ?? []));
  const [editProfileId, setEditProfileId] = useState(budget.profileId ?? '');
  const [editPhaseIds, setEditPhaseIds] = useState<Set<string>>(new Set(budget.phaseIds ?? []));
  const [editNotes, setEditNotes] = useState(budget.notes ?? '');
  const [editClientNotes, setEditClientNotes] = useState(budget.clientNotes ?? '');
  const [editValidUntil, setEditValidUntil] = useState(budget.validUntil ? budget.validUntil.slice(0, 10) : '');
  const [editRecommendation, setEditRecommendation] = useState(budget.recommendation ?? '');
  const [saving, setSaving] = useState(false);

  // Sector/size for pricing — default to tenant values but editable
  const [editBudgetSector, setEditBudgetSector] = useState(budget.sector ?? tenant?.sector ?? '');
  const [editBudgetSize, setEditBudgetSize] = useState(budget.businessSize ?? tenant?.businessSize ?? '');

  const { data: editSectorPhases, isLoading: editLoadingPhases } = useQuery({
    queryKey: ['sector-phases', editBudgetSector],
    queryFn: () => listSectorPhases(editBudgetSector),
    enabled: !!editBudgetSector,
  });

  // Fitxa de configuració vinculada al pressupost
  const { data: intake, refetch: refetchIntake } = useQuery<SetupIntakeResponse | null>({
    queryKey: ['intake', budget.id],
    queryFn: () => getIntakeByBudget(budget.id),
    enabled: mode === 'view',
    initialData: null,
  });
  const [creatingIntake, setCreatingIntake] = useState(false);
  const handleCreateIntake = async () => {
    setCreatingIntake(true);
    try {
      await createIntake(budget.id);
      await refetchIntake();
    } catch {
      toast('error', 'Error en generar la fitxa');
    } finally {
      setCreatingIntake(false);
    }
  };

  const isDraft = budget.status === 'DRAFT';
  const statusTone = budget.status === 'ACCEPTED' ? 'success'
    : budget.status === 'REJECTED' ? 'danger'
    : budget.status === 'SENT' ? 'info'
    : 'neutral';

  const profiles = setup?.profiles ?? [];
  const editProfile = profiles.find(p => p.profile.id === editProfileId);

  const editPhaseMap = new Map((editSectorPhases ?? []).map((p: SectorPhaseResponse) => [p.phaseNumber, p]));
  const editPhasesArr = Array.from(editPhaseNums).sort((a, b) => a - b);
  const editPhasesMonthly = editPhasesArr.reduce((sum, pn) => sum + (editPhaseMap.get(pn)?.monthlyPrice ?? 0), 0);
  const editPhasesSetup = editPhasesArr.reduce((sum, pn) => sum + (editPhaseMap.get(pn)?.setupPrice ?? 0), 0);
  const editTierAddon = WORKER_ADDONS[editBudgetSize] ?? { setup: 0, monthly: 0 };
  const editSetup = editPhasesSetup + editTierAddon.setup;
  const editMonthly = editPhasesMonthly + editTierAddon.monthly;

  const handleSend = async () => {
    setSending(true);
    try {
      const res = await sendBudget(budget.id);
      if (res?.acceptanceUrl) setAcceptanceUrl(res.acceptanceUrl);
      toast('success', 'Pressupost enviat — copia l\'enllaç per compartir-lo');
      qc.invalidateQueries({ queryKey: ['budgets', tenantId] });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error desconegut';
      toast('error', `Error enviant el pressupost: ${msg}`);
    } finally {
      setSending(false);
    }
  };

  const handleCancel = async () => {
    if (!confirm('Segur que vols eliminar aquest pressupost?')) return;
    setCancelling(true);
    try {
      await cancelBudget(budget.id);
      toast('success', 'Pressupost eliminat');
      onRefresh();
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error desconegut';
      toast('error', `Error eliminant el pressupost: ${msg}`);
    } finally {
      setCancelling(false);
    }
  };

  const handleClone = async () => {
    setCloning(true);
    try {
      const req: CreateBudgetRequest = isNexeLocal
        ? { phaseNumbers: budget.phaseNumbers!, notes: budget.notes ?? undefined, clientNotes: budget.clientNotes ?? undefined }
        : { profileId: budget.profileId ?? undefined, phaseIds: budget.phaseIds, notes: budget.notes ?? undefined, clientNotes: budget.clientNotes ?? undefined };
      await createBudget(tenantId, req);
      toast('success', 'Pressupost clonat com a DRAFT');
      onRefresh();
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error desconegut';
      toast('error', `Error clonant el pressupost: ${msg}`);
    } finally {
      setCloning(false);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isNexeLocal) {
      if (editPhaseNums.size === 0) { toast('error', 'Selecciona almenys una fase'); return; }
    } else {
      if (!editProfileId) { toast('error', 'Selecciona un perfil'); return; }
      if (editPhaseIds.size === 0) { toast('error', 'Selecciona almenys una fase'); return; }
    }
    setSaving(true);
    try {
      const req = isNexeLocal
        ? { phaseNumbers: Array.from(editPhaseNums).sort(), notes: editNotes || undefined, clientNotes: editClientNotes || undefined, validUntil: editValidUntil || undefined, recommendation: editRecommendation || undefined, sector: editBudgetSector || undefined, businessSize: editBudgetSize || undefined }
        : { profileId: editProfileId, phaseIds: Array.from(editPhaseIds), notes: editNotes || undefined, clientNotes: editClientNotes || undefined, validUntil: editValidUntil || undefined, recommendation: editRecommendation || undefined };
      await updateBudget(budget.id, req);
      toast('success', 'Pressupost actualitzat');
      onRefresh();
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error desconegut';
      toast('error', `Error actualitzant el pressupost: ${msg}`);
    } finally {
      setSaving(false);
    }
  };

  const inputCls = 'w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm text-ink-1 focus:outline-none focus:border-[#FF6B00]';
  const labelCls = 'f-mono text-[10px] uppercase tracking-wider text-ink-3 block mb-1';
  const metaRow = (label: string, value: string) => (
    <div className="flex justify-between py-1.5 border-b border-border-base last:border-0">
      <span className="text-xs text-ink-3">{label}</span>
      <span className="text-xs f-mono text-ink-1">{value}</span>
    </div>
  );

  return (
    <>
      <div className="fixed inset-0 bg-black/70 z-50 flex items-center justify-center p-4" onClick={onClose}>
        <div className="amg-card card-clip w-full max-w-lg max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>

          {/* Header */}
          <div className="flex items-center justify-between p-4 border-b border-border-base sticky top-0 bg-[var(--surface-card)] z-10">
            <div className="flex items-center gap-2 min-w-0">
              <AMGBadge tone={statusTone}>{budget.status}</AMGBadge>
              <span className="f-mono font-bold text-white truncate">{budget.budgetNumber}</span>
              {mode === 'edit' && <AMGBadge tone="warning">Editant</AMGBadge>}
            </div>
            <div className="flex items-center gap-1 shrink-0">
              {isDraft && mode === 'view' && (
                <button title="Editar" onClick={() => setMode('edit')}
                  className="p-1.5 rounded text-ink-2 hover:text-white hover:bg-[rgba(255,255,255,0.08)] transition">
                  <IconSet.Edit size={15} />
                </button>
              )}
              <button title="Clonar" onClick={handleClone} disabled={cloning}
                className="p-1.5 rounded text-ink-2 hover:text-white hover:bg-[rgba(255,255,255,0.08)] transition disabled:opacity-40">
                <IconSet.Copy size={15} />
              </button>
              {isDraft && mode === 'view' && (
                <button title="Enviar al client" onClick={handleSend} disabled={sending}
                  className="p-1.5 rounded text-ink-2 hover:text-white hover:bg-[rgba(255,255,255,0.08)] transition disabled:opacity-40">
                  <IconSet.ArrowRight size={15} />
                </button>
              )}
              <button title="Eliminar" onClick={handleCancel} disabled={cancelling}
                className="p-1.5 rounded text-red-400 hover:text-red-300 hover:bg-[rgba(239,68,68,0.12)] transition disabled:opacity-40">
                <IconSet.Trash size={15} />
              </button>
              <button onClick={mode === 'edit' ? () => setMode('view') : onClose}
                className="p-1.5 ml-1 rounded text-ink-2 hover:text-ink-0">
                <IconSet.X size={17} />
              </button>
            </div>
          </div>

          {mode === 'view' ? (
            <div className="p-5 space-y-5">

              {/* Enllaç d'acceptació */}
              {(budget.acceptanceUrl || acceptanceUrl) && (
                <div className="rounded-lg bg-green-500/10 border border-green-500/30 p-4 space-y-2">
                  <div className="text-green-400 text-xs font-semibold uppercase tracking-wider">Enllaç per al client</div>
                  <div className="flex items-center gap-2">
                    <input readOnly value={budget.acceptanceUrl ?? acceptanceUrl ?? ''}
                      className="flex-1 bg-[rgba(255,255,255,0.05)] border border-border-base rounded px-3 py-1.5 text-xs text-ink-1 f-mono truncate focus:outline-none" />
                    <button onClick={() => { navigator.clipboard.writeText(budget.acceptanceUrl ?? acceptanceUrl ?? ''); toast('success', 'Copiat'); }}
                      className="shrink-0 px-3 py-1.5 bg-green-500/20 hover:bg-green-500/30 border border-green-500/40 text-green-400 text-xs rounded transition">
                      Copiar
                    </button>
                  </div>
                </div>
              )}

              {/* Recomanació */}
              {budget.recommendation && (
                <div className="rounded-lg bg-amber-500/10 border border-amber-500/30 p-4">
                  <div className="text-amber-400 text-xs font-semibold uppercase tracking-wider mb-1">Recomanació</div>
                  <p className="text-sm text-ink-1">{budget.recommendation}</p>
                </div>
              )}

              {/* Meta dates */}
              <div>
                {metaRow('Creat', fmtDate(budget.createdAt))}
                {metaRow('Vàlid fins', fmtDate(budget.validUntil))}
                {budget.sentAt && metaRow('Enviat', fmtDate(budget.sentAt))}
                {budget.acceptedAt && metaRow('Acceptat', fmtDate(budget.acceptedAt))}
                {budget.rejectedAt && metaRow('Rebutjat', fmtDate(budget.rejectedAt))}
                {budget.notes && metaRow('Notes', budget.notes)}
                {budget.clientNotes && metaRow('Notes client', budget.clientNotes)}
              </div>

              {/* Fases — estil accept-budget */}
              {budget.phases.length > 0 && (
                <div className="space-y-3">
                  <div className="text-xs text-ink-3 uppercase tracking-wider font-bold">Fases</div>
                  {budget.phases.map((phase, pi) => {
                    const isRec = budget.recommendedPhaseIds?.includes(phase.phaseId ?? '');
                    return (
                      <div key={pi} className="rounded-lg border border-border-base overflow-hidden">
                        <div className="flex items-center justify-between px-4 py-3 bg-[rgba(255,255,255,0.04)]">
                          <div className="flex items-center gap-3">
                            <div className="w-7 h-7 rounded-full bg-[#FF6B00]/20 border border-[#FF6B00]/40 flex items-center justify-center text-xs font-bold text-[#FF6B00] shrink-0">
                              {phase.sortOrder ?? pi + 1}
                            </div>
                            <div>
                              <div className="text-sm font-semibold text-white">{phase.name}</div>
                              {isRec && <div className="text-xs text-amber-400">★ Recomanada</div>}
                            </div>
                          </div>
                          <div className="flex gap-4 text-right shrink-0">
                            <div>
                              <div className="text-xs text-ink-3">Setup</div>
                              <div className="text-sm f-mono font-bold text-white">{fmt(phase.phaseTotal)}</div>
                            </div>
                            <div>
                              <div className="text-xs text-ink-3">Mensual</div>
                              <div className="text-sm f-mono font-bold text-[#FF6B00]">{fmt(phase.phaseMonthlyTotal)}/mes</div>
                            </div>
                          </div>
                        </div>
                        {phase.lines.length > 1 && (
                          <div className="divide-y divide-border-base">
                            {phase.lines.map((line, li) => (
                              <div key={li} className="flex items-center justify-between px-4 py-2 gap-4">
                                <span className="text-xs text-ink-3 flex-1">{line.serviceName}</span>
                                <div className="flex gap-3 text-xs f-mono text-ink-3 shrink-0">
                                  {line.setupPrice > 0 && <span>{fmt(line.setupPrice)}</span>}
                                  {line.monthlyPrice > 0 && <span className="text-[#FF6B00]">{fmt(line.monthlyPrice)}/mes</span>}
                                </div>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}

              {/* Resum de preus */}
              <div className="rounded-lg border border-border-base overflow-hidden">
                <div className="grid grid-cols-2 divide-x divide-border-base">
                  <div className="p-4 text-center">
                    <div className="text-xs text-ink-3 uppercase tracking-wider mb-1">Inversió inicial</div>
                    <div className="text-xl font-bold f-mono text-white">{fmt(budget.total)}</div>
                    {budget.discountTotal > 0 && (
                      <div className="text-xs text-green-400 mt-0.5">Descompte: -{fmt(budget.discountTotal)}</div>
                    )}
                  </div>
                  <div className="p-4 text-center">
                    <div className="text-xs text-ink-3 uppercase tracking-wider mb-1">Quota mensual</div>
                    <div className="text-xl font-bold f-mono text-[#FF6B00]">{fmt(budget.monthlyTotal ?? 0)}<span className="text-sm font-normal text-ink-3">/mes</span></div>
                  </div>
                </div>
              </div>

              {/* Fitxa de configuració */}
              <div className="rounded-lg border border-border-base overflow-hidden">
                <div className="px-4 py-3 bg-[rgba(255,255,255,0.04)] border-b border-border-base">
                  <div className="text-xs text-ink-3 uppercase tracking-wider font-bold">Fitxa de configuració</div>
                </div>
                <div className="p-4">
                  {intake ? (
                    <div className="space-y-3">
                      {/* Badge d'estat */}
                      <div className="flex items-center gap-2">
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                          intake.status === 'COMPLETE' ? 'bg-green-500/20 text-green-400'
                          : intake.status === 'IN_PROGRESS' ? 'bg-amber-500/20 text-amber-400'
                          : 'bg-gray-500/20 text-gray-400'
                        }`}>
                          {intake.status === 'COMPLETE' ? 'Completada' : intake.status === 'IN_PROGRESS' ? 'En progrés' : 'Pendent'}
                        </span>
                      </div>
                      {/* URL copiable */}
                      <div className="flex items-center gap-2">
                        <input readOnly value={intake.intakeUrl}
                          className="flex-1 bg-[rgba(255,255,255,0.05)] border border-border-base rounded px-3 py-1.5 text-xs text-ink-1 f-mono truncate focus:outline-none" />
                        <button onClick={() => { navigator.clipboard.writeText(intake.intakeUrl); toast('success', 'Copiat'); }}
                          className="shrink-0 px-3 py-1.5 bg-[rgba(255,255,255,0.08)] hover:bg-[rgba(255,255,255,0.12)] border border-border-base text-ink-1 text-xs rounded transition">
                          Copiar
                        </button>
                        <a href={intake.intakeUrl} target="_blank" rel="noreferrer"
                          className="shrink-0 px-3 py-1.5 bg-[#FF6B00]/20 hover:bg-[#FF6B00]/30 border border-[#FF6B00]/40 text-[#FF6B00] text-xs rounded transition">
                          Obrir
                        </a>
                      </div>
                    </div>
                  ) : (
                    <button
                      onClick={handleCreateIntake}
                      disabled={creatingIntake}
                      className="w-full py-2.5 rounded-lg border border-dashed border-border-base hover:border-[#FF6B00]/50 text-sm text-ink-3 hover:text-[#FF6B00] transition disabled:opacity-50"
                    >
                      {creatingIntake ? 'Generant…' : '+ Generar fitxa de configuració'}
                    </button>
                  )}
                </div>
              </div>
            </div>
          ) : (
            /* Edit mode — similar al formulari de creació */
            <form onSubmit={handleSave} className="p-5 space-y-4">
              {/* Sector i mida — sempre visibles, s'envien al backend per recalcular preus */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className={labelCls}>Sector</label>
                  <select value={editBudgetSector} onChange={e => { setEditBudgetSector(e.target.value); setEditBudgetSize(''); }} className={inputCls}>
                    <option value="">Sense sector</option>
                    {(Object.keys(SECTOR_LABELS) as string[]).map(k => (
                      <option key={k} value={k}>{SECTOR_LABELS[k]}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className={labelCls}>Nombre de treballadors</label>
                  <select value={editBudgetSize} onChange={e => setEditBudgetSize(e.target.value)} className={inputCls} disabled={!editBudgetSector}>
                    <option value="">Selecciona mida</option>
                    {(SECTOR_SIZES[editBudgetSector] ?? []).map(sz => (
                      <option key={sz} value={sz}>{SIZE_LABELS[sz] ?? sz}</option>
                    ))}
                  </select>
                </div>
              </div>

              {isNexeLocal ? (
                <>
                  <div>
                    <label className={labelCls}>Fases</label>
                    {editLoadingPhases ? (
                      <p className="text-sm text-ink-3">Carregant fases…</p>
                    ) : (
                      <div className="space-y-2">
                        {(editSectorPhases ?? []).map((phase: SectorPhaseResponse) => {
                          const checked = editPhaseNums.has(phase.phaseNumber);
                          const badge = DEP_BADGE[phase.dependencyType] ?? '';
                          return (
                            <label key={phase.phaseNumber} className={`flex items-start gap-3 p-3 border rounded cursor-pointer transition ${checked ? 'border-[#FF6B00] bg-accent-muted' : 'border-border-base hover:border-ink-2'}`}>
                              <input type="checkbox" checked={checked}
                                onChange={() => setEditPhaseNums(prev => { const s = new Set(prev); s.has(phase.phaseNumber) ? s.delete(phase.phaseNumber) : s.add(phase.phaseNumber); return s; })}
                                className="accent-[#FF6B00] mt-0.5" />
                              <div className="flex-1 min-w-0">
                                <div className="flex items-center gap-1.5">
                                  <span className="text-xs leading-none">{badge}</span>
                                  <span className="text-sm font-medium">F{phase.phaseNumber} · {phase.name}</span>
                                </div>
                                <div className="text-xs text-ink-3 mt-0.5 line-clamp-2">{phase.description}</div>
                                {checked && (
                                  <div className="text-xs text-ink-3 f-mono flex gap-3 mt-1">
                                    <span>Setup: {fmt(phase.setupPrice)}</span>
                                    <span>{fmt(phase.monthlyPrice)}/mes</span>
                                  </div>
                                )}
                              </div>
                            </label>
                          );
                        })}
                        {editPhaseNums.size > 0 && (
                          <div className="p-3 rounded bg-[rgba(255,107,0,0.08)] border border-[rgba(255,107,0,0.2)] space-y-1.5">
                            {editTierAddon.setup > 0 && (
                              <div className="text-xs text-ink-3">Add-on equip ({SIZE_LABELS[editBudgetSize] ?? editBudgetSize}): +{fmt(editTierAddon.setup)} setup, +{fmt(editTierAddon.monthly)}/mes</div>
                            )}
                            <div className="grid grid-cols-2 gap-2">
                              <div><div className="text-xs text-ink-3">Setup total</div><div className="text-sm font-bold f-mono text-white">{fmt(editSetup)}</div></div>
                              <div><div className="text-xs text-ink-3">Mensual</div><div className="text-sm font-bold f-mono text-[#FF6B00]">{fmt(editMonthly)}/mes</div></div>
                            </div>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </>
              ) : (
                <>
                  <div>
                    <label className={labelCls}>Perfil</label>
                    {profiles.length === 0 ? (
                      <p className="text-sm text-ink-3">Cap perfil assignat.</p>
                    ) : (
                      <div className="space-y-2">
                        {profiles.map((p) => (
                          <button key={p.profile.id} type="button"
                            onClick={() => { setEditProfileId(p.profile.id); setEditPhaseIds(new Set()); }}
                            className={`w-full text-left p-3 border rounded transition text-sm ${editProfileId === p.profile.id ? 'border-[#FF6B00] bg-accent-muted' : 'border-border-base hover:border-ink-2'}`}>
                            <span className="font-semibold">{p.profile.name}</span>
                            <span className="text-ink-3 ml-2 text-xs">{p.phases.length} fases</span>
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                  {editProfile && (
                    <div>
                      <label className={labelCls}>Fases a incloure</label>
                      <div className="space-y-2">
                        {editProfile.phases.map((ph) => (
                          <label key={ph.phase.id} className="flex items-center gap-3 p-3 border border-border-base rounded cursor-pointer hover:border-ink-2 transition">
                            <input type="checkbox" checked={editPhaseIds.has(ph.phase.id)}
                              onChange={() => setEditPhaseIds(prev => { const s = new Set(prev); s.has(ph.phase.id) ? s.delete(ph.phase.id) : s.add(ph.phase.id); return s; })}
                              className="accent-[#FF6B00]" />
                            <span className="text-sm flex-1">{ph.phase.name}</span>
                            <span className="f-mono text-xs text-ink-3">{ph.services.length} serveis</span>
                          </label>
                        ))}
                      </div>
                    </div>
                  )}
                </>
              )}

              <div>
                <label className={labelCls}>Recomanació per al client</label>
                <textarea value={editRecommendation} onChange={(e) => setEditRecommendation(e.target.value)} rows={3}
                  className={`${inputCls} resize-none`} />
              </div>
              <div>
                <label className={labelCls}>Notes internes</label>
                <textarea value={editNotes} onChange={(e) => setEditNotes(e.target.value)} rows={2}
                  className={`${inputCls} resize-none`} />
              </div>
              <div>
                <label className={labelCls}>Notes per al client</label>
                <textarea value={editClientNotes} onChange={(e) => setEditClientNotes(e.target.value)} rows={2}
                  className={`${inputCls} resize-none`} />
              </div>
              <div>
                <label className={labelCls}>Vàlid fins</label>
                <input type="date" value={editValidUntil} onChange={(e) => setEditValidUntil(e.target.value)}
                  className={inputCls} />
              </div>

              <div className="flex gap-3 pt-2 border-t border-border-base">
                <AMGButton type="submit" disabled={saving} loading={saving} className="flex-1 justify-center">
                  Desar canvis
                </AMGButton>
                <AMGButton type="button" variant="outline" onClick={() => setMode('view')}>Cancel·lar</AMGButton>
              </div>
            </form>
          )}
        </div>
      </div>
    </>
  );
}

function DeleteTenantModal({ tenantId, tenantName, onClose, onDeleted }: {
  tenantId: string; tenantName: string;
  onClose: () => void; onDeleted: () => void;
}) {
  const { toast } = useToast();
  const [check, setCheck] = useState<DeleteTenantCheck | null>(null);
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    checkTenantDeletion(tenantId)
      .then(setCheck)
      .catch(() => toast('error', 'Error comprovant les condicions d\'eliminació'))
      .finally(() => setLoading(false));
  }, [tenantId]);

  const handleDelete = async () => {
    setDeleting(true);
    try {
      await deleteTenant(tenantId);
      toast('success', `Tenant "${tenantName}" eliminat`);
      onDeleted();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error desconegut';
      toast('error', `Error eliminant el tenant: ${msg}`);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/70 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-md p-6 space-y-5" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base text-white">Eliminar tenant</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><IconSet.X size={18} /></button>
        </div>

        {loading ? (
          <div className="flex justify-center py-8">
            <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          </div>
        ) : check && (
          <div className="space-y-4">
            <p className="text-sm text-ink-2">
              Estàs a punt d&apos;eliminar <span className="font-semibold text-white">{tenantName}</span> de forma permanent.
            </p>

            {[...check.blockers, ...check.warnings].length > 0 && (
              <ul className="space-y-2">
                {[...check.blockers, ...check.warnings].map((msg, i) => (
                  <li key={i} className="flex items-start gap-2.5 f-mono text-xs text-ink-2">
                    <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-[#FF6B00] flex-shrink-0" />
                    {msg}
                  </li>
                ))}
              </ul>
            )}

            <p className="text-xs text-ink-3">Aquesta acció és irreversible.</p>
            <div className="flex gap-3 pt-1 border-t border-border-base">
              <button
                onClick={handleDelete}
                disabled={deleting}
                className={`flex-1 px-4 py-2.5 rounded text-sm font-semibold transition border border-red-500 bg-[rgba(239,68,68,0.12)] text-red-400 hover:bg-[rgba(239,68,68,0.2)] ${deleting ? 'opacity-50 cursor-not-allowed' : ''}`}>
                {deleting ? 'Eliminant...' : 'Confirmar i eliminar'}
              </button>
              <AMGButton variant="ghost" onClick={onClose}>Cancel·lar</AMGButton>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// ─── DPA Section ──────────────────────────────────────────────

function DpaSection({ tenantId, locale }: { tenantId: string; locale: string }) {
  const qc = useQueryClient();
  const { toast } = useToast();
  const [sending, setSending] = useState(false);

  const { data: dpa } = useQuery<DpaStatus>({
    queryKey: ['dpa-status', tenantId],
    queryFn: () => getDpaStatus(tenantId),
  });

  const status = dpa?.status ?? 'NOT_SENT';

  const statusConfig: Record<string, { label: string; color: string }> = {
    NOT_SENT: { label: 'No enviat', color: 'text-ink-3' },
    PENDING:  { label: 'Pendent de signatura', color: 'text-yellow-400' },
    SIGNED:   { label: 'Signat', color: 'text-green-400' },
    EXPIRED:  { label: 'Caducat', color: 'text-red-400' },
  };
  const { label, color } = statusConfig[status] ?? statusConfig.NOT_SENT;

  const handleSend = async () => {
    setSending(true);
    try {
      await sendDpaRequest(tenantId);
      qc.invalidateQueries({ queryKey: ['dpa-status', tenantId] });
      toast('success', 'Acord de Tractament de Dades enviat per email');
    } catch (e: unknown) {
      toast('error', e instanceof Error ? e.message : 'Error en enviar');
    } finally {
      setSending(false);
    }
  };

  return (
    <CollapsibleSection
      eyebrow="Legal" title="Acord de Tractament de Dades (ATD)"
      status={status === 'SIGNED' ? 'active' : 'neutral'}
      collapsed
      onToggle={() => {}}
    >
      <div className="amg-card card-clip p-5 space-y-4">
        <div className="flex items-center justify-between">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <span className={`w-2 h-2 rounded-full flex-shrink-0 ${status === 'SIGNED' ? 'bg-green-400' : status === 'PENDING' ? 'bg-yellow-400' : 'bg-gray-400'}`} />
              <span className={`f-mono text-xs font-semibold ${color}`}>{label}</span>
            </div>
            {status === 'SIGNED' && dpa?.signerName && (
              <p className="f-mono text-xs text-ink-3">
                Signat per <strong className="text-ink-1">{dpa.signerName}</strong>
                {dpa.signerPosition && <> ({dpa.signerPosition})</>}
                {dpa.signedAt && <> · {new Date(dpa.signedAt).toLocaleDateString('ca-ES', { day: 'numeric', month: 'long', year: 'numeric' })}</>}
              </p>
            )}
            {status === 'PENDING' && dpa?.token && (
              <p className="f-mono text-xs text-ink-3">
                Pendent de signatura ·{' '}
                <a
                  href={`/${locale}/dpa/${dpa.token}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-accent-light underline"
                >
                  Previsualitzar
                </a>
              </p>
            )}
          </div>

          <div className="flex gap-2 flex-wrap justify-end">
            {status === 'SIGNED' && dpa?.token && (
              <a
                href={`/${locale}/dpa/${dpa.token}`}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1 px-3 py-1.5 border border-border-base text-xs f-mono text-ink-1 hover:border-[#FF6B00] hover:text-accent-light transition-colors"
              >
                Veure document signat
              </a>
            )}
            <AMGButton
              type="button"
              variant={status === 'SIGNED' ? 'ghost' : 'secondary'}
              size="sm"
              loading={sending}
              onClick={handleSend}
            >
              {status === 'NOT_SENT' ? 'Enviar ATD per email' : status === 'SIGNED' ? 'Reenviar' : 'Reenviar enllaç'}
            </AMGButton>
          </div>
        </div>

        {status === 'NOT_SENT' && (
          <p className="f-mono text-xs text-ink-3">
            Envia l'Acord de Tractament de Dades al tenant perquè el llegeixi i signi digitalment. La signatura es registra amb data, hora i IP.
          </p>
        )}
      </div>
    </CollapsibleSection>
  );
}

export default function TenantDetailPage() {
  const params = useParams<{ id: string; locale: string }>();
  const { id } = params;
  const locale = params.locale as string;
  const router = useRouter();
  const qc = useQueryClient();
  const { toast } = useToast();
  const [showAssignProfile, setShowAssignProfile] = useState(false);
  const [showAddPhase, setShowAddPhase] = useState(false);
  const [showAddService, setShowAddService] = useState(false);
  const [showNewBudget, setShowNewBudget] = useState(false);
  const [showDeleteTenant, setShowDeleteTenant] = useState(false);
  const [selectedBudget, setSelectedBudget] = useState<BudgetResponse | null>(null);
  const [togglingFree, setTogglingFree] = useState(false);
  const [togglingActive, setTogglingActive] = useState(false);
  const [editingEmail, setEditingEmail] = useState(false);
  const [editEmailValue, setEditEmailValue] = useState('');
  const [editingInfo, setEditingInfo] = useState(false);
  const [savingInfo, setSavingInfo] = useState(false);
  const [infoForm, setInfoForm] = useState({ name: '', nif: '', email: '', phone: '', address: '', city: '', contactPhone: '' });

  const openEditInfo = (t: TenantResponse) => {
    setInfoForm({ name: t.name, nif: t.nif ?? '', email: t.email ?? '', phone: t.phone ?? '', address: t.address ?? '', city: t.city ?? '', contactPhone: t.contactPhone ?? '' });
    setEditingInfo(true);
  };

  const saveInfo = async () => {
    setSavingInfo(true);
    try {
      await updateTenant(id, {
        name: infoForm.name || undefined,
        nif: infoForm.nif || undefined,
        email: infoForm.email || undefined,
        phone: infoForm.phone || undefined,
        address: infoForm.address || undefined,
        city: infoForm.city || undefined,
        contactPhone: infoForm.contactPhone || undefined,
      });
      qc.invalidateQueries({ queryKey: ['tenant', id] });
      toast('success', 'Dades actualitzades');
      setEditingInfo(false);
    } catch {
      toast('error', 'Error desant les dades');
    } finally {
      setSavingInfo(false);
    }
  };

  const toggleActive = async (current: boolean) => {
    setTogglingActive(true);
    try {
      await updateTenant(id, { isActive: !current });
      qc.invalidateQueries({ queryKey: ['tenant', id] });
      toast('success', !current ? 'Tenant activat' : 'Tenant desactivat');
    } catch {
      toast('error', 'Error actualitzant l\'estat del tenant');
    } finally {
      setTogglingActive(false);
    }
  };

  const saveEmail = async () => {
    setSavingInfo(true);
    try {
      await updateTenant(id, { email: editEmailValue || undefined });
      qc.invalidateQueries({ queryKey: ['tenant', id] });
      toast('success', 'Email actualitzat');
      setEditingEmail(false);
    } catch {
      toast('error', 'Error desant l\'email');
    } finally {
      setSavingInfo(false);
    }
  };

  const toggleFree = async (current: boolean) => {
    setTogglingFree(true);
    try {
      await updateTenant(id, { isFree: !current });
      qc.invalidateQueries({ queryKey: ['tenant', id] });
      toast('success', !current ? 'Compte marcat com a gratuït' : 'Facturació activada');
    } catch {
      toast('error', 'Error actualitzant la facturació');
    } finally {
      setTogglingFree(false);
    }
  };

  const handleRemovePhase = async (phase: string) => {
    const current = tenant?.contractedPhases ?? [];
    const updated = current.filter(p => p !== phase);
    try {
      await updateTenant(id, { contractedPhases: updated });
      qc.invalidateQueries({ queryKey: ['tenant', id] });
      toast('success', `Fase ${phase} eliminada del contracte`);
    } catch {
      toast('error', 'Error eliminant la fase');
    }
  };

  const handleTogglePhase = async (phase: string, enable: boolean) => {
    const contracted = tenant?.contractedPhases ?? [];
    const currentActive = tenant?.activePhases ?? contracted;
    const updated = enable
      ? Array.from(new Set([...currentActive, phase])).sort()
      : currentActive.filter(p => p !== phase);
    try {
      await updateTenant(id, { activePhases: updated });
      qc.invalidateQueries({ queryKey: ['tenant', id] });
      toast('success', `Fase ${phase} ${enable ? 'activada' : 'desactivada'}`);
    } catch {
      toast('error', 'Error canviant estat de la fase');
    }
  };

  const { data: tenant, isLoading: loadingTenant, error: tenantErr } = useQuery({
    queryKey: ['tenant', id],
    queryFn: () => getTenant(id),
  });

  const { data: setup, isLoading: loadingSetup } = useQuery({
    queryKey: ['tenant-setup', id],
    queryFn: () => getTenantSetup(id),
    enabled: !!tenant,
  });

  const { data: services } = useQuery({
    queryKey: ['catalog-services'],
    queryFn: () => listCatalogServices(),
  });

  const { data: landings } = useQuery({
    queryKey: ['tenant-landings', id],
    queryFn: () => listLandings(id),
    enabled: !!tenant,
  });

  const { data: budgets, refetch: refetchBudgets } = useQuery({
    queryKey: ['budgets', id],
    queryFn: () => listBudgets(id),
    enabled: !!tenant,
  });

  // Status queries — reuse React Query cache from sub-components, no extra network calls
  const { data: agentChannels } = useQuery({
    queryKey: ['agent-channels', id],
    queryFn: () => getAgentChannels(id),
    enabled: !!tenant,
    retry: false,
  });
  const { data: tgConfig } = useQuery({
    queryKey: ['tg-config', id],
    queryFn: () => getTelegramConfig(id),
    enabled: !!tenant,
    retry: false,
  });
  const { data: waConfig } = useQuery({
    queryKey: ['wa-config', id],
    queryFn: () => getWhatsAppConfig(id),
    enabled: !!tenant,
    retry: false,
  });
  const { data: gcConfig } = useQuery({
    queryKey: ['gc-config', id],
    queryFn: () => getGoCardlessConfig(id),
    enabled: !!tenant,
    retry: false,
  });
  const { data: usageStats } = useQuery({
    queryKey: ['channel-usage-stats', id],
    queryFn: () => getChannelUsageStats(id),
    enabled: !!tenant,
    retry: false,
    staleTime: 5 * 60 * 1000,
  });

  const [secCollapsed, setSecCollapsed] = useState<Record<string, boolean>>({});
  const toggleSec = (key: string) => setSecCollapsed(p => ({ ...p, [key]: !p[key] }));

  const invalidateTenant = () => qc.invalidateQueries({ queryKey: ['tenant', id] });

  const invalidateSetup = () => {
    qc.invalidateQueries({ queryKey: ['tenant-setup', id] });
    qc.invalidateQueries({ queryKey: ['tenant-landings', id] });
  };

  if (loadingTenant) {
    return (
      <PortalShell breadcrumb="admin · tenants · carregant" backHref={`/${locale}/portal/admin/tenants`}>
        <div className="p-4 sm:p-8 space-y-6">
          <div className="flex justify-center py-12">
            <span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          </div>
        </div>
      </PortalShell>
    );
  }

  if (tenantErr || !tenant) {
    return (
      <PortalShell breadcrumb="admin · tenants · error" backHref={`/${locale}/portal/admin/tenants`}>
        <div className="p-4 sm:p-8 text-center py-12">
          <IconSet.AlertCircle size={28} stroke="#ff6666" className="mx-auto mb-3" />
          <div className="f-display font-bold text-sm mb-1">Error carregant el tenant</div>
          <p className="f-mono text-xs text-ink-2 mb-4">No s'ha pogut carregar la informació del tenant</p>
          <AMGButton size="sm" onClick={() => window.location.reload()}>Reintentar</AMGButton>
        </div>
      </PortalShell>
    );
  }

  const landingCount = landings?.length ?? 0;
  const profileCount = setup?.profiles.length ?? 0;
  const serviceCount = setup?.profiles.reduce((acc, p) =>
    acc + p.phases.reduce((a, ph) => a + ph.services.length, 0), 0) ?? 0;

  // Section status derivation
  const secStatus = {
    info:      (tenant?.email ? 'active' : 'warning') as SectionStatus,
    billing:   'neutral' as SectionStatus,
    contract:  ((tenant?.contractedPhases?.length ?? 0) > 0 ? 'active' : tenant?.sector ? 'warning' : 'neutral') as SectionStatus,
    services:  ((setup?.profiles?.length ?? 0) > 0 || (setup?.standalone?.length ?? 0) > 0 ? 'active' : 'neutral') as SectionStatus,
    agent:     (agentChannels?.isActive ? 'active' : agentChannels ? 'inactive' : 'neutral') as SectionStatus,
    telegram:  (tgConfig?.status === 'CONNECTED' ? 'active' : tgConfig ? 'warning' : 'neutral') as SectionStatus,
    whatsapp:  (waConfig?.status === 'CONNECTED' ? 'active' : waConfig ? 'warning' : 'neutral') as SectionStatus,
    gocardless:(gcConfig?.isActive ? 'active' : 'neutral') as SectionStatus,
    budgets:   ((budgets?.content.length ?? 0) > 0 ? 'active' : 'neutral') as SectionStatus,
    lifecycle: 'neutral' as SectionStatus,
  };

  // Find services pending configuration that have a wizard defined
  const pendingServices: Array<{ serviceId: string; serviceName: string; serviceType: string; slug: string }> = [];
  for (const p of setup?.profiles ?? []) {
    for (const ph of p.phases) {
      for (const svc of ph.services) {
        const isPending = svc.status === 'PENDING' || svc.status === 'CONFIGURING' || svc.status === 'AWAITING_CLIENT';
        if (isPending && getWizardConfig(svc.service.slug, svc.service.type)) {
          pendingServices.push({ serviceId: svc.service.id, serviceName: svc.service.name, serviceType: svc.service.type, slug: svc.service.slug });
        }
      }
    }
  }

  return (
    <PortalShell breadcrumb={`admin · tenants · ${tenant.name}`} backHref={`/${locale}/portal/admin/tenants`}>
      <div className="p-4 sm:p-8 space-y-6">
        {/* Header */}
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / tenants /</span>
            <div className="flex items-center gap-3 mt-1">
              <div className="f-display font-bold text-xl">{tenant.name}</div>
              {tenant.isActive
                ? <AMGBadge tone="success">Actiu</AMGBadge>
                : <AMGBadge tone="neutral">Inactiu</AMGBadge>}
            </div>
            <div className="flex flex-wrap gap-x-4 gap-y-1 mt-2 text-sm text-ink-2">
              {editingEmail ? (
                <span className="flex items-center gap-1">
                  <IconSet.Mail size={12} />
                  <input
                    type="email"
                    value={editEmailValue}
                    onChange={(e) => setEditEmailValue(e.target.value)}
                    onBlur={() => { if (editEmailValue !== tenant.email) saveEmail(); else setEditingEmail(false); }}
                    onKeyDown={(e) => { if (e.key === 'Enter') saveEmail(); if (e.key === 'Escape') { setEditEmailValue(tenant.email ?? ''); setEditingEmail(false); } }}
                    className="bg-[rgba(255,255,255,0.06)] border border-border-base px-2 py-0.5 text-sm text-ink-0 rounded focus:outline-none focus:border-accent w-48"
                    autoFocus
                    ref={(el) => el?.select()}
                  />
                  <button onClick={saveEmail} className="text-accent-light hover:text-accent p-0.5"><IconSet.Check size={12} /></button>
                  <button onClick={() => { setEditEmailValue(tenant.email ?? ''); setEditingEmail(false); }} className="text-ink-2 hover:text-ink-0 p-0.5"><IconSet.X size={12} /></button>
                </span>
              ) : (
                <button onClick={() => { setEditEmailValue(tenant.email ?? ''); setEditingEmail(true); }} className="flex items-center gap-1 hover:text-accent-light transition-colors group">
                  <IconSet.Mail size={12} />
                  <span>{tenant.email || '—'}</span>
                  <IconSet.Edit size={10} className="opacity-0 group-hover:opacity-100 transition-opacity ml-0.5" />
                </button>
              )}
              {tenant.phone && <span className="flex items-center gap-1"><IconSet.Smartphone size={12} />{tenant.phone}</span>}
              <span className="f-mono text-xs text-ink-3">/{tenant.slug}</span>
              <span className="f-mono text-xs text-ink-3">Creat {fmtDate(tenant.createdAt)}</span>
            </div>
          </div>
          <div className="flex gap-2 flex-wrap">
            <AMGButton
              size="sm"
              variant={tenant.isActive ? 'ghost' : 'secondary'}
              disabled={togglingActive}
              onClick={() => toggleActive(tenant.isActive)}
            >
              {tenant.isActive ? 'Desactivar' : 'Activar'}
            </AMGButton>
            <AMGButton
              size="sm"
              icon={IconSet.Plus}
              onClick={() => window.location.href = `/portal/landings/new?tenantId=${id}`}
            >
              Crear landing
            </AMGButton>
            <AMGButton
              size="sm"
              icon={IconSet.Search}
              onClick={() => router.push(`/${locale}/portal/admin/tenants/${id}/diagnostic`)}
            >
              Diagnòstic →
            </AMGButton>
            <AMGButton
              size="sm"
              variant="secondary"
              icon={IconSet.Receipt}
              onClick={() => setShowNewBudget(true)}
            >
              Nou pressupost
            </AMGButton>
            {pendingServices.map((svc) => (
              <AMGButton
                key={svc.serviceId}
                size="sm"
                variant="secondary"
                onClick={() => window.location.href = `/portal/admin/tenants/${id}/services/${svc.serviceId}/setup`}
              >
                Configurar {svc.serviceName}
              </AMGButton>
            ))}
            <AMGButton
              size="sm"
              icon={IconSet.Sparkles}
              onClick={() => router.push(`/${locale}/portal/admin/tenants/${id}/wizard`)}
            >
              Setup Wizard
            </AMGButton>
            <AMGButton
              size="sm"
              variant="ghost"
              icon={IconSet.Bell}
              onClick={() => router.push(`/${locale}/portal/admin/tenants/${id}/notifications`)}
            >
              Notificacions
            </AMGButton>
            <AMGButton
              size="sm"
              variant="ghost"
              icon={IconSet.BarChart}
              onClick={() => router.push(`/${locale}/portal/analytics`)}
            >
              Analítica
            </AMGButton>
            <AMGButton
              size="sm"
              variant="ghost"
              icon={IconSet.FileText}
              onClick={() => router.push(`/${locale}/portal/admin/tenants/${id}/visits`)}
            >
              Visites
            </AMGButton>
            <AMGButton
              size="sm"
              variant="ghost"
              icon={IconSet.Globe}
              onClick={() => router.push(`/${locale}/portal/admin/tenants/${id}/social`)}
            >
              Social
            </AMGButton>
            <button
              type="button"
              onClick={() => setShowDeleteTenant(true)}
              className="px-3 py-1.5 rounded text-xs f-mono font-semibold border border-red-500/40 bg-[rgba(239,68,68,0.06)] text-red-400 hover:bg-[rgba(239,68,68,0.15)] transition"
            >
              Eliminar
            </button>
          </div>
        </div>

        {/* Stat cards */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="card-clip amg-card p-5">
            <span className="f-mono uppercase text-label tracking-widest text-ink-3">Landings</span>
            <div className="f-display font-bold text-2xl text-accent-light mt-2">{landingCount}</div>
          </div>
          <div className="card-clip amg-card p-5">
            <span className="f-mono uppercase text-label tracking-widest text-ink-3">Perfils</span>
            <div className="f-display font-bold text-2xl text-accent-light mt-2">{profileCount}</div>
          </div>
          <div className="card-clip amg-card p-5">
            <span className="f-mono uppercase text-label tracking-widest text-ink-3">Serveis actius</span>
            <div className="f-display font-bold text-2xl text-accent-light mt-2">{serviceCount}</div>
          </div>
        </div>

        {/* Dades d'identificació */}
        <CollapsibleSection
          eyebrow="Identificació" title="Dades d'identificació"
          status={secStatus.info}
          warning={!tenant.email ? 'Falta email' : undefined}
          collapsed={!!secCollapsed['info']} onToggle={() => toggleSec('info')}
        >
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
            <AMGSectionTitle eyebrow="Identificació" title="Dades d'identificació" />
            {!editingInfo ? (
              <AMGButton size="sm" variant="ghost" icon={IconSet.Edit} onClick={() => openEditInfo(tenant)}>
                Editar
              </AMGButton>
            ) : (
              <div className="flex gap-2">
                <AMGButton size="sm" variant="ghost" onClick={() => setEditingInfo(false)}>Cancel·lar</AMGButton>
                <AMGButton size="sm" loading={savingInfo} onClick={saveInfo}>Desar</AMGButton>
              </div>
            )}
          </div>
          <div className="p-5">
            {editingInfo ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {[
                  { key: 'name', label: 'Nom empresa', placeholder: 'Empresa SL' },
                  { key: 'nif', label: 'NIF / CIF', placeholder: 'B12345678' },
                  { key: 'email', label: 'Correu electrònic', placeholder: 'contacte@empresa.com' },
                  { key: 'phone', label: 'Telèfon', placeholder: '+34612345678' },
                  { key: 'contactPhone', label: 'Telèfon de contacte', placeholder: '+34612345678' },
                  { key: 'address', label: 'Adreça', placeholder: 'Carrer Exemple, 1' },
                  { key: 'city', label: 'Municipi', placeholder: 'Palma' },
                ].map(({ key, label, placeholder }) => (
                  <div key={key} className={key === 'address' || key === 'city' ? 'sm:col-span-2' : ''}>
                    <label className="f-mono text-[10px] uppercase tracking-wider text-ink-3 block mb-1.5">{label}</label>
                    <input
                      type="text"
                      value={infoForm[key as keyof typeof infoForm]}
                      onChange={(e) => setInfoForm(f => ({ ...f, [key]: e.target.value }))}
                      placeholder={placeholder}
                      className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00] placeholder:text-ink-3"
                    />
                  </div>
                ))}
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {[
                  { label: 'NIF / CIF', value: tenant.nif },
                  { label: 'Correu electrònic', value: tenant.email },
                  { label: 'Telèfon', value: tenant.phone },
                  { label: 'Telèfon de contacte', value: tenant.contactPhone },
                  { label: 'Adreça', value: tenant.address },
                  { label: 'Municipi', value: tenant.city },
                ].map(({ label, value }) => (
                  <div key={label}>
                    <div className="f-mono text-label uppercase text-ink-3 mb-1">{label}</div>
                    <div className="text-sm text-ink-1">{value || <span className="text-ink-3 italic">—</span>}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
        </CollapsibleSection>

        {/* Facturació */}
        <CollapsibleSection
          eyebrow="Compte" title="Compte gratuït"
          status={secStatus.billing}
          collapsed={!!secCollapsed['billing']} onToggle={() => toggleSec('billing')}
        >
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base">
            <AMGSectionTitle eyebrow="Compte" title="Compte gratuït" />
          </div>
          <div className="p-5">
            <button
              type="button"
              disabled={togglingFree}
              onClick={() => toggleFree(tenant.isFree)}
              className={`flex items-center gap-3 w-full max-w-sm px-4 py-3 border rounded text-sm transition ${
                tenant.isFree
                  ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.08)] text-white'
                  : 'border-border-base hover:border-ink-2 text-ink-2'
              } ${togglingFree ? 'opacity-50 cursor-not-allowed' : ''}`}
            >
              <div className={`w-10 h-5 rounded-full transition-colors relative flex-shrink-0 ${tenant.isFree ? 'bg-[#FF6B00]' : 'bg-[rgba(255,255,255,0.12)]'}`}>
                <div className={`absolute top-0.5 w-4 h-4 rounded-full bg-white shadow transition-all ${tenant.isFree ? 'left-5' : 'left-0.5'}`} />
              </div>
              <div>
                <div className="font-semibold">{tenant.isFree ? 'Compte gratuït activat' : 'Compte de pagament'}</div>
                <div className="text-xs opacity-60">
                  {tenant.isFree
                    ? 'No es generen factures ni quotes mensuals'
                    : 'Es generen factures i quotes mensuals'}
                </div>
              </div>
            </button>
          </div>
        </div>
        </CollapsibleSection>

        {/* Contracte NexeLocal */}
        <CollapsibleSection
          eyebrow="Grandària" title="Contracte NexeLocal"
          status={secStatus.contract}
          warning={(tenant.contractedPhases?.length ?? 0) === 0 && tenant.sector ? 'Sense fases' : undefined}
          collapsed={!!secCollapsed['contract']} onToggle={() => toggleSec('contract')}
        >
          <ContractSection tenant={tenant} onRefresh={invalidateTenant} />
        </CollapsibleSection>

        {/* Cicle de vida de facturació i onboarding */}
        <CollapsibleSection
          eyebrow="Onboarding" title="Cicle de Vida"
          status={secStatus.lifecycle}
          collapsed={!!secCollapsed['lifecycle']} onToggle={() => toggleSec('lifecycle')}
        >
          <div className="amg-card card-clip">
            <LifecycleSection tenant={tenant} onRefresh={invalidateTenant} />
          </div>
        </CollapsibleSection>

        {/* Serveis assignats */}
        <CollapsibleSection
          eyebrow="Assignació" title="Serveis assignats"
          status={secStatus.services}
          collapsed={!!secCollapsed['services']} onToggle={() => toggleSec('services')}
        >
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex flex-wrap items-center justify-between gap-2">
            <AMGSectionTitle eyebrow="Assignació" title="Serveis assignats" />
            <div className="flex items-center gap-2">
              <AMGButton size="sm" variant="ghost" icon={IconSet.Layers} onClick={() => setShowAddPhase(true)}>Fase</AMGButton>
              <AMGButton size="sm" variant="ghost" icon={IconSet.Zap} onClick={() => setShowAddService(true)}>Servei</AMGButton>
            </div>
          </div>
          {loadingSetup ? (
            <div className="flex justify-center py-8">
              <span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
            </div>
          ) : setup ? (
            <SetupSection
              setup={setup}
              tenantId={id}
              contractedPhases={tenant?.contractedPhases}
              activePhases={tenant?.activePhases ?? tenant?.contractedPhases}
              onRefresh={invalidateSetup}
              onRemovePhase={handleRemovePhase}
              onTogglePhase={handleTogglePhase}
            />
          ) : (
            <div className="p-8 text-center">
              <IconSet.AlertCircle size={28} stroke="#ff6666" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Error de càrrega</div>
            </div>
          )}
        </div>
        </CollapsibleSection>

        {/* Agent IA & Canals */}
        <CollapsibleSection
          sectionId="section-agent-config"
          eyebrow="IA" title="Agent & Canals"
          status={secStatus.agent}
          collapsed={!!secCollapsed['agent']} onToggle={() => toggleSec('agent')}
        >
          <div className="amg-card card-clip p-5 space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-semibold text-ink-1">
                  {agentChannels?.isActive
                    ? <span className="text-green-400">● Agent actiu</span>
                    : <span className="text-ink-3">○ Agent aturat</span>
                  }
                  {agentChannels?.agentMode && (
                    <span className="ml-2 f-mono text-[10px] text-ink-3 uppercase">
                      · mode {agentChannels.agentMode}
                    </span>
                  )}
                </p>
                <p className="text-xs text-ink-3 mt-1">
                  {agentChannels?.telegramLinked && '📱 Telegram  '}
                  {agentChannels?.whatsappEnabled && '📞 WhatsApp  '}
                  {agentChannels?.widgetEnabled && '💬 Widget'}
                  {!agentChannels?.telegramLinked && !agentChannels?.whatsappEnabled && !agentChannels?.widgetEnabled && 'Cap canal configurat'}
                </p>
              </div>
              <a
                href="/portal/agents"
                className="f-mono text-xs uppercase text-accent-light border border-accent/40 hover:border-accent hover:bg-accent/10 px-4 h-8 flex items-center gap-1.5 shrink-0 transition-colors rounded"
              >
                <IconSet.Bot size={12} />
                Configurar agent →
              </a>
            </div>
          </div>
        </CollapsibleSection>

        {/* Telegram Bot per tenant */}
        <CollapsibleSection
          sectionId="section-telegram"
          eyebrow="Canal" title="Telegram Bot"
          status={secStatus.telegram}
          collapsed={!!secCollapsed['telegram']} onToggle={() => toggleSec('telegram')}
        >
          <TelegramBotCard tenantId={id} />
        </CollapsibleSection>

        {/* WhatsApp Business API */}
        <CollapsibleSection
          sectionId="section-whatsapp"
          eyebrow="Canal" title="WhatsApp Business"
          status={secStatus.whatsapp}
          collapsed={!!secCollapsed['whatsapp']} onToggle={() => toggleSec('whatsapp')}
        >
          <WhatsAppMetaCard tenantId={id} />
        </CollapsibleSection>

        {/* Web Hosting */}
        <CollapsibleSection
          eyebrow="Web" title="Allotjament web"
          status="neutral"
          collapsed={!!secCollapsed['hosting']} onToggle={() => toggleSec('hosting')}
        >
          <TenantWebSection tenantId={id} tenantEmail={tenant?.email ?? ''} />
        </CollapsibleSection>

        {/* Meta Ads Analytics */}
        <CollapsibleSection
          eyebrow="Anuncis" title="Meta Ads Analytics"
          status="neutral"
          collapsed={!!secCollapsed['meta-ads']} onToggle={() => toggleSec('meta-ads')}
        >
          <MetaAdsConfigCard tenantId={id} />
        </CollapsibleSection>

        {/* GoCardless SEPA */}
        <CollapsibleSection
          eyebrow="Pagament" title="GoCardless SEPA"
          status={secStatus.gocardless}
          collapsed={!!secCollapsed['gocardless']} onToggle={() => toggleSec('gocardless')}
        >
          <GoCardlessCard tenantId={id} />
        </CollapsibleSection>

        {/* Pressupostos */}
        <CollapsibleSection
          sectionId="section-budgets"
          eyebrow="Facturació" title="Pressupostos"
          status={secStatus.budgets}
          collapsed={!!secCollapsed['budgets']} onToggle={() => toggleSec('budgets')}
        >
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
            <AMGSectionTitle eyebrow="Facturació" title="Pressupostos" />
            <AMGButton size="sm" icon={IconSet.Plus} onClick={() => setShowNewBudget(true)}>
              Nou pressupost
            </AMGButton>
          </div>
          <div className="p-5">
            {!budgets || budgets.content.length === 0 ? (
              <div className="text-center py-6">
                <IconSet.Receipt size={28} stroke="#64748b" className="mx-auto mb-3" />
                <p className="text-sm text-ink-2">Cap pressupost generat per aquest tenant.</p>
              </div>
            ) : (
              <div className="space-y-2">
                {budgets.content.map((b) => (
                  <button
                    key={b.id}
                    type="button"
                    onClick={() => setSelectedBudget(b)}
                    className="w-full flex items-center justify-between px-4 py-3 bg-[rgba(255,255,255,0.02)] border border-border-base rounded hover:border-[#FF6B00] hover:bg-[rgba(255,107,0,0.04)] transition cursor-pointer text-left"
                  >
                    <div className="flex items-center gap-3">
                      <AMGBadge tone={
                        b.status === 'ACCEPTED' ? 'success'
                        : b.status === 'REJECTED' ? 'danger'
                        : b.status === 'SENT' ? 'info'
                        : 'neutral'
                      }>
                        {b.status}
                      </AMGBadge>
                      <span className="f-mono text-sm text-ink-1">{b.budgetNumber}</span>
                      {b.sentAt && (
                        <span className="f-mono text-xs text-ink-3">Enviat {fmtDate(b.sentAt)}</span>
                      )}
                    </div>
                    <div className="flex items-center gap-3">
                      <span className="f-display font-bold text-sm text-white">{b.total.toFixed(2)} €</span>
                      <IconSet.Chevron size={14} className="text-ink-3" />
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
        </CollapsibleSection>

        {/* Acord de Tractament de Dades */}
        <DpaSection tenantId={id} locale={locale} />

        {/* Plantilles de documents */}
        <CollapsibleSection
          eyebrow="Documents" title="Plantilles de documents"
          status="neutral"
          collapsed={!!secCollapsed['doc-templates']} onToggle={() => toggleSec('doc-templates')}
        >
          <div className="amg-card card-clip p-5 space-y-3">
            <p className="f-mono text-xs text-ink-2">
              Crea plantilles de factura, pressupost, albarà, etc. perquè el tenant les usi per generar documents pels seus clients.
            </p>
            <div className="flex gap-3 flex-wrap">
              <a
                href={`/${locale}/portal/admin/documents?tenantId=${id}`}
                className="inline-flex items-center gap-2 px-3 py-1.5 border border-border-base text-xs f-mono text-ink-1 hover:border-[#FF6B00] hover:text-accent-light transition-colors"
              >
                Veure plantilles
              </a>
              <a
                href={`/${locale}/portal/admin/documents/new?tenantId=${id}`}
                className="inline-flex items-center gap-2 px-3 py-1.5 border border-[#FF6B00] bg-accent-muted text-xs f-mono text-accent-light hover:bg-[rgba(255,107,0,0.15)] transition-colors"
              >
                + Nova plantilla
              </a>
              <a
                href={`/${locale}/portal/admin/documents/list?tenantId=${id}`}
                className="inline-flex items-center gap-2 px-3 py-1.5 border border-border-base text-xs f-mono text-ink-1 hover:border-[#FF6B00] hover:text-accent-light transition-colors"
              >
                Documents generats
              </a>
            </div>
          </div>
        </CollapsibleSection>

        {/* Ús de canals */}
        <CollapsibleSection
          eyebrow="Últims 30 dies" title="Activitat de canals"
          status={usageStats && (usageStats.whatsappMessages + usageStats.whatsappMetaMessages + usageStats.telegramMessages + usageStats.emailMessages + usageStats.chatMessages) > 0 ? 'active' : 'neutral'}
          collapsed={!!secCollapsed['usage']} onToggle={() => toggleSec('usage')}
        >
          <div className="amg-card card-clip p-4 sm:p-5">
            {usageStats ? (
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                {[
                  { label: 'WhatsApp (Twilio)', value: usageStats.whatsappMessages, icon: '📱' },
                  { label: 'WhatsApp Meta',     value: usageStats.whatsappMetaMessages, icon: '💚' },
                  { label: 'Telegram',          value: usageStats.telegramMessages, icon: '✈️' },
                  { label: 'Email',             value: usageStats.emailMessages, icon: '✉️' },
                  { label: 'Xat web',           value: usageStats.chatMessages, icon: '💬' },
                  { label: 'Tokens IA',         value: usageStats.aiTokens.toLocaleString('ca-ES'), icon: '🤖' },
                ].map(({ label, value, icon }) => (
                  <div key={label} className="bg-[#0d0d1a] border border-border-base rounded p-3">
                    <div className="text-base mb-1">{icon}</div>
                    <div className="f-mono text-lg font-bold text-ink-0">{value}</div>
                    <div className="f-mono text-[9px] uppercase tracking-wider text-ink-3 mt-0.5">{label}</div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="flex justify-center py-6">
                <span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
              </div>
            )}
          </div>
        </CollapsibleSection>

        {/* Catàleg de serveis */}
        <CollapsibleSection
          eyebrow="Catàleg" title="Serveis disponibles"
          status="neutral"
          collapsed={!!secCollapsed['catalog']} onToggle={() => toggleSec('catalog')}
        >
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base">
            <AMGSectionTitle eyebrow="Catàleg" title="Serveis disponibles" />
          </div>
          <div className="overflow-x-auto">
            {services ? (
              <ServiceCatalogTable services={services} />
            ) : (
              <div className="flex justify-center py-8">
                <span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
              </div>
            )}
          </div>
        </div>
        </CollapsibleSection>
      </div>

      {showAssignProfile && (
        <AssignProfileModal tenantId={id} onClose={() => setShowAssignProfile(false)} onAssigned={invalidateSetup} />
      )}
      {showAddPhase && (
        <AddPhaseModal tenantId={id} onClose={() => setShowAddPhase(false)} onAdded={invalidateSetup} />
      )}
      {showAddService && (
        <AddServiceModal tenantId={id} onClose={() => setShowAddService(false)} onAdded={invalidateSetup} />
      )}

      {showNewBudget && (
        <NewBudgetModal
          tenantId={id}
          tenant={tenant}
          setup={setup ?? null}
          onClose={() => setShowNewBudget(false)}
          onCreated={() => refetchBudgets()}
        />
      )}

      {selectedBudget && (
        <BudgetDetailModal
          budget={selectedBudget}
          tenantId={id}
          tenant={tenant}
          setup={setup ?? null}
          onClose={() => setSelectedBudget(null)}
          onRefresh={() => { refetchBudgets(); setSelectedBudget(null); }}
        />
      )}

      {showDeleteTenant && (
        <DeleteTenantModal
          tenantId={id}
          tenantName={tenant.name}
          onClose={() => setShowDeleteTenant(false)}
          onDeleted={() => { window.location.href = '/portal/admin/tenants'; }}
        />
      )}
    </PortalShell>
  );
}
