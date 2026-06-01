'use client';

import { useState } from 'react';
import { createPortal } from 'react-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import {
  getCampaign, getCampaignProspects, exportProspect, enrichAllProspects, enrichProspect,
  exportContactableProspects, scoreProspects, qualifyByMinScore,
  updateProspect, exportQualifiedProspects,
  type Campaign, type Prospect,
} from '@/services/prospecting';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { I } from '@/components/ui/icons';
import { useRouter, useParams } from 'next/navigation';

const PROSPECT_STATUS_TONE: Record<string, 'neutral' | 'info' | 'success' | 'danger' | 'warning'> = {
  NEW: 'neutral',
  FOUND: 'neutral',
  QUALIFIED: 'success',
  EXPORTED: 'success',
  DISCARDED: 'danger',
  CONTACTED: 'info',
};

const PROSPECT_STATUS_LABEL: Record<string, string> = {
  NEW: 'Nou', FOUND: 'Trobat', QUALIFIED: 'Qualificat',
  EXPORTED: 'Exportat', DISCARDED: 'Descartat', CONTACTED: 'Contactat',
};

const CAMPAIGN_STATUS_TONE: Record<string, 'neutral' | 'info' | 'success' | 'danger' | 'warning' | 'accent'> = {
  DRAFT: 'neutral', IN_PROGRESS: 'info', COMPLETED: 'success', FAILED: 'danger',
};

const CAMPAIGN_STATUS_LABEL: Record<string, string> = {
  DRAFT: 'Pendent', IN_PROGRESS: 'En curs', COMPLETED: 'Completada', FAILED: 'Error',
};

function StarRating({ rating }: { rating: number }) {
  const full = Math.floor(rating);
  const half = rating - full >= 0.5;
  return (
    <span className="flex items-center gap-0.5">
      {Array.from({ length: 5 }, (_, i) => (
        <span key={i} className={`text-[10px] ${i < full ? 'text-yellow-400' : (i === full && half ? 'text-yellow-300' : 'text-ink-3')}`}>★</span>
      ))}
      <span className="f-mono text-[10px] text-ink-2 ml-1">{rating.toFixed(1)}</span>
    </span>
  );
}

