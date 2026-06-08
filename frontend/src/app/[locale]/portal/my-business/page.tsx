'use client';

import { useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { PortalShell } from '@/components/portal/PortalShell';
import { getOwnerTenant } from '@/services/admin';

export default function MyBusinessPage() {
  const { locale } = useParams<{ locale: string }>();
  const router = useRouter();

  const { data: owner, isError } = useQuery({
    queryKey: ['ownerTenant'],
    queryFn: getOwnerTenant,
    retry: false,
  });

  useEffect(() => {
    if (owner) {
      router.replace(`/${locale}/portal/admin/tenants/${owner.id}/setup`);
    }
  }, [owner, locale, router]);

  return (
    <PortalShell breadcrumb="El meu negoci">
      <div className="flex items-center justify-center h-full py-32">
        {isError ? (
          <p className="text-ink-2 text-sm">No s&apos;ha trobat el tenant propietari. Contacta amb el suport tècnic.</p>
        ) : (
          <p className="text-ink-3 text-sm f-mono">Carregant…</p>
        )}
      </div>
    </PortalShell>
  );
}
