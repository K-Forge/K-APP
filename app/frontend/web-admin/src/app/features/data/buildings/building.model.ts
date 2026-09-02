export interface Floor {
  level: number;
  name: string;
  planImageUrl: string;
  imageWidth: number;
  imageHeight: number;
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
