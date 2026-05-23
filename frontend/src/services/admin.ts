import { apiFetch } from './api';

export interface UserResponse {
  id: string; email: string; name: string | null;
  role: 'SUPER_ADMIN' | 'ADMIN' | 'CLIENT';
  tenantId: string | null; tenantName: string | null;
  isActive: boolean; isBlocked: boolean;
  lastLoginAt: string | null; createdAt: string;
}

export interface TenantResponse {
  id: string; name: string; slug: string;
  email: string | null; phone: string | null;
  address: string | null;
  sector: string | null; businessSize: string | null; contractedPhases: string[] | null;
  agentSystemPrompt: string | null;
  isActive: boolean; isFree: boolean; createdAt: string;
}

export interface PageResponse<T> {
  content: T[]; totalPages: number; totalElements: number;
  number: number; size: number;
}

export interface CreateUserRequest {
  email: string; password: string; name: string;
  role: 'SUPER_ADMIN' | 'ADMIN' | 'CLIENT'; tenantId?: string;
}

export interface UpdateUserRequest {
  email?: string; name?: string;
  role?: 'SUPER_ADMIN' | 'ADMIN' | 'CLIENT';
  tenantId?: string; isActive?: boolean;
}

export interface CreateTenantRequest {
  name: string; slug: string; email?: string; phone?: string; address?: string;
}

export interface UpdateTenantRequest {
  name?: string; email?: string; phone?: string; address?: string;
  isActive?: boolean; isFree?: boolean; agentSystemPrompt?: string;
}

export const listUsers = (params: { page?: number; size?: number; role?: string; tenantId?: string; search?: string } = {}) => {
  const q = new URLSearchParams({
    page: String(params.page ?? 0),
    size: String(params.size ?? 20),
  });
  if (params.role) q.set('role', params.role);
  if (params.tenantId) q.set('tenantId', params.tenantId);
  if (params.search) q.set('search', params.search);
  return apiFetch<PageResponse<UserResponse>>(`/users?${q}`);
};

export const getUser = (id: string) =>
  apiFetch<UserResponse>(`/users/${id}`);

export const createUser = (data: CreateUserRequest) =>
  apiFetch<UserResponse>('/users', { method: 'POST', body: JSON.stringify(data) });

export const updateUser = (id: string, data: UpdateUserRequest) =>
  apiFetch<UserResponse>(`/users/${id}`, { method: 'PUT', body: JSON.stringify(data) });

export const deleteUser = (id: string) =>
  apiFetch<void>(`/users/${id}`, { method: 'DELETE' });

export const unlockUser = (id: string) =>
  apiFetch<UserResponse>(`/users/${id}/unlock`, { method: 'POST' });

export const listTenants = (params: { page?: number; size?: number; search?: string; isActive?: boolean; isFree?: boolean; sort?: string } = {}) => {
  const q = new URLSearchParams({
    page: String(params.page ?? 0),
    size: String(params.size ?? 20),
  });
  if (params.search) q.set('search', params.search);
  if (params.isActive !== undefined) q.set('isActive', String(params.isActive));
  if (params.isFree !== undefined) q.set('isFree', String(params.isFree));
  if (params.sort) q.set('sort', params.sort);
  return apiFetch<PageResponse<TenantResponse>>(`/tenants?${q}`);
};

export const getTenant = (id: string) =>
  apiFetch<TenantResponse>(`/tenants/${id}`);

export const createTenant = (data: CreateTenantRequest) =>
  apiFetch<TenantResponse>('/tenants', { method: 'POST', body: JSON.stringify(data) });

export const updateTenant = (id: string, data: UpdateTenantRequest) =>
  apiFetch<TenantResponse>(`/tenants/${id}`, { method: 'PUT', body: JSON.stringify(data) });

// --- Vault API (tenant services) ---

export interface CatalogService {
  id: string; name: string; slug: string;
  description: string; type: string; isAddon: boolean;
  cost: number; salePrice: number; monthlyPrice: number; version: number;
}

export interface OutdatedServiceResponse {
  tenantServiceId: string;
  tenantId: string;
  serviceId: string;
  serviceName: string;
  serviceSlug: string;
  catalogVersion: number;
  lockedVersion: number;
  outdatedAt: string;
}

export interface TenantSetup {
  profiles: Array<{
    profile: { id: string; name: string; slug: string };
    phases: Array<{
      phase: { id: string; name: string; sortOrder: number };
      approvalStatus: string;
      services: Array<{
        tenantServiceId: string;
        service: { id: string; name: string; slug: string; type: string };
        status: string;
        isEnabled: boolean;
      }>;
    }>;
  }>;
  addons: Array<{
    service: { id: string; name: string };
    approvalRequired: boolean;
    approvalStatus: string;
  }>;
}

