import type { Metadata } from 'next';
import { headers } from 'next/headers';
import { NextIntlClientProvider } from 'next-intl';
import { getMessages, getTranslations } from 'next-intl/server';
import { notFound } from 'next/navigation';
import { routing } from '@/i18n/routing';
import dynamic from 'next/dynamic';

const CookieConsentBanner = dynamic(() => import('@/components/CookieConsentBanner').then(m => m.CookieConsentBanner), { ssr: false });

type Props = {
  children: React.ReactNode;
  params: Promise<{ locale: string }>;
};

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? 'https://amgdl.com';
const OG_LOCALES: Record<string, string> = { ca: 'ca_ES', es: 'es_ES', en: 'en_US', de: 'de_DE' };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: 'landing.hero' });

  return {
    title: {
      default: 'AMG Digitalitzacions · Agent IA per a negocis locals a Mallorca',
      template: '%s · AMG Digitalitzacions',
    },
    description: t('subtitle'),
    alternates: {
      canonical: `${SITE_URL}/${locale}`,
      languages: Object.fromEntries(routing.locales.map(l => [l, `${SITE_URL}/${l}`])),
    },
    openGraph: {
      title: 'AMG Digitalitzacions',
      description: t('subtitle'),
      url: `${SITE_URL}/${locale}`,
      siteName: 'AMG Digitalitzacions',
      locale: OG_LOCALES[locale] ?? 'ca_ES',
      type: 'website',
    },
    twitter: {
      card: 'summary_large_image',
      title: 'AMG Digitalitzacions',
      description: t('subtitle'),
    },
  };
}

export function generateStaticParams() {
  return routing.locales.map(locale => ({ locale }));
}

export default async function LocaleLayout({ children, params }: Props) {
  const { locale } = await params;

  if (!routing.locales.includes(locale as 'ca' | 'es' | 'en' | 'de')) {
    notFound();
  }

  const messages = await getMessages();
  const nonce = (await headers()).get('x-nonce') ?? '';

  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'ProfessionalService',
    name: 'AMG Digitalitzacions',
    description: 'Disseny web i automatització amb agent IA per a negocis locals a Mallorca. Webs, WhatsApp automatitzat, reserves i seguiment de clients.',
    url: `${SITE_URL}/${locale}`,
    logo: `${SITE_URL}/favicon.svg`,
    telephone: '+34614492062',
    email: 'info@amgdl.com',
    priceRange: '€€',
    areaServed: {
      '@type': 'Place',
      name: 'Mallorca',
    },
    address: {
      '@type': 'PostalAddress',
      addressLocality: 'Palma',
      addressRegion: 'Illes Balears',
      addressCountry: 'ES',
    },
    contactPoint: {
      '@type': 'ContactPoint',
      telephone: '+34614492062',
      email: 'info@amgdl.com',
      contactType: 'customer service',
      availableLanguage: ['Catalan', 'Spanish', 'English', 'German'],
    },
  };

  return (
    <NextIntlClientProvider messages={messages}>
      <script
        nonce={nonce}
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
      />
      <main id="main-content">
        {children}
      </main>
      <CookieConsentBanner />
    </NextIntlClientProvider>
  );
}
