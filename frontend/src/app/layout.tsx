import type { Metadata } from 'next';
import { Orbitron, Space_Grotesk, Share_Tech_Mono } from 'next/font/google';
import { Providers } from '@/components/Providers';
import { ToastContainer } from '@/components/ToastContainer';
import { ErrorBoundary } from '@/components/ErrorBoundary';
import './globals.css';

const orbitron = Orbitron({
  subsets: ['latin'],
  weight: ['500', '700', '900'],
  display: 'swap',
  variable: '--font-display',
});

const spaceGrotesk = Space_Grotesk({
  subsets: ['latin'],
  weight: ['300', '400', '500', '600', '700'],
  display: 'swap',
  variable: '--font-sans',
});

const shareTechMono = Share_Tech_Mono({
  subsets: ['latin'],
  weight: ['400'],
  display: 'swap',
  variable: '--font-mono',
});

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? 'https://amg.digital';

export const metadata: Metadata = {
  title: {
    default: 'AMG · Enginyeria Digital per a Pimes',
    template: '%s · AMG Digitalització',
  },
  description: 'Plataforma SaaS per automatitzar la presència digital de pimes a Mallorca. Webs, automatitzacions, facturació i CRM en un sol lloc.',
  metadataBase: new URL(SITE_URL),
  openGraph: {
    title: 'AMG · Enginyeria Digital per a Pimes',
    description: 'Plataforma SaaS per automatitzar la presència digital de pimes a Mallorca.',
    url: SITE_URL,
    siteName: 'AMG Digitalització',
    locale: 'ca_ES',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'AMG · Enginyeria Digital per a Pimes',
    description: 'Plataforma SaaS per automatitzar la presència digital de pimes a Mallorca.',
  },
  robots: { index: true, follow: true },
  icons: {
    icon: [{ url: '/favicon.svg', type: 'image/svg+xml' }],
  },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ca" className={`${orbitron.variable} ${spaceGrotesk.variable} ${shareTechMono.variable}`}>
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
      </head>
      <body>
        {/* Skip nav: primer element del body per a navegació per teclat */}
        <a
          href="#main-content"
          className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:px-4 focus:py-2 focus:bg-[#FF6B00] focus:text-black focus:font-semibold focus:rounded"
        >
          Salta al contingut principal
        </a>
        <ErrorBoundary>
          <Providers>
            <main id="main-content">
              {children}
            </main>
            <ToastContainer />
          </Providers>
        </ErrorBoundary>
      </body>
    </html>
  );
}
