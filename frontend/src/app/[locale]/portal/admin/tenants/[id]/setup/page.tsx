'use client';

import { useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getTenant, getNexeSetup, toggleContractedPhase, sendCommTemplate,
  type NexeSetupPhaseStatus, type NexeSetupPrerequisites,
} from '@/services/admin';
import {
  getNexeConfigs, saveNexeConfig,
  getCalendarOAuthUrl, provisionCalendar,
  getAgendaDefaults, getPressupostosDefaults, DEFAULT_FIDELITZACIO, DEFAULT_EQUIP,
  type AgendaConfig, type PressupostosConfig, type FidelitzacioConfig, type EquipConfig,
} from '@/services/nexe-configs';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { IconSet } from '@/components/ui/icons';
import { useToast } from '@/lib/toast-context';

// ── Styles ────────────────────────────────────────────────────────────────────

const inp = 'w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00] placeholder:text-ink-3';
const lbl = 'f-mono text-[10px] uppercase tracking-wider text-ink-3 block mb-1.5';
const sel = `${inp} cursor-pointer`;

// ── Phase metadata ────────────────────────────────────────────────────────────

const PHASE_META: Record<string, { label: string; desc: string; color: string }> = {
  F1: { label: 'Captació', desc: 'Landing, web o WhatsApp directe com a canal d\'entrada de leads.', color: '#FF6B00' },
  F2: { label: 'Agenda', desc: 'Cites automàtiques, recordatoris i gestió d\'absències.', color: '#3b82f6' },
  F3: { label: 'Pressupostos', desc: 'L\'agent genera pressupostos i fa seguiment.', color: '#8b5cf6' },
  F4: { label: 'Seguiment', desc: 'Ressenyes, reactivació de clients inactius i seguiment post-servei.', color: '#10b981' },
  F5: { label: 'Alertes & Equip', desc: 'Notificacions al grup de Telegram i informe diari.', color: '#f59e0b' },
};

// ── Helper components ─────────────────────────────────────────────────────────

function StatusDot({ ok, label }: { ok: boolean; label: string }) {
  return (
    <span className={`inline-flex items-center gap-1.5 text-xs f-mono ${ok ? 'text-[#39d353]' : 'text-amber-400'}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${ok ? 'bg-[#39d353]' : 'bg-amber-400 animate-pulse'}`} />
      {label}
    </span>
  );
}

