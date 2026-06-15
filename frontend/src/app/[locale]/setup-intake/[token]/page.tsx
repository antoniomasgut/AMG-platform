'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams } from 'next/navigation';
import { AMGLogo } from '@/components/ui/AMGLogo';
import {
  getIntakePublic,
  saveIntake,
  type IntakeResponse,
  type IntakeData,
} from '@/services/setupIntake';

// ── Constants de sector ────────────────────────────────────────────────────────
const OFICIS = ['PINTOR', 'ELECTRICISTA', 'FONTANER', 'JARDINER', 'NETEJA', 'TALLER_MECANIC'];
const SALUT = ['FISIOTERAPEUTA', 'PSICOLEG', 'NUTRICIONISTA'];
const CITES = ['PERRUQUERIA', 'ESTETICA', 'VETERINARI', 'PERRUQUERIA_CANINA', 'RESTAURANTE'];

type SectionKey = 'AGENT' | 'WEB' | 'PRESSUPOSTOS' | 'AGENDA' | 'FIDELITZACIO';

function getSections(sector: string | null, phases: number[] | null): SectionKey[] {
  const p = new Set(phases ?? []);
  const s = sector ?? '';
  const sections: SectionKey[] = ['AGENT', 'WEB']; // sempre presents

  if (OFICIS.includes(s)) {
    if (p.has(1)) sections.push('PRESSUPOSTOS');
    if (p.has(4)) sections.push('AGENDA');
    if (p.has(7)) sections.push('FIDELITZACIO');
  } else if (SALUT.includes(s)) {
    sections.push('AGENDA');
    if (p.has(5)) sections.push('PRESSUPOSTOS');
    if (p.has(6)) sections.push('FIDELITZACIO');
  } else if (s === 'RESTAURANTE') {
    sections.push('AGENDA');
    if (p.has(5)) sections.push('FIDELITZACIO');
  } else if (s === 'ACADEMIA') {
    if (p.has(2)) sections.push('PRESSUPOSTOS');
    if (p.has(6)) sections.push('FIDELITZACIO');
  } else if (CITES.includes(s)) {
    sections.push('AGENDA');
    if (p.has(4) || p.has(6)) sections.push('FIDELITZACIO');
  } else if (s === 'GESTORIA') {
    sections.push('PRESSUPOSTOS');
    sections.push('AGENDA');
    if (p.has(6)) sections.push('FIDELITZACIO');
  } else if (s === 'INMOBILIARIA') {
    if (p.has(3)) sections.push('AGENDA');
    if (p.has(6)) sections.push('FIDELITZACIO');
  } else {
    if ((phases?.length ?? 0) > 1) sections.push('PRESSUPOSTOS');
    if ((phases?.length ?? 0) > 2) sections.push('AGENDA');
  }
  return sections;
}

// ── Helpers visuals ────────────────────────────────────────────────────────────
const SECTION_META: Record<SectionKey, { title: string; color: string }> = {
  AGENT:       { title: "L'agent IA",                color: '#6366f1' },
  WEB:         { title: 'Web i presència digital',    color: '#0ea5e9' },
  PRESSUPOSTOS:{ title: 'Pressupostos i preus',       color: '#f59e0b' },
  AGENDA:      { title: 'Agenda i cites',             color: '#10b981' },
  FIDELITZACIO:{ title: 'Seguiment i fidelització',   color: '#ec4899' },
};

const labelCls = 'block text-xs uppercase tracking-wider text-gray-400 font-semibold mb-1';
const inputCls =
  'w-full rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-800 placeholder-gray-300 focus:outline-none focus:ring-2 focus:ring-[#FF6B00]/40 focus:border-[#FF6B00]';
const textareaCls = inputCls + ' resize-none';

// ── Tipus d'element de catàleg ─────────────────────────────────────────────────
interface CatalogItem { name: string; unitPrice: number; unit: string }

