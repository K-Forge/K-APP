/** Mirrors SpaceType in docs/api/map.openapi.yaml, in the same order. */
export const SPACE_TYPES = [
  'CLASSROOM',
  'LAB',
  'AUDITORIUM',
  'LIBRARY',
  'CAFETERIA',
  'RESTROOM',
  'OFFICE',
  'ADMIN_OFFICE',
  'WELLBEING',
  'TERRACE',
  'ELEVATOR',
  'STAIRS',
  'CORRIDOR',
  'ENTRANCE',
  'OTHER',
] as const;

export type SpaceType = (typeof SPACE_TYPES)[number];

/** The types a space's `accessVia` may point at: the things people actually travel through. */
export const CIRCULATION_TYPES: readonly SpaceType[] = ['ELEVATOR', 'STAIRS', 'ENTRANCE'];

export const WINGS = ['NORTE', 'SUR', 'CENTRAL'] as const;
export type Wing = (typeof WINGS)[number];

/** Mirrors Space in docs/api/map.openapi.yaml. */
export interface Space {
  id: string;
  code: string;
  /** The code without its wing suffix. Derived server-side; never sent. */
  baseCode: string;
  wing?: Wing | null;
  name: string;
  type: SpaceType;
  buildingId: string;
  buildingCode: string;
  campus: string;
  floorLevel: number;
  aliases: string[];
  gridRow: number;
  gridColumn: number;
  rowSpan: number;
  colSpan: number;
  accessVia?: string | null;
  capacity?: number;
}

/**
 * Mirrors SpaceRequest - the create/update payload.
 *
 * `baseCode` is absent on purpose: the server derives it from the code and refuses to accept it.
 */
export interface SpaceRequest {
  code: string;
  wing?: Wing | null;
  name: string;
  type: SpaceType;
  buildingCode: string;
  floorLevel: number;
  aliases: string[];
  gridRow: number;
  gridColumn: number;
  rowSpan?: number;
  colSpan?: number;
  accessVia?: string | null;
  capacity?: number;
}

export interface SpaceSearchFilters {
  /** Omit to list rather than search: the server lists by the other filters. */
  q?: string;
  page: number;
  size: number;
  campus?: string;
  type?: SpaceType;
  buildingCode?: string;
  wing?: Wing;
}
