'use client';

import { useState, useEffect, type FC } from 'react';
import { useEditorStore } from '@/store/editor';
import { BLOCK_TEMPLATES, generateBlock, listAIModels, getLandingDefaults, type AIModelInfo } from '@/services/factory';
import { ImagePicker } from './ImagePicker';
import { IconSet } from '@/components/ui/icons';
import { useAuth } from '@/lib/auth-context';

export const BlockProperties: FC = () => {
  const selectedBlockId = useEditorStore((s) => s.selectedBlockId);
  const block = useEditorStore((s) => s.content.blocks.find((b) => b.id === selectedBlockId));
  const updateBlockProps = useEditorStore((s) => s.updateBlockProps);
  const landing = useEditorStore((s) => s.landing);
  const styles = useEditorStore((s) => s.styles);
  const { user } = useAuth();
  const [imgPickerFor, setImgPickerFor] = useState<string | null>(null);
  const [showJson, setShowJson] = useState(false);
  const [syncingReviews, setSyncingReviews] = useState(false);
  const [syncMsg, setSyncMsg] = useState('');
  const [autoFilling, setAutoFilling] = useState(false);
  const [autoFillMsg, setAutoFillMsg] = useState('');

  // IA generation state
  const [aiOpen, setAiOpen] = useState(false);
  const [aiModels, setAiModels] = useState<AIModelInfo[]>([]);
  const [aiModel, setAiModel] = useState('');
  const [aiName, setAiName] = useState('');
  const [aiSector, setAiSector] = useState('');
  const [aiGenerating, setAiGenerating] = useState(false);
  const [aiError, setAiError] = useState('');
  const [aiSuccess, setAiSuccess] = useState('');

  useEffect(() => {
    if (aiOpen && aiModels.length === 0) {
      listAIModels().then((models) => {
        setAiModels(models);
        if (models.length > 0 && !aiModel) setAiModel(models[0].id);
      }).catch(() => setAiError('No s\'han pogut carregar els models'));
    }
  }, [aiOpen]); // eslint-disable-line

  useEffect(() => {
    if (landing?.title) setAiName(landing.title);
    if (styles.businessType) setAiSector(styles.businessType);
  }, [landing?.title, styles.businessType]); // eslint-disable-line

  const handleGenerate = async () => {
    if (!block || !aiModel) return;
    setAiGenerating(true);
    setAiError('');
    setAiSuccess('');
    try {
      const result = await generateBlock(block.type, aiName, aiSector, aiModel);
      updateBlockProps(block.id, result.props);
      setAiSuccess(`Generat amb ${result.model}`);
      setTimeout(() => setAiSuccess(''), 3000);
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : String(e);
      setAiError(msg.includes('MISSING_API_KEY') ? 'Clau API no configurada. Ves a Configuració → API Keys.' : msg);
    } finally {
      setAiGenerating(false);
    }
  };

  if (!block) {
    return (
      <div className="p-3 text-ink-3 text-xs text-center mt-6">
        Selecciona un bloc per editar les seves propietats
      </div>
    );
  }

  const tpl = BLOCK_TEMPLATES[block.type];

  const handleChange = (key: string, value: unknown) => {
    updateBlockProps(block.id, { [key]: value });
  };

  const handleSyncGBPReviews = async () => {
    if (!user?.tenantId) return;
    setSyncingReviews(true);
    setSyncMsg('');
    try {
      const res = await fetch(`/api/v1/tenants/${user.tenantId}/google/business/reviews/sync`, { method: 'POST' });
      const data = await res.json();
      setSyncMsg(`Sincronitzades ${data.synced ?? 0} ressenyes`);
    } catch {
      setSyncMsg('Error en sincronitzar');
    } finally {
      setSyncingReviews(false);
    }
  };

  const handleAutoFill = async () => {
    if (!user?.tenantId || !block) return;
    setAutoFilling(true);
    setAutoFillMsg('');
    try {
      const defaults = await getLandingDefaults(user.tenantId);
      const updates: Record<string, unknown> = {};
      if (block.type === 'hero') {
        if (defaults.heroSuggestions.title && !block.props.title) updates.title = defaults.heroSuggestions.title;
        if (defaults.heroSuggestions.subtitle && !block.props.subtitle) updates.subtitle = defaults.heroSuggestions.subtitle;
      }
      if (block.type === 'contact-form') {
        if (defaults.phone && !block.props.phone) updates.phone = defaults.phone;
        if (defaults.email && !block.props.email) updates.email = defaults.email;
        if (defaults.address && !block.props.address) updates.address = defaults.address;
      }
      if (block.type === 'footer') {
        if (defaults.phone && !block.props.phone) updates.phone = defaults.phone;
        if (defaults.email && !block.props.email) updates.email = defaults.email;
        if (defaults.address && !block.props.address) updates.address = defaults.address;
        if (defaults.businessName && !block.props.businessName) updates.businessName = defaults.businessName;
      }
      if (Object.keys(updates).length > 0) {
        updateBlockProps(block.id, updates);
        setAutoFillMsg(`${Object.keys(updates).length} camps omplerts`);
      } else {
        setAutoFillMsg('Ja estan omplerts');
      }
      setTimeout(() => setAutoFillMsg(''), 3000);
    } catch {
      setAutoFillMsg('Error en carregar les dades');
    } finally {
      setAutoFilling(false);
    }
  };

  const renderField = (key: string, value: unknown) => {
    // Rich text body
    if (key === 'body') {
      return (
        <div key={key} className="mb-3">
          <label className="f-mono text-label uppercase text-ink-3 block mb-1">{key}</label>
          <textarea
            value={String(value || '')}
            onChange={(e) => handleChange(key, e.target.value)}
            className="w-full bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0 h-24 font-mono resize-y"
          />
        </div>
      );
    }

    // Image URL field with picker button
    if (key === 'bgImage' || key === 'ogImage' || key === 'beforeImage' || key === 'afterImage') {
      const imgVal = String(value || '');
      const imgLabel = key === 'ogImage' ? 'Imatge OG' : key === 'beforeImage' ? 'Imatge Abans' : key === 'afterImage' ? 'Imatge Després' : 'Imatge de fons';
      return (
        <div key={key} className="mb-3">
          <label className="f-mono text-label uppercase text-ink-3 block mb-1">
            {imgLabel}
          </label>
          {imgVal ? (
            <div className="rounded overflow-hidden bg-[#0d0d1a] border border-border-base relative group">
              <div className="aspect-video w-full">
                <img src={imgVal} alt="" className="w-full h-full object-cover" />
              </div>
              <div className="absolute inset-0 bg-black/0 group-hover:bg-black/40 transition flex items-center justify-center gap-2 opacity-0 group-hover:opacity-100">
                <button
                  onClick={() => setImgPickerFor(key)}
                  className="px-3 py-1.5 bg-[#FF6B00] text-black text-xs font-bold rounded f-mono hover:bg-[#FF9A3C]"
                >
                  Canviar
                </button>
                <button
                  onClick={() => handleChange(key, '')}
                  className="px-3 py-1.5 bg-red-600 text-white text-xs font-bold rounded f-mono hover:bg-red-500"
                >
                  Eliminar
                </button>
              </div>
            </div>
          ) : (
            <button
              onClick={() => setImgPickerFor(key)}
              className="w-full aspect-video flex flex-col items-center justify-center gap-2 border-2 border-dashed border-border-base rounded text-ink-3 hover:border-[#FF6B00] hover:text-accent-light transition bg-[#0d0d1a]"
            >
              <IconSet.Image size={22} />
              <span className="f-mono text-[10px] uppercase tracking-wider">Afegir imatge</span>
            </button>
          )}
        </div>
      );
    }

    // Array of items (services, faq, testimonials)
    if (key === 'items' && Array.isArray(value)) {
      const templateItems = tpl?.defaultProps?.items;
      const blankItem = Array.isArray(templateItems) && templateItems.length > 0
        ? Object.fromEntries(Object.keys(templateItems[0] as object).map((k) => [k, k === 'rating' ? 5 : '']))
        : { name: '', desc: '' };
      return (
        <div key={key} className="mb-3">
          <label className="f-mono text-label uppercase text-ink-3 block mb-1">{key}</label>
          {value.map((item: Record<string, unknown>, i: number) => (
            <div key={i} className="bg-[#0d0d1a] p-2 rounded mb-1 border border-[rgba(255,107,0,0.1)]">
              {Object.entries(item).map(([k, v]) => (
                <input
                  key={k}
                  value={String(v || '')}
                  onChange={(e) => {
                    const ni = [...value];
                    ni[i] = { ...ni[i], [k]: e.target.value };
                    handleChange(key, ni);
                  }}
                  placeholder={k}
                  className="w-full bg-transparent text-xs text-ink-0 p-1 mb-1 border-b border-[rgba(255,107,0,0.1)]"
                />
              ))}
              <button
                onClick={() => {
                  const ni = [...value]; ni.splice(i, 1); handleChange(key, ni);
                }}
                className="text-red-400 text-label mt-1 f-mono"
              >
                Eliminar
              </button>
            </div>
          ))}
          <button
            onClick={() => handleChange(key, [...value, blankItem])}
            className="text-accent-light text-label mt-1 f-mono"
          >
            + Afegir
          </button>
        </div>
      );
    }

    // Gallery images array
    if (key === 'images' && Array.isArray(value)) {
      return (
        <div key={key} className="mb-3">
          <label className="f-mono text-label uppercase text-ink-3 block mb-1">Imatges</label>
          <div className="grid grid-cols-3 gap-1 mb-2">
            {(value as string[]).map((url, i) => (
              <div key={i} className="relative aspect-square">
                <img src={url} alt="" className="w-full h-full object-cover rounded" />
                <button
                  onClick={() => {
                    const ni = [...(value as string[])]; ni.splice(i, 1); handleChange(key, ni);
                  }}
                  className="absolute top-0 right-0 w-4 h-4 bg-red-500 text-white text-[10px] rounded-bl flex items-center justify-center"
                >
                  ×
                </button>
              </div>
            ))}
          </div>
          <button
            onClick={() => setImgPickerFor(key)}
            className="w-full py-2 bg-[#0d0d1a] border border-[rgba(255,107,0,0.3)] text-accent-light text-xs rounded f-mono hover:border-[#FF6B00] transition"
          >
            + Afegir imatge
          </button>
        </div>
      );
    }

    // Opening hours — editable grid
    if (key === 'hours' && Array.isArray(value)) {
      return (
        <div key={key} className="mb-3">
          <label className="f-mono text-label uppercase text-ink-3 block mb-1">Horaris</label>
          <div className="space-y-1">
            {(value as Array<{ day: string; open: string; close: string; closed: boolean }>).map((row, i) => (
              <div key={i} className="flex items-center gap-1 bg-[#0d0d1a] rounded px-2 py-1">
                <span className="f-mono text-[10px] text-ink-2 w-20 shrink-0">{row.day}</span>
                {row.closed ? (
                  <span className="f-mono text-[10px] text-ink-3 flex-1">Tancat</span>
                ) : (
                  <>
                    <input
                      type="time"
                      value={row.open}
                      onChange={(e) => {
                        const nh = [...(value as object[])]; nh[i] = { ...nh[i], open: e.target.value }; handleChange(key, nh);
                      }}
                      className="bg-transparent text-xs text-ink-0 w-14 border-b border-border-base"
                    />
                    <span className="text-ink-3 text-xs">–</span>
                    <input
                      type="time"
                      value={row.close}
                      onChange={(e) => {
                        const nh = [...(value as object[])]; nh[i] = { ...nh[i], close: e.target.value }; handleChange(key, nh);
                      }}
                      className="bg-transparent text-xs text-ink-0 w-14 border-b border-border-base"
                    />
                  </>
                )}
                <button
                  onClick={() => {
                    const nh = [...(value as object[])]; nh[i] = { ...nh[i], closed: !row.closed }; handleChange(key, nh);
                  }}
                  className={`ml-auto text-[10px] f-mono px-1 py-0.5 rounded ${row.closed ? 'bg-[rgba(255,107,0,0.15)] text-accent-light' : 'text-ink-3'}`}
                >
                  {row.closed ? 'Obrir' : 'Tancar'}
                </button>
              </div>
            ))}
          </div>
        </div>
      );
    }

    // Default text input
    return (
      <div key={key} className="mb-3">
        <label className="f-mono text-label uppercase text-ink-3 block mb-1">{key}</label>
        <input
          value={String(value || '')}
          onChange={(e) => handleChange(key, e.target.value)}
          className="w-full bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0"
        />
      </div>
    );
  };

  const aiBlockTypes = ['hero','text','services','faq','cta','testimonials','pricing','team','reviews'];
  const canGenerate = aiBlockTypes.includes(block.type);
  const canAutoFill = ['hero', 'contact-form', 'footer'].includes(block.type);

  return (
    <div className="p-3">
      {canAutoFill && (
        <div className="mb-3 flex items-center gap-2">
          <button
            onClick={handleAutoFill}
            disabled={autoFilling}
            className="flex items-center gap-1.5 px-2 py-1 rounded text-xs f-mono bg-[rgba(255,107,0,0.1)] text-accent-light hover:bg-[rgba(255,107,0,0.2)] transition disabled:opacity-40"
          >
            {autoFilling
              ? <span className="w-3 h-3 border-2 border-accent-light border-t-transparent rounded-full animate-spin inline-block" />
              : '↓'}
            Auto-omplir
          </button>
          {autoFillMsg && <span className="f-mono text-[9px] text-green-400">{autoFillMsg}</span>}
        </div>
      )}
      <div className="flex items-center justify-between mb-3">
        <div className="f-mono text-label uppercase tracking-widest text-accent-light">
          {tpl?.label || block.type}
        </div>
        <button
          onClick={() => setShowJson((v) => !v)}
          title="Veure configuració del bloc"
          className={`flex items-center gap-1 px-2 py-1 rounded text-xs f-mono transition ${
            showJson
              ? 'bg-[rgba(255,107,0,0.15)] text-[#FF9A3C]'
              : 'text-ink-2 hover:text-ink-0 hover:bg-[rgba(255,255,255,0.05)]'
          }`}
        >
          <IconSet.Eye size={12} />
          <span>Config</span>
        </button>
      </div>

      {showJson && (
        <div className="mb-4">
          <div className="f-mono text-[9px] uppercase text-ink-3 mb-1 tracking-widest">Configuració actual</div>
          <pre className="bg-[#07070f] border border-border-base rounded p-3 text-[10px] text-green-300 overflow-auto max-h-64 leading-relaxed whitespace-pre-wrap break-all">
            {JSON.stringify(block.props, null, 2)}
          </pre>
        </div>
      )}


      {/* Panel IA */}
      {canGenerate && (
        <div className="mb-4 border border-[rgba(255,107,0,0.25)] rounded overflow-hidden">
          <button
            onClick={() => setAiOpen((v) => !v)}
            className="w-full flex items-center justify-between px-3 py-2 bg-[rgba(255,107,0,0.07)] hover:bg-[rgba(255,107,0,0.12)] transition"
          >
            <span className="f-mono text-[10px] uppercase tracking-wider text-accent-light flex items-center gap-1.5">
              <span>✦</span> Generar amb IA
            </span>
            <span className="text-ink-3 text-xs">{aiOpen ? '▲' : '▼'}</span>
          </button>

          {aiOpen && (
            <div className="p-3 space-y-2 bg-[#0a0a18]">
              <div>
                <label className="f-mono text-[9px] uppercase text-ink-3 block mb-1">Nom del negoci</label>
                <input
                  value={aiName}
                  onChange={(e) => setAiName(e.target.value)}
                  placeholder="Ex: Taller Mecànic Palma"
                  className="w-full bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0 placeholder:text-ink-3"
                />
              </div>
              <div>
                <label className="f-mono text-[9px] uppercase text-ink-3 block mb-1">Sector / Especialitat</label>
                <input
                  value={aiSector}
                  onChange={(e) => setAiSector(e.target.value)}
                  placeholder="Ex: taller mecànic, fisioterapeuta..."
                  className="w-full bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0 placeholder:text-ink-3"
                />
              </div>
              <div>
                <label className="f-mono text-[9px] uppercase text-ink-3 block mb-1">Model IA</label>
                {aiModels.length === 0 ? (
                  <div className="f-mono text-[9px] text-ink-3">Carregant models...</div>
                ) : (
                  <select
                    value={aiModel}
                    onChange={(e) => setAiModel(e.target.value)}
                    className="w-full bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0"
                  >
                    {aiModels.map((m) => (
                      <option key={m.id} value={m.id}>{m.label} ({m.provider})</option>
                    ))}
                  </select>
                )}
              </div>
              {aiError && <div className="f-mono text-[9px] text-red-400 leading-tight">{aiError}</div>}
              {aiSuccess && <div className="f-mono text-[9px] text-green-400">{aiSuccess}</div>}
              <button
                onClick={handleGenerate}
                disabled={aiGenerating || !aiModel}
                className="w-full py-2 bg-[#FF6B00] text-black text-xs font-bold rounded f-mono disabled:opacity-40 flex items-center justify-center gap-2"
              >
                {aiGenerating ? (
                  <><span className="w-3 h-3 border-2 border-black border-t-transparent rounded-full animate-spin inline-block" /> Generant...</>
                ) : '✦ Generar contingut'}
              </button>
              <div className="f-mono text-[8px] text-ink-3">El contingut generat sobreescriurà les propietats actuals del bloc.</div>
            </div>
          )}
        </div>
      )}

      {block.type === 'reviews' && (
        <div className="mb-4 border border-[rgba(255,107,0,0.2)] rounded p-3 space-y-3 bg-[#0a0a18]">
          <div className="f-mono text-[9px] uppercase tracking-widest text-accent-light mb-2">Font de ressenyes</div>

          {/* Source toggle */}
          <div className="flex gap-2">
            {(['manual', 'google_business'] as const).map((src) => (
              <button
                key={src}
                onClick={() => handleChange('source', src)}
                className={`flex-1 py-1.5 rounded text-xs f-mono transition ${
                  (block.props.source ?? 'manual') === src
                    ? 'bg-[#FF6B00] text-black font-bold'
                    : 'bg-[#0d0d1a] border border-border-medium text-ink-2 hover:text-ink-0'
                }`}
              >
                {src === 'manual' ? 'Manual' : 'Google Business'}
              </button>
            ))}
          </div>

          {(block.props.source ?? 'manual') === 'google_business' && (
            <div className="space-y-2">
              <div className="flex gap-2">
                <div className="flex-1">
                  <label className="f-mono text-[9px] uppercase text-ink-3 block mb-1">Valoració mínima</label>
                  <select
                    value={String(block.props.minRating ?? 4)}
                    onChange={(e) => handleChange('minRating', Number(e.target.value))}
                    className="w-full bg-[#0d0d1a] border border-border-medium rounded p-1.5 text-xs text-ink-0"
                  >
                    {[3, 4, 5].map((v) => <option key={v} value={v}>{v} ★</option>)}
                  </select>
                </div>
                <div className="flex-1">
                  <label className="f-mono text-[9px] uppercase text-ink-3 block mb-1">Màxim a mostrar</label>
                  <select
                    value={String(block.props.maxItems ?? 6)}
                    onChange={(e) => handleChange('maxItems', Number(e.target.value))}
                    className="w-full bg-[#0d0d1a] border border-border-medium rounded p-1.5 text-xs text-ink-0"
                  >
                    {[3, 6, 9, 12].map((v) => <option key={v} value={v}>{v}</option>)}
                  </select>
                </div>
              </div>
              <button
                onClick={handleSyncGBPReviews}
                disabled={syncingReviews || !user?.tenantId}
                className="w-full py-1.5 bg-[#0d0d1a] border border-[rgba(255,107,0,0.4)] text-accent-light text-xs rounded f-mono hover:border-[#FF6B00] transition disabled:opacity-40 flex items-center justify-center gap-2"
              >
                {syncingReviews ? (
                  <><span className="w-3 h-3 border-2 border-accent-light border-t-transparent rounded-full animate-spin inline-block" /> Sincronitzant...</>
                ) : '↻ Sincronitzar ressenyes GBP ara'}
              </button>
              {syncMsg && <div className="f-mono text-[9px] text-green-400">{syncMsg}</div>}
              <div className="f-mono text-[8px] text-ink-3">
                Sincronització automàtica diària a les 3:00 AM. Requereix Google Business connectat al portal.
              </div>
            </div>
          )}
        </div>
      )}

      {Object.keys(tpl?.defaultProps || {}).map((key) => {
        // Ocultar camps de source/minRating/maxItems (gestionats al panell superior)
        if (block.type === 'reviews' && ['source', 'minRating', 'maxItems'].includes(key)) return null;
        // Ocultar items si la font és google_business
        if (block.type === 'reviews' && key === 'items' && (block.props.source ?? 'manual') === 'google_business') return null;
        const value = key in (block.props || {}) ? block.props[key] : (tpl?.defaultProps || {})[key];
        return renderField(key, value);
      })}

      {/* Separació inferior (shape divider) */}
      {!['header', 'footer'].includes(block.type) && (() => {
        const div = (block.props.dividerBottom ?? {}) as Record<string, unknown>;
        const shapes = [
          { id: 'none',      label: '—'  },
          { id: 'wave',      label: '〜' },
          { id: 'wave-soft', label: '∿'  },
          { id: 'triangle',  label: '▽'  },
          { id: 'oblique',   label: '╱'  },
          { id: 'curve',     label: '⌣'  },
        ];
        const active = String(div.shape ?? 'none');
        const setDiv = (patch: Record<string, unknown>) =>
          handleChange('dividerBottom', { shape: 'none', color: '#ffffff', flip: false, height: 60, ...div, ...patch });
        return (
          <div className="mt-4 border-t border-border-subtle pt-4">
            <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3 mb-2">Separació inferior</div>
            <div className="flex gap-1 mb-3 flex-wrap">
              {shapes.map((sh) => (
                <button key={sh.id} onClick={() => setDiv({ shape: sh.id })}
                  title={sh.id}
                  className={`w-9 h-7 rounded text-base border transition ${active === sh.id ? 'bg-[#FF6B00] border-[#FF6B00] text-black' : 'bg-[#0d0d1a] border-border-medium text-ink-1 hover:border-[#FF6B00]/50'}`}>
                  {sh.label}
                </button>
              ))}
            </div>
            {active !== 'none' && (
              <div className="space-y-2">
                <div className="flex items-center gap-3">
                  <label className="f-mono text-[9px] uppercase text-ink-3 w-16 shrink-0">Color</label>
                  <input type="color" value={String(div.color ?? '#ffffff')}
                    onChange={(e) => setDiv({ color: e.target.value })}
                    className="w-8 h-7 rounded cursor-pointer border-0 bg-transparent" />
                  <span className="f-mono text-[9px] text-ink-3">{String(div.color ?? '#ffffff')}</span>
                </div>
                <div className="flex items-center gap-3">
                  <label className="f-mono text-[9px] uppercase text-ink-3 w-16 shrink-0">Alçada</label>
                  <input type="range" min="30" max="120" value={Number(div.height ?? 60)}
                    onChange={(e) => setDiv({ height: Number(e.target.value) })}
                    className="flex-1 accent-[#FF6B00]" />
                  <span className="f-mono text-[9px] text-ink-3 w-8">{Number(div.height ?? 60)}px</span>
                </div>
                <div className="flex items-center gap-3">
                  <label className="f-mono text-[9px] uppercase text-ink-3 w-16 shrink-0">Girar</label>
                  <button onClick={() => setDiv({ flip: !div.flip })}
                    className={`px-3 py-1 rounded text-xs f-mono border transition ${div.flip ? 'bg-[#FF6B00] border-[#FF6B00] text-black' : 'bg-[#0d0d1a] border-border-medium text-ink-2 hover:border-[#FF6B00]/50'}`}>
                    ↔ Horitzontal
                  </button>
                </div>
              </div>
            )}
          </div>
        );
      })()}

      {/* ImagePicker modal */}
      {imgPickerFor && (
        <ImagePicker
          onSelect={(url) => {
            if (imgPickerFor === 'images') {
              const current = Array.isArray(block.props.images) ? block.props.images as string[] : [];
              handleChange('images', [...current, url]);
            } else {
              handleChange(imgPickerFor, url);
            }
            setImgPickerFor(null);
          }}
          onClose={() => setImgPickerFor(null)}
        />
      )}
    </div>
  );
};
