'use client';

import { useParams, useRouter } from 'next/navigation';
import { useLocale } from 'next-intl';
import { useState, useRef } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { IconSet } from '@/components/ui/icons';
import { useToast } from '@/lib/toast-context';
import { GenerateImageModal } from '@/components/meta-ads/GenerateImageModal';
import {
  createCampaign, createAdSet, createAd, uploadImage, searchInterests, searchLocations,
  type GeoLocation, type Interest, type TargetingItem,
} from '@/services/meta-ads';

const inp = "w-full bg-surface-base border border-border-base rounded px-3 py-1.5 text-sm text-ink-1 focus:outline-none focus:border-accent-light";
const lbl = "block text-xs text-ink-2 mb-1";

const OBJECTIVES = [
  { value: 'OUTCOME_LEADS',      icon: '🎯', label: 'Leads',       desc: 'Generar formularis de contacte' },
  { value: 'OUTCOME_TRAFFIC',    icon: '🔗', label: 'Tràfic',      desc: 'Portar visites a la web' },
  { value: 'OUTCOME_AWARENESS',  icon: '👁',  label: 'Notorietat', desc: 'Mostrar la marca a nous públics' },
  { value: 'OUTCOME_ENGAGEMENT', icon: '💬', label: 'Interacció',  desc: 'Aconseguir m\'agrades i comentaris' },
];

const CTA_OPTIONS = ['LEARN_MORE', 'CONTACT_US', 'GET_QUOTE', 'SIGN_UP', 'SUBSCRIBE', 'BOOK_TRAVEL', 'BUY_NOW'];

const STEPS = ['Objectiu', 'Públic', 'Creatiu', 'Revisió'];

interface FormStep1 { name: string; objective: string; dailyBudget: string; startTime: string; stopTime: string; }
interface FormStep2 {
  adSetName: string; ageMin: string; ageMax: string; genders: string;
  publisherPlatforms: string; geoLocations: GeoLocation[]; interests: Interest[];
}
interface FormStep3 {
  adName: string; headline: string; body: string; description: string;
  callToAction: string; linkUrl: string;
  imageFile: File | null; imagePreview: string | null; metaImageHash: string | null;
}

function StepIndicator({ current }: { current: number }) {
  return (
    <div className="flex items-center gap-2">
      {STEPS.map((s, i) => (
        <div key={s} className="flex items-center gap-2">
          <div className={`flex items-center gap-1.5 text-xs ${i === current ? 'text-ink-1 font-medium' : i < current ? 'text-[#39d353]' : 'text-ink-3'}`}>
            <span className={`w-5 h-5 rounded-full flex items-center justify-center text-[10px] border ${
              i < current ? 'bg-[#39d353] border-[#39d353] text-black' :
              i === current ? 'border-accent-light text-accent-light' :
              'border-border-base text-ink-3'
            }`}>
              {i < current ? '✓' : i + 1}
            </span>
            <span className="hidden sm:inline">{s}</span>
          </div>
          {i < STEPS.length - 1 && <span className="text-border-base text-xs">›</span>}
        </div>
      ))}
    </div>
  );
}

function TargetingSearch({
  tenantId, type, selected, onAdd, onRemove,
}: {
  tenantId: string;
  type: 'interests' | 'locations';
  selected: Array<{ id: string; name: string }>;
  onAdd: (item: TargetingItem) => void;
  onRemove: (id: string) => void;
}) {
  const [q, setQ] = useState('');
  const [results, setResults] = useState<TargetingItem[]>([]);
  const [loading, setLoading] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  function search(val: string) {
    setQ(val);
    if (timerRef.current) clearTimeout(timerRef.current);
    if (val.length < 2) { setResults([]); return; }
    timerRef.current = setTimeout(async () => {
      setLoading(true);
      try {
        const items = type === 'interests'
          ? await searchInterests(tenantId, val)
          : await searchLocations(tenantId, val);
        setResults(items.filter(r => !selected.find(s => s.id === r.id)));
      } catch { setResults([]); }
      finally { setLoading(false); }
    }, 400);
  }

  return (
    <div className="space-y-2">
      <div className="relative">
        <input
          className={inp}
          placeholder={type === 'interests' ? 'Cerca interessos...' : 'Cerca localitzacions...'}
          value={q}
          onChange={e => search(e.target.value)}
        />
        {loading && <span className="absolute right-2 top-2 text-xs text-ink-3">...</span>}
      </div>
      {results.length > 0 && (
        <div className="border border-border-base rounded bg-surface-raised max-h-40 overflow-y-auto">
          {results.map(r => (
            <button
              key={r.id}
              type="button"
              onClick={() => { onAdd(r); setResults([]); setQ(''); }}
              className="w-full text-left px-3 py-1.5 text-xs hover:bg-surface-hover flex justify-between"
            >
              <span className="text-ink-1">{r.name}</span>
              {r.audienceSize && <span className="text-ink-3">{(r.audienceSize / 1000).toFixed(0)}K</span>}
            </button>
          ))}
        </div>
      )}
      {selected.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {selected.map(s => (
            <span key={s.id} className="inline-flex items-center gap-1 bg-surface-raised border border-border-base rounded px-2 py-0.5 text-xs text-ink-1">
              {s.name}
              <button type="button" onClick={() => onRemove(s.id)} className="text-ink-3 hover:text-red-400">×</button>
            </span>
          ))}
        </div>
      )}
    </div>
  );
}

