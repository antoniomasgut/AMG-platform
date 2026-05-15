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

// Use globalThis directly inside functions to avoid TDZ with circular-dep init order.
// `function` declarations are hoisted (no TDZ), so wizard files can call these safely
// even when this module's body hasn't fully executed yet.
type G = Record<string, unknown>;
function slugMap(): Map<string, ServiceWizardConfig> {
  const g = globalThis as G;
  if (!g.__amgWizardSlugs) g.__amgWizardSlugs = new Map<string, ServiceWizardConfig>();
  return g.__amgWizardSlugs as Map<string, ServiceWizardConfig>;
}
function typeMap(): Map<string, string> {
  const g = globalThis as G;
  if (!g.__amgWizardTypes) g.__amgWizardTypes = new Map<string, string>();
  return g.__amgWizardTypes as Map<string, string>;
}

export function registerWizard(config: ServiceWizardConfig) {
  slugMap().set(config.slug, config);
  if (!typeMap().has(config.serviceType)) {
    typeMap().set(config.serviceType, config.slug);
  }
}

export function getWizardConfig(slug: string, serviceType?: string): ServiceWizardConfig | undefined {
  const exact = slugMap().get(slug);
  if (exact) return exact;
  if (serviceType) {
    const fallbackSlug = typeMap().get(serviceType);
    if (fallbackSlug) return slugMap().get(fallbackSlug);
  }
  return undefined;
}

export function listWizardSlugs(): string[] {
  return Array.from(slugMap().keys());
}

// Import all wizards (side-effect: they call registerWizard)
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
