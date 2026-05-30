'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useQuery, useMutation } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { getKnowledge, updateEntries, testKnowledgeResponse, addDocument, deleteDocument } from '@/services/knowledge';
import { PortalShell } from '@/components/portal/PortalShell';

// ─── Types ───────────────────────────────────────────────────────────────────

type Step = 'personalitat' | 'negoci' | 'serveis' | 'restriccions' | 'test';

interface ServiceItem { name: string; description: string; price: string; duration: string; bookingRequired: string }
interface FaqItem { question: string; answer: string }

interface WizardState {
  // Step 1 — Personalitat
  botName: string;
  tone: string;
  language: string;
  emojiUse: string;
  personalityNotes: string;
  // Step 2 — Negoci
  businessName: string;
  businessType: string;
  address: string;
  phone: string;
  email: string;
  website: string;
  locationHints: string;
  schedule: string;
  scheduleWeekend: string;
  holidays: string;
  bookingMethod: string;
  cancellationPolicy: string;
  // Step 3 — Serveis + FAQ
  services: ServiceItem[];
  faq: FaqItem[];
  // Step 4 — Restriccions
  topicsOffLimits: string;
  escalationTriggers: string;
  escalationContact: string;
  disclaimer: string;
  extra: string;
}

const BLANK_SERVICE: ServiceItem = { name: '', description: '', price: '', duration: '', bookingRequired: 'Sí' };
const BLANK_FAQ: FaqItem = { question: '', answer: '' };

const STEPS: { key: Step; label: string; icon: string }[] = [
  { key: 'personalitat', label: 'Personalitat', icon: '🤖' },
  { key: 'negoci', label: 'Negoci', icon: '🏪' },
  { key: 'serveis', label: 'Serveis i FAQ', icon: '📋' },
  { key: 'restriccions', label: 'Restriccions', icon: '🛡' },
  { key: 'test', label: 'Test', icon: '✅' },
];

// ─── Serializers ─────────────────────────────────────────────────────────────

const LANGUAGE_INSTRUCTIONS: Record<string, string> = {
  auto:             'Detecta l\'idioma del client i respon sempre en el mateix idioma',
  bilingual_adapt:  'Adapta l\'idioma al client: si escriu en català respon en català, si escriu en castellà respon en castellà',
  catala:           'Respon sempre en català, independentment de l\'idioma del client',
  castella:         'Respon sempre en castellà, independentment de l\'idioma del client',
  english:          'Always respond in English, regardless of the language the client uses',
  german:           'Antworte immer auf Deutsch, unabhängig von der Sprache des Kunden',
};

function serializeBehavior(s: WizardState): string {
  const langInstruction = LANGUAGE_INSTRUCTIONS[s.language] ?? s.language;
  const lines = [
    `Nom del bot: ${s.botName || 'Assistent Virtual'}`,
    `To: ${s.tone}`,
    `Idioma: ${s.language}`,
    `Instrucció d'idioma: ${langInstruction}`,
    `Ús d'emojis: ${s.emojiUse}`,
  ];
  if (s.personalityNotes) lines.push(`Notes: ${s.personalityNotes}`);
  return lines.join('\n');
}

function serializeBusinessInfo(s: WizardState): string {
  const lines = [];
  if (s.businessName) lines.push(`Nom: ${s.businessName}`);
  if (s.businessType) lines.push(`Tipus: ${s.businessType}`);
  if (s.address) lines.push(`Adreça: ${s.address}`);
  if (s.phone) lines.push(`Telèfon: ${s.phone}`);
  if (s.email) lines.push(`Email: ${s.email}`);
  if (s.website) lines.push(`Web: ${s.website}`);
  if (s.locationHints) lines.push(`Com arribar: ${s.locationHints}`);
  if (s.bookingMethod) lines.push(`Com demanar cita: ${s.bookingMethod}`);
  if (s.cancellationPolicy) lines.push(`Cancel·lació: ${s.cancellationPolicy}`);
  return lines.join('\n');
}

function serializeSchedule(s: WizardState): string {
  const lines = [];
  if (s.schedule) lines.push(`Horari: ${s.schedule}`);
  if (s.scheduleWeekend) lines.push(`Cap de setmana: ${s.scheduleWeekend}`);
  if (s.holidays) lines.push(`Festius: ${s.holidays}`);
  return lines.join('\n');
}

