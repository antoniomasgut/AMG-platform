'use client';

import { useState } from 'react';
import { useParams } from 'next/navigation';
import { AMGLogo } from '@/components/ui/AMGLogo';
import { cancelBooking } from '@/services/booking';

type State = 'confirm' | 'cancelling' | 'done' | 'error';

export default function CancelBookingPage() {
  const { token } = useParams<{ token: string }>();
  const [state, setState] = useState<State>('confirm');
  const [errorMsg, setErrorMsg] = useState('');

  const handleCancel = async () => {
    setState('cancelling');
    try {
      await cancelBooking(token);
      setState('done');
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : 'Error desconegut';
      setErrorMsg(msg);
      setState('error');
    }
  };

  return (
    <div className="min-h-dvh bg-[#0d0d1a] flex items-start justify-center px-4 py-10">
      <div className="w-full max-w-md">
        <div className="flex justify-center mb-8">
          <AMGLogo className="h-8 w-auto opacity-80" />
        </div>
        <div className="bg-[#13132a] border border-[#2a2a50] rounded-2xl p-6 shadow-2xl">

          {state === 'confirm' && (
            <div className="space-y-5 text-center">
              <p className="text-4xl">🗓️</p>
              <div>
                <h1 className="text-lg font-bold text-white">Cancel·lar la cita</h1>
                <p className="text-sm text-[#a0a0c0] mt-2">
                  Estàs a punt de cancel·lar la teva cita reservada. Aquesta acció no es pot desfer.
                </p>
              </div>
              <div className="flex flex-col gap-3 pt-2">
                <button
                  onClick={handleCancel}
                  className="w-full py-3 bg-red-600 hover:bg-red-700 text-white font-semibold rounded-xl transition-colors text-sm"
                >
                  Sí, cancel·la la cita
                </button>
                <a
                  href="javascript:history.back()"
                  className="w-full py-3 text-center border border-[#2a2a50] text-[#a0a0c0] hover:text-white hover:border-[#4a4a80] font-medium rounded-xl transition-colors text-sm block"
                >
                  Enrere
                </a>
              </div>
            </div>
          )}

          {state === 'cancelling' && (
            <div className="text-center py-8 space-y-3">
              <div className="w-8 h-8 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin mx-auto" />
              <p className="text-sm text-[#a0a0c0]">Cancel·lant la cita...</p>
            </div>
          )}

          {state === 'done' && (
            <div className="text-center space-y-4 py-4">
              <p className="text-4xl">✅</p>
              <div>
                <h1 className="text-lg font-bold text-white">Cita cancel·lada</h1>
                <p className="text-sm text-[#a0a0c0] mt-2">
                  La teva cita ha estat cancel·lada correctament. El negoci ha estat notificat.
                </p>
              </div>
              <p className="text-xs text-[#6060a0] pt-2">
                Si vols concertar una nova cita, posa't en contacte amb nosaltres.
              </p>
            </div>
          )}

          {state === 'error' && (
            <div className="text-center space-y-4 py-4">
              <p className="text-4xl">⚠️</p>
              <div>
                <h1 className="text-lg font-bold text-white">No s'ha pogut cancel·lar</h1>
                <p className="text-sm text-[#a0a0c0] mt-2">
                  {errorMsg.includes('no estava confirmada')
                    ? 'Aquesta cita ja estava cancel·lada o no es trobava.'
                    : errorMsg.includes('invàlid') || errorMsg.includes('expirat')
                    ? "L'enllaç de cancel·lació ha expirat o no és vàlid."
                    : 'Ha ocorregut un error. Prova de nou o contacta directament amb el negoci.'}
                </p>
              </div>
              <button
                onClick={() => setState('confirm')}
                className="text-xs text-[#FF6B00] hover:underline"
              >
                Tornar a intentar-ho
              </button>
            </div>
          )}

        </div>
        <p className="text-center text-xs text-[#404060] mt-4">
          AMG Digitalitzacions · amgdl.com
        </p>
      </div>
    </div>
  );
}
