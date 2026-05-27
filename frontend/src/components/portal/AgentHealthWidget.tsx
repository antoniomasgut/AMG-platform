'use client';

import { useQuery } from '@tanstack/react-query';
import { getAgentHealth, type AgentHealthResponse } from '@/services/agents-conversational';
import Link from 'next/link';
import { useLocale } from 'next-intl';

const STATUS_COLOR = {
  GREEN: { bar: 'bg-[#39d353]', text: 'text-[#39d353]', label: 'Bon funcionament', border: 'border-[#39d353]/20' },
  YELLOW: { bar: 'bg-[#f0b429]', text: 'text-[#f0b429]', label: 'Millores recomanades', border: 'border-[#f0b429]/20' },
  RED: { bar: 'bg-[#f85149]', text: 'text-[#f85149]', label: 'Atenció necessària', border: 'border-[#f85149]/20' },
};

const CHECK_ICON = {
  OK: (
    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" className="text-[#39d353] shrink-0">
      <path d="M20 6L9 17l-5-5" />
    </svg>
  ),
  WARNING: (
    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" className="text-[#f0b429] shrink-0">
      <path d="M12 9v4M12 17h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
    </svg>
  ),
  ERROR: (
    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" className="text-[#f85149] shrink-0">
      <circle cx="12" cy="12" r="10" /><path d="M15 9l-6 6M9 9l6 6" />
    </svg>
  ),
};

interface AgentHealthWidgetProps {
  tenantId: string;
  compact?: boolean;
}

export function AgentHealthWidget({ tenantId, compact = false }: AgentHealthWidgetProps) {
  const locale = useLocale();

  const { data: health, isLoading } = useQuery({
    queryKey: ['agent-health', tenantId],
    queryFn: () => getAgentHealth(tenantId),
    staleTime: 2 * 60 * 1000,
  });

  if (isLoading) {
    return (
      <div className="amg-card card-clip p-4 animate-pulse">
        <div className="h-3 bg-bg-2 rounded w-32 mb-2" />
        <div className="h-2 bg-bg-2 rounded w-full mb-1" />
        <div className="h-2 bg-bg-2 rounded w-3/4" />
      </div>
    );
  }

  if (!health) return null;

  const colors = STATUS_COLOR[health.status];

  if (compact) {
    return (
      <div className={`amg-card card-clip p-3 relative overflow-hidden border ${colors.border}`}>
        <div className={`absolute top-0 left-0 right-0 h-[2px] ${colors.bar}`} />
        <div className="flex items-center justify-between gap-3">
          <div>
            <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3">Salut de l'agent</div>
            <div className={`f-display font-black text-xl mt-0.5 ${colors.text}`}>{health.score}%</div>
            <div className={`f-mono text-[10px] mt-0.5 ${colors.text}`}>{colors.label}</div>
          </div>
          <div className="flex flex-col gap-1">
            {health.checks.slice(0, 3).map((c) => (
              <div key={c.key} className="flex items-center gap-1">
                {CHECK_ICON[c.status]}
                <span className="f-mono text-[9px] text-ink-2">{c.label}</span>
              </div>
            ))}
          </div>
        </div>
        {(health.suggestions.length > 0 || (health.recommendations?.length ?? 0) > 0) && (
          <Link
            href={`/${locale}/portal/agents`}
            className={`mt-2 block f-mono text-[9px] uppercase tracking-widest ${colors.text} hover:opacity-80 transition`}
          >
            {health.suggestions.length + (health.recommendations?.length ?? 0)} acció{health.suggestions.length + (health.recommendations?.length ?? 0) > 1 ? 'ns' : ''} disponible{health.suggestions.length + (health.recommendations?.length ?? 0) > 1 ? 's' : ''} →
          </Link>
        )}
      </div>
    );
  }

  return (
    <div className={`amg-card card-clip relative overflow-hidden border ${colors.border}`}>
      <div className={`absolute top-0 left-0 right-0 h-[2px] ${colors.bar}`} />

      {/* Header */}
      <div className="flex items-center justify-between px-5 pt-4 pb-3 border-b border-border-subtle">
        <div className="flex items-center gap-3">
          <div>
            <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3">Salut de l'agent IA</div>
            <div className="flex items-center gap-2 mt-0.5">
              <span className={`f-display font-black text-2xl ${colors.text}`}>{health.score}%</span>
              <span className={`f-mono text-[10px] ${colors.text}`}>{colors.label}</span>
            </div>
          </div>
        </div>
        {/* Score bar */}
        <div className="w-24">
          <div className="h-1.5 bg-bg-2 rounded-full overflow-hidden">
            <div
              className={`h-full ${colors.bar} transition-all duration-500`}
              style={{ width: `${health.score}%` }}
            />
          </div>
        </div>
      </div>

      {/* Checks */}
      <div className="px-5 py-3 space-y-2">
        {health.checks.map((check) => (
          <div key={check.key} className="flex items-start gap-2">
            <div className="mt-0.5">{CHECK_ICON[check.status]}</div>
            <div className="min-w-0">
              <span className="f-mono text-[10px] uppercase tracking-widest text-ink-2">{check.label}</span>
              <span className="f-mono text-[10px] text-ink-3 ml-2">— {check.detail}</span>
            </div>
          </div>
        ))}
      </div>

      {/* Suggestions — errors / config pending */}
      {health.suggestions.length > 0 && (
        <div className="px-5 pb-4 border-t border-border-subtle pt-3 space-y-1.5">
          <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3 mb-2">Configuració pendent</div>
          {health.suggestions.map((s, i) => (
            <div key={i} className="flex items-start gap-2">
              <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-accent shrink-0 mt-0.5">
                <path d="M5 12h14M13 5l7 7-7 7" />
              </svg>
              <span className="f-mono text-[10px] text-ink-2 leading-relaxed">{s}</span>
            </div>
          ))}
          <Link
            href={`/${locale}/portal/agents`}
            className="mt-2 inline-flex items-center gap-1 f-mono text-[10px] uppercase tracking-widest text-accent-light hover:text-accent transition"
          >
            Configurar l'agent →
          </Link>
        </div>
      )}

      {/* Recommendations — upgrade / performance opportunities */}
      {health.recommendations && health.recommendations.length > 0 && (
        <div className="px-5 pb-4 border-t border-border-subtle pt-3 space-y-2">
          <div className="flex items-center gap-1.5 mb-2">
            <svg width="10" height="10" viewBox="0 0 24 24" fill="currentColor" className="text-[#f0b429] shrink-0">
              <path d="M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 17l-6.2 4.3 2.4-7.4L2 9.4h7.6z" />
            </svg>
            <div className="f-mono text-[9px] uppercase tracking-widest text-[#f0b429]">Millores de rendiment</div>
          </div>
          {health.recommendations.map((r, i) => (
            <div key={i} className="flex items-start gap-2 p-2 rounded bg-[rgba(240,180,41,0.06)] border border-[rgba(240,180,41,0.15)]">
              <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" className="text-[#f0b429] shrink-0 mt-0.5">
                <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
              </svg>
              <span className="f-mono text-[10px] text-ink-1 leading-relaxed">{r}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
