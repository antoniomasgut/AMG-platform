'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { IconSet } from '@/components/ui/icons';
import { useToast } from '@/lib/toast-context';
import {
  getKnowledge,
  updateEntries,
  deleteDocument,
  uploadDocument,
  vectorizeKnowledge,
  previewPromptBlock,
  testKnowledgeResponse,
  type KnowledgeEntry,
  type KnowledgeDocument,
} from '@/services/knowledge';

type CategoryTab = 'BEHAVIOR' | 'BUSINESS_INFO' | 'SCHEDULE' | 'SERVICE' | 'FAQ' | 'RESTRICTION' | 'EXTRA';

const CATEGORIES: { key: CategoryTab; label: string }[] = [
  { key: 'BEHAVIOR', label: 'Comportament' },
  { key: 'BUSINESS_INFO', label: 'Info negoci' },
  { key: 'SCHEDULE', label: 'Horaris' },
  { key: 'SERVICE', label: 'Serveis' },
  { key: 'FAQ', label: 'FAQ' },
  { key: 'RESTRICTION', label: 'Restriccions' },
  { key: 'EXTRA', label: 'Extra' },
];

export default function KnowledgeDetailPage() {
  const { id: tenantId } = useParams<{ id: string }>();
  const router = useRouter();
  const t = useTranslations('knowledge');
  const { toast } = useToast();
  const qc = useQueryClient();
  const [activeTab, setActiveTab] = useState<CategoryTab | 'preview' | 'test'>('BEHAVIOR');
  const [testMessage, setTestMessage] = useState('');
  const [testResult, setTestResult] = useState<{ response: string; preview: string } | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [testLoading, setTestLoading] = useState(false);

  const { data: kb, isLoading } = useQuery({
    queryKey: ['knowledge', tenantId],
    queryFn: () => getKnowledge(tenantId),
  });

  const { mutate: doUpdateEntries, isPending: updatingEntries } = useMutation({
    mutationFn: ({ category, entries }: { category: string; entries: { key?: string; content: string; sortOrder?: number }[] }) =>
      updateEntries(tenantId, category, entries),
    onSuccess: () => {
      toast('success', 'Entrades actualitzades');
      qc.invalidateQueries({ queryKey: ['knowledge', tenantId] });
    },
    onError: () => toast('error', 'Error actualitzant entrades'),
  });

  const { mutate: doDeleteDoc } = useMutation({
    mutationFn: (docId: string) => deleteDocument(tenantId, docId),
    onSuccess: () => {
      toast('success', 'Document eliminat');
      qc.invalidateQueries({ queryKey: ['knowledge', tenantId] });
    },
    onError: () => toast('error', 'Error eliminant document'),
  });

  const { mutate: doUpload, isPending: uploading } = useMutation({
    mutationFn: (file: File) => uploadDocument(tenantId, file),
    onSuccess: () => {
      toast('success', 'Document pujat');
      qc.invalidateQueries({ queryKey: ['knowledge', tenantId] });
    },
    onError: (err: Error) => toast('error', err.message),
  });

  const { mutate: doVectorize, isPending: vectorizing } = useMutation({
    mutationFn: () => vectorizeKnowledge(tenantId),
    onSuccess: (res) => {
      if (res.error) toast('error', res.error);
      else toast('success', `Vectoritzat: ${res.vectorized}/${res.total}${res.failed ? ` · ${res.failed} han fallat (revisa OPENAI_API_KEY)` : ''}`);
      qc.invalidateQueries({ queryKey: ['knowledge', tenantId] });
    },
    onError: () => toast('error', 'Error vectoritzant (revisa OPENAI_API_KEY)'),
  });

  const handlePreview = async () => {
    setPreviewLoading(true);
    try {
      const text = await previewPromptBlock(tenantId);
      setPreview(text);
      setActiveTab('preview');
    } catch {
      toast('error', 'Error generant previsualització');
    } finally {
      setPreviewLoading(false);
    }
  };

  const handleTest = async () => {
    if (!testMessage.trim()) return;
    setTestLoading(true);
    try {
      const res = await testKnowledgeResponse(tenantId, testMessage);
      setTestResult({ response: res.response, preview: res.systemPromptPreview });
      setActiveTab('test');
    } catch {
      toast('error', 'Error provant la resposta');
    } finally {
      setTestLoading(false);
    }
  };

  const entries = (kb?.entriesByCategory?.[activeTab] ?? []) as KnowledgeEntry[];
  const docs = (kb?.documents ?? []) as KnowledgeDocument[];

  return (
    <PortalShell breadcrumb={`admin · coneixement · ${tenantId.slice(0, 8)}`} backHref="/portal/admin/knowledge">
      <div className="p-4 sm:p-8 space-y-6">
        {/* Header */}
        <div className="flex items-start justify-between gap-4">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / coneixement /</span>
            <div className="f-display font-bold text-xl mt-1">
              {t('editing')} <span className="text-ink-3">{tenantId.slice(0, 8)}</span>
            </div>
          </div>
          <div className="flex gap-2">
            <AMGButton size="sm" variant="outline" icon={IconSet.Zap} onClick={() => doVectorize()} loading={vectorizing}>
              Vectoritzar
            </AMGButton>
            <AMGButton size="sm" variant="outline" icon={IconSet.Eye} onClick={handlePreview} loading={previewLoading}>
              {t('preview')}
            </AMGButton>
          </div>
        </div>

        {/* Status bar */}
        <div className="amg-card card-clip p-4 flex items-center gap-4">
          <div className="flex items-center gap-2">
            <span className="f-mono text-label uppercase text-ink-2 text-xs">{t('kbStatus')}</span>
            <AMGBadge tone={kb?.isActive ? 'success' : 'neutral'}>
              {kb?.isActive ? t('active') : t('inactive')}
            </AMGBadge>
          </div>
          <div className="flex items-center gap-2">
            <span className="f-mono text-label uppercase text-ink-2 text-xs">{t('version')}</span>
            <span className="f-mono text-xs bg-[rgba(255,255,255,0.05)] px-2 py-0.5 rounded text-ink-1">
              v{kb?.version ?? '-'}
            </span>
          </div>
        </div>

        {/* Tabs */}
        <div className="flex gap-1 border-b border-border-base flex-wrap">
          {CATEGORIES.map(cat => (
            <button
              key={cat.key}
              onClick={() => setActiveTab(cat.key)}
              className={`f-mono text-label uppercase pb-3 px-2 transition-colors ${
                activeTab === cat.key
                  ? 'text-accent-light border-b-2 border-[#FF6B00]'
                  : 'text-ink-2 hover:text-ink-0'
              }`}
            >
              {cat.label}
            </button>
          ))}
          <button
            onClick={() => setActiveTab('preview')}
            className={`f-mono text-label uppercase pb-3 px-2 transition-colors ${
              activeTab === 'preview'
                ? 'text-accent-light border-b-2 border-[#FF6B00]'
                : 'text-ink-2 hover:text-ink-0'
            }`}
          >
            {t('preview')}
          </button>
          <button
            onClick={() => setActiveTab('test')}
            className={`f-mono text-label uppercase pb-3 px-2 transition-colors ${
              activeTab === 'test'
                ? 'text-accent-light border-b-2 border-[#FF6B00]'
                : 'text-ink-2 hover:text-ink-0'
            }`}
          >
            {t('test')}
          </button>
        </div>

        {/* Tab content */}
        {isLoading ? (
          <div className="flex justify-center py-12">
            <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
          </div>
        ) : activeTab === 'preview' ? (
          <PreviewPanel preview={preview} />
        ) : activeTab === 'test' ? (
          <TestPanel
            testMessage={testMessage}
            setTestMessage={setTestMessage}
            handleTest={handleTest}
            testLoading={testLoading}
            testResult={testResult}
          />
        ) : (
          <>
            {/* Entries section */}
            <EntriesPanel
              category={activeTab}
              entries={entries}
              onSave={(entries) => doUpdateEntries({ category: activeTab, entries })}
              saving={updatingEntries}
              t={t}
            />

            {/* Documents section (shown on EXTRA tab) */}
            {activeTab === 'EXTRA' && (
              <div className="mt-8 space-y-4">
                <div className="flex items-center justify-between">
                  <span className="f-mono text-label uppercase text-ink-3">{t('documents')}</span>
                </div>
                <DocumentUpload onUpload={doUpload} uploading={uploading} t={t} />
                <DocumentList docs={docs} onDelete={(id) => doDeleteDoc(id)} t={t} />
              </div>
            )}
          </>
        )}
      </div>
    </PortalShell>
  );
}

