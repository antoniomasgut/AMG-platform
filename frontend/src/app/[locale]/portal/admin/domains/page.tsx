'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@/lib/toast-context';
import {
  listAllDomains, checkDomainAvailability, registerDomain, renewDomain,
  configureDns, cancelDomain, listTldPricing, addExternalDomain,
  assignAmgSubdomain, transferDomain, verifyDns,
  STATUS_LABELS, STATUS_TONE, SCENARIO_LABELS, SCENARIO_TONE,
  type ManagedDomainResponse, type DomainCheckResponse, type TldPricingResponse,
} from '@/services/domainService';
import { listTenants } from '@/services/admin';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { AMGInput } from '@/components/ui/input';
import { AMGSectionTitle } from '@/components/ui/stat';
import { IconSet } from '@/components/ui/icons';

// ── Utilitats ──────────────────────────────────────────────────────────────

const AMG_SERVER_IP = '65.108.148.62';

function fmtDate(d: string | null) {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}
function daysUntil(d: string | null): number | null {
  if (!d) return null;
  return Math.ceil((new Date(d).getTime() - Date.now()) / 86400000);
}

// ── Selecció d'escenari ──────────────────────────────────────────────────

type Scenario = 'OPENPROVIDER_NEW' | 'OPENPROVIDER_TRANSFER' | 'EXTERNAL_OWN' | 'EXTERNAL_SUBDOMAIN' | 'AMG_SUBDOMAIN';

const SCENARIOS: Array<{ id: Scenario; icon: React.ReactNode; title: string; subtitle: string }> = [
  {
    id: 'AMG_SUBDOMAIN',
    icon: <IconSet.Globe size={20} />,
    title: 'Subdomini AMG',
    subtitle: 'Assigna nom.amgdl.com al client. Immediat, sense cost.',
  },
  {
    id: 'EXTERNAL_OWN',
    icon: <IconSet.Link size={20} />,
    title: 'Domini extern propi',
    subtitle: 'El client té un domini a GoDaddy, Namecheap... Configura DNS per apuntar-lo.',
  },
  {
    id: 'EXTERNAL_SUBDOMAIN',
    icon: <IconSet.Link size={20} />,
    title: 'Subdomini extern',
    subtitle: 'El client afegeix carta.negoci.com. Només cal un CNAME.',
  },
  {
    id: 'OPENPROVIDER_NEW',
    icon: <IconSet.Plus size={20} />,
    title: 'Registre nou (OpenProvider)',
    subtitle: 'AMG registra el domini i gestiona tot el cicle de vida.',
  },
  {
    id: 'OPENPROVIDER_TRANSFER',
    icon: <IconSet.ArrowRight size={20} />,
    title: 'Transferència (OpenProvider)',
    subtitle: 'El client vol transferir el seu domini cap a AMG. Cal el codi EPP.',
  },
];

// ── DNS Records helper ───────────────────────────────────────────────────

function DnsGuide({ scenario, domainName }: { scenario: Scenario; domainName: string }) {
  if (scenario === 'AMG_SUBDOMAIN' || scenario === 'OPENPROVIDER_NEW' || scenario === 'OPENPROVIDER_TRANSFER') return null;

  const isSubdomain = scenario === 'EXTERNAL_SUBDOMAIN';
  const name = isSubdomain ? domainName.split('.')[0] : '@';
  const recordType = isSubdomain ? 'CNAME' : 'A';
  const recordValue = isSubdomain ? 'amgdl.com.' : AMG_SERVER_IP;

  return (
    <div className="p-4 border border-border-base bg-[rgba(255,255,255,0.02)] rounded space-y-3">
      <div className="f-mono text-xs text-ink-2 uppercase tracking-wider">Registres DNS a configurar</div>
      <div className="space-y-2">
        <div className="flex items-center gap-3 font-mono text-xs">
          <span className="text-accent-light w-14">{recordType}</span>
          <span className="text-ink-1 w-16">{name}</span>
          <span className="text-ink-0">{recordValue}</span>
          <span className="text-ink-3 ml-auto">TTL 3600</span>
        </div>
        {!isSubdomain && (
          <div className="flex items-center gap-3 font-mono text-xs">
            <span className="text-accent-light w-14">A</span>
            <span className="text-ink-1 w-16">www</span>
            <span className="text-ink-0">{AMG_SERVER_IP}</span>
            <span className="text-ink-3 ml-auto">TTL 3600</span>
          </div>
        )}
      </div>
      <p className="f-mono text-[10px] text-ink-3">
        La propagació DNS pot trigar entre 1 i 48 hores. Un cop configurat, prem &quot;Verificar DNS&quot; per confirmar.
      </p>
    </div>
  );
}