export default function NewCampaignPage() {
  const { id: tenantId } = useParams<{ id: string }>();
  const router = useRouter();
  const locale = useLocale();
  const qc = useQueryClient();
  const { toast } = useToast();

  const [step, setStep] = useState(0);
  const [step1, setStep1] = useState<FormStep1>({
    name: '', objective: 'OUTCOME_LEADS', dailyBudget: '', startTime: '', stopTime: '',
  });
  const [step2, setStep2] = useState<FormStep2>({
    adSetName: 'Conjunt d\'anuncis 1', ageMin: '25', ageMax: '55',
    genders: 'ALL', publisherPlatforms: 'facebook,instagram',
    geoLocations: [], interests: [],
  });
  const [step3, setStep3] = useState<FormStep3>({
    adName: '', headline: '', body: '', description: '',
    callToAction: 'LEARN_MORE', linkUrl: '',
    imageFile: null, imagePreview: null, metaImageHash: null,
  });
  const [uploading, setUploading] = useState(false);
  const [showGenModal, setShowGenModal] = useState(false);

  const createMut = useMutation({
    mutationFn: async () => {
      const campaign = await createCampaign(tenantId, {
        name: step1.name,
        objective: step1.objective,
        dailyBudget: step1.dailyBudget ? parseFloat(step1.dailyBudget) : undefined,
        startTime: step1.startTime || undefined,
        stopTime: step1.stopTime || undefined,
      });

      const adSet = await createAdSet(tenantId, campaign.id, {
        name: step2.adSetName,
        ageMin: parseInt(step2.ageMin),
        ageMax: parseInt(step2.ageMax),
        genders: step2.genders,
        publisherPlatforms: step2.publisherPlatforms,
        geoLocations: step2.geoLocations,
        interests: step2.interests,
      });

      await createAd(tenantId, adSet.id, {
        name: step3.adName || step3.headline,
        headline: step3.headline,
        body: step3.body,
        description: step3.description,
        callToAction: step3.callToAction,
        linkUrl: step3.linkUrl,
        metaImageHash: step3.metaImageHash || undefined,
      });

      return campaign;
    },
    onSuccess: (campaign) => {
      toast('success', 'Campanya creada com a esborrany');
      qc.invalidateQueries({ queryKey: ['campaigns', tenantId] });
      router.push(`/${locale}/portal/admin/tenants/${tenantId}/meta-ads/${campaign.id}`);
    },
    onError: (e: any) => toast('error', e.message),
  });

  async function handleImageUpload(file: File) {
    setUploading(true);
    try {
      const preview = URL.createObjectURL(file);
      const result = await uploadImage(tenantId, file);
      setStep3(f => ({ ...f, imageFile: file, imagePreview: preview, metaImageHash: result.hash }));
      toast('success', 'Imatge pujada a Meta');
    } catch (e: any) {
      toast('error', e.message);
    } finally {
      setUploading(false);
    }
  }

  const canNext = [
    step1.name.trim().length > 0,
    step2.adSetName.trim().length > 0,
    step3.headline.trim().length > 0 && step3.linkUrl.trim().length > 0,
    true,
  ];

  return (
    <PortalShell
      breadcrumb="admin · meta ads · nova campanya"
      backHref={`/portal/admin/tenants/${tenantId}/meta-ads`}
    >
      <div className="p-4 sm:p-8 max-w-2xl space-y-6">
        <div>
          <h1 className="text-xl font-bold text-ink-1 mb-3">Nova campanya</h1>
          <StepIndicator current={step} />
        </div>

        {/* Step 0: Objectiu */}
        {step === 0 && (
          <div className="space-y-5">
            <div>
              <label className={lbl}>Nom de la campanya *</label>
              <input
                className={inp}
                placeholder="Ex: Campanya estiu 2026 — Leads"
                value={step1.name}
                onChange={e => setStep1(f => ({ ...f, name: e.target.value }))}
              />
            </div>

            <div>
              <label className={lbl}>Objectiu *</label>
              <div className="grid grid-cols-2 gap-3">
                {OBJECTIVES.map(obj => (
                  <button
                    key={obj.value}
                    type="button"
                    onClick={() => setStep1(f => ({ ...f, objective: obj.value }))}
                    className={`text-left p-3 rounded-lg border transition-all ${
                      step1.objective === obj.value
                        ? 'border-accent-light bg-[rgba(99,102,241,0.1)]'
                        : 'border-border-base bg-surface-base hover:border-border-raised'
                    }`}
                  >
                    <div className="text-lg mb-1">{obj.icon}</div>
                    <div className="text-sm font-medium text-ink-1">{obj.label}</div>
                    <div className="text-xs text-ink-3 mt-0.5">{obj.desc}</div>
                  </button>
                ))}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className={lbl}>Pressupost diari (€)</label>
                <input
                  className={inp} type="number" min="1" step="0.01" placeholder="Ex: 5.00"
                  value={step1.dailyBudget}
                  onChange={e => setStep1(f => ({ ...f, dailyBudget: e.target.value }))}
                />
              </div>
              <div />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className={lbl}>Data inici</label>
                <input className={inp} type="date" value={step1.startTime}
                  onChange={e => setStep1(f => ({ ...f, startTime: e.target.value }))} />
              </div>
              <div>
                <label className={lbl}>Data fi</label>
                <input className={inp} type="date" value={step1.stopTime}
                  onChange={e => setStep1(f => ({ ...f, stopTime: e.target.value }))} />
              </div>
            </div>
          </div>
        )}

        {/* Step 1: Públic */}
        {step === 1 && (
          <div className="space-y-5">
            <div>
              <label className={lbl}>Nom del conjunt d'anuncis *</label>
              <input className={inp} value={step2.adSetName}
                onChange={e => setStep2(f => ({ ...f, adSetName: e.target.value }))} />
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div>
                <label className={lbl}>Edat mínima</label>
                <input className={inp} type="number" min="18" max="65" value={step2.ageMin}
                  onChange={e => setStep2(f => ({ ...f, ageMin: e.target.value }))} />
              </div>
              <div>
                <label className={lbl}>Edat màxima</label>
                <input className={inp} type="number" min="18" max="65" value={step2.ageMax}
                  onChange={e => setStep2(f => ({ ...f, ageMax: e.target.value }))} />
              </div>
              <div>
                <label className={lbl}>Gènere</label>
                <select className={inp} value={step2.genders}
                  onChange={e => setStep2(f => ({ ...f, genders: e.target.value }))}>
                  <option value="ALL">Tots</option>
                  <option value="MALE">Homes</option>
                  <option value="FEMALE">Dones</option>
                </select>
              </div>
            </div>

            <div>
              <label className={lbl}>Plataformes</label>
              <div className="flex gap-3">
                {['facebook', 'instagram', 'messenger', 'audience_network'].map(p => {
                  const platforms = step2.publisherPlatforms.split(',').filter(Boolean);
                  const active = platforms.includes(p);
                  return (
                    <label key={p} className="flex items-center gap-1.5 text-xs cursor-pointer">
                      <input
                        type="checkbox"
                        checked={active}
                        onChange={() => {
                          const updated = active ? platforms.filter(x => x !== p) : [...platforms, p];
                          setStep2(f => ({ ...f, publisherPlatforms: updated.join(',') }));
                        }}
                        className="accent-accent-light"
                      />
                      <span className="text-ink-1 capitalize">{p.replace('_', ' ')}</span>
                    </label>
                  );
                })}
              </div>
            </div>

            <div>
              <label className={lbl}>Localitzacions</label>
              <TargetingSearch
                tenantId={tenantId}
                type="locations"
                selected={step2.geoLocations.map(g => ({ id: g.key, name: g.name }))}
                onAdd={item => setStep2(f => ({
                  ...f,
                  geoLocations: [...f.geoLocations, { key: item.id, name: item.name }],
                }))}
                onRemove={id => setStep2(f => ({
                  ...f,
                  geoLocations: f.geoLocations.filter(g => g.key !== id),
                }))}
              />
            </div>

            <div>
              <label className={lbl}>Interessos</label>
              <TargetingSearch
                tenantId={tenantId}
                type="interests"
                selected={step2.interests}
                onAdd={item => setStep2(f => ({
                  ...f,
                  interests: [...f.interests, { id: item.id, name: item.name }],
                }))}
                onRemove={id => setStep2(f => ({
                  ...f,
                  interests: f.interests.filter(i => i.id !== id),
                }))}
              />
            </div>
          </div>
        )}

        {/* Step 2: Creatiu */}
        {step === 2 && (
          <div className="space-y-4">
            <div>
              <label className={lbl}>Nom de l'anunci</label>
              <input className={inp} placeholder="Ex: Anunci imatge principal"
                value={step3.adName}
                onChange={e => setStep3(f => ({ ...f, adName: e.target.value }))} />
            </div>

            {showGenModal && (
              <GenerateImageModal
                tenantId={tenantId}
                initialPrompt={step3.headline || step1.name}
                onAccept={r => setStep3(f => ({ ...f, imagePreview: r.url, metaImageHash: r.hash }))}
                onClose={() => setShowGenModal(false)}
              />
            )}

            <div>
              <div className="flex items-center justify-between mb-1">
                <label className={lbl}>Imatge</label>
                <button
                  type="button"
                  onClick={() => setShowGenModal(true)}
                  className="flex items-center gap-1.5 text-xs text-[#FF6B00] hover:text-[#FF9A3C] font-medium transition-colors"
                >
                  <IconSet.Sparkles size={13} />
                  Genera amb IA
                </button>
              </div>
              {step3.imagePreview ? (
                <div className="flex items-center gap-3">
                  <img src={step3.imagePreview} alt="preview" className="w-24 h-16 object-cover rounded border border-border-base" />
                  <div className="space-y-1">
                    <p className="text-xs text-[#39d353]">✓ Pujada a Meta</p>
                    <p className="text-xs text-ink-3 font-mono">{step3.metaImageHash?.slice(0, 16)}...</p>
                    <div className="flex gap-2">
                      <button type="button" onClick={() => setShowGenModal(true)}
                        className="text-xs text-[#FF6B00] hover:underline">
                        Regenerar
                      </button>
                      <span className="text-ink-3">·</span>
                      <button type="button" onClick={() => setStep3(f => ({ ...f, imageFile: null, imagePreview: null, metaImageHash: null }))}
                        className="text-xs text-red-400 hover:underline">
                        Eliminar
                      </button>
                    </div>
                  </div>
                </div>
              ) : (
                <label className="flex flex-col items-center justify-center border-2 border-dashed border-border-base rounded-lg p-6 cursor-pointer hover:border-accent-light transition-colors">
                  {uploading ? (
                    <span className="text-xs text-ink-3">Pujant...</span>
                  ) : (
                    <>
                      <IconSet.Upload size={20} className="text-ink-3 mb-2" />
                      <span className="text-xs text-ink-2">Fes clic per seleccionar una imatge</span>
                      <span className="text-xs text-ink-3 mt-0.5">JPG, PNG — recomanat 1200×628px</span>
                    </>
                  )}
                  <input type="file" accept="image/*" className="hidden"
                    onChange={e => e.target.files?.[0] && handleImageUpload(e.target.files[0])}
                    disabled={uploading} />
                </label>
              )}
            </div>

            <div>
              <label className={lbl}>Títol principal *</label>
              <input className={inp} placeholder="Ex: Reforma el teu bany aquest estiu" maxLength={255}
                value={step3.headline}
                onChange={e => setStep3(f => ({ ...f, headline: e.target.value }))} />
            </div>

            <div>
              <label className={lbl}>Text de l'anunci</label>
              <textarea className={inp + ' resize-none'} rows={3} maxLength={600}
                placeholder="Cos del missatge que veurà l'usuari..."
                value={step3.body}
                onChange={e => setStep3(f => ({ ...f, body: e.target.value }))} />
              <p className="text-xs text-ink-3 mt-0.5 text-right">{step3.body.length}/600</p>
            </div>

            <div>
              <label className={lbl}>Descripció</label>
              <input className={inp} placeholder="Text addicional sota el títol" maxLength={255}
                value={step3.description}
                onChange={e => setStep3(f => ({ ...f, description: e.target.value }))} />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className={lbl}>URL destí *</label>
                <input className={inp} type="url" placeholder="https://exemple.com"
                  value={step3.linkUrl}
                  onChange={e => setStep3(f => ({ ...f, linkUrl: e.target.value }))} />
              </div>
              <div>
                <label className={lbl}>Botó (CTA)</label>
                <select className={inp} value={step3.callToAction}
                  onChange={e => setStep3(f => ({ ...f, callToAction: e.target.value }))}>
                  {CTA_OPTIONS.map(c => <option key={c} value={c}>{c.replace(/_/g, ' ')}</option>)}
                </select>
              </div>
            </div>
          </div>
        )}

        {/* Step 3: Revisió */}
        {step === 3 && (
          <div className="space-y-5">
            <div className="bg-surface-base border border-border-base rounded-lg p-4 space-y-3 text-sm">
              <h2 className="text-xs font-mono uppercase tracking-wider text-ink-3 mb-3">Resum</h2>

              <div className="space-y-2">
                <div className="flex justify-between">
                  <span className="text-ink-3">Nom</span>
                  <span className="text-ink-1 font-medium">{step1.name}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-ink-3">Objectiu</span>
                  <span className="text-ink-1">{OBJECTIVES.find(o => o.value === step1.objective)?.label}</span>
                </div>
                {step1.dailyBudget && (
                  <div className="flex justify-between">
                    <span className="text-ink-3">Pressupost diari</span>
                    <span className="text-ink-1">{step1.dailyBudget} €</span>
                  </div>
                )}
              </div>

              <hr className="border-border-base" />

              <div className="space-y-1.5">
                <span className="text-xs text-ink-3">Ad Set: {step2.adSetName}</span>
                <div className="flex gap-4 text-xs">
                  <span><span className="text-ink-3">Edat: </span><span className="text-ink-1">{step2.ageMin}–{step2.ageMax}</span></span>
                  <span><span className="text-ink-3">Gènere: </span><span className="text-ink-1">{{ ALL: 'Tots', MALE: 'Homes', FEMALE: 'Dones' }[step2.genders] ?? step2.genders}</span></span>
                </div>
                {step2.geoLocations.length > 0 && (
                  <p className="text-xs"><span className="text-ink-3">Localitzacions: </span><span className="text-ink-1">{step2.geoLocations.map(g => g.name).join(', ')}</span></p>
                )}
                {step2.interests.length > 0 && (
                  <p className="text-xs"><span className="text-ink-3">Interessos: </span><span className="text-ink-1">{step2.interests.map(i => i.name).join(', ')}</span></p>
                )}
              </div>

              <hr className="border-border-base" />

              <div className="space-y-1.5">
                {step3.imagePreview && (
                  <img src={step3.imagePreview} alt="preview" className="w-full max-w-xs h-24 object-cover rounded border border-border-base" />
                )}
                <div className="flex justify-between text-xs">
                  <span className="text-ink-3">Títol</span>
                  <span className="text-ink-1 font-medium">{step3.headline}</span>
                </div>
                {step3.body && (
                  <p className="text-xs text-ink-2 bg-surface-raised rounded p-2">{step3.body}</p>
                )}
                <div className="flex justify-between text-xs">
                  <span className="text-ink-3">URL</span>
                  <span className="text-ink-1 truncate max-w-48">{step3.linkUrl}</span>
                </div>
              </div>
            </div>

            <p className="text-xs text-ink-3">
              La campanya es desarà com a <strong className="text-ink-2">esborrany</strong>. Podràs revisar-la i publicar-la des del detall de la campanya.
            </p>
          </div>
        )}

        {/* Navigation */}
        <div className="flex justify-between pt-2 border-t border-border-base">
          <AMGButton variant="ghost" onClick={() => step === 0 ? router.back() : setStep(s => s - 1)}>
            {step === 0 ? 'Cancel·lar' : '← Enrere'}
          </AMGButton>

          {step < 3 ? (
            <AMGButton disabled={!canNext[step]} onClick={() => setStep(s => s + 1)}>
              Continuar →
            </AMGButton>
          ) : (
            <AMGButton disabled={createMut.isPending} onClick={() => createMut.mutate()}>
              {createMut.isPending ? 'Creant...' : 'Crear campanya'}
            </AMGButton>
          )}
        </div>
      </div>
    </PortalShell>
  );
}
