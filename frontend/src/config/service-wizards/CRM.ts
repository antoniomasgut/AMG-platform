import { registerWizard } from './index';

registerWizard({
  slug: 'crm-pipeline',
  serviceType: 'OTHER',
  titleKey: 'wizard.crm.title',
  descriptionKey: 'wizard.crm.description',
  prerequisitesKey: 'wizard.crm.prerequisites',
  steps: [
    {
      id: 'pipeline-stages',
      titleKey: 'wizard.crm.step1.title',
      descriptionKey: 'wizard.crm.step1.description',
      type: 'form',
      fields: [
        { id: 'stages', labelKey: 'wizard.crm.field.stages', type: 'text', required: true, placeholderKey: 'wizard.crm.field.stages_placeholder' },
      ],
    },
    {
      id: 'notifications',
      titleKey: 'wizard.crm.step2.title',
      descriptionKey: 'wizard.crm.step2.description',
      type: 'form',
      fields: [
        { id: 'notification_whatsapp', labelKey: 'wizard.crm.field.notify_whatsapp', type: 'select', required: true, options: [
          { value: 'yes', labelKey: 'wizard.common.yes' },
          { value: 'no', labelKey: 'wizard.common.no' },
        ]},
      ],
    },
    {
      id: 'activate',
      titleKey: 'wizard.crm.step3.title',
      descriptionKey: 'wizard.crm.step3.description',
      type: 'verify',
      action: { type: 'advance' },
    },
  ],
});
