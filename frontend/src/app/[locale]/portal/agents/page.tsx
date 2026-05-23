'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import {
  getAgentStatus,
  getPendingConversations,
  getConversations,
  approveResponse,
  editAndSend,
  discardResponse,
  updateAgentMode,
  getAvailableModels,
  getAIConfig,
  updateAIConfig,
  testModel,
  getChannels,
  activateAgent,
  deactivateAgent,
  getActivationInstructions,
  updateChannels,
  type AgentStatusResponse,
  type PendingResponseDto,
  type ConversationResponse,
  type ModelInfo,
} from '@/services/agents-conversational';
import {
  getKnowledge,
  updateEntries,
  addDocument,
  deleteDocument,
  previewPromptBlock,
  type KnowledgeBase,
} from '@/services/knowledge';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGBadge } from '@/components/ui/badge';
import { I } from '@/components/ui/icons';

type Tab = 'agent' | 'pending' | 'conversations' | 'coneixement' | 'ia';

const PROVIDER_LABELS: Record<string, string> = {
  anthropic: 'Anthropic',
  deepseek: 'DeepSeek',
  ollama: 'Ollama (local)',
};

const KNOWLEDGE_CATEGORIES = [
  { key: 'BEHAVIOR', label: 'Comportament', hint: 'To, personalitat i instruccions de com ha de respondre l\'agent' },
  { key: 'BUSINESS_INFO', label: 'Informació del negoci', hint: 'Nom, adreça, telèfon, xarxes socials, descripció general' },
  { key: 'SCHEDULE', label: 'Horaris', hint: 'Horari d\'atenció, dies tancats, festius, horari especial' },
  { key: 'SERVICE', label: 'Serveis i preus', hint: 'Llista de serveis amb descripció i preus' },
  { key: 'FAQ', label: 'Preguntes freqüents', hint: 'Respostes a preguntes habituals dels clients' },
  { key: 'RESTRICTION', label: 'Restriccions', hint: 'Temes que l\'agent no ha de tractar, límits d\'actuació' },
  { key: 'EXTRA', label: 'Informació addicional', hint: 'Qualsevol altra informació rellevant' },
];

