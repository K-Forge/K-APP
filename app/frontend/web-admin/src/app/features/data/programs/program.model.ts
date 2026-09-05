export const PROGRAM_LEVELS = [
  'PREGRADO',
  'POSGRADO',
  'TECNOLOGIA',
  'MAESTRIA',
  'DOCTORADO',
  'CURSOS_DIPLOMADOS',
  'ESPECIALIZACION',
] as const;

/** Mirrors Program in docs/api/semaphore.openapi.yaml. */
export interface Program {
  code: string;
  name: string;
  faculty: string;
  level: (typeof PROGRAM_LEVELS)[number];
  activePensumCode: string | null;
}

/**
 * Mirrors ProgramRequest - the create/replace payload.
 *
 * `activePensumCode` is absent: it is derived from the curricula that point at the program, not
 * set directly, so accepting it here would let the portal claim a pensum is active when no
 * curriculum says so.
 */
export interface ProgramRequest {
  code: string;
  name: string;
  faculty: string;
  level: (typeof PROGRAM_LEVELS)[number];
}
