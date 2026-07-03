import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  // sockjs-client se asteapta la `global` (Node); sub Vite il mapam pe `window`.
  define: { global: 'window' },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/ws': { target: 'http://localhost:8080', ws: true },
    },
  },
});
