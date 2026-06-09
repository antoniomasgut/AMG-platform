'use client';

import { useState } from 'react';
import { createPortal } from 'react-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import {
  getCampaign, getCampaignProspects, exportProspect, enrichAllProspects, enrichProspect,
  exportContactableProspects, scoreProspects, qualifyByMinScore,
  updateProspect, exportQualifiedProspects, generateOutreach,
  scheduleCampaign, unscheduleCampaign,
  type Campaign, type Prospect, type UpdateProspectPayload,
} from '@/services/prospecting';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { IconSet } from '@/components/ui/icons';
import { useRouter, useParams } from 'next/navigation';

const PROSPECT_STATUS_TONE: Record<string, 'neutral' | 'info' | 'success' | 'danger' | 'warning'> = {
  NEW: 'neutral', FOUND: 'neutral', QUALIFIED: 'success',
  EXPORTED: 'success', DISCARDED: 'danger', CONTACTED: 'info',
};
const PROSPECT_STATUS_LABEL: Record<string, string> = {
  NEW: 'Nou', FOUND: 'Trobat', QUALIFIED: 'Qualificat',
  EXPORTED: 'Exportat', DISCARDED: 'Descartat', CONTACTED: 'Contactat',
};
const CAMPAIGN_STATUS_TONE: Record<string, 'neutral' | 'info' | 'success' | 'danger' | 'warning' | 'accent'> = {
  DRAFT: 'neutral', SCHEDULED: 'accent', IN_PROGRESS: 'info', COMPLETED: 'success', FAILED: 'danger',
};
const CAMPAIGN_STATUS_LABEL: Record<string, string> = {
  DRAFT: 'Pendent', SCHEDULED: 'Programada', IN_PROGRESS: 'En curs', COMPLETED: 'Completada', FAILED: 'Error',
};
const SOURCE_LABEL: Record<string, string> = {
  GOOGLE_MAPS: 'Google Maps', INSTAGRAM: 'Instagram',
  PAGINAS_AMARILLAS: 'Pàgines Grogues', MANUAL: 'Manual',
};

// ─── How it works panel ───────────────────────────────────────────────────────

