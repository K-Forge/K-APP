import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClientService } from '../../../core/http/api-client.service';
import type { Program } from './program.model';

/** /api/catalog/programs - read-only in this contract, see ProgramsPage for the reason. */
@Injectable({ providedIn: 'root' })
export class ProgramsService {
  private readonly api = inject(ApiClientService);

  list(): Observable<Program[]> {
    return this.api.get<Program[]>('/api/catalog/programs');
  }
}