// ── Component principal ────────────────────────────────────────────────────────
export default function SetupIntakePage() {
  const params = useParams();
  const token = Array.isArray(params.token) ? params.token[0] : params.token as string;

  const [intake, setIntake] = useState<IntakeResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [completed, setCompleted] = useState(false);
  const [saving, setSaving] = useState(false);
  const [lastSaved, setLastSaved] = useState<Date | null>(null);

  // Estat del formulari
  const [data, setData] = useState<IntakeData>({});
  const [catalogItems, setCatalogItems] = useState<CatalogItem[]>([{ name: '', unitPrice: 0, unit: 'unitat' }]);

  // Debounce per auto-save
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Carrega la fitxa al muntar
  useEffect(() => {
    if (!token) return;
    getIntakePublic(token)
      .then(r => {
        setIntake(r);
        if (r.status === 'COMPLETE') setCompleted(true);
        if (r.intakeData) {
          try {
            const parsed = JSON.parse(r.intakeData) as IntakeData;
            setData(parsed);
            if (parsed.catalogItems) setCatalogItems(parsed.catalogItems);
          } catch {
            // intakeData corrupte: ignora
          }
        }
      })
      .catch(() => setError('No s\'ha trobat la fitxa de configuració.'))
      .finally(() => setLoading(false));
  }, [token]);

  // Auto-save amb debounce 1.5s
  const scheduleAutoSave = useCallback((nextData: IntakeData) => {
    if (!token) return;
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(async () => {
      try {
        setSaving(true);
        await saveIntake(token, nextData, false);
        setLastSaved(new Date());
      } catch {
        // error silenciós en auto-save
      } finally {
        setSaving(false);
      }
    }, 1500);
  }, [token]);

  function updateData(patch: Partial<IntakeData>) {
    const next = { ...data, ...patch };
    setData(next);
    scheduleAutoSave(next);
  }

  function updateCatalogItems(items: CatalogItem[]) {
    setCatalogItems(items);
    const next = { ...data, catalogItems: items };
    setData(next);
    scheduleAutoSave(next);
  }

  async function handleComplete() {
    if (!token) return;
    try {
      setSaving(true);
      const result = await saveIntake(token, { ...data, catalogItems }, true);
      setIntake(result);
      setCompleted(true);
    } catch {
      alert('Error en desar la fitxa. Torna-ho a intentar.');
    } finally {
      setSaving(false);
    }
  }

  // ── Renders condicionals ──
  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-gray-400 text-sm">Carregant fitxa…</div>
      </div>
    );
  }

  if (error || !intake) {
    return (
      <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center gap-4 p-6">
        <AMGLogo className="w-40 h-auto" />
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 max-w-md w-full text-center">
          <div className="text-4xl mb-3">🔍</div>
          <h1 className="text-lg font-bold text-gray-800 mb-2">Fitxa no trobada</h1>
          <p className="text-sm text-gray-500">{error ?? 'L\'enllaç és incorrecte o ha caducat.'}</p>
        </div>
      </div>
    );
  }

  const sections = getSections(intake.sector, intake.phaseNumbers);

  if (completed) {
    return (
      <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center gap-6 p-6">
        <AMGLogo className="w-48 h-auto" />
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 max-w-lg w-full text-center">
          <div className="text-5xl mb-4">✅</div>
          <h1 className="text-xl font-bold text-gray-800 mb-2">Fitxa completada</h1>
          <p className="text-sm text-gray-500 mb-6">
            Hem rebut la informació de configuració. L&apos;equip d&apos;AMG Digitalitzacions
            es posarà en contacte aviat per iniciar la implementació.
          </p>
          {intake.tenantName && (
            <div className="rounded-lg bg-orange-50 border border-orange-200 p-4 text-left">
              <div className="text-xs text-orange-600 font-semibold uppercase tracking-wider mb-2">Negoci</div>
              <div className="text-sm font-semibold text-gray-800">{intake.tenantName}</div>
              {intake.sector && <div className="text-xs text-gray-500 mt-1">{intake.sector}</div>}
            </div>
          )}
        </div>
      </div>
    );
  }

  // ── Formulari principal ────────────────────────────────────────────────────────
  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4">
      <div className="max-w-2xl mx-auto space-y-6">

        {/* Capçalera */}
        <div className="flex flex-col items-center gap-4 text-center">
          <AMGLogo className="w-44 h-auto" />
          <div>
            <h1 className="text-xl font-bold text-gray-800">Fitxa de configuració</h1>
            {intake.tenantName && (
              <p className="text-sm text-gray-500 mt-1">{intake.tenantName}</p>
            )}
          </div>
        </div>

        {/* Barra de progrés */}
        <ProgressBar sections={sections} data={data} />

        {/* Indicador de desat */}
        <div className="text-right text-xs text-gray-400 h-4">
          {saving ? 'Desant…' : lastSaved ? `Desat ${lastSaved.toLocaleTimeString('ca-ES', { hour: '2-digit', minute: '2-digit' })}` : ''}
        </div>

        {/* Seccions */}
        {sections.map(section => (
          <SectionCard key={section} section={section}>
            {section === 'AGENT' && (
              <AgentSection data={data} onChange={updateData} />
            )}
            {section === 'WEB' && (
              <WebSection data={data} onChange={updateData} />
            )}
            {section === 'PRESSUPOSTOS' && (
              <PressupostosSection data={data} catalogItems={catalogItems} onChange={updateData} onChangeCatalog={updateCatalogItems} />
            )}
            {section === 'AGENDA' && (
              <AgendaSection data={data} onChange={updateData} />
            )}
            {section === 'FIDELITZACIO' && (
              <FidelitzacioSection data={data} onChange={updateData} />
            )}
          </SectionCard>
        ))}

        {/* Notes generals */}
        <SectionCard section="AGENT" customTitle="Notes addicionals" customColor="#6b7280">
          <div>
            <label className={labelCls}>Qualsevol altra informació rellevant</label>
            <textarea
              rows={4}
              value={data.notes ?? ''}
              onChange={e => updateData({ notes: e.target.value })}
              placeholder="Afegeix qualsevol comentari, preferència o informació especial…"
              className={textareaCls}
            />
          </div>
        </SectionCard>

        {/* Botó de completar */}
        <div className="pb-8">
          <button
            onClick={handleComplete}
            disabled={saving}
            className="w-full py-3 rounded-2xl bg-[#FF6B00] hover:bg-[#e55f00] text-white font-semibold text-sm transition disabled:opacity-50"
          >
            {saving ? 'Desant…' : 'Completar fitxa'}
          </button>
          <p className="text-center text-xs text-gray-400 mt-2">
            Les dades es desen automàticament mentre omplis el formulari
          </p>
        </div>
      </div>
    </div>
  );
}

