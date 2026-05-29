'use client';

import { useParams, useRouter } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, useEffect } from 'react';
import { getTenant } from '@/services/admin';
import {
  getNexeConfig, saveNexeConfig,
  AgendaConfig, AgendaMode, getAgendaMode, getAgendaDefaults,
  PressupostosConfig, QuoteMode, getQuoteMode, getPressupostosDefaults, ServiceItem,
  FidelitzacioConfig, DEFAULT_FIDELITZACIO,
  EquipConfig, DEFAULT_EQUIP, TeamMember,
  ClientQuestion,
} from '@/services/nexe-configs';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { I } from '@/components/ui/icons';

// ─── Helpers ───────────────────────────────────────────────────

function parseConfig<T>(raw: string | undefined, def: T): T {
  if (!raw || raw === '{}') return def;
  try { return { ...def, ...JSON.parse(raw) }; } catch { return def; }
}

function SectionCard({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="bg-surface-overlay border border-border-base rounded-md overflow-hidden mb-4">
      <div className="border-b border-border-base px-4 py-2.5">
        <span className="f-mono text-xs font-semibold text-ink-1 uppercase tracking-wider">{title}</span>
      </div>
      <div className="p-4">{children}</div>
    </div>
  );
}

function Field({ label, children, hint }: { label: string; children: React.ReactNode; hint?: string }) {
  return (
    <div className="mb-3">
      <label className="block f-mono text-xs text-ink-2 mb-1 uppercase tracking-wide">{label}</label>
      {children}
      {hint && <p className="f-mono text-xs text-ink-3 mt-1">{hint}</p>}
    </div>
  );
}

const inp = "w-full bg-surface-base border border-border-base rounded px-3 py-2 text-sm text-ink-0 focus:outline-none focus:border-accent placeholder:text-ink-3";
const sel = inp + " appearance-none";

function SaveRow({ isPending, isSuccess, isError }: { isPending: boolean; isSuccess: boolean; isError: boolean }) {
  return (
    <div className="flex justify-end gap-3 pt-2">
      <AMGButton type="submit" loading={isPending}>Desar configuració</AMGButton>
      {isSuccess && <span className="f-mono text-xs text-green-400 self-center">Guardat!</span>}
      {isError && <span className="f-mono text-xs text-red-400 self-center">Error en desar</span>}
    </div>
  );
}

// ─── Working hours grid (shared) ───────────────────────────────

const DAYS = [
  { key: 'monday', label: 'Dl' }, { key: 'tuesday', label: 'Dm' },
  { key: 'wednesday', label: 'Dc' }, { key: 'thursday', label: 'Dj' },
  { key: 'friday', label: 'Dv' }, { key: 'saturday', label: 'Ds' },
  { key: 'sunday', label: 'Dg' },
];

function WorkingHoursGrid({ hours, onChange }: {
  hours: Record<string, { enabled: boolean; start: string; end: string }>;
  onChange: (day: string, field: string, value: string | boolean) => void;
}) {
  return (
    <div className="space-y-2">
      {DAYS.map(({ key, label }) => {
        const day = hours[key] ?? { enabled: false, start: '09:00', end: '18:00' };
        return (
          <div key={key} className="flex items-center gap-3">
            <label className="flex items-center gap-2 w-24 cursor-pointer">
              <input type="checkbox" checked={day.enabled}
                onChange={e => onChange(key, 'enabled', e.target.checked)}
                className="accent-accent w-3 h-3" />
              <span className="f-mono text-xs text-ink-1 uppercase">{label}</span>
            </label>
            <input type="time" className={`${inp} w-28 text-xs`} value={day.start} disabled={!day.enabled}
              onChange={e => onChange(key, 'start', e.target.value)} />
            <span className="text-ink-3 text-xs">—</span>
            <input type="time" className={`${inp} w-28 text-xs`} value={day.end} disabled={!day.enabled}
              onChange={e => onChange(key, 'end', e.target.value)} />
          </div>
        );
      })}
    </div>
  );
}

// ─── Questions builder (shared) ────────────────────────────────

