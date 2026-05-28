import React from 'react';

type IconProps = { size?: number; stroke?: string; className?: string };

const Icon = ({ d, size = 16, stroke = 'currentColor', children, className = '' }: {
  d?: string; size?: number; stroke?: string; children?: React.ReactNode; className?: string;
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={stroke}
       strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" className={className}>
    {children || (d && <path d={d} />)}
  </svg>
);

export const I = {
  Dashboard: (p: IconProps) => <Icon {...p}><rect x="3" y="3" width="7" height="9"/><rect x="14" y="3" width="7" height="5"/><rect x="14" y="12" width="7" height="9"/><rect x="3" y="16" width="7" height="5"/></Icon>,
  Users: (p: IconProps) => <Icon {...p}><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></Icon>,
  Box: (p: IconProps) => <Icon {...p}><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><path d="M3.27 6.96 12 12.01l8.73-5.05"/><path d="M12 22.08V12"/></Icon>,
  Receipt: (p: IconProps) => <Icon {...p}><path d="M4 2v20l2-1.5L8 22l2-1.5L12 22l2-1.5L16 22l2-1.5L20 22V2l-2 1.5L16 2l-2 1.5L12 2l-2 1.5L8 2 6 3.5 4 2z"/><path d="M8 7h8M8 11h8M8 15h5"/></Icon>,
  Settings: (p: IconProps) => <Icon {...p}><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09a1.65 1.65 0 0 0 1.51-1 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33h.01a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51h.01a1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82v.01a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></Icon>,
  Plus: (p: IconProps) => <Icon {...p}><path d="M12 5v14M5 12h14"/></Icon>,
  Search: (p: IconProps) => <Icon {...p}><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></Icon>,
  Bell: (p: IconProps) => <Icon {...p}><path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/></Icon>,
  Chevron: (p: IconProps) => <Icon {...p}><path d="m9 18 6-6-6-6"/></Icon>,
  ChevDown: (p: IconProps) => <Icon {...p}><path d="m6 9 6 6 6-6"/></Icon>,
  Menu: (p: IconProps) => <Icon {...p}><path d="M3 12h18M3 6h18M3 18h18"/></Icon>,
  X: (p: IconProps) => <Icon {...p}><path d="M18 6 6 18M6 6l12 12"/></Icon>,
  Check: (p: IconProps) => <Icon {...p}><path d="M20 6 9 17l-5-5"/></Icon>,
  Download: (p: IconProps) => <Icon {...p}><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><path d="M7 10l5 5 5-5"/><path d="M12 15V3"/></Icon>,
  Edit: (p: IconProps) => <Icon {...p}><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4Z"/></Icon>,
  Trash: (p: IconProps) => <Icon {...p}><path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></Icon>,
  More: (p: IconProps) => <Icon {...p}><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/><circle cx="5" cy="12" r="1"/></Icon>,
  Bot: (p: IconProps) => <Icon {...p}><rect x="3" y="8" width="18" height="12" rx="2"/><path d="M12 8V4M8 4h8"/><circle cx="9" cy="14" r="1"/><circle cx="15" cy="14" r="1"/></Icon>,
  Zap: (p: IconProps) => <Icon {...p}><path d="M13 2 3 14h9l-1 8 10-12h-9l1-8z"/></Icon>,
  Globe: (p: IconProps) => <Icon {...p}><circle cx="12" cy="12" r="10"/><path d="M2 12h20M12 2a15 15 0 0 1 0 20M12 2a15 15 0 0 0 0 20"/></Icon>,
  Mail: (p: IconProps) => <Icon {...p}><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-10 6L2 7"/></Icon>,
  Lock: (p: IconProps) => <Icon {...p}><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></Icon>,
  Building: (p: IconProps) => <Icon {...p}><rect x="4" y="2" width="16" height="20" rx="1"/><path d="M9 22v-4h6v4M8 6h.01M12 6h.01M16 6h.01M8 10h.01M12 10h.01M16 10h.01M8 14h.01M12 14h.01M16 14h.01"/></Icon>,
  CreditCard: (p: IconProps) => <Icon {...p}><rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20M6 15h4"/></Icon>,
  ArrowUp: (p: IconProps) => <Icon {...p}><path d="m18 15-6-6-6 6"/></Icon>,
  ArrowRight: (p: IconProps) => <Icon {...p}><path d="M5 12h14M13 5l7 7-7 7"/></Icon>,
  Terminal: (p: IconProps) => <Icon {...p}><path d="m4 17 6-6-6-6M12 19h8"/></Icon>,
  Layers: (p: IconProps) => <Icon {...p}><path d="m12 2 10 6-10 6L2 8l10-6z"/><path d="m2 17 10 6 10-6M2 12l10 6 10-6"/></Icon>,
  Sparkles: (p: IconProps) => <Icon {...p}><path d="m12 3-1.5 4.5L6 9l4.5 1.5L12 15l1.5-4.5L18 9l-4.5-1.5L12 3zM19 14l-.75 2.25L16 17l2.25.75L19 20l.75-2.25L22 17l-2.25-.75L19 14zM5 16l-.5 1.5L3 18l1.5.5L5 20l.5-1.5L7 18l-1.5-.5L5 16z"/></Icon>,
  Calendar: (p: IconProps) => <Icon {...p}><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></Icon>,
  Filter: (p: IconProps) => <Icon {...p}><path d="M22 3H2l8 9.46V19l4 2v-8.54L22 3z"/></Icon>,
  Upload: (p: IconProps) => <Icon {...p}><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M17 8l-5-5-5 5M12 3v12"/></Icon>,
  Link: (p: IconProps) => <Icon {...p}><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></Icon>,
  Image: (p: IconProps) => <Icon {...p}><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.09-3.09a2 2 0 0 0-2.82 0L6 21"/></Icon>,
  Shield: (p: IconProps) => <Icon {...p}><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></Icon>,
  Smartphone: (p: IconProps) => <Icon {...p}><rect x="5" y="2" width="14" height="20" rx="2"/><path d="M12 18h.01"/></Icon>,
  Trending: (p: IconProps) => <Icon {...p}><path d="m23 6-9.5 9.5-5-5L1 18"/><path d="M17 6h6v6"/></Icon>,
  Activity: (p: IconProps) => <Icon {...p}><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></Icon>,
  Clock: (p: IconProps) => <Icon {...p}><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></Icon>,
  Play: (p: IconProps) => <Icon {...p}><path d="M5 3v18l15-9z"/></Icon>,
  Eye: (p: IconProps) => <Icon {...p}><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="3"/></Icon>,
  EyeOff: (p: IconProps) => <Icon {...p}><path d="M17.94 17.94A10.07 10.07 0 0 1 12 19c-7 0-10-7-10-7a18.37 18.37 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 3c7 0 10 7 10 7a18.5 18.5 0 0 1-2.16 3.19M14.12 14.12a3 3 0 1 1-4.24-4.24M1 1l22 22"/></Icon>,
  AlertCircle: (p: IconProps) => <Icon {...p}><circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/></Icon>,
  Database: (p: IconProps) => <Icon {...p}><ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"/><path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"/></Icon>,
  Server: (p: IconProps) => <Icon {...p}><rect x="2" y="2" width="20" height="8" rx="2"/><rect x="2" y="14" width="20" height="8" rx="2"/><path d="M6 6h.01M6 18h.01"/></Icon>,
  Key: (p: IconProps) => <Icon {...p}><circle cx="7.5" cy="15.5" r="5.5"/><path d="M21 2l-9.6 9.6M15.5 7.5l2 2L21 6l-2-2"/></Icon>,
  Flow: (p: IconProps) => <Icon {...p}><rect x="3" y="3" width="5" height="5" rx="1"/><rect x="16" y="3" width="5" height="5" rx="1"/><rect x="16" y="16" width="5" height="5" rx="1"/><path d="M8 5.5h4a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1H8"/><path d="M18.5 8v4a1 1 0 0 1-1 1h-1"/><path d="M18.5 16V14"/></Icon>,
  Copy: (p: IconProps) => <Icon {...p}><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></Icon>,
  Heart: (p: IconProps) => <Icon {...p}><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></Icon>,
  AlertTriangle: (p: IconProps) => <Icon {...p}><path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><path d="M12 9v4M12 17h.01"/></Icon>,
  ChevronLeft: (p: IconProps) => <Icon {...p}><path d="m15 18-6-6 6-6"/></Icon>,
};
