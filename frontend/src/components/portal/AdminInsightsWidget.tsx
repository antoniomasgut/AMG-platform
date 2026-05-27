'use client';

import { useQuery } from '@tanstack/react-query';
import { getAdminInsights, type PlatformRecommendation } from '@/services/platform';

const CATEGORY_CONFIG = {
  UPGRADE_OFFER: {
    label: 'Oportunitats de venda',
    color: 'text-[#39d353]',
    bar: 'bg-[#39d353]',
    rowBg: 'bg-[rgba(57,211,83,0.05)]',
    rowBorder: 'border-[rgba(57,211,83,0.18)]',
    icon: '💰',
  },
  COST_OPTIMIZATION: {
    label: 'Optimització de cost',
    color: 'text-[#f0b429]',
    bar: 'bg-[#f0b429]',
    rowBg: 'bg-[rgba(240,180,41,0.05)]',
    rowBorder: 'border-[rgba(240,180,41,0.15)]',
    icon: '⚡',
  },
} as const;

const IMPACT_DOT: Record<string, string> = {
  HIGH: 'bg-[#f85149]',
  MEDIUM: 'bg-[#f0b429]',
  LOW: 'bg-ink-3',
};

export function AdminInsightsWidget({ tenantId }: { tenantId: string }) {
  const { data: insights, isLoading } = useQuery({
    queryKey: ['admin-insights', tenantId],
    queryFn: () => getAdminInsights(tenantId),
    staleTime: 5 * 60 * 1000,
  });

  if (isLoading) return (
    <div className="amg-card card-clip p-4 animate-pulse">
      <div className="h-3 bg-bg-2 rounded w-48 mb-3" />
      <div className="h-10 bg-bg-2 rounded mb-2" />
      <div className="h-10 bg-bg-2 rounded" />
    </div>
  );

  if (!insights || insights.length === 0) return null;

  const upgrades = insights.filter(r => r.category === 'UPGRADE_OFFER');
  const costOpts = insights.filter(r => r.category === 'COST_OPTIMIZATION');

  return (
    <div className="amg-card card-clip">
      <div className="px-5 pt-4 pb-3 border-b border-border-subtle">
        <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3">
          Anàlisi de client · {insights.length} indicador{insights.length > 1 ? 's' : ''}
        </div>
      </div>

      {upgrades.length > 0 && (
        <InsightSection
          title="Oportunitats d'upgrade"
          subtitle="Serveis que podem oferir a aquest client"
          items={upgrades}
          cat="UPGRADE_OFFER"
        />
      )}

      {costOpts.length > 0 && (
        <InsightSection
          title="Optimització de rendiment"
          subtitle="Millores internes per reduir cost i millorar qualitat"
          items={costOpts}
          cat="COST_OPTIMIZATION"
          borderTop={upgrades.length > 0}
        />
      )}
    </div>
  );
}

function InsightSection({
  title, subtitle, items, cat, borderTop = false,
}: {
  title: string;
  subtitle: string;
  items: PlatformRecommendation[];
  cat: 'UPGRADE_OFFER' | 'COST_OPTIMIZATION';
  borderTop?: boolean;
}) {
  const cfg = CATEGORY_CONFIG[cat];
  return (
    <div className={`px-5 py-3 space-y-2 ${borderTop ? 'border-t border-border-subtle' : ''}`}>
      <div className="mb-2">
        <span className={`f-mono text-[9px] uppercase tracking-widest ${cfg.color}`}>
          {cfg.icon} {title}
        </span>
        <div className="f-mono text-[9px] text-ink-3 mt-0.5">{subtitle}</div>
      </div>
      {items.map((item, i) => (
        <div key={i} className={`flex items-start gap-3 p-3 rounded border ${cfg.rowBg} ${cfg.rowBorder}`}>
          <span className={`w-1.5 h-1.5 rounded-full flex-shrink-0 mt-1.5 ${IMPACT_DOT[item.impact] ?? 'bg-ink-3'}`} />
          <div className="min-w-0">
            <div className="f-mono text-[10px] font-semibold text-ink-1 mb-0.5">{item.title}</div>
            <p className="f-mono text-[10px] text-ink-2 leading-relaxed">{item.description}</p>
          </div>
        </div>
      ))}
    </div>
  );
}