function EntriesPanel({ category, entries, onSave, saving, t }: {
  category: string;
  entries: KnowledgeEntry[];
  onSave: (entries: { key?: string; content: string; sortOrder?: number }[]) => void;
  saving: boolean;
  t: ReturnType<typeof useTranslations<'knowledge'>>;
}) {
  const [items, setItems] = useState<{ key: string; content: string; sortOrder: number }[]>(
    entries.length > 0
      ? entries.map(e => ({ key: e.key, content: e.content, sortOrder: e.sortOrder }))
      : [{ key: '', content: '', sortOrder: 0 }]
  );

  const addItem = () => setItems(i => [...i, { key: '', content: '', sortOrder: i.length }]);
  const removeItem = (idx: number) => setItems(i => i.filter((_, k) => k !== idx));
  const updateItem = (idx: number, field: 'key' | 'content' | 'sortOrder', value: string | number) =>
    setItems(i => i.map((item, k) => k === idx ? { ...item, [field]: value } : item));

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <span className="f-mono text-label uppercase text-ink-3">{t('entries')} · {category}</span>
        <div className="flex gap-2">
          <AMGButton size="sm" variant="outline" icon={IconSet.Plus} onClick={addItem}>{t('addEntry')}</AMGButton>
          <AMGButton size="sm" onClick={() => onSave(items)} loading={saving}>{t('save')}</AMGButton>
        </div>
      </div>
      <div className="space-y-3">
        {items.map((item, idx) => (
          <div key={idx} className="amg-card card-clip p-4 space-y-2">
            <div className="flex items-center justify-between gap-2">
              <input
                type="text"
                placeholder={t('entryKeyPlaceholder')}
                value={item.key}
                onChange={(e) => updateItem(idx, 'key', e.target.value)}
                className="flex-1 bg-[#0d0d1a] border border-border-base px-3 h-8 text-xs text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00] f-mono"
              />
              <div className="flex items-center gap-2">
                <span className="f-mono text-[10px] text-ink-3">{t('order')}</span>
                <input
                  type="number"
                  value={item.sortOrder}
                  onChange={(e) => updateItem(idx, 'sortOrder', parseInt(e.target.value) || 0)}
                  className="w-14 bg-[#0d0d1a] border border-border-base px-2 h-8 text-xs text-ink-0 text-center focus:outline-none focus:border-[#FF6B00]"
                />
                <button onClick={() => removeItem(idx)} className="text-ink-2 hover:text-danger transition-colors">
                  <IconSet.X size={14} />
                </button>
              </div>
            </div>
            <textarea
              placeholder={t('contentPlaceholder')}
              value={item.content}
              onChange={(e) => updateItem(idx, 'content', e.target.value)}
              rows={4}
              className="w-full bg-[#0d0d1a] border border-border-base px-3 py-2 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00] resize-none"
            />
          </div>
        ))}
      </div>
    </div>
  );
}

