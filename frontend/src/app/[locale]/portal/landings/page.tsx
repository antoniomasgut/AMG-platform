'use client';

import { useRouter } from '@/i18n/navigation';
import { useQuery } from '@tanstack/react-query';
import { getCurrentUser } from '@/services/auth';
import { listLandings, getLandingStats } from '@/services/factory';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { IconSet } from '@/components/ui/icons';
import { PortalShell } from '@/components/portal/PortalShell';

function LandingStats({ tenantId, landingId }: { tenantId: string; landingId: string }) {
  const { data } = useQuery({
    queryKey: ['landing-stats', landingId],
    queryFn: () => getLandingStats(tenantId, landingId),
    staleTime: 30_000,
  });
  if (!data) return null;
  return (
    <div className="flex gap-3 mt-2">
      <span className="f-mono text-[10px] text-ink-2 flex items-center gap-1">
        <IconSet.Eye size={11} stroke="currentColor" /> {data.viewCount} visites
      </span>
      <span className="f-mono text-[10px] text-ink-2 flex items-center gap-1">
        <IconSet.Mail size={11} stroke="currentColor" /> {data.contactCount} contactes
      </span>
    </div>
  );
}

export default function LandingsPage() {
  const router = useRouter();
  const user = getCurrentUser();

  const { data: landings = [], isLoading } = useQuery({
    queryKey: ['landings', user?.tenantId],
    queryFn: () => user!.tenantId ? listLandings(user!.tenantId!) : [],
    enabled: !!user?.tenantId,
  });

  if (!user) {
    return (
      <div className="w-full min-h-dvh bg-[#0d0d1a] flex items-center justify-center">
        <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin"></span>
      </div>
    );
  }

  return (
    <PortalShell breadcrumb="landings">
    <div className="p-4 sm:p-8 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / landings /</span>
          <div className="f-display font-bold text-xl mt-1">Les meves landings</div>
        </div>
        <AMGButton size="sm" icon={IconSet.Plus} onClick={() => router.push('/portal/landings/new')}>
          Nova landing
        </AMGButton>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12">
          <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin"></span>
        </div>
      ) : landings.length === 0 ? (
        <div className="amg-card card-clip p-8 text-center">
          <IconSet.Globe size={32} stroke="#64748b" className="mx-auto mb-3" />
          <div className="f-display font-bold text-base mb-1">Cap landing creada</div>
          <p className="text-ui text-ink-1 mb-4">Crea la teva primera landing per començar</p>
          <AMGButton size="sm" icon={IconSet.Plus} onClick={() => router.push('/portal/landings/new')}>
            Crear landing
          </AMGButton>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {landings.map((l) => (
            <div key={l.id} className="amg-card card-clip p-5">
              <div className="flex items-start justify-between mb-3">
                <div className="f-display font-bold text-sm truncate">{l.title}</div>
                <AMGBadge tone={l.status === 'PUBLISHED' ? 'success' : 'neutral'}>
                  {l.status === 'PUBLISHED' ? 'Publicada' : 'Esborrany'}
                </AMGBadge>
              </div>
              <div className="f-mono text-caption text-ink-3 mb-1">/{l.slug}</div>
              {l.status === 'PUBLISHED' && (
                <LandingStats tenantId={user.tenantId!} landingId={l.id} />
              )}
              <div className="flex gap-2 mt-4">
                <AMGButton
                  size="sm"
                  variant="secondary"
                  icon={IconSet.Edit}
                  onClick={() => router.push(`/portal/landings/${l.id}/edit`)}
                >
                  Editar
                </AMGButton>
                {l.status === 'PUBLISHED' && (
                  <AMGButton
                    size="sm"
                    variant="ghost"
                    icon={IconSet.Globe}
                    onClick={() => window.open(l.publicUrl, '_blank')}
                  >
                    Veure
                  </AMGButton>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
    </PortalShell>
  );
}
