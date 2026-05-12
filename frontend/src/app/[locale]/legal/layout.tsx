import { ReactNode } from 'react';

export default function LegalLayout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-dvh bg-[#0d0d1a]">
      <div className="fixed inset-0 amg-grid-sm pointer-events-none" />
      <div className="relative z-10 max-w-3xl mx-auto px-4 py-16 sm:py-24">
        {children}
      </div>
    </div>
  );
}
