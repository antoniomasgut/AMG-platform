'use client';

import { useQuery } from '@tanstack/react-query';
import { useParams, useRouter } from 'next/navigation';
import { getTenantSetup } from '@/services/vault';
import { getTenant } from '@/services/admin';
import { PortalShell } from '@/components/portal/PortalShell';
import { ServiceWizard } from '@/components/wizard/ServiceWizard';
import { AMGButton } from '@/components/ui/button';

export default function ServiceSetupPage() {
  const { id: tenantId, serviceId } = useParams<{ id: string; serviceId: string }>();
  const router = useRouter();

  const { data: tenant } = useQuery({
    queryKey: ['tenant', tenantId],
    queryFn: () => getTenant(tenantId),
  });

  const { data: setup, isLoading, error } = useQuery({
    queryKey: ['tenant-setup', tenantId],
    queryFn: () => getTenantSetup(tenantId),
    enabled: !!tenantId,
  });

  // Find the service info from setup
  const serviceInfo = (() => {
    if (!setup) return null;
    for (const profile of setup.profiles ?? []) {
      for (const phase of profile.phases ?? []) {
        for (const svc of phase.services ?? []) {
          if (svc.service.id === serviceId) {
            return {
              name: svc.service.name,
              slug: svc.service.slug,
              type: svc.service.type,
              status: svc.status,
            };
          }
        }
      }
    }
    // Also check profile.directServices if available (handled by the backend model)
    return null;
  })();

  const handleComplete = () => {
    router.push(`/portal/admin/tenants/${tenantId}`);
    router.refresh();
  };

  const handleCancel = () => {
    router.push(`/portal/admin/tenants/${tenantId}`);
  };

  if (isLoading) {
    return (
      <PortalShell breadcrumb={`admin · tenants · ${tenant?.name ?? '...'} · configuració`}>
        <div className="p-4 sm:p-8">
          <div className="max-w-2xl mx-auto flex justify-center py-12">
            <span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          </div>
        </div>
      </PortalShell>
    );
  }

  if (error || !serviceInfo) {
    return (
      <PortalShell breadcrumb={`admin · tenants · ${tenant?.name ?? 'error'} · configuració`}>
        <div className="p-4 sm:p-8">
          <div className="max-w-2xl mx-auto text-center py-12">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#ff6666" strokeWidth="1.6" className="mx-auto mb-3">
              <circle cx="12" cy="12" r="10" />
              <path d="m15 9-6 6M9 9l6 6" />
            </svg>
            <div className="f-display font-bold text-sm mb-1">Servei no trobat</div>
            <p className="f-mono text-xs text-ink-2 mb-4">
              No s&apos;ha trobat el servei al setup del tenant.
            </p>
            <AMGButton size="sm" onClick={handleCancel}>Tornar al tenant</AMGButton>
          </div>
        </div>
      </PortalShell>
    );
  }

  return (
    <PortalShell breadcrumb={`admin · tenants · ${tenant?.name ?? ''} · configurar ${serviceInfo.name}`}>
      <div className="p-4 sm:p-8">
        <ServiceWizard
          tenantId={tenantId}
          serviceId={serviceId}
          slug={serviceInfo.slug}
          serviceType={serviceInfo.type}
          serviceName={serviceInfo.name}
          onComplete={handleComplete}
          onCancel={handleCancel}
        />
      </div>
    </PortalShell>
  );
}
