'use client';

import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { convertLead, type ConvertLeadResult } from '@/services/leads';
import { AMGButton } from '@/components/ui/button';
import { IconSet } from '@/components/ui/icons';
import { useToast } from '@/lib/toast-context';

const inp = "w-full bg-[#0d0d1a] border border-[#2a2a50] rounded px-3 py-2 text-sm font-mono text-white focus:outline-none focus:border-[#FF6B00] placeholder:text-[#404060]";

const SECTORS = [
  'RESTAURACIO', 'SALUT_BENESTAR', 'COMERC', 'SERVEIS', 'TURISME',
  'EDUCACIO', 'TECNOLOGIA', 'CONSTRUCCIO', 'IMMOBILIARIA', 'ALTRES',
];
const SIZES = ['AUTONOMO', 'PETIT', 'MITJA'];
const SIZE_LABEL: Record<string, string> = { AUTONOMO: 'Autònom', PETIT: 'Petit (2-10)', MITJA: 'Mitjà (11-50)' };

interface Props {
  leadId: string;
  leadName: string;
  leadEmail?: string;
  initialPhone?: string;
  initialSector?: string;
  initialSize?: string;
  initialSetup?: number;
  initialMonthly?: number;
  onClose: () => void;
}

function SuccessView({ result, onClose }: { result: ConvertLeadResult; onClose: () => void }) {
  const [copiedStripe, setCopiedStripe] = useState(false);
  const [copiedGC, setCopiedGC] = useState(false);

  const copy = (text: string, setCopied: (v: boolean) => void) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="space-y-5">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-full bg-[rgba(57,211,83,0.15)] border border-[#39d353]/40 flex items-center justify-center text-xl">✓</div>
        <div>
          <p className="font-semibold text-white">Tenant creat correctament</p>
          <p className="text-xs text-[#a0a0c0]">{result.tenantName}</p>
        </div>
      </div>

      <div className="space-y-3">
        {result.stripeCheckoutUrl && (
          <div className="bg-[#0d0d1a] border border-[#2a2a50] rounded-lg p-4">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <span className="text-base">💳</span>
                <span className="text-sm font-medium text-white">Pagament Setup — Stripe</span>
              </div>
              <button
                onClick={() => copy(result.stripeCheckoutUrl!, setCopiedStripe)}
                className="text-xs text-[#FF6B00] hover:underline flex items-center gap-1"
              >
                <IconSet.Copy size={11} /> {copiedStripe ? 'Copiat!' : 'Copiar link'}
              </button>
            </div>
            <p className="text-xs text-[#6060a0] mb-2">Envia aquest link al client perquè pagui el setup ara</p>
            <a
              href={result.stripeCheckoutUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-xs text-[#FF6B00] hover:underline break-all"
            >
              {result.stripeCheckoutUrl.substring(0, 60)}...
            </a>
          </div>
        )}

        {result.goCardlessRedirectUrl && (
          <div className="bg-[#0d0d1a] border border-[#2a2a50] rounded-lg p-4">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <span className="text-base">🏦</span>
                <span className="text-sm font-medium text-white">Domiciliació SEPA — GoCardless</span>
              </div>
              <button
                onClick={() => copy(result.goCardlessRedirectUrl!, setCopiedGC)}
                className="text-xs text-[#FF6B00] hover:underline flex items-center gap-1"
              >
                <IconSet.Copy size={11} /> {copiedGC ? 'Copiat!' : 'Copiar link'}
              </button>
            </div>
            <p className="text-xs text-[#6060a0] mb-2">El client autoritza la domiciliació per als mensuals</p>
            <a
              href={result.goCardlessRedirectUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-xs text-[#FF6B00] hover:underline break-all"
            >
              {result.goCardlessRedirectUrl.substring(0, 60)}...
            </a>
          </div>
        )}

        {result.holdedInvoiceId && (
          <div className="bg-[#0d0d1a] border border-[#2a2a50] rounded-lg p-4 flex items-center gap-3">
            <span className="text-base">📄</span>
            <div>
              <p className="text-sm font-medium text-white">Factura Holded creada</p>
              <p className="text-xs text-[#6060a0] font-mono">{result.holdedInvoiceId}</p>
            </div>
          </div>
        )}

        {!result.stripeCheckoutUrl && !result.goCardlessRedirectUrl && (
          <div className="bg-[rgba(255,107,0,0.08)] border border-[rgba(255,107,0,0.3)] rounded-lg p-4 text-xs text-[#c0a060]">
            Stripe i GoCardless no estan configurats. Configura'ls a Sistemes → Claus API per activar els pagaments automàtics.
          </div>
        )}
      </div>

      <AMGButton size="sm" onClick={onClose}>Tancar</AMGButton>
    </div>
  );
}

export function ConvertLeadModal({ leadId, leadName, leadEmail, initialPhone, initialSector, initialSize, initialSetup, initialMonthly, onClose }: Props) {
  const { toast } = useToast();
  const qc = useQueryClient();
  const [result, setResult] = useState<ConvertLeadResult | null>(null);

  const [form, setForm] = useState({
    tenantName: leadName,
    billingEmail: leadEmail ?? '',
    billingNif: '',
    billingPhone: initialPhone ?? '',
    billingAddress: '',
    billingCity: '',
    sector: initialSector ?? '',
    businessSize: initialSize ?? 'AUTONOMO',
    setupAmount: initialSetup?.toString() ?? '',
    monthlyAmount: initialMonthly?.toString() ?? '',
  });

  const { mutate: doConvert, isPending } = useMutation({
    mutationFn: () => convertLead(leadId, {
      ...form,
      setupAmount: form.setupAmount ? parseFloat(form.setupAmount) : undefined,
      monthlyAmount: form.monthlyAmount ? parseFloat(form.monthlyAmount) : undefined,
      portalBaseUrl: typeof window !== 'undefined' ? window.location.origin : undefined,
    }),
    onSuccess: (data) => {
      toast('success', `Tenant "${data.tenantName}" creat`);
      setResult(data);
      qc.invalidateQueries({ queryKey: ['lead', leadId] });
      qc.invalidateQueries({ queryKey: ['leads'] });
    },
    onError: (e: Error) => toast('error', e.message || 'Error en la conversió'),
  });

  const isValid = form.tenantName.trim().length > 0 && form.billingEmail.trim().length > 0;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/70" onClick={onClose} />
      <div className="relative bg-[#13132a] border border-[#2a2a50] rounded-2xl w-full max-w-lg shadow-2xl max-h-[90dvh] overflow-y-auto">
        <div className="p-5 border-b border-[#2a2a50] flex items-center justify-between sticky top-0 bg-[#13132a]">
          <div>
            <h2 className="font-semibold text-white">Convertir a client</h2>
            <p className="text-xs text-[#a0a0c0]">{leadName}</p>
          </div>
          <button onClick={onClose} className="text-[#6060a0] hover:text-white transition-colors">
            <IconSet.X size={18} />
          </button>
        </div>

        <div className="p-5">
          {result ? (
            <SuccessView result={result} onClose={onClose} />
          ) : (
            <div className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="sm:col-span-2">
                  <label className="text-xs text-[#6060a0] block mb-1">Nom empresa / Client *</label>
                  <input className={inp} value={form.tenantName}
                    onChange={e => setForm(f => ({ ...f, tenantName: e.target.value }))} />
                </div>
                <div className="sm:col-span-2">
                  <label className="text-xs text-[#6060a0] block mb-1">Email facturació *</label>
                  <input type="email" className={inp} value={form.billingEmail}
                    onChange={e => setForm(f => ({ ...f, billingEmail: e.target.value }))} />
                </div>
                <div>
                  <label className="text-xs text-[#6060a0] block mb-1">NIF / CIF</label>
                  <input className={inp} placeholder="B12345678" value={form.billingNif}
                    onChange={e => setForm(f => ({ ...f, billingNif: e.target.value }))} />
                </div>
                <div>
                  <label className="text-xs text-[#6060a0] block mb-1">Telèfon</label>
                  <input className={inp} value={form.billingPhone}
                    onChange={e => setForm(f => ({ ...f, billingPhone: e.target.value }))} />
                </div>
                <div>
                  <label className="text-xs text-[#6060a0] block mb-1">Adreça</label>
                  <input className={inp} value={form.billingAddress}
                    onChange={e => setForm(f => ({ ...f, billingAddress: e.target.value }))} />
                </div>
                <div>
                  <label className="text-xs text-[#6060a0] block mb-1">Població</label>
                  <input className={inp} placeholder="Palma" value={form.billingCity}
                    onChange={e => setForm(f => ({ ...f, billingCity: e.target.value }))} />
                </div>
                <div>
                  <label className="text-xs text-[#6060a0] block mb-1">Sector</label>
                  <select className={inp} value={form.sector}
                    onChange={e => setForm(f => ({ ...f, sector: e.target.value }))}>
                    <option value="">— Selecciona —</option>
                    {SECTORS.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
                  </select>
                </div>
                <div>
                  <label className="text-xs text-[#6060a0] block mb-1">Mida empresa</label>
                  <select className={inp} value={form.businessSize}
                    onChange={e => setForm(f => ({ ...f, businessSize: e.target.value }))}>
                    {SIZES.map(s => <option key={s} value={s}>{SIZE_LABEL[s]}</option>)}
                  </select>
                </div>
              </div>

              <div className="border-t border-[#2a2a50] pt-4 space-y-3">
                <p className="text-xs font-medium text-[#a0a0c0] uppercase tracking-wider">Imports</p>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs text-[#6060a0] block mb-1">Setup (€)</label>
                    <input type="number" min="0" step="10" className={inp} placeholder="0"
                      value={form.setupAmount}
                      onChange={e => setForm(f => ({ ...f, setupAmount: e.target.value }))} />
                    <p className="text-[10px] text-[#404060] mt-0.5">Genera link Stripe + factura Holded</p>
                  </div>
                  <div>
                    <label className="text-xs text-[#6060a0] block mb-1">Mensual (€)</label>
                    <input type="number" min="0" step="5" className={inp} placeholder="0"
                      value={form.monthlyAmount}
                      onChange={e => setForm(f => ({ ...f, monthlyAmount: e.target.value }))} />
                    <p className="text-[10px] text-[#404060] mt-0.5">Genera mandat SEPA GoCardless</p>
                  </div>
                </div>
              </div>

              <div className="flex gap-2 pt-2">
                <AMGButton
                  size="sm"
                  loading={isPending}
                  disabled={!isValid}
                  onClick={() => doConvert()}
                >
                  Convertir a client
                </AMGButton>
                <AMGButton size="sm" variant="ghost" onClick={onClose}>
                  Cancel·lar
                </AMGButton>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
