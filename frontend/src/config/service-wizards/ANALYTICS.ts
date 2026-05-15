import { registerWizard } from './index';

registerWizard({
  slug: 'google-analytics',
  serviceType: 'OTHER',
  titleKey: 'wizard.analytics.title',
  descriptionKey: 'wizard.analytics.description',
  prerequisitesKey: 'wizard.analytics.prerequisites',
  steps: [
    {
      id: 'google-oauth',
      titleKey: 'wizard.analytics.step1.title',
      descriptionKey: 'wizard.analytics.step1.description',
      type: 'credentials',
      fields: [
        { id: 'ga_property_id', labelKey: 'wizard.analytics.field.property_id', type: 'text', required: true, placeholderKey: 'wizard.analytics.field.property_id_placeholder' },
        { id: 'google_oauth_key', labelKey: 'wizard.analytics.field.oauth', type: 'password', required: true },
      ],
    },
    {
      id: 'verify-tracking',
      titleKey: 'wizard.analytics.step2.title',
      descriptionKey: 'wizard.analytics.step2.description',
      type: 'verify',
      action: { type: 'verify' },
    },
  ],
});