function QuestionsBuilder({ questions, onChange }: {
  questions: ClientQuestion[];
  onChange: (qs: ClientQuestion[]) => void;
}) {
  const add = () => onChange([...questions, { id: crypto.randomUUID(), question: '', required: false }]);
  const remove = (id: string) => onChange(questions.filter(q => q.id !== id));
  const update = (id: string, field: keyof ClientQuestion, val: string | boolean) =>
    onChange(questions.map(q => q.id === id ? { ...q, [field]: val } : q));

  return (
    <>
      <div className="space-y-2 mb-3">
        {questions.map((q, i) => (
          <div key={q.id} className="flex items-center gap-2">
            <span className="f-mono text-xs text-ink-3 w-5">{i + 1}.</span>
            <input className={`${inp} flex-1`} placeholder="Escriu la pregunta..."
              value={q.question} onChange={e => update(q.id, 'question', e.target.value)} />
            <label className="flex items-center gap-1 cursor-pointer">
              <input type="checkbox" checked={q.required}
                onChange={e => update(q.id, 'required', e.target.checked)}
                className="accent-accent w-3 h-3" />
              <span className="f-mono text-xs text-ink-2">Req.</span>
            </label>
            <button type="button" onClick={() => remove(q.id)}
              className="text-ink-3 hover:text-red-400 transition-colors">
              <I.X size={12} />
            </button>
          </div>
        ))}
      </div>
      <AMGButton type="button" variant="ghost" size="sm" onClick={add}>+ Afegir pregunta</AMGButton>
    </>
  );
}

// ─── AGENDA FORM ───────────────────────────────────────────────

const AGENDA_MODE_LABELS: Record<AgendaMode, { title: string; slotLabel: string; confirmLabel: string; zoneLabel?: string }> = {
  appointment: { title: 'Gestió de cites', slotLabel: 'Durada de la cita', confirmLabel: 'Missatge de confirmació de cita' },
  inspection:  { title: 'Visites d\'inspecció', slotLabel: 'Durada de la visita', confirmLabel: 'Missatge de confirmació de visita', zoneLabel: 'Zona de servei (municipis o km de radi)' },
  vehicle:     { title: 'Recepció de vehicles', slotLabel: 'Franja de deixada', confirmLabel: 'Missatge de confirmació de deixada' },
  meeting:     { title: 'Reunions amb clients', slotLabel: 'Durada de la reunió', confirmLabel: 'Missatge de confirmació de reunió' },
};

