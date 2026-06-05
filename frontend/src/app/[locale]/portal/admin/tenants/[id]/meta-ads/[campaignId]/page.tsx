'use client';

import { useParams, useRouter } from 'next/navigation';
import { useLocale } from 'next-intl';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { I } from '@/components/ui/icons';
import { useToast } from '@/lib/toast-context';
import { GenerateImageModal } from '@/components/meta-ads/GenerateImageModal';
import {
  getCampaign, publishCampaign, pauseCampaign, resumeCampaign, archiveCampaign,
  createAdSet, deleteAdSet, createAd, deleteAd,
  type AdSetResponse, type AdResponse, type ImageUpload,
} from '@/services/meta-ads';

const inp = "w-full bg-surface-base border border-border-base rounded px-3 py-1.5 text-sm text-ink-1 focus:outline-none focus:border-accent-light";
const lbl = "block text-xs text-ink-2 mb-1";

const STATUS_LABEL: Record<string, string> = {
  DRAFT: 'Esborrany', PENDING_REVIEW: 'En revisió', ACTIVE: 'Activa',
  PAUSED: 'Pausada', REJECTED: 'Rebutjada', ARCHIVED: 'Arxivada', ERROR: 'Error',
};

const STATUS_TONE: Record<string, string> = {
  DRAFT: 'neutral', PENDING_REVIEW: 'warning', ACTIVE: 'success',
  PAUSED: 'neutral', REJECTED: 'danger', ARCHIVED: 'neutral', ERROR: 'danger',
};

const CTA_OPTIONS = ['LEARN_MORE', 'CONTACT_US', 'GET_QUOTE', 'SIGN_UP', 'SUBSCRIBE', 'BOOK_TRAVEL', 'BUY_NOW'];

function fmtDate(s: string | null) {
  if (!s) return '—';
  return new Date(s).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

function fmtBudget(n: number | null) {
  if (n == null) return '—';
  return new Intl.NumberFormat('ca-ES', { style: 'currency', currency: 'EUR' }).format(n);
}

function PublishProgressModal({ steps, status, onClose }: {
  steps: string[];
  status: string;
  onClose: () => void;
}) {
  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
      <div className="bg-surface-raised border border-border-base rounded-xl w-full max-w-md p-6 space-y-4">
        <h2 className="text-lg font-bold text-ink-1">Publicant campanya...</h2>
        <div className="space-y-1.5">
          {steps.map((s, i) => (
            <p key={i} className={`text-sm ${s.startsWith('✕') ? 'text-red-400' : s.startsWith('⏳') ? 'text-amber-400' : 'text-[#39d353]'}`}>
              {s}
            </p>
          ))}
        </div>
        {(status === 'PENDING_REVIEW' || status === 'ERROR') && (
          <AMGButton onClick={onClose} className="w-full">Tancar</AMGButton>
        )}
      </div>
    </div>
  );
}

interface NewAdSetForm {
  name: string; dailyBudget: string; ageMin: string; ageMax: string;
  genders: string; publisherPlatforms: string;
}

interface NewAdForm {
  name: string; headline: string; body: string; callToAction: string; linkUrl: string;
  metaImageHash: string | null; imagePreview: string | null;
}

