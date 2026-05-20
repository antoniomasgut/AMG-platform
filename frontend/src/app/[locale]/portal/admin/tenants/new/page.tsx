'use client';

import { useTranslations } from 'next-intl';
import { useState, useEffect, useCallback } from 'react';
import { useRouter } from '@/i18n/navigation';
import { AMGButton } from '@/components/ui/button';
import { AMGInput } from '@/components/ui/input';
import { I } from '@/components/ui/icons';
import { createTenant } from '@/services/tenantService';
import { getCurrentUser } from '@/services/auth';
import {
  SECTORS, SIZES, PHASES, SECTOR_SIZES,
  SECTOR_LABELS, SIZE_LABELS, PHASE_LABELS,
  lookupSectorPricing, getSectorPromptTemplate,
  PHASE_UPGRADE_PRICE, type SectorPricingResponse,
} from '@/services/admin';

const MONTHLY_BY_COUNT: Record<number, keyof SectorPricingResponse> = {
  1: 'monthlyF1',
  2: 'monthlyF1f2',
  3: 'monthlyF1f2f3',
};

export default function NewTenantPage() {
  const t = useTranslations('admin');
  const router = useRouter();

  const [form, setForm] = useState({
    name: '', slug: '', email: '', phone: '', address: '',
    sector: '', businessSize: '',
    agentSystemPrompt: '',
  });
  const [selectedPhases, setSelectedPhases] = useState<string[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [pricing, setPricing] = useState<SectorPricingResponse | null>(null);
  const [promptLoading, setPromptLoading] = useState(false);
  const [promptCustomized, setPromptCustomized] = useState(false);

  useEffect(() => {
    const user = getCurrentUser();
    if (user?.role !== 'SUPER_ADMIN') router.push(`/portal/admin/tenants`);
  }, [router]);

  const lookupPricing = useCallback(async (sector: string, size: string) => {
    if (!sector || !size) { setPricing(null); return; }
    try { setPricing(await lookupSectorPricing(sector, size)); }
    catch { setPricing(null); }
  }, []);

  const fetchPromptTemplate = useCallback(async (sector: string) => {
    if (!sector || promptCustomized) return;
    setPromptLoading(true);
    try {
      const { template } = await getSectorPromptTemplate(sector);
      setForm(prev => ({ ...prev, agentSystemPrompt: template }));
    } catch { /* silent */ }
    finally { setPromptLoading(false); }
  }, [promptCustomized]);

  const handleSectorChange = (sector: string) => {
    const availableSizes = SECTOR_SIZES[sector] ?? [];
    const sizeValid = availableSizes.includes(form.businessSize);
    const newSize = sizeValid ? form.businessSize : (availableSizes.length === 1 ? availableSizes[0] : '');
    setForm(prev => ({ ...prev, sector, businessSize: newSize }));
    lookupPricing(sector, newSize);
    fetchPromptTemplate(sector);
  };

  const handleSizeChange = (businessSize: string) => {
    setForm(prev => ({ ...prev, businessSize }));
    lookupPricing(form.sector, businessSize);
  };

  const togglePhase = (phase: string) => {
    setSelectedPhases(prev =>
      prev.includes(phase) ? prev.filter(p => p !== phase) : [...prev, phase]
    );
  };

  const generateSlug = (name: string) =>
    name.toLowerCase().replace(/[^a-z0-9\s-]/g, '').replace(/\s+/g, '-').replace(/-+/g, '-');

  const availableSizes = form.sector ? (SECTOR_SIZES[form.sector] ?? SIZES) : [];
  const phaseCount = selectedPhases.length;
  const monthlyKey = MONTHLY_BY_COUNT[phaseCount] ?? 'monthlyComplete';
  const monthlyPrice = pricing ? (pricing[monthlyKey] as number) : null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const errs: Record<string, string> = {};
    if (!form.name) errs.name = 'Nom requerit';
    if (!form.slug) errs.slug = 'Slug requerit';
    else if (!/^[a-z0-9-]+$/.test(form.slug)) errs.slug = 'Slug invàlid (només minúscules, números i guions)';
    setErrors(errs);
    if (Object.keys(errs).length > 0) return;

    setLoading(true);
    try {
      await createTenant({
        name: form.name, slug: form.slug,
        email: form.email || undefined, phone: form.phone || undefined, address: form.address || undefined,
        sector: form.sector || undefined, businessSize: form.businessSize || undefined,
        contractedPhases: selectedPhases.length > 0 ? selectedPhases : undefined,
      });
      router.push('/portal/admin/tenants');
    } catch (err: any) {
      setErrors({ submit: err.message });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-4 sm:p-8 max-w-2xl space-y-6">
      <div className="flex items-center gap-3">
        <AMGButton variant="ghost" size="sm" onClick={() => router.back()}>
          <I.Chevron size={16} className="rotate-180" />
        </AMGButton>
        <h1 className="f-display font-black text-lg text-white">{t('tenants.new')}</h1>
      </div>

      <form onSubmit={handleSubmit} className="space-y-8">
        {/* Dades bàsiques */}
        <div className="space-y-5">
          <div className="f-mono text-label uppercase tracking-widest text-ink-3">Dades bàsiques</div>
          <AMGInput label={t('tenants.fields.name' as any)} value={form.name}
            onChange={(e) => setForm(p => ({ ...p, name: e.target.value, slug: generateSlug(e.target.value) }))} error={errors.name} />
          <AMGInput label={t('tenants.fields.slug' as any)} value={form.slug} mono
            onChange={(e) => setForm(p => ({ ...p, slug: e.target.value }))} error={errors.slug} />
          <AMGInput label={t('tenants.fields.email' as any)} type="email" value={form.email}
            onChange={(e) => setForm(p => ({ ...p, email: e.target.value }))} />
          <AMGInput label={t('tenants.fields.phone' as any)} value={form.phone}
            onChange={(e) => setForm(p => ({ ...p, phone: e.target.value }))} />
          <AMGInput label={t('tenants.fields.address' as any)} value={form.address}
            onChange={(e) => setForm(p => ({ ...p, address: e.target.value }))} />
        </div>

        {/* Model de negoci */}
        <div className="space-y-5">
          <div className="f-mono text-label uppercase tracking-widest text-ink-3">Model de negoci</div>

          {/* Sector */}
          <div className="space-y-2">
            <label className="block text-sm text-ink-2">Sector professional</label>
            <select value={form.sector} onChange={(e) => handleSectorChange(e.target.value)}
              className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2.5 text-sm text-ink-1 focus:outline-none focus:border-[#FF6B00] transition">
              <option value="">— Selecciona sector —</option>
              {SECTORS.map((s) => <option key={s} value={s}>{SECTOR_LABELS[s] ?? s}</option>)}
            </select>
          </div>

          {/* Mida */}
          {form.sector && (
            <div className="space-y-2">
              <label className="block text-sm text-ink-2">Mida de l&apos;empresa</label>
              <div className="flex gap-2 flex-wrap">
                {availableSizes.map((sz) => (
                  <button key={sz} type="button" onClick={() => handleSizeChange(sz)}
                    className={`flex-1 min-w-[140px] text-left px-3 py-2.5 border rounded text-sm transition ${
                      form.businessSize === sz
                        ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.12)] text-white'
                        : 'border-border-base hover:border-ink-2 text-ink-2'
                    }`}>
                    <div className="font-semibold">{sz}</div>
                    <div className="text-xs opacity-70">{SIZE_LABELS[sz]?.split(' (')[1]?.replace(')', '') ?? ''}</div>
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Fases — selecció múltiple */}
          {form.businessSize && (
            <div className="space-y-2">
              <label className="block text-sm text-ink-2">
                Fases contractades
                <span className="ml-2 f-mono text-xs text-ink-3">
                  {phaseCount === 0 ? '— selecciona les que necessita' : `${phaseCount} fase${phaseCount > 1 ? 's' : ''} seleccionada${phaseCount > 1 ? 's' : ''}`}
                </span>
              </label>
              <div className="flex gap-2 flex-wrap">
                {PHASES.map((ph) => {
                  const active = selectedPhases.includes(ph);
                  return (
                    <button key={ph} type="button" onClick={() => togglePhase(ph)}
                      className={`px-4 py-2.5 border rounded text-sm transition relative ${
                        active
                          ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.12)] text-white'
                          : 'border-border-base hover:border-ink-2 text-ink-2'
                      }`}>
                      {active && (
                        <span className="absolute -top-1.5 -right-1.5 w-4 h-4 bg-[#FF6B00] rounded-full flex items-center justify-center">
                          <I.Check size={9} stroke="#fff" />
                        </span>
                      )}
                      <div className="font-bold">{ph}</div>
                      <div className="text-[10px] opacity-70 whitespace-nowrap">{PHASE_LABELS[ph]?.split(' — ')[1] ?? ''}</div>
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Pricing preview */}
          {pricing && phaseCount > 0 && (
            <div className="amg-card card-clip p-4 border border-[rgba(255,107,0,0.25)] bg-[rgba(255,107,0,0.06)]">
              <div className="f-mono text-label uppercase text-accent-light tracking-widest mb-3">Preu estimat</div>
              <div className="flex gap-6 flex-wrap">
                <div>
                  <div className="f-mono text-xs text-ink-3 mb-1">Setup</div>
                  <div className="f-display font-bold text-xl text-white">{pricing.setupPrice} €</div>
                </div>
                {monthlyPrice !== null && (
                  <div>
                    <div className="f-mono text-xs text-ink-3 mb-1">Mensual ({phaseCount} fase{phaseCount > 1 ? 's' : ''})</div>
                    <div className="f-display font-bold text-xl text-accent-light">{monthlyPrice} €/mes</div>
                  </div>
                )}
              </div>
              <div className="f-mono text-[10px] text-ink-3 mt-2">
                {SECTOR_LABELS[form.sector]} · {SIZE_LABELS[form.businessSize]} · {selectedPhases.sort().join(' + ')}
              </div>
              <div className="mt-3 pt-3 border-t border-[rgba(255,107,0,0.2)] f-mono text-[10px] text-ink-3">
                Ampliació futura de fase: <span className="text-ink-2 font-semibold">{PHASE_UPGRADE_PRICE} € / fase</span>
              </div>
            </div>
          )}

          {pricing && phaseCount === 0 && form.businessSize && (
            <div className="f-mono text-xs text-ink-3 p-3 border border-border-base rounded">
              Selecciona les fases per veure el preu
            </div>
          )}

          {form.sector && form.businessSize && !pricing && (
            <div className="f-mono text-xs text-ink-3 p-3 border border-border-base rounded">
              No hi ha preu definit per a {SECTOR_LABELS[form.sector]} + {form.businessSize}
            </div>
          )}
        </div>

        {/* Prompt agent IA */}
        {form.sector && (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <div className="f-mono text-label uppercase tracking-widest text-ink-3">Prompt agent IA</div>
              {promptLoading
                ? <span className="w-3.5 h-3.5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
                : form.agentSystemPrompt
                  ? <span className="f-mono text-[10px] text-ink-3">{promptCustomized ? 'personalitzat' : 'template del sector'}</span>
                  : null}
            </div>
            <textarea value={form.agentSystemPrompt}
              onChange={(e) => { setPromptCustomized(true); setForm(p => ({ ...p, agentSystemPrompt: e.target.value })); }}
              rows={12}
              placeholder="El prompt s'omplirà automàticament en seleccionar el sector..."
              className="w-full bg-[rgba(255,255,255,0.04)] border border-border-base rounded px-3 py-2.5 text-xs f-mono text-ink-1 focus:outline-none focus:border-[#FF6B00] transition resize-y"
            />
            {promptCustomized && (
              <button type="button" onClick={() => { setPromptCustomized(false); fetchPromptTemplate(form.sector); }}
                className="f-mono text-[10px] text-accent-light hover:text-accent transition">
                ↺ Restaurar template del sector
              </button>
            )}
          </div>
        )}

        {errors.submit && (
          <div className="bg-[rgba(255,68,68,0.12)] border border-[rgba(255,68,68,0.35)] rounded p-3 text-sm text-[#ff6666]">
            {errors.submit}
          </div>
        )}

        <div className="flex gap-3 pt-2">
          <AMGButton variant="primary" type="submit" loading={loading}>{t('save')}</AMGButton>
          <AMGButton variant="ghost" type="button" onClick={() => router.back()}>{t('cancel')}</AMGButton>
        </div>
      </form>
    </div>
  );
}
