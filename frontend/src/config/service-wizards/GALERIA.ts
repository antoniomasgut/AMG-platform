import { registerWizard } from './index';

registerWizard({
  slug: 'galeria-fotos',
  serviceType: 'OTHER',
  titleKey: 'wizard.galeria.title',
  descriptionKey: 'wizard.galeria.description',
  prerequisitesKey: 'wizard.galeria.prerequisites',
  steps: [
    {
      id: 'upload-photos',
      titleKey: 'wizard.galeria.step1.title',
      descriptionKey: 'wizard.galeria.step1.description',
      type: 'form',
      fields: [
        { id: 'categories', labelKey: 'wizard.galeria.field.categories', type: 'text', required: false, placeholderKey: 'wizard.galeria.field.categories_placeholder' },
      ],
    },
    {
      id: 'publish',
      titleKey: 'wizard.galeria.step2.title',
      descriptionKey: 'wizard.galeria.step2.description',
      type: 'verify',
      action: { type: 'advance' },
    },
  ],
});