function PhaseToggle({ isActive, onToggle, loading }: { isActive: boolean; onToggle: () => void; loading: boolean }) {
  return (
    <button
      onClick={e => { e.stopPropagation(); onToggle(); }}
      disabled={loading}
      title={isActive ? 'Desactivar fase' : 'Activar fase'}
      className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer items-center rounded-full border-2 transition-colors focus:outline-none disabled:opacity-40 ${
        isActive ? 'bg-[#FF6B00] border-[#FF6B00]' : 'bg-[rgba(255,255,255,0.08)] border-border-base'
      }`}
    >
      <span className={`pointer-events-none inline-block h-3.5 w-3.5 rounded-full bg-white shadow transition-transform ${
        isActive ? 'translate-x-3.5' : 'translate-x-0.5'
      }`} />
    </button>
  );
}

// ── Prerequisites bar ─────────────────────────────────────────────────────────

function PrerequisiteBar({ prereqs, locale, tenantId }: {
  prereqs: NexeSetupPrerequisites;
  locale: string;
  tenantId: string;
}) {
  const router = useRouter();
  const items = [
    {
      label: 'WhatsApp',
      ok: prereqs.whatsappConnected,
      action: () => router.push(`/${locale}/portal/admin/tenants/${tenantId}/diagnostic`),
      actionLabel: 'Connectar',
    },
    {
      label: 'Agent IA',
      ok: prereqs.agentConfigured,
      action: () => router.push(`/${locale}/portal/admin/tenants/${tenantId}/wizard`),
      actionLabel: 'Configurar',
    },
    {
      label: prereqs.landingCount > 0 ? `Landing (${prereqs.landingCount})` : 'Landing',
      ok: prereqs.landingCount > 0,
      action: () => router.push(`/${locale}/portal/landings`),
      actionLabel: 'Crear landing',
    },
  ];

  return (
    <div className="amg-card card-clip p-4 flex flex-wrap gap-4 items-center">
      <span className="f-mono text-[10px] uppercase tracking-widest text-ink-3 shrink-0">Prerequisits</span>
      {items.map(item => (
        <div key={item.label} className="flex items-center gap-2">
          <StatusDot ok={item.ok} label={item.label} />
          {!item.ok && (
            <button onClick={item.action} className="f-mono text-[10px] text-accent-light hover:underline">
              {item.actionLabel} →
            </button>
          )}
        </div>
      ))}
    </div>
  );
}

// ── "Demanar al client" modal ─────────────────────────────────────────────────

type TemplateAction = 'SETUP_CALENDAR_AUTH' | 'SETUP_REVIEWS_URL' | 'SETUP_TELEGRAM_GROUP' | 'SETUP_SERVICE_CATALOG' | 'SETUP_WEB_URL';

interface AskClientModalProps {
  action: TemplateAction;
  tenantId: string;
  tenantPhone: string | null;
  tenantEmail: string | null;
  tenantName: string;
  sector: string | null;
  extraVars?: Record<string, string>;
  onClose: () => void;
}

const ACTION_LABELS: Record<TemplateAction, string> = {
  SETUP_CALENDAR_AUTH:    'Autorització Google Calendar',
  SETUP_REVIEWS_URL:      'URL ressenyes Google',
  SETUP_TELEGRAM_GROUP:   'Grup de Telegram',
  SETUP_SERVICE_CATALOG:  'Catàleg de serveis',
  SETUP_WEB_URL:          'URL de la web',
};

function AskClientModal({ action, tenantId, tenantPhone, tenantEmail, tenantName, sector, extraVars = {}, onClose }: AskClientModalProps) {
  const { toast } = useToast();
  const [channel, setChannel] = useState<'WHATSAPP' | 'EMAIL'>(tenantPhone ? 'WHATSAPP' : 'EMAIL');
  const [to, setTo] = useState(channel === 'WHATSAPP' ? (tenantPhone ?? '') : (tenantEmail ?? ''));
  const [sending, setSending] = useState(false);

  const handleChannelChange = (ch: 'WHATSAPP' | 'EMAIL') => {
    setChannel(ch);
    setTo(ch === 'WHATSAPP' ? (tenantPhone ?? '') : (tenantEmail ?? ''));
  };

  const handleSend = async () => {
    if (!to.trim()) { toast('error', 'Introdueix un destinatari'); return; }
    setSending(true);
    try {
      await sendCommTemplate({
        channel,
        to: to.trim(),
        action,
        sector: sector ?? undefined,
        language: 'ca',
        variables: {
          NOM_CLIENT: tenantName,
          NOM_NEGOCI: tenantName,
          NOM_AGENCIA: 'AMG Digitalització',
          NOM_BOT: 'amgdigitalitzacio_bot',
          ...extraVars,
        },
      });
      toast('success', 'Missatge enviat correctament');
      onClose();
    } catch {
      toast('error', 'Error enviant el missatge');
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/70 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-md" onClick={e => e.stopPropagation()}>
        <div className="p-5 border-b border-border-base">
          <div className="f-display font-bold text-sm">Demanar al client</div>
          <div className="f-mono text-xs text-ink-3 mt-0.5">{ACTION_LABELS[action]}</div>
        </div>
        <div className="p-5 space-y-4">
          <div>
            <label className={lbl}>Canal</label>
            <div className="flex gap-2">
              {(['WHATSAPP', 'EMAIL'] as const).map(ch => (
                <button key={ch} type="button" onClick={() => handleChannelChange(ch)}
                  className={`px-3 py-1.5 rounded text-xs f-mono border transition ${
                    channel === ch ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.12)] text-white' : 'border-border-base text-ink-2 hover:border-ink-2'
                  }`}>
                  {ch === 'WHATSAPP' ? '📱 WhatsApp' : '✉️ Email'}
                </button>
              ))}
            </div>
          </div>
          <div>
            <label className={lbl}>{channel === 'WHATSAPP' ? 'Número WhatsApp' : 'Adreça email'}</label>
            <input className={inp} value={to} onChange={e => setTo(e.target.value)}
              placeholder={channel === 'WHATSAPP' ? '+34612345678' : 'client@negoci.com'} />
          </div>
          <div className="flex gap-3 pt-2">
            <AMGButton onClick={handleSend} loading={sending} disabled={!to.trim()} className="flex-1 justify-center">
              Enviar missatge
            </AMGButton>
            <AMGButton variant="outline" onClick={onClose}>Cancel·lar</AMGButton>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── F1 config ─────────────────────────────────────────────────────────────────

function F1Config({ tenantId, tenant, prereqs, onAskClient }: {
  tenantId: string; tenant: { agentSystemPrompt: string | null }; prereqs: NexeSetupPrerequisites;
  onAskClient: (action: TemplateAction) => void;
}) {
  const router = useRouter();
  const params = useParams();
  const locale = (params.locale as string) ?? 'ca';

  return (
    <div className="space-y-4">
      <div className="p-4 border border-border-base rounded space-y-3">
        <div className="flex items-center justify-between">
          <span className="text-sm font-medium">Canal d'entrada de leads</span>
        </div>
        <div className="space-y-2">
          <div className="flex items-center justify-between text-sm">
            <span className="text-ink-2">Landings publicades</span>
            <div className="flex items-center gap-2">
              <StatusDot ok={prereqs.landingCount > 0} label={prereqs.landingCount > 0 ? `${prereqs.landingCount} activa/es` : 'Cap'} />
              <button onClick={() => router.push(`/${locale}/portal/landings`)}
                className="f-mono text-[10px] text-accent-light hover:underline">Gestionar →</button>
            </div>
          </div>
          <div className="flex items-center justify-between text-sm">
            <span className="text-ink-2">Web existent</span>
            <button onClick={() => onAskClient('SETUP_WEB_URL')}
              className="f-mono text-[10px] text-accent-light hover:underline">📱 Demanar URL al client</button>
          </div>
        </div>
      </div>
      <div className="p-4 border border-border-base rounded flex items-center justify-between">
        <span className="text-sm text-ink-2">Agent IA configurat</span>
        <div className="flex items-center gap-2">
          <StatusDot ok={!!tenant.agentSystemPrompt} label={tenant.agentSystemPrompt ? 'Sí' : 'Pendent'} />
          <button onClick={() => router.push(`/${locale}/portal/admin/tenants/${tenantId}/wizard`)}
            className="f-mono text-[10px] text-accent-light hover:underline">Configurar →</button>
        </div>
      </div>
    </div>
  );
}

// ── F2 config ─────────────────────────────────────────────────────────────────

function F2Config({ tenantId, sector, existingConfig, oauthUrl, onOAuthRequest, onVerifyOAuth, onSave, onAskClient, loadingOauth, verifyingOauth }: {
  tenantId: string; sector: string | null; existingConfig: AgendaConfig | null;
  oauthUrl: string | null; onOAuthRequest: () => void; onVerifyOAuth: () => void;
  onSave: (cfg: AgendaConfig) => Promise<void>; onAskClient: (action: TemplateAction, extra?: Record<string, string>) => void;
  loadingOauth: boolean; verifyingOauth: boolean;
}) {
  const { toast } = useToast();
  const defaults = getAgendaDefaults(sector);
  const [cfg, setCfg] = useState<AgendaConfig>(existingConfig ?? defaults);
  const [saving, setSaving] = useState(false);
  const set = <K extends keyof AgendaConfig>(k: K, v: AgendaConfig[K]) => setCfg(c => ({ ...c, [k]: v }));

  const days = ['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday'];
  const dayLabels: Record<string, string> = { monday: 'Dll', tuesday: 'Dm', wednesday: 'Dc', thursday: 'Dj', friday: 'Dv', saturday: 'Ds', sunday: 'Dg' };

  return (
    <div className="space-y-5">
      {/* Calendar type */}
      <div>
        <label className={lbl}>Tipus de calendari</label>
        <div className="flex flex-wrap gap-2">
          {(['manual', 'google_oauth', 'calendly'] as const).map(t => (
            <button key={t} type="button" onClick={() => set('calendar_type', t)}
              className={`px-3 py-1.5 rounded text-xs f-mono border transition ${cfg.calendar_type === t ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.12)] text-white' : 'border-border-base text-ink-2 hover:border-ink-2'}`}>
              {t === 'manual' ? 'Manual' : t === 'google_oauth' ? 'Google Calendar' : 'Calendly'}
            </button>
          ))}
        </div>
      </div>

      {/* Google OAuth */}
      {cfg.calendar_type === 'google_oauth' && (
        <div className="p-4 border border-[rgba(255,107,0,0.3)] rounded bg-[rgba(255,107,0,0.04)] space-y-3">
          <div className="flex items-center justify-between flex-wrap gap-2">
            <span className="text-sm font-medium text-ink-1">Autorització Google Calendar</span>
            {!oauthUrl
              ? <AMGButton size="sm" onClick={onOAuthRequest} loading={loadingOauth}>Obtenir URL</AMGButton>
              : (
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="w-2 h-2 rounded-full bg-amber-400 animate-pulse" />
                  <span className="f-mono text-xs text-amber-400">Esperant autorització</span>
                  <AMGButton size="sm" variant="ghost" onClick={onVerifyOAuth} loading={verifyingOauth}>Verificar</AMGButton>
                </div>
              )
            }
          </div>
          {oauthUrl && (
            <div className="flex items-center gap-2">
              <code className="f-mono text-xs bg-[rgba(255,255,255,0.04)] border border-border-base px-2 py-1 rounded flex-1 truncate">{oauthUrl}</code>
              <button onClick={() => navigator.clipboard.writeText(oauthUrl)} className="text-xs text-accent-light shrink-0">Copiar</button>
              <button onClick={() => onAskClient('SETUP_CALENDAR_AUTH', { URL_OAUTH: oauthUrl })}
                className="f-mono text-xs text-accent-light hover:underline shrink-0">📱 Enviar WA</button>
            </div>
          )}
        </div>
      )}

      {/* Hours + duration */}
      <div className="grid grid-cols-3 gap-3">
        <div>
          <label className={lbl}>Durada cita (min)</label>
          <input type="number" className={inp} value={cfg.slot_duration_minutes} min={15} max={480}
            onChange={e => set('slot_duration_minutes', Number(e.target.value))} />
        </div>
        <div>
          <label className={lbl}>Marge (min)</label>
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

      <AMGButton onClick={async () => { setSaving(true); try { await onSave(cfg); toast('success', 'F2 desada'); } finally { setSaving(false); } }} loading={saving}>
        Desar F2
      </AMGButton>
    </div>
  );
}