function serializeServices(services: ServiceItem[]): string {
  return services.filter(s => s.name).map(s => {
    const parts = [`${s.name}`];
    if (s.description) parts.push(s.description);
    if (s.price) parts.push(`Preu: ${s.price}`);
    if (s.duration) parts.push(`Durada: ${s.duration}`);
    if (s.bookingRequired) parts.push(`Cita prèvia: ${s.bookingRequired}`);
    return parts.join(' — ');
  }).join('\n');
}

function serializeFaq(faq: FaqItem[]): string {
  return faq.filter(f => f.question).map(f => `P: ${f.question}\nR: ${f.answer}`).join('\n\n');
}

function serializeRestrictions(s: WizardState): string {
  const lines = [];
  if (s.topicsOffLimits) lines.push(`Temes prohibits: ${s.topicsOffLimits}`);
  if (s.escalationTriggers) lines.push(`Escalar a humà quan: ${s.escalationTriggers}`);
  if (s.escalationContact) lines.push(`Contacte d'escalada: ${s.escalationContact}`);
  if (s.disclaimer) lines.push(`Avís legal: ${s.disclaimer}`);
  return lines.join('\n');
}

// ─── Component ───────────────────────────────────────────────────────────────

export default function KnowledgeWizardPage() {
  const router = useRouter();
  const { user } = useAuth();
  const tenantId = user?.tenantId;

  const [step, setStep] = useState<Step>('personalitat');
  const [state, setState] = useState<WizardState>({
    botName: '', tone: 'professional', language: 'auto',
    emojiUse: 'moderat', personalityNotes: '',
    businessName: '', businessType: '', address: '', phone: '',
    email: '', website: '', locationHints: '',
    schedule: 'Dilluns a Divendres: 9h–14h · 16h–20h',
    scheduleWeekend: 'Dissabte: 10h–14h. Diumenge: Tancat.',
    holidays: '', bookingMethod: '', cancellationPolicy: '',
    services: [{ ...BLANK_SERVICE }],
    faq: [{ ...BLANK_FAQ }],
    topicsOffLimits: '', escalationTriggers: '', escalationContact: '',
    disclaimer: '', extra: '',
  });
  const [testMessage, setTestMessage] = useState('Hola! Pots presentar-te i explicar-me els vostres serveis principals?');
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');

  const { data: existing } = useQuery({
    queryKey: ['knowledge', tenantId],
    queryFn: () => getKnowledge(tenantId!),
    enabled: !!tenantId,
  });

  // Pre-fill from existing KB
  useEffect(() => {
    if (!existing) return;
    const cat = (c: string) => (existing.entriesByCategory[c] ?? []).map(e => e.content).join('\n');
    const behavior = cat('BEHAVIOR');
    if (behavior) {
      const nameMatch = behavior.match(/Nom del bot:\s*(.+)/);
      const toneMatch = behavior.match(/To:\s*(.+)/);
      const langMatch = behavior.match(/Idioma:\s*(.+)/);
      const emojiMatch = behavior.match(/Ús d'emojis:\s*(.+)/);
      const notesMatch = behavior.match(/Notes:\s*(.+)/);
      if (nameMatch) setState(s => ({ ...s, botName: nameMatch[1].trim() }));
      if (toneMatch) setState(s => ({ ...s, tone: toneMatch[1].trim() }));
      if (langMatch) setState(s => ({ ...s, language: langMatch[1].trim() }));
      if (emojiMatch) setState(s => ({ ...s, emojiUse: emojiMatch[1].trim() }));
      if (notesMatch) setState(s => ({ ...s, personalityNotes: notesMatch[1].trim() }));
    }
    const business = cat('BUSINESS_INFO');
    if (business) {
      const get = (label: string) => business.match(new RegExp(`${label}:\\s*(.+)`))?.[1]?.trim() ?? '';
      setState(s => ({
        ...s,
        businessName: get('Nom') || s.businessName,
        businessType: get('Tipus') || s.businessType,
        address: get('Adreça') || s.address,
        phone: get('Telèfon') || s.phone,
        email: get('Email') || s.email,
        website: get('Web') || s.website,
        locationHints: get('Com arribar') || s.locationHints,
        bookingMethod: get('Com demanar cita') || s.bookingMethod,
        cancellationPolicy: get('Cancel·lació') || s.cancellationPolicy,
      }));
    }
    const schedule = cat('SCHEDULE');
    if (schedule) {
      const sched = schedule.match(/Horari:\s*(.+)/)?.[1]?.trim() ?? '';
      const weekend = schedule.match(/Cap de setmana:\s*(.+)/)?.[1]?.trim() ?? '';
      const holidays = schedule.match(/Festius:\s*(.+)/)?.[1]?.trim() ?? '';
      if (sched) setState(s => ({ ...s, schedule: sched }));
      if (weekend) setState(s => ({ ...s, scheduleWeekend: weekend }));
      if (holidays) setState(s => ({ ...s, holidays }));
    }
    const services = cat('SERVICE');
    if (services) {
      const parsed = services.split('\n').filter(Boolean).map(line => {
        const parts = line.split(' — ');
        return {
          name: parts[0] ?? '',
          description: parts[1] ?? '',
          price: parts.find(p => p.startsWith('Preu:'))?.replace('Preu: ', '') ?? '',
          duration: parts.find(p => p.startsWith('Durada:'))?.replace('Durada: ', '') ?? '',
          bookingRequired: parts.find(p => p.startsWith('Cita prèvia:'))?.replace('Cita prèvia: ', '') ?? 'Sí',
        };
      });
      if (parsed.length > 0) setState(s => ({ ...s, services: parsed }));
    }
    const faq = cat('FAQ');
    if (faq) {
      const pairs = faq.split('\n\n').filter(Boolean).map(block => {
        const q = block.match(/P:\s*(.+)/)?.[1]?.trim() ?? '';
        const a = block.match(/R:\s*(.+)/)?.[1]?.trim() ?? '';
        return { question: q, answer: a };
      }).filter(f => f.question);
      if (pairs.length > 0) setState(s => ({ ...s, faq: pairs }));
    }
    const restrictions = cat('RESTRICTION');
    if (restrictions) {
      const get = (label: string) => restrictions.match(new RegExp(`${label}:\\s*(.+)`))?.[1]?.trim() ?? '';
      setState(s => ({
        ...s,
        topicsOffLimits: get('Temes prohibits') || s.topicsOffLimits,
        escalationTriggers: get('Escalar a humà quan') || s.escalationTriggers,
        escalationContact: get("Contacte d'escalada") || s.escalationContact,
        disclaimer: get('Avís legal') || s.disclaimer,
      }));
    }
    const extra = cat('EXTRA');
    if (extra) setState(s => ({ ...s, extra }));
  }, [existing]);

  const kbTestMutation = useMutation({
    mutationFn: () => testKnowledgeResponse(tenantId!, testMessage),
  });

  const upd = (key: keyof WizardState) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) =>
    setState(s => ({ ...s, [key]: e.target.value }));

  const saveCurrentStep = async (): Promise<boolean> => {
    if (!tenantId) return false;
    setSaving(true);
    setSaveError('');
    try {
      const saves: Promise<void>[] = [];
      if (step === 'personalitat') {
        saves.push(updateEntries(tenantId, 'BEHAVIOR', [{ content: serializeBehavior(state) }]));
      } else if (step === 'negoci') {
        const biz = serializeBusinessInfo(state);
        const sched = serializeSchedule(state);
        if (biz) saves.push(updateEntries(tenantId, 'BUSINESS_INFO', [{ content: biz }]));
        if (sched) saves.push(updateEntries(tenantId, 'SCHEDULE', [{ content: sched }]));
      } else if (step === 'serveis') {
        const svc = serializeServices(state.services);
        const faq = serializeFaq(state.faq);
        if (svc) saves.push(updateEntries(tenantId, 'SERVICE', [{ content: svc }]));
        if (faq) saves.push(updateEntries(tenantId, 'FAQ', [{ content: faq }]));
      } else if (step === 'restriccions') {
        const rest = serializeRestrictions(state);
        if (rest) saves.push(updateEntries(tenantId, 'RESTRICTION', [{ content: rest }]));
        if (state.extra) saves.push(updateEntries(tenantId, 'EXTRA', [{ content: state.extra }]));
      }
      await Promise.all(saves);
      return true;
    } catch (e) {
      setSaveError(e instanceof Error ? e.message : 'Error desant');
      return false;
    } finally {
      setSaving(false);
    }
  };

  const goNext = async () => {
    const ok = await saveCurrentStep();
    if (!ok) return;
    const idx = STEPS.findIndex(s => s.key === step);
    if (idx < STEPS.length - 1) setStep(STEPS[idx + 1].key);
  };

  const goPrev = () => {
    const idx = STEPS.findIndex(s => s.key === step);
    if (idx > 0) setStep(STEPS[idx - 1].key);
  };

  const stepIdx = STEPS.findIndex(s => s.key === step);

  const inputCls = 'w-full px-3 py-2 bg-bg-1 border border-border-base rounded text-sm focus:outline-none focus:border-accent';
  const labelCls = 'block text-xs text-ink-3 uppercase tracking-wider mb-1 f-mono';
  const textareaCls = `${inputCls} resize-none font-mono`;

  const addService = () => setState(s => ({ ...s, services: [...s.services, { ...BLANK_SERVICE }] }));
  const removeService = (i: number) => setState(s => ({ ...s, services: s.services.filter((_, j) => j !== i) }));
  const updateService = (i: number, key: keyof ServiceItem, val: string) =>
    setState(s => { const svcs = [...s.services]; svcs[i] = { ...svcs[i], [key]: val }; return { ...s, services: svcs }; });

  const addFaq = () => setState(s => ({ ...s, faq: [...s.faq, { ...BLANK_FAQ }] }));
  const removeFaq = (i: number) => setState(s => ({ ...s, faq: s.faq.filter((_, j) => j !== i) }));
  const updateFaq = (i: number, key: keyof FaqItem, val: string) =>
    setState(s => { const f = [...s.faq]; f[i] = { ...f[i], [key]: val }; return { ...s, faq: f }; });

  return (
    <PortalShell breadcrumb="Assistent de configuració del Bot IA" backHref="/portal/agents">
      <div className="max-w-3xl mx-auto px-4 py-6">

        {/* Step indicator */}
        <div className="flex items-center gap-1 mb-8 overflow-x-auto pb-1">
          {STEPS.map((s, i) => (
            <div key={s.key} className="flex items-center gap-1 shrink-0">
              <button
                onClick={() => i < stepIdx && setStep(s.key)}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs f-mono transition
                  ${s.key === step ? 'bg-accent text-black font-bold' :
                    i < stepIdx ? 'bg-accent/20 text-accent cursor-pointer hover:bg-accent/30' :
                    'bg-bg-1 text-ink-3'}`}
              >
                <span>{s.icon}</span><span className="hidden sm:inline">{s.label}</span>
              </button>
              {i < STEPS.length - 1 && <span className="text-ink-3 text-xs">›</span>}
            </div>
          ))}
        </div>

        {/* Step 1 — Personalitat */}
        {step === 'personalitat' && (
          <div className="space-y-5">
            <div>
              <h2 className="text-lg font-bold text-ink-0 mb-1">Personalitat del bot</h2>
              <p className="text-sm text-ink-3">Defineix com es presentarà i comunicarà el bot amb els teus clients.</p>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className={labelCls}>Nom del bot</label>
                <input value={state.botName} onChange={upd('botName')} placeholder="Ex: Marta, Assistent de Clínica Pons..." className={inputCls} />
              </div>
              <div>
                <label className={labelCls}>To de comunicació</label>
                <select value={state.tone} onChange={upd('tone')} className={inputCls}>
                  <option value="professional">Professional</option>
                  <option value="proper">Proper i amigable</option>
                  <option value="formal">Formal</option>
                  <option value="desinvolt">Desinvolt i modern</option>
                </select>
              </div>
              <div>
                <label className={labelCls}>Idioma</label>
                <select value={state.language} onChange={upd('language')} className={inputCls}>
                  <option value="auto">Automàtic (idioma del client)</option>
                  <option value="bilingual_adapt">Adaptatiu (català/castellà)</option>
                  <option value="catala">Sempre en català</option>
                  <option value="castella">Sempre en castellà</option>
                  <option value="english">Sempre en anglès</option>
                  <option value="german">Sempre en alemany</option>
                </select>
              </div>
              <div>
                <label className={labelCls}>Emojis</label>
                <select value={state.emojiUse} onChange={upd('emojiUse')} className={inputCls}>
                  <option value="cap">Sense emojis</option>
                  <option value="moderat">Moderat (recomanat)</option>
                  <option value="expressiu">Expressiu</option>
                </select>
              </div>
            </div>
            <div>
              <label className={labelCls}>Notes de personalitat (opcional)</label>
              <textarea value={state.personalityNotes} onChange={upd('personalityNotes')} rows={3}
                placeholder="Ex: Som un negoci familiar, respon amb calidesa. Usa 'vosaltres'."
                className={textareaCls} />
            </div>
          </div>
        )}

        {/* Step 2 — Negoci */}
        {step === 'negoci' && (
          <div className="space-y-5">
            <div>
              <h2 className="text-lg font-bold text-ink-0 mb-1">Informació del negoci</h2>
              <p className="text-sm text-ink-3">Dades que el bot usarà per respondre consultes sobre on sou, quan obriu i com demanar cita.</p>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className={labelCls}>Nom del negoci</label>
                <input value={state.businessName} onChange={upd('businessName')} placeholder="Ex: Clínica Dental Pons" className={inputCls} />
              </div>
              <div>
                <label className={labelCls}>Tipus de negoci</label>
                <input value={state.businessType} onChange={upd('businessType')} placeholder="Ex: Clínica dental a Palma" className={inputCls} />
              </div>
              <div className="sm:col-span-2">
                <label className={labelCls}>Adreça completa</label>
                <input value={state.address} onChange={upd('address')} placeholder="Ex: Av. Joan Miró 45, Palma (davant Mercadona)" className={inputCls} />
              </div>
              <div>
                <label className={labelCls}>Telèfon</label>
                <input value={state.phone} onChange={upd('phone')} placeholder="+34 971 234 567" className={inputCls} />
              </div>
              <div>
                <label className={labelCls}>Email</label>
                <input value={state.email} onChange={upd('email')} placeholder="info@negoci.com" className={inputCls} />
              </div>
              <div>
                <label className={labelCls}>Web (opcional)</label>
                <input value={state.website} onChange={upd('website')} placeholder="www.negoci.com" className={inputCls} />
              </div>
              <div>
                <label className={labelCls}>Indicacions per arribar (opcional)</label>
                <input value={state.locationHints} onChange={upd('locationHints')} placeholder="Planta baixa, porta blava" className={inputCls} />
              </div>
            </div>
            <div className="border-t border-border-base pt-4">
              <div className="text-sm font-semibold text-ink-1 mb-3">Horaris d'atenció</div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className={labelCls}>Dilluns – Divendres</label>
                  <input value={state.schedule} onChange={upd('schedule')} placeholder="9h–14h · 16h–20h" className={inputCls} />
                </div>
                <div>
                  <label className={labelCls}>Cap de setmana</label>
                  <input value={state.scheduleWeekend} onChange={upd('scheduleWeekend')} placeholder="Dissabte: 10h–14h. Diumenge: Tancat." className={inputCls} />
                </div>
                <div className="sm:col-span-2">
                  <label className={labelCls}>Festius / Tancaments especials (opcional)</label>
                  <input value={state.holidays} onChange={upd('holidays')} placeholder="Tancat el 17 de gener (Sant Antoni)" className={inputCls} />
                </div>
              </div>
            </div>
            <div className="border-t border-border-base pt-4">
              <div className="text-sm font-semibold text-ink-1 mb-3">Cites i reserves</div>
              <div className="grid grid-cols-1 gap-4">
                <div>
                  <label className={labelCls}>Com demanar cita</label>
                  <input value={state.bookingMethod} onChange={upd('bookingMethod')} placeholder="Per WhatsApp o trucada. Mínim 24h d'antelació." className={inputCls} />
                </div>
                <div>
                  <label className={labelCls}>Política de cancel·lació</label>
                  <input value={state.cancellationPolicy} onChange={upd('cancellationPolicy')} placeholder="Cancel·lació gratuïta fins 4h abans." className={inputCls} />
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Step 3 — Serveis + FAQ */}
        {step === 'serveis' && (
          <div className="space-y-6">
            <div>
              <h2 className="text-lg font-bold text-ink-0 mb-1">Serveis i preguntes freqüents</h2>
              <p className="text-sm text-ink-3">El bot usarà aquesta informació per respondre "quant costa...?" i "feis...?"</p>
            </div>

            {/* Serveis */}
            <div>
              <div className="flex items-center justify-between mb-3">
                <span className="text-sm font-semibold text-ink-1">Serveis</span>
                <button onClick={addService} className="text-xs text-accent hover:opacity-70 f-mono">+ Afegir servei</button>
              </div>
              <div className="space-y-3">
                {state.services.map((svc, i) => (
                  <div key={i} className="amg-card card-clip p-4 space-y-3">
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                      <div className="col-span-2">
                        <label className={labelCls}>Nom del servei *</label>
                        <input value={svc.name} onChange={e => updateService(i, 'name', e.target.value)} placeholder="Ex: Neteja dental" className={inputCls} />
                      </div>
                      <div>
                        <label className={labelCls}>Preu</label>
                        <input value={svc.price} onChange={e => updateService(i, 'price', e.target.value)} placeholder="45€" className={inputCls} />
                      </div>
                      <div>
                        <label className={labelCls}>Durada</label>
                        <input value={svc.duration} onChange={e => updateService(i, 'duration', e.target.value)} placeholder="30 min" className={inputCls} />
                      </div>
                      <div className="col-span-2 sm:col-span-3">
                        <label className={labelCls}>Descripció</label>
                        <input value={svc.description} onChange={e => updateService(i, 'description', e.target.value)}
                          placeholder="Breu descripció..." className={inputCls} />
                      </div>
                      <div>
                        <label className={labelCls}>Cita prèvia</label>
                        <select value={svc.bookingRequired} onChange={e => updateService(i, 'bookingRequired', e.target.value)} className={inputCls}>
                          <option>Sí</option>
                          <option>No</option>
                          <option>Recomanable</option>
                        </select>
                      </div>
                    </div>
                    {state.services.length > 1 && (
                      <button onClick={() => removeService(i)} className="text-xs text-danger hover:opacity-70">Eliminar</button>
                    )}
                  </div>
                ))}
              </div>
            </div>

            {/* FAQ */}
            <div className="border-t border-border-base pt-5">
              <div className="flex items-center justify-between mb-3">
                <span className="text-sm font-semibold text-ink-1">Preguntes freqüents</span>
                <button onClick={addFaq} className="text-xs text-accent hover:opacity-70 f-mono">+ Afegir pregunta</button>
              </div>
              <div className="space-y-3">
                {state.faq.map((item, i) => (
                  <div key={i} className="amg-card card-clip p-4 space-y-2">
                    <input value={item.question} onChange={e => updateFaq(i, 'question', e.target.value)}
                      placeholder="Pregunta del client..." className={inputCls} />
                    <textarea value={item.answer} onChange={e => updateFaq(i, 'answer', e.target.value)}
                      placeholder="Resposta completa i útil..." rows={2} className={textareaCls} />
                    {state.faq.length > 1 && (
                      <button onClick={() => removeFaq(i)} className="text-xs text-danger hover:opacity-70">Eliminar</button>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* Step 4 — Restriccions */}
        {step === 'restriccions' && (
          <div className="space-y-5">
            <div>
              <h2 className="text-lg font-bold text-ink-0 mb-1">Restriccions i escalada</h2>
              <p className="text-sm text-ink-3">Defineix quan el bot ha de parar i avisar a un humà.</p>
            </div>
            <div>
              <label className={labelCls}>Temes que el bot NO ha de respondre</label>
              <textarea value={state.topicsOffLimits} onChange={upd('topicsOffLimits')} rows={3}
                placeholder="Ex: No donar preus de la competència. No parlar de casos de clients concrets."
                className={textareaCls} />
            </div>
            <div>
              <label className={labelCls}>Situacions que requereixen atenció humana</label>
              <textarea value={state.escalationTriggers} onChange={upd('escalationTriggers')} rows={3}
                placeholder="Ex: Queixes greus, urgències, pressupostos +500€."
                className={textareaCls} />
            </div>
            <div>
              <label className={labelCls}>Contacte per derivar (telèfon, email, persona)</label>
              <input value={state.escalationContact} onChange={upd('escalationContact')}
                placeholder="Ex: Truqueu al 971 234 567 o demaneu la Sra. Pons."
                className={inputCls} />
            </div>
            <div>
              <label className={labelCls}>Avís legal / Disclaimer (opcional)</label>
              <textarea value={state.disclaimer} onChange={upd('disclaimer')} rows={2}
                placeholder="Ex: La informació és orientativa. Consulteu sempre el professional."
                className={textareaCls} />
            </div>
            <div className="border-t border-border-base pt-4">
              <label className={labelCls}>Informació addicional (opcional)</label>
              <textarea value={state.extra} onChange={upd('extra')} rows={4}
                placeholder="Qualsevol altra informació: història del negoci, premis, certificats, valors..."
                className={textareaCls} />
            </div>
          </div>
        )}

        {/* Step 5 — Test */}
        {step === 'test' && (
          <div className="space-y-6">
            <div>
              <h2 className="text-lg font-bold text-ink-0 mb-1">Prova el bot</h2>
              <p className="text-sm text-ink-3">Configura el bot guardada. Envia un missatge per veure com respondria.</p>
            </div>
            <div className="amg-card card-clip p-5 bg-success/5 border border-success/30 space-y-2">
              <div className="text-sm text-success font-semibold">✓ Configuració desada correctament</div>
              <div className="text-xs text-ink-3">El bot ja coneix el negoci, els serveis i les restriccions configurats.</div>
            </div>
            <div className="space-y-3">
              <div className="flex gap-2">
                <input
                  value={testMessage}
                  onChange={e => setTestMessage(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && testMessage.trim() && kbTestMutation.mutate()}
                  placeholder="Escriu un missatge de prova..."
                  className={`flex-1 ${inputCls}`}
                />
                <button
                  onClick={() => kbTestMutation.mutate()}
                  disabled={kbTestMutation.isPending || !testMessage.trim()}
                  className="px-4 py-2 bg-accent text-black rounded text-sm hover:opacity-90 disabled:opacity-50 shrink-0 f-mono font-bold"
                >
                  {kbTestMutation.isPending ? '…' : 'Enviar'}
                </button>
              </div>
              {kbTestMutation.data && (
                <div className="p-4 amg-card card-clip space-y-2">
                  <div className="text-xs text-ink-3 f-mono uppercase tracking-wider">Resposta del bot</div>
                  <p className="text-sm text-ink-1 whitespace-pre-wrap leading-relaxed">{kbTestMutation.data.response}</p>
                </div>
              )}
              {kbTestMutation.isError && (
                <div className="p-3 bg-danger/5 border border-danger/30 rounded text-xs text-danger">
                  {kbTestMutation.error instanceof Error ? kbTestMutation.error.message : 'Error generant resposta'}
                </div>
              )}
            </div>
            <div className="flex gap-3">
              <button
                onClick={() => router.push('/portal/agents')}
                className="px-6 py-2.5 bg-accent text-black rounded text-sm font-bold hover:opacity-90 f-mono"
              >
                Anar als Agents IA →
              </button>
              <button
                onClick={() => setStep('personalitat')}
                className="px-4 py-2.5 border border-border-base rounded text-sm hover:border-accent hover:text-accent transition"
              >
                Tornar a editar
              </button>
            </div>
          </div>
        )}

        {/* Error */}
        {saveError && (
          <div className="mt-4 p-3 bg-danger/5 border border-danger/30 rounded text-xs text-danger">{saveError}</div>
        )}

        {/* Navigation */}
        {step !== 'test' && (
          <div className="flex items-center justify-between mt-8 pt-4 border-t border-border-base">
            <button
              onClick={goPrev}
              disabled={stepIdx === 0}
              className="px-4 py-2 border border-border-base rounded text-sm hover:border-accent hover:text-accent transition disabled:opacity-30"
            >
              ← Anterior
            </button>
            <button
              onClick={goNext}
              disabled={saving}
              className="px-6 py-2 bg-accent text-black rounded text-sm font-bold hover:opacity-90 disabled:opacity-50 f-mono"
            >
              {saving ? 'Desant...' : stepIdx === STEPS.length - 2 ? 'Finalitzar →' : 'Continua →'}
            </button>
          </div>
        )}
      </div>
    </PortalShell>
  );
}
