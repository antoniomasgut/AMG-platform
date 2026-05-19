'use client';

import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';

type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface ToastAction {
  label: string;
  onClick: () => void;
}

export interface Toast {
  id: string;
  type: ToastType;
  title: string;
  message?: string;
  action?: ToastAction;
}

interface ToastContextType {
  toasts: Toast[];
  toast: (type: ToastType, title: string, messageOrOpts?: string | { action?: ToastAction }) => void;
  dismiss: (id: string) => void;
}

const ToastContext = createContext<ToastContextType | null>(null);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const dismiss = useCallback((id: string) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  }, []);

  const toast = useCallback((type: ToastType, title: string, messageOrOpts?: string | { action?: ToastAction }) => {
    const id = `${Date.now()}-${Math.random().toString(36).slice(2, 6)}`;
    const message = typeof messageOrOpts === 'string' ? messageOrOpts : undefined;
    const action = typeof messageOrOpts === 'object' ? messageOrOpts?.action : undefined;
    setToasts(prev => [...prev, { id, type, title, message, action }]);
    setTimeout(() => dismiss(id), action ? 8000 : 4500);
  }, [dismiss]);

  return (
    <ToastContext.Provider value={{ toasts, toast, dismiss }}>
      {children}
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}
