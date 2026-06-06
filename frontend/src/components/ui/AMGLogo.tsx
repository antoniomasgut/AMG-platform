import type { SVGProps } from 'react';

/** Clean, professional AMG logo. No network/cyber elements. */
export function AMGLogo({ className, ...props }: SVGProps<SVGSVGElement>) {
  return (
    <svg
      width="260" height="60" viewBox="0 0 260 60"
      xmlns="http://www.w3.org/2000/svg"
      aria-label="AMG Digitalitzacions"
      className={className}
      {...props}
    >
      <text x="0" y="36"
            fontFamily="Orbitron, 'Share Tech Mono', monospace"
            fontSize="32" fontWeight="700" fill="#FF6B00" letterSpacing="4">
        AMG
      </text>
      <text x="0" y="54"
            fontFamily="'Space Grotesk', system-ui, sans-serif"
            fontSize="10" fontWeight="400" fill="#94a3b8" letterSpacing="4.5">
        DIGITALITZACIONS
      </text>
      <line x1="0" y1="42" x2="84" y2="42" stroke="#FF6B00" strokeWidth="1.5" strokeOpacity="0.3" />
      <circle cx="86" cy="7" r="3" fill="#FF6B00" opacity="0.15" />
    </svg>
  );
}

/** Square icon-only variant. */
export function AMGIcon({ className, ...props }: SVGProps<SVGSVGElement>) {
  return (
    <svg
      width="48" height="48" viewBox="0 0 48 48"
      xmlns="http://www.w3.org/2000/svg"
      aria-label="AMG"
      className={className}
      {...props}
    >
      <rect x="2" y="2" width="44" height="44" rx="0"
            fill="#0f172a" stroke="#FF6B00" strokeWidth="1.5" strokeOpacity="0.25" />
      <text x="24" y="30"
            fontFamily="Orbitron, 'Share Tech Mono', monospace"
            fontSize="18" fontWeight="700" fill="#FF6B00" textAnchor="middle" letterSpacing="2">
        AM
      </text>
      <circle cx="42" cy="6" r="2" fill="#FF6B00" opacity="0.2" />
    </svg>
  );
}
