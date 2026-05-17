'use client';

import { useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@/lib/toast-context';
import {
  getProfile, updateProfile,
  addPhaseToProfile, updatePhase, deletePhase,
  addServiceToPhase, addServiceToProfile, deleteService, listCatalogServices,
  type CatalogProfileResponse,
  type CatalogPhaseResponse,
  type CatalogServiceDetail,
} from '@/services/admin';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { I } from '@/components/ui/icons';
import { profileFormSchema, serviceFormSchema, phaseNameSchema, sortOrderSchema } from '@/lib/validations/vault';
import { FieldError } from '@/components/ui/field-error';

const SERVICE_TYPES = ['CREDENTIALS', 'LANDING', 'AUTOMATION', 'BILLING', 'OTHER'];

function EditProfileModal({ profile, onClose, onUpdated }: { profile: CatalogProfileResponse; onClose: () => void; onUpdated: () => void }) {
  const { toast } = useToast();
  const [form, setForm] = useState({ name: profile.name, slug: profile.slug, description: profile.description || '' });
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
      await updateProfile(profile.id, form);
      toast('success', 'Perfil actualitzat');
      onUpdated();
      onClose();
    } catch {
      toast('error', 'Error actualitzant el perfil');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-md p-6 space-y-4" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Editar perfil</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><I.X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Nom</label>
            <input type="text" required value={form.name} onChange={(e) => setForm(f => ({ ...f, name: e.target.value }))}
              className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 focus:outline-none focus:border-[#FF6B00]" />
            <FieldError error={errors.name} />
          </div>
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Slug</label>
            <input type="text" required value={form.slug} onChange={(e) => setForm(f => ({ ...f, slug: e.target.value }))}
              className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 focus:outline-none focus:border-[#FF6B00]" />
            <FieldError error={errors.slug} />
          </div>
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Descripció</label>
            <textarea value={form.description} onChange={(e) => setForm(f => ({ ...f, description: e.target.value }))}
              className="w-full bg-[#0d0d1a] border border-border-base px-3 py-2 h-20 text-sm text-ink-0 focus:outline-none focus:border-[#FF6B00] resize-none" />
          </div>
          <div className="flex gap-3 pt-2">
            <AMGButton type="submit" disabled={loading} className="flex-1 justify-center">{loading ? 'Guardant...' : 'Guardar'}</AMGButton>
            <AMGButton type="button" variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
          </div>
        </form>
      </div>
    </div>
  );
}

function AddPhaseModal({ profileId, onClose, onCreated }: { profileId: string; onClose: () => void; onCreated: () => void }) {
  const { toast } = useToast();
  const [name, setName] = useState('');
  const [sortOrder, setSortOrder] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrors({});
    const nameResult = phaseNameSchema.safeParse(name);
    const sortResult = sortOrderSchema.safeParse(sortOrder);
    const fieldErrors: Record<string, string> = {};
    if (!nameResult.success) fieldErrors.name = nameResult.error.issues[0].message;
    if (!sortResult.success) fieldErrors.sortOrder = sortResult.error.issues[0].message;
    if (Object.keys(fieldErrors).length > 0) { setErrors(fieldErrors); return; }
    setLoading(true);
    try {
      await addPhaseToProfile(profileId, { name, sortOrder: sortOrder ? parseInt(sortOrder) : undefined });
      toast('success', 'Fase afegida');
      onCreated();
      onClose();
    } catch {
      toast('error', 'Error afegint la fase');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-md p-6 space-y-4" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Nova fase</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><I.X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Nom de la fase</label>
            <input type="text" required placeholder="Ex: Configuració bàsica"
              value={name} onChange={(e) => setName(e.target.value)}
              className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
            <FieldError error={errors.name} />
          </div>
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Ordre</label>
            <input type="number" placeholder="1"
              value={sortOrder} onChange={(e) => setSortOrder(e.target.value)}
              className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
            <FieldError error={errors.sortOrder} />
          </div>
          <div className="flex gap-3 pt-2">
            <AMGButton type="submit" disabled={loading} className="flex-1 justify-center">{loading ? 'Afegint...' : 'Afegir fase'}</AMGButton>
            <AMGButton type="button" variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
          </div>
        </form>
      </div>
    </div>
  );
}

function AddServiceToPhaseModal({ phaseId, onClose, onCreated }: { phaseId: string; onClose: () => void; onCreated: () => void }) {
  const { toast } = useToast();
  const { data: catalogServices } = useQuery({
    queryKey: ['vault-services'],
    queryFn: () => listCatalogServices(),
  });

  const [form, setForm] = useState({
    name: '', slug: '', description: '', type: 'LANDING',
    cost: '', salePrice: '', sortOrder: '',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [selectedServiceId, setSelectedServiceId] = useState('');
  const [loading, setLoading] = useState(false);
  const [mode, setMode] = useState<'select' | 'create'>('select');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrors({});
    setLoading(true);
    try {
      if (mode === 'select' && selectedServiceId) {
        const svc = catalogServices?.find(s => s.id === selectedServiceId);
        if (!svc) { toast('error', 'Servei no trobat'); setLoading(false); return; }
        await addServiceToPhase(phaseId, {
          name: svc.name, slug: svc.slug,
          description: svc.description,
          type: svc.type, cost: svc.cost, salePrice: svc.salePrice,
        });
      } else {
        const result = serviceFormSchema.safeParse(form);
        if (!result.success) {
          const fieldErrors: Record<string, string> = {};
          for (const issue of result.error.issues) {
            const field = issue.path[0] as string;
            if (!fieldErrors[field]) fieldErrors[field] = issue.message;
          }
          setErrors(fieldErrors);
          setLoading(false);
          return;
        }
        await addServiceToPhase(phaseId, {
          name: form.name, slug: form.slug,
          description: form.description || undefined,
          type: form.type,
          cost: parseFloat(form.cost) || 0,
          salePrice: parseFloat(form.salePrice) || 0,
          sortOrder: form.sortOrder ? parseInt(form.sortOrder) : undefined,
        });
      }
      toast('success', 'Servei afegit a la fase');
      onCreated();
      onClose();
    } catch {
      toast('error', 'Error afegint el servei');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-md p-6 space-y-4" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Afegir servei a la fase</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><I.X size={18} /></button>
        </div>

        <div className="flex gap-2">
          <button onClick={() => setMode('select')}
            className={`f-mono text-xs uppercase px-3 py-1.5 transition ${mode === 'select' ? 'bg-accent text-black' : 'bg-bg-2 text-ink-2 hover:text-ink-0'}`}>
            Del catàleg
          </button>
          <button onClick={() => setMode('create')}
            className={`f-mono text-xs uppercase px-3 py-1.5 transition ${mode === 'create' ? 'bg-accent text-black' : 'bg-bg-2 text-ink-2 hover:text-ink-0'}`}>
            Nou servei
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {mode === 'select' ? (
            <div>
              <label className="f-mono text-label uppercase text-ink-2 block mb-1">Servei</label>
              {!catalogServices || catalogServices.length === 0 ? (
                <p className="text-sm text-ink-3">No hi ha serveis al catàleg. Crea'n un de nou.</p>
              ) : (
                <select value={selectedServiceId} onChange={(e) => setSelectedServiceId(e.target.value)} required
                  className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 focus:outline-none focus:border-[#FF6B00]">
                  <option value="">Selecciona un servei...</option>
                  {catalogServices.filter(s => !s.isAddon).map(s => (
                    <option key={s.id} value={s.id}>{s.name} — {s.salePrice.toFixed(2)} €</option>
                  ))}
                </select>
              )}
            </div>
          ) : (
            <>
              <div>
                <label className="f-mono text-label uppercase text-ink-2 block mb-1">Nom</label>
                <input type="text" required placeholder="Ex: Landing Pro" value={form.name}
                  onChange={(e) => setForm(f => ({ ...f, name: e.target.value }))}
                  className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
                <FieldError error={errors.name} />
              </div>
              <div>
                <label className="f-mono text-label uppercase text-ink-2 block mb-1">Slug</label>
                <input type="text" required placeholder="landing-pro" value={form.slug}
                  onChange={(e) => setForm(f => ({ ...f, slug: e.target.value }))}
                  className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
                <FieldError error={errors.slug} />
              </div>
              <div>
                <label className="f-mono text-label uppercase text-ink-2 block mb-1">Tipus</label>
                <select value={form.type} onChange={(e) => setForm(f => ({ ...f, type: e.target.value }))}
                  className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 focus:outline-none focus:border-[#FF6B00]">
                  {SERVICE_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="f-mono text-label uppercase text-ink-2 block mb-1">Cost (€)</label>
                  <input type="number" step="0.01" required placeholder="0" value={form.cost}
                    onChange={(e) => setForm(f => ({ ...f, cost: e.target.value }))}
                    className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
                  <FieldError error={errors.cost} />
                </div>
                <div>
                  <label className="f-mono text-label uppercase text-ink-2 block mb-1">Preu venda (€)</label>
                  <input type="number" step="0.01" required placeholder="0" value={form.salePrice}
                    onChange={(e) => setForm(f => ({ ...f, salePrice: e.target.value }))}
                    className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
                  <FieldError error={errors.salePrice} />
                </div>
              </div>
            </>
          )}
          <div className="flex gap-3 pt-2">
            <AMGButton type="submit" disabled={loading} className="flex-1 justify-center">{loading ? 'Afegint...' : 'Afegir servei'}</AMGButton>
            <AMGButton type="button" variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
          </div>
        </form>
      </div>
    </div>
  );
}

function AddDirectServiceModal({ profileId, onClose, onCreated }: { profileId: string; onClose: () => void; onCreated: () => void }) {
  const { toast } = useToast();
  const { data: catalogServices } = useQuery({
    queryKey: ['vault-services'],
    queryFn: () => listCatalogServices(),
  });

  const [form, setForm] = useState({
    name: '', slug: '', description: '', type: 'LANDING',
    cost: '', salePrice: '',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [selectedServiceId, setSelectedServiceId] = useState('');
  const [loading, setLoading] = useState(false);
  const [mode, setMode] = useState<'select' | 'create'>('select');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrors({});
    setLoading(true);
    try {
      if (mode === 'select' && selectedServiceId) {
        const svc = catalogServices?.find(s => s.id === selectedServiceId);
        if (!svc) { toast('error', 'Servei no trobat'); setLoading(false); return; }
        await addServiceToProfile(profileId, {
          name: svc.name, slug: svc.slug,
          description: svc.description,
          type: svc.type, cost: svc.cost, salePrice: svc.salePrice,
        });
      } else {
        const result = serviceFormSchema.safeParse(form);
        if (!result.success) {
          const fieldErrors: Record<string, string> = {};
          for (const issue of result.error.issues) {
            const field = issue.path[0] as string;
            if (!fieldErrors[field]) fieldErrors[field] = issue.message;
          }
          setErrors(fieldErrors);
          setLoading(false);
          return;
        }
        await addServiceToProfile(profileId, {
          name: form.name, slug: form.slug,
          description: form.description || undefined,
          type: form.type,
          cost: parseFloat(form.cost) || 0,
          salePrice: parseFloat(form.salePrice) || 0,
        });
      }
      toast('success', 'Servei afegit al perfil');
      onCreated();
      onClose();
    } catch {
      toast('error', 'Error afegint el servei');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-md p-6 space-y-4" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Afegir servei directe</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><I.X size={18} /></button>
        </div>

        <div className="flex gap-2">
          <button onClick={() => setMode('select')}
            className={`f-mono text-xs uppercase px-3 py-1.5 transition ${mode === 'select' ? 'bg-accent text-black' : 'bg-bg-2 text-ink-2 hover:text-ink-0'}`}>
            Del catàleg
          </button>
          <button onClick={() => setMode('create')}
            className={`f-mono text-xs uppercase px-3 py-1.5 transition ${mode === 'create' ? 'bg-accent text-black' : 'bg-bg-2 text-ink-2 hover:text-ink-0'}`}>
            Nou servei
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {mode === 'select' ? (
            <div>
              <label className="f-mono text-label uppercase text-ink-2 block mb-1">Servei</label>
              {!catalogServices || catalogServices.length === 0 ? (
                <p className="text-sm text-ink-3">No hi ha serveis al catàleg. Crea'n un de nou.</p>
              ) : (
                <select value={selectedServiceId} onChange={(e) => setSelectedServiceId(e.target.value)} required
                  className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 focus:outline-none focus:border-[#FF6B00]">
                  <option value="">Selecciona un servei...</option>
                  {catalogServices.map(s => (
                    <option key={s.id} value={s.id}>{s.name} — {s.salePrice.toFixed(2)} €</option>
                  ))}
                </select>
              )}
            </div>
          ) : (
            <>
              <div>
                <label className="f-mono text-label uppercase text-ink-2 block mb-1">Nom</label>
                <input type="text" required placeholder="Ex: SMTP Corporatiu" value={form.name}
                  onChange={(e) => setForm(f => ({ ...f, name: e.target.value }))}
                  className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
                <FieldError error={errors.name} />
              </div>
              <div>
                <label className="f-mono text-label uppercase text-ink-2 block mb-1">Slug</label>
                <input type="text" required placeholder="smtp-corporatiu" value={form.slug}
                  onChange={(e) => setForm(f => ({ ...f, slug: e.target.value }))}
                  className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
                <FieldError error={errors.slug} />
              </div>
              <div>
                <label className="f-mono text-label uppercase text-ink-2 block mb-1">Tipus</label>
                <select value={form.type} onChange={(e) => setForm(f => ({ ...f, type: e.target.value }))}
                  className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 focus:outline-none focus:border-[#FF6B00]">
                  {SERVICE_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="f-mono text-label uppercase text-ink-2 block mb-1">Cost (€)</label>
                  <input type="number" step="0.01" required placeholder="0" value={form.cost}
                    onChange={(e) => setForm(f => ({ ...f, cost: e.target.value }))}
                    className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
                  <FieldError error={errors.cost} />
                </div>
                <div>
                  <label className="f-mono text-label uppercase text-ink-2 block mb-1">Preu venda (€)</label>
                  <input type="number" step="0.01" required placeholder="0" value={form.salePrice}
                    onChange={(e) => setForm(f => ({ ...f, salePrice: e.target.value }))}
                    className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
                  <FieldError error={errors.salePrice} />
                </div>
              </div>
            </>
          )}
          <div className="flex gap-3 pt-2">
            <AMGButton type="submit" disabled={loading} className="flex-1 justify-center">{loading ? 'Afegint...' : 'Afegir servei'}</AMGButton>
            <AMGButton type="button" variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
          </div>
        </form>
      </div>
    </div>
  );
}

function PhaseCard({ phase, profileId, onUpdated, onDeleted, onAddService }: {
  phase: CatalogPhaseResponse; profileId: string;
  onUpdated: () => void; onDeleted: () => void;
  onAddService: (phaseId: string) => void;
}) {
  const { toast } = useToast();
  const [editing, setEditing] = useState(false);
  const [editName, setEditName] = useState(phase.name);
  const [editSort, setEditSort] = useState(String(phase.sortOrder ?? ''));

  const handleUpdate = async () => {
    const nameResult = phaseNameSchema.safeParse(editName);
    const sortResult = sortOrderSchema.safeParse(editSort);
    if (!nameResult.success || !sortResult.success) {
      toast('error', 'Nom: mínim 2 caràcters | Ordre: número positiu');
      return;
    }
    try {
      await updatePhase(profileId, phase.id, { name: editName, sortOrder: editSort ? parseInt(editSort) : undefined });
      toast('success', 'Fase actualitzada');
      onUpdated();
      setEditing(false);
    } catch {
      toast('error', 'Error actualitzant la fase');
    }
  };

  const handleDelete = async () => {
    if (!confirm('Eliminar aquesta fase i els seus serveis?')) return;
    try {
      await deletePhase(profileId, phase.id);
      toast('success', 'Fase eliminada');
      onDeleted();
    } catch {
      toast('error', 'Error eliminant la fase');
    }
  };

  const totalPrice = phase.services?.reduce((sum, s) => sum + (s.salePrice || 0), 0) ?? 0;

  return (
    <div className="border border-border-base rounded">
      <div className="p-4 bg-[rgba(255,255,255,0.02)] border-b border-border-base">
        <div className="flex items-center justify-between gap-3">
          {editing ? (
            <div className="flex items-center gap-2 flex-1">
              <input type="text" value={editName} onChange={(e) => setEditName(e.target.value)}
                className="bg-[#0d0d1a] border border-border-base px-2 h-8 text-sm text-ink-0 focus:outline-none focus:border-[#FF6B00] flex-1" />
              <input type="number" value={editSort} onChange={(e) => setEditSort(e.target.value)} placeholder="Ordre"
                className="bg-[#0d0d1a] border border-border-base px-2 h-8 text-sm text-ink-0 w-16 focus:outline-none focus:border-[#FF6B00]" />
              <AMGButton size="sm" onClick={handleUpdate}>Guardar</AMGButton>
              <AMGButton size="sm" variant="ghost" onClick={() => setEditing(false)}>Cancel·lar</AMGButton>
            </div>
          ) : (
            <>
              <div className="flex items-center gap-3 min-w-0">
                <div className="w-6 h-6 rounded-full bg-accent-muted border border-accent/30 flex items-center justify-center text-accent-light f-mono text-xs shrink-0">
                  {phase.sortOrder ?? '?'}
                </div>
                <div>
                  <span className="f-display font-bold text-sm">{phase.name}</span>
                  {phase.services && <span className="f-mono text-xs text-ink-3 ml-2">{phase.services.length} serveis</span>}
                </div>
              </div>
              <div className="flex items-center gap-2 shrink-0">
                <span className="f-mono text-xs text-ink-2">{totalPrice.toFixed(2)} €</span>
                <button onClick={() => { setEditing(true); setEditName(phase.name); setEditSort(String(phase.sortOrder ?? '')); }}
                  className="text-ink-2 hover:text-accent-light transition"><I.Edit size={14} /></button>
                <button onClick={handleDelete} className="text-ink-2 hover:text-danger transition"><I.Trash size={14} /></button>
              </div>
            </>
          )}
        </div>
      </div>

      {/* Services in this phase */}
      <div className="p-4 space-y-2">
        {(!phase.services || phase.services.length === 0) ? (
          <div className="text-sm text-ink-3 text-center py-2">Cap servei en aquesta fase</div>
        ) : (
          phase.services.map((svc) => (
            <div key={svc.id} className="flex items-center justify-between gap-3 px-3 py-2 bg-[rgba(255,255,255,0.02)] rounded">
              <div className="flex items-center gap-2 min-w-0">
                <span className="w-1.5 h-1.5 rounded-full bg-accent shrink-0" />
                <span className="text-sm text-ink-0">{svc.name}</span>
                <AMGBadge tone="neutral">{svc.type}</AMGBadge>
              </div>
              <div className="f-mono text-xs text-ink-2 shrink-0">{svc.salePrice?.toFixed(2)} €</div>
            </div>
          ))
        )}
        <AMGButton size="sm" variant="outline" onClick={() => onAddService(phase.id)} icon={I.Plus}>
          Afegir servei
        </AMGButton>
      </div>
    </div>
  );
}

export default function ProfileDetailPage() {
  const { id } = useParams<{ id: string }>();
  const qc = useQueryClient();
  const router = useRouter();

  const { data: profile, isLoading, error } = useQuery({
    queryKey: ['vault-profile', id],
    queryFn: () => getProfile(id),
  });

  const [showEditProfile, setShowEditProfile] = useState(false);
  const [showAddPhase, setShowAddPhase] = useState(false);
  const [showAddDirectService, setShowAddDirectService] = useState(false);
  const [addServicePhaseId, setAddServicePhaseId] = useState<string | null>(null);

  const invalidate = () => qc.invalidateQueries({ queryKey: ['vault-profile', id] });

  const phaseServiceCount = profile?.phases?.reduce((a, p) => a + (p.services?.length ?? 0), 0) ?? 0;
  const directServiceCount = profile?.directServices?.length ?? 0;
  const totalServiceCount = phaseServiceCount + directServiceCount;
  const totalPrice = (profile?.phases?.reduce((a, p) => a + (p.services?.reduce((s, sv) => s + (sv.salePrice || 0), 0) ?? 0), 0) ?? 0)
    + (profile?.directServices?.reduce((s, sv) => s + (sv.salePrice || 0), 0) ?? 0);

  if (isLoading) {
    return (
      <PortalShell breadcrumb="admin · catàleg · carregant">
        <div className="flex justify-center py-12">
          <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
        </div>
      </PortalShell>
    );
  }

  if (error || !profile) {
    return (
      <PortalShell breadcrumb="admin · catàleg · error">
        <div className="p-8 text-center">
          <I.AlertCircle size={28} stroke="#ff6666" className="mx-auto mb-3" />
          <div className="f-display font-bold text-sm mb-1">Error carregant el perfil</div>
          <AMGButton size="sm" onClick={() => router.push('/portal/admin/vault')}>Tornar al catàleg</AMGButton>
        </div>
      </PortalShell>
    );
  }

  return (
    <PortalShell breadcrumb={`admin · catàleg · ${profile.name}`}>
      <div className="p-4 sm:p-8 space-y-6">
        {/* Header */}
        <div className="flex items-start justify-between gap-4">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / catàleg / perfils /</span>
            <div className="flex items-center gap-3 mt-1">
              <div className="f-display font-bold text-xl">{profile.name}</div>
              {!profile.isActive && <AMGBadge tone="neutral">Inactiu</AMGBadge>}
            </div>
            <div className="flex items-center gap-3 mt-1 text-sm text-ink-2">
              <span className="f-mono text-xs text-ink-3">/{profile.slug}</span>
              {profile.description && <span>{profile.description}</span>}
            </div>
          </div>
          <AMGButton size="sm" variant="secondary" onClick={() => setShowEditProfile(true)} icon={I.Edit}>
            Editar perfil
          </AMGButton>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
          <div className="card-clip amg-card p-5">
            <span className="f-mono uppercase text-label tracking-widest text-ink-3">Fases</span>
            <div className="f-display font-bold text-2xl text-accent-light mt-2">{profile.phases?.length ?? 0}</div>
          </div>
          <div className="card-clip amg-card p-5">
            <span className="f-mono uppercase text-label tracking-widest text-ink-3">Serveis directes</span>
            <div className="f-display font-bold text-2xl text-accent-light mt-2">{directServiceCount}</div>
          </div>
          <div className="card-clip amg-card p-5">
            <span className="f-mono uppercase text-label tracking-widest text-ink-3">Serveis totals</span>
            <div className="f-display font-bold text-2xl text-accent-light mt-2">{totalServiceCount}</div>
          </div>
          <div className="card-clip amg-card p-5">
            <span className="f-mono uppercase text-label tracking-widest text-ink-3">Preu total</span>
            <div className="f-display font-bold text-2xl text-accent-light mt-2">{totalPrice.toFixed(2)} €</div>
          </div>
        </div>

        {/* Direct Services */}
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
            <div>
              <div className="f-mono text-label uppercase text-ink-3 text-xs tracking-widest">Serveis directes</div>
              <div className="f-display font-bold text-sm mt-0.5">Serveis sense fase assignada</div>
            </div>
            <AMGButton size="sm" icon={I.Plus} onClick={() => setShowAddDirectService(true)}>Afegir servei</AMGButton>
          </div>
          <div className="p-4 sm:p-5">
            {!profile.directServices || profile.directServices.length === 0 ? (
              <div className="text-center py-8">
                <I.Box size={28} stroke="#64748b" className="mx-auto mb-3" />
                <div className="f-display font-bold text-sm mb-1">Cap servei directe</div>
                <p className="f-mono text-xs text-ink-2">Afegeix serveis que no requereixin fase</p>
              </div>
            ) : (
              <div className="space-y-2">
                {profile.directServices.map((svc) => (
                  <div key={svc.id} className="flex items-center justify-between gap-3 px-4 py-3 bg-[rgba(255,255,255,0.02)] rounded border border-border-base">
                    <div className="flex items-center gap-3 min-w-0">
                      <span className="w-1.5 h-1.5 rounded-full bg-accent shrink-0" />
                      <div>
                        <div className="text-sm text-ink-0">{svc.name}</div>
                        <div className="f-mono text-xs text-ink-3">/{svc.slug}</div>
                      </div>
                      <AMGBadge tone="neutral">{svc.type}</AMGBadge>
                    </div>
                    <div className="flex items-center gap-3 shrink-0">
                      <span className="f-mono text-xs text-ink-2">{svc.salePrice?.toFixed(2)} €</span>
                      <button
                        onClick={() => { if (confirm(`Eliminar "${svc.name}" del perfil?`)) deleteService(svc.id).then(() => invalidate()); }}
                        className="text-ink-2 hover:text-danger transition">
                        <I.X size={14} />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Phases */}
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
            <div>
              <div className="f-mono text-label uppercase text-ink-3 text-xs tracking-widest">Fases</div>
              <div className="f-display font-bold text-sm mt-0.5">Fases i serveis del perfil</div>
            </div>
            <AMGButton size="sm" icon={I.Plus} onClick={() => setShowAddPhase(true)}>Nova fase</AMGButton>
          </div>
          <div className="p-4 sm:p-5 space-y-4">
            {(!profile.phases || profile.phases.length === 0) ? (
              <div className="text-center py-8">
                <I.Box size={28} stroke="#64748b" className="mx-auto mb-3" />
                <div className="f-display font-bold text-sm mb-1">Cap fase definida</div>
                <p className="f-mono text-xs text-ink-2">Afegeix la primera fase al perfil</p>
              </div>
            ) : (
              [...profile.phases]
                .sort((a, b) => (a.sortOrder ?? 99) - (b.sortOrder ?? 99))
                .map((phase) => (
                  <PhaseCard key={phase.id} phase={phase} profileId={profile.id}
                    onUpdated={invalidate} onDeleted={invalidate}
                    onAddService={(pid) => setAddServicePhaseId(pid)} />
                ))
            )}
          </div>
        </div>
      </div>

      {showEditProfile && <EditProfileModal profile={profile} onClose={() => setShowEditProfile(false)} onUpdated={invalidate} />}
      {showAddPhase && <AddPhaseModal profileId={profile.id} onClose={() => setShowAddPhase(false)} onCreated={invalidate} />}
      {showAddDirectService && <AddDirectServiceModal profileId={profile.id} onClose={() => setShowAddDirectService(false)} onCreated={invalidate} />}
      {addServicePhaseId && (
        <AddServiceToPhaseModal phaseId={addServicePhaseId}
          onClose={() => setAddServicePhaseId(null)} onCreated={invalidate} />
      )}
    </PortalShell>
  );
}
