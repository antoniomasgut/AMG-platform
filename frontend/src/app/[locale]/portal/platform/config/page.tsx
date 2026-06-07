'use client';

import { useState } from 'react';
import { useLocale } from 'next-intl';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { IconSet } from '@/components/ui/icons';
import { useToast } from '@/lib/toast-context';
import { getPlatformSelf } from '@/services/platform';
import { getTenant, listTenants } from '@/services/admin';
import { getSystemConfig, setSystemConfig, type ConfigStatus } from '@/services/sysconfig';

const inp = "w-full bg-surface-base border border-border-base rounded px-3 py-1.5 text-sm font-mono text-ink-1 focus:outline-none focus:border-accent-light";

// Keys shown in the platform quick-config (most relevant for the platform itself)
const PLATFORM_KEYS = new Set([
  'PLATFORM_TENANT_ID',
  'ANTHROPIC_API_KEY',
  'OPENAI_API_KEY',
  'TELEGRAM_BOT_TOKEN',
  'BREVO_API_KEY',
  'BREVO_SENDER_EMAIL',
  'META_WEBHOOK_VERIFY_TOKEN',
  'META_PAGE_ACCESS_TOKEN',
]);

const CATEGORY_LABEL: Record<string, string> = {
  GENERAL: 'General',
  AGENTS: 'Agents IA',
  IMAGE_GEN: 'Generació d\'imatges',
  META_ADS: 'Meta Ads',
  INFRAOPS: 'InfraOps',
  PROSPECTING: 'Prospecció',
  PAYMENTS: 'Pagaments',
  FINOPS: 'FinOps',
  BACKUP: 'Backup',
  AUTOMATIONS: 'Automatitzacions',
  CALENDAR: 'Calendari',
  EMAIL_INBOUND: 'Email Inbound',
};

function ConfigKeyRow({ item, onSave }: {
  item: ConfigStatus;
  onSave: (key: string, value: string) => void;
}) {
  const [editing, setEditing] = useState(false);
  const [value, setValue] = useState('');
  const [show, setShow] = useState(false);

  return (
    <div className="py-3 border-b border-border-base last:border-0">
      <div className="flex items-center justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-xs font-medium text-ink-1">{item.label}</span>
            <span className="text-[10px] font-mono text-ink-3">{item.key}</span>
            {item.configured ? (
              <AMGBadge tone={item.source === 'ENV' ? 'success' : 'info'}>
                {item.source === 'ENV' ? 'Entorn' : 'DB ✓'}
              </AMGBadge>
            ) : (
              <AMGBadge tone="danger">Buit</AMGBadge>
            )}
          </div>
          <p className="text-[11px] text-ink-3 mt-0.5">{item.description}</p>
        </div>
        <div className="flex-shrink-0">
          {!item.configured && (
            <AMGButton size="sm" onClick={() => setEditing(true)}>Configurar</AMGButton>
          )}
          {item.configured && item.source === 'DB' && (
            <AMGButton size="sm" variant="secondary" onClick={() => setEditing(true)}>
              <IconSet.Edit size={12} />Editar
            </AMGButton>
          )}
        </div>
      </div>

      {editing && (
        <div className="mt-2 flex gap-2 items-center">
          <div className="flex-1 relative">
            <input
              type={item.secret && !show ? 'password' : 'text'}
              className={inp}
              value={value}
              onChange={e => setValue(e.target.value)}
              placeholder={`Introdueix ${item.label}…`}
              autoFocus
            />
            {item.secret && (
              <button type="button" onClick={() => setShow(s => !s)}
                className="absolute right-2 top-1/2 -translate-y-1/2 text-ink-3 hover:text-ink-1">
                {show ? <IconSet.EyeOff size={14} /> : <IconSet.Eye size={14} />}
              </button>
            )}
          </div>
          <AMGButton size="sm" disabled={!value.trim()}
            onClick={() => { onSave(item.key, value.trim()); setEditing(false); setValue(''); }}>
            Desar
          </AMGButton>
          <AMGButton size="sm" variant="ghost" onClick={() => { setEditing(false); setValue(''); }}>
            ×
          </AMGButton>
        </div>
      )}
    </div>
  );
}

