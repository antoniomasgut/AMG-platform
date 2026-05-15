import { registerWizard } from './index';

registerWizard({
  slug: 'historial-medic',
  serviceType: 'OTHER',
  titleKey: 'wizard.historial.title',
  descriptionKey: 'wizard.historial.description',
  prerequisitesKey: 'wizard.historial.prerequisites',
  steps: [
    {
      id: 'configure',
      titleKey: 'wizard.historial.step1.title',
      descriptionKey: 'wizard.historial.step1.description',
      type: 'form',
      fields: [
        { id: 'fields_config', labelKey: 'wizard.historial.field.config', type: 'text', required: true, placeholderKey: 'wizard.historial.field.config_placeholder' },
      ],
    },
    {
      id: 'activate',
      titleKey: 'wizard.historial.step2.title',
      descriptionKey: 'wizard.historial.step2.description',
      type: 'verify',
      action: { type: 'advance' },
    },
  ],
});
