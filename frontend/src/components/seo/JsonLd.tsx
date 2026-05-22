import { getTranslations } from 'next-intl/server';

export async function JsonLd({ locale }: { locale: string }) {
  const t = await getTranslations({ locale, namespace: 'common' });

  const organization = {
    '@context': 'https://schema.org',
    '@type': 'Organization',
    name: 'AMG Digitalitzacions',
    url: `https://amgdl.com/${locale}`,
    logo: 'https://amgdl.com/logo.png',
    description: t('tagline'),
    address: {
      '@type': 'PostalAddress',
      addressLocality: 'Mallorca',
      addressCountry: 'ES',
    },
  };

  const website = {
    '@context': 'https://schema.org',
    '@type': 'WebSite',
    name: 'AMG Digitalitzacions',
    url: `https://amgdl.com/${locale}`,
    description: t('tagline'),
    inLanguage: locale,
  };

  return (
    <script
      type="application/ld+json"
      dangerouslySetInnerHTML={{
        __html: JSON.stringify([organization, website]),
      }}
    />
  );
}
