'use client';

import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGBadge } from '@/components/ui/badge';
import { AMGButton } from '@/components/ui/button';
import { listAllAdminSites, approveSite, rejectSite, downloadCompose, type WebSiteResponse, type WebsiteStatus } from '@/services/hosting';

const STATUS_LABELS: Record<WebsiteStatus, string> = {
  PENDING_REVIEW: 'Pendent revisió',
  APPROVED: 'Aprovada',
  DEPLOYING: 'Desplegant',
  ACTIVE: 'Activa',
  SUSPENDED: 'Suspesa',
  REJECTED: 'Rebutjada',
};

const STATUS_TONE: Record<WebsiteStatus, 'warning' | 'success' | 'info' | 'danger' | 'neutral'> = {
  PENDING_REVIEW: 'warning',
  APPROVED: 'success',
  DEPLOYING: 'info',
  ACTIVE: 'success',
  SUSPENDED: 'neutral',
  REJECTED: 'danger',
};

type FilterStatus = 'ALL' | WebsiteStatus;

function ReviewModal({
  site,
  onClose,
  onDone,
}: {
  site: WebSiteResponse;
  onClose: () => void;
  onDone: () => void;
}) {
  const [notes, setNotes] = useState('');
  const [upstreamContainer, setUpstreamContainer] = useState('');
  const [upstreamPort, setUpstreamPort] = useState('80');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const isContainer = site.type === 'CONTAINER';

  async function handleApprove() {
    if (isContainer && !upstreamContainer.trim()) {
      setError('Cal indicar el nom del contenidor upstream.');
      return;
    }
    setLoading(true);
    try {
      await approveSite(
        site.id,
        notes || undefined,
        isContainer ? upstreamContainer.trim() : undefined,
        isContainer ? (parseInt(upstreamPort) || 80) : undefined,
      );
      onDone();
    } catch {
      setError('Error en aprovar el lloc.');
    } finally {
      setLoading(false);
    }
  }

  async function handleReject() {
    if (!notes.trim()) { setError('Cal indicar un motiu de rebuig.'); return; }
    setLoading(true);
    try {
      await rejectSite(site.id, notes);
      onDone();
    } catch {
      setError('Error en rebutjar el lloc.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-lg p-6 space-y-4" onClick={e => e.stopPropagation()}>
        <div className="f-display font-bold text-lg">Revisar web</div>

        <div className="space-y-1 f-mono text-label text-xs text-ink-2">
          <div><span className="text-ink-0">Tipus:</span> {site.type}</div>
          <div><span className="text-ink-0">Domini:</span> {site.domain ?? '—'}</div>
          {site.clientNotes && (
            <div className="mt-2 p-3 bg-[#212140] text-ink-1 text-xs">
              <div className="text-ink-3 mb-1 uppercase tracking-wider">Notes del client</div>
              {site.clientNotes}
            </div>
          )}
        </div>

        {isContainer && (
          <>
            <AMGButton
              variant="secondary"
              size="sm"
              onClick={() => downloadCompose(site.id).catch(() => setError('Error descarregant el compose.'))}
            >
              ↓ Descarregar docker-compose.yml
            </AMGButton>

            <div className="p-3 bg-[#1a1a30] border border-border-base text-xs f-mono text-ink-2 space-y-1">
              <div className="text-ink-3 uppercase tracking-wider mb-2">Desplegament manual (SSH)</div>
              <div>1. Revisa el compose descarregat</div>
              <div>2. Copia els fitxers al servidor</div>
              <div className="text-accent-light">ssh root@65.108.148.62</div>
              <div className="text-accent-light">docker compose -f docker-compose.yml up -d</div>
              <div>3. Anota el nom del contenidor frontend i el port</div>
              <div>4. Omple els camps de baix i aprova</div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="f-mono text-label text-xs uppercase text-ink-2">
                  Contenidor upstream <span className="text-red-400">*</span>
                </label>
                <input
                  type="text"
                  className="w-full bg-[#0d0d1a] border border-border-base text-ink-0 text-sm p-2 focus:outline-none focus:border-accent"
                  placeholder="nom-del-contenidor"
                  value={upstreamContainer}
                  onChange={e => setUpstreamContainer(e.target.value)}
                />
              </div>
              <div className="space-y-1">
                <label className="f-mono text-label text-xs uppercase text-ink-2">Port</label>
                <input
                  type="number"
                  className="w-full bg-[#0d0d1a] border border-border-base text-ink-0 text-sm p-2 focus:outline-none focus:border-accent"
                  value={upstreamPort}
                  onChange={e => setUpstreamPort(e.target.value)}
                  min={1}
                  max={65535}
                />
              </div>
            </div>
          </>
        )}

        <div className="space-y-1">
          <label className="f-mono text-label text-xs uppercase text-ink-2">Notes internes (opcional per aprovar, obligatòries per rebutjar)</label>
          <textarea
            className="w-full bg-[#0d0d1a] border border-border-base text-ink-0 text-sm p-3 resize-none h-20 focus:outline-none focus:border-accent"
            placeholder="Indica el motiu de rebuig o notes d'aprovació..."
            value={notes}
            onChange={e => setNotes(e.target.value)}
          />
        </div>

        {error && <p className="f-mono text-xs text-danger">{error}</p>}

        <div className="flex gap-3 justify-end pt-2">
          <AMGButton variant="secondary" size="sm" onClick={onClose}>Cancel·lar</AMGButton>
          <AMGButton variant="danger" size="sm" disabled={loading} onClick={handleReject}>
            Rebutjar
          </AMGButton>
          <AMGButton variant="primary" size="sm" disabled={loading} onClick={handleApprove}>
            {loading ? '…' : 'Aprovar'}
          </AMGButton>
        </div>
      </div>
    </div>
  );
}

export default function AdminHostingPage() {
  const qc = useQueryClient();
  const [filter, setFilter] = useState<FilterStatus>('ALL');
  const [reviewing, setReviewing] = useState<WebSiteResponse | null>(null);

  const { data: sites = [], isLoading } = useQuery({
    queryKey: ['admin-sites-all'],
    queryFn: listAllAdminSites,
    staleTime: 30_000,
  });

  const filtered = filter === 'ALL' ? sites : sites.filter(s => s.status === filter);
  const pendingCount = sites.filter(s => s.status === 'PENDING_REVIEW').length;

  const FILTERS: { value: FilterStatus; label: string }[] = [
    { value: 'ALL', label: `Totes (${sites.length})` },
    { value: 'PENDING_REVIEW', label: `Pendents (${pendingCount})` },
    { value: 'ACTIVE', label: 'Actives' },
    { value: 'REJECTED', label: 'Rebutjades' },
  ];

  return (
    <PortalShell breadcrumb="hosting" backHref="/portal/admin/tenants">
      <div className="p-4 sm:p-6 space-y-4">

        <div className="flex items-center justify-between">
          <div>
            <div className="f-display font-bold text-lg">Web Hosting</div>
            <p className="f-mono text-label text-xs text-ink-2 mt-0.5">
              Gestiona les webs pujades pels clients.
            </p>
          </div>
          {pendingCount > 0 && (
            <AMGBadge tone="warning">{pendingCount} pendent{pendingCount > 1 ? 's' : ''}</AMGBadge>
          )}
        </div>

        {/* Filters */}
        <div className="flex gap-2 flex-wrap">
          {FILTERS.map(f => (
            <button
              key={f.value}
              onClick={() => setFilter(f.value)}
              className={`f-mono text-label text-xs uppercase px-3 h-7 border transition-colors ${
                filter === f.value
                  ? 'border-accent bg-accent-muted text-accent-light'
                  : 'border-border-base text-ink-2 hover:text-ink-0'
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>

        {isLoading ? (
          <div className="space-y-2">
            {[...Array(3)].map((_, i) => <div key={i} className="h-16 animate-pulse bg-[#212140] rounded" />)}
          </div>
        ) : filtered.length === 0 ? (
          <div className="amg-card card-clip p-8 text-center text-ink-3 f-mono text-sm">
            Cap web en aquesta categoria.
          </div>
        ) : (
          <div className="space-y-2">
            {filtered.map(site => (
              <div key={site.id} className="amg-card card-clip p-4 flex items-center gap-4">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <AMGBadge tone={STATUS_TONE[site.status]}>{STATUS_LABELS[site.status]}</AMGBadge>
                    <span className="f-mono text-label text-xs text-ink-3">{site.type}</span>
                  </div>
                  <div className="f-display font-semibold text-sm truncate">
                    {site.domain ?? <span className="text-ink-3">Sense domini</span>}
                  </div>
                  <div className="f-mono text-label text-xs text-ink-3 mt-0.5">
                    Tenant: {site.tenantId.toString().slice(0, 8)}…
                    {' · '}
                    {new Date(site.createdAt).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' })}
                    {site.storageBytes ? ` · ${(site.storageBytes / 1024).toFixed(0)} KB` : ''}
                  </div>
                  {site.reviewNotes && (
                    <div className="f-mono text-label text-xs text-ink-2 mt-1 italic">Notes: {site.reviewNotes}</div>
                  )}
                </div>
                {site.status === 'PENDING_REVIEW' && (
                  <AMGButton
                    variant="primary"
                    size="sm"
                    onClick={() => setReviewing(site)}
                  >
                    Revisar
                  </AMGButton>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {reviewing && (
        <ReviewModal
          site={reviewing}
          onClose={() => setReviewing(null)}
          onDone={() => {
            setReviewing(null);
            qc.invalidateQueries({ queryKey: ['admin-sites-all'] });
          }}
        />
      )}
    </PortalShell>
  );
}
