import { registerWizard } from './index';

registerWizard({
  slug: 'cobrament-targeta',
  serviceType: 'BILLING',
  titleKey: 'wizard.cobrament.title',
  descriptionKey: 'wizard.cobrament.description',
  prerequisitesKey: 'wizard.cobrament.prerequisites',
  steps: [
    {
      id: 'stripe-credentials',
      titleKey: 'wizard.cobrament.step1.title',
      descriptionKey: 'wizard.cobrament.step1.description',
      type: 'credentials',
      fields: [
        { id: 'stripe_secret_key', labelKey: 'wizard.cobrament.field.secret_key', type: 'password', required: true },
        { id: 'stripe_public_key', labelKey: 'wizard.cobrament.field.public_key', type: 'text', required: true },
        { id: 'stripe_webhook_secret', labelKey: 'wizard.cobrament.field.webhook_secret', type: 'password', required: true },
      ],
    },
    {
      id: 'webhook',
      titleKey: 'wizard.cobrament.step2.title',
      descriptionKey: 'wizard.cobrament.step2.description',
      type: 'copy',
      fields: [{ id: 'webhook_url', labelKey: 'wizard.cobrament.field.webhook', type: 'url', required: false }],
    },
    {
      id: 'verify',
      titleKey: 'wizard.cobrament.step3.title',
      descriptionKey: 'wizard.cobrament.step3.description',
      type: 'verify',
      action: { type: 'verify' },
    },
  ],
});