export interface ChannelsConfig {
  tenantId: string;
  agentMode: 'AUTO' | 'HYBRID' | 'MANUAL';
  isActive: boolean;
  telegramLinked: boolean;
  telegramChatId: number | null;
  whatsappPhoneNumber: string | null;
  whatsappMetaPhoneNumberId: string | null;
}

export interface AIConfig {
  tenantId: string;
  preferredModel: string;
  maxTokens: number;
  temperature: number;
}

export interface ModelInfo {
  id: string; label: string; provider: string; requiresApiKey: boolean;
}

export interface AssignProfileResponse {
  profileId: string;
  phases: Array<{ phaseId: string; name: string; sortOrder: number; approvalStatus: string; totalServices: number; totalPrice: number }>;
  totalPrice: number;
}

export const listCatalogServices = () =>
  apiFetch<CatalogService[]>('/vault/services');

export const getTenantSetup = (tenantId: string) =>
  apiFetch<TenantSetup>(`/vault/tenants/${tenantId}/setup`);

export const assignProfileToTenant = (tenantId: string, profileId: string) =>
  apiFetch<AssignProfileResponse>(`/vault/tenants/${tenantId}/profiles/${profileId}`, { method: 'POST' });

export const removeProfileFromTenant = (tenantId: string, profileId: string) =>
  apiFetch<void>(`/vault/tenants/${tenantId}/profiles/${profileId}`, { method: 'DELETE' });

export const toggleTenantService = (tenantId: string, serviceId: string) =>
  apiFetch<{ isEnabled: boolean }>(`/vault/tenants/${tenantId}/services/${serviceId}/toggle`, { method: 'PATCH' });

export const getAgentChannels = (tenantId: string) =>
  apiFetch<ChannelsConfig>(`/agents/conversational/${tenantId}/channels`);

export const updateAgentChannels = (tenantId: string, data: Partial<ChannelsConfig>) =>
  apiFetch<ChannelsConfig>(`/agents/conversational/${tenantId}/channels`, {
    method: 'PUT', body: JSON.stringify(data),
  });

export const getAIConfig = (tenantId: string) =>
  apiFetch<AIConfig>(`/agents/conversational/${tenantId}/ai-config`);

export const updateAIConfig = (tenantId: string, data: Partial<AIConfig>) =>
  apiFetch<AIConfig>(`/agents/conversational/${tenantId}/ai-config`, {
    method: 'PUT', body: JSON.stringify(data),
  });

export const getAvailableModels = () =>
  apiFetch<ModelInfo[]>('/agents/conversational/models');

// --- Vault Catalog Management (profiles, phases, services) ---

export interface CatalogProfileResponse {
  id: string; name: string; slug: string;
  description: string; isActive: boolean;
  directServices: CatalogServiceDetail[];
  phases: CatalogPhaseResponse[];
  createdAt: string; updatedAt: string;
}

export interface CatalogPhaseResponse {
  id: string; name: string; description: string;
  sortOrder: number; services: CatalogServiceDetail[];
}

export interface CatalogServiceDetail {
  id: string; name: string; slug: string; description: string;
  type: string; isAddon: boolean;
  cost: number; salePrice: number; sortOrder: number;
}

export interface CreateCatalogProfileRequest {
  name: string; slug: string; description?: string;
}

export interface UpdateCatalogProfileRequest {
  name?: string; slug?: string; description?: string;
}

export interface CreateCatalogPhaseRequest {
  name: string; description?: string; sortOrder?: number;
}

export interface UpdateCatalogPhaseRequest {
  name?: string; description?: string; sortOrder?: number;
}

export interface CreateCatalogServiceRequest {
  name: string; slug: string; description?: string;
  type: string; cost: number; salePrice: number; sortOrder?: number;
}

// --- Vault catalog API functions ---

export const listProfiles = () =>
  apiFetch<CatalogProfileResponse[]>('/vault/profiles');

export const getProfile = (id: string) =>
  apiFetch<CatalogProfileResponse>(`/vault/profiles/${id}`);

export const createProfile = (data: CreateCatalogProfileRequest) =>
  apiFetch<CatalogProfileResponse>('/vault/profiles', { method: 'POST', body: JSON.stringify(data) });

