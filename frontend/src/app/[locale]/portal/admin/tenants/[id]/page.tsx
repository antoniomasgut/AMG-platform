'use client';

import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'next/navigation';
import { useToast } from '@/lib/toast-context';
import {
  getTenant, getTenantSetup, listCatalogServices,
  listProfiles, assignProfileToTenant, removeProfileFromTenant,
  assignPhaseToTenant, addStandaloneServiceToTenant, removeTenantService,
  lookupSectorPricing, updateTenant, toggleTenantService,
  getAgentChannels, updateAgentChannels, getAIConfig, updateAIConfig, getAvailableModels,
  getGoCardlessConfig, getGoCardlessMandate, initiateGoCardlessMandate, cancelGoCardlessMandate,
  listGoCardlessPayments, configureGoCardless,
  SECTOR_LABELS, SIZE_LABELS, SECTOR_SIZES, PHASE_LABELS, PHASE_UPGRADE_PRICE,
  listSectorPhases,
  type SectorPhaseResponse,
  type TenantResponse, type TenantSetup, type CatalogService, type CatalogPhaseResponse,
  type CatalogProfileResponse, type SectorPricingResponse, type ChannelsConfig, type AIConfig, type ModelInfo,
  type GoCardlessConfig, type GoCardlessMandate,
  type WhatsAppWabaConfig,
  getWhatsAppConfig, connectWhatsApp, verifyWhatsApp, disconnectWhatsApp, sendWhatsAppTest,
  getTelegramConfig, connectTelegram, verifyTelegram, disconnectTelegram,
  type TelegramConfig,
  checkTenantDeletion, deleteTenant,
  type DeleteTenantCheck,
  calcMonthly,
} from '@/services/admin';
import { createBudget, listBudgets, sendBudget, cancelBudget, updateBudget, type BudgetResponse, type CreateBudgetRequest } from '@/services/billing';
import { listLandings } from '@/services/factory';
import { getWizardConfig } from '@/config/service-wizards';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGSectionTitle } from '@/components/ui/stat';
import { I } from '@/components/ui/icons';

