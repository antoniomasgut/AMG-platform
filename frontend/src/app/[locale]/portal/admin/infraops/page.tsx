'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import { getInfraStatus, getRecommendations, deleteRecommendation, getContainers, type InfraStatus, type Recommendation, type ContainerStatus } from '@/services/infraops';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGBadge } from '@/components/ui/badge';
import { AMGStat } from '@/components/ui/stat';
import { IconSet } from '@/components/ui/icons';

const SEVERITY_TONE: Record<string, 'neutral' | 'info' | 'success' | 'danger' | 'warning'> = {
  LOW: 'info',
  MEDIUM: 'warning',
  HIGH: 'danger',
  CRITICAL: 'danger',
};

function fmtDate(d: string | null) {
  if (!d) return '';
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
}

function containerHealthy(c: ContainerStatus) {
  return c.state?.toLowerCase() === 'running' && !c.status?.toLowerCase().includes('unhealthy');
}

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

  const { data: containers = [] } = useQuery({
    queryKey: ['infra-containers'],
    queryFn: getContainers,
    enabled: !!user && isSuperAdmin,
    refetchInterval: 30000,
  });

  const qc = useQueryClient();
  const { toast } = useToast();
  const delMut = useMutation({
    mutationFn: (id: string) => deleteRecommendation(id),
    onSuccess: () => { toast('success', 'Missatge esborrat'); qc.invalidateQueries({ queryKey: ['infra-recommendations'] }); },
    onError: () => toast('error', 'Error esborrant el missatge'),
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
                tone={s.overallStatus === 'OK' ? 'success' : s.overallStatus === 'DEGRADED' ? 'warning' : 'danger'}
              >
                {s.overallStatus}
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
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <AMGStat
                label="CPU"
                value={`${s.cpu.percent.toFixed(1)}%`}
                icon={IconSet.Activity}
                tone={metricTone(s.cpu.percent)}
              />
              <AMGStat
                label="RAM"
                value={`${s.ram.percent.toFixed(1)}%`}
                icon={IconSet.Server}
                tone={metricTone(s.ram.percent)}
              />
              <AMGStat
                label="Disc"
                value={`${s.disk.percent.toFixed(1)}%`}
                icon={IconSet.Database}
                tone={metricTone(s.disk.percent)}
              />
              <AMGStat
                label="BD Connexions"
                value={`${s.database.activeConnections ?? 0}/${s.database.maxConnections ?? 0}`}
                icon={IconSet.Box}
                tone={metricTone(s.database.percent)}
              />
            </div>

            <div className="amg-card card-clip p-6 space-y-4">
              <div className="f-mono text-label uppercase text-ink-2 tracking-widest mb-2">Ús de recursos</div>
              <MetricBar label="CPU" value={s.cpu.percent} />
              <MetricBar label={`RAM (${s.ram.usedMb ?? 0} / ${s.ram.totalMb ?? 0} MB)`} value={s.ram.percent} />
              <MetricBar label={`Disc (${s.disk.usedGb ?? 0} / ${s.disk.totalGb ?? 0} GB)`} value={s.disk.percent} />
              <MetricBar label="Base de dades" value={s.database.percent} />
            </div>
          </>
        ) : null}

        {/* Contenidors */}
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
            <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Contenidors</div>
            {(containers as ContainerStatus[]).length > 0 && (
              <AMGBadge tone={(containers as ContainerStatus[]).every(containerHealthy) ? 'success' : 'danger'}>
                {(containers as ContainerStatus[]).filter((c) => !containerHealthy(c)).length === 0
                  ? 'Tots OK'
                  : `${(containers as ContainerStatus[]).filter((c) => !containerHealthy(c)).length} amb problemes`}
              </AMGBadge>
            )}
          </div>
          {(containers as ContainerStatus[]).length === 0 ? (
            <div className="p-6 text-center f-mono text-label text-ink-3">
              Sense dades de l&apos;agent. Configura l&apos;agent del host i la clau INFRAOPS_AGENT_TOKEN.
            </div>
          ) : (
            <div className="divide-y divide-border-base">
              {(containers as ContainerStatus[]).map((c) => {
                const ok = containerHealthy(c);
                return (
                  <div key={c.name} className="px-4 sm:px-5 py-3 flex items-center gap-3">
                    <span className={`w-2 h-2 rounded-full shrink-0 ${ok ? 'bg-success' : 'bg-danger'}`} />
                    <span className="text-sm text-ink-1 font-semibold flex-1 min-w-0 truncate">{c.name}</span>
                    <span className="f-mono text-label text-ink-3 truncate max-w-[45%]">{c.status}</span>
                  </div>
                );
              })}
            </div>
          )}
        </div>

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
              <IconSet.Check size={28} stroke="#39d353" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1 text-success">Tot correcte</div>
              <p className="f-mono text-label text-ink-2">No hi ha recomanacions pendents</p>
            </div>
          ) : (
            <div className="divide-y divide-border-base">
              {recs.map((rec) => (
                <div key={rec.id} className={`px-4 sm:px-5 py-4 flex items-start gap-3 ${rec.resolved ? 'opacity-50' : ''}`}>
                  <AMGBadge tone={SEVERITY_TONE[rec.severity] ?? 'neutral'}>{rec.severity}</AMGBadge>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="f-mono text-label text-ink-2 uppercase tracking-wider">{rec.type}</span>
                      {rec.createdAt && <span className="f-mono text-label text-ink-3">· {fmtDate(rec.createdAt)}</span>}
                    </div>
                    <p className="text-sm text-ink-1">{rec.message}</p>
                  </div>
                  {rec.resolved && <AMGBadge tone="success">Resolt</AMGBadge>}
                  <button
                    onClick={() => delMut.mutate(rec.id)}
                    disabled={delMut.isPending}
                    title="Esborrar aquest missatge"
                    className="shrink-0 text-ink-3 hover:text-danger transition-colors disabled:opacity-50 mt-0.5"
                  >
                    <IconSet.Trash size={16} />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </PortalShell>
  );
}
