'use client';

import { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useLocale } from 'next-intl';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';

import { IconSet } from '@/components/ui/icons';
import { getAppointments, type BookingToken } from '@/services/booking';
import { apiFetch } from '@/services/api';

interface FollowupLogEntry {
  id: string;
  type: string;
  contact: string;
  sentAt: string;
}

// ── Utilitats de dates ────────────────────────────────────────────────────────

function startOfWeek(d: Date): Date {
  const day = d.getDay();
  const diff = day === 0 ? -6 : 1 - day; // dilluns com a inici
  const start = new Date(d);
  start.setDate(d.getDate() + diff);
  start.setHours(0, 0, 0, 0);
  return start;
}

function addDays(d: Date, n: number): Date {
  const r = new Date(d);
  r.setDate(r.getDate() + n);
  return r;
}

function isoDate(d: Date): string {
  return d.toISOString().slice(0, 10);
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString('ca', { hour: '2-digit', minute: '2-digit' });
}

function formatDay(d: Date): string {
  return d.toLocaleDateString('ca', { weekday: 'long', day: 'numeric', month: 'long' });
}

function isToday(d: Date): boolean {
  const t = new Date();
  return d.getDate() === t.getDate() && d.getMonth() === t.getMonth() && d.getFullYear() === t.getFullYear();
}

const DAY_NAMES = ['Dl', 'Dt', 'Dc', 'Dj', 'Dv', 'Ds', 'Dg'];

// ── Components ────────────────────────────────────────────────────────────────

function AppointmentCard({ bt, past }: { bt: BookingToken; past: boolean }) {
  const locale = useLocale();
  const name = bt.recipientName ?? bt.leadName;
  const contact = bt.recipientPhone ?? bt.leadEmail;
  const label = bt.bookingLabel ?? 'Cita';

  return (
    <div className={`rounded border px-4 py-3 flex items-start gap-4 ${past ? 'border-border-base opacity-50' : 'border-border-base bg-surface-card'}`}>
      <div className="text-center min-w-[48px]">
        <div className="text-lg font-bold text-ink-0 leading-none">
          {bt.meetingAt ? formatTime(bt.meetingAt) : '—'}
        </div>
      </div>
      <div className="flex-1 min-w-0">
        <div className="font-medium text-ink-0 truncate">{name}</div>
        {contact && <div className="text-xs text-ink-3 mt-0.5">{contact}</div>}
        <div className="mt-1">
          <AMGBadge tone="neutral">{label}</AMGBadge>
        </div>
      </div>
      <div className="flex items-center gap-2 shrink-0">
        {bt.meetLink && (
          <a href={bt.meetLink} target="_blank" rel="noopener noreferrer">
            <AMGButton variant="outline" size="sm">
              <IconSet.Video size={13} />
              <span className="ml-1 hidden sm:inline">Meet</span>
            </AMGButton>
          </a>
        )}
        {bt.leadId && (
          <a href={`/${locale}/portal/leads/${bt.leadId}`}>
            <AMGButton variant="ghost" size="sm">
              <IconSet.Users size={13} />
            </AMGButton>
          </a>
        )}
      </div>
    </div>
  );
}

function PendingCard({ bt }: { bt: BookingToken }) {
  const locale = useLocale();
  const name = bt.recipientName ?? bt.leadName;
  const contact = bt.recipientPhone ?? bt.leadEmail;
  const label = bt.bookingLabel ?? 'Cita';
  const expiresAt = new Date(bt.expiresAt);
  const daysLeft = Math.ceil((expiresAt.getTime() - Date.now()) / 86400000);

  return (
    <div className="rounded border border-dashed border-border-base px-4 py-3 flex items-start gap-4">
      <div className="min-w-[48px] flex items-center justify-center pt-1">
        <IconSet.Clock size={16} className="text-ink-3" />
      </div>
      <div className="flex-1 min-w-0">
        <div className="font-medium text-ink-0 truncate">{name}</div>
        {contact && <div className="text-xs text-ink-3 mt-0.5">{contact}</div>}
        <div className="flex items-center gap-2 mt-1">
          <AMGBadge tone="neutral">{label}</AMGBadge>
          <span className="text-[10px] text-ink-3">Caduca en {daysLeft}d</span>
        </div>
      </div>
      <div className="shrink-0">
        {bt.leadId && (
          <a href={`/${locale}/portal/leads/${bt.leadId}`}>
            <AMGButton variant="ghost" size="sm">
              <IconSet.Users size={13} />
            </AMGButton>
          </a>
        )}
      </div>
    </div>
  );
}

// ── Pàgina principal ──────────────────────────────────────────────────────────

