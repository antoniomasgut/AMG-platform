import type { ServiceGuideConfig } from './index';

const config: ServiceGuideConfig = {
  type: 'AUTOMATION',
  titleKey: 'guides.service_titles.AUTOMATION',
  descriptionKey: 'guides.service_descriptions.AUTOMATION',
  sections: [
    {
      id: 'access',
      titleKey: 'guides.section_access',
      type: 'link',
      content: 'guides.automation.access_link',
    },
    {
      id: 'workflows',
      titleKey: 'guides.section_workflows',
      type: 'text',
      content: 'guides.automation.workflows_text',
    },
    {
      id: 'how-to-manage',
      titleKey: 'guides.section_howto_manage',
      type: 'steps',
      content: [
        'guides.automation.manage_step_1',
        'guides.automation.manage_step_2',
        'guides.automation.manage_step_3',
      ],
    },
  ],
  faq: [
    { questionKey: 'guides.automation.faq_failed_q', answerKey: 'guides.automation.faq_failed_a' },
    { questionKey: 'guides.automation.faq_new_automations_q', answerKey: 'guides.automation.faq_new_automations_a' },
    { questionKey: 'guides.automation.faq_monitoring_q', answerKey: 'guides.automation.faq_monitoring_a' },
  ],
};

export default config;
