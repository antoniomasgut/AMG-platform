'use client';

import { useParams } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import React, { useState, useEffect } from 'react';
import { getTenant } from '@/services/admin';
import { apiFetch } from '@/services/api';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';

// ─── Types ─────────────────────────────────────────────────────

interface NotifConfig {
  enabled: boolean;
  telegram: { contactForm: boolean; chatWidgetNew: boolean; whatsappNew: boolean; emailNew: boolean; leadCreated: boolean; booking: boolean };
  email:    { contactForm: boolean; booking: boolean };
  quietHours: { start: number | null; end: number | null; timezone: string };
  cooldownMinutes: number;
}

const DEFAULT: NotifConfig = {
  enabled: true,
  telegram: { contactForm: true, chatWidgetNew: true, whatsappNew: true, emailNew: true, leadCreated: false, booking: true },
  email:    { contactForm: true, booking: true },
  quietHours: { start: null, end: null, timezone: 'Europe/Madrid' },
  cooldownMinutes: 0,
};

async function getNotifConfig(tenantId: string): Promise<NotifConfig> {
  return apiFetch(`/api/v1/notifications/tenants/${tenantId}/config`);
}

async function saveNotifConfig(tenantId: string, cfg: NotifConfig): Promise<NotifConfig> {
  return apiFetch(`/api/v1/notifications/tenants/${tenantId}/config`, { method: 'PUT', body: JSON.stringify(cfg) });
}

// ─── UI helpers ────────────────────────────────────────────────

function SectionCard({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="bg-surface-overlay border border-border-base rounded-md overflow-hidden mb-4">
      <div className="border-b border-border-base px-4 py-2.5">
        <span className="f-mono text-xs font-semibold text-ink-1 uppercase tracking-wider">{title}</span>
      </div>
      <div className="p-4 space-y-3">{children}</div>
    </div>
  );
}

function Toggle({ label, checked, onChange }: { label: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <label className="flex items-center gap-2 cursor-pointer select-none">
      <input type="checkbox" checked={checked} onChange={e => onChange(e.target.checked)}
        className="w-4 h-4 accent-orange-500" />
      <span className="text-sm text-ink-1">{label}</span>
    </label>
  );
}

const inp = "w-full bg-surface-base border border-border-base rounded px-3 py-1.5 text-sm text-ink-1 focus:outline-none focus:border-accent-light";

// ─── Page ──────────────────────────────────────────────────────