function AgendaForm({ tenantId, sector }: { tenantId: string; sector?: string | null }) {
  const qc = useQueryClient();
  const mode = getAgendaMode(sector);
  const labels = AGENDA_MODE_LABELS[mode];

  const { data: raw } = useQuery({
    queryKey: ['nexe-config', tenantId, 'AGENDA'],
    queryFn: () => getNexeConfig(tenantId, 'AGENDA'),
  });

  const sectorDefaults = getAgendaDefaults(sector);
  const [cfg, setCfg] = useState<AgendaConfig>(sectorDefaults);
  const [questions, setQuestions] = useState<ClientQuestion[]>(sectorDefaults.client_questions);

  useEffect(() => {
    const stored = parseConfig<AgendaConfig>(raw, sectorDefaults);
    setCfg(stored);
    setQuestions(stored.client_questions ?? []);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [raw]);

  const mut = useMutation({
    mutationFn: () => saveNexeConfig(tenantId, 'AGENDA', { ...cfg, client_questions: questions }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['nexe-config', tenantId, 'AGENDA'] }),
  });

  const setDay = (day: string, field: string, value: string | boolean) =>
    setCfg(c => ({ ...c, working_hours: { ...c.working_hours, [day]: { ...c.working_hours[day], [field]: value } } }));

  return (
    <form onSubmit={e => { e.preventDefault(); mut.mutate(); }}>
      {/* Calendar integration */}
      {(mode === 'appointment' || mode === 'meeting') && (
        <SectionCard title="Integració de calendari">
          <Field label="Sistema de gestió">
            <select className={sel} value={cfg.calendar_type}
              onChange={e => setCfg(c => ({ ...c, calendar_type: e.target.value as AgendaConfig['calendar_type'] }))}>
              <option value="manual">Manual (el bot gestiona les cites)</option>
              <option value="google">Google Calendar</option>
              <option value="calendly">Calendly</option>
            </select>
          </Field>
          {cfg.calendar_type === 'google' && (
            <>
              <Field label="Google Calendar ID">
                <input className={inp} placeholder="example@group.calendar.google.com"
                  value={cfg.google_calendar_id}
                  onChange={e => setCfg(c => ({ ...c, google_calendar_id: e.target.value }))} />
              </Field>
              <Field label="Google API Key">
                <input className={inp} type="password" placeholder="AIza..."
                  value={cfg.google_api_key}
                  onChange={e => setCfg(c => ({ ...c, google_api_key: e.target.value }))} />
              </Field>
            </>
          )}
        </SectionCard>
      )}

      {/* Working hours */}
      <SectionCard title="Horari d'atenció">
        <WorkingHoursGrid hours={cfg.working_hours} onChange={setDay} />
      </SectionCard>

      {/* Slot configuration */}
      <SectionCard title="Configuració de les visites">
        <div className={`grid gap-3 ${mode === 'vehicle' ? 'grid-cols-2' : 'grid-cols-2'}`}>
          <Field label={labels.slotLabel + ' (min)'}>
            <select className={sel} value={cfg.slot_duration_minutes}
              onChange={e => setCfg(c => ({ ...c, slot_duration_minutes: +e.target.value }))}>
              {[15, 30, 45, 60, 90, 120].map(m => <option key={m} value={m}>{m} min</option>)}
            </select>
          </Field>
          {(mode === 'appointment' || mode === 'meeting') && (
            <Field label="Marge entre cites (min)">
              <select className={sel} value={cfg.buffer_minutes}
                onChange={e => setCfg(c => ({ ...c, buffer_minutes: +e.target.value }))}>
                {[0, 5, 10, 15, 30].map(m => <option key={m} value={m}>{m} min</option>)}
              </select>
            </Field>
          )}
          {mode !== 'vehicle' && (
            <Field label="Màxim dies d'avança">
              <input className={inp} type="number" min={1} max={365} value={cfg.max_days_advance}
                onChange={e => setCfg(c => ({ ...c, max_days_advance: +e.target.value }))} />
            </Field>
          )}
          <Field label={mode === 'inspection' ? 'Avís mínim (hores)' : 'Avís mínim (hores)'}>
            <input className={inp} type="number" min={0} max={168} value={cfg.min_notice_hours}
              onChange={e => setCfg(c => ({ ...c, min_notice_hours: +e.target.value }))} />
          </Field>
          {mode === 'vehicle' && (
            <Field label="Dies estimats de recollida" hint="Informat als clients en la confirmació">
              <input className={inp} type="number" min={0} max={30} value={cfg.estimated_pickup_days ?? 2}
                onChange={e => setCfg(c => ({ ...c, estimated_pickup_days: +e.target.value }))} />
            </Field>
          )}
        </div>
        {mode === 'inspection' && labels.zoneLabel && (
          <Field label={labels.zoneLabel} hint="Ex: Palma, Calvià i Llucmajor · o bé: radi 30km de Palma">
            <input className={inp} placeholder="Municipis o radi de cobertura..."
              value={cfg.service_zone ?? ''}
              onChange={e => setCfg(c => ({ ...c, service_zone: e.target.value }))} />
          </Field>
        )}
      </SectionCard>

      {/* Client questions */}
      <SectionCard title={mode === 'vehicle' ? 'Informació del vehicle' : mode === 'inspection' ? 'Informació per a la visita' : 'Preguntes al client'}>
        <p className="f-mono text-xs text-ink-3 mb-3">
          {mode === 'vehicle'
            ? 'Dades que el bot recollirà quan el client vulgui deixar el vehicle.'
            : mode === 'inspection'
            ? 'Dades que el bot recollirà per preparar la visita d\'inspecció.'
            : "Variables disponibles: {{nom}}, {{telefon}}"}
        </p>
        <QuestionsBuilder questions={questions} onChange={setQuestions} />
      </SectionCard>

      {/* Confirmation template */}
      <SectionCard title={labels.confirmLabel}>
        <Field label="Plantilla del missatge" hint="Variables: {{nom}}, {{data}}, {{hora}}, {{servei}}">
          <textarea className={`${inp} h-24 resize-none`}
            value={cfg.confirmation_template}
            onChange={e => setCfg(c => ({ ...c, confirmation_template: e.target.value }))} />
        </Field>
        {mode === 'vehicle' && (
          <p className="f-mono text-xs text-ink-3">Variable addicional: {'{{dies_recollida}}'}</p>
        )}
      </SectionCard>

      <SaveRow isPending={mut.isPending} isSuccess={mut.isSuccess} isError={mut.isError} />
    </form>
  );
}

