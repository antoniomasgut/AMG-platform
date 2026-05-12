import type { Config } from 'tailwindcss';

const config: Config = {
  content: ['./src/**/*.{js,ts,jsx,tsx,mdx}'],
  theme: {
    extend: {
      colors: {
        accent: '#FF6B00',
        'accent-light': '#FF9A3C',
        'bg-0': '#0d0d1a',
        'bg-1': '#13132a',
        'bg-2': '#1a1a2e',
        'bg-3': '#212140',
        danger: '#ff4444',
        success: '#39d353',
        info: '#58a6ff',
        warning: '#f0b429',
        'ink-0': '#e2e8f0',
        'ink-1': '#94a3b8',
        'ink-2': '#64748b',
      },
      fontFamily: {
        display: ['Orbitron', 'sans-serif'],
        sans: ['Rajdhani', 'sans-serif'],
        mono: ['"Share Tech Mono"', 'ui-monospace', 'monospace'],
      },
    },
  },
  plugins: [],
};

export default config;
