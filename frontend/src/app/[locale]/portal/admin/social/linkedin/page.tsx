'use client';

import { useEffect, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { useToast } from '@/lib/toast-context';
import { listTenants } from '@/services/admin';
import { getLinkedInStatus, getLinkedInAuthUrl } from '@/services/social';

/**
 * Connexió LinkedIn — només per al tenant propietari AMG (Mòdul 56 F4).
 */
export default function LinkedInConnectPage() {
  const { toast } = useToast();
  const searchParams = useSearchParams();
  const [connecting, setConnecting] = useState(false);

  // Localitza el tenant propietari (isOwner)
  const { data: tenantsPage } = useQuery({
    queryKey: ['tenants-owner'],
    queryFn: () => listTenants({ size: 200 }),
  });
  const owner = tenantsPage?.content.find((t) => t.isOwner);

  const { data: status, refetch } = useQuery({
    queryKey: ['linkedin-status', owner?.id],
    queryFn: () => getLinkedInStatus(owner!.id),
    enabled: !!owner,
  });

  // Missatges de retorn del callback OAuth
  useEffect(() => {
    if (searchParams.get('success') === 'true') {
      toast('success', 'LinkedIn connectat', searchParams.get('name') ?? undefined);
      refetch();
    } else if (searchParams.get('error')) {
      toast('error', 'Error connectant LinkedIn', searchParams.get('error') ?? undefined);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  const onConnect = async () => {
    if (!owner) return;
    setConnecting(true);
    try {
      const { authUrl } = await getLinkedInAuthUrl(owner.id);
      window.location.href = authUrl;
    } catch (e) {
      toast('error', 'Error', (e as Error).message);
      setConnecting(false);
    }
  };

  return (
    <PortalShell breadcrumb="Social · LinkedIn">
      <div className="mx-auto max-w-2xl space-y-6 p-6">
        <div>
          <h1 className="text-2xl font-semibold">LinkedIn</h1>
          <p className="text-sm text-muted-foreground">
            Publicació a LinkedIn des del Social Publisher (només per al negoci propietari AMG).
          </p>
        </div>

        {!owner ? (
          <p className="text-sm text-muted-foreground">No s&apos;ha trobat el tenant propietari.</p>
        ) : (
          <div className="rounded-lg border p-6 space-y-4">
            <div className="flex items-center gap-3">
              <span
                className={`inline-block h-2.5 w-2.5 rounded-full ${
                  status?.connected ? 'bg-green-500' : 'bg-gray-300'
                }`}
              />
              <span className="text-sm font-medium">
                {status?.connected ? 'Connectat' : 'No connectat'}
              </span>
            </div>
            <p className="text-sm text-muted-foreground">
              {status?.connected
                ? 'Ja pots publicar a LinkedIn amb l\'opció «L» al flux /publica del Telegram.'
                : 'Connecta el teu perfil de LinkedIn per habilitar la publicació (scope w_member_social).'}
            </p>
            <AMGButton onClick={onConnect} disabled={connecting}>
              {status?.connected ? 'Tornar a connectar' : 'Connectar LinkedIn'}
            </AMGButton>
          </div>
        )}
      </div>
    </PortalShell>
  );
}
