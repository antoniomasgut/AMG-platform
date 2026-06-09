'use client';
import { Link } from '@/i18n/navigation';

import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { PortalShell } from '@/components/portal/PortalShell';
import { IconSet } from '@/components/ui/icons';
import { AMGBadge } from '@/components/ui/badge';

export default function KnowledgeListPage() {
  const t = useTranslations('knowledge');
  const router = useRouter();
  const { user, isAdmin, isSuperAdmin } = useAuth();

  const canManage = isAdmin || isSuperAdmin;

  const { data: tenantsPage, isLoading } = useQuery({
    queryKey: ['tenants'],
    queryFn: () => fetch('/api/v1/tenants?size=200').then(r => r.json()),
    enabled: canManage,
  });
  const tenants = tenantsPage?.content ?? [];

  return (
    <PortalShell breadcrumb="admin · coneixement">
      <div className="p-4 sm:p-8 space-y-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / coneixement /</span>
            <div className="f-display font-bold text-xl mt-1">{t('title')}</div>
            <p className="text-sm text-ink-2 mt-1">{t('subtitle')}</p>
          </div>
        </div>

        <div className="flex gap-3 border-b border-border-base pb-3 flex-wrap">
          <Link href="/portal/admin/users" className="f-mono text-label uppercase text-ink-2 hover:text-ink-0 pb-2">Usuaris</Link>
          <Link href="/portal/admin/tenants" className="f-mono text-label uppercase text-ink-2 hover:text-ink-0 pb-2">Tenants</Link>
          <Link href="/portal/admin/knowledge" className="f-mono text-label uppercase text-accent-light border-b-2 border-[#FF6B00] pb-2">Coneixement</Link>
        </div>

        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <span className="f-mono text-label uppercase text-ink-3">{t('tenantBases')}</span>
          </div>

          {isLoading ? (
            <div className="flex justify-center py-12">
              <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
            </div>
          ) : !tenants || tenants.length === 0 ? (
            <div className="amg-card card-clip p-8 text-center">
              <IconSet.Box size={28} stroke="#64748b" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">{t('noTenants')}</div>
              <p className="f-mono text-xs text-ink-2">{t('noTenantsDesc')}</p>
            </div>
          ) : (
            <div className="amg-card card-clip overflow-hidden">
              <table className="w-full min-w-[700px]">
                <thead>
                  <tr className="border-b border-border-base">
                    <th className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">{t('tenant')}</th>
                    <th className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">{t('kbStatus')}</th>
                    <th className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">{t('entries')}</th>
                    <th className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">{t('documents')}</th>
                    <th className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">{t('lastUpdated')}</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {tenants.map((tnt: { id: string; name: string; slug: string }) => (
                    <KnowledgeRow key={tnt.id} tenantId={tnt.id} tenantName={tnt.name} router={router} t={t} />
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

function KnowledgeRow({ tenantId, tenantName, router, t }: {
  tenantId: string;
  tenantName: string;
  router: ReturnType<typeof useRouter>;
  t: ReturnType<typeof useTranslations<'knowledge'>>;
}) {
  const { data: kb, isLoading } = useQuery({
    queryKey: ['knowledge', tenantId],
    queryFn: () => fetch(`/api/v1/agents/knowledge/${tenantId}`).then(r => r.json()),
  });

  const countEntries = kb?.entriesByCategory
    ? Object.values(kb.entriesByCategory as Record<string, unknown[]>).reduce((a: number, b: unknown[]) => a + b.length, 0)
    : 0;
  const countDocs = kb?.documents?.length ?? 0;

  return (
    <tr className="border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,255,255,0.02)] transition-colors">
      <td className="px-4 sm:px-5 py-3">
        <div className="f-display font-bold text-sm">{tenantName}</div>
        <div className="f-mono text-xs text-ink-3">{tenantId.slice(0, 8)}…</div>
      </td>
      <td className="px-4 sm:px-5 py-3">
        {isLoading ? (
          <span className="w-3 h-3 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin inline-block" />
        ) : (
          <AMGBadge tone={kb?.isActive ? 'success' : 'neutral'}>
            {kb?.isActive ? t('active') : t('inactive')}
          </AMGBadge>
        )}
      </td>
      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{countEntries}</td>
      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{countDocs}</td>
      <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-2">
        {kb?.updatedAt ? new Date(kb.updatedAt).toLocaleDateString() : '-'}
      </td>
      <td className="px-4 sm:px-5 py-3">
        <button
          onClick={() => router.push(`/portal/admin/knowledge/${tenantId}`)}
          className="f-mono text-[10px] uppercase text-ink-2 hover:text-accent-light border border-border-base hover:border-[#FF6B00]/40 px-2 py-1 transition-colors"
        >
          {t('manage')}
        </button>
      </td>
    </tr>
  );
}
