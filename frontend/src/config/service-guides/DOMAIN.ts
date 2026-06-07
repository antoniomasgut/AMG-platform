import type { ServiceGuideConfig } from './index';

const config: ServiceGuideConfig = {
  type: 'DOMAIN',
  titleKey: 'guides.service_titles.DOMAIN',
  descriptionKey: 'guides.service_descriptions.DOMAIN',
  sections: [
    {
      id: 'dns',
      titleKey: 'guides.section_dns',
      type: 'credentials',
      content: ['Nameserver 1', 'Nameserver 2'],
    },
    {
      id: 'setup',
      titleKey: 'guides.section_setup',
      type: 'steps',
      content: [
        'guides.domain.setup_step_1',
        'guides.domain.setup_step_2',
        'guides.domain.setup_step_3',
      ],
    },
    {
      id: 'propagation',
      titleKey: 'guides.section_propagation',
      type: 'info',
      content: 'guides.domain.propagation_text',
    },
    {
      id: 'verify',
      titleKey: 'guides.section_verify',
      type: 'link',
      content: 'guides.domain.verify_link',
    },
  ],
  faq: [
    { questionKey: 'guides.domain.faq_time_q', answerKey: 'guides.domain.faq_time_a' },
    { questionKey: 'guides.domain.faq_not_visible_q', answerKey: 'guides.domain.faq_not_visible_a' },
    { questionKey: 'guides.domain.faq_ssl_q', answerKey: 'guides.domain.faq_ssl_a' },
  ],
};

export default config;
