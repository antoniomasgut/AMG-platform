import type { ServiceGuideConfig } from './index';

const config: ServiceGuideConfig = {
  type: 'SMTP',
  titleKey: 'guides.service_titles.SMTP',
  descriptionKey: 'guides.service_descriptions.SMTP',
  sections: [
    {
      id: 'credentials',
      titleKey: 'guides.section_credentials',
      type: 'credentials',
      content: ['Servidor', 'Port', 'Usuari', 'Contrasenya', 'Seguretat (TLS/SSL)'],
    },
    {
      id: 'outlook-setup',
      titleKey: 'guides.smtp.outlook_title',
      type: 'steps',
      content: [
        'guides.smtp.outlook_step_1',
        'guides.smtp.outlook_step_2',
        'guides.smtp.outlook_step_3',
      ],
    },
    {
      id: 'gmail-setup',
      titleKey: 'guides.smtp.gmail_title',
      type: 'steps',
      content: [
        'guides.smtp.gmail_step_1',
        'guides.smtp.gmail_step_2',
        'guides.smtp.gmail_step_3',
        'guides.smtp.gmail_step_4',
      ],
    },
    {
      id: 'thunderbird-setup',
      titleKey: 'guides.smtp.thunderbird_title',
      type: 'steps',
      content: [
        'guides.smtp.thunderbird_step_1',
        'guides.smtp.thunderbird_step_2',
        'guides.smtp.thunderbird_step_3',
        'guides.smtp.thunderbird_step_4',
      ],
    },
    {
      id: 'test',
      titleKey: 'guides.section_test',
      type: 'link',
      content: 'guides.smtp.test_link',
    },
  ],
  faq: [
    { questionKey: 'guides.smtp.faq_send_q', answerKey: 'guides.smtp.faq_send_a' },
    { questionKey: 'guides.smtp.faq_port_q', answerKey: 'guides.smtp.faq_port_a' },
    { questionKey: 'guides.smtp.faq_auth_q', answerKey: 'guides.smtp.faq_auth_a' },
  ],
};

export default config;
