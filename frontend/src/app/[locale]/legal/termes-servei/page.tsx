import Link from 'next/link';
import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Termes del Servei',
  description: 'Condicions de contractació dels serveis d\'AMG Digitalitzacions.',
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
            <p>Els presents Termes del Servei regulen la relació contractual entre AMG Digitalitzacions i els seus clients per a la prestació de serveis de digitalització, automatització i gestió de presència digital.</p>
          </section>

          <section>
            <h2 className="f-display font-black text-xl text-ink-0 mb-3">2. Serveis i preus</h2>
            <p className="mb-3">Els serveis contractats i els seus preus queden definits a l'oferta personalitzada enviada al client. Tots els preus s'indiquen sense IVA i comprenen dos conceptes: (a) un pagament únic de <strong>setup</strong> (implementació i posada en marxa) i (b) una <strong>quota mensual fixa</strong> per al servei actiu, que inclou l'agent IA 24/7, la monitorització i fins a 2 canvis menors al mes.</p>
            <p className="mb-3">Les fases del servei (F1, F2, F3, F4) són acumulatives. El client contracta les fases que necessita i pot ampliar en qualsevol moment abonant el cost d'ampliació corresponent. Les tarifes varien per sector professional i mida del negoci.</p>
            <p>El catàleg de serveis i tarifes vigents es pot consultar a l'apartat de preus del lloc web.</p>
          </section>

          <section>
            <h2 className="f-display font-black text-xl text-ink-0 mb-3">3. Condicions de pagament</h2>
            <p className="mb-3"><strong>Setup:</strong> El pagament del setup s'efectua en acceptar el pressupost, prèviament a l'inici dels treballs, mitjançant transferència bancària o targeta de crèdit. El servei no s'activarà fins a la confirmació del pagament.</p>
            <p className="mb-3"><strong>Quota mensual:</strong> La quota mensual es factura el primer dia de cada mes per al mes en curs. El primer mes es prorrategeja des de la data d'activació. El no-pagament en els terminis establerts pot comportar la suspensió temporal del servei amb preavís de 10 dies.</p>
            <p>AMG Digitalitzacions emetrà la factura corresponent en el termini de 30 dies des de cada pagament.</p>
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
            <p className="mb-3"><strong>Setup:</strong> El client pot cancel·lar la contractació en el termini de 14 dies naturals des de la confirmació del pagament del setup, sempre que els treballs d'implementació no hagin estat iniciats. En aquest cas, AMG Digitalitzacions reembossarà l'import íntegre del setup. Un cop iniciada la implementació, no procedirà devolució del setup.</p>
            <p className="mb-3"><strong>Quota mensual:</strong> El client pot donar de baixa el servei en qualsevol moment amb un preavís mínim de 30 dies naturals. Les quotes mensuals ja facturades no es reembossen. No es cobren penalitzacions per baixa voluntària.</p>
            <p>En cas de discrepàncies tècniques, AMG Digitalitzacions es compromet a resoldre les incidències cobertes pel servei sense cost addicional.</p>
          </section>

          <section>
            <h2 className="f-display font-black text-xl text-ink-0 mb-3">7. Limitació de responsabilitat</h2>
            <p>AMG Digitalitzacions no es fa responsable dels perjudicis causats per força major, fallades de tercers (allotjament, DNS, plataformes externes), o ús inadequat del servei per part del client. La responsabilitat màxima en cap cas superarà l'import abonat pel client.</p>
          </section>

          <section>
            <h2 className="f-display font-black text-xl text-ink-0 mb-3">8. Propietat intel·lectual</h2>
            <p>Un cop abonat el preu del servei, el client adquireix tots els drets sobre els materials creats específicament per a ell (webs, dissenys, textos). AMG Digitalitzacions conserva els drets sobre els seus frameworks, plantilles i eines pròpies.</p>
          </section>

          <section>
            <h2 className="f-display font-black text-xl text-ink-0 mb-3">9. Resolució de conflictes</h2>
            <p>Les parts se sotmeten als Jutjats i Tribunals de Palma de Mallorca per a la resolució de qualsevol conflicte derivat d'aquests termes, amb renúncia expressa al seu propi fur si fos diferent.</p>
          </section>

          <section>
            <h2 className="f-display font-black text-xl text-ink-0 mb-3">10. Modificació dels termes</h2>
            <p>AMG Digitalitzacions es reserva el dret de modificar els presents Termes amb un preavís de 30 dies. L'ús continuat del servei un cop transcorregut aquest termini implica l'acceptació de les noves condicions.</p>
          </section>

          <section>
            <h2 className="f-display font-black text-xl text-ink-0 mb-3">11. Tractament de dades personals — Acord d'Encarregat (Art. 28 RGPD)</h2>
            <p className="mb-3">En la mesura en què el client (tenant) introdueixi dades personals de tercers (clients finals, leads, contactes) a la plataforma, AMG Digitalitzacions actua com a <strong>encarregat del tractament</strong> i el client actua com a <strong>responsable del tractament</strong> d'acord amb el Reglament (UE) 2016/679 (RGPD).</p>
            <p className="mb-3">AMG Digitalitzacions es compromet a:</p>
            <ul className="list-disc pl-5 space-y-2 mb-3">
              <li>Tractar les dades personals únicament seguint les instruccions documentades del responsable i per a les finalitats descrites al servei contractat.</li>
              <li>Garantir que les persones autoritzades a tractar les dades s'hagin compromès a la confidencialitat.</li>
              <li>Aplicar les mesures de seguretat tècniques i organitzatives adequades (xifratge AES-256, control d'accés per rols, còpies de seguretat diàries).</li>
              <li>No subcontractar cap nou subencàrregat sense informar prèviament el responsable, qui podrà oposar-s'hi.</li>
              <li>Assistir el responsable en l'atenció de sol·licituds d'exercici de drets (accés, rectificació, supressió, portabilitat).</li>
              <li>Notificar qualsevol violació de seguretat que afecti les dades del responsable en un termini màxim de 72 hores des que en tingui coneixement.</li>
              <li>Retornar o destruir totes les dades personals un cop finalitzada la prestació del servei, a elecció del responsable.</li>
            </ul>
            <p>El client (responsable) és el qui determina la finalitat i els mitjans del tractament de les dades dels seus clients finals i n'assumeix la responsabilitat legal. Es compromet a recollir les dades amb una base legal vàlida i a informar els afectats d'acord amb els Arts. 13 i 14 del RGPD.</p>
          </section>
        </div>

        <div className="mt-12 pt-8 border-t border-border-subtle flex flex-wrap gap-4 f-mono text-label text-ink-3">
          <Link href="/ca/legal/avis-legal" className="hover:text-ink-1 transition-colors">Avís Legal</Link>
          <Link href="/ca/legal/privacitat" className="hover:text-ink-1 transition-colors">Privacitat</Link>
          <Link href="/ca/legal/cookies" className="hover:text-ink-1 transition-colors">Cookies</Link>
          <Link href="/ca" className="ml-auto hover:text-ink-1 transition-colors">← Inici</Link>
        </div>
      </div>
    </div>
  );
}
