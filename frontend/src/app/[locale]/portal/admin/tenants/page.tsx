'use client';

import { useTranslations } from 'next-intl';
import { useState, useEffect, useCallback } from 'react';
import { useRouter } from '@/i18n/navigation';
import { DataTable } from '@/components/admin/DataTable';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { I } from '@/components/ui/icons';
import { listTenants } from '@/services/tenantService';
import { getCurrentUser } from '@/services/auth';

export default function TenantsPage() {
  const t = useTranslations('admin');
  const router = useRouter();
  const [tenants, setTenants] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [search, setSearch] = useState('');
  const [currentUser, setCurrentUser] = useState<any>(null);

  const fetchTenants = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setCurrentUser(getCurrentUser());
      const result = await listTenants({ page, size: 20, search: search || undefined });
      setTenants(result.content);
      setTotalPages(result.totalPages);
    } catch (err: any) {
      setError(err.message || 'Error carregant tenants');
    } finally {
      setLoading(false);
    }
  }, [page, search]);

  useEffect(() => {
    const timer = setTimeout(() => fetchTenants(), search ? 300 : 0);
    return () => clearTimeout(timer);
  }, [fetchTenants, search]);

  const formatDate = (d: string) => new Date(d).toLocaleDateString();

  const columns = [
    { label: t('tenants.fields.name' as any), render: (t: any) => t.name },
    { label: t('tenants.fields.slug' as any), render: (t: any) => <span className="f-mono text-xs">{t.slug}</span> },
    { label: t('tenants.fields.email' as any), render: (t: any) => t.email || '-' },
    { label: t('tenants.fields.phone' as any), render: (t: any) => t.phone || '-' },
    {
      label: 'Estat',
      render: (t: any) => <AMGBadge tone={t.isActive ? 'success' : 'neutral'}>{t.isActive ? 'Actiu' : 'Inactiu'}</AMGBadge>,
    },
    { label: 'Creat', render: (t: any) => formatDate(t.createdAt) },
    {
      label: '',
      className: 'text-right w-16',
      render: (t: any) => (
        <div className="flex justify-end">
          <AMGButton variant="ghost" size="sm" onClick={() => router.push(`/portal/admin/tenants/${t.id}`)}>
            <I.Edit size={14} />
          </AMGButton>
        </div>
      ),
    },
  ];

  return (
    <div className="p-4 sm:p-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <h1 className="f-display font-black text-lg text-white">{t('tenants.title')}</h1>
        {currentUser?.role === 'SUPER_ADMIN' && (
          <AMGButton variant="primary" size="sm" onClick={() => router.push('/portal/admin/tenants/new')}>
            <I.Plus size={14} className="mr-1.5" /> {t('tenants.new')}
          </AMGButton>
        )}
      </div>

      {/* Search */}
      <div className="relative">
        <I.Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-2/50" />
        <input
          className="w-full h-10 bg-[#1a1a2e]/80 border border-[rgba(255,107,0,0.14)] rounded pl-9 pr-3 text-sm text-[#e2e8f0] placeholder:text-[#64748b] outline-none focus:border-[#FF6B00] transition"
          placeholder={t('tenants.search')}
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0); }}
        />
      </div>

      {/* Error */}
      {error && (
        <div className="bg-[rgba(255,68,68,0.12)] border border-[rgba(255,68,68,0.35)] rounded p-4 text-sm text-[#ff6666] flex items-center justify-between">
          <span>{error}</span>
          <AMGButton variant="ghost" size="sm" onClick={fetchTenants}>Reintentar</AMGButton>
        </div>
      )}

      {/* Table */}
      <DataTable
        columns={columns}
        data={tenants}
        page={page}
        totalPages={totalPages}
        onPageChange={setPage}
        loading={loading}
        emptyMessage={t('tenants.noResults')}
        keyExtractor={(t: any) => t.id}
      />
    </div>
  );
}
