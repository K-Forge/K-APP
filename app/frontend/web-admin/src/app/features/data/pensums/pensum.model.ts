export interface PensumArea {
  code: string;
  name: string;
  color: string;
  credits: number;
  hours: number;
}

export interface PensumCourse {
  code: string | null;
  pensumItemCode: string;
  name: string;
  level: number;
  credits: number;
  weeklyHours: number;
  totalHours?: number;
  area: string;
  isElectiveSlot: boolean;
  prerequisites: string[];
}

/** Mirrors Pensum in docs/api/semaphore.openapi.yaml - a whole pensum document. */
export interface Pensum {
  pensumCode: string;
  programCode: string;
  programName: string;
  faculty: string;
  reform: string;
  status: 'ACTIVE' | 'DRAFT' | 'OBSOLETE';
  totalCredits: number;
  totalHours: number;
  levels: number;
  areas: PensumArea[];
  courses: PensumCourse[];
}

/** A minimal, valid starting document for the "New pensum" editor. */
export const PENSUM_SKELETON: Pensum = {
  pensumCode: '',
  programCode: '',
  programName: '',
  faculty: '',
  reform: '',
  status: 'DRAFT',
  totalCredits: 0,
  totalHours: 0,
  levels: 9,
  areas: [],
  courses: [],
};

/**
 * A pensum without its courses — what `GET /api/catalog/pensums` returns.
 *
 * <p>Enough to fill a picker and a listing; the full document is one call away. Twenty-four
 * pensums of sixty courses each would be fifteen hundred objects a dropdown has no use for.
 */
export interface PensumSummary {
  pensumCode: string;
  programCode: string;
  programName: string;
  faculty: string;
  reform: string;
  status: 'DRAFT' | 'ACTIVE' | 'OBSOLETE';
  totalCredits: number;
  totalHours: number;
  levels: number;
  courses: number;
}