function DocumentUpload({ onUpload, uploading, t }: {
  onUpload: (file: File) => void;
  uploading: boolean;
  t: ReturnType<typeof useTranslations<'knowledge'>>;
}) {
  const handleFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 10 * 1024 * 1024) {
      alert('El fitxer supera el límit de 10 MB');
      return;
    }
    if (!['application/pdf', 'text/plain'].includes(file.type)) {
      alert('Només s\'accepten PDF i TXT');
      return;
    }
    onUpload(file);
    e.target.value = '';
  };

  return (
    <div className="amg-card card-clip p-4">
      <label className="cursor-pointer flex items-center gap-3">
        <IconSet.Upload size={18} className="text-ink-2" />
        <span className="f-mono text-xs text-ink-2">{t('uploadDoc')}</span>
        <input type="file" accept=".pdf,.txt,application/pdf,text/plain" onChange={handleFile} className="hidden" disabled={uploading} />
      </label>
    </div>
  );
}

function DocumentList({ docs, onDelete, t }: {
  docs: KnowledgeDocument[];
  onDelete: (id: string) => void;
  t: ReturnType<typeof useTranslations<'knowledge'>>;
}) {
  if (docs.length === 0) {
    return (
      <div className="amg-card card-clip p-6 text-center">
        <IconSet.FileText size={24} stroke="#64748b" className="mx-auto mb-2" />
        <p className="f-mono text-xs text-ink-2">{t('noDocs')}</p>
      </div>
    );
  }

  return (
    <div className="space-y-2">
      {docs.map(doc => (
        <div key={doc.id} className="amg-card card-clip p-3 flex items-center justify-between">
          <div className="flex items-center gap-3 min-w-0">
            <IconSet.FileText size={16} className="text-ink-2 shrink-0" />
            <div className="min-w-0">
              <div className="f-display font-bold text-sm truncate">{doc.filename}</div>
              <div className="f-mono text-[10px] text-ink-3">
                {doc.fileSize ? `${(doc.fileSize / 1024).toFixed(0)} KB` : '-'} · {doc.contentType ?? '-'} · {new Date(doc.uploadedAt).toLocaleDateString()}
              </div>
            </div>
          </div>
          <button onClick={() => onDelete(doc.id)} className="text-ink-2 hover:text-danger transition-colors shrink-0">
            <IconSet.Trash size={14} />
          </button>
        </div>
      ))}
    </div>
  );
}