// ── Barra de progrés ───────────────────────────────────────────────────────────
function ProgressBar({ sections, data }: { sections: SectionKey[]; data: IntakeData }) {
  const filled = sections.filter(s => isSectionFilled(s, data)).length;
  const pct = sections.length > 0 ? Math.round((filled / sections.length) * 100) : 0;

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
      <div className="flex justify-between text-xs text-gray-500 mb-2">
        <span>{filled} de {sections.length} seccions completades</span>
        <span className="font-semibold text-[#FF6B00]">{pct}%</span>
      </div>
      <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
        <div
          className="h-full bg-[#FF6B00] rounded-full transition-all duration-500"
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  );
}

function isSectionFilled(section: SectionKey, data: IntakeData): boolean {
  switch (section) {
    case 'AGENT': return !!(data.businessName && data.whatsappPhone);
    case 'WEB': return data.wantsLanding !== undefined;
    case 'PRESSUPOSTOS': return !!(data.budgetValidityDays);
    case 'AGENDA': return !!(data.visitDuration);
    case 'FIDELITZACIO': return !!(data.googleReviewsUrl);
    default: return false;
  }
}

// ── Card contenidor de secció ──────────────────────────────────────────────────
function SectionCard({
  section,
  children,
  customTitle,
  customColor,
}: {
  section: SectionKey;
  children: React.ReactNode;
  customTitle?: string;
  customColor?: string;
}) {
  const meta = SECTION_META[section];
  const color = customColor ?? meta.color;
  const title = customTitle ?? meta.title;

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
      <div
        className="px-5 py-3 border-l-4"
        style={{ borderLeftColor: color, backgroundColor: `${color}10` }}
      >
        <h2 className="text-sm font-bold text-gray-800">{title}</h2>
      </div>
      <div className="p-5 space-y-4">
        {children}
      </div>
    </div>
  );
}

