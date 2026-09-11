import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClientService } from '../../../core/http/api-client.service';
import type { Pensum, PensumSummary } from './pensum.model';

/** /api/catalog/pensums — list, read, write and delete the plans of study. */
@Injectable({ providedIn: 'root' })
export class PensumsService {
  private readonly api = inject(ApiClientService);

  /** Summaries only — see PensumSummary. Not paginated: the catalogue is a few dozen rows. */
  list(): Observable<PensumSummary[]> {
    return this.api.get<PensumSummary[]>('/api/catalog/pensums');
  }

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
