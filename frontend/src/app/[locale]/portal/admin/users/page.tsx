'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@/lib/toast-context';
import {
  listUsers, deleteUser, unlockUser, createUser,
  type UserResponse,
} from '@/services/admin';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGSectionTitle } from '@/components/ui/stat';
import { I } from '@/components/ui/icons';

const ROLE_TONE: Record<string, 'danger' | 'warning' | 'accent'> = {
  SUPER_ADMIN: 'danger', ADMIN: 'warning', CLIENT: 'accent',
};

const ROLE_LABEL: Record<string, string> = {
  SUPER_ADMIN: 'Super Admin', ADMIN: 'Admin', CLIENT: 'Client',
};

function fmtDate(d: string | null) {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

const ROLES = ['', 'SUPER_ADMIN', 'ADMIN', 'CLIENT'];
const PAGE_SIZE = 20;

function NewUserModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const { toast } = useToast();
  const [form, setForm] = useState({ email: '', name: '', password: '', role: 'CLIENT' as const });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await createUser(form);
      toast('success', 'Usuari creat');
      onCreated();
      onClose();
    } catch {
      toast('error', 'Error creant l\'usuari');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-md p-6 space-y-4" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Nou usuari</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><I.X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          {([
            { key: 'email', label: 'Email', type: 'email', placeholder: 'nom@empresa.com' },
            { key: 'name', label: 'Nom', type: 'text', placeholder: 'Nom Cognoms' },
            { key: 'password', label: 'Contrasenya', type: 'password', placeholder: '········' },
          ] as const).map(({ key, label, type, placeholder }) => (
            <div key={key}>
              <label className="f-mono text-label uppercase text-ink-2 block mb-1">{label}</label>
              <input
                type={type}
                required
                placeholder={placeholder}
                value={form[key]}
                onChange={(e) => setForm((f) => ({ ...f, [key]: e.target.value }))}
                className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]"
              />
            </div>
          ))}
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Rol</label>
            <select
              value={form.role}
              onChange={(e) => setForm((f) => ({ ...f, role: e.target.value as typeof form.role }))}
              className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 focus:outline-none focus:border-[#FF6B00]"
            >
              <option value="CLIENT">Client</option>
              <option value="ADMIN">Admin</option>
              <option value="SUPER_ADMIN">Super Admin</option>
            </select>
          </div>
          <div className="flex gap-3 pt-2">
            <AMGButton type="submit" disabled={loading} className="flex-1 justify-center">
              {loading ? 'Creant...' : 'Crear usuari'}
            </AMGButton>
            <AMGButton type="button" variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function AdminUsersPage() {
  const { toast } = useToast();
  const qc = useQueryClient();
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [page, setPage] = useState(0);
  const [showNewUser, setShowNewUser] = useState(false);

  const { data: usersPage, isLoading } = useQuery({
    queryKey: ['users', search, roleFilter, page],
    queryFn: () => listUsers({ search: search || undefined, role: roleFilter || undefined, page, size: PAGE_SIZE }),
  });

  const users = usersPage?.content ?? [];
  const totalPages = usersPage?.totalPages ?? 1;

  const invalidate = () => qc.invalidateQueries({ queryKey: ['users'] });

  const { mutate: doDelete } = useMutation({
    mutationFn: deleteUser,
    onSuccess: () => { toast('success', 'Usuari eliminat'); invalidate(); },
    onError: () => toast('error', 'Error eliminant l\'usuari'),
  });

  const { mutate: doUnlock } = useMutation({
    mutationFn: unlockUser,
    onSuccess: () => { toast('success', 'Usuari desbloquejat'); invalidate(); },
    onError: () => toast('error', 'Error desbloquejant l\'usuari'),
  });

  return (
    <PortalShell breadcrumb="admin · usuaris">
      <div className="p-4 sm:p-8 space-y-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / usuaris /</span>
            <div className="f-display font-bold text-xl mt-1">Gestió d&apos;usuaris</div>
          </div>
          <AMGButton size="sm" icon={I.Plus} onClick={() => setShowNewUser(true)}>Nou usuari</AMGButton>
        </div>

        {/* Admin sub-nav */}
        <div className="flex gap-3 border-b border-border-base pb-3">
          <a href="/portal/admin/users" className="f-mono text-label uppercase text-accent-light border-b-2 border-[#FF6B00] pb-2">
            Usuaris
          </a>
          <a href="/portal/admin/tenants" className="f-mono text-label uppercase text-ink-2 hover:text-ink-0 pb-2">
            Tenants
          </a>
        </div>

        {/* Filters */}
        <div className="flex flex-wrap gap-3">
          <div className="relative flex-1 min-w-[200px]">
            <I.Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-2" />
            <input
              type="search"
              placeholder="Cerca per email o nom..."
              value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(0); }}
              className="w-full bg-[#0d0d1a] border border-border-base pl-9 pr-3 h-9 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]"
            />
          </div>
          <div className="flex gap-2">
            {ROLES.map((r) => (
              <button
                key={r}
                onClick={() => { setRoleFilter(r); setPage(0); }}
                className={`f-mono text-label uppercase px-3 h-9 border transition-colors ${
                  roleFilter === r
                    ? 'border-[#FF6B00] text-accent-light bg-accent-muted'
                    : 'border-border-base text-ink-2 hover:text-ink-1'
                }`}
              >
                {r || 'TOTS'}
              </button>
            ))}
          </div>
        </div>

        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base">
            <AMGSectionTitle eyebrow="Registre" title="Usuaris" />
          </div>

          {isLoading ? (
            <div className="flex justify-center py-12">
              <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
            </div>
          ) : users.length === 0 ? (
            <div className="p-8 text-center">
              <I.Users size={28} stroke="#64748b" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Cap usuari trobat</div>
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[640px]">
                  <thead>
                    <tr className="border-b border-border-base">
                      {['Usuari', 'Rol', 'Tenant', 'Estat', 'Darrer accés', 'Accions'].map((h) => (
                        <th key={h} className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {users.map((u: UserResponse) => (
                      <tr key={u.id} className="border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,255,255,0.02)] transition-colors">
                        <td className="px-4 sm:px-5 py-3">
                          <div className="f-display font-bold text-sm">{u.name || '—'}</div>
                          <div className="f-mono text-label text-ink-2">{u.email}</div>
                        </td>
                        <td className="px-4 sm:px-5 py-3">
                          <AMGBadge tone={ROLE_TONE[u.role] ?? 'neutral'}>{ROLE_LABEL[u.role] ?? u.role}</AMGBadge>
                        </td>
                        <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{u.tenantName ?? '—'}</td>
                        <td className="px-4 sm:px-5 py-3">
                          {u.isBlocked ? (
                            <AMGBadge tone="danger">Bloquejat</AMGBadge>
                          ) : u.isActive ? (
                            <AMGBadge tone="success">Actiu</AMGBadge>
                          ) : (
                            <AMGBadge tone="neutral">Inactiu</AMGBadge>
                          )}
                        </td>
                        <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{fmtDate(u.lastLoginAt)}</td>
                        <td className="px-4 sm:px-5 py-3">
                          <div className="flex gap-2">
                            {u.isBlocked && (
                              <AMGButton size="sm" variant="secondary" icon={I.Lock} onClick={() => doUnlock(u.id)}>
                                Desbloquejar
                              </AMGButton>
                            )}
                            <AMGButton
                              size="sm" variant="ghost" icon={I.Trash}
                              onClick={() => { if (confirm(`Eliminar ${u.email}?`)) doDelete(u.id); }}
                            />
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {totalPages > 1 && (
                <div className="p-4 flex items-center justify-center gap-3 border-t border-border-base">
                  <AMGButton size="sm" variant="outline" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                    ← Anterior
                  </AMGButton>
                  <span className="f-mono text-label text-ink-2">{page + 1} / {totalPages}</span>
                  <AMGButton size="sm" variant="outline" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>
                    Següent →
                  </AMGButton>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {showNewUser && (
        <NewUserModal onClose={() => setShowNewUser(false)} onCreated={invalidate} />
      )}
    </PortalShell>
  );
}
