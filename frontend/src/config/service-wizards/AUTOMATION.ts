import { registerWizard } from './index';

registerWizard({
  slug: 'n8n-automation-generic',
  serviceType: 'AUTOMATION',
  titleKey: 'wizard.automation.title',
  descriptionKey: 'wizard.automation.description',
  prerequisitesKey: 'wizard.automation.prerequisites',
  steps: [
    {
      id: 'n8n-connection',
      titleKey: 'wizard.automation.step1.title',
      descriptionKey: 'wizard.automation.step1.description',
      type: 'credentials',
      fields: [
        { id: 'n8n_url', labelKey: 'wizard.automation.field.url', type: 'url', required: true, placeholderKey: 'wizard.automation.field.url_placeholder' },
        { id: 'n8n_api_key', labelKey: 'wizard.automation.field.api_key', type: 'password', required: true },
      ],
    },
    {
      id: 'workflows',
      titleKey: 'wizard.automation.step2.title',
      descriptionKey: 'wizard.automation.step2.description',
      type: 'info',
    },
    {
      id: 'verify',
      titleKey: 'wizard.automation.step3.title',
      descriptionKey: 'wizard.automation.step3.description',
      type: 'verify',
      action: { type: 'verify' },
    },
  ],
});
