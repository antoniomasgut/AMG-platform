'use client';

import { useTranslations } from 'next-intl';
import { useAuth } from '@/lib/auth-context';
import { PortalShell } from '@/components/portal/PortalShell';
import { ServiceGuidesList } from '@/components/guides/ServiceGuidesList';

export default function ServeisPage() {
  const t = useTranslations();
  const { user, isSuperAdmin, isAdmin } = useAuth();
  const userRole = isSuperAdmin ? 'SUPER_ADMIN' : isAdmin ? 'ADMIN' : 'CLIENT';

  if (!user?.tenantId) return null;

  return (
    <PortalShell breadcrumb="serveis">
      <div className="p-4 sm:p-6 space-y-6">
        <div>
          <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3 mb-1">
            {t('guides.title')}
          </div>
          <h1 className="f-display font-black text-2xl">{t('guides.subtitle')}</h1>
        </div>
        <ServiceGuidesList tenantId={user.tenantId} userRole={userRole} />
      </div>
    </PortalShell>
  );
}
