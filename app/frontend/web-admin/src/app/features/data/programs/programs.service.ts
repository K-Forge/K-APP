import { inject, Injectable } from '@angular/core';
import type { Observable } from 'rxjs';
import { ApiClientService } from '../../../core/http/api-client.service';
import type { Program, ProgramRequest } from './program.model';

@Injectable({ providedIn: 'root' })
export class ProgramsService {
  private readonly api = inject(ApiClientService);

  list(): Observable<Program[]> {
    return this.api.get<Program[]>('/api/catalog/programs');
  }

  create(request: ProgramRequest): Observable<Program> {
    return this.api.post<Program>('/api/catalog/programs', request);
  }

  /** PUT, not PATCH: the contract replaces the whole program. */
  replace(code: string, request: ProgramRequest): Observable<Program> {
    return this.api.put<Program>(`/api/catalog/programs/${encodeURIComponent(code)}`, request);
  }

  /**
   * Never cascades. A program that still has curricula comes back as `409` naming them, which
   * the error banner renders from the envelope's `details`.
   */
  delete(code: string): Observable<void> {
    return this.api.delete(`/api/catalog/programs/${encodeURIComponent(code)}`);
  }
}
