'use client';

import { useState } from 'react';
import { useParams } from 'next/navigation';
import { useQuery, useMutation } from '@tanstack/react-query';
import {
  getTokenInfo, getAvailableDays, getSlotsForDay, confirmBooking,
  type BookingResult,
} from '@/services/booking';
import { AMGLogo } from '@/components/ui/AMGLogo';

const MONTH_NAMES_CA = [
  'gener','febrer','març','abril','maig','juny',
  'juliol','agost','setembre','octubre','novembre','desembre',
];
const DAY_NAMES_CA = ['dl','dt','dc','dj','dv','ds','dg'];

function parseLocalDate(s: string) {
  const [y, m, d] = s.split('-').map(Number);
  return new Date(y, m - 1, d);
}

function isoDate(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function CalendarGrid({
  availableDays,
  selectedDay,
  onSelect,
}: {
  availableDays: Set<string>;
  selectedDay: string | null;
  onSelect: (d: string) => void;
}) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const [viewYear, setViewYear] = useState(today.getFullYear());
  const [viewMonth, setViewMonth] = useState(today.getMonth());

  const firstDay = new Date(viewYear, viewMonth, 1);
  const lastDay = new Date(viewYear, viewMonth + 1, 0);
  // Monday-first: (getDay() + 6) % 7
  const startOffset = (firstDay.getDay() + 6) % 7;

  const cells: (Date | null)[] = Array(startOffset).fill(null);
  for (let d = 1; d <= lastDay.getDate(); d++) {
    cells.push(new Date(viewYear, viewMonth, d));
  }

  const prevMonth = () => {
    if (viewMonth === 0) { setViewYear(y => y - 1); setViewMonth(11); }
    else setViewMonth(m => m - 1);
  };
  const nextMonth = () => {
    if (viewMonth === 11) { setViewYear(y => y + 1); setViewMonth(0); }
    else setViewMonth(m => m + 1);
  };

  return (
    <div className="w-full">
      <div className="flex items-center justify-between mb-4">
        <button onClick={prevMonth} className="p-1.5 rounded hover:bg-white/10 transition-colors text-[#a0a0c0]">
          ◀
        </button>
        <span className="text-sm font-semibold text-white capitalize">
          {MONTH_NAMES_CA[viewMonth]} {viewYear}
        </span>
        <button onClick={nextMonth} className="p-1.5 rounded hover:bg-white/10 transition-colors text-[#a0a0c0]">
          ▶
        </button>
      </div>

      <div className="grid grid-cols-7 mb-1">
        {DAY_NAMES_CA.map(d => (
          <div key={d} className="text-center text-[10px] uppercase tracking-wider text-[#6060a0] py-1">{d}</div>
        ))}
      </div>

      <div className="grid grid-cols-7 gap-0.5">
        {cells.map((cell, i) => {
          if (!cell) return <div key={`e${i}`} />;
          const iso = isoDate(cell);
          const available = availableDays.has(iso);
          const selected = selectedDay === iso;
          const past = cell < today;
          return (
            <button
              key={iso}
              disabled={!available || past}
              onClick={() => onSelect(iso)}
              className={`
                aspect-square rounded-lg text-sm font-medium transition-all
                ${past ? 'text-[#404060] cursor-default' :
                  !available ? 'text-[#404060] cursor-default' :
                  selected ? 'bg-[#FF6B00] text-white shadow-lg shadow-[#FF6B00]/30' :
                  'bg-[#1e1e3a] text-[#c0c0e0] hover:bg-[#2a2a50] hover:text-white cursor-pointer'}
              `}
            >
              {cell.getDate()}
            </button>
          );
        })}
      </div>
    </div>
  );
}

function TimeSlots({
  slots,
  selected,
  onSelect,
  duration,
}: {
  slots: string[];
  selected: string | null;
  onSelect: (t: string) => void;
  duration: number;
}) {
  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
      {slots.map(t => (
        <button
          key={t}
          onClick={() => onSelect(t)}
          className={`
            py-2.5 rounded-lg text-sm font-medium transition-all border
            ${selected === t
              ? 'bg-[#FF6B00] border-[#FF6B00] text-white shadow-lg shadow-[#FF6B00]/30'
              : 'border-[#2a2a50] text-[#c0c0e0] hover:border-[#FF6B00]/60 hover:text-white'}
          `}
        >
          {t}
          <span className="block text-[10px] opacity-60">{duration} min</span>
        </button>
      ))}
    </div>
  );
}

function SuccessScreen({ result, leadName }: { result: BookingResult; leadName: string }) {
  const dt = new Date(result.meetingAt + 'Z');
  const dateStr = dt.toLocaleDateString('ca-ES', {
    weekday: 'long', year: 'numeric', month: 'long', day: 'numeric',
    timeZone: 'Europe/Madrid',
  });
  const timeStr = dt.toLocaleTimeString('ca-ES', { hour: '2-digit', minute: '2-digit', timeZone: 'Europe/Madrid' });

  return (
    <div className="text-center space-y-5">
      <div className="w-16 h-16 rounded-full bg-[rgba(57,211,83,0.15)] border border-[#39d353]/40 flex items-center justify-center mx-auto text-3xl">
        ✓
      </div>
      <div>
        <h2 className="text-xl font-bold text-white">Reserva confirmada</h2>
        <p className="text-sm text-[#a0a0c0] mt-1">T'esperem, {leadName.split(' ')[0]}!</p>
      </div>
      <div className="bg-[#0d0d1a] border border-[#2a2a50] rounded-xl p-5 text-left space-y-2">
        <div className="flex items-center gap-2 text-sm text-[#c0c0e0]">
          <span className="text-base">📅</span>
          <span className="capitalize">{dateStr} a les {timeStr}h</span>
        </div>
        {result.meetLink && (
          <div className="flex items-center gap-2 text-sm">
            <span className="text-base">🎥</span>
            <a
              href={result.meetLink}
              target="_blank"
              rel="noopener noreferrer"
              className="text-[#FF6B00] hover:underline break-all"
            >
              Unir-se a Google Meet →
            </a>
          </div>
        )}
        <p className="text-xs text-[#6060a0] pt-1">
          T'hem enviat un correu de confirmació. No necessites cap compte de Google — el link s'obre directament al navegador.
        </p>
      </div>
    </div>
  );
}

