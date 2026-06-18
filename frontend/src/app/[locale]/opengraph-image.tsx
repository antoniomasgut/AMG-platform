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
          fontFamily: 'sans-serif',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        {/* Barra vertical esquerra taronja */}
        <div
          style={{
            position: 'absolute',
            left: 0,
            top: 0,
            width: 6,
            height: 630,
            backgroundColor: '#ff6b00',
          }}
        />

        {/* Línies de graella de fons — efecte tècnic */}
        {[120, 240, 360, 480].map((y) => (
          <div
            key={y}
            style={{
              position: 'absolute',
              left: 0,
              top: y,
              width: 1200,
              height: 1,
              backgroundColor: 'rgba(255,255,255,0.04)',
            }}
          />
        ))}
        {[200, 400, 600, 800, 1000].map((x) => (
          <div
            key={x}
            style={{
              position: 'absolute',
              left: x,
              top: 0,
              width: 1,
              height: 630,
              backgroundColor: 'rgba(255,255,255,0.04)',
            }}
          />
        ))}

        {/* Glow taronja fons dreta */}
        <div
          style={{
            position: 'absolute',
            right: -80,
            bottom: -80,
            width: 520,
            height: 520,
            borderRadius: '50%',
            background: 'radial-gradient(circle, rgba(255,107,0,0.18) 0%, transparent 70%)',
          }}
        />

        {/* "AMG" watermark fons — gran i esvaït */}
        <div
          style={{
            position: 'absolute',
            right: 48,
            top: 48,
            fontSize: 280,
            fontWeight: 900,
            color: 'rgba(255,107,0,0.06)',
            letterSpacing: -8,
            lineHeight: 1,
          }}
        >
          AMG
        </div>

        {/* Contingut principal */}
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            padding: '56px 72px 56px 80px',
            width: '100%',
          }}
        >
          {/* Top: badge */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
            <div style={{ width: 8, height: 8, backgroundColor: '#ff6b00' }} />
            <span
              style={{
                fontSize: 13,
                color: '#555',
                letterSpacing: 5,
                textTransform: 'uppercase',
              }}
            >
              AMG Digitalitzacions · amgdl.com
            </span>
          </div>

          {/* Centre: titular principal */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
            <div
              style={{
                fontSize: 88,
                fontWeight: 900,
                color: '#f2f2f2',
                lineHeight: 0.95,
                letterSpacing: -4,
                textTransform: 'uppercase',
              }}
            >
              Agent IA
            </div>
            <div
              style={{
                fontSize: 88,
                fontWeight: 900,
                color: '#ff6b00',
                lineHeight: 0.95,
                letterSpacing: -4,
                textTransform: 'uppercase',
              }}
            >
              per a negocis
            </div>
            <div
              style={{
                fontSize: 88,
                fontWeight: 900,
                color: '#f2f2f2',
                lineHeight: 0.95,
                letterSpacing: -4,
                textTransform: 'uppercase',
              }}
            >
              locals.
            </div>
          </div>

          {/* Bottom: separador + info */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <div
              style={{
                width: 48,
                height: 2,
                backgroundColor: '#ff6b00',
              }}
            />
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', gap: 40, alignItems: 'center' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                  <span style={{ fontSize: 12, color: '#444', letterSpacing: 3, textTransform: 'uppercase' }}>
                    Web + WhatsApp + Reserves
                  </span>
                  <span style={{ fontSize: 18, color: '#ccc', fontWeight: 600 }}>
                    Des de 59€/mes · Mallorca
                  </span>
                </div>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4 }}>
                <span style={{ fontSize: 12, color: '#444', letterSpacing: 3, textTransform: 'uppercase' }}>
                  Consulta gratuïta
                </span>
                <span style={{ fontSize: 22, color: '#ff6b00', fontWeight: 700, letterSpacing: -0.5 }}>
                  +34 614 492 062
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    ),
    { ...size },
  );
}
