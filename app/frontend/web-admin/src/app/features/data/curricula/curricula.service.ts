import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClientService } from '../../../core/http/api-client.service';
import type { Curriculum } from './curriculum.model';

/**
 * /api/catalog/curricula - there is no "list all curricula" endpoint in the contract, only
 * get-by-code, so CurriculaPage is a lookup tool rather than a browsable table.
 */
@Injectable({ providedIn: 'root' })
export class CurriculaService {
  private readonly api = inject(ApiClientService);

  getByCode(pensumCode: string): Observable<Curriculum> {
    return this.api.get<Curriculum>(`/api/catalog/curricula/${encodeURIComponent(pensumCode)}`);
  }

  create(curriculum: Curriculum): Observable<Curriculum> {
    return this.api.post<Curriculum>('/api/catalog/curricula', curriculum);
  }

  replace(pensumCode: string, curriculum: Curriculum): Observable<Curriculum> {
    return this.api.put<Curriculum>(`/api/catalog/curricula/${encodeURIComponent(pensumCode)}`, curriculum);
  }
}