export default function NotificationsPage() {
  const { id: tenantId } = useParams<{ id: string }>();
  const qc = useQueryClient();

  const { data: tenant } = useQuery({ queryKey: ['tenant', tenantId], queryFn: () => getTenant(tenantId) });
  const { data: raw } = useQuery({ queryKey: ['notif-config', tenantId], queryFn: () => getNotifConfig(tenantId) });

  const [cfg, setCfg] = useState<NotifConfig>(DEFAULT);
  useEffect(() => { if (raw) setCfg({ ...DEFAULT, ...raw, telegram: { ...DEFAULT.telegram, ...raw.telegram }, email: { ...DEFAULT.email, ...raw.email }, quietHours: { ...DEFAULT.quietHours, ...raw.quietHours } }); }, [raw]);

  const setTg = (key: keyof NotifConfig['telegram'], v: boolean) =>
    setCfg(c => ({ ...c, telegram: { ...c.telegram, [key]: v } }));
  const setEm = (key: keyof NotifConfig['email'], v: boolean) =>
    setCfg(c => ({ ...c, email: { ...c.email, [key]: v } }));

  const mut = useMutation({
    mutationFn: () => saveNotifConfig(tenantId, cfg),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notif-config', tenantId] }),
  });

  const hasTelegram = !!(tenant as any)?.telegramConnected;

  return (
    <PortalShell breadcrumb={`admin · tenants · ${tenant?.name ?? tenantId} · notificacions`} backHref={`/portal/admin/tenants/${tenantId}`}>
      <div className="max-w-2xl mx-auto px-4 py-6">
        <div className="mb-6">
          <h1 className="text-lg font-semibold text-ink-0">Notificacions</h1>
          <p className="text-sm text-ink-2 mt-1">{tenant?.name}</p>
        </div>

        {!hasTelegram && (
          <div className="mb-4 p-3 bg-yellow-500/10 border border-yellow-500/30 rounded text-sm text-yellow-300">
            ⚠️ Telegram no vinculat — les notificacions instantànies no s'enviaran. Ves a
            <span className="font-mono ml-1">Configuració → Canals → Telegram</span> per vincular-lo.
          </div>
        )}

        <form onSubmit={e => { e.preventDefault(); mut.mutate(); }}>
          <SectionCard title="Activació global">
            <Toggle label="Activar notificacions proactives" checked={cfg.enabled}
              onChange={v => setCfg(c => ({ ...c, enabled: v }))} />
          </SectionCard>

          <SectionCard title="Telegram — per event">
            <Toggle label="Nou contacte via formulari de landing"     checked={cfg.telegram.contactForm}    onChange={v => setTg('contactForm', v)} />
            <Toggle label="Primera sessió de xat widget"             checked={cfg.telegram.chatWidgetNew}  onChange={v => setTg('chatWidgetNew', v)} />
            <Toggle label="Primer WhatsApp d'un contacte nou"        checked={cfg.telegram.whatsappNew}    onChange={v => setTg('whatsappNew', v)} />
            <Toggle label="Primer email d'un contacte nou"           checked={cfg.telegram.emailNew}       onChange={v => setTg('emailNew', v)} />
            <Toggle label="Cita confirmada per l'agent"              checked={cfg.telegram.booking}        onChange={v => setTg('booking', v)} />
            <Toggle label="Lead creat manualment (des del portal)"   checked={cfg.telegram.leadCreated}    onChange={v => setTg('leadCreated', v)} />
          </SectionCard>

          <SectionCard title="Email — per event">
            <Toggle label="Nou contacte via formulari de landing" checked={cfg.email.contactForm} onChange={v => setEm('contactForm', v)} />
            <Toggle label="Cita confirmada per l'agent"           checked={cfg.email.booking}     onChange={v => setEm('booking', v)} />
          </SectionCard>

          <SectionCard title="Horari de silenci (Telegram)">
            <p className="text-xs text-ink-2">Deixa en blanc per desactivar el silenci.</p>
            <div className="flex gap-3 items-center">
              <div className="flex-1">
                <label className="block text-xs text-ink-2 mb-1">Des de les (hora)</label>
                <input className={inp} type="number" min={0} max={23} placeholder="p.ex. 22"
                  value={cfg.quietHours.start ?? ''}
                  onChange={e => setCfg(c => ({ ...c, quietHours: { ...c.quietHours, start: e.target.value ? +e.target.value : null } }))} />
              </div>
              <div className="flex-1">
                <label className="block text-xs text-ink-2 mb-1">Fins a les (hora)</label>
                <input className={inp} type="number" min={0} max={23} placeholder="p.ex. 8"
                  value={cfg.quietHours.end ?? ''}
                  onChange={e => setCfg(c => ({ ...c, quietHours: { ...c.quietHours, end: e.target.value ? +e.target.value : null } }))} />
              </div>
            </div>
          </SectionCard>

          <SectionCard title="Anti-spam">
            <label className="block text-xs text-ink-2 mb-1">Temps mínim entre notificacions del mateix event (minuts, 0 = sense límit)</label>
            <input className={inp} type="number" min={0} max={60} value={cfg.cooldownMinutes}
              onChange={e => setCfg(c => ({ ...c, cooldownMinutes: +e.target.value }))} />
          </SectionCard>

          <div className="flex items-center gap-3">
            <AMGButton type="submit" disabled={mut.isPending}>
              {mut.isPending ? 'Guardant…' : 'Guardar configuració'}
            </AMGButton>
            {mut.isSuccess && <span className="text-sm text-green-400">✓ Guardat</span>}
            {mut.isError   && <span className="text-sm text-red-400">Error en guardar</span>}
          </div>
        </form>
      </div>
    </PortalShell>
  );
}
