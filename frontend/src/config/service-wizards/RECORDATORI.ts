import { registerWizard } from './index';

// Genera un codi d'enllaç aleatori de 8 caràcters
function generateLinkCode(): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let code = '';
  for (let i = 0; i < 8; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return code;
}

registerWizard({
  slug: 'recordatori-24h',
  serviceType: 'AUTOMATION',
  titleKey: 'wizard.recordatori.title',
  descriptionKey: 'wizard.recordatori.description',
  prerequisitesKey: 'wizard.recordatori.prerequisites',
  defaultFormData: {
    link_code: generateLinkCode(),
  },
  steps: [
    {
      id: 'configure-message',
      titleKey: 'wizard.recordatori.step1.title',
      descriptionKey: 'wizard.recordatori.step1.description',
      type: 'credentials',
      fields: [
        { id: 'reminder_template', labelKey: 'wizard.recordatori.field.template', type: 'text', required: true, placeholderKey: 'wizard.recordatori.field.template_placeholder' },
        { id: 'hours_before', labelKey: 'wizard.recordatori.field.hours', type: 'number', required: true, placeholderKey: '24' },
      ],
    },
    {
      id: 'link-telegram',
      titleKey: 'wizard.recordatori.step2.title',
      descriptionKey: 'wizard.recordatori.step2.description',
      type: 'copy',
      fields: [
        { id: 'link_code', labelKey: 'wizard.recordatori.field.link_code', type: 'text', required: false },
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
