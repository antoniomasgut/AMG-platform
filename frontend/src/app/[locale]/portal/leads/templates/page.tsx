'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@/lib/toast-context';
import {
  listTemplates,
  createTemplate,
  updateTemplate,
  deleteTemplate,
  type MessageTemplate,
  type TemplateType,
  type TemplateRequest,
} from '@/services/message-templates';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { I } from '@/components/ui/icons';
import { useRouter } from 'next/navigation';
import { useParams } from 'next/navigation';

const TYPE_LABEL: Record<TemplateType, string> = {
  WHATSAPP: 'WhatsApp',
  EMAIL: 'Email',
  CALL_SCRIPT: 'Guió de trucada',
};

const TYPE_COLOR: Record<TemplateType, string> = {
  WHATSAPP: 'text-green-400 border-green-400/30 bg-green-400/5',
  EMAIL: 'text-accent-light border-accent/30 bg-accent/5',
  CALL_SCRIPT: 'text-orange-400 border-orange-400/30 bg-orange-400/5',
};

const TYPES: TemplateType[] = ['WHATSAPP', 'EMAIL', 'CALL_SCRIPT'];

const VARIABLES_HINT = '{{nom}}, {{telefon}}, {{email}}, {{empresa}}';

interface TemplateFormProps {
  initial?: MessageTemplate;
  onSave: (req: TemplateRequest) => void;
  onCancel: () => void;
  saving: boolean;
}

function TemplateForm({ initial, onSave, onCancel, saving }: TemplateFormProps) {
  const [name, setName] = useState(initial?.name ?? '');
  const [type, setType] = useState<TemplateType>(initial?.type ?? 'WHATSAPP');
  const [subject, setSubject] = useState(initial?.subject ?? '');
  const [body, setBody] = useState(initial?.body ?? '');

  const submit = () => {
    if (!name.trim() || !body.trim()) return;
    onSave({ name: name.trim(), type, subject: subject.trim() || undefined, body: body.trim() });
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
      <div className="bg-bg-0 border border-border-base w-full max-w-2xl max-h-[90vh] flex flex-col shadow-2xl">
        <div className="flex items-center justify-between p-5 border-b border-border-base shrink-0">
          <div className="f-display font-bold text-base">
            {initial ? 'Editar plantilla' : 'Nova plantilla'}
          </div>
          <button onClick={onCancel} className="p-1.5 text-ink-3 hover:text-ink-0 transition-colors">
            <I.X size={16} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-5 space-y-4">
          {/* Name */}
          <div>
            <label className="f-mono text-[10px] uppercase text-ink-3 tracking-wider block mb-1">Nom</label>
            <input
              type="text"
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="Ex: Introducció WhatsApp"
              className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
            />
          </div>

          {/* Type */}
          <div>
            <label className="f-mono text-[10px] uppercase text-ink-3 tracking-wider block mb-1">Tipus</label>
            <div className="flex gap-2">
              {TYPES.map(t => (
                <button
                  key={t}
                  onClick={() => setType(t)}
                  className={`px-3 py-1.5 f-mono text-xs border transition-colors ${
                    type === t
                      ? 'border-accent bg-accent-muted text-accent-light'
                      : 'border-border-base text-ink-2 hover:border-accent/40'
                  }`}
                >
                  {TYPE_LABEL[t]}
                </button>
              ))}
            </div>
          </div>

          {/* Subject (email only) */}
          {type === 'EMAIL' && (
            <div>
              <label className="f-mono text-[10px] uppercase text-ink-3 tracking-wider block mb-1">Assumpte</label>
              <input
                type="text"
                value={subject}
                onChange={e => setSubject(e.target.value)}
                placeholder="Ex: {{nom}} — Som AMG Digitalització"
                className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
              />
            </div>
          )}

          {/* Body */}
          <div>
            <label className="f-mono text-[10px] uppercase text-ink-3 tracking-wider block mb-1">
              Cos del missatge{' '}
              <span className="normal-case text-ink-3">({VARIABLES_HINT})</span>
            </label>
            <textarea
              value={body}
              onChange={e => setBody(e.target.value)}
              rows={10}
              placeholder="Escriu el missatge aquí..."
              className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 py-2 f-mono text-xs focus:outline-none focus:border-accent resize-none"
            />
          </div>
        </div>

        <div className="flex justify-end gap-2 p-4 border-t border-border-base shrink-0">
          <AMGButton variant="secondary" onClick={onCancel}>Cancel·lar</AMGButton>
          <AMGButton onClick={submit} disabled={!name.trim() || !body.trim() || saving}>
            {saving ? 'Desant...' : 'Desar'}
          </AMGButton>
        </div>
      </div>
    </div>
  );
}

