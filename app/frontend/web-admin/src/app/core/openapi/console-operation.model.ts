import type { HttpMethod, SchemaObject } from './openapi.types';

export interface ConsoleParam {
  name: string;
  in: 'path' | 'query';
  required: boolean;
  schema: SchemaObject;
  description?: string;
}

/** One row of the operation picker: a single method+path, flattened and ref-resolved. */
export interface ConsoleOperation {
  serviceId: string;
  method: HttpMethod;
  path: string;
  operationId: string;
  summary: string;
  description: string;
  tags: string[];
  pathParams: ConsoleParam[];
  queryParams: ConsoleParam[];
  requestBodySchema?: SchemaObject;
  requestBodyExample?: unknown;
  /** Empty array means the spec marks this operation public (`security: []`). */
  security?: unknown[];
}
