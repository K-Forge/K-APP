/**
 * The roles an invitation code may grant.
 *
 * `ROLE_ADMIN` is deliberately absent, and the server refuses it too. These codes are seeded in
 * the repository and read out to whole intakes: a code that could mint an administrator would
 * mean anyone who can read the repository can escalate.
 */
export const INVITATION_ROLES = ['ROLE_STUDENT', 'ROLE_PROFESSOR'] as const;
export type InvitationRole = (typeof INVITATION_ROLES)[number];

/** Mirrors InvitationCode in docs/api/auth.openapi.yaml. */
export interface InvitationCode {
  code: string;
  role: InvitationRole;
  maxUses: number;
  timesUsed: number;
  remainingUses: number;
  active: boolean;
  expiresAt: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Mirrors InvitationCodeRequest. */
export interface InvitationCodeRequest {
  code: string;
  role: InvitationRole;
  maxUses: number;
  expiresAt?: string | null;
  notes?: string | null;
}

/** Mirrors InvitationCodeStatusRequest - the only thing a code's PATCH accepts. */
export interface InvitationCodeStatusRequest {
  active: boolean;
}
