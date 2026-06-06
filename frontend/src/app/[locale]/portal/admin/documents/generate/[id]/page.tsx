'use client';

import { useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useQuery, useMutation } from '@tanstack/react-query';
import { useToast } from '@/lib/toast-context';
import {
  getTemplate, generateDocument, generatePdf,
  type ArticleLine,
} from '@/services/documents';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { I } from '@/components/ui/icons';

export default function GenerateDocumentPage() {
  const params = useParams();
  const router = useRouter();
  const { toast } = useToast();
  const id = params.id as string;

  const [customerData, setCustomerData] = useState<Record<string, string>>({ name: '', taxId: '', address: '', phone: '', email: '' });
  const [variables, setVariables] = useState<Record<string, string>>({});
  const [articles, setArticles] = useState<ArticleLine[]>([{ description: '', quantity: 1, unitPrice: 0 }]);
  const [resultId, setResultId] = useState<string | null>(null);

  const { data: template, isLoading } = useQuery({
    queryKey: ['document-template', id],
    queryFn: () => getTemplate(id),
    enabled: !!id,
  });

  const { mutate: doGenerate, isPending: generating } = useMutation({
    mutationFn: () => generateDocument({
      templateId: id,
      customerData: Object.fromEntries(Object.entries(customerData).filter(([_, v]) => v)),
      variables: Object.fromEntries(Object.entries(variables).filter(([_, v]) => v)),
      articles: articles.filter(a => a.description.trim()),
    }),
    onSuccess: (doc) => {
      toast('success', `Document ${doc.number} generat`);
      setResultId(doc.id);
    },
    onError: () => toast('error', 'Error generant el document'),
  });

  const { mutate: doGeneratePdf, isPending: generatingPdf } = useMutation({
    mutationFn: () => generatePdf({
      templateId: id,
      customerData: Object.fromEntries(Object.entries(customerData).filter(([_, v]) => v)),
      variables: Object.fromEntries(Object.entries(variables).filter(([_, v]) => v)),
      articles: articles.filter(a => a.description.trim()),
    }),
    onSuccess: (doc) => {
      toast('success', `PDF generat: ${doc.number}`);
      setResultId(doc.id);
    },
    onError: () => toast('error', 'Error generant el PDF'),
  });

  const updateArticle = (idx: number, field: keyof ArticleLine, value: string | number) => {
    setArticles(prev => prev.map((a, i) => i === idx ? { ...a, [field]: value } : a));
  };

  const addArticle = () => {
    setArticles(prev => [...prev, { description: '', quantity: 1, unitPrice: 0 }]);
  };

  const removeArticle = (idx: number) => {
    setArticles(prev => prev.filter((_, i) => i !== idx));
  };

  const total = articles.reduce((sum, a) => sum + (a.quantity || 0) * (a.unitPrice || 0), 0);

  if (isLoading) {
    return (
      <PortalShell breadcrumb="admin · documents · generate" backHref="/portal/admin/documents">
        <div className="flex justify-center py-20">
          <span className="w-5 h-5 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
        </div>
      </PortalShell>
    );
  }

  if (!template) {
    return (
      <PortalShell breadcrumb="admin · documents · generate" backHref="/portal/admin/documents">
        <div className="p-8 text-center">
          <I.FileText size={28} stroke="#64748b" className="mx-auto mb-3" />
          <div className="f-display font-bold text-sm">Plantilla no trobada</div>
        </div>
      </PortalShell>
    );
  }

  if (resultId) {
    return (
      <PortalShell breadcrumb={`admin · documents · ${template.name}`} backHref="/portal/admin/documents">
        <div className="p-4 sm:p-8 space-y-6 max-w-2xl mx-auto text-center">
          <I.Check size={48} className="mx-auto text-green-500" />
          <div className="f-display font-bold text-xl mt-4">Document generat correctament</div>
          <p className="text-ink-1 mt-2">Pots veure el document o generar-ne un altre.</p>
          <div className="flex gap-3 justify-center mt-6">
            <AMGButton onClick={() => router.push(`/portal/admin/documents/view/${resultId}`)}>
              <I.Eye size={14} />
              Veure document
            </AMGButton>
            <AMGButton variant="outline" onClick={() => setResultId(null)}>
              Generar un altre
            </AMGButton>
          </div>
        </div>
      </PortalShell>
    );
  }

  return (
    <PortalShell breadcrumb={`admin · documents · ${template.name}`} backHref="/portal/admin/documents">
      <div className="p-4 sm:p-8 space-y-6 max-w-3xl">
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / documents / generate /</span>
          <div className="flex items-center gap-3 mt-1">
            <div className="f-display font-bold text-xl">Generar: {template.name}</div>
            <AMGBadge tone="info">{template.documentType}</AMGBadge>
          </div>
        </div>

        <div className="amg-card card-clip p-6 space-y-6">
          {/* Customer data */}
          <div>
            <div className="f-display font-bold text-sm mb-3">Dades del client</div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="f-mono text-[10px] uppercase text-ink-2 block mb-1">Nom</label>
                <input value={customerData.name} onChange={(e) => setCustomerData(prev => ({ ...prev, name: e.target.value }))}
                  className="w-full bg-[#0d0d1a] border border-border-base rounded px-3 py-2 text-sm text-ink-0" placeholder="Nom del client" />
              </div>
              <div>
                <label className="f-mono text-[10px] uppercase text-ink-2 block mb-1">CIF/NIF</label>
                <input value={customerData.taxId} onChange={(e) => setCustomerData(prev => ({ ...prev, taxId: e.target.value }))}
                  className="w-full bg-[#0d0d1a] border border-border-base rounded px-3 py-2 text-sm text-ink-0" placeholder="12345678A" />
              </div>
              <div className="sm:col-span-2">
                <label className="f-mono text-[10px] uppercase text-ink-2 block mb-1">Adreça</label>
                <input value={customerData.address} onChange={(e) => setCustomerData(prev => ({ ...prev, address: e.target.value }))}
                  className="w-full bg-[#0d0d1a] border border-border-base rounded px-3 py-2 text-sm text-ink-0" placeholder="C/ Major 1, Palma" />
              </div>
              <div>
                <label className="f-mono text-[10px] uppercase text-ink-2 block mb-1">Telèfon</label>
                <input value={customerData.phone} onChange={(e) => setCustomerData(prev => ({ ...prev, phone: e.target.value }))}
                  className="w-full bg-[#0d0d1a] border border-border-base rounded px-3 py-2 text-sm text-ink-0" placeholder="+34 600 000 000" />
              </div>
              <div>
                <label className="f-mono text-[10px] uppercase text-ink-2 block mb-1">Email</label>
                <input value={customerData.email} onChange={(e) => setCustomerData(prev => ({ ...prev, email: e.target.value }))}
                  className="w-full bg-[#0d0d1a] border border-border-base rounded px-3 py-2 text-sm text-ink-0" placeholder="client@example.com" />
              </div>
            </div>
          </div>

          {/* Articles */}
          <div className="border-t border-border-base pt-4">
            <div className="flex items-center justify-between mb-3">
              <div className="f-display font-bold text-sm">Articles / Serveis</div>
              <AMGButton size="sm" variant="secondary" onClick={addArticle}>
                <I.Plus size={14} />
                Afegir línia
              </AMGButton>
            </div>
            <div className="space-y-2">
              {articles.map((article, idx) => (
                <div key={idx} className="flex gap-2 items-start">
                  <div className="flex-1">
                    <input value={article.description} onChange={(e) => updateArticle(idx, 'description', e.target.value)}
                      className="w-full bg-[#0d0d1a] border border-border-base rounded px-3 py-2 text-sm text-ink-0" placeholder="Descripció" />
                  </div>
                  <div className="w-20">
                    <input type="number" min={0} value={article.quantity} onChange={(e) => updateArticle(idx, 'quantity', parseInt(e.target.value) || 0)}
                      className="w-full bg-[#0d0d1a] border border-border-base rounded px-3 py-2 text-sm text-ink-0 text-center" placeholder="Qty" />
                  </div>
                  <div className="w-24">
                    <input type="number" min={0} step={0.01} value={article.unitPrice} onChange={(e) => updateArticle(idx, 'unitPrice', parseFloat(e.target.value) || 0)}
                      className="w-full bg-[#0d0d1a] border border-border-base rounded px-3 py-2 text-sm text-ink-0 text-center" placeholder="Preu" />
                  </div>
                  <div className="w-20 flex items-center justify-end text-sm text-ink-1 pt-2">
                    {(article.quantity * article.unitPrice).toFixed(2)} €
                  </div>
                  {articles.length > 1 && (
                    <button onClick={() => removeArticle(idx)} className="pt-2 text-ink-2 hover:text-red-400">
                      <I.X size={16} />
                    </button>
                  )}
                </div>
              ))}
            </div>
            <div className="flex justify-end mt-4 pt-3 border-t border-border-base">
              <div className="text-right">
                <span className="f-mono text-[10px] uppercase text-ink-2">Total</span>
                <div className="f-display font-bold text-lg">{total.toFixed(2)} €</div>
              </div>
            </div>
          </div>

          {/* Actions */}
          <div className="flex gap-3 border-t border-border-base pt-4">
            <AMGButton onClick={() => doGenerate()} disabled={generating} icon={I.FileText}>
              {generating ? 'Generant...' : 'Generar document'}
            </AMGButton>
            <AMGButton variant="secondary" onClick={() => doGeneratePdf()} disabled={generatingPdf} icon={I.Download}>
              {generatingPdf ? 'Generant...' : 'Generar PDF'}
            </AMGButton>
          </div>
        </div>
      </div>
    </PortalShell>
  );
}