function fmtDate(d: string | null) {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

function fmt(n: number) {
  return new Intl.NumberFormat('ca-ES', { style: 'currency', currency: 'EUR' }).format(n);
}

const NEXE_PHASE_NAMES: Record<number, string> = {
  1: 'Comunicació 24/7',
  2: 'Gestió de cites',
  3: 'Pressupostos',
  4: 'Fidelització',
  5: 'Equip',
};

const WORKER_ADDONS: Record<string, { setup: number; monthly: number }> = {
  AUTONOMO: { setup: 0, monthly: 0 },
  PETIT:    { setup: 99, monthly: 20 },
  MITJA:    { setup: 199, monthly: 35 },
  EMPRESA:  { setup: 299, monthly: 50 },
};

const DEP_BADGE: Record<string, string> = {
  BASE: '🔵', REQUIRED: '🔴', OPTIONAL: '🟡',
};

function statusBadge(status: string, activeLabel: string, inactiveLabel: string) {
  return status === 'APPROVED' || status === 'ACTIVE' || status === 'COMPLETED'
    ? <AMGBadge tone="success">{activeLabel}</AMGBadge>
    : status === 'PENDING' || status === 'REJECTED'
    ? <AMGBadge tone="warning">{status === 'REJECTED' ? 'Rebutjat' : 'Pendent'}</AMGBadge>
    : <AMGBadge tone="neutral">{inactiveLabel}</AMGBadge>;
}

function ServiceCatalogTable({ services }: { services: CatalogService[] }) {
  const [query, setQuery] = useState('');
  const [typeFilter, setTypeFilter] = useState<'ALL' | 'RECURRING' | 'ONE_TIME'>('ALL');
  const [addonOnly, setAddonOnly] = useState(false);

  const filtered = services.filter((s) => {
    const q = query.toLowerCase();
    const matchesQuery = !q || s.name.toLowerCase().includes(q) || s.slug.toLowerCase().includes(q);
    const matchesType = typeFilter === 'ALL' || s.type === typeFilter;
    const matchesAddon = !addonOnly || s.isAddon;
    return matchesQuery && matchesType && matchesAddon;
  });

  const filterBtn = (label: string, active: boolean, onClick: () => void) => (
    <button type="button" onClick={onClick}
      className={`px-3 py-1.5 rounded text-xs f-mono transition border ${
        active
          ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.12)] text-white'
          : 'border-border-base text-ink-2 hover:border-ink-2'
      }`}>
      {label}
    </button>
  );

  return (
    <div>
      {/* Toolbar */}
      <div className="flex flex-wrap items-center gap-3 px-4 sm:px-5 py-3 border-b border-border-base">
        <div className="relative flex-1 min-w-[180px] max-w-xs">
          <I.Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-3" />
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Cercar servei..."
            className="w-full pl-8 pr-3 py-1.5 bg-[rgba(255,255,255,0.04)] border border-border-base rounded text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00] placeholder:text-ink-3"
          />
        </div>
        <div className="flex items-center gap-2">
          {filterBtn('Tots', typeFilter === 'ALL', () => setTypeFilter('ALL'))}
          {filterBtn('Recurrent', typeFilter === 'RECURRING', () => setTypeFilter('RECURRING'))}
          {filterBtn('Únic', typeFilter === 'ONE_TIME', () => setTypeFilter('ONE_TIME'))}
          {filterBtn('Addon', addonOnly, () => setAddonOnly(v => !v))}
        </div>
        {(query || typeFilter !== 'ALL' || addonOnly) && (
          <span className="f-mono text-xs text-ink-3">{filtered.length} / {services.length}</span>
        )}
      </div>

      {filtered.length === 0 ? (
        <p className="text-sm text-ink-2 px-5 py-6">Cap servei coincideix amb el filtre.</p>
      ) : (
        <table className="w-full min-w-[500px]">
          <thead>
            <tr className="border-b border-border-base">
              {['Servei', 'Tipus', 'Preu venda', 'Addon'].map((h) => (
                <th key={h} className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {filtered.map((s) => (
              <tr key={s.id} className="border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,255,255,0.02)] transition-colors">
                <td className="px-4 sm:px-5 py-3">
                  <div className="f-display font-bold text-sm">{s.name}</div>
                  <div className="f-mono text-xs text-ink-3 mt-0.5">{s.slug}</div>
                </td>
                <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-2 capitalize">{s.type.toLowerCase()}</td>
                <td className="px-4 sm:px-5 py-3 f-mono text-xs text-ink-1">{s.salePrice.toFixed(2)} €</td>
                <td className="px-4 sm:px-5 py-3">{s.isAddon ? <AMGBadge tone="info">Addon</AMGBadge> : '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

function ServiceToggle({ tenantId, serviceId, enabled, onToggle }: {
  tenantId: string; serviceId: string; enabled: boolean; onToggle: () => void;
}) {
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();

  const handleToggle = async () => {
    setLoading(true);
    try {
      await toggleTenantService(tenantId, serviceId);
      onToggle();
    } catch {
      toast('error', 'Error canviant l\'estat del servei');
    } finally {
      setLoading(false);
    }
  };

  return (
    <button
      type="button"
      onClick={handleToggle}
      disabled={loading}
      title={enabled ? 'Desactivar servei' : 'Activar servei'}
      className={`flex-shrink-0 w-9 h-5 rounded-full transition-colors relative ${
        enabled ? 'bg-[#FF6B00]' : 'bg-[rgba(255,255,255,0.12)]'
      } ${loading ? 'opacity-50 cursor-not-allowed' : ''}`}
    >
      <div className={`absolute top-0.5 w-4 h-4 rounded-full bg-white shadow transition-all ${enabled ? 'left-4' : 'left-0.5'}`} />
    </button>
  );
}

function SetupSection({ setup, tenantId, onRefresh }: { setup: TenantSetup; tenantId: string; onRefresh: () => void }) {
  const hasProfiles = setup.profiles.length > 0;
  const hasAddons = setup.addons.length > 0;
  const hasStandalone = (setup.standalone?.length ?? 0) > 0;
  const { toast } = useToast();
  const [removingId, setRemovingId] = useState<string | null>(null);

  const handleRemove = async (tenantServiceId: string) => {
    setRemovingId(tenantServiceId);
    try {
      await removeTenantService(tenantId, tenantServiceId);
      onRefresh();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      toast('error', `Error eliminant el servei: ${msg}`);
    } finally {
      setRemovingId(null);
    }
  };

  const canRemove = (status: string, isEnabled: boolean) => !isEnabled || status !== 'VERIFIED';

  if (!hasProfiles && !hasAddons && !hasStandalone) {
    return (
      <div className="p-8 text-center">
        <I.Box size={28} stroke="#64748b" className="mx-auto mb-3" />
        <div className="f-display font-bold text-sm mb-1">Cap servei assignat</div>
        <p className="f-mono text-xs text-ink-2">Aquest tenant encara no té perfils ni serveis assignats</p>
      </div>
    );
  }

  const ServiceRow = ({ svc }: { svc: TenantSetup['profiles'][0]['phases'][0]['services'][0] }) => {
    const isPending = svc.status === 'PENDING' || svc.status === 'CONFIGURING' || svc.status === 'AWAITING_CLIENT';
    return (
      <div className={`flex items-center gap-2 pl-2 transition-opacity ${!svc.isEnabled ? 'opacity-40' : ''}`}>
        <ServiceToggle tenantId={tenantId} serviceId={svc.service.id} enabled={svc.isEnabled} onToggle={onRefresh} />
        <span className="text-sm text-ink-1">{svc.service.name}</span>
        <span className="f-mono text-[10px] text-ink-3 uppercase">{svc.service.type}</span>
        {statusBadge(svc.status, 'Actiu', 'Inactiu')}
        {isPending && getWizardConfig(svc.service.slug, svc.service.type) && (
          <a href={`/portal/admin/tenants/${tenantId}/services/${svc.service.id}/setup`}
            className="ml-auto text-[10px] f-mono uppercase text-accent-light hover:text-accent transition">
            Configurar
          </a>
        )}
        {canRemove(svc.status, svc.isEnabled) && (
          <button
            type="button"
            onClick={() => handleRemove(svc.tenantServiceId)}
            disabled={removingId === svc.tenantServiceId}
            title="Eliminar servei"
            className="ml-auto text-ink-3 hover:text-red-400 transition disabled:opacity-40"
          >
            {removingId === svc.tenantServiceId
              ? <span className="w-3 h-3 border border-current border-t-transparent rounded-full animate-spin inline-block" />
              : <I.Trash size={13} />}
          </button>
        )}
      </div>
    );
  };

  return (
    <div className="space-y-6 p-5">
      {setup.profiles.map((p) => (
        <div key={p.profile.id} className="border border-border-base rounded p-4 space-y-3">
          <div className="flex items-center gap-2">
            <I.Box size={14} className="text-accent-light" />
            <span className="f-display font-bold text-sm">{p.profile.name}</span>
          </div>
          {p.phases.map((ph) => (
            <div key={ph.phase.id} className="ml-5 border-l-2 border-border-base pl-4 space-y-2">
              <div className="flex items-center gap-2">
                <span className="f-mono text-label uppercase text-ink-3">{ph.phase.name}</span>
                {statusBadge(ph.approvalStatus, 'Aprovat', 'Pendent')}
              </div>
              {ph.services.map((svc) => (
                <ServiceRow key={svc.tenantServiceId} svc={svc} />
              ))}
            </div>
          ))}
        </div>
      ))}
      {hasStandalone && (
        <div className="border border-border-base rounded p-4 space-y-2">
          <div className="flex items-center gap-2">
            <I.Zap size={14} className="text-accent-light" />
            <span className="f-display font-bold text-sm">Serveis individuals</span>
          </div>
          {setup.standalone!.map((svc) => (
            <ServiceRow key={svc.tenantServiceId} svc={svc} />
          ))}
        </div>
      )}
      {hasAddons && (
        <div className="border border-border-base rounded p-4 space-y-2">
          <div className="flex items-center gap-2">
            <I.Plus size={14} className="text-accent-light" />
            <span className="f-display font-bold text-sm">Add-ons</span>
          </div>
          {setup.addons.map((a) => (
            <div key={a.service.id} className="flex items-center gap-2 pl-2">
              <span className="w-1.5 h-1.5 rounded-full bg-accent flex-shrink-0" />
              <span className="text-sm text-ink-1">{a.service.name}</span>
              {statusBadge(a.approvalStatus, 'Aprovat', 'No aprovat')}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function AssignProfileModal({ tenantId, onClose, onAssigned }: { tenantId: string; onClose: () => void; onAssigned: () => void }) {
  const { toast } = useToast();
  const { data: profiles, isLoading } = useQuery({
    queryKey: ['vault-profiles'],
    queryFn: () => listProfiles(),
  });

  const [selectedProfileId, setSelectedProfileId] = useState('');
  const [assigning, setAssigning] = useState(false);

  const activeProfiles = profiles?.filter(p => p.isActive) ?? [];

  const handleAssign = async () => {
    if (!selectedProfileId) return;
    setAssigning(true);
    try {
      const result = await assignProfileToTenant(tenantId, selectedProfileId);
      toast('success', `Perfil assignat — ${result.phases.length} fases, total ${result.totalPrice.toFixed(2)} €`);
      onAssigned();
      onClose();
    } catch {
      toast('error', 'Error assignant el perfil');
    } finally {
      setAssigning(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-md p-6 space-y-4" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Assignar perfil</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><I.X size={18} /></button>
        </div>
        {isLoading ? (
          <div className="flex justify-center py-8">
            <span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          </div>
        ) : activeProfiles.length === 0 ? (
          <div className="text-center py-6">
            <I.Box size={24} stroke="#64748b" className="mx-auto mb-2" />
            <div className="f-display font-bold text-sm mb-1">Cap perfil disponible</div>
            <p className="f-mono text-xs text-ink-2">Crea perfils des de la secció Catàleg</p>
          </div>
        ) : (
          <div className="space-y-3">
            <p className="text-sm text-ink-2">Selecciona un perfil per assignar-lo al tenant:</p>
            {activeProfiles.map((p: CatalogProfileResponse) => (
              <button key={p.id} onClick={() => setSelectedProfileId(p.id)}
                className={`w-full text-left p-3 border rounded transition ${
                  selectedProfileId === p.id
                    ? 'border-[#FF6B00] bg-accent-muted'
                    : 'border-border-base hover:border-ink-2'
                }`}>
                <div className="f-display font-bold text-sm">{p.name}</div>
                {p.description && <div className="f-mono text-xs text-ink-3 mt-0.5">{p.description}</div>}
                <div className="f-mono text-xs text-ink-2 mt-1">{p.phases?.length ?? 0} fases</div>
              </button>
            ))}
            <div className="flex gap-3 pt-2">
              <AMGButton onClick={handleAssign} disabled={!selectedProfileId || assigning} loading={assigning} className="flex-1 justify-center">
                Assignar perfil
              </AMGButton>
              <AMGButton variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function AddPhaseModal({ tenantId, onClose, onAdded }: { tenantId: string; onClose: () => void; onAdded: () => void }) {
  const { toast } = useToast();
  const { data: profiles, isLoading } = useQuery({
    queryKey: ['vault-profiles'],
    queryFn: () => listProfiles(),
  });
  const [selectedProfileId, setSelectedProfileId] = useState('');
  const [selectedPhaseIds, setSelectedPhaseIds] = useState<Set<string>>(new Set());
  const [adding, setAdding] = useState(false);

  const activeProfiles = profiles?.filter(p => p.isActive) ?? [];
  const selectedProfile = activeProfiles.find(p => p.id === selectedProfileId);

  const togglePhase = (id: string) => setSelectedPhaseIds(prev => {
    const next = new Set(prev);
    next.has(id) ? next.delete(id) : next.add(id);
    return next;
  });

  const handleAdd = async () => {
    if (selectedPhaseIds.size === 0) { toast('error', 'Selecciona almenys una fase'); return; }
    setAdding(true);
    try {
      await Promise.all(Array.from(selectedPhaseIds).map(phaseId => assignPhaseToTenant(tenantId, phaseId)));
      toast('success', `${selectedPhaseIds.size} fase${selectedPhaseIds.size > 1 ? 's' : ''} afegida${selectedPhaseIds.size > 1 ? 's' : ''}`);
      onAdded(); onClose();
    } catch {
      toast('error', 'Error afegint fases');
    } finally {
      setAdding(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-md p-6 space-y-4 max-h-[85vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Afegir fases</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><I.X size={18} /></button>
        </div>
        {isLoading ? (
          <div className="flex justify-center py-8"><span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" /></div>
        ) : activeProfiles.length === 0 ? (
          <p className="text-sm text-ink-3 text-center py-6">Cap perfil disponible al catàleg</p>
        ) : (
          <div className="space-y-4">
            <div>
              <label className="f-mono text-label uppercase text-ink-2 block mb-2">Perfil</label>
              <div className="space-y-2">
                {activeProfiles.map(p => (
                  <button key={p.id} type="button" onClick={() => { setSelectedProfileId(p.id); setSelectedPhaseIds(new Set()); }}
                    className={`w-full text-left p-3 border rounded transition text-sm ${
                      selectedProfileId === p.id ? 'border-[#FF6B00] bg-accent-muted' : 'border-border-base hover:border-ink-2'
                    }`}>
                    <span className="font-semibold">{p.name}</span>
                    <span className="text-ink-3 ml-2 text-xs">{p.phases?.length ?? 0} fases</span>
                  </button>
                ))}
              </div>
            </div>
            {selectedProfile && (
              <div>
                <label className="f-mono text-label uppercase text-ink-2 block mb-2">Fases</label>
                <div className="space-y-2">
                  {selectedProfile.phases.map((ph: CatalogPhaseResponse) => (
                    <label key={ph.id} className="flex items-center gap-3 p-3 border border-border-base rounded cursor-pointer hover:border-ink-2 transition">
                      <input type="checkbox" checked={selectedPhaseIds.has(ph.id)} onChange={() => togglePhase(ph.id)} className="accent-[#FF6B00]" />
                      <div className="flex-1">
                        <span className="text-sm">{ph.name}</span>
                        <span className="f-mono text-xs text-ink-3 ml-2">{ph.services.length} serveis</span>
                      </div>
                    </label>
                  ))}
                </div>
              </div>
            )}
            <div className="flex gap-3 pt-2">
              <AMGButton onClick={handleAdd} disabled={selectedPhaseIds.size === 0 || adding} loading={adding} className="flex-1 justify-center">
                Afegir fases ({selectedPhaseIds.size})
              </AMGButton>
              <AMGButton variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function AddServiceModal({ tenantId, onClose, onAdded }: { tenantId: string; onClose: () => void; onAdded: () => void }) {
  const { toast } = useToast();
  const { data: services, isLoading } = useQuery({
    queryKey: ['catalog-services'],
    queryFn: () => listCatalogServices(),
  });
  const [query, setQuery] = useState('');
  const [selectedId, setSelectedId] = useState('');
  const [adding, setAdding] = useState(false);

  const filtered = (services ?? []).filter(s => !query || s.name.toLowerCase().includes(query.toLowerCase()) || s.slug.toLowerCase().includes(query.toLowerCase()));

  const handleAdd = async () => {
    if (!selectedId) return;
    setAdding(true);
    try {
      await addStandaloneServiceToTenant(tenantId, selectedId);
      const name = services?.find(s => s.id === selectedId)?.name ?? 'Servei';
      toast('success', `${name} afegit`);
      onAdded(); onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error desconegut';
      toast('error', `Error afegint el servei: ${msg}`);
    } finally {
      setAdding(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-md p-6 space-y-4 max-h-[85vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Afegir servei individual</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><I.X size={18} /></button>
        </div>
        {isLoading ? (
          <div className="flex justify-center py-8"><span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" /></div>
        ) : (
          <div className="space-y-3">
            <div className="relative">
              <I.Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-3" />
              <input type="text" value={query} onChange={e => setQuery(e.target.value)}
                placeholder="Cercar servei..."
                className="w-full pl-8 pr-3 py-1.5 bg-[rgba(255,255,255,0.04)] border border-border-base rounded text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00] placeholder:text-ink-3" />
            </div>
            <div className="space-y-1.5 max-h-64 overflow-y-auto">
              {filtered.map(s => (
                <button key={s.id} type="button" onClick={() => setSelectedId(s.id)}
                  className={`w-full text-left p-2.5 border rounded transition ${
                    selectedId === s.id ? 'border-[#FF6B00] bg-accent-muted' : 'border-border-base hover:border-ink-2'
                  }`}>
                  <div className="text-sm font-semibold">{s.name}</div>
                  <div className="f-mono text-[10px] text-ink-3 mt-0.5">{s.slug} · {s.type.toLowerCase()} · {s.salePrice.toFixed(2)} €</div>
                </button>
              ))}
              {filtered.length === 0 && <p className="text-sm text-ink-3 text-center py-4">Cap servei trobat</p>}
            </div>
            <div className="flex gap-3 pt-2">
              <AMGButton onClick={handleAdd} disabled={!selectedId || adding} loading={adding} className="flex-1 justify-center">
                Afegir servei
              </AMGButton>
              <AMGButton variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function ContractSection({ tenant, onRefresh }: { tenant: TenantResponse; onRefresh: () => void }) {
  const { toast } = useToast();
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editSector, setEditSector] = useState(tenant.sector ?? '');
  const [editSize, setEditSize] = useState(tenant.businessSize ?? '');

  const { data: pricing } = useQuery({
    queryKey: ['pricing', tenant.sector, tenant.businessSize],
    queryFn: () => lookupSectorPricing(tenant.sector!, tenant.businessSize!),
    enabled: !!tenant.sector && !!tenant.businessSize,
  });

  const { data: editPricing } = useQuery({
    queryKey: ['pricing', editSector, editSize],
    queryFn: () => lookupSectorPricing(editSector, editSize),
    enabled: !!editSector && !!editSize,
  });

  const phases = tenant.contractedPhases ?? [];
  const phaseCount = phases.length;
  const monthlyPrice = pricing && phaseCount > 0 ? calcMonthly(pricing, phaseCount) : null;

  const handleSave = async () => {
    setSaving(true);
    try {
      await updateTenant(tenant.id, { sector: editSector || null, businessSize: editSize || null });
      toast('success', 'Contracte actualitzat');
      onRefresh();
      setEditing(false);
    } catch {
      toast('error', 'Error desant els canvis');
    } finally {
      setSaving(false);
    }
  };

  const availableSizes = editSector ? (SECTOR_SIZES[editSector] ?? []) : [];
  const lbl = 'f-mono text-[10px] uppercase tracking-wider text-ink-3 block mb-1.5';

  return (
    <div className="amg-card card-clip">
      <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
        <AMGSectionTitle eyebrow="Grandària" title="Contracte" />
        {!editing ? (
          <AMGButton size="sm" variant="ghost" icon={I.Edit} onClick={() => { setEditSector(tenant.sector ?? ''); setEditSize(tenant.businessSize ?? ''); setEditing(true); }}>
            Editar
          </AMGButton>
        ) : (
          <div className="flex gap-2">
            <AMGButton size="sm" variant="ghost" onClick={() => setEditing(false)}>Cancel·lar</AMGButton>
            <AMGButton size="sm" loading={saving} onClick={handleSave}>Desar</AMGButton>
          </div>
        )}
      </div>
      <div className="p-5 space-y-4">
        {editing ? (
          <div className="space-y-4">
            <div>
              <label className={lbl}>Sector</label>
              <div className="grid grid-cols-3 gap-1.5 max-h-48 overflow-y-auto pr-1">
                {(Object.keys(SECTOR_LABELS) as string[]).map(k => (
                  <button key={k} type="button"
                    onClick={() => { setEditSector(k); setEditSize(''); }}
                    className={`px-2 py-2 text-xs border rounded text-center transition leading-tight ${editSector === k ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.12)] text-white font-semibold' : 'border-border-base text-ink-2 hover:border-ink-2'}`}>
                    {SECTOR_LABELS[k]}
                  </button>
                ))}
              </div>
            </div>
            {editSector && availableSizes.length > 0 && (
              <div>
                <label className={lbl}>Mida de negoci</label>
                <div className="flex gap-2 flex-wrap">
                  {availableSizes.map(sz => (
                    <button key={sz} type="button"
                      onClick={() => setEditSize(sz)}
                      className={`px-4 py-2 text-sm border rounded transition ${editSize === sz ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.12)] text-white font-semibold' : 'border-border-base text-ink-2 hover:border-ink-2'}`}>
                      {SIZE_LABELS[sz] ?? sz}
                    </button>
                  ))}
                </div>
              </div>
            )}
            {editPricing && editSector && editSize && (
              <div className="bg-[rgba(255,107,0,0.06)] border border-[rgba(255,107,0,0.2)] rounded p-3 flex gap-6 flex-wrap">
                <div>
                  <div className="f-mono text-[10px] text-ink-3 uppercase">Setup</div>
                  <div className="f-display font-bold text-white">{editPricing.setupPrice} €</div>
                </div>
                <div>
                  <div className="f-mono text-[10px] text-ink-3 uppercase">1 fase/mes</div>
                  <div className="f-display font-bold text-accent-light">{editPricing.priceF1} €</div>
                </div>
                <div>
                  <div className="f-mono text-[10px] text-ink-3 uppercase">2 fases/mes</div>
                  <div className="f-display font-bold text-accent-light">{editPricing.priceF1 + editPricing.priceF2} €</div>
                </div>
              </div>
            )}
          </div>
        ) : (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <div className="f-mono text-label uppercase text-ink-3 mb-1">Sector</div>
                <div className="text-sm text-ink-1 font-semibold">
                  {tenant.sector ? (SECTOR_LABELS[tenant.sector] ?? tenant.sector) : <span className="text-ink-3 italic">Sense sector</span>}
                </div>
              </div>
              <div>
                <div className="f-mono text-label uppercase text-ink-3 mb-1">Mida</div>
                <div className="text-sm text-ink-1 font-semibold">
                  {tenant.businessSize ? (SIZE_LABELS[tenant.businessSize] ?? tenant.businessSize) : <span className="text-ink-3 italic">Sense mida</span>}
                </div>
              </div>
              {phaseCount > 0 && (
                <div>
                  <div className="f-mono text-label uppercase text-ink-3 mb-1">
                    Fases contractades <span className="normal-case">({phaseCount})</span>
                  </div>
                  <div className="flex flex-wrap gap-1.5 mt-1">
                    {phases.sort().map((ph) => (
                      <span key={ph} className="f-mono text-xs px-2 py-0.5 border border-[rgba(255,107,0,0.4)] bg-[rgba(255,107,0,0.08)] text-accent-light rounded">
                        {ph}
                      </span>
                    ))}
                  </div>
                  <div className="f-mono text-[10px] text-ink-3 mt-1">
                    {phases.sort().map(ph => PHASE_LABELS[ph]?.split(' — ')[1]).filter(Boolean).join(' · ')}
                  </div>
                </div>
              )}
            </div>
            {pricing && (
              <div className="border-t border-border-base pt-4 flex gap-8 flex-wrap">
                <div>
                  <div className="f-mono text-label uppercase text-ink-3 mb-1">Setup</div>
                  <div className="f-display font-bold text-lg text-white">{pricing.setupPrice} €</div>
                </div>
                {monthlyPrice !== null && (
                  <div>
                    <div className="f-mono text-label uppercase text-ink-3 mb-1">Mensual ({phaseCount} fase{phaseCount > 1 ? 's' : ''})</div>
                    <div className="f-display font-bold text-lg text-accent-light">{monthlyPrice} €/mes</div>
                  </div>
                )}
                <div className="self-end">
                  <div className="f-mono text-[10px] text-ink-3">Ampliació futura: <span className="text-ink-2 font-semibold">{PHASE_UPGRADE_PRICE} € / fase</span></div>
                </div>
              </div>
            )}
            {tenant.agentSystemPrompt && (
              <div className="border-t border-border-base pt-4">
                <div className="f-mono text-label uppercase text-ink-3 mb-2">Prompt agent IA</div>
                <pre className="text-xs f-mono text-ink-2 whitespace-pre-wrap bg-[rgba(255,255,255,0.02)] border border-border-base rounded p-3 max-h-48 overflow-y-auto">
                  {tenant.agentSystemPrompt}
                </pre>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

const MODE_LABELS: Record<string, { label: string; desc: string }> = {
  AUTO: { label: 'Automàtic', desc: 'L\'agent respon immediatament' },
  HYBRID: { label: 'Híbrid', desc: 'Respostes pendents d\'aprovació' },
  MANUAL: { label: 'Manual', desc: 'Només notifica, no respon' },
};

const TELEGRAM_BOT_USERNAME = 'AMGDL_Test_Bot';

function LockHint() {
  return (
    <span className="f-mono text-[10px] text-ink-3 flex items-center gap-1 mt-1">
      <I.Lock size={10} /> Atura el bot per editar
    </span>
  );
}

function ActivationModal({ channels, agentSystemPrompt, onConfirm, onClose, confirming }: {
  channels: ChannelsConfig;
  agentSystemPrompt: string | null;
  onConfirm: () => void;
  onClose: () => void;
  confirming: boolean;
}) {
  const { toast } = useToast();

  const lines: string[] = ['El vostre assistent virtual ja és actiu!', ''];
  lines.push(`📱 Telegram: t.me/${TELEGRAM_BOT_USERNAME}`);
  if (channels.whatsappPhoneNumber) lines.push(`📞 WhatsApp: ${channels.whatsappPhoneNumber}`);
  if (!channels.whatsappPhoneNumber && !channels.telegramLinked) {
    lines.push('Contacteu amb el vostre gestor per a més informació.');
  }
  const instructions = lines.join('\n');

  return (
    <div className="fixed inset-0 bg-black/70 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-md p-6 space-y-5" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Activar el bot</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><I.X size={18} /></button>
        </div>
        <p className="text-sm text-ink-2">
          El bot es posarà en marxa immediatament. Compartiu aquestes instruccions amb el client:
        </p>
        <div className="bg-[rgba(255,107,0,0.06)] border border-[rgba(255,107,0,0.2)] rounded p-4">
          <pre className="f-mono text-xs text-ink-1 whitespace-pre-wrap">{instructions}</pre>
        </div>
        <button type="button"
          onClick={() => { navigator.clipboard.writeText(instructions); toast('success', 'Instruccions copiades'); }}
          className="f-mono text-[11px] text-accent-light hover:text-accent transition">
          ↗ Copiar instruccions
        </button>
        <div className="flex gap-3 pt-1 border-t border-border-base">
          <AMGButton loading={confirming} onClick={onConfirm} className="flex-1 justify-center">
            Confirmar i activar
          </AMGButton>
          <AMGButton variant="ghost" onClick={onClose}>Cancel·lar</AMGButton>
        </div>
      </div>
    </div>
  );
}

function AgentConfigCard({ tenantId, agentSystemPrompt }: { tenantId: string; agentSystemPrompt: string | null }) {
  const qc = useQueryClient();
  const { toast } = useToast();
  const [saving, setSaving] = useState<string | null>(null);
  const [promptDraft, setPromptDraft] = useState<string | null>(null);
  const [showActivationModal, setShowActivationModal] = useState(false);

  const { data: channels } = useQuery({
    queryKey: ['agent-channels', tenantId],
    queryFn: () => getAgentChannels(tenantId),
  });

  const { data: aiConfig } = useQuery({
    queryKey: ['agent-ai-config', tenantId],
    queryFn: () => getAIConfig(tenantId),
  });

  const { data: models } = useQuery({
    queryKey: ['agent-models'],
    queryFn: () => getAvailableModels(),
  });

  const isLocked = channels?.isActive === true;

  const save = async (key: string, fn: () => Promise<unknown>) => {
    setSaving(key);
    try {
      await fn();
      qc.invalidateQueries({ queryKey: ['agent-channels', tenantId] });
      qc.invalidateQueries({ queryKey: ['agent-ai-config', tenantId] });
      qc.invalidateQueries({ queryKey: ['tenant', tenantId] });
      toast('success', 'Guardat');
    } catch {
      toast('error', 'Error guardant la configuració');
    } finally {
      setSaving(null);
    }
  };

  const handleToggleActive = () => {
    if (!channels?.isActive) {
      setShowActivationModal(true);
    } else {
      save('active', () => updateAgentChannels(tenantId, { isActive: false }));
    }
  };

  const confirmActivate = async () => {
    await save('active', () => updateAgentChannels(tenantId, { isActive: true }));
    setShowActivationModal(false);
  };

  const telegramWebhookUrl = `https://api.amgdl.com/api/v1/agents/telegram/webhook/${tenantId}`;

  return (
    <>
      <div className="amg-card card-clip">
        <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
          <AMGSectionTitle eyebrow="Agent IA" title="Agent IA & Canals" />
          {isLocked
            ? <span className="f-mono text-[10px] px-2 py-1 rounded bg-[rgba(57,211,83,0.12)] text-[#39d353] border border-[rgba(57,211,83,0.3)]">● ACTIU</span>
            : <span className="f-mono text-[10px] px-2 py-1 rounded bg-[rgba(255,255,255,0.04)] text-ink-3 border border-border-base">○ ATURAT</span>
          }
        </div>
        <div className="p-5 space-y-6">

          {/* Botó d'activació */}
          <button type="button"
            disabled={saving === 'active'}
            onClick={handleToggleActive}
            className={`flex items-center gap-3 px-4 py-3 border rounded text-sm transition w-full max-w-sm ${
              isLocked ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.08)] text-white' : 'border-border-base hover:border-ink-2 text-ink-2'
            } ${saving === 'active' ? 'opacity-50 cursor-not-allowed' : ''}`}>
            <div className={`w-10 h-5 rounded-full transition-colors relative flex-shrink-0 ${isLocked ? 'bg-[#FF6B00]' : 'bg-[rgba(255,255,255,0.12)]'}`}>
              <div className={`absolute top-0.5 w-4 h-4 rounded-full bg-white shadow transition-all ${isLocked ? 'left-5' : 'left-0.5'}`} />
            </div>
            <span className="font-semibold">{isLocked ? 'Bot actiu — clic per aturar' : 'Bot aturat — clic per activar'}</span>
          </button>

          {/* Mode — sempre editable */}
          <div className="space-y-2">
            <div className="f-mono text-label uppercase tracking-widest text-ink-3">Mode de resposta</div>
            <div className="flex gap-2 flex-wrap">
              {(['AUTO', 'HYBRID', 'MANUAL'] as const).map((mode) => {
                const active = channels?.agentMode === mode;
                return (
                  <button key={mode} type="button"
                    disabled={saving === 'mode'}
                    onClick={() => save('mode', () => updateAgentChannels(tenantId, { agentMode: mode }))}
                    className={`flex-1 min-w-[120px] text-left px-3 py-2.5 border rounded text-sm transition ${
                      active ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.12)] text-white' : 'border-border-base hover:border-ink-2 text-ink-2'
                    }`}>
                    <div className="font-semibold">{MODE_LABELS[mode].label}</div>
                    <div className="text-[10px] opacity-70">{MODE_LABELS[mode].desc}</div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Telegram */}
          <div className="space-y-2">
            <div className="f-mono text-label uppercase tracking-widest text-ink-3">Telegram</div>
            <div className="space-y-2 p-3 border border-border-base rounded">
              <div className="flex items-center gap-2">
                {channels?.telegramLinked
                  ? <><span className="w-2 h-2 rounded-full bg-green-400 flex-shrink-0" /><span className="text-sm text-ink-1">Vinculat (chat {channels.telegramChatId})</span></>
                  : <><span className="w-2 h-2 rounded-full bg-ink-3 flex-shrink-0" /><span className="text-sm text-ink-2">No vinculat — el client ha d&apos;enviar un missatge al bot</span></>
                }
              </div>
              <div className="f-mono text-[10px] text-ink-3">Bot: <span className="text-ink-2">@{TELEGRAM_BOT_USERNAME}</span></div>
              <div className="f-mono text-[10px] text-ink-3">Webhook URL:</div>
              <div className="flex items-center gap-2">
                <code className="f-mono text-[10px] text-ink-2 bg-[rgba(255,255,255,0.04)] px-2 py-1 rounded flex-1 truncate">
                  {telegramWebhookUrl}
                </code>
                <button type="button" onClick={() => { navigator.clipboard.writeText(telegramWebhookUrl); toast('success', 'Copiat'); }}
                  className="text-[10px] f-mono text-accent-light hover:text-accent transition flex-shrink-0">
                  Copiar
                </button>
              </div>
            </div>
          </div>

          {/* WhatsApp — bloquejat quan actiu */}
          <div className="space-y-2">
            <div className="f-mono text-label uppercase tracking-widest text-ink-3">WhatsApp</div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="f-mono text-xs text-ink-2">Twilio (número E.164)</label>
                <input
                  type="text"
                  readOnly={isLocked}
                  defaultValue={channels?.whatsappPhoneNumber ?? ''}
                  key={`wa-twilio-${channels?.whatsappPhoneNumber ?? ''}`}
                  placeholder="+34612345678"
                  className={`w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none transition ${
                    isLocked ? 'opacity-50 cursor-not-allowed' : 'focus:border-[#FF6B00]'
                  }`}
                  onBlur={(e) => {
                    if (isLocked) return;
                    const val = e.target.value.trim();
                    if (val !== (channels?.whatsappPhoneNumber ?? ''))
                      save('wa-twilio', () => updateAgentChannels(tenantId, { whatsappPhoneNumber: val }));
                  }}
                />
                {isLocked && <LockHint />}
              </div>
              <div className="space-y-1">
                <label className="f-mono text-xs text-ink-2">Meta (Phone Number ID)</label>
                <input
                  type="text"
                  readOnly={isLocked}
                  defaultValue={channels?.whatsappMetaPhoneNumberId ?? ''}
                  key={`wa-meta-${channels?.whatsappMetaPhoneNumberId ?? ''}`}
                  placeholder="123456789012345"
                  className={`w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none transition ${
                    isLocked ? 'opacity-50 cursor-not-allowed' : 'focus:border-[#FF6B00]'
                  }`}
                  onBlur={(e) => {
                    if (isLocked) return;
                    const val = e.target.value.trim();
                    if (val !== (channels?.whatsappMetaPhoneNumberId ?? ''))
                      save('wa-meta', () => updateAgentChannels(tenantId, { whatsappMetaPhoneNumberId: val }));
                  }}
                />
                {isLocked && <LockHint />}
              </div>
            </div>
          </div>

          {/* Model d'IA — bloquejat quan actiu */}
          <div className="space-y-2">
            <div className="f-mono text-label uppercase tracking-widest text-ink-3">Model d&apos;IA</div>
            <select
              disabled={isLocked}
              value={aiConfig?.preferredModel ?? ''}
              onChange={(e) => save('model', () => updateAIConfig(tenantId, { preferredModel: e.target.value }))}
              className={`w-full sm:w-auto bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2.5 text-sm text-ink-1 focus:outline-none transition ${
                isLocked ? 'opacity-50 cursor-not-allowed' : 'focus:border-[#FF6B00]'
              }`}>
              {models?.map((m) => (
                <option key={m.id} value={m.id}>{m.label} ({m.provider})</option>
              ))}
              {(!models || models.length === 0) && aiConfig?.preferredModel && (
                <option value={aiConfig.preferredModel}>{aiConfig.preferredModel}</option>
              )}
            </select>
            {isLocked && <LockHint />}
          </div>

          {/* Prompt del sistema — bloquejat quan actiu */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <div className="f-mono text-label uppercase tracking-widest text-ink-3">Prompt del sistema</div>
              {saving === 'prompt' && <span className="w-3.5 h-3.5 border-2 border-accent border-t-transparent rounded-full animate-spin" />}
            </div>
            <textarea
              readOnly={isLocked}
              value={promptDraft ?? agentSystemPrompt ?? ''}
              onChange={(e) => { if (!isLocked) setPromptDraft(e.target.value); }}
              rows={10}
              placeholder="Introdueix el prompt del sistema per a l'agent..."
              className={`w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2.5 text-xs f-mono text-ink-1 focus:outline-none transition resize-y ${
                isLocked ? 'opacity-50 cursor-not-allowed' : 'focus:border-[#FF6B00]'
              }`}
            />
            {isLocked
              ? <LockHint />
              : promptDraft !== null && promptDraft !== (agentSystemPrompt ?? '') && (
                <div className="flex gap-2">
                  <AMGButton size="sm" loading={saving === 'prompt'}
                    onClick={() => save('prompt', () => updateTenant(tenantId, { agentSystemPrompt: promptDraft }).then(() => setPromptDraft(null)))}>
                    Guardar prompt
                  </AMGButton>
                  <AMGButton size="sm" variant="ghost" onClick={() => setPromptDraft(null)}>
                    Cancel·lar
                  </AMGButton>
                </div>
              )
            }
          </div>

        </div>
      </div>

      {showActivationModal && channels && (
        <ActivationModal
          channels={channels}
          agentSystemPrompt={agentSystemPrompt}
          onConfirm={confirmActivate}
          onClose={() => setShowActivationModal(false)}
          confirming={saving === 'active'}
        />
      )}
    </>
  );
}

const WA_STATUS_TONE: Record<string, 'success' | 'warning' | 'danger' | 'neutral'> = {
  CONNECTED: 'success', PENDING: 'warning', ERROR: 'danger', DISCONNECTED: 'neutral',
};
const WA_STATUS_LABEL: Record<string, string> = {
  CONNECTED: 'Connectat', PENDING: 'Pendent', ERROR: 'Error', DISCONNECTED: 'Desconnectat',
};

function TelegramBotCard({ tenantId }: { tenantId: string }) {
  const qc = useQueryClient();
  const { toast } = useToast();
  const [showForm, setShowForm] = useState(false);
  const [botToken, setBotToken] = useState('');
  const [saving, setSaving] = useState(false);
  const [verifying, setVerifying] = useState(false);

  const { data: config, isLoading } = useQuery({
    queryKey: ['tg-config', tenantId],
    queryFn: () => getTelegramConfig(tenantId).catch(() => null),
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ['tg-config', tenantId] });

  const handleConnect = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      await connectTelegram(tenantId, botToken);
      toast('success', 'Bot configurat i webhook registrat');
      setBotToken('');
      setShowForm(false);
      invalidate();
    } catch {
      toast('error', 'Error configurant el bot — comprova el token');
    } finally {
      setSaving(false);
    }
  };

  const handleVerify = async () => {
    setVerifying(true);
    try {
      await verifyTelegram(tenantId);
      toast('success', 'Webhook re-registrat correctament');
      invalidate();
    } catch {
      toast('error', 'Error verificant el bot — comprova que el token és vàlid');
    } finally {
      setVerifying(false);
    }
  };

  const handleDisconnect = async () => {
    if (!confirm('Eliminar la configuració del bot? El webhook deixarà de funcionar.')) return;
    try {
      await disconnectTelegram(tenantId);
      toast('success', 'Bot desconnectat');
      invalidate();
    } catch {
      toast('error', 'Error desconnectant el bot');
    }
  };

  const statusTone: Record<string, 'success' | 'warning' | 'danger' | 'neutral'> = {
    CONNECTED: 'success', PENDING: 'warning', ERROR: 'danger', DISCONNECTED: 'neutral',
  };

  return (
    <div className="amg-card card-clip">
      <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
        <AMGSectionTitle eyebrow="Missatgeria" title="Telegram Bot" />
        <div className="flex items-center gap-2">
          {config && (
            <AMGBadge tone={statusTone[config.status] ?? 'neutral'}>
              {WA_STATUS_LABEL[config.status] ?? config.status}
            </AMGBadge>
          )}
          <AMGButton size="sm" variant="ghost" onClick={() => setShowForm(v => !v)}>
            {config ? 'Editar' : 'Configurar'}
          </AMGButton>
        </div>
      </div>

      <div className="p-5 space-y-5">
        {showForm && (
          <form onSubmit={handleConnect} className="space-y-3 p-4 border border-border-base rounded bg-[rgba(255,255,255,0.02)]">
            <div className="f-mono text-label uppercase text-ink-3 text-xs tracking-widest mb-2">
              Token del bot (de @BotFather)
            </div>
            <div>
              <label className="f-mono text-xs text-ink-2 block mb-1">Bot Token *</label>
              <input
                type="password"
                required
                value={botToken}
                onChange={(e) => setBotToken(e.target.value)}
                placeholder="1234567890:ABCdefGHIjklMNOpqrsTUVwxyz"
                className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00]"
              />
              <p className="f-mono text-[10px] text-ink-3 mt-1">
                Crea el bot amb @BotFather a Telegram i copia el token aquí
              </p>
            </div>
            <div className="flex gap-2">
              <AMGButton type="submit" size="sm" loading={saving}>Desar i registrar webhook</AMGButton>
              <AMGButton type="button" size="sm" variant="ghost" onClick={() => setShowForm(false)}>Cancel·lar</AMGButton>
            </div>
          </form>
        )}

        {!isLoading && config && !showForm && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              {config.botUsername && (
                <div>
                  <div className="f-mono text-label uppercase text-ink-3 text-[10px]">Bot</div>
                  <div className="text-sm text-ink-1 font-semibold">@{config.botUsername}</div>
                </div>
              )}
              <div>
                <div className="f-mono text-label uppercase text-ink-3 text-[10px]">Webhook</div>
                <div className="text-sm text-ink-1">{config.webhookRegistered ? '✓ Registrat' : '✗ No registrat'}</div>
              </div>
              {config.connectedAt && (
                <div>
                  <div className="f-mono text-label uppercase text-ink-3 text-[10px]">Connectat el</div>
                  <div className="text-sm text-ink-1">{fmtDate(config.connectedAt)}</div>
                </div>
              )}
            </div>
            {config.botUsername && (
              <div className="f-mono text-[10px] text-ink-3">
                Enllaç del bot:{' '}
                <a href={`https://t.me/${config.botUsername}`} target="_blank" rel="noopener noreferrer"
                  className="text-accent-light hover:text-accent transition">
                  t.me/{config.botUsername}
                </a>
              </div>
            )}
            <div className="flex items-center gap-2 flex-wrap">
              {(config.status === 'ERROR' || !config.webhookRegistered) && (
                <AMGButton size="sm" icon={I.Zap} onClick={handleVerify} loading={verifying}>
                  Re-registrar webhook
                </AMGButton>
              )}
              <AMGButton size="sm" variant="ghost" onClick={handleDisconnect}>
                Desconnectar
              </AMGButton>
            </div>
          </div>
        )}

        {!isLoading && !config && !showForm && (
          <div className="text-center py-6">
            <I.Smartphone size={28} stroke="#64748b" className="mx-auto mb-3" />
            <p className="text-sm text-ink-2 mb-3">Cap bot de Telegram configurat per aquest tenant.</p>
            <p className="f-mono text-[10px] text-ink-3 mb-4">Crea un bot amb @BotFather i entra el token aquí. El webhook es registrarà automàticament.</p>
            <AMGButton size="sm" onClick={() => setShowForm(true)}>Configurar bot</AMGButton>
          </div>
        )}
      </div>
    </div>
  );
}

type WaProvider = 'TWILIO' | 'META';

function WhatsAppMetaCard({ tenantId }: { tenantId: string }) {
  const qc = useQueryClient();
  const { toast } = useToast();
  const [provider, setProvider] = useState<WaProvider>('TWILIO');
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ phoneNumberId: '', accessToken: '', wabaId: '' });
  const [saving, setSaving] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [testPhone, setTestPhone] = useState('');
  const [sendingTest, setSendingTest] = useState(false);

  const { data: wabaConfig } = useQuery({
    queryKey: ['wa-config', tenantId],
    queryFn: () => getWhatsAppConfig(tenantId),
    retry: false,
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ['wa-config', tenantId] });

  const handleConnect = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      await connectWhatsApp(tenantId, form);
      toast('success', 'Configuració desada — prem "Verificar" per activar');
      invalidate();
      setShowForm(false);
    } catch {
      toast('error', 'Error desant la configuració');
    } finally {
      setSaving(false);
    }
  };

  const handleVerify = async () => {
    setVerifying(true);
    try {
      await verifyWhatsApp(tenantId);
      toast('success', 'WhatsApp connectat correctament');
      invalidate();
    } catch {
      toast('error', 'Verificació fallida — comprova el token i el Phone Number ID');
    } finally {
      setVerifying(false);
    }
  };

  const handleDisconnect = async () => {
    if (!confirm('Desconnectar WhatsApp Business? Els missatges deixaran d\'arribar.')) return;
    try {
      await disconnectWhatsApp(tenantId);
      toast('success', 'WhatsApp desconnectat');
      invalidate();
    } catch {
      toast('error', 'Error desconnectant');
    }
  };

  const handleTest = async (e: React.FormEvent) => {
    e.preventDefault();
    setSendingTest(true);
    try {
      await sendWhatsAppTest(tenantId, testPhone);
      toast('success', 'Missatge de prova enviat');
    } catch {
      toast('error', 'Error enviant el missatge de prova');
    } finally {
      setSendingTest(false);
    }
  };

  const providerTab = (p: WaProvider, label: string) => (
    <button
      type="button"
      onClick={() => { setProvider(p); setShowForm(false); }}
      className={`px-3 py-1.5 rounded text-xs f-mono transition ${
        provider === p
          ? 'bg-[rgba(255,107,0,0.15)] text-accent-light border border-[rgba(255,107,0,0.4)]'
          : 'text-ink-3 border border-border-base hover:text-ink-1'
      }`}
    >
      {label}
    </button>
  );

  return (
    <div className="amg-card card-clip">
      <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
        <AMGSectionTitle eyebrow="WhatsApp" title="WhatsApp Business" />
        <div className="flex items-center gap-2">
          {provider === 'META' && wabaConfig && (
            <AMGBadge tone={WA_STATUS_TONE[wabaConfig.status] ?? 'neutral'}>
              {WA_STATUS_LABEL[wabaConfig.status] ?? wabaConfig.status}
            </AMGBadge>
          )}
          {provider === 'META' && (
            <AMGButton size="sm" variant="ghost" onClick={() => setShowForm(v => !v)}>
              {wabaConfig ? 'Editar' : 'Configurar'}
            </AMGButton>
          )}
        </div>
      </div>

      <div className="p-5 space-y-5">
        {/* Selector de proveïdor */}
        <div className="flex items-center gap-2">
          <span className="f-mono text-[10px] uppercase text-ink-3 tracking-wider mr-1">Proveïdor:</span>
          {providerTab('TWILIO', 'Twilio')}
          {providerTab('META', 'Meta Business Suite')}
        </div>

        {/* Twilio — gestionat a nivell de plataforma */}
        {provider === 'TWILIO' && (
          <div className="space-y-3">
            <div className="p-4 bg-[rgba(255,255,255,0.02)] border border-border-base rounded text-sm space-y-2">
              <div className="f-mono text-[10px] uppercase text-ink-3 tracking-wider">Twilio (compte AMG)</div>
              <p className="text-ink-2 text-xs">Els missatges WhatsApp s&apos;envien via el compte Twilio d&apos;AMG. Tots els tenants comparteixen el mateix número sender configurat a les claus del sistema.</p>
              <div className="grid grid-cols-2 gap-3 pt-1">
                <div>
                  <div className="f-mono text-[10px] text-ink-3 uppercase">Account SID</div>
                  <div className="f-mono text-xs text-ink-1">Configurat a API Keys</div>
                </div>
                <div>
                  <div className="f-mono text-[10px] text-ink-3 uppercase">From number</div>
                  <div className="f-mono text-xs text-ink-1">Configurat a API Keys</div>
                </div>
              </div>
              <p className="f-mono text-[10px] text-ink-3">Per canviar les credencials Twilio, ves a <span className="text-accent-light">Sistema → API Keys → Twilio</span></p>
            </div>

            {/* Test message via Twilio */}
            <form onSubmit={handleTest} className="flex gap-2 items-end">
              <div className="flex-1">
                <label className="f-mono text-xs text-ink-3 block mb-1">Enviar missatge de prova (E.164)</label>
                <input type="text" value={testPhone}
                  onChange={(e) => setTestPhone(e.target.value)}
                  placeholder="+34612345678"
                  className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00]" />
              </div>
              <AMGButton type="submit" size="sm" variant="secondary" loading={sendingTest}>
                Enviar prova
              </AMGButton>
            </form>
          </div>
        )}

        {/* Meta Business Suite — configuració per tenant */}
        {provider === 'META' && (
          <div className="space-y-5">
            {/* Manual config form */}
            {showForm && (
              <form onSubmit={handleConnect} className="space-y-3 p-4 border border-border-base rounded bg-[rgba(255,255,255,0.02)]">
                <div className="f-mono text-label uppercase text-ink-3 text-xs tracking-widest mb-2">Configuració Meta Cloud API</div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="f-mono text-xs text-ink-2 block mb-1">Phone Number ID *</label>
                    <input type="text" required value={form.phoneNumberId}
                      onChange={(e) => setForm(f => ({ ...f, phoneNumberId: e.target.value }))}
                      placeholder="123456789012345"
                      className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00]" />
                  </div>
                  <div>
                    <label className="f-mono text-xs text-ink-2 block mb-1">WABA ID (opcional)</label>
                    <input type="text" value={form.wabaId}
                      onChange={(e) => setForm(f => ({ ...f, wabaId: e.target.value }))}
                      placeholder="987654321098765"
                      className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00]" />
                  </div>
                  <div className="sm:col-span-2">
                    <label className="f-mono text-xs text-ink-2 block mb-1">Access Token permanent *</label>
                    <input type="password" required value={form.accessToken}
                      onChange={(e) => setForm(f => ({ ...f, accessToken: e.target.value }))}
                      placeholder="EAAxxxxxxxxxx..."
                      className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00]" />
                    <p className="f-mono text-[10px] text-ink-3 mt-1">System User Access Token del Meta Business Manager</p>
                  </div>
                </div>
                <div className="flex gap-2">
                  <AMGButton type="submit" size="sm" loading={saving}>Desar</AMGButton>
                  <AMGButton type="button" size="sm" variant="ghost" onClick={() => setShowForm(false)}>Cancel·lar</AMGButton>
                </div>
              </form>
            )}

            {/* Status details */}
            {wabaConfig && !showForm && (
              <div className="space-y-3">
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                  {wabaConfig.displayPhoneNumber && (
                    <div>
                      <div className="f-mono text-label uppercase text-ink-3 text-[10px]">Número</div>
                      <div className="text-sm text-ink-1 font-semibold">{wabaConfig.displayPhoneNumber}</div>
                    </div>
                  )}
                  {wabaConfig.businessName && (
                    <div>
                      <div className="f-mono text-label uppercase text-ink-3 text-[10px]">Negoci</div>
                      <div className="text-sm text-ink-1">{wabaConfig.businessName}</div>
                    </div>
                  )}
                  {wabaConfig.phoneNumberId && (
                    <div>
                      <div className="f-mono text-label uppercase text-ink-3 text-[10px]">Phone Number ID</div>
                      <div className="f-mono text-xs text-ink-2 truncate">{wabaConfig.phoneNumberId}</div>
                    </div>
                  )}
                </div>
                <div className="flex items-center gap-2 flex-wrap">
                  {wabaConfig.status === 'PENDING' && (
                    <AMGButton size="sm" icon={I.Zap} onClick={handleVerify} loading={verifying}>
                      Verificar connexió
                    </AMGButton>
                  )}
                  {wabaConfig.status === 'ERROR' && (
                    <AMGButton size="sm" icon={I.Zap} onClick={handleVerify} loading={verifying}>
                      Reintentar verificació
                    </AMGButton>
                  )}
                  {wabaConfig.status !== 'DISCONNECTED' && (
                    <AMGButton size="sm" variant="ghost" onClick={handleDisconnect}>
                      Desconnectar
                    </AMGButton>
                  )}
                </div>
                {wabaConfig.status === 'CONNECTED' && (
                  <form onSubmit={handleTest} className="flex gap-2 items-end">
                    <div className="flex-1">
                      <label className="f-mono text-xs text-ink-3 block mb-1">Número de prova (E.164)</label>
                      <input type="text" value={testPhone}
                        onChange={(e) => setTestPhone(e.target.value)}
                        placeholder="+34612345678"
                        className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00]" />
                    </div>
                    <AMGButton type="submit" size="sm" variant="secondary" loading={sendingTest}>
                      Enviar prova
                    </AMGButton>
                  </form>
                )}
              </div>
            )}

            {!wabaConfig && !showForm && (
              <div className="text-center py-6">
                <I.Smartphone size={28} stroke="#64748b" className="mx-auto mb-3" />
                <p className="text-sm text-ink-2 mb-3">Meta Business Suite no configurat per aquest tenant.</p>
                <AMGButton size="sm" onClick={() => setShowForm(true)}>Configurar Meta</AMGButton>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

const MANDATE_STATUS_LABEL: Record<string, string> = {
  PENDING_SUBMISSION: 'Pendent d\'enviament',
  SUBMITTED: 'Enviat al banc',
  ACTIVE: 'Actiu',
  FAILED: 'Fallat',
  CANCELLED: 'Cancel·lat',
  EXPIRED: 'Expirat',
};
const MANDATE_STATUS_TONE: Record<string, 'success' | 'warning' | 'danger' | 'neutral'> = {
  ACTIVE: 'success', SUBMITTED: 'warning', PENDING_SUBMISSION: 'warning',
  FAILED: 'danger', CANCELLED: 'danger', EXPIRED: 'danger',
};

function GoCardlessCard({ tenantId }: { tenantId: string }) {
  const qc = useQueryClient();
  const { toast } = useToast();
  const [showConfigForm, setShowConfigForm] = useState(false);
  const [configForm, setConfigForm] = useState({ apiKeyRef: '', environment: 'SANDBOX' as 'SANDBOX' | 'LIVE', creditorId: '', webhookSecret: '' });
  const [configuring, setConfiguring] = useState(false);
  const [initiating, setInitiating] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [mandateUrl, setMandateUrl] = useState<string | null>(null);

  const { data: gcConfig } = useQuery({
    queryKey: ['gc-config', tenantId],
    queryFn: () => getGoCardlessConfig(tenantId),
  });

  const { data: mandate } = useQuery({
    queryKey: ['gc-mandate', tenantId],
    queryFn: () => getGoCardlessMandate(tenantId),
    enabled: !!gcConfig?.isActive,
  });

  const { data: payments } = useQuery({
    queryKey: ['gc-payments', tenantId],
    queryFn: () => listGoCardlessPayments(tenantId),
    enabled: mandate?.status === 'ACTIVE',
  });

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['gc-config', tenantId] });
    qc.invalidateQueries({ queryKey: ['gc-mandate', tenantId] });
    qc.invalidateQueries({ queryKey: ['gc-payments', tenantId] });
  };

  const handleConfigure = async (e: React.FormEvent) => {
    e.preventDefault();
    setConfiguring(true);
    try {
      await configureGoCardless(tenantId, configForm);
      toast('success', 'GoCardless configurat');
      invalidate();
      setShowConfigForm(false);
    } catch {
      toast('error', 'Error configurant GoCardless');
    } finally {
      setConfiguring(false);
    }
  };

  const handleInitiate = async () => {
    setInitiating(true);
    try {
      const resp = await initiateGoCardlessMandate(tenantId);
      setMandateUrl(resp.redirectUrl);
      invalidate();
    } catch {
      toast('error', 'Error iniciant el mandat SEPA');
    } finally {
      setInitiating(false);
    }
  };

  const handleCancel = async () => {
    if (!confirm('Cancel·lar el mandat SEPA? Deixarà de funcionar el cobrament automàtic.')) return;
    setCancelling(true);
    try {
      await cancelGoCardlessMandate(tenantId);
      toast('success', 'Mandat cancel·lat');
      invalidate();
    } catch {
      toast('error', 'Error cancel·lant el mandat');
    } finally {
      setCancelling(false);
    }
  };

  return (
    <div className="amg-card card-clip">
      <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
        <AMGSectionTitle eyebrow="Pagament" title="GoCardless — SEPA Directe" />
        <div className="flex items-center gap-2">
          {gcConfig?.isActive
            ? <span className="f-mono text-[10px] px-2 py-1 rounded bg-[rgba(57,211,83,0.12)] text-[#39d353] border border-[rgba(57,211,83,0.3)]">● Configurat</span>
            : <span className="f-mono text-[10px] px-2 py-1 rounded bg-[rgba(255,255,255,0.04)] text-ink-3 border border-border-base">○ No configurat</span>
          }
          {!showConfigForm && (
            <AMGButton size="sm" variant="ghost" onClick={() => setShowConfigForm(true)}>
              {gcConfig ? 'Actualitzar' : 'Configurar'}
            </AMGButton>
          )}
        </div>
      </div>

      <div className="p-5 space-y-5">
        {/* Config form */}
        {showConfigForm && (
          <form onSubmit={handleConfigure} className="space-y-3 p-4 border border-border-base rounded bg-[rgba(255,255,255,0.02)]">
            <div className="f-mono text-label uppercase text-ink-3 text-xs tracking-widest mb-2">Configuració GoCardless</div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="f-mono text-xs text-ink-2 block mb-1">API Key Ref</label>
                <input type="text" required value={configForm.apiKeyRef}
                  onChange={(e) => setConfigForm(f => ({ ...f, apiKeyRef: e.target.value }))}
                  placeholder="GC_API_KEY_SANDBOX"
                  className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00]" />
              </div>
              <div>
                <label className="f-mono text-xs text-ink-2 block mb-1">Entorn</label>
                <select value={configForm.environment}
                  onChange={(e) => setConfigForm(f => ({ ...f, environment: e.target.value as 'SANDBOX' | 'LIVE' }))}
                  className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm text-ink-1 focus:outline-none focus:border-[#FF6B00]">
                  <option value="SANDBOX">Sandbox (proves)</option>
                  <option value="LIVE">Live (producció)</option>
                </select>
              </div>
              <div>
                <label className="f-mono text-xs text-ink-2 block mb-1">Creditor ID (opcional)</label>
                <input type="text" value={configForm.creditorId}
                  onChange={(e) => setConfigForm(f => ({ ...f, creditorId: e.target.value }))}
                  placeholder="CR000..."
                  className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00]" />
              </div>
              <div>
                <label className="f-mono text-xs text-ink-2 block mb-1">Webhook Secret (opcional)</label>
                <input type="password" value={configForm.webhookSecret}
                  onChange={(e) => setConfigForm(f => ({ ...f, webhookSecret: e.target.value }))}
                  className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00]" />
              </div>
            </div>
            <div className="flex gap-2">
              <AMGButton type="submit" size="sm" loading={configuring}>Guardar</AMGButton>
              <AMGButton type="button" size="sm" variant="ghost" onClick={() => setShowConfigForm(false)}>Cancel·lar</AMGButton>
            </div>
          </form>
        )}

        {/* Mandate status */}
        {gcConfig?.isActive && (
          <div className="space-y-3">
            <div className="f-mono text-label uppercase text-ink-3 text-xs tracking-widest">Mandat SEPA</div>

            {!mandate ? (
              <div className="p-4 bg-[rgba(255,255,255,0.02)] border border-border-base rounded space-y-3">
                <p className="text-sm text-ink-2">Cap mandat actiu. Inicia el flux de domiciliació per al client.</p>
                <AMGButton size="sm" onClick={handleInitiate} loading={initiating} icon={I.Zap}>
                  Iniciar mandat SEPA
                </AMGButton>
              </div>
            ) : (
              <div className="p-4 bg-[rgba(255,255,255,0.02)] border border-border-base rounded space-y-2">
                <div className="flex items-center gap-2">
                  <AMGBadge tone={MANDATE_STATUS_TONE[mandate.status] ?? 'neutral'}>
                    {MANDATE_STATUS_LABEL[mandate.status] ?? mandate.status}
                  </AMGBadge>
                  {mandate.accountHolderName && (
                    <span className="text-sm text-ink-1">{mandate.accountHolderName}</span>
                  )}
                </div>
                {mandate.bankName && (
                  <div className="f-mono text-xs text-ink-3">
                    {mandate.bankName}{mandate.lastFourDigits && ` ····${mandate.lastFourDigits}`}
                  </div>
                )}
                {mandate.status === 'ACTIVE' && (
                  <AMGButton size="sm" variant="ghost" onClick={handleCancel} loading={cancelling}>
                    Cancel·lar mandat
                  </AMGButton>
                )}
              </div>
            )}

            {/* Redirect URL (after initiating) */}
            {mandateUrl && (
              <div className="p-3 bg-[rgba(255,107,0,0.06)] border border-[rgba(255,107,0,0.2)] rounded space-y-2">
                <p className="text-xs text-ink-2">Envia aquesta URL al client per autoritzar la domiciliació:</p>
                <div className="flex items-center gap-2">
                  <code className="f-mono text-xs text-ink-1 bg-[rgba(255,255,255,0.04)] px-2 py-1 rounded flex-1 truncate">
                    {mandateUrl}
                  </code>
                  <button onClick={() => { navigator.clipboard.writeText(mandateUrl); toast('success', 'URL copiada'); }}
                    className="text-xs f-mono text-accent-light hover:text-accent transition flex-shrink-0">
                    Copiar
                  </button>
                </div>
                <button onClick={() => setMandateUrl(null)} className="text-xs text-ink-3 hover:text-ink-1">Tancar</button>
              </div>
            )}
          </div>
        )}

        {/* Recent payments */}
        {payments && payments.content.length > 0 && (
          <div className="space-y-2">
            <div className="f-mono text-label uppercase text-ink-3 text-xs tracking-widest">Últims pagaments</div>
            {payments.content.slice(0, 5).map((p) => (
              <div key={p.id} className="flex items-center justify-between px-3 py-2 bg-[rgba(255,255,255,0.02)] rounded">
                <div className="flex items-center gap-2">
                  <AMGBadge tone={p.status === 'PAID_OUT' ? 'success' : p.status === 'FAILED' ? 'danger' : 'warning'}>
                    {p.status}
                  </AMGBadge>
                  <span className="f-mono text-xs text-ink-3">
                    {p.chargeDate ? new Date(p.chargeDate).toLocaleDateString('ca-ES') : '—'}
                  </span>
                </div>
                <span className="f-mono text-sm text-ink-1">{Number(p.amount).toFixed(2)} €</span>
              </div>
            ))}
          </div>
        )}

        {!gcConfig && !showConfigForm && (
          <div className="text-center py-6">
            <I.CreditCard size={28} stroke="#64748b" className="mx-auto mb-3" />
            <p className="text-sm text-ink-2 mb-3">GoCardless no està configurat per aquest tenant.</p>
            <AMGButton size="sm" onClick={() => setShowConfigForm(true)}>Configurar GoCardless</AMGButton>
          </div>
        )}
      </div>
    </div>
  );
}

function NewBudgetModal({ tenantId, tenant, setup, onClose, onCreated }: {
  tenantId: string;
  tenant?: TenantResponse;
  setup: TenantSetup | null;
  onClose: () => void;
  onCreated: () => void;
}) {
  const { toast } = useToast();

  // Sector/size state — default to tenant values but editable
  const [budgetSector, setBudgetSector] = useState(tenant?.sector ?? '');
  const [budgetSize, setBudgetSize] = useState(tenant?.businessSize ?? '');

  // El mode NexeLocal s'activa quan l'usuari selecciona un sector (no el del tenant)
  const isNexeLocal = !!budgetSector;

  const { data: sectorPhases, isLoading: loadingPhases } = useQuery({
    queryKey: ['sector-phases', budgetSector],
    queryFn: () => listSectorPhases(budgetSector),
    enabled: !!budgetSector,
  });

  // NexeLocal state
  const [selectedPhaseNums, setSelectedPhaseNums] = useState<Set<number>>(new Set());

  // Catalog state
  const [selectedProfileId, setSelectedProfileId] = useState('');
  const [selectedPhaseIds, setSelectedPhaseIds] = useState<Set<string>>(new Set());
  const [recommendedPhaseIds, setRecommendedPhaseIds] = useState<Set<string>>(new Set());

  const [notes, setNotes] = useState('');
  const [clientNotes, setClientNotes] = useState('');
  const [validUntil, setValidUntil] = useState('');
  const [recommendation, setRecommendation] = useState('');
  const [creating, setCreating] = useState(false);

  const profiles = setup?.profiles ?? [];
  const selectedProfile = profiles.find(p => p.profile.id === selectedProfileId);

  const phaseMap = new Map((sectorPhases ?? []).map((p: SectorPhaseResponse) => [p.phaseNumber, p]));
  const selectedPhasesArr = Array.from(selectedPhaseNums).sort((a, b) => a - b);
  const phasesMonthly = selectedPhasesArr.reduce((sum, pn) => sum + (phaseMap.get(pn)?.monthlyPrice ?? 0), 0);
  const phasesSetup = selectedPhasesArr.reduce((sum, pn) => sum + (phaseMap.get(pn)?.setupPrice ?? 0), 0);
  const tierAddon = WORKER_ADDONS[budgetSize] ?? { setup: 0, monthly: 0 };
  const nexeLocalSetup = phasesSetup + tierAddon.setup;
  const nexeLocalMonthly = phasesMonthly + tierAddon.monthly;

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isNexeLocal) {
      if (!budgetSector) { toast('error', 'Selecciona el tipus d\'empresa'); return; }
      if (selectedPhaseNums.size === 0) { toast('error', 'Selecciona almenys una fase'); return; }
    } else {
      if (!selectedProfileId) { toast('error', 'Selecciona un perfil'); return; }
      if (selectedPhaseIds.size === 0) { toast('error', 'Selecciona almenys una fase'); return; }
    }
    setCreating(true);
    try {
      const req: CreateBudgetRequest = isNexeLocal ? {
        phaseNumbers: Array.from(selectedPhaseNums).sort(),
        notes: notes || undefined,
        clientNotes: clientNotes || undefined,
        validUntil: validUntil || undefined,
        recommendation: recommendation || undefined,
        sector: budgetSector || undefined,
        businessSize: budgetSize || undefined,
      } : {
        profileId: selectedProfileId,
        phaseIds: Array.from(selectedPhaseIds),
        notes: notes || undefined,
        clientNotes: clientNotes || undefined,
        validUntil: validUntil || undefined,
        recommendation: recommendation || undefined,
        recommendedPhaseIds: recommendedPhaseIds.size > 0 ? Array.from(recommendedPhaseIds) : undefined,
      };
      await createBudget(tenantId, req);
      toast('success', 'Pressupost creat');
      onCreated();
      onClose();
    } catch {
      toast('error', 'Error creant el pressupost');
    } finally {
      setCreating(false);
    }
  };

  const ta = 'w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm text-ink-1 focus:outline-none focus:border-[#FF6B00] resize-none';
  const lbl = 'f-mono text-[10px] uppercase tracking-wider text-ink-3 block mb-1';
  const sel = 'w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm text-ink-1 focus:outline-none focus:border-[#FF6B00]';

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-lg p-6 space-y-4 max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Nou pressupost</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><I.X size={18} /></button>
        </div>

        <form onSubmit={handleCreate} className="space-y-4">
          {/* Sector i mida — sempre visibles, activen mode NexeLocal quan es seleccionen */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className={lbl}>Sector <span className="text-ink-3 normal-case">(opcional — activa preus NexeLocal)</span></label>
              <select value={budgetSector} onChange={e => { setBudgetSector(e.target.value); setBudgetSize(''); setSelectedPhaseNums(new Set()); }} className={sel}>
                <option value="">— Sense sector (mode catàleg) —</option>
                {(Object.keys(SECTOR_LABELS) as string[]).map(k => (
                  <option key={k} value={k}>{SECTOR_LABELS[k]}</option>
                ))}
              </select>
            </div>
            <div>
              <label className={lbl}>Nombre de treballadors</label>
              <select value={budgetSize} onChange={e => setBudgetSize(e.target.value)} className={sel} disabled={!budgetSector}>
                <option value="">Selecciona mida</option>
                {(SECTOR_SIZES[budgetSector] ?? []).map(sz => (
                  <option key={sz} value={sz}>{SIZE_LABELS[sz] ?? sz}</option>
                ))}
              </select>
            </div>
          </div>

          {isNexeLocal ? (
            /* Mode NexeLocal: fases per sector */
            <>
              <div>
                <label className={lbl}>Fases</label>
                {!budgetSector ? (
                  <p className="text-sm text-ink-3 italic">Selecciona primer el tipus d&apos;empresa</p>
                ) : loadingPhases ? (
                  <p className="text-sm text-ink-3">Carregant fases…</p>
                ) : !sectorPhases?.length ? (
                  <p className="text-sm text-ink-3 italic">Cap fase disponible per a aquest sector</p>
                ) : (
                  <div className="space-y-2">
                    {sectorPhases.map((phase: SectorPhaseResponse) => {
                      const checked = selectedPhaseNums.has(phase.phaseNumber);
                      const badge = DEP_BADGE[phase.dependencyType] ?? '';
                      return (
                        <label key={phase.phaseNumber} className={`flex items-start gap-3 p-3 border rounded cursor-pointer transition ${checked ? 'border-[#FF6B00] bg-accent-muted' : 'border-border-base hover:border-ink-2'}`}>
                          <input type="checkbox" checked={checked}
                            onChange={() => setSelectedPhaseNums(prev => { const s = new Set(prev); s.has(phase.phaseNumber) ? s.delete(phase.phaseNumber) : s.add(phase.phaseNumber); return s; })}
                            className="accent-[#FF6B00] mt-0.5" />
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-1.5">
                              <span className="text-xs leading-none">{badge}</span>
                              <span className="text-sm font-medium">F{phase.phaseNumber} · {phase.name}</span>
                            </div>
                            <div className="text-xs text-ink-3 mt-0.5 line-clamp-2">{phase.description}</div>
                            {checked && (
                              <div className="text-xs text-ink-3 f-mono flex gap-3 mt-1">
                                <span>Setup: {fmt(phase.setupPrice)}</span>
                                <span>{fmt(phase.monthlyPrice)}/mes</span>
                              </div>
                            )}
                          </div>
                        </label>
                      );
                    })}
                    {selectedPhaseNums.size > 0 && (
                      <div className="mt-2 p-3 rounded bg-[rgba(255,107,0,0.08)] border border-[rgba(255,107,0,0.2)] space-y-1.5">
                        {tierAddon.setup > 0 && (
                          <div className="text-xs text-ink-3">Add-on equip ({SIZE_LABELS[budgetSize] ?? budgetSize}): +{fmt(tierAddon.setup)} setup, +{fmt(tierAddon.monthly)}/mes</div>
                        )}
                        <div className="grid grid-cols-2 gap-2">
                          <div>
                            <div className="text-xs text-ink-3">Setup total</div>
                            <div className="text-sm font-bold f-mono text-white">{fmt(nexeLocalSetup)}</div>
                          </div>
                          <div>
                            <div className="text-xs text-ink-3">Mensual</div>
                            <div className="text-sm font-bold f-mono text-[#FF6B00]">{fmt(nexeLocalMonthly)}/mes</div>
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </>
          ) : (
            /* Mode catàleg */
            <>
              <div>
                <label className={lbl}>Perfil</label>
                {profiles.length === 0 ? (
                  <p className="text-sm text-ink-3">Cap perfil assignat. Assigna primer un perfil.</p>
                ) : (
                  <div className="space-y-2">
                    {profiles.map((p) => (
                      <button key={p.profile.id} type="button"
                        onClick={() => { setSelectedProfileId(p.profile.id); setSelectedPhaseIds(new Set()); }}
                        className={`w-full text-left p-3 border rounded transition text-sm ${selectedProfileId === p.profile.id ? 'border-[#FF6B00] bg-accent-muted' : 'border-border-base hover:border-ink-2'}`}>
                        <span className="font-semibold">{p.profile.name}</span>
                        <span className="text-ink-3 ml-2 text-xs">{p.phases.length} fases</span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
              {selectedProfile && (
                <div>
                  <label className={lbl}>Fases a incloure</label>
                  <div className="space-y-2">
                    {selectedProfile.phases.map((ph) => (
                      <div key={ph.phase.id} className="flex items-center gap-2 p-3 border border-border-base rounded">
                        <input type="checkbox" checked={selectedPhaseIds.has(ph.phase.id)}
                          onChange={() => setSelectedPhaseIds(prev => { const s = new Set(prev); s.has(ph.phase.id) ? s.delete(ph.phase.id) : s.add(ph.phase.id); return s; })}
                          className="accent-[#FF6B00]" />
                        <div className="flex-1 min-w-0">
                          <span className="text-sm">{ph.phase.name}</span>
                          <span className="f-mono text-xs text-ink-3 ml-2">{ph.services.length} serveis</span>
                        </div>
                        <label className="flex items-center gap-1.5 cursor-pointer shrink-0">
                          <input type="checkbox" checked={recommendedPhaseIds.has(ph.phase.id)}
                            onChange={() => setRecommendedPhaseIds(prev => { const s = new Set(prev); s.has(ph.phase.id) ? s.delete(ph.phase.id) : s.add(ph.phase.id); return s; })}
                            className="accent-amber-500" />
                          <span className="text-xs text-amber-400">Recomanada</span>
                        </label>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}

          <div>
            <label className={lbl}>Recomanació per al client</label>
            <textarea value={recommendation} onChange={(e) => setRecommendation(e.target.value)} rows={3}
              placeholder="Per a un negoci com el teu, et recomanem les fases F1 i F2 per arrancar..."
              className={ta} />
          </div>
          <div>
            <label className={lbl}>Notes internes</label>
            <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={2} className={ta} />
          </div>
          <div>
            <label className={lbl}>Notes per al client</label>
            <textarea value={clientNotes} onChange={(e) => setClientNotes(e.target.value)} rows={2} className={ta} />
          </div>
          <div>
            <label className={lbl}>Vàlid fins</label>
            <input type="date" value={validUntil} onChange={(e) => setValidUntil(e.target.value)}
              className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm text-ink-1 focus:outline-none focus:border-[#FF6B00]" />
          </div>

          <div className="flex gap-3 pt-2 border-t border-border-base">
            <AMGButton type="submit" disabled={creating} loading={creating} className="flex-1 justify-center">
              Crear pressupost
            </AMGButton>
            <AMGButton type="button" variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
          </div>
        </form>
      </div>
    </div>
  );
}

function BudgetDetailModal({ budget, tenantId, tenant, setup, onClose, onRefresh }: {
  budget: BudgetResponse;
  tenantId: string;
  tenant?: TenantResponse;
  setup: TenantSetup | null;
  onClose: () => void;
  onRefresh: () => void;
}) {
  const { toast } = useToast();
  const qc = useQueryClient();
  const [mode, setMode] = useState<'view' | 'edit'>('view');
  const [sending, setSending] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [cloning, setCloning] = useState(false);
  const [acceptanceUrl, setAcceptanceUrl] = useState<string | null>(null);

  const isNexeLocal = !!(budget.phaseNumbers?.length);

  // Edit state — pre-filled from budget
  const [editPhaseNums, setEditPhaseNums] = useState<Set<number>>(new Set(budget.phaseNumbers ?? []));
  const [editProfileId, setEditProfileId] = useState(budget.profileId ?? '');
  const [editPhaseIds, setEditPhaseIds] = useState<Set<string>>(new Set(budget.phaseIds ?? []));
  const [editNotes, setEditNotes] = useState(budget.notes ?? '');
  const [editClientNotes, setEditClientNotes] = useState(budget.clientNotes ?? '');
  const [editValidUntil, setEditValidUntil] = useState(budget.validUntil ? budget.validUntil.slice(0, 10) : '');
  const [editRecommendation, setEditRecommendation] = useState(budget.recommendation ?? '');
  const [saving, setSaving] = useState(false);

  // Sector/size for pricing — default to tenant values but editable
  const [editBudgetSector, setEditBudgetSector] = useState(budget.sector ?? tenant?.sector ?? '');
  const [editBudgetSize, setEditBudgetSize] = useState(budget.businessSize ?? tenant?.businessSize ?? '');

  const { data: editSectorPhases, isLoading: editLoadingPhases } = useQuery({
    queryKey: ['sector-phases', editBudgetSector],
    queryFn: () => listSectorPhases(editBudgetSector),
    enabled: !!editBudgetSector,
  });

  const isDraft = budget.status === 'DRAFT';
  const statusTone = budget.status === 'ACCEPTED' ? 'success'
    : budget.status === 'REJECTED' ? 'danger'
    : budget.status === 'SENT' ? 'info'
    : 'neutral';

  const profiles = setup?.profiles ?? [];
  const editProfile = profiles.find(p => p.profile.id === editProfileId);

  const editPhaseMap = new Map((editSectorPhases ?? []).map((p: SectorPhaseResponse) => [p.phaseNumber, p]));
  const editPhasesArr = Array.from(editPhaseNums).sort((a, b) => a - b);
  const editPhasesMonthly = editPhasesArr.reduce((sum, pn) => sum + (editPhaseMap.get(pn)?.monthlyPrice ?? 0), 0);
  const editPhasesSetup = editPhasesArr.reduce((sum, pn) => sum + (editPhaseMap.get(pn)?.setupPrice ?? 0), 0);
  const editTierAddon = WORKER_ADDONS[editBudgetSize] ?? { setup: 0, monthly: 0 };
  const editSetup = editPhasesSetup + editTierAddon.setup;
  const editMonthly = editPhasesMonthly + editTierAddon.monthly;

  const handleSend = async () => {
    setSending(true);
    try {
      const res = await sendBudget(budget.id);
      if (res?.acceptanceUrl) setAcceptanceUrl(res.acceptanceUrl);
      toast('success', 'Pressupost enviat — copia l\'enllaç per compartir-lo');
      qc.invalidateQueries({ queryKey: ['budgets', tenantId] });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error desconegut';
      toast('error', `Error enviant el pressupost: ${msg}`);
    } finally {
      setSending(false);
    }
  };

  const handleCancel = async () => {
    if (!confirm('Segur que vols eliminar aquest pressupost?')) return;
    setCancelling(true);
    try {
      await cancelBudget(budget.id);
      toast('success', 'Pressupost eliminat');
      onRefresh();
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error desconegut';
      toast('error', `Error eliminant el pressupost: ${msg}`);
    } finally {
      setCancelling(false);
    }
  };

  const handleClone = async () => {
    setCloning(true);
    try {
      const req: CreateBudgetRequest = isNexeLocal
        ? { phaseNumbers: budget.phaseNumbers!, notes: budget.notes ?? undefined, clientNotes: budget.clientNotes ?? undefined }
        : { profileId: budget.profileId ?? undefined, phaseIds: budget.phaseIds, notes: budget.notes ?? undefined, clientNotes: budget.clientNotes ?? undefined };
      await createBudget(tenantId, req);
      toast('success', 'Pressupost clonat com a DRAFT');
      onRefresh();
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error desconegut';
      toast('error', `Error clonant el pressupost: ${msg}`);
    } finally {
      setCloning(false);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isNexeLocal) {
      if (editPhaseNums.size === 0) { toast('error', 'Selecciona almenys una fase'); return; }
    } else {
      if (!editProfileId) { toast('error', 'Selecciona un perfil'); return; }
      if (editPhaseIds.size === 0) { toast('error', 'Selecciona almenys una fase'); return; }
    }
    setSaving(true);
    try {
      const req = isNexeLocal
        ? { phaseNumbers: Array.from(editPhaseNums).sort(), notes: editNotes || undefined, clientNotes: editClientNotes || undefined, validUntil: editValidUntil || undefined, recommendation: editRecommendation || undefined, sector: editBudgetSector || undefined, businessSize: editBudgetSize || undefined }
        : { profileId: editProfileId, phaseIds: Array.from(editPhaseIds), notes: editNotes || undefined, clientNotes: editClientNotes || undefined, validUntil: editValidUntil || undefined, recommendation: editRecommendation || undefined };
      await updateBudget(budget.id, req);
      toast('success', 'Pressupost actualitzat');
      onRefresh();
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error desconegut';
      toast('error', `Error actualitzant el pressupost: ${msg}`);
    } finally {
      setSaving(false);
    }
  };

  const inputCls = 'w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm text-ink-1 focus:outline-none focus:border-[#FF6B00]';
  const labelCls = 'f-mono text-[10px] uppercase tracking-wider text-ink-3 block mb-1';
  const metaRow = (label: string, value: string) => (
    <div className="flex justify-between py-1.5 border-b border-border-base last:border-0">
      <span className="text-xs text-ink-3">{label}</span>
      <span className="text-xs f-mono text-ink-1">{value}</span>
    </div>
  );

  return (
    <>
      <div className="fixed inset-0 bg-black/70 z-50 flex items-center justify-center p-4" onClick={onClose}>
        <div className="amg-card card-clip w-full max-w-lg max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>

          {/* Header */}
          <div className="flex items-center justify-between p-4 border-b border-border-base sticky top-0 bg-[var(--surface-card)] z-10">
            <div className="flex items-center gap-2 min-w-0">
              <AMGBadge tone={statusTone}>{budget.status}</AMGBadge>
              <span className="f-mono font-bold text-white truncate">{budget.budgetNumber}</span>
              {mode === 'edit' && <AMGBadge tone="warning">Editant</AMGBadge>}
            </div>
            <div className="flex items-center gap-1 shrink-0">
              {isDraft && mode === 'view' && (
                <button title="Editar" onClick={() => setMode('edit')}
                  className="p-1.5 rounded text-ink-2 hover:text-white hover:bg-[rgba(255,255,255,0.08)] transition">
                  <I.Edit size={15} />
                </button>
              )}
              <button title="Clonar" onClick={handleClone} disabled={cloning}
                className="p-1.5 rounded text-ink-2 hover:text-white hover:bg-[rgba(255,255,255,0.08)] transition disabled:opacity-40">
                <I.Copy size={15} />
              </button>
              {isDraft && mode === 'view' && (
                <button title="Enviar al client" onClick={handleSend} disabled={sending}
                  className="p-1.5 rounded text-ink-2 hover:text-white hover:bg-[rgba(255,255,255,0.08)] transition disabled:opacity-40">
                  <I.ArrowRight size={15} />
                </button>
              )}
              <button title="Eliminar" onClick={handleCancel} disabled={cancelling}
                className="p-1.5 rounded text-red-400 hover:text-red-300 hover:bg-[rgba(239,68,68,0.12)] transition disabled:opacity-40">
                <I.Trash size={15} />
              </button>
              <button onClick={mode === 'edit' ? () => setMode('view') : onClose}
                className="p-1.5 ml-1 rounded text-ink-2 hover:text-ink-0">
                <I.X size={17} />
              </button>
            </div>
          </div>

          {mode === 'view' ? (
            <div className="p-5 space-y-5">

              {/* Enllaç d'acceptació */}
              {(budget.acceptanceUrl || acceptanceUrl) && (
                <div className="rounded-lg bg-green-500/10 border border-green-500/30 p-4 space-y-2">
                  <div className="text-green-400 text-xs font-semibold uppercase tracking-wider">Enllaç per al client</div>
                  <div className="flex items-center gap-2">
                    <input readOnly value={budget.acceptanceUrl ?? acceptanceUrl ?? ''}
                      className="flex-1 bg-[rgba(255,255,255,0.05)] border border-border-base rounded px-3 py-1.5 text-xs text-ink-1 f-mono truncate focus:outline-none" />
                    <button onClick={() => { navigator.clipboard.writeText(budget.acceptanceUrl ?? acceptanceUrl ?? ''); toast('success', 'Copiat'); }}
                      className="shrink-0 px-3 py-1.5 bg-green-500/20 hover:bg-green-500/30 border border-green-500/40 text-green-400 text-xs rounded transition">
                      Copiar
                    </button>
                  </div>
                </div>
              )}

              {/* Recomanació */}
              {budget.recommendation && (
                <div className="rounded-lg bg-amber-500/10 border border-amber-500/30 p-4">
                  <div className="text-amber-400 text-xs font-semibold uppercase tracking-wider mb-1">Recomanació</div>
                  <p className="text-sm text-ink-1">{budget.recommendation}</p>
                </div>
              )}

              {/* Meta dates */}
              <div>
                {metaRow('Creat', fmtDate(budget.createdAt))}
                {metaRow('Vàlid fins', fmtDate(budget.validUntil))}
                {budget.sentAt && metaRow('Enviat', fmtDate(budget.sentAt))}
                {budget.acceptedAt && metaRow('Acceptat', fmtDate(budget.acceptedAt))}
                {budget.rejectedAt && metaRow('Rebutjat', fmtDate(budget.rejectedAt))}
                {budget.notes && metaRow('Notes', budget.notes)}
                {budget.clientNotes && metaRow('Notes client', budget.clientNotes)}
              </div>

              {/* Fases — estil accept-budget */}
              {budget.phases.length > 0 && (
                <div className="space-y-3">
                  <div className="text-xs text-ink-3 uppercase tracking-wider font-bold">Fases</div>
                  {budget.phases.map((phase, pi) => {
                    const isRec = budget.recommendedPhaseIds?.includes(phase.phaseId ?? '');
                    return (
                      <div key={pi} className="rounded-lg border border-border-base overflow-hidden">
                        <div className="flex items-center justify-between px-4 py-3 bg-[rgba(255,255,255,0.04)]">
                          <div className="flex items-center gap-3">
                            <div className="w-7 h-7 rounded-full bg-[#FF6B00]/20 border border-[#FF6B00]/40 flex items-center justify-center text-xs font-bold text-[#FF6B00] shrink-0">
                              {phase.sortOrder ?? pi + 1}
                            </div>
                            <div>
                              <div className="text-sm font-semibold text-white">{phase.name}</div>
                              {isRec && <div className="text-xs text-amber-400">★ Recomanada</div>}
                            </div>
                          </div>
                          <div className="flex gap-4 text-right shrink-0">
                            <div>
                              <div className="text-xs text-ink-3">Setup</div>
                              <div className="text-sm f-mono font-bold text-white">{fmt(phase.phaseTotal)}</div>
                            </div>
                            <div>
                              <div className="text-xs text-ink-3">Mensual</div>
                              <div className="text-sm f-mono font-bold text-[#FF6B00]">{fmt(phase.phaseMonthlyTotal)}/mes</div>
                            </div>
                          </div>
                        </div>
                        {phase.lines.length > 1 && (
                          <div className="divide-y divide-border-base">
                            {phase.lines.map((line, li) => (
                              <div key={li} className="flex items-center justify-between px-4 py-2 gap-4">
                                <span className="text-xs text-ink-3 flex-1">{line.serviceName}</span>
                                <div className="flex gap-3 text-xs f-mono text-ink-3 shrink-0">
                                  {line.setupPrice > 0 && <span>{fmt(line.setupPrice)}</span>}
                                  {line.monthlyPrice > 0 && <span className="text-[#FF6B00]">{fmt(line.monthlyPrice)}/mes</span>}
                                </div>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}

              {/* Resum de preus */}
              <div className="rounded-lg border border-border-base overflow-hidden">
                <div className="grid grid-cols-2 divide-x divide-border-base">
                  <div className="p-4 text-center">
                    <div className="text-xs text-ink-3 uppercase tracking-wider mb-1">Inversió inicial</div>
                    <div className="text-xl font-bold f-mono text-white">{fmt(budget.total)}</div>
                    {budget.discountTotal > 0 && (
                      <div className="text-xs text-green-400 mt-0.5">Descompte: -{fmt(budget.discountTotal)}</div>
                    )}
                  </div>
                  <div className="p-4 text-center">
                    <div className="text-xs text-ink-3 uppercase tracking-wider mb-1">Quota mensual</div>
                    <div className="text-xl font-bold f-mono text-[#FF6B00]">{fmt(budget.monthlyTotal ?? 0)}<span className="text-sm font-normal text-ink-3">/mes</span></div>
                  </div>
                </div>
              </div>
            </div>
          ) : (
            /* Edit mode — similar al formulari de creació */
            <form onSubmit={handleSave} className="p-5 space-y-4">
              {/* Sector i mida — sempre visibles, s'envien al backend per recalcular preus */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className={labelCls}>Sector</label>
                  <select value={editBudgetSector} onChange={e => { setEditBudgetSector(e.target.value); setEditBudgetSize(''); }} className={inputCls}>
                    <option value="">Sense sector</option>
                    {(Object.keys(SECTOR_LABELS) as string[]).map(k => (
                      <option key={k} value={k}>{SECTOR_LABELS[k]}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className={labelCls}>Nombre de treballadors</label>
                  <select value={editBudgetSize} onChange={e => setEditBudgetSize(e.target.value)} className={inputCls} disabled={!editBudgetSector}>
                    <option value="">Selecciona mida</option>
                    {(SECTOR_SIZES[editBudgetSector] ?? []).map(sz => (
                      <option key={sz} value={sz}>{SIZE_LABELS[sz] ?? sz}</option>
                    ))}
                  </select>
                </div>
              </div>

              {isNexeLocal ? (
                <>
                  <div>
                    <label className={labelCls}>Fases</label>
                    {editLoadingPhases ? (
                      <p className="text-sm text-ink-3">Carregant fases…</p>
                    ) : (
                      <div className="space-y-2">
                        {(editSectorPhases ?? []).map((phase: SectorPhaseResponse) => {
                          const checked = editPhaseNums.has(phase.phaseNumber);
                          const badge = DEP_BADGE[phase.dependencyType] ?? '';
                          return (
                            <label key={phase.phaseNumber} className={`flex items-start gap-3 p-3 border rounded cursor-pointer transition ${checked ? 'border-[#FF6B00] bg-accent-muted' : 'border-border-base hover:border-ink-2'}`}>
                              <input type="checkbox" checked={checked}
                                onChange={() => setEditPhaseNums(prev => { const s = new Set(prev); s.has(phase.phaseNumber) ? s.delete(phase.phaseNumber) : s.add(phase.phaseNumber); return s; })}
                                className="accent-[#FF6B00] mt-0.5" />
                              <div className="flex-1 min-w-0">
                                <div className="flex items-center gap-1.5">
                                  <span className="text-xs leading-none">{badge}</span>
                                  <span className="text-sm font-medium">F{phase.phaseNumber} · {phase.name}</span>
                                </div>
                                <div className="text-xs text-ink-3 mt-0.5 line-clamp-2">{phase.description}</div>
                                {checked && (
                                  <div className="text-xs text-ink-3 f-mono flex gap-3 mt-1">
                                    <span>Setup: {fmt(phase.setupPrice)}</span>
                                    <span>{fmt(phase.monthlyPrice)}/mes</span>
                                  </div>
                                )}
                              </div>
                            </label>
                          );
                        })}
                        {editPhaseNums.size > 0 && (
                          <div className="p-3 rounded bg-[rgba(255,107,0,0.08)] border border-[rgba(255,107,0,0.2)] space-y-1.5">
                            {editTierAddon.setup > 0 && (
                              <div className="text-xs text-ink-3">Add-on equip ({SIZE_LABELS[editBudgetSize] ?? editBudgetSize}): +{fmt(editTierAddon.setup)} setup, +{fmt(editTierAddon.monthly)}/mes</div>
                            )}
                            <div className="grid grid-cols-2 gap-2">
                              <div><div className="text-xs text-ink-3">Setup total</div><div className="text-sm font-bold f-mono text-white">{fmt(editSetup)}</div></div>
                              <div><div className="text-xs text-ink-3">Mensual</div><div className="text-sm font-bold f-mono text-[#FF6B00]">{fmt(editMonthly)}/mes</div></div>
                            </div>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </>
              ) : (
                <>
                  <div>
                    <label className={labelCls}>Perfil</label>
                    {profiles.length === 0 ? (
                      <p className="text-sm text-ink-3">Cap perfil assignat.</p>
                    ) : (
                      <div className="space-y-2">
                        {profiles.map((p) => (
                          <button key={p.profile.id} type="button"
                            onClick={() => { setEditProfileId(p.profile.id); setEditPhaseIds(new Set()); }}
                            className={`w-full text-left p-3 border rounded transition text-sm ${editProfileId === p.profile.id ? 'border-[#FF6B00] bg-accent-muted' : 'border-border-base hover:border-ink-2'}`}>
                            <span className="font-semibold">{p.profile.name}</span>
                            <span className="text-ink-3 ml-2 text-xs">{p.phases.length} fases</span>
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                  {editProfile && (
                    <div>
                      <label className={labelCls}>Fases a incloure</label>
                      <div className="space-y-2">
                        {editProfile.phases.map((ph) => (
                          <label key={ph.phase.id} className="flex items-center gap-3 p-3 border border-border-base rounded cursor-pointer hover:border-ink-2 transition">
                            <input type="checkbox" checked={editPhaseIds.has(ph.phase.id)}
                              onChange={() => setEditPhaseIds(prev => { const s = new Set(prev); s.has(ph.phase.id) ? s.delete(ph.phase.id) : s.add(ph.phase.id); return s; })}
                              className="accent-[#FF6B00]" />
                            <span className="text-sm flex-1">{ph.phase.name}</span>
                            <span className="f-mono text-xs text-ink-3">{ph.services.length} serveis</span>
                          </label>
                        ))}
                      </div>
                    </div>
                  )}
                </>
              )}

              <div>
                <label className={labelCls}>Recomanació per al client</label>
                <textarea value={editRecommendation} onChange={(e) => setEditRecommendation(e.target.value)} rows={3}
                  className={`${inputCls} resize-none`} />
              </div>
              <div>
                <label className={labelCls}>Notes internes</label>
                <textarea value={editNotes} onChange={(e) => setEditNotes(e.target.value)} rows={2}
                  className={`${inputCls} resize-none`} />
              </div>
              <div>
                <label className={labelCls}>Notes per al client</label>
                <textarea value={editClientNotes} onChange={(e) => setEditClientNotes(e.target.value)} rows={2}
                  className={`${inputCls} resize-none`} />
              </div>
              <div>
                <label className={labelCls}>Vàlid fins</label>
                <input type="date" value={editValidUntil} onChange={(e) => setEditValidUntil(e.target.value)}
                  className={inputCls} />
              </div>

              <div className="flex gap-3 pt-2 border-t border-border-base">
                <AMGButton type="submit" disabled={saving} loading={saving} className="flex-1 justify-center">
                  Desar canvis
                </AMGButton>
                <AMGButton type="button" variant="outline" onClick={() => setMode('view')}>Cancel·lar</AMGButton>
              </div>
            </form>
          )}
        </div>
      </div>
    </>
  );
}

function DeleteTenantModal({ tenantId, tenantName, onClose, onDeleted }: {
  tenantId: string; tenantName: string;
  onClose: () => void; onDeleted: () => void;
}) {
  const { toast } = useToast();
  const [check, setCheck] = useState<DeleteTenantCheck | null>(null);
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    checkTenantDeletion(tenantId)
      .then(setCheck)
      .catch(() => toast('error', 'Error comprovant les condicions d\'eliminació'))
      .finally(() => setLoading(false));
  }, [tenantId]);

  const handleDelete = async () => {
    setDeleting(true);
    try {
      await deleteTenant(tenantId);
      toast('success', `Tenant "${tenantName}" eliminat`);
      onDeleted();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error desconegut';
      toast('error', `Error eliminant el tenant: ${msg}`);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/70 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-md p-6 space-y-5" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base text-white">Eliminar tenant</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><I.X size={18} /></button>
        </div>

        {loading ? (
          <div className="flex justify-center py-8">
            <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          </div>
        ) : check && (
          <div className="space-y-4">
            <p className="text-sm text-ink-2">
              Estàs a punt d&apos;eliminar <span className="font-semibold text-white">{tenantName}</span> de forma permanent.
            </p>

            {[...check.blockers, ...check.warnings].length > 0 && (
              <ul className="space-y-2">
                {[...check.blockers, ...check.warnings].map((msg, i) => (
                  <li key={i} className="flex items-start gap-2.5 f-mono text-xs text-ink-2">
                    <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-[#FF6B00] flex-shrink-0" />
                    {msg}
                  </li>
                ))}
              </ul>
            )}

            <p className="text-xs text-ink-3">Aquesta acció és irreversible.</p>
            <div className="flex gap-3 pt-1 border-t border-border-base">
              <button
                onClick={handleDelete}
                disabled={deleting}
                className={`flex-1 px-4 py-2.5 rounded text-sm font-semibold transition border border-red-500 bg-[rgba(239,68,68,0.12)] text-red-400 hover:bg-[rgba(239,68,68,0.2)] ${deleting ? 'opacity-50 cursor-not-allowed' : ''}`}>
                {deleting ? 'Eliminant...' : 'Confirmar i eliminar'}
              </button>
              <AMGButton variant="ghost" onClick={onClose}>Cancel·lar</AMGButton>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default function TenantDetailPage() {
  const { id } = useParams<{ id: string }>();
  const qc = useQueryClient();
  const { toast } = useToast();
  const [showAssignProfile, setShowAssignProfile] = useState(false);
  const [showAddPhase, setShowAddPhase] = useState(false);
  const [showAddService, setShowAddService] = useState(false);
  const [showNewBudget, setShowNewBudget] = useState(false);
  const [showDeleteTenant, setShowDeleteTenant] = useState(false);
  const [selectedBudget, setSelectedBudget] = useState<BudgetResponse | null>(null);
  const [togglingFree, setTogglingFree] = useState(false);
  const [togglingActive, setTogglingActive] = useState(false);
  const [editingInfo, setEditingInfo] = useState(false);
  const [savingInfo, setSavingInfo] = useState(false);
  const [infoForm, setInfoForm] = useState({ name: '', nif: '', email: '', phone: '', address: '', contactPhone: '' });

  const openEditInfo = (t: TenantResponse) => {
    setInfoForm({ name: t.name, nif: t.nif ?? '', email: t.email ?? '', phone: t.phone ?? '', address: t.address ?? '', contactPhone: t.contactPhone ?? '' });
    setEditingInfo(true);
  };

  const saveInfo = async () => {
    setSavingInfo(true);
    try {
      await updateTenant(id, {
        name: infoForm.name || undefined,
        nif: infoForm.nif || undefined,
        email: infoForm.email || undefined,
        phone: infoForm.phone || undefined,
        address: infoForm.address || undefined,
        contactPhone: infoForm.contactPhone || undefined,
      });
      qc.invalidateQueries({ queryKey: ['tenant', id] });
      toast('success', 'Dades actualitzades');
      setEditingInfo(false);
    } catch {
      toast('error', 'Error desant les dades');
    } finally {
      setSavingInfo(false);
    }
  };

  const toggleActive = async (current: boolean) => {
    setTogglingActive(true);
    try {
      await updateTenant(id, { isActive: !current });
      qc.invalidateQueries({ queryKey: ['tenant', id] });
      toast('success', !current ? 'Tenant activat' : 'Tenant desactivat');
    } catch {
      toast('error', 'Error actualitzant l\'estat del tenant');
    } finally {
      setTogglingActive(false);
    }
  };

  const toggleFree = async (current: boolean) => {
    setTogglingFree(true);
    try {
      await updateTenant(id, { isFree: !current });
      qc.invalidateQueries({ queryKey: ['tenant', id] });
      toast('success', !current ? 'Compte marcat com a gratuït' : 'Facturació activada');
    } catch {
      toast('error', 'Error actualitzant la facturació');
    } finally {
      setTogglingFree(false);
    }
  };

  const { data: tenant, isLoading: loadingTenant, error: tenantErr } = useQuery({
    queryKey: ['tenant', id],
    queryFn: () => getTenant(id),
  });

  const { data: setup, isLoading: loadingSetup } = useQuery({
    queryKey: ['tenant-setup', id],
    queryFn: () => getTenantSetup(id),
    enabled: !!tenant,
  });

  const { data: services } = useQuery({
    queryKey: ['catalog-services'],
    queryFn: () => listCatalogServices(),
  });

  const { data: landings } = useQuery({
    queryKey: ['tenant-landings', id],
    queryFn: () => listLandings(id),
    enabled: !!tenant,
  });

  const { data: budgets, refetch: refetchBudgets } = useQuery({
    queryKey: ['budgets', id],
    queryFn: () => listBudgets(id),
    enabled: !!tenant,
  });

  const invalidateTenant = () => qc.invalidateQueries({ queryKey: ['tenant', id] });

  const invalidateSetup = () => {
    qc.invalidateQueries({ queryKey: ['tenant-setup', id] });
    qc.invalidateQueries({ queryKey: ['tenant-landings', id] });
  };

  if (loadingTenant) {
    return (
      <PortalShell breadcrumb="admin · tenants · carregant">
        <div className="p-4 sm:p-8 space-y-6">
          <div className="flex justify-center py-12">
            <span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          </div>
        </div>
      </PortalShell>
    );
  }

  if (tenantErr || !tenant) {
    return (
      <PortalShell breadcrumb="admin · tenants · error">
        <div className="p-4 sm:p-8 text-center py-12">
          <I.AlertCircle size={28} stroke="#ff6666" className="mx-auto mb-3" />
          <div className="f-display font-bold text-sm mb-1">Error carregant el tenant</div>
          <p className="f-mono text-xs text-ink-2 mb-4">No s'ha pogut carregar la informació del tenant</p>
          <AMGButton size="sm" onClick={() => window.location.reload()}>Reintentar</AMGButton>
        </div>
      </PortalShell>
    );
  }

  const landingCount = landings?.length ?? 0;
  const profileCount = setup?.profiles.length ?? 0;
  const serviceCount = setup?.profiles.reduce((acc, p) =>
    acc + p.phases.reduce((a, ph) => a + ph.services.length, 0), 0) ?? 0;

  // Find services pending configuration that have a wizard defined
  const pendingServices: Array<{ serviceId: string; serviceName: string; serviceType: string; slug: string }> = [];
  for (const p of setup?.profiles ?? []) {
    for (const ph of p.phases) {
      for (const svc of ph.services) {
        const isPending = svc.status === 'PENDING' || svc.status === 'CONFIGURING' || svc.status === 'AWAITING_CLIENT';
        if (isPending && getWizardConfig(svc.service.slug, svc.service.type)) {
          pendingServices.push({ serviceId: svc.service.id, serviceName: svc.service.name, serviceType: svc.service.type, slug: svc.service.slug });
        }
      }
    }
  }

  return (
    <PortalShell breadcrumb={`admin · tenants · ${tenant.name}`}>
      <div className="p-4 sm:p-8 space-y-6">
        {/* Header */}
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / tenants /</span>
            <div className="flex items-center gap-3 mt-1">
              <div className="f-display font-bold text-xl">{tenant.name}</div>
              {tenant.isActive
                ? <AMGBadge tone="success">Actiu</AMGBadge>
                : <AMGBadge tone="neutral">Inactiu</AMGBadge>}
            </div>
            <div className="flex flex-wrap gap-x-4 gap-y-1 mt-2 text-sm text-ink-2">
              {tenant.email && <span className="flex items-center gap-1"><I.Mail size={12} />{tenant.email}</span>}
              {tenant.phone && <span className="flex items-center gap-1"><I.Smartphone size={12} />{tenant.phone}</span>}
              <span className="f-mono text-xs text-ink-3">/{tenant.slug}</span>
              <span className="f-mono text-xs text-ink-3">Creat {fmtDate(tenant.createdAt)}</span>
            </div>
          </div>
          <div className="flex gap-2 flex-wrap">
            <AMGButton
              size="sm"
              variant={tenant.isActive ? 'ghost' : 'secondary'}
              disabled={togglingActive}
              onClick={() => toggleActive(tenant.isActive)}
            >
              {tenant.isActive ? 'Desactivar' : 'Activar'}
            </AMGButton>
            <AMGButton
              size="sm"
              icon={I.Plus}
              onClick={() => window.location.href = `/portal/landings/new?tenantId=${id}`}
            >
              Crear landing
            </AMGButton>
            <AMGButton
              size="sm"
              variant="secondary"
              icon={I.Receipt}
              onClick={() => setShowNewBudget(true)}
            >
              Nou pressupost
            </AMGButton>
            {pendingServices.map((svc) => (
              <AMGButton
                key={svc.serviceId}
                size="sm"
                variant="secondary"
                onClick={() => window.location.href = `/portal/admin/tenants/${id}/services/${svc.serviceId}/setup`}
              >
                Configurar {svc.serviceName}
              </AMGButton>
            ))}
            <button
              type="button"
              onClick={() => setShowDeleteTenant(true)}
              className="px-3 py-1.5 rounded text-xs f-mono font-semibold border border-red-500/40 bg-[rgba(239,68,68,0.06)] text-red-400 hover:bg-[rgba(239,68,68,0.15)] transition"
            >
              Eliminar
            </button>
          </div>
        </div>

        {/* Stat cards */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="card-clip amg-card p-5">
            <span className="f-mono uppercase text-label tracking-widest text-ink-3">Landings</span>
            <div className="f-display font-bold text-2xl text-accent-light mt-2">{landingCount}</div>
          </div>
          <div className="card-clip amg-card p-5">
            <span className="f-mono uppercase text-label tracking-widest text-ink-3">Perfils</span>
            <div className="f-display font-bold text-2xl text-accent-light mt-2">{profileCount}</div>
          </div>
          <div className="card-clip amg-card p-5">
            <span className="f-mono uppercase text-label tracking-widest text-ink-3">Serveis actius</span>
            <div className="f-display font-bold text-2xl text-accent-light mt-2">{serviceCount}</div>
          </div>
        </div>

        {/* Dades d'identificació */}
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
            <AMGSectionTitle eyebrow="Identificació" title="Dades d'identificació" />
            {!editingInfo ? (
              <AMGButton size="sm" variant="ghost" icon={I.Edit} onClick={() => openEditInfo(tenant)}>
                Editar
              </AMGButton>
            ) : (
              <div className="flex gap-2">
                <AMGButton size="sm" variant="ghost" onClick={() => setEditingInfo(false)}>Cancel·lar</AMGButton>
                <AMGButton size="sm" loading={savingInfo} onClick={saveInfo}>Desar</AMGButton>
              </div>
            )}
          </div>
          <div className="p-5">
            {editingInfo ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {[
                  { key: 'name', label: 'Nom empresa', placeholder: 'Empresa SL' },
                  { key: 'nif', label: 'NIF / CIF', placeholder: 'B12345678' },
                  { key: 'email', label: 'Correu electrònic', placeholder: 'contacte@empresa.com' },
                  { key: 'phone', label: 'Telèfon', placeholder: '+34612345678' },
                  { key: 'contactPhone', label: 'Telèfon de contacte', placeholder: '+34612345678' },
                  { key: 'address', label: 'Adreça', placeholder: 'Carrer Exemple, 1' },
                ].map(({ key, label, placeholder }) => (
                  <div key={key} className={key === 'address' ? 'sm:col-span-2' : ''}>
                    <label className="f-mono text-[10px] uppercase tracking-wider text-ink-3 block mb-1.5">{label}</label>
                    <input
                      type="text"
                      value={infoForm[key as keyof typeof infoForm]}
                      onChange={(e) => setInfoForm(f => ({ ...f, [key]: e.target.value }))}
                      placeholder={placeholder}
                      className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00] placeholder:text-ink-3"
                    />
                  </div>
                ))}
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {[
                  { label: 'NIF / CIF', value: tenant.nif },
                  { label: 'Correu electrònic', value: tenant.email },
                  { label: 'Telèfon', value: tenant.phone },
                  { label: 'Telèfon de contacte', value: tenant.contactPhone },
                  { label: 'Adreça', value: tenant.address },
                ].map(({ label, value }) => (
                  <div key={label}>
                    <div className="f-mono text-label uppercase text-ink-3 mb-1">{label}</div>
                    <div className="text-sm text-ink-1">{value || <span className="text-ink-3 italic">—</span>}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Facturació */}
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base">
            <AMGSectionTitle eyebrow="Compte" title="Compte gratuït" />
          </div>
          <div className="p-5">
            <button
              type="button"
              disabled={togglingFree}
              onClick={() => toggleFree(tenant.isFree)}
              className={`flex items-center gap-3 w-full max-w-sm px-4 py-3 border rounded text-sm transition ${
                tenant.isFree
                  ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.08)] text-white'
                  : 'border-border-base hover:border-ink-2 text-ink-2'
              } ${togglingFree ? 'opacity-50 cursor-not-allowed' : ''}`}
            >
              <div className={`w-10 h-5 rounded-full transition-colors relative flex-shrink-0 ${tenant.isFree ? 'bg-[#FF6B00]' : 'bg-[rgba(255,255,255,0.12)]'}`}>
                <div className={`absolute top-0.5 w-4 h-4 rounded-full bg-white shadow transition-all ${tenant.isFree ? 'left-5' : 'left-0.5'}`} />
              </div>
              <div>
                <div className="font-semibold">{tenant.isFree ? 'Compte gratuït activat' : 'Compte de pagament'}</div>
                <div className="text-xs opacity-60">
                  {tenant.isFree
                    ? 'No es generen factures ni quotes mensuals'
                    : 'Es generen factures i quotes mensuals'}
                </div>
              </div>
            </button>
          </div>
        </div>

        {/* Contracte NexeLocal */}
        <ContractSection tenant={tenant} onRefresh={invalidateTenant} />

        {/* Serveis assignats */}
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex flex-wrap items-center justify-between gap-2">
            <AMGSectionTitle eyebrow="Assignació" title="Serveis assignats" />
            <div className="flex items-center gap-2">
              <AMGButton size="sm" variant="ghost" icon={I.Layers} onClick={() => setShowAddPhase(true)}>Fase</AMGButton>
              <AMGButton size="sm" variant="ghost" icon={I.Zap} onClick={() => setShowAddService(true)}>Servei</AMGButton>
            </div>
          </div>
          {loadingSetup ? (
            <div className="flex justify-center py-8">
              <span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
            </div>
          ) : setup ? (
            <SetupSection setup={setup} tenantId={id} onRefresh={invalidateSetup} />
          ) : (
            <div className="p-8 text-center">
              <I.AlertCircle size={28} stroke="#ff6666" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Error de càrrega</div>
            </div>
          )}
        </div>

        {/* Agent IA & Canals */}
        <AgentConfigCard tenantId={id} agentSystemPrompt={tenant.agentSystemPrompt} />

        {/* Telegram Bot per tenant */}
        <TelegramBotCard tenantId={id} />

        {/* WhatsApp Business API */}
        <WhatsAppMetaCard tenantId={id} />

        {/* GoCardless SEPA */}
        <GoCardlessCard tenantId={id} />

        {/* Pressupostos */}
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
            <AMGSectionTitle eyebrow="Facturació" title="Pressupostos" />
            <AMGButton size="sm" icon={I.Plus} onClick={() => setShowNewBudget(true)}>
              Nou pressupost
            </AMGButton>
          </div>
          <div className="p-5">
            {!budgets || budgets.length === 0 ? (
              <div className="text-center py-6">
                <I.Receipt size={28} stroke="#64748b" className="mx-auto mb-3" />
                <p className="text-sm text-ink-2">Cap pressupost generat per aquest tenant.</p>
              </div>
            ) : (
              <div className="space-y-2">
                {budgets.map((b) => (
                  <button
                    key={b.id}
                    type="button"
                    onClick={() => setSelectedBudget(b)}
                    className="w-full flex items-center justify-between px-4 py-3 bg-[rgba(255,255,255,0.02)] border border-border-base rounded hover:border-[#FF6B00] hover:bg-[rgba(255,107,0,0.04)] transition cursor-pointer text-left"
                  >
                    <div className="flex items-center gap-3">
                      <AMGBadge tone={
                        b.status === 'ACCEPTED' ? 'success'
                        : b.status === 'REJECTED' ? 'danger'
                        : b.status === 'SENT' ? 'info'
                        : 'neutral'
                      }>
                        {b.status}
                      </AMGBadge>
                      <span className="f-mono text-sm text-ink-1">{b.budgetNumber}</span>
                      {b.sentAt && (
                        <span className="f-mono text-xs text-ink-3">Enviat {fmtDate(b.sentAt)}</span>
                      )}
                    </div>
                    <div className="flex items-center gap-3">
                      <span className="f-display font-bold text-sm text-white">{b.total.toFixed(2)} €</span>
                      <I.Chevron size={14} className="text-ink-3" />
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Catàleg de serveis */}
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base">
            <AMGSectionTitle eyebrow="Catàleg" title="Serveis disponibles" />
          </div>
          <div className="overflow-x-auto">
            {services ? (
              <ServiceCatalogTable services={services} />
            ) : (
              <div className="flex justify-center py-8">
                <span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
              </div>
            )}
          </div>
        </div>
      </div>

      {showAssignProfile && (
        <AssignProfileModal tenantId={id} onClose={() => setShowAssignProfile(false)} onAssigned={invalidateSetup} />
      )}
      {showAddPhase && (
        <AddPhaseModal tenantId={id} onClose={() => setShowAddPhase(false)} onAdded={invalidateSetup} />
      )}
      {showAddService && (
        <AddServiceModal tenantId={id} onClose={() => setShowAddService(false)} onAdded={invalidateSetup} />
      )}

      {showNewBudget && (
        <NewBudgetModal
          tenantId={id}
          tenant={tenant}
          setup={setup ?? null}
          onClose={() => setShowNewBudget(false)}
          onCreated={() => refetchBudgets()}
        />
      )}

      {selectedBudget && (
        <BudgetDetailModal
          budget={selectedBudget}
          tenantId={id}
          tenant={tenant}
          setup={setup ?? null}
          onClose={() => setSelectedBudget(null)}
          onRefresh={() => { refetchBudgets(); setSelectedBudget(null); }}
        />
      )}

      {showDeleteTenant && (
        <DeleteTenantModal
          tenantId={id}
          tenantName={tenant.name}
          onClose={() => setShowDeleteTenant(false)}
          onDeleted={() => { window.location.href = '/portal/admin/tenants'; }}
        />
      )}
    </PortalShell>
  );
}
