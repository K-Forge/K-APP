export interface CurriculumArea {
  code: string;
  name: string;
  color: string;
  credits: number;
  hours: number;
}

export interface CurriculumCourse {
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

/** Mirrors Curriculum in docs/api/semaphore.openapi.yaml - a whole pensum document. */
export interface Curriculum {
  pensumCode: string;
  programCode: string;
  programName: string;
  faculty: string;
  reform: string;
  status: 'ACTIVE' | 'DRAFT' | 'OBSOLETE';
  totalCredits: number;
  totalHours: number;
  levels: number;
  areas: CurriculumArea[];
  courses: CurriculumCourse[];
}

/** A minimal, valid starting document for the "New curriculum" editor. */
export const CURRICULUM_SKELETON: Curriculum = {
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
