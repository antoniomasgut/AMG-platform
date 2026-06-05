'use client';

import { useLocale } from 'next-intl';
import { useQuery } from '@tanstack/react-query';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { I } from '@/components/ui/icons';
import { getPlatformSelf } from '@/services/platform';
import { getTenant } from '@/services/admin';
import { getMetaAdsConfig } from '@/services/meta-ads';


function SetupBanner({ locale }: { locale: string }) {
  return (
    <div className="bg-[rgba(255,107,0,0.08)] border border-[rgba(255,107,0,0.3)] rounded-xl p-6 space-y-3">
      <div className="flex items-start gap-3">
        <I.AlertCircle size={18} className="text-[#FF6B00] mt-0.5 flex-shrink-0" />
        <div>
          <h2 className="text-sm font-semibold text-ink-1">Tenant de plataforma no configurat</h2>
          <p className="text-xs text-ink-2 mt-1">
            Per fer servir els serveis d'AMG Digitalitzacions (agents, leads, Meta Ads...)
            sense crear un tenant client, cal assignar un <strong>PLATFORM_TENANT_ID</strong>.
          </p>
        </div>
      </div>
      <div className="pl-6 space-y-2 text-xs text-ink-2">
        <p>1. Crea un tenant nou a <a href={`/${locale}/portal/admin/tenants`} className="text-accent-light hover:underline">Clients → Tenants</a> amb nom "AMG Digitalitzacions".</p>
        <p>2. Copia el UUID i ves a <a href={`/${locale}/portal/platform/config`} className="text-accent-light hover:underline">Configuració</a> → secció GENERAL → camp <code className="bg-surface-overlay px-1 rounded">PLATFORM_TENANT_ID</code>.</p>
        <p>3. Torna aquí — apareixerà el resum de la plataforma.</p>
      </div>
      <div className="pl-6">
        <AMGButton size="sm" onClick={() => window.location.href = `/${locale}/portal/platform/config`}>
          Anar a Configuració →
        </AMGButton>
      </div>
    </div>
  );
}

export default function PlatformHubPage() {
  const locale = useLocale();

  const { data: self, isLoading: selfLoading } = useQuery({
    queryKey: ['platform-self'],
    queryFn: getPlatformSelf,
    retry: false,
  });

  const platformTenantId = self?.tenantId || '';
  const isConfigured = platformTenantId.length > 0;

  const { data: tenant } = useQuery({
    queryKey: ['tenant', platformTenantId],
    queryFn: () => getTenant(platformTenantId),
    enabled: isConfigured,
  });

  const { data: metaConfig } = useQuery({
    queryKey: ['meta-ads-config', platformTenantId],
    queryFn: () => getMetaAdsConfig(platformTenantId),
    enabled: isConfigured,
    retry: false,
  });

  if (selfLoading) {
    return (
      <PortalShell breadcrumb="plataforma">
        <div className="p-8 text-sm text-ink-3">Carregant...</div>
      </PortalShell>
    );
  }

  return (
    <PortalShell breadcrumb="plataforma · resum">
      <div className="p-4 sm:p-8 max-w-3xl space-y-6">

        {/* Header */}
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-xs font-mono uppercase tracking-wider text-[#FF6B00] mb-1">Plataforma</p>
            <h1 className="text-2xl font-bold text-ink-1">AMG Digitalitzacions</h1>
            <p className="text-sm text-ink-3 mt-0.5">Serveis i agents del propi negoci</p>
          </div>
          {isConfigured && (
            <AMGBadge tone="success">Configurat</AMGBadge>
          )}
        </div>

        {/* Setup banner if not configured */}
        {!isConfigured && <SetupBanner locale={locale} />}

        {/* Configured state */}
        {isConfigured && (
          <>
            {/* Tenant info */}
            <div className="bg-surface-base border border-border-base rounded-lg p-4 flex items-center justify-between">
              <div>
                <p className="text-xs text-ink-3 mb-0.5">Tenant assignat</p>
                <p className="text-sm font-medium text-ink-1">{tenant?.name ?? 'AMG Digitalitzacions'}</p>
                <p className="text-xs text-ink-3 font-mono mt-0.5">{platformTenantId}</p>
              </div>
              <a
                href={`/${locale}/portal/admin/tenants/${platformTenantId}`}
                className="text-xs text-accent-light hover:underline"
              >
                Gestionar tenant →
              </a>
            </div>

            {/* Quick links grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {([
                {
                  label: 'Meta Ads AMG',
                  desc: 'Campanyes de Facebook i Instagram Ads per a AMG Digitalitzacions',
                  icon: '📢',
                  href: `/${locale}/portal/admin/tenants/${platformTenantId}/meta-ads`,
                  badge: metaConfig?.enabled ? 'Actiu' : undefined,
                },
                {
                  label: 'Agents IA',
                  desc: 'Canals, mode i base de coneixement de l\'agent d\'AMG',
                  icon: '🤖',
                  href: `/${locale}/portal/admin/tenants/${platformTenantId}/activate`,
                  badge: undefined,
                },
                {
                  label: 'Landings AMG',
                  desc: 'Pàgines de captació i webs d\'AMG Digitalitzacions',
                  icon: '🌐',
                  href: `/${locale}/portal/admin/tenants/${platformTenantId}/landings`,
                  badge: undefined,
                },
                {
                  label: 'Configuració',
                  desc: 'Claus API, PLATFORM_TENANT_ID i paràmetres del sistema',
                  icon: '⚙️',
                  href: `/${locale}/portal/platform/config`,
                  badge: undefined,
                },
              ] as const).map(link => (
                <a
                  key={link.label}
                  href={link.href}
                  className="group bg-surface-base border border-border-base rounded-lg p-4 hover:border-[#FF6B00]/50 transition-all space-y-2"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className="text-xl">{link.icon}</span>
                      <span className="text-sm font-medium text-ink-1 group-hover:text-[#FF6B00] transition-colors">{link.label}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      {link.badge && (
                        <span className="text-[10px] text-[#39d353] font-mono">✓ {link.badge}</span>
                      )}
                      <I.ArrowRight size={14} className="text-ink-3 group-hover:text-[#FF6B00] transition-colors" />
                    </div>
                  </div>
                  <p className="text-xs text-ink-3">{link.desc}</p>
                </a>
              ))}
            </div>
          </>
        )}
      </div>
    </PortalShell>
  );
}
