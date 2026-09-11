/**
 * The roles an invitation code may grant.
 *
 * `ROLE_ADMIN` is deliberately absent, and the server refuses it too. These codes are seeded in
 * the repository and read out to whole intakes: a code that could mint an administrator would
 * mean anyone who can read the repository can escalate.
 */
export const INVITATION_ROLES = ['ROLE_STUDENT', 'ROLE_PROFESSOR'] as const;
export type InvitationRole = (typeof INVITATION_ROLES)[number];

/**
 * Mirrors InvitationCode in docs/api/auth.openapi.yaml — and now actually does.
 *
 * <p>It used to declare `remainingUses`, `createdAt` and `updatedAt` as well. The server sends
 * none of the three, so the Remaining column rendered empty on every row for as long as the
 * screen has existed: TypeScript believed the field was a number because the interface said so,
 * and nothing at runtime disagreed.
 */
export interface InvitationCode {
  code: string;
  role: InvitationRole;
  maxUses: number;
  timesUsed: number;
  active: boolean;
  expiresAt: string | null;
  notes: string | null;
}

/** How many accounts a code can still create. Derived, because the server does not send it. */
export function remainingUses(code: InvitationCode): number {
  return Math.max(0, code.maxUses - code.timesUsed);
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
