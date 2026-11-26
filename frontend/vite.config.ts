/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// vite.config ruleaza in Node; declaram `process` ambient ca sa nu cerem @types/node doar pentru asta.
declare const process: { env: Record<string, string | undefined> };

// Tinta backend-ului; suprascrie cu VITE_API_TARGET (ex. alt port) fara sa atingi configul.
const API_TARGET = process.env.VITE_API_TARGET ?? 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  // sockjs-client se asteapta la `global` (Node); sub Vite il mapam pe `window`.
  define: { global: 'window' },
  server: {
    // xfwd: trimite IP-ul real in X-Forwarded-For (limitele de rata ale autentificarii sunt per IP)
    proxy: {
      '/api': { target: API_TARGET, xfwd: true },
      '/ws': { target: API_TARGET, ws: true, xfwd: true },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    css: false,
  },
});
