/** Paging envelope, identical across every KApp service that paginates (see each spec's PageResponse). */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
