import { ImageResponse } from 'next/og';

export const runtime = 'edge';
export const size = { width: 32, height: 32 };
export const contentType = 'image/png';

export default function Icon() {
  return new ImageResponse(
    (
      <div
        style={{
          width: '100%',
          height: '100%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          background: '#0d0d1a',
        }}
      >
        <div
          style={{
            width: 26,
            height: 26,
            background: '#FF6B00',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            clipPath: 'polygon(calc(100% - 6px) 0%, 100% 6px, 100% 100%, 6px 100%, 0% calc(100% - 6px), 0% 0%)',
          }}
        >
          <span style={{ color: '#0d0d1a', fontWeight: 900, fontSize: 16, fontFamily: 'serif' }}>A</span>
        </div>
      </div>
    ),
    { width: 32, height: 32 },
  );
}
