'use client';

import { useEffect, useRef } from 'react';
import { useToast } from '@/lib/toast-context';
import { useAuth } from '@/lib/auth-context';

interface ApiError extends Error {
  status?: number;
  body?: unknown;
}

function isApiError(err: unknown): err is ApiError {
  return err instanceof Error && 'status' in err;
}

/**
 * Hook that intercepts API errors and shows global toasts.
 * Usage: wrap async API calls with handleApiError()
 */
export function useApiErrorHandler() {
  const { toast } = useToast();
  const { logout } = useAuth();

  const handleRef = useRef(async (err: unknown, context?: string) => {
    if (!isApiError(err)) {
      toast('error', 'Error inesperat', err instanceof Error ? err.message : undefined);
      return;
    }

    switch (err.status) {
      case 401:
        toast('error', 'Sessió expirada', 'Torna a iniciar sessió');
        await logout();
        if (typeof window !== 'undefined') window.location.href = '/login';
        break;
      case 403:
        toast('error', 'Accés denegat', 'No tens permís per a aquesta operació');
        break;
      case 404:
        toast('warning', 'No trobat', context ? `${context} no trobat` : undefined);
        break;
      case 409:
        toast('warning', 'Conflicte', err.message || 'La operació no es pot completar');
        break;
      case 422:
      case 400:
        toast('warning', 'Dades invàlides', err.message);
        break;
      case 429:
        toast('warning', 'Massa peticions', 'Espera uns segons i torna-ho a provar');
        break;
      default:
        toast('error', 'Error del servidor', err.message || 'Torna-ho a provar més tard');
    }
  });

  return handleRef.current;
}