// ── Secció AGENT ───────────────────────────────────────────────────────────────
function AgentSection({ data, onChange }: { data: IntakeData; onChange: (p: Partial<IntakeData>) => void }) {
  return (
    <>
      <div>
        <label className={labelCls}>Nom del negoci <span className="text-red-400">*</span></label>
        <input type="text" className={inputCls}
          value={data.businessName ?? ''}
          onChange={e => onChange({ businessName: e.target.value })}
          placeholder="Ex: Electricitat Miquel Garcia" />
      </div>
      <div>
        <label className={labelCls}>Número de WhatsApp Business <span className="text-red-400">*</span></label>
        <input type="tel" className={inputCls}
          value={data.whatsappPhone ?? ''}
          onChange={e => onChange({ whatsappPhone: e.target.value })}
          placeholder="+34 6XX XXX XXX" />
      </div>
      <div>
        <label className={labelCls}>Horaris d'atenció</label>
        <input type="text" className={inputCls}
          value={data.schedule ?? ''}
          onChange={e => onChange({ schedule: e.target.value })}
          placeholder="Dll-Dv 9-18h, Sàb 9-13h" />
      </div>
      <div>
        <label className={labelCls}>Zona d'actuació / ubicació</label>
        <input type="text" className={inputCls}
          value={data.location ?? ''}
          onChange={e => onChange({ location: e.target.value })}
          placeholder="Palma, Mallorca" />
      </div>
      <div>
        <label className={labelCls}>Principals serveis o productes</label>
        <textarea rows={3} className={textareaCls}
          value={data.mainServices ?? ''}
          onChange={e => onChange({ mainServices: e.target.value })}
          placeholder="Descriu breument els serveis principals que ofereix el teu negoci…" />
      </div>
    </>
  );
}

