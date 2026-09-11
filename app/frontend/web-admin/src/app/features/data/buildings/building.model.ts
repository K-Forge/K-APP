/** Mirrors GridPoint in docs/api/map.openapi.yaml. Zero-based, row 0 at the top as drawn. */
export interface GridPoint {
  row: number;
  col: number;
}

/** Mirrors Corridor. Drawn in the offline grid editor, not here - see FloorFormComponent. */
export interface Corridor {
  code: string;
  name: string;
  color: string;
  path: GridPoint[];
}

/**
 * Mirrors Floor. A floor is a grid the client draws, not a plan image it overlays: it used to
 * carry planImageUrl and the image's pixel dimensions, and this model followed that change.
 */
export interface Floor {
  level: number;
  name: string;
  gridRows: number;
  gridColumns: number;
  corridors?: Corridor[];
}

/** Mirrors Building in docs/api/map.openapi.yaml. */
export interface Building {
  id: string;
  code: string;
  name: string;
  campus: string;
  description?: string;
  floors: Floor[];
}

/** Mirrors BuildingRequest - the create/update payload. `id` is server-generated. */
export interface BuildingRequest {
  code: string;
  name: string;
  campus: string;
  description?: string;
  floors: Floor[];
}
