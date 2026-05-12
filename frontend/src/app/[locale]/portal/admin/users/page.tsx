'use client';

import { useTranslations } from 'next-intl';
import { useState, useEffect, useCallback } from 'react';
import { useRouter } from '@/i18n/navigation';
import { DataTable } from '@/components/admin/DataTable';
import { UserStatusBadge } from '@/components/admin/UserStatusBadge';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { I } from '@/components/ui/icons';
import { listUsers, deleteUser, unlockUser } from '@/services/userService';
import { getCurrentUser } from '@/services/auth';

export default function UsersPage() {
  const t = useTranslations('admin');
  const router = useRouter();
  const [users, setUsers] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [currentUser, setCurrentUser] = useState<any>(null);
  const [actionMsg, setActionMsg] = useState('');

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const user = getCurrentUser();
      setCurrentUser(user);
      const result = await listUsers({ page, size: 20, search: search || undefined, role: roleFilter || undefined });
      setUsers(result.content);
      setTotalPages(result.totalPages);
    } catch (err: any) {
      setError(err.message || 'Error carregant usuaris');
    } finally {
      setLoading(false);
    }
  }, [page, search, roleFilter]);

  useEffect(() => {
    const timer = setTimeout(() => fetchUsers(), search ? 300 : 0);
    return () => clearTimeout(timer);
  }, [fetchUsers, search]);

  const handleDelete = async (id: string) => {
    if (!window.confirm(t('users.deleteConfirm'))) return;
    try {
      await deleteUser(id);
      setActionMsg(t('users.deleted'));
      fetchUsers();
    } catch (err: any) {
      setError(err.message);
    }
  };

  const handleUnlock = async (id: string) => {
    try {
      await unlockUser(id);
      setActionMsg(t('users.unlockSuccess'));
      fetchUsers();
    } catch (err: any) {
      setError(err.message);
    }
  };

  const roleTone = (role: string) => {
    switch (role) {
      case 'SUPER_ADMIN': return 'accent' as const;
      case 'ADMIN': return 'info' as const;
      default: return 'neutral' as const;
    }
  };

  const formatDate = (d: string | null) => {
    if (!d) return t('users.never');
    return new Date(d).toLocaleDateString();
  };

  const columns = [
    { label: 'Email', render: (u: any) => u.email },
    { label: 'Nom', render: (u: any) => u.name },
    {
      label: 'Rol',
      render: (u: any) => <AMGBadge tone={roleTone(u.role)}>{t(`users.roles.${u.role}` as any)}</AMGBadge>,
    },
    { label: 'Tenant', render: (u: any) => u.tenant?.name || '-' },
    {
      label: 'Estat',
      render: (u: any) => <UserStatusBadge isActive={u.isActive} isBlocked={u.isBlocked} />,
    },
    { label: 'Última connexió', render: (u: any) => formatDate(u.lastLoginAt) },
    {
      label: '',
      className: 'text-right w-28',
      render: (u: any) => (
        <div className="flex items-center justify-end gap-1" onClick={(e) => e.stopPropagation()}>
          <AMGButton variant="ghost" size="sm" onClick={() => router.push(`/portal/admin/users/${u.id}`)}>
            <I.Edit size={14} />
          </AMGButton>
          {u.isBlocked && (
            <AMGButton variant="ghost" size="sm" onClick={() => handleUnlock(u.id)}>
              <I.Check size={14} />
            </AMGButton>
          )}
          {currentUser?.role === 'SUPER_ADMIN' && (
            <AMGButton variant="ghost" size="sm" onClick={() => handleDelete(u.id)}>
              <I.Trash size={14} />
            </AMGButton>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="p-4 sm:p-8 space-y-6">
      {/* Action msg */}
      {actionMsg && (
        <div className="bg-[rgba(57,211,83,0.12)] border border-[rgba(57,211,83,0.35)] rounded px-4 py-2 text-sm text-[#39d353]">
          {actionMsg}
          <button className="ml-3 text-xs opacity-60 hover:opacity-100" onClick={() => setActionMsg('')}>X</button>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <h1 className="f-display font-black text-lg text-white">{t('users.title')}</h1>
        <AMGButton variant="primary" size="sm" onClick={() => router.push('/portal/admin/users/new')}>
          <I.Plus size={14} className="mr-1.5" /> {t('users.new')}
        </AMGButton>
      </div>

      {/* Search + Filter */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <I.Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-2/50" />
          <input
            className="w-full h-10 bg-[#1a1a2e]/80 border border-[rgba(255,107,0,0.14)] rounded pl-9 pr-3 text-sm text-[#e2e8f0] placeholder:text-[#64748b] outline-none focus:border-[#FF6B00] transition"
            placeholder={t('users.search')}
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0); }}
          />
        </div>
        <select
          className="h-10 px-3 bg-[#1a1a2e]/80 border border-[rgba(255,107,0,0.14)] rounded text-sm text-[#e2e8f0] outline-none focus:border-[#FF6B00] transition"
          value={roleFilter}
          onChange={(e) => { setRoleFilter(e.target.value); setPage(0); }}
        >
          <option value="">Tots els rols</option>
          {currentUser?.role === 'SUPER_ADMIN' ? (
            <>
              <option value="SUPER_ADMIN">{t('users.roles.SUPER_ADMIN' as any)}</option>
              <option value="ADMIN">{t('users.roles.ADMIN' as any)}</option>
              <option value="CLIENT">{t('users.roles.CLIENT' as any)}</option>
            </>
          ) : (
            <option value="CLIENT">{t('users.roles.CLIENT' as any)}</option>
          )}
        </select>
      </div>

      {/* Error */}
      {error && (
        <div className="bg-[rgba(255,68,68,0.12)] border border-[rgba(255,68,68,0.35)] rounded p-4 text-sm text-[#ff6666] flex items-center justify-between">
          <span>{error}</span>
          <AMGButton variant="ghost" size="sm" onClick={fetchUsers}>Reintentar</AMGButton>
        </div>
      )}

      {/* Table */}
      <DataTable
        columns={columns}
        data={users}
        page={page}
        totalPages={totalPages}
        onPageChange={setPage}
        loading={loading}
        emptyMessage={t('users.noResults')}
        keyExtractor={(u: any) => u.id}
      />
    </div>
  );
}
