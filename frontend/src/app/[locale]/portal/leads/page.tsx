'use client';

import { useState } from 'react';
import { createPortal } from 'react-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import { getLeads, getLeadStats, changeStage, sendOutreach, sendTemplate, setWhatsapp, type Lead, type OutreachRequest } from '@/services/leads';
import { listTemplates, SECTORS, type MessageTemplate } from '@/services/message-templates';
import { createDemoSession, updateDemoSession } from '@/services/demo';
import { SECTOR_CONTEXTS, getSectorContext } from '@/services/sector-contexts';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGStat } from '@/components/ui/stat';
import { I } from '@/components/ui/icons';
import { useRouter } from 'next/navigation';
import { useParams } from 'next/navigation';

const STAGES = ['NEW', 'CONTACTED', 'QUALIFIED', 'PROPOSAL', 'NEGOTIATION', 'WON', 'LOST'] as const;

const STAGE_LABEL: Record<string, string> = {
  NEW: 'Nou', CONTACTED: 'Contactat', QUALIFIED: 'Qualificat',
  PROPOSAL: 'Proposta', NEGOTIATION: 'Negociació', WON: 'Guanyat', LOST: 'Perdut',
};

const STAGE_TONE: Record<string, 'neutral' | 'info' | 'accent' | 'warning' | 'success' | 'danger'> = {
  NEW: 'neutral', CONTACTED: 'info', QUALIFIED: 'accent',
  PROPOSAL: 'warning', NEGOTIATION: 'warning', WON: 'success', LOST: 'danger',
};

const SOURCE_LABEL: Record<string, string> = {
  WEBSITE: 'Web', REFERRAL: 'Referit', COLD_CALL: 'Cold Call',
  SOCIAL_MEDIA: 'RRSS', GOOGLE_MAPS: 'Google Maps', OTHER: 'Altre',
};

const TEMPLATES: Record<'ca' | 'es', { subject: string; body: string }> = {
  ca: {
    subject: '{{nom}} — Hem creat una demo digital per al vostre negoci',
    body: `Hola,

He vist el negoci {{nom}} i he creat una demo de com podríeu tenir una presència digital professional.

Podeu veure-la aquí: {{demoUrl}}

M'agradaria comentar-vos com podria ajudar al vostre negoci. Esteu disponibles per a una reunió breu presencial o per videoconferència?

Salutacions,
Antonio
AMG Digitalització
Tel: 654 048 164`,
  },
  es: {
    subject: '{{nom}} — Hemos creado una demo digital para su negocio',
    body: `Hola,

He visto el negocio {{nom}} y he creado una demo de cómo podrían tener una presencia digital profesional.

Pueden verla aquí: {{demoUrl}}

Me gustaría comentarles cómo podría ayudar a su negocio. ¿Están disponibles para una reunión breve presencial o por videoconferencia?

Saludos,
Antonio
AMG Digitalització
Tel: 654 048 164`,
  },
};

const DEFAULT_DEMO_CONTEXT = (name: string) =>
  `Ets l'assistent virtual de ${name}. Respon preguntes sobre els nostres serveis, preus i com podem ajudar el negoci a créixer digitalment.`;

function substitute(text: string, lead: Lead) {
  return text
    .replace(/\{\{nom\}\}/g, lead.name)
    .replace(/\{\{email\}\}/g, lead.email ?? '')
    .replace(/\{\{telefon\}\}/g, lead.phone ?? '');
}