// ── F3 config ─────────────────────────────────────────────────────────────────

function F3Config({ sector, existingConfig, onSave, onAskClient }: {
  sector: string | null; existingConfig: PressupostosConfig | null;
  onSave: (cfg: PressupostosConfig) => Promise<void>; onAskClient: (action: TemplateAction) => void;
}) {
  const { toast } = useToast();
  const defaults = getPressupostosDefaults(sector);
  const [cfg, setCfg] = useState<PressupostosConfig>(existingConfig ?? defaults);
  const [saving, setSaving] = useState(false);
  const set = <K extends keyof PressupostosConfig>(k: K, v: PressupostosConfig[K]) => setCfg(c => ({ ...c, [k]: v }));

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className={lbl}>Validesa pressupost (dies)</label>
          <input type="number" className={inp} value={cfg.quote_validity_days} min={1} max={365}
            onChange={e => set('quote_validity_days', Number(e.target.value))} />
        </div>
        <div>
          <label className={lbl}>Seguiment (dies sense resposta)</label>
          <input type="number" className={inp} value={cfg.quote_followup_days} min={1} max={30}
            onChange={e => set('quote_followup_days', Number(e.target.value))} />
        </div>
      </div>
      <div>
        <div className="flex items-center justify-between mb-2">
          <label className={lbl}>Catàleg de serveis ({cfg.services_catalog.length})</label>
          <div className="flex items-center gap-2">
            <button onClick={() => onAskClient('SETUP_SERVICE_CATALOG')}
              className="f-mono text-[10px] text-accent-light hover:underline">📱 Demanar al client</button>
            <button type="button" onClick={() => set('services_catalog', [...cfg.services_catalog, { id: crypto.randomUUID(), name: '', price: 0, unit: 'unitat', description: '' }])}
              className="text-xs text-accent-light f-mono flex items-center gap-1">
              <IconSet.Plus size={11} /> Afegir
            </button>
          </div>
        </div>
        <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
          {cfg.services_catalog.map((svc, idx) => (
            <div key={svc.id} className="grid grid-cols-[1fr_80px_60px_auto] gap-2 items-center">
              <input className={inp} value={svc.name} placeholder="Nom del servei"
                onChange={e => set('services_catalog', cfg.services_catalog.map((s, i) => i === idx ? { ...s, name: e.target.value } : s))} />
              <input type="number" className={inp} value={svc.price} placeholder="Preu"
                onChange={e => set('services_catalog', cfg.services_catalog.map((s, i) => i === idx ? { ...s, price: Number(e.target.value) } : s))} />
              <input className={inp} value={svc.unit} placeholder="unitat"
                onChange={e => set('services_catalog', cfg.services_catalog.map((s, i) => i === idx ? { ...s, unit: e.target.value } : s))} />
              <button type="button" onClick={() => set('services_catalog', cfg.services_catalog.filter((_, i) => i !== idx))}
                className="text-ink-3 hover:text-red-400 p-1"><IconSet.Trash size={12} /></button>
            </div>
          ))}
          {cfg.services_catalog.length === 0 && (
            <p className="text-sm text-ink-3 italic py-2">Cap servei. Afegeix manualment o demana'l al client.</p>
          )}
        </div>
      </div>
      <AMGButton onClick={async () => { setSaving(true); try { await onSave(cfg); toast('success', 'F3 desada'); } finally { setSaving(false); } }} loading={saving}>
        Desar F3
      </AMGButton>
    </div>
  );
}

