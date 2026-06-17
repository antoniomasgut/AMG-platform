import type { Metadata } from 'next';
import dynamic from 'next/dynamic';
import Link from 'next/link';
import { getTranslations } from 'next-intl/server';
import { routing } from '@/i18n/routing';
import { LandingHeader } from '@/components/landing/LandingHeader';
import { LandingFooter } from '@/components/landing/LandingFooter';
import { VerticalFAQ } from '@/components/landing/VerticalFAQ';
import { VerticalPage } from '@/components/landing/VerticalPage';

const AgencyChatWidget = dynamic(
  () => import('@/components/landing/AgencyChatWidget').then(m => m.AgencyChatWidget),
  { ssr: false }
);

export async function generateMetadata({
  params,
}: {
  params: Promise<{ locale: string }>;
}): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: 'verticals.citaPrevia' });
  const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? 'https://amgdl.com';
  const ogLocales: Record<string, string> = { ca: 'ca_ES', es: 'es_ES', en: 'en_US', de: 'de_DE' };

  return {
    title: t('meta.title'),
    description: t('meta.description'),
    alternates: {
      canonical: `${siteUrl}/${locale}/cita-previa`,
      languages: Object.fromEntries(routing.locales.map(l => [l, `${siteUrl}/${l}/cita-previa`])),
    },
    openGraph: {
      title: t('meta.title'),
      description: t('meta.description'),
      url: `${siteUrl}/${locale}/cita-previa`,
      siteName: 'AMG Digitalitzacions',
      locale: ogLocales[locale] ?? 'ca_ES',
      type: 'website',
    },
  };
}

export default async function CitaPrevia({
  params,
}: {
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: 'verticals.citaPrevia' });
  const ts = await getTranslations({ locale, namespace: 'verticals.shared' });

  const problems = t.raw('problems') as Array<{ num: string; title: string; desc: string }>;
  const solutions = t.raw('solutions') as Array<{ num: string; title: string; desc: string; pills: string[] }>;
  const forWho = t.raw('forWho') as string[];
  const faqs = t.raw('faqs') as Array<{ q: string; a: string }>;

  return (
    <div className="bg-bg-0 text-ink-0">
      <LandingHeader />
      <main>
        <VerticalPage
          locale={locale}
          badge={t('badge')}
          h1={t('h1')}
          h1Accent={t('h1Accent')}
          subtitle={t('subtitle')}
          problemLabel={t('problemLabel')}
          problemHeadline={t('problemHeadline')}
          problems={problems}
          solutionsLabel={t('solutionsLabel')}
          solutionsHeadline={t('solutionsHeadline')}
          solutions={solutions}
          forWhoLabel={t('forWhoLabel')}
          forWho={forWho}
          faqTitle={t('faqTitle')}
          faqSectionLabel={ts('faqSectionLabel')}
          faqs={faqs}
          testimonialQuote={t('testimonialQuote')}
          testimonialName={t('testimonialName')}
          testimonialRole={t('testimonialRole')}
          ctaTitle={t('ctaTitle')}
          ctaSubtitle={t('ctaSubtitle')}
          ctaButton={ts('ctaButton')}
          ctaContact={ts('ctaContact')}
          consultaGratuita={ts('consultaGratuita')}
          veureFunciona={ts('veureFunciona')}
          portalLink={ts('portalLink')}
        />
      </main>
      <LandingFooter />
      <AgencyChatWidget />
    </div>
  );
}
