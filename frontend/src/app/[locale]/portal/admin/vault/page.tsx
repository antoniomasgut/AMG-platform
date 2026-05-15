'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useRouter, useSearchParams } from 'next/navigation';
import { useToast } from '@/lib/toast-context';
import {
  listProfiles, createProfile, deleteProfile,
  listCatalogServices, createAddonService,
  type CatalogProfileResponse, type CreateCatalogProfileRequest,
  type CatalogService, type CreateCatalogServiceRequest,
} from '@/services/admin';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGSectionTitle } from '@/components/ui/stat';
import { I } from '@/components/ui/icons';

type Tab = 'profiles' | 'services';

const SERVICE_TYPE_OPTIONS = ['CREDENTIALS', 'LANDING', 'AUTOMATION', 'BILLING', 'OTHER'];

function NewProfileModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const { toast } = useToast();
  const [form, setForm] = useState({ name: '', slug: '', description: '' });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await createProfile({ name: form.name, slug: form.slug, description: form.description || undefined });
      toast('success', 'Perfil creat');
      onCreated();
      onClose();
    } catch {
      toast('error', 'Error creant el perfil');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-md p-6 space-y-4" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Nou perfil</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><I.X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Nom</label>
            <input type="text" required placeholder="Ex: Professionals amb cita"
              value={form.name} onChange={(e) => setForm(f => ({ ...f, name: e.target.value }))}
              className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
          </div>
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Slug</label>
            <input type="text" required placeholder="professionals-cita"
              value={form.slug} onChange={(e) => setForm(f => ({ ...f, slug: e.target.value }))}
              className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
          </div>
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Descripció</label>
            <textarea placeholder="Perfil orientatiu per a negocis amb cita prèvia"
              value={form.description} onChange={(e) => setForm(f => ({ ...f, description: e.target.value }))}
              className="w-full bg-[#0d0d1a] border border-border-base px-3 py-2 h-20 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00] resize-none" />
          </div>
          <div className="flex gap-3 pt-2">
            <AMGButton type="submit" disabled={loading} className="flex-1 justify-center">
              {loading ? 'Creant...' : 'Crear perfil'}
            </AMGButton>
            <AMGButton type="button" variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
          </div>
        </form>
      </div>
    </div>
  );
}

function NewServiceModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const { toast } = useToast();
  const [form, setForm] = useState({
    name: '', slug: '', description: '', type: 'LANDING',
    cost: '', salePrice: '', isAddon: false,
  });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const data: CreateCatalogServiceRequest = {
        name: form.name, slug: form.slug,
        description: form.description || undefined,
        type: form.type,
        cost: parseFloat(form.cost) || 0,
        salePrice: parseFloat(form.salePrice) || 0,
      };
      if (form.isAddon) {
        await createAddonService(data);
      } else {
        toast('error', 'Selecciona "És addon" per crear un servei solt, o afegeix-lo dins d\'una fase des de l\'editor de perfil');
        return;
      }
      toast('success', 'Servei creat');
      onCreated();
      onClose();
    } catch {
      toast('error', 'Error creant el servei');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-md p-6 space-y-4" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Nou servei</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><I.X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Nom</label>
            <input type="text" required placeholder="Ex: Landing Pro"
              value={form.name} onChange={(e) => setForm(f => ({ ...f, name: e.target.value }))}
              className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
          </div>
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Slug</label>
            <input type="text" required placeholder="landing-pro"
              value={form.slug} onChange={(e) => setForm(f => ({ ...f, slug: e.target.value }))}
              className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
          </div>
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Tipus</label>
            <select value={form.type} onChange={(e) => setForm(f => ({ ...f, type: e.target.value }))}
              className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 focus:outline-none focus:border-[#FF6B00]">
              {SERVICE_TYPE_OPTIONS.map(t => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="f-mono text-label uppercase text-ink-2 block mb-1">Cost (€)</label>
              <input type="number" step="0.01" required placeholder="0"
                value={form.cost} onChange={(e) => setForm(f => ({ ...f, cost: e.target.value }))}
                className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
            </div>
            <div>
              <label className="f-mono text-label uppercase text-ink-2 block mb-1">Preu venda (€)</label>
              <input type="number" step="0.01" required placeholder="0"
                value={form.salePrice} onChange={(e) => setForm(f => ({ ...f, salePrice: e.target.value }))}
                className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
            </div>
          </div>
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Descripció</label>
            <textarea placeholder="Descripció del servei"
              value={form.description} onChange={(e) => setForm(f => ({ ...f, description: e.target.value }))}
              className="w-full bg-[#0d0d1a] border border-border-base px-3 py-2 h-20 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00] resize-none" />
          </div>
          <label className="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" checked={form.isAddon}
              onChange={(e) => setForm(f => ({ ...f, isAddon: e.target.checked }))}
              className="accent-[#FF6B00]" />
            <span className="f-mono text-label uppercase text-ink-2 text-xs">És un add-on (servei extra opcional)</span>
          </label>
          <div className="flex gap-3 pt-2">
            <AMGButton type="submit" disabled={loading} className="flex-1 justify-center">
              {loading ? 'Creant...' : 'Crear servei'}
            </AMGButton>
            <AMGButton type="button" variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
          </div>
        </form>
      </div>
    </div>
  );
}

function ProfileCard({ profile }: { profile: CatalogProfileResponse }) {
  const router = useRouter();
  const phaseCount = profile.phases?.length ?? 0;
  const serviceCount = profile.phases?.reduce((a, p) => a + (p.services?.length ?? 0), 0) ?? 0;

  return (
    <button onClick={() => router.push(`/portal/admin/vault/profiles/${profile.id}`)}
      className="amg-card card-clip p-5 text-left hover:ring-1 hover:ring-[#FF6B00] transition group w-full">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="f-display font-bold text-sm text-ink-0 group-hover:text-accent-light transition truncate">
            {profile.name}
          </div>
          <div className="f-mono text-xs text-ink-3 mt-0.5">/{profile.slug}</div>
        </div>
        {!profile.isActive && <AMGBadge tone="neutral">Inactiu</AMGBadge>}
      </div>
      {profile.description && (
        <div className="text-sm text-ink-2 mt-2 line-clamp-2">{profile.description}</div>
      )}
      <div className="flex gap-3 mt-3 text-xs text-ink-3">
        <span>{phaseCount} fases</span>
        <span>{serviceCount} serveis</span>
      </div>
    </button>
  );
}

export default function AdminVaultPage() {
  const { toast } = useToast();
  const qc = useQueryClient();
  const searchParams = useSearchParams();
  const [tab, setTab] = useState<Tab>((searchParams.get('tab') as Tab) || 'profiles');
  const [showNewProfile, setShowNewProfile] = useState(false);
  const [showNewService, setShowNewService] = useState(false);

  const { data: profiles, isLoading: loadingProfiles } = useQuery({
    queryKey: ['vault-profiles'],
    queryFn: () => listProfiles(),
  });

  const { data: services, isLoading: loadingServices } = useQuery({
    queryKey: ['vault-services'],
    queryFn: () => listCatalogServices(),
  });

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['vault-profiles'] });
    qc.invalidateQueries({ queryKey: ['vault-services'] });
  };

  const { mutate: doDeleteProfile } = useMutation({
    mutationFn: (id: string) => deleteProfile(id),
    onSuccess: () => { toast('success', 'Perfil eliminat'); invalidate(); },
    onError: () => toast('error', 'Error eliminant el perfil'),
  });

  return (
    <PortalShell breadcrumb="admin · catàleg">
      <div className="p-4 sm:p-8 space-y-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / catàleg /</span>
            <div className="f-display font-bold text-xl mt-1">Gestió del catàleg</div>
          </div>
        </div>

        {/* Admin sub-nav */}
        <div className="flex gap-3 border-b border-border-base pb-3 flex-wrap">
          <a href="/portal/admin/users" className="f-mono text-label uppercase text-ink-2 hover:text-ink-0 pb-2">Usuaris</a>
          <a href="/portal/admin/tenants" className="f-mono text-label uppercase text-ink-2 hover:text-ink-0 pb-2">Tenants</a>
          <a href="/portal/admin/vault" className="f-mono text-label uppercase text-accent-light border-b-2 border-[#FF6B00] pb-2">Catàleg</a>
        </div>

        {/* Tabs */}
        <div className="flex gap-4 border-b border-border-base">
          <button onClick={() => setTab('profiles')}
            className={`f-mono text-label uppercase pb-3 transition-colors ${tab === 'profiles' ? 'text-accent-light border-b-2 border-[#FF6B00]' : 'text-ink-2 hover:text-ink-0'}`}>
            Perfils
          </button>
          <button onClick={() => setTab('services')}
            className={`f-mono text-label uppercase pb-3 transition-colors ${tab === 'services' ? 'text-accent-light border-b-2 border-[#FF6B00]' : 'text-ink-2 hover:text-ink-0'}`}>
            Serveis
          </button>
        </div>

        {/* Profiles tab */}
        {tab === 'profiles' && (
          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <span className="f-mono text-label uppercase text-ink-3">Perfils de serveis</span>
              <AMGButton size="sm" icon={I.Plus} onClick={() => setShowNewProfile(true)}>Nou perfil</AMGButton>
            </div>
            {loadingProfiles ? (
              <div className="flex justify-center py-12">
                <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
              </div>
            ) : !profiles || profiles.length === 0 ? (
              <div className="amg-card card-clip p-8 text-center">
                <I.Box size={28} stroke="#64748b" className="mx-auto mb-3" />
                <div className="f-display font-bold text-sm mb-1">Cap perfil</div>
                <p className="f-mono text-xs text-ink-2">Crea el primer perfil per començar</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {profiles.map((p) => (
                  <div key={p.id} className="relative group">
                    <ProfileCard profile={p} />
                    <button onClick={(e) => { e.stopPropagation(); if (confirm('Eliminar aquest perfil?')) doDeleteProfile(p.id); }}
                      className="absolute top-2 right-2 w-7 h-7 flex items-center justify-center rounded bg-black/40 opacity-0 group-hover:opacity-100 hover:bg-danger/80 transition text-ink-2 hover:text-white">
                      <I.X size={14} />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Services tab */}
        {tab === 'services' && (
          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <span className="f-mono text-label uppercase text-ink-3">Catàleg de serveis</span>
              <AMGButton size="sm" icon={I.Plus} onClick={() => setShowNewService(true)}>Nou servei</AMGButton>
            </div>
            {loadingServices ? (
              <div className="flex justify-center py-12">
                <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
              </div>
            ) : !services || services.length === 0 ? (
              <div className="amg-card card-clip p-8 text-center">
                <I.Box size={28} stroke="#64748b" className="mx-auto mb-3" />
                <div className="f-display font-bold text-sm mb-1">Cap servei al catàleg</div>
                <p className="f-mono text-xs text-ink-2">Crea serveis per afegir-los al catàleg</p>
              </div>
            ) : (
              <div className="amg-card card-clip overflow-hidden">
                <table className="w-full min-w-[600px]">
                  <thead>
                    <tr className="border-b border-border-base">
                      {['Servei', 'Slug', 'Tipus', 'Cost', 'Preu venda', 'Addon'].map(h => (
                        <th key={h} className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {services.map((s: CatalogService) => (
                      <tr key={s.id} className="border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,255,255,0.02)] transition-colors">
                        <td className="px-4 sm:px-5 py-3">
                          <div className="f-display font-bold text-sm">{s.name}</div>
                          {s.description && <div className="f-mono text-xs text-ink-3 mt-0.5">{s.description}</div>}
                        </td>
                        <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-2">{s.slug}</td>
                        <td className="px-4 sm:px-5 py-3">
                          <AMGBadge tone={s.type === 'LANDING' ? 'accent' : s.type === 'CREDENTIALS' ? 'info' : 'neutral'}>
                            {s.type}
                          </AMGBadge>
                        </td>
                        <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{s.cost?.toFixed(2)} €</td>
                        <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{s.salePrice?.toFixed(2)} €</td>
                        <td className="px-4 sm:px-5 py-3">{s.isAddon ? <AMGBadge tone="info">Addon</AMGBadge> : '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
      </div>

      {showNewProfile && <NewProfileModal onClose={() => setShowNewProfile(false)} onCreated={invalidate} />}
      {showNewService && <NewServiceModal onClose={() => setShowNewService(false)} onCreated={invalidate} />}
    </PortalShell>
  );
}
