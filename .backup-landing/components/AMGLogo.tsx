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
        <radialGradient id="glowBgH" cx="38" cy="49" r="45" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#FF6B00" stopOpacity="0.10"/>
          <stop offset="60%" stopColor="#FF6B00" stopOpacity="0.04"/>
          <stop offset="100%" stopColor="#FF6B00" stopOpacity="0"/>
        </radialGradient>
        <filter id="glH" x="-30%" y="-30%" width="160%" height="160%">
          <feGaussianBlur in="SourceGraphic" stdDeviation="2" result="b"/>
          <feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge>
        </filter>
        <filter id="glHsig" x="-80%" y="-80%" width="260%" height="260%">
          <feGaussianBlur in="SourceGraphic" stdDeviation="3" result="b"/>
          <feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge>
        </filter>
        <filter id="glHCenter" x="-60%" y="-60%" width="220%" height="220%">
          <feGaussianBlur in="SourceGraphic" stdDeviation="4" result="b"/>
          <feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge>
        </filter>
      </defs>

      {/* Frame */}
      <polygon points="68,0 76,8 76,84 8,84 0,76 0,0"
               fill="#13132a" stroke="#FF6B00" strokeWidth="0.75" strokeOpacity="0.22"/>

      {/* Background glow */}
      <circle cx="38" cy="47" r="36" fill="url(#glowBgH)"/>

      {/* Triangle — perfectly equilateral, side=56, centroid at (38,47) */}
      <polygon points="38,14.7 10,63.2 66,63.2" fill="url(#glowBgH)"/>

      {/* Static edges */}
      <line x1="38" y1="14.7" x2="10" y2="63.2" stroke="#FF6B00" strokeWidth="2" strokeOpacity="0.2" strokeLinecap="square"/>
      <line x1="38" y1="14.7" x2="66" y2="63.2" stroke="#FF6B00" strokeWidth="2" strokeOpacity="0.2" strokeLinecap="square"/>
      <line x1="10" y1="63.2" x2="66" y2="63.2" stroke="#FF6B00" strokeWidth="2" strokeOpacity="0.2" strokeLinecap="square"/>

      {/* Animated signals */}
      <line x1="38" y1="14.7" x2="10" y2="63.2"
            stroke="#FF9A3C" strokeWidth="3" strokeLinecap="round"
            strokeDasharray="6 400" filter="url(#glHsig)">
        <animate attributeName="stroke-dashoffset" from="0" to="-406" dur="2s" repeatCount="indefinite" begin="0s"/>
      </line>
      <line x1="38" y1="14.7" x2="66" y2="63.2"
            stroke="#FF9A3C" strokeWidth="3" strokeLinecap="round"
            strokeDasharray="6 400" filter="url(#glHsig)">
        <animate attributeName="stroke-dashoffset" from="0" to="-406" dur="2s" repeatCount="indefinite" begin="0.2s"/>
      </line>
      <line x1="10" y1="63.2" x2="66" y2="63.2"
            stroke="#FF9A3C" strokeWidth="3" strokeLinecap="round"
            strokeDasharray="6 400" filter="url(#glHsig)">
        <animate attributeName="stroke-dashoffset" from="0" to="-406" dur="2s" repeatCount="indefinite" begin="0.4s"/>
      </line>

      {/* Vertex nodes */}
      <circle cx="38" cy="14.7" r="5" fill="#13132a" stroke="#FF6B00" strokeWidth="2.5" filter="url(#glH)">
        <animate attributeName="stroke-opacity" values="1;0.3;1" dur="2s" repeatCount="indefinite" begin="0s"/>
      </circle>
      <circle cx="10" cy="63.2" r="5" fill="#13132a" stroke="#FF6B00" strokeWidth="2.5">
        <animate attributeName="stroke-opacity" values="0.3;1;0.3" dur="2s" repeatCount="indefinite" begin="0.72s"/>
      </circle>
      <circle cx="66" cy="63.2" r="5" fill="#13132a" stroke="#FF6B00" strokeWidth="2.5">
        <animate attributeName="stroke-opacity" values="0.3;1;0.3" dur="2s" repeatCount="indefinite" begin="1.44s"/>
      </circle>

      {/* Center node — core pulsing */}
      <circle cx="38" cy="47" r="6" fill="#13132a" stroke="#FF9A3C" strokeWidth="2.5" filter="url(#glHCenter)">
        <animate attributeName="r" values="5;7;5" dur="3s" repeatCount="indefinite" begin="0s"/>
        <animate attributeName="stroke-opacity" values="0.3;1;0.3" dur="3s" repeatCount="indefinite" begin="0s"/>
      </circle>
      {/* Ripple wave 1 */}
      <circle cx="38" cy="47" r="6" fill="none" stroke="#FF9A3C" strokeWidth="1.5" strokeOpacity="0.5">
        <animate attributeName="r" values="6;18;6" dur="3s" repeatCount="indefinite" begin="0s"/>
        <animate attributeName="stroke-opacity" values="0.5;0;0.5" dur="3s" repeatCount="indefinite" begin="0s"/>
      </circle>
      {/* Ripple wave 2 (offset) */}
      <circle cx="38" cy="47" r="6" fill="none" stroke="#FF9A3C" strokeWidth="1" strokeOpacity="0.3">
        <animate attributeName="r" values="6;22;6" dur="3s" repeatCount="indefinite" begin="1s"/>
        <animate attributeName="stroke-opacity" values="0.3;0;0.3" dur="3s" repeatCount="indefinite" begin="1s"/>
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
        <radialGradient id="glowBg" cx="100" cy="100" r="95" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#FF6B00" stopOpacity="0.10"/>
          <stop offset="60%" stopColor="#FF6B00" stopOpacity="0.04"/>
          <stop offset="100%" stopColor="#FF6B00" stopOpacity="0"/>
        </radialGradient>
        <filter id="glIcon" x="-30%" y="-30%" width="160%" height="160%">
          <feGaussianBlur in="SourceGraphic" stdDeviation="2.5" result="blur"/>
          <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
        </filter>
        <filter id="glIconSig" x="-60%" y="-60%" width="220%" height="220%">
          <feGaussianBlur in="SourceGraphic" stdDeviation="4" result="blur"/>
          <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
        </filter>
        <filter id="glIconCenter" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur in="SourceGraphic" stdDeviation="6" result="blur"/>
          <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
        </filter>
      </defs>

      {/* Frame */}
      <polygon points="184,4 196,16 196,196 16,196 4,184 4,4"
               fill="#13132a" stroke="#FF6B00" strokeWidth="0.75" strokeOpacity="0.20"/>

      {/* Background glow */}
      <circle cx="100" cy="100" r="92" fill="url(#glowBg)"/>

      {/* Triangle — perfectly equilateral, side=150, centroid at (100,100) */}
      <polygon points="100,13.4 25,143.3 175,143.3" fill="url(#glowBg)"/>

      {/* Static edges */}
      <line x1="100" y1="13.4" x2="25" y2="143.3" stroke="#FF6B00" strokeWidth="3.5" strokeOpacity="0.2" strokeLinecap="square"/>
      <line x1="100" y1="13.4" x2="175" y2="143.3" stroke="#FF6B00" strokeWidth="3.5" strokeOpacity="0.2" strokeLinecap="square"/>
      <line x1="25" y1="143.3" x2="175" y2="143.3" stroke="#FF6B00" strokeWidth="3.5" strokeOpacity="0.2" strokeLinecap="square"/>

      {/* Animated signals */}
      <line x1="100" y1="13.4" x2="25" y2="143.3"
            stroke="#FF9A3C" strokeWidth="5" strokeLinecap="round"
            strokeDasharray="10 400" filter="url(#glIconSig)">
        <animate attributeName="stroke-dashoffset" from="0" to="-410" dur="2s" repeatCount="indefinite" begin="0s"/>
      </line>
      <line x1="100" y1="13.4" x2="175" y2="143.3"
            stroke="#FF9A3C" strokeWidth="5" strokeLinecap="round"
            strokeDasharray="10 400" filter="url(#glIconSig)">
        <animate attributeName="stroke-dashoffset" from="0" to="-410" dur="2s" repeatCount="indefinite" begin="0.2s"/>
      </line>
      <line x1="25" y1="143.3" x2="175" y2="143.3"
            stroke="#FF9A3C" strokeWidth="5" strokeLinecap="round"
            strokeDasharray="10 400" filter="url(#glIconSig)">
        <animate attributeName="stroke-dashoffset" from="0" to="-410" dur="2s" repeatCount="indefinite" begin="0.4s"/>
      </line>

      {/* Vertex nodes */}
      <circle cx="100" cy="13.4" r="8" fill="#13132a" stroke="#FF6B00" strokeWidth="4" filter="url(#glIcon)">
        <animate attributeName="stroke-opacity" values="1;0.3;1" dur="2s" repeatCount="indefinite" begin="0s"/>
      </circle>
      <circle cx="25" cy="143.3" r="8" fill="#13132a" stroke="#FF6B00" strokeWidth="4">
        <animate attributeName="stroke-opacity" values="0.3;1;0.3" dur="2s" repeatCount="indefinite" begin="0.72s"/>
      </circle>
      <circle cx="175" cy="143.3" r="8" fill="#13132a" stroke="#FF6B00" strokeWidth="4">
        <animate attributeName="stroke-opacity" values="0.3;1;0.3" dur="2s" repeatCount="indefinite" begin="1.44s"/>
      </circle>

      {/* Center node — core pulsing */}
      <circle cx="100" cy="100" r="10" fill="#13132a" stroke="#FF9A3C" strokeWidth="4" filter="url(#glIconCenter)">
        <animate attributeName="r" values="8;12;8" dur="3s" repeatCount="indefinite" begin="0s"/>
        <animate attributeName="stroke-opacity" values="0.3;1;0.3" dur="3s" repeatCount="indefinite" begin="0s"/>
      </circle>
      {/* Ripple wave 1 */}
      <circle cx="100" cy="100" r="10" fill="none" stroke="#FF9A3C" strokeWidth="2" strokeOpacity="0.5">
        <animate attributeName="r" values="10;35;10" dur="3s" repeatCount="indefinite" begin="0s"/>
        <animate attributeName="stroke-opacity" values="0.5;0;0.5" dur="3s" repeatCount="indefinite" begin="0s"/>
      </circle>
      {/* Ripple wave 2 (offset) */}
      <circle cx="100" cy="100" r="10" fill="none" stroke="#FF9A3C" strokeWidth="1.5" strokeOpacity="0.3">
        <animate attributeName="r" values="10;45;10" dur="3s" repeatCount="indefinite" begin="1s"/>
        <animate attributeName="stroke-opacity" values="0.3;0;0.3" dur="3s" repeatCount="indefinite" begin="1s"/>
      </circle>
    </svg>
  );
}
