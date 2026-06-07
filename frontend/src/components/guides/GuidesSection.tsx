'use client';

import { useTranslations } from 'next-intl';
import { useQuery } from '@tanstack/react-query';
import { getTenantSetup } from '@/services/admin';
import { getGuideConfig, getAllGuideConfigs } from '@/config/service-guides';
import { IconSet } from '@/components/ui/icons';
import { AMGButton } from '@/components/ui/button';
import { Link } from '@/i18n/navigation';

interface GuidesSectionProps {
  tenantId: string;
}

const SERVICE_ICONS: Record<string, React.FC<{ size?: number; className?: string }>> = {
  LANDING: IconSet.Globe,
  WHATSAPP: IconSet.Smartphone,
  SMTP: IconSet.Mail,
  BOT_IA: IconSet.Bot,
  AUTOMATION: IconSet.Zap,
  DOMAIN: IconSet.Link,
};

export function GuidesSection({ tenantId }: GuidesSectionProps) {
  const t = useTranslations();

  const { data: setup } = useQuery({
    queryKey: ['tenant-setup-guides-section', tenantId],
    queryFn: () => getTenantSetup(tenantId),
    enabled: !!tenantId,
  });

  const allGuideConfigs = getAllGuideConfigs();
  const guideTypes = new Set(allGuideConfigs.map((g) => g.type));

  const readyServices = setup
    ? [
        ...(setup.profiles ?? []).flatMap((p) =>
          (p.phases ?? []).flatMap((ph) =>
            (ph.services ?? [])
              .filter((s) => s.status === 'READY_FOR_DELIVERY' && guideTypes.has(s.service.type))
              .map((s) => ({
                serviceId: s.service.id,
                type: s.service.type,
                name: s.service.name,
                status: s.status,
              }))
          )
        ),
        ...(setup.standalone ?? [])
          .filter((s) => s.status === 'READY_FOR_DELIVERY' && guideTypes.has(s.service.type))
          .map((s) => ({
            serviceId: s.service.id,
            type: s.service.type,
            name: s.service.name,
            status: s.status,
          })),
      ].slice(0, 3)
    : [];

  if (readyServices.length === 0) {
    return null;
  }

  return (
    <div className="amg-card card-clip p-4 sm:p-5">
      <div className="flex items-center justify-between mb-4">
        <div>
          <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3">{t('guides.title')}</div>
          <div className="f-display font-bold text-sm mt-0.5">{t('guides.subtitle')}</div>
        </div>
        <Link href="/portal/serveis">
          <span className="f-mono text-[10px] uppercase text-accent-light hover:underline cursor-pointer">
            {t('guides.view_all')}
          </span>
        </Link>
      </div>
      <div className="space-y-2">
        {readyServices.map((svc) => {
          const Icon = SERVICE_ICONS[svc.type] || IconSet.Box;
          return (
            <div key={svc.serviceId} className="flex items-center gap-3 py-2 border-b border-border-subtle last:border-0">
              <div className="w-8 h-8 rounded bg-accent-muted flex items-center justify-center shrink-0">
                <Icon size={14} className="text-accent-light" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="f-display font-semibold text-xs truncate">{svc.name}</div>
                <div className="flex items-center gap-1.5 mt-0.5">
                  <IconSet.Check size={10} className="text-[#39d353] shrink-0" />
                  <span className="f-mono text-[9px] uppercase text-ink-2">{t('guides.service_ready')}</span>
                </div>
              </div>
              <Link href={`/portal/serveis/${svc.type}/${svc.serviceId}`}>
                <AMGButton size="sm" variant="ghost" icon={IconSet.ArrowRight}>
                  {t('guides.view_guide')}
                </AMGButton>
              </Link>
            </div>
          );
        })}
      </div>
    </div>
  );
}
