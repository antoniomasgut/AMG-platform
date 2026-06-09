'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import {
  getLead, getActivities, changeStage, createActivity, setWhatsapp, updateLead, deleteLead,
  completeActivity, updateActivity, deleteActivity,
  analyzeNotes, type AnalyzeNotesResponse, type Activity,
} from '@/services/leads';
import { createBookingToken, getTokensForLead, type BookingToken } from '@/services/booking';
import dynamic from 'next/dynamic';
import { PortalShell } from '@/components/portal/PortalShell';

const ConvertLeadModal = dynamic(() => import('@/components/leads/ConvertLeadModal').then(mod => mod.ConvertLeadModal), { ssr: false });
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { IconSet } from '@/components/ui/icons';
import { useRouter, useParams } from 'next/navigation';

const STAGES = ['NEW', 'CONTACTED', 'QUALIFIED', 'PROPOSAL', 'NEGOTIATION', 'WON', 'LOST'] as const;

const STAGE_LABEL: Record<string, string> = {
  NEW: 'Nou', CONTACTED: 'Contactat', QUALIFIED: 'Qualificat',
  PROPOSAL: 'Proposta', NEGOTIATION: 'Negociació', WON: 'Guanyat', LOST: 'Perdut',
};

const STAGE_TONE: Record<string, 'neutral' | 'info' | 'accent' | 'warning' | 'success' | 'danger'> = {
  NEW: 'neutral', CONTACTED: 'info', QUALIFIED: 'accent',
  PROPOSAL: 'warning', NEGOTIATION: 'warning', WON: 'success', LOST: 'danger',
};

const SOURCE_OPTIONS = [
  { value: 'WEBSITE', label: 'Web' },
  { value: 'REFERRAL', label: 'Referit' },
  { value: 'COLD_CALL', label: 'Cold Call' },
  { value: 'SOCIAL_MEDIA', label: 'RRSS' },
  { value: 'GOOGLE_MAPS', label: 'Google Maps' },
  { value: 'INSTAGRAM', label: 'Instagram' },
  { value: 'PAGINAS_AMARILLAS', label: 'Pàg. Grogues' },
  { value: 'META_LEAD_ADS', label: 'Meta Ads' },
  { value: 'OTHER', label: 'Altre' },
];

const SOURCE_LABEL: Record<string, string> = Object.fromEntries(SOURCE_OPTIONS.map(o => [o.value, o.label]));

function fmtDate(d: string) {
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="f-mono text-label text-ink-3 uppercase tracking-wider mb-0.5">{label}</div>
      <div className="f-display text-sm text-ink-0">{value || '—'}</div>
    </div>
  );
}

function EditField({ label, value, onChange, type = 'text', placeholder }: {
  label: string; value: string; onChange: (v: string) => void; type?: string; placeholder?: string;
}) {
  return (
    <div>
      <label className="f-mono text-[10px] uppercase text-ink-3 tracking-wider block mb-1">{label}</label>
      <input
        type={type} value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder}
        className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
      />
    </div>
  );
}

function LostReasonModal({ onClose, onConfirm, loading }: {
  onClose: () => void; onConfirm: (reason: string) => void; loading: boolean;
}) {
  const [reason, setReason] = useState('');
  return (
    <>
      <div className="fixed inset-0 bg-black/60 z-40" onClick={onClose} />
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div className="bg-bg-0 border border-border-base w-full max-w-sm shadow-2xl">
          <div className="flex items-center justify-between p-5 border-b border-border-base">
            <div className="f-display font-bold text-base">Marcar com a perdut</div>
            <button onClick={onClose} className="p-1.5 text-ink-3 hover:text-ink-0"><IconSet.X size={16} /></button>
          </div>
          <div className="p-5">
            <label className="f-mono text-[10px] uppercase text-ink-3 tracking-widest block mb-2">Motiu de pèrdua</label>
            <input
              type="text" value={reason} onChange={e => setReason(e.target.value)} autoFocus
              placeholder="Ex: Preu massa alt, ja té proveïdor..."
              onKeyDown={e => e.key === 'Enter' && onConfirm(reason.trim() || 'No especificat')}
              className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
            />
          </div>
          <div className="p-4 border-t border-border-base flex justify-end gap-2">
            <AMGButton variant="ghost" onClick={onClose}>Cancel·lar</AMGButton>
            <AMGButton loading={loading} onClick={() => onConfirm(reason.trim() || 'No especificat')}>Confirmar</AMGButton>
          </div>
        </div>
      </div>
    </>
  );
}