// ─── PRESSUPOSTOS / LLISTAT DE PREUS FORM ─────────────────────

function PressupostosForm({ tenantId, sector }: { tenantId: string; sector?: string | null }) {
  const qc = useQueryClient();
  const mode: QuoteMode = getQuoteMode(sector);

  const { data: raw } = useQuery({
    queryKey: ['nexe-config', tenantId, 'PRESSUPOSTOS'],
    queryFn: () => getNexeConfig(tenantId, 'PRESSUPOSTOS'),
  });

  const sectorDefaults = getPressupostosDefaults(sector);
  const [cfg, setCfg] = useState<PressupostosConfig>(sectorDefaults);

  useEffect(() => {
    setCfg(parseConfig<PressupostosConfig>(raw, sectorDefaults));
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [raw]);

  const mut = useMutation({
    mutationFn: () => saveNexeConfig(tenantId, 'PRESSUPOSTOS', cfg),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['nexe-config', tenantId, 'PRESSUPOSTOS'] }),
  });

  const addService = () => {
    setCfg(c => ({
      ...c,
      services_catalog: [...c.services_catalog, { id: crypto.randomUUID(), name: '', price: 0, unit: mode === 'pricelist' ? 'sessió' : 'unitat', duration: mode === 'pricelist' ? '' : undefined, description: '' }],
    }));
  };

  const removeService = (id: string) => setCfg(c => ({ ...c, services_catalog: c.services_catalog.filter(s => s.id !== id) }));

  const updateService = (id: string, field: keyof ServiceItem, value: string | number) => {
    setCfg(c => ({ ...c, services_catalog: c.services_catalog.map(s => s.id === id ? { ...s, [field]: value } : s) }));
  };

  return (
    <form onSubmit={e => { e.preventDefault(); mut.mutate(); }}>
      {mode === 'formal' && (
        <SectionCard title="Configuració del pressupost">
          <div className="grid grid-cols-2 gap-3">
            <Field label="Dies de validesa">
              <input className={inp} type="number" min={1} max={365} value={cfg.quote_validity_days}
                onChange={e => setCfg(c => ({ ...c, quote_validity_days: +e.target.value }))} />
            </Field>
          </div>
          <Field label="Capçalera del pressupost">
            <textarea className={`${inp} h-20 resize-none`} placeholder="Text introductori del pressupost..."
              value={cfg.quote_header}
              onChange={e => setCfg(c => ({ ...c, quote_header: e.target.value }))} />
          </Field>
          <Field label="Peu del pressupost">
            <textarea className={`${inp} h-20 resize-none`}
              value={cfg.quote_footer}
              onChange={e => setCfg(c => ({ ...c, quote_footer: e.target.value }))} />
          </Field>
        </SectionCard>
      )}

      <SectionCard title={mode === 'pricelist' ? 'Llistat de serveis i preus' : 'Catàleg de serveis'}>
        <p className="f-mono text-xs text-ink-3 mb-3">
          {mode === 'pricelist'
            ? 'El bot mostrarà aquest llistat quan el client pregunti pels preus o disponibilitat de serveis.'
            : 'El bot usarà aquest catàleg per generar pressupostos automàtics.'}
        </p>
        <div className="space-y-2 mb-3">
          {cfg.services_catalog.map((s, i) => (
            <div key={s.id} className="bg-surface-base border border-border-base rounded p-3">
              <div className="flex items-center justify-between mb-2">
                <span className="f-mono text-xs text-ink-3">{mode === 'pricelist' ? 'Tractament' : 'Servei'} {i + 1}</span>
                <button type="button" onClick={() => removeService(s.id)}
                  className="text-ink-3 hover:text-red-400 transition-colors">
                  <I.X size={12} />
                </button>
              </div>
              <div className={`grid gap-2 ${mode === 'pricelist' ? 'grid-cols-2' : 'grid-cols-2'}`}>
                <Field label="Nom">
                  <input className={inp} placeholder={mode === 'pricelist' ? 'Ex: Sessió de fisioteràpia' : 'Ex: Pintura interior per m²'}
                    value={s.name} onChange={e => updateService(s.id, 'name', e.target.value)} />
                </Field>
                <Field label="Preu (€)">
                  <input className={inp} type="number" min={0} step={0.01}
                    value={s.price} onChange={e => updateService(s.id, 'price', +e.target.value)} />
                </Field>
                {mode === 'pricelist' ? (
                  <Field label="Durada">
                    <input className={inp} placeholder="Ex: 1h, 45min, 2h 30min"
                      value={s.duration ?? ''} onChange={e => updateService(s.id, 'duration', e.target.value)} />
                  </Field>
                ) : (
                  <Field label="Unitat">
                    <input className={inp} placeholder="m², hora, unitat, servei..."
                      value={s.unit} onChange={e => updateService(s.id, 'unit', e.target.value)} />
                  </Field>
                )}
                <Field label="Descripció breu">
                  <input className={inp} placeholder="Informació addicional (opcional)"
                    value={s.description} onChange={e => updateService(s.id, 'description', e.target.value)} />
                </Field>
              </div>
            </div>
          ))}
        </div>
        <AMGButton type="button" variant="ghost" size="sm" onClick={addService}>
          {mode === 'pricelist' ? '+ Afegir tractament' : '+ Afegir servei'}
        </AMGButton>
      </SectionCard>

      <SaveRow isPending={mut.isPending} isSuccess={mut.isSuccess} isError={mut.isError} />
    </form>
  );
}

