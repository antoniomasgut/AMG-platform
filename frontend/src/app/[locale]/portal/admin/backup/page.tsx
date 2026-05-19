'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import { triggerBackup, listBackups, getBackupDashboard, type BackupTask } from '@/services/backup';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGStat } from '@/components/ui/stat';
import { I } from '@/components/ui/icons';

const STATUS_TONE: Record<string, 'neutral' | 'info' | 'success' | 'danger' | 'warning' | 'accent'> = {
  PENDING: 'neutral',
  RUNNING: 'info',
  COMPLETED: 'success',
  FAILED: 'danger',
};

const STATUS_LABEL: Record<string, string> = {
  PENDING: 'Pendent', RUNNING: 'En curs', COMPLETED: 'Completat', FAILED: 'Error',
};

const BACKUP_TYPES = ['FULL', 'INCREMENTAL', 'DATABASE'] as const;

function fmtDate(d?: string | null) {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('ca-ES', {
    day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  });
}

function fmtSize(bytes?: number | null) {
  if (!bytes) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  return `${(bytes / 1024 / 1024 / 1024).toFixed(2)} GB`;
}

export default function BackupPage() {
  const { user, isSuperAdmin } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();
  const [backupType, setBackupType] = useState<string>('FULL');

  const { data: dashboard } = useQuery({
    queryKey: ['backup-dashboard'],
    queryFn: getBackupDashboard,
    enabled: !!user && isSuperAdmin,
    refetchInterval: 30000,
  });

  const { data: backups = [], isLoading } = useQuery({
    queryKey: ['backups'],
    queryFn: listBackups,
    enabled: !!user && isSuperAdmin,
    refetchInterval: 15000,
  });

  const { mutate: doBackup, isPending: triggering } = useMutation({
    mutationFn: () => triggerBackup(backupType),
    onSuccess: () => {
      toast('success', `Backup ${backupType} iniciat correctament`);
      qc.invalidateQueries({ queryKey: ['backups'] });
      qc.invalidateQueries({ queryKey: ['backup-dashboard'] });
    },
    onError: () => toast('error', 'Error iniciant el backup'),
  });

  if (!user || !isSuperAdmin) return null;

  return (
    <PortalShell breadcrumb="admin / backup">
      <div className="p-4 sm:p-8 space-y-6">
        <div className="flex items-start justify-between">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / backup /</span>
            <div className="f-display font-bold text-xl mt-1">Backup Operacional</div>
          </div>
          <div className="flex items-center gap-2">
            <select
              value={backupType}
              onChange={(e) => setBackupType(e.target.value)}
              className="bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
            >
              {BACKUP_TYPES.map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </select>
            <AMGButton icon={I.Database} loading={triggering} onClick={() => doBackup()}>
              Backup Manual
            </AMGButton>
          </div>
        </div>

        {dashboard && (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
            <AMGStat
              label="Total backups"
              value={String(dashboard.totalBackups)}
              icon={I.Database}
              tone="accent"
            />
            <AMGStat
              label="Últim backup"
              value={dashboard.lastBackup ? new Date(dashboard.lastBackup).toLocaleDateString('ca-ES') : 'Mai'}
              icon={I.Clock}
              tone={dashboard.lastBackupStatus === 'COMPLETED' ? 'success' : 'danger'}
            />
            <AMGStat
              label="Proper programat"
              value={dashboard.nextScheduledBackup ? new Date(dashboard.nextScheduledBackup).toLocaleDateString('ca-ES') : '—'}
              icon={I.Calendar}
              tone="info"
            />
            <AMGStat
              label="Backups manuals"
              value={String(dashboard.manualFullCount ?? 0)}
              icon={I.Check}
              tone="success"
            />
          </div>
        )}

        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base">
            <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Historial de Backups</div>
          </div>

          {isLoading ? (
            <div className="flex justify-center py-12">
              <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
            </div>
          ) : (backups as BackupTask[]).length === 0 ? (
            <div className="p-8 text-center">
              <I.Database size={28} stroke="#64748b" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Cap backup</div>
              <p className="f-mono text-label text-ink-2">Inicia el primer backup manualment</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[560px]">
                <thead>
                  <tr className="border-b border-border-base">
                    {['Tipus', 'Estat', 'Mida', 'Iniciat', 'Completat'].map((h) => (
                      <th key={h} className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {(backups as BackupTask[]).map((b) => (
                    <tr key={b.id} className="border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,255,255,0.02)] transition-colors">
                      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-accent-light font-semibold uppercase">{b.type}</td>
                      <td className="px-4 sm:px-5 py-3">
                        <AMGBadge tone={STATUS_TONE[b.status] ?? 'neutral'}>
                          {STATUS_LABEL[b.status] ?? b.status}
                        </AMGBadge>
                      </td>
                      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{fmtSize(b.sizeBytes)}</td>
                      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{fmtDate(b.startedAt)}</td>
                      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{fmtDate(b.completedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </PortalShell>
  );
}
