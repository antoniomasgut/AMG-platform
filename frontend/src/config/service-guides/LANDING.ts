import type { ServiceGuideConfig } from './index';

const config: ServiceGuideConfig = {
  type: 'LANDING',
  titleKey: 'guides.service_titles.LANDING',
  descriptionKey: 'guides.service_descriptions.LANDING',
  sections: [
    {
      id: 'access',
      titleKey: 'guides.section_access',
      type: 'link',
      content: 'guides.landing.access_link',
    },
    {
      id: 'how-to-edit',
      titleKey: 'guides.section_howto_edit',
      type: 'steps',
      content: [
        'guides.landing.edit_step_1',
        'guides.landing.edit_step_2',
        'guides.landing.edit_step_3',
        'guides.landing.edit_step_4',
      ],
    },
    {
      id: 'how-to-publish',
      titleKey: 'guides.section_howto_publish',
      type: 'steps',
      content: [
        'guides.landing.publish_step_1',
        'guides.landing.publish_step_2',
        'guides.landing.publish_step_3',
      ],
    },
    {
      id: 'domains',
      titleKey: 'guides.section_domains',
      type: 'text',
      content: 'guides.landing.domains_text',
    },
  ],
  faq: [
    { questionKey: 'guides.landing.faq_colors_q', answerKey: 'guides.landing.faq_colors_a' },
    { questionKey: 'guides.landing.faq_sections_q', answerKey: 'guides.landing.faq_sections_a' },
    { questionKey: 'guides.landing.faq_domain_q', answerKey: 'guides.landing.faq_domain_a' },
    { questionKey: 'guides.landing.faq_seo_q', answerKey: 'guides.landing.faq_seo_a' },
  ],
};

export default config;
