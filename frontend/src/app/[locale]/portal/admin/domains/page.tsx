'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@/lib/toast-context';
import {
  listAllDomains, checkDomainAvailability, registerDomain, renewDomain,
  configureDns, cancelDomain, listTldPricing,
  STATUS_LABELS, STATUS_TONE,
  type ManagedDomainResponse, type DomainCheckResponse, type TldPricingResponse,
} from '@/services/domainService';
import { listTenants } from '@/services/admin';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGInput } from '@/components/ui/input';
import { AMGSectionTitle } from '@/components/ui/stat';
import { I } from '@/components/ui/icons';

function fmtDate(d: string | null) {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

function daysUntil(d: string | null): number | null {
  if (!d) return null;
  return Math.ceil((new Date(d).getTime() - Date.now()) / 86400000);
}

function StatusBadge({ status }: { status: string }) {
  const tone = STATUS_TONE[status] ?? 'neutral';
  return <AMGBadge tone={tone as any}>{STATUS_LABELS[status] ?? status}</AMGBadge>;
}

function RegisterDomainModal({ onClose, onRegistered }: { onClose: () => void; onRegistered: () => void }) {
  const { toast } = useToast();
  const [step, setStep] = useState<'search' | 'confirm'>('search');
  const [domainInput, setDomainInput] = useState('');
  const [selectedTld, setSelectedTld] = useState('cat');
  const [checkResult, setCheckResult] = useState<DomainCheckResponse | null>(null);
  const [checking, setChecking] = useState(false);
  const [tenantId, setTenantId] = useState('');
  const [autoRenew, setAutoRenew] = useState(true);
  const [registering, setRegistering] = useState(false);

  const { data: tenants } = useQuery({
    queryKey: ['tenants-for-domain'],
    queryFn: () => listTenants({ size: 100 }),
  });

  const { data: tldPricing } = useQuery({
    queryKey: ['tld-pricing'],
    queryFn: listTldPricing,
  });

  const activeTlds = tldPricing?.filter(t => t.isActive).map(t => t.tld) ?? ['cat', 'es', 'com'];

  const handleCheck = async () => {
    if (!domainInput.trim()) return;
    setChecking(true);
    setCheckResult(null);
    try {
      const fullDomain = `${domainInput.trim().toLowerCase()}.${selectedTld}`;
      const result = await checkDomainAvailability(fullDomain);
      setCheckResult(result);
    } catch {
      toast('error', 'Error comprovant disponibilitat');
    } finally {
      setChecking(false);
    }
  };

  const handleRegister = async () => {
    if (!checkResult || !tenantId) return;
    setRegistering(true);
    try {
      await registerDomain({ domainName: checkResult.domainName, tenantId, autoRenew });
      toast('success', `Domini ${checkResult.domainName} registrat correctament`);
      onRegistered();
      onClose();
    } catch (err: any) {
      toast('error', err.message ?? 'Error registrant el domini');
    } finally {
      setRegistering(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-lg p-6 space-y-5" onClick={e => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="f-display font-bold text-base">Registrar domini</div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><I.X size={18} /></button>
        </div>

        {/* Cercador */}
        <div className="space-y-3">
          <div className="flex gap-2">
            <div className="flex-1">
              <AMGInput
                label="Nom del domini"
                value={domainInput}
                onChange={e => { setDomainInput(e.target.value); setCheckResult(null); }}
                placeholder="perruqueria-maria"
                mono
              />
            </div>
            <div className="pt-5">
              <select
                value={selectedTld}
                onChange={e => { setSelectedTld(e.target.value); setCheckResult(null); }}
                className="h-10 bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 text-sm text-ink-1 focus:outline-none focus:border-[#FF6B00]"
              >
                {activeTlds.map(tld => (
                  <option key={tld} value={tld}>.{tld}</option>
                ))}
              </select>
            </div>
          </div>
          <AMGButton onClick={handleCheck} loading={checking} disabled={!domainInput.trim()} className="w-full justify-center">
            Comprovar disponibilitat
          </AMGButton>
        </div>

        {/* Resultat */}
        {checkResult && (
          <div className={`p-4 rounded border ${checkResult.available
            ? 'border-[rgba(34,197,94,0.3)] bg-[rgba(34,197,94,0.06)]'
            : 'border-[rgba(255,68,68,0.3)] bg-[rgba(255,68,68,0.06)]'
          }`}>
            <div className="flex items-center gap-2 mb-2">
              {checkResult.available
                ? <><I.Check size={16} className="text-green-400" /><span className="font-semibold text-green-400">Disponible</span></>
                : <><I.X size={16} className="text-red-400" /><span className="font-semibold text-red-400">No disponible</span></>
              }
              <span className="f-mono text-sm text-ink-1 ml-1">{checkResult.domainName}</span>
            </div>
            {checkResult.available && (
              <div className="flex gap-6 mt-2">
                <div>
                  <div className="f-mono text-[10px] text-ink-3 mb-0.5">Registre (1r any)</div>
                  <div className="f-display font-bold text-lg text-accent-light">{checkResult.saleRegister} €</div>
                </div>
                <div>
                  <div className="f-mono text-[10px] text-ink-3 mb-0.5">Renovació anual</div>
                  <div className="f-display font-bold text-lg text-white">{checkResult.saleRenew} €/any</div>
                </div>
              </div>
            )}
            {!checkResult.available && checkResult.alternatives.length > 0 && (
              <div className="mt-2">
                <div className="f-mono text-[10px] text-ink-3 mb-1">Alternatives disponibles:</div>
                <div className="flex flex-wrap gap-1.5">
                  {checkResult.alternatives.map(alt => (
                    <button key={alt} onClick={() => {
                      const parts = alt.split('.');
                      setDomainInput(parts.slice(0, -1).join('.'));
                      setSelectedTld(parts[parts.length - 1]);
                      setCheckResult(null);
                    }}
                      className="f-mono text-xs px-2 py-1 border border-border-base hover:border-accent-light rounded text-ink-2 hover:text-accent-light transition">
                      {alt}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Configuració si disponible */}
        {checkResult?.available && (
          <div className="space-y-3 pt-2 border-t border-border-base">
            <div className="space-y-2">
              <label className="block text-sm text-ink-2">Tenant</label>
              <select
                value={tenantId}
                onChange={e => setTenantId(e.target.value)}
                className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2.5 text-sm text-ink-1 focus:outline-none focus:border-[#FF6B00]"
              >
                <option value="">— Selecciona el client —</option>
                {tenants?.content.map(t => (
                  <option key={t.id} value={t.id}>{t.name}</option>
                ))}
              </select>
            </div>
            <label className="flex items-center gap-2 cursor-pointer">
              <input type="checkbox" checked={autoRenew} onChange={e => setAutoRenew(e.target.checked)}
                className="w-4 h-4 accent-[#FF6B00]" />
              <span className="text-sm text-ink-2">Renovació automàtica anual</span>
            </label>
            <AMGButton
              variant="primary"
              onClick={handleRegister}
              loading={registering}
              disabled={!tenantId}
              className="w-full justify-center"
            >
              Registrar {checkResult.domainName}
            </AMGButton>
          </div>
        )}
      </div>
    </div>
  );
}

function DomainRow({ domain, onAction }: { domain: ManagedDomainResponse; onAction: () => void }) {
  const { toast } = useToast();
  const days = daysUntil(domain.expiresAt);
  const [loading, setLoading] = useState(false);

  const handleRenew = async () => {
    setLoading(true);
    try {
      await renewDomain(domain.id);
      toast('success', `Domini ${domain.domainName} renovat`);
      onAction();
    } catch { toast('error', 'Error renovant el domini'); }
    finally { setLoading(false); }
  };

  const handleConfigureDns = async () => {
    setLoading(true);
    try {
      await configureDns(domain.id);
      toast('success', 'DNS configurat correctament');
      onAction();
    } catch { toast('error', 'Error configurant DNS'); }
    finally { setLoading(false); }
  };

  return (
    <tr className="border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,255,255,0.02)] transition-colors">
      <td className="px-4 py-3">
        <div className="f-mono font-semibold text-sm text-ink-1">{domain.domainName}</div>
        <div className="f-mono text-[10px] text-ink-3 mt-0.5">.{domain.tld}</div>
      </td>
      <td className="px-4 py-3">
        <StatusBadge status={domain.status} />
      </td>
      <td className="px-4 py-3">
        {domain.dnsConfigured
          ? <span className="f-mono text-xs text-green-400">✓ Configurat</span>
          : <span className="f-mono text-xs text-ink-3">— Pendent</span>}
      </td>
      <td className="px-4 py-3 f-mono text-xs text-ink-2">
        {fmtDate(domain.expiresAt)}
        {days !== null && days <= 30 && (
          <span className={`ml-1 ${days <= 7 ? 'text-red-400' : 'text-yellow-400'}`}>({days}d)</span>
        )}
      </td>
      <td className="px-4 py-3">
        <div className="flex gap-2">
          {!domain.dnsConfigured && domain.status === 'ACTIVE' && (
            <AMGButton size="sm" variant="secondary" onClick={handleConfigureDns} loading={loading}>
              DNS auto
            </AMGButton>
          )}
          {(domain.status === 'EXPIRING_SOON' || (days !== null && days <= 30)) && (
            <AMGButton size="sm" variant="secondary" onClick={handleRenew} loading={loading}>
              Renovar
            </AMGButton>
          )}
        </div>
      </td>
    </tr>
  );
}

export default function DomainsPage() {
  const qc = useQueryClient();
  const [showRegister, setShowRegister] = useState(false);
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ['domains', page],
    queryFn: () => listAllDomains(page, 20),
  });

  const { data: expiring } = useQuery({
    queryKey: ['domains-expiring'],
    queryFn: () => listExpiringDomains(30),
  });

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['domains'] });
    qc.invalidateQueries({ queryKey: ['domains-expiring'] });
  };

  const domains = data?.content ?? [];
  const totalPages = data?.totalPages ?? 1;
  const expiringCount = expiring?.length ?? 0;

  return (
    <PortalShell breadcrumb="admin · dominis">
      <div className="p-4 sm:p-8 space-y-6">
        {/* Header */}
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin /</span>
            <h1 className="f-display font-bold text-xl mt-1">Dominis</h1>
            <p className="text-sm text-ink-2 mt-1">Gestió i venda de dominis per als clients</p>
          </div>
          <AMGButton variant="primary" icon={I.Plus} onClick={() => setShowRegister(true)}>
            Registrar domini
          </AMGButton>
        </div>

        {/* Alerta expiració */}
        {expiringCount > 0 && (
          <div className="flex items-center gap-3 p-4 border border-[rgba(255,107,0,0.35)] bg-[rgba(255,107,0,0.06)] rounded">
            <I.AlertCircle size={18} className="text-accent-light flex-shrink-0" />
            <div className="text-sm text-ink-1">
              <span className="font-semibold text-accent-light">{expiringCount} domini{expiringCount > 1 ? 's' : ''}</span>
              {' '}expira{expiringCount > 1 ? 'n' : ''} en els pròxims 30 dies
            </div>
          </div>
        )}

        {/* Taula */}
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
            <AMGSectionTitle eyebrow="Reseller" title="Tots els dominis" />
            <span className="f-mono text-xs text-ink-3">{data?.totalElements ?? 0} en total</span>
          </div>
          <div className="overflow-x-auto">
            {isLoading ? (
              <div className="flex justify-center py-12">
                <span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
              </div>
            ) : domains.length === 0 ? (
              <div className="p-12 text-center">
                <I.Link size={28} stroke="#64748b" className="mx-auto mb-3" />
                <div className="f-display font-bold text-sm mb-1">Cap domini registrat</div>
                <p className="f-mono text-xs text-ink-2 mb-4">Registra el primer domini per a un client</p>
                <AMGButton size="sm" onClick={() => setShowRegister(true)}>Registrar domini</AMGButton>
              </div>
            ) : (
              <>
                <table className="w-full min-w-[600px]">
                  <thead>
                    <tr className="border-b border-border-base">
                      {['Domini', 'Estat', 'DNS', 'Expira', 'Accions'].map(h => (
                        <th key={h} className="text-left f-mono text-label uppercase text-ink-2 px-4 py-3 font-normal">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {domains.map(d => (
                      <DomainRow key={d.id} domain={d} onAction={invalidate} />
                    ))}
                  </tbody>
                </table>
                {totalPages > 1 && (
                  <div className="flex justify-center gap-2 p-4">
                    <AMGButton size="sm" variant="ghost" disabled={page === 0} onClick={() => setPage(p => p - 1)}>← Anterior</AMGButton>
                    <span className="f-mono text-xs text-ink-3 self-center">{page + 1} / {totalPages}</span>
                    <AMGButton size="sm" variant="ghost" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Següent →</AMGButton>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </div>

      {showRegister && (
        <RegisterDomainModal onClose={() => setShowRegister(false)} onRegistered={invalidate} />
      )}
    </PortalShell>
  );
}

function listExpiringDomains(days: number) {
  return import('@/services/domainService').then(m => m.listExpiringDomains(days));
}
