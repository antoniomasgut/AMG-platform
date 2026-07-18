'use client';

import { useState, useEffect, useMemo } from 'react';
import DOMPurify from 'dompurify';
import { useParams } from 'next/navigation';
import { AMGLogo } from '@/components/ui/AMGLogo';
import {
  getDocumentView,
  acceptDocument,
  downloadUrl,
  type DocumentViewResponse,
} from '@/services/secureDocuments';

function AcceptedBanner({ signerName, acceptedAt }: { signerName: string; acceptedAt: string }) {
  const date = new Date(acceptedAt).toLocaleDateString('ca-ES', {
    day: 'numeric', month: 'long', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
  return (
    <div className="bg-green-50 border border-green-200 rounded-xl p-6 text-center space-y-2">
      <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center mx-auto">
        <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
        </svg>
      </div>
      <p className="font-semibold text-green-800 text-sm">Pressupost acceptat</p>
      <p className="text-xs text-green-700">
        Acceptat per <strong>{signerName}</strong> el {date}
      </p>
      <p className="text-xs text-green-600">
        Hem notificat l'acceptació al nostre equip. Ens posarem en contacte amb vós en breu.
      </p>
    </div>
  );
}

function AcceptForm({
  token,
  onAccepted,
}: {
  token: string;
  onAccepted: (signerName: string) => void;
}) {
  const [signerName, setSignerName] = useState('');
  const [accepting, setAccepting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleAccept = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!signerName.trim()) return;
    setAccepting(true);
    setError(null);
    try {
      const res = await acceptDocument(token, signerName.trim());
      onAccepted(signerName.trim());
      // Mòdul 53: cobrament online — redirigir al checkout d'Stripe del negoci
      if (res.paymentUrl) {
        window.location.href = res.paymentUrl;
        return;
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Error en acceptar el document');
    } finally {
      setAccepting(false);
    }
  };

  return (
    <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-6 space-y-4">
      <div>
        <h2 className="text-sm font-semibold text-gray-800">Acceptació del pressupost</h2>
        <p className="text-xs text-gray-500 mt-1">
          En acceptar, confirmeu que heu llegit i esteu d'acord amb les condicions del pressupost.
          L'acceptació es registra amb data, hora i adreça IP per a la seva validesa.
        </p>
      </div>

      <form onSubmit={handleAccept} className="space-y-3">
        <div>
          <label className="block text-xs font-medium text-gray-600 mb-1">
            Nom i cognoms del signant <span className="text-red-400">*</span>
          </label>
          <input
            className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-400 bg-white"
            placeholder="Joan García Pérez"
            value={signerName}
            onChange={e => setSignerName(e.target.value)}
            required
          />
        </div>

        {error && <p className="text-red-500 text-xs">{error}</p>}

        <button
          type="submit"
          disabled={accepting || !signerName.trim()}
          className="w-full bg-orange-500 hover:bg-orange-600 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold text-sm py-3 rounded-lg transition-colors flex items-center justify-center gap-2"
        >
          {accepting ? (
            <>
              <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
              Acceptant...
            </>
          ) : (
            'Acceptar el pressupost'
          )}
        </button>
      </form>
    </div>
  );
}

function ErrorState({ type }: { type: 'not_found' | 'expired' | 'error' }) {
  const msgs: Record<string, { icon: string; title: string; desc: string }> = {
    not_found: {
      icon: '🔒',
      title: 'Enllaç no vàlid',
      desc: "L'enllaç del document no existeix o ha estat revocat. Si creus que és un error, contacta'ns.",
    },
    expired: {
      icon: '⏰',
      title: 'Enllaç expirat',
      desc: "L'enllaç ha caducat. Contacta amb nosaltres per rebre un nou accés al document.",
    },
    error: {
      icon: '⚠️',
      title: 'Error de càrrega',
      desc: 'No hem pogut carregar el document. Intenta-ho de nou o contacta amb nosaltres.',
    },
  };
  const m = msgs[type] ?? msgs.error;

  return (
    <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-10 text-center space-y-3">
      <p className="text-4xl">{m.icon}</p>
      <p className="font-semibold text-gray-800">{m.title}</p>
      <p className="text-sm text-gray-500 max-w-sm mx-auto">{m.desc}</p>
      <a
        href="mailto:hola@amgdl.com"
        className="inline-block mt-2 text-sm text-orange-500 hover:underline"
      >
        hola@amgdl.com
      </a>
    </div>
  );
}

function PaymentBanner({ paid }: { paid: boolean }) {
  return paid ? (
    <div className="bg-green-50 border border-green-200 rounded-xl p-4 flex items-center gap-3">
      <span className="text-xl">💳</span>
      <div>
        <div className="text-sm font-semibold text-green-800">Pagament completat</div>
        <p className="text-xs text-green-700">Hem rebut el teu pagament correctament. Gràcies!</p>
      </div>
    </div>
  ) : (
    <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex items-center gap-3">
      <span className="text-xl">⚠️</span>
      <div>
        <div className="text-sm font-semibold text-amber-800">Pagament no completat</div>
        <p className="text-xs text-amber-700">El pressupost ha quedat acceptat, però el pagament no s&apos;ha completat. El negoci es posarà en contacte amb tu.</p>
      </div>
    </div>
  );
}

export default function DocumentViewPage() {
  const { token } = useParams<{ token: string }>();
  const [paidParam, setPaidParam] = useState<string | null>(null);
  useEffect(() => {
    setPaidParam(new URLSearchParams(window.location.search).get('paid'));
  }, []);
  const [doc, setDoc] = useState<DocumentViewResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [errorType, setErrorType] = useState<'not_found' | 'expired' | 'error' | null>(null);
  const [acceptedBy, setAcceptedBy] = useState<string | null>(null);

  useEffect(() => {
    getDocumentView(token)
      .then(d => {
        setDoc(d);
        if (d.accepted && d.signerName) setAcceptedBy(d.signerName);
      })
      .catch((e: Error) => {
        const msg = e.message;
        setErrorType(
          msg === 'not_found' ? 'not_found'
          : msg === 'expired' ? 'expired'
          : 'error'
        );
      })
      .finally(() => setLoading(false));
  }, [token]);

  const safeHtml = useMemo(
    () => (doc?.htmlContent ? DOMPurify.sanitize(doc.htmlContent, { USE_PROFILES: { html: true } }) : null),
    [doc?.htmlContent]
  );
  const docTitle = doc?.description ?? doc?.fileName ?? 'Document';
  const isAccepted = doc?.accepted || acceptedBy !== null;
  const signerNameDisplay = acceptedBy ?? doc?.signerName ?? '';
  const acceptedAtDisplay = doc?.acceptedAt ?? new Date().toISOString();

  return (
    <div className="min-h-dvh bg-gray-50">
      <header className="bg-white border-b border-gray-100 px-6 py-4 flex items-center justify-between">
        <AMGLogo className="h-7 w-auto" />
        {doc && (
          <span className="text-xs text-gray-400 font-mono truncate max-w-[200px] sm:max-w-none">
            {docTitle}
          </span>
        )}
      </header>

      <main className="max-w-4xl mx-auto px-4 py-10 space-y-6">
        {loading && (
          <div className="flex justify-center py-20">
            <div className="w-8 h-8 border-2 border-orange-500 border-t-transparent rounded-full animate-spin" />
          </div>
        )}

        {errorType && !loading && <ErrorState type={errorType} />}

        {doc && !loading && (
          <>
            {paidParam !== null && <PaymentBanner paid={paidParam === '1'} />}

            {/* Document HTML */}
            {safeHtml ? (
              <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-8 overflow-x-auto">
                <div
                  className="prose prose-sm max-w-none"
                  dangerouslySetInnerHTML={{ __html: safeHtml ?? '' }}
                />
              </div>
            ) : (
              <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-8 text-center text-gray-400 text-sm">
                Vista prèvia no disponible — descarregueu el document per veure'l.
              </div>
            )}

            {/* Accept section */}
            {doc.canAccept && (
              isAccepted ? (
                <AcceptedBanner
                  signerName={signerNameDisplay}
                  acceptedAt={acceptedAtDisplay}
                />
              ) : (
                <AcceptForm token={token} onAccepted={name => {
                  setAcceptedBy(name);
                  setDoc(d => d ? { ...d, accepted: true, signerName: name, acceptedAt: new Date().toISOString() } : d);
                }} />
              )
            )}

            {/* Download button */}
            <div className="flex justify-center">
              <a
                href={downloadUrl(token)}
                download={doc.fileName}
                className="inline-flex items-center gap-2 bg-gray-800 hover:bg-gray-900 text-white text-sm font-medium px-6 py-3 rounded-lg transition-colors"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                    d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                </svg>
                Descarregar PDF
              </a>
            </div>
          </>
        )}
      </main>

      <footer className="text-center py-8 text-xs text-gray-400">
        AMG Digitalització S.L. · hola@amgdl.com · Mallorca, Illes Balears
      </footer>
    </div>
  );
}
