'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@/lib/toast-context';
import {
  listTenants, createTenant, updateTenant, deleteTenant,
  type TenantResponse,
} from '@/services/admin';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGSectionTitle } from '@/components/ui/stat';
import { I } from '@/components/ui/icons';

const PAGE_SIZE = 20;

function fmtDate(d: string) {
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

function NewTenantModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const { toast } = useToast();
  const [form, setForm] = useState({ name: '', slug: '', email: '', phone: '', address: '' });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await createTenant({ name: form.name, slug: form.slug, email: form.email || undefined, phone: form.phone || undefined, address: form.address || undefined });
      toast('success', 'Tenant creat');
      onCreated();
      onClose();
    } catch {
      toast('error', 'Error creant el tenant');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-md p-6 space-y-4" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Nou tenant</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><I.X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          {([
            { key: 'name', label: 'Nom', required: true, placeholder: 'Nom de l\'empresa' },
            { key: 'slug', label: 'Slug', required: true, placeholder: 'nom-empresa' },
            { key: 'email', label: 'Email', required: false, placeholder: 'info@empresa.com' },
            { key: 'phone', label: 'Telèfon', required: false, placeholder: '+34 600 000 000' },
            { key: 'address', label: 'Adreça', required: false, placeholder: 'Carrer, núm., localitat' },
          ] as const).map(({ key, label, required, placeholder }) => (
            <div key={key}>
              <label className="f-mono text-label uppercase text-ink-2 block mb-1">{label}</label>
              <input
                type="text"
                required={required}
                placeholder={placeholder}
                value={form[key]}
                onChange={(e) => setForm((f) => ({ ...f, [key]: e.target.value }))}
                className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]"
              />
            </div>
          ))}
          <div className="flex gap-3 pt-2">
            <AMGButton type="submit" disabled={loading} className="flex-1 justify-center">
              {loading ? 'Creant...' : 'Crear tenant'}
            </AMGButton>
            <AMGButton type="button" variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
          </div>
        </form>
      </div>
    </div>
  );
}

type ActiveFilter = 'all' | 'active' | 'inactive';
type FreeFilter = 'all' | 'free' | 'paying';
type SortOption = 'name,asc' | 'name,desc' | 'createdAt,desc' | 'createdAt,asc';

