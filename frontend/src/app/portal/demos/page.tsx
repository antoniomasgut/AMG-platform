'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { IconSet } from '@/components/ui/icons';
import {
  createDemoSession,
  listDemoSessions,
  deactivateDemoSession,
  type DemoSession,
} from '@/services/demo';

const LOCALES: { value: string; label: string }[] = [
  { value: 'ca', label: 'Català' },
  { value: 'es', label: 'Castellà' },
  { value: 'en', label: 'English' },
  { value: 'de', label: 'Deutsch' },
];

const SECTORS: { value: string; label: string }[] = [
  { value: 'PERRUQUERIA', label: 'Perruqueria' },
  { value: 'ESTETICA', label: 'Estètica' },
  { value: 'FISIOTERAPEUTA', label: 'Fisioterapeuta' },
  { value: 'PSICOLEG', label: 'Psicòleg' },
  { value: 'NUTRICIONISTA', label: 'Nutricionista' },
  { value: 'TALLER_MECANIC', label: 'Taller Mecànic' },
  { value: 'VETERINARI', label: 'Veterinari' },
  { value: 'RESTAURANTE', label: 'Restaurant' },
  { value: 'ELECTRICISTA', label: 'Electricista' },
  { value: 'FONTANER', label: 'Fontaner' },
  { value: 'JARDINER', label: 'Jardiner' },
  { value: 'NETEJA', label: 'Neteja' },
  { value: 'GESTORIA', label: 'Gestoria' },
  { value: 'ACADEMIA', label: 'Acadèmia' },
  { value: 'PERRUQUERIA_CANINA', label: 'Perruqueria Canina' },
  { value: 'INMOBILIARIA', label: 'Immobiliària' },
  { value: 'MARE_DE_DIA', label: 'Mare de dia' },
  { value: 'AGENCIA_IA', label: 'Agència IA' },
];

function sessionStatus(s: DemoSession): 'active' | 'expired' | 'deactivated' {
  if (!s.active) return 'deactivated';
  if (new Date(s.expiresAt) < new Date()) return 'expired';
  return 'active';
}

function StatusBadge({ session }: { session: DemoSession }) {
  const st = sessionStatus(session);
  if (st === 'active') return <AMGBadge tone="success">Activa</AMGBadge>;
  if (st === 'expired') return <AMGBadge tone="neutral">Caducada</AMGBadge>;
  return <AMGBadge tone="danger">Desactivada</AMGBadge>;
}

function CopyButton({ url }: { url: string }) {
  const [copied, setCopied] = useState(false);
  const copy = () => {
    navigator.clipboard.writeText(url);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };
  return (
    <button
      onClick={copy}
      className="ml-2 p-1 rounded hover:bg-white/10 transition-colors"
      title="Copiar URL"
    >
      {copied
        ? <IconSet.Check size={14} stroke="#22c55e" />
        : <IconSet.Link size={14} stroke="#94a3b8" />}
    </button>
  );
}

