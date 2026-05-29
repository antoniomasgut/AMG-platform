'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import { getCampaigns, createCampaign, runCampaign, deleteCampaign, cloneCampaign, type Campaign, type ProspectSource } from '@/services/prospecting';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { I } from '@/components/ui/icons';
import { useRouter, useParams } from 'next/navigation';

const STATUS_TONE: Record<string, 'neutral' | 'info' | 'success' | 'danger' | 'warning' | 'accent'> = {
  DRAFT: 'neutral',
  IN_PROGRESS: 'info',
  COMPLETED: 'success',
  FAILED: 'danger',
};

const STATUS_LABEL: Record<string, string> = {
  DRAFT: 'Pendent', IN_PROGRESS: 'En curs', COMPLETED: 'Completada', FAILED: 'Error',
};

function fmtDate(d: string) {
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

export default function ProspectingPage() {
  const { user, isAdmin } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();
  const router = useRouter();
  const params = useParams();
  const locale = params.locale as string;

  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({
    name: '', sector: '', location: '', source: 'GOOGLE_MAPS' as ProspectSource,
    maxResults: '20', keywords: '',
  });

  const { data: campaigns = [], isLoading } = useQuery({
    queryKey: ['campaigns'],
    queryFn: getCampaigns,
    enabled: !!user && isAdmin,
  });

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
      qc.invalidateQueries({ queryKey: ['campaigns'] });
    },
    onError: () => toast('error', 'Error creant la campanya'),
  });

  const { mutate: doRun, isPending: running, variables: runningId } = useMutation({
    mutationFn: (id: string) => runCampaign(id),
    onSuccess: () => {
      toast('success', 'Campanya iniciada');
      qc.invalidateQueries({ queryKey: ['campaigns'] });
    },
    onError: () => toast('error', 'Error executant la campanya'),
  });

  const { mutate: doDelete } = useMutation({
    mutationFn: (id: string) => deleteCampaign(id),
    onSuccess: () => {
      toast('success', 'Campanya eliminada');
      qc.invalidateQueries({ queryKey: ['campaigns'] });
    },
    onError: () => toast('error', 'Error eliminant la campanya'),
  });

  const { mutate: doClone, isPending: cloning, variables: cloningId } = useMutation({
    mutationFn: (id: string) => cloneCampaign(id),
    onSuccess: () => {
      toast('success', 'Campanya clonada');
      qc.invalidateQueries({ queryKey: ['campaigns'] });
    },
    onError: () => toast('error', 'Error clonant la campanya'),
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
          <AMGButton icon={I.Plus} onClick={() => setShowForm(!showForm)}>
            Nova Campanya
          </AMGButton>
        </div>

        {showForm && (
          <div className="amg-card card-clip p-6 space-y-4">
            <div className="f-mono text-label uppercase text-ink-2 tracking-widest mb-2">Nova Campanya</div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {([['Nom', 'name'], ['Sector principal', 'sector'], ['Localitat', 'location']] as const).map(([label, key]) => (
                <div key={key}>
                  <label className="f-mono text-label text-ink-3 uppercase tracking-wider block mb-1">{label}</label>
                  <input
                    type="text"
                    value={form[key]}
                    onChange={(e) => setForm((f) => ({ ...f, [key]: e.target.value }))}
                    className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
                    placeholder={key === 'location' ? 'p. ex. Palma' : key === 'sector' ? 'p. ex. pintors' : ''}
                  />
                </div>
              ))}
              <div>
                <label className="f-mono text-label text-ink-3 uppercase tracking-wider block mb-1">Màxim resultats</label>
                <input
                  type="number"
                  min={5} max={200}
                  value={form.maxResults}
                  onChange={(e) => setForm((f) => ({ ...f, maxResults: e.target.value }))}
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
                onChange={(e) => setForm((f) => ({ ...f, keywords: e.target.value }))}
                className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
                placeholder="p. ex. pintura, reformes, empresa de pintura"
              />
              <p className="f-mono text-[10px] text-ink-3 mt-1">
                Cada terme fa una cerca independent → més varietat de resultats. Els duplicats s'eliminen automàticament.
              </p>
            </div>
            <div className="flex gap-3">
              <AMGButton loading={creating} icon={I.Plus} onClick={() => doCreate()}>
                Crear
              </AMGButton>
              <AMGButton variant="ghost" onClick={() => setShowForm(false)}>
                Cancel·lar
              </AMGButton>
            </div>
          </div>
        )}

        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base">
            <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Campanyes</div>
          </div>

          {isLoading ? (
            <div className="flex justify-center py-12">
              <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
            </div>
          ) : (campaigns as Campaign[]).length === 0 ? (
            <div className="p-8 text-center">
              <I.Search size={28} stroke="#64748b" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Cap campanya</div>
              <p className="f-mono text-label text-ink-2">Crea la primera campanya de prospecció</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[640px]">
                <thead>
                  <tr className="border-b border-border-base">
                    {['Nom', 'Sector / Ciutat', 'Estat', 'Prospects', 'Data', 'Accions'].map((h) => (
                      <th key={h} className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {(campaigns as Campaign[]).map((c) => (
                    <tr key={c.id} className="border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,255,255,0.02)] transition-colors">
                      <td className="px-4 sm:px-5 py-3 f-display font-bold text-sm">{c.name}</td>
                      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">
                        {c.sector} / {c.location}
                      </td>
                      <td className="px-4 sm:px-5 py-3">
                        <AMGBadge tone={STATUS_TONE[c.status] ?? 'neutral'}>
                          {STATUS_LABEL[c.status] ?? c.status}
                        </AMGBadge>
                      </td>
                      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">
                        {c.totalFound ?? 0}
                      </td>
                      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{fmtDate(c.createdAt)}</td>
                      <td className="px-4 sm:px-5 py-3">
                        <div className="flex gap-2">
                          <AMGButton
                            size="sm"
                            icon={I.Eye}
                            variant="secondary"
                            onClick={() => router.push(`/${locale}/portal/prospecting/${c.id}`)}
                          >
                            Veure
                          </AMGButton>
                          {c.status === 'DRAFT' && (
                            <AMGButton
                              size="sm"
                              icon={I.Play}
                              loading={running && runningId === c.id}
                              onClick={() => doRun(c.id)}
                            >
                              Executar
                            </AMGButton>
                          )}
                          <AMGButton
                            size="sm"
                            variant="ghost"
                            icon={I.Copy}
                            loading={cloning && cloningId === c.id}
                            onClick={() => doClone(c.id)}
                          />
                          <AMGButton
                            size="sm"
                            variant="ghost"
                            icon={I.Trash}
                            onClick={() => { if (confirm('Eliminar campanya?')) doDelete(c.id); }}
                          />
                        </div>
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
