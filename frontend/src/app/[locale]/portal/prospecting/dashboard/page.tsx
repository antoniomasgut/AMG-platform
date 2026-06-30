'use client';

import { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { useToast } from '@/lib/toast-context';
import {
  getGlobalDashboard, getCampaigns, analyzeAllWebCampaign,
  type Campaign,
} from '@/services/prospecting';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGBadge } from '@/components/ui/badge';
import { AMGButton } from '@/components/ui/button';
import { IconSet } from '@/components/ui/icons';
import Link from 'next/link';
import { useParams } from 'next/navigation';

const TIER_LABEL: Record<string, string> = {
  PRIORITY: 'Prioritari', DEMO: 'Demo', REVIEW: 'Revisar', DISCARD: 'Descartar',
};
const TIER_COLOR: Record<string, string> = {
  PRIORITY: 'text-[#FF6B00]', DEMO: 'text-green-400', REVIEW: 'text-yellow-400', DISCARD: 'text-ink-3',
};
const TIER_BG: Record<string, string> = {
  PRIORITY: 'bg-[rgba(255,107,0,0.12)]', DEMO: 'bg-[rgba(34,197,94,0.1)]',
  REVIEW: 'bg-[rgba(234,179,8,0.1)]', DISCARD: 'bg-[rgba(255,255,255,0.04)]',
};

function StatCard({ label, value, sub, accent }: { label: string; value: string | number; sub?: string; accent?: boolean }) {
  return (
    <div className={`amg-card p-5 flex flex-col gap-1 ${accent ? 'border-[rgba(255,107,0,0.3)]' : ''}`}>
      <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3">{label}</div>
      <div className={`f-display text-3xl font-bold ${accent ? 'text-[#FF6B00]' : 'text-ink-0'}`}>{value}</div>
      {sub && <div className="f-mono text-[10px] text-ink-3">{sub}</div>}
    </div>
  );
}

