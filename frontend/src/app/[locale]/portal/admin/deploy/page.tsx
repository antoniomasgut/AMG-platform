'use client';

import { useLocale } from 'next-intl';
import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { IconSet } from '@/components/ui/icons';
import { getSystemConfig, type ConfigStatus } from '@/services/sysconfig';
import {
  listCommTemplates, ACTIONS, LANGUAGES, type CommTemplate,
} from '@/services/commTemplates';
import { getBookingSettings } from '@/services/booking';
import { listTenants } from '@/services/admin';

// ── Tipus ─────────────────────────────────────────────────────────────────────

type DeployStatus = 'ok' | 'warn' | 'missing';

// ── Grups de configuració del sistema ────────────────────────────────────────

const DEPLOY_GROUPS: {
  id: string;
  label: string;
  icon: keyof typeof IconSet;
  configPage: string;
  criticalKeys: string[];
  optionalKeys: string[];
}[] = [
  {
    id: 'core',
    label: 'IA & Canals base',
    icon: 'Bot',
    configPage: '/portal/admin/config',
    criticalKeys: ['ANTHROPIC_API_KEY', 'BREVO_API_KEY', 'TELEGRAM_BOT_TOKEN'],
    optionalKeys: ['AMG_SALES_CHAT_ID', 'AMG_NOTIFY_BUDGET_ACCEPTED'],
  },
  {
    id: 'whatsapp',
    label: 'WhatsApp (Twilio)',
    icon: 'Phone',
    configPage: '/portal/admin/config',
    criticalKeys: ['TWILIO_ACCOUNT_SID', 'TWILIO_AUTH_TOKEN', 'TWILIO_WHATSAPP_FROM'],
    optionalKeys: ['WHATSAPP_WEBHOOK_SECRET'],
  },
  {
    id: 'stripe',
    label: 'Pagaments (Stripe)',
    icon: 'CreditCard',
    configPage: '/portal/admin/config',
    criticalKeys: ['STRIPE_API_KEY', 'STRIPE_WEBHOOK_SECRET'],
    optionalKeys: ['STRIPE_SETUP_PRICE_ID'],
  },
  {
    id: 'calendar',
    label: 'Agenda (Google Calendar)',
    icon: 'Calendar',
    configPage: '/portal/admin/config',
    criticalKeys: ['GOOGLE_CALENDAR_SA_JSON'],
    optionalKeys: ['GOOGLE_CALENDAR_DOMAIN'],
  },
  {
    id: 'storage',
    label: 'Emmagatzematge (MinIO)',
    icon: 'Database',
    configPage: '/portal/admin/config',
    criticalKeys: ['MINIO_ENDPOINT', 'MINIO_ACCESS_KEY', 'MINIO_SECRET_KEY'],
    optionalKeys: ['MINIO_BUCKET'],
  },
  {
    id: 'meta',
    label: 'Meta (Lead Ads)',
    icon: 'Zap',
    configPage: '/portal/admin/config',
    criticalKeys: ['META_WEBHOOK_VERIFY_TOKEN'],
    optionalKeys: ['META_APP_ID', 'META_APP_SECRET'],
  },
  {
    id: 'finops',
    label: 'FinOps (Holded)',
    icon: 'BarChart',
    configPage: '/portal/admin/config',
    criticalKeys: [],
    optionalKeys: ['HOLDED_API_KEY', 'HOLDED_COMPANY_ID'],
  },
];

// ── Helpers d'estat ───────────────────────────────────────────────────────────

function calcGroupStatus(
  keys: string[],
  configMap: Map<string, ConfigStatus>,
  requiredKeys: string[],
): DeployStatus {
  if (requiredKeys.length === 0) {
    const optional = keys.filter(k => configMap.get(k)?.configured);
    return optional.length > 0 ? 'ok' : 'warn';
  }
  const allCritical = requiredKeys.every(k => configMap.get(k)?.configured);
  if (allCritical) return 'ok';
  const someCritical = requiredKeys.some(k => configMap.get(k)?.configured);
  return someCritical ? 'warn' : 'missing';
}

