import { ImageResponse } from 'next/og';

export const runtime = 'edge';
export const alt = 'AMG Digitalitzacions · Agent IA per a negocis locals a Mallorca';
export const size = { width: 1200, height: 630 };
export const contentType = 'image/png';

export default async function OgImage() {
  return new ImageResponse(
    (
      <div
        style={{
          width: 1200,
          height: 630,
          backgroundColor: '#0a0a0a',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
          padding: '64px 72px',
          fontFamily: 'sans-serif',
          position: 'relative',
        }}
      >
        {/* Accent glow top-right */}
        <div
          style={{
            position: 'absolute',
            top: -100,
            right: -100,
            width: 500,
            height: 500,
            borderRadius: '50%',
            background: 'radial-gradient(circle, rgba(255,107,0,0.25) 0%, transparent 70%)',
          }}
        />

        {/* Top: logo + domain */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <div
            style={{
              width: 10,
              height: 10,
              backgroundColor: '#ff6b00',
            }}
          />
          <span
            style={{
              fontSize: 18,
              color: '#666',
              letterSpacing: 4,
              textTransform: 'uppercase',
            }}
          >
            amgdl.com
          </span>
        </div>

        {/* Center: headline */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          <div
            style={{
              fontSize: 72,
              fontWeight: 900,
              color: '#f0f0f0',
              lineHeight: 1.05,
              letterSpacing: -2,
              textTransform: 'uppercase',
            }}
          >
            Agent IA per a{' '}
            <span style={{ color: '#ff6b00' }}>negocis locals</span>
            {'\n'}a Mallorca.
          </div>
          <div
            style={{
              fontSize: 24,
              color: '#888',
              maxWidth: 700,
              lineHeight: 1.4,
            }}
          >
            Web + WhatsApp automatitzat + reserves + seguiment de clients. Des de 59€/mes.
          </div>
        </div>

        {/* Bottom: contact + badge */}
        <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <span style={{ fontSize: 16, color: '#555', letterSpacing: 2, textTransform: 'uppercase' }}>
              Consulta gratuïta
            </span>
            <span style={{ fontSize: 22, color: '#ff6b00', fontWeight: 700 }}>
              +34 614 492 062
            </span>
          </div>
          <div
            style={{
              border: '1px solid #333',
              padding: '12px 24px',
              display: 'flex',
              alignItems: 'center',
              gap: 10,
            }}
          >
            <div style={{ width: 8, height: 8, backgroundColor: '#ff6b00' }} />
            <span style={{ fontSize: 14, color: '#666', letterSpacing: 3, textTransform: 'uppercase' }}>
              AMG Digitalitzacions
            </span>
          </div>
        </div>
      </div>
    ),
    { ...size },
  );
}
