import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { ApiConfigService } from '../config/api-config.service';
import { parseApiError } from './api-error.util';
import { AppHttpError } from './api-http-error';

export type QueryParams = Record<string, string | number | boolean | undefined | null>;

/**
 * The one HTTP service every data-management feature calls through. It resolves paths against
 * the currently configured gateway (ApiConfigService, so a runtime base-URL change applies to
 * every request without touching feature code) and normalizes every failure into AppHttpError so
 * a component only ever needs one catch block to get a renderable ApiError.
 */
@Injectable({ providedIn: 'root' })
export class ApiClientService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(ApiConfigService);

  get<T>(path: string, params?: QueryParams): Observable<T> {
    return this.http.get<T>(this.url(path), { params: toHttpParams(params) }).pipe(catchError(rethrow));
  }

  post<T>(path: string, body: unknown): Observable<T> {
    return this.http.post<T>(this.url(path), body).pipe(catchError(rethrow));
  }

  put<T>(path: string, body: unknown, params?: QueryParams): Observable<T> {
    return this.http.put<T>(this.url(path), body, { params: toHttpParams(params) }).pipe(catchError(rethrow));
  }

  patch<T>(path: string, body: unknown, params?: QueryParams): Observable<T> {
    return this.http.patch<T>(this.url(path), body, { params: toHttpParams(params) }).pipe(catchError(rethrow));
  }

  delete(path: string, params?: QueryParams): Observable<void> {
    return this.http.delete<void>(this.url(path), { params: toHttpParams(params) }).pipe(catchError(rethrow));
  }

  private url(path: string): string {
    return `${this.config.baseUrl()}${path.startsWith('/') ? path : `/${path}`}`;
  }
}

function toHttpParams(params?: QueryParams): HttpParams {
  let httpParams = new HttpParams();
  if (!params) {
    return httpParams;
  }
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      httpParams = httpParams.set(key, String(value));
    }
  }
  return httpParams;
}

function rethrow(err: unknown) {
  return throwError(() => new AppHttpError(parseApiError(err)));
}
