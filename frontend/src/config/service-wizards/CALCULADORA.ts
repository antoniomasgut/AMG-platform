import { registerWizard } from './index';

registerWizard({
  slug: 'calc-pressupost',
  serviceType: 'OTHER',
  titleKey: 'wizard.calculadora.title',
  descriptionKey: 'wizard.calculadora.description',
  prerequisitesKey: 'wizard.calculadora.prerequisites',
  steps: [
    {
      id: 'form-fields',
      titleKey: 'wizard.calculadora.step1.title',
      descriptionKey: 'wizard.calculadora.step1.description',
      type: 'form',
      fields: [
        { id: 'pricing_rules', labelKey: 'wizard.calculadora.field.rules', type: 'text', required: true, placeholderKey: 'wizard.calculadora.field.rules_placeholder' },
      ],
    },
    {
      id: 'notification',
      titleKey: 'wizard.calculadora.step2.title',
      descriptionKey: 'wizard.calculadora.step2.description',
      type: 'form',
      fields: [
        { id: 'notification_template', labelKey: 'wizard.calculadora.field.template', type: 'text', required: true, placeholderKey: 'wizard.calculadora.field.template_placeholder' },
      ],
    },
    {
      id: 'activate',
      titleKey: 'wizard.calculadora.step3.title',
      descriptionKey: 'wizard.calculadora.step3.description',
      type: 'verify',
      action: { type: 'advance' },
    },
  ],
});
