import { registerWizard } from './index';

registerWizard({
  slug: 'whatsapp-business',
  serviceType: 'CREDENTIALS',
  titleKey: 'wizard.whatsapp.title',
  descriptionKey: 'wizard.whatsapp.description',
  prerequisitesKey: 'wizard.whatsapp.prerequisites',
  steps: [
    {
      id: 'api-credentials',
      titleKey: 'wizard.whatsapp.step1.title',
      descriptionKey: 'wizard.whatsapp.step1.description',
      type: 'credentials',
      fields: [
        { id: 'api_key', labelKey: 'wizard.whatsapp.field.api_key', type: 'password', required: true },
        { id: 'phone_number', labelKey: 'wizard.whatsapp.field.phone', type: 'text', required: true, validation: { pattern: '^\\+?[0-9]{7,15}$', messageKey: 'wizard.validation.phone' } },
        { id: 'account_id', labelKey: 'wizard.whatsapp.field.account_id', type: 'text', required: true },
      ],
    },
    {
      id: 'webhook',
      titleKey: 'wizard.whatsapp.step2.title',
      descriptionKey: 'wizard.whatsapp.step2.description',
      type: 'copy',
      fields: [{ id: 'webhook_url', labelKey: 'wizard.whatsapp.field.webhook_url', type: 'url', required: false }],
    },
    {
      id: 'verify',
      titleKey: 'wizard.whatsapp.step3.title',
      descriptionKey: 'wizard.whatsapp.step3.description',
      type: 'verify',
      action: { type: 'verify' },
    },
  ],
});