function PreviewPanel({ preview }: { preview: string | null }) {
  return (
    <div className="space-y-4">
      <span className="f-mono text-label uppercase text-ink-3">System Prompt Preview</span>
      {preview ? (
        <pre className="amg-card card-clip p-4 text-sm text-ink-1 whitespace-pre-wrap font-mono leading-relaxed max-h-[600px] overflow-auto">
          {preview}
        </pre>
      ) : (
        <div className="amg-card card-clip p-8 text-center">
          <IconSet.Eye size={24} stroke="#64748b" className="mx-auto mb-2" />
          <p className="f-mono text-xs text-ink-2">Fes clic a "Previsualitzar" per veure el system prompt</p>
        </div>
      )}
    </div>
  );
}

function TestPanel({ testMessage, setTestMessage, handleTest, testLoading, testResult }: {
  testMessage: string;
  setTestMessage: (v: string) => void;
  handleTest: () => void;
  testLoading: boolean;
  testResult: { response: string; preview: string } | null;
}) {
  return (
    <div className="space-y-4">
      <div className="flex gap-3 items-end">
        <div className="flex-1">
          <label className="f-mono text-label uppercase text-ink-2 text-xs block mb-1">Missatge de prova</label>
          <input
            type="text"
            placeholder="Ex: Quant costa una neteja dental?"
            value={testMessage}
            onChange={(e) => setTestMessage(e.target.value)}
            className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]"
            onKeyDown={(e) => e.key === 'Enter' && handleTest()}
          />
        </div>
        <AMGButton size="sm" icon={IconSet.Play} onClick={handleTest} loading={testLoading} disabled={!testMessage.trim()}>
          Provar
        </AMGButton>
      </div>

      {testResult && (
        <div className="space-y-4">
          <div className="amg-card card-clip p-4">
            <div className="f-mono text-label uppercase text-ink-3 text-xs mb-2">Resposta de l'agent</div>
            <div className="text-sm text-ink-0 whitespace-pre-wrap">{testResult.response}</div>
          </div>
          <details className="amg-card card-clip p-4">
            <summary className="f-mono text-label uppercase text-ink-3 text-xs cursor-pointer hover:text-ink-1">
              System prompt complet
            </summary>
            <pre className="mt-3 text-xs text-ink-2 whitespace-pre-wrap font-mono leading-relaxed max-h-[400px] overflow-auto">
              {testResult.preview}
            </pre>
          </details>
        </div>
      )}
    </div>
  );
}