// ── Components d'estat ────────────────────────────────────────────────────────

function StatusIcon({ status }: { status: DeployStatus }) {
  if (status === 'ok')
    return <span className="flex items-center justify-center w-5 h-5 rounded-full bg-[#39d353]/20 text-[#39d353]"><IconSet.Check size={12} /></span>;
  if (status === 'warn')
    return <span className="flex items-center justify-center w-5 h-5 rounded-full bg-amber-400/20 text-amber-400"><IconSet.AlertTriangle size={11} /></span>;
  return <span className="flex items-center justify-center w-5 h-5 rounded-full bg-red-500/20 text-red-400"><IconSet.X size={12} /></span>;
}

function StatusLabel({ status }: { status: DeployStatus }) {
  if (status === 'ok')   return <AMGBadge tone="success">Configurat</AMGBadge>;
  if (status === 'warn') return <AMGBadge tone="warning">Parcial</AMGBadge>;
  return <AMGBadge tone="danger">Pendent</AMGBadge>;
}

// ── Secció de sistema ─────────────────────────────────────────────────────────

function SysConfigSection({ configs }: { configs: ConfigStatus[] }) {
  const locale = useLocale();
  const configMap = new Map(configs.map(c => [c.key, c]));

  return (
    <div className="space-y-3">
      {DEPLOY_GROUPS.map(group => {
        const allKeys = [...group.criticalKeys, ...group.optionalKeys];
        const status  = calcGroupStatus(allKeys, configMap, group.criticalKeys);
        const Icon    = IconSet[group.icon] as React.FC<{ size?: number }>;
        const configured = allKeys.filter(k => configMap.get(k)?.configured).length;

        return (
          <div key={group.id} className="flex items-center gap-3 p-3 border border-border-base rounded bg-surface-card hover:border-[#333] transition-colors">
            <div className="shrink-0 text-ink-3"><Icon size={16} /></div>
            <div className="flex-1 min-w-0">
              <div className="text-sm font-medium text-ink-0">{group.label}</div>
              <div className="text-xs text-ink-3 f-mono mt-0.5">
                {configured}/{allKeys.length} claus configurades
              </div>
              {status !== 'ok' && group.criticalKeys.length > 0 && (
                <div className="mt-1 flex flex-wrap gap-1">
                  {group.criticalKeys.filter(k => !configMap.get(k)?.configured).map(k => (
                    <span key={k} className="f-mono text-[9px] text-red-400 border border-red-500/30 rounded px-1">{k}</span>
                  ))}
                </div>
              )}
            </div>
            <div className="shrink-0 flex items-center gap-2">
              <StatusLabel status={status} />
              <a href={`/${locale}${group.configPage}`} className="text-ink-3 hover:text-ink-0 transition-colors">
                <IconSet.Settings size={14} />
              </a>
            </div>
          </div>
        );
      })}
    </div>
  );
}

// ── Matriu de plantilles de comunicació ──────────────────────────────────────

