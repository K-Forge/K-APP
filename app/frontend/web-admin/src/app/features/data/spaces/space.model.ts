export const SPACE_TYPES = [
  'CLASSROOM',
  'LAB',
  'AUDITORIUM',
  'OFFICE',
  'LIBRARY',
  'CAFETERIA',
  'RESTROOM',
  'WELLBEING',
  'OTHER',
] as const;

export type SpaceType = (typeof SPACE_TYPES)[number];

/** Mirrors Space in docs/api/map.openapi.yaml. */
export interface Space {
  id: string;
  code: string;
  name: string;
  type: SpaceType;
  buildingId: string;
  buildingCode: string;
  campus: string;
  floorLevel: number;
  aliases: string[];
  x: number;
  y: number;
  capacity?: number;
}

/** Mirrors SpaceRequest - the create/update payload. */
export interface SpaceRequest {
  code: string;
  name: string;
  type: SpaceType;
  buildingCode: string;
  floorLevel: number;
  aliases: string[];
  x: number;
  y: number;
  capacity?: number;
}

export interface SpaceSearchFilters {
  q: string;
  page: number;
  size: number;
  campus?: string;
  type?: SpaceType;
  buildingCode?: string;
}
