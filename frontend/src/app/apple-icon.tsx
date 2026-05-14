import { ImageResponse } from 'next/og';

export const runtime = 'edge';
export const size = { width: 180, height: 180 };
export const contentType = 'image/png';

export default function AppleIcon() {
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
            width: 130,
            height: 130,
            background: '#FF6B00',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            clipPath: 'polygon(calc(100% - 18px) 0%, 100% 18px, 100% 100%, 18px 100%, 0% calc(100% - 18px), 0% 0%)',
          }}
        >
          <span style={{ color: '#0d0d1a', fontWeight: 900, fontSize: 72, fontFamily: 'serif' }}>A</span>
        </div>
      </div>
    ),
    { width: 180, height: 180 },
  );
}
