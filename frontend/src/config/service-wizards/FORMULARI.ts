import { registerWizard } from './index';

registerWizard({
  slug: 'formulari-leads',
  serviceType: 'OTHER',
  titleKey: 'wizard.formulari.title',
  descriptionKey: 'wizard.formulari.description',
  prerequisitesKey: 'wizard.formulari.prerequisites',
  steps: [
    {
      id: 'configure-fields',
      titleKey: 'wizard.formulari.step1.title',
      descriptionKey: 'wizard.formulari.step1.description',
      type: 'form',
      fields: [
        { id: 'notification_email', labelKey: 'wizard.formulari.field.email', type: 'text', required: true, placeholderKey: 'wizard.formulari.field.email_placeholder' },
      ],
    },
    {
      id: 'activate',
      titleKey: 'wizard.formulari.step2.title',
      descriptionKey: 'wizard.formulari.step2.description',
      type: 'verify',
      action: { type: 'advance' },
    },
  ],
});
