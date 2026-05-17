'use client';

import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { getInfraStatus, getRecommendations, type InfraStatus, type Recommendation } from '@/services/infraops';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGBadge } from '@/components/ui/badge';
import { AMGStat } from '@/components/ui/stat';
import { I } from '@/components/ui/icons';

const SEVERITY_TONE: Record<string, 'neutral' | 'info' | 'success' | 'danger' | 'warning'> = {
  LOW: 'info',
  MEDIUM: 'warning',
  HIGH: 'danger',
  CRITICAL: 'danger',
};

function metricTone(percent: number): 'success' | 'info' | 'danger' | 'accent' {
  if (percent >= 90) return 'danger';
  if (percent >= 75) return 'info';
  if (percent >= 50) return 'accent';
  return 'success';
}

function MetricBar({ label, value }: { label: string; value: number }) {
  const color =
    value >= 90 ? 'bg-danger' : value >= 75 ? 'bg-warning' : value >= 50 ? 'bg-accent' : 'bg-success';
  return (
    <div>
      <div className="flex justify-between mb-1">
        <span className="f-mono text-label text-ink-2 uppercase tracking-wider">{label}</span>
        <span className="f-mono text-label text-ink-0 font-semibold">{value.toFixed(1)}%</span>
      </div>
      <div className="h-2 bg-bg-1 rounded-full overflow-hidden">
        <div className={`h-full ${color} transition-all`} style={{ width: `${Math.min(value, 100)}%` }} />
      </div>
    </div>
  );
}

export default function InfraOpsPage() {
  const { user, isSuperAdmin } = useAuth();

  const { data: status, isLoading: loadingStatus, error: statusError } = useQuery({
    queryKey: ['infra-status'],
    queryFn: getInfraStatus,
    enabled: !!user && isSuperAdmin,
    refetchInterval: 30000,
  });

  const { data: recommendations = [], isLoading: loadingRecs } = useQuery({
    queryKey: ['infra-recommendations'],
    queryFn: getRecommendations,
    enabled: !!user && isSuperAdmin,
    refetchInterval: 60000,
  });

  if (!user || !isSuperAdmin) return null;

  const s = status as InfraStatus | undefined;
  const recs = recommendations as Recommendation[];
  const activeRecs = recs.filter((r) => !r.resolved);

  return (
    <PortalShell breadcrumb="admin / infraops">
      <div className="p-4 sm:p-8 space-y-6">
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / infraops /</span>
          <div className="flex items-center gap-3 mt-1">
            <div className="f-display font-bold text-xl">InfraOps</div>
            {s && (
              <AMGBadge
                tone={s.status === 'HEALTHY' ? 'success' : s.status === 'DEGRADED' ? 'warning' : 'danger'}
              >
                {s.status}
              </AMGBadge>
            )}
          </div>
        </div>

        {statusError && (
          <div className="p-3 border-l-2 border-l-danger bg-danger/5">
            <span className="f-mono text-label text-danger-light">Error carregant les mètriques del servidor</span>
          </div>
        )}

        {loadingStatus ? (
          <div className="flex justify-center py-12">
            <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          </div>
        ) : s ? (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <AMGStat
                label="CPU"
                value={`${s.cpuPercent.toFixed(1)}%`}
                icon={I.Activity}
                tone={metricTone(s.cpuPercent)}
              />
              <AMGStat
                label="RAM"
                value={`${s.ramPercent.toFixed(1)}%`}
                icon={I.Server}
                tone={metricTone(s.ramPercent)}
              />
              <AMGStat
                label="Disc"
                value={`${s.diskPercent.toFixed(1)}%`}
                icon={I.Database}
                tone={metricTone(s.diskPercent)}
              />
            </div>

            <div className="amg-card card-clip p-6 space-y-4">
              <div className="f-mono text-label uppercase text-ink-2 tracking-widest mb-2">Ús de recursos</div>
              <MetricBar label="CPU" value={s.cpuPercent} />
              <MetricBar label="Memòria RAM" value={s.ramPercent} />
              <MetricBar label="Disc" value={s.diskPercent} />
            </div>
          </>
        ) : null}

        {/* Recomanacions */}
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
            <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Recomanacions</div>
            {activeRecs.length > 0 && (
              <AMGBadge tone="warning">{activeRecs.length} actives</AMGBadge>
            )}
          </div>

          {loadingRecs ? (
            <div className="flex justify-center py-8">
              <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
            </div>
          ) : recs.length === 0 ? (
            <div className="p-8 text-center">
              <I.Check size={28} stroke="#39d353" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1 text-success">Tot correcte</div>
              <p className="f-mono text-label text-ink-2">No hi ha recomanacions pendents</p>
            </div>
          ) : (
            <div className="divide-y divide-border-base">
              {recs.map((rec) => (
                <div key={rec.id} className={`px-4 sm:px-5 py-4 flex items-start gap-3 ${rec.resolved ? 'opacity-50' : ''}`}>
                  <AMGBadge tone={SEVERITY_TONE[rec.severity] ?? 'neutral'}>{rec.severity}</AMGBadge>
                  <div className="flex-1 min-w-0">
                    <div className="f-mono text-label text-ink-2 uppercase tracking-wider mb-1">{rec.type}</div>
                    <p className="text-sm text-ink-1">{rec.message}</p>
                  </div>
                  {rec.resolved && (
                    <AMGBadge tone="success">Resolt</AMGBadge>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </PortalShell>
  );
}
