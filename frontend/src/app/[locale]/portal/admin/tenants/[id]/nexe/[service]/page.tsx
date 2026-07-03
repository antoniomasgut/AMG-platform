'use client';
import { Link } from '@/i18n/navigation';

import { useParams, useRouter } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import React, { useState, useEffect } from 'react';
import { getTenant } from '@/services/admin';
import { useToast } from '@/lib/toast-context';
import {
  getNexeConfig, saveNexeConfig,
  provisionCalendar, getCalendarOAuthUrl, getCalendarSaEmail, validateCalendarId,
  AgendaConfig, AgendaMode, getAgendaMode, getAgendaDefaults,
  PressupostosConfig, QuoteMode, getQuoteMode, getPressupostosDefaults, ServiceItem,
  FidelitzacioConfig, DEFAULT_FIDELITZACIO, type SeasonalCampaign,
  EquipConfig, DEFAULT_EQUIP, TeamMember,
  ClientQuestion,
} from '@/services/nexe-configs';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { IconSet } from '@/components/ui/icons';

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
              <IconSet.X size={12} />
            </button>
          </div>
        ))}
      </div>
      <AMGButton type="button" variant="ghost" size="sm" onClick={add}>+ Afegir pregunta</AMGButton>
    </>
  );
}

// ─── Holidays manager ─────────────────────────────────────────

const MALLORCA_HOLIDAYS_2026 = [
  '2026-01-01', '2026-01-06', '2026-04-02', '2026-04-03',
  '2026-05-01', '2026-07-28', '2026-08-15', '2026-10-12',
  '2026-11-01', '2026-12-06', '2026-12-08', '2026-12-25',
];

