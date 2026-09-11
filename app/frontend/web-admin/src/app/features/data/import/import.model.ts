/** Mirrors PensumImportReport in docs/api/semaphore.openapi.yaml. */
export interface ImportedPensum {
  pensumCode: string;
  programCode: string;
  programName: string;
  courses: number;
  declaredCredits: number;
  computedCredits: number;
  declaredHours: number;
  computedHours: number;
  programCreated: boolean;
  pensumCreated: boolean;
}

export interface PensumImportReport {
  dryRun: boolean;
  rowsRead: number;
  pensums: ImportedPensum[];
}
