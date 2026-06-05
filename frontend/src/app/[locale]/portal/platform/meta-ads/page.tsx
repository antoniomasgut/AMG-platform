'use client';

import { useEffect } from 'react';
import { useLocale } from 'next-intl';
import { useQuery } from '@tanstack/react-query';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { getPlatformSelf } from '@/services/platform';

export default function PlatformMetaAdsPage() {
  const locale = useLocale();

  const { data: self, isLoading } = useQuery({
    queryKey: ['platform-self'],
    queryFn: getPlatformSelf,
    retry: false,
  });

  const tenantId = self?.tenantId || '';
  const isConfigured = tenantId.length > 0;

  useEffect(() => {
    if (isConfigured) {
      window.location.href = `/${locale}/portal/admin/tenants/${tenantId}/meta-ads`;
    }
  }, [isConfigured, tenantId, locale]);

  return (
    <PortalShell breadcrumb="plataforma · Meta Ads">
      <div className="p-8 space-y-4">
        {isLoading && <p className="text-sm text-ink-3">Carregant...</p>}
        {!isLoading && !isConfigured && (
          <div className="space-y-3">
            <p className="text-sm text-ink-2">
              El tenant de la plataforma no està configurat.
            </p>
            <AMGButton size="sm" onClick={() => window.location.href = `/${locale}/portal/platform/config`}>
              Anar a Configuració →
            </AMGButton>
          </div>
        )}
        {isConfigured && <p className="text-sm text-ink-3">Redirigint...</p>}
      </div>
    </PortalShell>
  );
}
