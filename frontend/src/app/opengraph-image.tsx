import { ImageResponse } from 'next/og';

export const runtime = 'edge';
export const alt = 'AMG Digitalitzacions · Agent IA per a negocis locals';
export const size = { width: 1200, height: 630 };
export const contentType = 'image/png';

export default function OgImage() {
  return new ImageResponse(
    (
      <div
        style={{
          width: '100%',
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          background: '#0d0d1a',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        {/* Grid background */}
        <div
          style={{
            position: 'absolute',
            inset: 0,
            backgroundImage:
              'linear-gradient(rgba(255,107,0,0.04) 1px, transparent 1px), linear-gradient(90deg, rgba(255,107,0,0.04) 1px, transparent 1px)',
            backgroundSize: '40px 40px',
          }}
        />

        {/* Radial glow */}
        <div
          style={{
            position: 'absolute',
            inset: 0,
            background:
              'radial-gradient(ellipse at 20% 50%, rgba(255,107,0,0.18) 0%, transparent 50%), radial-gradient(ellipse at 80% 20%, rgba(255,154,60,0.10) 0%, transparent 40%)',
          }}
        />

        {/* Orange accent bar top */}
        <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: 4, background: '#FF6B00' }} />

        {/* Content */}
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            padding: '64px 80px',
            height: '100%',
            position: 'relative',
          }}
        >
          {/* Logo + brand */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 20 }}>
            {/* Hexagon-ish clip logo */}
            <div
              style={{
                width: 56,
                height: 56,
                background: '#FF6B00',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                clipPath: 'polygon(calc(100% - 12px) 0%, 100% 12px, 100% 100%, 12px 100%, 0% calc(100% - 12px), 0% 0%)',
              }}
            >
              <span style={{ color: '#0d0d1a', fontWeight: 900, fontSize: 28, fontFamily: 'serif' }}>A</span>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <span style={{ color: '#e2e8f0', fontWeight: 800, fontSize: 20, letterSpacing: '0.12em' }}>AMG</span>
              <span style={{ color: '#FF9A3C', fontSize: 11, letterSpacing: '0.25em', fontFamily: 'monospace' }}>
                DIGITALITZACIONS
              </span>
            </div>
          </div>

          {/* Main headline */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <div
              style={{
                color: '#FF9A3C',
                fontSize: 13,
                letterSpacing: '0.20em',
                fontFamily: 'monospace',
                fontWeight: 400,
              }}
            >
              PORTAL · DIGITALITZACIÓ · MALLORCA
            </div>
            <div
              style={{
                color: '#e2e8f0',
                fontSize: 64,
                fontWeight: 900,
                lineHeight: 1.0,
                letterSpacing: '-0.02em',
              }}
            >
              PIMES{' '}
              <span style={{ color: '#FF6B00' }}>AUTOMATITZADES</span>
              {'\n'}EN MENYS DE 48H.
            </div>
            <div
              style={{
                color: '#94a3b8',
                fontSize: 22,
                fontWeight: 400,
                marginTop: 8,
                maxWidth: 700,
              }}
            >
              Webs, automatitzacions, facturació i CRM en un sol portal administrat.
            </div>
          </div>

          {/* Bottom stats bar */}
          <div
            style={{
              display: 'flex',
              gap: 40,
              alignItems: 'center',
              borderTop: '1px solid rgba(255,107,0,0.20)',
              paddingTop: 24,
            }}
          >
            {[
              { label: 'Uptime', value: '99.98%' },
              { label: 'Pimes actives', value: '48+' },
              { label: 'Time to value', value: '< 48h' },
            ].map((stat) => (
              <div key={stat.label} style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                <span style={{ color: '#FF9A3C', fontSize: 22, fontWeight: 800 }}>{stat.value}</span>
                <span style={{ color: '#64748b', fontSize: 12, letterSpacing: '0.10em', fontFamily: 'monospace' }}>
                  {stat.label.toUpperCase()}
                </span>
              </div>
            ))}
            <div
              style={{
                marginLeft: 'auto',
                color: '#8896aa',
                fontSize: 13,
                fontFamily: 'monospace',
                letterSpacing: '0.08em',
              }}
            >
              amgdl.com
            </div>
          </div>
        </div>

        {/* Orange accent bar right */}
        <div style={{ position: 'absolute', top: 80, right: 0, width: 4, height: 120, background: '#FF6B00' }} />
      </div>
    ),
    { width: 1200, height: 630 },
  );
}
