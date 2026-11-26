import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError, cerere } from './client';

function raspuns(status: number, corp?: unknown, antete: Record<string, string> = {}): Response {
  return new Response(corp === undefined ? null : JSON.stringify(corp), { status, headers: antete });
}

function stergeCookies() {
  document.cookie.split('; ').forEach((c) => {
    const nume = c.split('=')[0];
    if (nume) document.cookie = `${nume}=; Max-Age=0; Path=/`;
  });
}

describe('cerere', () => {
  const fetchMock = vi.fn<typeof fetch>();

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock);
    stergeCookies();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    fetchMock.mockReset();
  });

  test('GET fara header CSRF, cu credentiale same-origin', async () => {
    fetchMock.mockResolvedValueOnce(raspuns(200, { ok: 1 }));

    await expect(cerere('/v1/x')).resolves.toEqual({ ok: 1 });

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/v1/x');
    expect(init?.credentials).toBe('same-origin');
    expect((init?.headers as Record<string, string>)['X-XSRF-TOKEN']).toBeUndefined();
  });

  test('POST trimite tokenul din cookie in header si corpul JSON', async () => {
    document.cookie = 'XSRF-TOKEN=abc123; Path=/';
    fetchMock.mockResolvedValueOnce(raspuns(204));

    await expect(cerere('/v1/auth/deconectare', { method: 'POST', body: { a: 1 } })).resolves.toBeUndefined();

    const init = fetchMock.mock.calls[0][1]!;
    const antete = init.headers as Record<string, string>;
    expect(antete['X-XSRF-TOKEN']).toBe('abc123');
    expect(antete['Content-Type']).toBe('application/json');
    expect(init.body).toBe('{"a":1}');
  });

  test('POST fara cookie XSRF cere intai /v1/auth/eu', async () => {
    fetchMock.mockImplementationOnce(async () => {
      document.cookie = 'XSRF-TOKEN=nou; Path=/';
      return raspuns(204);
    });
    fetchMock.mockResolvedValueOnce(raspuns(202));

    await cerere('/v1/auth/parola-uitata', { method: 'POST', body: { email: 'a@b.ro' } });

    expect(fetchMock.mock.calls[0][0]).toBe('/api/v1/auth/eu');
    expect((fetchMock.mock.calls[1][1]!.headers as Record<string, string>)['X-XSRF-TOKEN']).toBe('nou');
  });

  test('403 csrf: reincearca o singura data cu token proaspat', async () => {
    document.cookie = 'XSRF-TOKEN=vechi; Path=/';
    fetchMock
      .mockResolvedValueOnce(raspuns(403, { status: 403, title: 'Sesiune expirată', cod: 'csrf' }))
      .mockImplementationOnce(async () => {
        document.cookie = 'XSRF-TOKEN=proaspat; Path=/';
        return raspuns(204);
      })
      .mockResolvedValueOnce(raspuns(200, { id: 1 }));

    await expect(cerere('/v1/auth/autentificare', { method: 'POST', body: {} })).resolves.toEqual({ id: 1 });

    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect((fetchMock.mock.calls[2][1]!.headers as Record<string, string>)['X-XSRF-TOKEN']).toBe('proaspat');
  });

  test('403 csrf repetat: nu intra in bucla', async () => {
    document.cookie = 'XSRF-TOKEN=vechi; Path=/';
    fetchMock.mockImplementation(async (url) =>
      String(url).endsWith('/eu')
        ? (() => {
            document.cookie = 'XSRF-TOKEN=tot-respins; Path=/';
            return raspuns(204);
          })()
        : raspuns(403, { status: 403, title: 'Sesiune expirată', cod: 'csrf' }),
    );

    await expect(cerere('/v1/auth/autentificare', { method: 'POST', body: {} })).rejects.toMatchObject({ cod: 'csrf' });
    expect(fetchMock.mock.calls.filter(([u]) => !String(u).endsWith('/eu'))).toHaveLength(2);
  });

  test('eroare ProblemDetail: cod, erori si retryAfter', async () => {
    document.cookie = 'XSRF-TOKEN=t; Path=/';
    fetchMock.mockResolvedValueOnce(
      raspuns(400, { status: 400, title: 'Date invalide', detail: 'Verifică', erori: { email: 'invalid' }, cod: 'x' }),
    );

    const eroare = await cerere('/v1/auth/inregistrare', { method: 'POST', body: {} }).catch((e) => e);

    expect(eroare).toBeInstanceOf(ApiError);
    expect(eroare).toMatchObject({ status: 400, cod: 'x', erori: { email: 'invalid' } });
  });

  test('429: retryAfter din antet cand lipseste din corp', async () => {
    fetchMock.mockResolvedValueOnce(raspuns(429, { status: 429, title: 'Prea multe' }, { 'Retry-After': '120' }));

    await expect(cerere('/v1/x')).rejects.toMatchObject({ status: 429, retryAfter: 120 });
  });

  test('eroare de retea → ApiError 0', async () => {
    fetchMock.mockRejectedValueOnce(new TypeError('failed'));

    await expect(cerere('/v1/x')).rejects.toMatchObject({ status: 0, title: 'Conexiune eșuată' });
  });
});
