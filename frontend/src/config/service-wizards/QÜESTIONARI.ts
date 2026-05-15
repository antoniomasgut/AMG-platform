import { registerWizard } from './index';

registerWizard({
  slug: 'qüestionari-salut',
  serviceType: 'OTHER',
  titleKey: 'wizard.quest.title',
  descriptionKey: 'wizard.quest.description',
  prerequisitesKey: 'wizard.quest.prerequisites',
  steps: [
    {
      id: 'configure',
      titleKey: 'wizard.quest.step1.title',
      descriptionKey: 'wizard.quest.step1.description',
      type: 'form',
      fields: [
        { id: 'questions', labelKey: 'wizard.quest.field.questions', type: 'text', required: true, placeholderKey: 'wizard.quest.field.questions_placeholder' },
      ],
    },
    {
      id: 'activate',
      titleKey: 'wizard.quest.step2.title',
      descriptionKey: 'wizard.quest.step2.description',
      type: 'verify',
      action: { type: 'advance' },
    },
  ],
});
