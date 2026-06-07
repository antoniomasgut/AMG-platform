'use client';

import { useTranslations } from 'next-intl';
import { useAuth } from '@/lib/auth-context';
import { useParams } from 'next/navigation';
import { PortalShell } from '@/components/portal/PortalShell';
import { ServiceGuide } from '@/components/guides/ServiceGuide';

export default function ServiceGuideTypePage() {
  const t = useTranslations();
  const { user, isSuperAdmin, isAdmin } = useAuth();
  const params = useParams();
  const serviceType = params.serviceType as string;
  const userRole = isSuperAdmin ? 'SUPER_ADMIN' : isAdmin ? 'ADMIN' : 'CLIENT';

  if (!user?.tenantId) return null;

  return (
    <PortalShell breadcrumb={`serveis / ${serviceType}`} backHref="/portal/serveis">
      <div className="p-4 sm:p-6">
        <ServiceGuide
          serviceType={serviceType}
          tenantId={user.tenantId}
          userRole={userRole}
        />
      </div>
    </PortalShell>
  );
}
