import Link from 'next/link';
import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Termes del Servei',
  description: 'Condicions de contractació dels serveis d\'AMG Digitalització.',
};

export default function TernesServeiPage() {
  const year = new Date().getFullYear();

  return (
    <div className="min-h-dvh bg-bg-0 text-ink-0">
      <div className="max-w-3xl mx-auto px-6 py-16">
        <div className="mb-10">
          <div className="flex items-center gap-2 mb-4">
            <div className="w-1.5 h-1.5 bg-accent" />
            <span className="f-mono text-label uppercase tracking-widest text-accent-light">Legal</span>
          </div>
          <h1 className="f-display font-black text-4xl tracking-display mb-2">TERMES DEL SERVEI</h1>
          <p className="f-mono text-data text-ink-3">Versió 1.0 · Actualitzat: {year}</p>
        </div>

        <div className="space-y-10 text-ui text-ink-1 leading-relaxed">
          <section>
            <h2 className="f-display font-black text-xl text-ink-0 mb-3">1. Objecte del contracte</h2>
            <p>Els presents Termes del Servei regulen la relació contractual entre AMG Digitalització i els seus clients per a la prestació de serveis de digitalització, automatització i gestió de presència digital.</p>
          </section>

          <section>
            <h2 className="f-display font-black text-xl text-ink-0 mb-3">2. Serveis i preus</h2>
            <p className="mb-3">Els serveis contractats i els seus preus queden definits a l'oferta personalitzada enviada al client. Tots els preus s'indiquen sense IVA i corresponen a un pagament únic de setup, sense quotes mensuals recurrents, llevat que s'indiqui expressament el contrari.</p>
            <p>El catàleg de serveis i tarifes vigents es pot consultar a l'apartat de preus del portal.</p>
          </section>

          <section>
            <h2 className="f-display font-black text-xl text-ink-0 mb-3">3. Condicions de pagament</h2>
            <p className="mb-3">El pagament s'efectua mitjançant transferència bancària o targeta de crèdit, prèviament a l'inici dels treballs, llevat d'acord exprés diferent. El servei no s'activarà fins a la confirmació del pagament.</p>
            <p>AMG Digitalització emetrà la factura corresponent en el termini de 30 dies des de la recepció del pagament.</p>
          </section>

          <section>
            <h2 className="f-display font-black text-xl text-ink-0 mb-3">4. Terminis d'execució</h2>
            <p>El termini d'execució és de 48 hores per als serveis estàndard, comptades des de la confirmació del pagament i la recepció de tots els materials necessaris per part del client. Els terminis per a projectes complexos s'acorden expressament a l'oferta.</p>
          </section>

          <section>
            <h2 className="f-display font-black text-xl text-ink-0 mb-3">5. Obligacions del client</h2>
            <ul className="list-disc pl-5 space-y-2">
              <li>Proporcionar la informació i materials necessaris per a l'execució del servei en el termini acordat.</li>
              <li>Mantenir la confidencialitat de les credencials d'accés al portal.</li>
              <li>Notificar qualsevol incidència o mal funcionament en el termini de 24 hores.</li>
              <li>Usar el servei d'acord amb la legislació vigent.</li>
            </ul>
          </section>

          <section>
            <h2 className="f-display font-black text-xl text-ink-0 mb-3">6. Cancel·lació i devolucions</h2>
            <p className="mb-3">El client pot cancel·lar la contractació en el termini de 14 dies naturals des de la confirmació del pagament, sempre que els treballs no hagin estat iniciats. En aquest cas, AMG Digitalització reembossarà l'import íntegre.</p>
            <p>Un cop iniciats els treballs, no procedirà cap devolució, però AMG Digitalització s'compromet a completar el servei contractat.</p>
          </section>

          <section>
            <h2 className="f-display font-black text-xl text-ink-0 mb-3">7. Limitació de responsabilitat</h2>
            <p>AMG Digitalització no es fa responsable dels perjudicis causats per força major, fallades de tercers (allotjament, DNS, plataformes externes), o ús inadequat del servei per part del client. La responsabilitat màxima en cap cas superarà l'import abonat pel client.</p>
          </section>

          <section>
            <h2 className="f-display font-black text-xl text-ink-0 mb-3">8. Propietat intel·lectual</h2>
            <p>Un cop abonat el preu del servei, el client adquireix tots els drets sobre els materials creats específicament per a ell (webs, dissenys, textos). AMG Digitalització conserva els drets sobre els seus frameworks, plantilles i eines pròpies.</p>
          </section>

          <section>
            <h2 className="f-display font-black text-xl text-ink-0 mb-3">9. Resolució de conflictes</h2>
            <p>Les parts se sotmeten als Jutjats i Tribunals de Palma de Mallorca per a la resolució de qualsevol conflicte derivat d'aquests termes, amb renúncia expressa al seu propi fur si fos diferent.</p>
          </section>

          <section>
            <h2 className="f-display font-black text-xl text-ink-0 mb-3">10. Modificació dels termes</h2>
            <p>AMG Digitalització es reserva el dret de modificar els presents Termes amb un preavís de 30 dies. L'ús continuat del servei un cop transcorregut aquest termini implica l'acceptació de les noves condicions.</p>
          </section>
        </div>

        <div className="mt-12 pt-8 border-t border-border-subtle flex flex-wrap gap-4 f-mono text-label text-ink-3">
          <Link href="/ca/legal/avis-legal" className="hover:text-ink-1 transition-colors">Avís Legal</Link>
          <Link href="/ca/legal/politica-privacitat" className="hover:text-ink-1 transition-colors">Privacitat</Link>
          <Link href="/ca/legal/cookies" className="hover:text-ink-1 transition-colors">Cookies</Link>
          <Link href="/ca" className="ml-auto hover:text-ink-1 transition-colors">← Inici</Link>
        </div>
      </div>
    </div>
  );
}
