'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import { getSystemConfig, setSystemConfig, deleteSystemConfig, testSystemConfig, TESTABLE_KEYS, type ConfigStatus } from '@/services/sysconfig';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { I } from '@/components/ui/icons';

const CATEGORY_LABEL: Record<string, string> = {
  AGENTS: 'Agents IA',
  INFRAOPS: 'InfraOps',
  PROSPECTING: 'Prospecció',
  PAYMENTS: 'Pagaments',
  FINOPS: 'FinOps',
  BACKUP: 'Backup',
  AUTOMATIONS: 'Automatitzacions',
};

function StatusBadge({ status }: { status: ConfigStatus }) {
  if (status.configured && status.source === 'ENV') {
    return <AMGBadge tone="success">Variable d'entorn</AMGBadge>;
  }
  if (status.configured && status.source === 'DB') {
    return <AMGBadge tone="info">Configurat (DB)</AMGBadge>;
  }
  return <AMGBadge tone="danger">No configurat</AMGBadge>;
}

function KeyRow({ item, onSave, onDelete }: {
  item: ConfigStatus;
  onSave: (key: string, value: string) => void;
  onDelete: (key: string) => void;
}) {
  const [editing, setEditing] = useState(false);
  const [value, setValue] = useState('');
  const [show, setShow] = useState(false);
  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState<{ ok: boolean; message: string } | null>(null);

  const handleTest = async () => {
    setTesting(true);
    setTestResult(null);
    try {
      const res = await testSystemConfig(item.key);
      setTestResult(res);
    } catch {
      setTestResult({ ok: false, message: 'Error connectant amb el servidor.' });
    } finally {
      setTesting(false);
    }
  };

  return (
    <div className={`px-4 sm:px-5 py-4 border-b border-border-base last:border-0 ${!item.configured ? 'bg-danger/[0.03]' : ''}`}>
      <div className="flex items-start gap-3">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap mb-0.5">
            <span className="f-display font-bold text-sm">{item.label}</span>
            <AMGBadge tone="neutral">{CATEGORY_LABEL[item.category] ?? item.category}</AMGBadge>
            <StatusBadge status={item} />
          </div>
          <p className="f-mono text-label text-ink-2 text-xs mt-0.5">{item.description}</p>
          <p className="f-mono text-label text-ink-3 text-xs mt-0.5 opacity-60">{item.key}</p>
        </div>

        <div className="flex gap-2 shrink-0 flex-wrap justify-end">
          {item.configured && TESTABLE_KEYS.has(item.key) && (
            <AMGButton size="sm" variant="secondary" onClick={handleTest} disabled={testing}>
              {testing ? '…' : 'Provar'}
            </AMGButton>
          )}
          {!item.configured && (
            <AMGButton size="sm" icon={I.Plus} onClick={() => setEditing(true)}>
              Configurar
            </AMGButton>
          )}
          {item.configured && item.source === 'DB' && (
            <>
              <AMGButton size="sm" variant="secondary" icon={I.Edit} onClick={() => setEditing(true)}>
                Editar
              </AMGButton>
              <AMGButton
                size="sm"
                variant="ghost"
                icon={I.Trash}
                onClick={() => { if (confirm(`Eliminar la clau ${item.key}?`)) onDelete(item.key); }}
              />
            </>
          )}
          {item.configured && item.source === 'ENV' && (
            <span className="f-mono text-label text-xs text-ink-3 self-center">Llegida d'entorn</span>
          )}
        </div>
      </div>

      {testResult && (
        <div className={`mt-3 p-3 rounded text-xs f-mono ${testResult.ok ? 'bg-success/10 text-success border border-success/30' : 'bg-danger/10 text-danger border border-danger/30'}`}>
          {testResult.ok ? '✓ ' : '✗ '}{testResult.message}
        </div>
      )}

      {editing && (
        <div className="mt-3 flex gap-2 items-start">
          <div className="flex-1 relative">
            <input
              type={item.secret && !show ? 'password' : 'text'}
              value={value}
              onChange={(e) => setValue(e.target.value)}
              placeholder={`Introdueix ${item.label}…`}
              className="w-full bg-bg-1 border border-accent text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none pr-9"
              autoFocus
            />
            {item.secret && (
              <button
                type="button"
                onClick={() => setShow(!show)}
                className="absolute right-2 top-1/2 -translate-y-1/2 text-ink-3 hover:text-ink-1"
              >
                {show ? <I.EyeOff size={14} /> : <I.Eye size={14} />}
              </button>
            )}
          </div>
          <AMGButton
            size="sm"
            icon={I.Check}
            disabled={!value.trim()}
            onClick={() => { onSave(item.key, value.trim()); setEditing(false); setValue(''); }}
          >
            Desar
          </AMGButton>
          <AMGButton size="sm" variant="ghost" onClick={() => { setEditing(false); setValue(''); }}>
            Cancel·lar
          </AMGButton>
        </div>
      )}
    </div>
  );
}

