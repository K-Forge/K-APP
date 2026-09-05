import { inject, Injectable } from '@angular/core';
import type { Observable } from 'rxjs';
import { ApiClientService } from '../../../core/http/api-client.service';
import type { CurriculumImportReport } from './import.model';

@Injectable({ providedIn: 'root' })
export class CurriculumImportService {
  private readonly api = inject(ApiClientService);

  /**
   * Sends the file as multipart/form-data.
   *
   * <p>The Content-Type header is deliberately not set: the browser has to add it itself so it
   * can append the multipart boundary, and setting it by hand produces a body the server cannot
   * parse.
   */
  upload(file: File, dryRun: boolean): Observable<CurriculumImportReport> {
    const body = new FormData();
    body.append('file', file);
    return this.api.post<CurriculumImportReport>(
      `/api/catalog/curricula/import?dryRun=${dryRun}`,
      body,
    );
  }
}
