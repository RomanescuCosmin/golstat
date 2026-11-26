import { describe, expect, test, vi } from 'vitest';
import { citesteSiCurataLinkEmail } from './linkEmail';

const TOKEN = 'AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-abcde';

function locatie(hash: string): Location {
  return { hash, pathname: '/meci/5', search: '?x=1' } as Location;
}

function istoric() {
  return { state: { k: 1 }, replaceState: vi.fn() } as unknown as History & { replaceState: ReturnType<typeof vi.fn> };
}

describe('citesteSiCurataLinkEmail', () => {
  test('confirmare: intoarce tokenul si curata fragmentul pastrand calea', () => {
    const h = istoric();

    expect(citesteSiCurataLinkEmail(locatie(`#confirmare=${TOKEN}`), h)).toEqual({ tip: 'confirmare', token: TOKEN });
    expect(h.replaceState).toHaveBeenCalledWith({ k: 1 }, '', '/meci/5?x=1');
  });

  test('resetare', () => {
    expect(citesteSiCurataLinkEmail(locatie(`#resetare=${TOKEN}`), istoric())).toEqual({ tip: 'resetare', token: TOKEN });
  });

  test('token malformat: curata URL-ul dar nu intoarce nimic', () => {
    const h = istoric();

    expect(citesteSiCurataLinkEmail(locatie('#resetare=<script>'), h)).toBeNull();
    expect(h.replaceState).toHaveBeenCalled();
  });

  test('alt fragment: nu atinge istoricul', () => {
    const h = istoric();

    expect(citesteSiCurataLinkEmail(locatie('#sectiune'), h)).toBeNull();
    expect(h.replaceState).not.toHaveBeenCalled();
  });
});
