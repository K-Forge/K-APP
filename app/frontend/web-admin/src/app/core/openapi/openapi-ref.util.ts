import type { OpenApiDocument, RefObject, SchemaObject } from './openapi.types';
import { isRef } from './openapi.types';

/** Resolves a local `#/a/b/c` JSON pointer against the document. Every $ref in these five specs is local. */
export function resolveRef<T>(doc: OpenApiDocument, ref: string): T {
  const segments = ref.replace(/^#\//, '').split('/');
  let node: unknown = doc;
  for (const segment of segments) {
    if (typeof node !== 'object' || node === null) {
      throw new Error(`Cannot resolve $ref "${ref}": stopped at "${segment}"`);
    }
    node = (node as Record<string, unknown>)[segment];
  }
  return node as T;
}

/** Resolves a value that might itself be a $ref, one level. */
export function deref<T>(doc: OpenApiDocument, value: T | RefObject): T {
  return isRef(value) ? resolveRef<T>(doc, value.$ref) : value;
}

/**
 * Resolves `allOf` composition and a top-level $ref so callers get one flat schema to read
 * `properties` off. Deep enough for these specs (SpaceDetail/FloorDetail use one level of
 * allOf); does not attempt to merge oneOf, which the app treats as "pick the first branch".
 */
export function resolveSchema(doc: OpenApiDocument, schema: SchemaObject | RefObject | undefined): SchemaObject | undefined {
  if (!schema) {
    return undefined;
  }
  const resolved = deref(doc, schema);
  if (resolved.allOf) {
    const merged: SchemaObject = { type: 'object', properties: {}, required: [] };
    for (const part of resolved.allOf) {
      const partSchema = resolveSchema(doc, part);
      if (!partSchema) continue;
      Object.assign(merged.properties!, partSchema.properties ?? {});
      merged.required = [...(merged.required ?? []), ...(partSchema.required ?? [])];
    }
    return merged;
  }
  return resolved;
}