function HolidaysManager({ holidays, onChange }: {
  holidays: string[];
  onChange: (h: string[]) => void;
}) {
  const [dateInput, setDateInput] = React.useState('');

  const add = () => {
    const d = dateInput.trim();
    if (!d || holidays.includes(d)) return;
    onChange([...holidays].concat(d).sort());
    setDateInput('');
  };

  const remove = (d: string) => onChange(holidays.filter(h => h !== d));

  const preload = () => {
    const merged = Array.from(new Set([...holidays, ...MALLORCA_HOLIDAYS_2026])).sort();
    onChange(merged);
  };

  const formatDate = (d: string) => {
    try {
      return new Date(d + 'T00:00:00').toLocaleDateString('ca-ES', { day: 'numeric', month: 'long', year: 'numeric' });
    } catch { return d; }
  };

  return (
    <div>
      <div className="flex gap-2 mb-3">
        <input
          type="date"
          className={`${inp} flex-1`}
          value={dateInput}
          onChange={e => setDateInput(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && (e.preventDefault(), add())}
        />
        <AMGButton type="button" variant="secondary" size="sm" onClick={add}>Afegir</AMGButton>
      </div>
      {holidays.length > 0 ? (
        <div className="space-y-1 mb-3 max-h-48 overflow-y-auto">
          {holidays.map(d => (
            <div key={d} className="flex items-center justify-between bg-surface-base rounded px-3 py-1.5">
              <span className="f-mono text-xs text-ink-1">{formatDate(d)}</span>
              <button type="button" onClick={() => remove(d)} className="text-ink-3 hover:text-red-400 transition-colors ml-2">
                <IconSet.X size={12} />
              </button>
            </div>
          ))}
        </div>
      ) : (
        <p className="f-mono text-xs text-ink-3 mb-3">Cap dia marcat com a festiu.</p>
      )}
      <AMGButton type="button" variant="ghost" size="sm" onClick={preload}>
        + Pre-carregar festius Mallorca 2026
      </AMGButton>
    </div>
  );
}

// ─── AGENDA FORM ───────────────────────────────────────────────

const AGENDA_MODE_LABELS: Record<AgendaMode, { title: string; slotLabel: string; confirmLabel: string; zoneLabel?: string }> = {
  appointment: { title: 'Cites / Visites', slotLabel: 'Durada de la cita', confirmLabel: 'Missatge de confirmació de cita' },
  inspection:  { title: 'Visites d\'inspecció', slotLabel: 'Durada de la visita', confirmLabel: 'Missatge de confirmació de visita', zoneLabel: 'Zona de servei (municipis o km de radi)' },
  vehicle:     { title: 'Recepció de vehicles', slotLabel: 'Franja de deixada', confirmLabel: 'Missatge de confirmació de deixada' },
  meeting:     { title: 'Reunions amb clients', slotLabel: 'Durada de la reunió', confirmLabel: 'Missatge de confirmació de reunió' },
};

function AgendaForm({ tenantId, sector }: { tenantId: string; sector?: string | null }) {
  const qc = useQueryClient();
  const { toast } = useToast();
  const mode = getAgendaMode(sector);
  const labels = AGENDA_MODE_LABELS[mode];
  const [provisioning, setProvisioning] = React.useState(false);
  const [oauthLoading, setOauthLoading] = React.useState(false);
  const [validating, setValidating] = React.useState(false);
  const [validateResult, setValidateResult] = React.useState<{ valid: boolean; message?: string; error?: string } | null>(null);

  const { data: raw } = useQuery({
    queryKey: ['nexe-config', tenantId, 'AGENDA'],
    queryFn: () => getNexeConfig(tenantId, 'AGENDA'),
  });

  const sectorDefaults = getAgendaDefaults(sector);
  const [cfg, setCfg] = useState<AgendaConfig>(sectorDefaults);

  const { data: saInfo } = useQuery({
    queryKey: ['calendar-sa-email', tenantId],
    queryFn: () => getCalendarSaEmail(tenantId),
    enabled: cfg.calendar_type === 'google',
  });
  const [questions, setQuestions] = useState<ClientQuestion[]>(sectorDefaults.client_questions);
  const [holidays, setHolidays] = useState<string[]>([]);

  useEffect(() => {
    const stored = parseConfig<AgendaConfig>(raw, sectorDefaults);
    setCfg(stored);
    setQuestions(stored.client_questions ?? []);
    setHolidays(stored.holidays ?? []);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [raw]);

  const mut = useMutation({
    mutationFn: () => saveNexeConfig(tenantId, 'AGENDA', { ...cfg, client_questions: questions, holidays }),
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
              <option value="manual">Manual (el bot gestiona les cites sense calendari extern)</option>
              <option value="google">Google Calendar — AMG crea el calendari (recomanat)</option>
              <option value="google_oauth">Google Calendar — Client connecta el seu compte Google</option>
              <option value="calendly">Calendly</option>
            </select>
          </Field>

          {cfg.calendar_type === 'google' && (
            <div className="space-y-4">
              {cfg.google_calendar_id ? (
                <div className="flex items-center gap-3 p-3 bg-green-500/10 border border-green-500/30 rounded">
                  <span className="w-2 h-2 rounded-full bg-green-400 flex-shrink-0" />
                  <div className="flex-1 min-w-0">
                    <p className="f-mono text-xs text-green-400">Calendari configurat</p>
                    <p className="f-mono text-[10px] text-ink-3 truncate">{cfg.google_calendar_id}</p>
                  </div>
                  <AMGButton type="button" variant="ghost" size="sm"
                    onClick={() => setCfg(c => ({ ...c, google_calendar_id: '' }))}>
                    Canviar
                  </AMGButton>
                </div>
              ) : (
                <div className="space-y-3">
                  <div className="bg-[rgba(255,107,0,0.04)] border border-[rgba(255,107,0,0.2)] rounded p-4 space-y-2">
                    <p className="f-mono text-xs font-semibold text-accent-light uppercase tracking-wide">
                      Opció A — AMG crea el calendari automàticament
                    </p>
                    <p className="f-mono text-xs text-ink-2">
                      Crea un Google Calendar "Cites — {'{nom del tenant}'}" i el comparteix amb l'email del tenant.
                      Requereix que <code className="bg-surface-base px-1 rounded text-[10px]">GOOGLE_CALENDAR_SA_JSON</code> estigui configurat a{' '}
                      <Link href="/portal/admin/config" className="text-accent-light underline">Configuració del sistema</Link>.
                    </p>
                    <AMGButton type="button" variant="secondary" size="sm"
                      loading={provisioning}
                      onClick={async () => {
                        setProvisioning(true);
                        try {
                          const r = await provisionCalendar(tenantId);
                          setCfg(c => ({ ...c, google_calendar_id: r.calendarId, calendar_type: 'google' }));
                          toast('success', r.message);
                          qc.invalidateQueries({ queryKey: ['nexe-config', tenantId, 'AGENDA'] });
                        } catch (e: unknown) {
                          toast('error', e instanceof Error ? e.message : 'Error desconegut');
                        } finally {
                          setProvisioning(false);
                        }
                      }}>
                      Crear calendari AMG
                    </AMGButton>
                  </div>
                  <div className="text-center f-mono text-xs text-ink-3">— o bé —</div>
                  <div className="bg-[rgba(255,107,0,0.04)] border border-[rgba(255,107,0,0.2)] rounded p-4 space-y-3">
                    <p className="f-mono text-xs font-semibold text-accent-light uppercase tracking-wide">
                      Opció B — El tenant comparteix el seu calendari amb AMG
                    </p>
                    {saInfo?.configured === 'true' && saInfo.email ? (
                      <div className="space-y-2">
                        <p className="f-mono text-xs text-ink-2">
                          El tenant ha de compartir el seu calendari amb el Service Account d'AMG. Pasos:
                        </p>
                        <ol className="space-y-1.5 list-none">
                          {[
                            <>Obre <strong>Google Calendar</strong> → selecciona el calendari → <strong>Configuració i compartir</strong></>,
                            <><strong>Compartir amb persones específiques</strong> → afegeix aquest email:</>,
                            <><strong>Permís: Fer canvis als events</strong> → <strong>Envia</strong></>,
                            <>Copia l'<strong>ID del calendari</strong> (apareix a "Integra el calendari") i enganxa'l aquí sota</>,
                          ].map((step, i) => (
                            <li key={i} className="flex gap-2">
                              <span className="flex-shrink-0 w-4 h-4 rounded-full bg-accent-subtle border border-[rgba(255,107,0,0.4)] text-accent-light f-mono text-[9px] font-bold flex items-center justify-center">{i + 1}</span>
                              <span className="f-mono text-xs text-ink-1">{step}</span>
                            </li>
                          ))}
                        </ol>
                        <div className="flex items-center gap-2 p-2 bg-surface-base border border-border-base rounded">
                          <code className="f-mono text-xs text-accent-light flex-1 break-all">{saInfo.email}</code>
                          <button type="button"
                            className="f-mono text-[10px] text-ink-3 hover:text-ink-1 transition-colors flex-shrink-0"
                            onClick={() => navigator.clipboard.writeText(saInfo.email)}>
                            Copiar
                          </button>
                        </div>
                      </div>
                    ) : (
                      <p className="f-mono text-xs text-yellow-400">
                        Service Account no configurat — afegeix <code className="bg-surface-base px-1 rounded text-[10px]">GOOGLE_CALENDAR_SA_JSON</code> a la configuració del sistema.
                      </p>
                    )}
                    <Field label="ID del calendari del tenant">
                      <div className="flex gap-2">
                        <input className={`${inp} flex-1`} placeholder="exemple@group.calendar.google.com"
                          value={cfg.google_calendar_id}
                          onChange={e => { setCfg(c => ({ ...c, google_calendar_id: e.target.value })); setValidateResult(null); }} />
                        <AMGButton type="button" variant="secondary" size="sm"
                          loading={validating}
                          disabled={!cfg.google_calendar_id.trim()}
                          onClick={async () => {
                            setValidating(true);
                            setValidateResult(null);
                            try {
                              const r = await validateCalendarId(tenantId, cfg.google_calendar_id);
                              setValidateResult(r);
                            } catch (e: unknown) {
                              setValidateResult({ valid: false, error: e instanceof Error ? e.message : 'Error' });
                            } finally {
                              setValidating(false);
                            }
                          }}>
                          Verificar
                        </AMGButton>
                      </div>
                      {validateResult && (
                        <div className={`flex items-center gap-2 mt-2 p-2 rounded border ${validateResult.valid ? 'bg-green-500/10 border-green-500/30' : 'bg-red-500/10 border-red-500/30'}`}>
                          <span className={`w-2 h-2 rounded-full flex-shrink-0 ${validateResult.valid ? 'bg-green-400' : 'bg-red-400'}`} />
                          <span className={`f-mono text-xs ${validateResult.valid ? 'text-green-400' : 'text-red-400'}`}>
                            {validateResult.valid ? validateResult.message : (validateResult.error ?? 'No accessible')}
                          </span>
                        </div>
                      )}
                    </Field>
                  </div>
                </div>
              )}
            </div>
          )}

          {cfg.calendar_type === 'google_oauth' && (
            <div className="space-y-3">
              {cfg.google_refresh_token ? (
                <div className="space-y-3">
                  <div className="flex items-center gap-3 p-3 bg-green-500/10 border border-green-500/30 rounded">
                    <span className="w-2 h-2 rounded-full bg-green-400 flex-shrink-0" />
                    <div className="flex-1">
                      <p className="f-mono text-xs text-green-400">Google Calendar connectat (OAuth)</p>
                      <p className="f-mono text-[10px] text-ink-3">
                        Calendari: {cfg.google_calendar_id || 'primary'}
                      </p>
                    </div>
                    <AMGButton type="button" variant="ghost" size="sm"
                      onClick={() => setCfg(c => ({ ...c, google_refresh_token: undefined, google_calendar_id: '' }))}>
                      Desconnectar
                    </AMGButton>
                  </div>
                  <Field label="ID del calendari (opcional)" hint='Deixa "primary" per usar el calendari principal del compte'>
                    <input className={inp} placeholder="primary"
                      value={cfg.google_calendar_id || 'primary'}
                      onChange={e => setCfg(c => ({ ...c, google_calendar_id: e.target.value }))} />
                  </Field>
                </div>
              ) : (
                <div className="bg-[rgba(255,107,0,0.04)] border border-[rgba(255,107,0,0.2)] rounded p-4 space-y-3">
                  <p className="f-mono text-xs font-semibold text-accent-light uppercase tracking-wide">
                    Connecta el compte Google del client
                  </p>
                  <p className="f-mono text-xs text-ink-2">
                    El client authoritza AMG a crear events al seu propi Google Calendar.
                    Requereix <code className="bg-surface-base px-1 rounded text-[10px]">GOOGLE_OAUTH_CLIENT_ID</code> i <code className="bg-surface-base px-1 rounded text-[10px]">GOOGLE_OAUTH_CLIENT_SECRET</code> configurats.
                  </p>
                  <AMGButton type="button" variant="secondary" size="sm"
                    loading={oauthLoading}
                    onClick={async () => {
                      setOauthLoading(true);
                      try {
                        const { url } = await getCalendarOAuthUrl(tenantId);
                        window.open(url, '_blank', 'width=600,height=700');
                      } catch (e: unknown) {
                        toast('error', e instanceof Error ? e.message : 'Error obtenint URL OAuth');
                      } finally {
                        setOauthLoading(false);
                      }
                    }}>
                    Connecta Google Calendar →
                  </AMGButton>
                </div>
              )}
            </div>
          )}
        </SectionCard>
      )}

      {/* Working hours */}
      <SectionCard title="Horari d'atenció">
        <WorkingHoursGrid hours={cfg.working_hours} onChange={setDay} />
      </SectionCard>

      {/* Holidays */}
      <SectionCard title="Festius i dies tancats">
        <p className="f-mono text-xs text-ink-3 mb-3">
          Dies en què el negoci no obre. El bot no oferirà cites en aquestes dates.
          Pots afegir-les manualment o via <code className="bg-surface-base px-1 rounded text-[10px]">/festiu YYYY-MM-DD</code> al grup de Telegram.
        </p>
        <HolidaysManager holidays={holidays} onChange={setHolidays} />
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
                  <IconSet.X size={12} />
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

      <SectionCard title="Cobrament online en acceptar">
        <p className="f-mono text-xs text-ink-3 mb-3">
          Quan el client accepta el pressupost, se'l pot redirigir a pagar amb targeta
          (Stripe del client — els diners van directes al seu compte). Requereix tenir
          l&apos;Stripe del negoci configurat a la secció de pagaments.
        </p>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Mode de cobrament">
            <select className={inp} value={cfg.online_payment_mode ?? 'OFF'}
              onChange={e => setCfg(c => ({ ...c, online_payment_mode: e.target.value as 'OFF' | 'FULL' | 'DEPOSIT' }))}>
              <option value="OFF">Sense cobrament (només acceptació)</option>
              <option value="FULL">Cobrar el total en acceptar</option>
              <option value="DEPOSIT">Paga i senyal (percentatge)</option>
            </select>
          </Field>
          {(cfg.online_payment_mode ?? 'OFF') === 'DEPOSIT' && (
            <Field label="Percentatge de paga i senyal (%)">
              <input className={inp} type="number" min={5} max={90}
                value={cfg.deposit_percent ?? 30}
                onChange={e => setCfg(c => ({ ...c, deposit_percent: +e.target.value }))} />
            </Field>
          )}
        </div>
      </SectionCard>

      <SectionCard title="Seguiment de pressupostos no contestats">
        <Field label="">
          <label className="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" checked={cfg.quote_followup_enabled}
              onChange={e => setCfg(c => ({ ...c, quote_followup_enabled: e.target.checked }))} />
            <span className="text-sm text-ink-1">Activar seguiment automàtic de pressupostos</span>
          </label>
        </Field>
        {cfg.quote_followup_enabled && (<>
          <Field label="Dies sense resposta per enviar el recordatori">
            <input className={inp} type="number" min={1} max={14} value={cfg.quote_followup_days}
              onChange={e => setCfg(c => ({ ...c, quote_followup_days: +e.target.value }))} />
          </Field>
          <Field label="Missatge de seguiment" hint="Variable: {{nom}}">
            <textarea className={`${inp} h-24 resize-none`}
              value={cfg.quote_followup_message}
              onChange={e => setCfg(c => ({ ...c, quote_followup_message: e.target.value }))} />
          </Field>
        </>)}
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

      <SectionCard title="Campanyes estacionals">
        <p className="f-mono text-xs text-ink-3 mb-3">
          Missatges puntuals (Nadal, estiu, rebaixes...) que s&apos;envien el dia indicat
          als clients amb activitat durant l&apos;últim any. Variables: {'{{nom}}'}
        </p>
        <div className="space-y-2 mb-3">
          {(cfg.seasonal_campaigns ?? []).map((camp, i) => (
            <div key={camp.id} className="bg-surface-base border border-border-base rounded p-3">
              <div className="flex items-center justify-between mb-2">
                <span className="f-mono text-xs text-ink-3">Campanya {i + 1}</span>
                <button type="button"
                  onClick={() => setCfg(c => ({ ...c, seasonal_campaigns: (c.seasonal_campaigns ?? []).filter(x => x.id !== camp.id) }))}
                  className="text-ink-3 hover:text-red-400 transition-colors">
                  <IconSet.X size={12} />
                </button>
              </div>
              <div className="grid grid-cols-2 gap-2">
                <Field label="Nom">
                  <input className={inp} placeholder="Ex: Promoció de Nadal"
                    value={camp.name}
                    onChange={e => setCfg(c => ({ ...c, seasonal_campaigns: (c.seasonal_campaigns ?? []).map(x => x.id === camp.id ? { ...x, name: e.target.value } : x) }))} />
                </Field>
                <Field label="Data d'enviament">
                  <input className={inp} type="date"
                    value={camp.send_date}
                    onChange={e => setCfg(c => ({ ...c, seasonal_campaigns: (c.seasonal_campaigns ?? []).map(x => x.id === camp.id ? { ...x, send_date: e.target.value } : x) }))} />
                </Field>
              </div>
              <Field label="Missatge">
                <textarea className={`${inp} h-20 resize-none`}
                  placeholder="Hola {{nom}}! Aquest desembre..."
                  value={camp.message}
                  onChange={e => setCfg(c => ({ ...c, seasonal_campaigns: (c.seasonal_campaigns ?? []).map(x => x.id === camp.id ? { ...x, message: e.target.value } : x) }))} />
              </Field>
            </div>
          ))}
        </div>
        <AMGButton type="button" variant="ghost" size="sm"
          onClick={() => setCfg(c => ({ ...c, seasonal_campaigns: [...(c.seasonal_campaigns ?? []), { id: crypto.randomUUID(), name: '', send_date: '', message: '' }] }))}>
          + Afegir campanya
        </AMGButton>
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
                  <IconSet.X size={12} />
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

      <SectionCard title="Alertes de leads sense resposta">
        <Field label="">
          <label className="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" checked={cfg.unresponded_alert_enabled}
              onChange={e => setCfg(c => ({ ...c, unresponded_alert_enabled: e.target.checked }))} />
            <span className="text-sm text-ink-1">Avisar al grup Telegram quan un lead no rep resposta</span>
          </label>
        </Field>
        {cfg.unresponded_alert_enabled && (
          <Field label="Hores sense resposta per activar l'alerta">
            <input className={inp} type="number" min={1} max={24} value={cfg.unresponded_hours_threshold}
              onChange={e => setCfg(c => ({ ...c, unresponded_hours_threshold: +e.target.value }))} />
          </Field>
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
      return { title: 'Seguiment', phase: 'F4', description: 'Seguiment post-servei, sol·licitud de ressenyes i reactivació de clients.' };
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
