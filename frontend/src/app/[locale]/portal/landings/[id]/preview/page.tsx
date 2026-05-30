'use client';

import { useParams, useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { getCurrentUser } from '@/services/auth';
import { getLanding } from '@/services/factory';
import { BlockRenderer } from '@/components/factory/BlockRenderer';

export default function PreviewLandingPage() {
  const params = useParams();
  const router = useRouter();
  const landingId = params.id as string;
  const user = getCurrentUser();

  const { data: landing, isLoading } = useQuery({
    queryKey: ['landing', landingId],
    queryFn: () => getLanding(user!.tenantId!, landingId),
    enabled: !!user?.tenantId,
  });

  if (!user) {
    router.replace('/login');
    return null;
  }

  if (isLoading) {
    return (
      <div className="w-full min-h-dvh bg-white flex items-center justify-center">
        <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin"></span>
      </div>
    );
  }

  const publishedVersion = landing?.versions.find((v) => v.status === 'PUBLISHED');
  const draftVersion = landing?.versions.find((v) => v.status === 'DRAFT');
  const version = publishedVersion || draftVersion;
  const styles = version?.styles || {
    fontHeading: 'Montserrat, sans-serif',
    fontBody: 'Inter, sans-serif',
    primaryColor: '#FF6B00',
    accentColor: '#1e293b',
    bgColor: '#ffffff',
    textColor: '#1e293b',
    borderRadius: '8px',
  };

  return (
    <div className="min-h-dvh bg-white" style={{ fontFamily: styles.fontBody ?? styles.fontHeading }}>
      {version?.content.blocks.map((block) => (
        <BlockRenderer key={block.id} block={block} styles={styles} preview />
      ))}
    </div>
  );
}
