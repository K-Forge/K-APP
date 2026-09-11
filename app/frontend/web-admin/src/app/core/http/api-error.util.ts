import { HttpErrorResponse } from '@angular/common/http';
import type { ApiError } from './api-error.model';

/**
 * Normalizes anything an HTTP call can fail with into the one `ApiError` shape the rest of the
 * app renders. The services agree on this envelope (see api-error.model.ts), but two situations
 * never produce it: the request never reaching a server at all (status 0 - wrong base URL, CORS,
 * nothing listening) and a body that fails to parse as JSON. Both are synthesized into the same
 * shape so a component only ever has one thing to render, never a special case for "the server
 * didn't even answer".
 */
export function parseApiError(err: unknown): ApiError {
  const timestamp = new Date().toISOString();

  if (err instanceof HttpErrorResponse) {
    const body = err.error;

    if (isApiErrorShape(body)) {
      return {
        timestamp: typeof body.timestamp === 'string' ? body.timestamp : timestamp,
        status: body.status,
        error: body.error,
        message: body.message,
        path: typeof body.path === 'string' ? body.path : (err.url ?? ''),
        details: Array.isArray(body.details) ? body.details : undefined,
      };
    }

    if (err.status === 0) {
      return {
        timestamp,
        status: 0,
        error: 'Network Error',
        message: `Could not reach ${err.url ?? 'the API'}. Check the base URL in Settings and that the service is running.`,
        path: err.url ?? '',
      };
    }

    return {
      timestamp,
      status: err.status,
      error: err.statusText || `HTTP ${err.status}`,
      message: bodyAsMessage(body) ?? err.message,
      path: err.url ?? '',
    };
  }

  return {
    timestamp,
    status: 0,
    error: 'Unexpected Error',
    message: err instanceof Error ? err.message : String(err),
    path: '',
  };
}

function isApiErrorShape(value: unknown): value is ApiError {
  return (
    isPlainObject(value) &&
    typeof value['status'] === 'number' &&
    typeof value['message'] === 'string' &&
    typeof value['error'] === 'string'
  );
}

function bodyAsMessage(body: unknown): string | null {
  if (typeof body === 'string' && body.trim() !== '') {
    return body;
  }
  if (isPlainObject(body) && typeof body['message'] === 'string') {
    return body['message'];
  }
  return null;
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
