'use client';

import { useState, useEffect, type FC } from 'react';
import { useEditorStore } from '@/store/editor';
import { BLOCK_TEMPLATES, generateBlock, listAIModels, type AIModelInfo } from '@/services/factory';
import { ImagePicker } from './ImagePicker';

export const BlockProperties: FC = () => {
  const selectedBlockId = useEditorStore((s) => s.selectedBlockId);
  const block = useEditorStore((s) => s.content.blocks.find((b) => b.id === selectedBlockId));
  const updateBlockProps = useEditorStore((s) => s.updateBlockProps);
  const landing = useEditorStore((s) => s.landing);
  const styles = useEditorStore((s) => s.styles);
  const [imgPickerFor, setImgPickerFor] = useState<string | null>(null);

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
    if (key === 'bgImage' || key === 'ogImage') {
      return (
        <div key={key} className="mb-3">
          <label className="f-mono text-label uppercase text-ink-3 block mb-1">Imatge de fons</label>
          <div className="flex gap-1">
            <input
              value={String(value || '')}
              onChange={(e) => handleChange(key, e.target.value)}
              placeholder="URL o puja..."
              className="flex-1 bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0 min-w-0"
            />
            <button
              onClick={() => setImgPickerFor(key)}
              className="px-2 py-1 bg-[#FF6B00] text-black text-xs rounded hover:bg-[#FF9A3C] shrink-0 f-mono"
              title="Seleccionar imatge"
            >
              📷
            </button>
          </div>
          {!!value && (
            <div className="mt-1 h-16 rounded overflow-hidden bg-[#0d0d1a]">
              <img src={String(value)} alt="" className="w-full h-full object-cover" />
            </div>
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

  return (
    <div className="p-3">
      <div className="f-mono text-label uppercase tracking-widest text-accent-light mb-3">
        {tpl?.label || block.type}
      </div>

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

      {Object.keys(tpl?.defaultProps || {}).map((key) => {
        const value = key in (block.props || {}) ? block.props[key] : (tpl?.defaultProps || {})[key];
        return renderField(key, value);
      })}

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