function ProspectDrawer({
  prospect,
  onClose,
  onExport,
  onEnrich,
  onQualify,
  onDiscard,
  exporting,
  enriching,
  updatingStatus,
}: {
  prospect: Prospect;
  onClose: () => void;
  onExport: (id: string) => void;
  onEnrich: (id: string) => void;
  onQualify: (id: string) => void;
  onDiscard: (id: string) => void;
  exporting: boolean;
  enriching: boolean;
  updatingStatus: boolean;
}) {
  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/40 z-40"
        onClick={onClose}
      />
      {/* Drawer */}
      <div className="fixed top-0 right-0 h-full w-full sm:w-[420px] bg-bg-0 border-l border-border-base z-50 flex flex-col shadow-2xl">
        {/* Header */}
        <div className="flex items-start justify-between p-5 border-b border-border-base">
          <div className="flex-1 min-w-0 pr-3">
            <div className="f-display font-bold text-base leading-tight truncate">{prospect.name}</div>
            {prospect.sector && (
              <div className="f-mono text-[10px] text-ink-3 uppercase tracking-wider mt-0.5">{prospect.sector}</div>
            )}
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <AMGBadge tone={PROSPECT_STATUS_TONE[prospect.status] ?? 'neutral'}>
              {PROSPECT_STATUS_LABEL[prospect.status] ?? prospect.status}
            </AMGBadge>
            <button
              onClick={onClose}
              className="p-1.5 text-ink-3 hover:text-ink-0 hover:bg-bg-2 rounded transition-colors"
            >
              <I.X size={16} />
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-5 space-y-4">
          {/* Contact */}
          <section>
            <div className="f-mono text-[10px] uppercase tracking-widest text-ink-3 mb-2">Contacte</div>
            <div className="space-y-2">
              <DetailRow icon={<I.Phone size={13} />} label="Telèfon" value={prospect.phone} />
              <DetailRow icon={<I.Mail size={13} />} label="Email" value={prospect.email} />
              <DetailRow
                icon={<I.Globe size={13} />}
                label="Web"
                value={prospect.website
                  ? <a href={prospect.website} target="_blank" rel="noopener noreferrer"
                      className="text-accent-light hover:underline truncate block max-w-[250px]">
                      {prospect.website.replace(/^https?:\/\//, '')}
                    </a>
                  : null}
              />
            </div>
          </section>

          {/* Location */}
          <section>
            <div className="f-mono text-[10px] uppercase tracking-widest text-ink-3 mb-2">Ubicació</div>
            <div className="space-y-2">
              <DetailRow icon={<I.MapPin size={13} />} label="Adreça" value={prospect.address} />
              <DetailRow icon={<I.Building size={13} />} label="Municipi" value={
                [prospect.city, prospect.postalCode].filter(Boolean).join(' · ') || null
              } />
            </div>
          </section>

          {/* Google */}
          {(prospect.googleRating || prospect.googleReviews || prospect.description) && (
            <section>
              <div className="f-mono text-[10px] uppercase tracking-widest text-ink-3 mb-2">Google Places</div>
              <div className="space-y-2">
                {prospect.googleRating && (
                  <div className="flex items-center gap-2">
                    <span className="f-mono text-[10px] text-ink-3 w-16 shrink-0">Valoració</span>
                    <StarRating rating={prospect.googleRating} />
                    {prospect.googleReviews && (
                      <span className="f-mono text-[10px] text-ink-3">({prospect.googleReviews} ressenyes)</span>
                    )}
                  </div>
                )}
                {prospect.description && (
                  <div>
                    <span className="f-mono text-[10px] text-ink-3 block mb-1">Descripció</span>
                    <p className="f-mono text-xs text-ink-1 leading-relaxed">{prospect.description}</p>
                  </div>
                )}
              </div>
            </section>
          )}

          {/* Opinions */}
          {prospect.reviews && prospect.reviews.length > 0 && (
            <section>
              <div className="f-mono text-[10px] uppercase tracking-widest text-ink-3 mb-2">
                Opinions recents ({prospect.reviews.length})
              </div>
              <div className="space-y-2">
                {prospect.reviews.map((review, i) => (
                  <blockquote key={i} className="border-l-2 border-border-base pl-3 py-0.5">
                    <p className="f-mono text-[11px] text-ink-1 leading-relaxed italic">{review}</p>
                  </blockquote>
                ))}
              </div>
            </section>
          )}

          {/* Notes */}
          {prospect.notes && (
            <section>
              <div className="f-mono text-[10px] uppercase tracking-widest text-ink-3 mb-2">Notes</div>
              <p className="f-mono text-xs text-ink-1">{prospect.notes}</p>
            </section>
          )}
        </div>

        {/* Actions */}
        <div className="p-4 border-t border-border-base flex flex-col gap-2">
          {prospect.status === 'QUALIFIED' && (
            <AMGButton
              icon={I.ArrowRight}
              loading={exporting}
              onClick={() => onExport(prospect.id)}
              className="w-full justify-center"
            >
              Exportar a Lead
            </AMGButton>
          )}
          {prospect.status !== 'EXPORTED' && prospect.status !== 'QUALIFIED' && prospect.status !== 'DISCARDED' && (
            <div className="flex gap-2">
              <AMGButton
                icon={I.Check}
                loading={updatingStatus}
                onClick={() => onQualify(prospect.id)}
                className="flex-1 justify-center bg-success/10 border-success/30 text-success hover:bg-success/20"
              >
                Qualificar
              </AMGButton>
              <AMGButton
                variant="secondary"
                icon={I.X}
                loading={updatingStatus}
                onClick={() => onDiscard(prospect.id)}
                className="flex-1 justify-center text-danger hover:bg-danger/10"
              >
                Descartar
              </AMGButton>
            </div>
          )}
          {prospect.status === 'DISCARDED' && (
            <AMGButton
              variant="secondary"
              icon={I.Refresh}
              loading={updatingStatus}
              onClick={() => onQualify(prospect.id)}
              className="w-full justify-center"
            >
              Recuperar
            </AMGButton>
          )}
          <AMGButton
            variant="secondary"
            icon={I.Refresh}
            loading={enriching}
            onClick={() => onEnrich(prospect.id)}
            className="w-full justify-center"
          >
            Enriquir dades
          </AMGButton>
        </div>
      </div>
    </>
  );
}

function DetailRow({ icon, label, value }: { icon: React.ReactNode; label: string; value?: string | React.ReactNode | null }) {
  if (!value) return null;
  return (
    <div className="flex items-start gap-2">
      <span className="text-ink-3 mt-0.5 shrink-0">{icon}</span>
      <span className="f-mono text-[10px] text-ink-3 w-14 shrink-0 pt-0.5">{label}</span>
      <span className="f-mono text-xs text-ink-1 break-all">{value}</span>
    </div>
  );
}

export default function CampaignDetailPage() {
  const { user, isAdmin } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();
  const router = useRouter();
  const params = useParams();
  const locale = params.locale as string;
  const id = params.id as string;

  const [selectedProspect, setSelectedProspect] = useState<Prospect | null>(null);
  const [minScoreFilter, setMinScoreFilter] = useState<number>(5);
  const [showDiscarded, setShowDiscarded] = useState(false);

  const { data: campaign, isLoading: loadingCampaign } = useQuery({
    queryKey: ['campaign', id],
    queryFn: () => getCampaign(id),
    enabled: !!user && isAdmin && !!id,
  });

  const { data: prospects = [], isLoading: loadingProspects } = useQuery({
    queryKey: ['campaign-prospects', id],
    queryFn: () => getCampaignProspects(id),
    enabled: !!user && isAdmin && !!id,
  });

  const { mutate: doExport, isPending: exporting, variables: exportingId } = useMutation({
    mutationFn: (prospectId: string) => exportProspect(prospectId),
    onSuccess: (_, prospectId) => {
      toast('success', 'Prospect exportat a Leads');
      qc.invalidateQueries({ queryKey: ['campaign-prospects', id] });
      qc.invalidateQueries({ queryKey: ['campaign', id] });
      // Update selected prospect status
      setSelectedProspect(prev => prev?.id === prospectId ? { ...prev, status: 'EXPORTED' } : prev);
    },
    onError: () => toast('error', 'Error exportant el prospect'),
  });

  const { mutate: doEnrich, isPending: enriching, variables: enrichingId } = useMutation({
    mutationFn: (prospectId: string) => enrichProspect(prospectId),
    onSuccess: (updated) => {
      toast('success', 'Dades actualitzades');
      qc.invalidateQueries({ queryKey: ['campaign-prospects', id] });
      setSelectedProspect(updated as Prospect);
    },
    onError: () => toast('error', 'Error enriquint el prospect'),
  });

  const { mutate: doEnrichAll, isPending: enrichingAll } = useMutation({
    mutationFn: () => enrichAllProspects(id),
    onSuccess: (data) => {
      toast('success', `${data.enriched} prospect${data.enriched !== 1 ? 's' : ''} enriquit${data.enriched !== 1 ? 's' : ''}`);
      qc.invalidateQueries({ queryKey: ['campaign-prospects', id] });
    },
    onError: () => toast('error', 'Error enriquint els prospects'),
  });

  const { mutate: doExportContactable, isPending: exportingContactable } = useMutation({
    mutationFn: () => exportContactableProspects(id),
    onSuccess: (data) => {
      toast('success', `${data.exported} prospect${data.exported !== 1 ? 's' : ''} exportat${data.exported !== 1 ? 's' : ''} a Leads`);
      qc.invalidateQueries({ queryKey: ['campaign-prospects', id] });
      qc.invalidateQueries({ queryKey: ['campaign', id] });
    },
    onError: () => toast('error', 'Error exportant els prospects'),
  });

  const { mutate: doScore, isPending: scoring } = useMutation({
    mutationFn: () => scoreProspects(id),
    onSuccess: () => {
      toast('success', 'Prospects puntuats');
      qc.invalidateQueries({ queryKey: ['campaign-prospects', id] });
    },
    onError: () => toast('error', 'Error puntant els prospects'),
  });

  const { mutate: doQualify, isPending: qualifying } = useMutation({
    mutationFn: () => qualifyByMinScore(id, minScoreFilter),
    onSuccess: (data) => {
      toast('success', `${data.qualified} prospect${data.qualified !== 1 ? 's' : ''} qualificat${data.qualified !== 1 ? 's' : ''} (telèfon obtingut)`);
      qc.invalidateQueries({ queryKey: ['campaign-prospects', id] });
    },
    onError: () => toast('error', 'Error obtenint els detalls'),
  });

  const { mutate: doUpdateStatus, isPending: updatingStatus } = useMutation({
    mutationFn: ({ prospectId, status }: { prospectId: string; status: string }) =>
      updateProspect(prospectId, status),
    onSuccess: (updated) => {
      qc.invalidateQueries({ queryKey: ['campaign-prospects', id] });
      setSelectedProspect(updated as Prospect);
    },
    onError: () => toast('error', 'Error actualitzant el prospect'),
  });

  const { mutate: doExportQualified, isPending: exportingQualified } = useMutation({
    mutationFn: () => exportQualifiedProspects(id),
    onSuccess: (data) => {
      toast('success', `${data.exported} prospect${data.exported !== 1 ? 's' : ''} exportat${data.exported !== 1 ? 's' : ''} a Leads`);
      qc.invalidateQueries({ queryKey: ['campaign-prospects', id] });
      qc.invalidateQueries({ queryKey: ['campaign', id] });
    },
    onError: () => toast('error', 'Error exportant els prospects qualificats'),
  });

  if (!user || !isAdmin) return null;

  if (loadingCampaign) {
    return (
      <PortalShell breadcrumb="prospecting / detall" backHref={`/${locale}/portal/prospecting`}>
        <div className="flex justify-center py-20">
          <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
        </div>
      </PortalShell>
    );
  }

  if (!campaign) {
    return (
      <PortalShell breadcrumb="prospecting / detall" backHref={`/${locale}/portal/prospecting`}>
        <div className="p-8">
          <div className="p-3 border-l-2 border-l-danger bg-danger/5">
            <span className="f-mono text-label text-danger-light">Campanya no trobada</span>
          </div>
        </div>
      </PortalShell>
    );
  }

  const c = campaign as Campaign;
  const prospectList = prospects as Prospect[];

  return (
    <>
    <PortalShell breadcrumb={`prospecting / ${c.name}`} backHref={`/${locale}/portal/prospecting`}>
      <div className="p-4 sm:p-8 space-y-6 max-w-4xl">
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / prospecting /</span>
          <div className="flex items-center gap-3 mt-1">
            <div className="f-display font-bold text-xl">{c.name}</div>
            <AMGBadge tone={CAMPAIGN_STATUS_TONE[c.status]}>
              {CAMPAIGN_STATUS_LABEL[c.status] ?? c.status}
            </AMGBadge>
          </div>
        </div>

        {/* Campaign info */}
        <div className="amg-card card-clip p-5">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            {[
              { label: 'Sector', value: c.sector },
              { label: 'Localitat', value: c.location },
              { label: 'Font', value: c.source },
              { label: 'Prospects', value: String(prospectList.length) },
            ].map(({ label, value }) => (
              <div key={label}>
                <div className="f-mono text-[10px] text-ink-3 uppercase tracking-wider mb-0.5">{label}</div>
                <div className="f-display font-bold text-sm text-ink-0">{value}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Prospects table */}
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
            <div className="f-mono text-label uppercase text-ink-2 tracking-widest">
              Prospects
              <span className="ml-2 text-ink-3 normal-case">({prospectList.length})</span>
            </div>
            {prospectList.length > 0 && (
              <div className="flex items-center gap-2 flex-wrap justify-end">
                <AMGButton
                  size="sm"
                  variant="secondary"
                  icon={I.Sparkles}
                  loading={scoring}
                  onClick={() => doScore()}
                >
                  Puntuar
                </AMGButton>
                {prospectList.some(p => p.score != null) && (
                  <div className="flex items-center gap-1">
                    <select
                      value={minScoreFilter}
                      onChange={e => setMinScoreFilter(Number(e.target.value))}
                      className="f-mono text-[11px] bg-bg-1 border border-border-base rounded px-2 py-1 text-ink-1"
                    >
                      {[3, 4, 5, 6, 7, 8].map(v => (
                        <option key={v} value={v}>≥ {v} pts</option>
                      ))}
                    </select>
                    <AMGButton
                      size="sm"
                      variant="secondary"
                      icon={I.Phone}
                      loading={qualifying}
                      onClick={() => {
                        const count = prospectList.filter(p => (p.score ?? 0) >= minScoreFilter && !p.phone).length;
                        if (confirm(`Obtenir telèfon i web per ${count} prospect${count !== 1 ? 's' : ''} (puntuació ≥ ${minScoreFilter})?`)) {
                          doQualify();
                        }
                      }}
                    >
                      Obtenir detalls
                    </AMGButton>
                  </div>
                )}
                <AMGButton
                  size="sm"
                  variant="secondary"
                  icon={I.Refresh}
                  loading={enrichingAll}
                  onClick={() => doEnrichAll()}
                >
                  Enriquir tots
                </AMGButton>
                {prospectList.some(p => p.status === 'QUALIFIED') && (
                  <AMGButton
                    size="sm"
                    icon={I.ArrowRight}
                    loading={exportingQualified}
                    onClick={() => {
                      const count = prospectList.filter(p => p.status === 'QUALIFIED').length;
                      if (confirm(`Exportar ${count} prospect${count !== 1 ? 's' : ''} qualificat${count !== 1 ? 's' : ''} a Leads?`)) {
                        doExportQualified();
                      }
                    }}
                  >
                    Exportar qualificats
                  </AMGButton>
                )}
                {prospectList.some(p => p.status === 'DISCARDED') && (
                  <button
                    onClick={() => setShowDiscarded(v => !v)}
                    className={`f-mono text-[11px] px-2 py-1 rounded border transition-colors ${showDiscarded ? 'border-border-base text-ink-1 bg-bg-1' : 'border-transparent text-ink-3 hover:text-ink-2'}`}
                  >
                    {showDiscarded ? 'Ocultar descartats' : 'Veure descartats'}
                  </button>
                )}
              </div>
            )}
          </div>

          {loadingProspects ? (
            <div className="flex justify-center py-12">
              <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
            </div>
          ) : prospectList.length === 0 ? (
            <div className="p-8 text-center">
              <I.Search size={28} stroke="#64748b" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Cap prospect trobat</div>
              <p className="f-mono text-label text-ink-2">Executa la campanya per obtenir resultats</p>
            </div>
          ) : (
            <table className="w-full">
              <thead>
                <tr className="border-b border-border-base">
                  {['', 'Empresa', 'Telèfon', 'Punts', 'Estat', ''].map((h, i) => (
                    <th key={i} className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal first:px-4 first:w-6">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {[...prospectList]
                  .filter(p => showDiscarded || p.status !== 'DISCARDED')
                  .sort((a, b) => {
                    // QUALIFIED primer, DISCARDED al final
                    const order = (s: string) => s === 'QUALIFIED' ? 0 : s === 'DISCARDED' ? 2 : 1;
                    const od = order(a.status) - order(b.status);
                    if (od !== 0) return od;
                    return (b.score ?? -1) - (a.score ?? -1);
                  })
                  .map((p) => {
                    const hasContact = !!(p.phone || p.email);
                    const scored = p.score != null;
                    const scoreColor = scored
                      ? p.score! >= 8 ? 'text-success' : p.score! >= 5 ? 'text-accent-light' : 'text-ink-2'
                      : 'text-ink-3';
                    const rowClass = p.status === 'QUALIFIED'
                      ? 'border-b border-success/10 bg-success/[0.03] hover:bg-success/[0.06]'
                      : p.status === 'DISCARDED'
                      ? 'border-b border-[rgba(226,232,240,0.04)] opacity-40 hover:opacity-60'
                      : 'border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,255,255,0.02)]';
                    return (
                      <tr
                        key={p.id}
                        className={`transition-colors ${rowClass}`}
                      >
                        <td className="pl-4 pr-0 py-2.5 w-6">
                          <span
                            title={hasContact ? 'Té dades de contacte' : 'Sense telèfon ni email'}
                            className={`inline-block w-2 h-2 rounded-full ${hasContact ? 'bg-success' : 'bg-ink-3'}`}
                          />
                        </td>
                        <td className="px-4 sm:px-5 py-2.5">
                          <div className="f-display font-bold text-sm">{p.name}</div>
                          {p.city && <div className="f-mono text-[10px] text-ink-3 mt-0.5">{p.city}</div>}
                        </td>
                        <td className="px-4 sm:px-5 py-2.5 f-mono text-xs text-ink-1">
                          {p.phone ?? (p.email
                            ? <span className="text-ink-2">{p.email}</span>
                            : <span className="text-ink-3">—</span>
                          )}
                        </td>
                        <td className="px-4 sm:px-5 py-2.5">
                          <span className={`f-mono text-xs font-bold ${scoreColor}`}>
                            {scored ? p.score : '—'}
                          </span>
                        </td>
                        <td className="px-4 sm:px-5 py-2.5">
                          <AMGBadge tone={PROSPECT_STATUS_TONE[p.status] ?? 'neutral'}>
                            {PROSPECT_STATUS_LABEL[p.status] ?? p.status}
                          </AMGBadge>
                        </td>
                        <td className="px-4 sm:px-5 py-2.5 text-right">
                          <AMGButton
                            size="sm"
                            variant="ghost"
                            icon={I.Eye}
                            onClick={() => setSelectedProspect(p)}
                          >
                            Veure
                          </AMGButton>
                        </td>
                      </tr>
                    );
                  })}
              </tbody>
            </table>
          )}
        </div>
      </div>

    </PortalShell>
    {selectedProspect && typeof document !== 'undefined' && createPortal(
      <ProspectDrawer
        prospect={selectedProspect}
        onClose={() => setSelectedProspect(null)}
        onExport={(pid) => doExport(pid)}
        onEnrich={(pid) => doEnrich(pid)}
        onQualify={(pid) => doUpdateStatus({ prospectId: pid, status: 'QUALIFIED' })}
        onDiscard={(pid) => doUpdateStatus({ prospectId: pid, status: 'DISCARDED' })}
        exporting={exporting && exportingId === selectedProspect.id}
        enriching={enriching && enrichingId === selectedProspect.id}
        updatingStatus={updatingStatus}
      />,
      document.body
    )}
    </>
  );
}
