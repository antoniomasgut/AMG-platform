import type { Config } from 'tailwindcss';

const config: Config = {
  content: ['./src/**/*.{js,ts,jsx,tsx,mdx}'],
  theme: {
    extend: {
      colors: {
        /* ── Brand accent ── */
        accent:          '#FF6B00',
        'accent-light':  '#FF9A3C',
        'accent-hover':  '#E05A00',
        'accent-subtle': 'rgba(255,107,0,0.08)',
        'accent-muted':  'rgba(255,107,0,0.12)',

        /* ── Themed surfaces (CSS variables) ── */
        'bg-0': 'var(--color-bg-0)',
        'bg-1': 'var(--color-bg-1)',
        'bg-2': 'var(--color-bg-2)',
        'bg-3': 'var(--color-bg-3)',

        /* ── Themed borders ── */
        'border-subtle': 'var(--color-border-subtle)',
        'border-base':   'var(--color-border-base)',
        'border-medium': 'var(--color-border-medium)',
        'border-strong': 'var(--color-border-strong)',

        /* ── Themed text ── */
        'ink-0': 'var(--color-ink-0)',
        'ink-1': 'var(--color-ink-1)',
        'ink-2': 'var(--color-ink-2)',
        'ink-3': 'var(--color-ink-3)',

        /* ── Semantic: solid ── */
        danger:  '#ef4444',
        success: '#22c55e',
        info:    '#3b82f6',
        warning: '#f59e0b',

        /* ── Semantic: subtle backgrounds ── */
        'danger-subtle':  'rgba(239,68,68,0.08)',
        'success-subtle': 'rgba(34,197,94,0.08)',
        'info-subtle':    'rgba(59,130,246,0.08)',
        'warning-subtle': 'rgba(245,158,11,0.08)',

        /* ── Semantic: light text variants ── */
        'danger-light':  '#fca5a5',
        'success-light': '#86efac',
        'info-light':    '#93c5fd',
        'warning-light': '#fcd34d',
      },

      fontFamily: {
        display: ['Orbitron', 'sans-serif'],
        sans:    ['"Space Grotesk"', 'system-ui', 'sans-serif'],
        mono:    ['"Share Tech Mono"', 'ui-monospace', 'monospace'],
      },

      fontSize: {
        /* Named scale — substitueix els text-[Xpx] inline */
        'label':   ['10px', { lineHeight: '1',    letterSpacing: '0.14em' }],
        'caption': ['11px', { lineHeight: '1.2',  letterSpacing: '0.10em' }],
        'data':    ['12px', { lineHeight: '1.35', letterSpacing: '0.02em' }],
        'ui':      ['13px', { lineHeight: '1.4',  letterSpacing: '0.01em' }],
        /* Escala modular Major Third (×1.25) des de 14px */
        'base':    ['14px', { lineHeight: '1.5' }],
        'md':      ['16px', { lineHeight: '1.5' }],
        'lg':      ['18px', { lineHeight: '1.4' }],
        'xl':      ['20px', { lineHeight: '1.3' }],
        '2xl':     ['24px', { lineHeight: '1.2' }],
        '3xl':     ['30px', { lineHeight: '1.1' }],
        '4xl':     ['36px', { lineHeight: '1.05' }],
        '5xl':     ['48px', { lineHeight: '1' }],
        '6xl':     ['60px', { lineHeight: '1' }],
        '7xl':     ['72px', { lineHeight: '1' }],
      },

      letterSpacing: {
        label:   '0.14em',
        caption: '0.10em',
        badge:   '0.08em',
        wide:    '0.04em',
        wider:   '0.12em',
        widest:  '0.20em',
        display: '-0.02em',
      },
    },
  },
  plugins: [],
};

export default config;
