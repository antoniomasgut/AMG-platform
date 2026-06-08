'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { IconSet } from '@/components/ui/icons';
import {
  listCommTemplates,
  updateCommTemplate,
  sendComm,
  ACTIONS,
  LANGUAGES,
  type CommTemplate,
  type CommSendRequest,
} from '@/services/commTemplates';

const ACTION_LABEL: Record<string, string> = Object.fromEntries(ACTIONS.map(a => [a.value, a.label]));
const LANG_LABEL: Record<string, string>   = Object.fromEntries(LANGUAGES.map(l => [l.value, l.label]));
const CHANNEL_TONE: Record<string, 'success' | 'info'> = { EMAIL: 'info', WHATSAPP: 'success' };

// ── Edit modal ────────────────────────────────────────────────────────────────

function EditModal({
  tpl,
  onClose,
  onSave,
}: {
  tpl: CommTemplate;
  onClose: () => void;
  onSave: (id: string, body: Partial<CommTemplate>) => void;
}) {
  const [subject, setSubject] = useState(tpl.subject ?? '');
  const [body, setBody]       = useState(tpl.body);
  const vars = ACTIONS.find(a => a.value === tpl.action)?.variables ?? [];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
      <div className="amg-card card-clip w-full max-w-2xl max-h-[90vh] flex flex-col">
        <div className="flex items-center justify-between p-5 border-b border-white/10">
          <div>
            <div className="f-display font-bold text-sm">Editar plantilla</div>
            <div className="flex items-center gap-2 mt-1 flex-wrap">
              <AMGBadge tone={CHANNEL_TONE[tpl.channel] ?? 'neutral'}>{tpl.channel}</AMGBadge>
              <AMGBadge tone="neutral">{LANG_LABEL[tpl.language] ?? tpl.language}</AMGBadge>
              <span className="f-mono text-caption text-ink-2">{ACTION_LABEL[tpl.action] ?? tpl.action} · {tpl.sector}</span>
            </div>
          </div>
          <button onClick={onClose}><IconSet.X size={18} stroke="#94a3b8" /></button>
        </div>

        <div className="flex-1 overflow-y-auto p-5 space-y-4">
          {tpl.channel === 'EMAIL' && (
            <div className="space-y-1">
              <label className="f-mono text-label text-ink-2 uppercase tracking-wider">Assumpte</label>
              <input
                value={subject}
                onChange={e => setSubject(e.target.value)}
                className="w-full bg-[#0d0d1a] border border-white/10 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-[#FF6B00] transition-colors"
              />
            </div>
          )}
          <div className="space-y-1">
            <label className="f-mono text-label text-ink-2 uppercase tracking-wider">Missatge</label>
            <textarea
              value={body}
              onChange={e => setBody(e.target.value)}
              rows={12}
              className="w-full bg-[#0d0d1a] border border-white/10 rounded-lg px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-[#FF6B00] transition-colors resize-y"
            />
          </div>
          {vars.length > 0 && (
            <div className="bg-white/5 rounded-lg p-3 text-xs text-ink-2 f-mono">
              Variables: {vars.map(v => `{${v}}`).join(' · ')}
            </div>
          )}
        </div>

        <div className="p-5 border-t border-white/10 flex justify-end gap-2">
          <AMGButton variant="ghost" onClick={onClose}>Cancel·lar</AMGButton>
          <AMGButton
            icon={IconSet.Check}
            onClick={() => { onSave(tpl.id, { subject: subject || undefined, body }); onClose(); }}
          >
            Desar
          </AMGButton>
        </div>
      </div>
    </div>
  );
}

// ── Send modal ────────────────────────────────────────────────────────────────