function AdSetCard({ adSet, tenantId, onDeleted, onAdded }: {
  adSet: AdSetResponse;
  tenantId: string;
  onDeleted: () => void;
  onAdded: () => void;
}) {
  const [expanded, setExpanded] = useState(false);
  const [showNewAd, setShowNewAd] = useState(false);
  const [showGenModal, setShowGenModal] = useState(false);
  const [adForm, setAdForm] = useState<NewAdForm>({
    name: '', headline: '', body: '', callToAction: 'LEARN_MORE', linkUrl: '',
    metaImageHash: null, imagePreview: null,
  });
  const { toast } = useToast();

  const deleteSetMut = useMutation({
    mutationFn: () => deleteAdSet(tenantId, adSet.id),
    onSuccess: () => { toast('success', 'Ad set eliminat'); onDeleted(); },
    onError: (e: any) => toast('error', e.message),
  });

  const createAdMut = useMutation({
    mutationFn: () => createAd(tenantId, adSet.id, {
      name: adForm.name || adForm.headline,
      headline: adForm.headline,
      body: adForm.body,
      callToAction: adForm.callToAction,
      linkUrl: adForm.linkUrl,
      metaImageHash: adForm.metaImageHash || undefined,
    }),
    onSuccess: () => {
      toast('success', 'Anunci afegit');
      setShowNewAd(false);
      setAdForm({ name: '', headline: '', body: '', callToAction: 'LEARN_MORE', linkUrl: '', metaImageHash: null, imagePreview: null });
      onAdded();
    },
    onError: (e: any) => toast('error', e.message),
  });

  const deleteAdMut = useMutation({
    mutationFn: (adId: string) => deleteAd(tenantId, adId),
    onSuccess: () => { toast('success', 'Anunci eliminat'); onAdded(); },
    onError: (e: any) => toast('error', e.message),
  });

  return (
    <>
    {showGenModal && (
      <GenerateImageModal
        tenantId={tenantId}
        initialPrompt={adForm.headline}
        onAccept={(r: ImageUpload) => setAdForm(f => ({ ...f, metaImageHash: r.hash, imagePreview: r.url }))}
        onClose={() => setShowGenModal(false)}
      />
    )}
    <div className="border border-border-base rounded-lg overflow-hidden">
      <button
        type="button"
        className="w-full flex items-center justify-between p-3 bg-surface-base hover:bg-surface-raised text-left"
        onClick={() => setExpanded(e => !e)}
      >
        <div className="flex items-center gap-2">
          {expanded ? <I.ChevDown size={12} className="text-ink-3" /> : <I.Chevron size={12} className="text-ink-3" />}
          <span className="text-sm font-medium text-ink-1">{adSet.name}</span>
          <AMGBadge tone={STATUS_TONE[adSet.status] as any}>
            {STATUS_LABEL[adSet.status] ?? adSet.status}
          </AMGBadge>
          <span className="text-xs text-ink-3">{adSet.ads.length} ad{adSet.ads.length !== 1 ? 's' : ''}</span>
        </div>
        <div className="flex items-center gap-3 text-xs text-ink-3">
          <span>{fmtBudget(adSet.dailyBudget)}/dia</span>
          {adSet.metaAdSetId && (
            <span className="font-mono text-[10px]">#{adSet.metaAdSetId.slice(-6)}</span>
          )}
        </div>
      </button>

      {expanded && (
        <div className="border-t border-border-base bg-surface-raised p-3 space-y-3">
          {adSet.metaError && (
            <p className="text-xs text-red-400 bg-red-950/30 rounded px-2 py-1">{adSet.metaError}</p>
          )}

          <div className="grid grid-cols-2 gap-x-6 gap-y-1 text-xs">
            <div><span className="text-ink-3">Edat: </span><span className="text-ink-1">{adSet.ageMin}–{adSet.ageMax}</span></div>
            <div><span className="text-ink-3">Gènere: </span><span className="text-ink-1">{{ ALL: 'Tots', MALE: 'Homes', FEMALE: 'Dones' }[adSet.genders ?? 'ALL'] ?? adSet.genders}</span></div>
            <div><span className="text-ink-3">Plataformes: </span><span className="text-ink-1">{adSet.publisherPlatforms ?? '—'}</span></div>
          </div>

          {adSet.ads.length > 0 && (
            <div className="space-y-2">
              <h4 className="text-xs font-mono uppercase tracking-wider text-ink-3">Anuncis</h4>
              {adSet.ads.map((ad: AdResponse) => (
                <div key={ad.id} className="flex items-start justify-between gap-2 bg-surface-base rounded p-2">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-medium text-ink-1 truncate">{ad.headline ?? ad.name}</span>
                      <AMGBadge tone={STATUS_TONE[ad.status] as any}>
                        {STATUS_LABEL[ad.status] ?? ad.status}
                      </AMGBadge>
                    </div>
                    {ad.body && <p className="text-xs text-ink-3 mt-0.5 truncate">{ad.body}</p>}
                    {ad.linkUrl && <p className="text-xs text-ink-3 truncate">{ad.linkUrl}</p>}
                    {ad.metaError && <p className="text-xs text-red-400">{ad.metaError}</p>}
                  </div>
                  <button
                    type="button"
                    onClick={() => deleteAdMut.mutate(ad.id)}
                    className="text-xs text-red-400 hover:text-red-300 flex-shrink-0"
                  >
                    Eliminar
                  </button>
                </div>
              ))}
            </div>
          )}

          {showNewAd ? (
            <div className="space-y-3 bg-surface-base rounded p-3">
              <h4 className="text-xs font-mono uppercase tracking-wider text-ink-3">Nou anunci</h4>
              <div>
                <label className={lbl}>Títol *</label>
                <input className={inp} value={adForm.headline}
                  onChange={e => setAdForm(f => ({ ...f, headline: e.target.value }))}
                  placeholder="Ex: Reforma el teu bany" />
              </div>
              <div>
                <label className={lbl}>Text</label>
                <textarea className={inp + ' resize-none'} rows={2}
                  value={adForm.body}
                  onChange={e => setAdForm(f => ({ ...f, body: e.target.value }))} />
              </div>

              {/* Imatge */}
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className={lbl}>Imatge</label>
                  <button
                    type="button"
                    onClick={() => setShowGenModal(true)}
                    className="flex items-center gap-1 text-xs text-[#FF6B00] hover:text-[#FF9A3C] font-medium"
                  >
                    <I.Sparkles size={12} />
                    Genera amb IA
                  </button>
                </div>
                {adForm.imagePreview ? (
                  <div className="flex items-center gap-2">
                    <img src={adForm.imagePreview} alt="preview" className="w-20 h-12 object-cover rounded border border-border-base" />
                    <div>
                      <p className="text-xs text-[#39d353]">✓ Pujada a Meta</p>
                      <button type="button"
                        onClick={() => setAdForm(f => ({ ...f, metaImageHash: null, imagePreview: null }))}
                        className="text-xs text-red-400 hover:underline">Eliminar</button>
                    </div>
                  </div>
                ) : (
                  <p className="text-xs text-ink-3 italic">Sense imatge (opcional)</p>
                )}
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className={lbl}>URL *</label>
                  <input className={inp} type="url" value={adForm.linkUrl}
                    onChange={e => setAdForm(f => ({ ...f, linkUrl: e.target.value }))} />
                </div>
                <div>
                  <label className={lbl}>CTA</label>
                  <select className={inp} value={adForm.callToAction}
                    onChange={e => setAdForm(f => ({ ...f, callToAction: e.target.value }))}>
                    {CTA_OPTIONS.map(c => <option key={c} value={c}>{c.replace(/_/g, ' ')}</option>)}
                  </select>
                </div>
              </div>
              <div className="flex gap-2">
                <AMGButton size="sm"
                  disabled={!adForm.headline.trim() || !adForm.linkUrl.trim() || createAdMut.isPending}
                  onClick={() => createAdMut.mutate()}>
                  Afegir anunci
                </AMGButton>
                <AMGButton size="sm" variant="ghost" onClick={() => setShowNewAd(false)}>
                  Cancel·lar
                </AMGButton>
              </div>
            </div>
          ) : (
            <button type="button" onClick={() => setShowNewAd(true)}
              className="flex items-center gap-1 text-xs text-accent-light hover:underline">
              <I.Plus size={12} />Afegir anunci
            </button>
          )}

          {adSet.status === 'DRAFT' && (
            <div className="pt-1 border-t border-border-base">
              <button
                type="button"
                onClick={() => confirm('Eliminar aquest ad set?') && deleteSetMut.mutate()}
                className="text-xs text-red-400 hover:text-red-300"
              >
                Eliminar ad set
              </button>
            </div>
          )}
        </div>
      )}
    </div>
    </>
  );
}

