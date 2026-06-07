'use client';

import { useParams } from 'next/navigation';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { getTenant } from '@/services/admin';
import {
  listVisits, createVisit, updateVisit, deleteVisit,
  type VisitRecordResponse, type VisitRecordRequest,
} from '@/services/analytics';
import { useToast } from '@/lib/toast-context';
import { IconSet } from '@/components/ui/icons';

const inp = "w-full bg-surface-base border border-border-base rounded px-3 py-1.5 text-sm text-ink-1 focus:outline-none focus:border-accent-light";

function VisitForm({
  initial, onSave, onCancel,
}: {
  initial?: Partial<VisitRecordRequest>;
  onSave: (r: VisitRecordRequest) => void;
  onCancel: () => void;
}) {
  const [form, setForm] = useState<VisitRecordRequest>({
    contactIdentifier: initial?.contactIdentifier ?? '',
    contactName:       initial?.contactName       ?? '',
    visitDate:         initial?.visitDate         ?? new Date().toISOString().slice(0, 10),
    treatmentType:     initial?.treatmentType     ?? '',
    notes:             initial?.notes             ?? '',
    nextVisitDue:      initial?.nextVisitDue      ?? '',
  });

  return (
    <div className="space-y-3">
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="block text-xs text-ink-2 mb-1">Identificador (tel/email)</label>
          <input className={inp} value={form.contactIdentifier}
            onChange={e => setForm(f => ({ ...f, contactIdentifier: e.target.value }))} />
        </div>
        <div>
          <label className="block text-xs text-ink-2 mb-1">Nom del client</label>
          <input className={inp} value={form.contactName ?? ''}
            onChange={e => setForm(f => ({ ...f, contactName: e.target.value }))} />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="block text-xs text-ink-2 mb-1">Data visita</label>
          <input className={inp} type="date" value={form.visitDate ?? ''}
            onChange={e => setForm(f => ({ ...f, visitDate: e.target.value }))} />
        </div>
        <div>
          <label className="block text-xs text-ink-2 mb-1">Propera revisió</label>
          <input className={inp} type="date" value={form.nextVisitDue ?? ''}
            onChange={e => setForm(f => ({ ...f, nextVisitDue: e.target.value || undefined }))} />
        </div>
      </div>
      <div>
        <label className="block text-xs text-ink-2 mb-1">Tipus tractament / servei</label>
        <input className={inp} value={form.treatmentType ?? ''}
          onChange={e => setForm(f => ({ ...f, treatmentType: e.target.value }))} />
      </div>
      <div>
        <label className="block text-xs text-ink-2 mb-1">Notes</label>
        <textarea className={inp + ' resize-none'} rows={3} value={form.notes ?? ''}
          onChange={e => setForm(f => ({ ...f, notes: e.target.value }))} />
      </div>
      <div className="flex gap-2 pt-1">
        <AMGButton size="sm" onClick={() => onSave(form)}>Desar</AMGButton>
        <AMGButton size="sm" variant="ghost" onClick={onCancel}>Cancel·lar</AMGButton>
      </div>
    </div>
  );
}

