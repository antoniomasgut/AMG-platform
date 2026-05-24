'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'next/navigation';
import { useToast } from '@/lib/toast-context';
import {
  getTenant, getTenantSetup, listCatalogServices,
  listProfiles, assignProfileToTenant, removeProfileFromTenant,
  lookupSectorPricing, updateTenant, toggleTenantService,
  getAgentChannels, updateAgentChannels, getAIConfig, updateAIConfig, getAvailableModels,
  getGoCardlessConfig, getGoCardlessMandate, initiateGoCardlessMandate, cancelGoCardlessMandate,
  listGoCardlessPayments, configureGoCardless,
  SECTOR_LABELS, SIZE_LABELS, PHASE_LABELS, PHASE_UPGRADE_PRICE,
  type TenantResponse, type TenantSetup, type CatalogService,
  type CatalogProfileResponse, type SectorPricingResponse, type ChannelsConfig, type AIConfig, type ModelInfo,
  type GoCardlessConfig, type GoCardlessMandate,
  type WhatsAppWabaConfig,
  getWhatsAppConfig, connectWhatsApp, verifyWhatsApp, disconnectWhatsApp, sendWhatsAppTest,
  checkTenantDeletion, deleteTenant,
  type DeleteTenantCheck,
  calcMonthly,
} from '@/services/admin';
import { createBudget, listBudgets, type BudgetResponse, type CreateBudgetRequest } from '@/services/billing';
import { listLandings } from '@/services/factory';
import { getWizardConfig } from '@/config/service-wizards';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGSectionTitle } from '@/components/ui/stat';
import { I } from '@/components/ui/icons';

function fmtDate(d: string) {
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

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

  if (!hasProfiles && !hasAddons) {
    return (
      <div className="p-8 text-center">
        <I.Box size={28} stroke="#64748b" className="mx-auto mb-3" />
        <div className="f-display font-bold text-sm mb-1">Cap servei assignat</div>
        <p className="f-mono text-xs text-ink-2">Aquest tenant encara no té perfils ni serveis assignats</p>
      </div>
    );
  }

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
              {ph.services.map((svc) => {
                const isPending = svc.status === 'PENDING' || svc.status === 'CONFIGURING' || svc.status === 'AWAITING_CLIENT';
                return (
                  <div key={svc.service.id} className={`flex items-center gap-2 pl-2 transition-opacity ${!svc.isEnabled ? 'opacity-40' : ''}`}>
                    <ServiceToggle
                      tenantId={tenantId}
                      serviceId={svc.service.id}
                      enabled={svc.isEnabled}
                      onToggle={onRefresh}
                    />
                    <span className="text-sm text-ink-1">{svc.service.name}</span>
                    <span className="f-mono text-[10px] text-ink-3 uppercase">{svc.service.type}</span>
                    {statusBadge(svc.status, 'Actiu', 'Inactiu')}
                    {isPending && getWizardConfig(svc.service.slug, svc.service.type) && (
                      <a
                        href={`/portal/admin/tenants/${tenantId}/services/${svc.service.id}/setup`}
                        className="ml-auto text-[10px] f-mono uppercase text-accent-light hover:text-accent transition"
                      >
                        Configurar
                      </a>
                    )}
                  </div>
                );
              })}
            </div>
          ))}
        </div>
      ))}
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