export default function ProspectingDashboardPage() {
  const { locale } = useParams<{ locale: string }>();
  const { toast } = useToast();
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [analyzingCampaign, setAnalyzingCampaign] = useState<string | null>(null);

  const { data: dash, isLoading } = useQuery({
    queryKey: ['prospecting-global-dashboard'],
    queryFn: getGlobalDashboard,
    refetchInterval: 60_000,
  });

  const { data: campaignsPage } = useQuery({
    queryKey: ['campaigns-list-dashboard'],
    queryFn: () => getCampaigns({ size: 50 }),
  });
  const campaigns: Campaign[] = campaignsPage?.content ?? [];

  const analyzeAll = useMutation({
    mutationFn: (id: string) => analyzeAllWebCampaign(id),
    onSuccess: (data) => {
      toast('success', `Analitzats ${(data as { analyzed: number }).analyzed} prospects`);
      setAnalyzingCampaign(null);
    },
    onError: () => {
      toast('error', 'Error en l\'anàlisi');
      setAnalyzingCampaign(null);
    },
  });

  const byTier = (dash?.byTier as Record<string, number>) ?? {};
  const byStatus = (dash?.byStatus as Record<string, number>) ?? {};
  const topCampaigns = (dash?.topCampaigns as Array<{
    id: string; name: string; total: number; priority: number; demo: number;
  }>) ?? [];

  const totalProspects = (dash?.totalProspects as number) ?? 0;
  const avgScore = (dash?.avgScore as number) ?? 0;
  const totalCampaigns = (dash?.totalCampaigns as number) ?? 0;

  return (
    <PortalShell breadcrumb="prospecting / dashboard" backHref={`/${locale}/portal/prospecting`}>
      <div className="max-w-6xl mx-auto space-y-8">

        {/* Capçalera */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="f-display text-2xl font-bold text-ink-0">Dashboard Prospecció</h1>
            <p className="f-mono text-xs text-ink-3 mt-1">Resum global · scoring 0–100 · tiers PRIORITY → DISCARD</p>
          </div>
          <Link href={`/${locale}/portal/prospecting`}>
            <AMGButton size="sm" variant="secondary">
              ← Campanyes
            </AMGButton>
          </Link>
        </div>

        {isLoading ? (
          <div className="flex items-center justify-center h-48">
            <span className="w-6 h-6 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
          </div>
        ) : (
          <>
            {/* KPIs globals */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <StatCard label="Campanyes" value={totalCampaigns} />
              <StatCard label="Prospects totals" value={totalProspects} />
              <StatCard label="Score mitjà" value={avgScore > 0 ? avgScore.toFixed(0) : '—'} sub="de 100" accent={avgScore >= 61} />
              <StatCard label="Prioritaris" value={byTier['PRIORITY'] ?? 0} sub="score ≥ 81" accent={(byTier['PRIORITY'] ?? 0) > 0} />
            </div>

            {/* Distribució per tier */}
            <div className="amg-card p-6">
              <h2 className="f-mono text-[10px] uppercase tracking-widest text-ink-3 mb-4">Distribució per tier</h2>
              {['PRIORITY', 'DEMO', 'REVIEW', 'DISCARD'].map((tier) => {
                const count = byTier[tier] ?? 0;
                const pct = totalProspects > 0 ? Math.round((count / totalProspects) * 100) : 0;
                return (
                  <div key={tier} className="flex items-center gap-3 mb-3">
                    <div className={`w-20 shrink-0 f-mono text-[10px] font-bold ${TIER_COLOR[tier]}`}>{TIER_LABEL[tier]}</div>
                    <div className="flex-1 bg-bg-1 rounded-full h-2 overflow-hidden">
                      <div
                        className={`h-full rounded-full transition-all ${
                          tier === 'PRIORITY' ? 'bg-[#FF6B00]' :
                          tier === 'DEMO'     ? 'bg-green-400' :
                          tier === 'REVIEW'   ? 'bg-yellow-400' : 'bg-ink-3'
                        }`}
                        style={{ width: `${pct}%` }}
                      />
                    </div>
                    <div className="f-mono text-xs text-ink-1 w-10 text-right">{count}</div>
                    <div className="f-mono text-[10px] text-ink-3 w-8 text-right">{pct}%</div>
                  </div>
                );
              })}
            </div>

            {/* Distribució per estat */}
            <div className="amg-card p-6">
              <h2 className="f-mono text-[10px] uppercase tracking-widest text-ink-3 mb-4">Distribució per estat</h2>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                {Object.entries(byStatus).map(([status, count]) => (
                  <div key={status} className="flex items-center justify-between bg-bg-1 rounded px-3 py-2">
                    <span className="f-mono text-[10px] text-ink-2">{status}</span>
                    <span className="f-mono text-sm font-bold text-ink-0">{count}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Top campanyes */}
            {topCampaigns.length > 0 && (
              <div className="amg-card p-6">
                <h2 className="f-mono text-[10px] uppercase tracking-widest text-ink-3 mb-4">Top campanyes per prospects prioritaris</h2>
                <div className="space-y-2">
                  {topCampaigns.map((c) => (
                    <div key={c.id} className="flex items-center gap-3 bg-bg-1 rounded px-4 py-3">
                      <div className="flex-1 min-w-0">
                        <Link href={`/${locale}/portal/prospecting/${c.id}`} className="f-mono text-sm font-bold text-ink-0 hover:text-[#FF6B00] transition truncate block">
                          {c.name}
                        </Link>
                      </div>
                      <div className="flex items-center gap-3 shrink-0">
                        <span className="f-mono text-[10px] text-ink-3">{c.total} prospects</span>
                        {c.priority > 0 && (
                          <span className={`f-mono text-[10px] font-bold px-2 py-0.5 rounded ${TIER_BG['PRIORITY']} ${TIER_COLOR['PRIORITY']}`}>
                            {c.priority} prioritaris
                          </span>
                        )}
                        {c.demo > 0 && (
                          <span className={`f-mono text-[10px] font-bold px-2 py-0.5 rounded ${TIER_BG['DEMO']} ${TIER_COLOR['DEMO']}`}>
                            {c.demo} demo
                          </span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Accions per campanya — anàlisi web massiva */}
            <div className="amg-card p-6">
              <h2 className="f-mono text-[10px] uppercase tracking-widest text-ink-3 mb-1">Anàlisi web massiva</h2>
              <p className="f-mono text-[10px] text-ink-3 mb-4">Analitza la web de tots els prospects d'una campanya (SSL, CMS, mètriques, xarxes socials) i recalcula el score.</p>
              <div className="space-y-2">
                {campaigns.filter(c => c.status === 'COMPLETED').map((c) => (
                  <div key={c.id} className="flex items-center justify-between bg-bg-1 rounded px-4 py-3">
                    <div>
                      <div className="f-mono text-sm text-ink-0">{c.name}</div>
                      <div className="f-mono text-[10px] text-ink-3">{c.totalFound} prospects · {c.sector}</div>
                    </div>
                    <AMGButton
                      size="sm"
                      variant="secondary"
                      disabled={analyzingCampaign === c.id}
                      onClick={() => {
                        setAnalyzingCampaign(c.id);
                        analyzeAll.mutate(c.id);
                      }}
                    >
                      {analyzingCampaign === c.id ? (
                        <><span className="w-3 h-3 border border-current border-t-transparent rounded-full animate-spin" /> Analitzant...</>
                      ) : (
                        <><IconSet.Search size={12} /> Analitzar webs</>
                      )}
                    </AMGButton>
                  </div>
                ))}
                {campaigns.filter(c => c.status === 'COMPLETED').length === 0 && (
                  <p className="f-mono text-[10px] text-ink-3">Cap campanya completada. Executa una campanya primer.</p>
                )}
              </div>
            </div>
          </>
        )}
      </div>
    </PortalShell>
  );
}
