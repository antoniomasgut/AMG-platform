import { defineRouting } from 'next-intl/routing';

export const routing = defineRouting({
  locales: ['ca', 'es', 'en', 'de'],
  defaultLocale: 'ca',
  localePrefix: 'always',
});
