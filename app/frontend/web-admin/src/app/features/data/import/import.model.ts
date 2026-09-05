/** Mirrors CurriculumImportReport in docs/api/semaphore.openapi.yaml. */
export interface ImportedCurriculum {
  pensumCode: string;
  programCode: string;
  programName: string;
  courses: number;
  declaredCredits: number;
  computedCredits: number;
  declaredHours: number;
  computedHours: number;
  programCreated: boolean;
  curriculumCreated: boolean;
}

export interface CurriculumImportReport {
  dryRun: boolean;
  rowsRead: number;
  curricula: ImportedCurriculum[];
}
