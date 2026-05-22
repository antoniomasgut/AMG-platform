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
  type AgentStatusResponse,
  type PendingResponseDto,
  type ConversationResponse,
  type ModelInfo,
} from '@/services/agents-conversational';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGBadge } from '@/components/ui/badge';
import { I } from '@/components/ui/icons';

type Tab = 'agent' | 'pending' | 'conversations' | 'ia';

const PROVIDER_LABELS: Record<string, string> = {
  anthropic: 'Anthropic',
  deepseek: 'DeepSeek',
  ollama: 'Ollama (local)',
};

export default function AgentsPage() {
  const { user, isAdmin } = useAuth();
  const tenantId = user?.tenantId;
  const [activeTab, setActiveTab] = useState<Tab>('agent');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editContent, setEditContent] = useState<string>('');
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

            <div className="amg-card card-clip p-6 space-y-4">
              <div className="f-mono text-label uppercase text-ink-2 tracking-widest mb-4">Canals actius</div>
              <div className="space-y-3">
                <div className="flex items-center justify-between p-3 bg-bg-1 rounded">
                  <div className="flex items-center gap-3">
                    <I.Bell size={18} />
                    <span className="text-sm">Telegram (intern)</span>
                  </div>
                  <AMGBadge tone={status?.telegramLinked ? 'success' : 'neutral'}>
                    {status?.telegramLinked ? 'Vinculat' : 'No vinculat'}
                  </AMGBadge>
                </div>
                <div className="flex items-center justify-between p-3 bg-bg-1 rounded">
                  <div className="flex items-center gap-3">
                    <I.Smartphone size={18} />
                    <span className="text-sm">WhatsApp</span>
                  </div>
                  <AMGBadge tone={status?.whatsappConfigured ? 'success' : 'neutral'}>
                    {status?.whatsappConfigured ? 'Configurat' : 'Pendent'}
                  </AMGBadge>
                </div>
                <div className="flex items-center justify-between p-3 bg-bg-1 rounded">
                  <div className="flex items-center gap-3">
                    <I.Mail size={18} />
                    <span className="text-sm">Email</span>
                  </div>
                  <AMGBadge tone={status?.emailConfigured ? 'success' : 'neutral'}>
                    {status?.emailConfigured ? 'Configurat' : 'Pendent'}
                  </AMGBadge>
                </div>
              </div>
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
    </PortalShell>
  );
}
