import { registerWizard } from './index';

registerWizard({
  slug: 'xat-web',
  serviceType: 'CREDENTIALS',
  titleKey: 'wizard.xat.title',
  descriptionKey: 'wizard.xat.description',
  prerequisitesKey: 'wizard.xat.prerequisites',
  steps: [
    {
      id: 'api-credentials',
      titleKey: 'wizard.xat.step1.title',
      descriptionKey: 'wizard.xat.step1.description',
      type: 'credentials',
      fields: [
        { id: 'chat_api_key', labelKey: 'wizard.xat.field.api_key', type: 'password', required: true },
        { id: 'chat_widget_id', labelKey: 'wizard.xat.field.widget_id', type: 'text', required: false, placeholderKey: 'wizard.xat.field.widget_id_placeholder' },
      ],
    },
    {
      id: 'verify',
      titleKey: 'wizard.xat.step2.title',
      descriptionKey: 'wizard.xat.step2.description',
      type: 'verify',
      action: { type: 'verify' },
    },
  ],
});