export const updateProfile = (id: string, data: UpdateCatalogProfileRequest) =>
  apiFetch<CatalogProfileResponse>(`/vault/profiles/${id}`, { method: 'PUT', body: JSON.stringify(data) });

export const deleteProfile = (id: string) =>
  apiFetch<void>(`/vault/profiles/${id}`, { method: 'DELETE' });

export const addPhaseToProfile = (profileId: string, data: CreateCatalogPhaseRequest) =>
  apiFetch<CatalogProfileResponse>(`/vault/profiles/${profileId}/phases`, { method: 'POST', body: JSON.stringify(data) });

export const updatePhase = (profileId: string, phaseId: string, data: UpdateCatalogPhaseRequest) =>
  apiFetch<CatalogProfileResponse>(`/vault/profiles/${profileId}/phases/${phaseId}`, { method: 'PUT', body: JSON.stringify(data) });

export const deletePhase = (profileId: string, phaseId: string) =>
  apiFetch<CatalogProfileResponse>(`/vault/profiles/${profileId}/phases/${phaseId}`, { method: 'DELETE' });

export const addServiceToPhase = (phaseId: string, data: CreateCatalogServiceRequest) =>
  apiFetch<CatalogProfileResponse>(`/vault/phases/${phaseId}/services`, { method: 'POST', body: JSON.stringify(data) });

export const addServiceToProfile = (profileId: string, data: CreateCatalogServiceRequest) =>
  apiFetch<CatalogProfileResponse>(`/vault/profiles/${profileId}/services`, { method: 'POST', body: JSON.stringify(data) });

export const updateService = (serviceId: string, data: Partial<CreateCatalogServiceRequest>) =>
  apiFetch<CatalogProfileResponse>(`/vault/services/${serviceId}`, { method: 'PUT', body: JSON.stringify(data) });

export const deleteService = (serviceId: string) =>
  apiFetch<void>(`/vault/services/${serviceId}`, { method: 'DELETE' });

export const createAddonService = (data: CreateCatalogServiceRequest) =>
  apiFetch<CatalogProfileResponse>('/vault/services', { method: 'POST', body: JSON.stringify(data) });

export const bumpCatalogVersion = (serviceId: string) =>
  apiFetch<void>(`/vault/catalog-services/${serviceId}/bump`, { method: 'POST' });

export const acknowledgeOutdated = (tenantId: string, serviceId: string) =>
  apiFetch<void>(`/vault/tenants/${tenantId}/services/${serviceId}/acknowledge`, { method: 'POST' });

export const listOutdatedServices = () =>
  apiFetch<OutdatedServiceResponse[]>('/vault/outdated');

// --- Sector Pricing (NexeLocal model) ---

export const SECTOR_SIZES: Record<string, string[]> = {
  PINTOR: ['AUTONOMO', 'PETIT'],
  ELECTRICISTA: ['AUTONOMO', 'PETIT'],
  FONTANER: ['AUTONOMO'],
  JARDINER: ['AUTONOMO'],
  NETEJA: ['AUTONOMO', 'MITJA'],
  FISIOTERAPEUTA: ['AUTONOMO', 'PETIT', 'MITJA'],
  PSICOLEG: ['AUTONOMO', 'PETIT'],
  NUTRICIONISTA: ['AUTONOMO'],
  PERRUQUERIA: ['AUTONOMO', 'PETIT'],
  ESTETICA: ['AUTONOMO', 'MITJA'],
  GESTORIA: ['AUTONOMO', 'MITJA'],
  ACADEMIA: ['AUTONOMO', 'MITJA'],
  TALLER_MECANIC: ['PETIT', 'MITJA'],
  VETERINARI: ['AUTONOMO', 'PETIT'],
  PERRUQUERIA_CANINA: ['AUTONOMO'],
};

export const SECTORS = [
  'PINTOR', 'ELECTRICISTA', 'FONTANER', 'JARDINER', 'NETEJA',
  'FISIOTERAPEUTA', 'PSICOLEG', 'NUTRICIONISTA',
  'PERRUQUERIA', 'ESTETICA',
  'GESTORIA', 'ACADEMIA',
  'TALLER_MECANIC', 'VETERINARI', 'PERRUQUERIA_CANINA',
] as const;

export const SIZES = ['AUTONOMO', 'PETIT', 'MITJA'] as const;
export const PHASES = ['F1', 'F2', 'F3', 'F4', 'F5'] as const;