function ContractSection({ tenant }: { tenant: TenantResponse }) {
  const { data: pricing } = useQuery({
    queryKey: ['pricing', tenant.sector, tenant.businessSize],
    queryFn: () => lookupSectorPricing(tenant.sector!, tenant.businessSize!),
    enabled: !!tenant.sector && !!tenant.businessSize,
  });

  const phases = tenant.contractedPhases ?? [];
  const phaseCount = phases.length;
  const hasMeta = tenant.sector || tenant.businessSize || phaseCount > 0;
  if (!hasMeta) return null;

  const monthlyPrice = pricing && phaseCount > 0 ? calcMonthly(pricing, phaseCount) : null;

  return (
    <div className="amg-card card-clip">
      <div className="p-4 sm:p-5 border-b border-border-base">
        <AMGSectionTitle eyebrow="NexeLocal" title="Contracte" />
      </div>
      <div className="p-5 space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          {tenant.sector && (
            <div>
              <div className="f-mono text-label uppercase text-ink-3 mb-1">Sector</div>
              <div className="text-sm text-ink-1 font-semibold">{SECTOR_LABELS[tenant.sector] ?? tenant.sector}</div>
            </div>
          )}
          {tenant.businessSize && (
            <div>
              <div className="f-mono text-label uppercase text-ink-3 mb-1">Mida</div>
              <div className="text-sm text-ink-1 font-semibold">{SIZE_LABELS[tenant.businessSize] ?? tenant.businessSize}</div>
            </div>
          )}
          {phaseCount > 0 && (
            <div>
              <div className="f-mono text-label uppercase text-ink-3 mb-1">
                Fases contractades
                <span className="ml-1 normal-case">({phaseCount})</span>
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
          <AMGSectionTitle eyebrow="Mòdul 20" title="Agent IA & Canals" />
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

function WhatsAppMetaCard({ tenantId }: { tenantId: string }) {
  const qc = useQueryClient();
  const { toast } = useToast();
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

  return (
    <div className="amg-card card-clip">
      <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
        <AMGSectionTitle eyebrow="Spec 27" title="WhatsApp Business API" />
        <div className="flex items-center gap-2">
          {wabaConfig ? (
            <AMGBadge tone={WA_STATUS_TONE[wabaConfig.status] ?? 'neutral'}>
              {WA_STATUS_LABEL[wabaConfig.status] ?? wabaConfig.status}
            </AMGBadge>
          ) : (
            <span className="f-mono text-[10px] px-2 py-1 rounded bg-[rgba(255,255,255,0.04)] text-ink-3 border border-border-base">○ No configurat</span>
          )}
          <AMGButton size="sm" variant="ghost" onClick={() => setShowForm(v => !v)}>
            {wabaConfig ? 'Editar' : 'Configurar'}
          </AMGButton>
        </div>
      </div>

      <div className="p-5 space-y-5">
        {/* Embedded Signup info */}
        <div className="p-3 bg-[rgba(255,107,0,0.04)] border border-[rgba(255,107,0,0.15)] rounded text-xs text-ink-2 space-y-1">
          <div className="font-semibold text-accent-light">Embedded Signup (recomanat)</div>
          <p>Quan la Facebook App estigui aprovada per Meta, el client podrà connectar el seu WABA directament des d&apos;aquí amb un clic. Fins llavors, usa la configuració manual.</p>
        </div>

        {/* Manual config form */}
        {showForm && (
          <form onSubmit={handleConnect} className="space-y-3 p-4 border border-border-base rounded bg-[rgba(255,255,255,0.02)]">
            <div className="f-mono text-label uppercase text-ink-3 text-xs tracking-widest mb-2">Configuració manual</div>
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

            {/* Test message */}
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
            <p className="text-sm text-ink-2 mb-3">WhatsApp Business no configurat per aquest tenant.</p>
            <AMGButton size="sm" onClick={() => setShowForm(true)}>Configurar WhatsApp</AMGButton>
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
        <AMGSectionTitle eyebrow="Spec 09b" title="GoCardless — SEPA Directe" />
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

function NewBudgetModal({ tenantId, setup, onClose, onCreated }: {
  tenantId: string;
  setup: TenantSetup | null;
  onClose: () => void;
  onCreated: () => void;
}) {
  const { toast } = useToast();
  const [selectedProfileId, setSelectedProfileId] = useState('');
  const [selectedPhaseIds, setSelectedPhaseIds] = useState<Set<string>>(new Set());
  const [notes, setNotes] = useState('');
  const [clientNotes, setClientNotes] = useState('');
  const [validUntil, setValidUntil] = useState('');
  const [creating, setCreating] = useState(false);

  const profiles = setup?.profiles ?? [];
  const selectedProfile = profiles.find(p => p.profile.id === selectedProfileId);

  const togglePhase = (phaseId: string) => {
    setSelectedPhaseIds(prev => {
      const next = new Set(prev);
      if (next.has(phaseId)) next.delete(phaseId);
      else next.add(phaseId);
      return next;
    });
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProfileId) { toast('error', 'Selecciona un perfil'); return; }
    if (selectedPhaseIds.size === 0) { toast('error', 'Selecciona almenys una fase'); return; }
    setCreating(true);
    try {
      const req: CreateBudgetRequest = {
        profileId: selectedProfileId,
        phaseIds: Array.from(selectedPhaseIds),
        notes: notes || undefined,
        clientNotes: clientNotes || undefined,
        validUntil: validUntil || undefined,
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

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-lg p-6 space-y-4 max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Nou pressupost</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><I.X size={18} /></button>
        </div>

        <form onSubmit={handleCreate} className="space-y-4">
          {/* Profile selector */}
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-2">Perfil</label>
            {profiles.length === 0 ? (
              <p className="text-sm text-ink-3">Cap perfil assignat a aquest tenant. Assigna primer un perfil.</p>
            ) : (
              <div className="space-y-2">
                {profiles.map((p) => (
                  <button key={p.profile.id} type="button"
                    onClick={() => { setSelectedProfileId(p.profile.id); setSelectedPhaseIds(new Set()); }}
                    className={`w-full text-left p-3 border rounded transition text-sm ${
                      selectedProfileId === p.profile.id
                        ? 'border-[#FF6B00] bg-accent-muted'
                        : 'border-border-base hover:border-ink-2'
                    }`}>
                    <span className="font-semibold">{p.profile.name}</span>
                    <span className="text-ink-3 ml-2 text-xs">{p.phases.length} fases</span>
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Phases selector */}
          {selectedProfile && (
            <div>
              <label className="f-mono text-label uppercase text-ink-2 block mb-2">Fases a incloure</label>
              <div className="space-y-2">
                {selectedProfile.phases.map((ph) => (
                  <label key={ph.phase.id} className="flex items-center gap-3 p-3 border border-border-base rounded cursor-pointer hover:border-ink-2 transition">
                    <input type="checkbox" checked={selectedPhaseIds.has(ph.phase.id)}
                      onChange={() => togglePhase(ph.phase.id)}
                      className="accent-[#FF6B00]" />
                    <div className="flex-1">
                      <span className="text-sm">{ph.phase.name}</span>
                      <span className="f-mono text-xs text-ink-3 ml-2">{ph.services.length} serveis</span>
                    </div>
                    <AMGBadge tone={ph.approvalStatus === 'APPROVED' ? 'success' : 'neutral'}>
                      {ph.approvalStatus}
                    </AMGBadge>
                  </label>
                ))}
              </div>
            </div>
          )}

          {/* Optional fields */}
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Notes internes (opcional)</label>
            <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={2}
              className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm text-ink-1 focus:outline-none focus:border-[#FF6B00] resize-none" />
          </div>
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Notes per al client (opcional)</label>
            <textarea value={clientNotes} onChange={(e) => setClientNotes(e.target.value)} rows={2}
              className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm text-ink-1 focus:outline-none focus:border-[#FF6B00] resize-none" />
          </div>
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Vàlid fins (opcional)</label>
            <input type="date" value={validUntil} onChange={(e) => setValidUntil(e.target.value)}
              className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm text-ink-1 focus:outline-none focus:border-[#FF6B00]" />
          </div>

          <div className="flex gap-3 pt-2 border-t border-border-base">
            <AMGButton type="submit" disabled={creating || profiles.length === 0} loading={creating} className="flex-1 justify-center">
              Crear pressupost
            </AMGButton>
            <AMGButton type="button" variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
          </div>
        </form>
      </div>
    </div>
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

  useState(() => {
    checkTenantDeletion(tenantId)
      .then(setCheck)
      .catch(() => toast('error', 'Error comprovant les condicions d\'eliminació'))
      .finally(() => setLoading(false));
  });

  const handleDelete = async () => {
    setDeleting(true);
    try {
      await deleteTenant(tenantId);
      toast('success', `Tenant "${tenantName}" eliminat`);
      onDeleted();
    } catch {
      toast('error', 'Error eliminant el tenant');
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
  const [showNewBudget, setShowNewBudget] = useState(false);
  const [showDeleteTenant, setShowDeleteTenant] = useState(false);
  const [togglingFree, setTogglingFree] = useState(false);
  const [togglingActive, setTogglingActive] = useState(false);

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

        {/* Facturació */}
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base">
            <AMGSectionTitle eyebrow="Facturació" title="Compte gratuït" />
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
        <ContractSection tenant={tenant} />

        {/* Serveis assignats */}
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
            <AMGSectionTitle eyebrow="Assignació" title="Serveis assignats" />
            <AMGButton size="sm" icon={I.Plus} onClick={() => setShowAssignProfile(true)}>Assignar perfil</AMGButton>
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

        {/* WhatsApp Business API */}
        <WhatsAppMetaCard tenantId={id} />

        {/* GoCardless SEPA */}
        <GoCardlessCard tenantId={id} />

        {/* Pressupostos */}
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
            <AMGSectionTitle eyebrow="Spec 07" title="Pressupostos" />
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
                  <div key={b.id} className="flex items-center justify-between px-4 py-3 bg-[rgba(255,255,255,0.02)] border border-border-base rounded hover:border-ink-2 transition">
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
                    <span className="f-display font-bold text-sm text-white">{b.total.toFixed(2)} €</span>
                  </div>
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

      {showNewBudget && (
        <NewBudgetModal
          tenantId={id}
          setup={setup ?? null}
          onClose={() => setShowNewBudget(false)}
          onCreated={() => refetchBudgets()}
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