export default function AgentsPage() {
  const { user, isAdmin } = useAuth();
  const tenantId = user?.tenantId;
  const [activeTab, setActiveTab] = useState<Tab>('agent');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editContent, setEditContent] = useState<string>('');
  const [editingCategory, setEditingCategory] = useState<string | null>(null);
  const [categoryDraft, setCategoryDraft] = useState<string>('');
  const [docFilename, setDocFilename] = useState('');
  const [docContent, setDocContent] = useState('');
  const [showDocForm, setShowDocForm] = useState(false);
  const [showPreview, setShowPreview] = useState(false);
  const [showActivationModal, setShowActivationModal] = useState(false);
  const [whatsappPhone, setWhatsappPhone] = useState('');
  const [whatsappMetaId, setWhatsappMetaId] = useState('');
  const [testMessage, setTestMessage] = useState('Hola! Pots presentar-te breument?');
  const [testSystemPrompt, setTestSystemPrompt] = useState('');
  const [testResult, setTestResult] = useState<{ model: string; provider: string; response: string } | null>(null);
  const [testError, setTestError] = useState<string | null>(null);
  const [selectedTestModel, setSelectedTestModel] = useState<string>('');
  const queryClient = useQueryClient();

  const { data: status } = useQuery({
    queryKey: ['agent-status', tenantId],
    queryFn: () => getAgentStatus(tenantId!),
    enabled: !!user && !!tenantId,
  });

  const { data: channels } = useQuery({
    queryKey: ['channels', tenantId],
    queryFn: () => getChannels(tenantId!),
    enabled: !!user && !!tenantId && activeTab === 'agent',
  });

  const { data: activationInstructions } = useQuery({
    queryKey: ['activation-instructions', tenantId],
    queryFn: () => getActivationInstructions(tenantId!),
    enabled: !!tenantId && showActivationModal,
  });

  const { data: pending = [] } = useQuery({
    queryKey: ['pending-responses', tenantId],
    queryFn: () => getPendingConversations(tenantId!),
    enabled: !!user && !!tenantId,
    refetchInterval: 30000,
  });

  const { data: conversations = [] } = useQuery({
    queryKey: ['conversations', tenantId],
    queryFn: () => getConversations(tenantId!),
    enabled: !!user && !!tenantId,
  });

  const { data: models = [] } = useQuery({
    queryKey: ['available-models'],
    queryFn: () => getAvailableModels(),
    enabled: !!user && isAdmin && activeTab === 'ia',
  });

  const { data: aiConfig } = useQuery({
    queryKey: ['ai-config', tenantId],
    queryFn: () => getAIConfig(tenantId!),
    enabled: !!user && !!tenantId && isAdmin && activeTab === 'ia',
  });

  const updateModeMutation = useMutation({
    mutationFn: (mode: 'AUTO' | 'HYBRID' | 'MANUAL') => updateAgentMode(tenantId!, mode),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['agent-status', tenantId] });
    },
  });

  const activateMutation = useMutation({
    mutationFn: () => activateAgent(tenantId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['channels', tenantId] });
      setShowActivationModal(true);
    },
  });

  const deactivateMutation = useMutation({
    mutationFn: () => deactivateAgent(tenantId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['channels', tenantId] });
    },
  });

  const updateChannelsMutation = useMutation({
    mutationFn: (data: { whatsappPhoneNumber?: string; whatsappMetaPhoneNumberId?: string }) =>
      updateChannels(tenantId!, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['channels', tenantId] });
      setWhatsappPhone('');
      setWhatsappMetaId('');
    },
  });

  const updateAIConfigMutation = useMutation({
    mutationFn: (model: string) => updateAIConfig(tenantId!, { preferredModel: model }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ai-config', tenantId] });
    },
  });

  const testModelMutation = useMutation({
    mutationFn: () => testModel({
      model: selectedTestModel || aiConfig?.preferredModel || 'claude-haiku-4-5-20251001',
      message: testMessage,
      systemPrompt: testSystemPrompt || undefined,
    }),
    onSuccess: (data) => {
      if (data.error) {
        setTestError(data.error);
        setTestResult(null);
      } else {
        setTestResult(data);
        setTestError(null);
      }
    },
    onError: (err: Error) => {
      setTestError(err.message);
      setTestResult(null);
    },
  });

  const approveMutation = useMutation({
    mutationFn: (id: number) => approveResponse(tenantId!, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pending-responses', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['conversations', tenantId] });
    },
  });

  const editMutation = useMutation({
    mutationFn: (vars: { id: number; content: string }) => editAndSend(tenantId!, vars.id, vars.content),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pending-responses', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['conversations', tenantId] });
      setEditingId(null);
      setEditContent('');
    },
  });

  const discardMutation = useMutation({
    mutationFn: (id: number) => discardResponse(tenantId!, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pending-responses', tenantId] });
    },
  });

  const { data: knowledge } = useQuery({
    queryKey: ['knowledge', tenantId],
    queryFn: () => getKnowledge(tenantId!),
    enabled: !!tenantId && activeTab === 'coneixement',
  });

  const { data: promptPreview } = useQuery({
    queryKey: ['knowledge-preview', tenantId],
    queryFn: () => previewPromptBlock(tenantId!),
    enabled: !!tenantId && showPreview,
  });

  const updateEntriesMutation = useMutation({
    mutationFn: ({ category, content }: { category: string; content: string }) => {
      const lines = content.split('\n').filter(l => l.trim());
      const entries = lines.map((line, i) => ({ key: `entry_${i}`, content: line.trim(), sortOrder: i }));
      return updateEntries(tenantId!, category, entries);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['knowledge', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['knowledge-preview', tenantId] });
      setEditingCategory(null);
    },
  });

  const addDocMutation = useMutation({
    mutationFn: () => addDocument(tenantId!, docFilename, docContent),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['knowledge', tenantId] });
      setDocFilename('');
      setDocContent('');
      setShowDocForm(false);
    },
  });

  const deleteDocMutation = useMutation({
    mutationFn: (docId: string) => deleteDocument(tenantId!, docId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['knowledge', tenantId] });
    },
  });

  if (!user || !tenantId) return null;

  const modes: ('AUTO' | 'HYBRID' | 'MANUAL')[] = ['AUTO', 'HYBRID', 'MANUAL'];
  const currentMode = status?.agentMode || 'AUTO';

  const modelsByProvider = models.reduce<Record<string, ModelInfo[]>>((acc, m) => {
    if (!acc[m.provider]) acc[m.provider] = [];
    acc[m.provider].push(m);
    return acc;
  }, {});

  const tabs: { key: Tab; label: string }[] = [
    { key: 'agent', label: 'Agent' },
    { key: 'pending', label: `Pendents (${pending.length})` },
    { key: 'conversations', label: 'Converses' },
    ...(isAdmin ? [{ key: 'coneixement' as Tab, label: 'Coneixement' }] : []),
    ...(isAdmin ? [{ key: 'ia' as Tab, label: 'Model IA' }] : []),
  ];

  return (
    <PortalShell breadcrumb="agents">
      <div className="p-4 sm:p-8 space-y-6">
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / agents /</span>
          <div className="flex items-center gap-3 mt-1">
            <div className="f-display font-bold text-xl">Agents Conversacionals</div>
            {pending.length > 0 && (
              <AMGBadge tone="warning">{pending.length} pendents</AMGBadge>
            )}
          </div>
        </div>

        {/* Tabs */}
        <div className="flex gap-1 border-b border-border-base">
          {tabs.map(({ key, label }) => (
            <button
              key={key}
              onClick={() => setActiveTab(key)}
              className={`px-4 py-2 f-mono text-label uppercase tracking-wider border-b-2 transition ${
                activeTab === key
                  ? 'border-accent text-accent'
                  : 'border-transparent text-ink-2 hover:text-ink-1'
              }`}
            >
              {label}
            </button>
          ))}
        </div>

        {/* Agent Tab */}
        {activeTab === 'agent' && (
          <div className="space-y-6">

            {/* Status */}
            <div className="flex items-center gap-3">
              <span className="f-mono text-label uppercase text-ink-2 tracking-widest">Estat del bot:</span>
              {channels?.isActive ? (
                <AMGBadge tone="success">ACTIU</AMGBadge>
              ) : (channels?.telegramLinked || !!channels?.whatsappPhoneNumber) ? (
                <AMGBadge tone="warning">ATURAT</AMGBadge>
              ) : (
                <AMGBadge tone="neutral">PENDENT CONFIGURAR</AMGBadge>
              )}
            </div>

            {/* Mode selector */}
            <div className="amg-card card-clip p-6 space-y-4">
              <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Mode de funcionament</div>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                {modes.map((mode) => (
                  <button
                    key={mode}
                    onClick={() => updateModeMutation.mutate(mode)}
                    disabled={updateModeMutation.isPending}
                    className={`p-4 rounded border-2 transition text-center ${
                      currentMode === mode
                        ? 'border-accent bg-accent/10'
                        : 'border-border-base hover:border-accent'
                    } ${updateModeMutation.isPending ? 'opacity-50 cursor-not-allowed' : ''}`}
                  >
                    <div className="f-mono text-label font-semibold mb-2">{mode}</div>
                    <p className="text-xs text-ink-2">
                      {mode === 'AUTO' && 'Respostes automàtiques'}
                      {mode === 'HYBRID' && 'Aprova antes d\'enviar'}
                      {mode === 'MANUAL' && 'Resposta manual'}
                    </p>
                  </button>
                ))}
              </div>
            </div>

            {/* Channels */}
            <div className="amg-card card-clip p-6 space-y-4">
              <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Canals</div>

              {/* Telegram */}
              <div className="p-4 bg-bg-1 rounded space-y-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <I.Bell size={18} />
                    <span className="text-sm font-medium">Telegram</span>
                  </div>
                  <AMGBadge tone={channels?.telegramLinked ? 'success' : 'neutral'}>
                    {channels?.telegramLinked ? 'Vinculat' : 'No vinculat'}
                  </AMGBadge>
                </div>
                {channels?.telegramLinked && channels.telegramChatId && (
                  <p className="text-xs text-ink-3 pl-7">Chat ID: {channels.telegramChatId}</p>
                )}
                {!channels?.telegramLinked && (
                  <p className="text-xs text-ink-3 pl-7">
                    El client ha d&apos;escriure al bot de Telegram de l&apos;empresa per vincular el compte.
                  </p>
                )}
              </div>

              {/* WhatsApp */}
              <div className="p-4 bg-bg-1 rounded space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <I.Smartphone size={18} />
                    <span className="text-sm font-medium">WhatsApp</span>
                  </div>
                  <AMGBadge tone={channels?.whatsappPhoneNumber ? 'success' : 'neutral'}>
                    {channels?.whatsappPhoneNumber ? 'Configurat' : 'Pendent'}
                  </AMGBadge>
                </div>
                {channels?.whatsappPhoneNumber && (
                  <p className="text-xs text-ink-3 pl-7">Telèfon: {channels.whatsappPhoneNumber}</p>
                )}
                {!channels?.isActive && !channels?.whatsappPhoneNumber && (
                  <div className="pl-7 space-y-2">
                    <input
                      type="text"
                      value={whatsappPhone}
                      onChange={(e) => setWhatsappPhone(e.target.value)}
                      placeholder="+34612345678"
                      className="w-full p-2 bg-bg-base border border-border-base rounded text-sm"
                    />
                    <input
                      type="text"
                      value={whatsappMetaId}
                      onChange={(e) => setWhatsappMetaId(e.target.value)}
                      placeholder="Meta Phone Number ID"
                      className="w-full p-2 bg-bg-base border border-border-base rounded text-sm"
                    />
                    <button
                      onClick={() => updateChannelsMutation.mutate({
                        whatsappPhoneNumber: whatsappPhone,
                        whatsappMetaPhoneNumberId: whatsappMetaId,
                      })}
                      disabled={updateChannelsMutation.isPending || !whatsappPhone.trim() || !whatsappMetaId.trim()}
                      className="px-4 py-2 bg-accent text-white rounded text-sm hover:opacity-90 disabled:opacity-50"
                    >
                      Desar configuració
                    </button>
                  </div>
                )}
              </div>

              {/* Email */}
              <div className="flex items-center justify-between p-4 bg-bg-1 rounded">
                <div className="flex items-center gap-3">
                  <I.Mail size={18} />
                  <span className="text-sm font-medium">Email</span>
                </div>
                <AMGBadge tone={status?.emailConfigured ? 'success' : 'neutral'}>
                  {status?.emailConfigured ? 'Configurat' : 'Pendent'}
                </AMGBadge>
              </div>
            </div>

            {/* Activate / Deactivate */}
            <div className="flex items-center gap-4">
              {!channels?.isActive ? (
                <button
                  onClick={() => activateMutation.mutate()}
                  disabled={
                    activateMutation.isPending ||
                    (!channels?.telegramLinked && !channels?.whatsappPhoneNumber)
                  }
                  className="px-6 py-3 bg-success text-white rounded font-semibold hover:opacity-90 disabled:opacity-40 flex items-center gap-2"
                >
                  <I.Bot size={16} />
                  {activateMutation.isPending ? 'Activant...' : 'ACTIVAR BOT'}
                </button>
              ) : (
                <button
                  onClick={() => deactivateMutation.mutate()}
                  disabled={deactivateMutation.isPending}
                  className="px-6 py-3 bg-danger text-white rounded font-semibold hover:opacity-90 disabled:opacity-40"
                >
                  {deactivateMutation.isPending ? 'Aturant...' : 'ATURAR BOT'}
                </button>
              )}
              {!channels?.isActive && !channels?.telegramLinked && !channels?.whatsappPhoneNumber && (
                <p className="text-xs text-ink-3">
                  Configura almenys un canal per activar el bot.
                </p>
              )}
            </div>
          </div>
        )}

        {/* Pending Tab */}
        {activeTab === 'pending' && (
          <div>
            {pending.length === 0 ? (
              <div className="amg-card card-clip p-12 text-center">
                <I.Check size={32} stroke="#39d353" className="mx-auto mb-3" />
                <div className="f-display font-bold text-sm mb-1 text-success">Tot al dia</div>
                <p className="f-mono text-label text-ink-2">No hi ha respostes pendents</p>
              </div>
            ) : (
              <div className="space-y-3">
                {pending.map((item) => (
                  <div key={item.id} className="amg-card card-clip overflow-hidden">
                    <div className="p-4 sm:p-5 border-b border-border-base">
                      <div className="flex items-center justify-between mb-2">
                        <div className="flex items-center gap-2">
                          <AMGBadge tone="neutral">{item.channel}</AMGBadge>
                          <span className="text-xs text-ink-2">{item.customerIdentifier}</span>
                        </div>
                        <span className="text-xs text-ink-3">
                          {new Date(item.createdAt).toLocaleString('ca-ES')}
                        </span>
                      </div>
                    </div>

                    <div className="p-4 sm:p-5 space-y-4">
                      <div>
                        <div className="f-mono text-label text-ink-2 uppercase tracking-wider mb-2">Missatge del client</div>
                        <p className="text-sm text-ink-1 bg-bg-1 p-3 rounded">{item.customerMessage}</p>
                      </div>

                      {editingId === item.id ? (
                        <div>
                          <div className="f-mono text-label text-ink-2 uppercase tracking-wider mb-2">Editar resposta</div>
                          <textarea
                            value={editContent}
                            onChange={(e) => setEditContent(e.target.value)}
                            className="w-full p-3 bg-bg-1 border border-border-base rounded text-sm resize-none"
                            rows={4}
                          />
                          <div className="flex gap-2 mt-3">
                            <button
                              onClick={() => editMutation.mutate({ id: item.id, content: editContent })}
                              disabled={editMutation.isPending || !editContent.trim()}
                              className="px-4 py-2 bg-accent text-white rounded text-sm hover:opacity-90 disabled:opacity-50"
                            >
                              Enviar
                            </button>
                            <button
                              onClick={() => {
                                setEditingId(null);
                                setEditContent('');
                              }}
                              className="px-4 py-2 bg-border-base text-ink-1 rounded text-sm hover:bg-border-strong"
                            >
                              Cancelar
                            </button>
                          </div>
                        </div>
                      ) : (
                        <div>
                          <div className="f-mono text-label text-ink-2 uppercase tracking-wider mb-2">Resposta suggerida</div>
                          <p className="text-sm text-ink-1 bg-bg-1 p-3 rounded">{item.suggestedResponse}</p>
                        </div>
                      )}

                      {editingId !== item.id && (
                        <div className="flex gap-2 pt-2">
                          <button
                            onClick={() => approveMutation.mutate(item.id)}
                            disabled={approveMutation.isPending}
                            className="px-4 py-2 bg-success text-white rounded text-sm hover:opacity-90 disabled:opacity-50"
                          >
                            Enviar
                          </button>
                          <button
                            onClick={() => {
                              setEditingId(item.id);
                              setEditContent(item.suggestedResponse);
                            }}
                            className="px-4 py-2 bg-accent text-white rounded text-sm hover:opacity-90"
                          >
                            Editar
                          </button>
                          <button
                            onClick={() => discardMutation.mutate(item.id)}
                            disabled={discardMutation.isPending}
                            className="px-4 py-2 bg-danger text-white rounded text-sm hover:opacity-90 disabled:opacity-50"
                          >
                            Descartar
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Conversations Tab */}
        {activeTab === 'conversations' && (
          <div>
            {conversations.length === 0 ? (
              <div className="amg-card card-clip p-12 text-center">
                <I.Bot size={32} stroke="#6366f1" className="mx-auto mb-3" />
                <div className="f-display font-bold text-sm mb-1 text-accent">Sense converses</div>
                <p className="f-mono text-label text-ink-2">Quan els clients escribin, apareixeran aquí</p>
              </div>
            ) : (
              <div className="space-y-3">
                {conversations.map((msg) => (
                  <div key={msg.id} className={`amg-card card-clip p-4 sm:p-5 ${msg.role === 'USER' ? 'bg-bg-1' : ''}`}>
                    <div className="flex items-start gap-3">
                      <div className={`flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold ${
                        msg.role === 'USER' ? 'bg-accent text-white' : 'bg-success text-white'
                      }`}>
                        {msg.role === 'USER' ? 'C' : 'A'}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1">
                          <AMGBadge tone={msg.role === 'USER' ? 'neutral' : 'success'}>
                            {msg.role === 'USER' ? 'Client' : 'Agent'}
                          </AMGBadge>
                          <span className="text-xs text-ink-3">{msg.customerIdentifier}</span>
                          <span className="text-xs text-ink-3">
                            {new Date(msg.createdAt).toLocaleString('ca-ES')}
                          </span>
                        </div>
                        <p className="text-sm text-ink-1">{msg.content}</p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Coneixement Tab */}
        {activeTab === 'coneixement' && isAdmin && (
          <div className="space-y-6">

            {/* Preview toggle */}
            <div className="flex items-center justify-between">
              <p className="text-sm text-ink-2">
                Gestiona la informació que l&apos;agent coneix del negoci. Cada línia és una entrada independent.
              </p>
              <button
                onClick={() => setShowPreview(!showPreview)}
                className="px-3 py-1.5 border border-border-base rounded text-xs f-mono text-ink-2 hover:text-accent hover:border-accent transition"
              >
                {showPreview ? 'Ocultar' : 'Previsualitzar'} prompt
              </button>
            </div>

            {showPreview && (
              <div className="amg-card card-clip p-4 bg-bg-1">
                <div className="f-mono text-label uppercase text-ink-3 tracking-widest mb-2">System prompt generat</div>
                <pre className="text-xs text-ink-2 whitespace-pre-wrap overflow-auto max-h-64">
                  {promptPreview || 'Carregant...'}
                </pre>
              </div>
            )}

            {/* Category blocks */}
            {knowledge && (
              <div className="space-y-4">
                {KNOWLEDGE_CATEGORIES.map(({ key, label, hint }) => {
                  const entries = knowledge.entriesByCategory[key] ?? [];
                  const isEditing = editingCategory === key;
                  return (
                    <div key={key} className="amg-card card-clip p-5 space-y-3">
                      <div className="flex items-center justify-between">
                        <div>
                          <div className="f-mono text-label uppercase text-ink-2 tracking-widest">{label}</div>
                          <div className="text-xs text-ink-3 mt-0.5">{hint}</div>
                        </div>
                        {!isEditing && (
                          <button
                            onClick={() => {
                              setEditingCategory(key);
                              setCategoryDraft(entries.map(e => e.content).join('\n'));
                            }}
                            className="px-3 py-1 text-xs border border-border-base rounded hover:border-accent hover:text-accent transition"
                          >
                            Editar
                          </button>
                        )}
                      </div>

                      {isEditing ? (
                        <div className="space-y-2">
                          <textarea
                            value={categoryDraft}
                            onChange={(e) => setCategoryDraft(e.target.value)}
                            className="w-full p-3 bg-bg-1 border border-border-base rounded text-sm resize-none font-mono"
                            rows={6}
                            placeholder={`Una entrada per línia...`}
                          />
                          <div className="flex gap-2">
                            <button
                              onClick={() => updateEntriesMutation.mutate({ category: key, content: categoryDraft })}
                              disabled={updateEntriesMutation.isPending}
                              className="px-4 py-2 bg-accent text-white rounded text-sm hover:opacity-90 disabled:opacity-50"
                            >
                              Desar
                            </button>
                            <button
                              onClick={() => setEditingCategory(null)}
                              className="px-4 py-2 border border-border-base rounded text-sm hover:border-accent"
                            >
                              Cancelar
                            </button>
                          </div>
                        </div>
                      ) : entries.length > 0 ? (
                        <ul className="space-y-1">
                          {entries.map((e) => (
                            <li key={e.id} className="text-sm text-ink-1 flex items-start gap-2">
                              <span className="text-accent mt-0.5">•</span>
                              <span>{e.content}</span>
                            </li>
                          ))}
                        </ul>
                      ) : (
                        <p className="text-xs text-ink-3 italic">Sense entrades</p>
                      )}
                    </div>
                  );
                })}
              </div>
            )}

            {/* Documents */}
            <div className="amg-card card-clip p-5 space-y-4">
              <div className="flex items-center justify-between">
                <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Documents</div>
                <button
                  onClick={() => setShowDocForm(!showDocForm)}
                  className="px-3 py-1 text-xs border border-border-base rounded hover:border-accent hover:text-accent transition"
                >
                  + Afegir document
                </button>
              </div>

              {showDocForm && (
                <div className="space-y-3 p-4 bg-bg-1 rounded border border-border-base">
                  <input
                    type="text"
                    value={docFilename}
                    onChange={(e) => setDocFilename(e.target.value)}
                    placeholder="Nom del fitxer (ex: menu.txt)"
                    className="w-full p-2 bg-bg-base border border-border-base rounded text-sm"
                  />
                  <textarea
                    value={docContent}
                    onChange={(e) => setDocContent(e.target.value)}
                    placeholder="Contingut del document..."
                    className="w-full p-2 bg-bg-base border border-border-base rounded text-sm resize-none font-mono"
                    rows={6}
                  />
                  <div className="flex gap-2">
                    <button
                      onClick={() => addDocMutation.mutate()}
                      disabled={addDocMutation.isPending || !docFilename.trim() || !docContent.trim()}
                      className="px-4 py-2 bg-accent text-white rounded text-sm hover:opacity-90 disabled:opacity-50"
                    >
                      Afegir
                    </button>
                    <button
                      onClick={() => setShowDocForm(false)}
                      className="px-4 py-2 border border-border-base rounded text-sm"
                    >
                      Cancelar
                    </button>
                  </div>
                </div>
              )}

              {knowledge?.documents && knowledge.documents.length > 0 ? (
                <div className="space-y-2">
                  {knowledge.documents.map((doc) => (
                    <div key={doc.id} className="flex items-center justify-between p-3 bg-bg-1 rounded">
                      <div>
                        <span className="text-sm font-medium text-ink-1">{doc.filename}</span>
                        <span className="text-xs text-ink-3 ml-3">
                          {new Date(doc.uploadedAt).toLocaleDateString('ca-ES')}
                        </span>
                      </div>
                      <button
                        onClick={() => deleteDocMutation.mutate(doc.id)}
                        disabled={deleteDocMutation.isPending}
                        className="text-xs text-danger hover:opacity-70 disabled:opacity-40"
                      >
                        Eliminar
                      </button>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-ink-3 italic">Sense documents afegits</p>
              )}
            </div>
          </div>
        )}

        {/* Model IA Tab (ADMIN / SUPER_ADMIN only) */}
        {activeTab === 'ia' && isAdmin && (
          <div className="space-y-6">

            {/* Current model */}
            <div className="amg-card card-clip p-6 space-y-4">
              <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Model actiu</div>
              {aiConfig ? (
                <div className="flex items-center gap-3">
                  <div className="flex-1">
                    <div className="font-semibold text-ink-1">{aiConfig.preferredModel}</div>
                    <div className="text-xs text-ink-2 mt-1">
                      max_tokens: {aiConfig.maxTokens} · temperatura: {aiConfig.temperature}
                    </div>
                  </div>
                  <AMGBadge tone="success">Actiu</AMGBadge>
                </div>
              ) : (
                <div className="text-sm text-ink-2">Carregant configuració...</div>
              )}
            </div>

            {/* Model selector */}
            <div className="amg-card card-clip p-6 space-y-5">
              <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Seleccionar model</div>

              {models.length === 0 ? (
                <div className="text-sm text-ink-2 py-4 text-center">
                  Carregant models disponibles...
                </div>
              ) : (
                Object.entries(modelsByProvider).map(([provider, providerModels]) => (
                  <div key={provider} className="space-y-2">
                    <div className="text-xs f-mono uppercase tracking-widest text-ink-3 pb-1 border-b border-border-base">
                      {PROVIDER_LABELS[provider] ?? provider}
                    </div>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                      {providerModels.map((m) => {
                        const isActive = aiConfig?.preferredModel === m.id;
                        return (
                          <button
                            key={m.id}
                            onClick={() => updateAIConfigMutation.mutate(m.id)}
                            disabled={updateAIConfigMutation.isPending || isActive}
                            className={`p-3 rounded border-2 text-left transition ${
                              isActive
                                ? 'border-accent bg-accent/10 cursor-default'
                                : 'border-border-base hover:border-accent'
                            } ${updateAIConfigMutation.isPending ? 'opacity-50 cursor-not-allowed' : ''}`}
                          >
                            <div className="flex items-center justify-between">
                              <div className="text-sm font-medium text-ink-1 truncate">{m.label}</div>
                              {isActive && <AMGBadge tone="success">Actiu</AMGBadge>}
                            </div>
                            <div className="text-xs text-ink-3 mt-1 f-mono">{m.id}</div>
                          </button>
                        );
                      })}
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* Test model */}
            <div className="amg-card card-clip p-6 space-y-4">
              <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Provar model</div>

              <div className="space-y-3">
                <div>
                  <label className="block text-xs f-mono text-ink-2 uppercase tracking-wider mb-1">Model a provar</label>
                  <select
                    value={selectedTestModel || aiConfig?.preferredModel || ''}
                    onChange={(e) => setSelectedTestModel(e.target.value)}
                    className="w-full p-2 bg-bg-1 border border-border-base rounded text-sm text-ink-1"
                  >
                    {models.map((m) => (
                      <option key={m.id} value={m.id}>{m.label}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs f-mono text-ink-2 uppercase tracking-wider mb-1">
                    System prompt (opcional)
                  </label>
                  <input
                    type="text"
                    value={testSystemPrompt}
                    onChange={(e) => setTestSystemPrompt(e.target.value)}
                    placeholder="Ets un assistent útil."
                    className="w-full p-2 bg-bg-1 border border-border-base rounded text-sm text-ink-1 placeholder:text-ink-3"
                  />
                </div>

                <div>
                  <label className="block text-xs f-mono text-ink-2 uppercase tracking-wider mb-1">Missatge</label>
                  <textarea
                    value={testMessage}
                    onChange={(e) => setTestMessage(e.target.value)}
                    className="w-full p-2 bg-bg-1 border border-border-base rounded text-sm text-ink-1 resize-none"
                    rows={3}
                  />
                </div>

                <button
                  onClick={() => testModelMutation.mutate()}
                  disabled={testModelMutation.isPending || !testMessage.trim()}
                  className="px-5 py-2 bg-accent text-white rounded text-sm hover:opacity-90 disabled:opacity-50 flex items-center gap-2"
                >
                  {testModelMutation.isPending ? (
                    <>
                      <I.Zap size={14} />
                      Processant...
                    </>
                  ) : (
                    <>
                      <I.Bot size={14} />
                      Enviar
                    </>
                  )}
                </button>
              </div>

              {testError && (
                <div className="p-3 bg-danger/10 border border-danger/30 rounded text-sm text-danger">
                  {testError}
                </div>
              )}

              {testResult && (
                <div className="space-y-2">
                  <div className="flex items-center gap-2">
                    <div className="text-xs f-mono text-ink-3">
                      {testResult.provider} · {testResult.model}
                    </div>
                  </div>
                  <div className="p-3 bg-bg-1 border border-border-base rounded text-sm text-ink-1 whitespace-pre-wrap">
                    {testResult.response}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Activation Instructions Modal */}
      {showActivationModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="amg-card card-clip p-6 max-w-md w-full space-y-4">
            <div className="flex items-center justify-between">
              <div className="f-display font-bold text-lg text-success">Bot Activat!</div>
              <button
                onClick={() => setShowActivationModal(false)}
                className="text-ink-2 hover:text-ink-1"
              >
                <I.X size={20} />
              </button>
            </div>
            <p className="text-sm text-ink-2">
              El bot ja està actiu i responent als clients. Instruccions per a cada canal:
            </p>

            {activationInstructions?.telegram?.configured && (
              <div className="p-4 bg-bg-1 rounded space-y-2">
                <div className="flex items-center gap-2">
                  <I.Bell size={16} />
                  <span className="text-sm font-semibold">Telegram</span>
                  {activationInstructions.telegram.active && (
                    <AMGBadge tone="success">Actiu</AMGBadge>
                  )}
                </div>
                {activationInstructions.telegram.instructions && (
                  <p className="text-xs text-ink-2">{activationInstructions.telegram.instructions}</p>
                )}
                {activationInstructions.telegram.link && (
                  <a
                    href={activationInstructions.telegram.link}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-xs text-accent hover:underline block"
                  >
                    Obrir chat de Telegram →
                  </a>
                )}
              </div>
            )}

            {activationInstructions?.whatsapp?.configured && (
              <div className="p-4 bg-bg-1 rounded space-y-2">
                <div className="flex items-center gap-2">
                  <I.Smartphone size={16} />
                  <span className="text-sm font-semibold">WhatsApp</span>
                  {activationInstructions.whatsapp.active && (
                    <AMGBadge tone="success">Actiu</AMGBadge>
                  )}
                </div>
                {activationInstructions.whatsapp.instructions && (
                  <p className="text-xs text-ink-2">{activationInstructions.whatsapp.instructions}</p>
                )}
                {activationInstructions.whatsapp.link && (
                  <p className="text-xs text-ink-3 font-mono break-all">{activationInstructions.whatsapp.link}</p>
                )}
              </div>
            )}

            {!activationInstructions && (
              <p className="text-xs text-ink-3 text-center">Carregant instruccions...</p>
            )}

            <button
              onClick={() => setShowActivationModal(false)}
              className="w-full px-4 py-2 bg-accent text-white rounded text-sm hover:opacity-90"
            >
              Entesos
            </button>
          </div>
        </div>
      )}
    </PortalShell>
  );
}
