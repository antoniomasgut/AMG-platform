import { registerWizard } from './index';

registerWizard({
  slug: 'bot-ia-basic',
  serviceType: 'OTHER',
  titleKey: 'wizard.bot_ia.title',
  descriptionKey: 'wizard.bot_ia.description',
  prerequisitesKey: 'wizard.bot_ia.prerequisites',
  steps: [
    {
      id: 'api-config',
      titleKey: 'wizard.bot_ia.step1.title',
      descriptionKey: 'wizard.bot_ia.step1.description',
      type: 'credentials',
      fields: [
        { id: 'api_key', labelKey: 'wizard.bot_ia.field.api_key', type: 'password', required: true },
        { id: 'model', labelKey: 'wizard.bot_ia.field.model', type: 'select', required: true, options: [
          { value: 'claude-sonnet-4-6', labelKey: 'wizard.bot_ia.model.sonnet' },
          { value: 'claude-haiku-4-5', labelKey: 'wizard.bot_ia.model.haiku' },
          { value: 'gpt-4o', labelKey: 'wizard.bot_ia.model.gpt4o' },
        ]},
        { id: 'bot_name', labelKey: 'wizard.bot_ia.field.name', type: 'text', required: true, placeholderKey: 'wizard.bot_ia.field.name_placeholder' },
      ],
    },
    {
      id: 'welcome-message',
      titleKey: 'wizard.bot_ia.step2.title',
      descriptionKey: 'wizard.bot_ia.step2.description',
      type: 'form',
      fields: [{ id: 'welcome_text', labelKey: 'wizard.bot_ia.field.welcome', type: 'text', required: true, placeholderKey: 'wizard.bot_ia.field.welcome_placeholder' }],
    },
    {
      id: 'verify',
      titleKey: 'wizard.bot_ia.step3.title',
      descriptionKey: 'wizard.bot_ia.step3.description',
      type: 'verify',
      action: { type: 'verify' },
    },
  ],
});