export default function SystemConfigPage() {
  const { user, isSuperAdmin } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();

  const { data: configs = [], isLoading } = useQuery({
    queryKey: ['system-config'],
    queryFn: getSystemConfig,
    enabled: !!user && isSuperAdmin,
  });

  const { mutate: doSave } = useMutation({
    mutationFn: ({ key, value }: { key: string; value: string }) => setSystemConfig(key, value),
    onSuccess: (_, { key }) => {
      toast('success', `Clau ${key} desada correctament`);
      qc.invalidateQueries({ queryKey: ['system-config'] });
    },
    onError: () => toast('error', 'Error desant la clau'),
  });

  const { mutate: doDelete } = useMutation({
    mutationFn: (key: string) => deleteSystemConfig(key),
    onSuccess: (_, key) => {
      toast('success', `Clau ${key} eliminada`);
      qc.invalidateQueries({ queryKey: ['system-config'] });
    },
    onError: () => toast('error', 'Error eliminant la clau'),
  });

  if (!user || !isSuperAdmin) return null;

  const list = configs as ConfigStatus[];
  const missing = list.filter((c) => !c.configured).length;

  // Group by category
  const byCategory = list.reduce<Record<string, ConfigStatus[]>>((acc, c) => {
    const cat = c.category;
    if (!acc[cat]) acc[cat] = [];
    acc[cat].push(c);
    return acc;
  }, {});

  return (
    <PortalShell breadcrumb="admin / config">
      <div className="p-4 sm:p-8 space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / config /</span>
            <div className="flex items-center gap-3 mt-1">
              <div className="f-display font-bold text-xl">API Keys del Sistema</div>
              {missing > 0 && <AMGBadge tone="danger">{missing} no configurades</AMGBadge>}
              {missing === 0 && list.length > 0 && <AMGBadge tone="success">Tot configurat</AMGBadge>}
            </div>
          </div>
        </div>

        <div className="amg-card card-clip p-4 border-l-2 border-l-accent bg-accent/5">
          <p className="f-mono text-label text-ink-1 text-xs leading-relaxed">
            Les claus d'API s'emmagatzemen xifrades (AES-256) a la base de dades.
            Les variables d'entorn tenen prioritat sobre les claus de la BD.
            Canviar una clau aquí no requereix reiniciar el servidor.
          </p>
        </div>

        {isLoading ? (
          <div className="flex justify-center py-12">
            <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          </div>
        ) : (
          Object.entries(byCategory).map(([category, items]) => (
            <div key={category} className="amg-card card-clip">
              <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
                <div className="f-mono text-label uppercase text-ink-2 tracking-widest">
                  {CATEGORY_LABEL[category] ?? category}
                </div>
                <AMGBadge tone={items.every((i) => i.configured) ? 'success' : 'warning'}>
                  {items.filter((i) => i.configured).length}/{items.length}
                </AMGBadge>
              </div>
              <div>
                {items.map((item) => (
                  <KeyRow
                    key={item.key}
                    item={item}
                    onSave={(key, value) => doSave({ key, value })}
                    onDelete={(key) => doDelete(key)}
                  />
                ))}
              </div>
            </div>
          ))
        )}
      </div>
    </PortalShell>
  );
}
