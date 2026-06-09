'use client';

import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '@/lib/auth-context';
import { getGoogleStatus } from '@/services/google';
import { getWhatsAppConfig } from '@/services/whatsapp-oauth';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { IconSet } from '@/components/ui/icons';
import { useRouter, useParams } from 'next/navigation';

type IconName = 'Key' | 'Smartphone' | 'Send' | 'Mail' | 'Trending' | 'CreditCard';

interface Integration {
  id: string;
  name: string;
  desc: string;
  icon: IconName;
  href: string;
  status: 'connected' | 'partial' | 'disconnected' | 'loading';
  statusLabel: string;
}

export default function IntegrationsPage() {
  const { user, isAdmin } = useAuth();
  const router = useRouter();
  const params = useParams();
  const locale = params.locale as string;
  const tenantId = user?.tenantId ?? '';

  const [googleStatus, setGoogleStatus] = useState<Integration['status']>('loading');
  const [whatsappStatus, setWhatsappStatus] = useState<Integration['status']>('loading');

  useEffect(() => {
    if (!tenantId) return;
    getGoogleStatus(tenantId)
      .then(s => setGoogleStatus(s.connected ? 'connected' : 'disconnected'))
      .catch(() => setGoogleStatus('disconnected'));
    getWhatsAppConfig(tenantId)
      .then(c => setWhatsappStatus(c.status === 'CONNECTED' ? 'connected' : 'disconnected'))
      .catch(() => setWhatsappStatus('disconnected'));
  }, [tenantId]);

  const integrations: Integration[] = [
    {
      id: 'google-workspace',
      name: 'Google Workspace',
      desc: 'Connecta Drive, Gmail, Calendar i Sheets',
      icon: 'Key',
      href: `/${locale}/portal/admin/integrations/google`,
      status: googleStatus,
      statusLabel: googleStatus === 'connected' ? 'Connectat' : googleStatus === 'loading' ? '' : 'No connectat',
    },
    {
      id: 'whatsapp',
      name: 'WhatsApp Business',
      desc: 'Canal de comunicació amb clients via WhatsApp (Meta Cloud API)',
      icon: 'Smartphone',
      href: `/${locale}/portal/admin/integrations/whatsapp`,
      status: whatsappStatus,
      statusLabel: whatsappStatus === 'connected' ? 'Connectat' : whatsappStatus === 'loading' ? '' : 'Connecta',
    },
    {
      id: 'telegram',
      name: 'Telegram Bot',
      desc: 'Bot de Telegram per notificacions i alertes',
      icon: 'Send',
      href: `/${locale}/portal/admin/tenants/${tenantId}`,
      status: 'disconnected',
      statusLabel: 'Configura',
    },
    {
      id: 'smtp',
      name: 'SMTP Corporatiu',
      desc: 'Correu corporatiu per enviar factures i pressupostos',
      icon: 'Mail',
      href: `/${locale}/portal/admin/tenants/${tenantId}`,
      status: 'disconnected',
      statusLabel: 'Configura',
    },
    {
      id: 'meta-ads',
      name: 'Meta Ads',
      desc: 'Campanyes de publicitat a Facebook i Instagram',
      icon: 'Trending',
      href: `/${locale}/portal/admin/tenants/${tenantId}`,
      status: 'disconnected',
      statusLabel: 'Configura',
    },
    {
      id: 'gocardless',
      name: 'GoCardless (SEPA)',
      desc: 'Domiciliació bancària automàtica',
      icon: 'CreditCard',
      href: `/${locale}/portal/admin/tenants/${tenantId}`,
      status: 'disconnected',
      statusLabel: 'Configura',
    },
  ];

  return (
    <PortalShell breadcrumb="admin · integrations">
      <div className="p-4 sm:p-8 space-y-6 max-w-4xl">
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / integrations /</span>
          <div className="f-display font-bold text-xl mt-1">Integracions</div>
          <p className="text-sm text-ink-2 mt-1">Connecta els serveis externs del teu negoci</p>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          {integrations.map((int) => (
            <button
              key={int.id}
              onClick={() => router.push(int.href)}
              className="amg-card card-clip p-5 text-left hover:border-accent/30 transition-colors group cursor-pointer"
            >
              <div className="flex items-start gap-4">
                <div className="w-10 h-10 bg-bg-2 rounded-lg flex items-center justify-center shrink-0 group-hover:bg-accent/10 transition-colors">
                  {int.icon === 'Key' && <IconSet.Key size={18} className="text-ink-1 group-hover:text-accent-light" />}
                  {int.icon === 'Smartphone' && <IconSet.Smartphone size={18} className="text-ink-1 group-hover:text-accent-light" />}
                  {int.icon === 'Send' && <IconSet.Bot size={18} className="text-ink-1 group-hover:text-accent-light" />}
                  {int.icon === 'Mail' && <IconSet.Mail size={18} className="text-ink-1 group-hover:text-accent-light" />}
                  {int.icon === 'Trending' && <IconSet.Trending size={18} className="text-ink-1 group-hover:text-accent-light" />}
                  {int.icon === 'CreditCard' && <IconSet.CreditCard size={18} className="text-ink-1 group-hover:text-accent-light" />}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-2">
                    <div className="f-display font-bold text-sm">{int.name}</div>
                    {int.status !== 'loading' && int.status !== 'disconnected' && (
                      <AMGBadge tone={int.status === 'connected' ? 'success' : 'neutral'}>
                        {int.statusLabel}
                      </AMGBadge>
                    )}
                  </div>
                  <p className="f-mono text-xs text-ink-2 mt-1">{int.desc}</p>
                  {int.status === 'loading' && (
                    <span className="inline-block mt-2 w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
                  )}
                  {int.status === 'disconnected' && (
                    <span className="f-mono text-[10px] text-accent-light mt-2 inline-block group-hover:underline">
                      {int.statusLabel} →
                    </span>
                  )}
                </div>
              </div>
            </button>
          ))}
        </div>
      </div>
    </PortalShell>
  );
}