export default function AdminTenantsPage() {
  const { toast } = useToast();
  const qc = useQueryClient();
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [showNewTenant, setShowNewTenant] = useState(false);
  const [activeFilter, setActiveFilter] = useState<ActiveFilter>('all');
  const [freeFilter, setFreeFilter] = useState<FreeFilter>('all');
  const [sort, setSort] = useState<SortOption>('createdAt,desc');
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [bulkLoading, setBulkLoading] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const isActiveParam = activeFilter === 'active' ? true : activeFilter === 'inactive' ? false : undefined;
  const isFreeParam = freeFilter === 'free' ? true : freeFilter === 'paying' ? false : undefined;

  const resetPage = () => setPage(0);

  const { data: tenantsPage, isLoading } = useQuery({
    queryKey: ['tenants', search, page, activeFilter, freeFilter, sort],
    queryFn: () => listTenants({
      search: search || undefined,
      page,
      size: PAGE_SIZE,
      isActive: isActiveParam,
      isFree: isFreeParam,
      sort,
    }),
  });

  const tenants = tenantsPage?.content ?? [];
  const totalPages = tenantsPage?.totalPages ?? 1;

  const invalidate = () => {
    setSelected(new Set());
    qc.invalidateQueries({ queryKey: ['tenants'] });
  };

  const { mutate: doToggle } = useMutation({
    mutationFn: ({ id, isActive }: { id: string; isActive: boolean }) =>
      updateTenant(id, { isActive }),
    onSuccess: () => { toast('success', 'Tenant actualitzat'); invalidate(); },
    onError: () => toast('error', 'Error actualitzant el tenant'),
  });

  const allOnPageIds = tenants.map(t => t.id);
  const allSelected = allOnPageIds.length > 0 && allOnPageIds.every(id => selected.has(id));
  const someSelected = allOnPageIds.some(id => selected.has(id));

  const toggleSelectAll = () => {
    if (allSelected) {
      setSelected(prev => { const next = new Set(prev); allOnPageIds.forEach(id => next.delete(id)); return next; });
    } else {
      setSelected(prev => new Set(Array.from(prev).concat(allOnPageIds)));
    }
  };

  const toggleSelect = (id: string) => {
    setSelected(prev => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const selectedIds = Array.from(selected);

  const handleBulkActivate = async (activate: boolean) => {
    setBulkLoading(true);
    try {
      await Promise.all(selectedIds.map(id => updateTenant(id, { isActive: activate })));
      toast('success', `${selectedIds.length} tenant${selectedIds.length > 1 ? 's' : ''} ${activate ? 'activat' : 'desactivat'}${selectedIds.length > 1 ? 's' : ''}`);
      invalidate();
    } catch {
      toast('error', 'Error actualitzant tenants');
    } finally {
      setBulkLoading(false);
    }
  };

  const handleBulkDelete = async () => {
    setBulkLoading(true);
    const errors: string[] = [];
    for (const id of selectedIds) {
      try { await deleteTenant(id); } catch { errors.push(id); }
    }
    const deleted = selectedIds.length - errors.length;
    if (deleted > 0) toast('success', `${deleted} tenant${deleted > 1 ? 's' : ''} eliminat${deleted > 1 ? 's' : ''}`);
    if (errors.length > 0) toast('error', `${errors.length} tenant${errors.length > 1 ? 's' : ''} no s\'han pogut eliminar`);
    setConfirmDelete(false);
    invalidate();
    setBulkLoading(false);
  };

  return (
    <PortalShell breadcrumb="admin · tenants">
      <div className="p-4 sm:p-8 space-y-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / tenants /</span>
            <div className="f-display font-bold text-xl mt-1">Gestió de tenants</div>
          </div>
          <AMGButton size="sm" icon={I.Plus} onClick={() => setShowNewTenant(true)}>Nou tenant</AMGButton>
        </div>

        {/* Admin sub-nav */}
        <div className="flex gap-3 border-b border-border-base pb-3">
          <a href="/portal/admin/users" className="f-mono text-label uppercase text-ink-2 hover:text-ink-0 pb-2">
            Usuaris
          </a>
          <a href="/portal/admin/tenants" className="f-mono text-label uppercase text-accent-light border-b-2 border-[#FF6B00] pb-2">
            Tenants
          </a>
        </div>

        {/* Search + filtres + ordenació */}
        <div className="flex flex-wrap gap-3 items-center">
          <div className="relative">
            <I.Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-2" />
            <input
              type="search"
              placeholder="Cerca per nom o slug..."
              value={search}
              onChange={(e) => { setSearch(e.target.value); resetPage(); }}
              className="bg-[#0d0d1a] border border-border-base pl-9 pr-3 h-9 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00] w-56"
            />
          </div>

          {/* Estat */}
          <div className="flex gap-1">
            {([['all', 'Tots'], ['active', 'Actius'], ['inactive', 'Inactius']] as [ActiveFilter, string][]).map(([val, label]) => (
              <button
                key={val}
                onClick={() => { setActiveFilter(val); resetPage(); }}
                className={`f-mono text-[10px] uppercase tracking-wider px-2.5 py-1 border transition-colors ${
                  activeFilter === val
                    ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.12)] text-white'
                    : 'border-border-base text-ink-2 hover:text-ink-0 hover:border-ink-2'
                }`}
              >
                {label}
              </button>
            ))}
          </div>

          {/* Facturació */}
          <div className="flex gap-1">
            {([['all', 'Tots'], ['free', 'Gratuïts'], ['paying', 'De pagament']] as [FreeFilter, string][]).map(([val, label]) => (
              <button
                key={val}
                onClick={() => { setFreeFilter(val); resetPage(); }}
                className={`f-mono text-[10px] uppercase tracking-wider px-2.5 py-1 border transition-colors ${
                  freeFilter === val
                    ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.12)] text-white'
                    : 'border-border-base text-ink-2 hover:text-ink-0 hover:border-ink-2'
                }`}
              >
                {label}
              </button>
            ))}
          </div>

          {/* Ordenació */}
          <select
            value={sort}
            onChange={(e) => { setSort(e.target.value as SortOption); resetPage(); }}
            className="bg-[#0d0d1a] border border-border-base px-2 h-9 text-sm text-ink-1 focus:outline-none focus:border-[#FF6B00] f-mono"
          >
            <option value="createdAt,desc">Més recents</option>
            <option value="createdAt,asc">Més antics</option>
            <option value="name,asc">Nom A→Z</option>
            <option value="name,desc">Nom Z→A</option>
          </select>
        </div>

        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between gap-4">
            <AMGSectionTitle eyebrow="Registre" title="Tenants" />
            {selected.size > 0 && (
              <div className="flex items-center gap-2">
                <span className="f-mono text-xs text-ink-2">{selected.size} seleccionat{selected.size > 1 ? 's' : ''}</span>
                <AMGButton size="sm" variant="ghost" disabled={bulkLoading} onClick={() => handleBulkActivate(true)}>
                  Activar
                </AMGButton>
                <AMGButton size="sm" variant="ghost" disabled={bulkLoading} onClick={() => handleBulkActivate(false)}>
                  Desactivar
                </AMGButton>
                <AMGButton size="sm" variant="ghost" disabled={bulkLoading}
                  onClick={() => setConfirmDelete(true)}
                  className="text-red-400 hover:text-red-300">
                  <I.Trash size={14} className="mr-1" />Eliminar
                </AMGButton>
                <button onClick={() => setSelected(new Set())} className="text-ink-3 hover:text-ink-1 ml-1">
                  <I.X size={14} />
                </button>
              </div>
            )}
          </div>

          {isLoading ? (
            <div className="flex justify-center py-12">
              <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
            </div>
          ) : tenants.length === 0 ? (
            <div className="p-8 text-center">
              <I.Building size={28} stroke="#64748b" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Cap tenant trobat</div>
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[580px]">
                  <thead>
                    <tr className="border-b border-border-base">
                      <th className="px-4 py-3 w-10">
                        <input
                          type="checkbox"
                          checked={allSelected}
                          ref={el => { if (el) el.indeterminate = someSelected && !allSelected; }}
                          onChange={toggleSelectAll}
                          className="accent-[#FF6B00] w-3.5 h-3.5 cursor-pointer"
                        />
                      </th>
                      {['Tenant', 'Slug', 'Email', 'Estat', 'Facturació', 'Creat', 'Accions'].map((h) => (
                        <th key={h} className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {tenants.map((t: TenantResponse) => (
                      <tr key={t.id} className={`border-b border-[rgba(226,232,240,0.04)] transition-colors ${
                        selected.has(t.id) ? 'bg-[rgba(255,107,0,0.06)]' : 'hover:bg-[rgba(255,255,255,0.02)]'
                      }`}>
                        <td className="px-4 py-3 w-10">
                          <input
                            type="checkbox"
                            checked={selected.has(t.id)}
                            onChange={() => toggleSelect(t.id)}
                            className="accent-[#FF6B00] w-3.5 h-3.5 cursor-pointer"
                          />
                        </td>
                        <td className="px-4 sm:px-5 py-3">
                          <div className="f-display font-bold text-sm">{t.name}</div>
                        </td>
                        <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-2">{t.slug}</td>
                        <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{t.email ?? '—'}</td>
                        <td className="px-4 sm:px-5 py-3">
                          {t.isActive ? (
                            <AMGBadge tone="success">Actiu</AMGBadge>
                          ) : (
                            <AMGBadge tone="neutral">Inactiu</AMGBadge>
                          )}
                        </td>
                        <td className="px-4 sm:px-5 py-3">
                          {t.isFree
                            ? <AMGBadge tone="warning">Gratuït</AMGBadge>
                            : <AMGBadge tone="neutral">Pagament</AMGBadge>}
                        </td>
                        <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{fmtDate(t.createdAt)}</td>
                        <td className="px-4 sm:px-5 py-3">
                          <div className="flex gap-2 items-center">
                            <AMGButton
                              size="sm"
                              variant="secondary"
                              onClick={() => window.location.href = `/portal/admin/tenants/${t.id}`}
                            >
                              Gestionar
                            </AMGButton>
                            <AMGButton
                              size="sm"
                              variant={t.isActive ? 'ghost' : 'secondary'}
                              onClick={() => doToggle({ id: t.id, isActive: !t.isActive })}
                            >
                              {t.isActive ? 'Desactivar' : 'Activar'}
                            </AMGButton>
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

      {showNewTenant && (
        <NewTenantModal onClose={() => setShowNewTenant(false)} onCreated={invalidate} />
      )}

      {confirmDelete && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={() => setConfirmDelete(false)}>
          <div className="amg-card card-clip w-full max-w-sm p-6 space-y-4" onClick={e => e.stopPropagation()}>
            <div className="flex items-center gap-3">
              <I.AlertCircle size={20} className="text-red-400 flex-shrink-0" />
              <div className="f-display font-bold text-base">Eliminar {selected.size} tenant{selected.size > 1 ? 's' : ''}</div>
            </div>
            <p className="text-sm text-ink-2">Aquesta acció és irreversible. S&apos;eliminaran tots els serveis i dades associats.</p>
            <div className="flex gap-3">
              <AMGButton
                onClick={handleBulkDelete}
                disabled={bulkLoading}
                loading={bulkLoading}
                className="flex-1 justify-center bg-red-600 hover:bg-red-700 border-red-600"
              >
                Confirmar eliminació
              </AMGButton>
              <AMGButton variant="outline" onClick={() => setConfirmDelete(false)}>Cancel·lar</AMGButton>
            </div>
          </div>
        </div>
      )}
    </PortalShell>
  );
}
