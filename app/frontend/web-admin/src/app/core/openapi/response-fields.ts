import { resolveSchema } from './openapi-ref.util';
import type { OpenApiDocument, SchemaObject } from './openapi.types';

/**
 * One line of "what this field means", read straight out of the contract.
 *
 * <p>`path` is dotted, with `[]` for an array's items — `keys[].kid` — so a nested response
 * reads as a list rather than needing a tree widget.
 */
export interface ResponseField {
  path: string;
  type: string;
  required: boolean;
  description?: string;
  enumValues?: string[];
  example?: string;
}

/** Arrays of objects nest; without a ceiling a recursive schema would not terminate. */
const MAX_DEPTH = 4;

/**
 * Flattens the JSON response schema of an operation into field descriptions.
 *
 * <p>This exists because the console rendered the raw response and nothing else, which is fine
 * until the response is a JWKS and the answer is `{"kty":"RSA","e":"AQAB","kid":…}`. Those names
 * are already explained in the contract — every one of them — and nobody was ever shown it.
 *
 * <p>Read from the contract rather than written out again here on purpose: a second copy of the
 * explanation is a second copy to keep true, and the one that drifts is always the one further
 * from the code.
 */
export function describeResponse(
  doc: OpenApiDocument,
  operation: { responses?: Record<string, unknown> } | undefined,
): ResponseField[] {
  const schema = successSchema(doc, operation);
  return schema ? flatten(doc, schema, '', 0, []) : [];
}

function successSchema(
  doc: OpenApiDocument,
  operation: { responses?: Record<string, unknown> } | undefined,
): SchemaObject | undefined {
  const responses = operation?.responses ?? {};
  // The first 2xx that carries JSON. 204s carry nothing, and a 200 is not always first in the map.
  const code = Object.keys(responses)
    .filter((c) => /^2\d\d$/.test(c))
    .sort()
    .find((c) => hasJson(doc, responses[c]));
  if (!code) {
    return undefined;
  }
  const response = deref(doc, responses[code]) as { content?: Record<string, { schema?: unknown }> };
  return resolveSchema(doc, response.content?.['application/json']?.schema as SchemaObject);
}

function hasJson(doc: OpenApiDocument, response: unknown): boolean {
  const resolved = deref(doc, response) as { content?: Record<string, unknown> } | undefined;
  return !!resolved?.content?.['application/json'];
}

function deref(doc: OpenApiDocument, value: unknown): unknown {
  if (value && typeof value === 'object' && '$ref' in (value as object)) {
    const ref = (value as { $ref: string }).$ref;
    const segments = ref.replace(/^#\//, '').split('/');
    let node: unknown = doc;
    for (const segment of segments) {
      node = (node as Record<string, unknown>)?.[segment];
    }
    return node;
  }
  return value;
}

function flatten(
  doc: OpenApiDocument,
  schema: SchemaObject,
  prefix: string,
  depth: number,
  seen: SchemaObject[],
): ResponseField[] {
  if (depth > MAX_DEPTH || seen.includes(schema)) {
    return [];
  }
  const trail = [...seen, schema];

  if (schema.type === 'array' && schema.items) {
    const items = resolveSchema(doc, schema.items);
    return items ? flatten(doc, items, `${prefix}[]`, depth, trail) : [];
  }

  const properties = schema.properties;
  if (!properties) {
    return prefix
      ? [{ path: prefix, type: typeName(schema), required: false, description: schema.description, example: exampleOf(schema) }]
      : [];
  }

  const required = new Set(schema.required ?? []);
  const fields: ResponseField[] = [];

  for (const [name, raw] of Object.entries(properties)) {
    const child = resolveSchema(doc, raw);
    if (!child) continue;
    const path = prefix ? `${prefix}.${name}` : name;

    fields.push({
      path,
      type: typeName(child),
      required: required.has(name),
      description: child.description?.trim(),
      enumValues: child.enum?.map(String),
      example: exampleOf(child),
    });

    const nested =
      child.type === 'array' && child.items ? resolveSchema(doc, child.items) : child.properties ? child : undefined;
    if (nested?.properties) {
      fields.push(...flatten(doc, nested, child.type === 'array' ? `${path}[]` : path, depth + 1, trail));
    }
  }

  return fields;
}

function typeName(schema: SchemaObject): string {
  const type = Array.isArray(schema.type) ? schema.type.filter((t) => t !== 'null').join(' | ') : schema.type;
  if (type === 'array') {
    return 'array';
  }
  return schema.format ? `${type} (${schema.format})` : (type ?? 'any');
}

function exampleOf(schema: SchemaObject): string | undefined {
  const value = (schema as { example?: unknown }).example;
  if (value === undefined || value === null) {
    return undefined;
  }
  const text = typeof value === 'string' ? value : JSON.stringify(value);
  // A JWKS modulus is 340 characters of base64 and tells you nothing after the first few.
  return text.length > 60 ? `${text.slice(0, 57)}…` : text;
}
