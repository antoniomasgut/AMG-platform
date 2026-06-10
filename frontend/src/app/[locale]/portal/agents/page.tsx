'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
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
  getAgencyChatConfig,
  updateAgencyChatConfig,
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
  testKnowledgeResponse,
  type KnowledgeBase,
} from '@/services/knowledge';
import { listContacts, getContactThread, clearContactMemory, testTenantEmail, portalChat, getUsageSummary, getBudgetDefaults, applyBudgetDefaults, renameContact, type ContactSummary } from '@/services/agents-conversational';
import { useRef } from 'react';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGBadge } from '@/components/ui/badge';
import { IconSet } from '@/components/ui/icons';

type Tab = 'agent' | 'pending' | 'conversations' | 'coneixement' | 'ia' | 'xat' | 'widget';
type ChatMsg = { role: 'user' | 'agent'; text: string };

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

const AGENCY_CHAT_TEMPLATES = [
  {
    label: 'Agència Digital (per defecte)',
    systemPrompt: "Ets l'assistent virtual d'AMG Digitalitzacions, una agència digital de Mallorca especialitzada en digitalització de negocis locals. Ajudes els visitants a entendre els serveis (landings, WhatsApp Business, agents IA, automatitzacions) i a demanar informació o pressupost. Respon en l'idioma del visitant (català, castellà, anglès o alemany). Sigues amable, concís i professional.",
  },
  {
    label: 'Consultoria (formal)',
    systemPrompt: "Ets l'assistent virtual d'AMG Digitalitzacions. Atens consultes professionals sobre digitalització empresarial. Recull les necessitats del visitant i ofereix una primera orientació, animant-los a concertar una reunió. Tracta de vostè. Respon en l'idioma del visitant.",
  },
  {
    label: 'Captació de leads',
    systemPrompt: "Ets l'assistent virtual d'AMG Digitalitzacions. El teu objectiu és entendre el negoci del visitant i recomanar-li el paquet de digitalització adequat (Fase 1: agent IA, Fase 2: agenda, Fase 3: pressupostos). Sempre acaba convidant-los a deixar el seu contacte o a demanar una demo. Respon en l'idioma del visitant.",
  },
  {
    label: 'Suport tècnic',
    systemPrompt: "Ets l'assistent de suport tècnic d'AMG Digitalitzacions. Ajudes als clients existents a resoldre dubtes sobre la plataforma, configuració d'agents, canals de comunicació i facturació. Si no pots resoldre-ho, indica que un tècnic contactarà en breu. Respon en l'idioma del visitant.",
  },
];

