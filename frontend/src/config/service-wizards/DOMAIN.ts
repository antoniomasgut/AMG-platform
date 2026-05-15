import { registerWizard } from './index';

registerWizard({
  slug: 'domini-gestionat',
  serviceType: 'OTHER',
  titleKey: 'wizard.domain.title',
  descriptionKey: 'wizard.domain.description',
  prerequisitesKey: 'wizard.domain.prerequisites',
  steps: [
    {
      id: 'domain-name',
      titleKey: 'wizard.domain.step1.title',
      descriptionKey: 'wizard.domain.step1.description',
      type: 'form',
      fields: [
        { id: 'domain_name', labelKey: 'wizard.domain.field.name', type: 'text', required: true, placeholderKey: 'wizard.domain.field.name_placeholder', validation: { pattern: '^([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$', messageKey: 'wizard.validation.domain' } },
        { id: 'domain_type', labelKey: 'wizard.domain.field.type', type: 'select', required: true, options: [
          { value: 'managed', labelKey: 'wizard.domain.type.managed' },
          { value: 'self', labelKey: 'wizard.domain.type.self' },
        ]},
      ],
    },
    {
      id: 'dns-config',
      titleKey: 'wizard.domain.step2.title',
      descriptionKey: 'wizard.domain.step2.description',
      type: 'copy',
      fields: [
        { id: 'nameserver1', labelKey: 'wizard.domain.field.ns1', type: 'text', required: false },
        { id: 'nameserver2', labelKey: 'wizard.domain.field.ns2', type: 'text', required: false },
      ],
    },
    {
      id: 'verify',
      titleKey: 'wizard.domain.step3.title',
      descriptionKey: 'wizard.domain.step3.description',
      type: 'verify',
      action: { type: 'verify' },
    },
  ],
});