export default function AgendaPage() {
  const [weekOffset, setWeekOffset] = useState(0);

  const weekStart = useMemo(() => {
    const base = startOfWeek(new Date());
    return addDays(base, weekOffset * 7);
  }, [weekOffset]);

  const weekEnd = addDays(weekStart, 6);
  const monthRange = { from: isoDate(weekStart), to: isoDate(weekEnd) };

  const { data, isLoading } = useQuery({
    queryKey: ['appointments', monthRange.from, monthRange.to],
    queryFn: () => getAppointments(monthRange.from, monthRange.to),
  });

  const { data: followupLogs = [] } = useQuery({
    queryKey: ['followup-history'],
    queryFn: () => apiFetch<FollowupLogEntry[]>('/agents/followups/history'),
  });

  const confirmed = data?.confirmed ?? [];
  const pending   = data?.pending   ?? [];

  // Agrupa cites confirmades per dia
  const byDay = useMemo(() => {
    const map: Record<string, BookingToken[]> = {};
    for (const bt of confirmed) {
      if (!bt.meetingAt) continue;
      const day = bt.meetingAt.slice(0, 10);
      if (!map[day]) map[day] = [];
      map[day].push(bt);
    }
    return map;
  }, [confirmed]);

  const days = Array.from({ length: 7 }, (_, i) => addDays(weekStart, i));
  const todayStr = isoDate(new Date());
  const isCurrentWeek = weekOffset === 0;

  const weekLabel = (() => {
    const s = weekStart.toLocaleDateString('ca', { day: 'numeric', month: 'short' });
    const e = weekEnd.toLocaleDateString('ca', { day: 'numeric', month: 'short' });
    return `${s} — ${e}`;
  })();

  return (
    <PortalShell breadcrumb="agenda">
      <div className="p-4 sm:p-6 space-y-6 max-w-4xl">

        {/* Capçalera */}
        <div className="flex items-center justify-between gap-4 flex-wrap">
          <div>
            <div className="f-mono text-[9px] uppercase tracking-widest text-ink-3 mb-1">F2 · Agenda</div>
            <h1 className="f-display font-black text-2xl">Cites</h1>
          </div>
          <div className="flex items-center gap-2">
            <AMGButton variant="outline" size="sm" onClick={() => setWeekOffset(w => w - 1)}>
              <IconSet.ChevronLeft size={14} />
            </AMGButton>
            <button
              onClick={() => setWeekOffset(0)}
              className={`px-3 py-1 text-sm rounded border transition-colors ${isCurrentWeek ? 'border-accent-light text-accent-light' : 'border-border-base text-ink-2 hover:text-ink-0'}`}
            >
              {isCurrentWeek ? 'Aquesta setmana' : weekLabel}
            </button>
            <AMGButton variant="outline" size="sm" onClick={() => setWeekOffset(w => w + 1)}>
              <IconSet.Chevron size={14} />
            </AMGButton>
          </div>
        </div>

        {/* Mini calendari setmanal */}
        <div className="grid grid-cols-7 gap-1">
          {days.map((day, i) => {
            const dayStr = isoDate(day);
            const count  = byDay[dayStr]?.length ?? 0;
            const today  = isToday(day);
            const past   = dayStr < todayStr;
            return (
              <div
                key={dayStr}
                className={`rounded p-2 text-center border transition-colors ${
                  today
                    ? 'border-accent-light bg-accent-light/10'
                    : count > 0
                    ? 'border-border-base bg-surface-card'
                    : 'border-transparent'
                }`}
              >
                <div className={`text-[10px] font-medium uppercase tracking-wide ${past && !today ? 'text-ink-4' : 'text-ink-3'}`}>
                  {DAY_NAMES[i]}
                </div>
                <div className={`text-base font-bold mt-0.5 ${today ? 'text-accent-light' : past ? 'text-ink-4' : 'text-ink-0'}`}>
                  {day.getDate()}
                </div>
                {count > 0 && (
                  <div className="mt-1 flex justify-center">
                    <span className={`w-1.5 h-1.5 rounded-full ${today ? 'bg-accent-light' : 'bg-ink-3'}`} />
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Cites per dia */}
        {isLoading ? (
          <div className="text-sm text-ink-3 py-8 text-center">Carregant cites…</div>
        ) : (
          <div className="space-y-6">
            {days.map(day => {
              const dayStr = isoDate(day);
              const appts  = byDay[dayStr] ?? [];
              if (appts.length === 0) return null;
              const past = dayStr < todayStr;
              return (
                <div key={dayStr}>
                  <div className={`text-xs font-semibold uppercase tracking-widest mb-2 ${isToday(day) ? 'text-accent-light' : past ? 'text-ink-4' : 'text-ink-2'}`}>
                    {isToday(day) ? '▸ Avui · ' : ''}{formatDay(day)}
                  </div>
                  <div className="space-y-2">
                    {appts.map(bt => (
                      <AppointmentCard key={bt.id} bt={bt} past={past} />
                    ))}
                  </div>
                </div>
              );
            })}
            {confirmed.length === 0 && (
              <div className="text-sm text-ink-3 py-8 text-center border border-dashed border-border-base rounded">
                Cap cita confirmada aquesta setmana
              </div>
            )}
          </div>
        )}

        {/* Invitacions pendents */}
        {pending.length > 0 && (
          <div>
            <div className="text-xs font-semibold uppercase tracking-widest text-ink-3 mb-2">
              Invitacions pendents de confirmar
            </div>
            <div className="space-y-2">
              {pending.map(bt => (
                <PendingCard key={bt.id} bt={bt} />
              ))}
            </div>
          </div>
        )}

        {/* Seguiments enviats (F4) */}
        <div>
          <div className="text-xs font-semibold uppercase tracking-widest text-ink-3 mb-2">
            Seguiments enviats (F4)
          </div>
          {followupLogs.length === 0 ? (
            <div className="text-sm text-ink-3 py-6 text-center border border-dashed border-border-base rounded">
              Cap seguiment enviat encara
            </div>
          ) : (
            <div className="divide-y divide-border-base border border-border-base rounded">
              {followupLogs.map(log => (
                <div key={log.id} className="px-4 py-3 flex items-center gap-4">
                  <div className="shrink-0">
                    <span className={`f-mono text-[10px] uppercase px-2 py-0.5 border rounded ${
                      log.type === 'APPOINTMENT'
                        ? 'border-accent/40 text-accent-light'
                        : 'border-border-base text-ink-2'
                    }`}>
                      {log.type === 'APPOINTMENT' ? 'Cita' : 'Servei'}
                    </span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="text-sm text-ink-1 truncate">{log.contact}</div>
                  </div>
                  <div className="shrink-0 f-mono text-xs text-ink-3">
                    {new Date(log.sentAt).toLocaleDateString('ca-ES', {
                      day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit',
                    })}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

      </div>
    </PortalShell>
  );
}
