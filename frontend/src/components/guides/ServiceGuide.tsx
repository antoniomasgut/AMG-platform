'use client';

import { useTranslations } from 'next-intl';
import { getGuideConfig } from '@/config/service-guides';
import { IconSet } from '@/components/ui/icons';
import { AMGButton } from '@/components/ui/button';
import { Link } from '@/i18n/navigation';
import type { GuideSection } from '@/config/service-guides';

interface ServiceGuideProps {
  serviceType: string;
  serviceId?: string;
  tenantId: string;
  userRole: 'SUPER_ADMIN' | 'ADMIN' | 'CLIENT';
}

function SectionRenderer({ section, t }: { section: GuideSection; t: (key: string) => string }) {
  const title = t(section.titleKey);
  const typeIcons: Record<string, React.ReactNode> = {
    text: <IconSet.FileText size={14} className="shrink-0 mt-0.5" />,
    credentials: <IconSet.Key size={14} className="shrink-0 mt-0.5" />,
    link: <IconSet.Link size={14} className="shrink-0 mt-0.5" />,
    steps: <IconSet.Layers size={14} className="shrink-0 mt-0.5" />,
    warning: <IconSet.AlertTriangle size={14} className="shrink-0 mt-0.5 text-[#f0b429]" />,
    info: <IconSet.AlertCircle size={14} className="shrink-0 mt-0.5 text-accent-light" />,
  };

  const borderColors: Record<string, string> = {
    text: 'border-border-base',
    credentials: 'border-[#f0b429]/40',
    link: 'border-accent-muted',
    steps: 'border-border-base',
    warning: 'border-[#f0b429]/40',
    info: 'border-accent-muted',
  };

  return (
    <div className={`amg-card card-clip p-4 border ${borderColors[section.type] || 'border-border-base'}`}>
      <div className="flex items-center gap-2 mb-2">
        {typeIcons[section.type] || null}
        <div className="f-mono text-[10px] uppercase tracking-widest text-ink-2">{title}</div>
      </div>
      <div className="text-sm text-ink-1 leading-relaxed">
        {section.type === 'steps' && Array.isArray(section.content) ? (
          <ol className="list-decimal list-inside space-y-2">
            {section.content.map((step, idx) => (
              <li key={idx} className="text-sm text-ink-1">{t(step)}</li>
            ))}
          </ol>
        ) : section.type === 'link' && typeof section.content === 'string' ? (
          <a
            href={t(section.content)}
            target="_blank"
            rel="noopener noreferrer"
            className="text-accent-light hover:underline break-all"
          >
            {t(section.content)}
          </a>
        ) : section.type === 'credentials' && Array.isArray(section.content) ? (
          <div className="space-y-2">
            {section.content.map((field, idx) => (
              <div key={idx} className="flex flex-col gap-0.5">
                <span className="f-mono text-[10px] text-ink-3 uppercase">{field}</span>
                <div className="flex items-center gap-2">
                  <span className="font-mono text-xs text-ink-0 bg-[#0d0d1a] px-2 py-1 rounded border border-border-base break-all">
                    ••••••••
                  </span>
                </div>
              </div>
            ))}
          </div>
        ) : typeof section.content === 'string' ? (
          <p>{t(section.content)}</p>
        ) : null}
      </div>
    </div>
  );
}

function FaqItem({ questionKey, answerKey, t }: { questionKey: string; answerKey: string; t: (key: string) => string }) {
  return (
    <details className="group">
      <summary className="flex items-center gap-2 cursor-pointer py-2 text-sm text-ink-1 hover:text-accent-light transition-colors">
        <IconSet.Chevron size={12} className="shrink-0 group-open:rotate-90 transition-transform" />
        <span>{t(questionKey)}</span>
      </summary>
      <p className="ml-5 pb-3 text-sm text-ink-2 leading-relaxed">{t(answerKey)}</p>
    </details>
  );
}

export function ServiceGuideSkeleton() {
  return (
    <div className="space-y-4">
      <div className="h-8 w-48 animate-pulse bg-[#212140] rounded" />
      <div className="h-4 w-96 animate-pulse bg-[#212140] rounded" />
      {[...Array(3)].map((_, idx) => (
        <div key={idx} className="h-20 animate-pulse bg-[#212140] rounded" />
      ))}
    </div>
  );
}

export function ServiceGuide({ serviceType, serviceId, tenantId, userRole }: ServiceGuideProps) {
  const t = useTranslations();
  const config = getGuideConfig(serviceType);

  if (!config) {
    return (
      <div className="amg-card card-clip p-8 text-center">
        <IconSet.AlertCircle size={32} className="mx-auto mb-3 text-ink-3" />
        <div className="f-display font-bold text-sm mb-1">{t('guides.no_guides')}</div>
        <p className="text-ink-2 text-sm mb-4">{t('guides.no_guides_desc')}</p>
        <Link href="/portal/serveis">
          <AMGButton variant="outline" size="sm" icon={IconSet.ArrowRight}>
            {t('guides.back_to_list')}
          </AMGButton>
        </Link>
      </div>
    );
  }

  const title = t(config.titleKey);
  const description = t(config.descriptionKey);

  return (
    <div className="space-y-6">
      <div>
        <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3 mb-1">{t('guides.guide')}</div>
        <h1 className="f-display font-black text-2xl">{title}</h1>
        <p className="text-sm text-ink-2 mt-1">{description}</p>
      </div>

      <div className="space-y-4">
        {config.sections.map((section) => (
          <SectionRenderer key={section.id} section={section} t={t} />
        ))}
      </div>

      {config.faq && config.faq.length > 0 && (
        <div className="amg-card card-clip p-4">
          <div className="flex items-center gap-2 mb-3">
            <IconSet.AlertCircle size={14} className="shrink-0" />
            <div className="f-mono text-[10px] uppercase tracking-widest text-ink-2">{t('guides.faq_title')}</div>
          </div>
          <div className="divide-y divide-border-subtle">
            {config.faq.map((faq, idx) => (
              <FaqItem key={idx} questionKey={faq.questionKey} answerKey={faq.answerKey} t={t} />
            ))}
          </div>
        </div>
      )}

      {serviceId && userRole !== 'CLIENT' && (
        <div className="flex justify-center pt-2">
          <Link href={`/portal/admin/tenants/${tenantId}`}>
            <AMGButton variant="outline" size="sm" icon={IconSet.Settings}>
              {t('guides.configure_service')}
            </AMGButton>
          </Link>
        </div>
      )}
    </div>
  );
}
