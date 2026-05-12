import { MetadataRoute } from 'next';

const locales = ['ca', 'es', 'en', 'de'] as const;
const baseUrl = 'https://amg.cat';

const routes = ['', '/login', '/forgot-password', '/legal/avis-legal', '/legal/politica-privacitat', '/legal/termes-servei'];

export default function sitemap(): MetadataRoute.Sitemap {
  const entries: MetadataRoute.Sitemap = [];

  for (const locale of locales) {
    for (const route of routes) {
      const url = `${baseUrl}/${locale}${route}`;
      const alternates: Record<string, string> = {
        'x-default': `${baseUrl}/ca${route}`,
      };
      for (const alt of locales) {
        alternates[alt] = `${baseUrl}/${alt}${route}`;
      }

      entries.push({
        url,
        lastModified: new Date(),
        changeFrequency: route === '' ? 'monthly' : 'yearly',
        priority: route === '' ? 1 : route.startsWith('/legal') ? 0.3 : 0.8,
        alternates: {
          languages: alternates,
        },
      });
    }
  }

  return entries;
}
