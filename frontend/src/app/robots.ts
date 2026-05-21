import { MetadataRoute } from 'next';

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: '*',
      allow: '/',
      disallow: ['/api/', '/portal/', '/reset-password'],
    },
    sitemap: 'https://amgdl.com/sitemap.xml',
  };
}
