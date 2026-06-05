'use client';

import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { useToast } from '@/lib/toast-context';
import { getBookingSettings, updateBookingSettings, type MeetingSettings } from '@/services/booking';

const inp = "w-full bg-surface-base border border-border-base rounded px-3 py-1.5 text-sm font-mono text-ink-1 focus:outline-none focus:border-accent-light";

const DAYS = [
  { key: 'MON', label: 'Dl' },
  { key: 'TUE', label: 'Dt' },
  { key: 'WED', label: 'Dc' },
  { key: 'THU', label: 'Dj' },
  { key: 'FRI', label: 'Dv' },
  { key: 'SAT', label: 'Ds' },
  { key: 'SUN', label: 'Dg' },
];

export default function BookingSettingsPage() {
  const { toast } = useToast();
  const qc = useQueryClient();

  const { data: settings, isLoading } = useQuery({
    queryKey: ['booking-settings'],
    queryFn: getBookingSettings,
  });

  const [form, setForm] = useState({
    workingDays: 'MON,TUE,WED,THU,FRI',
    startTime: '09:00',
    endTime: '18:00',
    slotDurationMinutes: 45,
    bufferMinutes: 15,
    minNoticeHours: 24,
    maxAdvanceDays: 30,
    calendarId: '',
  });

  useEffect(() => {
    if (settings) {
      setForm({
        workingDays: settings.workingDays,
        startTime: settings.startTime?.substring(0, 5) ?? '09:00',
        endTime: settings.endTime?.substring(0, 5) ?? '18:00',
        slotDurationMinutes: settings.slotDurationMinutes,
        bufferMinutes: settings.bufferMinutes,
        minNoticeHours: settings.minNoticeHours,
        maxAdvanceDays: settings.maxAdvanceDays,
        calendarId: settings.calendarId ?? '',
      });
    }
  }, [settings]);

  const { mutate: doSave, isPending: saving } = useMutation({
    mutationFn: () => updateBookingSettings({
      ...form,
      calendarId: form.calendarId || null,
    } as Partial<MeetingSettings>),
    onSuccess: () => {
      toast('success', 'Configuració desada');
      qc.invalidateQueries({ queryKey: ['booking-settings'] });
    },
    onError: () => toast('error', 'Error desant la configuració'),
  });

  const toggleDay = (day: string) => {
    const days = form.workingDays.split(',').filter(Boolean);
    const newDays = days.includes(day) ? days.filter(d => d !== day) : [...days, day];
    setForm(f => ({ ...f, workingDays: newDays.join(',') }));
  };

  const activeDays = new Set(form.workingDays.split(',').filter(Boolean));

  // Calcula el nombre de slots per dia
  const slotCount = (() => {
    const [sh, sm] = form.startTime.split(':').map(Number);
    const [eh, em] = form.endTime.split(':').map(Number);
    const totalMins = (eh * 60 + em) - (sh * 60 + sm);
    const step = form.slotDurationMinutes + form.bufferMinutes;
    return Math.max(0, Math.floor((totalMins - form.slotDurationMinutes) / step) + 1);
  })();

  return (
    <PortalShell breadcrumb="admin · configuració de reunions">
      <div className="p-4 sm:p-8 max-w-2xl space-y-6">

        <div>
          <p className="text-xs font-mono uppercase tracking-wider text-[#FF6B00] mb-1">Booking</p>
          <h1 className="text-2xl font-bold text-ink-1">Configuració de reunions</h1>
          <p className="text-sm text-ink-3 mt-0.5">Horari de videoconferències per als leads</p>
        </div>

        {isLoading ? (
          <div className="text-sm text-ink-3">Carregant...</div>
        ) : (
          <div className="space-y-5">

            {/* Hores */}
            <div className="bg-surface-base border border-border-base rounded-xl overflow-hidden">
              <div className="px-5 py-4 border-b border-border-base">
                <h2 className="text-sm font-semibold text-ink-1">Franja horària</h2>
              </div>
              <div className="p-5 space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-xs text-ink-3 block mb-1">Hora inici</label>
                    <input type="time" className={inp} value={form.startTime}
                      onChange={e => setForm(f => ({ ...f, startTime: e.target.value }))} />
                  </div>
                  <div>
                    <label className="text-xs text-ink-3 block mb-1">Hora fi</label>
                    <input type="time" className={inp} value={form.endTime}
                      onChange={e => setForm(f => ({ ...f, endTime: e.target.value }))} />
                  </div>
                </div>

                <div>
                  <label className="text-xs text-ink-3 block mb-2">Dies laborables</label>
                  <div className="flex gap-2 flex-wrap">
                    {DAYS.map(({ key, label }) => (
                      <button
                        key={key}
                        onClick={() => toggleDay(key)}
                        className={`w-9 h-9 rounded-lg text-xs font-medium transition-all ${
                          activeDays.has(key)
                            ? 'bg-[#FF6B00] text-white'
                            : 'bg-surface-overlay text-ink-2 hover:text-ink-1'
                        }`}
                      >
                        {label}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            </div>

            {/* Slots */}
            <div className="bg-surface-base border border-border-base rounded-xl overflow-hidden">
              <div className="px-5 py-4 border-b border-border-base flex items-center justify-between">
                <h2 className="text-sm font-semibold text-ink-1">Franges de temps</h2>
                <AMGBadge tone="info">{slotCount} slots/dia</AMGBadge>
              </div>
              <div className="p-5 space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-xs text-ink-3 block mb-1">Durada reunió (min)</label>
                    <input type="number" min={15} max={180} step={15} className={inp}
                      value={form.slotDurationMinutes}
                      onChange={e => setForm(f => ({ ...f, slotDurationMinutes: +e.target.value }))} />
                  </div>
                  <div>
                    <label className="text-xs text-ink-3 block mb-1">Marge entre reunions (min)</label>
                    <input type="number" min={0} max={60} step={5} className={inp}
                      value={form.bufferMinutes}
                      onChange={e => setForm(f => ({ ...f, bufferMinutes: +e.target.value }))} />
                  </div>
                  <div>
                    <label className="text-xs text-ink-3 block mb-1">Avís mínim (hores)</label>
                    <input type="number" min={1} max={72} className={inp}
                      value={form.minNoticeHours}
                      onChange={e => setForm(f => ({ ...f, minNoticeHours: +e.target.value }))} />
                  </div>
                  <div>
                    <label className="text-xs text-ink-3 block mb-1">Reserva màx. (dies)</label>
                    <input type="number" min={7} max={90} className={inp}
                      value={form.maxAdvanceDays}
                      onChange={e => setForm(f => ({ ...f, maxAdvanceDays: +e.target.value }))} />
                  </div>
                </div>
              </div>
            </div>

            {/* Google Calendar */}
            <div className="bg-surface-base border border-border-base rounded-xl overflow-hidden">
              <div className="px-5 py-4 border-b border-border-base">
                <h2 className="text-sm font-semibold text-ink-1">Google Calendar</h2>
                <p className="text-xs text-ink-3 mt-0.5">
                  ID del calendari per comprovar disponibilitat i crear events Meet
                </p>
              </div>
              <div className="p-5 space-y-2">
                <input
                  type="text"
                  className={inp}
                  placeholder="exemple@group.calendar.google.com"
                  value={form.calendarId}
                  onChange={e => setForm(f => ({ ...f, calendarId: e.target.value }))}
                />
                {!form.calendarId && (
                  <p className="text-xs text-[#FF6B00]/80">
                    Sense calendari configurat no es crearan events Meet automàticament.
                  </p>
                )}
              </div>
            </div>

            <AMGButton loading={saving} onClick={() => doSave()}>
              Desar configuració
            </AMGButton>
          </div>
        )}
      </div>
    </PortalShell>
  );
}
