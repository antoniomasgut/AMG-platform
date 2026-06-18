'use client';

import { useQuery } from '@tanstack/react-query';
import { PortalShell } from '@/components/portal/PortalShell';
import { getPipeline, type PipelineCard, type PipelineColumn } from '@/services/pipeline';

function fmtDate(iso: string | null): string {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleDateString('ca-ES', { day: 'numeric', month: 'short' });
}

const STAGE_COLORS: Record<string, string> = {
  NEW_LEADS:     'bg-[#1a1a2e] border-[#333]',
  QUALIFIED:     'bg-[#1a2e1a] border-[#2a4a2a]',
  PROPOSAL_SENT: 'bg-[#2e2a1a] border-[#4a3a1a]',
  SETUP_PENDING: 'bg-[#2e1a1a] border-[#4a2a2a]',
  IMPLEMENTING:  'bg-[#1a2a2e] border-[#1a3a4a]',
  ACTIVE:        'bg-[#0f1f0f] border-[#1a3a1a]',
};

const COUNT_COLORS: Record<string, string> = {
  NEW_LEADS:     'text-[#6b8cff]',
  QUALIFIED:     'text-[#6bff8c]',
  PROPOSAL_SENT: 'text-[#ffb06b]',
  SETUP_PENDING: 'text-[#ff6b6b]',
  IMPLEMENTING:  'text-[#6bd4ff]',
  ACTIVE:        'text-[#ff6b00]',
};

function KanbanCard({ card }: { card: PipelineCard }) {
  return (
    <a
      href={card.actionUrl}
      target="_blank"
      rel="noopener noreferrer"
      className="block bg-[#111] border border-[#222] hover:border-[#333] p-3 transition-colors group"
    >
      <div className="font-semibold text-sm text-[#f0f0f0] truncate group-hover:text-white">
        {card.name}
      </div>
      {card.sector && (
        <div className="font-mono text-[10px] text-[#555] mt-0.5 uppercase tracking-widest">
          {card.sector.toLowerCase()}
        </div>
      )}
      <div className="flex items-center justify-between mt-2 gap-2">
        {card.value ? (
          <span className="font-mono text-[11px] text-[#ff6b00]">{card.value}</span>
        ) : (
          <span />
        )}
        {card.date && (
          <span className="font-mono text-[10px] text-[#444]">{fmtDate(card.date)}</span>
        )}
      </div>
      {card.contact && !card.value && (
        <div className="font-mono text-[10px] text-[#555] mt-1 truncate">{card.contact}</div>
      )}
    </a>
  );
}

function KanbanColumn({ col }: { col: PipelineColumn }) {
  const bg    = STAGE_COLORS[col.stage]  ?? 'bg-[#111] border-[#222]';
  const color = COUNT_COLORS[col.stage] ?? 'text-[#888]';

  return (
    <div className={`flex flex-col min-w-[220px] max-w-[260px] border rounded-none ${bg}`}>
      <div className="px-3 py-2.5 border-b border-[#222] flex items-center justify-between shrink-0">
        <span className="font-mono text-[10px] uppercase tracking-widest text-[#888]">
          {col.label}
        </span>
        <span className={`font-black text-sm font-mono ${color}`}>{col.cards.length}</span>
      </div>
      <div className="flex-1 overflow-y-auto p-2 space-y-1.5 max-h-[70vh]">
        {col.cards.length === 0 ? (
          <div className="text-center py-8 font-mono text-[10px] text-[#333] uppercase tracking-widest">
            Buit
          </div>
        ) : (
          col.cards.map((card) => <KanbanCard key={card.id} card={card} />)
        )}
      </div>
    </div>
  );
}

export default function PipelinePage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['pipeline'],
    queryFn: getPipeline,
    refetchInterval: 60_000,
  });

  return (
    <PortalShell breadcrumb="Pipeline de vendes">
      {isLoading && (
        <div className="flex items-center gap-2 py-8 text-[#555] font-mono text-sm">
          <span className="w-4 h-4 border-2 border-[#ff6b00] border-t-transparent rounded-full animate-spin" />
          Carregant pipeline…
        </div>
      )}

      {error && (
        <div className="py-8 font-mono text-sm text-[#ff4444]">
          Error carregant el pipeline.
        </div>
      )}

      {data && (
        <div className="overflow-x-auto pb-4">
          <div className="flex gap-3 min-w-max">
            {data.columns.map((col) => (
              <KanbanColumn key={col.stage} col={col} />
            ))}
          </div>
        </div>
      )}
    </PortalShell>
  );
}
