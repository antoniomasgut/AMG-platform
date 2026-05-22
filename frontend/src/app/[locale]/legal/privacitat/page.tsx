import type { Metadata } from 'next';
import Link from 'next/link';

export const metadata: Metadata = {
  title: 'Política de Privacitat',
  robots: { index: false },
};

export default function PrivacitatPage() {
  return (
    <div className="min-h-screen bg-[#0d0d1a] text-ink-0 py-16 px-4">
      <div className="max-w-3xl mx-auto">
        <Link href="/login" className="text-accent text-sm hover:underline mb-8 inline-block">← Tornar</Link>
        <h1 className="text-3xl font-bold mb-2">Política de Privacitat</h1>
        <p className="text-ink-3 text-sm mb-10">Darrera actualització: maig 2026 · Conforme al Reglament (UE) 2016/679 (RGPD) i la LOPDGDD</p>

        <section className="space-y-8 text-ink-1 leading-relaxed">
          <div>
            <h2 className="text-lg font-semibold text-ink-0 mb-3">1. Responsable del tractament</h2>
            <ul className="space-y-1 pl-4 border-l border-[#FF6B00]/30">
              <li><strong className="text-ink-0">Responsable:</strong> Antonio Mas Gutiérrez (AMG Digitalitzacions)</li>
              <li><strong className="text-ink-0">NIF:</strong> 182237442B</li>
              <li><strong className="text-ink-0">Adreça:</strong> Carrer Joan Capó, núm. 7, 1r esquerra, 07200 Felanitx, Illes Balears</li>
              <li><strong className="text-ink-0">Contacte:</strong> privacitat@amgdl.com</li>
            </ul>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-ink-0 mb-3">2. Dades que recollim</h2>
            <p>Recollim les dades personals que ens facilites voluntàriament a través dels nostres formularis:</p>
            <ul className="mt-3 space-y-2 pl-4">
              <li>• <strong className="text-ink-0">Formulari de contacte:</strong> nom, correu electrònic, telèfon i missatge</li>
              <li>• <strong className="text-ink-0">Portal de clients:</strong> nom, correu electrònic, dades de facturació i dades de la teva empresa</li>
              <li>• <strong className="text-ink-0">Comunicacions:</strong> historial de missatges i sol·licituds</li>
            </ul>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-ink-0 mb-3">3. Finalitat i base legal del tractament</h2>
            <div className="overflow-x-auto">
              <table className="w-full text-sm border-collapse mt-2">
                <thead>
                  <tr className="border-b border-border-medium">
                    <th className="text-left py-2 pr-4 text-ink-0">Finalitat</th>
                    <th className="text-left py-2 pr-4 text-ink-0">Base legal</th>
                  </tr>
                </thead>
                <tbody>
                  <tr className="border-b border-[rgba(255,255,255,0.06)]"><td className="py-2 pr-4">Gestió de clients i contractes</td><td className="py-2">Execució del contracte (art. 6.1.b RGPD)</td></tr>
                  <tr className="border-b border-[rgba(255,255,255,0.06)]"><td className="py-2 pr-4">Atenció a consultes i suport</td><td className="py-2">Interès legítim (art. 6.1.f RGPD)</td></tr>
                  <tr className="border-b border-[rgba(255,255,255,0.06)]"><td className="py-2 pr-4">Enviament de comunicacions comercials</td><td className="py-2">Consentiment (art. 6.1.a RGPD)</td></tr>
                  <tr><td className="py-2 pr-4">Compliment d'obligacions fiscals</td><td className="py-2">Obligació legal (art. 6.1.c RGPD)</td></tr>
                </tbody>
              </table>
            </div>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-ink-0 mb-3">4. Conservació de les dades</h2>
            <p>Les dades es conserven durant el temps necessari per a la finalitat per a la qual van ser recollides i, en tot cas, durant els terminis legalment exigits (5 anys per a dades fiscals i comptables, d'acord amb el Codi de Comerç).</p>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-ink-0 mb-3">5. Destinataris de les dades</h2>
            <p>Les teves dades no es cediran a tercers, excepte per obligació legal o als proveïdors de serveis que tracten dades en nom nostre (hosting, plataforma de pagaments Stripe, comptabilitat Holded), tots ells amb les garanties adequades de protecció de dades.</p>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-ink-0 mb-3">6. Els teus drets</h2>
            <p>Pots exercir en qualsevol moment els següents drets enviant un correu a <strong className="text-ink-0">privacitat@amgdl.com</strong> amb una còpia del teu DNI:</p>
            <ul className="mt-3 space-y-1 pl-4">
              <li>• <strong className="text-ink-0">Accés:</strong> conèixer quines dades tenim sobre tu</li>
              <li>• <strong className="text-ink-0">Rectificació:</strong> corregir dades inexactes</li>
              <li>• <strong className="text-ink-0">Supressió:</strong> sol·licitar l'eliminació de les teves dades</li>
              <li>• <strong className="text-ink-0">Oposició:</strong> oposar-te al tractament per interès legítim</li>
              <li>• <strong className="text-ink-0">Portabilitat:</strong> rebre les teves dades en format estructurat</li>
              <li>• <strong className="text-ink-0">Limitació:</strong> sol·licitar la limitació del tractament</li>
            </ul>
            <p className="mt-3">Tens dret a presentar una reclamació davant l'<strong className="text-ink-0">Agència Espanyola de Protecció de Dades (AEPD)</strong> a <a href="https://www.aepd.es" target="_blank" rel="noopener noreferrer" className="text-accent hover:underline">www.aepd.es</a>.</p>
          </div>
        </section>

        <div className="mt-12 pt-6 border-t border-border-base flex gap-6 text-sm text-ink-3">
          <Link href="/legal/avis-legal" className="hover:text-accent-light transition">Avís Legal</Link>
          <Link href="/legal/cookies" className="hover:text-accent-light transition">Política de Cookies</Link>
        </div>
      </div>
    </div>
  );
}
