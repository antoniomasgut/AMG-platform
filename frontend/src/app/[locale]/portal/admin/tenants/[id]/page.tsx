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
  SECTOR_LABELS, SIZE_LABELS, PHASE_LABELS, PHASE_UPGRADE_PRICE,
  type TenantResponse, type TenantSetup, type CatalogService,
  type CatalogProfileResponse, type SectorPricingResponse, type ChannelsConfig, type AIConfig, type ModelInfo,
  calcMonthly,
} from '@/services/admin';
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
  if (services.length === 0) return <p className="text-sm text-ink-2 py-4">Cap servei al catàleg</p>;
  return (
    <table className="w-full min-w-[500px]">
      <thead>
        <tr className="border-b border-border-base">
          {['Servei', 'Tipus', 'Preu venda', 'Addon'].map((h) => (
            <th key={h} className="text-left f-mono text-label uppercase text-ink-2 px-4 sm:px-5 py-3 font-normal">{h}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {services.map((s) => (
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

export default function TenantDetailPage() {
  const { id } = useParams<{ id: string }>();
  const qc = useQueryClient();
  const { toast } = useToast();
  const [showAssignProfile, setShowAssignProfile] = useState(false);
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
    </PortalShell>
  );
}
