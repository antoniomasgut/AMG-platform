'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import {
  getCampaigns, createCampaign, runCampaign, deleteCampaign, cloneCampaign,
  scheduleCampaign, unscheduleCampaign,
  type Campaign, type ProspectSource,
} from '@/services/prospecting';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { IconSet } from '@/components/ui/icons';
import { useRouter, useParams } from 'next/navigation';

const PAGE_SIZE = 20;

const STATUS_TONE: Record<string, 'neutral' | 'info' | 'success' | 'danger' | 'warning' | 'accent'> = {
  DRAFT: 'neutral', SCHEDULED: 'accent', IN_PROGRESS: 'info', COMPLETED: 'success', FAILED: 'danger',
};
const STATUS_LABEL: Record<string, string> = {
  DRAFT: 'Pendent', SCHEDULED: 'Programada', IN_PROGRESS: 'En curs', COMPLETED: 'Completada', FAILED: 'Error',
};
const SOURCE_LABEL: Record<string, string> = {
  GOOGLE_MAPS: 'Google Maps', INSTAGRAM: 'Instagram', PAGINAS_AMARILLAS: 'Pàgines Grogues', MANUAL: 'Manual',
};

function fmtDate(d: string) {
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

type StatusFilter = '' | 'DRAFT' | 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';

export default function ProspectingPage() {
  const { user, isAdmin } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();
  const router = useRouter();
  const params = useParams();
  const locale = params.locale as string;

  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({
    name: '', sector: '', location: '',
    source: 'GOOGLE_MAPS' as ProspectSource,
    maxResults: '20', keywords: '',
  });
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('');
  const [page, setPage] = useState(0);

  const queryKey = ['campaigns', statusFilter, page];

  const { data, isLoading } = useQuery({
    queryKey,
    queryFn: () => getCampaigns({
      status: statusFilter || undefined,
      page,
      size: PAGE_SIZE,
    }),
    enabled: !!user && isAdmin,
  });

  const campaigns = data?.content ?? [];
  const totalPages = data?.totalPages ?? 1;
  const totalElements = data?.totalElements ?? 0;

  const invalidate = () => qc.invalidateQueries({ queryKey: ['campaigns'] });

  const { mutate: doCreate, isPending: creating } = useMutation({
    mutationFn: () => createCampaign({
      ...form,
      searchParams: JSON.stringify({
        maxResults: parseInt(form.maxResults) || 20,
        keywords: form.keywords ? form.keywords.split(',').map(k => k.trim()).filter(Boolean) : [],
      }),
    }),
    onSuccess: () => {
      toast('success', 'Campanya creada');
      setForm({ name: '', sector: '', location: '', source: 'GOOGLE_MAPS', maxResults: '20', keywords: '' });
      setShowForm(false);
      invalidate();
    },
    onError: () => toast('error', 'Error creant la campanya'),
  });

  const { mutate: doRun, isPending: running, variables: runningId } = useMutation({
    mutationFn: (id: string) => runCampaign(id),
    onSuccess: () => { toast('success', 'Campanya iniciada'); invalidate(); },
    onError: () => toast('error', 'Error executant la campanya'),
  });

  const { mutate: doDelete } = useMutation({
    mutationFn: (id: string) => deleteCampaign(id),
    onSuccess: () => { toast('success', 'Campanya eliminada'); invalidate(); },
    onError: () => toast('error', 'Error eliminant la campanya'),
  });

  const { mutate: doClone, isPending: cloning, variables: cloningId } = useMutation({
    mutationFn: (id: string) => cloneCampaign(id),
    onSuccess: () => { toast('success', 'Campanya clonada'); invalidate(); },
    onError: () => toast('error', 'Error clonant la campanya'),
  });

  const { mutate: doSchedule, isPending: scheduling, variables: schedulingId } = useMutation({
    mutationFn: ({ id, days }: { id: string; days: number }) =>
      scheduleCampaign(id, new Date(Date.now() + 86400000).toISOString(), days),
    onSuccess: () => { toast('success', 'Campanya programada'); invalidate(); },
    onError: () => toast('error', 'Error programant la campanya'),
  });

  const { mutate: doUnschedule } = useMutation({
    mutationFn: (id: string) => unscheduleCampaign(id),
    onSuccess: () => { toast('success', 'Programació cancel·lada'); invalidate(); },
    onError: () => toast('error', 'Error cancel·lant la programació'),
  });

  if (!user || !isAdmin) return null;

  return (
    <PortalShell breadcrumb="prospecting">
      <div className="p-4 sm:p-8 space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / prospecting /</span>
            <div className="f-display font-bold text-xl mt-1">Prospecció</div>
          </div>
          <AMGButton icon={IconSet.Plus} onClick={() => setShowForm(!showForm)}>
            Nova campanya
          </AMGButton>
        </div>

        {/* Formulari nova campanya */}
        {showForm && (
          <div className="amg-card card-clip p-6 space-y-4">
            <div className="f-mono text-label uppercase text-ink-2 tracking-widest mb-2">Nova campanya</div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="f-mono text-label text-ink-3 uppercase tracking-wider block mb-1">Nom</label>
                <input
                  type="text"
                  value={form.name}
                  onChange={(e) => setForm(f => ({ ...f, name: e.target.value }))}
                  className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="f-mono text-label text-ink-3 uppercase tracking-wider block mb-1">Sector principal</label>
                <input
                  type="text"
                  value={form.sector}
                  onChange={(e) => setForm(f => ({ ...f, sector: e.target.value }))}
                  placeholder="p. ex. pintors"
                  className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="f-mono text-label text-ink-3 uppercase tracking-wider block mb-1">Localitat</label>
                <input
                  type="text"
                  value={form.location}
                  onChange={(e) => setForm(f => ({ ...f, location: e.target.value }))}
                  placeholder="p. ex. Palma"
                  className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="f-mono text-label text-ink-3 uppercase tracking-wider block mb-1">Font</label>
                <select
                  value={form.source}
                  onChange={(e) => setForm(f => ({ ...f, source: e.target.value as ProspectSource }))}
                  className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
                >
                  <option value="GOOGLE_MAPS">Google Maps</option>
                  <option value="PAGINAS_AMARILLAS">Pàgines Grogues</option>
                  <option value="INSTAGRAM">Instagram</option>
                  <option value="MANUAL">Manual</option>
                </select>
              </div>
              <div>
                <label className="f-mono text-label text-ink-3 uppercase tracking-wider block mb-1">Màxim resultats</label>
                <input
                  type="number"
                  min={5} max={200}
                  value={form.maxResults}
                  onChange={(e) => setForm(f => ({ ...f, maxResults: e.target.value }))}
                  className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
                />
              </div>
            </div>
            <div>
              <label className="f-mono text-label text-ink-3 uppercase tracking-wider block mb-1">
                Termes alternatius <span className="normal-case text-ink-3">(separats per comes)</span>
              </label>
              <input
                type="text"
                value={form.keywords}
                onChange={(e) => setForm(f => ({ ...f, keywords: e.target.value }))}
                placeholder="p. ex. pintura, reformes, empresa de pintura"
                className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
              />
              <p className="f-mono text-[10px] text-ink-3 mt-1">
                Cada terme fa una cerca independent → més varietat de resultats. Els duplicats s&apos;eliminen automàticament.
              </p>
            </div>
            <div className="flex gap-3">
              <AMGButton loading={creating} icon={IconSet.Plus} onClick={() => doCreate()}>
                Crear
              </AMGButton>
              <AMGButton variant="ghost" onClick={() => setShowForm(false)}>
                Cancel·lar
              </AMGButton>
            </div>
          </div>
        )}

        {/* Filtres */}
        <div className="flex flex-wrap gap-2 items-center">
          {([['', 'Totes'], ['DRAFT', 'Pendents'], ['SCHEDULED', 'Programades'], ['IN_PROGRESS', 'En curs'], ['COMPLETED', 'Completades'], ['FAILED', 'Error']] as [StatusFilter, string][]).map(([val, label]) => (
            <button
              key={val}
              onClick={() => { setStatusFilter(val); setPage(0); }}
              className={`f-mono text-[10px] uppercase tracking-wider px-2.5 py-1 border transition-colors ${
                statusFilter === val
                  ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.12)] text-white'
                  : 'border-border-base text-ink-2 hover:text-ink-0 hover:border-ink-2'
              }`}
            >
              {label}
            </button>
          ))}
          {totalElements > 0 && (
            <span className="f-mono text-[10px] text-ink-3 ml-auto">
              {totalElements} campanya{totalElements !== 1 ? 'es' : ''}
            </span>
          )}
        </div>

        {/* Taula */}
        <div className="amg-card card-clip">
          {isLoading ? (
            <div className="flex justify-center py-12">
              <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
            </div>
          ) : campaigns.length === 0 ? (
            <div className="p-8 text-center">
              <IconSet.Search size={28} stroke="#64748b" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Cap campanya</div>
              <p className="f-mono text-label text-ink-2">
                {statusFilter ? 'Cap campanya amb aquest estat.' : 'Crea la primera campanya de prospecció.'}
              </p>
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[680px]">
                  <thead>
                    <tr className="border-b border-border-base">
                      {['Nom', 'Sector / Ciutat', 'Font', 'Estat', 'Prospects', 'Data', 'Accions'].map((h) => (
                        <th key={h} className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {campaigns.map((c: Campaign) => (
                      <tr key={c.id} className="border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,255,255,0.02)] transition-colors">
                        <td className="px-4 sm:px-5 py-3 f-display font-bold text-sm">{c.name}</td>
                        <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{c.sector} / {c.location}</td>
                        <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-2">{SOURCE_LABEL[c.source] ?? c.source}</td>
                        <td className="px-4 sm:px-5 py-3 space-y-1">
                          <AMGBadge tone={STATUS_TONE[c.status] ?? 'neutral'}>
                            {STATUS_LABEL[c.status] ?? c.status}
                          </AMGBadge>
                          {c.status === 'SCHEDULED' && c.repeatIntervalDays && (
                            <div className="f-mono text-[10px] text-ink-3">
                              Cada {c.repeatIntervalDays} d{c.repeatIntervalDays === 1 ? 'ia' : 'ies'}
                            </div>
                          )}
                        </td>
                        <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{c.totalFound ?? 0}</td>
                        <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{fmtDate(c.createdAt)}</td>
                        <td className="px-4 sm:px-5 py-3">
                          <div className="flex gap-1.5 flex-wrap">
                            <AMGButton
                              size="sm" icon={IconSet.Eye} variant="secondary"
                              onClick={() => router.push(`/${locale}/portal/prospecting/${c.id}`)}
                            >
                              Veure
                            </AMGButton>
                            {(c.status === 'DRAFT' || c.status === 'SCHEDULED') && (
                              <AMGButton
                                size="sm" icon={IconSet.Play}
                                loading={running && runningId === c.id}
                                onClick={() => doRun(c.id)}
                              >
                                Executar
                              </AMGButton>
                            )}
                            {c.status === 'DRAFT' && (
                              <ScheduleDropdown
                                loading={scheduling && (schedulingId as any)?.id === c.id}
                                onSchedule={(days) => doSchedule({ id: c.id, days })}
                              />
                            )}
                            {c.status === 'SCHEDULED' && (
                              <AMGButton size="sm" variant="ghost" icon={IconSet.X} onClick={() => doUnschedule(c.id)}>
                                Aturar
                              </AMGButton>
                            )}
                            <AMGButton
                              size="sm" variant="ghost" icon={IconSet.Copy}
                              loading={cloning && cloningId === c.id}
                              onClick={() => doClone(c.id)}
                            />
                            <AMGButton
                              size="sm" variant="ghost" icon={IconSet.Trash}
                              onClick={() => { if (confirm('Eliminar campanya?')) doDelete(c.id); }}
                            />
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Paginació */}
              {totalPages > 1 && (
                <div className="p-4 flex items-center justify-center gap-3 border-t border-border-base">
                  <AMGButton size="sm" variant="outline" disabled={page === 0} onClick={() => setPage(p => p - 1)}>
                    ← Anterior
                  </AMGButton>
                  <span className="f-mono text-label text-ink-2">{page + 1} / {totalPages}</span>
                  <AMGButton size="sm" variant="outline" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>
                    Següent →
                  </AMGButton>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </PortalShell>
  );
}

function ScheduleDropdown({ loading, onSchedule }: { loading: boolean; onSchedule: (days: number) => void }) {
  const [open, setOpen] = useState(false);
  const options = [
    { days: 1, label: 'Cada dia' },
    { days: 7, label: 'Cada setmana' },
    { days: 14, label: 'Cada 2 setmanes' },
    { days: 30, label: 'Cada mes' },
  ];
  return (
    <div className="relative">
      <AMGButton size="sm" variant="secondary" icon={IconSet.Clock} loading={loading} onClick={() => setOpen(o => !o)}>
        Programar
      </AMGButton>
      {open && (
        <>
          <div className="fixed inset-0 z-10" onClick={() => setOpen(false)} />
          <div className="absolute left-0 top-full mt-1 z-20 bg-bg-0 border border-border-base shadow-lg min-w-[140px]">
            {options.map(({ days, label }) => (
              <button
                key={days}
                onClick={() => { onSchedule(days); setOpen(false); }}
                className="block w-full text-left px-3 py-2 f-mono text-xs text-ink-1 hover:bg-bg-1 hover:text-ink-0 transition-colors"
              >
                {label}
              </button>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
