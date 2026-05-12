'use client';

import { useEffect, useState } from 'react';
import { useRouter } from '@/i18n/navigation';
import { getCurrentUser } from '@/services/auth';

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [authorized, setAuthorized] = useState(false);

  useEffect(() => {
    const user = getCurrentUser();
    if (!user || user.role === 'CLIENT') {
      router.push('/portal');
    } else {
      setAuthorized(true);
    }
  }, [router]);

  if (!authorized) return null;

  return <>{children}</>;
}
