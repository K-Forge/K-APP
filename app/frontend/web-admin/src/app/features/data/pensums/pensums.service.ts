import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClientService } from '../../../core/http/api-client.service';
import type { Pensum } from './pensum.model';

/**
 * /api/catalog/pensums - there is no "list all pensums" endpoint in the contract, only
 * get-by-code, so PensumsPage is a lookup tool rather than a browsable table.
 */
@Injectable({ providedIn: 'root' })
export class PensumsService {
  private readonly api = inject(ApiClientService);

  getByCode(pensumCode: string): Observable<Pensum> {
    return this.api.get<Pensum>(`/api/catalog/pensums/${encodeURIComponent(pensumCode)}`);
  }

  create(pensum: Pensum): Observable<Pensum> {
    return this.api.post<Pensum>('/api/catalog/pensums', pensum);
  }

  replace(pensumCode: string, pensum: Pensum): Observable<Pensum> {
    return this.api.put<Pensum>(`/api/catalog/pensums/${encodeURIComponent(pensumCode)}`, pensum);
  }

  /**
   * Never cascades. A pensum that students are following comes back as `409` saying how
   * many, which the error banner renders from the envelope's `details`.
   */
  delete(pensumCode: string): Observable<void> {
    return this.api.delete(`/api/catalog/pensums/${encodeURIComponent(pensumCode)}`);
  }
}
