import type { ApiError } from './api-error.model';

/** Thrown by ApiClientService so callers can catch one type and read `.apiError` for display. */
export class AppHttpError extends Error {
  constructor(readonly apiError: ApiError) {
    super(apiError.message);
    this.name = 'AppHttpError';
  }
}
