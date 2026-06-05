'use client';

import { useParams, useRouter } from 'next/navigation';
import { useLocale } from 'next-intl';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { I } from '@/components/ui/icons';
import { useToast } from '@/lib/toast-context';
import { getTenant } from '@/services/admin';
import {
  listCampaigns, publishCampaign, pauseCampaign, resumeCampaign,
  archiveCampaign, duplicateCampaign, deleteCampaign,
  type Campaign,
} from '@/services/meta-ads';

const STATUS_LABEL: Record<string, string> = {
  DRAFT: 'Esborrany',
  PENDING_REVIEW: 'En revisió',
  ACTIVE: 'Activa',
  PAUSED: 'Pausada',
  REJECTED: 'Rebutjada',
  ARCHIVED: 'Arxivada',
  ERROR: 'Error',
};

const STATUS_TONE: Record<string, string> = {
  DRAFT: 'neutral',
  PENDING_REVIEW: 'warning',
  ACTIVE: 'success',
  PAUSED: 'neutral',
  REJECTED: 'danger',
  ARCHIVED: 'neutral',
  ERROR: 'danger',
};

const OBJECTIVE_LABEL: Record<string, string> = {
  OUTCOME_LEADS: 'Leads',
  OUTCOME_TRAFFIC: 'Tràfic',
  OUTCOME_AWARENESS: 'Notorietat',
  OUTCOME_ENGAGEMENT: 'Interacció',
};

function fmtBudget(n: number | null) {
  if (n == null) return '—';
  return new Intl.NumberFormat('ca-ES', { style: 'currency', currency: 'EUR' }).format(n);
}

