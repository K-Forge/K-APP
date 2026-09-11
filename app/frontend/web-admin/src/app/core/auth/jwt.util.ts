import type { DecodedJwt, DecodedToken } from './jwt.model';

/**
 * Decodes a JWT for DISPLAY only - it never checks the RS256 signature. Trusting an unverified
 * token would be a real vulnerability in a service that authorizes requests, but this portal
 * only ever forwards the token as-is to the gateway, which validates it against auth-service's
 * JWKS. Here it is purely "what does this token actually say", which is the whole point of the
 * identity screen: a developer can see a token is malformed or expired without guessing.
 *
 * Returns null instead of throwing so a bad token degrades to "can't read this" in the UI rather
 * than crashing the app.
 */
export function decodeJwt(token: string): DecodedToken | null {
  if (typeof token !== 'string' || token.trim() === '') {
    return null;
  }

  const parts = token.split('.');
  if (parts.length !== 3) {
    return null;
  }

  try {
    const header = JSON.parse(base64UrlDecode(parts[0]));
    const claims = JSON.parse(base64UrlDecode(parts[1]));

    if (!isPlainObject(header) || !isPlainObject(claims)) {
      return null;
    }

    return { raw: token, header, claims: claims as DecodedJwt };
  } catch {
    return null;
  }
}

/** `exp` is seconds since epoch per RFC 7519; Date.now() is milliseconds. */
export function isTokenExpired(claims: Pick<DecodedJwt, 'exp'>, nowMs = Date.now()): boolean {
  return typeof claims.exp !== 'number' || claims.exp * 1000 <= nowMs;
}

export function secondsUntilExpiry(claims: Pick<DecodedJwt, 'exp'>, nowMs = Date.now()): number {
  if (typeof claims.exp !== 'number') {
    return 0;
  }
  return Math.floor((claims.exp * 1000 - nowMs) / 1000);
}

export function rolesOf(claims: Pick<DecodedJwt, 'roles'>): string[] {
  return Array.isArray(claims.roles) ? claims.roles.filter((r): r is string => typeof r === 'string') : [];
}

function base64UrlDecode(segment: string): string {
  const base64 = segment.replace(/-/g, '+').replace(/_/g, '/');
  const padLength = (4 - (base64.length % 4)) % 4;
  const padded = base64 + '='.repeat(padLength);
  const binary = atob(padded);
  const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0));
  return new TextDecoder('utf-8').decode(bytes);
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