// ─── FIDELITZACIÓ FORM ─────────────────────────────────────────

function FidelitzacioForm({ tenantId }: { tenantId: string }) {
  const qc = useQueryClient();
  const { data: raw } = useQuery({
    queryKey: ['nexe-config', tenantId, 'FIDELITZACIO'],
    queryFn: () => getNexeConfig(tenantId, 'FIDELITZACIO'),
  });

  const [cfg, setCfg] = useState<FidelitzacioConfig>(DEFAULT_FIDELITZACIO);
  useEffect(() => { setCfg(parseConfig<FidelitzacioConfig>(raw, DEFAULT_FIDELITZACIO)); }, [raw]);

  const mut = useMutation({
    mutationFn: () => saveNexeConfig(tenantId, 'FIDELITZACIO', cfg),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['nexe-config', tenantId, 'FIDELITZACIO'] }),
  });

  return (
    <form onSubmit={e => { e.preventDefault(); mut.mutate(); }}>
      <SectionCard title="Google Reviews">
        <Field label="URL de Google Reviews"
          hint="Ves a Google Business → Obtenir ressenya de clients → Copiar enllaç">
          <input className={inp} placeholder="https://g.page/r/..."
            value={cfg.google_reviews_url}
            onChange={e => setCfg(c => ({ ...c, google_reviews_url: e.target.value }))} />
        </Field>
      </SectionCard>

      <SectionCard title="Seguiment post-servei">
        <Field label="Dies després del servei per enviar el missatge">
          <input className={inp} type="number" min={0} max={30} value={cfg.followup_days}
            onChange={e => setCfg(c => ({ ...c, followup_days: +e.target.value }))} />
        </Field>
        <Field label="Missatge de seguiment" hint="Variables: {{nom}}, {{url_ressenya}}, {{servei}}">
          <textarea className={`${inp} h-28 resize-none`}
            value={cfg.followup_template}
            onChange={e => setCfg(c => ({ ...c, followup_template: e.target.value }))} />
        </Field>
      </SectionCard>

      <SectionCard title="Reenganchament de clients inactius">
        <Field label="Mesos d'inactivitat per disparar la campanya">
          <input className={inp} type="number" min={1} max={24} value={cfg.reengagement_months}
            onChange={e => setCfg(c => ({ ...c, reengagement_months: +e.target.value }))} />
        </Field>
        <Field label="Missatge de reenganchament" hint="Variables: {{nom}}, {{ultima_visita}}, {{oferta}}">
          <textarea className={`${inp} h-28 resize-none`}
            value={cfg.reengagement_template}
            onChange={e => setCfg(c => ({ ...c, reengagement_template: e.target.value }))} />
        </Field>
      </SectionCard>

      <SaveRow isPending={mut.isPending} isSuccess={mut.isSuccess} isError={mut.isError} />
    </form>
  );
}

