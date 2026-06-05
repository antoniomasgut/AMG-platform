'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState, useEffect, type ReactNode } from 'react';
import { AuthProvider } from '@/lib/auth-context';
import { ToastProvider, useToast } from '@/lib/toast-context';
import { useRouter, usePathname } from 'next/navigation';
import { initSentry } from '@/lib/sentry';

initSentry();

function MissingApiKeyListener() {
  const { toast } = useToast();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    const handler = (e: Event) => {
      const detail = (e as CustomEvent).detail;
      const service = detail?.service ?? 'desconegut';
      // Extract locale from pathname (e.g. /ca/portal/...)
      const locale = pathname.split('/')[1] ?? 'ca';
      toast('error', `API key no configurada: ${service}. Configura-la a Ajustos → API Keys.`, {
        action: {
          label: 'Configurar',
          onClick: () => router.push(`/${locale}/portal/admin/config`),
        },
      });
    };
    window.addEventListener('missing-api-key', handler);
    return () => window.removeEventListener('missing-api-key', handler);
  }, [toast, router, pathname]);

  return null;
}

export function Providers({ children, nonce: _nonce }: { children: ReactNode; nonce?: string }) {
  const [queryClient] = useState(() => new QueryClient());
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <ToastProvider>
          <MissingApiKeyListener />
          {children}
        </ToastProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}
