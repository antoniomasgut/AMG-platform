import { NextIntlClientProvider } from 'next-intl';
import { getMessages, getTranslations } from 'next-intl/server';
import { notFound } from 'next/navigation';
import { routing } from '@/i18n/routing';
import { CookieConsentBanner } from '@/components/landing/CookieConsentBanner';
import { JsonLd } from '@/components/seo/JsonLd';
import { ReactNode } from 'react';

type Props = {
  children: ReactNode;
  params: Promise<{ locale: string }>;
};

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: 'common' });

  return {
    title: t('siteName'),
    description: t('tagline'),
    metadataBase: new URL('https://amg.cat'),
    alternates: {
      canonical: `/${locale}`,
      languages: {
        ca: '/ca',
        es: '/es',
        en: '/en',
        de: '/de',
        'x-default': '/ca',
      },
    },
    openGraph: {
      title: t('siteName'),
      description: t('tagline'),
      url: `https://amg.cat/${locale}`,
      siteName: 'AMG · Enginyeria Digital',
      locale: locale === 'ca' ? 'ca_ES' : locale === 'es' ? 'es_ES' : `${locale}_${locale.toUpperCase()}`,
      type: 'website',
    },
  };
}

export default async function LocaleLayout({ children, params }: Props) {
  const { locale } = await params;

  if (!routing.locales.includes(locale as typeof routing.locales extends (infer U)[] ? U : never)) {
    notFound();
  }

  const messages = await getMessages();

  return (
    <NextIntlClientProvider messages={messages}>
      {children}
      <CookieConsentBanner />
      <JsonLd locale={locale} />
    </NextIntlClientProvider>
  );
}