export default function PlatformConfigPage() {
  const locale = useLocale();
  const { toast } = useToast();
  const qc = useQueryClient();

  const { data: self } = useQuery({ queryKey: ['platform-self'], queryFn: getPlatformSelf, retry: false });
  const platformTenantId = self?.tenantId || '';
  const isConfigured = platformTenantId.length > 0;

  const { data: tenant } = useQuery({
    queryKey: ['tenant', platformTenantId],
    queryFn: () => getTenant(platformTenantId),
    enabled: isConfigured,
  });

  const { data: configs = [] } = useQuery({
    queryKey: ['system-config'],
    queryFn: getSystemConfig,
  });

  const { mutate: doSave } = useMutation({
    mutationFn: ({ key, value }: { key: string; value: string }) => setSystemConfig(key, value),
    onSuccess: (_, { key }) => {
      toast('success', `${key} desada`);
      qc.invalidateQueries({ queryKey: ['system-config'] });
      qc.invalidateQueries({ queryKey: ['platform-self'] });
    },
    onError: () => toast('error', 'Error desant la clau'),
  });

  const list = (configs as ConfigStatus[]).filter(c => PLATFORM_KEYS.has(c.key));
  const missingCount = list.filter(c => !c.configured).length;

  return (
    <PortalShell breadcrumb="plataforma · configuració">
      <div className="p-4 sm:p-8 max-w-2xl space-y-6">

        {/* Header */}
        <div>
          <p className="text-xs font-mono uppercase tracking-wider text-[#FF6B00] mb-1">Plataforma AMG</p>
          <h1 className="text-2xl font-bold text-ink-1">Configuració</h1>
          <p className="text-sm text-ink-3 mt-0.5">Paràmetres de la plataforma i claus d'API principals</p>
        </div>

        {/* Platform Tenant section */}
        <div className="bg-surface-base border border-border-base rounded-xl overflow-hidden">
          <div className="px-5 py-4 border-b border-border-base flex items-center justify-between">
            <div>
              <h2 className="text-sm font-semibold text-ink-1">Tenant de la plataforma</h2>
              <p className="text-xs text-ink-3 mt-0.5">El tenant d'AMG Digitalitzacions que gestiona els serveis propis</p>
            </div>
            {isConfigured
              ? <AMGBadge tone="success">Configurat</AMGBadge>
              : <AMGBadge tone="danger">No configurat</AMGBadge>
            }
          </div>

          <div className="p-5 space-y-4">
            {isConfigured ? (
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-ink-1">{tenant?.name ?? 'Carregant...'}</p>
                  <p className="text-xs font-mono text-ink-3 mt-0.5">{platformTenantId}</p>
                </div>
                <div className="flex gap-2">
                  <a
                    href={`/${locale}/portal/admin/tenants/${platformTenantId}`}
                    className="text-xs text-accent-light hover:underline"
                  >
                    Gestionar tenant →
                  </a>
                </div>
              </div>
            ) : (
              <div className="space-y-3 text-xs text-ink-2">
                <p>Crea un tenant amb nom <strong>"AMG Digitalitzacions"</strong> i configura el seu UUID aquí baix com a <code className="bg-surface-overlay px-1 rounded">PLATFORM_TENANT_ID</code>.</p>
                <AMGButton size="sm" variant="secondary"
                  onClick={() => window.open(`/${locale}/portal/admin/tenants`, '_blank')}>
                  Crear tenant →
                </AMGButton>
              </div>
            )}
          </div>
        </div>

        {/* Platform key config */}
        <div className="bg-surface-base border border-border-base rounded-xl overflow-hidden">
          <div className="px-5 py-4 border-b border-border-base flex items-center justify-between">
            <div>
              <h2 className="text-sm font-semibold text-ink-1">Claus principals</h2>
              <p className="text-xs text-ink-3 mt-0.5">Les claus d'API que usa la plataforma directament</p>
            </div>
            {missingCount > 0 && (
              <AMGBadge tone="warning">{missingCount} sense configurar</AMGBadge>
            )}
          </div>

          <div className="px-5">
            {list.map(item => (
              <ConfigKeyRow
                key={item.key}
                item={item}
                onSave={(key, value) => doSave({ key, value })}
              />
            ))}
          </div>
        </div>

        {/* Link to full config */}
        <div className="bg-surface-base border border-border-base rounded-xl p-5 flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-ink-1">Totes les claus del sistema</p>
            <p className="text-xs text-ink-3 mt-0.5">Stripe, Holded, GCS, n8n, Twilio, Google Calendar...</p>
          </div>
          <a href={`/${locale}/portal/admin/config`} className="text-xs text-accent-light hover:underline flex items-center gap-1">
            Obrir configuració completa <IconSet.ArrowRight size={12} />
          </a>
        </div>

      </div>
    </PortalShell>
  );
}
