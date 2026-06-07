import type { ServiceGuideConfig } from './index';

const config: ServiceGuideConfig = {
  type: 'WHATSAPP',
  titleKey: 'guides.service_titles.WHATSAPP',
  descriptionKey: 'guides.service_descriptions.WHATSAPP',
  sections: [
    {
      id: 'access',
      titleKey: 'guides.section_access',
      type: 'text',
      content: 'guides.whatsapp.access_text',
    },
    {
      id: 'how-it-works',
      titleKey: 'guides.section_howto',
      type: 'text',
      content: 'guides.whatsapp.how_it_works',
    },
    {
      id: 'api-credentials',
      titleKey: 'guides.section_credentials',
      type: 'credentials',
      content: ['API Key', 'Webhook URL'],
    },
    {
      id: 'test',
      titleKey: 'guides.section_test',
      type: 'link',
      content: 'guides.whatsapp.test_link',
    },
  ],
  faq: [
    { questionKey: 'guides.whatsapp.faq_no_messages_q', answerKey: 'guides.whatsapp.faq_no_messages_a' },
    { questionKey: 'guides.whatsapp.faq_change_number_q', answerKey: 'guides.whatsapp.faq_change_number_a' },
    { questionKey: 'guides.whatsapp.faq_template_q', answerKey: 'guides.whatsapp.faq_template_a' },
  ],
};

export default config;
