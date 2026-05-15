'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@/lib/toast-context';
import {
  listTenants, createTenant, updateTenant,
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

export default function AdminTenantsPage() {
  const { toast } = useToast();
  const qc = useQueryClient();
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [showNewTenant, setShowNewTenant] = useState(false);

  const { data: tenantsPage, isLoading } = useQuery({
    queryKey: ['tenants', search, page],
    queryFn: () => listTenants({ search: search || undefined, page, size: PAGE_SIZE }),
  });

  const tenants = tenantsPage?.content ?? [];
  const totalPages = tenantsPage?.totalPages ?? 1;

  const invalidate = () => qc.invalidateQueries({ queryKey: ['tenants'] });

  const { mutate: doToggle } = useMutation({
    mutationFn: ({ id, isActive }: { id: string; isActive: boolean }) =>
      updateTenant(id, { isActive }),
    onSuccess: () => { toast('success', 'Tenant actualitzat'); invalidate(); },
    onError: () => toast('error', 'Error actualitzant el tenant'),
  });

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

        {/* Search */}
        <div className="relative max-w-sm">
          <I.Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-2" />
          <input
            type="search"
            placeholder="Cerca per nom o slug..."
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0); }}
            className="w-full bg-[#0d0d1a] border border-border-base pl-9 pr-3 h-9 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]"
          />
        </div>

        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base">
            <AMGSectionTitle eyebrow="Registre" title="Tenants" />
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
                      {['Tenant', 'Slug', 'Email', 'Estat', 'Creat', 'Accions'].map((h) => (
                        <th key={h} className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {tenants.map((t: TenantResponse) => (
                      <tr key={t.id} className="border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,255,255,0.02)] transition-colors">
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
    </PortalShell>
  );
}
