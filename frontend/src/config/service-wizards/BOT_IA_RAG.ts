import { registerWizard } from './index';

registerWizard({
  slug: 'bot-ia-rag',
  serviceType: 'OTHER',
  titleKey: 'wizard.bot_ia_rag.title',
  descriptionKey: 'wizard.bot_ia_rag.description',
  prerequisitesKey: 'wizard.bot_ia_rag.prerequisites',
  steps: [
    {
      id: 'api-config',
      titleKey: 'wizard.bot_ia_rag.step1.title',
      descriptionKey: 'wizard.bot_ia_rag.step1.description',
      type: 'credentials',
      fields: [
        { id: 'api_key', labelKey: 'wizard.bot_ia_rag.field.api_key', type: 'password', required: true },
        { id: 'model', labelKey: 'wizard.bot_ia_rag.field.model', type: 'select', required: true, options: [
          { value: 'claude-sonnet-4-6', labelKey: 'wizard.bot_ia_rag.model.sonnet' },
          { value: 'claude-haiku-4-5', labelKey: 'wizard.bot_ia_rag.model.haiku' },
        ]},
        { id: 'bot_name', labelKey: 'wizard.bot_ia_rag.field.name', type: 'text', required: true, placeholderKey: 'wizard.bot_ia_rag.field.name_placeholder' },
      ],
    },
    {
      id: 'documents',
      titleKey: 'wizard.bot_ia_rag.step2.title',
      descriptionKey: 'wizard.bot_ia_rag.step2.description',
      type: 'form',
      fields: [{ id: 'document_urls', labelKey: 'wizard.bot_ia_rag.field.documents', type: 'text', required: false, hintKey: 'wizard.bot_ia_rag.field.documents_hint', placeholderKey: 'wizard.bot_ia_rag.field.documents_placeholder' }],
    },
    {
      id: 'verify',
      titleKey: 'wizard.bot_ia_rag.step3.title',
      descriptionKey: 'wizard.bot_ia_rag.step3.description',
      type: 'verify',
      action: { type: 'verify' },
    },
  ],
});