// ── F4 config ─────────────────────────────────────────────────────────────────

function F4Config({ existingConfig, onSave, onAskClient }: {
  existingConfig: FidelitzacioConfig | null;
  onSave: (cfg: FidelitzacioConfig) => Promise<void>; onAskClient: (action: TemplateAction) => void;
}) {
  const { toast } = useToast();
  const [cfg, setCfg] = useState<FidelitzacioConfig>(existingConfig ?? DEFAULT_FIDELITZACIO);
  const [saving, setSaving] = useState(false);
  const set = <K extends keyof FidelitzacioConfig>(k: K, v: FidelitzacioConfig[K]) => setCfg(c => ({ ...c, [k]: v }));

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <div className="flex-1">
          <label className={lbl}>URL ressenyes Google</label>
          <input className={inp} value={cfg.google_reviews_url} placeholder="https://g.page/r/..."
            onChange={e => set('google_reviews_url', e.target.value)} />
        </div>
        <div className="pt-5">
          <button onClick={() => onAskClient('SETUP_REVIEWS_URL')}
            className="f-mono text-xs text-accent-light hover:underline whitespace-nowrap">📱 Demanar al client</button>
        </div>
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
      <AMGButton onClick={async () => { setSaving(true); try { await onSave(cfg); toast('success', 'F4 desada'); } finally { setSaving(false); } }} loading={saving}>
        Desar F4
      </AMGButton>
    </div>
  );
}

