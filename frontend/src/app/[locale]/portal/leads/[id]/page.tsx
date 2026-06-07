'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import {
  getLead, getActivities, changeStage, createActivity, setWhatsapp,
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

function fmtDate(d: string) {
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
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

  const { mutate: doChangeStage, isPending: changingStage } = useMutation({
    mutationFn: (stage: string) => changeStage(id, stage),
    onSuccess: () => {
      toast('success', 'Etapa actualitzada');
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

  const { data: bookingTokens = [] } = useQuery({
    queryKey: ['booking-tokens', id],
    queryFn: () => getTokensForLead(id),
    enabled: !!user && !!id,
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
          <div className="flex items-center gap-3 mt-1">
            <div className="f-display font-bold text-xl">{lead.name}</div>
            <AMGBadge tone={STAGE_TONE[lead.stage]}>{STAGE_LABEL[lead.stage]}</AMGBadge>
          </div>
        </div>

        {/* Informació del lead */}
        <div className="amg-card card-clip p-6">
          <div className="f-mono text-label uppercase text-ink-2 tracking-widest mb-4">Informació del Lead</div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {[
              { label: 'Nom / Empresa', value: lead.name },
              { label: 'Email', value: lead.email ?? '—' },
              { label: 'Telèfon', value: lead.phone ?? '—' },
              { label: 'Origen', value: lead.source },
              { label: 'Creat', value: fmtDate(lead.createdAt) },
              { label: 'Actualitzat', value: fmtDate(lead.updatedAt) },
            ].map(({ label, value }) => (
              <div key={label}>
                <div className="f-mono text-label text-ink-3 uppercase tracking-wider mb-0.5">{label}</div>
                <div className="f-display text-sm text-ink-0">{value}</div>
              </div>
            ))}
          </div>
          {lead.phone && (
            <div className="mt-4 pt-4 border-t border-border-base">
              <div className="f-mono text-label text-ink-3 uppercase tracking-wider mb-2">WhatsApp</div>
              <div className="flex items-center gap-2 flex-wrap">
                <span className={`f-mono text-xs ${lead.hasWhatsapp ? 'text-success' : 'text-ink-3'}`}>
                  {lead.hasWhatsapp ? '✓ Verificat' : 'No verificat'}
                </span>
                <a
                  href={(() => { const d = lead.phone.replace(/\D/g, ''); return `https://wa.me/${d.length === 9 ? '34' + d : d}`; })()}
                  target="_blank"
                  rel="noopener noreferrer"
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
          {lead.notes && (
            <div className="mt-4 pt-4 border-t border-border-base">
              <div className="f-mono text-label text-ink-3 uppercase tracking-wider mb-1">Notes</div>
              <p className="text-sm text-ink-1">{lead.notes}</p>
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
                onClick={() => doChangeStage(stage)}
                className={`f-mono text-label uppercase px-3 h-8 border transition-colors ${
                  lead.stage === stage
                    ? 'border-accent text-accent-light bg-accent-muted cursor-default'
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
            <AMGButton
              size="sm"
              icon={IconSet.Calendar}
              loading={creatingToken}
              onClick={() => doCreateToken()}
            >
              Generar link de reserva
            </AMGButton>
          </div>

          {bookingTokens.length === 0 && (
            <p className="text-sm text-ink-3">
              Genera un link de reserva per enviar al lead. Podrà triar dia i hora des d'una pàgina pública.
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
                      onClick={() => {
                        navigator.clipboard.writeText(bookingUrl);
                        setCopiedToken(true);
                        setTimeout(() => setCopiedToken(false), 2000);
                      }}
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
                      <a
                        href={bt.meetLink}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-xs text-accent-light hover:underline flex items-center gap-1"
                      >
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
            <IconSet.Zap size={14} className="text-[#FF6B00]" /> Notes d'Entrevista (IA)
          </div>
          <p className="text-sm text-ink-3">
            Escriu les necessitats, problemes i dolors del client durant la videoconferència. La IA les analitzarà per preparar un pressupost.
          </p>
          <textarea
            value={interviewNotes}
            onChange={e => setInterviewNotes(e.target.value)}
            placeholder="Ex: És una perruqueria. El client diu que perd vendes per no tenir web, està molt enfadat perquè no pot agafar cites online i vol una solució..."
            rows={6}
            className="w-full bg-bg-0 border border-border-base text-ink-0 px-3 py-2 f-mono text-xs focus:outline-none focus:border-[#FF6B00] resize-y"
          />
          <div className="flex justify-end">
            <AMGButton
              size="sm"
              icon={IconSet.Zap}
              loading={analyzingNotes}
              disabled={!interviewNotes.trim()}
              onClick={() => doAnalyzeNotes()}
            >
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
                <p className="text-xs text-ink-1 italic">"{analysis.recommendationPitch}"</p>
              </div>

              <div className="pt-2 flex justify-end">
                <AMGButton
                  onClick={() => setShowConvertModal(true)}
                  icon={IconSet.Briefcase}
                >
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
                  value={meetingNote}
                  onChange={e => setMeetingNote(e.target.value)}
                  placeholder="Punts tractats, acords, seguiment necessari..."
                  rows={5}
                  className="w-full bg-bg-0 border border-border-base text-ink-0 px-3 py-2 f-mono text-xs focus:outline-none focus:border-accent resize-none"
                />
                <AMGButton
                  size="sm"
                  loading={savingNote}
                  disabled={!meetingNote.trim()}
                  onClick={() => doSaveMeetingNote(meetingNote.trim())}
                >
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
                    onChange={(e) => setNewActivity((a) => ({ ...a, type: e.target.value }))}
                    className="w-full bg-bg-0 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
                  >
                    {['CALL', 'EMAIL', 'MEETING', 'NOTE', 'TASK'].map((t) => (
                      <option key={t} value={t}>{t}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="f-mono text-label text-ink-3 uppercase tracking-wider block mb-1">Data prevista</label>
                  <input
                    type="date"
                    value={newActivity.dueDate}
                    onChange={(e) => setNewActivity((a) => ({ ...a, dueDate: e.target.value }))}
                    className="w-full bg-bg-0 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
                  />
                </div>
              </div>
              <div>
                <label className="f-mono text-label text-ink-3 uppercase tracking-wider block mb-1">Descripció</label>
                <textarea
                  value={newActivity.description}
                  onChange={(e) => setNewActivity((a) => ({ ...a, description: e.target.value }))}
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
              {(activities as Activity[]).map((act) => (
                <div key={act.id} className="px-4 sm:px-5 py-3 flex items-start gap-3">
                  <AMGBadge tone={act.completedAt ? 'success' : 'neutral'}>{act.type}</AMGBadge>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-ink-1">{act.description}</p>
                    {act.dueDate && (
                      <span className="f-mono text-label text-ink-3">{fmtDate(act.dueDate)}</span>
                    )}
                  </div>
                  <span className="f-mono text-label text-ink-3 shrink-0">{fmtDate(act.createdAt)}</span>
                </div>
              ))}
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
    </PortalShell>
  );
}
