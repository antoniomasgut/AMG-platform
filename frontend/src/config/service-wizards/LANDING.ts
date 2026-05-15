import { registerWizard } from './index';

registerWizard({
  slug: 'landing-pro',
  serviceType: 'LANDING',
  titleKey: 'wizard.landing.title',
  descriptionKey: 'wizard.landing.description',
  prerequisitesKey: 'wizard.landing.prerequisites',
  steps: [
    {
      id: 'select-template',
      titleKey: 'wizard.landing.step1.title',
      descriptionKey: 'wizard.landing.step1.description',
      type: 'form',
      fields: [{ id: 'template_id', labelKey: 'wizard.landing.field.template', type: 'select', required: true, options: [] }],
    },
    {
      id: 'fill-content',
      titleKey: 'wizard.landing.step2.title',
      descriptionKey: 'wizard.landing.step2.description',
      type: 'form',
    },
    {
      id: 'publish',
      titleKey: 'wizard.landing.step3.title',
      descriptionKey: 'wizard.landing.step3.description',
      type: 'verify',
      action: { type: 'advance' },
    },
  ],
});
