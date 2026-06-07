'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import {
  getOpsDashboard, getOpsHealth, getAllHealth, listIncidents,
  acknowledgeIncident, resolveIncident, startBackup, listBackups,
  type IncidentResponse, type ServiceHealthDto, type AllHealthResponse, type HealthResponse,
} from '@/services/ops';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGStat, AMGSectionTitle } from '@/components/ui/stat';
import { IconSet } from '@/components/ui/icons';

const SEVERITY_TONE: Record<string, 'danger' | 'warning' | 'info'> = {
  CRITICAL: 'danger', WARNING: 'warning', INFO: 'info',
};

const INCIDENT_STATUS_TONE: Record<string, 'danger' | 'warning' | 'success'> = {
  OPEN: 'danger', ACKNOWLEDGED: 'warning', RESOLVED: 'success',
};

const INCIDENT_LABEL: Record<string, string> = {
  OPEN: 'Obert', ACKNOWLEDGED: 'Reconegut', RESOLVED: 'Resolt',
};

const HEALTH_COLOR: Record<string, string> = {
  UP: 'text-success', DOWN: 'text-danger-light', DEGRADED: 'text-warning',
};

const HEALTH_DOT: Record<string, string> = {
  UP: 'bg-[#39d353]', DOWN: 'bg-[#ff4444]', DEGRADED: 'bg-[#f0b429]',
};

function ServiceCard({ svc }: { svc: ServiceHealthDto }) {
  return (
    <div className="amg-card card-clip p-4">
      <div className="flex items-center gap-2 mb-2">
        <span className={`w-2 h-2 rounded-full ${HEALTH_DOT[svc.status] ?? 'bg-[#8896aa]'} ${svc.status === 'UP' ? 'amg-blink' : ''}`} />
        <span className="f-display font-bold text-xs">{svc.serviceName}</span>
      </div>
      <div className={`f-mono text-label uppercase ${HEALTH_COLOR[svc.status] ?? 'text-ink-2'}`}>{svc.status}</div>
      {svc.responseTimeMs != null && (
        <div className="f-mono text-label text-ink-2 mt-1">{svc.responseTimeMs} ms</div>
      )}
      {svc.errorMessage && (
        <div className="f-mono text-label text-danger-light mt-1 truncate">{svc.errorMessage}</div>
      )}
    </div>
  );
}

function fmtDate(d: string | null) {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
}

