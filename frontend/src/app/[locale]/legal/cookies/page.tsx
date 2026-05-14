import type { Metadata } from 'next';
import Link from 'next/link';

export const metadata: Metadata = {
  title: 'Política de Cookies',
  robots: { index: false },
};

export default function CookiesPage() {
  return (
    <div className="min-h-screen bg-[#0d0d1a] text-ink-0 py-16 px-4">
      <div className="max-w-3xl mx-auto">
        <Link href="/login" className="text-accent text-sm hover:underline mb-8 inline-block">← Tornar</Link>
        <h1 className="text-3xl font-bold mb-2">Política de Cookies</h1>
        <p className="text-ink-3 text-sm mb-10">Darrera actualització: maig 2026 · Conforme a la LSSI-CE i el RGPD</p>

        <section className="space-y-8 text-ink-1 leading-relaxed">
          <div>
            <h2 className="text-lg font-semibold text-ink-0 mb-3">1. Què és una cookie?</h2>
            <p>Una cookie és un fitxer petit que el servidor envia al teu navegador i que s'emmagatzema al teu dispositiu. Permet reconèixer el teu navegador en visites posteriors i millorar la teva experiència.</p>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-ink-0 mb-3">2. Cookies que utilitzem</h2>
            <div className="overflow-x-auto">
              <table className="w-full text-sm border-collapse mt-2">
                <thead>
                  <tr className="border-b border-border-medium">
                    <th className="text-left py-2 pr-4 text-ink-0">Cookie</th>
                    <th className="text-left py-2 pr-4 text-ink-0">Tipus</th>
                    <th className="text-left py-2 pr-4 text-ink-0">Finalitat</th>
                    <th className="text-left py-2 text-ink-0">Durada</th>
                  </tr>
                </thead>
                <tbody>
                  <tr className="border-b border-[rgba(255,255,255,0.06)]">
                    <td className="py-2 pr-4 font-mono text-xs text-info-light">access_token</td>
                    <td className="py-2 pr-4">Tècnica (essencial)</td>
                    <td className="py-2 pr-4">Autenticació al portal</td>
                    <td className="py-2">Sessió</td>
                  </tr>
                  <tr className="border-b border-[rgba(255,255,255,0.06)]">
                    <td className="py-2 pr-4 font-mono text-xs text-info-light">refresh_token</td>
                    <td className="py-2 pr-4">Tècnica (essencial)</td>
                    <td className="py-2 pr-4">Renovació de sessió</td>
                    <td className="py-2">7 dies</td>
                  </tr>
                  <tr>
                    <td className="py-2 pr-4 font-mono text-xs text-info-light">cookie_consent</td>
                    <td className="py-2 pr-4">Tècnica (essencial)</td>
                    <td className="py-2 pr-4">Guardar preferència de cookies</td>
                    <td className="py-2">1 any</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p className="mt-4 text-sm">Actualment <strong className="text-ink-0">no fem servir cookies de tercers</strong> ni cookies analítiques. Si en el futur n'incorporem, actualitzarem aquesta política i tornarem a demanar el teu consentiment.</p>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-ink-0 mb-3">3. Com gestionar les cookies</h2>
            <p>Pots configurar el teu navegador per bloquejar o eliminar cookies. Tingues en compte que bloquejar les cookies tècniques essencials pot impedir l'accés al portal de clients.</p>
            <ul className="mt-3 space-y-2 pl-4">
              <li>• <a href="https://support.google.com/chrome/answer/95647" target="_blank" rel="noopener noreferrer" className="text-accent hover:underline">Google Chrome</a></li>
              <li>• <a href="https://support.mozilla.org/ca/kb/activa-i-desactiva-les-galetes" target="_blank" rel="noopener noreferrer" className="text-accent hover:underline">Mozilla Firefox</a></li>
              <li>• <a href="https://support.apple.com/ca-es/guide/safari/sfri11471" target="_blank" rel="noopener noreferrer" className="text-accent hover:underline">Safari</a></li>
            </ul>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-ink-0 mb-3">4. Actualitzacions d'aquesta política</h2>
            <p>Podem actualitzar aquesta política quan afegim noves funcionalitats o cookies. Et notificarem els canvis significatius mitjançant un nou banner de consentiment.</p>
          </div>
        </section>

        <div className="mt-12 pt-6 border-t border-border-base flex gap-6 text-sm text-ink-3">
          <Link href="/legal/avis-legal" className="hover:text-accent-light transition">Avís Legal</Link>
          <Link href="/legal/privacitat" className="hover:text-accent-light transition">Política de Privacitat</Link>
        </div>
      </div>
    </div>
  );
}