// ── Secció WEB ────────────────────────────────────────────────────────────────
function WebSection({ data, onChange }: { data: IntakeData; onChange: (p: Partial<IntakeData>) => void }) {
  return (
    <>
      <div>
        <label className={labelCls}>Tens web pròpia ja?</label>
        <div className="flex gap-3">
          <ToggleButton active={data.hasExistingWeb === true} onClick={() => onChange({ hasExistingWeb: true })}>Sí</ToggleButton>
          <ToggleButton active={data.hasExistingWeb === false} onClick={() => onChange({ hasExistingWeb: false })}>No</ToggleButton>
        </div>
      </div>
      {data.hasExistingWeb && (
        <div>
          <label className={labelCls}>URL de la web actual</label>
          <input type="url" className={inputCls}
            value={data.existingWebUrl ?? ''}
            onChange={e => onChange({ existingWebUrl: e.target.value })}
            placeholder="https://www.elnomdelaweb.com" />
        </div>
      )}
      <div>
        <label className={labelCls}>Vols crear una landing page?</label>
        <div className="space-y-2 mt-1">
          {[
            { value: 'none', label: 'Cap landing page' },
            { value: 'micro', label: 'Micro-landing (30€ setup + 9€/mes) — blocs bàsics + formulari' },
            { value: 'pro', label: 'Landing Pro (80€ setup + 15€/mes) — tots els blocs + widget IA' },
          ].map(opt => (
            <label key={opt.value} className="flex items-start gap-2 cursor-pointer">
              <input type="radio" name="wantsLanding"
                checked={data.wantsLanding === opt.value}
                onChange={() => onChange({ wantsLanding: opt.value as IntakeData['wantsLanding'] })}
                className="mt-0.5 accent-[#FF6B00]" />
              <span className="text-sm text-gray-700">{opt.label}</span>
            </label>
          ))}
        </div>
      </div>
      <div>
        <label className={labelCls}>Vols el widget de xat IA a la web?</label>
        <div className="flex gap-3">
          <ToggleButton active={data.wantsChatWidget === true} onClick={() => onChange({ wantsChatWidget: true })}>Sí</ToggleButton>
          <ToggleButton active={data.wantsChatWidget === false} onClick={() => onChange({ wantsChatWidget: false })}>No</ToggleButton>
        </div>
      </div>
      <div>
        <label className={labelCls}>Vols botó de WhatsApp a la web?</label>
        <div className="flex gap-3">
          <ToggleButton active={data.wantsWhatsappButton === true} onClick={() => onChange({ wantsWhatsappButton: true })}>Sí</ToggleButton>
          <ToggleButton active={data.wantsWhatsappButton === false} onClick={() => onChange({ wantsWhatsappButton: false })}>No</ToggleButton>
        </div>
      </div>
      <div>
        <label className={labelCls}>Domini</label>
        <div className="space-y-2 mt-1">
          {[
            { value: 'existing', label: 'Ja en tinc un' },
            { value: 'new', label: 'Vull un de nou' },
            { value: 'none', label: 'Sense domini per ara' },
          ].map(opt => (
            <label key={opt.value} className="flex items-center gap-2 cursor-pointer">
              <input type="radio" name="domainPreference"
                checked={data.domainPreference === opt.value}
                onChange={() => onChange({ domainPreference: opt.value as IntakeData['domainPreference'] })}
                className="accent-[#FF6B00]" />
              <span className="text-sm text-gray-700">{opt.label}</span>
            </label>
          ))}
        </div>
      </div>
      {(data.domainPreference === 'existing' || data.domainPreference === 'new') && (
        <div>
          <label className={labelCls}>
            {data.domainPreference === 'existing' ? 'Quin domini tens?' : 'Preferència de nom de domini'}
          </label>
          <input type="text" className={inputCls}
            value={data.domainName ?? ''}
            onChange={e => onChange({ domainName: e.target.value })}
            placeholder={data.domainPreference === 'existing' ? 'elnomdelaweb.com' : 'elnomdelaweb.com'} />
        </div>
      )}
    </>
  );
}

// ── Secció PRESSUPOSTOS ────────────────────────────────────────────────────────
function PressupostosSection({
  data, catalogItems, onChange, onChangeCatalog
}: {
  data: IntakeData;
  catalogItems: CatalogItem[];
  onChange: (p: Partial<IntakeData>) => void;
  onChangeCatalog: (items: CatalogItem[]) => void;
}) {
  function updateItem(i: number, patch: Partial<CatalogItem>) {
    const next = catalogItems.map((item, idx) => idx === i ? { ...item, ...patch } : item);
    onChangeCatalog(next);
  }
  function addItem() { onChangeCatalog([...catalogItems, { name: '', unitPrice: 0, unit: 'unitat' }]); }
  function removeItem(i: number) { onChangeCatalog(catalogItems.filter((_, idx) => idx !== i)); }

  return (
    <>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className={labelCls}>Validesa del pressupost</label>
          <div className="flex items-center gap-2">
            <input type="number" className={inputCls}
              value={data.budgetValidityDays ?? 30}
              onChange={e => onChange({ budgetValidityDays: Number(e.target.value) })}
              min={1} max={365} />
            <span className="text-sm text-gray-500 whitespace-nowrap">dies</span>
          </div>
        </div>
        <div>
          <label className={labelCls}>Condicions de pagament</label>
          <input type="text" className={inputCls}
            value={data.paymentTerms ?? ''}
            onChange={e => onChange({ paymentTerms: e.target.value })}
            placeholder="50% inici, 50% finalització" />
        </div>
      </div>
      <div>
        <label className={labelCls}>Catàleg de serveis</label>
        <div className="space-y-2">
          {catalogItems.map((item, i) => (
            <div key={i} className="grid grid-cols-[1fr_auto_auto_auto] gap-2 items-center">
              <input type="text" className={inputCls}
                value={item.name}
                onChange={e => updateItem(i, { name: e.target.value })}
                placeholder="Nom del servei" />
              <input type="number" className={`${inputCls} w-24`}
                value={item.unitPrice}
                onChange={e => updateItem(i, { unitPrice: Number(e.target.value) })}
                placeholder="Preu" min={0} />
              <input type="text" className={`${inputCls} w-20`}
                value={item.unit}
                onChange={e => updateItem(i, { unit: e.target.value })}
                placeholder="unitat" />
              <button type="button" onClick={() => removeItem(i)}
                className="p-2 text-gray-400 hover:text-red-500 transition">
                ✕
              </button>
            </div>
          ))}
          <button type="button" onClick={addItem}
            className="text-xs text-[#FF6B00] hover:text-[#e55f00] font-medium mt-1">
            + Afegir servei
          </button>
        </div>
      </div>
    </>
  );
}

