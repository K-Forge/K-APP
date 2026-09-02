import type { OpenApiDocument, SchemaObject } from './openapi.types';
import { resolveSchema } from './openapi-ref.util';

/**
 * Builds a plausible JSON value from a schema when the spec has no worked example to fall back
 * on. Used to seed the console's body editor so a developer edits a realistic skeleton instead of
 * starting from `{}` and reverse-engineering the shape from the spec by hand.
 */
export function buildExample(doc: OpenApiDocument, schema: SchemaObject | undefined, depth = 0): unknown {
  if (!schema || depth > 6) {
    return null;
  }

  const resolved = resolveSchema(doc, schema);
  if (!resolved) {
    return null;
  }

  if (resolved.example !== undefined) {
    return resolved.example;
  }
  if (resolved.default !== undefined) {
    return resolved.default;
  }
  if (resolved.enum?.length) {
    return resolved.enum[0];
  }
  if (resolved.oneOf?.length) {
    return buildExample(doc, resolveSchema(doc, resolved.oneOf[0]), depth + 1);
  }

  const type = Array.isArray(resolved.type) ? resolved.type.find((t) => t !== 'null') : resolved.type;

  switch (type) {
    case 'object': {
      const result: Record<string, unknown> = {};
      for (const [name, propSchema] of Object.entries(resolved.properties ?? {})) {
        result[name] = buildExample(doc, resolveSchema(doc, propSchema), depth + 1);
      }
      return result;
    }
    case 'array':
      return [buildExample(doc, resolveSchema(doc, resolved.items), depth + 1)];
    case 'integer':
    case 'number':
      return resolved.minimum ?? 0;
    case 'boolean':
      return false;
    case 'string':
      return stringExample(resolved);
    default:
      // properties present without an explicit type is common enough in these specs to treat
      // as an implicit object rather than falling through to null.
      return resolved.properties ? buildExample(doc, { ...resolved, type: 'object' }, depth) : null;
  }
}

function stringExample(schema: SchemaObject): string {
  switch (schema.format) {
    case 'date-time':
      return new Date().toISOString();
    case 'date':
      return new Date().toISOString().slice(0, 10);
    case 'email':
      return 'user@konradlorenz.edu.co';
    case 'uuid':
      return '00000000-0000-0000-0000-000000000000';
    case 'uri':
      return 'https://example.com';
    default:
      return '';
  }
}
