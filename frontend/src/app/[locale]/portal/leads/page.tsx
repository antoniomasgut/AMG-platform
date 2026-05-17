'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import { getLeads, getLeadStats, changeStage, type Lead } from '@/services/leads';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGStat } from '@/components/ui/stat';
import { I } from '@/components/ui/icons';
import { useRouter } from 'next/navigation';
import { useParams } from 'next/navigation';

const STAGES = ['NEW', 'CONTACTED', 'QUALIFIED', 'PROPOSAL', 'NEGOTIATION', 'WON', 'LOST'] as const;

const STAGE_LABEL: Record<string, string> = {
  NEW: 'Nou', CONTACTED: 'Contactat', QUALIFIED: 'Qualificat',
  PROPOSAL: 'Proposta', NEGOTIATION: 'Negociació', WON: 'Guanyat', LOST: 'Perdut',
};

const STAGE_TONE: Record<string, 'neutral' | 'info' | 'accent' | 'warning' | 'success' | 'danger'> = {
  NEW: 'neutral', CONTACTED: 'info', QUALIFIED: 'accent',
  PROPOSAL: 'warning', NEGOTIATION: 'warning', WON: 'success', LOST: 'danger',
};

const SOURCE_LABEL: Record<string, string> = {
  WEBSITE: 'Web', REFERRAL: 'Referit', COLD_CALL: 'Cold Call',
  SOCIAL_MEDIA: 'RRSS', OTHER: 'Altre',
};

function fmt(n: number) {
  return `${(n * 100).toFixed(1)}%`;
}

export default function LeadsPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();
  const router = useRouter();
  const params = useParams();
  const locale = params.locale as string;

  const { data: leads = [], isLoading } = useQuery({
    queryKey: ['leads'],
    queryFn: getLeads,
    enabled: !!user,
  });

  const { data: stats } = useQuery({
    queryKey: ['leads-stats'],
    queryFn: getLeadStats,
    enabled: !!user,
  });

  const { mutate: doChangeStage } = useMutation({
    mutationFn: ({ id, stage }: { id: string; stage: string }) => changeStage(id, stage),
    onSuccess: () => {
      toast('success', 'Etapa actualitzada');
      qc.invalidateQueries({ queryKey: ['leads'] });
      qc.invalidateQueries({ queryKey: ['leads-stats'] });
    },
    onError: () => toast('error', 'Error actualitzant etapa'),
  });

  const leadsByStage = STAGES.reduce<Record<string, Lead[]>>((acc, stage) => {
    acc[stage] = leads.filter((l: Lead) => l.stage === stage);
    return acc;
  }, {} as Record<string, Lead[]>);

  return (
    <PortalShell breadcrumb="leads">
      <div className="p-4 sm:p-8 space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / leads /</span>
            <div className="f-display font-bold text-xl mt-1">CRM de Leads</div>
          </div>
          <AMGButton icon={I.Plus} onClick={() => router.push(`/${locale}/portal/leads/new`)}>
            Nou Lead
          </AMGButton>
        </div>

        {stats && (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
            <AMGStat label="Total leads" value={String(stats.total)} icon={I.Users} tone="accent" />
            <AMGStat
              label="Guanyats"
              value={String(stats.byStage?.WON ?? 0)}
              icon={I.Check}
              tone="success"
            />
            <AMGStat
              label="Perduts"
              value={String(stats.byStage?.LOST ?? 0)}
              icon={I.X}
              tone="danger"
            />
            <AMGStat
              label="Conversió"
              value={fmt(stats.conversionRate)}
              icon={I.Trending}
              tone="info"
            />
          </div>
        )}

        {isLoading ? (
          <div className="flex justify-center py-12">
            <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          </div>
        ) : (
          <div className="overflow-x-auto pb-4">
            <div className="flex gap-3 min-w-max">
              {STAGES.map((stage) => (
                <div key={stage} className="w-[220px] shrink-0">
                  <div className="amg-card card-clip p-3 mb-2 flex items-center justify-between">
                    <AMGBadge tone={STAGE_TONE[stage]}>{STAGE_LABEL[stage]}</AMGBadge>
                    <span className="f-mono text-label text-ink-2">{leadsByStage[stage].length}</span>
                  </div>
                  <div className="space-y-2">
                    {leadsByStage[stage].map((lead) => (
                      <div
                        key={lead.id}
                        className="amg-card card-clip p-3 cursor-pointer hover:border-accent/40 transition-colors"
                        onClick={() => router.push(`/${locale}/portal/leads/${lead.id}`)}
                      >
                        <div className="f-display font-bold text-sm text-ink-0 truncate">{lead.companyName}</div>
                        <div className="f-mono text-label text-ink-2 mt-1 truncate">{lead.contactName}</div>
                        <div className="f-mono text-label text-ink-3 truncate">{lead.contactEmail}</div>
                        <div className="mt-2">
                          <AMGBadge tone="neutral">{SOURCE_LABEL[lead.source] ?? lead.source}</AMGBadge>
                        </div>
                        {stage !== 'WON' && stage !== 'LOST' && (
                          <div className="mt-2 flex gap-1 flex-wrap">
                            {stage !== STAGES[STAGES.indexOf(stage) + 1] && STAGES.indexOf(stage) < STAGES.length - 3 && (
                              <button
                                className="f-mono text-[9px] uppercase text-accent-light hover:underline"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  doChangeStage({ id: lead.id, stage: STAGES[STAGES.indexOf(stage) + 1] });
                                }}
                              >
                                Avançar →
                              </button>
                            )}
                          </div>
                        )}
                      </div>
                    ))}
                    {leadsByStage[stage].length === 0 && (
                      <div className="amg-card card-clip p-3 text-center">
                        <span className="f-mono text-label text-ink-3">Cap lead</span>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </PortalShell>
  );
}
