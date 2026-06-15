'use client';

import { Link } from '@/i18n/navigation';
import type { TenantResponse } from '@/services/admin';

const PHASES = ['F1', 'F2', 'F3', 'F4', 'F5'] as const;

const PHASE_LABELS: Record<string, string> = {
  F1: 'Agent IA',
  F2: 'Agenda',
  F3: 'Pressupostos',
  F4: 'Fidelització',
  F5: 'Equip',
};

interface Props {
  tenants: TenantResponse[];
}

export function TenantPhaseMatrix({ tenants }: Props) {
  const activeOnly = tenants.filter(t => t.isActive !== false);

  const counts = PHASES.reduce((acc, p) => {
    acc[p] = activeOnly.filter(t => (t.contractedPhases ?? []).includes(p)).length;
    return acc;
  }, {} as Record<string, number>);

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-border-base">
            <th className="text-left py-3 px-4 f-mono text-label uppercase text-ink-2 font-medium">Tenant</th>
            <th className="text-left py-3 px-4 f-mono text-label uppercase text-ink-2 font-medium">Sector</th>
            {PHASES.map(p => (
              <th key={p} className="text-center py-3 px-3 f-mono text-label uppercase text-ink-2 font-medium">
                <div>{p}</div>
                <div className="text-[10px] text-ink-3 normal-case">{PHASE_LABELS[p]}</div>
              </th>
            ))}
            <th className="text-left py-3 px-4 f-mono text-label uppercase text-ink-2 font-medium">Acció</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-border-base/40">
          <tr className="bg-bg-0/50">
            <td className="py-2 px-4 text-ink-2 text-xs f-mono">{activeOnly.length} tenants actius</td>
            <td />
            {PHASES.map(p => (
              <td key={p} className="py-2 px-3 text-center">
                <span className="text-xs text-ink-1 font-semibold">{counts[p]}</span>
                <span className="text-[10px] text-ink-3 ml-0.5">/{activeOnly.length}</span>
              </td>
            ))}
            <td />
          </tr>
          {tenants.map(tenant => {
            const contracted = tenant.contractedPhases ?? [];
            return (
              <tr key={tenant.id} className="hover:bg-bg-0/30 transition-colors">
                <td className="py-3 px-4">
                  <div className="font-medium text-ink-0 text-sm">{tenant.name}</div>
                  <div className="text-xs text-ink-3 f-mono">{tenant.email}</div>
                </td>
                <td className="py-3 px-4">
                  {tenant.sector ? (
                    <span className="text-xs text-ink-2 f-mono">{tenant.sector}</span>
                  ) : (
                    <span className="text-xs text-ink-3">—</span>
                  )}
                </td>
                {PHASES.map(p => {
                  const active = contracted.includes(p);
                  return (
                    <td key={p} className="py-3 px-3 text-center">
                      {active ? (
                        <span className="inline-flex items-center justify-center w-6 h-6 rounded-full bg-[#00D4AA]/15 text-[#00D4AA]">
                          <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                            <path d="M2 6l3 3 5-5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                          </svg>
                        </span>
                      ) : (
                        <span className="inline-flex items-center justify-center w-6 h-6 rounded-full bg-border-base/30 text-ink-3">
                          <svg width="10" height="10" viewBox="0 0 10 10" fill="none">
                            <line x1="2" y1="5" x2="8" y2="5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
                          </svg>
                        </span>
                      )}
                    </td>
                  );
                })}
                <td className="py-3 px-4">
                  <Link
                    href={`/portal/admin/tenants/${tenant.id}`}
                    className="text-xs text-ink-2 hover:text-accent transition-colors f-mono"
                  >
                    Veure →
                  </Link>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      {tenants.length === 0 && (
        <div className="text-center py-12 text-ink-3 text-sm">Cap tenant trobat</div>
      )}
    </div>
  );
}
