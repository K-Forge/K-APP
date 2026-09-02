import { Injectable } from '@angular/core';
import { SERVICES, type ServiceDescriptor } from './openapi-catalog';
import type { ConsoleOperation, ConsoleParam } from './console-operation.model';
import { deref, resolveSchema } from './openapi-ref.util';
import { HTTP_METHODS, isRef } from './openapi.types';
import type { ExampleObject, OpenApiDocument, ParameterObject, RefObject, SchemaObject } from './openapi.types';
import { buildExample } from './example.util';

/**
 * Turns the five raw OpenAPI documents into a flat list of operations the console and role
 * inspector can render without either of them knowing anything about $ref resolution or path-item
 * level parameter inheritance - both real OpenAPI features these hand-written specs use.
 */
@Injectable({ providedIn: 'root' })
export class OpenApiCatalogService {
  readonly services: ServiceDescriptor[] = SERVICES;

  private readonly cache = new Map<string, ConsoleOperation[]>();

  operationsFor(serviceId: string): ConsoleOperation[] {
    if (this.cache.has(serviceId)) {
      return this.cache.get(serviceId)!;
    }
    const service = this.services.find((s) => s.id === serviceId);
    const operations = service ? flattenOperations(service) : [];
    this.cache.set(serviceId, operations);
    return operations;
  }

  allOperations(): ConsoleOperation[] {
    return this.services.flatMap((service) => this.operationsFor(service.id));
  }
}

function flattenOperations(service: ServiceDescriptor): ConsoleOperation[] {
  const { doc } = service;
  const operations: ConsoleOperation[] = [];

  for (const [path, pathItem] of Object.entries(doc.paths)) {
    const pathLevelParams = (pathItem.parameters ?? []).map((p) => deref<ParameterObject>(doc, p));

    for (const method of HTTP_METHODS) {
      const operation = pathItem[method];
      if (!operation) continue;

      const ownParams = (operation.parameters ?? []).map((p) => deref<ParameterObject>(doc, p));
      const allParams = mergeParams(pathLevelParams, ownParams);

      const requestBody = operation.requestBody ? deref(doc, operation.requestBody) : undefined;
      const jsonBody = requestBody?.content?.['application/json'];
      const bodySchema = jsonBody?.schema ? resolveSchema(doc, jsonBody.schema) : undefined;

      operations.push({
        serviceId: service.id,
        method,
        path,
        operationId: operation.operationId ?? `${method.toUpperCase()} ${path}`,
        summary: operation.summary ?? '',
        description: operation.description ?? '',
        tags: operation.tags ?? [],
        pathParams: allParams.filter((p) => p.in === 'path').map(toConsoleParam),
        queryParams: allParams.filter((p) => p.in === 'query').map(toConsoleParam),
        requestBodySchema: bodySchema,
        requestBodyExample: jsonBody ? firstExample(doc, jsonBody, bodySchema) : undefined,
        security: operation.security,
      });
    }
  }

  return operations.sort((a, b) => a.path.localeCompare(b.path) || a.method.localeCompare(b.method));
}

/** Operation-level parameters override a path-level parameter with the same name+location. */
function mergeParams(pathLevel: ParameterObject[], ownLevel: ParameterObject[]): ParameterObject[] {
  const merged = new Map<string, ParameterObject>();
  for (const param of [...pathLevel, ...ownLevel]) {
    merged.set(`${param.in}:${param.name}`, param);
  }
  return [...merged.values()];
}

function toConsoleParam(param: ParameterObject): ConsoleParam {
  return {
    name: param.name,
    in: param.in as 'path' | 'query',
    required: Boolean(param.required),
    schema: param.schema ?? {},
    description: param.description,
  };
}

function firstExample(
  doc: OpenApiDocument,
  media: { example?: unknown; examples?: Record<string, ExampleObject | RefObject> },
  bodySchema: SchemaObject | undefined,
): unknown {
  if (media.example !== undefined) {
    return media.example;
  }
  const namedExamples = media.examples ? Object.values(media.examples) : [];
  if (namedExamples.length > 0) {
    const first = isRef(namedExamples[0]) ? deref<ExampleObject>(doc, namedExamples[0]) : namedExamples[0];
    if (first.value !== undefined) {
      return first.value;
    }
  }
  return buildExample(doc, bodySchema);
}
