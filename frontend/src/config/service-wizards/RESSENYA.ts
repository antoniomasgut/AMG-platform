import { registerWizard } from './index';

registerWizard({
  slug: 'ressenya-google',
  serviceType: 'AUTOMATION',
  titleKey: 'wizard.ressenya.title',
  descriptionKey: 'wizard.ressenya.description',
  prerequisitesKey: 'wizard.ressenya.prerequisites',
  steps: [
    {
      id: 'google-url',
      titleKey: 'wizard.ressenya.step1.title',
      descriptionKey: 'wizard.ressenya.step1.description',
      type: 'form',
      fields: [
        { id: 'google_reviews_url', labelKey: 'wizard.ressenya.field.url', type: 'url', required: true, placeholderKey: 'wizard.ressenya.field.url_placeholder' },
      ],
    },
    {
      id: 'trigger',
      titleKey: 'wizard.ressenya.step2.title',
      descriptionKey: 'wizard.ressenya.step2.description',
      type: 'info',
    },
    {
      id: 'activate',
      titleKey: 'wizard.ressenya.step3.title',
      descriptionKey: 'wizard.ressenya.step3.description',
      type: 'verify',
      action: { type: 'advance' },
    },
  ],
});
