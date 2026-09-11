/** Claims every KApp access token carries, per docs/api/auth.openapi.yaml. */
export interface DecodedJwt {
  /** Subject - the account id, matching UserProfile.id in the User API. */
  sub: string;
  email: string;
  roles: string[];
  /** Issuer URL of the signing auth-service. */
  iss: string;
  /** Issued-at, seconds since epoch. */
  iat: number;
  /** Expiry, seconds since epoch. */
  exp: number;
  /** Anything else the token happens to carry - shown as-is in the raw claims view. */
  [claim: string]: unknown;
}

export interface DecodedToken {
  raw: string;
  header: Record<string, unknown>;
  claims: DecodedJwt;
}
