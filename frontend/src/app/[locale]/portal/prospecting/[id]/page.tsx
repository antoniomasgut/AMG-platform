'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import {
  getCampaign, getCampaignProspects, exportProspect,
  type Campaign, type Prospect,
} from '@/services/prospecting';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { I } from '@/components/ui/icons';
import { useRouter, useParams } from 'next/navigation';

const PROSPECT_STATUS_TONE: Record<string, 'neutral' | 'info' | 'success' | 'danger' | 'warning'> = {
  FOUND: 'neutral',
  EXPORTED: 'success',
  REJECTED: 'danger',
};

const CAMPAIGN_STATUS_TONE: Record<string, 'neutral' | 'info' | 'success' | 'danger' | 'warning' | 'accent'> = {
  PENDING: 'neutral', RUNNING: 'info', COMPLETED: 'success', FAILED: 'danger',
};

const CAMPAIGN_STATUS_LABEL: Record<string, string> = {
  PENDING: 'Pendent', RUNNING: 'En curs', COMPLETED: 'Completada', FAILED: 'Error',
};

export default function CampaignDetailPage() {
  const { user, isAdmin } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();
  const router = useRouter();
  const params = useParams();
  const locale = params.locale as string;
  const id = params.id as string;

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
    onSuccess: () => {
      toast('success', 'Prospect exportat a Leads');
      qc.invalidateQueries({ queryKey: ['campaign-prospects', id] });
    },
    onError: () => toast('error', 'Error exportant el prospect'),
  });

  if (!user || !isAdmin) return null;

  if (loadingCampaign) {
    return (
      <PortalShell breadcrumb="prospecting / detall">
        <div className="flex justify-center py-20">
          <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
        </div>
      </PortalShell>
    );
  }

  if (!campaign) {
    return (
      <PortalShell breadcrumb="prospecting / detall">
        <div className="p-8">
          <div className="p-3 border-l-2 border-l-danger bg-danger/5">
            <span className="f-mono text-label text-danger-light">Campanya no trobada</span>
          </div>
        </div>
      </PortalShell>
    );
  }

  const c = campaign as Campaign;

  return (
    <PortalShell breadcrumb={`prospecting / ${c.name}`}>
      <div className="p-4 sm:p-8 space-y-6 max-w-5xl">
        <div>
          <button
            onClick={() => router.push(`/${locale}/portal/prospecting`)}
            className="f-mono text-label text-ink-2 hover:text-accent-light flex items-center gap-1 mb-3"
          >
            <I.ArrowRight size={12} className="rotate-180" /> Tornar
          </button>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / prospecting /</span>
          <div className="flex items-center gap-3 mt-1">
            <div className="f-display font-bold text-xl">{c.name}</div>
            <AMGBadge tone={CAMPAIGN_STATUS_TONE[c.status]}>
              {CAMPAIGN_STATUS_LABEL[c.status] ?? c.status}
            </AMGBadge>
          </div>
        </div>

        {/* Info campanya */}
        <div className="amg-card card-clip p-6">
          <div className="f-mono text-label uppercase text-ink-2 tracking-widest mb-4">Informació de la Campanya</div>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            {[
              { label: 'Sector', value: c.sector },
              { label: 'Localitat', value: c.location },
              { label: 'Font', value: c.source },
              { label: 'Prospects trobats', value: String(c.prospectsFound) },
            ].map(({ label, value }) => (
              <div key={label}>
                <div className="f-mono text-label text-ink-3 uppercase tracking-wider mb-0.5">{label}</div>
                <div className="f-display font-bold text-sm text-ink-0">{value}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Taula de prospects */}
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
            <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Prospects</div>
            <span className="f-mono text-label text-ink-3">{(prospects as Prospect[]).length} resultats</span>
          </div>

          {loadingProspects ? (
            <div className="flex justify-center py-12">
              <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
            </div>
          ) : (prospects as Prospect[]).length === 0 ? (
            <div className="p-8 text-center">
              <I.Search size={28} stroke="#64748b" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Cap prospect trobat</div>
              <p className="f-mono text-label text-ink-2">Executa la campanya per obtenir resultats</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[640px]">
                <thead>
                  <tr className="border-b border-border-base">
                    {['Empresa', 'Telèfon', 'Email', 'Adreça', 'Estat', 'Accions'].map((h) => (
                      <th key={h} className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {(prospects as Prospect[]).map((p) => (
                    <tr key={p.id} className="border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,255,255,0.02)] transition-colors">
                      <td className="px-4 sm:px-5 py-3 f-display font-bold text-sm">{p.businessName}</td>
                      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{p.phone ?? '—'}</td>
                      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{p.email ?? '—'}</td>
                      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-2 max-w-[180px] truncate">{p.address ?? '—'}</td>
                      <td className="px-4 sm:px-5 py-3">
                        <AMGBadge tone={PROSPECT_STATUS_TONE[p.status] ?? 'neutral'}>
                          {p.status}
                        </AMGBadge>
                      </td>
                      <td className="px-4 sm:px-5 py-3">
                        {p.status !== 'EXPORTED' && (
                          <AMGButton
                            size="sm"
                            icon={I.ArrowRight}
                            loading={exporting && exportingId === p.id}
                            onClick={() => doExport(p.id)}
                          >
                            Exportar a Lead
                          </AMGButton>
                        )}
                      </td>
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
