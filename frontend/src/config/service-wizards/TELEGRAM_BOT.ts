import { registerWizard, type ServiceWizardConfig } from './index';

function makeTelegramConfig(slug: string): ServiceWizardConfig {
  return {
    slug,
    serviceType: 'OTHER',
    titleKey: 'wizard.telegram.title',
    descriptionKey: 'wizard.telegram.description',
    prerequisitesKey: 'wizard.telegram.prerequisites',
    steps: [
      {
        id: 'bot-credentials',
        titleKey: 'wizard.telegram.step1.title',
        descriptionKey: 'wizard.telegram.step1.description',
        type: 'credentials',
        fields: [
          { id: 'telegram_bot_token', labelKey: 'wizard.telegram.field.token', type: 'password', required: true, hintKey: 'wizard.telegram.field.token_hint' },
          { id: 'bot_username', labelKey: 'wizard.telegram.field.username', type: 'text', required: true, placeholderKey: 'wizard.telegram.field.username_placeholder' },
        ],
      },
      {
        id: 'webhook',
        titleKey: 'wizard.telegram.step2.title',
        descriptionKey: 'wizard.telegram.step2.description',
        type: 'copy',
        fields: [{ id: 'webhook_url', labelKey: 'wizard.telegram.field.webhook', type: 'url', required: false }],
      },
      {
        id: 'commands',
        titleKey: 'wizard.telegram.step3.title',
        descriptionKey: 'wizard.telegram.step3.description',
        type: 'form',
        fields: [
          { id: 'commands_list', labelKey: 'wizard.telegram.field.commands', type: 'text', required: false, placeholderKey: 'wizard.telegram.field.commands_placeholder', hintKey: 'wizard.telegram.field.commands_hint' },
        ],
      },
      {
        id: 'verify',
        titleKey: 'wizard.telegram.step4.title',
        descriptionKey: 'wizard.telegram.step4.description',
        type: 'verify',
        action: { type: 'verify' },
      },
    ],
  };
}

registerWizard(makeTelegramConfig('telegram-bot'));
registerWizard(makeTelegramConfig('bot-telegram'));