// ─── EQUIP FORM ────────────────────────────────────────────────

function Step({ n, children }: { n: number; children: React.ReactNode }) {
  return (
    <div className="flex gap-3">
      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-accent-subtle border border-[rgba(255,107,0,0.4)] text-accent-light f-mono text-[10px] font-bold flex items-center justify-center">{n}</span>
      <span className="f-mono text-xs text-ink-1 leading-relaxed">{children}</span>
    </div>
  );
}

function EquipForm({ tenantId }: { tenantId: string }) {
  const qc = useQueryClient();
  const { data: raw } = useQuery({
    queryKey: ['nexe-config', tenantId, 'EQUIP'],
    queryFn: () => getNexeConfig(tenantId, 'EQUIP'),
  });

  const [cfg, setCfg] = useState<EquipConfig>(DEFAULT_EQUIP);
  useEffect(() => { setCfg(parseConfig<EquipConfig>(raw, DEFAULT_EQUIP)); }, [raw]);

  const mut = useMutation({
    mutationFn: () => saveNexeConfig(tenantId, 'EQUIP', cfg),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['nexe-config', tenantId, 'EQUIP'] }),
  });

  const addMember = () => setCfg(c => ({ ...c, members: [...c.members, { id: crypto.randomUUID(), name: '', role: '', whatsapp: '' }] }));
  const removeMember = (id: string) => setCfg(c => ({ ...c, members: c.members.filter(m => m.id !== id) }));
  const updateMember = (id: string, field: keyof TeamMember, value: string) =>
    setCfg(c => ({ ...c, members: c.members.map(m => m.id === id ? { ...m, [field]: value } : m) }));

  const groupConfigured = cfg.telegram_group_id.trim().length > 0;

  return (
    <form onSubmit={e => { e.preventDefault(); mut.mutate(); }}>

      {/* Telegram group setup */}
      <SectionCard title="Grup de Telegram de l'equip">
        <div className="bg-[rgba(255,107,0,0.04)] border border-[rgba(255,107,0,0.2)] rounded p-4 mb-4 space-y-3">
          <p className="f-mono text-xs font-semibold text-accent-light uppercase tracking-wide mb-2">Com configurar el grup</p>
          <Step n={1}>Obre Telegram i crea un nou grup. Afegeix-hi tots els membres de l'equip.</Step>
          <Step n={2}>
            Afegeix el bot de l'empresa al grup (el mateix bot configurat a F1). Busca'l pel seu @username i afegeix-lo com a membre.
          </Step>
          <Step n={3}>
            Fes el bot <strong>administrador</strong> del grup perquè pugui enviar missatges (Informació del grup → Administradors → Afegir administrador).
          </Step>
          <Step n={4}>
            Afegeix <strong>@userinfobot</strong> al grup. Automàticament respondrà amb l'ID del grup (un número negatiu com <code className="bg-surface-base px-1 rounded text-[10px]">-1001234567890</code>). Copia aquest número.
          </Step>
          <Step n={5}>Enganxa l'ID del grup al camp de baix i desa. Ja pots eliminar @userinfobot del grup si vols.</Step>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <Field label="ID del grup de Telegram" hint="Número negatiu obtingut amb @userinfobot">
            <input className={inp} placeholder="-1001234567890"
              value={cfg.telegram_group_id}
              onChange={e => setCfg(c => ({ ...c, telegram_group_id: e.target.value }))} />
          </Field>
          <Field label="Nom del grup (referència)">
            <input className={inp} placeholder="Ex: Equip Clínica Martí"
              value={cfg.telegram_group_name}
              onChange={e => setCfg(c => ({ ...c, telegram_group_name: e.target.value }))} />
          </Field>
        </div>

        {groupConfigured && (
          <div className="flex items-center gap-2 mt-2">
            <span className="w-2 h-2 rounded-full bg-green-400 flex-shrink-0" />
            <span className="f-mono text-xs text-green-400">Grup configurat: {cfg.telegram_group_name || cfg.telegram_group_id}</span>
          </div>
        )}
        {!groupConfigured && (
          <div className="flex items-center gap-2 mt-2">
            <span className="w-2 h-2 rounded-full bg-yellow-400 flex-shrink-0" />
            <span className="f-mono text-xs text-yellow-400">Pendent — sense grup configurat el bot no pot enviar informes a l'equip</span>
          </div>
        )}
      </SectionCard>

      {/* Team members */}
      <SectionCard title="Membres de l'equip">
        <p className="f-mono text-xs text-ink-3 mb-3">
          Informació de contacte de cada membre (per referència interna i notificacions directes per WhatsApp si cal).
        </p>
        <div className="space-y-3 mb-3">
          {cfg.members.map((m, i) => (
            <div key={m.id} className="bg-surface-base border border-border-base rounded p-3">
              <div className="flex items-center justify-between mb-2">
                <span className="f-mono text-xs text-ink-3">Membre {i + 1}</span>
                <button type="button" onClick={() => removeMember(m.id)}
                  className="text-ink-3 hover:text-red-400 transition-colors">
                  <I.X size={12} />
                </button>
              </div>
              <div className="grid grid-cols-2 gap-2">
                <Field label="Nom">
                  <input className={inp} placeholder="Nom complet"
                    value={m.name} onChange={e => updateMember(m.id, 'name', e.target.value)} />
                </Field>
                <Field label="Rol">
                  <input className={inp} placeholder="Tècnic, Recepció, Terapeuta..."
                    value={m.role} onChange={e => updateMember(m.id, 'role', e.target.value)} />
                </Field>
                <Field label="WhatsApp (+34...)">
                  <input className={inp} placeholder="+34 6XX XXX XXX"
                    value={m.whatsapp} onChange={e => updateMember(m.id, 'whatsapp', e.target.value)} />
                </Field>
              </div>
            </div>
          ))}
        </div>
        <AMGButton type="button" variant="ghost" size="sm" onClick={addMember}>+ Afegir membre</AMGButton>
      </SectionCard>

      <SectionCard title="Informe diari">
        <div className="flex items-center gap-3 mb-3">
          <label className="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" checked={cfg.daily_report_enabled}
              onChange={e => setCfg(c => ({ ...c, daily_report_enabled: e.target.checked }))}
              className="accent-accent w-4 h-4" />
            <span className="f-mono text-xs text-ink-1">Activar informe diari</span>
          </label>
        </div>
        {cfg.daily_report_enabled && !groupConfigured && (
          <div className="flex items-center gap-2 mb-3 p-2 bg-yellow-500/10 border border-yellow-500/30 rounded">
            <span className="w-2 h-2 rounded-full bg-yellow-400 flex-shrink-0" />
            <span className="f-mono text-xs text-yellow-400">Cal configurar el grup de Telegram abans d'activar l'informe diari</span>
          </div>
        )}
        {cfg.daily_report_enabled && (
          <>
            <Field label="Hora d'enviament" hint={groupConfigured ? `S'enviarà al grup "${cfg.telegram_group_name || cfg.telegram_group_id}"` : undefined}>
              <input type="time" className={`${inp} w-32`}
                value={cfg.daily_report_time}
                onChange={e => setCfg(c => ({ ...c, daily_report_time: e.target.value }))} />
            </Field>
            <Field label="Plantilla de l'informe" hint="Variables: {{data}}, {{num_cites}}, {{nous_contactes}}">
              <textarea className={`${inp} h-28 resize-none`}
                value={cfg.report_template}
                onChange={e => setCfg(c => ({ ...c, report_template: e.target.value }))} />
            </Field>
          </>
        )}
      </SectionCard>

      <SaveRow isPending={mut.isPending} isSuccess={mut.isSuccess} isError={mut.isError} />
    </form>
  );
}

