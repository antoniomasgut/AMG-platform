'use client';

import { useState, useEffect, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getTenant, updateTenant,
  getAgentChannels, updateAgentChannels, getAIConfig, updateAIConfig, getAvailableModels,
  SECTOR_LABELS, SIZE_LABELS, SECTOR_SIZES,
  type TenantResponse, type ChannelsConfig, type UpdateTenantRequest,
} from '@/services/admin';
import {
  getNexeConfigs, saveNexeConfig, getCalendarOAuthUrl, provisionCalendar,
  getAgendaDefaults, getPressupostosDefaults, DEFAULT_FIDELITZACIO, DEFAULT_EQUIP,
  type AgendaConfig, type PressupostosConfig, type FidelitzacioConfig, type EquipConfig,
} from '@/services/nexe-configs';
import { SECTOR_CONTEXTS } from '@/services/sector-contexts';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { IconSet } from '@/components/ui/icons';
import { useToast } from '@/lib/toast-context';

// ── Styles ───────────────────────────────────────────────────────────────────

const inp = 'w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00] placeholder:text-ink-3';
const lbl = 'f-mono text-[10px] uppercase tracking-wider text-ink-3 block mb-1.5';
const sel = `${inp} cursor-pointer`;

// ── Phase definitions ────────────────────────────────────────────────────────

const PHASE_INFO: Record<string, { label: string; desc: string; icon: React.ComponentType<{ size?: number; className?: string }> }> = {
  F1: { label: 'Captació', desc: 'Captura leads des de web, WhatsApp i email. Agent classifica i crea fitxes automàticament.', icon: IconSet.Globe },
  F2: { label: 'Agenda', desc: 'Confirma cites, envia recordatoris automàtics i gestiona absències.', icon: IconSet.Calendar },
  F3: { label: 'Pressupostos', desc: "L'agent genera pressupostos i fa seguiment dels que queden sense resposta.", icon: IconSet.Receipt },
  F4: { label: 'Seguiment', desc: "Cap client oblidat: recordatoris, sol·licituds de ressenya i reactivació d'inactius.", icon: IconSet.Heart },
  F5: { label: 'Alertes & Equip', desc: 'Alertes proactives al Telegram: leads, cites, informe diari i gestió de l\'equip.', icon: IconSet.Users },
};

// Recommended phases by sector
const SECTOR_RECOMMENDED: Record<string, string[]> = {
  FISIOTERAPEUTA: ['F1', 'F2', 'F4'],
  PSICOLEG: ['F1', 'F2', 'F4'],
  NUTRICIONISTA: ['F1', 'F2'],
  PERRUQUERIA: ['F1', 'F2'],
  ESTETICA: ['F1', 'F2'],
  PERRUQUERIA_CANINA: ['F1', 'F2'],
  VETERINARI: ['F1', 'F2', 'F4'],
  PINTOR: ['F1', 'F2', 'F3'],
  ELECTRICISTA: ['F1', 'F2', 'F3'],
  FONTANER: ['F1', 'F2', 'F3'],
  JARDINER: ['F1', 'F3'],
  NETEJA: ['F1', 'F2', 'F3'],
  TALLER_MECANIC: ['F1', 'F2', 'F3'],
  GESTORIA: ['F1', 'F2', 'F3'],
  ACADEMIA: ['F1', 'F2'],
  RESTAURANTE: ['F1', 'F4'],
  INMOBILIARIA: ['F1', 'F3', 'F4'],
  AGENCIA_IA: ['F1', 'F3', 'F5'],
};

// ── LocalStorage helpers ─────────────────────────────────────────────────────

interface WizardStorage {
  currentStep: number;
  selectedPhases: string[];
}

function loadWizardState(tenantId: string): WizardStorage | null {
  try {
    const raw = localStorage.getItem(`wizard:${tenantId}`);
    if (!raw) return null;
    return JSON.parse(raw) as WizardStorage;
  } catch { return null; }
}

function saveWizardState(tenantId: string, state: WizardStorage) {
  try { localStorage.setItem(`wizard:${tenantId}`, JSON.stringify(state)); } catch {}
}

function clearWizardState(tenantId: string) {
  try { localStorage.removeItem(`wizard:${tenantId}`); } catch {}
}

// ── Step sidebar ─────────────────────────────────────────────────────────────

type StepStatus = 'done' | 'active' | 'pending' | 'waiting';

function Sidebar({ steps, current, completedIds, onGo }: {
  steps: Array<{ id: string; label: string }>;
  current: number;
  completedIds: Set<string>;
  onGo: (i: number) => void;
}) {
  return (
    <div className="hidden lg:flex w-52 shrink-0 bg-[#0e0e20] border-r border-border-base flex-col py-4 gap-0.5 overflow-y-auto">
      <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3 px-4 pb-2">Configuració</div>
      {steps.map((s, i) => {
        const done = completedIds.has(s.id);
        const active = i === current;
        const canGo = done || i <= current;
        return (
          <button key={s.id} onClick={() => canGo && onGo(i)} disabled={!canGo}
            className={`flex items-center gap-2.5 px-4 py-2 text-left transition-colors ${
              active ? 'bg-accent-muted text-accent-light' :
              done   ? 'text-ink-2 hover:text-ink-0 hover:bg-[rgba(255,255,255,0.03)]' :
              'text-ink-3 cursor-not-allowed'
            }`}>
            <span className={`w-4 h-4 rounded-full flex-shrink-0 flex items-center justify-center text-[9px] font-bold border ${
              done   ? 'bg-[rgba(57,211,83,0.2)] border-[rgba(57,211,83,0.5)] text-[#39d353]' :
              active ? 'border-accent-light bg-accent-muted text-accent-light' :
              'border-border-base'
            }`}>
              {done ? '✓' : String(i + 1)}
            </span>
            <span className="f-mono text-[11px] leading-tight">{s.label}</span>
          </button>
        );
      })}
    </div>
  );
}

// ── Step: General Info ───────────────────────────────────────────────────────