// ── Secció AGENDA ──────────────────────────────────────────────────────────────
function AgendaSection({ data, onChange }: { data: IntakeData; onChange: (p: Partial<IntakeData>) => void }) {
  return (
    <>
      <div>
        <label className={labelCls}>Durada habitual de la visita/cita</label>
        <div className="flex items-center gap-2">
          <input type="number" className={`${inputCls} w-32`}
            value={data.visitDuration ?? 60}
            onChange={e => onChange({ visitDuration: Number(e.target.value) })}
            min={5} max={480} step={5} />
          <span className="text-sm text-gray-500">minuts</span>
        </div>
      </div>
      <div>
        <label className={labelCls}>Horari d'agenda (si és diferent del general)</label>
        <input type="text" className={inputCls}
          value={data.scheduleNotes ?? ''}
          onChange={e => onChange({ scheduleNotes: e.target.value })}
          placeholder="Ex: Dll-Dj 10-14h i 16-19h" />
      </div>
      <div>
        <label className={labelCls}>Email de Google Calendar a sincronitzar</label>
        <input type="email" className={inputCls}
          value={data.googleCalendarEmail ?? ''}
          onChange={e => onChange({ googleCalendarEmail: e.target.value })}
          placeholder="correu@gmail.com" />
      </div>
    </>
  );
}

// ── Secció FIDELITZACIO ────────────────────────────────────────────────────────
function FidelitzacioSection({ data, onChange }: { data: IntakeData; onChange: (p: Partial<IntakeData>) => void }) {
  return (
    <>
      <div>
        <label className={labelCls}>URL de ressenyes Google</label>
        <input type="url" className={inputCls}
          value={data.googleReviewsUrl ?? ''}
          onChange={e => onChange({ googleReviewsUrl: e.target.value })}
          placeholder="https://g.page/r/…" />
      </div>
      <div>
        <label className={labelCls}>Dies fins al seguiment post-servei</label>
        <div className="flex items-center gap-2">
          <input type="number" className={`${inputCls} w-24`}
            value={data.followUpDays ?? 7}
            onChange={e => onChange({ followUpDays: Number(e.target.value) })}
            min={1} max={90} />
          <span className="text-sm text-gray-500">dies</span>
        </div>
      </div>
    </>
  );
}

// ── Botó toggle ────────────────────────────────────────────────────────────────
function ToggleButton({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button type="button" onClick={onClick}
      className={`px-4 py-2 rounded-lg border text-sm font-medium transition ${
        active
          ? 'bg-[#FF6B00] border-[#FF6B00] text-white'
          : 'bg-white border-gray-200 text-gray-600 hover:border-[#FF6B00]'
      }`}>
      {children}
    </button>
  );
}
