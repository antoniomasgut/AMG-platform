export interface ServiceGuideConfig {
  type: string;
  titleKey: string;
  descriptionKey: string;
  sections: GuideSection[];
  faq?: GuideFAQ[];
}

export interface GuideSection {
  id: string;
  titleKey: string;
  type: 'text' | 'credentials' | 'link' | 'steps' | 'warning' | 'info';
  content: string | string[];
}

export interface GuideFAQ {
  questionKey: string;
  answerKey: string;
}

const configs: Record<string, ServiceGuideConfig> = {};

export function registerGuideConfig(config: ServiceGuideConfig) {
  configs[config.type] = config;
}

export function getGuideConfig(serviceType: string): ServiceGuideConfig | undefined {
  return configs[serviceType];
}

export function getAllGuideTypes(): string[] {
  return Object.keys(configs);
}

export function getAllGuideConfigs(): ServiceGuideConfig[] {
  return Object.values(configs);
}

// Register all guide configs at module top level
import landingConfig from './LANDING';
import whatsappConfig from './WHATSAPP';
import smtpConfig from './SMTP';
import botIaConfig from './BOT_IA';
import automationConfig from './AUTOMATION';
import domainConfig from './DOMAIN';

registerGuideConfig(landingConfig);
registerGuideConfig(whatsappConfig);
registerGuideConfig(smtpConfig);
registerGuideConfig(botIaConfig);
registerGuideConfig(automationConfig);
registerGuideConfig(domainConfig);
