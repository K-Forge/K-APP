/**
 * Minimal structural types for the subset of OpenAPI 3.1 the console and role inspector actually
 * read. Not a complete OpenAPI type system on purpose - the five KApp specs are hand-written and
 * well-behaved, so this covers what they use rather than the full spec surface.
 */
export interface OpenApiDocument {
  info: { title: string; version: string };
  paths: Record<string, OpenApiPathItem>;
  components?: {
    schemas?: Record<string, SchemaObject>;
    parameters?: Record<string, ParameterObject>;
    responses?: Record<string, unknown>;
    examples?: Record<string, ExampleObject>;
    securitySchemes?: Record<string, unknown>;
  };
}

export type HttpMethod = 'get' | 'post' | 'put' | 'patch' | 'delete';
export const HTTP_METHODS: HttpMethod[] = ['get', 'post', 'put', 'patch', 'delete'];

export type OpenApiPathItem = {
  parameters?: (ParameterObject | RefObject)[];
} & Partial<Record<HttpMethod, OperationObject>>;

export interface OperationObject {
  operationId?: string;
  summary?: string;
  description?: string;
  tags?: string[];
  parameters?: (ParameterObject | RefObject)[];
  requestBody?: RequestBodyObject | RefObject;
  responses?: Record<string, ResponseObject>;
  security?: unknown[];
}

export interface ParameterObject {
  name: string;
  in: 'path' | 'query' | 'header' | 'cookie';
  required?: boolean;
  description?: string;
  schema?: SchemaObject;
  example?: unknown;
}

export interface RequestBodyObject {
  required?: boolean;
  content?: Record<string, MediaTypeObject>;
}

export interface MediaTypeObject {
  schema?: SchemaObject | RefObject;
  example?: unknown;
  examples?: Record<string, ExampleObject | RefObject>;
}

export interface ExampleObject {
  summary?: string;
  description?: string;
  value?: unknown;
}

export interface ResponseObject {
  description?: string;
  content?: Record<string, MediaTypeObject>;
}

export interface RefObject {
  $ref: string;
}

export interface SchemaObject {
  $ref?: string;
  type?: string | string[];
  format?: string;
  enum?: unknown[];
  items?: SchemaObject | RefObject;
  properties?: Record<string, SchemaObject | RefObject>;
  required?: string[];
  allOf?: (SchemaObject | RefObject)[];
  oneOf?: (SchemaObject | RefObject)[];
  minLength?: number;
  maxLength?: number;
  minimum?: number;
  maximum?: number;
  pattern?: string;
  default?: unknown;
  example?: unknown;
  description?: string;
  minItems?: number;
}

export function isRef(value: unknown): value is RefObject {
  return typeof value === 'object' && value !== null && '$ref' in value;
}