export default function VisitsPage() {
  const { id: tenantId } = useParams<{ id: string }>();
  const qc = useQueryClient();
  const { toast } = useToast();
  const [showForm, setShowForm] = useState(false);
  const [editing,  setEditing]  = useState<VisitRecordResponse | null>(null);

  const { data: tenant } = useQuery({
    queryKey: ['tenant', tenantId],
    queryFn: () => getTenant(tenantId),
  });

  const { data: visits = [], isLoading } = useQuery({
    queryKey: ['visits', tenantId],
    queryFn: () => listVisits(tenantId),
  });

  const createMut = useMutation({
    mutationFn: (r: VisitRecordRequest) => createVisit(tenantId, r),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['visits', tenantId] }); setShowForm(false); toast('success', 'Visita registrada'); },
    onError:   () => toast('error', 'Error registrant la visita'),
  });

  const updateMut = useMutation({
    mutationFn: ({ id, r }: { id: string; r: VisitRecordRequest }) => updateVisit(tenantId, id, r),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['visits', tenantId] }); setEditing(null); toast('success', 'Visita actualitzada'); },
    onError:   () => toast('error', 'Error actualitzant la visita'),
  });

  const deleteMut = useMutation({
    mutationFn: (id: string) => deleteVisit(tenantId, id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['visits', tenantId] }); toast('success', 'Visita eliminada'); },
    onError:   () => toast('error', 'Error eliminant la visita'),
  });

  return (
    <PortalShell
      breadcrumb={`admin · tenants · ${tenant?.name ?? tenantId} · visites`}
      backHref={`/portal/admin/tenants/${tenantId}`}
    >
      <div className="p-4 sm:p-8 space-y-6 max-w-3xl">

        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-lg font-semibold text-ink-0">Historial de Visites</h1>
            <p className="text-sm text-ink-2 mt-0.5">{tenant?.name}</p>
          </div>
          {!showForm && (
            <AMGButton size="sm" icon={IconSet.Plus} onClick={() => { setShowForm(true); setEditing(null); }}>
              Nova visita
            </AMGButton>
          )}
        </div>

        {showForm && !editing && (
          <div className="bg-surface-overlay border border-border-base rounded-md p-5">
            <div className="f-mono text-xs font-semibold text-ink-1 uppercase tracking-wider mb-4">Nova visita</div>
            <VisitForm
              onSave={r => createMut.mutate(r)}
              onCancel={() => setShowForm(false)}
            />
          </div>
        )}

        {isLoading ? (
          <div className="text-sm text-ink-3">Carregant…</div>
        ) : visits.length === 0 ? (
          <div className="text-sm text-ink-3 text-center py-10">Cap visita registrada.</div>
        ) : (
          <div className="space-y-3">
            {visits.map(v => (
              <div key={v.id} className="bg-surface-overlay border border-border-base rounded-md p-4">
                {editing?.id === v.id ? (
                  <VisitForm
                    initial={{
                      contactIdentifier: v.contactIdentifier,
                      contactName: v.contactName ?? '',
                      visitDate: v.visitDate,
                      treatmentType: v.treatmentType ?? '',
                      notes: v.notes ?? '',
                      nextVisitDue: v.nextVisitDue ?? '',
                    }}
                    onSave={r => updateMut.mutate({ id: v.id, r })}
                    onCancel={() => setEditing(null)}
                  />
                ) : (
                  <div className="flex items-start justify-between gap-4">
                    <div className="space-y-1 flex-1">
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-medium text-ink-0">{v.contactName ?? v.contactIdentifier}</span>
                        {v.contactName && (
                          <span className="f-mono text-xs text-ink-3">{v.contactIdentifier}</span>
                        )}
                      </div>
                      <div className="flex flex-wrap gap-x-4 text-xs text-ink-2">
                        <span>📅 {v.visitDate}</span>
                        {v.treatmentType && <span>🩺 {v.treatmentType}</span>}
                        {v.nextVisitDue   && <span className="text-amber-400">⏰ Revisió: {v.nextVisitDue}</span>}
                      </div>
                      {v.notes && <p className="text-xs text-ink-2 mt-1">{v.notes}</p>}
                    </div>
                    <div className="flex gap-1 shrink-0">
                      <button onClick={() => { setEditing(v); setShowForm(false); }}
                        className="p-1.5 rounded text-ink-3 hover:text-ink-0 hover:bg-surface-base transition">
                        <IconSet.Edit size={13} />
                      </button>
                      <button onClick={() => deleteMut.mutate(v.id)}
                        className="p-1.5 rounded text-ink-3 hover:text-red-400 hover:bg-[rgba(239,68,68,0.08)] transition">
                        <IconSet.Trash size={13} />
                      </button>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </PortalShell>
  );
}
