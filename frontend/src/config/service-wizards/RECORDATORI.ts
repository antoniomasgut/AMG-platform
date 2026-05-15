import { registerWizard } from './index';

registerWizard({
  slug: 'recordatori-24h',
  serviceType: 'AUTOMATION',
  titleKey: 'wizard.recordatori.title',
  descriptionKey: 'wizard.recordatori.description',
  prerequisitesKey: 'wizard.recordatori.prerequisites',
  steps: [
    {
      id: 'connect-calendar',
      titleKey: 'wizard.recordatori.step1.title',
      descriptionKey: 'wizard.recordatori.step1.description',
      type: 'credentials',
      fields: [
        { id: 'calendar_api_key', labelKey: 'wizard.recordatori.field.calendar_key', type: 'password', required: true },
        { id: 'calendar_id', labelKey: 'wizard.recordatori.field.calendar_id', type: 'text', required: true, placeholderKey: 'wizard.recordatori.field.calendar_id_placeholder' },
      ],
    },
    {
      id: 'message-template',
      titleKey: 'wizard.recordatori.step2.title',
      descriptionKey: 'wizard.recordatori.step2.description',
      type: 'form',
      fields: [
        { id: 'reminder_template', labelKey: 'wizard.recordatori.field.template', type: 'text', required: true, placeholderKey: 'wizard.recordatori.field.template_placeholder' },
        { id: 'hours_before', labelKey: 'wizard.recordatori.field.hours', type: 'number', required: true, placeholderKey: '24' },
      ],
    },
    {
      id: 'activate',
      titleKey: 'wizard.recordatori.step3.title',
      descriptionKey: 'wizard.recordatori.step3.description',
      type: 'verify',
      action: { type: 'advance' },
    },
  ],
});
