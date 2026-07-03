/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        bg: 'rgb(var(--gs-bg) / <alpha-value>)',
        card: 'rgb(var(--gs-card) / <alpha-value>)',
        ink: 'rgb(var(--gs-ink) / <alpha-value>)',
        ink2: 'rgb(var(--gs-ink2) / <alpha-value>)',
        line: 'rgb(var(--gs-line) / <alpha-value>)',
        primary: 'rgb(var(--gs-primary) / <alpha-value>)',
        accent: 'rgb(var(--gs-accent) / <alpha-value>)',
        win: 'rgb(var(--gs-win) / <alpha-value>)',
        draw: 'rgb(var(--gs-draw) / <alpha-value>)',
      },
      boxShadow: {
        card: '0 1px 2px 0 rgb(16 24 40 / 0.04)',
      },
    },
  },
  plugins: [],
};
