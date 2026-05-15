import { registerWizard } from './index';

registerWizard({
  slug: 'agenda-online',
  serviceType: 'AUTOMATION',
  titleKey: 'wizard.agenda.title',
  descriptionKey: 'wizard.agenda.description',
  prerequisitesKey: 'wizard.agenda.prerequisites',
  steps: [
    {
      id: 'calendar-integration',
      titleKey: 'wizard.agenda.step1.title',
      descriptionKey: 'wizard.agenda.step1.description',
      type: 'credentials',
      fields: [
        { id: 'calendar_type', labelKey: 'wizard.agenda.field.calendar_type', type: 'select', required: true, options: [
          { value: 'google', labelKey: 'wizard.agenda.calendar.google' },
          { value: 'calendly', labelKey: 'wizard.agenda.calendar.calendly' },
        ]},
        { id: 'api_key', labelKey: 'wizard.agenda.field.api_key', type: 'password', required: true, hintKey: 'wizard.agenda.field.api_key_hint' },
      ],
    },
    {
      id: 'confirmation',
      titleKey: 'wizard.agenda.step2.title',
      descriptionKey: 'wizard.agenda.step2.description',
      type: 'form',
      fields: [
        { id: 'confirmation_template', labelKey: 'wizard.agenda.field.confirmation', type: 'text', required: true, placeholderKey: 'wizard.agenda.field.confirmation_placeholder' },
      ],
    },
    {
      id: 'verify',
      titleKey: 'wizard.agenda.step3.title',
      descriptionKey: 'wizard.agenda.step3.description',
      type: 'verify',
      action: { type: 'verify' },
    },
  ],
});