function CommTemplatesMatrix({ templates }: { templates: CommTemplate[] }) {
  const locale = useLocale();

  const byActionChannel = new Map<string, Set<string>>();
  for (const t of templates) {
    const k = `${t.action}:${t.channel}`;
    if (!byActionChannel.has(k)) byActionChannel.set(k, new Set());
    byActionChannel.get(k)!.add(t.language);
  }

  const totalExpected = ACTIONS.length * 2 * LANGUAGES.length;
  const totalConfigured = templates.length;
  const coverage = Math.round((totalConfigured / totalExpected) * 100);

  const overallStatus: DeployStatus =
    coverage === 100 ? 'ok' : coverage >= 50 ? 'warn' : 'missing';

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <StatusIcon status={overallStatus} />
          <span className="text-sm text-ink-1">
            {totalConfigured}/{totalExpected} plantilles configurades ({coverage}%)
          </span>
        </div>
        <a href={`/${locale}/portal/admin/comm-templates`}>
          <AMGButton variant="ghost" size="sm">
            Gestionar plantilles
            <IconSet.Chevron size={12} className="ml-1" />
          </AMGButton>
        </a>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-xs border-collapse">
          <thead>
            <tr>
              <th className="text-left f-mono text-ink-3 uppercase tracking-wider pb-2 pr-4 font-normal whitespace-nowrap">
                Acció
              </th>
              {(['EMAIL', 'WHATSAPP'] as const).map(ch => (
                LANGUAGES.map(lang => (
                  <th key={`${ch}-${lang.value}`}
                      className="text-center pb-2 px-1 font-normal">
                    <div className={`text-[8px] f-mono uppercase tracking-wide ${ch === 'EMAIL' ? 'text-blue-400' : 'text-green-400'}`}>
                      {ch === 'EMAIL' ? '✉' : '💬'}
                    </div>
                    <div className="text-ink-4 text-[9px]">{lang.value}</div>
                  </th>
                ))
              ))}
            </tr>
          </thead>
          <tbody>
            {ACTIONS.map(action => (
              <tr key={action.value} className="border-t border-border-base/30">
                <td className="py-2 pr-4 text-ink-1 whitespace-nowrap">{action.label}</td>
                {(['EMAIL', 'WHATSAPP'] as const).map(ch =>
                  LANGUAGES.map(lang => {
                    const k = `${action.value}:${ch}`;
                    const hasLang = byActionChannel.get(k)?.has(lang.value) ?? false;
                    return (
                      <td key={`${ch}-${lang.value}`} className="text-center py-2 px-1">
                        {hasLang
                          ? <span className="text-[#39d353]">✓</span>
                          : <span className="text-ink-4">·</span>}
                      </td>
                    );
                  })
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex items-center gap-4 text-[10px] f-mono text-ink-3">
        <span><span className="text-blue-400">✉</span> EMAIL · <span className="text-green-400">💬</span> WHATSAPP</span>
        <span>Columnes: ca · es · en · de per canal</span>
      </div>
    </div>
  );
}

// ── Targeta de secció ─────────────────────────────────────────────────────────

function SectionCard({
  icon, title, badge, status, children,
}: {
  icon: React.ReactNode;
  title: string;
  badge?: React.ReactNode;
  status?: DeployStatus;
  children: React.ReactNode;
}) {
  return (
    <div className="amg-card card-clip p-5 space-y-4">
      <div className="flex items-center gap-3">
        <div className="text-ink-3 shrink-0">{icon}</div>
        <h2 className="f-display font-bold text-base flex-1">{title}</h2>
        {status && <StatusIcon status={status} />}
        {badge}
      </div>
      {children}
    </div>
  );
}

// ── Secció de reserves ────────────────────────────────────────────────────────

function BookingSection() {
  const locale  = useLocale();
  const { data, isLoading } = useQuery({
    queryKey: ['booking-settings-deploy'],
    queryFn: getBookingSettings,
  });

  const status: DeployStatus = !data
    ? (isLoading ? 'warn' : 'missing')
    : data.workingDays ? 'ok' : 'warn';

  return (
    <SectionCard icon={<IconSet.Calendar size={18} />} title="Configuració de reserves (F2)" status={status}>
      {isLoading && <div className="text-sm text-ink-3 animate-pulse">Carregant…</div>}
      {!isLoading && !data && (
        <div className="text-sm text-ink-3">Sense configuració de reserves.</div>
      )}
      {data && (
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
          {[
            { label: 'Dies hàbils', value: data.workingDays || '—' },
            { label: 'Horari', value: data.startTime && data.endTime ? `${data.startTime} – ${data.endTime}` : '—' },
            { label: 'Durada slot', value: data.slotDurationMinutes ? `${data.slotDurationMinutes} min` : '—' },
            { label: 'Marge entre cites', value: data.bufferMinutes != null ? `${data.bufferMinutes} min` : '—' },
            { label: 'Avís mínim', value: data.minNoticeHours != null ? `${data.minNoticeHours} h` : '—' },
            { label: 'Màxim avançament', value: data.maxAdvanceDays != null ? `${data.maxAdvanceDays} dies` : '—' },
          ].map(({ label, value }) => (
            <div key={label} className="bg-[rgba(255,255,255,0.03)] rounded p-2.5">
              <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3 mb-1">{label}</div>
              <div className="text-sm text-ink-0 f-mono">{value}</div>
            </div>
          ))}
        </div>
      )}
      <div className="flex justify-end">
        <a href={`/${locale}/portal/admin/booking`}>
          <AMGButton variant="ghost" size="sm">
            Configurar reserves
            <IconSet.Chevron size={12} className="ml-1" />
          </AMGButton>
        </a>
      </div>
    </SectionCard>
  );
}

// ── Secció de tenants ─────────────────────────────────────────────────────────

function TenantsSection() {
  const locale = useLocale();
  const { data: allTenants } = useQuery({
    queryKey: ['tenants-deploy-all'],
    queryFn: () => listTenants({ size: 200 }),
  });
  const { data: activeTenants } = useQuery({
    queryKey: ['tenants-deploy-active'],
    queryFn: () => listTenants({ isActive: true, size: 200 }),
  });

  const total  = allTenants?.totalElements ?? 0;
  const active = activeTenants?.totalElements ?? 0;
  const inactive = total - active;

  return (
    <SectionCard icon={<IconSet.Building size={18} />} title="Resum de tenants">
      <div className="grid grid-cols-3 gap-3">
        {[
          { label: 'Total',     value: total,    color: 'text-ink-0' },
          { label: 'Actius',    value: active,   color: 'text-[#39d353]' },
          { label: 'Inactius',  value: inactive, color: 'text-ink-3' },
        ].map(({ label, value, color }) => (
          <div key={label} className="bg-[rgba(255,255,255,0.03)] rounded p-3 text-center">
            <div className={`text-2xl font-black f-display ${color}`}>{value}</div>
            <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3 mt-1">{label}</div>
          </div>
        ))}
      </div>
      <div className="flex justify-end">
        <a href={`/${locale}/portal/admin/tenants`}>
          <AMGButton variant="ghost" size="sm">
            Veure tenants
            <IconSet.Chevron size={12} className="ml-1" />
          </AMGButton>
        </a>
      </div>
    </SectionCard>
  );
}

// ── Accés ràpid ───────────────────────────────────────────────────────────────

const QUICK_LINKS: { label: string; path: string; icon: keyof typeof IconSet; desc: string }[] = [
  { label: 'Claus API',          path: '/portal/admin/config',          icon: 'Key',        desc: 'System config' },
  { label: 'Plantilles com.',    path: '/portal/admin/comm-templates',  icon: 'Mail',       desc: 'WhatsApp + Email' },
  { label: 'Reserves',           path: '/portal/admin/booking',         icon: 'Calendar',   desc: 'Horaris F2' },
  { label: 'Templates landing',  path: '/portal/admin/templates',       icon: 'Layers',     desc: 'Sectors' },
  { label: 'Vault',              path: '/portal/admin/vault/profiles',  icon: 'Lock',       desc: 'Credencials' },
  { label: 'Knowledge base',     path: '/portal/admin/knowledge',       icon: 'Book',       desc: 'Base de coneixement' },
  { label: 'Infraops',           path: '/portal/admin/infraops',        icon: 'Server',     desc: 'Monitorització' },
  { label: 'Tenants',            path: '/portal/admin/tenants',         icon: 'Building',   desc: 'Gestió tenants' },
];

// ── Pàgina principal ──────────────────────────────────────────────────────────

export default function DeployPage() {
  const locale = useLocale();

  const { data: configs = [], isLoading: loadingConfigs } = useQuery({
    queryKey: ['system-config-deploy'],
    queryFn: getSystemConfig,
  });

  const { data: templates = [], isLoading: loadingTemplates } = useQuery({
    queryKey: ['comm-templates-deploy'],
    queryFn: listCommTemplates,
  });

  // Estat global de la plataforma
  const configMap = new Map(configs.map(c => [c.key, c]));
  const criticalKeys = [
    'ANTHROPIC_API_KEY', 'BREVO_API_KEY', 'TELEGRAM_BOT_TOKEN',
    'STRIPE_API_KEY', 'GOOGLE_CALENDAR_SA_JSON',
  ];
  const criticalConfigured = criticalKeys.filter(k => configMap.get(k)?.configured).length;
  const platformStatus: DeployStatus =
    criticalConfigured === criticalKeys.length ? 'ok'
    : criticalConfigured >= 3 ? 'warn'
    : 'missing';

  return (
    <PortalShell breadcrumb="deploy">
      <div className="p-4 sm:p-6 space-y-6 max-w-5xl">

        {/* Capçalera */}
        <div className="flex items-start gap-4 flex-wrap">
          <div className="flex-1">
            <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3 mb-1">
              AMG Digitalització · Hub de desplegament
            </div>
            <h1 className="f-display font-black text-2xl">Configuració global</h1>
            <p className="text-sm text-ink-2 mt-1">
              Visió unificada de totes les àrees de configuració de la plataforma.
            </p>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <StatusIcon status={platformStatus} />
            <span className={`text-sm f-mono ${
              platformStatus === 'ok' ? 'text-[#39d353]' :
              platformStatus === 'warn' ? 'text-amber-400' : 'text-red-400'
            }`}>
              {platformStatus === 'ok' ? 'Plataforma OK' :
               platformStatus === 'warn' ? 'Configuració parcial' :
               'Configuració incompleta'}
            </span>
          </div>
        </div>

        {/* Accés ràpid */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
          {QUICK_LINKS.map(link => {
            const Icon = IconSet[link.icon] as React.FC<{ size?: number }>;
            return (
              <a key={link.path} href={`/${locale}${link.path}`}
                 className="flex items-center gap-2 px-3 py-2.5 border border-border-base rounded bg-surface-card hover:border-[#444] transition-colors group">
                <div className="text-ink-3 group-hover:text-ink-1 transition-colors shrink-0">
                  <Icon size={14} />
                </div>
                <div className="min-w-0">
                  <div className="text-xs font-medium text-ink-0 truncate">{link.label}</div>
                  <div className="f-mono text-[9px] text-ink-3 truncate">{link.desc}</div>
                </div>
              </a>
            );
          })}
        </div>

        {/* Configuració del sistema (API Keys) */}
        <SectionCard
          icon={<IconSet.Key size={18} />}
          title="Claus API i integracions"
          badge={
            <span className="f-mono text-xs text-ink-3">
              {configs.filter(c => c.configured).length}/{configs.length} configurades
            </span>
          }
        >
          {loadingConfigs ? (
            <div className="text-sm text-ink-3 animate-pulse">Carregant…</div>
          ) : (
            <SysConfigSection configs={configs} />
          )}
          <div className="flex justify-end pt-1">
            <a href={`/${locale}/portal/admin/config`}>
              <AMGButton variant="ghost" size="sm">
                Gestionar claus API
                <IconSet.Chevron size={12} className="ml-1" />
              </AMGButton>
            </a>
          </div>
        </SectionCard>

        {/* Plantilles de comunicació */}
        <SectionCard
          icon={<IconSet.Mail size={18} />}
          title="Plantilles de comunicació"
          badge={
            <span className="f-mono text-xs text-ink-3">
              {templates.length} plantilles
            </span>
          }
        >
          {loadingTemplates ? (
            <div className="text-sm text-ink-3 animate-pulse">Carregant…</div>
          ) : (
            <CommTemplatesMatrix templates={templates} />
          )}
        </SectionCard>

        {/* Configuració de reserves */}
        <BookingSection />

        {/* Resum tenants */}
        <TenantsSection />

      </div>
    </PortalShell>
  );
}