// ── F5 config ─────────────────────────────────────────────────────────────────

function F5Config({ existingConfig, onSave, onAskClient }: {
  existingConfig: EquipConfig | null;
  onSave: (cfg: EquipConfig) => Promise<void>; onAskClient: (action: TemplateAction) => void;
}) {
  const { toast } = useToast();
  const [cfg, setCfg] = useState<EquipConfig>(existingConfig ?? DEFAULT_EQUIP);
  const [saving, setSaving] = useState(false);
  const set = <K extends keyof EquipConfig>(k: K, v: EquipConfig[K]) => setCfg(c => ({ ...c, [k]: v }));

  return (
    <div className="space-y-4">
      <div className="p-3 bg-[rgba(255,107,0,0.04)] border border-[rgba(255,107,0,0.2)] rounded text-xs text-ink-2">
        Afegeix el bot <span className="text-accent-light">@amgdigitalitzacio_bot</span> al grup i escriu <code className="bg-[rgba(255,255,255,0.08)] px-1 rounded">/start</code> per obtenir l'ID.
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="flex items-end gap-2">
          <div className="flex-1">
            <label className={lbl}>Telegram Group ID *</label>
            <input className={inp} value={cfg.telegram_group_id} placeholder="-1001234567890"
              onChange={e => set('telegram_group_id', e.target.value)} />
          </div>
          <button onClick={() => onAskClient('SETUP_TELEGRAM_GROUP')}
            className="f-mono text-xs text-accent-light hover:underline mb-2 whitespace-nowrap">📱 Enviar instruccions</button>
        </div>
        <div>
          <label className={lbl}>Nom del grup (opcional)</label>
          <input className={inp} value={cfg.telegram_group_name} placeholder="Equip de Vendes"
            onChange={e => set('telegram_group_name', e.target.value)} />
        </div>
      </div>
      <label className="flex items-center gap-3 cursor-pointer">
        <input type="checkbox" checked={cfg.daily_report_enabled} className="accent-[#FF6B00]"
          onChange={e => set('daily_report_enabled', e.target.checked)} />
        <span className="text-sm text-ink-1">Informe diari automàtic</span>
      </label>
      {cfg.daily_report_enabled && (
        <div>
          <label className={lbl}>Hora de l'informe</label>
          <input type="time" className={`${inp} w-32`} value={cfg.daily_report_time}
            onChange={e => set('daily_report_time', e.target.value)} />
        </div>
      )}
      <AMGButton onClick={async () => { setSaving(true); try { await onSave(cfg); toast('success', 'F5 desada'); } finally { setSaving(false); } }} loading={saving}>
        Desar F5
      </AMGButton>
    </div>
  );
}

