'use client';

import { useTranslations } from 'next-intl';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { PortalShell } from '@/components/portal/PortalShell';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import {
  getPendingItems, uploadItemPhoto, listPlans,
  type ContentPlanItem, type ContentItemStatus,
} from '@/services/content-plan';

const STATUS_TONE: Record<ContentItemStatus, string> = {
  PLANNED: 'bg-slate-100 text-slate-600',
  PHOTO_REQUESTED: 'bg-amber-100 text-amber-700',
  PHOTO_RECEIVED: 'bg-blue-100 text-blue-700',
  AWAITING_APPROVAL: 'bg-violet-100 text-violet-700',
  PUBLISHED: 'bg-green-100 text-green-700',
  FAILED: 'bg-red-100 text-red-700',
  SKIPPED: 'bg-slate-100 text-slate-400',
};

export default function ContentPlanTenantPage() {
  const t = useTranslations('contentPlan');
  const { user } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();
  const tenantId = user?.tenantId ?? '';

  const { data: pending = [] } = useQuery({
    queryKey: ['content-pending', tenantId],
    queryFn: () => getPendingItems(tenantId),
    enabled: !!tenantId,
  });
  const { data: plans = [] } = useQuery({
    queryKey: ['content-plans', tenantId],
    queryFn: () => listPlans(tenantId),
    enabled: !!tenantId,
  });

  const activePlan = plans.find((p) => p.status === 'ACTIVE') ?? plans[0];

  const uploadMut = useMutation({
    mutationFn: ({ itemId, file }: { itemId: string; file: File }) => uploadItemPhoto(itemId, file),
    onSuccess: () => {
      toast('success', t('photoUploaded'));
      qc.invalidateQueries({ queryKey: ['content-pending', tenantId] });
      qc.invalidateQueries({ queryKey: ['content-plans', tenantId] });
    },
    onError: () => toast('error', t('error')),
  });

  const onPick = (itemId: string) => (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) uploadMut.mutate({ itemId, file });
  };

  return (
    <PortalShell breadcrumb={t('title')}>
      <div className="mx-auto max-w-3xl space-y-6 p-4">
        <header>
          <h1 className="text-xl font-semibold text-ink-0">{t('title')}</h1>
          <p className="text-sm text-ink-3">{t('subtitle')}</p>
        </header>

        {/* Aquesta setmana / fotos pendents */}
        <section className="rounded-xl border border-line bg-surface-1 p-4">
          <h2 className="mb-3 text-sm font-semibold text-ink-1">📸 {t('thisWeek')}</h2>
          {pending.length === 0 ? (
            <p className="text-sm text-ink-3">{t('noPending')}</p>
          ) : (
            <ul className="space-y-3">
              {pending.map((it: ContentPlanItem) => (
                <li key={it.id} className="rounded-lg border border-line bg-surface-2 p-3">
                  <div className="text-sm font-semibold text-ink-1">{t(`pillars.${it.pillar}`)}</div>
                  <p className="mt-1 text-sm text-ink-2">{it.briefText}</p>
                  {it.exampleText && <p className="text-xs italic text-ink-3">{it.exampleText}</p>}
                  {it.photoDeadline && (
                    <p className="mt-1 text-xs text-amber-600">🗓️ {t('sendBefore')} {it.photoDeadline}</p>
                  )}
                  <label className="mt-2 inline-block cursor-pointer rounded-md bg-accent px-3 py-1.5 text-sm text-white">
                    {t('uploadPhoto')}
                    <input type="file" accept="image/*" className="hidden"
                      onChange={onPick(it.id)} disabled={uploadMut.isPending} />
                  </label>
                </li>
              ))}
            </ul>
          )}
        </section>

        {/* Pla del mes */}
        {activePlan && (
          <section className="rounded-xl border border-line bg-surface-1 p-4">
            <h2 className="mb-3 text-sm font-semibold text-ink-1">{activePlan.period}</h2>
            <ul className="space-y-2">
              {activePlan.items.map((it) => (
                <li key={it.id} className="flex items-center justify-between text-sm">
                  <span className="text-ink-2">
                    {t('week')} {it.weekNumber} · {t(`pillars.${it.pillar}`)}
                  </span>
                  <span className={`rounded-full px-2 py-0.5 text-[11px] ${STATUS_TONE[it.status]}`}>
                    {t(`statuses.${it.status}`)}
                  </span>
                </li>
              ))}
            </ul>
          </section>
        )}
      </div>
    </PortalShell>
  );
}
