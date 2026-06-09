'use client';

import { useState, useCallback, useRef } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import { createLead, searchLeads, type Lead } from '@/services/leads';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { IconSet } from '@/components/ui/icons';
import { useRouter, useParams } from 'next/navigation';

const SOURCES = [
  'WEBSITE', 'REFERRAL', 'COLD_CALL', 'SOCIAL_MEDIA',
  'GOOGLE_MAPS', 'INSTAGRAM', 'PAGINAS_AMARILLAS', 'META_LEAD_ADS', 'OTHER',
] as const;

const SOURCE_LABEL: Record<string, string> = {
  WEBSITE: 'Web', REFERRAL: 'Referit', COLD_CALL: 'Cold Call',
  SOCIAL_MEDIA: 'Xarxes Socials', GOOGLE_MAPS: 'Google Maps',
  INSTAGRAM: 'Instagram', PAGINAS_AMARILLAS: 'Pàg. Grogues',
  META_LEAD_ADS: 'Meta Ads', OTHER: 'Altre',
};

const STAGE_LABEL: Record<string, string> = {
  NEW: 'Nou', CONTACTED: 'Contactat', QUALIFIED: 'Qualificat',
  PROPOSAL: 'Proposta', NEGOTIATION: 'Negociació', WON: 'Guanyat',
  NURTURING: 'Nurturing', LOST: 'Perdut',
};

function DuplicateWarning({ lead, locale }: { lead: Lead; locale: string }) {
  const router = useRouter();
  return (
    <div className="flex items-start gap-2 mt-1.5 p-2.5 border border-warning/40 bg-warning/5">
      <IconSet.AlertTriangle size={13} className="text-warning shrink-0 mt-0.5" />
      <div className="flex-1 min-w-0">
        <span className="f-mono text-[10px] text-warning">
          Ja existeix un lead amb aquest contacte:
        </span>
        <button
          type="button"
          onClick={() => router.push(`/${locale}/portal/leads/${lead.id}`)}
          className="block f-display font-bold text-xs text-ink-0 hover:text-accent-light truncate mt-0.5"
        >
          {lead.name} · {STAGE_LABEL[lead.stage] ?? lead.stage} →
        </button>
      </div>
    </div>
  );
}

export default function NewLeadPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const router = useRouter();
  const params = useParams();
  const locale = params.locale as string;

  const [form, setForm] = useState({
    name: '', email: '', phone: '', source: 'WEBSITE', notes: '',
  });

  const [phoneMatch, setPhoneMatch] = useState<Lead | null>(null);
  const [emailMatch, setEmailMatch] = useState<Lead | null>(null);
  const phoneTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const emailTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const checkDuplicate = useCallback((value: string, field: 'phone' | 'email') => {
    const timer = field === 'phone' ? phoneTimer : emailTimer;
    const setMatch = field === 'phone' ? setPhoneMatch : setEmailMatch;
    if (timer.current) clearTimeout(timer.current);
    if (!value.trim() || value.length < 6) { setMatch(null); return; }
    timer.current = setTimeout(async () => {
      try {
        const results = await searchLeads(value.trim());
        const match = results.find(l =>
          field === 'phone'
            ? l.phone?.replace(/\s/g, '') === value.replace(/\s/g, '')
            : l.email?.toLowerCase() === value.toLowerCase()
        );
        setMatch(match ?? null);
      } catch { setMatch(null); }
    }, 600);
  }, []);

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

  const hasDuplicate = !!(phoneMatch || emailMatch);

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
          {/* Nom */}
          <div>
            <label className="f-mono text-label uppercase text-ink-2 tracking-widest block mb-1.5">Nom / Empresa *</label>
            <input
              type="text" value={form.name} required
              onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
              className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-10 f-mono text-xs focus:outline-none focus:border-accent transition-colors"
            />
          </div>

          {/* Email */}
          <div>
            <label className="f-mono text-label uppercase text-ink-2 tracking-widest block mb-1.5">Email</label>
            <input
              type="email" value={form.email}
              onChange={e => { setForm(f => ({ ...f, email: e.target.value })); checkDuplicate(e.target.value, 'email'); }}
              className={`w-full bg-bg-1 border text-ink-0 px-3 h-10 f-mono text-xs focus:outline-none transition-colors ${emailMatch ? 'border-warning focus:border-warning' : 'border-border-base focus:border-accent'}`}
            />
            {emailMatch && <DuplicateWarning lead={emailMatch} locale={locale} />}
          </div>

          {/* Telèfon */}
          <div>
            <label className="f-mono text-label uppercase text-ink-2 tracking-widest block mb-1.5">Telèfon</label>
            <input
              type="tel" value={form.phone}
              onChange={e => { setForm(f => ({ ...f, phone: e.target.value })); checkDuplicate(e.target.value, 'phone'); }}
              className={`w-full bg-bg-1 border text-ink-0 px-3 h-10 f-mono text-xs focus:outline-none transition-colors ${phoneMatch ? 'border-warning focus:border-warning' : 'border-border-base focus:border-accent'}`}
            />
            {phoneMatch && !emailMatch && <DuplicateWarning lead={phoneMatch} locale={locale} />}
          </div>

          {/* Origen */}
          <div>
            <label className="f-mono text-label uppercase text-ink-2 tracking-widest block mb-1.5">Origen</label>
            <select
              value={form.source}
              onChange={e => setForm(f => ({ ...f, source: e.target.value }))}
              className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-10 f-mono text-xs focus:outline-none focus:border-accent transition-colors"
            >
              {SOURCES.map(s => <option key={s} value={s}>{SOURCE_LABEL[s]}</option>)}
            </select>
          </div>

          {/* Notes */}
          <div>
            <label className="f-mono text-label uppercase text-ink-2 tracking-widest block mb-1.5">Notes</label>
            <textarea
              value={form.notes}
              onChange={e => setForm(f => ({ ...f, notes: e.target.value }))}
              rows={4}
              className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 py-2 f-mono text-xs focus:outline-none focus:border-accent transition-colors resize-none"
            />
          </div>

          {hasDuplicate && (
            <div className="p-3 border border-warning/30 bg-warning/5 f-mono text-[10px] text-warning">
              Ja existeix un lead amb aquest contacte. Pots crear-ne un de nou igualment si és un cas diferent.
            </div>
          )}

          <div className="flex gap-3 pt-2">
            <AMGButton type="submit" loading={isPending} icon={IconSet.Plus}>
              Crear Lead
            </AMGButton>
            <AMGButton variant="ghost" onClick={() => router.push(`/${locale}/portal/leads`)}>
              Cancel·lar
            </AMGButton>
          </div>
        </form>
      </div>
    </PortalShell>
  );
}
