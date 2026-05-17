'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import {
  getLead, getActivities, changeStage, createActivity,
  type Activity,
} from '@/services/leads';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { I } from '@/components/ui/icons';
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

  const [newActivity, setNewActivity] = useState({ type: 'CALL', notes: '', dueDate: '' });
  const [showActivityForm, setShowActivityForm] = useState(false);

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

  const { mutate: doCreateActivity, isPending: creatingActivity } = useMutation({
    mutationFn: () => createActivity(id, {
      type: newActivity.type,
      notes: newActivity.notes,
      dueDate: newActivity.dueDate || undefined,
    }),
    onSuccess: () => {
      toast('success', 'Activitat registrada');
      setNewActivity({ type: 'CALL', notes: '', dueDate: '' });
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
    <PortalShell breadcrumb={`leads / ${lead.companyName}`}>
      <div className="p-4 sm:p-8 space-y-6 max-w-3xl">
        <div>
          <button
            onClick={() => router.push(`/${locale}/portal/leads`)}
            className="f-mono text-label text-ink-2 hover:text-accent-light flex items-center gap-1 mb-3"
          >
            <I.ArrowRight size={12} className="rotate-180" /> Tornar
          </button>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / leads /</span>
          <div className="flex items-center gap-3 mt-1">
            <div className="f-display font-bold text-xl">{lead.companyName}</div>
            <AMGBadge tone={STAGE_TONE[lead.stage]}>{STAGE_LABEL[lead.stage]}</AMGBadge>
          </div>
        </div>

        {/* Informació del lead */}
        <div className="amg-card card-clip p-6">
          <div className="f-mono text-label uppercase text-ink-2 tracking-widest mb-4">Informació del Lead</div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {[
              { label: 'Contacte', value: lead.contactName },
              { label: 'Email', value: lead.contactEmail },
              { label: 'Telèfon', value: lead.contactPhone ?? '—' },
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

        {/* Activitats */}
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
            <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Activitats</div>
            <AMGButton size="sm" icon={I.Plus} onClick={() => setShowActivityForm(!showActivityForm)}>
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
                <label className="f-mono text-label text-ink-3 uppercase tracking-wider block mb-1">Notes</label>
                <textarea
                  value={newActivity.notes}
                  onChange={(e) => setNewActivity((a) => ({ ...a, notes: e.target.value }))}
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
              <I.Calendar size={24} stroke="#64748b" className="mx-auto mb-2" />
              <p className="f-mono text-label text-ink-3">Cap activitat registrada</p>
            </div>
          ) : (
            <div className="divide-y divide-border-base">
              {(activities as Activity[]).map((act) => (
                <div key={act.id} className="px-4 sm:px-5 py-3 flex items-start gap-3">
                  <AMGBadge tone={act.completed ? 'success' : 'neutral'}>{act.type}</AMGBadge>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-ink-1">{act.notes}</p>
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
    </PortalShell>
  );
}