function HowItWorks() {
  const [open, setOpen] = useState(false);
  const steps = [
    {
      n: 1, title: 'Puntuar',
      desc: 'Calcula una puntuació (0–16 pts) basada en senyals d\'oportunitat: web, WhatsApp, reviews, rating, Instagram. Gratuït i instantani. Fes-ho primer per saber en qui invertir les crides de pagament.',
      tag: 'Gratuït · Instant',
      tagTone: 'success' as const,
    },
    {
      n: 2, title: 'Obtenir detalls',
      desc: 'Consulta Google Places per als prospects amb puntuació ≥ X que encara no tenen telèfon. Obté telèfon, web, descripcions i opinions. Recalcula la puntuació (ara inclou el +5 per "sense web"). Costa crèdits de Google.',
      tag: 'Google Places · De pagament',
      tagTone: 'warning' as const,
    },
    {
      n: 3, title: 'Enriquir tots',
      desc: 'Cerca email i web corporativa via scraping per als prospects que els falten. No usa l\'API de Google. Gratuït però pot ser lent. Recalcula la puntuació si troba nova informació.',
      tag: 'Scraping · Gratuït',
      tagTone: 'success' as const,
    },
    {
      n: 4, title: 'Exportar qualificats',
      desc: 'Envia els prospects amb estat "Qualificat" al CRM de Leads. Des de Leads podràs enviar la demo per WhatsApp/email i gestionar el pipeline fins a tancar el client.',
      tag: '→ Leads CRM',
      tagTone: 'info' as const,
    },
  ];

  return (
    <div className="amg-card card-clip">
      <button
        onClick={() => setOpen(o => !o)}
        className="w-full flex items-center justify-between px-5 py-3 text-left hover:bg-bg-1 transition-colors"
      >
        <div className="flex items-center gap-2">
          <IconSet.AlertCircle size={14} className="text-ink-3" />
          <span className="f-mono text-[10px] uppercase tracking-widest text-ink-2">Com funciona</span>
        </div>
        <span className="text-ink-3 text-xs">{open ? '▲' : '▼'}</span>
      </button>
      {open && (
        <div className="px-5 pb-5 border-t border-border-base">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mt-4">
            {steps.map(s => (
              <div key={s.n} className="flex gap-3">
                <div className="w-6 h-6 shrink-0 flex items-center justify-center bg-accent-muted text-accent-light f-mono font-bold text-xs mt-0.5">
                  {s.n}
                </div>
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="f-display font-bold text-sm">{s.title}</span>
                    <AMGBadge tone={s.tagTone}>{s.tag}</AMGBadge>
                  </div>
                  <p className="f-mono text-xs text-ink-2 leading-relaxed">{s.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Prospect drawer ──────────────────────────────────────────────────────────

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

function EditField({ label, value, onChange }: { label: string; value: string; onChange: (v: string) => void }) {
  return (
    <div>
      <label className="f-mono text-[10px] text-ink-3 uppercase tracking-wider block mb-0.5">{label}</label>
      <input
        type="text"
        value={value}
        onChange={e => onChange(e.target.value)}
        className="w-full bg-bg-1 border border-border-base text-ink-0 px-2 h-8 f-mono text-xs focus:outline-none focus:border-accent"
      />
    </div>
  );
}

function ProspectDrawer({
  prospect, onClose, onExport, onEnrich, onQualify, onDiscard, onSave,
  exporting, enriching, updatingStatus, saving,
}: {
  prospect: Prospect;
  onClose: () => void;
  onExport: (id: string) => void;
  onEnrich: (id: string) => void;
  onQualify: (id: string) => void;
  onDiscard: (id: string) => void;
  onSave: (id: string, payload: UpdateProspectPayload) => void;
  exporting: boolean; enriching: boolean; updatingStatus: boolean; saving: boolean;
}) {
  const [editMode, setEditMode] = useState(false);
  const [draft, setDraft] = useState({
    name: prospect.name ?? '',
    phone: prospect.phone ?? '',
    email: prospect.email ?? '',
    website: prospect.website ?? '',
    address: prospect.address ?? '',
    city: prospect.city ?? '',
    notes: prospect.notes ?? '',
  });
  const [outreachChannel, setOutreachChannel] = useState<'email' | 'whatsapp'>('email');
  const [outreachText, setOutreachText] = useState('');
  const [copied, setCopied] = useState(false);

  const { mutate: doGenerateOutreach, isPending: generatingOutreach } = useMutation({
    mutationFn: (channel: 'email' | 'whatsapp') => generateOutreach(prospect.id, channel),
    onSuccess: (data) => setOutreachText(data.message),
  });

  const handleCopy = () => {
    navigator.clipboard.writeText(outreachText);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleSave = () => {
    onSave(prospect.id, draft);
    setEditMode(false);
  };

  return (
    <>
      <div className="fixed inset-0 bg-black/40 z-40" onClick={onClose} />
      <div className="fixed top-0 right-0 h-full w-full sm:w-[440px] bg-bg-0 border-l border-border-base z-50 flex flex-col shadow-2xl">
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
              onClick={() => setEditMode(e => !e)}
              title={editMode ? 'Veure dades' : 'Editar dades'}
              className={`p-1.5 rounded transition-colors ${editMode ? 'text-accent-light bg-accent-muted' : 'text-ink-3 hover:text-ink-0 hover:bg-bg-2'}`}
            >
              <IconSet.Edit size={15} />
            </button>
            <button onClick={onClose} className="p-1.5 text-ink-3 hover:text-ink-0 hover:bg-bg-2 rounded transition-colors">
              <IconSet.X size={16} />
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-5 space-y-4">
          {editMode ? (
            /* ── Mode edició ── */
            <div className="space-y-3">
              <div className="f-mono text-[10px] uppercase tracking-widest text-ink-3 mb-1">Editar dades</div>
              <EditField label="Nom" value={draft.name} onChange={v => setDraft(d => ({ ...d, name: v }))} />
              <EditField label="Telèfon" value={draft.phone} onChange={v => setDraft(d => ({ ...d, phone: v }))} />
              <EditField label="Email" value={draft.email} onChange={v => setDraft(d => ({ ...d, email: v }))} />
              <EditField label="Web" value={draft.website} onChange={v => setDraft(d => ({ ...d, website: v }))} />
              <EditField label="Adreça" value={draft.address} onChange={v => setDraft(d => ({ ...d, address: v }))} />
              <EditField label="Municipi" value={draft.city} onChange={v => setDraft(d => ({ ...d, city: v }))} />
              <div>
                <label className="f-mono text-[10px] text-ink-3 uppercase tracking-wider block mb-0.5">Notes</label>
                <textarea
                  value={draft.notes}
                  onChange={e => setDraft(d => ({ ...d, notes: e.target.value }))}
                  rows={3}
                  className="w-full bg-bg-1 border border-border-base text-ink-0 px-2 py-1.5 f-mono text-xs focus:outline-none focus:border-accent resize-none"
                />
              </div>
              <p className="f-mono text-[10px] text-ink-3">La puntuació es recalcularà automàticament en desar.</p>
            </div>
          ) : (
            /* ── Mode visualització ── */
            <>
              <section>
                <div className="f-mono text-[10px] uppercase tracking-widest text-ink-3 mb-2">Contacte</div>
                <div className="space-y-2">
                  <DetailRow icon={<IconSet.Phone size={13} />} label="Telèfon" value={prospect.phone} />
                  <DetailRow icon={<IconSet.Mail size={13} />}  label="Email"   value={prospect.email} />
                  <DetailRow
                    icon={<IconSet.Globe size={13} />}
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

              <section>
                <div className="f-mono text-[10px] uppercase tracking-widest text-ink-3 mb-2">Ubicació</div>
                <div className="space-y-2">
                  <DetailRow icon={<IconSet.MapPin size={13} />}  label="Adreça"   value={prospect.address} />
                  <DetailRow icon={<IconSet.Building size={13} />} label="Municipi" value={
                    [prospect.city, prospect.postalCode].filter(Boolean).join(' · ') || null
                  } />
                </div>
              </section>

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

              {prospect.notes && (
                <section>
                  <div className="f-mono text-[10px] uppercase tracking-widest text-ink-3 mb-2">Notes</div>
                  <p className="f-mono text-xs text-ink-1">{prospect.notes}</p>
                </section>
              )}

              {prospect.signals && prospect.signals.length > 0 && (
                <section>
                  <div className="f-mono text-[10px] uppercase tracking-widest text-ink-3 mb-2">Oportunitats detectades</div>
                  <div className="space-y-1.5">
                    {prospect.signals.map(sig => (
                      <div key={sig.code} className={`p-2 border flex items-start gap-2 ${
                        sig.tone === 'danger'      ? 'border-danger/30 bg-danger/5' :
                        sig.tone === 'warning'     ? 'border-warning/30 bg-warning/5' :
                        sig.tone === 'opportunity' ? 'border-success/30 bg-success/5' :
                        sig.tone === 'info'        ? 'border-accent/30 bg-accent/5' :
                                                     'border-border-base bg-bg-1'
                      }`}>
                        <div className="flex-1 min-w-0">
                          <div className={`f-mono text-[10px] font-bold ${
                            sig.tone === 'danger'      ? 'text-danger' :
                            sig.tone === 'warning'     ? 'text-warning' :
                            sig.tone === 'opportunity' ? 'text-success' :
                            sig.tone === 'info'        ? 'text-accent-light' :
                                                         'text-ink-2'
                          }`}>{sig.label}</div>
                          <div className="f-mono text-[10px] text-ink-3 mt-0.5">→ {sig.pitch}</div>
                        </div>
                        <span className="f-mono text-[10px] font-bold text-ink-3 shrink-0">+{sig.points}</span>
                      </div>
                    ))}
                  </div>
                </section>
              )}

              {prospect.score != null && (
                <section>
                  <div className="f-mono text-[10px] uppercase tracking-widest text-ink-3 mb-2">Puntuació</div>
                  <div className="flex items-center gap-2">
                    <span className={`f-mono text-2xl font-bold ${prospect.score >= 8 ? 'text-success' : prospect.score >= 5 ? 'text-accent-light' : 'text-ink-2'}`}>
                      {prospect.score}
                    </span>
                    <span className="f-mono text-xs text-ink-3">/ 16 pts</span>
                  </div>
                </section>
              )}

              <section>
                <div className="f-mono text-[10px] uppercase tracking-widest text-ink-3 mb-2">Generar missatge</div>
                <div className="flex gap-1.5 mb-2">
                  {(['email', 'whatsapp'] as const).map(ch => (
                    <button
                      key={ch}
                      onClick={() => { setOutreachChannel(ch); setOutreachText(''); }}
                      className={`f-mono text-[10px] uppercase tracking-wider px-2.5 py-1 border transition-colors ${
                        outreachChannel === ch
                          ? 'border-accent bg-accent/10 text-accent-light'
                          : 'border-border-base text-ink-3 hover:text-ink-0'
                      }`}
                    >
                      {ch === 'email' ? 'Email' : 'WhatsApp'}
                    </button>
                  ))}
                  <AMGButton
                    size="sm"
                    loading={generatingOutreach}
                    onClick={() => doGenerateOutreach(outreachChannel)}
                    icon={IconSet.Sparkles}
                  >
                    Generar
                  </AMGButton>
                </div>
                {outreachText && (
                  <div className="relative">
                    <textarea
                      readOnly
                      value={outreachText}
                      rows={10}
                      className="w-full bg-bg-1 border border-border-base text-ink-1 px-3 py-2 f-mono text-[11px] leading-relaxed resize-none focus:outline-none"
                    />
                    <button
                      onClick={handleCopy}
                      className="absolute top-2 right-2 f-mono text-[9px] uppercase tracking-wider px-2 py-1 bg-bg-2 border border-border-base text-ink-2 hover:text-ink-0 transition-colors"
                    >
                      {copied ? 'Copiat!' : 'Copiar'}
                    </button>
                  </div>
                )}
              </section>
            </>
          )}
        </div>

        {/* Actions */}
        <div className="p-4 border-t border-border-base flex flex-col gap-2">
          {editMode ? (
            <div className="flex gap-2">
              <AMGButton loading={saving} onClick={handleSave} className="flex-1 justify-center">
                Desar canvis
              </AMGButton>
              <AMGButton variant="outline" onClick={() => setEditMode(false)}>Cancel·lar</AMGButton>
            </div>
          ) : (
            <>
              {prospect.status === 'QUALIFIED' && (
                <AMGButton icon={IconSet.ArrowRight} loading={exporting} onClick={() => onExport(prospect.id)} className="w-full justify-center">
                  Exportar a Lead
                </AMGButton>
              )}
              {prospect.status !== 'EXPORTED' && prospect.status !== 'QUALIFIED' && prospect.status !== 'DISCARDED' && (
                <div className="flex gap-2">
                  <AMGButton icon={IconSet.Check} loading={updatingStatus} onClick={() => onQualify(prospect.id)}
                    className="flex-1 justify-center bg-success/10 border-success/30 text-success hover:bg-success/20">
                    Qualificar
                  </AMGButton>
                  <AMGButton variant="secondary" icon={IconSet.X} loading={updatingStatus} onClick={() => onDiscard(prospect.id)}
                    className="flex-1 justify-center text-danger hover:bg-danger/10">
                    Descartar
                  </AMGButton>
                </div>
              )}
              {prospect.status === 'DISCARDED' && (
                <AMGButton variant="secondary" icon={IconSet.Refresh} loading={updatingStatus} onClick={() => onQualify(prospect.id)} className="w-full justify-center">
                  Recuperar
                </AMGButton>
              )}
              <AMGButton variant="secondary" icon={IconSet.Refresh} loading={enriching} onClick={() => onEnrich(prospect.id)} className="w-full justify-center">
                Enriquir dades
              </AMGButton>
            </>
          )}
        </div>
      </div>
    </>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

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

  const invalidateProspects = () => qc.invalidateQueries({ queryKey: ['campaign-prospects', id] });
  const invalidateCampaign  = () => qc.invalidateQueries({ queryKey: ['campaign', id] });

  const { mutate: doExport, isPending: exporting, variables: exportingId } = useMutation({
    mutationFn: (prospectId: string) => exportProspect(prospectId),
    onSuccess: (_, pid) => {
      toast('success', 'Prospect exportat a Leads');
      invalidateProspects(); invalidateCampaign();
      setSelectedProspect(prev => prev?.id === pid ? { ...prev, status: 'EXPORTED' } : prev);
    },
    onError: () => toast('error', 'Error exportant el prospect'),
  });

  const { mutate: doEnrich, isPending: enriching, variables: enrichingId } = useMutation({
    mutationFn: (prospectId: string) => enrichProspect(prospectId),
    onSuccess: (updated) => {
      toast('success', 'Dades actualitzades');
      invalidateProspects();
      setSelectedProspect(updated as Prospect);
    },
    onError: () => toast('error', 'Error enriquint el prospect'),
  });

  const { mutate: doEnrichAll, isPending: enrichingAll } = useMutation({
    mutationFn: () => enrichAllProspects(id),
    onSuccess: (data) => {
      toast('success', `${data.enriched} prospect${data.enriched !== 1 ? 's' : ''} enriquit${data.enriched !== 1 ? 's' : ''}`);
      invalidateProspects();
    },
    onError: () => toast('error', 'Error enriquint els prospects'),
  });

  const { mutate: doExportContactable, isPending: exportingContactable } = useMutation({
    mutationFn: () => exportContactableProspects(id),
    onSuccess: (data) => {
      const skippedMsg = data.skipped ? `, ${data.skipped} ja existien` : '';
      toast('success', `${data.exported} nous leads creats${skippedMsg}`);
      invalidateProspects(); invalidateCampaign();
    },
    onError: () => toast('error', 'Error exportant els prospects'),
  });

  const { mutate: doScore, isPending: scoring } = useMutation({
    mutationFn: () => scoreProspects(id),
    onSuccess: () => { toast('success', 'Prospects puntuats'); invalidateProspects(); },
    onError: () => toast('error', 'Error puntant els prospects'),
  });

  const { mutate: doQualify, isPending: qualifying } = useMutation({
    mutationFn: () => qualifyByMinScore(id, minScoreFilter),
    onSuccess: (data) => {
      toast('success', `${data.qualified} prospect${data.qualified !== 1 ? 's' : ''} qualificat${data.qualified !== 1 ? 's' : ''}`);
      invalidateProspects();
    },
    onError: () => toast('error', 'Error obtenint els detalls'),
  });

  const { mutate: doUpdateStatus, isPending: updatingStatus } = useMutation({
    mutationFn: ({ prospectId, status }: { prospectId: string; status: string }) =>
      updateProspect(prospectId, { status }),
    onSuccess: (updated) => {
      invalidateProspects();
      setSelectedProspect(updated as Prospect);
    },
    onError: () => toast('error', 'Error actualitzant el prospect'),
  });

  const { mutate: doSave, isPending: saving } = useMutation({
    mutationFn: ({ prospectId, payload }: { prospectId: string; payload: UpdateProspectPayload }) =>
      updateProspect(prospectId, payload),
    onSuccess: (updated) => {
      toast('success', 'Dades desades');
      invalidateProspects();
      setSelectedProspect(updated as Prospect);
    },
    onError: () => toast('error', 'Error desant les dades'),
  });

  const { mutate: doExportQualified, isPending: exportingQualified } = useMutation({
    mutationFn: () => exportQualifiedProspects(id),
    onSuccess: (data) => {
      const skippedMsg = data.skipped ? `, ${data.skipped} ja existien` : '';
      toast('success', `${data.exported} nous leads creats${skippedMsg}`);
      invalidateProspects(); invalidateCampaign();
    },
    onError: () => toast('error', 'Error exportant els prospects qualificats'),
  });

  const { mutate: doSchedule, isPending: scheduling } = useMutation({
    mutationFn: () => scheduleCampaign(id, new Date(Date.now() + 86400000).toISOString(), 7),
    onSuccess: () => { toast('success', 'Campanya programada'); invalidateCampaign(); },
    onError: () => toast('error', 'Error programant la campanya'),
  });

  const { mutate: doUnschedule } = useMutation({
    mutationFn: () => unscheduleCampaign(id),
    onSuccess: () => { toast('success', 'Programació cancel·lada'); invalidateCampaign(); },
    onError: () => toast('error', 'Error cancel·lant la programació'),
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

        {/* Com funciona */}
        <HowItWorks />

        {/* Campaign info */}
        <div className="amg-card card-clip p-5 space-y-4">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            {[
              { label: 'Sector',    value: c.sector },
              { label: 'Localitat', value: c.location },
              { label: 'Font',      value: SOURCE_LABEL[c.source] ?? c.source },
              { label: 'Prospects', value: String(prospectList.length) },
            ].map(({ label, value }) => (
              <div key={label}>
                <div className="f-mono text-[10px] text-ink-3 uppercase tracking-wider mb-0.5">{label}</div>
                <div className="f-display font-bold text-sm text-ink-0">{value}</div>
              </div>
            ))}
          </div>
          <div className="flex flex-wrap gap-2 pt-2 border-t border-border-base">
            {c.status === 'DRAFT' && (
              <AMGButton size="sm" icon={IconSet.Clock} loading={scheduling} onClick={() => doSchedule()}>
                Programar (cada 7 dies)
              </AMGButton>
            )}
            {c.status === 'SCHEDULED' && (
              <>
                <AMGBadge tone="accent">Repeteix cada {c.repeatIntervalDays ?? 7} dies</AMGBadge>
                <AMGButton size="sm" variant="ghost" icon={IconSet.X} onClick={() => doUnschedule()}>
                  Aturar programació
                </AMGButton>
              </>
            )}
          </div>
        </div>

        {/* Prospects */}
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
            <div className="f-mono text-label uppercase text-ink-2 tracking-widest">
              Prospects
              <span className="ml-2 text-ink-3 normal-case">({prospectList.length})</span>
            </div>
            {prospectList.length > 0 && (
              <div className="flex items-center gap-2 flex-wrap justify-end">
                <AMGButton size="sm" variant="secondary" icon={IconSet.Sparkles} loading={scoring} onClick={() => doScore()}>
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
                      size="sm" variant="secondary" icon={IconSet.Phone} loading={qualifying}
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
                <AMGButton size="sm" variant="secondary" icon={IconSet.Refresh} loading={enrichingAll} onClick={() => doEnrichAll()}>
                  Enriquir tots
                </AMGButton>
                {prospectList.some(p => p.status === 'QUALIFIED') && (
                  <AMGButton
                    size="sm" icon={IconSet.ArrowRight} loading={exportingQualified}
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
              <IconSet.Search size={28} stroke="#64748b" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Cap prospect trobat</div>
              <p className="f-mono text-label text-ink-2">Executa la campanya per obtenir resultats</p>
            </div>
          ) : (
            <table className="w-full">
              <thead>
                <tr className="border-b border-border-base">
                  {['', 'Empresa', 'Telèfon', 'Punts', 'Estat', ''].map((h, i) => (
                    <th key={i} className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal first:px-4 first:w-6">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {[...prospectList]
                  .filter(p => showDiscarded || p.status !== 'DISCARDED')
                  .sort((a, b) => {
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
                      <tr key={p.id} className={`transition-colors ${rowClass}`}>
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
                          <div className="flex items-center gap-1.5">
                            <span className={`f-mono text-xs font-bold ${scoreColor}`}>{scored ? p.score : '—'}</span>
                            {p.signals && p.signals.length > 0 && (
                              <span
                                title={p.signals.map(s => s.label).join(' · ')}
                                className="inline-flex items-center justify-center w-4 h-4 rounded-full bg-warning/20 text-warning f-mono text-[9px] font-bold leading-none"
                              >
                                {p.signals.filter(s => s.tone !== 'neutral').length}
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="px-4 sm:px-5 py-2.5">
                          <AMGBadge tone={PROSPECT_STATUS_TONE[p.status] ?? 'neutral'}>
                            {PROSPECT_STATUS_LABEL[p.status] ?? p.status}
                          </AMGBadge>
                        </td>
                        <td className="px-4 sm:px-5 py-2.5 text-right">
                          <AMGButton size="sm" variant="ghost" icon={IconSet.Eye} onClick={() => setSelectedProspect(p)}>
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
        onSave={(pid, payload) => doSave({ prospectId: pid, payload })}
        exporting={exporting && exportingId === selectedProspect.id}
        enriching={enriching && enrichingId === selectedProspect.id}
        updatingStatus={updatingStatus}
        saving={saving}
      />,
      document.body
    )}
    </>
  );
}
