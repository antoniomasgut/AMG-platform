'use client';

import { useRouter } from 'next/navigation';
import { useMutation } from '@tanstack/react-query';
import { useToast } from '@/lib/toast-context';
import { createTemplate } from '@/services/templates';
import { TemplateForm } from '@/components/admin/TemplateForm';
import { PortalShell } from '@/components/portal/PortalShell';
import type { CreateTemplateRequest } from '@/services/templates';

export default function NewTemplatePage() {
  const router = useRouter();
  const { toast } = useToast();

  const { mutate, isPending } = useMutation({
    mutationFn: (data: CreateTemplateRequest) => createTemplate(data),
    onSuccess: (result) => {
      toast('success', 'Plantilla creada');
      router.push(`/portal/admin/templates/${result.id}`);
    },
    onError: () => toast('error', 'Error creant la plantilla'),
  });

  return (
    <PortalShell breadcrumb="admin · plantilles · nova">
      <div className="p-4 sm:p-8 max-w-lg space-y-6">
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / plantilles / nova /</span>
          <div className="f-display font-bold text-xl mt-1">Nova plantilla</div>
        </div>

        <div className="amg-card card-clip p-5">
          <TemplateForm
            onSave={(data) => mutate(data)}
            onCancel={() => router.back()}
            loading={isPending}
          />
        </div>
      </div>
    </PortalShell>
  );
}
