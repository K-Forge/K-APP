import { describe, expect, it } from 'vitest';
import { decodeJwt, isTokenExpired, rolesOf, secondsUntilExpiry } from './jwt.util';

/** Builds a real base64url JWT string from plain objects - no signing, decodeJwt never checks it. */
function makeToken(payload: Record<string, unknown>, header: Record<string, unknown> = { alg: 'RS256', typ: 'JWT' }): string {
  const encode = (obj: unknown) => base64UrlEncode(JSON.stringify(obj));
  return `${encode(header)}.${encode(payload)}.fake-signature`;
}

function base64UrlEncode(text: string): string {
  const bytes = new TextEncoder().encode(text);
  let binary = '';
  bytes.forEach((byte) => (binary += String.fromCharCode(byte)));
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

const nowSeconds = Math.floor(Date.now() / 1000);

describe('decodeJwt', () => {
  it('decodes a well-formed token into header and claims', () => {
    const token = makeToken({
      sub: '3f8a1c2e-7b4d-4e5a-9c6f-2d1b8e0a4c73',
      email: 'brian.vargasc@konradlorenz.edu.co',
      roles: ['ROLE_STUDENT'],
      iss: 'https://auth.kapp.konradlorenz.edu.co',
      iat: nowSeconds,
      exp: nowSeconds + 3600,
    });

    const decoded = decodeJwt(token);

    expect(decoded).not.toBeNull();
    expect(decoded?.header['alg']).toBe('RS256');
    expect(decoded?.claims.sub).toBe('3f8a1c2e-7b4d-4e5a-9c6f-2d1b8e0a4c73');
    expect(decoded?.claims.email).toBe('brian.vargasc@konradlorenz.edu.co');
    expect(decoded?.claims.roles).toEqual(['ROLE_STUDENT']);
  });

  it('decodes claims containing non-ASCII text correctly', () => {
    const token = makeToken({ sub: 'x', roles: [], iss: 'x', iat: 0, exp: 0, name: 'María Fernanda' });

    const decoded = decodeJwt(token);

    expect(decoded?.claims['name']).toBe('María Fernanda');
  });

  it('returns null for a string that is not a JWT at all', () => {
    expect(decodeJwt('not-a-token')).toBeNull();
    expect(decodeJwt('')).toBeNull();
  });

  it('returns null when a segment is not valid base64url', () => {
    expect(decodeJwt('not base64.also not base64.sig')).toBeNull();
  });

  it('returns null when a segment decodes to base64 but not JSON', () => {
    const notJson = base64UrlEncode('this is not json');
    expect(decodeJwt(`${notJson}.${notJson}.sig`)).toBeNull();
  });

  it('returns null when a segment decodes to a JSON array rather than an object', () => {
    const header = base64UrlEncode(JSON.stringify({ alg: 'RS256' }));
    const arrayPayload = base64UrlEncode(JSON.stringify(['not', 'an', 'object']));
    expect(decodeJwt(`${header}.${arrayPayload}.sig`)).toBeNull();
  });

  it('rejects a token with the wrong number of segments', () => {
    expect(decodeJwt('only.two')).toBeNull();
    expect(decodeJwt('a.b.c.d')).toBeNull();
  });
});

describe('isTokenExpired', () => {
  it('is false for a token whose exp is in the future', () => {
    expect(isTokenExpired({ exp: nowSeconds + 60 })).toBe(false);
  });

  it('is true for a token whose exp is in the past', () => {
    expect(isTokenExpired({ exp: nowSeconds - 60 })).toBe(true);
  });

  it('is true exactly at the expiry instant', () => {
    const nowMs = nowSeconds * 1000;
    expect(isTokenExpired({ exp: nowSeconds }, nowMs)).toBe(true);
  });

  it('treats a missing or non-numeric exp as expired rather than valid forever', () => {
    expect(isTokenExpired({} as { exp: number })).toBe(true);
  });
});

describe('secondsUntilExpiry', () => {
  it('returns the remaining whole seconds', () => {
    const nowMs = nowSeconds * 1000;
    expect(secondsUntilExpiry({ exp: nowSeconds + 90 }, nowMs)).toBe(90);
  });

  it('returns 0 when exp is missing', () => {
    expect(secondsUntilExpiry({} as { exp: number })).toBe(0);
  });
});

describe('rolesOf', () => {
  it('returns the roles array when present', () => {
    expect(rolesOf({ roles: ['ROLE_ADMIN', 'ROLE_STUDENT'] })).toEqual(['ROLE_ADMIN', 'ROLE_STUDENT']);
  });

  it('filters out non-string entries', () => {
    expect(rolesOf({ roles: ['ROLE_ADMIN', 42, null] as unknown as string[] })).toEqual(['ROLE_ADMIN']);
  });

  it('returns an empty array when roles is missing or malformed', () => {
    expect(rolesOf({} as { roles: string[] })).toEqual([]);
    expect(rolesOf({ roles: 'ROLE_ADMIN' as unknown as string[] })).toEqual([]);
  });
});