function GeneralStep({ tenant, onSave }: { tenant: TenantResponse; onSave: (data: Partial<TenantResponse>) => Promise<void> }) {
  const [form, setForm] = useState({
    name: tenant.name ?? '',
    email: tenant.email ?? '',
    phone: tenant.phone ?? '',
    contactPhone: tenant.contactPhone ?? '',
    address: tenant.address ?? '',
    nif: tenant.nif ?? '',
  });
  const [saving, setSaving] = useState(false);
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) => setForm(f => ({ ...f, [k]: e.target.value }));

  const handleSave = async () => {
    setSaving(true);
    try { await onSave(form); } finally { setSaving(false); }
  };

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-base font-semibold text-ink-0">Informació del negoci</h2>
        <p className="text-xs text-ink-3 mt-0.5">Comprova i actualitza les dades del client.</p>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {([
          { key: 'name',         label: 'Nom de l\'empresa', placeholder: 'Empresa SL' },
          { key: 'nif',          label: 'NIF / CIF', placeholder: 'B12345678' },
          { key: 'email',        label: 'Email de contacte', placeholder: 'hola@empresa.com' },
          { key: 'phone',        label: 'Telèfon', placeholder: '+34612345678' },
          { key: 'contactPhone', label: 'Telèfon de contacte (opcional)', placeholder: '+34612345678' },
          { key: 'address',      label: 'Adreça', placeholder: 'Carrer Exemple, 1, Palma' },
        ] as const).map(({ key, label, placeholder }) => (
          <div key={key} className={key === 'address' ? 'sm:col-span-2' : ''}>
            <label className={lbl}>{label}</label>
            <input className={inp} value={form[key as keyof typeof form]} onChange={set(key as keyof typeof form)} placeholder={placeholder} />
          </div>
        ))}
      </div>
      <div className="pt-2">
        <AMGButton onClick={handleSave} loading={saving}>Desar i continuar →</AMGButton>
      </div>
    </div>
  );
}

// ── Step: Phase Selection ────────────────────────────────────────────────────

function PhasesStep({ tenant, onSave }: {
  tenant: TenantResponse;
  onSave: (phases: string[], sector: string, size: string) => Promise<void>;
}) {
  const [sector, setSector] = useState(tenant.sector ?? '');
  const [size, setSize] = useState(tenant.businessSize ?? '');
  const [selected, setSelected] = useState<Set<string>>(new Set(tenant.contractedPhases ?? []));
  const [saving, setSaving] = useState(false);

  // Auto-select recommended when sector changes
  useEffect(() => {
    if (sector && (tenant.contractedPhases?.length ?? 0) === 0) {
      setSelected(new Set(SECTOR_RECOMMENDED[sector] ?? ['F1']));
    }
  }, [sector, tenant.contractedPhases]);

  const toggle = (p: string) => setSelected(prev => {
    const next = new Set(prev);
    next.has(p) ? next.delete(p) : next.add(p);
    return next;
  });

  const recommended = SECTOR_RECOMMENDED[sector] ?? [];

  const handleSave = async () => {
    setSaving(true);
    try { await onSave(Array.from(selected).sort(), sector, size); } finally { setSaving(false); }
  };

  const avail = sector ? (SECTOR_SIZES[sector] ?? []) : [];

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-base font-semibold text-ink-0">Sector i fases del servei</h2>
        <p className="text-xs text-ink-3 mt-0.5">Selecciona les fases contractades. Les recomanades per al sector s'indiquen amb una estrella.</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label className={lbl}>Sector</label>
          <select className={sel} value={sector} onChange={e => { setSector(e.target.value); setSize(''); }}>
            <option value="">— Selecciona sector —</option>
            {Object.entries(SECTOR_LABELS).map(([k, v]) => (
              <option key={k} value={k}>{v}</option>
            ))}
          </select>
        </div>
        {avail.length > 0 && (
          <div>
            <label className={lbl}>Mida del negoci</label>
            <select className={sel} value={size} onChange={e => setSize(e.target.value)}>
              <option value="">— Selecciona mida —</option>
              {avail.map(sz => <option key={sz} value={sz}>{SIZE_LABELS[sz] ?? sz}</option>)}
            </select>
          </div>
        )}
      </div>

      <div className="space-y-2">
        <label className={lbl}>Fases a contractar</label>
        {Object.entries(PHASE_INFO).map(([phase, info]) => {
          const isRec = recommended.includes(phase);
          const checked = selected.has(phase);
          const Icon = info.icon;
          return (
            <label key={phase} className={`flex items-start gap-3 p-3 border rounded cursor-pointer transition ${
              checked ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.06)]' : 'border-border-base hover:border-ink-2'
            }`}>
              <input type="checkbox" checked={checked} onChange={() => toggle(phase)} className="mt-0.5 accent-[#FF6B00]" />
              <Icon size={14} className="mt-0.5 text-ink-3 shrink-0" />
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <span className="f-mono text-xs font-bold text-accent-light">{phase}</span>
                  <span className="text-sm font-medium text-ink-0">{info.label}</span>
                  {isRec && <span className="f-mono text-[9px] px-1.5 py-0.5 rounded border border-amber-500/30 bg-amber-500/10 text-amber-400">Recomanada</span>}
                </div>
                <p className="text-xs text-ink-3 mt-0.5">{info.desc}</p>
              </div>
            </label>
          );
        })}
      </div>

      <div className="pt-2">
        <AMGButton onClick={handleSave} loading={saving} disabled={selected.size === 0}>
          Desar i continuar ({selected.size} fases) →
        </AMGButton>
      </div>
    </div>
  );
}

// ── Step: F1 — Captació ───────────────────────────────────────────────────────