export default function DemosPage() {
  const queryClient = useQueryClient();

  const [sector, setSector] = useState('PERRUQUERIA');
  const [locale, setLocale] = useState('ca');
  const [companyName, setCompanyName] = useState('');
  const [agentContext, setAgentContext] = useState('');
  const [lastCreated, setLastCreated] = useState<DemoSession | null>(null);

  const { data: sessions = [], isLoading } = useQuery({
    queryKey: ['demo-sessions'],
    queryFn: listDemoSessions,
  });

  const createMutation = useMutation({
    mutationFn: createDemoSession,
    onSuccess: (data) => {
      setLastCreated(data);
      setCompanyName('');
      setAgentContext('');
      queryClient.invalidateQueries({ queryKey: ['demo-sessions'] });
    },
  });

  const deactivateMutation = useMutation({
    mutationFn: deactivateDemoSession,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['demo-sessions'] }),
  });

  const handleCreate = () => {
    if (!companyName.trim()) return;
    createMutation.mutate({ sector, companyName: companyName.trim(), agentContext: agentContext.trim() || undefined, locale });
  };

  return (
    <div className="p-4 sm:p-8 space-y-8">

      {/* Header */}
      <div>
        <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / demos /</span>
        <div className="f-display font-bold text-xl mt-1">Sessions de demo</div>
        <p className="text-ui text-ink-1 mt-1">Crea una landing interactiva per sector i envia-la al teu client prospect.</p>
      </div>

      {/* Create form */}
      <div className="amg-card card-clip p-6 space-y-4">
        <div className="f-display font-bold text-base">Nova demo</div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="space-y-1">
            <label className="f-mono text-label text-ink-2 uppercase tracking-wider">Sector</label>
            <select
              value={sector}
              onChange={e => setSector(e.target.value)}
              className="w-full bg-[#0d0d1a] border border-white/10 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-[#FF6B00] transition-colors"
            >
              {SECTORS.map(s => (
                <option key={s.value} value={s.value}>{s.label}</option>
              ))}
            </select>
          </div>

          <div className="space-y-1">
            <label className="f-mono text-label text-ink-2 uppercase tracking-wider">Idioma landing</label>
            <select
              value={locale}
              onChange={e => setLocale(e.target.value)}
              className="w-full bg-[#0d0d1a] border border-white/10 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-[#FF6B00] transition-colors"
            >
              {LOCALES.map(l => (
                <option key={l.value} value={l.value}>{l.label}</option>
              ))}
            </select>
          </div>

          <div className="space-y-1">
            <label className="f-mono text-label text-ink-2 uppercase tracking-wider">Nom de l'empresa</label>
            <input
              type="text"
              value={companyName}
              onChange={e => setCompanyName(e.target.value)}
              placeholder="ex. Perruqueria Mireia"
              className="w-full bg-[#0d0d1a] border border-white/10 rounded-lg px-3 py-2 text-sm text-white placeholder-white/30 focus:outline-none focus:border-[#FF6B00] transition-colors"
            />
          </div>
        </div>

        <div className="space-y-1">
          <label className="f-mono text-label text-ink-2 uppercase tracking-wider">Context addicional per l'agent <span className="normal-case text-ink-3">(opcional)</span></label>
          <textarea
            value={agentContext}
            onChange={e => setAgentContext(e.target.value)}
            placeholder="ex. Especialitzats en coloració i tractaments capil·lars. Oberts de dilluns a dissabte."
            rows={2}
            className="w-full bg-[#0d0d1a] border border-white/10 rounded-lg px-3 py-2 text-sm text-white placeholder-white/30 focus:outline-none focus:border-[#FF6B00] transition-colors resize-none"
          />
        </div>

        <div className="flex items-center justify-between">
          <AMGButton
            size="sm"
            icon={IconSet.Play}
            onClick={handleCreate}
            disabled={!companyName.trim() || createMutation.isPending}
          >
            {createMutation.isPending ? 'Creant…' : 'Crear demo'}
          </AMGButton>
          {createMutation.isError && (
            <span className="text-sm text-red-400">Error en crear la demo</span>
          )}
        </div>

        {/* Success banner */}
        {lastCreated && (
          <div className="bg-green-950/40 border border-green-500/30 rounded-lg p-4 space-y-2">
            <div className="flex items-center gap-2 text-green-400 text-sm font-medium">
              <IconSet.Check size={16} stroke="#4ade80" />
              Demo creada correctament · 24h de durada
            </div>
            <div className="flex items-center gap-2">
              <span className="f-mono text-caption text-ink-2 break-all">{lastCreated.landingUrl}</span>
              <CopyButton url={lastCreated.landingUrl} />
              <a
                href={lastCreated.landingUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="shrink-0"
              >
                <IconSet.Globe size={14} stroke="#94a3b8" />
              </a>
            </div>
            <button
              onClick={() => setLastCreated(null)}
              className="text-xs text-ink-3 hover:text-ink-1 transition-colors"
            >
              Tancar
            </button>
          </div>
        )}
      </div>

      {/* Sessions list */}
      <div className="space-y-3">
        <div className="f-display font-bold text-base">Sessions existents</div>

        {isLoading ? (
          <div className="flex justify-center py-12">
            <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
          </div>
        ) : sessions.length === 0 ? (
          <div className="amg-card card-clip p-8 text-center">
            <IconSet.Bot size={32} stroke="#64748b" className="mx-auto mb-3" />
            <div className="f-display font-bold text-base mb-1">Cap sessió creada</div>
            <p className="text-ui text-ink-1">Crea la teva primera demo per enviar-la a un client</p>
          </div>
        ) : (
          <div className="space-y-2">
            {sessions.map(s => {
              const st = sessionStatus(s);
              const sectorLabel = SECTORS.find(x => x.value === s.sector)?.label ?? s.sector;
              return (
                <div key={s.token} className="amg-card card-clip p-4 flex items-center gap-4">
                  <div className="flex-1 min-w-0 space-y-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="f-display font-bold text-sm">{s.companyName}</span>
                      <AMGBadge tone="neutral">{sectorLabel}</AMGBadge>
                      <AMGBadge tone="info">{(s.locale || 'ca').toUpperCase()}</AMGBadge>
                      <StatusBadge session={s} />
                    </div>
                    <div className="flex items-center gap-1">
                      <span className="f-mono text-caption text-ink-3 truncate">{s.landingUrl}</span>
                      <CopyButton url={s.landingUrl} />
                    </div>
                    <div className="f-mono text-caption text-ink-3">
                      Caduca: {new Date(s.expiresAt).toLocaleString('ca-ES', { dateStyle: 'short', timeStyle: 'short' })}
                    </div>
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    <a href={s.landingUrl} target="_blank" rel="noopener noreferrer">
                      <AMGButton size="sm" variant="ghost" icon={IconSet.Eye}>
                        Veure
                      </AMGButton>
                    </a>
                    {st === 'active' && (
                      <AMGButton
                        size="sm"
                        variant="ghost"
                        icon={IconSet.X}
                        onClick={() => deactivateMutation.mutate(s.token)}
                        disabled={deactivateMutation.isPending}
                      >
                        Desactivar
                      </AMGButton>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