export const SECTOR_LABELS: Record<string, string> = {
  PINTOR: 'Pintor', ELECTRICISTA: 'Electricista', FONTANER: 'Fontaner',
  JARDINER: 'Jardiner', NETEJA: 'Neteja', FISIOTERAPEUTA: 'Fisioterapeuta',
  PSICOLEG: 'Psicòleg', NUTRICIONISTA: 'Nutricionista', PERRUQUERIA: 'Perruqueria',
  ESTETICA: 'Estètica', GESTORIA: 'Gestoria', ACADEMIA: 'Acadèmia',
  TALLER_MECANIC: 'Taller mecànic', VETERINARI: 'Veterinari',
  PERRUQUERIA_CANINA: 'Perruqueria canina',
};

export const SIZE_LABELS: Record<string, string> = {
  AUTONOMO: 'Autònom (1 persona)',
  PETIT: 'Equip petit (2-3 persones)',
  MITJA: 'Equip mitjà (3-5+ persones)',
};

export const PHASE_LABELS: Record<string, string> = {
  F1: 'F1 — Comunicació 24/7',
  F2: 'F2 — Gestió de cites',
  F3: 'F3 — Pressupostos',
  F4: 'F4 — Fidelització',
  F5: 'F5 — Equip',
};

export interface SectorPricingResponse {
  sector: string;
  businessSize: string;
  setupPrice: number;
  priceF1: number;
  priceF2: number;
  priceF3: number;
  priceF4: number;
  priceF5: number;
}

// Calcula el mensual sumant els N primers tiers (igual que el backend: count fases → priceF1+priceF2+...)
export function calcMonthly(pricing: SectorPricingResponse, phaseCount: number): number {
  const tiers = [pricing.priceF1, pricing.priceF2, pricing.priceF3, pricing.priceF4, pricing.priceF5];
  return tiers.slice(0, phaseCount).reduce((sum, p) => sum + (p ?? 0), 0);
}

export const listSectorPricing = () =>
  apiFetch<SectorPricingResponse[]>('/pricing');

export const lookupSectorPricing = (sector: string, size: string) =>
  apiFetch<SectorPricingResponse>(`/pricing/lookup?sector=${sector}&size=${size}`);

export const getSectorPromptTemplate = (sector: string) =>
  apiFetch<{ sector: string; template: string }>(`/prompts/sector/${sector}`);

// Preu fix d'ampliació de fase per a clients existents
export const PHASE_UPGRADE_PRICE = 75;

// --- GoCardless (Spec 09b) ---

export interface GoCardlessConfig {
  id: string;
  tenantId: string;
  environment: 'SANDBOX' | 'LIVE';
  creditorId: string | null;
  isActive: boolean;
  createdAt: string;
}

export interface GoCardlessMandate {
  id: string;
  tenantId: string;
  gcMandateId: string | null;
  status: string;
  accountHolderName: string | null;
  bankName: string | null;
  lastFourDigits: string | null;
  createdAt: string;
}

export interface GoCardlessPaymentItem {
  id: string;
  gcPaymentId: string;
  amount: number;
  status: string;
  chargeDate: string | null;
  paidOutAt: string | null;
  failureReason: string | null;
  createdAt: string;
}

export interface InitiateMandateResponse {
  redirectFlowId: string;
  redirectUrl: string;
}

export const getGoCardlessConfig = (tenantId: string) =>
  apiFetch<GoCardlessConfig>(`/gocardless/configure/${tenantId}`);

export const configureGoCardless = (tenantId: string, data: {
  apiKeyRef: string; environment: 'SANDBOX' | 'LIVE'; creditorId?: string; webhookSecret?: string;
}) =>
  apiFetch<GoCardlessConfig>(`/gocardless/configure/${tenantId}`, {
    method: 'POST',
    body: JSON.stringify({ tenantId, ...data }),
  });

export const getGoCardlessMandate = (tenantId: string) =>
  apiFetch<GoCardlessMandate>(`/gocardless/tenants/${tenantId}/mandate`);

export const initiateGoCardlessMandate = (tenantId: string, successReturnUrl?: string) => {
  const params = successReturnUrl ? `?successReturnUrl=${encodeURIComponent(successReturnUrl)}` : '';
  return apiFetch<InitiateMandateResponse>(`/gocardless/tenants/${tenantId}/mandate/initiate${params}`, { method: 'POST' });
};

export const cancelGoCardlessMandate = (tenantId: string) =>
  apiFetch<void>(`/gocardless/tenants/${tenantId}/mandate`, { method: 'DELETE' });

export const listGoCardlessPayments = (tenantId: string) =>
  apiFetch<{ content: GoCardlessPaymentItem[] }>(`/gocardless/tenants/${tenantId}/payments?size=10`);