export default function TemplatesPage() {
  const router = useRouter();
  const params = useParams();
  const locale = params.locale as string;
  const qc = useQueryClient();
  const { toast } = useToast();

  const [editTarget, setEditTarget] = useState<MessageTemplate | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [deleteId, setDeleteId] = useState<string | null>(null);

  const { data: templates = [], isLoading } = useQuery({
    queryKey: ['message-templates'],
    queryFn: listTemplates,
  });

  const { mutate: doCreate, isPending: creating } = useMutation({
    mutationFn: (req: TemplateRequest) => createTemplate(req),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['message-templates'] });
      setShowCreate(false);
      toast('success', 'Plantilla creada');
    },
    onError: () => toast('error', 'Error creant la plantilla'),
  });

  const { mutate: doUpdate, isPending: updating } = useMutation({
    mutationFn: ({ id, req }: { id: string; req: TemplateRequest }) => updateTemplate(id, req),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['message-templates'] });
      setEditTarget(null);
      toast('success', 'Plantilla actualitzada');
    },
    onError: () => toast('error', 'Error actualitzant la plantilla'),
  });

  const { mutate: doDelete } = useMutation({
    mutationFn: (id: string) => deleteTemplate(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['message-templates'] });
      setDeleteId(null);
      toast('success', 'Plantilla eliminada');
    },
    onError: () => toast('error', 'Error eliminant la plantilla'),
  });

  const byType = TYPES.reduce<Record<TemplateType, MessageTemplate[]>>((acc, t) => {
    acc[t] = templates.filter(tpl => tpl.type === t);
    return acc;
  }, {} as Record<TemplateType, MessageTemplate[]>);

  return (
    <PortalShell breadcrumb="leads">
      <div className="p-4 sm:p-8 space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / leads / plantilles /</span>
            <div className="f-display font-bold text-xl mt-1">Plantilles de missatges</div>
            <div className="f-mono text-xs text-ink-3 mt-1">
              Variables disponibles: {VARIABLES_HINT}
            </div>
          </div>
          <div className="flex gap-2">
            <AMGButton variant="secondary" onClick={() => router.push(`/${locale}/portal/leads`)}>
              ← Leads
            </AMGButton>
            <AMGButton icon={I.Plus} onClick={() => setShowCreate(true)}>
              Nova plantilla
            </AMGButton>
          </div>
        </div>

        {isLoading ? (
          <div className="flex justify-center py-12">
            <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          </div>
        ) : (
          <div className="space-y-8">
            {TYPES.map(type => (
              <div key={type}>
                <div className="flex items-center gap-3 mb-3">
                  <span className={`f-mono text-[10px] uppercase tracking-widest px-2 py-0.5 border ${TYPE_COLOR[type]}`}>
                    {TYPE_LABEL[type]}
                  </span>
                  <span className="f-mono text-[10px] text-ink-3">{byType[type].length} plantilla{byType[type].length !== 1 ? 'es' : ''}</span>
                </div>

                {byType[type].length === 0 ? (
                  <div className="border border-dashed border-border-base p-6 text-center">
                    <p className="f-mono text-xs text-ink-3">Cap plantilla de tipus {TYPE_LABEL[type]}</p>
                    <button
                      onClick={() => setShowCreate(true)}
                      className="mt-2 f-mono text-xs text-accent-light hover:underline"
                    >
                      + Crear-ne una
                    </button>
                  </div>
                ) : (
                  <div className="space-y-2">
                    {byType[type].map(tpl => (
                      <div
                        key={tpl.id}
                        className="bg-bg-1 border border-border-base p-4 flex items-start justify-between gap-4 hover:border-accent/40 transition-colors"
                      >
                        <div className="min-w-0 flex-1">
                          <div className="f-display font-bold text-sm">{tpl.name}</div>
                          {tpl.subject && (
                            <div className="f-mono text-xs text-ink-2 mt-0.5">Assumpte: {tpl.subject}</div>
                          )}
                          <p className="f-mono text-xs text-ink-3 mt-1.5 whitespace-pre-wrap line-clamp-3">{tpl.body}</p>
                        </div>
                        <div className="flex gap-1.5 shrink-0">
                          <button
                            onClick={() => setEditTarget(tpl)}
                            className="p-1.5 text-ink-3 hover:text-accent-light transition-colors"
                            title="Editar"
                          >
                            <I.Edit size={14} />
                          </button>
                          <button
                            onClick={() => setDeleteId(tpl.id)}
                            className="p-1.5 text-ink-3 hover:text-red-400 transition-colors"
                            title="Eliminar"
                          >
                            <I.Trash size={14} />
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Create modal */}
      {showCreate && (
        <TemplateForm
          onSave={req => doCreate(req)}
          onCancel={() => setShowCreate(false)}
          saving={creating}
        />
      )}

      {/* Edit modal */}
      {editTarget && (
        <TemplateForm
          initial={editTarget}
          onSave={req => doUpdate({ id: editTarget.id, req })}
          onCancel={() => setEditTarget(null)}
          saving={updating}
        />
      )}

      {/* Delete confirm */}
      {deleteId && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
          <div className="bg-bg-0 border border-border-base w-full max-w-sm p-6 shadow-2xl">
            <div className="f-display font-bold text-base mb-2">Eliminar plantilla?</div>
            <p className="f-mono text-xs text-ink-3 mb-5">Aquesta acció no es pot desfer.</p>
            <div className="flex justify-end gap-2">
              <AMGButton variant="secondary" onClick={() => setDeleteId(null)}>Cancel·lar</AMGButton>
              <AMGButton
                onClick={() => doDelete(deleteId)}
                className="bg-red-500/20 text-red-400 border-red-500/30 hover:bg-red-500/30"
              >
                Eliminar
              </AMGButton>
            </div>
          </div>
        </div>
      )}
    </PortalShell>
  );
}
