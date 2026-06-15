import { useAuth } from './auth-context';
import { useQuery } from '@tanstack/react-query';
import { getTenant } from '@/services/admin';

export interface TenantFeatures {
  hasF1: boolean;
  hasF2: boolean;
  hasF3: boolean;
  hasF4: boolean;
  hasF5: boolean;
  canAccessLeads: boolean;        // F1
  canAccessDocuments: boolean;    // F1 or F3
  canAccessAnalytics: boolean;    // F4
  canAccessVisits: boolean;       // F4
  canAccessReports: boolean;      // F5
  canAccessNotifications: boolean; // F5
  isLoading: boolean;
  contractedPhases: string[];
}

export function useTenantFeatures(overrideTenantId?: string): TenantFeatures {
  const { user, isAdmin, isSuperAdmin } = useAuth();
  const isStaff = isAdmin || isSuperAdmin;
  const tenantId = overrideTenantId ?? user?.tenantId ?? null;

  const { data: tenant, isLoading } = useQuery({
    queryKey: ['tenant', tenantId],
    queryFn: () => getTenant(tenantId!),
    enabled: !!tenantId && !isStaff,
    staleTime: 5 * 60 * 1000,
  });

  if (isStaff) {
    return {
      hasF1: true, hasF2: true, hasF3: true, hasF4: true, hasF5: true,
      canAccessLeads: true, canAccessDocuments: true, canAccessAnalytics: true,
      canAccessVisits: true, canAccessReports: true, canAccessNotifications: true,
      isLoading: false, contractedPhases: ['F1', 'F2', 'F3', 'F4', 'F5'],
    };
  }

  if (!tenantId) {
    return {
      hasF1: false, hasF2: false, hasF3: false, hasF4: false, hasF5: false,
      canAccessLeads: false, canAccessDocuments: false, canAccessAnalytics: false,
      canAccessVisits: false, canAccessReports: false, canAccessNotifications: false,
      isLoading: false, contractedPhases: [],
    };
  }

  // activePhases sobreescriu contractedPhases per al control operacional
  const phases = (tenant?.activePhases ?? tenant?.contractedPhases) ?? [];
  const hasF1 = phases.includes('F1');
  const hasF2 = phases.includes('F2');
  const hasF3 = phases.includes('F3');
  const hasF4 = phases.includes('F4');
  const hasF5 = phases.includes('F5');

  return {
    hasF1, hasF2, hasF3, hasF4, hasF5,
    canAccessLeads: hasF3 || hasF4,
    canAccessDocuments: hasF1 || hasF3,
    canAccessAnalytics: hasF4,
    canAccessVisits: hasF4,
    canAccessReports: hasF5,
    canAccessNotifications: hasF5,
    isLoading: isLoading && !tenant,
    contractedPhases: phases,
  };
}