export default function AgentsPage() {
  const { user, isAdmin, isSuperAdmin } = useAuth();
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
  const [emailDraft, setEmailDraft] = useState('');
  const [senderEmailDraft, setSenderEmailDraft] = useState('');
  const [emailTestResult, setEmailTestResult] = useState<{ ok: boolean; message: string } | null>(null);
  const [emailTesting, setEmailTesting] = useState(false);
  const [senderNameDraft, setSenderNameDraft] = useState('');
  const [replyToDraft, setReplyToDraft] = useState('');
  const [testMessage, setTestMessage] = useState('Hola! Pots presentar-te breument?');
  const [testSystemPrompt, setTestSystemPrompt] = useState('');
  const [testResult, setTestResult] = useState<{ model: string; provider: string; response: string } | null>(null);
  const [testError, setTestError] = useState<string | null>(null);
  const [selectedTestModel, setSelectedTestModel] = useState<string>('');
  const [kbTestMessage, setKbTestMessage] = useState('');
  const [kbTestResult, setKbTestResult] = useState<string | null>(null);
  const [kbTestError, setKbTestError] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const [chatMessages, setChatMessages] = useState<ChatMsg[]>([]);
  const [chatInput, setChatInput] = useState('');
  const [chatLoading, setChatLoading] = useState(false);
  const chatSessionId = useRef(crypto.randomUUID());
  const chatEndRef = useRef<HTMLDivElement>(null);

  const [selectedContactId, setSelectedContactId] = useState<string | null>(null);
  const [renamingContactId, setRenamingContactId] = useState<string | null>(null);
  const [renameDraft, setRenameDraft] = useState('');
  const [convFilter, setConvFilter] = useState<'all' | 'clients' | 'intern'>('all');

  const [widgetBusinessName, setWidgetBusinessName] = useState('');
  const [widgetSystemPrompt, setWidgetSystemPrompt] = useState('');
  const [widgetModel, setWidgetModel] = useState('');
  const [widgetSaving, setWidgetSaving] = useState(false);
  const [widgetSaved, setWidgetSaved] = useState(false);

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

  const { data: usageSummary } = useQuery({
    queryKey: ['usage-summary', tenantId],
    queryFn: () => getUsageSummary(tenantId!),
    enabled: !!user && !!tenantId && isAdmin && activeTab === 'ia',
  });

  const [budgetDraft, setBudgetDraft] = useState<string>('');
  const updateBudgetMutation = useMutation({
    mutationFn: (cents: number) => updateAIConfig(tenantId!, { monthlyCostBudgetEurCents: cents }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ai-config', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['usage-summary', tenantId] });
      setBudgetDraft('');
    },
  });

  const { data: budgetDefaults } = useQuery({
    queryKey: ['budget-defaults', tenantId],
    queryFn: () => getBudgetDefaults(tenantId!),
    enabled: !!user && !!tenantId && isAdmin && activeTab === 'ia',
  });

  const [showBreakdown, setShowBreakdown] = useState(false);
  const applyDefaultsMutation = useMutation({
    mutationFn: () => applyBudgetDefaults(tenantId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ai-config', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['usage-summary', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['budget-defaults', tenantId] });
    },
  });

  const { data: widgetConfig } = useQuery({
    queryKey: ['agency-chat-config', tenantId],
    queryFn: () => getAgencyChatConfig(tenantId!),
    enabled: !!user && !!tenantId && isSuperAdmin && activeTab === 'widget',
  });

  // Populate widget form when data loads
  const [widgetConfigLoaded, setWidgetConfigLoaded] = useState(false);
  if (widgetConfig && !widgetConfigLoaded) {
    setWidgetBusinessName(widgetConfig.businessName ?? '');
    setWidgetSystemPrompt(widgetConfig.systemPrompt ?? '');
    setWidgetModel(widgetConfig.preferredModel ?? '');
    setWidgetConfigLoaded(true);
  }

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
    mutationFn: (data: { widgetEnabled?: boolean; whatsappEnabled?: boolean; emailEnabled?: boolean; whatsappPhoneNumber?: string; whatsappMetaPhoneNumberId?: string; emailAddress?: string }) =>
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

  const updateSenderMutation = useMutation({
    mutationFn: (data: { senderEmail?: string; senderName?: string; replyToEmail?: string }) =>
      updateAIConfig(tenantId!, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ai-config', tenantId] });
      setSenderEmailDraft('');
      setSenderNameDraft('');
    },
  });

  const updateLanguageMutation = useMutation({
    mutationFn: (lang: string) => updateAIConfig(tenantId!, { responseLanguage: lang }),
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

  const { data: contacts = [], isLoading: loadingContacts } = useQuery({
    queryKey: ['contacts', tenantId],
    queryFn: () => listContacts(tenantId!),
    enabled: !!tenantId && (activeTab === 'conversations' || (activeTab === 'coneixement' && isAdmin)),
  });

  const { data: contactThread = [], isLoading: loadingThread } = useQuery({
    queryKey: ['contact-thread', tenantId, selectedContactId],
    queryFn: () => getContactThread(tenantId!, selectedContactId!),
    enabled: !!tenantId && !!selectedContactId,
  });

  const renameContactMutation = useMutation({
    mutationFn: ({ contactId, name }: { contactId: string; name: string }) =>
      renameContact(tenantId!, contactId, name),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['contacts', tenantId] });
      setRenamingContactId(null);
      setRenameDraft('');
    },
  });

  const kbTestMutation = useMutation({
    mutationFn: () => testKnowledgeResponse(tenantId!, kbTestMessage),
    onSuccess: (data) => { setKbTestResult(data.response); setKbTestError(null); },
    onError: (err: Error) => { setKbTestError(err.message); setKbTestResult(null); },
  });

  const clearMemoryMutation = useMutation({
    mutationFn: (contactId: string) => clearContactMemory(tenantId!, contactId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['contacts', tenantId] }),
  });

  const { data: promptPreview } = useQuery({
    queryKey: ['knowledge-preview', tenantId],
    queryFn: () => previewPromptBlock(tenantId!),
    enabled: !!tenantId && showPreview,
  });

  useEffect(() => {
    if (activeTab === 'conversations' && tenantId) {
      queryClient.invalidateQueries({ queryKey: ['contacts', tenantId] });
      setSelectedContactId(null);
    }
  }, [activeTab, tenantId, queryClient]);

  useEffect(() => {
    if (!tenantId) return;
    const key = `portal-xat-session-${tenantId}`;
    let id = localStorage.getItem(key);
    if (!id) {
      id = crypto.randomUUID();
      localStorage.setItem(key, id);
    }
    chatSessionId.current = id;
  }, [tenantId]);

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
    { key: 'xat', label: '💬 Xat' },
    { key: 'pending', label: `Pendents (${pending.length})` },
    { key: 'conversations', label: 'Converses' },
    ...(isAdmin ? [{ key: 'coneixement' as Tab, label: 'Coneixement' }] : []),
    ...(isAdmin ? [{ key: 'ia' as Tab, label: 'Model IA' }] : []),
    ...(isSuperAdmin ? [{ key: 'widget' as Tab, label: '🌐 Widget web' }] : []),
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
              ) : (channels?.telegramLinked || channels?.widgetEnabled || channels?.whatsappEnabled || channels?.emailEnabled) ? (
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

              {/* Widget (Xat web) */}
              <div className="p-4 bg-bg-1 rounded space-y-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <IconSet.Globe size={18} />
                    <span className="text-sm font-medium">Xat web (widget)</span>
                  </div>
                  <label className="flex items-center gap-2 cursor-pointer select-none">
                    <span className="text-xs text-ink-3">{channels?.widgetEnabled ? 'Habilitat' : 'Deshabilitat'}</span>
                    <button
                      role="switch"
                      aria-checked={!!channels?.widgetEnabled}
                      onClick={() => updateChannelsMutation.mutate({ widgetEnabled: !channels?.widgetEnabled })}
                      disabled={updateChannelsMutation.isPending}
                      className={`relative w-10 h-5 rounded-full transition-colors ${channels?.widgetEnabled ? 'bg-success' : 'bg-border-base'} disabled:opacity-50`}
                    >
                      <span className={`absolute top-0.5 left-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform ${channels?.widgetEnabled ? 'translate-x-5' : 'translate-x-0'}`} />
                    </button>
                  </label>
                </div>
                <p className="text-xs text-ink-3 pl-7">
                  Mostra el botó de xat a la landing page del negoci. Requereix que l&apos;agent estigui actiu.
                </p>
              </div>

              {/* Telegram */}
              <div className="p-4 bg-bg-1 rounded space-y-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <IconSet.Bell size={18} />
                    <span className="text-sm font-medium">Telegram</span>
                  </div>
                  <AMGBadge tone={channels?.telegramLinked ? 'success' : 'warning'}>
                    {channels?.telegramLinked ? 'Vinculat' : 'Pendent configurar'}
                  </AMGBadge>
                </div>
                {channels?.telegramLinked && channels.telegramChatId && (
                  <p className="text-xs text-ink-3 pl-7">Chat ID: {channels.telegramChatId}</p>
                )}
                {!channels?.telegramLinked && (
                  <div className="pl-7 space-y-2">
                    <p className="text-xs text-ink-2">
                      Per vincular Telegram, el client ha d&apos;escriure al bot de l&apos;empresa:
                    </p>
                    {channels?.telegramBotLink && (
                      <a
                        href={channels.telegramBotLink}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1 text-xs text-accent-light hover:underline f-mono border border-accent/30 px-2 py-1 rounded"
                      >
                        <IconSet.Bell size={12} />
                        Obrir bot de Telegram →
                      </a>
                    )}
                    <p className="text-xs text-ink-3">
                      Un cop el client escrigui al bot, quedarà vinculat automàticament.
                    </p>
                  </div>
                )}
              </div>

              {/* WhatsApp */}
              <div className="p-4 bg-bg-1 rounded space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <IconSet.Smartphone size={18} />
                    <span className="text-sm font-medium">WhatsApp</span>
                    <AMGBadge tone={channels?.whatsappPhoneNumber ? 'success' : 'neutral'}>
                      {channels?.whatsappPhoneNumber ? 'Configurat' : 'Pendent'}
                    </AMGBadge>
                  </div>
                  <label className="flex items-center gap-2 cursor-pointer select-none">
                    <span className="text-xs text-ink-3">{channels?.whatsappEnabled ? 'Habilitat' : 'Deshabilitat'}</span>
                    <button
                      role="switch"
                      aria-checked={!!channels?.whatsappEnabled}
                      onClick={() => updateChannelsMutation.mutate({ whatsappEnabled: !channels?.whatsappEnabled })}
                      disabled={updateChannelsMutation.isPending || !channels?.whatsappPhoneNumber}
                      className={`relative w-10 h-5 rounded-full transition-colors ${channels?.whatsappEnabled ? 'bg-success' : 'bg-border-base'} disabled:opacity-40`}
                    >
                      <span className={`absolute top-0.5 left-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform ${channels?.whatsappEnabled ? 'translate-x-5' : 'translate-x-0'}`} />
                    </button>
                  </label>
                </div>
                {channels?.whatsappPhoneNumber && (
                  <p className="text-xs text-ink-3 pl-7">Telèfon: {channels.whatsappPhoneNumber}</p>
                )}

                {/* Guia WhatsApp */}
                <div className="border border-border-base rounded p-3 space-y-3 bg-bg-base text-xs">
                  <p className="font-semibold text-ink-1">Com funciona WhatsApp per al bot — 3 casos</p>

                  <div className="space-y-3">
                    {/* Cas 1 */}
                    <div className="rounded border border-border-base p-2.5 space-y-1.5">
                      <div className="flex items-center gap-2">
                        <span>📱</span>
                        <p className="font-semibold text-ink-1">Cas 1 — El negoci ja té WhatsApp (normal o Business app)</p>
                      </div>
                      <p className="text-ink-3">Ex: el negoci usa WhatsApp Business app al mòbil per respondre manualment</p>
                      <div className="space-y-1 text-ink-2">
                        <p className="font-medium text-ink-1">Per què NO usar el número actual:</p>
                        <p>Connectar el número actual a l&apos;API <strong>desconnecta l&apos;app per sempre</strong> en aquell número — perdràs l&apos;historial de xats, els grups i l&apos;accés des del mòbil. A més, si tens el número a targetes, flyers, Google Business o Instagram, hauràs de canviar-ho tot.</p>
                        <p className="font-medium text-ink-1 pt-1">La solució: número nou dedicat al bot</p>
                        <p>Pensa-ho com el telèfon de l&apos;oficina: el teu mòbil personal segueix sent el teu, i el bot atén des d&apos;un número dedicat. Els nous clients contacten al bot; tu continues amb els clients antics des del teu número habitual.</p>
                        <p className="font-medium text-ink-1 pt-1">Com fer-ho:</p>
                        <p>① Aconsegueix un número nou (SIM de prepagament ~5€, o número virtual ~1€/mes).</p>
                        <p>② Posa el nou número al Google Business, web i xarxes socials com a &quot;Xat amb el bot&quot;.</p>
                        <p>③ Contacta&apos;ns i t&apos;ajudem a connectar-lo. El número antic continua funcionant al mòbil.</p>
                      </div>
                    </div>

                    {/* Cas 2 */}
                    <div className="rounded border border-border-base p-2.5 space-y-1.5">
                      <div className="flex items-center gap-2">
                        <span>🏢</span>
                        <p className="font-semibold text-ink-1">Cas 2 — El negoci ja té WhatsApp Business API</p>
                      </div>
                      <p className="text-ink-3">Ex: ja usa una plataforma de missatgeria professional (Twilio, 360dialog, etc.)</p>
                      <div className="space-y-1 text-ink-2">
                        <p>① Omple els camps de sota amb el <em>Telèfon</em> i el <em>Phone Number ID</em> de Meta.</p>
                        <p>② Contacta&apos;ns per configurar el webhook a la teva compte de Meta Business.</p>
                        <p>③ El bot ja pot enviar i rebre missatges pel teu número existent.</p>
                      </div>
                    </div>

                    {/* Cas 3 */}
                    <div className="rounded border border-border-base p-2.5 space-y-1.5">
                      <div className="flex items-center gap-2">
                        <span>✅</span>
                        <p className="font-semibold text-ink-1">Cas 3 — Sense WhatsApp Business (partir de zero)</p>
                      </div>
                      <p className="text-ink-3">El negoci no té WhatsApp Business API i vol activar-lo</p>
                      <div className="space-y-1 text-ink-2">
                        <p>① Aconsegueix un número nou dedicat al bot:</p>
                        <div className="ml-3 space-y-0.5 text-ink-3">
                          <p>· SIM de prepagament (Simyo, Lebara) — ~5€ + ~5€/mes</p>
                          <p>· Número virtual Twilio — ~1€/mes, sense SIM física</p>
                        </div>
                        <p>② Contacta&apos;ns. Creem el compte Meta Business, verifiquem el número i connectem el bot.</p>
                        <p>③ Els clients veuran el bot com un número de WhatsApp del negoci.</p>
                      </div>
                    </div>

                    <p className="text-ink-3 pt-1">⚠️ El client no necessita WhatsApp Business — qualsevol client amb WhatsApp normal pot escriure al bot.</p>
                  </div>
                </div>

                {!channels?.isActive && !channels?.whatsappPhoneNumber && (
                  <div className="pl-7 space-y-2">
                    <p className="text-xs text-ink-2 font-medium">Un cop tinguis el número llest (cas 2 o 3):</p>
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
              <div className="p-4 bg-bg-1 rounded space-y-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <IconSet.Mail size={18} />
                    <span className="text-sm font-medium">Email</span>
                    <AMGBadge tone={channels?.emailAddress ? 'success' : 'warning'}>
                      {channels?.emailAddress ? 'Configurat' : 'Pendent configurar'}
                    </AMGBadge>
                  </div>
                  <label className="flex items-center gap-2 cursor-pointer select-none">
                    <span className="text-xs text-ink-3">{channels?.emailEnabled ? 'Habilitat' : 'Deshabilitat'}</span>
                    <button
                      role="switch"
                      aria-checked={!!channels?.emailEnabled}
                      onClick={() => updateChannelsMutation.mutate({ emailEnabled: !channels?.emailEnabled })}
                      disabled={updateChannelsMutation.isPending || !channels?.emailAddress}
                      className={`relative w-10 h-5 rounded-full transition-colors ${channels?.emailEnabled ? 'bg-success' : 'bg-border-base'} disabled:opacity-40`}
                    >
                      <span className={`absolute top-0.5 left-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform ${channels?.emailEnabled ? 'translate-x-5' : 'translate-x-0'}`} />
                    </button>
                  </label>
                </div>
                {channels?.emailAddress && (
                  <p className="text-xs text-ink-3 pl-7">{channels.emailAddress}</p>
                )}
                <div className="pl-7 space-y-3">
                  <div className="space-y-2">
                    <p className="text-xs text-ink-2">
                      {channels?.emailAddress
                        ? 'Adreça de correu que l\'agent monitoritza (entrada):'
                        : 'Configura l\'adreça de correu que l\'agent ha de monitoritzar:'}
                    </p>
                    <div className="flex gap-2">
                      <input
                        type="email"
                        value={emailDraft}
                        onChange={(e) => setEmailDraft(e.target.value)}
                        placeholder={channels?.emailAddress ?? 'agent@empresa.com'}
                        className="flex-1 p-2 bg-bg-base border border-border-base rounded text-xs f-mono focus:outline-none focus:border-accent"
                      />
                      <button
                        onClick={() => updateChannelsMutation.mutate({ emailAddress: emailDraft })}
                        disabled={updateChannelsMutation.isPending || !emailDraft.trim()}
                        className="px-3 py-1.5 bg-accent text-white rounded text-xs hover:opacity-90 disabled:opacity-50 shrink-0"
                      >
                        Desar
                      </button>
                    </div>
                  </div>
                  {/* Guia de configuració de correu */}
                  <div className="border border-border-base rounded p-3 space-y-3 bg-bg-base">
                    <p className="text-xs font-semibold text-ink-1">Com configurar l&apos;email del bot (enviar + rebre)</p>
                    <div className="space-y-3 text-xs">

                      {/* Rebre — via inbound.amgdl.com */}
                      <div className="rounded border border-border-base p-2.5 space-y-1.5">
                        <div className="flex items-center gap-2">
                          <span>📥</span>
                          <p className="font-semibold text-ink-1">Per rebre — adreça inbound</p>
                        </div>
                        <div className="space-y-1 text-ink-2">
                          <p>① Al camp d&apos;adreça de dalt, escriu qualsevol adreça que acabi en <span className="f-mono text-accent-light">@inbound.amgdl.com</span>.</p>
                          <p className="pl-3 text-ink-3">Ex: <span className="f-mono">plomeria@inbound.amgdl.com</span> · <span className="f-mono">clinica-marti@inbound.amgdl.com</span></p>
                          <p>② Desa. El bot rebrà i respondrà automàticament els emails enviats a aquesta adreça.</p>
                          <p className="text-ink-3 italic">El domini inbound.amgdl.com és gestionat per AMG via Cloudflare — no cal cap configuració extra per al client.</p>
                        </div>
                      </div>

                      {/* Enviar — Brevo */}
                      <div className="rounded border border-border-base p-2.5 space-y-1.5">
                        <div className="flex items-center gap-2">
                          <span>📤</span>
                          <p className="font-semibold text-ink-1">Per enviar — remitent personalitzat (opcional)</p>
                        </div>
                        <div className="space-y-1 text-ink-2">
                          <p className="font-medium text-ink-1">Opció A — Sense configuració (per defecte)</p>
                          <p className="pl-3">Les respostes surten de <span className="f-mono">noreply@amgdl.com</span>. El camp <em>Respostes a</em> pot apuntar al correu del negoci.</p>
                          <p className="font-medium text-ink-1 pt-1">Opció B — Domini propi verificat a Brevo</p>
                          <p className="pl-3">① Verifica el domini a Brevo (SPF + DKIM). Llavors omple el camp <em>Remitent</em> amb <span className="f-mono">nom@tudomini.com</span>.</p>
                          <p className="pl-3">② Les respostes dels clients arriben directament al teu correu (camp <em>Respostes a</em>).</p>
                        </div>
                      </div>

                    </div>
                  </div>

                  {/* Remitent (From) — requereix verificació Brevo */}
                  <div className="space-y-2 border-t border-border-base pt-2">
                    <p className="text-xs font-medium text-ink-1">
                      Remitent (opcional — requereix verificació Brevo)
                      {aiConfig?.senderEmail && (
                        <span className="ml-2 text-success">{aiConfig.senderEmail}</span>
                      )}
                    </p>
                    <div className="flex gap-2">
                      <input
                        type="text"
                        value={senderNameDraft}
                        onChange={(e) => setSenderNameDraft(e.target.value)}
                        placeholder={aiConfig?.senderName ?? 'Nom del negoci'}
                        className="w-36 p-2 bg-bg-base border border-border-base rounded text-xs focus:outline-none focus:border-accent"
                      />
                      <input
                        type="email"
                        value={senderEmailDraft}
                        onChange={(e) => setSenderEmailDraft(e.target.value)}
                        placeholder={aiConfig?.senderEmail ?? 'info@negoci.com'}
                        className="flex-1 p-2 bg-bg-base border border-border-base rounded text-xs f-mono focus:outline-none focus:border-accent"
                      />
                      <button
                        onClick={() => updateSenderMutation.mutate({
                          senderEmail: senderEmailDraft || undefined,
                          senderName: senderNameDraft || undefined,
                        })}
                        disabled={updateSenderMutation.isPending || (!senderEmailDraft.trim() && !senderNameDraft.trim())}
                        className="px-3 py-1.5 bg-accent text-white rounded text-xs hover:opacity-90 disabled:opacity-50 shrink-0"
                      >
                        Desar
                      </button>
                    </div>
                  </div>

                  {/* Reply-To — funciona amb qualsevol correu */}
                  <div className="space-y-2">
                    <p className="text-xs font-medium text-ink-1">
                      Respostes a (recomanat — funciona amb qualsevol correu)
                      {aiConfig?.replyToEmail && (
                        <span className="ml-2 text-success">{aiConfig.replyToEmail}</span>
                      )}
                    </p>
                    <div className="flex gap-2">
                      <input
                        type="email"
                        value={replyToDraft}
                        onChange={(e) => setReplyToDraft(e.target.value)}
                        placeholder={aiConfig?.replyToEmail ?? 'negoci@gmail.com'}
                        className="flex-1 p-2 bg-bg-base border border-border-base rounded text-xs f-mono focus:outline-none focus:border-accent"
                      />
                      <button
                        onClick={() => updateSenderMutation.mutate({ replyToEmail: replyToDraft })}
                        disabled={updateSenderMutation.isPending || !replyToDraft.trim()}
                        className="px-3 py-1.5 bg-accent text-white rounded text-xs hover:opacity-90 disabled:opacity-50 shrink-0"
                      >
                        Desar
                      </button>
                    </div>
                    <p className="text-xs text-ink-3">Quan el client faci &quot;Respondre&quot; al correu del bot, el missatge arribarà a aquesta adreça.</p>
                  </div>

                  {/* Test email */}
                  <div className="border-t border-border-base pt-3">
                    <div className="flex items-center gap-3 flex-wrap">
                      <button
                        onClick={async () => {
                          setEmailTesting(true);
                          setEmailTestResult(null);
                          try {
                            const res = await testTenantEmail(tenantId!);
                            setEmailTestResult(res);
                          } catch {
                            setEmailTestResult({ ok: false, message: 'Error connectant amb el servidor.' });
                          } finally {
                            setEmailTesting(false);
                          }
                        }}
                        disabled={emailTesting}
                        className="px-3 py-1.5 border border-border-base rounded text-xs hover:border-accent hover:text-accent transition disabled:opacity-50"
                      >
                        {emailTesting ? 'Enviant...' : '✉ Provar configuració d\'email'}
                      </button>
                      <span className="text-xs text-ink-3">T'enviarem un email de prova al teu correu per verificar que funciona.</span>
                    </div>
                    {emailTestResult && (
                      <div className={`mt-2 p-2 rounded text-xs f-mono ${emailTestResult.ok ? 'bg-success/10 text-success border border-success/30' : 'bg-danger/10 text-danger border border-danger/30'}`}>
                        {emailTestResult.ok ? '✓ ' : '✗ '}{emailTestResult.message}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>

            {/* Activate / Deactivate */}
            <div className="flex items-center gap-4">
              {!channels?.isActive ? (
                <button
                  onClick={() => activateMutation.mutate()}
                  disabled={
                    activateMutation.isPending ||
                    (!channels?.telegramLinked && !channels?.widgetEnabled && !channels?.whatsappEnabled && !channels?.emailEnabled)
                  }
                  className="px-6 py-3 bg-success text-white rounded font-semibold hover:opacity-90 disabled:opacity-40 flex items-center gap-2"
                >
                  <IconSet.Bot size={16} />
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
              {!channels?.isActive && !channels?.telegramLinked && !channels?.widgetEnabled && !channels?.whatsappEnabled && !channels?.emailEnabled && (
                <p className="text-xs text-ink-3">
                  Habilita almenys un canal per activar el bot.
                </p>
              )}
            </div>
          </div>
        )}

        {/* Xat Tab */}
        {activeTab === 'xat' && (
          <div className="flex flex-col h-[calc(100vh-260px)] min-h-[400px]">
            <div className="flex-1 overflow-y-auto space-y-3 pr-1 pb-4" ref={(el) => { if (el) el.scrollTop = el.scrollHeight; }}>
              {chatMessages.length === 0 && (
                <div className="flex flex-col items-center justify-center h-full gap-3 text-center py-12">
                  <IconSet.Bot size={36} className="text-ink-3" />
                  <p className="text-ink-3 text-sm">Escriu un missatge per parlar amb l&apos;agent</p>
                  <p className="text-ink-3 text-xs f-mono">El xat usa la mateixa IA i coneixement configurat</p>
                </div>
              )}
              {chatMessages.map((msg, i) => (
                <div key={i} className={`flex gap-3 ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                  {msg.role === 'agent' && (
                    <div className="w-7 h-7 rounded-full bg-accent flex items-center justify-center shrink-0 mt-0.5">
                      <IconSet.Bot size={14} className="text-white" />
                    </div>
                  )}
                  <div className={`max-w-[75%] px-4 py-2.5 rounded-2xl text-sm whitespace-pre-wrap ${
                    msg.role === 'user'
                      ? 'bg-accent text-white rounded-br-sm'
                      : 'bg-bg-1 border border-border-base text-ink-1 rounded-bl-sm'
                  }`}>
                    {msg.text}
                  </div>
                  {msg.role === 'user' && (
                    <div className="w-7 h-7 rounded-full bg-bg-1 border border-border-base flex items-center justify-center shrink-0 mt-0.5">
                      <IconSet.Users size={14} className="text-ink-2" />
                    </div>
                  )}
                </div>
              ))}
              {chatLoading && (
                <div className="flex gap-3 justify-start">
                  <div className="w-7 h-7 rounded-full bg-accent flex items-center justify-center shrink-0">
                    <IconSet.Bot size={14} className="text-white" />
                  </div>
                  <div className="px-4 py-3 bg-bg-1 border border-border-base rounded-2xl rounded-bl-sm">
                    <div className="flex gap-1 items-center">
                      <span className="w-1.5 h-1.5 bg-ink-3 rounded-full animate-bounce [animation-delay:0ms]" />
                      <span className="w-1.5 h-1.5 bg-ink-3 rounded-full animate-bounce [animation-delay:150ms]" />
                      <span className="w-1.5 h-1.5 bg-ink-3 rounded-full animate-bounce [animation-delay:300ms]" />
                    </div>
                  </div>
                </div>
              )}
              <div ref={chatEndRef} />
            </div>

            <div className="border-t border-border-base pt-3 flex gap-2">
              <input
                type="text"
                value={chatInput}
                onChange={(e) => setChatInput(e.target.value)}
                onKeyDown={async (e) => {
                  if (e.key === 'Enter' && !e.shiftKey && chatInput.trim() && !chatLoading) {
                    e.preventDefault();
                    const msg = chatInput.trim();
                    setChatInput('');
                    setChatMessages(prev => [...prev, { role: 'user', text: msg }]);
                    setChatLoading(true);
                    try {
                      const res = await portalChat(tenantId!, msg, chatSessionId.current);
                      setChatMessages(prev => [...prev, { role: 'agent', text: res.reply }]);
                      queryClient.invalidateQueries({ queryKey: ['conversations', tenantId] });
                    } catch {
                      setChatMessages(prev => [...prev, { role: 'agent', text: 'Error connectant amb l\'agent.' }]);
                    } finally {
                      setChatLoading(false);
                      setTimeout(() => chatEndRef.current?.scrollIntoView({ behavior: 'smooth' }), 50);
                    }
                  }
                }}
                placeholder="Escriu un missatge… (Enter per enviar)"
                className="flex-1 px-4 py-2.5 bg-bg-1 border border-border-base rounded-xl text-sm focus:outline-none focus:border-accent"
                disabled={chatLoading}
              />
              <button
                onClick={async () => {
                  const msg = chatInput.trim();
                  if (!msg || chatLoading) return;
                  setChatInput('');
                  setChatMessages(prev => [...prev, { role: 'user', text: msg }]);
                  setChatLoading(true);
                  try {
                    const res = await portalChat(tenantId!, msg, chatSessionId.current);
                    setChatMessages(prev => [...prev, { role: 'agent', text: res.reply }]);
                    queryClient.invalidateQueries({ queryKey: ['conversations', tenantId] });
                  } catch {
                    setChatMessages(prev => [...prev, { role: 'agent', text: 'Error connectant amb l\'agent.' }]);
                  } finally {
                    setChatLoading(false);
                    setTimeout(() => chatEndRef.current?.scrollIntoView({ behavior: 'smooth' }), 50);
                  }
                }}
                disabled={chatLoading || !chatInput.trim()}
                className="px-4 py-2.5 bg-accent text-white rounded-xl hover:opacity-90 disabled:opacity-40 shrink-0"
              >
                <IconSet.Zap size={16} />
              </button>
              <button
                onClick={() => {
                  const newId = crypto.randomUUID();
                  chatSessionId.current = newId;
                  if (tenantId) localStorage.setItem(`portal-xat-session-${tenantId}`, newId);
                  setChatMessages([]);
                }}
                className="px-3 py-2.5 border border-border-base rounded-xl text-ink-3 hover:text-ink-1 hover:border-accent transition shrink-0"
                title="Nova conversa"
              >
                <IconSet.Refresh size={14} />
              </button>
            </div>
          </div>
        )}

        {/* Pending Tab */}
        {activeTab === 'pending' && (
          <div>
            {pending.length === 0 ? (
              <div className="amg-card card-clip p-12 text-center">
                <IconSet.Check size={32} stroke="#39d353" className="mx-auto mb-3" />
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

        {/* Conversations Tab — contact-based */}
        {activeTab === 'conversations' && (() => {
          const CHANNEL_ICON: Record<string, string> = {
            WIDGET: '💬', WHATSAPP: '📱', WHATSAPP_META: '📱', TELEGRAM: '✈', EMAIL: '✉',
          };
          const CHANNEL_LABEL: Record<string, string> = {
            WIDGET: 'Widget', WHATSAPP: 'WhatsApp', WHATSAPP_META: 'WhatsApp',
            TELEGRAM: 'Telegram', EMAIL: 'Email',
          };
          const CLIENT_CHANNELS = ['WHATSAPP', 'WHATSAPP_META', 'EMAIL', 'WIDGET'];

          const selectedContact = contacts.find(c => c.contactId === selectedContactId);

          const visibleContacts = contacts.filter(c => {
            if (!c.lastChannel) return true;
            if (convFilter === 'clients') return CLIENT_CHANNELS.includes(c.lastChannel);
            if (convFilter === 'intern') return !CLIENT_CHANNELS.includes(c.lastChannel);
            return true;
          });

          return (
            <div className="space-y-3">
              {/* Filter pills — only shown on contact list */}
              {!selectedContactId && contacts.some(c => !CLIENT_CHANNELS.includes(c.lastChannel ?? '')) && (
                <div className="flex gap-2">
                  {(['all', 'clients', 'intern'] as const).map(f => (
                    <button
                      key={f}
                      onClick={() => setConvFilter(f)}
                      className={`px-3 py-1 rounded-full f-mono text-xs transition ${
                        convFilter === f
                          ? 'bg-accent text-white'
                          : 'border border-border-base text-ink-2 hover:border-accent hover:text-accent'
                      }`}
                    >
                      {f === 'all' ? 'Tots' : f === 'clients' ? '👤 Clients' : '🏢 Intern'}
                    </button>
                  ))}
                </div>
              )}

              {selectedContactId ? (
                /* Thread detail */
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <button
                      onClick={() => setSelectedContactId(null)}
                      className="flex items-center gap-2 text-sm text-ink-2 hover:text-ink-1 transition"
                    >
                      ← Tots els contactes
                    </button>
                    {selectedContact && (
                      <div className="flex items-center gap-3">
                        {renamingContactId === selectedContactId ? (
                          <form
                            className="flex gap-2"
                            onSubmit={e => {
                              e.preventDefault();
                              if (renameDraft.trim())
                                renameContactMutation.mutate({ contactId: selectedContactId, name: renameDraft.trim() });
                            }}
                          >
                            <input
                              autoFocus
                              value={renameDraft}
                              onChange={e => setRenameDraft(e.target.value)}
                              className="px-2 py-1 text-xs bg-bg-1 border border-border-base rounded focus:outline-none focus:border-accent"
                              placeholder={selectedContact.displayName}
                            />
                            <button type="submit" className="text-xs text-accent hover:opacity-80" disabled={renameContactMutation.isPending}>
                              Desar
                            </button>
                            <button type="button" onClick={() => setRenamingContactId(null)} className="text-xs text-ink-3">
                              ×
                            </button>
                          </form>
                        ) : (
                          <button
                            onClick={() => { setRenamingContactId(selectedContactId); setRenameDraft(selectedContact.displayName); }}
                            className="text-xs text-ink-3 hover:text-accent transition"
                          >
                            Canviar nom
                          </button>
                        )}
                      </div>
                    )}
                  </div>

                  {selectedContact && (
                    <div className="amg-card card-clip p-4 flex flex-wrap gap-4 text-sm">
                      <div>
                        <div className="text-xs text-ink-3 mb-0.5">Nom</div>
                        <div className="font-medium text-ink-1">{selectedContact.displayName}</div>
                      </div>
                      {selectedContact.phone && (
                        <div>
                          <div className="text-xs text-ink-3 mb-0.5">Telèfon</div>
                          <div className="f-mono text-ink-1">{selectedContact.phone}</div>
                        </div>
                      )}
                      {selectedContact.email && (
                        <div>
                          <div className="text-xs text-ink-3 mb-0.5">Email</div>
                          <div className="f-mono text-ink-1">{selectedContact.email}</div>
                        </div>
                      )}
                      {selectedContact.channels.length > 0 && (
                        <div>
                          <div className="text-xs text-ink-3 mb-0.5">Canals</div>
                          <div className="flex gap-1 flex-wrap">
                            {selectedContact.channels.map((ch, i) => (
                              <span key={i} className="text-xs f-mono border border-border-base px-1.5 py-0.5 rounded text-ink-2">
                                {CHANNEL_ICON[ch.channel] ?? ''} {CHANNEL_LABEL[ch.channel] ?? ch.channel}
                              </span>
                            ))}
                          </div>
                        </div>
                      )}
                      <div>
                        <div className="text-xs text-ink-3 mb-0.5">Missatges</div>
                        <div className="text-ink-1">{selectedContact.totalMessageCount}</div>
                      </div>
                    </div>
                  )}

                  <div className="amg-card card-clip p-3 sm:p-4">
                    {loadingThread ? (
                      <p className="text-xs text-ink-3 text-center py-4">Carregant historial…</p>
                    ) : (
                      <div className="space-y-2">
                        {[...contactThread]
                          .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
                          .map((msg) => (
                            <div key={msg.id} className={`flex gap-2 ${msg.role === 'USER' ? 'justify-end' : 'justify-start'}`}>
                              <div className={`max-w-[80%] px-3 py-2 rounded-2xl text-sm ${
                                msg.role === 'USER'
                                  ? 'bg-accent text-white rounded-br-sm'
                                  : 'bg-bg-1 border border-border-base text-ink-1 rounded-bl-sm'
                              }`}>
                                {selectedContact && selectedContact.channels.length > 1 && (
                                  <p className={`text-[9px] mb-1 ${msg.role === 'USER' ? 'text-white/50' : 'text-ink-3'}`}>
                                    {CHANNEL_ICON[msg.channel] ?? ''} {msg.customerIdentifier}
                                  </p>
                                )}
                                <p className="whitespace-pre-wrap">{msg.content}</p>
                                <p className={`text-[10px] mt-1 ${msg.role === 'USER' ? 'text-white/60' : 'text-ink-3'}`}>
                                  {new Date(msg.createdAt).toLocaleString('ca-ES', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' })}
                                </p>
                              </div>
                            </div>
                          ))}
                      </div>
                    )}
                  </div>
                </div>
              ) : loadingContacts ? (
                <div className="text-sm text-ink-3 py-4">Carregant contactes…</div>
              ) : visibleContacts.length === 0 ? (
                <div className="amg-card card-clip p-12 text-center">
                  <IconSet.Bot size={32} stroke="#6366f1" className="mx-auto mb-3" />
                  <div className="f-display font-bold text-sm mb-1 text-accent">Sense converses</div>
                  <p className="f-mono text-label text-ink-2">Quan els clients escribin, apareixeran aquí</p>
                </div>
              ) : (
                /* Contact list */
                <div className="space-y-2">
                  {visibleContacts
                    .slice()
                    .sort((a, b) => new Date(b.lastMessageAt ?? 0).getTime() - new Date(a.lastMessageAt ?? 0).getTime())
                    .map(contact => (
                      <button
                        key={contact.contactId}
                        onClick={() => setSelectedContactId(contact.contactId)}
                        className="w-full text-left amg-card card-clip p-4 hover:border-accent transition-colors"
                      >
                        <div className="flex items-start gap-3">
                          <div className={`shrink-0 w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold mt-0.5 ${
                            contact.lastChannel === 'WIDGET' ? 'bg-accent/20 text-accent' :
                            contact.lastChannel === 'WHATSAPP' || contact.lastChannel === 'WHATSAPP_META' ? 'bg-green-500/20 text-green-400' :
                            contact.lastChannel === 'TELEGRAM' ? 'bg-blue-500/20 text-blue-400' :
                            'bg-bg-2 text-ink-2'
                          }`}>
                            {contact.displayName.charAt(0).toUpperCase()}
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2 mb-0.5">
                              <span className="text-sm font-medium text-ink-1 truncate">{contact.displayName}</span>
                              {contact.lastChannel && !CLIENT_CHANNELS.includes(contact.lastChannel) && (
                                <span className="text-[9px] f-mono border border-border-base px-1 py-0.5 rounded text-ink-3 shrink-0">intern</span>
                              )}
                              {contact.phone && (
                                <span className="text-[10px] f-mono text-ink-3 shrink-0">{contact.phone}</span>
                              )}
                              {contact.email && !contact.phone && (
                                <span className="text-[10px] f-mono text-ink-3 truncate">{contact.email}</span>
                              )}
                              {contact.pendingCount > 0 && (
                                <span className="ml-auto shrink-0 w-4 h-4 rounded-full bg-warning text-white text-[9px] flex items-center justify-center font-bold">
                                  {contact.pendingCount}
                                </span>
                              )}
                            </div>
                            <div className="flex items-center gap-2">
                              {contact.lastChannel && (
                                <span className="text-[10px] text-ink-3">{CHANNEL_ICON[contact.lastChannel] ?? ''}</span>
                              )}
                              <p className="text-xs text-ink-3 truncate flex-1">
                                {contact.lastMessageRole === 'ASSISTANT' ? 'Agent: ' : ''}{contact.lastMessage ?? '—'}
                              </p>
                              <span className="text-[10px] text-ink-3 shrink-0">
                                {contact.totalMessageCount} msg
                              </span>
                            </div>
                          </div>
                          {contact.lastMessageAt && (
                            <span className="text-[10px] text-ink-3 shrink-0">
                              {new Date(contact.lastMessageAt).toLocaleDateString('ca-ES', { day: '2-digit', month: '2-digit' })}
                            </span>
                          )}
                        </div>
                      </button>
                    ))}
                </div>
              )}
            </div>
          );
        })()}

        {/* Coneixement Tab */}
        {activeTab === 'coneixement' && isAdmin && (
          <div className="space-y-6">

            {/* Header row */}
            <div className="flex flex-wrap items-center justify-between gap-2">
              <p className="text-sm text-ink-2">
                Gestiona la informació que l&apos;agent coneix del negoci. Cada línia és una entrada independent.
              </p>
              <div className="flex gap-2">
                <Link
                  href="/portal/agents/knowledge"
                  className="px-3 py-1.5 bg-accent text-black rounded text-xs f-mono font-bold hover:opacity-90"
                >
                  ✦ Assistent de configuració
                </Link>
                <button
                  onClick={() => setShowPreview(!showPreview)}
                  className="px-3 py-1.5 border border-border-base rounded text-xs f-mono text-ink-2 hover:text-accent hover:border-accent transition"
                >
                  {showPreview ? 'Ocultar' : 'Previsualitzar'} prompt
                </button>
              </div>
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

            {/* Test de resposta */}
            <div className="amg-card card-clip p-5 space-y-4">
              <div>
                <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Test de resposta</div>
                <div className="text-xs text-ink-3 mt-0.5">Prova com respondria el bot amb la configuració actual de la KB</div>
              </div>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={kbTestMessage}
                  onChange={e => setKbTestMessage(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && kbTestMessage.trim() && kbTestMutation.mutate()}
                  placeholder="Escriu un missatge de prova..."
                  className="flex-1 p-2 bg-bg-1 border border-border-base rounded text-sm focus:outline-none focus:border-accent"
                />
                <button
                  onClick={() => kbTestMutation.mutate()}
                  disabled={kbTestMutation.isPending || !kbTestMessage.trim()}
                  className="px-4 py-2 bg-accent text-white rounded text-sm hover:opacity-90 disabled:opacity-50 shrink-0"
                >
                  {kbTestMutation.isPending ? '…' : 'Enviar'}
                </button>
              </div>
              {kbTestResult && (
                <div className="p-3 bg-bg-1 border border-success/30 rounded">
                  <div className="text-xs text-ink-3 mb-1 uppercase tracking-wider">Resposta del bot</div>
                  <p className="text-sm text-ink-1 whitespace-pre-wrap">{kbTestResult}</p>
                </div>
              )}
              {kbTestError && (
                <div className="p-3 bg-danger/5 border border-danger/30 rounded text-xs text-danger">{kbTestError}</div>
              )}
            </div>

            {/* Contactes i memòria */}
            <div className="amg-card card-clip p-5 space-y-4">
              <div>
                <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Contactes i memòria</div>
                <div className="text-xs text-ink-3 mt-0.5">Clients que han parlat amb el bot. Pots esborrar la seva memòria (resum de conversa) sense perdre l'historial.</div>
              </div>
              {loadingContacts && <p className="text-xs text-ink-3">Carregant contactes…</p>}
              {!loadingContacts && contacts.length === 0 && (
                <p className="text-xs text-ink-3 italic">Sense contactes registrats</p>
              )}
              {contacts.length > 0 && (
                <div className="divide-y divide-border-base">
                  {contacts.map(c => (
                    <div key={c.contactId} className="flex items-center gap-3 py-3">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-medium text-ink-1 truncate">{c.displayName}</span>
                          {c.hasSummary && (
                            <span className="px-1.5 py-0.5 text-[10px] f-mono border border-accent/30 bg-accent/8 text-accent-light rounded">
                              Memòria activa
                            </span>
                          )}
                        </div>
                        <div className="text-xs text-ink-3 mt-0.5">
                          {c.totalMessageCount} missatge{c.totalMessageCount !== 1 ? 's' : ''}
                          {c.lastChannel && ` · ${c.lastChannel}`}
                          {c.lastMessageAt && ` · ${new Date(c.lastMessageAt).toLocaleDateString('ca-ES')}`}
                        </div>
                      </div>
                      {c.hasSummary && (
                        <button
                          onClick={() => {
                            if (confirm(`Esborrar la memòria de ${c.displayName}? El bot perdrà el resum de converses passades però mantindrà l'historial.`))
                              clearMemoryMutation.mutate(c.contactId);
                          }}
                          disabled={clearMemoryMutation.isPending}
                          className="text-xs text-ink-3 hover:text-warning border border-border-base hover:border-warning/40 px-2 py-1 rounded transition disabled:opacity-40"
                        >
                          Esborrar memòria
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* Model IA Tab (ADMIN / SUPER_ADMIN only) */}
        {activeTab === 'ia' && isAdmin && (
          <div className="space-y-6">

            {/* Current model */}
            <div className="amg-card card-clip p-6 space-y-4">
              <div>
                <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Model per canal</div>
                <div className="text-xs text-ink-3 mt-0.5">Pots usar un model diferent per a cada canal. Si no n&apos;especifiques cap, s&apos;usa el model principal.</div>
              </div>
              {aiConfig ? (
                <div className="space-y-3">
                  {[
                    { key: 'chatModel' as const,     label: 'Chat / Widget',  hint: 'landing page i xat portal' },
                    { key: 'whatsappModel' as const, label: 'WhatsApp',       hint: 'Twilio i Meta Cloud API' },
                    { key: 'emailModel' as const,    label: 'Correu',         hint: 'canal email' },
                  ].map(({ key, label, hint }) => {
                    const current = aiConfig[key] ?? '';
                    return (
                      <div key={key} className="flex items-center gap-3">
                        <div className="w-32 shrink-0">
                          <div className="text-sm font-medium text-ink-1">{label}</div>
                          <div className="text-xs text-ink-3">{hint}</div>
                        </div>
                        <select
                          value={current}
                          onChange={(e) => updateAIConfig(tenantId!, { [key]: e.target.value || '' }).then(() =>
                            queryClient.invalidateQueries({ queryKey: ['ai-config', tenantId] })
                          )}
                          className="flex-1 p-2 bg-bg-1 border border-border-base rounded text-sm text-ink-1"
                        >
                          <option value="">Per defecte ({aiConfig.preferredModel})</option>
                          {models.map((m) => (
                            <option key={m.id} value={m.id}>{m.label}</option>
                          ))}
                        </select>
                      </div>
                    );
                  })}
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

            {/* Pressupost mensual */}
            <div className="amg-card card-clip p-6 space-y-4">
              <div>
                <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Pressupost mensual d&apos;IA</div>
                <div className="text-xs text-ink-3 mt-0.5">Cost real calculat per model. Quan s&apos;esgota s&apos;activa el model de backup.</div>
              </div>

              {usageSummary && (
                <div className="space-y-2">
                  <div className="flex items-end justify-between">
                    <span className="text-2xl font-bold text-ink-1">
                      €{usageSummary.usedCostEur.toFixed(4)}
                    </span>
                    <span className="text-sm text-ink-2">
                      {usageSummary.budgetMicros > 0
                        ? `de €${usageSummary.budgetEur.toFixed(2)}`
                        : 'sense límit'}
                    </span>
                  </div>
                  {usageSummary.budgetMicros > 0 && (
                    <div className="w-full bg-bg-2 rounded-full h-2">
                      <div
                        className={`h-2 rounded-full transition-all ${
                          usageSummary.budgetPercent >= 100 ? 'bg-danger' :
                          usageSummary.budgetPercent >= 80  ? 'bg-warning' : 'bg-success'
                        }`}
                        style={{ width: `${usageSummary.budgetPercent}%` }}
                      />
                    </div>
                  )}
                  <div className="text-xs text-ink-3">{usageSummary.usedTokens.toLocaleString()} tokens usats aquest mes</div>

                  {/* WhatsApp cost budget */}
                  {usageSummary.whatsappBudgetMicros > 0 && (
                    <div className="mt-3 pt-3 border-t border-border-base space-y-1">
                      <div className="flex items-end justify-between text-sm">
                        <span className="text-ink-2">WhatsApp</span>
                        <span className="text-ink-1 font-medium">
                          €{usageSummary.usedWhatsappCostEur.toFixed(4)}
                          <span className="text-ink-3 font-normal ml-1">de €{usageSummary.whatsappBudgetEur.toFixed(2)}</span>
                        </span>
                      </div>
                      <div className="w-full bg-bg-2 rounded-full h-1.5">
                        <div
                          className={`h-1.5 rounded-full transition-all ${
                            usageSummary.whatsappBudgetPercent >= 100 ? 'bg-danger' :
                            usageSummary.whatsappBudgetPercent >= 80  ? 'bg-warning' : 'bg-success'
                          }`}
                          style={{ width: `${usageSummary.whatsappBudgetPercent}%` }}
                        />
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* Pressupost recomanat pel sistema */}
              {budgetDefaults && (
                <div className="rounded border border-border-base bg-bg-1 text-xs">
                  <div className="flex items-center justify-between px-3 py-2 border-b border-border-base">
                    <span className="f-mono text-ink-3 uppercase tracking-widest text-[10px]">Recomanat pel sistema</span>
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => setShowBreakdown(v => !v)}
                        className="text-ink-3 hover:text-ink-1 transition f-mono text-[10px]"
                      >
                        {showBreakdown ? '▲ amaga detall' : '▼ veure detall'}
                      </button>
                      <button
                        type="button"
                        onClick={() => applyDefaultsMutation.mutate()}
                        disabled={applyDefaultsMutation.isPending}
                        className="px-2.5 py-1 bg-accent/10 text-accent border border-accent/30 rounded text-[10px] f-mono hover:bg-accent/20 transition disabled:opacity-50"
                      >
                        {applyDefaultsMutation.isPending ? '…' : 'Aplicar'}
                      </button>
                    </div>
                  </div>
                  <div className="px-3 py-2 flex gap-4 text-ink-2">
                    <span>IA: <span className="text-ink-1 font-medium">€{(budgetDefaults.costBudgetEurCents / 100).toFixed(2)}/mes</span></span>
                    <span>WA: <span className="text-ink-1 font-medium">{budgetDefaults.messageBudget} msg · €{(budgetDefaults.whatsappBudgetEurCents / 100).toFixed(2)}/mes</span></span>
                  </div>
                  {showBreakdown && (
                    <div className="px-3 pb-3">
                      <pre className="f-mono text-[10px] text-ink-3 whitespace-pre-wrap leading-relaxed border-t border-border-base pt-2">
                        {budgetDefaults.breakdown}
                      </pre>
                    </div>
                  )}
                </div>
              )}

              <div className="flex gap-2 items-center">
                <span className="text-sm text-ink-2 shrink-0">€</span>
                <input
                  type="number"
                  min="0"
                  step="1"
                  value={budgetDraft !== '' ? budgetDraft : (aiConfig?.monthlyCostBudgetEurCents != null ? (aiConfig.monthlyCostBudgetEurCents / 100).toFixed(2) : '')}
                  onChange={(e) => setBudgetDraft(e.target.value)}
                  placeholder="Sense límit"
                  className="flex-1 p-2 bg-bg-1 border border-border-base rounded text-sm text-ink-1 placeholder:text-ink-3"
                />
                <button
                  onClick={() => {
                    const eur = parseFloat(budgetDraft);
                    updateBudgetMutation.mutate(isNaN(eur) || eur <= 0 ? 0 : Math.round(eur * 100));
                  }}
                  disabled={updateBudgetMutation.isPending || budgetDraft === ''}
                  className="px-4 py-2 bg-accent text-white rounded text-sm hover:opacity-90 disabled:opacity-50"
                >
                  Desar
                </button>
              </div>
            </div>

            {/* Model de backup */}
            <div className="amg-card card-clip p-6 space-y-4">
              <div>
                <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Model de backup</div>
                <div className="text-xs text-ink-3 mt-0.5">S&apos;activa automàticament si el pressupost de tokens s&apos;esgota o si el provider principal falla.</div>
              </div>
              {aiConfig ? (
                <select
                  value={aiConfig.fallbackModel ?? ''}
                  onChange={(e) => updateAIConfig(tenantId!, { fallbackModel: e.target.value || '' }).then(() =>
                    queryClient.invalidateQueries({ queryKey: ['ai-config', tenantId] })
                  )}
                  className="w-full p-2 bg-bg-1 border border-border-base rounded text-sm text-ink-1"
                >
                  <option value="">Cap (l&apos;agent es bloqueja si el principal falla)</option>
                  {models.map((m) => (
                    <option key={m.id} value={m.id}>{m.label}</option>
                  ))}
                </select>
              ) : (
                <div className="text-sm text-ink-2">Carregant...</div>
              )}
            </div>

            {/* Idioma de resposta */}
            <div className="amg-card card-clip p-6 space-y-4">
              <div>
                <div className="f-mono text-label uppercase text-ink-2 tracking-widest">Idioma de resposta</div>
                <div className="text-xs text-ink-3 mt-0.5">Fixa l&apos;idioma en el qual l&apos;agent respon als clients, o deixa-ho en automàtic per detectar l&apos;idioma del missatge.</div>
              </div>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                {[
                  { value: 'auto', label: 'Automàtic', hint: 'Detecta l\'idioma del client' },
                  { value: 'ca', label: 'Català', hint: 'Sempre en català' },
                  { value: 'es', label: 'Espanyol', hint: 'Siempre en español' },
                  { value: 'en', label: 'Anglès', hint: 'Always in English' },
                  { value: 'de', label: 'Alemany', hint: 'Immer auf Deutsch' },
                ].map((opt) => {
                  const current = aiConfig?.responseLanguage ?? 'auto';
                  const isActive = current === opt.value || (opt.value === 'auto' && !current);
                  return (
                    <button
                      key={opt.value}
                      onClick={() => updateLanguageMutation.mutate(opt.value)}
                      disabled={updateLanguageMutation.isPending || isActive}
                      className={`p-3 rounded border-2 text-left transition ${
                        isActive
                          ? 'border-accent bg-accent/10 cursor-default'
                          : 'border-border-base hover:border-accent'
                      } ${updateLanguageMutation.isPending ? 'opacity-50 cursor-not-allowed' : ''}`}
                    >
                      <div className="flex items-center justify-between">
                        <div className="text-sm font-medium text-ink-1">{opt.label}</div>
                        {isActive && <AMGBadge tone="success">Actiu</AMGBadge>}
                      </div>
                      <div className="text-xs text-ink-3 mt-1">{opt.hint}</div>
                    </button>
                  );
                })}
              </div>
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
                      <IconSet.Zap size={14} />
                      Processant...
                    </>
                  ) : (
                    <>
                      <IconSet.Bot size={14} />
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
                <IconSet.X size={20} />
              </button>
            </div>
            <p className="text-sm text-ink-2">
              El bot ja està actiu i responent als clients. Instruccions per a cada canal:
            </p>

            {activationInstructions?.telegram?.configured && (
              <div className="p-4 bg-bg-1 rounded space-y-2">
                <div className="flex items-center gap-2">
                  <IconSet.Bell size={16} />
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
                  <IconSet.Smartphone size={16} />
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

        {/* Widget Web Tab (SUPER_ADMIN only) */}
        {activeTab === 'widget' && isSuperAdmin && (
          <div className="space-y-6 max-w-2xl">
            <div>
              <div className="text-sm font-semibold text-ink-1 mb-1">Widget de xat — amgdl.com</div>
              <p className="text-xs text-ink-3">Configura el comportament de l&apos;assistent que apareix al web públic d&apos;AMG Digitalitzacions.</p>
            </div>

            {/* Templates */}
            <div>
              <div className="text-xs font-semibold text-ink-2 uppercase tracking-wider mb-2">Plantilla de comportament</div>
              <div className="grid grid-cols-2 gap-2">
                {AGENCY_CHAT_TEMPLATES.map(t => (
                  <button
                    key={t.label}
                    onClick={() => { setWidgetSystemPrompt(t.systemPrompt); setWidgetConfigLoaded(false); }}
                    className="text-left px-3 py-2 rounded-lg border border-border-base text-xs text-ink-1 hover:border-accent hover:bg-accent/5 transition-colors"
                  >
                    {t.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Business name */}
            <div>
              <label className="text-xs font-semibold text-ink-2 block mb-1">Nom del negoci (mostrat al widget)</label>
              <input
                type="text"
                value={widgetBusinessName}
                onChange={e => setWidgetBusinessName(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-border-base rounded-lg focus:outline-none focus:border-accent bg-bg-1"
                placeholder="AMG Digitalitzacions"
              />
            </div>

            {/* Model */}
            <div>
              <label className="text-xs font-semibold text-ink-2 block mb-1">Model d&apos;IA</label>
              <select
                value={widgetModel}
                onChange={e => setWidgetModel(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-border-base rounded-lg focus:outline-none focus:border-accent bg-bg-1"
              >
                <option value="claude-haiku-4-5-20251001">Claude Haiku 4.5 (ràpid, econòmic)</option>
                <option value="claude-sonnet-4-6">Claude Sonnet 4.6 (equilibrat)</option>
                <option value="claude-opus-4-7">Claude Opus 4.7 (màxima qualitat)</option>
              </select>
            </div>

            {/* System prompt */}
            <div>
              <label className="text-xs font-semibold text-ink-2 block mb-1">Prompt del sistema (comportament de l&apos;agent)</label>
              <textarea
                value={widgetSystemPrompt}
                onChange={e => setWidgetSystemPrompt(e.target.value)}
                rows={8}
                className="w-full px-3 py-2 text-sm border border-border-base rounded-lg focus:outline-none focus:border-accent bg-bg-1 font-mono resize-y"
                placeholder="Instruccions de comportament per a l'agent..."
              />
              <div className="text-xs text-ink-3 mt-1">{widgetSystemPrompt.length} caràcters</div>
            </div>

            <button
              onClick={async () => {
                if (!tenantId) return;
                setWidgetSaving(true);
                setWidgetSaved(false);
                try {
                  await updateAgencyChatConfig(tenantId, {
                    businessName: widgetBusinessName,
                    systemPrompt: widgetSystemPrompt,
                    preferredModel: widgetModel,
                  });
                  setWidgetSaved(true);
                  setTimeout(() => setWidgetSaved(false), 3000);
                } finally {
                  setWidgetSaving(false);
                }
              }}
              disabled={widgetSaving}
              className="px-6 py-2 bg-accent text-white rounded-lg text-sm font-semibold hover:opacity-90 disabled:opacity-50"
            >
              {widgetSaving ? 'Desant...' : widgetSaved ? '✓ Desat' : 'Desar configuració'}
            </button>
          </div>
        )}

    </PortalShell>
  );
}
