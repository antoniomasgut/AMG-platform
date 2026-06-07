import type { ServiceGuideConfig } from './index';

const config: ServiceGuideConfig = {
  type: 'BOT_IA',
  titleKey: 'guides.service_titles.BOT_IA',
  descriptionKey: 'guides.service_descriptions.BOT_IA',
  sections: [
    {
      id: 'access',
      titleKey: 'guides.section_access',
      type: 'link',
      content: 'guides.bot_ia.access_link',
    },
    {
      id: 'how-it-works',
      titleKey: 'guides.section_howto',
      type: 'text',
      content: 'guides.bot_ia.how_it_works',
    },
    {
      id: 'manage-responses',
      titleKey: 'guides.section_manage_responses',
      type: 'link',
      content: 'guides.bot_ia.manage_link',
    },
  ],
  faq: [
    { questionKey: 'guides.bot_ia.faq_add_info_q', answerKey: 'guides.bot_ia.faq_add_info_a' },
    { questionKey: 'guides.bot_ia.faq_bad_response_q', answerKey: 'guides.bot_ia.faq_bad_response_a' },
    { questionKey: 'guides.bot_ia.faq_integration_q', answerKey: 'guides.bot_ia.faq_integration_a' },
  ],
};

export default config;
