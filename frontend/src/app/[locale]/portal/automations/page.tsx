'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import {
  listTemplates, listWorkflows, assignWorkflow, deployWorkflow,
  activateWorkflow, deactivateWorkflow, deleteWorkflow,
  type WorkflowResponse, type TemplateResponse,
} from '@/services/automations';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGSectionTitle } from '@/components/ui/stat';
import { I } from '@/components/ui/icons';

const STATUS_TONE: Record<string, 'success' | 'warning' | 'danger' | 'accent' | 'neutral'> = {
  ACTIVE: 'success', INACTIVE: 'neutral', DEPLOYED: 'info' as 'accent',
  DRAFT: 'accent', ERROR: 'danger', PENDING: 'warning',
};

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: 'Actiu', INACTIVE: 'Inactiu', DEPLOYED: 'Desplegat',
  DRAFT: 'Esborrany', ERROR: 'Error', PENDING: 'Pendent',
};

function fmtDate(d: string | null) {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short' });
}

export default function AutomationsPage() {
  const { user, isAdmin } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();
  const [showTemplates, setShowTemplates] = useState(false);

  const tenantId = user?.tenantId ?? '';

  const { data: workflows = [], isLoading: loadingWf } = useQuery({
    queryKey: ['workflows', tenantId],
    queryFn: async () => {
      const res = await listWorkflows(tenantId);
      return res.content;
    },
    enabled: !!tenantId,
  });

  const { data: templates = [], isLoading: loadingTpl } = useQuery({
    queryKey: ['templates'],
    queryFn: listTemplates,
    enabled: isAdmin && showTemplates,
  });

  const invalidateWf = () => qc.invalidateQueries({ queryKey: ['workflows'] });

  const { mutate: doAssign, isPending: assigning } = useMutation({
    mutationFn: (templateKey: string) => assignWorkflow(tenantId, templateKey),
    onSuccess: () => { toast('success', 'Workflow assignat'); invalidateWf(); setShowTemplates(false); },
    onError: () => toast('error', 'Error assignant el workflow'),
  });

  const { mutate: doDeploy } = useMutation({
    mutationFn: deployWorkflow,
    onSuccess: () => { toast('success', 'Workflow desplegat'); invalidateWf(); },
    onError: () => toast('error', 'Error desplegant el workflow'),
  });

  const { mutate: doActivate } = useMutation({
    mutationFn: activateWorkflow,
    onSuccess: () => { toast('success', 'Workflow activat'); invalidateWf(); },
    onError: () => toast('error', 'Error activant el workflow'),
  });

  const { mutate: doDeactivate } = useMutation({
    mutationFn: deactivateWorkflow,
    onSuccess: () => { toast('success', 'Workflow desactivat'); invalidateWf(); },
    onError: () => toast('error', 'Error desactivant el workflow'),
  });

  const { mutate: doDelete } = useMutation({
    mutationFn: deleteWorkflow,
    onSuccess: () => { toast('success', 'Workflow eliminat'); invalidateWf(); },
    onError: () => toast('error', 'Error eliminant el workflow'),
  });

  return (
    <PortalShell breadcrumb="automatitzacions">
      <div className="p-4 sm:p-8 space-y-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / automatitzacions /</span>
            <div className="f-display font-bold text-xl mt-1">Automatitzacions n8n</div>
          </div>
          {isAdmin && (
            <AMGButton size="sm" icon={I.Plus} onClick={() => setShowTemplates((v) => !v)}>
              {showTemplates ? 'Tancar' : 'Afegir workflow'}
            </AMGButton>
          )}
        </div>

        {showTemplates && isAdmin && (
          <div className="amg-card card-clip p-4 sm:p-5">
            <AMGSectionTitle eyebrow="Catàleg" title="Plantilles disponibles" />
            {loadingTpl ? (
              <div className="flex justify-center py-8">
                <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                {templates.map((tpl: TemplateResponse) => (
                  <div key={tpl.key} className="amg-card card-clip p-4 border border-border-base">
                    <div className="flex items-start justify-between gap-2 mb-2">
                      <div className="f-display font-bold text-sm">{tpl.name}</div>
                      <AMGBadge tone="accent">{tpl.category}</AMGBadge>
                    </div>
                    <p className="text-ui text-ink-2 mb-4 text-xs">{tpl.description}</p>
                    <AMGButton size="sm" variant="secondary" icon={I.Plus} disabled={assigning} onClick={() => doAssign(tpl.key)}>
                      Assignar
                    </AMGButton>
                  </div>
                ))}
                {templates.length === 0 && (
                  <div className="col-span-3 py-6 text-center f-mono text-label text-ink-2 uppercase">
                    Cap plantilla disponible
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base">
            <AMGSectionTitle eyebrow="Workflows actius" title="Els meus workflows" />
          </div>

          {loadingWf ? (
            <div className="flex justify-center py-12">
              <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
            </div>
          ) : workflows.length === 0 ? (
            <div className="p-8 text-center">
              <I.Zap size={28} stroke="#64748b" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Cap workflow configurat</div>
              <p className="text-ui text-ink-2">
                {isAdmin ? 'Afegeix un workflow des del catàleg de plantilles' : 'El teu tècnic configurarà els workflows per a tu'}
              </p>
            </div>
          ) : (
            <div className="divide-y divide-[rgba(226,232,240,0.04)]">
              {workflows.map((wf: WorkflowResponse) => (
                <div key={wf.id} className="p-4 sm:p-5 flex flex-wrap items-center gap-4">
                  <div className="flex items-center gap-3 flex-1 min-w-0">
                    <div className="w-9 h-9 bg-accent-muted border border-border-strong flex items-center justify-center shrink-0">
                      <I.Zap size={14} stroke="#FF9A3C" />
                    </div>
                    <div className="min-w-0">
                      <div className="f-display font-bold text-sm truncate">{wf.templateName}</div>
                      <div className="f-mono text-label text-ink-2 flex items-center gap-2 mt-0.5">
                        <AMGBadge tone={STATUS_TONE[wf.status] ?? 'neutral'}>
                          {STATUS_LABEL[wf.status] ?? wf.status}
                        </AMGBadge>
                        {wf.lastRunAt && (
                          <span className="text-ink-3">Darrera execució: {fmtDate(wf.lastRunAt)}</span>
                        )}
                      </div>
                      {wf.errorMessage && (
                        <div className="f-mono text-label text-danger-light mt-1 truncate">{wf.errorMessage}</div>
                      )}
                    </div>
                  </div>

                  <div className="flex gap-2 flex-wrap">
                    {wf.status === 'DRAFT' && (
                      <AMGButton size="sm" variant="secondary" icon={I.Upload} onClick={() => doDeploy(wf.id)}>
                        Desplegar
                      </AMGButton>
                    )}
                    {wf.status === 'DEPLOYED' && (
                      <AMGButton size="sm" icon={I.Play} onClick={() => doActivate(wf.id)}>
                        Activar
                      </AMGButton>
                    )}
                    {wf.status === 'ACTIVE' && (
                      <AMGButton size="sm" variant="outline" onClick={() => doDeactivate(wf.id)}>
                        Desactivar
                      </AMGButton>
                    )}
                    {wf.n8nWebhookUrl && (
                      <a href={wf.n8nWebhookUrl} target="_blank" rel="noopener">
                        <AMGButton size="sm" variant="ghost" icon={I.Link}>n8n</AMGButton>
                      </a>
                    )}
                    {isAdmin && (
                      <AMGButton size="sm" variant="ghost" icon={I.Trash} onClick={() => doDelete(wf.id)} />
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </PortalShell>
  );
}
