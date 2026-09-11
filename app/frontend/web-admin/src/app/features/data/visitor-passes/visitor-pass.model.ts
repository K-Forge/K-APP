/** Mirrors DocumentType in docs/api/auth.openapi.yaml. */
export const DOCUMENT_TYPES = ['CC', 'CE', 'TI', 'PASAPORTE', 'PPT'] as const;
export type DocumentType = (typeof DOCUMENT_TYPES)[number];

/**
 * Mirrors VisitorPass.
 *
 * Once redeemed this carries the visitor's name and identity document — personal data under Ley
 * 1581 de 2012. It is deleted automatically 30 days after redemption, by the database rather than
 * by anything this portal does.
 */
export interface VisitorPass {
  code: string;
  issuedBy: string;
  notes?: string | null;
  createdAt: string;
  redeemableUntil: string;
  redeemed: boolean;
  redeemedAt?: string | null;
  documentType?: DocumentType | null;
  documentNumber?: string | null;
  visitorName?: string | null;
  accessExpiresAt?: string | null;
  purgeAt?: string | null;
}