// ── Modal d'afegir domini ────────────────────────────────────────────────

function AddDomainModal({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const { toast } = useToast();
  const [step, setStep] = useState<'select' | 'form'>('select');
  const [scenario, setScenario] = useState<Scenario | null>(null);

  // Camps comuns
  const [tenantId, setTenantId] = useState('');
  const [landingId, setLandingId] = useState('');

  // Camps per escenari
  const [domainInput, setDomainInput] = useState('');
  const [selectedTld, setSelectedTld] = useState('cat');
  const [subdomain, setSubdomain] = useState('');
  const [eppCode, setEppCode] = useState('');
  const [autoRenew, setAutoRenew] = useState(true);
  const [registrantName, setRegistrantName] = useState('');
  const [registrantEmail, setRegistrantEmail] = useState('');
  const [registrantPhone, setRegistrantPhone] = useState('');
  const [registrantNif, setRegistrantNif] = useState('');

  // OpenProvider check
  const [checkResult, setCheckResult] = useState<DomainCheckResponse | null>(null);
  const [checking, setChecking] = useState(false);

  const { data: tenants } = useQuery({ queryKey: ['tenants-for-domain'], queryFn: () => listTenants({ size: 100 }) });
  const { data: tldPricing } = useQuery({ queryKey: ['tld-pricing'], queryFn: listTldPricing });
  const activeTlds = tldPricing?.filter((t: TldPricingResponse) => t.isActive).map((t: TldPricingResponse) => t.tld) ?? ['cat', 'es', 'com'];

  const { mutate: doAction, isPending } = useMutation({
    mutationFn: async () => {
      if (!tenantId) throw new Error('Selecciona un client');
      if (!scenario) throw new Error('Selecciona un escenari');

      if (scenario === 'AMG_SUBDOMAIN') {
        return assignAmgSubdomain({ tenantId, subdomain, landingId: landingId || undefined });
      }
      if (scenario === 'EXTERNAL_OWN' || scenario === 'EXTERNAL_SUBDOMAIN') {
        return addExternalDomain({ tenantId, domainName: domainInput, scenario, landingId: landingId || undefined });
      }
      if (scenario === 'OPENPROVIDER_NEW') {
        const fullDomain = `${domainInput}.${selectedTld}`;
        return registerDomain({ tenantId, domainName: fullDomain, autoRenew,
          registrantName, registrantEmail, registrantPhone, registrantNif,
          landingId: landingId || undefined });
      }
      if (scenario === 'OPENPROVIDER_TRANSFER') {
        return transferDomain({ tenantId, domainName: domainInput, eppCode, autoRenew,
          registrantName, registrantEmail, registrantPhone, registrantNif,
          landingId: landingId || undefined });
      }
      throw new Error('Escenari desconegut');
    },
    onSuccess: () => {
      toast('success', 'Domini afegit correctament');
      onDone();
      onClose();
    },
    onError: (err: Error) => toast('error', err.message || 'Error afegint el domini'),
  });

  const handleCheck = async () => {
    setChecking(true); setCheckResult(null);
    try {
      const result = await checkDomainAvailability(`${domainInput}.${selectedTld}`);
      setCheckResult(result);
    } catch { toast('error', 'Error comprovant disponibilitat'); }
    finally { setChecking(false); }
  };

  const selectedScenarioInfo = SCENARIOS.find(s => s.id === scenario);

  const canSubmit = (() => {
    if (!tenantId || !scenario) return false;
    if (scenario === 'AMG_SUBDOMAIN') return subdomain.length >= 2;
    if (scenario === 'EXTERNAL_OWN' || scenario === 'EXTERNAL_SUBDOMAIN') return domainInput.length >= 4;
    if (scenario === 'OPENPROVIDER_NEW') return !!checkResult?.available;
    if (scenario === 'OPENPROVIDER_TRANSFER') return domainInput.length >= 4 && eppCode.length >= 4;
    return false;
  })();

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="amg-card card-clip w-full max-w-xl p-6 space-y-5 max-h-[90vh] overflow-y-auto"
        onClick={e => e.stopPropagation()}>

        <div className="flex items-center justify-between">
          <div>
            <div className="f-display font-bold text-base">
              {step === 'select' ? 'Afegir domini' : selectedScenarioInfo?.title}
            </div>
            {step === 'form' && (
              <button onClick={() => { setStep('select'); setScenario(null); setCheckResult(null); }}
                className="f-mono text-xs text-ink-3 hover:text-ink-1 mt-0.5">
                ← Canviar escenari
              </button>
            )}
          </div>
          <button onClick={onClose} className="text-ink-2 hover:text-ink-0"><IconSet.X size={18} /></button>
        </div>

        {/* Pas 1: selecció d'escenari */}
        {step === 'select' && (
          <div className="space-y-2">
            {SCENARIOS.map(s => (
              <button key={s.id} onClick={() => { setScenario(s.id); setStep('form'); }}
                className="w-full flex items-start gap-3 p-4 border border-border-base rounded hover:border-[#FF6B00] hover:bg-[rgba(255,107,0,0.04)] transition text-left group">
                <span className="text-ink-3 group-hover:text-accent-light mt-0.5">{s.icon}</span>
                <div>
                  <div className="f-display font-bold text-sm text-ink-0">{s.title}</div>
                  <div className="f-mono text-xs text-ink-3 mt-0.5">{s.subtitle}</div>
                </div>
              </button>
            ))}
          </div>
        )}

        {/* Pas 2: formulari per escenari */}
        {step === 'form' && scenario && (
          <div className="space-y-4">
            {/* Client selector — comú a tots */}
            <div>
              <label className="f-mono text-xs text-ink-2 uppercase block mb-1">Client</label>
              <select value={tenantId} onChange={e => setTenantId(e.target.value)}
                className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2 text-sm text-ink-1 focus:outline-none focus:border-[#FF6B00]">
                <option value="">— Selecciona el client —</option>
                {tenants?.content.map((t: { id: string; name: string }) => (
                  <option key={t.id} value={t.id}>{t.name}</option>
                ))}
              </select>
            </div>

            {/* Escenari C: Subdomini AMG */}
            {scenario === 'AMG_SUBDOMAIN' && (
              <div className="space-y-3">
                <div>
                  <label className="f-mono text-xs text-ink-2 uppercase block mb-1">Nom del subdomini</label>
                  <div className="flex items-center gap-2">
                    <input value={subdomain} onChange={e => setSubdomain(e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, ''))}
                      placeholder="canpedro"
                      className="flex-1 bg-[#0d0d1a] border border-border-base rounded px-3 h-9 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]" />
                    <span className="f-mono text-sm text-ink-3 shrink-0">.amgdl.com</span>
                  </div>
                  {subdomain && (
                    <p className="f-mono text-xs text-accent-light mt-1">
                      → {subdomain}.amgdl.com
                    </p>
                  )}
                </div>
                <div className="p-3 border border-border-base bg-[rgba(255,255,255,0.02)] rounded">
                  <p className="f-mono text-[10px] text-ink-2">
                    El subdomini estarà actiu immediatament. AMG gestiona el DNS via Traefik. No cal cap configuració addicional.
                  </p>
                </div>
              </div>
            )}

            {/* Escenari A: Domini extern propi */}
            {scenario === 'EXTERNAL_OWN' && (
              <div className="space-y-3">
                <AMGInput label="Nom de domini complet" value={domainInput}
                  onChange={e => setDomainInput(e.target.value.toLowerCase())}
                  placeholder="restaurantmaria.com" mono />
                <DnsGuide scenario={scenario} domainName={domainInput} />
              </div>
            )}

            {/* Escenari D: Subdomini extern */}
            {scenario === 'EXTERNAL_SUBDOMAIN' && (
              <div className="space-y-3">
                <AMGInput label="Subdomini complet" value={domainInput}
                  onChange={e => setDomainInput(e.target.value.toLowerCase())}
                  placeholder="carta.restaurantmaria.com" mono />
                <DnsGuide scenario={scenario} domainName={domainInput} />
              </div>
            )}

            {/* Escenari B: Registre nou OpenProvider */}
            {scenario === 'OPENPROVIDER_NEW' && (
              <div className="space-y-3">
                <div className="flex gap-2">
                  <div className="flex-1">
                    <AMGInput label="Nom del domini" value={domainInput}
                      onChange={e => { setDomainInput(e.target.value); setCheckResult(null); }}
                      placeholder="perruqueria-maria" mono />
                  </div>
                  <div className="pt-5">
                    <select value={selectedTld} onChange={e => { setSelectedTld(e.target.value); setCheckResult(null); }}
                      className="h-10 bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 text-sm text-ink-1 focus:outline-none focus:border-[#FF6B00]">
                      {activeTlds.map((tld: string) => <option key={tld} value={tld}>.{tld}</option>)}
                    </select>
                  </div>
                </div>
                <AMGButton onClick={handleCheck} loading={checking} disabled={!domainInput.trim()} className="w-full justify-center">
                  Comprovar disponibilitat
                </AMGButton>
                {checkResult && (
                  <div className={`p-3 rounded border ${checkResult.available
                    ? 'border-green-500/30 bg-green-500/06' : 'border-red-500/30 bg-red-500/06'}`}>
                    <div className="flex items-center gap-2 mb-1">
                      {checkResult.available
                        ? <><IconSet.Check size={14} className="text-green-400" /><span className="text-green-400 text-sm font-semibold">Disponible</span></>
                        : <><IconSet.X size={14} className="text-red-400" /><span className="text-red-400 text-sm font-semibold">No disponible</span></>}
                      <span className="f-mono text-sm text-ink-1 ml-1">{checkResult.domainName}</span>
                    </div>
                    {checkResult.available && (
                      <div className="flex gap-6 mt-2 text-xs f-mono">
                        <div><span className="text-ink-3">Registre: </span><span className="text-accent-light font-bold">{checkResult.saleRegister}€</span></div>
                        <div><span className="text-ink-3">Renovació: </span><span className="text-white font-bold">{checkResult.saleRenew}€/any</span></div>
                      </div>
                    )}
                    {!checkResult.available && checkResult.alternatives.length > 0 && (
                      <div className="mt-2 flex flex-wrap gap-1">
                        {checkResult.alternatives.map(alt => (
                          <button key={alt} onClick={() => {
                            const parts = alt.split('.'); setDomainInput(parts.slice(0, -1).join('.')); setSelectedTld(parts[parts.length - 1]); setCheckResult(null);
                          }} className="f-mono text-xs px-2 py-0.5 border border-border-base hover:border-accent-light rounded text-ink-2 hover:text-accent-light transition">
                            {alt}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                )}
                {/* Dades registrant (col·lapsables) */}
                {checkResult?.available && (
                  <details className="border border-border-base rounded p-3">
                    <summary className="f-mono text-xs text-ink-2 cursor-pointer">Dades del registrant (opcional)</summary>
                    <div className="mt-3 space-y-2">
                      <AMGInput label="Nom" value={registrantName} onChange={e => setRegistrantName(e.target.value)} />
                      <AMGInput label="Email" value={registrantEmail} onChange={e => setRegistrantEmail(e.target.value)} />
                      <AMGInput label="Telèfon" value={registrantPhone} onChange={e => setRegistrantPhone(e.target.value)} />
                      <AMGInput label="NIF/CIF" value={registrantNif} onChange={e => setRegistrantNif(e.target.value)} />
                    </div>
                  </details>
                )}
                <label className="flex items-center gap-2 cursor-pointer">
                  <input type="checkbox" checked={autoRenew} onChange={e => setAutoRenew(e.target.checked)} className="w-4 h-4 accent-[#FF6B00]" />
                  <span className="text-sm text-ink-2">Renovació automàtica anual</span>
                </label>
              </div>
            )}

            {/* Escenari E: Transferència OpenProvider */}
            {scenario === 'OPENPROVIDER_TRANSFER' && (
              <div className="space-y-3">
                <AMGInput label="Domini a transferir" value={domainInput}
                  onChange={e => setDomainInput(e.target.value.toLowerCase())}
                  placeholder="restaurant.com" mono />
                <AMGInput label="Codi EPP (Auth code)" value={eppCode}
                  onChange={e => setEppCode(e.target.value)}
                  placeholder="ABC123def456" mono />
                <details className="border border-border-base rounded p-3">
                  <summary className="f-mono text-xs text-ink-2 cursor-pointer">Dades del registrant</summary>
                  <div className="mt-3 space-y-2">
                    <AMGInput label="Nom" value={registrantName} onChange={e => setRegistrantName(e.target.value)} />
                    <AMGInput label="Email" value={registrantEmail} onChange={e => setRegistrantEmail(e.target.value)} />
                    <AMGInput label="Telèfon" value={registrantPhone} onChange={e => setRegistrantPhone(e.target.value)} />
                    <AMGInput label="NIF/CIF" value={registrantNif} onChange={e => setRegistrantNif(e.target.value)} />
                  </div>
                </details>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input type="checkbox" checked={autoRenew} onChange={e => setAutoRenew(e.target.checked)} className="w-4 h-4 accent-[#FF6B00]" />
                  <span className="text-sm text-ink-2">Renovació automàtica anual</span>
                </label>
                <div className="p-3 border border-border-base bg-[rgba(255,255,255,0.02)] rounded">
                  <p className="f-mono text-[10px] text-ink-2">
                    La transferència pot trigar entre 5 i 7 dies. El client ha de desbloquejar el domini i proporcionar el codi EPP des del seu registrar actual.
                  </p>
                </div>
              </div>
            )}

            <AMGButton onClick={() => doAction()} disabled={!canSubmit || isPending} loading={isPending} className="w-full justify-center">
              {scenario === 'AMG_SUBDOMAIN' ? 'Assignar subdomini' :
               scenario === 'EXTERNAL_OWN' || scenario === 'EXTERNAL_SUBDOMAIN' ? 'Afegir domini' :
               scenario === 'OPENPROVIDER_NEW' ? 'Registrar domini' : 'Iniciar transferència'}
            </AMGButton>
          </div>
        )}
      </div>
    </div>
  );
}

// ── Fila de domini a la taula ─────────────────────────────────────────────

function DomainRow({ domain, onAction }: { domain: ManagedDomainResponse; onAction: () => void }) {
  const { toast } = useToast();
  const days = daysUntil(domain.expiresAt);
  const [loading, setLoading] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [verifyResult, setVerifyResult] = useState<{ verified: boolean; resolvedIp: string | null } | null>(null);

  const needsRenewal = domain.status === 'EXPIRING_SOON' || (days !== null && days <= 30);
  const isExternal = domain.scenario === 'EXTERNAL_OWN' || domain.scenario === 'EXTERNAL_SUBDOMAIN';
  const isAmg = domain.scenario === 'AMG_SUBDOMAIN';

  const handleRenew = async () => {
    setLoading(true);
    try { await renewDomain(domain.id); toast('success', 'Domini renovat'); onAction(); }
    catch { toast('error', 'Error renovant'); }
    finally { setLoading(false); }
  };

  const handleConfigureDns = async () => {
    setLoading(true);
    try { await configureDns(domain.id); toast('success', 'DNS configurat'); onAction(); }
    catch { toast('error', 'Error configurant DNS'); }
    finally { setLoading(false); }
  };

  const handleVerifyDns = async () => {
    setVerifying(true);
    try {
      const r = await verifyDns(domain.id);
      setVerifyResult({ verified: r.verified, resolvedIp: r.resolvedIp });
      toast(r.verified ? 'success' : 'error',
        r.verified ? 'DNS verificat correctament' : `El domini apunta a ${r.resolvedIp ?? '?'}, s'esperava ${r.expectedIp}`);
      onAction();
    } catch { toast('error', 'Error verificant DNS'); }
    finally { setVerifying(false); }
  };

  return (
    <tr className="border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,255,255,0.02)] transition-colors">
      <td className="px-4 py-3">
        <div className="f-mono font-semibold text-sm text-ink-1">{domain.domainName}</div>
        <div className="flex gap-1.5 mt-1 flex-wrap">
          <AMGBadge tone={STATUS_TONE[domain.status] as any ?? 'neutral'}>
            {STATUS_LABELS[domain.status] ?? domain.status}
          </AMGBadge>
          <AMGBadge tone={SCENARIO_TONE[domain.scenario] as any ?? 'neutral'}>
            {SCENARIO_LABELS[domain.scenario] ?? domain.scenario}
          </AMGBadge>
        </div>
      </td>
      <td className="px-4 py-3">
        <div className="flex items-center gap-1.5">
          {domain.dnsVerified
            ? <span className="f-mono text-xs text-green-400">✓ Verificat</span>
            : domain.dnsConfigured
              ? <span className="f-mono text-xs text-yellow-400">⚑ Configurat</span>
              : <span className="f-mono text-xs text-ink-3">— Pendent</span>}
          {verifyResult !== null && (
            <span className={`f-mono text-[10px] ${verifyResult.verified ? 'text-green-400' : 'text-red-400'}`}>
              {verifyResult.resolvedIp}
            </span>
          )}
        </div>
      </td>
      <td className="px-4 py-3 f-mono text-xs text-ink-2">
        {isAmg ? 'AMG' : isExternal ? 'Client' : fmtDate(domain.expiresAt)}
        {days !== null && days <= 30 && !isExternal && !isAmg && (
          <span className={`ml-1 ${days <= 7 ? 'text-red-400' : 'text-yellow-400'}`}>({days}d)</span>
        )}
      </td>
      <td className="px-4 py-3">
        <div className="flex gap-1.5 flex-wrap">
          {/* Verificar DNS — per a dominis externs */}
          {(isExternal) && (
            <AMGButton size="sm" variant="ghost" onClick={handleVerifyDns} loading={verifying}>
              Verificar DNS
            </AMGButton>
          )}
          {/* DNS auto — per a dominis OpenProvider actius */}
          {!domain.dnsConfigured && domain.status === 'ACTIVE' && !isExternal && !isAmg && (
            <AMGButton size="sm" variant="secondary" onClick={handleConfigureDns} loading={loading}>
              DNS auto
            </AMGButton>
          )}
          {/* Renovar — per a dominis OpenProvider propers a expirar */}
          {needsRenewal && !isExternal && !isAmg && (
            <AMGButton size="sm" variant="secondary" onClick={handleRenew} loading={loading}>
              Renovar
            </AMGButton>
          )}
        </div>
      </td>
    </tr>
  );
}

// ── Pàgina principal ──────────────────────────────────────────────────────

export default function DomainsPage() {
  const qc = useQueryClient();
  const [showAdd, setShowAdd] = useState(false);
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
            <p className="text-sm text-ink-2 mt-1">Gestió de dominis: registre, transferència, externs i subdominis</p>
          </div>
          <AMGButton variant="primary" icon={IconSet.Plus} onClick={() => setShowAdd(true)}>
            Afegir domini
          </AMGButton>
        </div>

        {/* Guia d'escenaris */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          {SCENARIOS.map(s => (
            <button key={s.id} onClick={() => setShowAdd(true)}
              className="amg-card card-clip p-3 text-left hover:ring-1 hover:ring-[#FF6B00] transition group">
              <div className="text-ink-3 group-hover:text-accent-light mb-1">{s.icon}</div>
              <div className="f-display font-bold text-xs text-ink-0">{s.title}</div>
              <div className="f-mono text-[10px] text-ink-3 mt-0.5 line-clamp-2">{s.subtitle}</div>
            </button>
          ))}
        </div>

        {/* Alerta expiració */}
        {expiringCount > 0 && (
          <div className="flex items-center gap-3 p-4 border border-[rgba(255,107,0,0.35)] bg-[rgba(255,107,0,0.06)] rounded">
            <IconSet.AlertCircle size={18} className="text-accent-light flex-shrink-0" />
            <span className="text-sm text-ink-1">
              <span className="font-semibold text-accent-light">{expiringCount} domini{expiringCount > 1 ? 's' : ''}</span>
              {' '}expira{expiringCount > 1 ? 'n' : ''} en els pròxims 30 dies
            </span>
          </div>
        )}

        {/* Taula */}
        <div className="amg-card card-clip">
          <div className="p-4 sm:p-5 border-b border-border-base flex items-center justify-between">
            <AMGSectionTitle eyebrow="Tots els escenaris" title="Dominis registrats" />
            <span className="f-mono text-xs text-ink-3">{data?.totalElements ?? 0} en total</span>
          </div>
          <div className="overflow-x-auto">
            {isLoading ? (
              <div className="flex justify-center py-12">
                <span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
              </div>
            ) : domains.length === 0 ? (
              <div className="p-12 text-center">
                <IconSet.Link size={28} stroke="#64748b" className="mx-auto mb-3" />
                <div className="f-display font-bold text-sm mb-1">Cap domini registrat</div>
                <p className="f-mono text-xs text-ink-2 mb-4">Afegeix el primer domini per a un client</p>
                <AMGButton size="sm" onClick={() => setShowAdd(true)}>Afegir domini</AMGButton>
              </div>
            ) : (
              <>
                <table className="w-full min-w-[600px]">
                  <thead>
                    <tr className="border-b border-border-base">
                      {['Domini / Estat', 'DNS', 'Expira / Gestió', 'Accions'].map(h => (
                        <th key={h} className="text-left f-mono text-label uppercase text-ink-2 px-4 py-3 font-normal">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {domains.map((d: ManagedDomainResponse) => (
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

      {showAdd && (
        <AddDomainModal onClose={() => setShowAdd(false)} onDone={invalidate} />
      )}
    </PortalShell>
  );
}

function listExpiringDomains(days: number) {
  return import('@/services/domainService').then(m => m.listExpiringDomains(days));
}
