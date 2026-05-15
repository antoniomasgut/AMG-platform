import { registerWizard } from './index';

registerWizard({
  slug: 'smtp-corporatiu',
  serviceType: 'CREDENTIALS',
  titleKey: 'wizard.smtp.title',
  descriptionKey: 'wizard.smtp.description',
  prerequisitesKey: 'wizard.smtp.prerequisites',
  steps: [
    {
      id: 'server-config',
      titleKey: 'wizard.smtp.step1.title',
      descriptionKey: 'wizard.smtp.step1.description',
      type: 'credentials',
      fields: [
        { id: 'smtp_host', labelKey: 'wizard.smtp.field.host', type: 'text', required: true, placeholderKey: 'wizard.smtp.field.host_placeholder' },
        { id: 'smtp_port', labelKey: 'wizard.smtp.field.port', type: 'number', required: true, placeholderKey: 'wizard.smtp.field.port_placeholder', validation: { pattern: '^[0-9]{2,5}$', messageKey: 'wizard.validation.port' } },
        { id: 'smtp_user', labelKey: 'wizard.smtp.field.user', type: 'text', required: true, placeholderKey: 'wizard.smtp.field.user_placeholder' },
        { id: 'smtp_pass', labelKey: 'wizard.smtp.field.password', type: 'password', required: true },
        { id: 'smtp_security', labelKey: 'wizard.smtp.field.security', type: 'select', required: true, options: [
          { value: 'TLS', labelKey: 'wizard.smtp.security.tls' },
          { value: 'SSL', labelKey: 'wizard.smtp.security.ssl' },
          { value: 'NONE', labelKey: 'wizard.smtp.security.none' },
        ]},
      ],
    },
    {
      id: 'verify',
      titleKey: 'wizard.smtp.step2.title',
      descriptionKey: 'wizard.smtp.step2.description',
      type: 'verify',
      action: { type: 'verify' },
    },
  ],
});
