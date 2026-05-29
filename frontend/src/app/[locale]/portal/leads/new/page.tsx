'use client';

import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import { createLead } from '@/services/leads';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { I } from '@/components/ui/icons';
import { useRouter, useParams } from 'next/navigation';

const SOURCES = ['WEBSITE', 'REFERRAL', 'COLD_CALL', 'SOCIAL_MEDIA', 'OTHER'] as const;
const SOURCE_LABEL: Record<string, string> = {
  WEBSITE: 'Web', REFERRAL: 'Referit', COLD_CALL: 'Cold Call',
  SOCIAL_MEDIA: 'Xarxes Socials', OTHER: 'Altre',
};

export default function NewLeadPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const router = useRouter();
  const params = useParams();
  const locale = params.locale as string;

  const [form, setForm] = useState({
    name: '',
    email: '',
    phone: '',
    source: 'WEBSITE',
    notes: '',
  });

  const { mutate: doCreate, isPending } = useMutation({
    mutationFn: () => createLead({
      name: form.name,
      email: form.email || undefined,
      phone: form.phone || undefined,
      source: form.source,
      notes: form.notes || undefined,
    }),
    onSuccess: (lead) => {
      toast('success', 'Lead creat correctament');
      router.push(`/${locale}/portal/leads/${lead.id}`);
    },
    onError: () => toast('error', 'Error creant el lead'),
  });

  if (!user) return null;

  const field = (label: string, key: keyof typeof form, type = 'text', required = false) => (
    <div>
      <label className="f-mono text-label uppercase text-ink-2 tracking-widest block mb-1.5">
        {label}{required && ' *'}
      </label>
      <input
        type={type}
        value={form[key]}
        onChange={(e) => setForm((f) => ({ ...f, [key]: e.target.value }))}
        required={required}
        className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-10 f-mono text-xs focus:outline-none focus:border-accent transition-colors"
      />
    </div>
  );

  return (
    <PortalShell breadcrumb="leads / nou" backHref={`/${locale}/portal/leads`}>
      <div className="p-4 sm:p-8 max-w-xl">
        <div className="mb-6">
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / leads / nou /</span>
          <div className="f-display font-bold text-xl mt-1">Nou Lead</div>
        </div>

        <form
          className="amg-card card-clip p-6 space-y-4"
          onSubmit={(e) => { e.preventDefault(); doCreate(); }}
        >
          {field('Nom / Empresa', 'name', 'text', true)}
          {field('Email', 'email', 'email')}
          {field('Telèfon', 'phone', 'tel')}

          <div>
            <label className="f-mono text-label uppercase text-ink-2 tracking-widest block mb-1.5">
              Origen
            </label>
            <select
              value={form.source}
              onChange={(e) => setForm((f) => ({ ...f, source: e.target.value }))}
              className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-10 f-mono text-xs focus:outline-none focus:border-accent transition-colors"
            >
              {SOURCES.map((s) => (
                <option key={s} value={s}>{SOURCE_LABEL[s]}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="f-mono text-label uppercase text-ink-2 tracking-widest block mb-1.5">
              Notes
            </label>
            <textarea
              value={form.notes}
              onChange={(e) => setForm((f) => ({ ...f, notes: e.target.value }))}
              rows={4}
              className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 py-2 f-mono text-xs focus:outline-none focus:border-accent transition-colors resize-none"
            />
          </div>

          <div className="flex gap-3 pt-2">
            <AMGButton type="submit" loading={isPending} icon={I.Plus}>
              Crear Lead
            </AMGButton>
            <AMGButton
              variant="ghost"
              onClick={() => router.push(`/${locale}/portal/leads`)}
            >
              Cancel·lar
            </AMGButton>
          </div>
        </form>
      </div>
    </PortalShell>
  );
}