export default function BookingPage() {
  const params = useParams();
  const token = params.token as string;

  const [selectedDay, setSelectedDay] = useState<string | null>(null);
  const [selectedTime, setSelectedTime] = useState<string | null>(null);
  const [bookingResult, setBookingResult] = useState<BookingResult | null>(null);

  const { data: info, isLoading: loadingInfo, error: infoError } = useQuery({
    queryKey: ['booking-info', token],
    queryFn: () => getTokenInfo(token),
    enabled: !!token,
    retry: false,
  });

  const { data: availableDays = [], isLoading: loadingDays } = useQuery({
    queryKey: ['booking-days', token],
    queryFn: () => getAvailableDays(token),
    enabled: !!token && !infoError,
  });

  const { data: slots = [], isLoading: loadingSlots } = useQuery({
    queryKey: ['booking-slots', token, selectedDay],
    queryFn: () => getSlotsForDay(token, selectedDay!),
    enabled: !!selectedDay,
  });

  const { mutate: doConfirm, isPending: confirming } = useMutation({
    mutationFn: () => {
      const datetime = `${selectedDay}T${selectedTime}:00`;
      return confirmBooking(token, datetime);
    },
    onSuccess: (result) => setBookingResult(result),
  });

  const availableDaysSet = new Set(availableDays);

  if (loadingInfo) {
    return (
      <BookingShell>
        <div className="text-center text-sm text-[#6060a0] py-12">Carregant...</div>
      </BookingShell>
    );
  }

  if (infoError || !info) {
    return (
      <BookingShell>
        <div className="text-center space-y-3 py-8">
          <p className="text-2xl">🔒</p>
          <p className="text-sm font-medium text-white">Enllaç invàlid o expirat</p>
          <p className="text-xs text-[#6060a0]">
            L'enllaç de reserva ja no és vàlid. Demana'n un de nou al consultor.
          </p>
        </div>
      </BookingShell>
    );
  }

  if (bookingResult) {
    return (
      <BookingShell>
        <SuccessScreen result={bookingResult} leadName={info.leadName} />
      </BookingShell>
    );
  }

  return (
    <BookingShell>
      <div className="space-y-6">
        <div>
          <p className="text-xs font-mono uppercase tracking-wider text-[#FF6B00] mb-1">AMG Digitalitzacions</p>
          <h1 className="text-xl font-bold text-white">{info.bookingLabel ?? 'Reserva una reunió'}</h1>
          <p className="text-sm text-[#a0a0c0] mt-0.5">
            Hola, <strong className="text-white">{info.leadName}</strong>. Tria un dia i una hora.
          </p>
        </div>

        {/* Calendar */}
        {loadingDays ? (
          <div className="text-center text-sm text-[#6060a0] py-8">Carregant disponibilitat...</div>
        ) : (
          <div className="bg-[#0d0d1a] border border-[#2a2a50] rounded-xl p-4">
            <CalendarGrid
              availableDays={availableDaysSet}
              selectedDay={selectedDay}
              onSelect={(d) => { setSelectedDay(d); setSelectedTime(null); }}
            />
          </div>
        )}

        {/* Time slots */}
        {selectedDay && (
          <div className="space-y-3">
            <p className="text-sm font-medium text-[#c0c0e0]">
              Horaris disponibles per al{' '}
              <span className="text-white capitalize">
                {parseLocalDate(selectedDay).toLocaleDateString('ca-ES', { weekday: 'long', day: 'numeric', month: 'long' })}
              </span>
            </p>
            {loadingSlots ? (
              <div className="text-sm text-[#6060a0]">Carregant horaris...</div>
            ) : slots.length === 0 ? (
              <p className="text-sm text-[#6060a0]">No hi ha horaris disponibles per a aquest dia.</p>
            ) : (
              <TimeSlots
                slots={slots}
                selected={selectedTime}
                onSelect={setSelectedTime}
                duration={info.slotDurationMinutes}
              />
            )}
          </div>
        )}

        {/* Confirm button */}
        {selectedDay && selectedTime && (
          <button
            onClick={() => doConfirm()}
            disabled={confirming}
            className="w-full py-3 bg-[#FF6B00] hover:bg-[#e55c00] disabled:opacity-50 text-white font-semibold rounded-xl transition-colors text-sm"
          >
            {confirming ? 'Confirmant...' : `Confirmar · ${selectedTime}h · ${parseLocalDate(selectedDay).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short' })}`}
          </button>
        )}
      </div>
    </BookingShell>
  );
}

function BookingShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-dvh bg-[#0d0d1a] flex items-start justify-center px-4 py-10">
      <div className="w-full max-w-md">
        <div className="flex justify-center mb-8">
          <AMGLogo className="h-8 w-auto opacity-80" />
        </div>
        <div className="bg-[#13132a] border border-[#2a2a50] rounded-2xl p-6 shadow-2xl">
          {children}
        </div>
        <p className="text-center text-xs text-[#404060] mt-4">
          La videoconferència és per Google Meet · No cal instal·lar res
        </p>
      </div>
    </div>
  );
}
