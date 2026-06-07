'use client';

import { useState, useEffect } from 'react';
import { useTranslations } from 'next-intl';
import { useQuery } from '@tanstack/react-query';
import { getTenantSetup } from '@/services/admin';
import { getAllGuideConfigs } from '@/config/service-guides';
import { IconSet } from '@/components/ui/icons';
import { AMGButton } from '@/components/ui/button';
import { Link } from '@/i18n/navigation';

interface ServiceDeliveryAlertProps {
  tenantId: string;
}

export function ServiceDeliveryAlert({ tenantId }: ServiceDeliveryAlertProps) {
  const t = useTranslations();
  const [dismissed, setDismissed] = useState(false);

  const { data: setup } = useQuery({
    queryKey: ['tenant-setup-alert', tenantId],
    queryFn: () => getTenantSetup(tenantId),
    enabled: !!tenantId,
  });

  const allGuideConfigs = getAllGuideConfigs();
  const guideTypes = new Set(allGuideConfigs.map((g) => g.type));

  const newServices = setup
    ? [
        ...(setup.profiles ?? []).flatMap((p) =>
          (p.phases ?? []).flatMap((ph) =>
            (ph.services ?? [])
              .filter((s) => s.status === 'READY_FOR_DELIVERY' && guideTypes.has(s.service.type))
              .map((s) => ({
                serviceId: s.service.id,
                name: s.service.name,
              }))
          )
        ),
        ...(setup.standalone ?? [])
          .filter((s) => s.status === 'READY_FOR_DELIVERY' && guideTypes.has(s.service.type))
          .map((s) => ({
            serviceId: s.service.id,
            name: s.service.name,
          })),
      ]
    : [];

  useEffect(() => {
    if (newServices.length === 0) return;
    const seen = newServices.every((s) =>
      localStorage.getItem(`amg_delivery_seen_${s.serviceId}`) === 'true'
    );
    if (seen) setDismissed(true);
  }, [newServices]);

  const handleDismiss = () => {
    newServices.forEach((s) => {
      localStorage.setItem(`amg_delivery_seen_${s.serviceId}`, 'true');
    });
    setDismissed(true);
  };

  if (dismissed || newServices.length === 0) return null;

  return (
    <div className="amg-card card-clip p-4 sm:p-5 border border-[#39d353]/30 bg-[#39d353]/5">
      <div className="flex items-start gap-3">
        <IconSet.Sparkles size={20} className="text-[#39d353] shrink-0 mt-0.5" />
        <div className="flex-1">
          <div className="f-display font-bold text-sm">{t('guides.new_services_title')}</div>
          <ul className="mt-2 space-y-1">
            {newServices.map((svc) => (
              <li key={svc.serviceId} className="flex items-center gap-2 text-sm text-ink-1">
                <IconSet.Check size={12} className="text-[#39d353] shrink-0" />
                {svc.name}
              </li>
            ))}
          </ul>
          <div className="flex gap-2 mt-4">
            <Link href="/portal/serveis">
              <AMGButton size="sm" icon={IconSet.ArrowRight}>
                {t('guides.new_services_cta')}
              </AMGButton>
            </Link>
            <AMGButton size="sm" variant="ghost" onClick={handleDismiss}>
              {t('guides.dismiss')}
            </AMGButton>
          </div>
        </div>
      </div>
    </div>
  );
}
