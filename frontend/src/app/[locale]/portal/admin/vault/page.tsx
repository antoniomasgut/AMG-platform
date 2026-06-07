'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useRouter, useSearchParams } from 'next/navigation';
import { useToast } from '@/lib/toast-context';
import {
  listProfiles, createProfile, deleteProfile,
  listCatalogServices, createAddonService,
  bumpCatalogVersion, acknowledgeOutdated, listOutdatedServices,
  type CatalogProfileResponse,
  type CatalogService,
  type OutdatedServiceResponse,
} from '@/services/admin';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGSectionTitle } from '@/components/ui/stat';
import { IconSet } from '@/components/ui/icons';
import { profileFormSchema, serviceFormSchema } from '@/lib/validations/vault';
import { FieldError } from '@/components/ui/field-error';

type Tab = 'profiles' | 'services' | 'outdated';

const SERVICE_TYPE_OPTIONS = ['CREDENTIALS', 'LANDING', 'AUTOMATION', 'BILLING', 'OTHER'];

function NewProfileModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const { toast } = useToast();
  const [form, setForm] = useState({ name: '', slug: '', description: '' });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrors({});
    const result = profileFormSchema.safeParse(form);
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      for (const issue of result.error.issues) {
        const field = issue.path[0] as string;
        if (!fieldErrors[field]) fieldErrors[field] = issue.message;
      }
      setErrors(fieldErrors);
      return;
    }
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
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><IconSet.X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Nom</label>
            <input type="text" required placeholder="Ex: Professionals amb cita"
              value={form.name} onChange={(e) => setForm(f => ({ ...f, name: e.target.value }))}
              className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
            <FieldError error={errors.name} />
          </div>
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Slug</label>
            <input type="text" required placeholder="professionals-cita"
              value={form.slug} onChange={(e) => setForm(f => ({ ...f, slug: e.target.value }))}
              className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
            <FieldError error={errors.slug} />
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
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrors({});
    const result = serviceFormSchema.safeParse(form);
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      for (const issue of result.error.issues) {
        const field = issue.path[0] as string;
        if (!fieldErrors[field]) fieldErrors[field] = issue.message;
      }
      setErrors(fieldErrors);
      return;
    }
    setLoading(true);
    try {
      const data = {
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
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><IconSet.X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Nom</label>
            <input type="text" required placeholder="Ex: Landing Pro"
              value={form.name} onChange={(e) => setForm(f => ({ ...f, name: e.target.value }))}
              className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
            <FieldError error={errors.name} />
          </div>
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Slug</label>
            <input type="text" required placeholder="landing-pro"
              value={form.slug} onChange={(e) => setForm(f => ({ ...f, slug: e.target.value }))}
              className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
            <FieldError error={errors.slug} />
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
              <FieldError error={errors.cost} />
            </div>
            <div>
              <label className="f-mono text-label uppercase text-ink-2 block mb-1">Preu venda (€)</label>
              <input type="number" step="0.01" required placeholder="0"
                value={form.salePrice} onChange={(e) => setForm(f => ({ ...f, salePrice: e.target.value }))}
                className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
              <FieldError error={errors.salePrice} />
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

  const { data: outdated, isLoading: loadingOutdated } = useQuery({
    queryKey: ['vault-outdated'],
    queryFn: () => listOutdatedServices(),
    enabled: tab === 'outdated',
  });

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['vault-profiles'] });
    qc.invalidateQueries({ queryKey: ['vault-services'] });
    qc.invalidateQueries({ queryKey: ['vault-outdated'] });
  };

  const { mutate: doDeleteProfile } = useMutation({
    mutationFn: (id: string) => deleteProfile(id),
    onSuccess: () => { toast('success', 'Perfil eliminat'); invalidate(); },
    onError: () => toast('error', 'Error eliminant el perfil'),
  });

  const { mutate: doBump } = useMutation({
    mutationFn: (id: string) => bumpCatalogVersion(id),
    onSuccess: () => { toast('success', 'Versió incrementada — clients afectats marcats com a desfasats'); invalidate(); },
    onError: () => toast('error', 'Error incrementant la versió'),
  });

  const { mutate: doAcknowledge } = useMutation({
    mutationFn: ({ tenantId, serviceId }: { tenantId: string; serviceId: string }) =>
      acknowledgeOutdated(tenantId, serviceId),
    onSuccess: () => { toast('success', 'Marcat com a revisat'); invalidate(); },
    onError: () => toast('error', 'Error marcant com a revisat'),
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
          <button onClick={() => setTab('outdated')}
            className={`f-mono text-label uppercase pb-3 transition-colors flex items-center gap-1.5 ${tab === 'outdated' ? 'text-accent-light border-b-2 border-[#FF6B00]' : 'text-ink-2 hover:text-ink-0'}`}>
            Desfasats
            {outdated && outdated.length > 0 && (
              <span className="bg-amber-500/20 text-amber-400 text-[10px] f-mono px-1.5 py-0.5 rounded-full">
                {outdated.length}
              </span>
            )}
          </button>
        </div>

        {/* Profiles tab */}
        {tab === 'profiles' && (
          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <span className="f-mono text-label uppercase text-ink-3">Perfils de serveis</span>
              <AMGButton size="sm" icon={IconSet.Plus} onClick={() => setShowNewProfile(true)}>Nou perfil</AMGButton>
            </div>
            {loadingProfiles ? (
              <div className="flex justify-center py-12">
                <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
              </div>
            ) : !profiles || profiles.length === 0 ? (
              <div className="amg-card card-clip p-8 text-center">
                <IconSet.Box size={28} stroke="#64748b" className="mx-auto mb-3" />
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
                      <IconSet.X size={14} />
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
              <AMGButton size="sm" icon={IconSet.Plus} onClick={() => setShowNewService(true)}>Nou servei</AMGButton>
            </div>
            {loadingServices ? (
              <div className="flex justify-center py-12">
                <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
              </div>
            ) : !services || services.length === 0 ? (
              <div className="amg-card card-clip p-8 text-center">
                <IconSet.Box size={28} stroke="#64748b" className="mx-auto mb-3" />
                <div className="f-display font-bold text-sm mb-1">Cap servei al catàleg</div>
                <p className="f-mono text-xs text-ink-2">Crea serveis per afegir-los al catàleg</p>
              </div>
            ) : (
              <div className="amg-card card-clip overflow-hidden">
                <table className="w-full min-w-[700px]">
                  <thead>
                    <tr className="border-b border-border-base">
                      {['Servei', 'Slug', 'Tipus', 'Setup', 'Mensual', 'Versió', ''].map(h => (
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
                        <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{s.salePrice?.toFixed(0)} €</td>
                        <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{s.monthlyPrice?.toFixed(0)} €/mes</td>
                        <td className="px-4 sm:px-5 py-3">
                          <span className="f-mono text-xs text-ink-2 bg-[rgba(255,255,255,0.05)] px-2 py-0.5 rounded">
                            v{s.version ?? 1}
                          </span>
                        </td>
                        <td className="px-4 sm:px-5 py-3">
                          <button
                            onClick={() => { if (confirm(`Incrementar versió de "${s.name}"? Els clients existents amb aquest servei quedaran marcats com a desfasats.`)) doBump(s.id); }}
                            className="f-mono text-[10px] uppercase text-ink-2 hover:text-amber-400 border border-border-base hover:border-amber-500/40 px-2 py-1 transition-colors"
                            title="Incrementar versió del catàleg"
                          >
                            Bump
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {/* Outdated tab */}
        {tab === 'outdated' && (
          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <span className="f-mono text-label uppercase text-ink-3">Serveis de client desfasats</span>
              <span className="f-mono text-xs text-ink-3">Catàleg actualitzat · cal revisar amb el client</span>
            </div>
            {loadingOutdated ? (
              <div className="flex justify-center py-12">
                <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
              </div>
            ) : !outdated || outdated.length === 0 ? (
              <div className="amg-card card-clip p-8 text-center">
                <IconSet.Check size={28} stroke="#22c55e" className="mx-auto mb-3" />
                <div className="f-display font-bold text-sm mb-1">Tot actualitzat</div>
                <p className="f-mono text-xs text-ink-2">Cap servei de client desfassat</p>
              </div>
            ) : (
              <div className="amg-card card-clip overflow-hidden">
                <table className="w-full min-w-[700px]">
                  <thead>
                    <tr className="border-b border-border-base">
                      {['Servei', 'Tenant', 'Versió client', 'Versió actual', 'Desfassat des de', ''].map(h => (
                        <th key={h} className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {outdated.map((o: OutdatedServiceResponse) => (
                      <tr key={o.tenantServiceId} className="border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,255,255,0.02)] transition-colors">
                        <td className="px-4 sm:px-5 py-3">
                          <div className="f-display font-bold text-sm">{o.serviceName}</div>
                          <div className="f-mono text-xs text-ink-3">{o.serviceSlug}</div>
                        </td>
                        <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-2">
                          {o.tenantId.slice(0, 8)}…
                        </td>
                        <td className="px-4 sm:px-5 py-3">
                          <span className="f-mono text-xs bg-amber-500/10 text-amber-400 px-2 py-0.5 rounded">
                            v{o.lockedVersion}
                          </span>
                        </td>
                        <td className="px-4 sm:px-5 py-3">
                          <span className="f-mono text-xs bg-[rgba(255,255,255,0.05)] text-ink-1 px-2 py-0.5 rounded">
                            v{o.catalogVersion}
                          </span>
                        </td>
                        <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-2">
                          {new Date(o.outdatedAt).toLocaleDateString('ca-ES')}
                        </td>
                        <td className="px-4 sm:px-5 py-3">
                          <button
                            onClick={() => doAcknowledge({ tenantId: o.tenantId, serviceId: o.serviceId })}
                            className="f-mono text-[10px] uppercase text-ink-2 hover:text-green-400 border border-border-base hover:border-green-500/40 px-2 py-1 transition-colors"
                          >
                            Revisat
                          </button>
                        </td>
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