function F1Step({ tenant, onSave }: { tenant: TenantResponse; onSave: () => Promise<void> }) {
  const [saving, setSaving] = useState(false);
  const handleSave = async () => { setSaving(true); try { await onSave(); } finally { setSaving(false); } };
  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-base font-semibold text-ink-0">F1 — Captació</h2>
        <p className="text-xs text-ink-3 mt-0.5">La captació s'activa automàticament quan hi ha un agent actiu i una landing publicada.</p>
      </div>
      <div className="p-4 bg-[rgba(255,107,0,0.05)] border border-[rgba(255,107,0,0.2)] rounded space-y-2">
        <div className="flex items-center gap-2">
          <IconSet.Globe size={14} className="text-accent-light shrink-0" />
          <span className="text-sm font-medium text-ink-0">Landings</span>
        </div>
        <p className="text-xs text-ink-2">Assegura't que hi hagi almenys una landing publicada per a aquest tenant. Podràs crear-la des de la secció <span className="text-accent-light">Landings</span>.</p>
      </div>
      <div className="p-4 border border-border-base rounded space-y-2">
        <div className="flex items-center gap-2">
          <IconSet.Bot size={14} className="text-ink-3 shrink-0" />
          <span className="text-sm text-ink-1">L'agent es configurarà al pas <span className="text-accent-light">Agent IA</span></span>
        </div>
      </div>
      <div className="pt-2">
        <AMGButton onClick={handleSave} loading={saving}>Continuar →</AMGButton>
      </div>
    </div>
  );
}

// ── Step: F2 — Agenda ────────────────────────────────────────────────────────

