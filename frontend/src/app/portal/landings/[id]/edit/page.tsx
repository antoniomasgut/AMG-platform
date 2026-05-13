'use client';

import { useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { getCurrentUser } from '@/services/auth';
import { getLanding } from '@/services/factory';
import { useEditorStore } from '@/store/editor';
import { FactoryLayout } from '@/components/factory/FactoryLayout';

export default function EditLandingPage() {
  const params = useParams();
  const router = useRouter();
  const landingId = params.id as string;
  const user = getCurrentUser();
  const loadLanding = useEditorStore((s) => s.loadLanding);
  const reset = useEditorStore((s) => s.reset);

  const { data: landing, isLoading, error } = useQuery({
    queryKey: ['landing', landingId],
    queryFn: () => getLanding(user!.tenantId!, landingId),
    enabled: !!user?.tenantId,
  });

  useEffect(() => {
    if (landing) {
      const draftVersion = landing.versions.find((v) => v.status === 'DRAFT');
      const versionId = draftVersion?.id || landing.versions[0]?.id;
      if (versionId) {
        loadLanding(landing, versionId);
      }
    }
    return () => reset();
  }, [landing, loadLanding, reset]);

  if (!user) {
    router.replace('/login');
    return null;
  }

  if (isLoading) {
    return (
      <div className="w-full h-full bg-[#0d0d1a] flex items-center justify-center">
        <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin"></span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="w-full h-full bg-[#0d0d1a] flex items-center justify-center text-red-400 text-sm">
        Error carregant la landing
      </div>
    );
  }

  return <FactoryLayout landingId={landingId} />;
}
