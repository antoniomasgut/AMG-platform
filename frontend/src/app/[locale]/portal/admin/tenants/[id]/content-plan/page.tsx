'use client';

import { useState } from 'react';
import { useParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { useToast } from '@/lib/toast-context';
import { getTenant } from '@/services/admin';
import {
  listPlans, createPlan, generatePlan, activatePlan, deletePlan, updateItem,
  getDefaultLanguage, setDefaultLanguage,
  type ContentPlan, type ContentPlanItem, type ContentItemStatus,
} from '@/services/content-plan';

const LANGS = ['ca', 'es', 'en', 'de'] as const;
const CHANNELS = ['INSTAGRAM', 'FACEBOOK', 'GOOGLE_BUSINESS', 'GOOGLE_PHOTO'] as const;

const STATUS_TONE: Record<ContentItemStatus, string> = {
  PLANNED: 'bg-slate-100 text-slate-600',
  PHOTO_REQUESTED: 'bg-amber-100 text-amber-700',
  PHOTO_RECEIVED: 'bg-blue-100 text-blue-700',
  AWAITING_APPROVAL: 'bg-violet-100 text-violet-700',
  PUBLISHED: 'bg-green-100 text-green-700',
  FAILED: 'bg-red-100 text-red-700',
  SKIPPED: 'bg-slate-100 text-slate-400',
};

export default function ContentPlanAdminPage() {
  const { id } = useParams<{ id: string }>();
  const t = useTranslations('contentPlan');
  const { toast } = useToast();
  const qc = useQueryClient();

  const currentMonth = new Date().toISOString().slice(0, 7);
  const [period, setPeriod] = useState(currentMonth);
  const [language, setLanguage] = useState('ca');

  const { data: tenant } = useQuery({ queryKey: ['tenant', id], queryFn: () => getTenant(id) });
  const { data: plans = [] } = useQuery({ queryKey: ['content-plans', id], queryFn: () => listPlans(id) });
  const { data: defaultLang } = useQuery({
    queryKey: ['content-default-lang', id],
    queryFn: () => getDefaultLanguage(id),
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ['content-plans', id] });

  const createMut = useMutation({
    mutationFn: () => createPlan(id, { period, contentLanguage: language, generate: true }),
    onSuccess: () => { toast('success', t('planCreated')); invalidate(); },
    onError: () => toast('error', t('error')),
  });
  const generateMut = useMutation({
    mutationFn: (planId: string) => generatePlan(planId),
    onSuccess: invalidate, onError: () => toast('error', t('error')),
  });
  const activateMut = useMutation({
    mutationFn: (planId: string) => activatePlan(planId),
    onSuccess: () => { toast('success', t('planActivated')); invalidate(); },
    onError: () => toast('error', t('error')),
  });
  const deleteMut = useMutation({
    mutationFn: (planId: string) => deletePlan(planId),
    onSuccess: invalidate, onError: () => toast('error', t('error')),
  });
  const defaultLangMut = useMutation({
    mutationFn: (lang: string) => setDefaultLanguage(id, lang),
    onSuccess: () => { toast('success', t('save')); qc.invalidateQueries({ queryKey: ['content-default-lang', id] }); },
    onError: () => toast('error', t('error')),
  });
  const updateItemMut = useMutation({
    mutationFn: ({ itemId, networks }: { itemId: string; networks: string }) => updateItem(itemId, { networks }),
    onSuccess: invalidate, onError: () => toast('error', t('error')),
  });

  const toggleChannel = (item: ContentPlanItem, ch: string) => {
    const set = new Set((item.networks ?? '').split(',').map((s) => s.trim()).filter(Boolean));
    set.has(ch) ? set.delete(ch) : set.add(ch);
    updateItemMut.mutate({ itemId: item.id, networks: Array.from(set).join(',') });
  };

  return (
    <PortalShell breadcrumb={t('title')} backHref={`/portal/admin/tenants/${id}`}>
      <div className="mx-auto max-w-4xl space-y-6 p-4">
        <header>
          <h1 className="text-xl font-semibold text-ink-0">{t('title')}</h1>
          <p className="text-sm text-ink-3">{tenant?.name} · {t('subtitle')}</p>
        </header>

        {/* Idioma per defecte del tenant */}
        <section className="rounded-xl border border-line bg-surface-1 p-4">
          <label className="f-mono text-[10px] uppercase tracking-widest text-ink-3">{t('defaultLanguage')}</label>
          <div className="mt-2 flex gap-2">
            {LANGS.map((l) => (
              <button
                key={l}
                onClick={() => defaultLangMut.mutate(l)}
                className={`rounded-md px-3 py-1 text-sm uppercase ${
                  defaultLang?.language === l ? 'bg-accent text-white' : 'bg-surface-2 text-ink-2'
                }`}
              >
                {l}
              </button>
            ))}
          </div>
        </section>

        {/* Nou pla */}
        <section className="rounded-xl border border-line bg-surface-1 p-4">
          <h2 className="mb-3 text-sm font-semibold text-ink-1">{t('newPlan')}</h2>
          <div className="flex flex-wrap items-end gap-3">
            <div>
              <label className="f-mono text-[10px] uppercase tracking-widest text-ink-3">{t('period')}</label>
              <input type="month" value={period} onChange={(e) => setPeriod(e.target.value)}
                className="mt-1 block rounded-md border border-line bg-surface-2 px-3 py-1.5 text-sm" />
            </div>
            <div>
              <label className="f-mono text-[10px] uppercase tracking-widest text-ink-3">{t('language')}</label>
              <select value={language} onChange={(e) => setLanguage(e.target.value)}
                className="mt-1 block rounded-md border border-line bg-surface-2 px-3 py-1.5 text-sm uppercase">
                {LANGS.map((l) => <option key={l} value={l}>{l}</option>)}
              </select>
            </div>
            <AMGButton onClick={() => createMut.mutate()} loading={createMut.isPending}>
              {t('newPlan')} + {t('generate')}
            </AMGButton>
          </div>
        </section>

        {/* Plans */}
        {plans.length === 0 ? (
          <p className="text-sm text-ink-3">{t('noPlans')} — {t('createFirst')}</p>
        ) : (
          plans.map((plan: ContentPlan) => (
            <section key={plan.id} className="rounded-xl border border-line bg-surface-1 p-4">
              <div className="mb-3 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-semibold text-ink-0">{plan.period}</span>
                  <span className="rounded-full bg-surface-2 px-2 py-0.5 text-[11px] uppercase text-ink-2">
                    {plan.status === 'ACTIVE' ? t('active') : plan.status === 'DONE' ? t('done') : t('draft')}
                  </span>
                  <span className="text-[11px] uppercase text-ink-3">{plan.contentLanguage}</span>
                </div>
                <div className="flex gap-2">
                  <AMGButton variant="ghost" onClick={() => generateMut.mutate(plan.id)}>{t('generate')}</AMGButton>
                  {plan.status !== 'ACTIVE' && (
                    <AMGButton onClick={() => activateMut.mutate(plan.id)}>{t('activate')}</AMGButton>
                  )}
                  <AMGButton variant="ghost" onClick={() => deleteMut.mutate(plan.id)}>{t('delete')}</AMGButton>
                </div>
              </div>
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-[11px] uppercase text-ink-3">
                    <th className="py-1">{t('week')}</th>
                    <th>{t('pillar')}</th>
                    <th>{t('brief')}</th>
                    <th>{t('channelsLabel')}</th>
                    <th>{t('deadline')}</th>
                    <th>{t('status')}</th>
                  </tr>
                </thead>
                <tbody>
                  {plan.items.map((it) => {
                    const active = new Set((it.networks ?? '').split(',').map((s) => s.trim()));
                    return (
                    <tr key={it.id} className="border-t border-line/50 align-top">
                      <td className="py-2">{it.weekNumber}</td>
                      <td>{t(`pillars.${it.pillar}`)}</td>
                      <td className="max-w-[240px] text-ink-2">{it.briefText}</td>
                      <td>
                        <div className="flex flex-wrap gap-1">
                          {CHANNELS.map((ch) => (
                            <button
                              key={ch}
                              onClick={() => toggleChannel(it, ch)}
                              className={`rounded-full px-2 py-0.5 text-[10px] ${
                                active.has(ch) ? 'bg-accent text-white' : 'bg-surface-2 text-ink-3 line-through'
                              }`}
                            >
                              {t(`channels.${ch}`)}
                            </button>
                          ))}
                        </div>
                      </td>
                      <td className="whitespace-nowrap text-ink-3">{it.photoDeadline}</td>
                      <td>
                        <span className={`rounded-full px-2 py-0.5 text-[11px] ${STATUS_TONE[it.status]}`}>
                          {t(`statuses.${it.status}`)}
                        </span>
                      </td>
                    </tr>
                    );
                  })}
                </tbody>
              </table>
            </section>
          ))
        )}
      </div>
    </PortalShell>
  );
}