// ── Phase card ────────────────────────────────────────────────────────────────

function PhaseCard({ phase, tenantId, tenant, configs, prereqs, onToggle, toggling }: {
  phase: NexeSetupPhaseStatus;
  tenantId: string;
  tenant: { sector: string | null; email: string | null; phone: string | null; name: string; agentSystemPrompt: string | null };
  configs: Record<string, string>;
  prereqs: NexeSetupPrerequisites;
  onToggle: () => void;
  toggling: boolean;
}) {
  const { toast } = useToast();
  const qc = useQueryClient();
  const [expanded, setExpanded] = useState(!phase.isConfigured);
  const [askClient, setAskClient] = useState<{ action: TemplateAction; extra?: Record<string, string> } | null>(null);
  const [oauthUrl, setOauthUrl] = useState<string | null>(null);
  const [loadingOauth, setLoadingOauth] = useState(false);
  const [verifyingOauth, setVerifyingOauth] = useState(false);

  const meta = PHASE_META[phase.key];
  if (!meta) return null;

  const existingAgendaConfig: AgendaConfig | null = (() => {
    try { const v = configs['AGENDA']; return v ? JSON.parse(v) : null; } catch { return null; }
  })();
  const existingPressupostosConfig: PressupostosConfig | null = (() => {
    try { const v = configs['PRESSUPOSTOS']; return v ? JSON.parse(v) : null; } catch { return null; }
  })();
  const existingFidelitzacioConfig: FidelitzacioConfig | null = (() => {
    try { const v = configs['FIDELITZACIO']; return v ? JSON.parse(v) : null; } catch { return null; }
  })();
  const existingEquipConfig: EquipConfig | null = (() => {
    try { const v = configs['EQUIP']; return v ? JSON.parse(v) : null; } catch { return null; }
  })();

  const saveConfig = async (key: string, cfg: object) => {
    await saveNexeConfig(tenantId, key, cfg);
    qc.invalidateQueries({ queryKey: ['nexe-setup', tenantId] });
  };

  const handleOAuthRequest = async () => {
    setLoadingOauth(true);
    try {
      const res = await getCalendarOAuthUrl(tenantId);
      setOauthUrl(res.url);
    } catch { toast('error', 'Error obtenint URL d\'autorització'); }
    finally { setLoadingOauth(false); }
  };

  const handleVerifyOAuth = async () => {
    setVerifyingOauth(true);
    try {
      await provisionCalendar(tenantId);
      toast('success', 'Google Calendar autoritzat correctament');
    } catch { toast('error', 'L\'autorització no s\'ha completat encara'); }
    finally { setVerifyingOauth(false); }
  };

  return (
    <>
      <div className="amg-card card-clip overflow-hidden">
        <div className={`h-0.5`} style={{ background: phase.isConfigured ? '#39d353' : phase.isActive ? meta.color : '#334155' }} />
        {/* Header */}
        <div
          className="flex items-center gap-3 p-4 cursor-pointer select-none"
          onClick={() => setExpanded(e => !e)}
        >
          <div className="w-9 h-9 shrink-0 flex items-center justify-center f-mono font-bold text-sm border-2 transition-colors"
            style={{ borderColor: meta.color, color: meta.color, background: `${meta.color}18` }}>
            {phase.key}
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="f-display font-bold text-sm">{meta.label}</span>
              <AMGBadge tone={phase.isConfigured ? 'success' : 'warning'}>
                {phase.isConfigured ? 'Configurat' : 'Pendent'}
              </AMGBadge>
              {!phase.isActive && <AMGBadge tone="neutral">Inactiva</AMGBadge>}
            </div>
            <p className="f-mono text-[11px] text-ink-3 mt-0.5 truncate">{meta.desc}</p>
          </div>
          <div className="flex items-center gap-3 shrink-0" onClick={e => e.stopPropagation()}>
            <PhaseToggle isActive={phase.isActive} onToggle={onToggle} loading={toggling} />
            <span className={`text-ink-3 transition-transform ${expanded ? 'rotate-90' : ''}`}>
              <IconSet.Chevron size={16} />
            </span>
          </div>
        </div>

        {/* Body */}
        {expanded && phase.isActive && (
          <div className="p-4 border-t border-border-base">
            {phase.key === 'F1' && (
              <F1Config tenantId={tenantId} tenant={tenant} prereqs={prereqs}
                onAskClient={(action) => setAskClient({ action })} />
            )}
            {phase.key === 'F2' && (
              <F2Config tenantId={tenantId} sector={tenant.sector}
                existingConfig={existingAgendaConfig} oauthUrl={oauthUrl}
                onOAuthRequest={handleOAuthRequest} onVerifyOAuth={handleVerifyOAuth}
                onSave={cfg => saveConfig('AGENDA', cfg)}
                onAskClient={(action, extra) => setAskClient({ action, extra })}
                loadingOauth={loadingOauth} verifyingOauth={verifyingOauth} />
            )}
            {phase.key === 'F3' && (
              <F3Config sector={tenant.sector} existingConfig={existingPressupostosConfig}
                onSave={cfg => saveConfig('PRESSUPOSTOS', cfg)}
                onAskClient={(action) => setAskClient({ action })} />
            )}
            {phase.key === 'F4' && (
              <F4Config existingConfig={existingFidelitzacioConfig}
                onSave={cfg => saveConfig('FIDELITZACIO', cfg)}
                onAskClient={(action) => setAskClient({ action })} />
            )}
            {phase.key === 'F5' && (
              <F5Config existingConfig={existingEquipConfig}
                onSave={cfg => saveConfig('EQUIP', cfg)}
                onAskClient={(action) => setAskClient({ action })} />
            )}
          </div>
        )}
        {expanded && !phase.isActive && (
          <div className="p-4 border-t border-border-base text-xs text-ink-3 italic">
            Fase desactivada. Activa-la amb el toggle per configurar-la.
          </div>
        )}
      </div>

      {askClient && (
        <AskClientModal
          action={askClient.action}
          extraVars={askClient.extra}
          tenantId={tenantId}
          tenantPhone={tenant.phone}
          tenantEmail={tenant.email}
          tenantName={tenant.name}
          sector={tenant.sector}
          onClose={() => setAskClient(null)}
        />
      )}
    </>
  );
}

