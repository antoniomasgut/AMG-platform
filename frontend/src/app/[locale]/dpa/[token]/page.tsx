'use client';

import { useState, useEffect } from 'react';
import { useParams } from 'next/navigation';
import { AMGLogo } from '@/components/ui/AMGLogo';
import { getDpaDocument, signDpa, type DpaDocument } from '@/services/dpa';

const DPA_VERSION = '1.0';
const DPA_DATE    = '11 de juny de 2026';

function DpaText({ tenantName }: { tenantName: string }) {
  return (
    <div className="prose prose-sm max-w-none text-gray-700 space-y-5 text-[13px] leading-relaxed">
      <h2 className="text-base font-bold text-gray-900 uppercase tracking-wide">
        Acord de Tractament de Dades (ATD)
      </h2>
      <p className="text-xs text-gray-500">Versió {DPA_VERSION} · {DPA_DATE}</p>

      <section>
        <h3 className="font-semibold text-gray-800 mb-1">1. Identificació de les parts</h3>
        <p>
          <strong>Responsable del tractament:</strong> {tenantName} (d'ara endavant, «el Responsable»), empresa o professional que contracta els serveis de la plataforma AMG.
        </p>
        <p className="mt-2">
          <strong>Encarregada del tractament:</strong> AMG Digitalització S.L., amb domicili a Mallorca (Illes Balears), adreça electrònica hola@amgdl.com (d'ara endavant, «l'Encarregada»).
        </p>
      </section>

      <section>
        <h3 className="font-semibold text-gray-800 mb-1">2. Objecte i marc legal</h3>
        <p>
          El present Acord regula les condicions en les quals l'Encarregada tracta dades personals per compte del Responsable en el marc de la prestació dels serveis de la plataforma NexeLocal (gestió de cites, pressupostos, comunicació amb clients, allotjament web i automatitzacions).
        </p>
        <p className="mt-2">
          L'acord s'emmarca en el Reglament (UE) 2016/679 (RGPD) i la Llei Orgànica 3/2018 (LOPDGDD).
        </p>
      </section>

      <section>
        <h3 className="font-semibold text-gray-800 mb-1">3. Dades tractades i finalitat</h3>
        <p>L'Encarregada tractarà, per compte del Responsable, les categories de dades personals següents:</p>
        <ul className="list-disc pl-5 space-y-1 mt-1">
          <li><strong>Dades de contacte dels clients:</strong> nom, telèfon, adreça electrònica (gestió de leads, cites i comunicació).</li>
          <li><strong>Dades de cites i serveis:</strong> data, hora, tipus de servei, notes (calendari i agenda).</li>
          <li><strong>Comunicacions:</strong> contingut de missatges via WhatsApp, Telegram o correu electrònic (resposta automatitzada per IA).</li>
          <li><strong>Dades de navegació:</strong> IP i identificadors de sessió (seguretat i logs).</li>
        </ul>
        <p className="mt-2">
          Les dades es tracten exclusivament per a les finalitats pactades en el contracte de servei i mai s'utilitzen per a fins comercials propis de l'Encarregada.
        </p>
      </section>

      <section>
        <h3 className="font-semibold text-gray-800 mb-1">4. Obligacions de l'Encarregada</h3>
        <ul className="list-disc pl-5 space-y-1">
          <li>Tractar les dades únicament seguint les instruccions documentades del Responsable.</li>
          <li>Garantir que el personal autoritzat es compromet a la confidencialitat.</li>
          <li>Aplicar les mesures de seguretat tècniques i organitzatives adequades (xifrat AES-256, accés restringit per rol, còpies de seguretat diàries).</li>
          <li>No subcontractar cap tractament sense informació prèvia al Responsable.</li>
          <li>Assistir el Responsable en la resposta a exercicis de drets (accés, rectificació, supressió, etc.).</li>
          <li>Notificar qualsevol violació de seguretat en un màxim de 72 hores des del seu coneixement.</li>
          <li>Posar a disposició del Responsable tota la informació necessària per demostrar el compliment del present Acord.</li>
        </ul>
      </section>

      <section>
        <h3 className="font-semibold text-gray-800 mb-1">5. Obligacions del Responsable</h3>
        <ul className="list-disc pl-5 space-y-1">
          <li>Facilitar a l'Encarregada les instruccions necessàries per al tractament.</li>
          <li>Assegurar que les bases legals per al tractament de les dades dels seus clients són vàlides (interès legítim, execució de contracte o consentiment).</li>
          <li>Informar els seus clients sobre el tractament de dades d'acord amb els articles 13 i 14 del RGPD.</li>
          <li>Notificar sense dilació qualsevol canvi en les instruccions de tractament.</li>
        </ul>
      </section>

      <section>
        <h3 className="font-semibold text-gray-800 mb-1">6. Subcontractistes (sub-encarregats)</h3>
        <p>L'Encarregada utilitza els proveïdors de serveis següents, als quals pot transferir dades per a la prestació del servei:</p>
        <table className="w-full text-xs border-collapse mt-2">
          <thead>
            <tr className="bg-gray-100">
              <th className="border border-gray-200 px-2 py-1 text-left">Proveïdor</th>
              <th className="border border-gray-200 px-2 py-1 text-left">Finalitat</th>
              <th className="border border-gray-200 px-2 py-1 text-left">Ubicació</th>
            </tr>
          </thead>
          <tbody>
            {[
              ['Hetzner Online GmbH', 'Allotjament de servidors i base de dades', 'Alemanya (UE)'],
              ['Google LLC', 'Google Calendar, Google Drive', 'EUA (Clàusules Contractuals Tipus)'],
              ['Anthropic PBC', 'Processament de missatges IA (Claude)', 'EUA (Clàusules Contractuals Tipus)'],
              ['Twilio Inc.', 'Enviament de missatges WhatsApp', 'EUA (Clàusules Contractuals Tipus)'],
              ['Brevo SAS', 'Enviament de correus electrònics transaccionals', 'França (UE)'],
              ['Meta Platforms', 'WhatsApp Business API', 'EUA (Clàusules Contractuals Tipus)'],
            ].map(([p, f, u]) => (
              <tr key={p}>
                <td className="border border-gray-200 px-2 py-1">{p}</td>
                <td className="border border-gray-200 px-2 py-1">{f}</td>
                <td className="border border-gray-200 px-2 py-1">{u}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section>
        <h3 className="font-semibold text-gray-800 mb-1">7. Mesures de seguretat</h3>
        <p>L'Encarregada aplica, com a mínim, les mesures següents:</p>
        <ul className="list-disc pl-5 space-y-1">
          <li>Xifrat de dades en repòs (AES-256) i en trànsit (TLS 1.3).</li>
          <li>Control d'accés basat en rols (RBAC) amb autenticació per JWT.</li>
          <li>Còpies de seguretat diàries xifrades emmagatzemades a Google Cloud Storage (UE).</li>
          <li>Registre d'auditoria de totes les accions sobre dades sensibles.</li>
          <li>Monitorització d'incidents i alertes automatitzades.</li>
        </ul>
      </section>

      <section>
        <h3 className="font-semibold text-gray-800 mb-1">8. Durada i supressió</h3>
        <p>
          El present Acord és vigent mentre el Responsable mantingui activa la relació contractual amb l'Encarregada. En finalitzar el contracte, l'Encarregada suprimirà o retornarà les dades personals tractades en un termini màxim de 30 dies, excepte si la normativa aplicable n'exigeix la conservació.
        </p>
      </section>

      <section>
        <h3 className="font-semibold text-gray-800 mb-1">9. Llei aplicable i jurisdicció</h3>
        <p>
          El present Acord es regeix pel dret espanyol i europeu en matèria de protecció de dades. Per a qualsevol controvèrsia, les parts se sotmeten als jutjats i tribunals de Palma de Mallorca, amb renúncia expressa a qualsevol altre fur.
        </p>
      </section>
    </div>
  );
}

function SignedBanner({ signerName, signerPosition, signedAt }: {
  signerName: string; signerPosition?: string; signedAt: string;
}) {
  return (
    <div className="bg-green-50 border border-green-200 rounded-lg p-6 text-center space-y-2">
      <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center mx-auto">
        <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
        </svg>
      </div>
      <p className="font-semibold text-green-800 text-sm">Document signat</p>
      <p className="text-xs text-green-700">
        Signat per <strong>{signerName}</strong>
        {signerPosition && <> ({signerPosition})</>}
        {' '}el {new Date(signedAt).toLocaleDateString('ca-ES', { day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' })}
      </p>
      <p className="text-xs text-green-600">Rebreu una còpia per correu electrònic.</p>
    </div>
  );
}

export default function DpaPage() {
  const { token } = useParams<{ token: string }>();
  const [doc, setDoc] = useState<DpaDocument | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [signerName, setSignerName] = useState('');
  const [signerPosition, setSignerPosition] = useState('');
  const [signing, setSigning] = useState(false);
  const [signed, setSigned] = useState(false);

  useEffect(() => {
    getDpaDocument(token)
      .then(d => { setDoc(d); if (d.signed) setSigned(true); })
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, [token]);

  const handleSign = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!signerName.trim()) return;
    setSigning(true);
    try {
      await signDpa(token, signerName.trim(), signerPosition.trim());
      setSigned(true);
      setDoc(d => d ? { ...d, signed: true, signedAt: new Date().toISOString(), signerName: signerName.trim(), signerPosition: signerPosition.trim() } : d);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Error en signar');
    } finally {
      setSigning(false);
    }
  };

  const inp = "w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-400 bg-white";

  return (
    <div className="min-h-dvh bg-gray-50">
      {/* Header */}
      <header className="bg-white border-b border-gray-100 px-6 py-4 flex items-center justify-between">
        <AMGLogo className="h-7 w-auto" />
        <span className="text-xs text-gray-400 font-mono">Acord de Tractament de Dades · v{DPA_VERSION}</span>
      </header>

      <main className="max-w-3xl mx-auto px-4 py-10 space-y-8">
        {loading && (
          <div className="flex justify-center py-20">
            <div className="w-8 h-8 border-2 border-orange-500 border-t-transparent rounded-full animate-spin" />
          </div>
        )}

        {error && !loading && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
            <p className="text-red-700 text-sm font-medium">{error}</p>
            <p className="text-red-500 text-xs mt-1">Si creus que és un error, contacta amb hola@amgdl.com</p>
          </div>
        )}

        {doc && !loading && (
          <>
            {/* Intro */}
            <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-6 space-y-2">
              <p className="text-sm text-gray-600">
                Hola, <strong>{doc.tenantName}</strong>. A continuació trobareu l'Acord de Tractament de Dades que regula com AMG Digitalització gestiona les dades dels vostres clients.
              </p>
              <p className="text-xs text-gray-400">
                Llegiu atentament el document i signeu-lo al final per confirmar-ne l'acceptació.
              </p>
            </div>

            {/* Document */}
            <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-8">
              <DpaText tenantName={doc.tenantName} />
            </div>

            {/* Signature area */}
            <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-6">
              {signed && doc.signedAt ? (
                <SignedBanner
                  signerName={doc.signerName ?? signerName}
                  signerPosition={doc.signerPosition ?? signerPosition}
                  signedAt={doc.signedAt}
                />
              ) : (
                <form onSubmit={handleSign} className="space-y-4">
                  <div>
                    <h3 className="text-sm font-semibold text-gray-800 mb-1">Signatura electrònica</h3>
                    <p className="text-xs text-gray-500">
                      En signar, confirmeu que heu llegit i accepteu l'Acord de Tractament de Dades en nom de <strong>{doc.tenantName}</strong>.
                      La signatura es registra amb data, hora i adreça IP per a la seva validesa legal.
                    </p>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs font-medium text-gray-600 mb-1">
                        Nom i cognoms del signant <span className="text-red-400">*</span>
                      </label>
                      <input
                        className={inp}
                        placeholder="Joan García Pérez"
                        value={signerName}
                        onChange={e => setSignerName(e.target.value)}
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-gray-600 mb-1">
                        Càrrec (opcional)
                      </label>
                      <input
                        className={inp}
                        placeholder="Gerent, Autònom, Director..."
                        value={signerPosition}
                        onChange={e => setSignerPosition(e.target.value)}
                      />
                    </div>
                  </div>

                  <div className="text-xs text-gray-400 bg-gray-50 rounded p-3">
                    Correu electrònic del signant: <strong>{doc.tenantEmail}</strong>
                  </div>

                  {error && (
                    <p className="text-red-500 text-xs">{error}</p>
                  )}

                  <button
                    type="submit"
                    disabled={signing || !signerName.trim()}
                    className="w-full bg-orange-500 hover:bg-orange-600 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold text-sm py-3 rounded-lg transition-colors flex items-center justify-center gap-2"
                  >
                    {signing ? (
                      <><div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" /> Signant...</>
                    ) : (
                      <> He llegit i accepto l'Acord de Tractament de Dades</>
                    )}
                  </button>

                  <p className="text-[11px] text-gray-400 text-center">
                    Rebreu una còpia de confirmació al correu {doc.tenantEmail}
                  </p>
                </form>
              )}
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