export default function OpsPage() {
  const { isSuperAdmin } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();

  const { data: dashboard } = useQuery({ queryKey: ['ops-dashboard'], queryFn: getOpsDashboard });
  const { data: health } = useQuery<AllHealthResponse | HealthResponse>({
    queryKey: ['ops-health', isSuperAdmin],
    queryFn: () => (isSuperAdmin ? getAllHealth() : getOpsHealth()),
  });
  const { data: incidents = [], isLoading: loadingInc } = useQuery({
    queryKey: ['incidents'],
    queryFn: () => listIncidents(),
  });
  const { data: backups = [] } = useQuery({ queryKey: ['backups'], queryFn: () => listBackups() });

  const invalidateAll = () => {
    qc.invalidateQueries({ queryKey: ['incidents'] });
    qc.invalidateQueries({ queryKey: ['ops-dashboard'] });
  };

  const { mutate: doAck } = useMutation({
    mutationFn: acknowledgeIncident,
    onSuccess: () => { toast('success', 'Incident reconegut'); invalidateAll(); },
    onError: () => toast('error', 'Error'),
  });

  const { mutate: doResolve } = useMutation({
    mutationFn: resolveIncident,
    onSuccess: () => { toast('success', 'Incident resolt'); invalidateAll(); },
    onError: () => toast('error', 'Error'),
  });

  const { mutate: doBackup, isPending: backingUp } = useMutation({
    mutationFn: () => startBackup('MANUAL', 'Còpia manual des del portal'),
    onSuccess: () => { toast('success', 'Còpia de seguretat iniciada'); qc.invalidateQueries({ queryKey: ['backups'] }); },
    onError: () => toast('error', 'Error iniciant la còpia'),
  });

  const openIncidents = (incidents as IncidentResponse[]).filter((i) => i.status !== 'RESOLVED');

  return (
    <PortalShell breadcrumb="ops">
      <div className="p-4 sm:p-8 space-y-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / ops /</span>
            <div className="f-display font-bold text-xl mt-1">Ops & Health</div>
          </div>
          <AMGButton size="sm" icon={IconSet.Upload} disabled={backingUp} onClick={() => doBackup()}>
            {backingUp ? 'Fent còpia...' : 'Còpia ara'}
          </AMGButton>
        </div>

        {dashboard && (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
            <AMGStat
              label="Serveis UP"
              value={`${dashboard.currentStatus.up} / ${dashboard.currentStatus.services}`}
              icon={IconSet.Activity}
              tone={dashboard.currentStatus.down > 0 ? 'danger' : 'success'}
            />
            <AMGStat
              label="Incidents oberts"
              value={String(dashboard.openIncidents)}
              icon={IconSet.AlertCircle}
              tone={dashboard.openIncidents > 0 ? 'danger' : 'success'}
            />
            <AMGStat
              label="Uptime"
              value={`${dashboard.uptimePercentage.toFixed(1)}%`}
              icon={IconSet.Trending}
              tone={dashboard.uptimePercentage >= 99 ? 'success' : 'danger'}
            />
            <AMGStat
              label="Resposta mitj."
              value={dashboard.avgResponseTimeMs ? `${Math.round(dashboard.avgResponseTimeMs)} ms` : '—'}
              icon={IconSet.Clock}
              tone="info"
            />
          </div>
        )}

        {health && health.services.length > 0 && (
          <div>
            <AMGSectionTitle eyebrow="Monitor" title="Estat dels serveis" />
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
              {health.services.map((svc) => <ServiceCard key={svc.serviceName} svc={svc} />)}
              {'tenantSites' in health && (health as AllHealthResponse).tenantSites.map((svc: ServiceHealthDto) => (
                <ServiceCard key={`tenant-${svc.serviceName}`} svc={svc} />
              ))}
            </div>
          </div>
        )}

        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex items-center gap-3">
            <AMGSectionTitle eyebrow="Alertes" title="Incidents" />
            {openIncidents.length > 0 && (
              <AMGBadge tone="danger" className="ml-auto">{openIncidents.length} oberts</AMGBadge>
            )}
          </div>

          {loadingInc ? (
            <div className="flex justify-center py-12">
              <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
            </div>
          ) : incidents.length === 0 ? (
            <div className="p-8 text-center">
              <IconSet.Check size={28} stroke="#39d353" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Cap incident actiu</div>
              <p className="text-ui text-ink-2">Tots els serveis funcionen correctament</p>
            </div>
          ) : (
            <div className="divide-y divide-[rgba(226,232,240,0.04)]">
              {(incidents as IncidentResponse[]).map((inc) => (
                <div key={inc.id} className="p-4 sm:p-5 flex flex-wrap items-start gap-4">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1 flex-wrap">
                      <AMGBadge tone={SEVERITY_TONE[inc.severity]}>{inc.severity}</AMGBadge>
                      <AMGBadge tone={INCIDENT_STATUS_TONE[inc.status]}>{INCIDENT_LABEL[inc.status]}</AMGBadge>
                      <span className="f-mono text-label text-ink-2">{inc.serviceName}</span>
                    </div>
                    <div className="f-display font-bold text-sm">{inc.title}</div>
                    {inc.description && <p className="text-ui text-ink-2 mt-1">{inc.description}</p>}
                    <div className="f-mono text-label text-ink-3 mt-1">Iniciat: {fmtDate(inc.startedAt)}</div>
                  </div>
                  <div className="flex gap-2">
                    {inc.status === 'OPEN' && (
                      <AMGButton size="sm" variant="outline" onClick={() => doAck(inc.id)}>Reconèixer</AMGButton>
                    )}
                    {inc.status !== 'RESOLVED' && (
                      <AMGButton size="sm" icon={IconSet.Check} onClick={() => doResolve(inc.id)}>Resoldre</AMGButton>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {backups.length > 0 && (
          <div className="amg-card card-clip">
            <div className="p-4 sm:p-5 border-b border-border-base">
              <AMGSectionTitle eyebrow="Backups" title="Còpies de seguretat" />
            </div>
            <div className="divide-y divide-[rgba(226,232,240,0.04)]">
              {backups.slice(0, 5).map((b) => (
                <div key={b.id} className="px-4 sm:px-5 py-3 flex items-center gap-4">
                  <IconSet.Upload size={14} stroke="#8896aa" />
                  <div className="flex-1 min-w-0">
                    <div className="f-mono text-xs text-ink-1">{b.type} — {b.description ?? '—'}</div>
                    <div className="f-mono text-label text-ink-3">{fmtDate(b.startedAt)}</div>
                  </div>
                  <AMGBadge tone={b.status === 'COMPLETED' ? 'success' : b.status === 'FAILED' ? 'danger' : 'warning'}>
                    {b.status}
                  </AMGBadge>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </PortalShell>
  );
}