// ── Main page ─────────────────────────────────────────────────────────────────

export default function SetupPage() {
  const params = useParams();
  const { toast } = useToast();
  const qc = useQueryClient();
  const tenantId = params.id as string;
  const locale = (params.locale as string) ?? 'ca';
  const [togglingPhase, setTogglingPhase] = useState<string | null>(null);

  const { data: tenant } = useQuery({
    queryKey: ['tenant', tenantId],
    queryFn: () => getTenant(tenantId),
    enabled: !!tenantId,
  });

  const { data: setup, isLoading } = useQuery({
    queryKey: ['nexe-setup', tenantId],
    queryFn: () => getNexeSetup(tenantId),
    enabled: !!tenantId,
  });

  const { data: nexeConfigs } = useQuery({
    queryKey: ['nexe-configs', tenantId],
    queryFn: () => getNexeConfigs(tenantId),
    enabled: !!tenantId,
  });

  const configs: Record<string, string> = nexeConfigs ?? {};

  const handleTogglePhase = async (phase: string) => {
    setTogglingPhase(phase);
    try {
      await toggleContractedPhase(tenantId, phase);
      qc.invalidateQueries({ queryKey: ['nexe-setup', tenantId] });
      qc.invalidateQueries({ queryKey: ['tenant', tenantId] });
    } catch {
      toast('error', 'Error canviant l\'estat de la fase');
    } finally {
      setTogglingPhase(null);
    }
  };

  const configuredCount = setup?.phases.filter(p => p.isConfigured).length ?? 0;
  const totalCount = setup?.phases.length ?? 0;

  return (
    <PortalShell breadcrumb="tenants">
      <div className="p-4 sm:p-8 space-y-6">
        {/* Header */}
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ configuració /</span>
          <div className="f-display font-bold text-xl mt-1">
            {tenant?.name ?? '…'}
          </div>
          {totalCount > 0 && (
            <p className="f-mono text-xs text-ink-3 mt-1">
              {configuredCount} de {totalCount} fases configurades
            </p>
          )}
        </div>

        {/* Progress bar */}
        {totalCount > 0 && (
          <div className="h-1.5 bg-[rgba(255,255,255,0.06)] rounded-full overflow-hidden">
            <div
              className="h-full bg-[#FF6B00] rounded-full transition-all"
              style={{ width: `${(configuredCount / totalCount) * 100}%` }}
            />
          </div>
        )}

        {/* Prerequisites */}
        {setup && (
          <PrerequisiteBar prereqs={setup.prerequisites} locale={locale} tenantId={tenantId} />
        )}

        {/* Phase cards */}
        {isLoading ? (
          <div className="flex justify-center py-12">
            <span className="w-5 h-5 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
          </div>
        ) : !setup || setup.phases.length === 0 ? (
          <div className="amg-card card-clip p-8 text-center">
            <IconSet.Layers size={28} stroke="#64748b" className="mx-auto mb-3" />
            <div className="f-display font-bold text-sm mb-1">Cap fase contractada</div>
            <p className="text-ui text-ink-2 text-sm">
              Quan el client accepti el pressupost, les fases apareixeran aquí.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {setup.phases.map(phase => (
              <PhaseCard
                key={phase.key}
                phase={phase}
                tenantId={tenantId}
                tenant={{
                  sector: tenant?.sector ?? null,
                  email: tenant?.email ?? null,
                  phone: tenant?.phone ?? null,
                  name: tenant?.name ?? '',
                  agentSystemPrompt: tenant?.agentSystemPrompt ?? null,
                }}
                configs={configs}
                prereqs={setup.prerequisites}
                onToggle={() => handleTogglePhase(phase.key)}
                toggling={togglingPhase === phase.key}
              />
            ))}
          </div>
        )}
      </div>
    </PortalShell>
  );
}
