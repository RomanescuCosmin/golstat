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
        // Accentul paginii de piete: violet, ca selectiile din filtre sa nu se confunde nici cu
        // albastrul de navigatie, nici cu verdele barelor de sansa.
        piata: 'rgb(var(--gs-piata) / <alpha-value>)',
        accent: 'rgb(var(--gs-accent) / <alpha-value>)',
        win: 'rgb(var(--gs-win) / <alpha-value>)',
        draw: 'rgb(var(--gs-draw) / <alpha-value>)',
      },
      borderRadius: {
        // Radius de brand (design system GolStat): carduri 18, butoane 12, inputuri 14.
        card: '18px',
        btn: '12px',
        input: '14px',
      },
      boxShadow: {
        // Umbra premium subtila, non-glossy (design system GolStat).
        card: '0 8px 30px rgba(15, 23, 42, 0.06)',
        cardHover: '0 12px 36px rgba(15, 23, 42, 0.10)',
      },
      keyframes: {
        // Plutire subtila a mingii-marca (idle).
        floaty: {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-3px)' },
        },
        // Aparitie subtila a continutului dupa incarcare.
        fadeIn: {
          from: { opacity: '0', transform: 'translateY(6px)' },
          to: { opacity: '1', transform: 'translateY(0)' },
        },
      },
      animation: {
        floaty: 'floaty 3s ease-in-out infinite',
        'fade-in': 'fadeIn 0.25s ease-out',
      },
    },
  },
  plugins: [],
};