function F2Step({ tenantId, tenant, existingConfig, onSave }: {
  tenantId: string;
  tenant: TenantResponse;
  existingConfig?: AgendaConfig | null;
  onSave: (cfg: AgendaConfig) => Promise<void>;
}) {
  const { toast } = useToast();
  const defaults = getAgendaDefaults(tenant.sector);
  const [cfg, setCfg] = useState<AgendaConfig>(existingConfig ?? defaults);
  const [saving, setSaving] = useState(false);
  const [oauthUrl, setOauthUrl] = useState<string | null>(null);
  const [loadingOauth, setLoadingOauth] = useState(false);
  const [verifyingOauth, setVerifyingOauth] = useState(false);
  const set = <K extends keyof AgendaConfig>(k: K, v: AgendaConfig[K]) => setCfg(c => ({ ...c, [k]: v }));

  const applyTemplate = () => {
    setCfg({ ...defaults });
    toast('success', 'Plantilla de sector aplicada');
  };

  const handleGetOAuth = async () => {
    setLoadingOauth(true);
    try {
      const res = await getCalendarOAuthUrl(tenantId);
      setOauthUrl(res.url);
    } catch {
      toast('error', 'Error obtenint URL d\'autorització');
    } finally { setLoadingOauth(false); }
  };

  const handleVerifyOAuth = async () => {
    setVerifyingOauth(true);
    try {
      await provisionCalendar(tenantId);
      toast('success', 'Google Calendar autoritzat correctament');
      set('calendar_type', 'google_oauth');
    } catch {
      toast('error', "L'autorització no s'ha completat encara");
    } finally { setVerifyingOauth(false); }
  };

  const handleSave = async () => {
    setSaving(true);
    try { await onSave(cfg); } finally { setSaving(false); }
  };

  const days = ['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday'];
  const dayLabels: Record<string, string> = { monday: 'Dll', tuesday: 'Dm', wednesday: 'Dc', thursday: 'Dj', friday: 'Dv', saturday: 'Ds', sunday: 'Dg' };

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between gap-3 flex-wrap">
        <div>
          <h2 className="text-base font-semibold text-ink-0">F2 — Agenda</h2>
          <p className="text-xs text-ink-3 mt-0.5">Configura el calendari i les hores de disponibilitat.</p>
        </div>
        {tenant.sector && (
          <AMGButton size="sm" variant="ghost" onClick={applyTemplate} icon={IconSet.Sparkles}>
            Plantilla {SECTOR_LABELS[tenant.sector] ?? tenant.sector}
          </AMGButton>
        )}
      </div>

      {/* Calendar type */}
      <div>
        <label className={lbl}>Tipus de calendari</label>
        <div className="flex flex-wrap gap-2">
          {(['manual', 'google_oauth', 'calendly'] as const).map(t => (
            <button key={t} type="button" onClick={() => set('calendar_type', t)}
              className={`px-3 py-1.5 rounded text-xs f-mono border transition ${cfg.calendar_type === t ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.12)] text-white' : 'border-border-base text-ink-2 hover:border-ink-2'}`}>
              {t === 'manual' ? 'Manual (sense integració)' : t === 'google_oauth' ? 'Google Calendar (OAuth)' : 'Calendly'}
            </button>
          ))}
        </div>
      </div>

      {/* Google OAuth */}
      {cfg.calendar_type === 'google_oauth' && (
        <div className="p-4 border border-[rgba(255,107,0,0.3)] rounded bg-[rgba(255,107,0,0.04)] space-y-3">
          <div className="flex items-center gap-2">
            <IconSet.Calendar size={14} className="text-accent-light" />
            <span className="text-sm font-medium text-ink-1">Autorització Google Calendar</span>
          </div>
          {!oauthUrl ? (
            <AMGButton size="sm" onClick={handleGetOAuth} loading={loadingOauth} icon={IconSet.Link}>
              Obtenir URL d'autorització
            </AMGButton>
          ) : (
            <div className="space-y-2">
              <p className="text-xs text-ink-2">Envia aquesta URL al client perquè autoritzi el calendari:</p>
              <div className="flex items-center gap-2">
                <code className="f-mono text-xs bg-[rgba(255,255,255,0.04)] border border-border-base px-2 py-1 rounded flex-1 truncate">{oauthUrl}</code>
                <button onClick={() => { navigator.clipboard.writeText(oauthUrl); }} className="text-xs text-accent-light hover:text-accent shrink-0">Copiar</button>
              </div>
              <div className="flex items-center gap-2 pt-1">
                <span className="w-2 h-2 rounded-full bg-amber-400 animate-pulse" />
                <span className="text-xs text-amber-400">Esperant que el client autoritzi...</span>
                <AMGButton size="sm" variant="ghost" onClick={handleVerifyOAuth} loading={verifyingOauth}>
                  Verificar autorització
                </AMGButton>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Slot duration */}
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
        <div>
          <label className={lbl}>Durada cita (min)</label>
          <input type="number" className={inp} value={cfg.slot_duration_minutes} min={15} max={480}
            onChange={e => set('slot_duration_minutes', Number(e.target.value))} />
        </div>
        <div>
          <label className={lbl}>Marge entre cites (min)</label>
          <input type="number" className={inp} value={cfg.buffer_minutes} min={0} max={60}
            onChange={e => set('buffer_minutes', Number(e.target.value))} />
        </div>
        <div>
          <label className={lbl}>Max dies avançament</label>
          <input type="number" className={inp} value={cfg.max_days_advance} min={1} max={365}
            onChange={e => set('max_days_advance', Number(e.target.value))} />
        </div>
      </div>

      {/* Working hours */}
      <div>
        <label className={lbl}>Horari laboral</label>
        <div className="space-y-1.5">
          {days.map(d => {
            const wh = cfg.working_hours[d] ?? { enabled: false, start: '09:00', end: '18:00' };
            return (
              <div key={d} className="flex items-center gap-3">
                <label className="flex items-center gap-2 w-16 shrink-0">
                  <input type="checkbox" checked={wh.enabled} className="accent-[#FF6B00]"
                    onChange={e => set('working_hours', { ...cfg.working_hours, [d]: { ...wh, enabled: e.target.checked } })} />
                  <span className="f-mono text-xs text-ink-2">{dayLabels[d]}</span>
                </label>
                {wh.enabled && (
                  <>
                    <input type="time" value={wh.start} className={`${inp} w-24`}
                      onChange={e => set('working_hours', { ...cfg.working_hours, [d]: { ...wh, start: e.target.value } })} />
                    <span className="text-ink-3 text-xs">—</span>
                    <input type="time" value={wh.end} className={`${inp} w-24`}
                      onChange={e => set('working_hours', { ...cfg.working_hours, [d]: { ...wh, end: e.target.value } })} />
                  </>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Confirmation template */}
      <div>
        <label className={lbl}>Missatge de confirmació</label>
        <textarea className={`${inp} resize-none`} rows={3} value={cfg.confirmation_template}
          onChange={e => set('confirmation_template', e.target.value)} />
        <p className="f-mono text-[10px] text-ink-3 mt-1">Variables: {'{{data}}'}, {'{{hora}}'}</p>
      </div>

      <div className="pt-2">
        <AMGButton onClick={handleSave} loading={saving}>Desar i continuar →</AMGButton>
      </div>
    </div>
  );
}

// ── Step: F3 — Pressupostos ──────────────────────────────────────────────────

function F3Step({ tenant, existingConfig, onSave }: {
  tenant: TenantResponse;
  existingConfig?: PressupostosConfig | null;
  onSave: (cfg: PressupostosConfig) => Promise<void>;
}) {
  const { toast } = useToast();
  const defaults = getPressupostosDefaults(tenant.sector);
  const [cfg, setCfg] = useState<PressupostosConfig>(existingConfig ?? defaults);
  const [saving, setSaving] = useState(false);
  const set = <K extends keyof PressupostosConfig>(k: K, v: PressupostosConfig[K]) => setCfg(c => ({ ...c, [k]: v }));

  const applyTemplate = () => { setCfg({ ...defaults }); toast('success', 'Plantilla aplicada'); };

  const handleSave = async () => { setSaving(true); try { await onSave(cfg); } finally { setSaving(false); } };

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between gap-3 flex-wrap">
        <div>
          <h2 className="text-base font-semibold text-ink-0">F3 — Pressupostos</h2>
          <p className="text-xs text-ink-3 mt-0.5">Defineix el catàleg de serveis per generar pressupostos.</p>
        </div>
        {tenant.sector && (
          <AMGButton size="sm" variant="ghost" onClick={applyTemplate} icon={IconSet.Sparkles}>
            Plantilla {SECTOR_LABELS[tenant.sector] ?? tenant.sector}
          </AMGButton>
        )}
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className={lbl}>Validesa del pressupost (dies)</label>
          <input type="number" className={inp} value={cfg.quote_validity_days} min={1} max={365}
            onChange={e => set('quote_validity_days', Number(e.target.value))} />
        </div>
        <div>
          <label className={lbl}>Seguiment automàtic (dies sense resposta)</label>
          <input type="number" className={inp} value={cfg.quote_followup_days} min={1} max={30}
            onChange={e => set('quote_followup_days', Number(e.target.value))} />
        </div>
      </div>

      <div>
        <label className={lbl}>Capçalera del pressupost</label>
        <textarea className={`${inp} resize-none`} rows={2} value={cfg.quote_header}
          onChange={e => set('quote_header', e.target.value)} placeholder="Text introductori del pressupost..." />
      </div>

      {/* Services catalog */}
      <div>
        <div className="flex items-center justify-between mb-2">
          <label className={lbl}>Catàleg de serveis ({cfg.services_catalog.length})</label>
          <button type="button" onClick={() => set('services_catalog', [...cfg.services_catalog, { id: crypto.randomUUID(), name: '', price: 0, unit: 'unitat', description: '' }])}
            className="text-xs text-accent-light hover:text-accent f-mono flex items-center gap-1">
            <IconSet.Plus size={11} /> Afegir servei
          </button>
        </div>
        <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
          {cfg.services_catalog.map((svc, idx) => (
            <div key={svc.id} className="grid grid-cols-[1fr_80px_80px_auto] gap-2 items-center">
              <input className={inp} value={svc.name} placeholder="Nom del servei"
                onChange={e => set('services_catalog', cfg.services_catalog.map((s, i) => i === idx ? { ...s, name: e.target.value } : s))} />
              <input type="number" className={inp} value={svc.price} placeholder="Preu"
                onChange={e => set('services_catalog', cfg.services_catalog.map((s, i) => i === idx ? { ...s, price: Number(e.target.value) } : s))} />
              <input className={inp} value={svc.unit} placeholder="unitat"
                onChange={e => set('services_catalog', cfg.services_catalog.map((s, i) => i === idx ? { ...s, unit: e.target.value } : s))} />
              <button type="button" onClick={() => set('services_catalog', cfg.services_catalog.filter((_, i) => i !== idx))}
                className="text-ink-3 hover:text-red-400 transition p-1">
                <IconSet.Trash size={12} />
              </button>
            </div>
          ))}
          {cfg.services_catalog.length === 0 && (
            <p className="text-sm text-ink-3 italic py-2">Cap servei. Fes clic a "Afegir servei" o aplica la plantilla del sector.</p>
          )}
        </div>
      </div>

      <div className="pt-2">
        <AMGButton onClick={handleSave} loading={saving}>Desar i continuar →</AMGButton>
      </div>
    </div>
  );
}

// ── Step: F4 — Seguiment ─────────────────────────────────────────────────────

function F4Step({ tenant, existingConfig, onSave }: {
  tenant: TenantResponse;
  existingConfig?: FidelitzacioConfig | null;
  onSave: (cfg: FidelitzacioConfig) => Promise<void>;
}) {
  const [cfg, setCfg] = useState<FidelitzacioConfig>(existingConfig ?? DEFAULT_FIDELITZACIO);
  const [saving, setSaving] = useState(false);
  const set = <K extends keyof FidelitzacioConfig>(k: K, v: FidelitzacioConfig[K]) => setCfg(c => ({ ...c, [k]: v }));

  const handleSave = async () => { setSaving(true); try { await onSave(cfg); } finally { setSaving(false); } };

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-base font-semibold text-ink-0">F4 — Seguiment</h2>
        <p className="text-xs text-ink-3 mt-0.5">Configura el seguiment post-servei i les ressenyes de Google.</p>
      </div>

      <div>
        <label className={lbl}>URL de ressenyes Google (opcional)</label>
        <input className={inp} value={cfg.google_reviews_url} placeholder="https://g.page/r/..."
          onChange={e => set('google_reviews_url', e.target.value)} />
        <p className="f-mono text-[10px] text-ink-3 mt-1">L'agent enviarà aquest enllaç als clients satisfets.</p>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className={lbl}>Seguiment inicial (dies)</label>
          <input type="number" className={inp} value={cfg.followup_days} min={1} max={30}
            onChange={e => set('followup_days', Number(e.target.value))} />
        </div>
        <div>
          <label className={lbl}>Reactivació inactius (mesos)</label>
          <input type="number" className={inp} value={cfg.reengagement_months} min={1} max={24}
            onChange={e => set('reengagement_months', Number(e.target.value))} />
        </div>
      </div>

      <div>
        <label className={lbl}>Missatge de seguiment</label>
        <textarea className={`${inp} resize-none`} rows={3} value={cfg.followup_template}
          onChange={e => set('followup_template', e.target.value)} />
        <p className="f-mono text-[10px] text-ink-3 mt-1">Variables: {'{{nom}}'}, {'{{url_ressenya}}'}</p>
      </div>

      <div>
        <label className={lbl}>Missatge reactivació</label>
        <textarea className={`${inp} resize-none`} rows={3} value={cfg.reengagement_template}
          onChange={e => set('reengagement_template', e.target.value)} />
        <p className="f-mono text-[10px] text-ink-3 mt-1">Variables: {'{{nom}}'}</p>
      </div>

      <div className="pt-2">
        <AMGButton onClick={handleSave} loading={saving}>Desar i continuar →</AMGButton>
      </div>
    </div>
  );
}

// ── Step: F5 — Alertes & Equip ───────────────────────────────────────────────

function F5Step({ existingConfig, onSave }: {
  existingConfig?: EquipConfig | null;
  onSave: (cfg: EquipConfig) => Promise<void>;
}) {
  const [cfg, setCfg] = useState<EquipConfig>(existingConfig ?? DEFAULT_EQUIP);
  const [saving, setSaving] = useState(false);
  const set = <K extends keyof EquipConfig>(k: K, v: EquipConfig[K]) => setCfg(c => ({ ...c, [k]: v }));

  const handleSave = async () => { setSaving(true); try { await onSave(cfg); } finally { setSaving(false); } };

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-base font-semibold text-ink-0">F5 — Alertes & Equip</h2>
        <p className="text-xs text-ink-3 mt-0.5">Configura les alertes al grup de Telegram i els informes diaris.</p>
      </div>

      <div className="p-4 bg-[rgba(255,107,0,0.04)] border border-[rgba(255,107,0,0.2)] rounded space-y-1">
        <p className="text-xs text-ink-2">Per obtenir el <strong>Telegram Group ID</strong>, afegeix el bot <span className="text-accent-light">@userinfobot</span> al grup i escriu <code className="bg-[rgba(255,255,255,0.08)] px-1 rounded">/start</code>.</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label className={lbl}>Telegram Group ID *</label>
          <input className={inp} value={cfg.telegram_group_id} placeholder="-1001234567890"
            onChange={e => set('telegram_group_id', e.target.value)} />
        </div>
        <div>
          <label className={lbl}>Nom del grup (opcional)</label>
          <input className={inp} value={cfg.telegram_group_name} placeholder="Equip de Vendes"
            onChange={e => set('telegram_group_name', e.target.value)} />
        </div>
      </div>

      <div className="space-y-3">
        <label className="flex items-center gap-3 cursor-pointer">
          <input type="checkbox" checked={cfg.daily_report_enabled} className="accent-[#FF6B00]"
            onChange={e => set('daily_report_enabled', e.target.checked)} />
          <span className="text-sm text-ink-1">Activar informe diari automàtic</span>
        </label>

        {cfg.daily_report_enabled && (
          <div>
            <label className={lbl}>Hora de l'informe</label>
            <input type="time" className={`${inp} w-32`} value={cfg.daily_report_time}
              onChange={e => set('daily_report_time', e.target.value)} />
          </div>
        )}

        <label className="flex items-center gap-3 cursor-pointer">
          <input type="checkbox" checked={cfg.unresponded_alert_enabled} className="accent-[#FF6B00]"
            onChange={e => set('unresponded_alert_enabled', e.target.checked)} />
          <span className="text-sm text-ink-1">Alertes de leads sense resposta</span>
        </label>

        {cfg.unresponded_alert_enabled && (
          <div>
            <label className={lbl}>Hores límit sense resposta</label>
            <input type="number" className={`${inp} w-24`} value={cfg.unresponded_hours_threshold} min={1} max={48}
              onChange={e => set('unresponded_hours_threshold', Number(e.target.value))} />
          </div>
        )}
      </div>

      <div className="pt-2">
        <AMGButton onClick={handleSave} loading={saving}>Desar i continuar →</AMGButton>
      </div>
    </div>
  );
}

// ── Step: Agent IA ───────────────────────────────────────────────────────────

function AgentStep({ tenantId, tenant, channels, onSave }: {
  tenantId: string;
  tenant: TenantResponse;
  channels?: ChannelsConfig | null;
  onSave: (prompt: string, mode: string) => Promise<void>;
}) {
  const [prompt, setPrompt] = useState(tenant.agentSystemPrompt ?? '');
  const [mode, setMode] = useState<string>(channels?.agentMode ?? 'AUTO');
  const [saving, setSaving] = useState(false);
  const { toast } = useToast();

  const applyTemplate = (sector: string) => {
    const ctx = SECTOR_CONTEXTS[sector];
    if (ctx) { setPrompt(ctx.systemPrompt.replace('{NOM_NEGOCI}', tenant.name ?? 'el negoci')); }
  };

  const handleSave = async () => { setSaving(true); try { await onSave(prompt, mode); } finally { setSaving(false); } };

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-base font-semibold text-ink-0">Configuració de l'Agent IA</h2>
        <p className="text-xs text-ink-3 mt-0.5">Defineix el comportament de l'agent i els canals de comunicació.</p>
      </div>

      {/* Mode */}
      <div>
        <label className={lbl}>Mode de resposta</label>
        <div className="flex flex-wrap gap-2">
          {[
            { key: 'AUTO',   label: 'Automàtic',  desc: "L'agent respon immediatament" },
            { key: 'HYBRID', label: 'Híbrid',      desc: 'Respostes pendents d\'aprovació' },
            { key: 'MANUAL', label: 'Manual',      desc: 'Només notifica' },
          ].map(({ key, label, desc }) => (
            <button key={key} type="button" onClick={() => setMode(key)}
              className={`flex-1 min-w-[120px] text-left px-3 py-2.5 border rounded transition ${
                mode === key ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.12)] text-white' : 'border-border-base hover:border-ink-2 text-ink-2'
              }`}>
              <div className="text-sm font-semibold">{label}</div>
              <div className="text-[10px] opacity-70">{desc}</div>
            </button>
          ))}
        </div>
      </div>

      {/* Prompt */}
      <div>
        <div className="flex items-center justify-between mb-2 flex-wrap gap-2">
          <label className={lbl}>Prompt del sistema</label>
          {tenant.sector && SECTOR_CONTEXTS[tenant.sector] && (
            <button type="button" onClick={() => applyTemplate(tenant.sector!)}
              className="flex items-center gap-1 text-xs text-accent-light hover:text-accent f-mono">
              <IconSet.Sparkles size={11} />
              Aplicar plantilla {SECTOR_LABELS[tenant.sector!] ?? tenant.sector}
            </button>
          )}
        </div>
        <textarea className={`${inp} resize-y`} rows={10} value={prompt}
          onChange={e => setPrompt(e.target.value)}
          placeholder="Ets l'assistent virtual de [negoci]..." />
        {!prompt && <p className="f-mono text-[10px] text-amber-400 mt-1">Aplica una plantilla de sector o escriu un prompt personalitzat.</p>}
      </div>

      {/* Channel status (info only) */}
      <div className="p-4 border border-border-base rounded space-y-2">
        <div className="f-mono text-[10px] uppercase tracking-wider text-ink-3">Canals configurats</div>
        <div className="flex flex-wrap gap-3 text-xs">
          <span className={`flex items-center gap-1.5 ${channels?.telegramLinked ? 'text-green-400' : 'text-ink-3'}`}>
            {channels?.telegramLinked ? '✓' : '○'} Telegram
          </span>
          <span className={`flex items-center gap-1.5 ${channels?.whatsappPhoneNumber || channels?.whatsappMetaPhoneNumberId ? 'text-green-400' : 'text-ink-3'}`}>
            {channels?.whatsappPhoneNumber || channels?.whatsappMetaPhoneNumberId ? '✓' : '○'} WhatsApp
          </span>
        </div>
        <p className="f-mono text-[10px] text-ink-3">Els canals es poden configurar des de la pàgina del tenant.</p>
      </div>

      <div className="pt-2">
        <AMGButton onClick={handleSave} loading={saving}>Desar i continuar →</AMGButton>
      </div>
    </div>
  );
}

// ── Step: Done ───────────────────────────────────────────────────────────────

function DoneStep({ tenant, selectedPhases, tenantId, locale, onRestart }: {
  tenant: TenantResponse;
  selectedPhases: string[];
  tenantId: string;
  locale: string;
  onRestart: () => void;
}) {
  const router = useRouter();
  return (
    <div className="space-y-6">
      <div className="text-center py-6">
        <div className="w-16 h-16 mx-auto rounded-full bg-[rgba(57,211,83,0.15)] border border-[rgba(57,211,83,0.3)] flex items-center justify-center mb-4">
          <IconSet.Check size={28} className="text-[#39d353]" />
        </div>
        <h2 className="text-lg font-bold text-ink-0">Configuració completada!</h2>
        <p className="text-sm text-ink-2 mt-1">{tenant.name} està llest per posar en marxa.</p>
      </div>

      <div className="space-y-2">
        <div className="f-mono text-[10px] uppercase tracking-wider text-ink-3">Fases configurades</div>
        {selectedPhases.map(phase => {
          const info = PHASE_INFO[phase];
          const Icon = info?.icon ?? IconSet.Check;
          return (
            <div key={phase} className="flex items-center gap-3 p-3 bg-[rgba(57,211,83,0.05)] border border-[rgba(57,211,83,0.2)] rounded">
              <IconSet.Check size={13} className="text-[#39d353] shrink-0" />
              <Icon size={13} className="text-ink-3 shrink-0" />
              <span className="text-sm text-ink-1">{phase} — {info?.label}</span>
            </div>
          );
        })}
      </div>

      <div className="flex flex-wrap gap-3 pt-2">
        <AMGButton onClick={() => router.push(`/${locale}/portal/admin/tenants/${tenantId}`)}>
          Anar al tenant →
        </AMGButton>
        <AMGButton variant="secondary" onClick={() => router.push(`/${locale}/portal/admin/tenants/${tenantId}/activate`)}>
          Activar Bot IA →
        </AMGButton>
        <AMGButton variant="ghost" onClick={onRestart}>
          Reiniciar wizard
        </AMGButton>
      </div>
    </div>
  );
}

// ── Main page ────────────────────────────────────────────────────────────────

export default function TenantWizardPage() {
  const params = useParams<{ id: string; locale: string }>();
  const tenantId = params.id;
  const locale = params.locale ?? 'ca';
  const router = useRouter();
  const qc = useQueryClient();
  const { toast } = useToast();

  const { data: tenant, isLoading: loadingTenant } = useQuery({
    queryKey: ['tenant', tenantId],
    queryFn: () => getTenant(tenantId),
  });

  const { data: configs = {} } = useQuery({
    queryKey: ['nexe-configs', tenantId],
    queryFn: () => getNexeConfigs(tenantId),
    enabled: !!tenantId,
  });

  const { data: channels } = useQuery({
    queryKey: ['agent-channels', tenantId],
    queryFn: () => getAgentChannels(tenantId),
    enabled: !!tenantId,
  });

  // Wizard state
  const [currentStep, setCurrentStep] = useState(0);
  const [selectedPhases, setSelectedPhases] = useState<string[]>([]);
  const [completedIds, setCompletedIds] = useState<Set<string>>(new Set());
  const [hydrated, setHydrated] = useState(false);

  // Load from localStorage on mount
  useEffect(() => {
    if (!tenantId || !tenant) return;
    const saved = loadWizardState(tenantId);
    if (saved) {
      setCurrentStep(saved.currentStep);
      setSelectedPhases(saved.selectedPhases);
      setCompletedIds(new Set(
        ['general', ...saved.selectedPhases.map(p => `phase-${p}`), 'agent', 'done']
          .slice(0, saved.currentStep + 1)
      ));
    } else {
      setSelectedPhases(tenant.contractedPhases ?? []);
    }
    setHydrated(true);
  }, [tenantId, tenant]);

  // Compute steps dynamically
  const steps = [
    { id: 'general', label: 'Info general' },
    { id: 'phases', label: 'Fases' },
    ...selectedPhases.sort().map(p => ({ id: `phase-${p}`, label: `${p} — ${PHASE_INFO[p]?.label ?? p}` })),
    { id: 'agent', label: 'Agent IA' },
    { id: 'done', label: 'Completat' },
  ];

  const saveState = useCallback((step: number, phases: string[]) => {
    saveWizardState(tenantId, { currentStep: step, selectedPhases: phases });
  }, [tenantId]);

  const advance = (stepId: string, phases?: string[]) => {
    const phasesToUse = phases ?? selectedPhases;
    const newSteps = [
      { id: 'general' }, { id: 'phases' },
      ...phasesToUse.sort().map(p => ({ id: `phase-${p}` })),
      { id: 'agent' }, { id: 'done' },
    ];
    const newIdx = Math.min(currentStep + 1, newSteps.length - 1);
    setCompletedIds(prev => new Set(Array.from(prev).concat(stepId)));
    setCurrentStep(newIdx);
    saveState(newIdx, phasesToUse);
    qc.invalidateQueries({ queryKey: ['tenant', tenantId] });
    qc.invalidateQueries({ queryKey: ['nexe-configs', tenantId] });
  };

  const invalidateAll = () => {
    qc.invalidateQueries({ queryKey: ['tenant', tenantId] });
    qc.invalidateQueries({ queryKey: ['nexe-configs', tenantId] });
    qc.invalidateQueries({ queryKey: ['agent-channels', tenantId] });
  };

  const handleRestart = () => {
    clearWizardState(tenantId);
    setCurrentStep(0);
    setSelectedPhases(tenant?.contractedPhases ?? []);
    setCompletedIds(new Set());
  };

  // Step handlers
  const handleGeneralSave = async (data: Partial<TenantResponse>) => {
    const payload = Object.fromEntries(Object.entries(data).filter(([, v]) => v !== null)) as UpdateTenantRequest;
    await updateTenant(tenantId, payload);
    toast('success', 'Informació desada');
    advance('general');
  };

  const handlePhasesSave = async (phases: string[], sector: string, size: string) => {
    await updateTenant(tenantId, {
      contractedPhases: phases,
      sector: sector || null,
      businessSize: size || null,
    });
    setSelectedPhases(phases);
    toast('success', `${phases.length} fases guardades`);
    advance('phases', phases);
  };

  const handleF1Save = async () => {
    toast('success', 'F1 Captació configurada');
    advance('phase-F1');
  };

  const handleF2Save = async (cfg: AgendaConfig) => {
    await saveNexeConfig(tenantId, 'AGENDA', cfg);
    await updateTenant(tenantId, { contractedPhases: Array.from(new Set((tenant?.contractedPhases ?? []).concat(['F2']))) });
    toast('success', 'Agenda desada — F2 activada');
    advance('phase-F2');
  };

  const handleF3Save = async (cfg: PressupostosConfig) => {
    await saveNexeConfig(tenantId, 'PRESSUPOSTOS', cfg);
    await updateTenant(tenantId, { contractedPhases: Array.from(new Set((tenant?.contractedPhases ?? []).concat(['F3']))) });
    toast('success', 'Pressupostos desats — F3 activada');
    advance('phase-F3');
  };

  const handleF4Save = async (cfg: FidelitzacioConfig) => {
    await saveNexeConfig(tenantId, 'FIDELITZACIO', cfg);
    await updateTenant(tenantId, { contractedPhases: Array.from(new Set((tenant?.contractedPhases ?? []).concat(['F4']))) });
    toast('success', 'Seguiment desat — F4 activada');
    advance('phase-F4');
  };

  const handleF5Save = async (cfg: EquipConfig) => {
    await saveNexeConfig(tenantId, 'EQUIP', cfg);
    await updateTenant(tenantId, { contractedPhases: Array.from(new Set((tenant?.contractedPhases ?? []).concat(['F5']))) });
    toast('success', 'Equip desat — F5 activada');
    advance('phase-F5');
  };

  const handleAgentSave = async (prompt: string, agentMode: string) => {
    await updateTenant(tenantId, { agentSystemPrompt: prompt });
    await updateAgentChannels(tenantId, { agentMode: agentMode as 'AUTO' | 'HYBRID' | 'MANUAL' });
    await updateTenant(tenantId, { contractedPhases: Array.from(new Set((tenant?.contractedPhases ?? []).concat(['F1']))) });
    invalidateAll();
    toast('success', 'Agent configurat — F1 activada');
    advance('agent');
  };

  // Parse existing nexe configs
  const parseConfig = <T,>(key: string): T | null => {
    const raw = configs[key];
    if (!raw || raw === '{}' || raw === 'null') return null;
    try { return JSON.parse(raw) as T; } catch { return null; }
  };

  const currentStepObj = steps[currentStep];

  if (loadingTenant || !tenant || !hydrated) {
    return (
      <PortalShell breadcrumb="wizard" backHref={`/${locale}/portal/admin/tenants/${tenantId}`}>
        <div className="flex items-center justify-center h-full py-24">
          <span className="w-5 h-5 border-2 border-accent-light border-t-transparent rounded-full animate-spin" />
        </div>
      </PortalShell>
    );
  }

  return (
    <PortalShell breadcrumb={`admin · tenants · ${tenant.name} · wizard`} backHref={`/${locale}/portal/admin/tenants/${tenantId}`}>
      <div className="flex h-full min-h-0">

        {/* Sidebar */}
        <Sidebar steps={steps} current={currentStep} completedIds={completedIds} onGo={setCurrentStep} />

        {/* Main */}
        <div className="flex-1 flex flex-col min-w-0 overflow-hidden">

          {/* Top bar */}
          <div className="flex items-center justify-between px-6 py-3 border-b border-border-base gap-3 shrink-0">
            <div>
              <span className="f-mono text-[10px] uppercase tracking-widest text-accent-light">Wizard de configuració</span>
              <div className="text-sm font-semibold text-ink-0 mt-0.5">{tenant.name}</div>
            </div>
            <div className="flex items-center gap-3">
              <div className="hidden sm:flex items-center gap-2">
                <div className="w-32 h-1.5 bg-surface-base rounded-full overflow-hidden">
                  <div className="h-full bg-accent-light rounded-full transition-all"
                    style={{ width: `${Math.round((completedIds.size / Math.max(steps.length - 1, 1)) * 100)}%` }} />
                </div>
                <span className="f-mono text-[10px] text-ink-3">{completedIds.size}/{steps.length - 1}</span>
              </div>
              <button onClick={handleRestart}
                className="text-xs f-mono text-ink-3 hover:text-ink-1 flex items-center gap-1 transition">
                <IconSet.Refresh size={12} /> Reiniciar
              </button>
            </div>
          </div>

          {/* Step content */}
          <div className="flex-1 overflow-y-auto">
            <div className="max-w-2xl mx-auto p-6 sm:p-8">

              {/* Mobile step indicator */}
              <div className="lg:hidden f-mono text-[10px] uppercase tracking-wider text-ink-3 mb-4">
                Pas {currentStep + 1} de {steps.length} — {currentStepObj?.label}
              </div>

              {currentStepObj?.id === 'general' && (
                <GeneralStep tenant={tenant} onSave={handleGeneralSave} />
              )}

              {currentStepObj?.id === 'phases' && (
                <PhasesStep tenant={tenant} onSave={handlePhasesSave} />
              )}

              {currentStepObj?.id === 'phase-F1' && (
                <F1Step tenant={tenant} onSave={handleF1Save} />
              )}

              {currentStepObj?.id === 'phase-F2' && (
                <F2Step
                  tenantId={tenantId}
                  tenant={tenant}
                  existingConfig={parseConfig<AgendaConfig>('AGENDA')}
                  onSave={handleF2Save}
                />
              )}

              {currentStepObj?.id === 'phase-F3' && (
                <F3Step
                  tenant={tenant}
                  existingConfig={parseConfig<PressupostosConfig>('PRESSUPOSTOS')}
                  onSave={handleF3Save}
                />
              )}

              {currentStepObj?.id === 'phase-F4' && (
                <F4Step
                  tenant={tenant}
                  existingConfig={parseConfig<FidelitzacioConfig>('FIDELITZACIO')}
                  onSave={handleF4Save}
                />
              )}

              {currentStepObj?.id === 'phase-F5' && (
                <F5Step
                  existingConfig={parseConfig<EquipConfig>('EQUIP')}
                  onSave={handleF5Save}
                />
              )}

              {currentStepObj?.id === 'agent' && (
                <AgentStep
                  tenantId={tenantId}
                  tenant={tenant}
                  channels={channels}
                  onSave={handleAgentSave}
                />
              )}

              {currentStepObj?.id === 'done' && (
                <DoneStep
                  tenant={tenant}
                  selectedPhases={selectedPhases}
                  tenantId={tenantId}
                  locale={locale}
                  onRestart={handleRestart}
                />
              )}

              {/* Back button (not on first step or done) */}
              {currentStep > 0 && currentStepObj?.id !== 'done' && (
                <div className="mt-6 pt-4 border-t border-border-base">
                  <button onClick={() => setCurrentStep(s => Math.max(0, s - 1))}
                    className="flex items-center gap-1.5 text-xs text-ink-3 hover:text-ink-1 f-mono transition">
                    <IconSet.ArrowRight size={12} className="rotate-180" /> Tornar
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </PortalShell>
  );
}
