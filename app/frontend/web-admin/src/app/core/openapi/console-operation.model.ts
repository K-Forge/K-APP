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

  /**
   * The operation's `x-roles`, written from what the services actually enforce. An empty array
   * means public; `['SERVICE_ONLY']` means an internal endpoint guarded by a shared token rather
   * than by a role. Absent only on an operation somebody added without it, which falls back to
   * reading the description's prose - see `resolveRoleRequirement`.
   */
  xRoles?: string[];
}
