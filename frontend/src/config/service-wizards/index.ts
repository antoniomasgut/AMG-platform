export interface WizardField {
  id: string;
  labelKey: string;
  type: 'text' | 'password' | 'url' | 'number' | 'select';
  required: boolean;
  placeholderKey?: string;
  hintKey?: string;
  options?: Array<{ value: string; labelKey: string }>;
  validation?: {
    pattern?: string;
    minLength?: number;
    maxLength?: number;
    messageKey: string;
  };
}

export interface WizardStep {
  id: string;
  titleKey: string;
  descriptionKey: string;
  type: 'credentials' | 'info' | 'form' | 'verify' | 'copy' | 'link';
  fields?: WizardField[];
  action?: {
    type: 'verify' | 'advance' | 'approve' | 'none';
  };
}

export interface ServiceWizardConfig {
  /** Slug del servei (ex: whatsapp-business, agenda-online) */
  slug: string;
  /** Tipus de servei (ex: CREDENTIALS, AUTOMATION) */
  serviceType: string;
  titleKey: string;
  descriptionKey: string;
  prerequisitesKey: string;
  steps: WizardStep[];
}

const slugRegistry = new Map<string, ServiceWizardConfig>();
/** Fallback: type → primer wizard registrat amb aquest type */
const typeFallback = new Map<string, string>();

export function registerWizard(config: ServiceWizardConfig) {
  slugRegistry.set(config.slug, config);
  if (!typeFallback.has(config.serviceType)) {
    typeFallback.set(config.serviceType, config.slug);
  }
}

export function getWizardConfig(slug: string, serviceType?: string): ServiceWizardConfig | undefined {
  // Try exact slug first
  const exact = slugRegistry.get(slug);
  if (exact) return exact;
  // Fallback to serviceType
  if (serviceType) {
    const fallbackSlug = typeFallback.get(serviceType);
    if (fallbackSlug) return slugRegistry.get(fallbackSlug);
  }
  return undefined;
}

export function listWizardSlugs(): string[] {
  return Array.from(slugRegistry.keys());
}

// Import all wizards
import './WHATSAPP';
import './SMTP';
import './LANDING';
import './LANDING_EXTRA';
import './BOT_IA';
import './BOT_IA_RAG';
import './AGENDA';
import './RECORDATORI';
import './REENGANXAMENT';
import './RESSENYA';
import './CALCULADORA';
import './CRM';
import './GALERIA';
import './COBRAMENT';
import './FACTURACIO';
import './ANALYTICS';
import './XAT_WEB';
import './FORMULARI';
import './QÜESTIONARI';
import './HISTORIAL';
import './DOMAIN';
import './TELEGRAM_BOT';
