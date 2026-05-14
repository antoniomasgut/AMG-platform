import type { Metadata } from 'next';
import Link from 'next/link';

export const metadata: Metadata = {
  title: 'Avís Legal',
  robots: { index: false },
};

export default function AvisLegalPage() {
  return (
    <div className="min-h-screen bg-[#0d0d1a] text-ink-0 py-16 px-4">
      <div className="max-w-3xl mx-auto">
        <Link href="/login" className="text-accent text-sm hover:underline mb-8 inline-block">← Tornar</Link>
        <h1 className="text-3xl font-bold mb-2">Avís Legal</h1>
        <p className="text-ink-3 text-sm mb-10">Darrera actualització: maig 2026</p>

        <section className="space-y-8 text-ink-1 leading-relaxed">
          <div>
            <h2 className="text-lg font-semibold text-ink-0 mb-3">1. Titular del lloc web</h2>
            <p>En compliment de la Llei 34/2002, d'11 de juliol, de Serveis de la Societat de la Informació i del Comerç Electrònic (LSSI-CE), s'informa que el titular d'aquest lloc web és:</p>
            <ul className="mt-3 space-y-1 pl-4 border-l border-[#FF6B00]/30">
              <li><strong className="text-ink-0">Denominació:</strong> AMG Digitalització</li>
              <li><strong className="text-ink-0">Titular:</strong> Antonio Mas Gutiérrez</li>
              <li><strong className="text-ink-0">NIF:</strong> 182237442B</li>
              <li><strong className="text-ink-0">Domicili:</strong> Carrer Joan Capó, núm. 7, 1r esquerra, 07200 Felanitx, Illes Balears</li>
              <li><strong className="text-ink-0">Correu electrònic:</strong> hola@amg.digital</li>
            </ul>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-ink-0 mb-3">2. Objecte i àmbit d'aplicació</h2>
            <p>Aquest avís legal regula l'accés i l'ús del lloc web <strong className="text-ink-0">amg.digital</strong> i dels seus subdominis, posats a disposició dels usuaris per AMG Digitalització amb la finalitat de proporcionar informació sobre els seus serveis de digitalització per a pimes.</p>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-ink-0 mb-3">3. Condicions d'accés i ús</h2>
            <p>L'accés i l'ús d'aquest lloc web és lliure i gratuït. L'usuari es compromet a fer-ne un ús adequat, de conformitat amb la llei, la moral, l'ordre públic i les presents condicions.</p>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-ink-0 mb-3">4. Propietat intel·lectual</h2>
            <p>Tots els continguts d'aquest lloc web (textos, imatges, logotips, dissenys, codi font i qualsevol altre element) són propietat de AMG Digitalització o de tercers que n'han autoritzat l'ús. Queda prohibida la seva reproducció, distribució o comunicació pública sense autorització expressa.</p>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-ink-0 mb-3">5. Exclusió de responsabilitat</h2>
            <p>AMG Digitalització no es fa responsable dels danys que puguin derivar-se de la interrupció, errors o desconnexions del servei, ni de la presència de virus en els continguts accessibles a través d'internet.</p>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-ink-0 mb-3">6. Llei aplicable i jurisdicció</h2>
            <p>Les presents condicions es regeixen per la legislació espanyola. Per a la resolució de qualsevol controvèrsia derivada de l'accés o l'ús d'aquest lloc web, les parts se sotmeten als jutjats i tribunals de Palma de Mallorca.</p>
          </div>
        </section>

        <div className="mt-12 pt-6 border-t border-border-base flex gap-6 text-sm text-ink-3">
          <Link href="/legal/privacitat" className="hover:text-accent-light transition">Política de Privacitat</Link>
          <Link href="/legal/cookies" className="hover:text-accent-light transition">Política de Cookies</Link>
        </div>
      </div>
    </div>
  );
}
