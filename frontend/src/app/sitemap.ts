import type { MetadataRoute } from 'next';

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? 'https://amg.digital';
const LOCALES = ['ca', 'es', 'en', 'de'] as const;

const PUBLIC_PATHS = [
  '',
  '/login',
  '/forgot-password',
  '/legal/avis-legal',
  '/legal/politica-privacitat',
  '/legal/cookies',
  '/legal/termes-servei',
];

export default function sitemap(): MetadataRoute.Sitemap {
  const entries: MetadataRoute.Sitemap = [];

  for (const locale of LOCALES) {
    for (const path of PUBLIC_PATHS) {
      const url = `${SITE_URL}/${locale}${path}`;
      entries.push({
        url,
        lastModified: new Date(),
        changeFrequency: path === '' ? 'weekly' : 'monthly',
        priority: path === '' ? 1.0 : path === '/login' ? 0.8 : 0.5,
        alternates: {
          languages: Object.fromEntries(
            LOCALES.map(l => [l, `${SITE_URL}/${l}${path}`])
          ),
        },
      });
    }
  }

  return entries;
}
