'use client';

import { useTranslations } from 'next-intl';
import { useQuery } from '@tanstack/react-query';
import { getTenantSetup } from '@/services/admin';
import { getGuideConfig, getAllGuideConfigs } from '@/config/service-guides';
import { IconSet } from '@/components/ui/icons';
import { AMGButton } from '@/components/ui/button';
import { Link } from '@/i18n/navigation';

interface ServiceGuidesListProps {
  tenantId: string;
  userRole: string;
}

const SERVICE_ICONS: Record<string, React.FC<{ size?: number; className?: string }>> = {
  LANDING: IconSet.Globe,
  WHATSAPP: IconSet.Smartphone,
  SMTP: IconSet.Mail,
  BOT_IA: IconSet.Bot,
  AUTOMATION: IconSet.Zap,
  DOMAIN: IconSet.Link,
};

export function ServiceGuidesList({ tenantId, userRole }: ServiceGuidesListProps) {
  const t = useTranslations();

  const { data: setup, isLoading } = useQuery({
    queryKey: ['tenant-setup-guides', tenantId],
    queryFn: () => getTenantSetup(tenantId),
    enabled: !!tenantId,
  });

  const allGuideConfigs = getAllGuideConfigs();
  const guideTypes = new Set(allGuideConfigs.map((g) => g.type));

  const servicesWithGuides = setup
    ? [
        ...(setup.profiles ?? []).flatMap((p) =>
          (p.phases ?? []).flatMap((ph) =>
            (ph.services ?? [])
              .filter((s) => guideTypes.has(s.service.type))
              .map((s) => ({
                id: s.tenantServiceId,
                serviceId: s.service.id,
                type: s.service.type,
                name: s.service.name,
                status: s.status,
                isEnabled: s.isEnabled,
              }))
          )
        ),
        ...(setup.standalone ?? [])
          .filter((s) => guideTypes.has(s.service.type))
          .map((s) => ({
            id: s.tenantServiceId,
            serviceId: s.service.id,
            type: s.service.type,
            name: s.service.name,
            status: s.status,
            isEnabled: s.isEnabled,
          })),
      ]
    : [];

  if (isLoading) {
    return (
      <div className="space-y-3">
        {[...Array(3)].map((_, idx) => (
          <div key={idx} className="h-16 animate-pulse bg-[#212140] rounded" />
        ))}
      </div>
    );
  }

  if (servicesWithGuides.length === 0) {
    return (
      <div className="amg-card card-clip p-8 text-center">
        <IconSet.Book size={32} className="mx-auto mb-3 text-ink-3" />
        <div className="f-display font-bold text-sm mb-1">{t('guides.no_services')}</div>
        <p className="text-ink-2 text-sm">{t('guides.no_services_desc')}</p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {servicesWithGuides.map((svc) => {
        const Icon = SERVICE_ICONS[svc.type] || IconSet.Box;
        const isReady = svc.status === 'READY_FOR_DELIVERY';
        return (
          <div key={svc.id} className="amg-card card-clip p-4 flex items-center gap-4">
            <div className={`w-9 h-9 rounded flex items-center justify-center shrink-0 ${isReady ? 'bg-accent-muted' : 'bg-[#212140]'}`}>
              <Icon size={16} className={isReady ? 'text-accent-light' : 'text-ink-3'} />
            </div>
            <div className="flex-1 min-w-0">
              <div className="f-display font-semibold text-sm truncate">{svc.name}</div>
              <div className="flex items-center gap-2 mt-0.5">
                <span className={`w-1.5 h-1.5 rounded-full ${isReady ? 'bg-[#39d353]' : 'bg-ink-3'}`} />
                <span className="f-mono text-[10px] uppercase text-ink-2">
                  {isReady ? t('guides.status_ready') : t('guides.status_configuring')}
                </span>
              </div>
            </div>
            <Link href={`/portal/serveis/${svc.type}/${svc.serviceId}`}>
              <AMGButton size="sm" variant="outline" icon={IconSet.ArrowRight}>
                {t('guides.view_guide')}
              </AMGButton>
            </Link>
          </div>
        );
      })}
    </div>
  );
}