function SendModal({
  defaultAction,
  defaultLanguage,
  defaultVariables,
  onClose,
}: {
  defaultAction?: string;
  defaultLanguage?: string;
  defaultVariables?: Record<string, string>;
  onClose: () => void;
}) {
  const [channel,  setChannel]  = useState<'EMAIL' | 'WHATSAPP'>('EMAIL');
  const [to,       setTo]       = useState('');
  const [action,   setAction]   = useState(defaultAction   ?? 'DEMO_SEND');
  const [language, setLanguage] = useState(defaultLanguage ?? 'ca');
  const [vars,     setVars]     = useState<Record<string, string>>(defaultVariables ?? {});
  const [sent,     setSent]     = useState(false);

  const actionDef = ACTIONS.find(a => a.value === action);

  const mutation = useMutation({
    mutationFn: (req: CommSendRequest) => sendComm(req),
    onSuccess: () => setSent(true),
  });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
      <div className="amg-card card-clip w-full max-w-lg">
        <div className="flex items-center justify-between p-5 border-b border-white/10">
          <div className="f-display font-bold text-sm">Enviar missatge</div>
          <button onClick={onClose}><IconSet.X size={18} stroke="#94a3b8" /></button>
        </div>

        {sent ? (
          <div className="p-8 text-center space-y-3">
            <IconSet.Check size={32} stroke="#4ade80" className="mx-auto" />
            <div className="f-display font-bold">Missatge enviat!</div>
            <AMGButton onClick={onClose}>Tancar</AMGButton>
          </div>
        ) : (
          <div className="p-5 space-y-4">
            <div className="grid grid-cols-3 gap-3">
              <div className="space-y-1">
                <label className="f-mono text-label text-ink-2 uppercase tracking-wider">Canal</label>
                <select
                  value={channel}
                  onChange={e => setChannel(e.target.value as 'EMAIL' | 'WHATSAPP')}
                  className="w-full bg-[#0d0d1a] border border-white/10 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-[#FF6B00]"
                >
                  <option value="EMAIL">Email</option>
                  <option value="WHATSAPP">WhatsApp</option>
                </select>
              </div>
              <div className="space-y-1">
                <label className="f-mono text-label text-ink-2 uppercase tracking-wider">Idioma</label>
                <select
                  value={language}
                  onChange={e => setLanguage(e.target.value)}
                  className="w-full bg-[#0d0d1a] border border-white/10 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-[#FF6B00]"
                >
                  {LANGUAGES.map(l => <option key={l.value} value={l.value}>{l.label}</option>)}
                </select>
              </div>
              <div className="space-y-1">
                <label className="f-mono text-label text-ink-2 uppercase tracking-wider">Acció</label>
                <select
                  value={action}
                  onChange={e => setAction(e.target.value)}
                  className="w-full bg-[#0d0d1a] border border-white/10 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-[#FF6B00]"
                >
                  {ACTIONS.map(a => <option key={a.value} value={a.value}>{a.label}</option>)}
                </select>
              </div>
            </div>

            <div className="space-y-1">
              <label className="f-mono text-label text-ink-2 uppercase tracking-wider">
                {channel === 'EMAIL' ? 'Email destinatari' : 'Telèfon (+34...)'}
              </label>
              <input
                value={to}
                onChange={e => setTo(e.target.value)}
                placeholder={channel === 'EMAIL' ? 'client@email.com' : '+34600123456'}
                className="w-full bg-[#0d0d1a] border border-white/10 rounded-lg px-3 py-2 text-sm text-white placeholder-white/30 focus:outline-none focus:border-[#FF6B00]"
              />
            </div>

            {actionDef && (
              <div className="space-y-2">
                <label className="f-mono text-label text-ink-2 uppercase tracking-wider">Variables</label>
                {actionDef.variables.map(v => (
                  <div key={v} className="flex items-center gap-2">
                    <span className="f-mono text-caption text-ink-3 w-40 shrink-0">{'{' + v + '}'}</span>
                    <input
                      value={vars[v] ?? ''}
                      onChange={e => setVars(prev => ({ ...prev, [v]: e.target.value }))}
                      className="flex-1 bg-[#0d0d1a] border border-white/10 rounded-lg px-3 py-1.5 text-sm text-white focus:outline-none focus:border-[#FF6B00]"
                    />
                  </div>
                ))}
              </div>
            )}

            {mutation.isError && (
              <p className="text-sm text-red-400">Error en enviar el missatge</p>
            )}

            <div className="flex justify-end gap-2 pt-2">
              <AMGButton variant="ghost" onClick={onClose}>Cancel·lar</AMGButton>
              <AMGButton
                icon={IconSet.Zap}
                onClick={() => mutation.mutate({ channel, to, action, language, variables: vars })}
                disabled={!to.trim() || mutation.isPending}
              >
                {mutation.isPending ? 'Enviant…' : 'Enviar'}
              </AMGButton>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────

export default function CommTemplatesPage() {
  const queryClient = useQueryClient();
  const [editing,     setEditing]     = useState<CommTemplate | null>(null);
  const [sending,     setSending]     = useState(false);
  const [filterLang,  setFilterLang]  = useState('ca');
  const [filterChan,  setFilterChan]  = useState<'ALL' | 'EMAIL' | 'WHATSAPP'>('ALL');

  const { data: templates = [], isLoading } = useQuery({
    queryKey: ['comm-templates'],
    queryFn: listCommTemplates,
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, body }: { id: string; body: Partial<CommTemplate> }) =>
      updateCommTemplate(id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['comm-templates'] }),
  });

  const filtered = templates.filter(t =>
    t.language === filterLang &&
    (filterChan === 'ALL' || t.channel === filterChan)
  );

  const grouped = ACTIONS.map(action => ({
    ...action,
    templates: filtered.filter(t => t.action === action.value),
  }));

  return (
    <div className="p-4 sm:p-8 space-y-8">

      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / comunicació /</span>
          <div className="f-display font-bold text-xl mt-1">Plantilles de comunicació</div>
          <p className="text-ui text-ink-1 mt-1">WhatsApp i Email per a demos, pressupostos i cites.</p>
        </div>
        <AMGButton size="sm" icon={IconSet.Zap} onClick={() => setSending(true)}>
          Enviar missatge
        </AMGButton>
      </div>

      {/* Filters */}
      <div className="flex items-center gap-3 flex-wrap">
        <div className="flex items-center gap-1 bg-white/5 rounded-lg p-1">
          {LANGUAGES.map(l => (
            <button
              key={l.value}
              onClick={() => setFilterLang(l.value)}
              className={`px-3 py-1 rounded text-sm transition-colors ${
                filterLang === l.value
                  ? 'bg-[#FF6B00] text-white font-medium'
                  : 'text-ink-2 hover:text-white'
              }`}
            >
              {l.label}
            </button>
          ))}
        </div>
        <div className="flex items-center gap-1 bg-white/5 rounded-lg p-1">
          {(['ALL', 'EMAIL', 'WHATSAPP'] as const).map(c => (
            <button
              key={c}
              onClick={() => setFilterChan(c)}
              className={`px-3 py-1 rounded text-sm transition-colors ${
                filterChan === c
                  ? 'bg-white/20 text-white font-medium'
                  : 'text-ink-2 hover:text-white'
              }`}
            >
              {c === 'ALL' ? 'Tots' : c}
            </button>
          ))}
        </div>
      </div>

      {/* Template list */}
      {isLoading ? (
        <div className="flex justify-center py-12">
          <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
        </div>
      ) : (
        <div className="space-y-6">
          {grouped.map(group => (
            <div key={group.value} className="space-y-2">
              <div className="f-mono text-label uppercase tracking-widest text-ink-2">{group.label}</div>
              {group.templates.length === 0 ? (
                <div className="amg-card card-clip p-4 text-sm text-ink-3 f-mono">
                  Sense plantilles per a {LANG_LABEL[filterLang]}
                  {filterChan !== 'ALL' ? ` / ${filterChan}` : ''}
                </div>
              ) : (
                <div className="space-y-2">
                  {group.templates.map(tpl => (
                    <div key={tpl.id} className="amg-card card-clip p-4 flex items-start gap-4">
                      <div className="flex-1 min-w-0 space-y-1">
                        <div className="flex items-center gap-2 flex-wrap">
                          <AMGBadge tone={CHANNEL_TONE[tpl.channel] ?? 'neutral'}>{tpl.channel}</AMGBadge>
                          <span className="f-mono text-caption text-ink-2">{tpl.sector}</span>
                          {!tpl.isActive && <AMGBadge tone="neutral">Inactiva</AMGBadge>}
                        </div>
                        {tpl.subject && (
                          <div className="text-sm font-medium text-white/80">{tpl.subject}</div>
                        )}
                        <pre className="text-xs text-ink-2 whitespace-pre-wrap line-clamp-4 font-mono">
                          {tpl.body}
                        </pre>
                      </div>
                      <AMGButton size="sm" variant="ghost" icon={IconSet.Edit} onClick={() => setEditing(tpl)}>
                        Editar
                      </AMGButton>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {editing && (
        <EditModal
          tpl={editing}
          onClose={() => setEditing(null)}
          onSave={(id, body) => updateMutation.mutate({ id, body })}
        />
      )}

      {sending && (
        <SendModal defaultLanguage={filterLang} onClose={() => setSending(false)} />
      )}
    </div>
  );
}