// ─── Service meta per sector ───────────────────────────────────

function getServiceMeta(service: string, sector?: string | null) {
  switch (service) {
    case 'agenda': {
      const mode = getAgendaMode(sector);
      const titles: Record<AgendaMode, string> = {
        appointment: 'Gestió de Cites',
        inspection: "Visites d'Inspecció",
        vehicle: 'Recepció de Vehicles',
        meeting: 'Reunions amb Clients',
      };
      return { title: titles[mode], phase: 'F2', description: 'Configura l\'horari, les preguntes al client i el missatge de confirmació.' };
    }
    case 'pressupostos': {
      const mode = getQuoteMode(sector);
      return {
        title: mode === 'pricelist' ? 'Llistat de Preus' : 'Pressupostos',
        phase: 'F3',
        description: mode === 'pricelist'
          ? 'Defineix els serveis i preus que el bot mostrarà als clients.'
          : 'Configura el catàleg de serveis, la plantilla i la validesa dels pressupostos.',
      };
    }
    case 'fidelitzacio':
      return { title: 'Fidelització', phase: 'F4', description: 'Seguiment post-servei, sol·licitud de ressenyes i reenganchament.' };
    case 'equip':
      return { title: "Gestió d'Equip", phase: 'F5', description: "Membres, rols, canals de contacte i informe diari." };
    default:
      return { title: 'Configuració', phase: '', description: '' };
  }
}

