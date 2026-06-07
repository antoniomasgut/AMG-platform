'use client';

import { useState, useEffect, useMemo } from 'react';
import { SECTOR_TEMPLATES, renderTemplate, extractPlaceholders, type SectorTemplateType, type TemplateVariables } from '@/data/sector-templates';
import { listSectorTemplates, updateSectorTemplate, type SectorTemplateResponse } from '@/services/sectorTemplates';
import { AMGButton } from '@/components/ui/button';
import { AMGInput } from '@/components/ui/input';
import { AMGBadge } from '@/components/ui/badge';
import { IconSet } from '@/components/ui/icons';
import { apiFetch } from '@/services/api';

type TabType = 'prospecting' | 'meta-ads' | 'agent-prompt';

const TAB_LABELS: Record<TabType, string> = {
  'prospecting': 'Prospecting',
  'meta-ads': 'Meta Ads',
  'agent-prompt': 'Agent IA',
};

interface LocalBlock {
  title: string;
  body: string;
  id?: string;
}

export default function SectorTemplatesPage() {
  const [selectedSector, setSelectedSector] = useState(SECTOR_TEMPLATES[0]?.key ?? '');
  const [selectedTab, setSelectedTab] = useState<TabType>('prospecting');
  const [selectedBlock, setSelectedBlock] = useState(0);
  const [variables, setVariables] = useState<TemplateVariables>({});
  const [editMode, setEditMode] = useState(false);
  const [editedBody, setEditedBody] = useState('');
  const [saving, setSaving] = useState(false);
  const [dbBlocks, setDbBlocks] = useState<SectorTemplateResponse[]>([]);
  const [loaded, setLoaded] = useState(false);

  const sector = SECTOR_TEMPLATES.find(s => s.key === selectedSector);

  useEffect(() => {
    setLoaded(false);
    setDbBlocks([]);
    setSelectedBlock(0);
    setEditMode(false);
    if (!selectedSector) return;
    listSectorTemplates({ sector: selectedSector, type: selectedTab })
      .then(setDbBlocks)
      .catch(() => setDbBlocks([]))
      .finally(() => setLoaded(true));
  }, [selectedSector, selectedTab]);

  const blocks: LocalBlock[] = useMemo(() => {
    if (dbBlocks.length > 0) {
      return dbBlocks.map(b => ({ title: b.title, body: b.body, id: b.id }));
    }
    const localSector = SECTOR_TEMPLATES.find(s => s.key === selectedSector);
    return localSector?.templates[selectedTab] ?? [];
  }, [dbBlocks, selectedSector, selectedTab]);

  const currentBlock = blocks[selectedBlock];
  const placeholders = currentBlock ? extractPlaceholders(currentBlock.body) : [];

  const renderedBody = useMemo(() => {
    const body = editMode ? editedBody : (currentBlock?.body ?? '');
    return renderTemplate(body, variables);
  }, [currentBlock, variables, editMode, editedBody]);

  function handleVarChange(key: string, value: string) {
    setVariables(prev => ({ ...prev, [key]: value }));
  }

  function copyToClipboard(text: string) {
    navigator.clipboard.writeText(text);
  }

  function enterEdit() {
    setEditedBody(currentBlock?.body ?? '');
    setEditMode(true);
  }

  async function saveToDb() {
    if (!currentBlock?.id) return;
    setSaving(true);
    try {
      await updateSectorTemplate(currentBlock.id, { body: editedBody });
      setDbBlocks(prev => prev.map(b => b.id === currentBlock.id ? { ...b, body: editedBody } : b));
      setEditMode(false);
    } catch {
      // fallback: keep editing
    } finally {
      setSaving(false);
    }
  }

  if (!sector) {
    return (
      <div className="p-8">
        <p className="text-ink-2">Selecciona un sector</p>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-5xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-display font-bold text-ink-0">Templates per sector</h1>
        <p className="text-ink-2 text-sm mt-1">Selecciona un sector i omple les dades del client per generar el template personalitzat</p>
      </div>

      {/* Sector selector */}
      <div className="flex flex-wrap gap-2">
        {SECTOR_TEMPLATES.map(s => (
          <button
            key={s.key}
            onClick={() => { setSelectedSector(s.key); setSelectedBlock(0); setVariables({}); setEditMode(false); }}
            className={`px-3 py-1.5 text-xs f-mono uppercase rounded-lg border transition-colors ${
              selectedSector === s.key
                ? 'bg-accent text-black border-accent font-bold'
                : 'bg-bg-2 text-ink-2 border-border-base hover:border-accent/50'
            }`}
          >
            {s.label}
          </button>
        ))}
      </div>

      {/* Tenant search for auto-fill */}
      <TenantSearch onSelect={(city) => {
        if (city) setVariables(prev => ({ ...prev, CIUTAT: city }));
      }} />

      {/* Tabs */}
      <div className="flex gap-1 border-b border-border-base">
        {(Object.keys(TAB_LABELS) as TabType[]).map(tab => (
          <button
            key={tab}
            onClick={() => { setSelectedTab(tab); setSelectedBlock(0); setEditMode(false); }}
            className={`px-4 py-2 text-xs f-mono uppercase transition-colors border-b-2 ${
              selectedTab === tab
                ? 'text-accent-light border-accent'
                : 'text-ink-3 border-transparent hover:text-ink-1'
            }`}
          >
            {TAB_LABELS[tab]}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
        {/* Bloc selector + variables */}
        <div className="lg:col-span-2 space-y-4">
          {blocks.length === 0 && loaded && (
            <p className="text-ink-3 text-sm">No hi ha templates per a aquest sector/tipus</p>
          )}
          {!loaded && (
            <p className="text-ink-3 text-sm">Carregant...</p>
          )}
          {/* Block selector */}
          {blocks.length > 1 && (
            <div className="space-y-1">
              <span className="text-label f-mono uppercase text-ink-3">Blocs</span>
              {blocks.map((b, i) => (
                <button
                  key={b.id ?? i}
                  onClick={() => { setSelectedBlock(i); setEditMode(false); }}
                  className={`block w-full text-left px-3 py-2 text-sm rounded-lg transition-colors ${
                    selectedBlock === i
                      ? 'bg-accent-muted text-accent-light border border-border-strong'
                      : 'text-ink-2 hover:bg-bg-2 border border-transparent'
                  }`}
                >
                  {b.title}
                </button>
              ))}
            </div>
          )}

          {/* Variables */}
          {placeholders.length > 0 && (
            <div className="space-y-3">
              <span className="text-label f-mono uppercase text-ink-3">Dades del client</span>
              {placeholders.map(ph => (
                <AMGInput
                  key={ph}
                  label={ph}
                  value={variables[ph] ?? ''}
                  onChange={e => handleVarChange(ph, e.target.value)}
                  placeholder={`{${ph}}`}
                />
              ))}
            </div>
          )}
        </div>

        {/* Preview */}
        <div className="lg:col-span-3 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-label f-mono uppercase text-ink-3">
              Previsualització
              {!editMode && placeholders.some(p => !variables[p]) && (
                <span className="text-warning ml-2">(omple les dades del client)</span>
              )}
            </span>
            <div className="flex gap-2">
              {!editMode ? (
                <>
                  {currentBlock && (
                    <AMGButton size="sm" variant="ghost" icon={IconSet.Edit} onClick={enterEdit}>
                      Editar
                    </AMGButton>
                  )}
                  <AMGButton size="sm" variant="ghost" icon={IconSet.Copy} onClick={() => copyToClipboard(renderedBody)}>
                    Copiar
                  </AMGButton>
                </>
              ) : (
                <>
                  <AMGButton size="sm" variant="ghost" icon={IconSet.X} onClick={() => setEditMode(false)}>
                    Cancel·lar
                  </AMGButton>
                  <AMGButton size="sm" variant="primary" icon={IconSet.Check} loading={saving} onClick={saveToDb}>
                    Desar
                  </AMGButton>
                </>
              )}
            </div>
          </div>

          <div className="card-clip amg-card p-4">
            {editMode ? (
              <textarea
                value={editedBody}
                onChange={e => setEditedBody(e.target.value)}
                className="w-full h-80 bg-bg-1 text-ink-0 text-sm font-mono p-3 rounded-lg border border-border-base resize-y focus:outline-none focus:border-accent"
              />
            ) : (
              <pre className="whitespace-pre-wrap text-sm text-ink-0 font-sans leading-relaxed">
                {renderedBody || 'Selecciona un bloc per veure el template'}
              </pre>
            )}
          </div>

          {placeholders.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {placeholders.map(ph => (
                <AMGBadge key={ph} tone={variables[ph] ? 'success' : 'warning'} mono={false}>
                  {ph}{variables[ph] ? ' ✅' : ' ⚠️'}
                </AMGBadge>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function TenantSearch({ onSelect }: { onSelect: (city: string | null) => void }) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<{ id: string; name: string; city: string | null }[]>([]);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (query.length < 2) { setResults([]); return; }
    const timer = setTimeout(async () => {
      try {
        const res = await apiFetch<{ content: { id: string; name: string; city: string | null }[] }>(`/tenants?search=${encodeURIComponent(query)}&size=10`);
        setResults(res.content);
        setOpen(true);
      } catch { setResults([]); }
    }, 300);
    return () => clearTimeout(timer);
  }, [query]);

  return (
    <div className="relative">
      <label className="f-mono text-[10px] uppercase tracking-wider text-ink-3 block mb-1">
        Cercar client per auto-emplenar {`{CIUTAT}`}
      </label>
      <input
        type="text"
        value={query}
        onChange={e => setQuery(e.target.value)}
        onFocus={() => results.length > 0 && setOpen(true)}
        onBlur={() => setTimeout(() => setOpen(false), 200)}
        placeholder="Nom del client..."
        className="w-full max-w-xs bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm f-mono text-ink-1 focus:outline-none focus:border-accent placeholder:text-ink-3"
      />
      {open && results.length > 0 && (
        <div className="absolute top-full left-0 mt-1 w-full max-w-xs bg-bg-2 border border-border-base rounded-lg shadow-xl z-10 max-h-48 overflow-y-auto">
          {results.map(r => (
            <button
              key={r.id}
              onMouseDown={() => {
                onSelect(r.city ?? null);
                setQuery(`${r.name}${r.city ? ` (${r.city})` : ''}`);
                setOpen(false);
              }}
              className="block w-full text-left px-3 py-2 text-sm text-ink-1 hover:bg-bg-3 transition-colors"
            >
              {r.name}{r.city ? <span className="text-ink-3 ml-2">— {r.city}</span> : null}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
