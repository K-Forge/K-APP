export interface ConsoleHeader {
  key: string;
  value: string;
}

export interface ConsoleResult {
  status: number;
  statusText: string;
  timeMs: number;
  headers: ConsoleHeader[];
  body: unknown;
  isError: boolean;
}

export interface HistoryEntry {
  id: string;
  timestamp: number;
  serviceId: string;
  method: string;
  /** Path template, e.g. /api/map/buildings/{code} - used to find the operation again. */
  path: string;
  pathValues: Record<string, string>;
  queryValues: Record<string, string>;
  bodyText: string;
  result: ConsoleResult;
}