function TemplateSendModal({
  leads,
  templates,
  onClose,
  onSend,
  sending,
}: {
  leads: Lead[];
  templates: MessageTemplate[];
  onClose: () => void;
  onSend: (templateId: string, leadIds: string[], channel: string) => void;
  sending: boolean;
}) {
  const [sector, setSector] = useState<string>('');
  const [channel, setChannel] = useState<'WHATSAPP' | 'EMAIL'>('WHATSAPP');
  const [selectedTemplate, setSelectedTemplate] = useState<MessageTemplate | null>(null);
  const [selectedLeads, setSelectedLeads] = useState<Set<string>>(new Set());
  const [previewIdx, setPreviewIdx] = useState(0);

  const filteredTemplates = templates.filter(t => {
    if (t.type !== channel) return false;
    if (!sector) return true;
    return !t.sector || t.sector === sector;
  });

  const eligibleLeads = leads.filter(l =>
    l.isActive && l.stage !== 'WON' && l.stage !== 'LOST' &&
    (channel === 'WHATSAPP' ? !!l.phone : !!l.email)
  );

  const selectedList = eligibleLeads.filter(l => selectedLeads.has(l.id));
  const previewLead = selectedList[previewIdx] ?? selectedList[0];

  const toggleLead = (id: string) => {
    const s = new Set(selectedLeads);
    s.has(id) ? s.delete(id) : s.add(id);
    setSelectedLeads(s);
    setPreviewIdx(0);
  };
  const toggleAll = () => {
    if (selectedLeads.size === eligibleLeads.length) setSelectedLeads(new Set());
    else setSelectedLeads(new Set(eligibleLeads.map(l => l.id)));
    setPreviewIdx(0);
  };

  const previewBody = selectedTemplate && previewLead ? substitute(selectedTemplate.body, previewLead) : null;
  const previewSubject = selectedTemplate?.subject && previewLead ? substitute(selectedTemplate.subject, previewLead) : null;

  const canSend = selectedTemplate && selectedLeads.size > 0;

  return (
    <>
      <div className="fixed inset-0 bg-black/60 z-40" onClick={onClose} />
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div className="bg-bg-0 border border-border-base w-full max-w-5xl max-h-[92vh] flex flex-col shadow-2xl">

          {/* Header */}
          <div className="flex items-center justify-between p-5 border-b border-border-base shrink-0">
            <div>
              <div className="f-display font-bold text-base">Enviar plantilla</div>
              <div className="f-mono text-[10px] text-ink-3 mt-0.5">
                {selectedLeads.size} lead{selectedLeads.size !== 1 ? 's' : ''} seleccionat{selectedLeads.size !== 1 ? 's' : ''}
              </div>
            </div>
            <button onClick={onClose} className="p-1.5 text-ink-3 hover:text-ink-0">
              <I.X size={16} />
            </button>
          </div>

          <div className="flex flex-1 min-h-0">

            {/* Col 1: Sector + canal + plantilla */}
            <div className="w-64 shrink-0 border-r border-border-base flex flex-col overflow-y-auto">
              {/* Canal */}
              <div className="p-3 border-b border-border-base">
                <div className="f-mono text-[10px] uppercase text-ink-3 tracking-widest mb-2">Canal</div>
                <div className="flex gap-1">
                  {(['WHATSAPP', 'EMAIL'] as const).map(ch => (
                    <button key={ch} onClick={() => { setChannel(ch); setSelectedTemplate(null); }}
                      className={`flex-1 f-mono text-[11px] py-1.5 border transition-colors ${channel === ch ? 'border-accent bg-accent-muted text-accent-light' : 'border-border-base text-ink-3 hover:border-accent/40'}`}>
                      {ch === 'WHATSAPP' ? '📱 WA' : '✉️ Email'}
                    </button>
                  ))}
                </div>
              </div>

              {/* Sector */}
              <div className="p-3 border-b border-border-base">
                <div className="f-mono text-[10px] uppercase text-ink-3 tracking-widest mb-2">Sector</div>
                <div className="space-y-0.5">
                  <button onClick={() => setSector('')}
                    className={`w-full text-left f-mono text-xs px-2 py-1.5 rounded transition-colors ${!sector ? 'bg-accent-muted text-accent-light' : 'text-ink-2 hover:bg-bg-2'}`}>
                    Tots els sectors
                  </button>
                  {SECTORS.map(s => (
                    <button key={s.key} onClick={() => setSector(s.key)}
                      className={`w-full text-left f-mono text-xs px-2 py-1.5 rounded transition-colors ${sector === s.key ? 'bg-accent-muted text-accent-light' : 'text-ink-2 hover:bg-bg-2'}`}>
                      {s.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Plantilles filtrades */}
              <div className="p-3 flex-1">
                <div className="f-mono text-[10px] uppercase text-ink-3 tracking-widest mb-2">
                  Plantilles <span className="text-ink-3 normal-case">({filteredTemplates.length})</span>
                </div>
                {filteredTemplates.length === 0 ? (
                  <p className="f-mono text-[11px] text-ink-3 italic">Cap plantilla per a aquest filtre.</p>
                ) : (
                  <div className="space-y-1">
                    {filteredTemplates.map(t => (
                      <button key={t.id} onClick={() => setSelectedTemplate(t)}
                        className={`w-full text-left px-2 py-2 border transition-colors ${selectedTemplate?.id === t.id ? 'border-accent bg-accent-muted' : 'border-border-base hover:border-accent/40'}`}>
                        <div className="f-display font-bold text-xs text-ink-0">{t.name}</div>
                        {t.sector && <div className="f-mono text-[9px] text-ink-3 mt-0.5">{t.sector}</div>}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* Col 2: Leads */}
            <div className="w-52 shrink-0 border-r border-border-base flex flex-col">
              <div className="p-3 border-b border-border-base flex items-center justify-between">
                <span className="f-mono text-[10px] uppercase text-ink-3 tracking-widest">Leads</span>
                <button onClick={toggleAll} className="f-mono text-[9px] text-accent-light hover:underline">
                  {selectedLeads.size === eligibleLeads.length ? 'Cap' : 'Tots'}
                </button>
              </div>
              <div className="flex-1 overflow-y-auto">
                {eligibleLeads.length === 0 ? (
                  <div className="p-4 text-center f-mono text-xs text-ink-3">
                    Cap lead amb {channel === 'WHATSAPP' ? 'telèfon' : 'email'}
                  </div>
                ) : eligibleLeads.map(lead => (
                  <label key={lead.id}
                    className="flex items-start gap-2 px-3 py-2 cursor-pointer hover:bg-bg-2 border-b border-[rgba(226,232,240,0.04)]">
                    <input type="checkbox" checked={selectedLeads.has(lead.id)}
                      onChange={() => toggleLead(lead.id)}
                      className="mt-0.5 shrink-0 accent-accent" />
                    <div className="min-w-0">
                      <div className="f-display font-bold text-xs truncate">{lead.name}</div>
                      <div className="f-mono text-[9px] text-ink-3 truncate">
                        {channel === 'WHATSAPP' ? lead.phone : lead.email}
                      </div>
                      <AMGBadge tone={STAGE_TONE[lead.stage] ?? 'neutral'} className="mt-0.5 text-[8px]">
                        {STAGE_LABEL[lead.stage] ?? lead.stage}
                      </AMGBadge>
                    </div>
                  </label>
                ))}
              </div>
            </div>

            {/* Col 3: Preview */}
            <div className="flex-1 flex flex-col min-w-0 p-5 overflow-y-auto">
              {!selectedTemplate ? (
                <div className="flex-1 flex items-center justify-center">
                  <p className="f-mono text-sm text-ink-3">← Selecciona una plantilla</p>
                </div>
              ) : (
                <>
                  <div className="flex items-center justify-between mb-4">
                    <div className="f-mono text-[10px] uppercase text-ink-3 tracking-widest">
                      Previsualització {previewLead ? `— ${previewLead.name}` : ''}
                    </div>
                    {selectedList.length > 1 && (
                      <div className="flex items-center gap-2">
                        <button onClick={() => setPreviewIdx(i => Math.max(0, i - 1))}
                          disabled={previewIdx === 0}
                          className="p-1 text-ink-3 hover:text-ink-0 disabled:opacity-30">
                          <I.ChevronLeft size={14} />
                        </button>
                        <span className="f-mono text-[10px] text-ink-3">{previewIdx + 1}/{selectedList.length}</span>
                        <button onClick={() => setPreviewIdx(i => Math.min(selectedList.length - 1, i + 1))}
                          disabled={previewIdx === selectedList.length - 1}
                          className="p-1 text-ink-3 hover:text-ink-0 disabled:opacity-30">
                          <I.Chevron size={14} />
                        </button>
                      </div>
                    )}
                  </div>

                  {!previewLead ? (
                    <p className="f-mono text-xs text-ink-3 italic">Selecciona algun lead per veure el preview.</p>
                  ) : channel === 'WHATSAPP' ? (
                    /* Preview WA */
                    <div className="bg-[#0a1929] rounded-lg p-4 flex-1 min-h-[200px]">
                      <div className="flex justify-end">
                        <div className="bg-[#005c4b] text-white rounded-lg rounded-tr-sm px-3 py-2 max-w-[85%]">
                          <pre className="f-mono text-[12px] whitespace-pre-wrap leading-relaxed">{previewBody}</pre>
                          <div className="f-mono text-[9px] text-white/50 text-right mt-1">ara ✓✓</div>
                        </div>
                      </div>
                    </div>
                  ) : (
                    /* Preview Email */
                    <div className="border border-border-base rounded overflow-hidden flex-1">
                      <div className="bg-bg-1 px-4 py-3 border-b border-border-base space-y-1">
                        <div className="flex gap-2">
                          <span className="f-mono text-[10px] text-ink-3 w-16">De:</span>
                          <span className="f-mono text-[10px] text-ink-1">hola@amgdl.com</span>
                        </div>
                        <div className="flex gap-2">
                          <span className="f-mono text-[10px] text-ink-3 w-16">Per a:</span>
                          <span className="f-mono text-[10px] text-ink-1">{previewLead.email}</span>
                        </div>
                        <div className="flex gap-2">
                          <span className="f-mono text-[10px] text-ink-3 w-16">Assumpte:</span>
                          <span className="f-mono text-[10px] text-accent-light font-bold">{previewSubject}</span>
                        </div>
                      </div>
                      <div className="p-4">
                        <pre className="f-mono text-xs text-ink-1 whitespace-pre-wrap leading-relaxed">{previewBody}</pre>
                      </div>
                    </div>
                  )}
                </>
              )}
            </div>
          </div>

          {/* Footer */}
          <div className="p-4 border-t border-border-base flex items-center justify-between shrink-0">
            <span className="f-mono text-xs text-ink-2">
              Els leads en estat &quot;Nou&quot; passaran a &quot;Contactat&quot; automàticament.
            </span>
            <div className="flex gap-2">
              <AMGButton variant="ghost" onClick={onClose}>Cancel·lar</AMGButton>
              <AMGButton
                icon={channel === 'WHATSAPP' ? I.Smartphone : I.Mail}
                loading={sending}
                disabled={!canSend}
                onClick={() => onSend(selectedTemplate!.id, Array.from(selectedLeads), channel)}
              >
                Enviar a {selectedLeads.size} lead{selectedLeads.size !== 1 ? 's' : ''}
              </AMGButton>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}

function DemoModal({
  lead,
  onClose,
  onCreated,
}: {
  lead: Lead;
  onClose: () => void;
  onCreated: (url: string, token: string) => void;
}) {
  const [companyName, setCompanyName] = useState(lead.name);
  const [sector, setSector] = useState('');
  const [contextEdited, setContextEdited] = useState(false);
  const [agentContext, setAgentContext] = useState(DEFAULT_DEMO_CONTEXT(lead.name));
  const [editToken, setEditToken] = useState('');
  const [url, setUrl] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSectorChange = (s: string) => {
    setSector(s);
    if (!contextEdited) {
      const ctx = getSectorContext(s);
      setAgentContext(ctx ? ctx.demoContext.replace('{NOM_NEGOCI}', companyName) : DEFAULT_DEMO_CONTEXT(companyName));
    }
  };

  const restoreTemplate = () => {
    const ctx = getSectorContext(sector);
    setAgentContext(ctx ? ctx.demoContext.replace('{NOM_NEGOCI}', companyName) : DEFAULT_DEMO_CONTEXT(companyName));
    setContextEdited(false);
  };

  const create = async () => {
    if (!lead.email) return;
    setLoading(true);
    try {
      const session = await createDemoSession(lead.email, companyName, agentContext);
      setUrl(session.url);
      setEditToken(session.token);
      navigator.clipboard.writeText(session.url).catch(() => {});
    } finally {
      setLoading(false);
    }
  };

  const save = async () => {
    if (!editToken) return;
    setLoading(true);
    try {
      await updateDemoSession(editToken, companyName, agentContext);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <div className="fixed inset-0 bg-black/60 z-40" onClick={onClose} />
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div className="bg-bg-0 border border-border-base w-full max-w-lg shadow-2xl max-h-[90vh] flex flex-col">
          <div className="flex items-center justify-between p-5 border-b border-border-base shrink-0">
            <div>
              <div className="f-display font-bold text-base">Crear demo inbox</div>
              <div className="f-mono text-[10px] text-ink-3 mt-0.5">{lead.name} · {lead.email}</div>
            </div>
            <button onClick={onClose} className="p-1.5 text-ink-3 hover:text-ink-0">
              <I.X size={16} />
            </button>
          </div>

          <div className="p-5 space-y-4 overflow-y-auto">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="f-mono text-[10px] uppercase text-ink-3 tracking-wider block mb-1">Nom de l&apos;empresa</label>
                <input
                  type="text"
                  value={companyName}
                  onChange={e => setCompanyName(e.target.value)}
                  className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="f-mono text-[10px] uppercase text-ink-3 tracking-wider block mb-1">Sector</label>
                <select
                  value={sector}
                  onChange={e => handleSectorChange(e.target.value)}
                  className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
                >
                  <option value="">— Selecciona sector —</option>
                  {Object.entries(SECTOR_CONTEXTS).map(([key, ctx]) => (
                    <option key={key} value={key}>{ctx.label}</option>
                  ))}
                </select>
              </div>
            </div>

            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="f-mono text-[10px] uppercase text-ink-3 tracking-wider">Context de l&apos;agent IA</label>
                <div className="flex items-center gap-2">
                  {contextEdited && sector && (
                    <button onClick={restoreTemplate} className="f-mono text-[9px] text-accent-light hover:underline">
                      Restaurar plantilla
                    </button>
                  )}
                  {contextEdited && <span className="f-mono text-[9px] text-warning">· personalitzat</span>}
                </div>
              </div>
              <textarea
                value={agentContext}
                onChange={e => { setAgentContext(e.target.value); setContextEdited(true); }}
                rows={6}
                className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 py-2 f-mono text-xs focus:outline-none focus:border-accent resize-none"
              />
              <div className="f-mono text-[9px] text-ink-3 mt-1">
                {sector ? 'Plantilla carregada del sector. Pots personalitzar-la.' : 'Selecciona un sector per carregar una plantilla automàticament.'}
              </div>
            </div>

            {url && (
              <div className="bg-bg-1 border border-success/30 p-3">
                <div className="f-mono text-[9px] text-success uppercase tracking-wider mb-1">URL creada — copiada al portapapers</div>
                <div className="f-mono text-xs text-ink-1 break-all">{url}</div>
                <button
                  onClick={() => navigator.clipboard.writeText(url)}
                  className="f-mono text-[9px] text-accent-light hover:underline mt-1"
                >
                  Copiar de nou
                </button>
              </div>
            )}
          </div>

          <div className="p-4 border-t border-border-base flex items-center justify-end gap-2 shrink-0">
            <AMGButton variant="ghost" onClick={onClose}>Tancar</AMGButton>
            {url ? (
              <AMGButton loading={loading} onClick={save}>Desar canvis</AMGButton>
            ) : (
              <AMGButton loading={loading} disabled={!lead.email} onClick={create}>Crear demo</AMGButton>
            )}
          </div>
        </div>
      </div>
    </>
  );
}

function OutreachModal({
  leads,
  onClose,
  onSend,
  sending,
}: {
  leads: Lead[];
  onClose: () => void;
  onSend: (req: OutreachRequest) => void;
  sending: boolean;
}) {
  const leadsWithEmail = leads.filter(l => l.email && l.isActive && l.stage !== 'WON' && l.stage !== 'LOST');
  const [selected, setSelected] = useState<Set<string>>(new Set(leadsWithEmail.map(l => l.id)));
  const [language, setLanguage] = useState<'ca' | 'es'>('ca');
  const [demoUrl, setDemoUrl] = useState('');
  const [subject, setSubject] = useState(TEMPLATES.ca.subject);
  const [body, setBody] = useState(TEMPLATES.ca.body);

  const handleLangChange = (lang: 'ca' | 'es') => {
    setLanguage(lang);
    setSubject(TEMPLATES[lang].subject);
    setBody(TEMPLATES[lang].body);
  };

  const toggleAll = () => {
    if (selected.size === leadsWithEmail.length) setSelected(new Set());
    else setSelected(new Set(leadsWithEmail.map(l => l.id)));
  };

  const toggle = (id: string) => {
    const s = new Set(selected);
    s.has(id) ? s.delete(id) : s.add(id);
    setSelected(s);
  };

  // Preview with first selected lead
  const previewLead = leadsWithEmail.find(l => selected.has(l.id));
  const preview = previewLead
    ? { subject: subject.replace('{{nom}}', previewLead.name), body: body.replace(/\{\{nom\}\}/g, previewLead.name).replace(/\{\{demoUrl\}\}/g, demoUrl || '[URL demo]') }
    : null;

  return (
    <>
      <div className="fixed inset-0 bg-black/60 z-40" onClick={onClose} />
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div className="bg-bg-0 border border-border-base w-full max-w-4xl max-h-[90vh] flex flex-col shadow-2xl">
          {/* Header */}
          <div className="flex items-center justify-between p-5 border-b border-border-base shrink-0">
            <div>
              <div className="f-display font-bold text-base">Enviar demo per correu</div>
              <div className="f-mono text-[10px] text-ink-3 mt-0.5">{selected.size} lead{selected.size !== 1 ? 's' : ''} seleccionat{selected.size !== 1 ? 's' : ''}</div>
            </div>
            <button onClick={onClose} className="p-1.5 text-ink-3 hover:text-ink-0 transition-colors">
              <I.X size={16} />
            </button>
          </div>

          <div className="flex flex-1 min-h-0">
            {/* Left: lead list */}
            <div className="w-56 shrink-0 border-r border-border-base flex flex-col">
              <div className="p-3 border-b border-border-base flex items-center justify-between">
                <span className="f-mono text-[10px] uppercase text-ink-3 tracking-widest">Leads</span>
                <button onClick={toggleAll} className="f-mono text-[9px] text-accent-light hover:underline">
                  {selected.size === leadsWithEmail.length ? 'Cap' : 'Tots'}
                </button>
              </div>
              <div className="flex-1 overflow-y-auto">
                {leadsWithEmail.length === 0 ? (
                  <div className="p-4 text-center f-mono text-xs text-ink-3">Cap lead amb email</div>
                ) : leadsWithEmail.map(lead => (
                  <label
                    key={lead.id}
                    className="flex items-start gap-2.5 px-3 py-2.5 cursor-pointer hover:bg-bg-2 border-b border-[rgba(226,232,240,0.04)]"
                  >
                    <input
                      type="checkbox"
                      checked={selected.has(lead.id)}
                      onChange={() => toggle(lead.id)}
                      className="mt-0.5 shrink-0 accent-accent"
                    />
                    <div className="min-w-0">
                      <div className="f-display font-bold text-xs truncate">{lead.name}</div>
                      <div className="f-mono text-[9px] text-ink-3 truncate">{lead.email}</div>
                      <AMGBadge tone={STAGE_TONE[lead.stage] ?? 'neutral'} className="mt-0.5 text-[8px]">
                        {STAGE_LABEL[lead.stage] ?? lead.stage}
                      </AMGBadge>
                    </div>
                  </label>
                ))}
              </div>
            </div>

            {/* Right: compose */}
            <div className="flex-1 flex flex-col min-w-0 overflow-y-auto p-5 space-y-4">
              {/* Language */}
              <div className="flex gap-2">
                {(['ca', 'es'] as const).map(lang => (
                  <button
                    key={lang}
                    onClick={() => handleLangChange(lang)}
                    className={`px-3 py-1.5 f-mono text-xs uppercase tracking-wider border transition-colors ${
                      language === lang
                        ? 'border-accent bg-accent-muted text-accent-light'
                        : 'border-border-base text-ink-2 hover:border-accent/40'
                    }`}
                  >
                    {lang === 'ca' ? 'Català' : 'Castellà'}
                  </button>
                ))}
              </div>

              {/* Demo URL */}
              <div>
                <label className="f-mono text-[10px] uppercase text-ink-3 tracking-wider block mb-1">URL de la Demo</label>
                <input
                  type="url"
                  value={demoUrl}
                  onChange={e => setDemoUrl(e.target.value)}
                  placeholder="https://demo.amgdl.com/..."
                  className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
                />
              </div>

              {/* Subject */}
              <div>
                <label className="f-mono text-[10px] uppercase text-ink-3 tracking-wider block mb-1">
                  Assumpte <span className="normal-case text-ink-3">(&#123;&#123;nom&#125;&#125; = nom de l'empresa)</span>
                </label>
                <input
                  type="text"
                  value={subject}
                  onChange={e => setSubject(e.target.value)}
                  className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
                />
              </div>

              {/* Body */}
              <div className="flex-1">
                <label className="f-mono text-[10px] uppercase text-ink-3 tracking-wider block mb-1">
                  Cos del correu <span className="normal-case text-ink-3">(&#123;&#123;nom&#125;&#125; i &#123;&#123;demoUrl&#125;&#125;)</span>
                </label>
                <textarea
                  value={body}
                  onChange={e => setBody(e.target.value)}
                  rows={10}
                  className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 py-2 f-mono text-xs focus:outline-none focus:border-accent resize-none"
                />
              </div>

              {/* Preview */}
              {preview && (
                <div className="border border-border-subtle p-3 bg-bg-1">
                  <div className="f-mono text-[9px] uppercase text-ink-3 tracking-widest mb-2">Previsualització — {previewLead!.name}</div>
                  <div className="f-mono text-xs text-accent-light mb-1">{preview.subject}</div>
                  <pre className="f-mono text-xs text-ink-1 whitespace-pre-wrap leading-relaxed">{preview.body}</pre>
                </div>
              )}
            </div>
          </div>

          {/* Footer */}
          <div className="p-4 border-t border-border-base flex items-center justify-between shrink-0">
            <span className="f-mono text-xs text-ink-2">
              Els leads en estat "Nou" passaran a "Contactat" automàticament.
            </span>
            <div className="flex gap-2">
              <AMGButton variant="ghost" onClick={onClose}>Cancel·lar</AMGButton>
              <AMGButton
                icon={I.Mail}
                loading={sending}
                disabled={selected.size === 0 || !demoUrl.trim()}
                onClick={() => onSend({ leadIds: Array.from(selected), subject, body, demoUrl, language })}
              >
                Enviar a {selected.size} lead{selected.size !== 1 ? 's' : ''}
              </AMGButton>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}

function fmt(n: number) {
  return `${(n * 100).toFixed(1)}%`;
}

export default function LeadsPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();
  const router = useRouter();
  const params = useParams();
  const locale = params.locale as string;

  const [showOutreach, setShowOutreach] = useState(false);
  const [showTemplateSend, setShowTemplateSend] = useState(false);
  const [demoLead, setDemoLead] = useState<Lead | null>(null);

  const { data: leads = [], isLoading } = useQuery({
    queryKey: ['leads'],
    queryFn: getLeads,
    enabled: !!user,
  });

  const { data: templates = [] } = useQuery({
    queryKey: ['templates'],
    queryFn: listTemplates,
    enabled: !!user,
  });

  const { data: stats } = useQuery({
    queryKey: ['leads-stats'],
    queryFn: getLeadStats,
    enabled: !!user,
  });

  const { mutate: doChangeStage } = useMutation({
    mutationFn: ({ id, stage, lostReason }: { id: string; stage: string; lostReason?: string }) =>
      changeStage(id, stage, lostReason),
    onSuccess: () => {
      toast('success', 'Etapa actualitzada');
      qc.invalidateQueries({ queryKey: ['leads'] });
      qc.invalidateQueries({ queryKey: ['leads-stats'] });
    },
    onError: () => toast('error', 'Error actualitzant etapa'),
  });

  const { mutate: doSetWhatsapp } = useMutation({
    mutationFn: ({ id, value }: { id: string; value: boolean }) => setWhatsapp(id, value),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['leads'] }),
    onError: () => toast('error', 'Error actualitzant WhatsApp'),
  });


  const { mutate: doSendOutreach, isPending: sendingOutreach } = useMutation({
    mutationFn: (req: OutreachRequest) => sendOutreach(req),
    onSuccess: (data) => {
      toast('success', `${data.sent} correu${data.sent !== 1 ? 's' : ''} enviat${data.sent !== 1 ? 's' : ''}`);
      setShowOutreach(false);
      qc.invalidateQueries({ queryKey: ['leads'] });
      qc.invalidateQueries({ queryKey: ['leads-stats'] });
    },
    onError: () => toast('error', 'Error enviant els correus'),
  });

  const { mutate: doSendTemplate, isPending: sendingTemplate } = useMutation({
    mutationFn: ({ templateId, leadIds, channel }: { templateId: string; leadIds: string[]; channel: string }) =>
      sendTemplate({ templateId, leadIds, channel }),
    onSuccess: (data) => {
      toast('success', `${data.sent} missatge${data.sent !== 1 ? 's' : ''} enviat${data.sent !== 1 ? 's' : ''}${data.failed ? ` · ${data.failed} error${data.failed !== 1 ? 's' : ''}` : ''}`);
      setShowTemplateSend(false);
      qc.invalidateQueries({ queryKey: ['leads'] });
      qc.invalidateQueries({ queryKey: ['leads-stats'] });
    },
    onError: () => toast('error', 'Error enviant la plantilla'),
  });

  const leadsByStage = STAGES.reduce<Record<string, Lead[]>>((acc, stage) => {
    acc[stage] = (leads as Lead[]).filter(l => l.stage === stage);
    return acc;
  }, {} as Record<string, Lead[]>);

  return (
    <PortalShell breadcrumb="leads">
      <div className="p-4 sm:p-8 space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / leads /</span>
            <div className="f-display font-bold text-xl mt-1">CRM de Leads</div>
          </div>
          <div className="flex gap-2">
            <AMGButton variant="secondary" onClick={() => router.push(`/${locale}/portal/leads/templates`)}>
              Plantilles
            </AMGButton>
            <AMGButton variant="secondary" icon={I.Mail} onClick={() => setShowOutreach(true)}>
              Enviar demo
            </AMGButton>
            <AMGButton variant="secondary" icon={I.Smartphone} onClick={() => setShowTemplateSend(true)}>
              Enviar plantilla
            </AMGButton>
            <AMGButton icon={I.Plus} onClick={() => router.push(`/${locale}/portal/leads/new`)}>
              Nou Lead
            </AMGButton>
          </div>
        </div>

        {stats && (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
            <AMGStat label="Total leads" value={String(stats.total)} icon={I.Users} tone="accent" />
            <AMGStat label="Guanyats" value={String(stats.byStage?.WON ?? 0)} icon={I.Check} tone="success" />
            <AMGStat label="Perduts" value={String(stats.byStage?.LOST ?? 0)} icon={I.X} tone="danger" />
            <AMGStat label="Conversió" value={fmt(stats.conversionRate)} icon={I.Trending} tone="info" />
          </div>
        )}

        {isLoading ? (
          <div className="flex justify-center py-12">
            <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          </div>
        ) : (
          <div className="overflow-x-auto pb-4">
            <div className="flex gap-3 min-w-max">
              {STAGES.map((stage) => (
                <div key={stage} className="w-[220px] shrink-0">
                  <div className="amg-card card-clip p-3 mb-2 flex items-center justify-between">
                    <AMGBadge tone={STAGE_TONE[stage]}>{STAGE_LABEL[stage]}</AMGBadge>
                    <span className="f-mono text-label text-ink-2">{leadsByStage[stage].length}</span>
                  </div>
                  <div className="space-y-2">
                    {leadsByStage[stage].map((lead) => (
                      <div
                        key={lead.id}
                        className="amg-card card-clip p-3 cursor-pointer hover:border-accent/40 transition-colors"
                        onClick={() => router.push(`/${locale}/portal/leads/${lead.id}`)}
                      >
                        <div className="f-display font-bold text-sm text-ink-0 truncate">{lead.name}</div>
                        {lead.email && <div className="f-mono text-label text-ink-2 mt-1 truncate">{lead.email}</div>}
                        {lead.phone && (
                          <div className="flex items-center gap-1.5 mt-1">
                            <span className="f-mono text-label text-ink-3 truncate">{lead.phone}</span>
                            {lead.hasWhatsapp === true && (
                              <span className="text-[10px] text-success font-bold">WA</span>
                            )}
                          </div>
                        )}
                        <div className="mt-2 flex items-center gap-2 flex-wrap">
                          <AMGBadge tone="neutral">{SOURCE_LABEL[lead.source] ?? lead.source}</AMGBadge>
                        </div>
                        {lead.phone && (
                          <div className="mt-2 flex items-center gap-1.5 flex-wrap">
                            <a
                              href={(() => { const d = lead.phone!.replace(/\D/g, ''); return `https://wa.me/${d.length === 9 ? '34' + d : d}`; })()}
                              target="_blank"
                              rel="noopener noreferrer"
                              onClick={e => e.stopPropagation()}
                              className="f-mono text-[9px] border border-border-base text-ink-2 hover:border-success hover:text-success px-1.5 py-0.5 transition-colors"
                            >
                              Provar WA →
                            </a>
                            <button
                              type="button"
                              className={`f-mono text-[9px] px-1.5 py-0.5 border transition-colors ${
                                lead.hasWhatsapp
                                  ? 'border-success text-success hover:border-danger hover:text-danger-light'
                                  : 'border-border-base text-ink-3 hover:border-success hover:text-success'
                              }`}
                              onClick={e => { e.stopPropagation(); doSetWhatsapp({ id: lead.id, value: !lead.hasWhatsapp }); }}
                            >
                              {lead.hasWhatsapp ? '✓ WA' : 'Marcar WA'}
                            </button>
                          </div>
                        )}
                        {stage !== 'WON' && stage !== 'LOST' && (
                          <div className="mt-2 flex gap-2 flex-wrap">
                            <button
                              type="button"
                              className="f-mono text-[9px] uppercase text-accent-light hover:underline"
                              onClick={e => {
                                e.stopPropagation();
                                const next = STAGES[STAGES.indexOf(stage) + 1];
                                doChangeStage({ id: lead.id, stage: next });
                              }}
                            >
                              {stage === 'NEGOTIATION' ? '✓ Guanyar' : 'Avançar →'}
                            </button>
                            <button
                              type="button"
                              className="f-mono text-[9px] uppercase text-ink-3 hover:text-danger-light hover:underline"
                              onClick={e => {
                                e.stopPropagation();
                                const reason = prompt('Motiu de pèrdua:') ?? 'No especificat';
                                doChangeStage({ id: lead.id, stage: 'LOST', lostReason: reason });
                              }}
                            >
                              Perdut
                            </button>
                            {lead.email && (
                              <button
                                type="button"
                                className="f-mono text-[9px] uppercase text-ink-3 hover:text-accent-light hover:underline"
                                onClick={e => { e.stopPropagation(); setDemoLead(lead); }}
                              >
                                Demo →
                              </button>
                            )}
                          </div>
                        )}
                      </div>
                    ))}
                    {leadsByStage[stage].length === 0 && (
                      <div className="amg-card card-clip p-3 text-center">
                        <span className="f-mono text-label text-ink-3">Cap lead</span>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {showOutreach && typeof document !== 'undefined' && createPortal(
        <OutreachModal
          leads={leads as Lead[]}
          onClose={() => setShowOutreach(false)}
          onSend={(req) => doSendOutreach(req)}
          sending={sendingOutreach}
        />,
        document.body
      )}

      {demoLead && typeof document !== 'undefined' && createPortal(
        <DemoModal
          lead={demoLead}
          onClose={() => setDemoLead(null)}
          onCreated={() => setDemoLead(null)}
        />,
        document.body
      )}

      {showTemplateSend && typeof document !== 'undefined' && createPortal(
        <TemplateSendModal
          leads={leads as Lead[]}
          templates={templates}
          onClose={() => setShowTemplateSend(false)}
          onSend={(templateId, leadIds, channel) => doSendTemplate({ templateId, leadIds, channel })}
          sending={sendingTemplate}
        />,
        document.body
      )}
    </PortalShell>
  );
}
