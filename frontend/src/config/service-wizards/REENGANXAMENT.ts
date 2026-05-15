import { registerWizard } from './index';

registerWizard({
  slug: 'reenganxament',
  serviceType: 'AUTOMATION',
  titleKey: 'wizard.reenganxament.title',
  descriptionKey: 'wizard.reenganxament.description',
  prerequisitesKey: 'wizard.reenganxament.prerequisites',
  steps: [
    {
      id: 'connect-crm',
      titleKey: 'wizard.reenganxament.step1.title',
      descriptionKey: 'wizard.reenganxament.step1.description',
      type: 'credentials',
      fields: [
        { id: 'crm_api_url', labelKey: 'wizard.reenganxament.field.api_url', type: 'url', required: true, placeholderKey: 'wizard.reenganxament.field.api_url_placeholder' },
        { id: 'crm_api_key', labelKey: 'wizard.reenganxament.field.api_key', type: 'password', required: true },
      ],
    },
    {
      id: 'config',
      titleKey: 'wizard.reenganxament.step2.title',
      descriptionKey: 'wizard.reenganxament.step2.description',
      type: 'form',
      fields: [
        { id: 'inactivity_months', labelKey: 'wizard.reenganxament.field.months', type: 'number', required: true, placeholderKey: '6' },
        { id: 'offer_template', labelKey: 'wizard.reenganxament.field.template', type: 'text', required: true, placeholderKey: 'wizard.reenganxament.field.template_placeholder' },
      ],
    },
    {
      id: 'activate',
      titleKey: 'wizard.reenganxament.step3.title',
      descriptionKey: 'wizard.reenganxament.step3.description',
      type: 'verify',
      action: { type: 'advance' },
    },
  ],
});
