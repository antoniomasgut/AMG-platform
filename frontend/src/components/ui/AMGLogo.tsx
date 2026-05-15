import type { SVGProps } from 'react';

/** Horizontal animated AMG logo. Pass className to control size (e.g. className="h-10 w-auto"). */
export function AMGLogo({ className, ...props }: SVGProps<SVGSVGElement>) {
  return (
    <svg
      width="340" height="84" viewBox="0 0 340 84"
      xmlns="http://www.w3.org/2000/svg"
      aria-label="AMG Digitalitzacions"
      className={className}
      {...props}
    >
      <defs>
        <linearGradient id="triH" x1="38" y1="8" x2="38" y2="76" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#FF6B00" stopOpacity="0.14"/>
          <stop offset="100%" stopColor="#FF6B00" stopOpacity="0.02"/>
        </linearGradient>
        <filter id="glH" x="-30%" y="-30%" width="160%" height="160%">
          <feGaussianBlur in="SourceGraphic" stdDeviation="2" result="b"/>
          <feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge>
        </filter>
        <filter id="glHsig" x="-80%" y="-80%" width="260%" height="260%">
          <feGaussianBlur in="SourceGraphic" stdDeviation="3.5" result="b"/>
          <feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge>
        </filter>
      </defs>

      {/* Icon frame */}
      <polygon points="68,0 76,8 76,84 8,84 0,76 0,0"
               fill="#13132a" stroke="#FF6B00" strokeWidth="0.75" strokeOpacity="0.22"/>

      {/* Triangle fill */}
      <polygon points="38,8 8,76 68,76" fill="url(#triH)"/>

      {/* Static wires */}
      <line x1="38" y1="8"  x2="8"  y2="76" stroke="#FF6B00" strokeWidth="2.5" strokeOpacity="0.22" strokeLinecap="square"/>
      <line x1="38" y1="8"  x2="68" y2="76" stroke="#FF6B00" strokeWidth="2.5" strokeOpacity="0.22" strokeLinecap="square"/>
      <line x1="18" y1="49" x2="58" y2="49" stroke="#FF6B00" strokeWidth="2.5" strokeOpacity="0.22" strokeLinecap="square"/>

      {/* Animated signals */}
      <line x1="38" y1="8" x2="8" y2="76"
            stroke="#FF9A3C" strokeWidth="3" strokeLinecap="round"
            strokeDasharray="7 400" filter="url(#glHsig)">
        <animate attributeName="stroke-dashoffset" from="0" to="-407" dur="2s" repeatCount="indefinite" begin="0s"/>
      </line>
      <line x1="38" y1="8" x2="68" y2="76"
            stroke="#FF9A3C" strokeWidth="3" strokeLinecap="round"
            strokeDasharray="7 400" filter="url(#glHsig)">
        <animate attributeName="stroke-dashoffset" from="0" to="-407" dur="2s" repeatCount="indefinite" begin="0.18s"/>
      </line>
      <line x1="18" y1="49" x2="58" y2="49"
            stroke="#FF9A3C" strokeWidth="3" strokeLinecap="round"
            strokeDasharray="7 400" filter="url(#glHsig)">
        <animate attributeName="stroke-dashoffset" from="0" to="-407" dur="2s" repeatCount="indefinite" begin="0.45s"/>
      </line>

      {/* Nodes */}
      <circle cx="38" cy="8"  r="4.5" fill="#13132a" stroke="#FF6B00" strokeWidth="2.5" filter="url(#glH)">
        <animate attributeName="stroke-opacity" values="1;0.35;1" dur="2s" repeatCount="indefinite" begin="0s"/>
      </circle>
      <circle cx="8"  cy="76" r="4.5" fill="#13132a" stroke="#FF6B00" strokeWidth="2.5">
        <animate attributeName="stroke-opacity" values="0.35;1;0.35" dur="2s" repeatCount="indefinite" begin="0.72s"/>
      </circle>
      <circle cx="68" cy="76" r="4.5" fill="#13132a" stroke="#FF6B00" strokeWidth="2.5">
        <animate attributeName="stroke-opacity" values="0.35;1;0.35" dur="2s" repeatCount="indefinite" begin="0.90s"/>
      </circle>
      <circle cx="18" cy="49" r="4.5" fill="#13132a" stroke="#FF6B00" strokeWidth="2.5">
        <animate attributeName="stroke-opacity" values="0.35;1;0.35" dur="2s" repeatCount="indefinite" begin="0.45s"/>
      </circle>
      <circle cx="58" cy="49" r="4.5" fill="#13132a" stroke="#FF6B00" strokeWidth="2.5">
        <animate attributeName="stroke-opacity" values="0.35;1;0.35" dur="2s" repeatCount="indefinite" begin="0.84s"/>
      </circle>

      {/* Divider */}
      <line x1="94" y1="12" x2="94" y2="72" stroke="#FF6B00" strokeWidth="0.75" strokeOpacity="0.22"/>

      {/* Wordmark */}
      <text x="108" y="51"
            fontFamily="Orbitron, 'Share Tech Mono', monospace"
            fontSize="36" fontWeight="700" fill="#FF6B00" letterSpacing="5">AMG</text>
      <text x="110" y="70"
            fontFamily="'Space Grotesk', system-ui, sans-serif"
            fontSize="11" fontWeight="400" fill="#8896aa" letterSpacing="5.5">DIGITALITZACIONS</text>
    </svg>
  );
}