export default function CampaignDetailPage() {
  const { id: tenantId, campaignId } = useParams<{ id: string; campaignId: string }>();
  const router = useRouter();
  const locale = useLocale();
  const qc = useQueryClient();
  const { toast } = useToast();

  const [publishResult, setPublishResult] = useState<{ steps: string[]; status: string } | null>(null);
  const [showNewAdSet, setShowNewAdSet] = useState(false);
  const [adSetForm, setAdSetForm] = useState<NewAdSetForm>({
    name: '', dailyBudget: '', ageMin: '25', ageMax: '55',
    genders: 'ALL', publisherPlatforms: 'facebook,instagram',
  });

  const { data: campaign, isLoading, refetch } = useQuery({
    queryKey: ['campaign', tenantId, campaignId],
    queryFn: () => getCampaign(tenantId, campaignId),
  });

  const publishMut = useMutation({
    mutationFn: () => publishCampaign(tenantId, campaignId),
    onSuccess: (res) => {
      setPublishResult({ steps: res.steps, status: res.status });
      qc.invalidateQueries({ queryKey: ['campaign', tenantId, campaignId] });
      qc.invalidateQueries({ queryKey: ['campaigns', tenantId] });
    },
    onError: (e: any) => toast('error', e.message),
  });

  const pauseMut = useMutation({
    mutationFn: () => pauseCampaign(tenantId, campaignId),
    onSuccess: () => { toast('success', 'Campanya pausada'); refetch(); },
    onError: (e: any) => toast('error', e.message),
  });

  const resumeMut = useMutation({
    mutationFn: () => resumeCampaign(tenantId, campaignId),
    onSuccess: () => { toast('success', 'Campanya represa'); refetch(); },
    onError: (e: any) => toast('error', e.message),
  });

  const archiveMut = useMutation({
    mutationFn: () => archiveCampaign(tenantId, campaignId),
    onSuccess: () => {
      toast('success', 'Campanya arxivada');
      router.push(`/${locale}/portal/admin/tenants/${tenantId}/meta-ads`);
    },
    onError: (e: any) => toast('error', e.message),
  });

  const createAdSetMut = useMutation({
    mutationFn: () => createAdSet(tenantId, campaignId, {
      name: adSetForm.name,
      dailyBudget: adSetForm.dailyBudget ? parseFloat(adSetForm.dailyBudget) : undefined,
      ageMin: parseInt(adSetForm.ageMin),
      ageMax: parseInt(adSetForm.ageMax),
      genders: adSetForm.genders,
      publisherPlatforms: adSetForm.publisherPlatforms,
    }),
    onSuccess: () => {
      toast('success', 'Ad set afegit');
      setShowNewAdSet(false);
      setAdSetForm({ name: '', dailyBudget: '', ageMin: '25', ageMax: '55', genders: 'ALL', publisherPlatforms: 'facebook,instagram' });
      refetch();
    },
    onError: (e: any) => toast('error', e.message),
  });

  if (isLoading) {
    return (
      <PortalShell breadcrumb="admin · meta ads · campanya">
        <div className="p-8 text-sm text-ink-3">Carregant...</div>
      </PortalShell>
    );
  }

  if (!campaign) {
    return (
      <PortalShell breadcrumb="admin · meta ads · campanya">
        <div className="p-8 text-sm text-red-400">Campanya no trobada.</div>
      </PortalShell>
    );
  }

  const canPublish = ['DRAFT', 'REJECTED', 'ERROR'].includes(campaign.status);
  const canPause = campaign.status === 'ACTIVE';
  const canResume = campaign.status === 'PAUSED';
  const activeAdSets = campaign.adSets.filter(s => s.status !== 'ARCHIVED');
  const totalAds = campaign.adSets.reduce((sum, s) => sum + s.ads.length, 0);

  return (
    <PortalShell
      breadcrumb={`admin · tenants · meta ads · ${campaign.name}`}
      backHref={`/portal/admin/tenants/${tenantId}/meta-ads`}
    >
      {publishResult && (
        <PublishProgressModal
          steps={publishResult.steps}
          status={publishResult.status}
          onClose={() => setPublishResult(null)}
        />
      )}

      <div className="p-4 sm:p-8 max-w-3xl space-y-6">
        {/* Header */}
        <div className="flex items-start justify-between gap-4 flex-wrap">
          <div>
            <div className="flex items-center gap-3 flex-wrap">
              <h1 className="text-2xl font-bold text-ink-1">{campaign.name}</h1>
              <AMGBadge tone={STATUS_TONE[campaign.status] as any}>
                {STATUS_LABEL[campaign.status] ?? campaign.status}
              </AMGBadge>
            </div>
            {campaign.metaCampaignId && (
              <p className="text-xs text-ink-3 font-mono mt-0.5">Meta ID: {campaign.metaCampaignId}</p>
            )}
            {campaign.metaError && (
              <p className="text-xs text-red-400 mt-1">{campaign.metaError}</p>
            )}
          </div>

          <div className="flex items-center gap-2 flex-wrap">
            {canPublish && (
              <AMGButton
                disabled={activeAdSets.length === 0 || publishMut.isPending}
                onClick={() => publishMut.mutate()}
              >
                {publishMut.isPending ? 'Publicant...' : 'Publicar a Meta'}
              </AMGButton>
            )}
            {canPause && (
              <AMGButton variant="ghost" disabled={pauseMut.isPending} onClick={() => pauseMut.mutate()}>
                Pausar
              </AMGButton>
            )}
            {canResume && (
              <AMGButton variant="ghost" disabled={resumeMut.isPending} onClick={() => resumeMut.mutate()}>
                Reprendre
              </AMGButton>
            )}
            {campaign.status !== 'ARCHIVED' && (
              <AMGButton
                variant="ghost"
                disabled={archiveMut.isPending}
                onClick={() => confirm('Arxivar aquesta campanya?') && archiveMut.mutate()}
                className="text-ink-3"
              >
                Arxivar
              </AMGButton>
            )}
          </div>
        </div>

        {/* Summary stats */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {[
            { label: 'Objectiu', value: campaign.objective.replace('OUTCOME_', '') },
            { label: 'Pressupost diari', value: fmtBudget(campaign.dailyBudget) },
            { label: 'Inici', value: fmtDate(campaign.startTime) },
            { label: 'Fi', value: fmtDate(campaign.stopTime) },
          ].map(item => (
            <div key={item.label} className="bg-surface-base border border-border-base rounded-lg p-3">
              <p className="text-xs text-ink-3">{item.label}</p>
              <p className="text-sm font-medium text-ink-1 mt-0.5">{item.value}</p>
            </div>
          ))}
        </div>

        <div className="flex items-center gap-4 text-xs text-ink-3">
          <span>{campaign.adSets.length} ad set{campaign.adSets.length !== 1 ? 's' : ''}</span>
          <span>·</span>
          <span>{totalAds} anunci{totalAds !== 1 ? 's' : ''}</span>
          <span>·</span>
          <span>Creat: {fmtDate(campaign.createdAt)}</span>
        </div>

        {/* Ad Sets */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="text-xs font-mono uppercase tracking-wider text-ink-3">Conjunts d'anuncis</h2>
            {campaign.status !== 'ARCHIVED' && (
              <button type="button" onClick={() => setShowNewAdSet(s => !s)}
                className="flex items-center gap-1 text-xs text-accent-light hover:underline">
                <I.Plus size={12} />Afegir ad set
              </button>
            )}
          </div>

          {showNewAdSet && (
            <div className="border border-border-base rounded-lg p-4 bg-surface-base space-y-3">
              <h3 className="text-xs font-mono uppercase tracking-wider text-ink-3">Nou conjunt d'anuncis</h3>
              <div className="grid grid-cols-2 gap-3">
                <div className="col-span-2">
                  <label className={lbl}>Nom *</label>
                  <input className={inp} value={adSetForm.name}
                    onChange={e => setAdSetForm(f => ({ ...f, name: e.target.value }))}
                    placeholder="Ex: Públic 25-45 Palma" />
                </div>
                <div>
                  <label className={lbl}>Pressupost diari (€)</label>
                  <input className={inp} type="number" min="1" step="0.01" value={adSetForm.dailyBudget}
                    onChange={e => setAdSetForm(f => ({ ...f, dailyBudget: e.target.value }))} />
                </div>
                <div>
                  <label className={lbl}>Gènere</label>
                  <select className={inp} value={adSetForm.genders}
                    onChange={e => setAdSetForm(f => ({ ...f, genders: e.target.value }))}>
                    <option value="ALL">Tots</option>
                    <option value="MALE">Homes</option>
                    <option value="FEMALE">Dones</option>
                  </select>
                </div>
                <div>
                  <label className={lbl}>Edat mínima</label>
                  <input className={inp} type="number" min="18" max="65" value={adSetForm.ageMin}
                    onChange={e => setAdSetForm(f => ({ ...f, ageMin: e.target.value }))} />
                </div>
                <div>
                  <label className={lbl}>Edat màxima</label>
                  <input className={inp} type="number" min="18" max="65" value={adSetForm.ageMax}
                    onChange={e => setAdSetForm(f => ({ ...f, ageMax: e.target.value }))} />
                </div>
              </div>
              <div className="flex gap-2">
                <AMGButton size="sm"
                  disabled={!adSetForm.name.trim() || createAdSetMut.isPending}
                  onClick={() => createAdSetMut.mutate()}>
                  Crear ad set
                </AMGButton>
                <AMGButton size="sm" variant="ghost" onClick={() => setShowNewAdSet(false)}>
                  Cancel·lar
                </AMGButton>
              </div>
            </div>
          )}

          {activeAdSets.length === 0 && !showNewAdSet && (
            <div className="text-center py-8 text-sm text-ink-3 border border-dashed border-border-base rounded-lg">
              Cap ad set. Afegeix-ne un per poder publicar la campanya.
            </div>
          )}

          {activeAdSets.map(adSet => (
            <AdSetCard
              key={adSet.id}
              adSet={adSet}
              tenantId={tenantId}
              onDeleted={() => refetch()}
              onAdded={() => refetch()}
            />
          ))}
        </div>
      </div>
    </PortalShell>
  );
}
