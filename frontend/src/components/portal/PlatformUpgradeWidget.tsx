'use client';

import { useQuery } from '@tanstack/react-query';
import { getClientRecommendations, type PlatformRecommendation } from '@/services/platform';
import Link from 'next/link';
import { useLocale } from 'next-intl';

const SERVICE_ICON: Record<string, string> = {
  CHANNEL: '📡',
  AGENT_MODE: '🤖',
  KNOWLEDGE_BASE: '📚',
  LANDING: '🌐',
};

const SERVICE_LINK: Record<string, string> = {
  CHANNEL: 'agents',
  AGENT_MODE: 'agents',
  KNOWLEDGE_BASE: 'agents',
  LANDING: 'factory',
};

export function PlatformUpgradeWidget({ tenantId }: { tenantId: string }) {
  const locale = useLocale();

  const { data: recs, isLoading } = useQuery({
    queryKey: ['platform-client-recs', tenantId],
    queryFn: () => getClientRecommendations(tenantId),
    staleTime: 5 * 60 * 1000,
  });

  if (isLoading) return (
    <div className="amg-card card-clip p-4 animate-pulse">
      <div className="h-3 bg-bg-2 rounded w-40 mb-3" />
      <div className="h-10 bg-bg-2 rounded" />
    </div>
  );

  if (!recs || recs.length === 0) return null;

  const highCount = recs.filter(r => r.impact === 'HIGH').length;
  const barColor = highCount > 0 ? 'bg-[#f85149]' : 'bg-[#f0b429]';
  const labelColor = highCount > 0 ? 'text-[#f85149]' : 'text-[#f0b429]';

  return (
    <div className="amg-card card-clip relative overflow-hidden">
      <div className={`absolute top-0 left-0 right-0 h-[2px] ${barColor}`} />
      <div className="px-5 pt-4 pb-3 border-b border-border-subtle">
        <div className={`f-mono text-[9px] uppercase tracking-widest ${labelColor}`}>
          {recs.length} aspecte{recs.length > 1 ? 's' : ''} a millorar al teu servei
        </div>
      </div>
      <div className="px-5 py-3 space-y-2">
        {recs.map((rec, i) => (
          <ClientRecRow key={i} rec={rec} locale={locale} />
        ))}
      </div>
    </div>
  );
}

function ClientRecRow({ rec, locale }: { rec: PlatformRecommendation; locale: string }) {
  const icon = SERVICE_ICON[rec.service] ?? '⚠️';
  const link = SERVICE_LINK[rec.service];
  const isHigh = rec.impact === 'HIGH';

  return (
    <div className={`flex items-start gap-3 p-3 rounded border ${
      isHigh
        ? 'bg-[rgba(248,81,73,0.05)] border-[rgba(248,81,73,0.2)]'
        : 'bg-[rgba(240,180,41,0.05)] border-[rgba(240,180,41,0.15)]'
    }`}>
      <span className="text-base shrink-0 leading-none mt-0.5">{icon}</span>
      <div className="flex-1 min-w-0">
        <div className={`f-mono text-[10px] font-semibold mb-0.5 ${isHigh ? 'text-[#f85149]' : 'text-[#f0b429]'}`}>
          {rec.title}
        </div>
        <p className="f-mono text-[10px] text-ink-2 leading-relaxed">{rec.description}</p>
      </div>
      {link && (
        <Link
          href={`/${locale}/portal/${link}`}
          className="shrink-0 f-mono text-[9px] uppercase tracking-widest text-accent-light hover:text-accent transition mt-0.5"
        >
          Revisar →
        </Link>
      )}
    </div>
  );
}
