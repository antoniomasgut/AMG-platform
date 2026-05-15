import { registerWizard } from './index';

registerWizard({
  slug: 'facturacio',
  serviceType: 'BILLING',
  titleKey: 'wizard.facturacio.title',
  descriptionKey: 'wizard.facturacio.description',
  prerequisitesKey: 'wizard.facturacio.prerequisites',
  steps: [
    {
      id: 'holded-credentials',
      titleKey: 'wizard.facturacio.step1.title',
      descriptionKey: 'wizard.facturacio.step1.description',
      type: 'credentials',
      fields: [
        { id: 'holded_api_key', labelKey: 'wizard.facturacio.field.api_key', type: 'password', required: true },
        { id: 'client_vat', labelKey: 'wizard.facturacio.field.vat', type: 'text', required: true, placeholderKey: 'wizard.facturacio.field.vat_placeholder' },
        { id: 'client_address', labelKey: 'wizard.facturacio.field.address', type: 'text', required: true, placeholderKey: 'wizard.facturacio.field.address_placeholder' },
      ],
    },
    {
      id: 'schedule',
      titleKey: 'wizard.facturacio.step2.title',
      descriptionKey: 'wizard.facturacio.step2.description',
      type: 'form',
      fields: [
        { id: 'billing_day', labelKey: 'wizard.facturacio.field.day', type: 'number', required: true, placeholderKey: '1' },
      ],
    },
    {
      id: 'verify',
      titleKey: 'wizard.facturacio.step3.title',
      descriptionKey: 'wizard.facturacio.step3.description',
      type: 'verify',
      action: { type: 'verify' },
    },
  ],
});
