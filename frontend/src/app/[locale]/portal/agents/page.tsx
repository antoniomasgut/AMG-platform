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
  type AgentStatusResponse,
  type PendingResponseDto,
  type ConversationResponse,
} from '@/services/agents-conversational';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGBadge } from '@/components/ui/badge';
import { I } from '@/components/ui/icons';

type Tab = 'agent' | 'pending' | 'conversations';

export default function AgentsPage() {
  const { user } = useAuth();
  const tenantId = user?.tenantId;
  const [activeTab, setActiveTab] = useState<Tab>('agent');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editContent, setEditContent] = useState<string>('');
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

  const updateModeMutation = useMutation({
    mutationFn: (mode: 'AUTO' | 'HYBRID' | 'MANUAL') => updateAgentMode(tenantId!, mode),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['agent-status', tenantId] });
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
          {(['agent', 'pending', 'conversations'] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`px-4 py-2 f-mono text-label uppercase tracking-wider border-b-2 transition ${
                activeTab === tab
                  ? 'border-accent text-accent'
                  : 'border-transparent text-ink-2 hover:text-ink-1'
              }`}
            >
              {tab === 'agent' && 'Agent'}
              {tab === 'pending' && `Pendents (${pending.length})`}
              {tab === 'conversations' && 'Converses'}
            </button>
          ))}
        </div>

        {/* Agent Tab */}
        {activeTab === 'agent' && (
          <div className="space-y-6">
            {/* Agent Mode */}
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

            {/* Channel Status */}
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
      </div>
    </PortalShell>
  );
}
