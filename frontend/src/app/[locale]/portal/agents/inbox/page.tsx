'use client';

import { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import {
  listContacts,
  getContactThread,
  sendReply,
  renameContact,
  updateAgentMode,
  getAgentStatus,
  approveResponse,
  editAndSend,
  discardResponse,
  type ContactSummary,
  type ConversationResponse,
} from '@/services/agents-conversational';
import { PortalShell } from '@/components/portal/PortalShell';
import { I } from '@/components/ui/icons';

// ─── helpers ──────────────────────────────────────────────────────────────────

type Channel = 'WHATSAPP' | 'WHATSAPP_META' | 'TELEGRAM' | 'EMAIL' | string;
type AgentMode = 'AUTO' | 'HYBRID' | 'MANUAL';

const CHANNEL_ICON: Record<string, string> = {
  WHATSAPP: '📱',
  WHATSAPP_META: '📱',
  TELEGRAM: '✈️',
  EMAIL: '📧',
};

const CHANNEL_LABEL: Record<string, string> = {
  WHATSAPP: 'WhatsApp',
  WHATSAPP_META: 'WhatsApp',
  TELEGRAM: 'Telegram',
  EMAIL: 'Email',
};

const MODE_LABELS: Record<AgentMode, string> = {
  AUTO: 'AUTO',
  HYBRID: 'HÍBRID',
  MANUAL: 'MANUAL',
};

const MODE_COLORS: Record<AgentMode, string> = {
  AUTO: 'text-green-400',
  HYBRID: 'text-yellow-400',
  MANUAL: 'text-orange-400',
};

function formatTime(iso: string | null): string {
  if (!iso) return '';
  const d = new Date(iso);
  const now = new Date();
  const diffMs = now.getTime() - d.getTime();
  const diffH = diffMs / 3_600_000;
  if (diffH < 24) return d.toLocaleTimeString('ca-ES', { hour: '2-digit', minute: '2-digit' });
  if (diffH < 48) return 'Ahir';
  return d.toLocaleDateString('ca-ES', { day: 'numeric', month: 'short' });
}

function channelIcon(channel: string) {
  return CHANNEL_ICON[channel] ?? '💬';
}

// ─── Contact List ─────────────────────────────────────────────────────────────

function ContactList({
  contacts,
  selectedId,
  onSelect,
  filter,
  onFilterChange,
  search,
  onSearchChange,
}: {
  contacts: ContactSummary[];
  selectedId: string | null;
  onSelect: (c: ContactSummary) => void;
  filter: string;
  onFilterChange: (f: string) => void;
  search: string;
  onSearchChange: (s: string) => void;
}) {
  const channels = Array.from(new Set(contacts.flatMap(c => c.channels.map(ch => ch.channel))));

  const filtered = contacts.filter(c => {
    const matchesSearch = !search ||
      c.displayName.toLowerCase().includes(search.toLowerCase()) ||
      c.channels.some(ch => ch.identifier.includes(search));
    const matchesFilter = filter === 'all' || c.channels.some(ch => ch.channel === filter);
    return matchesSearch && matchesFilter;
  });

  return (
    <div className="flex flex-col h-full border-r border-border-base">
      {/* Search */}
      <div className="p-3 border-b border-border-base">
        <div className="relative">
          <I.Search size={13} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-ink-3" />
          <input
            type="text"
            placeholder="Cerca contacte..."
            value={search}
            onChange={e => onSearchChange(e.target.value)}
            className="w-full bg-[#1a1a2e] border border-border-base rounded pl-8 pr-3 py-1.5 text-xs text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-accent-light"
          />
        </div>
      </div>

      {/* Channel tabs */}
      <div className="flex gap-1 px-2 py-1.5 border-b border-border-base overflow-x-auto">
        <button
          onClick={() => onFilterChange('all')}
          className={`px-2 py-0.5 rounded text-[10px] f-mono uppercase tracking-wider whitespace-nowrap transition-colors ${
            filter === 'all' ? 'bg-accent-muted text-accent-light' : 'text-ink-3 hover:text-ink-1'
          }`}
        >
          Tots
        </button>
        {channels.map(ch => (
          <button
            key={ch}
            onClick={() => onFilterChange(ch)}
            className={`px-2 py-0.5 rounded text-[10px] f-mono uppercase tracking-wider whitespace-nowrap transition-colors ${
              filter === ch ? 'bg-accent-muted text-accent-light' : 'text-ink-3 hover:text-ink-1'
            }`}
          >
            {channelIcon(ch)} {CHANNEL_LABEL[ch] ?? ch}
          </button>
        ))}
      </div>

      {/* Contact list */}
      <div className="flex-1 overflow-y-auto">
        {filtered.length === 0 && (
          <div className="text-center text-ink-3 text-xs p-6">Sense converses</div>
        )}
        {filtered.map(contact => (
          <button
            key={contact.contactId}
            onClick={() => onSelect(contact)}
            className={`w-full text-left px-3 py-2.5 border-b border-border-base/50 transition-colors ${
              selectedId === contact.contactId
                ? 'bg-accent-muted'
                : 'hover:bg-[rgba(255,255,255,0.03)]'
            }`}
          >
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-1.5 min-w-0">
                <span className="text-sm">{channelIcon(contact.lastChannel ?? '')}</span>
                <span className="text-xs font-medium text-ink-0 truncate">{contact.displayName}</span>
              </div>
              <div className="flex items-center gap-1 shrink-0">
                {contact.pendingCount > 0 && (
                  <span className="bg-orange-500 text-black text-[9px] font-bold rounded-full w-4 h-4 flex items-center justify-center">
                    {contact.pendingCount}
                  </span>
                )}
                <span className="text-[10px] text-ink-3">{formatTime(contact.lastMessageAt)}</span>
              </div>
            </div>
            {contact.lastMessage && (
              <div className="mt-0.5 flex items-center gap-1">
                {contact.lastMessageRole === 'ASSISTANT' && (
                  <span className="text-[10px] text-ink-3">Tu:</span>
                )}
                <p className="text-[10px] text-ink-3 truncate">{contact.lastMessage}</p>
              </div>
            )}
          </button>
        ))}
      </div>
    </div>
  );
}

// ─── Thread View ──────────────────────────────────────────────────────────────

function ThreadView({
  tenantId,
  contact,
  messages,
  agentMode,
  onReply,
  onRename,
  onPendingAction,
}: {
  tenantId: string;
  contact: ContactSummary;
  messages: ConversationResponse[];
  agentMode: AgentMode;
  onReply: (text: string) => Promise<void>;
  onRename: (name: string) => void;
  onPendingAction: () => void;
}) {
  const [replyText, setReplyText] = useState('');
  const [sending, setSending] = useState(false);
  const [editingName, setEditingName] = useState(false);
  const [nameInput, setNameInput] = useState(contact.displayName);
  const bottomRef = useRef<HTMLDivElement>(null);

  const approveMutation = useMutation({
    mutationFn: (id: number) => approveResponse(tenantId, id),
    onSuccess: onPendingAction,
  });
  const editMutation = useMutation({
    mutationFn: ({ id, content }: { id: number; content: string }) => editAndSend(tenantId, id, content),
    onSuccess: onPendingAction,
  });
  const discardMutation = useMutation({
    mutationFn: (id: number) => discardResponse(tenantId, id),
    onSuccess: onPendingAction,
  });

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    setNameInput(contact.displayName);
  }, [contact.displayName]);

  const handleSend = async () => {
    const text = replyText.trim();
    if (!text || sending) return;
    setSending(true);
    try {
      await onReply(text);
      setReplyText('');
    } finally {
      setSending(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const canReply = agentMode !== 'AUTO';

  return (
    <div className="flex flex-col h-full">
      {/* Thread header */}
      <div className="px-4 py-3 border-b border-border-base flex items-center gap-3">
        <div className="flex-1 min-w-0">
          {editingName ? (
            <input
              autoFocus
              value={nameInput}
              onChange={e => setNameInput(e.target.value)}
              onBlur={() => { setEditingName(false); onRename(nameInput); }}
              onKeyDown={e => { if (e.key === 'Enter') { setEditingName(false); onRename(nameInput); } }}
              className="bg-transparent border-b border-accent-light text-sm font-semibold text-ink-0 focus:outline-none"
            />
          ) : (
            <button
              onClick={() => setEditingName(true)}
              className="text-sm font-semibold text-ink-0 hover:text-accent-light transition-colors flex items-center gap-1.5"
            >
              {contact.displayName}
              <I.Edit size={11} className="text-ink-3" />
            </button>
          )}
          <div className="flex items-center gap-2 mt-0.5">
            {contact.channels.map(ch => (
              <span key={`${ch.channel}:${ch.identifier}`} className="text-[10px] text-ink-3">
                {channelIcon(ch.channel)} {ch.identifier}
              </span>
            ))}
          </div>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-2">
        {messages.length === 0 && (
          <div className="text-center text-ink-3 text-xs py-8">Sense missatges</div>
        )}
        {messages.map(msg => (
          <MessageBubble
            key={msg.id}
            msg={msg}
            onApprove={msg.pendingApproval ? () => approveMutation.mutate(msg.id) : undefined}
            onDiscard={msg.pendingApproval ? () => discardMutation.mutate(msg.id) : undefined}
            onEditSend={msg.pendingApproval ? (content) => editMutation.mutate({ id: msg.id, content }) : undefined}
          />
        ))}
        <div ref={bottomRef} />
      </div>

      {/* Reply box */}
      <div className="border-t border-border-base p-3">
        {agentMode === 'AUTO' ? (
          <div className="flex items-center gap-2 px-3 py-2 bg-[#1a1a2e] rounded border border-border-base">
            <I.Bot size={13} className="text-green-400 shrink-0" />
            <span className="text-[11px] text-ink-3">El bot respon automàticament. Canvia a Manual per escriure tu.</span>
          </div>
        ) : (
          <div className="flex gap-2">
            <textarea
              value={replyText}
              onChange={e => setReplyText(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={`Escriu per ${CHANNEL_LABEL[contact.lastChannel ?? ''] ?? 'canal desconegut'}... (Enter per enviar)`}
              rows={2}
              className="flex-1 bg-[#1a1a2e] border border-border-base rounded px-3 py-2 text-xs text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-accent-light resize-none"
            />
            <button
              onClick={handleSend}
              disabled={!replyText.trim() || sending}
              className="px-3 py-2 bg-accent-light text-black text-xs font-bold rounded disabled:opacity-40 hover:bg-orange-400 transition-colors self-end"
            >
              {sending ? '...' : 'Enviar'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

function MessageBubble({
  msg,
  onApprove,
  onDiscard,
  onEditSend,
}: {
  msg: ConversationResponse;
  onApprove?: () => void;
  onDiscard?: () => void;
  onEditSend?: (content: string) => void;
}) {
  const isUser = msg.role === 'USER';
  const [editing, setEditing] = useState(false);
  const [editText, setEditText] = useState(msg.content);

  return (
    <div className={`flex flex-col ${isUser ? 'items-start' : 'items-end'}`}>
      <div className={`max-w-[75%] rounded-lg px-3 py-2 ${
        isUser
          ? 'bg-[#1a1a2e] text-ink-0'
          : 'bg-accent-muted text-accent-light'
      } ${msg.pendingApproval ? 'opacity-70 border border-orange-400/40' : ''}`}>
        {editing ? (
          <textarea
            value={editText}
            onChange={e => setEditText(e.target.value)}
            rows={3}
            className="w-full bg-[#0d0d1a] border border-orange-400/40 rounded px-2 py-1 text-xs text-ink-0 focus:outline-none focus:border-orange-400 resize-none"
          />
        ) : (
          <p className="text-xs whitespace-pre-wrap break-words">{msg.content}</p>
        )}
        <div className="flex items-center gap-1.5 mt-1">
          <span className="text-[9px] text-ink-3">{channelIcon(msg.channel as string)}</span>
          <span className="text-[9px] text-ink-3">
            {new Date(msg.createdAt).toLocaleTimeString('ca-ES', { hour: '2-digit', minute: '2-digit' })}
          </span>
          {msg.pendingApproval && !editing && (
            <span className="text-[9px] text-orange-400">⏳ pendent</span>
          )}
        </div>
      </div>

      {/* Pending action buttons */}
      {msg.pendingApproval && onApprove && (
        <div className="flex items-center gap-1.5 mt-1 mr-1">
          {editing ? (
            <>
              <button
                onClick={() => { onEditSend?.(editText); setEditing(false); }}
                disabled={!editText.trim()}
                className="px-2 py-0.5 bg-orange-500 text-black text-[9px] font-bold rounded hover:bg-orange-400 disabled:opacity-40 transition-colors"
              >
                Enviar editat
              </button>
              <button
                onClick={() => { setEditing(false); setEditText(msg.content); }}
                className="px-2 py-0.5 bg-[#1a1a2e] text-ink-3 text-[9px] rounded border border-border-base hover:text-ink-1 transition-colors"
              >
                Cancel·lar
              </button>
            </>
          ) : (
            <>
              <button
                onClick={onApprove}
                className="px-2 py-0.5 bg-green-600 text-white text-[9px] font-bold rounded hover:bg-green-500 transition-colors"
              >
                ✓ Aprovar
              </button>
              <button
                onClick={() => setEditing(true)}
                className="px-2 py-0.5 bg-[#1a1a2e] text-ink-2 text-[9px] rounded border border-border-base hover:text-ink-0 hover:border-orange-400/50 transition-colors"
              >
                ✎ Editar
              </button>
              <button
                onClick={onDiscard}
                className="px-2 py-0.5 bg-[#1a1a2e] text-red-400 text-[9px] rounded border border-border-base hover:border-red-400/50 transition-colors"
              >
                ✕ Descartar
              </button>
            </>
          )}
        </div>
      )}
    </div>
  );
}

// ─── Mode Toggle ──────────────────────────────────────────────────────────────

function ModeToggle({ tenantId, currentMode }: { tenantId: string; currentMode: AgentMode }) {
  const qc = useQueryClient();
  const [open, setOpen] = useState(false);

  const mutation = useMutation({
    mutationFn: (mode: AgentMode) => updateAgentMode(tenantId, mode),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['agent-status', tenantId] }),
  });

  return (
    <div className="relative">
      <button
        onClick={() => setOpen(o => !o)}
        className={`flex items-center gap-1.5 px-2.5 py-1 rounded border border-border-base text-[10px] f-mono uppercase tracking-wider transition-colors hover:border-accent-light ${MODE_COLORS[currentMode]}`}
      >
        <span className="w-1.5 h-1.5 rounded-full bg-current" />
        {MODE_LABELS[currentMode]}
        <I.ChevDown size={10} />
      </button>
      {open && (
        <div className="absolute right-0 top-full mt-1 bg-[#1a1a2e] border border-border-base rounded shadow-xl z-10 min-w-[120px]">
          {(['AUTO', 'HYBRID', 'MANUAL'] as AgentMode[]).map(mode => (
            <button
              key={mode}
              onClick={() => { mutation.mutate(mode); setOpen(false); }}
              className={`w-full text-left px-3 py-2 text-[10px] f-mono uppercase tracking-wider transition-colors hover:bg-accent-muted ${MODE_COLORS[mode]} ${currentMode === mode ? 'bg-accent-muted/50' : ''}`}
            >
              {MODE_LABELS[mode]}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function InboxPage() {
  const { user, isAdmin, isSuperAdmin } = useAuth();
  const searchParams = useSearchParams();
  const qc = useQueryClient();

  const paramTenantId = searchParams.get('tenantId');
  const tenantId = (isAdmin || isSuperAdmin) && paramTenantId
    ? paramTenantId
    : user?.tenantId ?? null;

  const [selectedContact, setSelectedContact] = useState<ContactSummary | null>(null);
  const [filter, setFilter] = useState('all');
  const [search, setSearch] = useState('');

  const { data: contacts = [], isLoading } = useQuery({
    queryKey: ['contacts', tenantId],
    queryFn: () => listContacts(tenantId!),
    enabled: !!tenantId,
    refetchInterval: 30_000,
  });

  const { data: thread = [] } = useQuery({
    queryKey: ['contact-thread', tenantId, selectedContact?.contactId],
    queryFn: () => getContactThread(tenantId!, selectedContact!.contactId),
    enabled: !!tenantId && !!selectedContact,
    refetchInterval: 10_000,
  });

  const { data: status } = useQuery({
    queryKey: ['agent-status', tenantId],
    queryFn: () => getAgentStatus(tenantId!),
    enabled: !!tenantId,
  });

  const replyMutation = useMutation({
    mutationFn: (text: string) => sendReply(tenantId!, selectedContact!.contactId, text),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['contact-thread', tenantId, selectedContact?.contactId] });
      qc.invalidateQueries({ queryKey: ['contacts', tenantId] });
    },
  });

  const renameMutation = useMutation({
    mutationFn: ({ contactId, name }: { contactId: string; name: string }) =>
      renameContact(tenantId!, contactId, name),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['contacts', tenantId] }),
  });

  if (!tenantId) {
    return (
      <PortalShell breadcrumb="inbox">
        <div className="flex items-center justify-center h-64 text-ink-3 text-sm">
          No s'ha pogut determinar el tenant.
        </div>
      </PortalShell>
    );
  }

  const agentMode = (status?.agentMode ?? 'AUTO') as AgentMode;
  const totalPending = contacts.reduce((s, c) => s + c.pendingCount, 0);

  return (
    <PortalShell breadcrumb="inbox">
      <div className="flex flex-col h-[calc(100dvh-56px)]">
        {/* Header */}
        <div className="px-4 py-2.5 border-b border-border-base flex items-center justify-between gap-3 shrink-0">
          <div className="flex items-center gap-2">
            <I.Mail size={14} className="text-accent-light" />
            <span className="text-xs font-semibold text-ink-0">Converses</span>
            {totalPending > 0 && (
              <span className="bg-orange-500 text-black text-[9px] font-bold rounded-full px-1.5 py-0.5">
                {totalPending} pendents
              </span>
            )}
          </div>
          {status && <ModeToggle tenantId={tenantId} currentMode={agentMode} />}
        </div>

        {/* Body */}
        <div className="flex flex-1 min-h-0">
          {/* Sidebar */}
          <div className="w-[260px] shrink-0">
            {isLoading ? (
              <div className="flex items-center justify-center h-32 text-ink-3 text-xs">
                Carregant...
              </div>
            ) : (
              <ContactList
                contacts={contacts}
                selectedId={selectedContact?.contactId ?? null}
                onSelect={setSelectedContact}
                filter={filter}
                onFilterChange={setFilter}
                search={search}
                onSearchChange={setSearch}
              />
            )}
          </div>

          {/* Thread */}
          <div className="flex-1 min-w-0">
            {selectedContact ? (
              <ThreadView
                tenantId={tenantId}
                contact={selectedContact}
                messages={thread}
                agentMode={agentMode}
                onReply={text => replyMutation.mutateAsync(text)}
                onRename={name =>
                  renameMutation.mutate({ contactId: selectedContact.contactId, name })
                }
                onPendingAction={() => {
                  qc.invalidateQueries({ queryKey: ['contact-thread', tenantId, selectedContact.contactId] });
                  qc.invalidateQueries({ queryKey: ['contacts', tenantId] });
                }}
              />
            ) : (
              <div className="flex flex-col items-center justify-center h-full gap-3 text-ink-3">
                <I.Mail size={32} />
                <p className="text-sm">Selecciona una conversa</p>
                <p className="text-xs">
                  {contacts.length === 0
                    ? 'Encara no hi ha converses. Els missatges apareixeran aquí automàticament.'
                    : `${contacts.length} conversa${contacts.length > 1 ? 'es' : ''}`}
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    </PortalShell>
  );
}