// ─── PAGE ──────────────────────────────────────────────────────

export default function NexeServiceConfigPage() {
  const rawParams = useParams<{ id: string; service: string; locale: string }>();
  const tenantId = rawParams.id;
  const service = rawParams.service;
  const locale = rawParams.locale as string;
  const router = useRouter();

  const { data: tenant } = useQuery({
    queryKey: ['tenant', tenantId],
    queryFn: () => getTenant(tenantId),
  });

  const sector = tenant?.sector ?? null;
  const meta = getServiceMeta(service, sector);

  const renderForm = () => {
    switch (service) {
      case 'agenda':       return <AgendaForm tenantId={tenantId} sector={sector} />;
      case 'pressupostos': return <PressupostosForm tenantId={tenantId} sector={sector} />;
      case 'fidelitzacio': return <FidelitzacioForm tenantId={tenantId} />;
      case 'equip':        return <EquipForm tenantId={tenantId} />;
      default:
        return <p className="f-mono text-xs text-ink-3 text-center py-12">Servei desconegut: {service}</p>;
    }
  };

  return (
    <PortalShell breadcrumb={`admin · tenants · ${tenant?.name ?? '...'} · nexe · ${meta.title.toLowerCase()}`} backHref={`/${locale}/portal/admin/tenants/${tenantId}`}>
      <div className="p-4 sm:p-8">
        <div className="max-w-2xl mx-auto">
          <div className="flex items-start justify-between mb-6">
            <div>
              <div className="flex items-center gap-2 mb-1">
                {meta.phase && (
                  <span className="f-mono text-xs font-bold text-accent-light bg-accent-subtle border border-[rgba(255,107,0,0.3)] rounded px-2 py-0.5">
                    {meta.phase}
                  </span>
                )}
                <h1 className="f-display text-xl font-bold text-ink-0">{meta.title}</h1>
              </div>
              <p className="f-mono text-xs text-ink-2 mt-1">{meta.description}</p>
              <div className="flex items-center gap-2 mt-1">
                {tenant?.name && <span className="f-mono text-xs text-ink-3">{tenant.name}</span>}
                {sector && (
                  <span className="f-mono text-[10px] text-ink-3 border border-border-base rounded px-1.5 py-0.5 uppercase">
                    {sector.replace(/_/g, ' ')}
                  </span>
                )}
              </div>
            </div>
          </div>

          {renderForm()}
        </div>
      </div>
    </PortalShell>
  );
}