export default function LeadDetailPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();
  const router = useRouter();
  const params = useParams();
  const locale = params.locale as string;
  const id = params.id as string;

  const [newActivity, setNewActivity] = useState({ type: 'CALL', description: '', dueDate: '' });
  const [showActivityForm, setShowActivityForm] = useState(false);
  const [meetingNote, setMeetingNote] = useState('');
  const [showMeetingNoteForm, setShowMeetingNoteForm] = useState(false);
  const [copiedToken, setCopiedToken] = useState(false);
  const [interviewNotes, setInterviewNotes] = useState('');
  const [analysis, setAnalysis] = useState<AnalyzeNotesResponse | null>(null);
  const [showConvertModal, setShowConvertModal] = useState(false);
  const [showLostModal, setShowLostModal] = useState(false);
  const [showArchiveConfirm, setShowArchiveConfirm] = useState(false);

  const [editMode, setEditMode] = useState(false);
  const [editData, setEditData] = useState({
    name: '', email: '', phone: '', notes: '', estimatedValue: '', source: '',
  });

  const [editingActivityId, setEditingActivityId] = useState<string | null>(null);
  const [editActivityData, setEditActivityData] = useState({ type: 'CALL', description: '', dueDate: '' });
  const [deletingActivityId, setDeletingActivityId] = useState<string | null>(null);

  const { data: lead, isLoading: loadingLead } = useQuery({
    queryKey: ['lead', id],
    queryFn: () => getLead(id),
    enabled: !!user && !!id,
  });

  const { data: activities = [], isLoading: loadingActivities } = useQuery({
    queryKey: ['lead-activities', id],
    queryFn: () => getActivities(id),
    enabled: !!user && !!id,
  });

  const { data: bookingTokens = [] } = useQuery({
    queryKey: ['booking-tokens', id],
    queryFn: () => getTokensForLead(id),
    enabled: !!user && !!id,
  });

  const { mutate: doChangeStage, isPending: changingStage } = useMutation({
    mutationFn: ({ stage, lostReason }: { stage: string; lostReason?: string }) =>
      changeStage(id, stage, lostReason),
    onSuccess: () => {
      toast('success', 'Etapa actualitzada');
      setShowLostModal(false);
      qc.invalidateQueries({ queryKey: ['lead', id] });
      qc.invalidateQueries({ queryKey: ['leads'] });
    },
    onError: () => toast('error', 'Error actualitzant etapa'),
  });

  const { mutate: doSetWhatsapp } = useMutation({
    mutationFn: (value: boolean) => setWhatsapp(id, value),
    onSuccess: () => {
      toast('success', 'WhatsApp actualitzat');
      qc.invalidateQueries({ queryKey: ['lead', id] });
    },
    onError: () => toast('error', 'Error actualitzant WhatsApp'),
  });

  const { mutate: doUpdateLead, isPending: updatingLead } = useMutation({
    mutationFn: () => updateLead(id, {
      name: editData.name || undefined,
      email: editData.email || undefined,
      phone: editData.phone || undefined,
      notes: editData.notes || undefined,
      source: editData.source || undefined,
      estimatedValue: editData.estimatedValue ? Number(editData.estimatedValue) : undefined,
    }),
    onSuccess: () => {
      toast('success', 'Lead actualitzat');
      setEditMode(false);
      qc.invalidateQueries({ queryKey: ['lead', id] });
      qc.invalidateQueries({ queryKey: ['leads'] });
    },
    onError: () => toast('error', 'Error actualitzant el lead'),
  });

  const { mutate: doDeleteLead, isPending: deletingLead } = useMutation({
    mutationFn: () => deleteLead(id),
    onSuccess: () => {
      toast('success', 'Lead arxivat');
      router.push(`/${locale}/portal/leads`);
    },
    onError: () => toast('error', 'Error arxivant el lead'),
  });

  const { mutate: doCompleteActivity } = useMutation({
    mutationFn: (activityId: string) => completeActivity(id, activityId),
    onSuccess: () => {
      toast('success', 'Activitat completada');
      qc.invalidateQueries({ queryKey: ['lead-activities', id] });
    },
    onError: () => toast('error', 'Error completant activitat'),
  });

  const { mutate: doUpdateActivity, isPending: updatingActivity } = useMutation({
    mutationFn: (activityId: string) => updateActivity(id, activityId, {
      type: editActivityData.type,
      description: editActivityData.description,
      dueDate: editActivityData.dueDate || undefined,
    }),
    onSuccess: () => {
      toast('success', 'Activitat actualitzada');
      setEditingActivityId(null);
      qc.invalidateQueries({ queryKey: ['lead-activities', id] });
    },
    onError: () => toast('error', 'Error actualitzant activitat'),
  });

  const { mutate: doDeleteActivity } = useMutation({
    mutationFn: (activityId: string) => deleteActivity(id, activityId),
    onSuccess: () => {
      toast('success', 'Activitat esborrada');
      setDeletingActivityId(null);
      qc.invalidateQueries({ queryKey: ['lead-activities', id] });
    },
    onError: () => toast('error', 'Error esborrant activitat'),
  });

  const { mutate: doSaveMeetingNote, isPending: savingNote } = useMutation({
    mutationFn: (note: string) => createActivity(id, { type: 'NOTE', description: note }),
    onSuccess: () => {
      toast('success', 'Nota desada');
      setMeetingNote('');
      setShowMeetingNoteForm(false);
      qc.invalidateQueries({ queryKey: ['lead-activities', id] });
    },
    onError: () => toast('error', 'Error desant la nota'),
  });

  const { mutate: doCreateToken, isPending: creatingToken } = useMutation({
    mutationFn: () => createBookingToken(id),
    onSuccess: () => {
      toast('success', 'Link de reserva generat');
      qc.invalidateQueries({ queryKey: ['booking-tokens', id] });
    },
    onError: () => toast('error', 'Error generant el link'),
  });

  const { mutate: doAnalyzeNotes, isPending: analyzingNotes } = useMutation({
    mutationFn: () => analyzeNotes(id, { notes: interviewNotes }),
    onSuccess: (res) => {
      setAnalysis(res);
      toast('success', 'Notes analitzades amb IA');
      qc.invalidateQueries({ queryKey: ['lead-activities', id] });
    },
    onError: () => toast('error', 'Error analitzant les notes'),
  });

  const { mutate: doCreateActivity, isPending: creatingActivity } = useMutation({
    mutationFn: () => createActivity(id, {
      type: newActivity.type,
      description: newActivity.description,
      dueDate: newActivity.dueDate || undefined,
    }),
    onSuccess: () => {
      toast('success', 'Activitat registrada');
      setNewActivity({ type: 'CALL', description: '', dueDate: '' });
      setShowActivityForm(false);
      qc.invalidateQueries({ queryKey: ['lead-activities', id] });
    },
    onError: () => toast('error', 'Error registrant activitat'),
  });

  const enterEditMode = () => {
    if (!lead) return;
    setEditData({
      name: lead.name,
      email: lead.email ?? '',
      phone: lead.phone ?? '',
      notes: lead.notes ?? '',
      estimatedValue: lead.estimatedValue != null ? String(lead.estimatedValue) : '',
      source: lead.source ?? '',
    });
    setEditMode(true);
  };

  if (!user) return null;

  if (loadingLead) {
    return (
      <PortalShell breadcrumb="leads / detall">
        <div className="flex justify-center py-20">
          <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
        </div>
      </PortalShell>
    );
  }

  if (!lead) {
    return (
      <PortalShell breadcrumb="leads / detall">
        <div className="p-8">
          <div className="p-3 border-l-2 border-l-danger bg-danger/5">
            <span className="f-mono text-label text-danger-light">Lead no trobat</span>
          </div>
        </div>
      </PortalShell>
    );
  }

  const hasUtm = lead.utmSource || lead.utmMedium || lead.utmCampaign;

  return (
    <PortalShell breadcrumb={`leads / ${lead.name}`}>
      <div className="p-4 sm:p-8 space-y-6 max-w-3xl">
        <div>
          <button
            onClick={() => router.push(`/${locale}/portal/leads`)}
            className="f-mono text-label text-ink-2 hover:text-accent-light flex items-center gap-1 mb-3"
          >
            <IconSet.ArrowRight size={12} className="rotate-180" /> Tornar
          </button>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / leads /</span>
          <div className="flex items-center justify-between mt-1">
            <div className="flex items-center gap-3">
              <div className="f-display font-bold text-xl">{lead.name}</div>
              <AMGBadge tone={STAGE_TONE[lead.stage]}>{STAGE_LABEL[lead.stage]}</AMGBadge>
              {!lead.isActive && <AMGBadge tone="neutral">Arxivat</AMGBadge>}
            </div>
            {!showArchiveConfirm ? (
              <button
                onClick={() => setShowArchiveConfirm(true)}
                className="f-mono text-[10px] text-ink-3 hover:text-danger-light transition-colors"
              >
                Arxivar lead
              </button>
            ) : (
              <div className="flex items-center gap-2">
                <span className="f-mono text-[10px] text-ink-2">Confirmar?</span>
                <AMGButton size="sm" variant="ghost" onClick={() => setShowArchiveConfirm(false)}>No</AMGButton>
                <AMGButton size="sm" loading={deletingLead} onClick={() => doDeleteLead()}>Sí, arxivar</AMGButton>
              </div>
            )}
          </div>
        </div>

        {/* Informació del lead */}
        <div className="amg-card card-clip p-6">
          <div className="flex items-center justify-between mb-4">
            <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Informació del Lead</div>
            {!editMode ? (
              <button
                onClick={enterEditMode}
                className="flex items-center gap-1.5 f-mono text-[10px] text-ink-3 hover:text-accent-light transition-colors"
              >
                <IconSet.Edit size={12} /> Editar
              </button>
            ) : (
              <div className="flex gap-2">
                <AMGButton size="sm" variant="ghost" onClick={() => setEditMode(false)}>Cancel·lar</AMGButton>
                <AMGButton size="sm" loading={updatingLead} onClick={() => doUpdateLead()}>Desar</AMGButton>
              </div>
            )}
          </div>

          {editMode ? (
            <div className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <EditField label="Nom / Empresa" value={editData.name} onChange={v => setEditData(d => ({ ...d, name: v }))} />
                <EditField label="Email" value={editData.email} onChange={v => setEditData(d => ({ ...d, email: v }))} type="email" />
                <EditField label="Telèfon" value={editData.phone} onChange={v => setEditData(d => ({ ...d, phone: v }))} />
                <EditField label="Valor estimat (€)" value={editData.estimatedValue} onChange={v => setEditData(d => ({ ...d, estimatedValue: v }))} type="number" placeholder="0" />
                <div className="sm:col-span-2">
                  <label className="f-mono text-[10px] uppercase text-ink-3 tracking-wider block mb-1">Origen</label>
                  <select
                    value={editData.source}
                    onChange={e => setEditData(d => ({ ...d, source: e.target.value }))}
                    className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
                  >
                    <option value="">— Selecciona origen —</option>
                    {SOURCE_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                  </select>
                </div>
              </div>
              <div>
                <label className="f-mono text-[10px] uppercase text-ink-3 tracking-wider block mb-1">Notes</label>
                <textarea
                  value={editData.notes} onChange={e => setEditData(d => ({ ...d, notes: e.target.value }))}
                  rows={3}
                  className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 py-2 f-mono text-xs focus:outline-none focus:border-accent resize-none"
                />
              </div>
            </div>
          ) : (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Field label="Nom / Empresa" value={lead.name} />
                <Field label="Email" value={lead.email ?? ''} />
                <Field label="Telèfon" value={lead.phone ?? ''} />
                <Field label="Origen" value={SOURCE_LABEL[lead.source] ?? lead.source} />
                <Field label="Valor estimat" value={lead.estimatedValue != null ? `${lead.estimatedValue} €` : ''} />
                <Field label="Creat" value={fmtDate(lead.createdAt)} />
              </div>

              {lead.notes && (
                <div className="mt-4 pt-4 border-t border-border-base">
                  <div className="f-mono text-label text-ink-3 uppercase tracking-wider mb-1">Notes</div>
                  <p className="text-sm text-ink-1 whitespace-pre-wrap">{lead.notes}</p>
                </div>
              )}

              {lead.stage === 'LOST' && lead.lostReason && (
                <div className="mt-4 pt-4 border-t border-border-base">
                  <div className="f-mono text-label text-ink-3 uppercase tracking-wider mb-1">Motiu de pèrdua</div>
                  <p className="text-sm text-danger-light">{lead.lostReason}</p>
                </div>
              )}

              {lead.tags && (
                <div className="mt-4 pt-4 border-t border-border-base">
                  <div className="f-mono text-label text-ink-3 uppercase tracking-wider mb-2">Etiquetes</div>
                  <div className="flex flex-wrap gap-1.5">
                    {lead.tags.split(',').map(t => t.trim()).filter(Boolean).map(tag => (
                      <AMGBadge key={tag} tone="neutral">{tag}</AMGBadge>
                    ))}
                  </div>
                </div>
              )}

              {hasUtm && (
                <div className="mt-4 pt-4 border-t border-border-base">
                  <div className="f-mono text-label text-ink-3 uppercase tracking-wider mb-2">UTM / Origen digital</div>
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                    {lead.utmSource && <Field label="utm_source" value={lead.utmSource} />}
                    {lead.utmMedium && <Field label="utm_medium" value={lead.utmMedium} />}
                    {lead.utmCampaign && <Field label="utm_campaign" value={lead.utmCampaign} />}
                  </div>
                </div>
              )}
            </>
          )}

          {lead.phone && (
            <div className="mt-4 pt-4 border-t border-border-base">
              <div className="f-mono text-label text-ink-3 uppercase tracking-wider mb-2">WhatsApp</div>
              <div className="flex items-center gap-2 flex-wrap">
                <span className={`f-mono text-xs ${lead.hasWhatsapp ? 'text-success' : 'text-ink-3'}`}>
                  {lead.hasWhatsapp ? '✓ Verificat' : 'No verificat'}
                </span>
                <a
                  href={(() => { const d = lead.phone.replace(/\D/g, ''); return `https://wa.me/${d.length === 9 ? '34' + d : d}`; })()}
                  target="_blank" rel="noopener noreferrer"
                  className="f-mono text-xs border border-border-base text-ink-2 hover:border-success hover:text-success px-2 py-1 transition-colors"
                >
                  Provar WA →
                </a>
                <button
                  onClick={() => doSetWhatsapp(!lead.hasWhatsapp)}
                  className={`f-mono text-xs px-2 py-1 border transition-colors ${
                    lead.hasWhatsapp
                      ? 'border-success text-success hover:border-danger hover:text-danger-light'
                      : 'border-border-base text-ink-3 hover:border-success hover:text-success'
                  }`}
                >
                  {lead.hasWhatsapp ? 'Desmarcar WA' : 'Marcar WA'}
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Canvi d'etapa */}
        <div className="amg-card card-clip p-6">
          <div className="f-mono text-label uppercase text-ink-2 tracking-widest mb-4">Canviar Etapa</div>
          <div className="flex flex-wrap gap-2">
            {STAGES.map((stage) => (
              <button
                key={stage}
                disabled={lead.stage === stage || changingStage}
                onClick={() => {
                  if (stage === 'LOST') { setShowLostModal(true); return; }
                  doChangeStage({ stage });
                }}
                className={`f-mono text-label uppercase px-3 h-8 border transition-colors ${
                  lead.stage === stage
                    ? 'border-accent text-accent-light bg-accent-muted cursor-default'
                    : stage === 'LOST'
                    ? 'border-border-base text-ink-3 hover:border-danger hover:text-danger-light'
                    : 'border-border-base text-ink-2 hover:text-ink-0 hover:border-border-strong'
                } disabled:opacity-40`}
              >
                {STAGE_LABEL[stage]}
              </button>
            ))}
          </div>
        </div>

        {/* Reserva de reunió */}
        <div className="amg-card card-clip p-6 space-y-4">
          <div className="flex items-center justify-between">
            <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Videoconferència</div>
            <AMGButton size="sm" icon={IconSet.Calendar} loading={creatingToken} onClick={() => doCreateToken()}>
              Generar link de reserva
            </AMGButton>
          </div>

          {bookingTokens.length === 0 && (
            <p className="text-sm text-ink-3">
              Genera un link de reserva per enviar al lead. Podrà triar dia i hora des d&apos;una pàgina pública.
            </p>
          )}

          {(bookingTokens as BookingToken[]).map(bt => {
            const bookingUrl = `${typeof window !== 'undefined' ? window.location.origin : ''}/${locale}/book/${bt.token}`;
            const isExpired = new Date(bt.expiresAt) < new Date();
            return (
              <div key={bt.id} className={`border rounded-lg p-4 space-y-3 ${bt.confirmed ? 'border-success/40 bg-success/5' : isExpired ? 'border-border-base opacity-50' : 'border-accent/40 bg-accent-muted/20'}`}>
                <div className="flex items-center justify-between flex-wrap gap-2">
                  <div className="flex items-center gap-2">
                    <AMGBadge tone={bt.confirmed ? 'success' : isExpired ? 'neutral' : 'info'}>
                      {bt.confirmed ? 'Confirmat' : isExpired ? 'Expirat' : 'Pendent'}
                    </AMGBadge>
                    <span className="f-mono text-[10px] text-ink-3">
                      Expira {new Date(bt.expiresAt).toLocaleDateString('ca-ES')}
                    </span>
                  </div>
                  {!bt.confirmed && !isExpired && (
                    <button
                      onClick={() => { navigator.clipboard.writeText(bookingUrl); setCopiedToken(true); setTimeout(() => setCopiedToken(false), 2000); }}
                      className="f-mono text-xs text-accent-light hover:underline flex items-center gap-1"
                    >
                      <IconSet.Copy size={11} />
                      {copiedToken ? 'Copiat!' : 'Copiar link'}
                    </button>
                  )}
                </div>

                {bt.confirmed && bt.meetingAt && (
                  <div className="space-y-1.5">
                    <p className="text-sm text-ink-1 font-medium">
                      {new Date(bt.meetingAt).toLocaleString('ca-ES', {
                        weekday: 'long', day: 'numeric', month: 'long',
                        hour: '2-digit', minute: '2-digit', timeZone: 'Europe/Madrid',
                      })}
                    </p>
                    {bt.meetLink && (
                      <a href={bt.meetLink} target="_blank" rel="noopener noreferrer"
                        className="text-xs text-accent-light hover:underline flex items-center gap-1">
                        <IconSet.Video size={12} /> Unir-se a Google Meet →
                      </a>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Notes d'Entrevista (IA) */}
        <div className="amg-card card-clip p-6 space-y-4 border-l-4 border-l-[#FF6B00]">
          <div className="f-mono text-label uppercase text-ink-2 tracking-widest flex items-center gap-2">
            <IconSet.Zap size={14} className="text-[#FF6B00]" /> Notes d&apos;Entrevista (IA)
          </div>
          <p className="text-sm text-ink-3">
            Escriu les necessitats, problemes i dolors del client durant la videoconferència. La IA les analitzarà per preparar un pressupost.
          </p>
          <textarea
            value={interviewNotes} onChange={e => setInterviewNotes(e.target.value)}
            placeholder="Ex: És una perruqueria. El client diu que perd vendes per no tenir web, vol agafar cites online..."
            rows={6}
            className="w-full bg-bg-0 border border-border-base text-ink-0 px-3 py-2 f-mono text-xs focus:outline-none focus:border-[#FF6B00] resize-y"
          />
          <div className="flex justify-end">
            <AMGButton size="sm" icon={IconSet.Zap} loading={analyzingNotes} disabled={!interviewNotes.trim()} onClick={() => doAnalyzeNotes()}>
              Analitzar amb IA
            </AMGButton>
          </div>

          {analysis && (
            <div className="mt-6 pt-4 border-t border-[rgba(255,255,255,0.05)] space-y-4">
              <div className="flex items-center gap-2 mb-2">
                <IconSet.Check size={16} className="text-success" />
                <span className="font-semibold text-sm">Anàlisi completat</span>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <div className="f-mono text-[10px] uppercase text-ink-3">Punts de dolor detectats</div>
                  <ul className="space-y-1">
                    {analysis.painPoints.map((pp, i) => (
                      <li key={i} className="text-xs text-ink-1 flex items-start gap-1.5">
                        <span className="text-[#FF6B00] mt-0.5">•</span> {pp}
                      </li>
                    ))}
                  </ul>
                </div>
                <div className="space-y-3 bg-[rgba(255,255,255,0.02)] p-3 rounded border border-border-base">
                  <div className="flex justify-between items-center pb-2 border-b border-border-base">
                    <span className="f-mono text-[10px] uppercase text-ink-3">Segmentació</span>
                    <div className="flex gap-2">
                      <AMGBadge tone="neutral">{analysis.recommendedSector}</AMGBadge>
                      <AMGBadge tone="neutral">{analysis.recommendedSize}</AMGBadge>
                    </div>
                  </div>
                  <div className="flex justify-between items-center pb-2 border-b border-border-base">
                    <span className="f-mono text-[10px] uppercase text-ink-3">Pressupost Recomanat</span>
                    <div className="text-right">
                      <div className="text-sm font-bold text-white">Setup: {analysis.setupAmount} €</div>
                      <div className="text-xs text-ink-2">Quota: {analysis.monthlyAmount} €/m</div>
                    </div>
                  </div>
                </div>
              </div>

              <div className="bg-[rgba(255,107,0,0.05)] border border-[#FF6B00]/20 p-3 rounded">
                <div className="f-mono text-[10px] uppercase text-[#FF6B00] mb-1">Pitch de Venda Suggrit</div>
                <p className="text-xs text-ink-1 italic">&ldquo;{analysis.recommendationPitch}&rdquo;</p>
              </div>

              <div className="pt-2 flex justify-end">
                <AMGButton onClick={() => setShowConvertModal(true)} icon={IconSet.Briefcase}>
                  Convertir a Client amb aquesta proposta
                </AMGButton>
              </div>
            </div>
          )}
        </div>

        {/* Notes de reunió */}
        {(bookingTokens as BookingToken[]).some(bt => bt.confirmed && bt.meetingAt) && (
          <div className="amg-card card-clip p-6 space-y-3">
            <div className="flex items-center justify-between">
              <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Notes de la reunió</div>
              <AMGButton size="sm" variant="ghost" onClick={() => setShowMeetingNoteForm(s => !s)}>
                {showMeetingNoteForm ? 'Tancar' : '+ Afegir nota'}
              </AMGButton>
            </div>
            {showMeetingNoteForm && (
              <div className="space-y-2">
                <textarea
                  value={meetingNote} onChange={e => setMeetingNote(e.target.value)}
                  placeholder="Punts tractats, acords, seguiment necessari..."
                  rows={5}
                  className="w-full bg-bg-0 border border-border-base text-ink-0 px-3 py-2 f-mono text-xs focus:outline-none focus:border-accent resize-none"
                />
                <AMGButton size="sm" loading={savingNote} disabled={!meetingNote.trim()} onClick={() => doSaveMeetingNote(meetingNote.trim())}>
                  Desar notes
                </AMGButton>
              </div>
            )}
          </div>
        )}

        {/* Activitats */}
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
            <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Activitats</div>
            <AMGButton size="sm" icon={IconSet.Plus} onClick={() => setShowActivityForm(!showActivityForm)}>
              Nova activitat
            </AMGButton>
          </div>

          {showActivityForm && (
            <div className="p-4 sm:p-5 border-b border-border-base space-y-3 bg-bg-1">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="f-mono text-label text-ink-3 uppercase tracking-wider block mb-1">Tipus</label>
                  <select
                    value={newActivity.type}
                    onChange={e => setNewActivity(a => ({ ...a, type: e.target.value }))}
                    className="w-full bg-bg-0 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
                  >
                    {['CALL', 'EMAIL', 'MEETING', 'NOTE', 'TASK'].map(t => (
                      <option key={t} value={t}>{t}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="f-mono text-label text-ink-3 uppercase tracking-wider block mb-1">Data prevista</label>
                  <input
                    type="date" value={newActivity.dueDate}
                    onChange={e => setNewActivity(a => ({ ...a, dueDate: e.target.value }))}
                    className="w-full bg-bg-0 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
                  />
                </div>
              </div>
              <div>
                <label className="f-mono text-label text-ink-3 uppercase tracking-wider block mb-1">Descripció</label>
                <textarea
                  value={newActivity.description}
                  onChange={e => setNewActivity(a => ({ ...a, description: e.target.value }))}
                  rows={3}
                  className="w-full bg-bg-0 border border-border-base text-ink-0 px-3 py-2 f-mono text-xs focus:outline-none focus:border-accent resize-none"
                />
              </div>
              <AMGButton size="sm" loading={creatingActivity} onClick={() => doCreateActivity()}>
                Desar activitat
              </AMGButton>
            </div>
          )}

          {loadingActivities ? (
            <div className="flex justify-center py-8">
              <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
            </div>
          ) : activities.length === 0 ? (
            <div className="p-6 text-center">
              <IconSet.Calendar size={24} stroke="#64748b" className="mx-auto mb-2" />
              <p className="f-mono text-label text-ink-3">Cap activitat registrada</p>
            </div>
          ) : (
            <div className="divide-y divide-border-base">
              {(activities as Activity[]).map(act => {
                const isEditing = editingActivityId === act.id;
                const isConfirmDelete = deletingActivityId === act.id;
                return (
                  <div key={act.id} className={`px-4 sm:px-5 py-3 ${act.completedAt ? 'opacity-60' : ''}`}>
                    {isEditing ? (
                      <div className="space-y-2">
                        <div className="flex gap-2">
                          <select
                            value={editActivityData.type}
                            onChange={e => setEditActivityData(d => ({ ...d, type: e.target.value }))}
                            className="bg-bg-1 border border-border-base text-ink-0 px-2 h-8 f-mono text-xs focus:outline-none focus:border-accent"
                          >
                            {['CALL', 'EMAIL', 'MEETING', 'NOTE', 'TASK'].map(t => <option key={t} value={t}>{t}</option>)}
                          </select>
                          <input
                            type="date" value={editActivityData.dueDate}
                            onChange={e => setEditActivityData(d => ({ ...d, dueDate: e.target.value }))}
                            className="bg-bg-1 border border-border-base text-ink-0 px-2 h-8 f-mono text-xs focus:outline-none focus:border-accent"
                          />
                        </div>
                        <textarea
                          value={editActivityData.description}
                          onChange={e => setEditActivityData(d => ({ ...d, description: e.target.value }))}
                          rows={2}
                          className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 py-1.5 f-mono text-xs focus:outline-none focus:border-accent resize-none"
                        />
                        <div className="flex gap-2">
                          <AMGButton size="sm" loading={updatingActivity} onClick={() => doUpdateActivity(act.id)}>Desar</AMGButton>
                          <AMGButton size="sm" variant="ghost" onClick={() => setEditingActivityId(null)}>Cancel·lar</AMGButton>
                        </div>
                      </div>
                    ) : (
                      <div className="flex items-start gap-3">
                        <AMGBadge tone={act.completedAt ? 'success' : 'neutral'}>{act.type}</AMGBadge>
                        <div className="flex-1 min-w-0">
                          <p className={`text-sm text-ink-1 ${act.completedAt ? 'line-through' : ''}`}>{act.description}</p>
                          {act.dueDate && <span className="f-mono text-label text-ink-3">{fmtDate(act.dueDate)}</span>}
                          {act.completedAt && (
                            <div className="f-mono text-[10px] text-success mt-0.5">Completada {fmtDate(act.completedAt)}</div>
                          )}
                        </div>
                        <div className="flex items-center gap-1.5 shrink-0">
                          {!act.completedAt && (
                            <button
                              onClick={() => doCompleteActivity(act.id)}
                              className="f-mono text-[10px] text-ink-3 hover:text-success border border-border-base hover:border-success px-2 py-0.5 transition-colors"
                            >
                              ✓ Fet
                            </button>
                          )}
                          {!act.completedAt && (
                            <button
                              onClick={() => {
                                setEditingActivityId(act.id);
                                setEditActivityData({
                                  type: act.type as string,
                                  description: act.description,
                                  dueDate: act.dueDate ? new Date(act.dueDate).toISOString().split('T')[0] : '',
                                });
                              }}
                              className="p-1 text-ink-3 hover:text-accent-light transition-colors"
                              title="Editar"
                            >
                              <IconSet.Edit size={11} />
                            </button>
                          )}
                          {isConfirmDelete ? (
                            <div className="flex items-center gap-1">
                              <span className="f-mono text-[9px] text-ink-3">Segur?</span>
                              <button onClick={() => doDeleteActivity(act.id)} className="f-mono text-[9px] text-danger-light hover:underline">Sí</button>
                              <button onClick={() => setDeletingActivityId(null)} className="f-mono text-[9px] text-ink-3 hover:underline">No</button>
                            </div>
                          ) : (
                            <button
                              onClick={() => setDeletingActivityId(act.id)}
                              className="p-1 text-ink-3 hover:text-danger-light transition-colors"
                              title="Esborrar"
                            >
                              <IconSet.Trash size={11} />
                            </button>
                          )}
                          <span className="f-mono text-label text-ink-3">{fmtDate(act.createdAt)}</span>
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {showConvertModal && (
        <ConvertLeadModal
          leadId={id}
          leadName={lead.name}
          leadEmail={lead.email ?? ''}
          initialPhone={lead.phone ?? ''}
          initialSector={analysis?.recommendedSector ?? ''}
          initialSize={analysis?.recommendedSize ?? ''}
          initialSetup={analysis?.setupAmount}
          initialMonthly={analysis?.monthlyAmount}
          onClose={() => setShowConvertModal(false)}
        />
      )}

      {showLostModal && (
        <LostReasonModal
          loading={changingStage}
          onClose={() => setShowLostModal(false)}
          onConfirm={reason => doChangeStage({ stage: 'LOST', lostReason: reason })}
        />
      )}
    </PortalShell>
  );
}