/** Square icon-only variant. Pass className to control size. */
export function AMGIcon({ className, ...props }: SVGProps<SVGSVGElement>) {
  return (
    <svg
      width="200" height="200" viewBox="0 0 200 200"
      xmlns="http://www.w3.org/2000/svg"
      aria-label="AMG"
      className={className}
      {...props}
    >
      <defs>
        <linearGradient id="triGrad" x1="100" y1="35" x2="100" y2="170" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#FF6B00" stopOpacity="0.14"/>
          <stop offset="100%" stopColor="#FF6B00" stopOpacity="0.02"/>
        </linearGradient>
        <filter id="glIcon" x="-30%" y="-30%" width="160%" height="160%">
          <feGaussianBlur in="SourceGraphic" stdDeviation="2.5" result="blur"/>
          <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
        </filter>
        <filter id="glIconSig" x="-60%" y="-60%" width="220%" height="220%">
          <feGaussianBlur in="SourceGraphic" stdDeviation="4" result="blur"/>
          <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
        </filter>
      </defs>

      {/* Card-clip frame */}
      <polygon points="184,4 196,16 196,196 16,196 4,184 4,4"
               fill="#13132a" stroke="#FF6B00" strokeWidth="0.75" strokeOpacity="0.20"/>

      {/* Triangle fill */}
      <polygon points="100,35 22,170 178,170" fill="url(#triGrad)"/>

      {/* Static wires */}
      <line x1="100" y1="35" x2="22"  y2="170" stroke="#FF6B00" strokeWidth="4.5" strokeOpacity="0.22" strokeLinecap="square"/>
      <line x1="100" y1="35" x2="178" y2="170" stroke="#FF6B00" strokeWidth="4.5" strokeOpacity="0.22" strokeLinecap="square"/>
      <line x1="48"  y1="125" x2="152" y2="125" stroke="#FF6B00" strokeWidth="4.5" strokeOpacity="0.22" strokeLinecap="square"/>

      {/* Animated signals */}
      <line x1="100" y1="35" x2="22" y2="170"
            stroke="#FF9A3C" strokeWidth="5" strokeLinecap="round"
            strokeDasharray="10 400" filter="url(#glIconSig)">
        <animate attributeName="stroke-dashoffset" from="0" to="-410" dur="2s" repeatCount="indefinite" begin="0s"/>
      </line>
      <line x1="100" y1="35" x2="178" y2="170"
            stroke="#FF9A3C" strokeWidth="5" strokeLinecap="round"
            strokeDasharray="10 400" filter="url(#glIconSig)">
        <animate attributeName="stroke-dashoffset" from="0" to="-410" dur="2s" repeatCount="indefinite" begin="0.18s"/>
      </line>
      <line x1="48" y1="125" x2="152" y2="125"
            stroke="#FF9A3C" strokeWidth="5" strokeLinecap="round"
            strokeDasharray="10 400" filter="url(#glIconSig)">
        <animate attributeName="stroke-dashoffset" from="0" to="-410" dur="2s" repeatCount="indefinite" begin="0.66s"/>
      </line>

      {/* Nodes */}
      <circle cx="100" cy="35" r="7" fill="#13132a" stroke="#FF6B00" strokeWidth="4.5" filter="url(#glIcon)">
        <animate attributeName="stroke-opacity" values="1;0.4;1" dur="2s" repeatCount="indefinite" begin="0s"/>
      </circle>
      <circle cx="22" cy="170" r="7" fill="#13132a" stroke="#FF6B00" strokeWidth="4.5">
        <animate attributeName="stroke-opacity" values="0.4;1;0.4" dur="2s" repeatCount="indefinite" begin="1s"/>
      </circle>
      <circle cx="178" cy="170" r="7" fill="#13132a" stroke="#FF6B00" strokeWidth="4.5">
        <animate attributeName="stroke-opacity" values="0.4;1;0.4" dur="2s" repeatCount="indefinite" begin="1.1s"/>
      </circle>
      <circle cx="48" cy="125" r="7" fill="#13132a" stroke="#FF6B00" strokeWidth="4.5">
        <animate attributeName="stroke-opacity" values="0.4;1;0.4" dur="2s" repeatCount="indefinite" begin="0.66s"/>
      </circle>
      <circle cx="152" cy="125" r="7" fill="#13132a" stroke="#FF6B00" strokeWidth="4.5">
        <animate attributeName="stroke-opacity" values="0.4;1;0.4" dur="2s" repeatCount="indefinite" begin="1.32s"/>
      </circle>
    </svg>
  );
}