function fmtDate(s: string | null) {
  if (!s) return '—';
  return new Date(s).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

function CampaignCard({ campaign, tenantId, locale, onAction }: {
  campaign: Campaign;
  tenantId: string;
  locale: string;
  onAction: (action: string, id: string) => void;
}) {
  const canPublish = ['DRAFT', 'REJECTED', 'ERROR'].includes(campaign.status);
  const canPause = campaign.status === 'ACTIVE';
  const canResume = campaign.status === 'PAUSED';
  const canArchive = campaign.status !== 'ARCHIVED';

  return (
    <div className="bg-surface-base border border-border-base rounded-lg p-4 space-y-3">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="font-medium text-ink-1 truncate">{campaign.name}</span>
            <AMGBadge tone={STATUS_TONE[campaign.status] as any}>
              {STATUS_LABEL[campaign.status] ?? campaign.status}
            </AMGBadge>
            <span className="text-xs text-ink-3 bg-surface-raised px-1.5 py-0.5 rounded">
              {OBJECTIVE_LABEL[campaign.objective] ?? campaign.objective}
            </span>
          </div>
          {campaign.metaError && (
            <p className="text-xs text-red-400 mt-1 truncate">{campaign.metaError}</p>
          )}
          <div className="flex gap-4 mt-1.5 text-xs text-ink-3">
            <span>Pressupost diari: {fmtBudget(campaign.dailyBudget)}</span>
            <span>Inici: {fmtDate(campaign.startTime)}</span>
            <span>{campaign.adSets.length} ad set{campaign.adSets.length !== 1 ? 's' : ''}</span>
          </div>
        </div>
        <div className="flex items-center gap-1 flex-shrink-0">
          {canPublish && (
            <AMGButton size="sm" onClick={() => onAction('publish', campaign.id)}>
              Publicar
            </AMGButton>
          )}
          {canPause && (
            <AMGButton size="sm" variant="ghost" onClick={() => onAction('pause', campaign.id)}>
              Pausar
            </AMGButton>
          )}
          {canResume && (
            <AMGButton size="sm" variant="ghost" onClick={() => onAction('resume', campaign.id)}>
              Reprendre
            </AMGButton>
          )}
        </div>
      </div>

      <div className="flex items-center gap-2 pt-1 border-t border-border-base text-xs">
        <a
          href={`/${locale}/portal/admin/tenants/${tenantId}/meta-ads/${campaign.id}`}
          className="text-accent-light hover:underline"
        >
          Veure detall
        </a>
        <span className="text-border-base">·</span>
        <button type="button" onClick={() => onAction('duplicate', campaign.id)} className="text-ink-3 hover:text-ink-1">
          Duplicar
        </button>
        {canArchive && (
          <>
            <span className="text-border-base">·</span>
            <button type="button" onClick={() => onAction('archive', campaign.id)} className="text-ink-3 hover:text-ink-1">
              Arxivar
            </button>
          </>
        )}
        {campaign.status === 'DRAFT' && (
          <>
            <span className="text-border-base">·</span>
            <button type="button" onClick={() => onAction('delete', campaign.id)} className="text-red-400 hover:text-red-300">
              Eliminar
            </button>
          </>
        )}
        <span className="ml-auto text-ink-3">{fmtDate(campaign.createdAt)}</span>
      </div>
    </div>
  );
}

export default function MetaAdsCampaignsPage() {
  const { id: tenantId } = useParams<{ id: string }>();
  const router = useRouter();
  const locale = useLocale();
  const qc = useQueryClient();
  const { toast } = useToast();

  const { data: tenant } = useQuery({
    queryKey: ['tenant', tenantId],
    queryFn: () => getTenant(tenantId),
  });

  const { data: campaigns, isLoading } = useQuery({
    queryKey: ['campaigns', tenantId],
    queryFn: () => listCampaigns(tenantId),
  });

  const publishMut = useMutation({
    mutationFn: (id: string) => publishCampaign(tenantId, id),
    onSuccess: (res) => {
      if (res.status === 'ERROR') toast('error', res.errorMessage ?? 'Error publicant');
      else toast('success', 'Campanya enviada a Meta per revisió');
      qc.invalidateQueries({ queryKey: ['campaigns', tenantId] });
    },
    onError: (e: any) => toast('error', e.message),
  });

  const pauseMut = useMutation({
    mutationFn: (id: string) => pauseCampaign(tenantId, id),
    onSuccess: () => { toast('success', 'Campanya pausada'); qc.invalidateQueries({ queryKey: ['campaigns', tenantId] }); },
    onError: (e: any) => toast('error', e.message),
  });

  const resumeMut = useMutation({
    mutationFn: (id: string) => resumeCampaign(tenantId, id),
    onSuccess: () => { toast('success', 'Campanya represa'); qc.invalidateQueries({ queryKey: ['campaigns', tenantId] }); },
    onError: (e: any) => toast('error', e.message),
  });

  const archiveMut = useMutation({
    mutationFn: (id: string) => archiveCampaign(tenantId, id),
    onSuccess: () => { toast('success', 'Campanya arxivada'); qc.invalidateQueries({ queryKey: ['campaigns', tenantId] }); },
    onError: (e: any) => toast('error', e.message),
  });

  const duplicateMut = useMutation({
    mutationFn: (id: string) => duplicateCampaign(tenantId, id),
    onSuccess: () => { toast('success', 'Campanya duplicada'); qc.invalidateQueries({ queryKey: ['campaigns', tenantId] }); },
    onError: (e: any) => toast('error', e.message),
  });

  const deleteMut = useMutation({
    mutationFn: (id: string) => deleteCampaign(tenantId, id),
    onSuccess: () => { toast('success', 'Campanya eliminada'); qc.invalidateQueries({ queryKey: ['campaigns', tenantId] }); },
    onError: (e: any) => toast('error', e.message),
  });

  function handleAction(action: string, id: string) {
    if (action === 'publish') publishMut.mutate(id);
    else if (action === 'pause') pauseMut.mutate(id);
    else if (action === 'resume') resumeMut.mutate(id);
    else if (action === 'archive') archiveMut.mutate(id);
    else if (action === 'duplicate') duplicateMut.mutate(id);
    else if (action === 'delete') {
      if (confirm('Eliminar aquesta campanya?')) deleteMut.mutate(id);
    }
  }

  const active = campaigns?.filter(c => c.status === 'ACTIVE') ?? [];
  const drafts = campaigns?.filter(c => c.status === 'DRAFT') ?? [];
  const others = campaigns?.filter(c => !['ACTIVE', 'DRAFT'].includes(c.status)) ?? [];

  return (
    <PortalShell
      breadcrumb={`admin · tenants · ${tenant?.name ?? tenantId} · meta ads`}
      backHref={`/portal/admin/tenants/${tenantId}`}
    >
      <div className="p-4 sm:p-8 max-w-3xl space-y-6">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-ink-1">Meta Ads</h1>
            <p className="text-sm text-ink-3 mt-0.5">Campanyes de Facebook i Instagram Ads</p>
          </div>
          <AMGButton onClick={() => router.push(`/${locale}/portal/admin/tenants/${tenantId}/meta-ads/new`)}>
            <I.Plus size={14} />Nova campanya
          </AMGButton>
        </div>

        {isLoading && (
          <div className="text-sm text-ink-3 py-8 text-center">Carregant campanyes...</div>
        )}

        {!isLoading && (!campaigns || campaigns.length === 0) && (
          <div className="text-center py-16 space-y-3">
            <div className="text-4xl opacity-30">📢</div>
            <p className="text-ink-2">No hi ha campanyes encara.</p>
            <AMGButton onClick={() => router.push(`/${locale}/portal/admin/tenants/${tenantId}/meta-ads/new`)}>
              Crear la primera campanya
            </AMGButton>
          </div>
        )}

        {active.length > 0 && (
          <section className="space-y-3">
            <h2 className="text-xs font-mono uppercase tracking-wider text-[#39d353]">Actives</h2>
            {active.map(c => (
              <CampaignCard key={c.id} campaign={c} tenantId={tenantId} locale={locale} onAction={handleAction} />
            ))}
          </section>
        )}

        {drafts.length > 0 && (
          <section className="space-y-3">
            <h2 className="text-xs font-mono uppercase tracking-wider text-ink-3">Esborranys</h2>
            {drafts.map(c => (
              <CampaignCard key={c.id} campaign={c} tenantId={tenantId} locale={locale} onAction={handleAction} />
            ))}
          </section>
        )}

        {others.length > 0 && (
          <section className="space-y-3">
            <h2 className="text-xs font-mono uppercase tracking-wider text-ink-3">Historial</h2>
            {others.map(c => (
              <CampaignCard key={c.id} campaign={c} tenantId={tenantId} locale={locale} onAction={handleAction} />
            ))}
          </section>
        )}
      </div>
    </PortalShell>
  );
}
