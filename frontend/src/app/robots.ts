import { MetadataRoute } from 'next';

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: '*',
      allow: '/',
      disallow: ['/api/', '/portal/', '/reset-password'],
    },
    sitemap: 'https://amg.cat/sitemap.xml',
  };
}
